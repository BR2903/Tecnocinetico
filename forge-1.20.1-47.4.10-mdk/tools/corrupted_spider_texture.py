"""Texturas de la Araña Corrupta (modelo vanilla de araña, 64x32):
textures/entity/corrupted_spider.png y corrupted_spider_glow.png (ojos y cristales de cinetrita).

Uso (desde forge-1.20.1-47.4.10-mdk):  python tools/corrupted_spider_texture.py [preview.png]
Estilo: quitina negra con placas de metal oxidado fusionadas, cristales cian que brotan del abdomen,
ocho ojos cian y articulaciones de cobre.
"""
import sys
from pathlib import Path
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from turret_texture import ROOT, COPPER, box  # noqa: E402

CHITIN = [(16, 15, 18), (28, 26, 30), (42, 40, 46), (60, 58, 64)]
RUST = [(74, 44, 30), (108, 64, 40)]
CRYS = [(26, 110, 124), (52, 190, 206), (150, 240, 250), (220, 255, 255)]


def shade(face, y, h):
    if face == "top":
        return CHITIN[2]
    if face == "bottom":
        return CHITIN[0]
    return CHITIN[2] if y == 0 else CHITIN[1] if y < h - 1 else CHITIN[0]


def build():
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    glow = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    # cabeza 8x8x8: ocho ojos cian (dos grandes, seis pequeños) y quelíceros de metal
    def head(face, x, y, w, h):
        if face == "front":
            if y == 2 and x in (1, 2, 5, 6):
                return CRYS[2], True
            if y in (3, 4) and x in (2, 5):
                return (CRYS[3] if y == 3 else CRYS[1]), True              # ojos grandes
            if y == 3 and x in (0, 7):
                return CRYS[1], True
            if y >= 6 and x in (2, 5):
                return RUST[1] if y == 6 else COPPER[1]                    # quelíceros
            if y == 5 and 2 <= x <= 5:
                return CHITIN[0]
        if face == "top" and (x, y) in ((2, 5), (5, 5), (3, 6), (4, 6)):
            return RUST[0]
        return shade(face, y, h)
    box(img, glow, 32, 4, 8, 8, 8, head)

    # tórax 6x6x6: placas oxidadas
    box(img, glow, 0, 0, 6, 6, 6, lambda f, x, y, w, h: RUST[(x + y) % 2] if f == "top" and 1 <= x <= w - 2 and 1 <= y <= h - 2
        else shade(f, y, h))

    # abdomen 10x8x12: quitina con placas de metal y cristales que brotan del lomo (brillan)
    crystals = {(3, 3), (3, 4), (4, 4), (6, 7), (6, 8), (7, 8), (2, 9), (5, 2)}
    cores = {(3, 4), (6, 8)}

    def abdomen(face, x, y, w, h):
        if face == "top":
            if (x, y) in cores:
                return CRYS[3], True
            if (x, y) in crystals:
                return CRYS[2 if (x + y) % 2 else 1], True
            if any(abs(x - cx) + abs(y - cy) == 1 for cx, cy in cores):
                return CRYS[0]
            if y % 4 == 0:
                return CHITIN[1]                                          # segmentos
            return RUST[0] if (x in (0, w - 1)) else CHITIN[2] if (x + y) % 5 else CHITIN[3]
        if face == "back":
            if (x, y) in ((4, 2), (5, 2), (4, 3)):
                return CRYS[1], True
            if y >= h - 3 and 3 <= x <= 6:
                return RUST[1] if y == h - 3 else RUST[0]                 # hileras de metal
        if face in ("left", "right") and y in (2, 5):
            return CHITIN[0]
        if face in ("left", "right") and (x * 3 + y) % 11 == 0:
            return CRYS[1], True                                          # astillas en los costados
        return shade(face, y, h)
    box(img, glow, 0, 12, 10, 8, 12, abdomen)

    # patas 16x2x2: segmentos negros con articulaciones de cobre y punta de metal
    def leg(face, x, y, w, h):
        if face in ("front", "back", "top", "bottom"):
            if x in (5, 6, 11):
                return COPPER[1] if face != "bottom" else COPPER[0]
            if (face in ("front", "back") and x == 0) or (face in ("top", "bottom") and x == 0):
                return (150, 150, 156)                                    # punta afilada
        return shade(face, y, h) if face not in ("top",) else CHITIN[2]
    box(img, glow, 18, 0, 16, 2, 2, leg)
    return img, glow


if __name__ == "__main__":
    img, glow = build()
    img.save(ROOT / "corrupted_spider.png")
    glow.save(ROOT / "corrupted_spider_glow.png")
    if len(sys.argv) > 1:
        prev = Image.new("RGBA", (512, 256), (48, 50, 56, 255))
        prev.alpha_composite(img.resize((512, 256), Image.NEAREST))
        prev.save(sys.argv[1])
    print("ok: araña corrupta")
