"""
Error-driven pass of the BuildCraft 26.3 port: reads a javac log (from ./gradlew compileJava) and rewrites the exact spots
javac points at, for errors whose 26.3 fix is always the same (a renamed method or field, FluidStack's private amount,
Direction offsets...). Everything else is left for hand porting. Run it, recompile, and repeat while it still changes
things:

    ./gradlew compileJava -q > build.log 2>&1 ; python tools/port-26.3/fixerrs.py build.log [path-filter]
"""
import collections
import re
import sys

# (symbol name, kind, substring of the location's type) -> replacement name. kind is "method" or "variable".
RENAMES = {
    ('add', 'method', 'BlockPos'): 'offset',
    ('add', 'method', 'MutableBlockPos'): 'offset',
    ('getWorld', 'method', 'BlockEntity'): 'getLevel',
    ('getPos', 'method', 'BlockEntity'): 'getBlockPos',
    ('hasWorld', 'method', 'BlockEntity'): 'hasLevel',
    ('getMinecraft', 'method', 'Minecraft'): 'getInstance',
    ('grow', 'method', 'AABB'): 'inflate',
    ('offset', 'method', 'AABB'): 'move',
    ('getPropertyKeys', 'method', 'BlockState'): 'getProperties',
    ('getItemDamage', 'method', 'ItemStack'): 'getDamageValue',
    ('setItemDamage', 'method', 'ItemStack'): 'setDamageValue',
    ('getEntityWorld', 'method', ''): 'level',
    ('getHorizontalFacing', 'method', ''): 'getDirection',
    ('getIndex', 'method', 'Direction'): 'get3DDataValue',
    ('getHorizontal', 'method', 'Direction'): 'from2DDataValue',
    ('getBlockHardness', 'method', 'BlockState'): 'getDestroySpeed',
    ('setHeldItem', 'method', 'Player'): 'setItemInHand',
    ('detectAndSendChanges', 'method', ''): 'broadcastChanges',
    ('getUnlocalizedName', 'method', ''): 'getDescriptionId',
    ('getStackInSlot', 'method', 'Container'): 'getItem',
    ('getRenderViewEntity', 'method', 'Minecraft'): 'getCameraEntity',
    ('getResourcePath', 'method', 'Identifier'): 'getPath',
    ('getResourceDomain', 'method', 'Identifier'): 'getNamespace',
    ('getNamespace', 'method', 'Identifier'): 'getNamespace',
    ('getMetadata', 'method', 'ItemStack'): 'getDamageValue',
    ('isEmpty', 'method', 'FluidStack'): 'isEmpty',
    ('getLocalizedName', 'method', 'FluidStack'): 'getHoverName',
    ('getUnlocalizedName', 'method', 'FluidStack'): 'getDescriptionId',
    ('copy', 'method', 'FluidStack'): 'copy',
    ('getSize', 'method', 'CompoundTag'): 'size',
    ('getDoubleAt', 'method', 'ListTag'): 'getDoubleOr',
    ('isClientSide', 'method', 'Level'): 'isClientSide',
    ('startSection', 'method', 'ProfilerFiller'): 'push',
    ('endSection', 'method', 'ProfilerFiller'): 'pop',
    ('endStartSection', 'method', 'ProfilerFiller'): 'popPush',
    ('getDistanceSq', 'method', 'BlockPos'): 'distSqr',
    ('distanceSq', 'method', 'BlockPos'): 'distSqr',
    ('getBoundingBox', 'method', 'BlockState'): 'getShape',
    ('getRenderPartialTicks', 'method', 'Minecraft'): 'getDeltaTracker',
    ('getActiveRenderInfo', 'method', ''): 'getMainCamera',
}

# Field accesses that became methods: (field, location-type substring) -> method call (with parentheses).
FIELD_TO_METHOD = {
    ('rand', 'Level'): 'getRandom()',
    ('rand', 'ServerLevel'): 'getRandom()',
    ('playerEntities', 'Level'): 'players()',
    ('world', 'BlockEntity'): 'getLevel()',
    ('pos', 'BlockEntity'): 'getBlockPos()',
    ('inventoryContainer', 'Player'): 'inventoryMenu',
    ('fontRenderer', 'Minecraft'): 'font',
    ('player', 'Minecraft'): 'player',
    ('world', 'Minecraft'): 'level',
    ('objectMouseOver', 'Minecraft'): 'hitResult',
    ('gameSettings', 'Minecraft'): 'options',
    ('currentScreen', 'Minecraft'): 'screen',
    ('isDead', 'Entity'): 'isRemoved()',
}

