"""Texturas de la Torreta Centinela (modelo SentryTurretModel, 64x64):
textures/entity/sentry_turret.png, sentry_turret_glow.png (sensor cian) y sentry_turret_charge.png (sensor rojo).

Uso (desde forge-1.20.1-47.4.10-mdk):  python tools/turret_texture.py [preview.png]
Estilo: máquina del Núcleo Directriz: acero pavonado con paneles y cantos claros, franjas de peligro, cobre en
las juntas y un sensor que brilla.
"""
import sys
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tecnocinetico/textures/entity"
STEEL = [(22, 23, 27), (36, 38, 44), (56, 60, 68), (86, 92, 104)]
COPPER = [(96, 52, 30), (150, 88, 52), (204, 138, 88)]
HAZARD = [(214, 168, 40), (30, 30, 32)]
CYAN = [(30, 130, 146), (70, 210, 226), (180, 248, 255)]
RED = [(140, 20, 20), (240, 60, 50), (255, 170, 150)]


def faces(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def box(img, glow, u, v, w, h, d, fn):
    for name, (x0, y0, fw, fh) in faces(u, v, w, h, d).items():
        for y in range(fh):
            for x in range(fw):
                r = fn(name, x, y, fw, fh)
                if r is None:
                    continue
                c, g = r if isinstance(r[0], tuple) else (r, False)
                img.putpixel((x0 + x, y0 + y), (*c, 255))
                if g:
                    glow.putpixel((x0 + x, y0 + y), (*c, 255))


def panel(face, x, y, w, h):
    """Panel de acero: canto superior claro, contorno oscuro, cuerpo liso."""
    if x in (0, w - 1) or y in (0, h - 1):
        return STEEL[0]
    if y == 1 or x == 1:
        return STEEL[3] if face in ("top", "front") else STEEL[2]
    return STEEL[2] if face == "top" else STEEL[1]


def build(sensor):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))

    # placa base 12x2x12: franjas de peligro en el canto, tornillos en las esquinas
    def plate(face, x, y, w, h):
        if face == "top":
            if (x, y) in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):
                return COPPER[2]
            return panel(face, x, y, w, h)
        if face != "bottom":
            return HAZARD[((x + y) // 2) % 2]
        return STEEL[0]
    box(img, glow, 0, 0, 12, 2, 12, plate)
    # columna 6x7x6: acero con anillos de cobre
    box(img, glow, 0, 14, 6, 7, 6, lambda f, x, y, w, h: COPPER[1] if f not in ("top", "bottom") and y in (1, 5)
        else panel(f, x, y, w, h))
    # patas 14x1x2 y 2x1x14
    box(img, glow, 24, 14, 14, 1, 2, lambda f, x, y, w, h: STEEL[1] if (x % 4) else COPPER[1])
    box(img, glow, 32, 44, 2, 1, 14, lambda f, x, y, w, h: STEEL[1] if (y % 4 if f in ("top", "bottom") else x % 4) else COPPER[1])

    # cabeza 9x7x10: paneles con rejilla de ventilación arriba y franja de peligro al frente abajo
    def head(face, x, y, w, h):
        if face == "top" and 2 <= x <= w - 3 and 3 <= y <= h - 3:
            return STEEL[0] if y % 2 else STEEL[2]
        if face == "front" and y == h - 2:
            return HAZARD[(x // 2) % 2]
        if face in ("left", "right") and y == h // 2:
            return COPPER[1]
        return panel(face, x, y, w, h)
    box(img, glow, 0, 27, 9, 7, 10, head)
    # blindaje lateral 1x5x6
    box(img, glow, 38, 27, 1, 5, 6, lambda f, x, y, w, h: STEEL[3] if y == 0 else STEEL[2] if (x + y) % 3 else STEEL[1])
    # cañones 2x2x7: boca negra, anillo de cobre
    box(img, glow, 0, 44, 2, 2, 7, lambda f, x, y, w, h: (8, 8, 10) if f == "front"
        else COPPER[2] if (f in ("top", "bottom") and y == 1) or (f in ("left", "right") and x == 1) else STEEL[1])
    # sensor 3x2x1 (brilla) y antena 1x3x1
    box(img, glow, 18, 44, 3, 2, 1, lambda f, x, y, w, h: ((sensor[2] if (x, y) == (1, 0) else sensor[1]), True)
        if f == "front" else STEEL[0])
    box(img, glow, 26, 44, 1, 3, 1, lambda f, x, y, w, h: (sensor[1], True) if y == 0 else STEEL[2])
    return img, glow


if __name__ == "__main__":
    ROOT.mkdir(parents=True, exist_ok=True)
    img, glow = build(CYAN)
    img.save(ROOT / "sentry_turret.png")
    glow.save(ROOT / "sentry_turret_glow.png")
    _, charge = build(RED)
    charge.save(ROOT / "sentry_turret_charge.png")
    if len(sys.argv) > 1:
        prev = Image.new("RGBA", (64 * 8, 64 * 8), (48, 50, 56, 255))
        prev.alpha_composite(img.resize((512, 512), Image.NEAREST))
        prev.save(sys.argv[1])
    print("ok: torreta")
