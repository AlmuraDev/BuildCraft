"""
Converts BuildCraft's 1.12.2 client resources in buildcraft_resources/assets to 26.3:

  * blockstates: Forge's "forge_marker" format (and 1.12.2 vanilla files) become vanilla 26.3 variant files, with model
    ids pointing at models/block/ as 1.12.2 implied ("buildcraftcore:marker_volume" -> "buildcraftcore:block/marker_volume").
  * atlases: 26.3 only stitches textures listed by atlas sources, so BuildCraft's 1.12.2 texture folders are added to the
    block and item atlases (assets/minecraft/atlases/*.json).
  * items: the client item definitions (assets/<mod>/items/<id>.json) of the items in ITEMS below.

Safe to run again: converted blockstates are recognised and left alone. Only the mods named on the command line have
their blockstates converted (Forge "submodel" parts need multipart by hand, so check each module's warnings):

    python tools/port-26.3/resources.py buildcraftlib buildcraftcore
"""
import itertools
import json
import os
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..')
ASSETS = os.path.normpath(os.path.join(ROOT, 'buildcraft_resources', 'assets'))

# Texture folders sprites are looked up in. Block atlas: everything drawn in the world or by BuildCraft's sprite
# holders (triggers, icons, lasers...). Item atlas: item textures.
BLOCK_ATLAS_DIRS = ['blocks', 'icons', 'triggers', 'lasers', 'addons', 'filler', 'filling', 'gates', 'pipes', 'plugs',
                    'wires', 'station', 'entities']
ITEM_ATLAS_DIRS = ['items']

DYES = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple',
        'blue', 'brown', 'green', 'red', 'black']
# 1.12.2 model files named the light gray dye "silver"
DYE_MODEL = {d: ('silver' if d == 'light_gray' else d) for d in DYES}


def model(ns_path):
    return {'type': 'minecraft:model', 'model': ns_path}


def select_component(component, cases, fallback):
    return {
        'type': 'minecraft:select',
        'property': 'minecraft:component',
        'component': component,
        'cases': [{'when': when, 'model': model(m)} for when, m in cases],
        'fallback': model(fallback),
    }


def select_block_state(prop, cases, fallback):
    return {
        'type': 'minecraft:select',
        'property': 'minecraft:block_state',
        'block_state_property': prop,
        'cases': [{'when': when, 'model': model(m)} for when, m in cases],
        'fallback': model(fallback),
    }


# Client item definitions: mod -> {item registry path: item model}
ITEMS = {
    'buildcraftlib': {
        'guide': model('buildcraftlib:item/guide'),
        'guide_note': model('buildcraftlib:item/guide_note'),
        'debugger': model('buildcraftlib:item/debugger'),
    },
    'buildcraftcore': {
        'wrench': model('buildcraftcore:item/wrench'),
        'diamond_shard': model('buildcraftcore:item/diamond_shard'),
        'gear_wood': model('buildcraftcore:item/gears/wood'),
        'gear_stone': model('buildcraftcore:item/gears/stone'),
        'gear_iron': model('buildcraftcore:item/gears/iron'),
        'gear_gold': model('buildcraftcore:item/gears/gold'),
        'gear_diamond': model('buildcraftcore:item/gears/diamond'),
        # TODO(26.3): "used" lists (list_used) once the list's contents are a component
        'list': model('buildcraftcore:item/list_clean'),
        'map_location': select_component('buildcraftcore:map_location_type',
                                         [(t, 'buildcraftcore:item/map_location/' + t)
                                          for t in ['spot', 'area', 'path', 'path_repeating', 'zone']],
                                         'buildcraftcore:item/map_location/clean'),
        'paintbrush': select_component('buildcraftcore:paint_colour',
                                       [(d, 'buildcraftcore:item/paintbrush/' + DYE_MODEL[d]) for d in DYES],
                                       'buildcraftcore:item/paintbrush/clean'),
        'marker_connector': model('buildcraftcore:item/marker_connector'),
        'volume_box': model('buildcraftcore:item/volume_box'),
        'goggles': model('buildcraftcore:item/goggles'),
        'fragile_fluid_shard': model('buildcraftcore:item/fragile_fluid_shard'),
        'marker_volume': model('buildcraftcore:item/marker_volume'),
        'marker_path': model('buildcraftcore:item/marker_path'),
        'power_tester': model('buildcraftcore:item/power_tester'),
        'spring': model('buildcraftcore:item/spring'),
        'decorated': select_block_state('decoration_type',
                                        [(t, 'buildcraftcore:item/decorated/' + t)
                                         for t in ['destroy', 'blueprint', 'template', 'paper', 'leather', 'laser_back']],
                                        'buildcraftcore:item/decorated/destroy'),
        # TODO(26.3): the engine item models are BuildCraft variable models (a special item renderer is needed); until
        # then the items show the plain block models.
        'engine': select_block_state('type', [('wood', 'buildcraftcore:block/engine/wood'),
                                              ('creative', 'buildcraftcore:block/engine/creative')],
                                     'buildcraftcore:block/engine/wood'),
    },
}