ERROR_RE = re.compile(r'^(?P<file>[A-Za-z]:\\.*?\.java):(?P<line>\d+): error: (?P<msg>.*)$')


def parse(log):
    """-> list of dicts: file, line (1-based), col, msg, symbol, location"""
    lines = open(log, encoding='utf-8', errors='replace').read().splitlines()
    errors = []
    seen = set()
    i = 0
    while i < len(lines):
        m = ERROR_RE.match(lines[i])
        if not m:
            i += 1
            continue
        err = {'file': m.group('file'), 'line': int(m.group('line')), 'msg': m.group('msg'), 'col': -1,
               'symbol': '', 'location': ''}
        j = i + 1
        if j + 1 < len(lines) and '^' in lines[j + 1]:
            err['col'] = lines[j + 1].index('^')
            j += 2
        while j < len(lines) and not ERROR_RE.match(lines[j]) and j < i + 8:
            s = lines[j].strip()
            if s.startswith('symbol:'):
                err['symbol'] = s[len('symbol:'):].strip()
            elif s.startswith('location:'):
                err['location'] = s[len('location:'):].strip()
            j += 1
        key = (err['file'], err['line'], err['col'], err['msg'], err['symbol'])
        if key not in seen:
            seen.add(key)
            errors.append(err)
        i = j
    return errors


def location_type(location):
    # "variable pos of type BlockPos", "class BlockPos", "interface Foo"
    m = re.search(r'of type (\S+)', location)
    if m:
        return m.group(1)
    parts = location.split()
    return parts[-1] if parts else ''


def receiver_start(line, dot):
    """Index where the expression ending just before line[dot] ('.') starts: identifiers, dots and balanced brackets."""
    i = dot - 1
    depth = 0
    while i >= 0:
        c = line[i]
        if c in ')]':
            depth += 1
        elif c in '([':
            if depth == 0:
                break
            depth -= 1
        elif depth == 0 and not (c.isalnum() or c in '_.'):
            break
        i -= 1
    return i + 1


def call_end(line, open_paren):
    """Index just after the ')' closing the '(' at open_paren, or -1."""
    depth = 0
    for k in range(open_paren, len(line)):
        if line[k] == '(':
            depth += 1
        elif line[k] == ')':
            depth -= 1
            if depth == 0:
                return k + 1
    return -1


# receiver.method(arg) -> Static.call(receiver, arg), for single-argument methods that became static helpers
STATIC_CALLS = {
    ('isFluidEqual', 'FluidStack'): 'FluidUtilBC.isFluidEqual',
    ('isItemEqual', 'ItemStack'): 'ItemStack.isSameItem',
}


