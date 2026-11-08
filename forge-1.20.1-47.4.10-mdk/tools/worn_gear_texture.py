"""Textura de los gadgets puestos sobre el jugador (modelo client/WornGearModel, 64x64):
textures/entity/worn_gear.png y worn_gear_glow.png (lentes de las gafas, LED, toberas, turbinas).

Uso (desde forge-1.20.1-47.4.10-mdk):  python tools/worn_gear_texture.py [preview.png]
Las zonas deben coincidir con los texOffs del modelo:
  gafas: tubos (0,0) 3x3x3, lentes (12,0), batería (18,0), cuerpo (0,6) 5x2x2, soporte (14,6), brazo (22,6),
         correas (0,10)-(32,23) y correa superior (32,30)-(54,40)
  jetpack: (32,0)..(58,29)       botas: caña (0,24) 5x5x5, suela (0,34) 5x1x6, turbina (20,24) 3x4x3, aletas (22,34)
"""
import sys
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tecnocinetico/textures/entity"
STEEL = [(24, 25, 29), (40, 42, 47), (60, 64, 72), (86, 92, 104)]
COPPER = [(96, 56, 32), (150, 92, 56), (206, 140, 92), (236, 186, 132)]
LEATHER = [(40, 38, 32), (56, 52, 44), (72, 68, 56)]                  # correa de nailon oscuro
OLIVE = [(34, 38, 28), (52, 58, 42), (72, 80, 58), (96, 106, 78)]     # carcasa militar de las gafas
NV = [(40, 140, 60), (90, 230, 110), (200, 255, 200)]                  # verde de visión nocturna
CYAN = [(30, 130, 146), (70, 210, 226), (180, 248, 255)]


def faces(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def build():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))

    def put(x, y, c, g=False):
        img.putpixel((x, y), (*c, 255))
        if g:
            glow.putpixel((x, y), (*c, 255))

    def box(u, v, w, h, d, fn):
        for name, (x0, y0, fw, fh) in faces(u, v, w, h, d).items():
            for y in range(fh):
                for x in range(fw):
                    r = fn(name, x, y, fw, fh)
                    c, g = r if isinstance(r[0], tuple) else (r, False)
                    put(x0 + x, y0 + y, c, g)

    def fill(x0, y0, x1, y1, fn):
        for y in range(y0, y1):
            for x in range(x0, x1):
                put(x, y, fn(x, y))

    # ---- gafas de visión nocturna
    # tubos: carcasa oliva con aro negro al frente (la lente va en una pieza aparte, delante)
    box(0, 0, 3, 3, 3, lambda f, x, y, w, h: (16, 16, 18) if f == "front"
        else OLIVE[3] if f == "top" else OLIVE[1] if (y == h - 1 or x == 0) else OLIVE[2])
    fill(12, 0, 18, 3, lambda x, y: NV[2] if (x, y) in ((13, 1), (14, 1)) else NV[1])
    for x in range(12, 18):
        for y in range(0, 3):
            glow.putpixel((x, y), (*img.getpixel((x, y))[:3], 255))
    # batería en la nuca con LED verde
    box(18, 0, 3, 2, 1, lambda f, x, y, w, h: (NV[1], True) if (f == "back" and (x, y) == (1, 0)) else OLIVE[2] if y == 0 else OLIVE[1])
    box(0, 6, 5, 2, 2, lambda f, x, y, w, h: OLIVE[3] if f == "top" else (16, 16, 18) if (f == "front" and y == 1) else OLIVE[2])
    box(14, 6, 3, 2, 1, lambda f, x, y, w, h: STEEL[3] if y == 0 else STEEL[2])
    box(22, 6, 1, 2, 1, lambda f, x, y, w, h: STEEL[2])
    fill(0, 10, 32, 23, lambda x, y: LEATHER[2] if y in (10, 13) else LEATHER[1] if (x + y) % 5 else LEATHER[0])
    fill(32, 30, 56, 41, lambda x, y: LEATHER[2] if (x + y) % 6 == 0 else LEATHER[1])

    # ---- jetpack (sin cambios)
    box(32, 0, 7, 9, 1, lambda f, x, y, w, h: COPPER[2] if (x, y) in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2))
        else STEEL[3] if y == 0 else STEEL[1])
    box(32, 12, 3, 10, 3, lambda f, x, y, w, h: STEEL[2] if f in ("top", "bottom")
        else COPPER[0] if y in (1, 8) else COPPER[3] if x == 1 else COPPER[1] if x == 0 else COPPER[2])
    box(48, 0, 2, 1, 2, lambda f, x, y, w, h: STEEL[3])
    box(48, 4, 2, 2, 2, lambda f, x, y, w, h: (CYAN[1], True) if f == "bottom" else STEEL[1] if y else STEEL[2])
    box(44, 12, 2, 5, 2, lambda f, x, y, w, h: (CYAN[1], True) if f == "bottom" else COPPER[1] if y % 2 else STEEL[2])
    box(52, 12, 1, 8, 2, lambda f, x, y, w, h: STEEL[3] if y == 0 else STEEL[2])
    box(32, 26, 3, 1, 2, lambda f, x, y, w, h: STEEL[3] if f == "top" else STEEL[1])

    # ---- botas propulsoras
    def shell(f, x, y, w, h):
        if f == "top":
            return STEEL[1]
        if f == "bottom":
            return STEEL[0]
        if y == 2 and f in ("front", "left", "right"):
            return (CYAN[1], True)                                      # franja luminosa
        if y == 0:
            return STEEL[3]                                             # canto superior
        if f == "front" and y >= 3:
            return STEEL[2] if x % 2 else STEEL[1]                      # puntera con relieve
        return STEEL[2] if (x + y) % 4 else STEEL[1]
    box(0, 24, 5, 5, 5, shell)
    box(0, 34, 5, 1, 6, lambda f, x, y, w, h: (20, 20, 22) if f == "bottom" else (34, 34, 38))
    box(20, 24, 3, 4, 3, lambda f, x, y, w, h: ((CYAN[2] if (x + y) % 2 else CYAN[1]), True) if f == "bottom"
        else STEEL[0] if f == "top" else COPPER[1] if y in (0, 3) else STEEL[2])
    box(22, 34, 1, 3, 2, lambda f, x, y, w, h: STEEL[3] if y == 0 else COPPER[1])
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
