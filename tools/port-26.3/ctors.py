"""
Third mechanical pass: BuildCraft block and item constructors. 26.3 blocks and items get their (id-carrying) properties in
their constructor, so:

  Block:  BlockFoo(Material material, String id) { super(material, id); setHardness(2); ... }
       -> BlockFoo(BlockBehaviour.Properties properties, String id) { super(properties.destroyTime(2), id); ... }
          (setHardness, setResistance, setSoundType, setLightLevel, setBlockUnbreakable and setTickRandomly calls in the
          constructor become properties; a fixed material, super(Material.X, id), is dropped)
  Item:   ItemFoo(String id) { super(id); }   ->  ItemFoo(Item.Properties properties, String id) { super(properties, id); }
  Item block: ItemFoo(BlockFoo block) { super(block); } -> ItemFoo(BlockFoo block, Item.Properties properties) {...}

Run after rename.py:  python tools/port-26.3/ctors.py <file-or-dir>...   (running it twice is harmless)
"""
import os
import re
import sys

BLOCK_BASES = {'BlockBCBase_Neptune'}
ITEM_BASES = {'ItemBC_Neptune'}
ITEM_BLOCK_BASES = {'ItemBlockBC_Neptune'}

PROPERTY_CALLS = [
    (r'setHardness\(([^;]*)\)', r'.destroyTime(\1)'),
    (r'setResistance\(([^;]*)\)', r'.explosionResistance(\1)'),
    (r'setSoundType\(([^;]*)\)', r'.sound(\1)'),
    (r'setLightLevel\(([^;]*)\)', r'.lightLevel(state -> (int) ((\1) * 15))'),
    (r'setBlockUnbreakable\(\)', r'.strength(-1.0F, 3600000.0F)'),
    (r'setTickRandomly\(true\)', r'.randomTicks()'),
    (r'setLightOpacity\(([^;]*)\)', None),
]


def subclasses(files, bases):
    extends = {}
    for path in files:
        text = open(path, encoding='utf-8').read()
        for m in re.finditer(r'\bclass\s+(\w+)(?:<[^{]*?>)?\s+extends\s+(\w+)', text):
            extends[m.group(1)] = m.group(2)
    found = set(bases)
    changed = True
    while changed:
        changed = False
        for cls, parent in extends.items():
            if parent in found and cls not in found:
                found.add(cls)
                changed = True
    return found


def add_import(text, imp):
    line = 'import %s;' % imp
    if line in text:
        return text
    m = re.search(r'^import net\.minecraft\.[^\n]*\n', text, re.M) or re.search(r'^import [^\n]*\n', text, re.M)
    if m:
        return text[:m.start()] + line + '\n' + text[m.start():]
    m = re.search(r'^package [^\n]*\n', text, re.M)
    return text[:m.end()] + '\n' + line + '\n' + text[m.end():]


def ctor_body(text, open_brace):
    depth = 0
    for i in range(open_brace, len(text)):
        if text[i] == '{':
            depth += 1
        elif text[i] == '}':
            depth -= 1
            if depth == 0:
                return i
    return -1


def port_block_ctor(text, cls):
    ctor = re.compile(r'((?:public|protected|private)\s+)' + cls + r'\s*\(([^)]*)\)\s*\{')
    out = text
    for m in list(ctor.finditer(text))[::-1]:
        params = m.group(2)
        if 'Properties' in params:
            continue
        new_params = None
        if re.search(r'\bMaterial\s+\w+\s*,\s*String\s+(\w+)', params):
            new_params = re.sub(r'\bMaterial\s+(\w+)\s*,', 'BlockBehaviour.Properties properties,', params)
        elif re.match(r'\s*String\s+\w+\s*$', params):
            new_params = 'BlockBehaviour.Properties properties, ' + params.strip()
        if new_params is None:
            continue
        brace = m.end() - 1
        end = ctor_body(out, brace)
        body = out[brace + 1:end]
        # super(material, id) / super(Material.X, id)
        sm = re.search(r'super\(\s*(?:\w+|Material\.\w+)\s*,', body)
        if not sm:
            continue
        found = []
        for regex, repl in PROPERTY_CALLS:
            for pm in re.finditer(r'\n?[ \t]*(?:this\.)?' + regex + r'\s*;', body):
                if repl is not None:
                    found.append((pm.start(), re.sub(regex, repl, pm.group(0).strip().rstrip(';').replace('this.', ''))))
        # Keep the source order: a later call (strength) overrides an earlier one (explosionResistance)
        extra = ''.join(call for _, call in sorted(found))
        for regex, repl in PROPERTY_CALLS:
            body = re.sub(r'\n?[ \t]*(?:this\.)?' + regex + r'\s*;', '', body)
        sm = re.search(r'super\(\s*(?:\w+|Material\.\w+)\s*,', body)
        body = body[:sm.start()] + 'super(properties' + extra + ',' + body[sm.end():]
        out = out[:m.start()] + m.group(1) + cls + '(' + new_params + ') {' + body + out[end:]
    return out


