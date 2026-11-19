"""
Converts the 1.12.2 recipe JSON of the given mods (buildcraft_resources/assets/<mod>/recipes/*.json) into 26.3 recipes
(buildcraft_resources/data/<mod>/recipe/*.json):

  * forge:ore_shaped / forge:ore_shapeless / minecraft:crafting_* -> minecraft:crafting_shaped / crafting_shapeless
  * ore dictionary names -> common item tags (the same mapping as buildcraft.lib.misc.OreDictTags: keep them in step)
  * 1.12.2 damage values on vanilla items -> their 26.3 items; on BuildCraft blocks -> the block_state component
  * minecraft:item_exists conditions -> neoforge:item_exists, so recipes of disabled items are skipped

It also writes recipes that 1.12.2 built in code (CODE_RECIPES), block loot tables (LOOT) and the item tags BuildCraft's items had as ore
dictionary entries (TAGS).

    python tools/port-26.3/recipes.py buildcraftlib buildcraftcore
"""
import json
import os
import re
import sys

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..'))
RES = os.path.join(ROOT, 'buildcraft_resources')

PREFIXES = {
    'gear': 'gears', 'ingot': 'ingots', 'nugget': 'nuggets', 'dust': 'dusts', 'plate': 'plates', 'gem': 'gems',
    'ore': 'ores', 'block': 'storage_blocks', 'rod': 'rods', 'crop': 'crops', 'seed': 'seeds', 'dye': 'dyes',
    'glass': 'glass_blocks', 'pane': 'glass_panes',
}
SPECIAL = {
    'stickWood': 'c:rods/wooden', 'plankWood': 'minecraft:planks', 'logWood': 'minecraft:logs',
    'slabWood': 'minecraft:wooden_slabs', 'stairWood': 'minecraft:wooden_stairs', 'treeSapling': 'minecraft:saplings',
    'treeLeaves': 'minecraft:leaves', 'chest': 'c:chests', 'chestWood': 'c:chests/wooden',
    'cobblestone': 'c:cobblestones', 'stone': 'c:stones', 'sand': 'c:sands', 'gravel': 'c:gravels',
    'obsidian': 'c:obsidians', 'netherrack': 'c:netherracks', 'wool': 'minecraft:wool', 'string': 'c:strings',
    'leather': 'c:leathers', 'feather': 'c:feathers', 'gunpowder': 'c:gunpowders', 'enderpearl': 'c:ender_pearls',
    'slimeball': 'c:slime_balls', 'blockGlass': 'c:glass_blocks', 'paneGlass': 'c:glass_panes',
    'blockGlassColorless': 'c:glass_blocks/colorless', 'paneGlassColorless': 'c:glass_panes/colorless',
}
# Ore names that were a single item rather than a tag
SPECIAL_ITEMS = {'paper': 'minecraft:paper'}

# 1.12.2 dye damage -> colour
DYE_BY_DAMAGE = ['black', 'red', 'green', 'brown', 'blue', 'purple', 'cyan', 'light_gray', 'gray', 'pink', 'lime',
                 'yellow', 'light_blue', 'magenta', 'orange', 'white']
# 1.12.2 metadata colour (wool, glass...) -> colour
COLOUR_BY_META = list(reversed(DYE_BY_DAMAGE))
COLOURED_VANILLA = {'minecraft:wool': 'wool', 'minecraft:stained_glass': 'stained_glass',
                    'minecraft:stained_glass_pane': 'stained_glass_pane', 'minecraft:stained_hardened_clay': 'terracotta',
                    'minecraft:carpet': 'carpet', 'minecraft:concrete': 'concrete'}
# BuildCraft blocks whose 1.12.2 item damage became a block state value: block -> (property, [value by damage])
BLOCK_STATE_ITEMS = {
    'buildcraftcore:engine': ('type', ['wood', 'stone', 'iron', 'creative', 'rf']),
    'buildcraftcore:decorated': ('decoration_type', ['destroy', 'blueprint', 'template', 'paper', 'leather',
                                                     'laser_back']),
    'buildcraftcore:spring': ('type', ['water', 'oil']),
}


def ore_to_tag(name):
    if name in SPECIAL:
        return SPECIAL[name]
    for prefix, folder in PREFIXES.items():
        if len(name) > len(prefix) and name.startswith(prefix) and name[len(prefix)].isupper():
            return 'c:%s/%s' % (folder, re.sub(r'(?<!^)(?=[A-Z])', '_', name[len(prefix):]).lower())
    return None