# ---- Transport ----
# Pipe items: 1.12.2 built their models from the pipe's sprites in code. Until pipe items get a special model renderer,
# each one is a flat item of its pipe texture (pipe registry name -> texture under textures/pipes/).
PIPE_ITEM_TEXTURES = {'pipe_structure': 'structure'}
for _m, _tex in [('wood', 'wood'), ('cobble', 'cobblestone'), ('stone', 'stone'), ('quartz', 'quartz'), ('iron', 'iron'),
                 ('gold', 'gold'), ('clay', 'clay'), ('sandstone', 'sandstone'), ('void', 'void'), ('obsidian', 'obsidian'),
                 ('diamond', 'diamond'), ('diamond_wood', 'diamond_wood'), ('lapis', 'lapis'), ('daizuli', 'daizuli'),
                 ('emzuli', 'emzuli'), ('stripes', 'stripes')]:
    for _t in ['item', 'fluid', 'power', 'rf']:
        _name = '%s_%s' % (_tex, _t)
        if _m in ('wood', 'iron', 'diamond_wood', 'emzuli'):
            _name += '_clear'
        elif _m == 'lapis':
            _name += '_base'
        elif _m == 'daizuli':
            _name += '_white'
        PIPE_ITEM_TEXTURES['pipe_%s_%s' % (_m, _t)] = _name

ITEMS['buildcrafttransport'] = {
    'waterproof': model('buildcrafttransport:item/waterproof'),
    'filtered_buffer': model('buildcrafttransport:item/filtered_buffer'),
    'plug_blocker': model('buildcrafttransport:item/plug_blocker'),
    'plug_power_adaptor': model('buildcrafttransport:item/plug_power_adaptor'),
    'wire': select_component('buildcrafttransport:colour',
                             [(d, 'buildcrafttransport:item/wire/' + DYE_MODEL[d]) for d in DYES],
                             'buildcrafttransport:item/wire/white'),
}

# Item models written by this tool: mod -> {model path: model json}
MODELS = {'buildcrafttransport': {
    'item/plug_blocker': {'parent': 'item/generated', 'textures': {'layer0': 'buildcrafttransport:pipes/plug'}},
    'item/plug_power_adaptor': {'parent': 'item/generated',
                                'textures': {'layer0': 'buildcrafttransport:items/pipePowerAdapter'}},
    # Unused by the blockstate (ModelPipe draws the pipe holder); kept for anything that names the model
    'block/pipe_holder': {'textures': {'particle': 'buildcrafttransport:pipes/structure'}},
}}
BLOCKSTATES = {'buildcrafttransport': {
    # ModelPipe, registered in BCTransportModels (a NeoForge custom block state model)
    'pipe_holder': {'variants': {'': {'type': 'buildcrafttransport:pipe_holder'}}, '__converted_26_3': True},
}}


def _add_pipe_items():
    import os
    tex_dir = os.path.join(ASSETS, 'buildcrafttransport', 'textures', 'pipes')
    for pipe, tex in PIPE_ITEM_TEXTURES.items():
        if not os.path.isfile(os.path.join(tex_dir, tex + '.png')):
            continue
        MODELS['buildcrafttransport']['item/' + pipe] = {
            'parent': 'item/generated', 'textures': {'layer0': 'buildcrafttransport:pipes/' + tex}}
        ITEMS['buildcrafttransport'][pipe] = model('buildcrafttransport:item/' + pipe)


_add_pipe_items()


def write_models_and_states():
    for ns, models in MODELS.items():
        for path, data in models.items():
            write_json(os.path.join(ASSETS, ns, 'models', path + '.json'), data)
    for ns, states in BLOCKSTATES.items():
        for name, data in states.items():
            write_json(os.path.join(ASSETS, ns, 'blockstates', name + '.json'), data)


def block_model_id(ns, name):
    """1.12.2 blockstate model names were relative to models/block/."""
    if ':' in name:
        ns, name = name.split(':', 1)
    if not name.startswith('block/'):
        name = 'block/' + name
    return ns + ':' + name


