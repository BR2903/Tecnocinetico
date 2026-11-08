"""Textura de los gadgets puestos sobre el jugador (modelo client/WornGearModel, 64x64):
textures/entity/worn_gear.png y worn_gear_glow.png (lentes del visor, toberas del jetpack, rotor del propulsor).

Uso (desde forge-1.20.1-47.4.10-mdk):  python tools/worn_gear_texture.py [preview.png]
Misma paleta que los modelos de objeto (tools/ammo_gen.py).
"""
import sys
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tecnocinetico/textures/entity"
STEEL = [(24, 25, 29), (40, 42, 47), (60, 64, 72), (86, 92, 104)]
COPPER = [(96, 56, 32), (150, 92, 56), (206, 140, 92), (236, 186, 132)]
LEATHER = [(56, 38, 24), (74, 50, 32), (96, 66, 42)]
TEAL = [(30, 70, 66), (42, 92, 86), (60, 120, 110)]
CYAN = [(30, 130, 146), (70, 210, 226), (180, 248, 255)]


def faces(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def build():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))

    def box(u, v, w, h, d, fn):
        for name, (x0, y0, fw, fh) in faces(u, v, w, h, d).items():
            for y in range(fh):
                for x in range(fw):
                    r = fn(name, x, y, fw, fh)
                    c, g = r if isinstance(r[0], tuple) else (r, False)
                    img.putpixel((x0 + x, y0 + y), (*c, 255))
                    if g:
                        glow.putpixel((x0 + x, y0 + y), (*c, 255))

    def fill(x0, y0, x1, y1, fn):
        for y in range(y0, y1):
            for x in range(x0, x1):
                img.putpixel((x, y), (*fn(x, y), 255))

    # visor: tubos de acero con lente cian al frente, puente de cobre, correa de cuero (zona grande)
    box(0, 0, 3, 3, 2, lambda f, x, y, w, h: ((CYAN[2] if (x, y) == (0, 0) else CYAN[1]), True) if f == "front"
        else STEEL[3] if f == "top" else STEEL[2] if (x + y) % 3 else STEEL[1])
    box(10, 0, 2, 1, 1, lambda f, x, y, w, h: COPPER[2])
    fill(0, 10, 32, 23, lambda x, y: LEATHER[2] if y in (10, 13) else LEATHER[1] if (x + y) % 5 else LEATHER[0])
    # jetpack: placa de acero con tornillos, depósitos de cobre con aros, tapas y toberas (boca brillante)
    box(32, 0, 7, 9, 1, lambda f, x, y, w, h: COPPER[2] if (x, y) in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2))
        else STEEL[3] if y == 0 else STEEL[1])
    box(32, 12, 3, 10, 3, lambda f, x, y, w, h: STEEL[2] if f in ("top", "bottom")
        else COPPER[0] if y in (1, 8) else COPPER[3] if x == 1 else COPPER[1] if x == 0 else COPPER[2])
    box(48, 0, 2, 1, 2, lambda f, x, y, w, h: STEEL[3])
    box(48, 4, 2, 2, 2, lambda f, x, y, w, h: (CYAN[1], True) if f == "bottom" else STEEL[1] if y else STEEL[2])
    box(44, 12, 2, 5, 2, lambda f, x, y, w, h: (CYAN[1], True) if f == "bottom" else COPPER[1] if y % 2 else STEEL[2])
    box(52, 12, 1, 8, 2, lambda f, x, y, w, h: STEEL[3] if y == 0 else STEEL[2])
    box(32, 26, 3, 1, 2, lambda f, x, y, w, h: STEEL[3] if f == "top" else STEEL[1])
    # propulsor: carcasa de acero, rotor cian (brilla), aleta verde azulada
    box(0, 24, 5, 3, 5, lambda f, x, y, w, h: STEEL[3] if y == 0 else LEATHER[1] if y == 1 and f not in ("top", "bottom") else STEEL[1])
    box(20, 24, 2, 2, 1, lambda f, x, y, w, h: ((CYAN[2] if (x + y) % 2 else CYAN[1]), True) if f == "front" else STEEL[2])
    box(0, 34, 3, 1, 4, lambda f, x, y, w, h: TEAL[2] if f == "top" else TEAL[1] if x % 2 else TEAL[0])
    return img, glow


if __name__ == "__main__":
    img, glow = build()
    img.save(ROOT / "worn_gear.png")
    glow.save(ROOT / "worn_gear_glow.png")
    if len(sys.argv) > 1:
        prev = Image.new("RGBA", (512, 512), (48, 50, 56, 255))
        prev.alpha_composite(img.resize((512, 512), Image.NEAREST))
        prev.save(sys.argv[1])
    print("ok: gadgets puestos")
