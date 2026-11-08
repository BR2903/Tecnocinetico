"""Texturas del Anclador (modelo AnchorModel, 64x64):
textures/entity/anchor.png, anchor_glow.png (núcleo y cristal azul-violeta) y anchor_charge.png (blanco, al cargar).

Uso (desde forge-1.20.1-47.4.10-mdk):  python tools/anchor_texture.py [preview.png]
Estilo: máquina del Núcleo Directriz como la torreta, pero de placas de inercita azul-gris (pesada), con franjas
de peligro, rodillas de cobre y un núcleo de inercita que brilla azul-violeta.
"""
import sys
from pathlib import Path
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from turret_texture import ROOT, STEEL, COPPER, HAZARD, box  # noqa: E402

INER = [(40, 46, 60), (64, 72, 90), (88, 98, 120), (118, 130, 154)]
VIOLET = [(60, 60, 150), (110, 124, 230), (200, 210, 255)]
WHITE = [(170, 170, 210), (230, 230, 255), (255, 255, 255)]


def iplate(face, x, y, w, h):
    if x in (0, w - 1) or y in (0, h - 1):
        return INER[0]
    if y == 1 or x == 1:
        return INER[3] if face in ("top", "front") else INER[2]
    return INER[2] if face == "top" else INER[1]


def build(glowc):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))

    # casco 10x7x10: placas, franja de peligro abajo, remaches de cobre, rejilla arriba
    def hull(face, x, y, w, h):
        if face == "top":
            if (x, y) in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):
                return COPPER[2]
            if 3 <= x <= w - 4 and 3 <= y <= h - 4:
                return STEEL[0] if (x + y) % 2 else INER[1]
        if face in ("front", "back", "left", "right") and y == h - 2:
            return HAZARD[(x // 2) % 2]
        if face in ("left", "right") and y == 2 and x in (2, w - 3):
            return glowc[1], True                                          # luces laterales
        return iplate(face, x, y, w, h)
    box(img, glow, 0, 0, 10, 7, 10, hull)
    # ventana del núcleo 4x3x1
    box(img, glow, 40, 0, 4, 3, 1, lambda f, x, y, w, h: ((glowc[2] if (x, y) in ((1, 1), (2, 1)) else glowc[1]), True)
        if f == "front" else STEEL[0])
    # muslo 2x2x8: acero con bandas de inercita
    box(img, glow, 0, 18, 2, 2, 8, lambda f, x, y, w, h: INER[2] if ((y if f in ("top", "bottom") else x) % 3 == 0) else STEEL[1])
    # espinilla 2x14x2: acero oscuro, pie negro con franja
    box(img, glow, 20, 18, 2, 14, 2, lambda f, x, y, w, h: STEEL[0] if y >= h - 2 else HAZARD[0] if y == h - 3
        else INER[1] if y % 4 == 0 else STEEL[1])
    # rodilla 3x3x3 de cobre
    box(img, glow, 40, 26, 3, 3, 3, lambda f, x, y, w, h: COPPER[2] if (x, y) == (1, 1) else COPPER[1] if y < 2 else COPPER[0])
    # base del emisor 4x3x4 y cañón 2x2x5
    box(img, glow, 28, 18, 4, 3, 4, lambda f, x, y, w, h: iplate(f, x, y, w, h) if w > 2 and h > 2 else INER[1])
    box(img, glow, 44, 18, 2, 2, 5, lambda f, x, y, w, h: (glowc[2], True) if f == "front"
        else COPPER[1] if (f in ("top", "bottom") and y == 1) or (f in ("left", "right") and x == 1) else STEEL[0])
    # cristal 3x3x3 (brilla)
    box(img, glow, 28, 26, 3, 3, 3, lambda f, x, y, w, h: ((glowc[2] if (x, y) == (1, 1) else glowc[1] if (x + y) % 2 == 0
                                                             else glowc[0]), True))
    return img, glow


if __name__ == "__main__":
    img, glow = build(VIOLET)
    img.save(ROOT / "anchor.png")
    glow.save(ROOT / "anchor_glow.png")
    _, charge = build(WHITE)
    charge.save(ROOT / "anchor_charge.png")
    if len(sys.argv) > 1:
        prev = Image.new("RGBA", (512, 512), (48, 50, 56, 255))
        prev.alpha_composite(img.resize((512, 512), Image.NEAREST))
        prev.save(sys.argv[1])
    print("ok: anclador")