def fix_line(line, col, err):
    """-> new line, or None if this error isn't one we fix."""
    msg = err['msg']
    sym = err['symbol']
    loc_type = location_type(err['location'])

    if msg == 'cannot find symbol' and sym:
        m = re.match(r'method\s+(\w+)', sym)
        if m:
            for (name, t), static in STATIC_CALLS.items():
                if m.group(1) == name and t in loc_type:
                    dot = line.find('.' + name + '(', max(0, col - 1))
                    if dot < 0:
                        return None
                    start = receiver_start(line, dot)
                    paren = dot + len(name) + 1
                    end = call_end(line, paren)
                    if end < 0 or start >= dot:
                        return None
                    receiver = line[start:dot]
                    arg = line[paren + 1:end - 1]
                    return line[:start] + static + '(' + receiver + ', ' + arg + ')' + line[end:]
        m = re.match(r'(method|variable)\s+(\w+)', sym)
        if m:
            kind, name = m.group(1), m.group(2)
            if kind == 'method':
                for (n, k, t), new in RENAMES.items():
                    if n == name and k == kind and t in loc_type:
                        idx = line.find('.' + name + '(', max(0, col - 1))
                        if idx < 0:
                            idx = line.find(name + '(', max(0, col - 1))
                            if idx < 0:
                                return None
                            return line[:idx] + new + line[idx + len(name):]
                        return line[:idx + 1] + new + line[idx + 1 + len(name):]
            else:
                for (n, t), new in FIELD_TO_METHOD.items():
                    if n == name and t in loc_type:
                        idx = line.find('.' + name, max(0, col - 1))
                        if idx < 0:
                            return None
                        end = idx + 1 + len(name)
                        if end < len(line) and (line[end].isalnum() or line[end] == '_'):
                            return None
                        return line[:idx + 1] + new + line[end:]
        return None

    if msg == 'amount has private access in FluidStack':
        idx = line.find('.amount', max(0, col - 1))
        if idx < 0:
            return None
        end = idx + len('.amount')
        rest = line[end:]
        m = re.match(r'\s*(\+=|-=|=(?!=))\s*', rest)
        if m:
            # assignment: take the rest of the statement up to ';'
            semi = rest.find(';', m.end())
            if semi < 0:
                return None
            value = rest[m.end():semi]
            op = m.group(1)
            call = {'+=': 'grow', '-=': 'shrink', '=': 'setAmount'}[op]
            return line[:idx] + '.' + call + '(' + value.strip() + ')' + rest[semi:]
        m = re.match(r'(\+\+|--)', rest)
        if m:
            call = 'grow(1)' if m.group(1) == '++' else 'shrink(1)'
            return line[:idx] + '.' + call + rest[2:]
        return line[:idx] + '.getAmount()' + rest

    if msg.startswith('no suitable constructor found for FluidStack(FluidStack,int)'):
        idx = line.find('new FluidStack(', max(0, col - 4))
        if idx < 0:
            idx = line.find('new FluidStack(')
        if idx < 0:
            return None
        start = idx + len('new FluidStack(')
        depth = 0
        for k in range(start, len(line)):
            c = line[k]
            if c in '([':
                depth += 1
            elif c in ')]':
                depth -= 1
            elif c == ',' and depth == 0:
                first = line[start:k].strip()
                return line[:idx] + '(' + first + ').copyWithAmount(' + line[k + 1:].lstrip()
        return None

    if msg == 'incompatible types: Direction cannot be converted to Vec3i':
        idx = line.rfind('.offset(', 0, col + 1)
        if idx < 0:
            return None
        return line[:idx] + '.relative(' + line[idx + len('.offset('):]

    m = re.match(r'(x|z) has private access in ChunkPos', msg)
    if m:
        idx = line.find('.' + m.group(1), max(0, col - 1))
        if idx < 0:
            return None
        return line[:idx + 2] + '()' + line[idx + 2:]

    if msg == 'inventory has private access in Player':
        idx = line.find('.inventory', max(0, col - 1))
        if idx < 0:
            return None
        return line[:idx] + '.getInventory()' + line[idx + len('.inventory'):]

    return None


def main(log, flt=None):
    errors = parse(log)
    by_file = collections.defaultdict(list)
    for err in errors:
        if flt and flt not in err['file'].replace('\\', '/'):
            continue
        by_file[err['file']].append(err)
    fixed = 0
    for path, errs in by_file.items():
        try:
            text = open(path, encoding='utf-8').read()
        except OSError:
            continue
        newline = '\r\n' if '\r\n' in text else '\n'
        lines = text.split(newline)
        # Fix from the right so earlier columns stay valid.
        for err in sorted(errs, key=lambda e: (e['line'], e['col']), reverse=True):
            n = err['line'] - 1
            if n >= len(lines) or err['col'] < 0:
                continue
            new = fix_line(lines[n], err['col'], err)
            if new is not None and new != lines[n]:
                lines[n] = new
                fixed += 1
        new_text = newline.join(lines)
        if new_text != text:
            open(path, 'w', encoding='utf-8', newline='').write(new_text)
    print('%d fixes in %d files (%d errors read)' % (fixed, len(by_file), len(errors)))


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else None)