def ingredient(value, warnings, where):
    """1.12.2 ingredient JSON -> 26.3 ingredient (an item id or "#tag", or a list of them)."""
    if isinstance(value, list):
        out = []
        for v in value:
            i = ingredient(v, warnings, where)
            out.extend(i if isinstance(i, list) else [i])
        return out
    if isinstance(value, str):
        return value
    if value.get('type') == 'forge:ore_dict':
        ore = value['ore']
        if ore in SPECIAL_ITEMS:
            return SPECIAL_ITEMS[ore]
        tag = ore_to_tag(ore)
        if tag is None:
            warnings.append('%s: unknown ore name %s' % (where, ore))
            return '#c:unknown/' + ore
        return '#' + tag
    item = value['item']
    data = value.get('data')
    if data is None or data == 32767:
        return item
    if item == 'minecraft:dye':
        return '#c:dyes/' + DYE_BY_DAMAGE[data]
    if item in COLOURED_VANILLA:
        return 'minecraft:%s_%s' % (COLOUR_BY_META[data], COLOURED_VANILLA[item])
    if data == 0:
        return item
    warnings.append('%s: dropped damage %d on %s' % (where, data, item))
    return item


def result(value, warnings, where):
    out = {'id': value['item']}
    if value.get('count', 1) != 1:
        out['count'] = value['count']
    data = value.get('data')
    if value['item'] in BLOCK_STATE_ITEMS and data is not None:
        prop, values = BLOCK_STATE_ITEMS[value['item']]
        out['components'] = {'minecraft:block_state': {prop: values[data]}}
    elif data:
        warnings.append('%s: dropped result damage %d on %s' % (where, data, value['item']))
    return out


def conditions(data):
    out = []
    for c in data.get('conditions', []):
        if c.get('type') == 'minecraft:item_exists':
            out.append({'type': 'neoforge:item_exists', 'item': c['item']})
    return out


def convert(data, warnings, where):
    kind = data.get('type', '')
    if kind in ('forge:ore_shaped', 'minecraft:crafting_shaped'):
        out = {
            'type': 'minecraft:crafting_shaped',
            'category': 'misc',
            'key': {k: ingredient(v, warnings, where) for k, v in data['key'].items()},
            'pattern': data['pattern'],
            'result': result(data['result'], warnings, where),
        }
    elif kind in ('forge:ore_shapeless', 'minecraft:crafting_shapeless'):
        out = {
            'type': 'minecraft:crafting_shapeless',
            'category': 'misc',
            'ingredients': [ingredient(v, warnings, where) for v in data['ingredients']],
            'result': result(data['result'], warnings, where),
        }
    else:
        warnings.append('%s: unknown recipe type %s, skipped' % (where, kind))
        return None
    if 'group' in data:
        out['group'] = data['group']
    conds = conditions(data)
    if conds:
        out['neoforge:conditions'] = conds
    return out


def exists(item):
    return [{'type': 'neoforge:item_exists', 'item': item}]


def paint_colouring(colour):
    return {
        'type': 'minecraft:crafting_shapeless',
        'category': 'misc',
        'group': 'buildcraftcore:paintbrush_colouring',
        'ingredients': ['buildcraftcore:paintbrush', '#c:dyes/' + colour],
        'result': {'id': 'buildcraftcore:paintbrush', 'components': {'buildcraftcore:paint_colour': colour}},
        'neoforge:conditions': exists('buildcraftcore:paintbrush'),
    }


# Recipes 1.12.2 registered in code (BCCoreRecipes): mod -> {name: recipe}
CODE_RECIPES = {
    'buildcraftcore': dict(
        [('paintbrush', {
            'type': 'minecraft:crafting_shaped',
            'category': 'misc',
            'key': {'i': '#c:strings', 's': '#c:rods/wooden', 'g': '#c:gears/wood', 'w': '#minecraft:wool'},
            'pattern': [' iw', ' gi', 's  '],
            'result': {'id': 'buildcraftcore:paintbrush'},
            'neoforge:conditions': exists('buildcraftcore:paintbrush'),
        }),
         ('decorated_laser_back', {
             'type': 'minecraft:crafting_shaped',
             'category': 'building',
             'key': {'s': 'minecraft:obsidian', 'c': 'minecraft:redstone_block'},
             'pattern': ['sss', 'scs', 'sss'],
             'result': {'id': 'buildcraftcore:decorated', 'count': 16,
                        'components': {'minecraft:block_state': {'decoration_type': 'laser_back'}}},
             'neoforge:conditions': exists('buildcraftcore:decorated'),
         })]
        + [('paintbrush_' + c, paint_colouring(c)) for c in DYE_BY_DAMAGE]),
}

