"""Crea data/tecnocinetico/structures/empty.nbt: plantilla de 1x1x1 (aire) que usan los GameTests del mod.

Uso: python tools/empty_template_gen.py   (solo hace falta una vez; el archivo se sube a git)
"""
import gzip
import struct
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/data/tecnocinetico/structures/empty.nbt"


def name(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def tag_int(n, v):
    return b"\x03" + name(n) + struct.pack(">i", v)


def tag_string(n, v):
    return b"\x08" + name(n) + name(v)


def tag_list(n, elem_type, items):
    return b"\x09" + name(n) + bytes([elem_type]) + struct.pack(">i", len(items)) + b"".join(items)


def ints(*vals):
    return [struct.pack(">i", v) for v in vals]


def compound_payload(*tags):
    return b"".join(tags) + b"\x00"


root = compound_payload(
    tag_int("DataVersion", 3465),                                     # 1.20.1
    tag_list("size", 3, ints(1, 1, 1)),
    tag_list("palette", 10, [compound_payload(tag_string("Name", "minecraft:air"))]),
    tag_list("blocks", 10, [compound_payload(tag_list("pos", 3, ints(0, 0, 0)), tag_int("state", 0))]),
    tag_list("entities", 10, []),
)
OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_bytes(gzip.compress(b"\x0a" + name("") + root))
print("ok:", OUT)