def convert_variant(ns, v, warnings, path):
    out = {}
    for key, value in v.items():
        if key == 'model':
            out['model'] = block_model_id(ns, value)
        elif key in ('x', 'y', 'uvlock', 'weight'):
            out[key] = value
        elif key in ('textures', 'submodel', 'custom', 'transform'):
            warnings.append('%s: dropped Forge-only "%s"' % (path, key))
        else:
            warnings.append('%s: unknown variant key "%s"' % (path, key))
    return out


def convert_forge(ns, data, warnings, path):
    defaults = data.get('defaults', {})
    variants = data.get('variants', {})
    props = []      # (name, {value: variant}) for per-property blocks
    full = {}       # "a=b,c=d" style keys given as is
    for key, value in variants.items():
        if isinstance(value, dict) and value and all(isinstance(v, dict) for v in value.values()) \
                and '=' not in key and key not in ('normal', 'inventory'):
            props.append((key, value))
        elif key == 'inventory':
            continue  # item model hint, items are defined separately
        else:
            entry = value[0] if isinstance(value, list) else value
            full['' if key == 'normal' else key] = entry
    result = {}
    if props:
        names = [p[0] for p in props]
        for combo in itertools.product(*[list(p[1].items()) for p in props]):
            merged = dict(defaults)
            for _, variant in combo:
                merged.update(variant)
            key = ','.join('%s=%s' % (names[i], combo[i][0]) for i in range(len(names)))
            if 'model' not in merged:
                warnings.append('%s: variant %s has no model' % (path, key))
                continue
            result[key] = convert_variant(ns, merged, warnings, path)
    for key, value in full.items():
        merged = dict(defaults)
        merged.update(value)
        result[key] = convert_variant(ns, merged, warnings, path)
    if not result and 'model' in defaults:
        result[''] = convert_variant(ns, defaults, warnings, path)
    return {'variants': result}


def convert_vanilla(ns, data, warnings, path):
    if 'multipart' in data:
        for part in data['multipart']:
            apply = part.get('apply')
            if isinstance(apply, list):
                part['apply'] = [convert_variant(ns, a, warnings, path) for a in apply]
            elif apply:
                part['apply'] = convert_variant(ns, apply, warnings, path)
        return data
    result = {}
    for key, value in data.get('variants', {}).items():
        if key == 'inventory':
            continue
        if isinstance(value, list):
            result['' if key == 'normal' else key] = [convert_variant(ns, v, warnings, path) for v in value]
        else:
            result['' if key == 'normal' else key] = convert_variant(ns, value, warnings, path)
    return {'variants': result}


def is_converted(data):
    """Converted files carry this marker so the converter leaves them alone."""
    return data.get('__converted_26_3') is True


def convert_blockstates(warnings, mods):
    count = 0
    for ns in mods:
        bs_dir = os.path.join(ASSETS, ns, 'blockstates')
        if not os.path.isdir(bs_dir):
            continue
        for fn in sorted(os.listdir(bs_dir)):
            if not fn.endswith('.json'):
                continue
            path = os.path.join(bs_dir, fn)
            with open(path, encoding='utf-8') as f:
                data = json.load(f)
            if is_converted(data):
                continue
            if data.get('forge_marker') == 1:
                out = convert_forge(ns, data, warnings, ns + '/' + fn)
            else:
                out = convert_vanilla(ns, data, warnings, ns + '/' + fn)
            out = dict(out)
            out['__converted_26_3'] = True
            with open(path, 'w', encoding='utf-8', newline='\n') as f:
                json.dump(out, f, indent=2)
                f.write('\n')
            count += 1
    return count


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


def write_atlases():
    atlas_dir = os.path.join(ASSETS, 'minecraft', 'atlases')
    write_json(os.path.join(atlas_dir, 'blocks.json'), {'sources': [
        {'type': 'minecraft:directory', 'source': d, 'prefix': d + '/'} for d in BLOCK_ATLAS_DIRS]})
    write_json(os.path.join(atlas_dir, 'items.json'), {'sources': [
        {'type': 'minecraft:directory', 'source': d, 'prefix': d + '/'} for d in ITEM_ATLAS_DIRS]})


def write_items():
    count = 0
    for ns, items in ITEMS.items():
        for name, definition in items.items():
            write_json(os.path.join(ASSETS, ns, 'items', name + '.json'), {'model': definition})
            count += 1
    return count


def main():
    warnings = []
    n = convert_blockstates(warnings, sys.argv[1:])
    write_atlases()
    i = write_items()
    write_models_and_states()
    print('%d blockstates converted, %d item definitions written' % (n, i))
    for w in warnings:
        print('  warning: ' + w)


if __name__ == '__main__':
    sys.exit(main())
