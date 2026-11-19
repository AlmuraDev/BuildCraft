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


# The current mod's _constants.json (1.12.2 recipe constants: "#name" items)
CONSTANTS = {}


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
        if len(out) == 1:
            return out[0]
        if any(not isinstance(i, str) or i.startswith('#') for i in out):
            # A plain list may only hold item ids
            return {'neoforge:ingredient_type': 'neoforge:compound', 'children': out}
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
    if item == 'forge:bucketfilled':
        # 1.12.2's universal bucket holding a fluid: the fluid's buckets, in every heat value (BCEnergyFluids)
        fluid = value.get('nbt', {}).get('FluidName')
        if fluid is None:
            warnings.append('%s: filled bucket without a fluid' % where)
            return 'minecraft:bucket'
        return ['buildcraftenergy:bucket_' + fluid] + ['buildcraftenergy:bucket_%s_heat_%d' % (fluid, h)
                                                       for h in (1, 2)]
    if item.startswith('#') and ':' not in item:
        if item[1:] in CONSTANTS:
            return ingredient(CONSTANTS[item[1:]], warnings, where)
        return ingredient({'type': 'forge:ore_dict', 'ore': item[1:]}, warnings, where)
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


# Conditions 26.3 needs that 1.12.2 handled at runtime (disabled objects): "mod/file.json" -> conditions
EXTRA_CONDITIONS = {
    'buildcraftenergy/rf_engine.json': [{'type': 'buildcraftcore:engine_enabled', 'engine': 'rf'}],
    'buildcraftenergy/mj_dynamo.json': [{'type': 'neoforge:item_exists', 'item': 'buildcraftenergy:mj_dynamo'}],
}


def conditions(data, where=None):
    out = list(EXTRA_CONDITIONS.get(where, []))
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
    conds = conditions(data, where)
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
    'buildcraftenergy': {
        'mj_dynamo': self_drop('buildcraftenergy:mj_dynamo'),
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


# ---- Transport: pipes were crafted in code (BCTransportRecipes) ----

def _pipe_id(name):
    return 'buildcrafttransport:' + name


def _coloured(item, colour, strict_plain=False):
    """An ingredient for a pipe of the given colour (or an uncoloured one if colour is None)."""
    if colour is None:
        return {'neoforge:ingredient_type': 'neoforge:components', 'items': item, 'components': {}, 'strict': True}
    return {'neoforge:ingredient_type': 'neoforge:components', 'items': item,
            'components': {'buildcrafttransport:colour': colour}}


def _pipe_result(item, count, colour):
    out = {'id': item}
    if count != 1:
        out['count'] = count
    if colour is not None:
        out['components'] = {'buildcrafttransport:colour': colour}
    return out


# pipe -> (left, right) materials, as 1.12.2's addPipeRecipe
PIPES = {
    'pipe_wood_item': ('#minecraft:planks', '#minecraft:planks'),
    'pipe_cobble_item': ('#c:cobblestones', '#c:cobblestones'),
    'pipe_stone_item': ('#c:stones', '#c:stones'),
    'pipe_quartz_item': ('#c:storage_blocks/quartz', '#c:storage_blocks/quartz'),
    'pipe_iron_item': ('#c:ingots/iron', '#c:ingots/iron'),
    'pipe_gold_item': ('#c:ingots/gold', '#c:ingots/gold'),
    'pipe_clay_item': ('minecraft:clay', 'minecraft:clay'),
    'pipe_sandstone_item': ('#c:sandstone/blocks', '#c:sandstone/blocks'),
    'pipe_void_item': ('#c:dyes/black', '#c:dusts/redstone'),
    'pipe_obsidian_item': ('minecraft:obsidian', 'minecraft:obsidian'),
    'pipe_diamond_item': ('minecraft:diamond', 'minecraft:diamond'),
    'pipe_lapis_item': ('minecraft:lapis_block', 'minecraft:lapis_block'),
    'pipe_daizuli_item': ('minecraft:lapis_block', 'minecraft:diamond'),
    'pipe_diamond_wood_item': ('#minecraft:planks', 'minecraft:diamond'),
    'pipe_stripes_item': ('#c:gears/gold', '#c:gears/gold'),
}

# (from, to, additional), as 1.12.2's addPipeUpgradeRecipe
UPGRADES = [('pipe_diamond_wood_item', 'pipe_emzuli_item', 'minecraft:lapis_block')]
for m in ['wood', 'cobble', 'stone', 'quartz', 'iron', 'gold', 'clay', 'sandstone', 'void', 'obsidian', 'diamond',
          'diamond_wood']:
    UPGRADES.append(('pipe_%s_item' % m, 'pipe_%s_fluid' % m, 'buildcrafttransport:waterproof'))
for m in ['wood', 'cobble', 'stone', 'quartz', 'iron', 'gold', 'sandstone', 'diamond', 'diamond_wood']:
    UPGRADES.append(('pipe_%s_item' % m, 'pipe_%s_power' % m, '#c:dusts/redstone'))
for m in ['wood', 'cobble', 'stone']:
    UPGRADES.append(('pipe_%s_power' % m, 'pipe_%s_rf' % m, '#c:dusts/redstone'))


def transport_recipes():
    out = {}
    for pipe, (left, right) in PIPES.items():
        item = _pipe_id(pipe)
        for colour in [None] + DYE_BY_DAMAGE:
            glass = '#c:glass_blocks/colorless' if colour is None else 'minecraft:%s_stained_glass' % colour
            out['%s_%s' % (pipe, colour or 'colorless')] = {
                'type': 'minecraft:crafting_shaped',
                'category': 'misc',
                'group': item,
                'key': {'l': left, 'g': glass, 'r': right},
                'pattern': ['lgr'],
                'result': _pipe_result(item, 8, colour),
                'neoforge:conditions': exists(item),
            }
    for frm, to, additional in UPGRADES:
        f_item, t_item = _pipe_id(frm), _pipe_id(to)
        for colour in [None] + DYE_BY_DAMAGE:
            suffix = colour or 'colorless'
            conds = [{'type': 'neoforge:item_exists', 'item': f_item}, {'type': 'neoforge:item_exists', 'item': t_item}]
            out['%s_%s' % (to, suffix)] = {
                'type': 'minecraft:crafting_shapeless',
                'category': 'misc',
                'group': t_item,
                'ingredients': [_coloured(f_item, colour), additional],
                'result': _pipe_result(t_item, 1, colour),
                'neoforge:conditions': conds,
            }
            out['%s_%s_undo' % (to, suffix)] = {
                'type': 'minecraft:crafting_shapeless',
                'category': 'misc',
                'group': t_item,
                'ingredients': [_coloured(t_item, colour)],
                'result': _pipe_result(f_item, 1, colour),
                'neoforge:conditions': conds,
            }
    return out


CODE_RECIPES['buildcrafttransport'] = transport_recipes()
# TODO(26.3 silicon batch): wire (8 of a colour from redstone + dye) is an assembly table recipe.

LOOT['buildcrafttransport'] = {
    'filtered_buffer': self_drop('buildcrafttransport:filtered_buffer'),
    # the pipe holder computes its own drops (BlockPipeHolder.getDrops)
}


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
        CONSTANTS.clear()
        consts = os.path.join(src, '_constants.json')
        if os.path.isfile(consts):
            with open(consts, encoding='utf-8') as f:
                for c in json.load(f):
                    CONSTANTS[c['name']] = c['ingredient']
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
