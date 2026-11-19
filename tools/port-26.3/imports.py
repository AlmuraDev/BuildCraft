"""
Adds imports that the port's rewrites rely on (helpers like StackUtil, FluidUtilBC, NeoTransfer, BuiltInRegistries)
to files that use them without importing them. Safe to run any time:

    python tools/port-26.3/imports.py <file-or-dir>...
"""
import os
import re
import sys

NEEDED = {
    'StackUtil.': 'buildcraft.lib.misc.StackUtil',
    'CapUtil.': 'buildcraft.lib.misc.CapUtil',
    'FluidUtilBC.': 'buildcraft.lib.misc.FluidUtilBC',
    'NBTUtilBC.': 'buildcraft.lib.misc.NBTUtilBC',
    'NeoTransfer.': 'buildcraft.api.transfer.NeoTransfer',
    'NbtStacks.': 'buildcraft.api.core.NbtStacks',
    'BuiltInRegistries.': 'net.minecraft.core.registries.BuiltInRegistries',
    'ModLoadingContext.': 'net.neoforged.fml.ModLoadingContext',
    'Capabilities.': 'net.neoforged.neoforge.capabilities.Capabilities',
}


def fix(path):
    text = open(path, encoding='utf-8').read()
    original = text
    own_class = os.path.splitext(os.path.basename(path))[0]
    package = re.search(r'^package ([\w.]+);', text, re.M)
    package = package.group(1) if package else ''
    for needle, fqn in NEEDED.items():
        simple = fqn.rsplit('.', 1)[1]
        if simple == own_class or fqn.rsplit('.', 1)[0] == package:
            continue
        if not re.search(r'(?<![\w.])' + re.escape(needle), text):
            continue
        if re.search(r'^import ' + re.escape(fqn) + ';', text, re.M):
            continue
        m = re.search(r'^import .*$', text, re.M)
        if m:
            text = text[:m.start()] + 'import ' + fqn + ';\n' + text[m.start():]
        else:
            m = re.search(r'^package .*$', text, re.M)
            text = text[:m.end()] + '\n\nimport ' + fqn + ';' + text[m.end():]
    if text != original:
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
    changed = sum(1 for f in files if fix(f))
    print('%d of %d files changed' % (changed, len(files)))


if __name__ == '__main__':
    main(sys.argv[1:])
