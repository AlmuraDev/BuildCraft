"""Restores BuildCraft facades that a 26.3 world lost (saved as air) from the 1.12.2 (3.5) world they were migrated from.

Before the fix in LegacyBlockStates, BuildCraft read a 1.12.2 facade state ({Name, Properties}) as air and saved it
back as {id: "minecraft:air"}. This tool finds every facade plug in the 26.3 world whose states are all air, takes the
facade from the same pipe and side in the 1.12.2 world, and writes it back (in its 1.12.2 form, which BuildCraft now
upgrades when it loads). Nothing else in either world is changed; the 1.12.2 world is only read.

    python tools/port-26.3/repair_facades.py <1.12 region dir> <26.3 region dir>            # dry run: report only
    python tools/port-26.3/repair_facades.py <1.12 region dir> <26.3 region dir> --apply \\
        --backup <backup dir>                                                          # needs the server stopped

With --apply, each region file that changes is first copied to the backup dir, then rewritten whole (chunks keep their
timestamps and compression). Chunks stored outside the region file (.mcc) are reported and left alone.
"""
import gzip
import io
import os
import shutil
import struct
import sys
import zlib

# ---- Lossless NBT ----
# Values are (type, payload). Compounds: dict name -> (type, payload), in order. Lists: (element type, [payloads]).

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTES, STRING, LIST, COMPOUND, INTS, LONGS = range(13)


class Reader:
    def __init__(self, data):
        self.d = data
        self.i = 0

    def take(self, n):
        b = self.d[self.i:self.i + n]
        self.i += n
        return b

    def u8(self):
        return self.take(1)[0]

    def unpack(self, fmt):
        size = struct.calcsize(fmt)
        return struct.unpack(fmt, self.take(size))[0]

    def string(self):
        n = self.unpack('>H')
        return self.take(n).decode('utf-8', 'surrogateescape')

    def payload(self, t):
        if t == BYTE: return self.unpack('>b')
        if t == SHORT: return self.unpack('>h')
        if t == INT: return self.unpack('>i')
        if t == LONG: return self.unpack('>q')
        if t == FLOAT: return self.take(4)  # raw, so it round-trips bit for bit
        if t == DOUBLE: return self.take(8)
        if t == BYTES: return self.take(self.unpack('>i'))
        if t == STRING: return self.string()
        if t == LIST:
            et = self.u8()
            n = self.unpack('>i')
            return (et, [self.payload(et) for _ in range(n)])
        if t == COMPOUND:
            out = {}
            while True:
                et = self.u8()
                if et == END:
                    return out
                name = self.string()
                out[name] = (et, self.payload(et))
        if t == INTS: return self.take(4 * self.unpack('>i'))
        if t == LONGS: return self.take(8 * self.unpack('>i'))
        raise ValueError('bad tag type %d at %d' % (t, self.i))


def read_root(data):
    r = Reader(data)
    t = r.u8()
    name = r.string()
    return name, (t, r.payload(t))


def write_string(out, s):
    b = s.encode('utf-8', 'surrogateescape')
    out.write(struct.pack('>H', len(b)))
    out.write(b)