# Item tags BuildCraft's items had as ore dictionary names: tag -> items (all optional, as items can be disabled)
TAGS = {
    'c:gears/wood': ['buildcraftcore:gear_wood'],
    'c:gears/stone': ['buildcraftcore:gear_stone'],
    'c:gears/iron': ['buildcraftcore:gear_iron'],
    'c:gears/gold': ['buildcraftcore:gear_gold'],
    'c:gears/diamond': ['buildcraftcore:gear_diamond'],
    'c:gears': ['#c:gears/wood', '#c:gears/stone', '#c:gears/iron', '#c:gears/gold', '#c:gears/diamond'],
}


def self_drop(block, state_props=None):
    """Loot table: the block drops itself (keeping the given block state properties on the item)."""
    entry = {'type': 'minecraft:item', 'name': block}
    if state_props:
        entry['modifier'] = [{'type': 'minecraft:copy_state', 'block': block, 'properties': state_props}]
    return {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'entries': [entry], 'condition': {'type': 'minecraft:survives_explosion'}}],
        'random_sequence': block.split(':')[0] + ':blocks/' + block.split(':')[1],
    }


# Block loot tables (26.3 drops come from data): mod -> {block path: loot table}
LOOT = {
    'buildcraftcore': {
        'decorated': self_drop('buildcraftcore:decorated', ['decoration_type']),
        'engine': self_drop('buildcraftcore:engine', ['type']),
        'marker_volume': self_drop('buildcraftcore:marker_volume'),
        'marker_path': self_drop('buildcraftcore:marker_path'),
        'power_tester': self_drop('buildcraftcore:power_tester'),
    },
}


def convert_item_predicate(pred, warnings, where):
    """1.12.2 item predicate {"item": x, "data": d} -> 26.3 {"items": x}."""
    out = {}
    if 'item' in pred:
        out['items'] = pred['item']
    if 'data' in pred:
        warnings.append('%s: dropped item predicate damage on %s' % (where, pred.get('item')))
    for k in ('count',):
        if k in pred:
            out[k] = pred[k]
    return out


def convert_advancement(data, warnings, where):
    display = data.get('display')
    if display:
        icon = display.get('icon')
        if icon and 'item' in icon:
            display['icon'] = {'id': icon['item']}
        bg = display.get('background')
        if bg:
            m = re.match(r'([\w.-]+):textures/(.+)\.png$', bg)
            if m:
                display['background'] = m.group(1) + ':' + m.group(2)
    for crit in data.get('criteria', {}).values():
        cond = crit.get('conditions')
        if cond and 'items' in cond:
            cond['items'] = [convert_item_predicate(p, warnings, where) for p in cond['items']]
    return data


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


def main(mods):
    warnings = []
    count = 0
    for mod in mods:
        src = os.path.join(RES, 'assets', mod, 'recipes')
        if os.path.isdir(src):
            for fn in sorted(os.listdir(src)):
                if not fn.endswith('.json') or fn.startswith('_'):
                    continue
                where = mod + '/' + fn
                with open(os.path.join(src, fn), encoding='utf-8') as f:
                    data = json.load(f)
                out = convert(data, warnings, where)
                if out is not None:
                    write_json(os.path.join(RES, 'data', mod, 'recipe', fn), out)
                    count += 1
        for name, recipe in CODE_RECIPES.get(mod, {}).items():
            write_json(os.path.join(RES, 'data', mod, 'recipe', name + '.json'), recipe)
            count += 1
        adv = os.path.join(RES, 'assets', mod, 'advancements')
        if os.path.isdir(adv):
            for fn in sorted(os.listdir(adv)):
                if fn.endswith('.json'):
                    with open(os.path.join(adv, fn), encoding='utf-8') as f:
                        data = json.load(f)
                    write_json(os.path.join(RES, 'data', mod, 'advancement', fn),
                               convert_advancement(data, warnings, mod + '/' + fn))
        for name, table in LOOT.get(mod, {}).items():
            write_json(os.path.join(RES, 'data', mod, 'loot_table', 'blocks', name + '.json'), table)
    for tag, values in TAGS.items():
        if not any(v.lstrip('#').split(':')[0] in mods or v.startswith('#') for v in values):
            continue
        ns, path = tag.split(':', 1)
        entries = [{'id': v, 'required': False} for v in values]
        write_json(os.path.join(RES, 'data', ns, 'tags', 'item', path + '.json'), {'values': entries})
    print('%d recipes written' % count)
    for w in warnings:
        print('  warning: ' + w)


if __name__ == '__main__':
    main(sys.argv[1:])
