"""Textura del Caza del Núcleo (modelo CoreFighterModel, 128x256): textures/entity/core_fighter.png y
core_fighter_glow.png (toberas, líneas cian, luces de las alas y reflejo de la cabina).

Uso (desde forge-1.20.1-47.4.10-mdk):  python tools/ship_texture.py [preview.png]
Estilo: máquina del Núcleo Directriz robada: paneles de grafito con juntas, franja de inercita azul-gris por el
costado, línea de energía cian, franjas de peligro junto a los motores, cabina de cristal oscuro con reflejo.
Las posiciones UV deben coincidir con CoreFighterModel (texOffs).
"""
import sys
from pathlib import Path
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from turret_texture import ROOT, STEEL, COPPER, HAZARD, box  # noqa: E402

GRAPH = [(24, 26, 30), (38, 40, 46), (54, 57, 64), (74, 78, 88)]
INER = [(48, 54, 70), (64, 72, 90), (88, 98, 120), (118, 130, 154)]
CYAN = [(30, 130, 146), (70, 210, 226), (180, 248, 255)]
GLASS = [(10, 24, 30), (16, 40, 50), (30, 70, 84), (120, 200, 220)]


def panels(face, x, y, w, h, step=8):
    """Paneles de grafito: juntas oscuras cada `step` px, canto claro arriba, más oscuro abajo."""
    if face == "bottom":
        return GRAPH[0] if x % step == 0 else GRAPH[1]
    if x % step == 0 or (face != "top" and y == h - 1):
        return GRAPH[0]
    if y == 0:
        return GRAPH[3]
    return GRAPH[2] if face == "top" else GRAPH[1] if y > h // 2 else GRAPH[2]


def build():
    img = Image.new("RGBA", (128, 256), (0, 0, 0, 0))
    glow = Image.new("RGBA", (128, 256), (0, 0, 0, 0))

    # fuselaje 12x7x50: franja de inercita y línea cian por los costados, peligro junto a los motores, número de serie
    def fus(face, x, y, w, h):
        if face in ("left", "right"):
            if y == 2:
                return CYAN[1], True
            if y in (3, 4):
                return INER[2] if y == 3 else INER[1]
            if x > w - 10 and y == 5:
                return HAZARD[(x // 2) % 2]
        if face == "top" and 3 <= x <= 8 and y % 10 == 4:
            return INER[3]
        if face == "front":
            return GRAPH[2] if y < h - 1 else GRAPH[0]
        if face == "back":
            return GRAPH[0] if 3 <= x <= 8 else GRAPH[1]
        return panels(face, x, y, w, h)
    box(img, glow, 0, 0, 12, 7, 50, fus)
    # morro 8x5x12 y punta 4x3x6: más claros, con sensor cian en la punta
    box(img, glow, 0, 57, 8, 5, 12, lambda f, x, y, w, h: (CYAN[0], True) if f in ("left", "right") and y == 1 and x % 4 == 1
        else panels(f, x, y, w, h, 6))
    box(img, glow, 96, 85, 4, 3, 6, lambda f, x, y, w, h: ((CYAN[2], True) if f == "front" and (x, y) in ((1, 1), (2, 1))
                                                           else GRAPH[3] if y == 0 else GRAPH[2]))
    # lomo 6x3x20: tomas de aire
    box(img, glow, 0, 77, 6, 3, 20, lambda f, x, y, w, h: GRAPH[0] if f == "top" and 1 <= x <= w - 2 and y % 4 == 1
        else panels(f, x, y, w, h, 5))
    # cañones 1x1x10: acero con boca negra
    box(img, glow, 88, 100, 1, 1, 10, lambda f, x, y, w, h: (8, 8, 10) if f == "front" else STEEL[3] if (x + y) % 3 else STEEL[2])
    # motores 6x6x16: acero con anillos de cobre y peligro
    box(img, glow, 52, 77, 6, 6, 16, lambda f, x, y, w, h: COPPER[1] if f in ("left", "right", "top", "bottom") and (x if f in ("left", "right") else y) % 5 == 0
        else HAZARD[(x // 2) % 2] if f in ("left", "right") and y == h - 2 and x > w - 5
        else STEEL[2] if y < h // 2 else STEEL[1])
    # toberas 4x4x1 (brillan): núcleo cian
    box(img, glow, 96, 77, 4, 4, 1, lambda f, x, y, w, h: ((CYAN[2] if 1 <= x <= 2 and 1 <= y <= 2 else CYAN[1]), True)
        if f in ("front", "back") else STEEL[0])
    # estabilizadores 10x1x6
    box(img, glow, 88, 111, 10, 1, 6, lambda f, x, y, w, h: INER[2] if x == 0 or y == 0 else GRAPH[2])
    # alas 28x2x16: grafito, borde de ataque de inercita, luz cian en la punta, juntas
    def wing(face, x, y, w, h):
        if face in ("top", "bottom"):
            if x >= w - 2 and y <= 2:
                return CYAN[1], True                                         # luz de posición
            if y == 0:
                return INER[3]                                               # borde de ataque
            if x % 7 == 0:
                return GRAPH[0]
            if face == "top" and 4 <= y <= 5 and x > 6:
                return INER[1]                                               # franja
            return GRAPH[2] if face == "top" else GRAPH[1]
        return INER[2] if face == "front" else GRAPH[1]
    box(img, glow, 0, 100, 28, 2, 16, wing)
    # misiles bajo el ala 2x2x8: blancos con punta roja
    box(img, glow, 0, 118, 2, 2, 8, lambda f, x, y, w, h: (200, 50, 40) if f == "front"
        else (200, 50, 40) if (f in ("left", "right", "top", "bottom") and (x if f in ("left", "right") else y) == 0)
        else (210, 212, 216) if (x + y) % 5 else (170, 172, 176))
    # derivas 2x10x10: grafito con banda cian y punta de inercita
    box(img, glow, 88, 57, 2, 10, 10, lambda f, x, y, w, h: (CYAN[1], True) if f in ("left", "right") and y == 6
        else INER[3] if y == 0 else panels(f, x, y, w, h, 5))
    # cabina 8x5x14: cristal oscuro con reflejo y marco
    def glass(face, x, y, w, h):
        if face == "bottom":
            return GRAPH[0]
        if face == "top" and (x == y - 2 or x == y - 3):
            return GLASS[3], True                                            # reflejo
        if face in ("left", "right") and y == h - 1:
            return GRAPH[1]                                                  # marco
        if face == "top" and y in (6, 12):
            return GRAPH[1]                                                  # montantes
        if face in ("left", "right") and (x in (6, 12) or y == 0):
            return GRAPH[1]
        return GLASS[2] if y == 0 else GLASS[1] if (x + y) % 6 else GLASS[0]
    box(img, glow, 0, 128, 11, 14, 18, glass)
    # tren: pata y rueda
    box(img, glow, 122, 57, 1, 11, 1, lambda f, x, y, w, h: STEEL[2] if y % 4 else HAZARD[0])
    box(img, glow, 114, 57, 2, 2, 2, lambda f, x, y, w, h: STEEL[0])
    return img, glow


if __name__ == "__main__":
    img, glow = build()
    img.save(ROOT / "core_fighter.png")
    glow.save(ROOT / "core_fighter_glow.png")
    if len(sys.argv) > 1:
        prev = Image.new("RGBA", (256, 512), (48, 50, 56, 255))
        prev.alpha_composite(img.resize((256, 512), Image.NEAREST))
        prev.save(sys.argv[1])
    print("ok: caza del núcleo")
