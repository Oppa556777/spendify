#!/usr/bin/env python3
"""Manual zipalign: rebuild an APK so every entry's data starts on a 4-byte
boundary (padding goes into the local-header extra field, like the real
zipalign tool)."""
import struct
import sys
import zipfile
import zlib

SRC = sys.argv[1]
OUT = sys.argv[2]

entries = []
with zipfile.ZipFile(SRC) as z:
    for info in z.infolist():
        entries.append({
            'name': info.filename,
            'data': z.read(info.filename),
            'compress': info.compress_type,
        })

out = bytearray()
central = []
offset = 0
for e in entries:
    name = e['name'].encode('utf-8')
    data = e['data']
    if e['compress'] == 8:
        comp = zlib.compress(data)[2:-4]  # raw deflate (strip zlib header+adler)
    else:
        comp = data
    crc = zlib.crc32(data) & 0xFFFFFFFF

    base = offset + 30 + len(name)
    extra_len = (4 - (base % 4)) % 4
    extra = b'\0' * extra_len
    data_start = offset + 30 + len(name) + extra_len
    assert data_start % 4 == 0, (name, data_start)

    out += struct.pack('<IHHHHHIIIHH',
                       0x04034b50, 20, 0, e['compress'], 0, 0,
                       crc, len(comp), len(data), len(name), extra_len)
    out += name
    out += extra
    out += comp

    central.append((name, e['compress'], crc, len(comp), len(data), offset, extra_len, extra))
    offset = data_start + len(comp)

cd_start = len(out)
for (name, method, crc, comp_size, raw_size, lho, extra_len, extra) in central:
    out += struct.pack('<IHHHHHHIIIHHHHHII',
                       0x02014b50, 20, 20, 0, method, 0, 0,
                       crc, comp_size, raw_size, len(name), extra_len, 0, 0, 0,
                       0, lho)
    out += name
    out += extra

cd_size = len(out) - cd_start
out += struct.pack('<IHHHHIIH',
                   0x06054b50, 0, 0, len(central), len(central),
                   cd_size, cd_start, 0)

with open(OUT, 'wb') as f:
    f.write(out)

with zipfile.ZipFile(OUT) as z:
    bad = []
    for info in z.infolist():
        if info.compress_type == zipfile.ZIP_STORED:
            ds = info.header_offset + 30 + len(info.filename.encode()) + len(info.extra)
            if ds % 4 != 0:
                bad.append((info.filename, ds))
    print("entries:", len(z.infolist()), "| misaligned stored:", bad if bad else "NONE")
    t = z.testzip()
    print("testzip:", "OK" if t is None else f"BAD: {t}")