def port_item_ctor(text, cls):
    ctor = re.compile(r'((?:public|protected|private)\s+)' + cls + r'\s*\(\s*String\s+(\w+)([^)]*)\)\s*\{')
    out = text
    for m in list(ctor.finditer(text))[::-1]:
        brace = m.end() - 1
        end = ctor_body(out, brace)
        body = out[brace + 1:end]
        sm = re.search(r'super\(\s*' + m.group(2) + r'\b', body)
        if not sm:
            continue
        body = body[:sm.start()] + 'super(properties, ' + m.group(2) + body[sm.end():]
        out = (out[:m.start()] + m.group(1) + cls + '(Item.Properties properties, String ' + m.group(2) + m.group(3)
               + ') {' + body + out[end:])
    return out


def port_item_block_ctor(text, cls):
    ctor = re.compile(r'((?:public|protected|private)\s+)' + cls + r'\s*\(\s*(\w+(?:<[^>]*>)?)\s+(\w+)\s*\)\s*\{')
    out = text
    for m in list(ctor.finditer(text))[::-1]:
        brace = m.end() - 1
        end = ctor_body(out, brace)
        body = out[brace + 1:end]
        sm = re.search(r'super\(\s*' + m.group(3) + r'\s*\)', body)
        if not sm:
            continue
        body = body[:sm.start()] + 'super(' + m.group(3) + ', properties)' + body[sm.end():]
        out = (out[:m.start()] + m.group(1) + cls + '(' + m.group(2) + ' ' + m.group(3) + ', Item.Properties properties'
               + ') {' + body + out[end:])
    return out


def main(paths):
    files = []
    for p in paths:
        if os.path.isdir(p):
            for root, _, names in os.walk(p):
                files += [os.path.join(root, n) for n in names if n.endswith('.java')]
        else:
            files.append(p)
    here = os.path.dirname(os.path.abspath(__file__))
    common = os.path.join(here, '..', '..', 'common')
    all_files = []
    for root, _, names in os.walk(common):
        all_files += [os.path.join(root, n) for n in names if n.endswith('.java')]
    blocks = subclasses(all_files, BLOCK_BASES) - BLOCK_BASES
    item_blocks = subclasses(all_files, ITEM_BLOCK_BASES) - ITEM_BLOCK_BASES
    items = subclasses(all_files, ITEM_BASES) - ITEM_BASES
    changed = 0
    for f in files:
        text = open(f, encoding='utf-8').read()
        new = text
        for cls in blocks:
            if re.search(r'\bclass\s+' + cls + r'\b', new):
                new = port_block_ctor(new, cls)
        for cls in items:
            if re.search(r'\bclass\s+' + cls + r'\b', new):
                new = port_item_ctor(new, cls)
        for cls in item_blocks:
            if re.search(r'\bclass\s+' + cls + r'\b', new):
                new = port_item_block_ctor(new, cls)
        if new != text:
            if 'BlockBehaviour.Properties' in new:
                new = add_import(new, 'net.minecraft.world.level.block.state.BlockBehaviour')
            if 'Item.Properties' in new:
                new = add_import(new, 'net.minecraft.world.item.Item')
            new = new.replace('import net.minecraft.block.material.Material;\n', '')
            open(f, 'w', encoding='utf-8', newline='').write(new)
            changed += 1
    print('%d of %d files changed (%d block, %d item, %d item block classes known)' % (
        changed, len(files), len(blocks), len(items), len(item_blocks)))


if __name__ == '__main__':
    main(sys.argv[1:])
