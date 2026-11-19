"""
Second mechanical pass for BuildCraft tile classes (anything extending TileBC_Neptune, directly or through another
BuildCraft tile): 26.3 tiles are constructed with their position and block state, so

  public TileFoo() { ... }          ->  public TileFoo(BlockPos pos, BlockState state) { super(pos, state); ... }
  (no constructor at all)           ->  one is added
  invalidate() / validate()         ->  setRemoved() / clearRemoved()
  onChunkUnload()                   ->  onChunkUnloaded()

Run after rename.py:  python tools/port-26.3/tiles.py <file-or-dir>...   (running it twice is harmless)
"""
import os
import re
import sys

TILE_BASES = {'TileBC_Neptune'}


def find_tile_classes(files):
    """Every class (by simple name) that extends a tile base, found by following 'extends' through the given files."""
    extends = {}
    for path in files:
        text = open(path, encoding='utf-8').read()
        for m in re.finditer(r'\bclass\s+(\w+)(?:<[^{]*?>)?\s+extends\s+(\w+)', text):
            extends[m.group(1)] = m.group(2)
    tiles = set(TILE_BASES)
    changed = True
    while changed:
        changed = False
        for cls, parent in extends.items():
            if parent in tiles and cls not in tiles:
                tiles.add(cls)
                changed = True
    return tiles - TILE_BASES


def add_import(text, imp):
    line = 'import %s;' % imp
    if line in text:
        return text
    m = re.search(r'^import net\.minecraft\.[^\n]*\n', text, re.M) or re.search(r'^import [^\n]*\n', text, re.M)
    if m:
        return text[:m.start()] + line + '\n' + text[m.start():]
    m = re.search(r'^package [^\n]*\n', text, re.M)
    return text[:m.end()] + '\n' + line + '\n' + text[m.end():]


def port_tile(path, tiles):
    text = open(path, encoding='utf-8').read()
    original = text
    for cls in tiles:
        m = re.search(r'\bclass\s+' + cls + r'\b(?:<[^{]*?>)?\s+extends\s+(\w+)[^{]*\{', text)
        if not m:
            continue
        body_start = m.end()
        # no-arg constructor
        ctor = re.compile(r'(\n[ \t]*)((?:public|protected|private)\s+)?' + cls + r'\s*\(\s*\)\s*\{')
        found = False
        for cm in list(ctor.finditer(text, body_start)):
            found = True
            indent = cm.group(1)
            mods = cm.group(2) or ''
            replacement = '%s%s%s(BlockPos pos, BlockState state) {' % (indent, mods, cls)
            after = text[cm.end():]
            sm = re.match(r'\s*super\(\s*\)\s*;', after)
            if sm:
                after = indent + '    super(pos, state);' + after[sm.end():]
            elif not re.match(r'\s*(super|this)\(', after):
                after = indent + '    super(pos, state);' + (indent if after.startswith('}') else '') + after
            text = text[:cm.start()] + replacement + after
            break
        has_ctor = re.search(r'\n[ \t]*(?:public|protected|private)?\s*' + cls + r'\s*\(', text[body_start:])
        if not found and not has_ctor:
            indent = '    '
            ctor_text = '\n%spublic %s(BlockPos pos, BlockState state) {\n%s    super(pos, state);\n%s}\n' % (
                indent, cls, indent, indent)
            text = text[:body_start] + ctor_text + text[body_start:]
        # removal / unload hooks
        text = re.sub(r'public void invalidate\(\)', 'public void setRemoved()', text)
        text = re.sub(r'super\.invalidate\(\)', 'super.setRemoved()', text)
        text = re.sub(r'public void validate\(\)', 'public void clearRemoved()', text)
        text = re.sub(r'super\.validate\(\)', 'super.clearRemoved()', text)
        text = re.sub(r'public void onChunkUnload\(\)', 'public void onChunkUnloaded()', text)
        text = re.sub(r'super\.onChunkUnload\(\)', 'super.onChunkUnloaded()', text)
    if text != original:
        text = add_import(text, 'net.minecraft.world.level.block.state.BlockState')
        text = add_import(text, 'net.minecraft.core.BlockPos')
        open(path, 'w', encoding='utf-8', newline='').write(text)
        return True
    return False


def main(paths):
    files = []
    for p in paths:
        if os.path.isdir(p):
            for root, _, names in os.walk(p):
                files += [os.path.join(root, n) for n in names if n.endswith('.java')]
        else:
            files.append(p)
    # Look for tile classes across all of common/, so subclasses in other modules are found too.
    here = os.path.dirname(os.path.abspath(__file__))
    common = os.path.join(here, '..', '..', 'common')
    all_files = []
    for root, _, names in os.walk(common):
        all_files += [os.path.join(root, n) for n in names if n.endswith('.java')]
    tiles = find_tile_classes(all_files)
    changed = sum(1 for f in files if port_tile(f, tiles))
    print('%d of %d files changed (%d tile classes known)' % (changed, len(files), len(tiles)))


if __name__ == '__main__':
    main(sys.argv[1:])
