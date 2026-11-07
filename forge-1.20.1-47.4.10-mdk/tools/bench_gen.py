"""Modelo y textura del Banco de Armas Cinético.
python bench_gen.py <assets/tecnocinetico> [preview_dir]
"""
import json
import math
import random
import sys
from pathlib import Path
from PIL import Image

ASSETS = Path(sys.argv[1])
PPU = 2
random.seed(77)

STEEL = (58, 62, 70); STEEL_HI = (104, 110, 122); STEEL_LO = (30, 32, 38)
INER = (64, 72, 90); INER_HI = (104, 114, 136); INER_LO = (38, 43, 56)
MAT = (32, 36, 42); MAT_LINE = (44, 50, 58)
CYAN = (64, 206, 228); CYAN_HI = (176, 246, 255); CYAN_DIM = (40, 120, 140)
COPPER = (150, 92, 56)


def clamp(v):
    return max(0, min(255, int(round(v))))


def col(c, k=1.0):
    return (clamp(c[0] * k), clamp(c[1] * k), clamp(c[2] * k), 255)


def mix(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


class Face:
    def __init__(self, name, w, h):
        self.name, self.w, self.h = name, w, h

    def vf(self, y):
        return (y + 0.5) / self.h

    def uf(self, x):
        return (x + 0.5) / self.w

    @property
    def side(self):
        return self.name in ("north", "south", "east", "west")


def shaded(base, hi, lo, f, x, y, noise=0.04):
    c = base
    if f.side:
        c = mix(base, hi, max(0.0, 0.3 - f.vf(y)))
        c = mix(c, lo, max(0.0, f.vf(y) - 0.75))
    elif f.name == "up":
        c = mix(base, hi, 0.15)
    else:
        c = mix(base, lo, 0.4)
    if f.w > 2 and f.h > 2:
        if y == 0 or x == 0:
            c = mix(c, hi, 0.5)
        elif y == f.h - 1 or x == f.w - 1:
            c = mix(c, lo, 0.5)
    return col(c, 1 + random.uniform(-noise, noise))


def m_top(f, x, y):
    if f.name != "up":
        # canto del tablero: inercita con tira cian
        if f.side and f.h >= 3 and y == 1:
            return col(CYAN, 0.8)
        return shaded(INER, INER_HI, INER_LO, f, x, y)
    # Tapete de trabajo: marco metálico, rejilla y silueta cian de la pistola
    if x < 2 or y < 2 or x >= f.w - 2 or y >= f.h - 2:
        return shaded(STEEL, STEEL_HI, STEEL_LO, f, x, y)
    sx, sy = x - 2, y - 2   # coordenadas del tapete (y=0 delante)
    # silueta (de perfil, apuntando al este) delante-centro del tapete
    slide = 5 <= sx <= 21 and 8 <= sy <= 10
    grip = 6 <= sx <= 8 and 11 <= sy <= 15
    barrel = 22 <= sx <= 23 and sy == 9
    inside = slide or grip or barrel
    if inside:
        edge = not ((5 < sx < 21 and 8 < sy < 10) or (6 < sx < 8 and 11 <= sy < 15))
        return col(CYAN) if edge else col(CYAN_DIM, 0.7)
    if sx % 4 == 0 or sy % 4 == 0:
        return col(MAT_LINE)
    return col(MAT, 1 + random.uniform(-0.05, 0.05))


def m_leg(f, x, y):
    if f.side and y % 6 == 5:
        return col(INER_LO)
    return shaded(INER, INER_HI, INER_LO, f, x, y)


def m_shelf(f, x, y):
    if f.name == "up" and (x + y) % 4 == 0:
        return col(STEEL_HI, 0.9)            # chapa estriada
    return shaded(STEEL, STEEL_HI, STEEL_LO, f, x, y)


def m_back(f, x, y):
    if f.name == "north" and f.h >= 3:
        if y == f.h // 2:
            return col(CYAN_HI) if x % 8 < 6 else col(CYAN_DIM)   # tira de luz
    return shaded(INER, INER_HI, INER_LO, f, x, y)


def m_steel(f, x, y):
    return shaded(STEEL, STEEL_HI, STEEL_LO, f, x, y)


def m_jaw(f, x, y):
    if f.side and x % 2 == 0:
        return col(STEEL_LO)                 # dientes de la mordaza
    return shaded(STEEL_HI, (150, 156, 168), STEEL, f, x, y)


def m_lamp(f, x, y):
    if f.name == "up" or f.side and f.vf(y) < 0.6:
        t = abs(f.uf(x) - 0.5) * 2
        return col(mix(CYAN_HI, CYAN, t))
    return shaded(STEEL, STEEL_HI, STEEL_LO, f, x, y)


def m_apron(f, x, y):
    if f.name == "north" and f.h >= 2 and abs(f.uf(x) - 0.5) < 0.12:
        return col(COPPER)                   # placa de cobre con el emblema
    return shaded(INER, INER_HI, INER_LO, f, x, y)


MATS = {"top": m_top, "leg": m_leg, "shelf": m_shelf, "back": m_back, "steel": m_steel,
        "jaw": m_jaw, "lamp": m_lamp, "apron": m_apron}

ELEMENTS = [
    {"name": "top", "mat": "top", "from": [0, 12, 0], "to": [16, 14, 16]},
    {"name": "back_riser", "mat": "back", "from": [0, 14, 13], "to": [16, 16, 16]},
    {"name": "vise_base", "mat": "steel", "from": [10.5, 14, 2.5], "to": [14.5, 14.75, 6.5]},
    {"name": "vise_jaw_front", "mat": "jaw", "from": [11, 14.75, 3], "to": [14, 16, 4]},
    {"name": "vise_jaw_back", "mat": "jaw", "from": [11, 14.75, 5], "to": [14, 16, 6]},
    {"name": "lamp", "mat": "lamp", "from": [1.5, 14, 10], "to": [3.5, 15, 12]},
    {"name": "apron", "mat": "apron", "from": [2.5, 10.5, 0.5], "to": [13.5, 12, 1.5]},
    {"name": "leg_nw", "mat": "leg", "from": [0.5, 0, 0.5], "to": [2.5, 12, 2.5]},
    {"name": "leg_ne", "mat": "leg", "from": [13.5, 0, 0.5], "to": [15.5, 12, 2.5]},
    {"name": "leg_sw", "mat": "leg", "from": [0.5, 0, 13.5], "to": [2.5, 12, 15.5]},
    {"name": "leg_se", "mat": "leg", "from": [13.5, 0, 13.5], "to": [15.5, 12, 15.5]},
    {"name": "shelf", "mat": "shelf", "from": [1, 3, 1], "to": [15, 4.5, 15]},
]
FACES = ["north", "south", "east", "west", "up", "down"]


def face_size(el, face):
    d = [el["to"][i] - el["from"][i] for i in range(3)]
    w, h = {"north": (d[0], d[1]), "south": (d[0], d[1]), "east": (d[2], d[1]),
            "west": (d[2], d[1]), "up": (d[0], d[2]), "down": (d[0], d[2])}[face]
    return max(1, round(w * PPU)), max(1, round(h * PPU))


rects = [(i, f, *face_size(el, f)) for i, el in enumerate(ELEMENTS) for f in FACES]
for SIZE in (64, 128, 256):
    placed, x, y, row, ok = {}, 0, 0, 0, True
    for i, f, w, h in sorted(rects, key=lambda r: (-r[3], -r[2])):
        if x + w > SIZE:
            x, y, row = 0, y + row, 0
        if y + h > SIZE:
            ok = False
            break
        placed[(i, f)] = (x, y, w, h)
        x += w
        row = max(row, h)
    if ok:
        break

tex = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
uv = {}
for (i, f), (x0, y0, w, h) in placed.items():
    face = Face(f, w, h)
    for yy in range(h):
        for xx in range(w):
            tex.putpixel((x0 + xx, y0 + yy), MATS[ELEMENTS[i]["mat"]](face, xx, yy))
    s = 16 / SIZE
    uv[(i, f)] = [round(x0 * s, 4), round(y0 * s, 4), round((x0 + w) * s, 4), round((y0 + h) * s, 4)]
tex.save(ASSETS / "textures/block/weapon_bench.png")

model = {
    "parent": "minecraft:block/block",
    "credit": "Tecnocinetico - bench_gen.py",
    "texture_size": [SIZE, SIZE],
    "textures": {"0": "tecnocinetico:block/weapon_bench", "particle": "tecnocinetico:block/weapon_bench"},
    "elements": [{"name": el["name"], "from": el["from"], "to": el["to"],
                  "faces": {f: {"uv": uv[(i, f)], "texture": "#0"} for f in FACES}}
                 for i, el in enumerate(ELEMENTS)],
}
(ASSETS / "models/block/weapon_bench.json").write_text(json.dumps(model, indent=1), encoding="utf-8")
(ASSETS / "models/item/weapon_bench.json").write_text(
    json.dumps({"parent": "tecnocinetico:block/weapon_bench"}, indent=1), encoding="utf-8")
(ASSETS / "blockstates/weapon_bench.json").write_text(json.dumps({"variants": {
    "facing=north": {"model": "tecnocinetico:block/weapon_bench"},
    "facing=east": {"model": "tecnocinetico:block/weapon_bench", "y": 90},
    "facing=south": {"model": "tecnocinetico:block/weapon_bench", "y": 180},
    "facing=west": {"model": "tecnocinetico:block/weapon_bench", "y": 270}}}, indent=1), encoding="utf-8")
print("textura", SIZE)
