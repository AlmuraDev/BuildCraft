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
# (Sprite names must be unique across all atlases on 26.3: a folder can only be in one of them. Item models find
# block-atlas sprites by name too.)
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
        # Engine items are their engine models, drawn in code (BCCoreModels/BCEnergyModels.ENGINE_ITEM_QUADS)
        'engine': {'type': 'buildcraftcore:engine'},
    },
}



# ---- Transport ----
# Pipe items: drawn in code by ModelPipeItem, like 1.12.2 (pipe registry name -> its texture under textures/pipes/,
# which only says whether the pipe exists).
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
    # Plug models drawn in code (BCTransportModels.registerPlugItemModels)
    'plug_blocker': {'type': 'buildcrafttransport:plug_blocker'},
    'plug_power_adaptor': {'type': 'buildcrafttransport:plug_power_adaptor'},
    'wire': select_component('buildcrafttransport:colour',
                             [(d, 'buildcrafttransport:item/wire/' + DYE_MODEL[d]) for d in DYES],
                             'buildcrafttransport:item/wire/white'),
}

# Item models written by this tool: mod -> {model path: model json}
MODELS = {'buildcrafttransport': {
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
        # ModelPipeItem (BCTransportModels) draws the pipe from the stack's pipe definition, as 1.12.2 did
        ITEMS['buildcrafttransport'][pipe] = {'type': 'buildcrafttransport:pipe'}


_add_pipe_items()


# ---- Energy ----
# Fluids: name -> heat values, as BCEnergyFluids registers them ("oil", "oil_heat_1", "oil_heat_2"...). The textures
# are baked by fluid_textures.py.
ENERGY_FLUIDS = ['oil', 'oil_residue', 'oil_heavy', 'oil_dense', 'oil_distilled', 'fuel_dense', 'fuel_mixed_heavy',
                 'fuel_light', 'fuel_mixed_light', 'fuel_gaseous']


def _add_energy():
    ITEMS['buildcraftenergy'] = {
        'glob_of_oil': model('buildcraftenergy:item/glob_oil'),
        # The dynamo item is its engine-style model, drawn in code (BCEnergyModels.registerItemModels)
        'mj_dynamo': {'type': 'buildcraftenergy:mj_dynamo'},
    }
    MODELS['buildcraftenergy'] = {
        'block/mj_dynamo_fallback': {'parent': 'block/cube_all',
                                     'textures': {'all': 'buildcraftenergy:blocks/mj_dynamo/front'}},
    }
    BLOCKSTATES['buildcraftenergy'] = {
        # Drawn by its tile renderer; the model gives the particle texture
        'mj_dynamo': {'variants': {'': {'model': 'buildcraftenergy:block/mj_dynamo_fallback'}},
                      '__converted_26_3': True},
    }
    for name in ENERGY_FLUIDS:
        for heat in range(3):
            full = name if heat == 0 else '%s_heat_%d' % (name, heat)
            tex = 'buildcraftenergy:blocks/fluids/%s_heat_%d_still' % (name, heat)
            # Fluid blocks are drawn by their fluid model; the block model only gives the particle texture
            MODELS['buildcraftenergy']['block/fluid/%s_heat_%d' % (name, heat)] = {'textures': {'particle': tex}}
            BLOCKSTATES['buildcraftenergy']['fluid_block_%s_heat_%d' % (name, heat)] = {
                'variants': {'': {'model': 'buildcraftenergy:block/fluid/%s_heat_%d' % (name, heat)}},
                '__converted_26_3': True}
            ITEMS['buildcraftenergy']['bucket_' + full] = {
                'type': 'neoforge:fluid_container',
                'fluid': 'buildcraftenergy:' + full,
                'flip_gas': True,
                'textures': {'base': 'minecraft:item/bucket', 'fluid': 'neoforge:item/mask/bucket_fluid_drip'},
            }


_add_energy()


# ---- Factory ----
# Blockstates 1.12.2 built from Forge submodels and per-variant texture overrides, which vanilla blockstates can't
# express: the chute (connections as submodels) and flood gate (open/closed side textures) become multipart, the
# water gel and distiller get one model per texture set, and the heat exchanger uses its custom model (ModelHeatExchange).
CHUTE_FACING = {'up': (0, 0), 'down': (180, 0), 'east': (90, 90), 'south': (90, 180), 'west': (90, 270),
                'north': (90, 0)}
CHUTE_CONNECTED = {'up': (180, 0), 'down': (0, 0), 'east': (90, 270), 'south': (90, 0), 'west': (90, 90),
                   'north': (90, 180)}
GEL_STAGES = ['spread_0', 'spread_1', 'spread_2', 'spread_3', 'gelling_0', 'gelling_1', 'gel']
FACE_FROM_TO = {
    'up': ([0, 16, 0], [16, 16, 16]), 'down': ([0, 0, 0], [16, 0, 16]),
    'north': ([0, 0, 0], [16, 16, 0]), 'south': ([0, 0, 16], [16, 16, 16]),
    'west': ([0, 0, 0], [0, 16, 16]), 'east': ([16, 0, 0], [16, 16, 16]),
}


def _face_model(face, texture):
    """A model of one full face of a cube."""
    frm, to = FACE_FROM_TO[face]
    return {'parent': 'block/block', 'textures': {'particle': texture, 'face': texture},
            'elements': [{'from': frm, 'to': to,
                          'faces': {face: {'uv': [0, 0, 16, 16], 'texture': '#face', 'cullface': face}}}]}


def _rot(model, x, y):
    out = {'model': model}
    if x:
        out['x'] = x
    if y:
        out['y'] = y
    return out


def _add_factory():
    ns = 'buildcraftfactory'
    models = MODELS.setdefault(ns, {})
    states = BLOCKSTATES.setdefault(ns, {})
    items = ITEMS.setdefault(ns, {})

    parts = [{'when': {'facing': f}, 'apply': _rot('buildcraftfactory:block/chute', x, y)}
             for f, (x, y) in CHUTE_FACING.items()]
    parts += [{'when': {'connected_' + f: 'true'}, 'apply': _rot('buildcraftfactory:block/chute_connected', x, y)}
              for f, (x, y) in CHUTE_CONNECTED.items()]
    states['chute'] = {'multipart': parts, '__converted_26_3': True}

    tex = 'buildcraftfactory:blocks/flood_gate/'
    models['block/flood_gate/up'] = _face_model('up', tex + 'top')
    parts = [{'apply': {'model': 'buildcraftfactory:block/flood_gate/up'}}]
    for face in ['down', 'north', 'south', 'east', 'west']:
        for open_ in (True, False):
            name = 'block/flood_gate/%s_%s' % (face, 'open' if open_ else 'closed')
            models[name] = _face_model(face, tex + ('open' if open_ else 'closed'))
            parts.append({'when': {'connected_' + face: 'true' if open_ else 'false'},
                          'apply': {'model': 'buildcraftfactory:' + name}})
    states['flood_gate'] = {'multipart': parts, '__converted_26_3': True}

    variants = {}
    for stage in GEL_STAGES:
        models['block/water_gel/' + stage] = {'parent': 'block/cube_all',
                                              'textures': {'all': 'buildcraftfactory:blocks/gel/' + stage}}
        variants['stage=' + stage] = {'model': 'buildcraftfactory:block/water_gel/' + stage}
    states['water_gel'] = {'variants': variants, '__converted_26_3': True}

    models['block/distiller_textured'] = {'parent': 'buildcraftfactory:block/distiller', 'textures': {
        'particle': 'buildcraftfactory:blocks/distiller/tank_sprite_a',
        'sprite_a': 'buildcraftfactory:blocks/distiller/tank_sprite_a',
        'sprite_b': 'buildcraftfactory:blocks/distiller/tank_sprite_b',
        'power': 'buildcraftfactory:blocks/distiller/power_sprite_a'}}
    states['distiller'] = {'variants': {
        'facing=' + f: _rot('buildcraftfactory:block/distiller_textured', 0, y)
        for f, y in [('south', 270), ('east', 180), ('north', 90), ('west', 0)]}, '__converted_26_3': True}

    states['heat_exchange'] = {'variants': {'': {'type': 'buildcraftfactory:heat_exchange'}},
                               '__converted_26_3': True}

    for name in ['autoworkbench_item', 'mining_well', 'pump', 'flood_gate', 'tank', 'chute', 'distiller']:
        items[name] = model('buildcraftfactory:item/' + name)
    # TODO(26.3): the heat exchanger item was its baked variable model; the middle section's item model for now
    items['heat_exchange'] = model('buildcraftfactory:item/heat_exchange_middle')
    items['water_gel_spawn'] = model('buildcraftfactory:item/water_gel')
    items['gel'] = model('buildcraftfactory:item/gel')
    items['plastic_sheet'] = model('buildcraftfactory:item/plastic_sheet')


_add_factory()


# ---- Silicon ----
# 1.12.2 baked the gate, lens, pulsar, light sensor, timer and facade items from their pluggable models in code
# (TODO(26.3): special item model renderers); until then they are flat items.
def _add_silicon():
    ns = 'buildcraftsilicon'
    models = MODELS.setdefault(ns, {})
    items = ITEMS.setdefault(ns, {})
    for chipset in ['red', 'iron', 'gold', 'quartz', 'diamond']:
        items['redstone_chipset_' + chipset] = model('buildcraftsilicon:item/redstone_chipset/' + chipset)
    # The copier shows "full" while it holds copied gate data (custom data)
    items['gate_copier'] = {'type': 'minecraft:condition', 'property': 'minecraft:has_component',
                            'component': 'minecraft:custom_data',
                            'on_true': {'type': 'minecraft:model', 'model': 'buildcraftsilicon:item/gatecopier_full'},
                            'on_false': {'type': 'minecraft:model', 'model': 'buildcraftsilicon:item/gatecopier_empty'}}
    # Pulsar, light sensor and timer items are their plug models, drawn in code (BCSiliconModels.registerItemModels)
    for plug in ['pulsar', 'light_sensor', 'timer']:
        items['plug_' + plug] = {'type': 'buildcraftsilicon:' + plug}
    # Gates and lenses are also drawn from their plug models in code (BCSiliconModels.registerItemModels)
    items['plug_gate'] = {'type': 'buildcraftsilicon:gate'}
    items['plug_lens'] = {'type': 'buildcraftsilicon:lens'}
    items['plug_facade'] = {'type': 'buildcraftsilicon:facade'}
    for block in ['laser', 'assembly_table', 'advanced_crafting_table', 'integration_table', 'charging_table',
                  'programming_table']:
        items[block] = model('buildcraftsilicon:item/' + block)


_add_silicon()


# ---- Builders ----
# 1.12.2's Forge blockstates added the frame's connections, the builder's snapshot slot and the quarry's connected side
# textures as submodels or texture overrides; they become multipart here (the quarry gets one model per face).
HORIZONTAL_Y = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
QUARRY_FACE_TEXTURE = {'up': 'top', 'down': 'bottom', 'north': 'front', 'south': 'back', 'east': 'side',
                       'west': 'side'}
FRAME_CONNECTION = {'up': (270, 0), 'down': (90, 0), 'east': (0, 90), 'west': (180, 90), 'north': (0, 0),
                    'south': (180, 0)}


def _add_builders():
    ns = 'buildcraftbuilders'
    models = MODELS.setdefault(ns, {})
    states = BLOCKSTATES.setdefault(ns, {})
    items = ITEMS.setdefault(ns, {})
    tex_root = os.path.join(ASSETS, ns, 'textures')

    def facing_variants(block_model):
        return {'variants': {'facing=' + f: _rot(block_model, 0, y) for f, y in HORIZONTAL_Y.items()}}

    states['filler'] = facing_variants('buildcraftbuilders:block/filler/main')
    states['library'] = facing_variants('buildcraftbuilders:block/library')
    states['replacer'] = facing_variants('buildcraftbuilders:block/replacer')
    states['architect'] = {'variants': {
        'facing=%s,valid=%s' % (f, v): _rot('buildcraftbuilders:block/architect_' + ('on' if v == 'true' else 'off'),
                                            0, y)
        for f, y in HORIZONTAL_Y.items() for v in ('true', 'false')}}

    parts = [{'when': {'facing': f}, 'apply': _rot('buildcraftbuilders:block/builder/main', 0, y)}
             for f, y in HORIZONTAL_Y.items()]
    for snapshot in ('none', 'blueprint', 'template'):
        slot = 'empty' if snapshot == 'none' else snapshot
        parts += [{'when': {'facing': f, 'snapshot_type': snapshot},
                   'apply': _rot('buildcraftbuilders:block/builder/slot_' + slot, 0, y)}
                  for f, y in HORIZONTAL_Y.items()]
    states['builder'] = {'multipart': parts}

    parts = [{'apply': {'model': 'buildcraftbuilders:block/frame/base'}}]
    parts += [{'when': {'connected_' + f: 'true'}, 'apply': _rot('buildcraftbuilders:block/frame/connection', x, y)}
              for f, (x, y) in FRAME_CONNECTION.items()]
    states['frame'] = {'multipart': parts}

    parts = []
    for face, name in QUARRY_FACE_TEXTURE.items():
        for conn in ('normal', 'connected'):
            texture = 'blocks/quarry/%s/%s' % (conn, name)
            if not os.path.exists(os.path.join(tex_root, texture + '.png')):
                # 1.12.2 had no connected top texture (it showed the missing texture)
                texture = 'blocks/quarry/normal/' + name
            models['block/quarry/%s_%s' % (conn, face)] = _face_model(face, 'buildcraftbuilders:' + texture)
        for f, y in HORIZONTAL_Y.items():
            for conn in ('normal', 'connected'):
                parts.append({'when': {'facing': f, 'connected_' + face: 'true' if conn == 'connected' else 'false'},
                              'apply': _rot('buildcraftbuilders:block/quarry/%s_%s' % (conn, face), 0, y)})
    states['quarry'] = {'multipart': parts}

    for name in ('builder', 'filler', 'frame', 'quarry', 'replacer', 'filler_planner'):
        items[name] = model('buildcraftbuilders:item/' + name)
    items['architect'] = model('buildcraftbuilders:block/architect_off')
    items['library'] = model('buildcraftbuilders:block/library')

    def used(clean, dirty):
        return {'type': 'minecraft:condition', 'property': 'minecraft:has_component',
                'component': 'minecraft:custom_data', 'on_true': model(dirty), 'on_false': model(clean)}

    items['schematic_single'] = used('buildcraftbuilders:item/schematic_single/clean',
                                     'buildcraftbuilders:item/schematic_single/used')
    # Which snapshot a stack is: the buildcraftbuilders:snapshot_type component (no component = blueprint)
    items['snapshot'] = {
        'type': 'minecraft:select', 'property': 'minecraft:component', 'component': 'buildcraftbuilders:snapshot_type',
        'cases': [{'when': 'template', 'model': used('buildcraftbuilders:item/snapshot/template_clean',
                                                     'buildcraftbuilders:item/snapshot/template_used')}],
        'fallback': used('buildcraftbuilders:item/snapshot/blueprint_clean',
                         'buildcraftbuilders:item/snapshot/blueprint_used'),
    }


_add_builders()


# ---- Robotics ----
# Only the zone planner exists (1.12.2's requester and zone plan blockstates have no block).
def _add_robotics():
    ns = 'buildcraftrobotics'
    BLOCKSTATES.setdefault(ns, {})['zone_planner'] = {'variants': {
        'facing=' + f: _rot('buildcraftrobotics:block/zone_planner', 0, y) for f, y in HORIZONTAL_Y.items()}}
    ITEMS.setdefault(ns, {})['zone_planner'] = model('buildcraftrobotics:item/zone_planner')


_add_robotics()


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
