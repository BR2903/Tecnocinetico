"""Prensa Cinética Automática al estilo de las máquinas de Create: base con el soporte de la pieza, cuatro columnas
con remates de latón, cabecera arriba y el cabezal aparte (lo mueve client/AutoPressRenderer).

python tools/press_model_gen.py      (desde forge-1.20.1-47.4.10-mdk)
Escribe textures/block/auto_press.png (atlas 64x64, zonas de 16 px), models/block/auto_kinetic_press(_on).json,
models/block/auto_press_head.json y el modelo del objeto (el estado ya existía: facing/powered).
"""
import json
import random
import sys
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gunkit import resolve_zfight  # noqa: E402

A = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tecnocinetico"
Z = 16
ZONES = {"casing": (0, 0), "frame": (1, 0), "brass": (2, 0), "depot": (3, 0), "panel": (0, 1), "top": (1, 1),
         "head": (2, 1), "glow": (3, 1), "hazard": (0, 2), "shaft": (1, 2)}
STEEL = [(26, 28, 33), (44, 47, 54), (66, 70, 79), (96, 102, 112), (140, 146, 156)]


def atlas():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r = random.Random(21)

    def zone(n):
        c, rr = ZONES[n]
        return c * Z, rr * Z

    def plate(n, base, rivets=True):
        x0, y0 = zone(n)
        for y in range(Z):
            for x in range(Z):
                k = 1 + r.uniform(-0.025, 0.025)
                img.putpixel((x0 + x, y0 + y), tuple(int(min(255, v * k)) for v in base) + (255,))
        d.rectangle([x0, y0, x0 + Z - 1, y0 + Z - 1], outline=STEEL[0])
        d.line([(x0 + 1, y0 + 1), (x0 + Z - 2, y0 + 1)], fill=tuple(min(255, int(v * 1.35)) for v in base))
        d.line([(x0 + 1, y0 + 1), (x0 + 1, y0 + Z - 2)], fill=tuple(min(255, int(v * 1.2)) for v in base))
        if rivets:
            for (px, py) in ((3, 3), (Z - 4, 3), (3, Z - 4), (Z - 4, Z - 4)):
                d.point((x0 + px, y0 + py), fill=STEEL[4])
        return x0, y0

    plate("casing", (74, 78, 88))                                              # chapa de la carcasa
    plate("frame", STEEL[1], rivets=False)
    x0, y0 = plate("brass", (196, 152, 70), rivets=False)                      # latón (remates, como Create)
    d.line([(x0 + 2, y0 + 3), (x0 + 13, y0 + 3)], fill=(240, 206, 120))
    x0, y0 = plate("depot", (58, 62, 70), rivets=False)                        # soporte de la pieza
    d.rectangle([x0 + 3, y0 + 3, x0 + 12, y0 + 12], outline=(220, 180, 40))
    x0, y0 = plate("panel", (70, 74, 84))                                      # costado: rejilla y manómetro
    for yy in range(4, 12, 2):
        d.line([(x0 + 3, y0 + yy), (x0 + 8, y0 + yy)], fill=STEEL[0])
    d.ellipse([x0 + 9, y0 + 4, x0 + 13, y0 + 8], fill=(220, 220, 210), outline=STEEL[0])
    d.line([(x0 + 11, y0 + 6), (x0 + 12, y0 + 5)], fill=(200, 40, 30))
    x0, y0 = plate("top", (80, 84, 94))                                        # tapa con el pasante del eje
    d.ellipse([x0 + 4, y0 + 4, x0 + 11, y0 + 11], fill=STEEL[1], outline=(196, 152, 70))
    x0, y0 = plate("head", (110, 116, 128), rivets=False)                     # cabezal de la prensa
    d.rectangle([x0 + 2, y0 + 11, x0 + 13, y0 + 13], fill=(196, 152, 70))
    x0, y0 = zone("glow")
    for y in range(Z):
        for x in range(Z):
            img.putpixel((x0 + x, y0 + y), (110, 235, 250, 255))
    x0, y0 = zone("hazard")
    for y in range(Z):
        for x in range(Z):
            img.putpixel((x0 + x, y0 + y), ((222, 182, 42) if ((x + y) // 4) % 2 == 0 else (34, 34, 36)) + (255,))
    x0, y0 = zone("shaft")
    for y in range(Z):
        for x in range(Z):
            t = abs((x % 4) - 1.5) / 1.5
            img.putpixel((x0 + x, y0 + y), (int(170 - 40 * t), int(174 - 40 * t), int(184 - 40 * t), 255))
    img.save(A / "textures/block/auto_press.png")


def uv(n):
    c, r = ZONES[n]
    return [c * 4, r * 4, c * 4 + 4, r * 4 + 4]


def box(f, t, mat, **over):
    faces = {}
    for face in ("north", "south", "east", "west", "up", "down"):
        m = over.get(face, mat)
        if m == "#lamp":
            faces[face] = {"uv": [0, 0, 16, 16], "texture": "#lamp"}
        elif m:
            faces[face] = {"uv": uv(m), "texture": "#0"}
    return {"from": f, "to": t, "faces": faces}


def body():
    e = [box([0, 0, 0], [16, 6, 16], "casing", north="panel", south="panel", east="panel", west="panel", up="depot"),
         box([0, 5, 0], [16, 6, 16], "hazard", up=None, down=None),                    # franja del borde
         box([3, 6, 3], [13, 6.5, 13], "frame", up="depot"),                           # soporte de la pieza
         box([0, 12.5, 0], [16, 16, 16], "casing", up="top", north="panel", south="panel"),   # cabecera
         box([6.5, 12.0, 6.5], [9.5, 12.5, 9.5], "brass"),                              # guía del eje
         box([6.5, 1.5, -0.3], [9.5, 4.5, 0.0], "frame", north="#lamp")]                # luz de estado (frente)
    for (x, z) in ((0, 0), (13, 0), (0, 13), (13, 13)):                                # columnas
        e.append(box([x, 6, z], [x + 3, 12.5, z + 3], "frame"))
        e.append(box([x - 0.0, 6, z - 0.0], [x + 3, 7, z + 3], "brass"))               # remate inferior
        e.append(box([x, 11.5, z], [x + 3, 12.5, z + 3], "brass"))                     # remate superior
    return e


def head():
    """Cabezal + eje (el renderizador lo baja y lo sube): bloque 0..16 con el cabezal a la altura de reposo."""
    return [box([4, 9, 4], [12, 11, 12], "head", up="frame"),
            box([4.5, 8.4, 4.5], [11.5, 9, 11.5], "frame"),
            box([7, 11, 7], [9, 15.5, 9], "shaft"),                                  # no llega a la tapa (parpadeaba)
            box([6.8, 10.8, 7.8], [9.2, 11.2, 8.2], "glow")]


def jw(p, data):
    p.write_text(json.dumps(data, indent=1) + "\n", encoding="utf-8")


def main():
    atlas()
    els = resolve_zfight(body())
    for suffix, lamp in (("", "press_lamp_off"), ("_on", "press_lamp_on")):
        jw(A / f"models/block/auto_kinetic_press{suffix}.json", {
            "parent": "minecraft:block/block", "texture_size": [64, 64],
            "textures": {"0": "tecnocinetico:block/auto_press", "lamp": f"tecnocinetico:block/{lamp}", "particle": "tecnocinetico:block/auto_press"},
            "elements": els + resolve_zfight(head()) if False else els})
    jw(A / "models/block/auto_press_head.json", {"texture_size": [64, 64],
                                                 "textures": {"0": "tecnocinetico:block/auto_press", "particle": "tecnocinetico:block/auto_press"},
                                                 "elements": resolve_zfight(head())})
    # objeto: la prensa completa con el cabezal en reposo
    jw(A / "models/item/auto_kinetic_press.json", {
        "parent": "minecraft:block/block", "texture_size": [64, 64],
        "textures": {"0": "tecnocinetico:block/auto_press", "lamp": "tecnocinetico:block/press_lamp_off", "particle": "tecnocinetico:block/auto_press"},
        "elements": resolve_zfight(body() + head())})
    print("ok: prensa automática estilo Create")


if __name__ == "__main__":
    main()