def write_payload(out, t, v):
    if t == BYTE: out.write(struct.pack('>b', v))
    elif t == SHORT: out.write(struct.pack('>h', v))
    elif t == INT: out.write(struct.pack('>i', v))
    elif t == LONG: out.write(struct.pack('>q', v))
    elif t in (FLOAT, DOUBLE): out.write(v)
    elif t == BYTES:
        out.write(struct.pack('>i', len(v)))
        out.write(v)
    elif t == STRING: write_string(out, v)
    elif t == LIST:
        et, items = v
        out.write(bytes([et]))
        out.write(struct.pack('>i', len(items)))
        for item in items:
            write_payload(out, et, item)
    elif t == COMPOUND:
        for name, (et, ev) in v.items():
            out.write(bytes([et]))
            write_string(out, name)
            write_payload(out, et, ev)
        out.write(bytes([END]))
    elif t == INTS:
        out.write(struct.pack('>i', len(v) // 4))
        out.write(v)
    elif t == LONGS:
        out.write(struct.pack('>i', len(v) // 8))
        out.write(v)
    else:
        raise ValueError(t)


def write_root(name, tag):
    out = io.BytesIO()
    t, v = tag
    out.write(bytes([t]))
    write_string(out, name)
    write_payload(out, t, v)
    return out.getvalue()


def get(compound, key, t=None):
    entry = compound.get(key)
    if entry is None or (t is not None and entry[0] != t):
        return None
    return entry[1]


# ---- Region files ----

def read_region(path):
    """@return {index: (timestamp, compression, nbt bytes or None if external)}"""
    data = open(path, 'rb').read()
    chunks = {}
    if len(data) < 8192:
        return chunks
    for i in range(1024):
        loc = int.from_bytes(data[i * 4:i * 4 + 3], 'big')
        if loc == 0:
            continue
        ts = struct.unpack('>i', data[4096 + i * 4:4096 + i * 4 + 4])[0]
        p = loc * 4096
        length = struct.unpack('>i', data[p:p + 4])[0]
        comp = data[p + 4]
        raw = data[p + 5:p + 4 + length]
        chunks[i] = (ts, comp, raw)
    return chunks


def decompress(comp, raw):
    c = comp & 0x7F
    if comp & 0x80:
        return None  # external .mcc file
    if c == 1: return gzip.decompress(raw)
    if c == 2: return zlib.decompress(raw)
    if c == 3: return raw
    raise ValueError('unsupported chunk compression %d' % c)


def compress(comp, data):
    c = comp & 0x7F
    if c == 1: return gzip.compress(data)
    if c == 2: return zlib.compress(data)
    if c == 3: return data
    raise ValueError(c)


def write_region(path, chunks):
    """Writes a whole region file from {index: (timestamp, compression, compressed bytes)}."""
    locations = bytearray(4096)
    timestamps = bytearray(4096)
    body = io.BytesIO()
    sector = 2
    for i in sorted(chunks):
        ts, comp, raw = chunks[i]
        payload = struct.pack('>i', len(raw) + 1) + bytes([comp]) + raw
        pad = (-len(payload)) % 4096
        payload += bytes(pad)
        count = len(payload) // 4096
        if count > 255:
            raise ValueError('chunk %d too big for the region file' % i)
        locations[i * 4:i * 4 + 4] = sector.to_bytes(3, 'big') + bytes([count])
        timestamps[i * 4:i * 4 + 4] = struct.pack('>i', ts)
        body.write(payload)
        sector += count
    tmp = path + '.repairtmp'
    with open(tmp, 'wb') as f:
        f.write(locations)
        f.write(timestamps)
        f.write(body.getvalue())
    os.replace(tmp, path)


# ---- Facades ----

def facade_plugs_112(region_dir, rx, rz):
    """@return {(x, y, z, side): facade compound} from a 1.12 region file."""
    path = os.path.join(region_dir, 'r.%d.%d.mca' % (rx, rz))
    out = {}
    if not os.path.exists(path):
        return out
    for i, (ts, comp, raw) in read_region(path).items():
        data = decompress(comp, raw)
        if data is None:
            continue
        _, (t, root) = read_root(data)
        level = get(root, 'Level', COMPOUND) or {}
        tes = get(level, 'TileEntities', LIST)
        if not tes:
            continue
        for te in tes[1]:
            plugs = get(te, 'plugs', COMPOUND)
            if not plugs:
                continue
            pos = (get(te, 'x', INT), get(te, 'y', INT), get(te, 'z', INT))
            for side, (pt, plug) in plugs.items():
                if pt != COMPOUND or 'facade' not in (get(plug, 'id', STRING) or ''):
                    continue
                pdata = get(plug, 'data', COMPOUND) or {}
                facade = get(pdata, 'facade', COMPOUND)
                if facade is not None:
                    out[pos + (side,)] = facade
    return out


def is_air_facade(facade):
    states = get(facade, 'states', LIST)
    if not states or not states[1]:
        return True
    for s in states[1]:
        state = get(s, 'state', COMPOUND) or {}
        name = get(state, 'id', STRING) or get(state, 'Name', STRING) or 'minecraft:air'
        if name != 'minecraft:air':
            return False
    return True


def has_block(facade):
    return not is_air_facade(facade)


def main():
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    apply = '--apply' in sys.argv
    backup = None
    if '--backup' in sys.argv:
        backup = sys.argv[sys.argv.index('--backup') + 1]
        args = [a for a in args if a != backup]
    if len(args) != 2:
        print(__doc__)
        sys.exit(1)
    old_dir, new_dir = args
    if apply and not backup:
        sys.exit('--apply needs --backup <dir>')
    lock = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(new_dir)))), 'session.lock')

    totals = {'air facades': 0, 'restored': 0, 'no 1.12 facade': 0, '1.12 also air': 0, 'external chunks': 0}
    for name in sorted(os.listdir(new_dir)):
        if not (name.startswith('r.') and name.endswith('.mca')):
            continue
        _, rx, rz, _ = name.split('.')
        rx, rz = int(rx), int(rz)
        path = os.path.join(new_dir, name)
        chunks = read_region(path)
        old_facades = None
        changed = False
        new_chunks = {}
        restored_here = 0
        for i, (ts, comp, raw) in chunks.items():
            data = decompress(comp, raw)
            if data is None:
                totals['external chunks'] += 1
                new_chunks[i] = (ts, comp, raw)
                continue
            root_name, (rt, root) = read_root(data)
            bes = get(root, 'block_entities', LIST)
            chunk_changed = False
            if bes:
                for be in bes[1]:
                    plugs = get(be, 'plugs', COMPOUND)
                    if not plugs:
                        continue
                    pos = (get(be, 'x', INT), get(be, 'y', INT), get(be, 'z', INT))
                    for side, (pt, plug) in plugs.items():
                        if pt != COMPOUND or 'facade' not in (get(plug, 'id', STRING) or ''):
                            continue
                        pdata = get(plug, 'data', COMPOUND)
                        facade = get(pdata, 'facade', COMPOUND) if pdata else None
                        if facade is None or not is_air_facade(facade):
                            continue
                        totals['air facades'] += 1
                        if old_facades is None:
                            old_facades = facade_plugs_112(old_dir, rx, rz)
                        old = old_facades.get(pos + (side,))
                        if old is None:
                            totals['no 1.12 facade'] += 1
                            continue
                        if not has_block(old):
                            totals['1.12 also air'] += 1
                            continue
                        pdata['facade'] = (COMPOUND, old)
                        totals['restored'] += 1
                        restored_here += 1
                        chunk_changed = True
            if chunk_changed:
                changed = True
                new_chunks[i] = (ts, comp, compress(comp, write_root(root_name, (rt, root))))
            else:
                new_chunks[i] = (ts, comp, raw)
        if restored_here:
            print('%s: %d facade(s) %s' % (name, restored_here, 'restored' if apply else 'to restore'))
        if changed and apply:
            if os.path.exists(lock):
                try:
                    os.rename(lock, lock)  # a running server keeps the lock open on Windows
                except OSError:
                    sys.exit('The world is in use (session.lock is held): stop the server first')
            os.makedirs(backup, exist_ok=True)
            shutil.copy2(path, os.path.join(backup, name))
            write_region(path, new_chunks)
    print('Totals:', totals)
    if not apply:
        print('Dry run: nothing written. Add --apply --backup <dir> to write.')


if __name__ == '__main__':
    main()
