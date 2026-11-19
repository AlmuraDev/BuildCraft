"""Bakes BuildCraft Energy's fluid textures.

1.12.2 recoloured the grey heat_<h>_still/flow textures for every fluid at runtime (AtlasSpriteFluid, a swappable
atlas sprite). 26.3 has no hook for that, so this writes the recoloured textures as files instead:
textures/blocks/fluids/<fluid>_heat_<h>_still.png and _flow.png (with their .mcmeta), for every fluid and heat value
in FLUIDS (keep it in step with BCEnergyFluids.preInit).

Each colour channel is lerped from the fluid's dark colour to its light colour by the grey value, alpha is opaque,
exactly as AtlasSpriteFluid.recolourPixel did.

    python tools/port-26.3/fluid_textures.py
"""
import os
import shutil
import struct
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
FLUID_DIR = os.path.join(ROOT, 'buildcraft_resources', 'assets', 'buildcraftenergy', 'textures', 'blocks', 'fluids')

# name: (light colour, dark colour), from BCEnergyFluids.preInit's table
FLUIDS = {
    'oil': (0x50_50_50, 0x05_05_05),
    'oil_residue': (0x10_0F_10, 0x42_10_42),
    'oil_heavy': (0xA0_8F_1F, 0x42_35_20),
    'oil_dense': (0x87_6E_77, 0x42_24_24),
    'oil_distilled': (0xE4_AF_78, 0xB4_7F_00),
    'fuel_dense': (0xFF_AF_3F, 0xE0_7F_00),
    'fuel_mixed_heavy': (0xF2_A7_00, 0xC4_87_00),
    'fuel_light': (0xFF_FF_30, 0xE4_CF_00),
    'fuel_mixed_light': (0xF6_D7_00, 0xC4_B7_00),
    'fuel_gaseous': (0xFA_F6_30, 0xE0_D9_00),
}


def read_png(path):
    data = open(path, 'rb').read()
    assert data[:8] == b'\x89PNG\r\n\x1a\n', path
    pos = 8
    idat = b''
    width = height = None
    while pos < len(data):
        length, kind = struct.unpack('>I4s', data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if kind == b'IHDR':
            width, height, depth, colour, _, _, interlace = struct.unpack('>IIBBBBB', body)
            if depth != 8 or colour != 6 or interlace != 0:
                raise ValueError(path + ': only 8-bit non-interlaced RGBA is supported')
        elif kind == b'IDAT':
            idat += body
        elif kind == b'IEND':
            break
    raw = zlib.decompress(idat)
    stride = width * 4
    rows = []
    prev = bytearray(stride)
    i = 0
    for _ in range(height):
        filt = raw[i]
        line = bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        for x in range(stride):
            a = line[x - 4] if x >= 4 else 0
            b = prev[x]
            c = prev[x - 4] if x >= 4 else 0
            if filt == 1:
                line[x] = (line[x] + a) & 0xFF
            elif filt == 2:
                line[x] = (line[x] + b) & 0xFF
            elif filt == 3:
                line[x] = (line[x] + ((a + b) >> 1)) & 0xFF
            elif filt == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pred = a if pa <= pb and pa <= pc else (b if pb <= pc else c)
                line[x] = (line[x] + pred) & 0xFF
        rows.append(line)
        prev = line
    return width, height, rows


def write_png(path, width, height, rows):
    def chunk(kind, body):
        return struct.pack('>I', len(body)) + kind + body + struct.pack('>I', zlib.crc32(kind + body) & 0xFFFFFFFF)

    raw = b''.join(b'\x00' + bytes(r) for r in rows)
    png = b'\x89PNG\r\n\x1a\n'
    png += chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))
    png += chunk(b'IDAT', zlib.compress(raw, 9))
    png += chunk(b'IEND', b'')
    with open(path, 'wb') as f:
        f.write(png)


def recolour(rows, light, dark):
    out = []
    lc = ((light >> 16) & 0xFF, (light >> 8) & 0xFF, light & 0xFF)
    dc = ((dark >> 16) & 0xFF, (dark >> 8) & 0xFF, dark & 0xFF)
    for row in rows:
        line = bytearray(len(row))
        for x in range(0, len(row), 4):
            for ch in range(3):
                data = row[x + ch]
                line[x + ch] = (dc[ch] * (256 - data) + lc[ch] * data) // 256
            line[x + 3] = 0xFF
        out.append(line)
    return out


def main():
    count = 0
    for heat in range(3):
        for kind in ('still', 'flow'):
            src = os.path.join(FLUID_DIR, 'heat_%d_%s.png' % (heat, kind))
            width, height, rows = read_png(src)
            for name, (light, dark) in FLUIDS.items():
                dst = os.path.join(FLUID_DIR, '%s_heat_%d_%s.png' % (name, heat, kind))
                write_png(dst, width, height, recolour(rows, light, dark))
                if os.path.isfile(src + '.mcmeta'):
                    shutil.copyfile(src + '.mcmeta', dst + '.mcmeta')
                count += 1
    print('%d fluid textures written' % count)


if __name__ == '__main__':
    main()
