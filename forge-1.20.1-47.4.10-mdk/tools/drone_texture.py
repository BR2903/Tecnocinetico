"""Texturas del Dron del Núcleo (modelo DroneModel, 64x64):
textures/entity/drone.png, drone_glow.png (sensor cian, patrulla) y drone_alert.png (sensor rojo, ataque).

Uso (desde forge-1.20.1-47.4.10-mdk):  python tools/drone_texture.py [preview.png]
Mismo estilo que la Torreta Centinela (acero pavonado, cobre, franjas de peligro): reutiliza turret_texture.py.
"""
import sys
from pathlib import Path
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from turret_texture import ROOT, STEEL, COPPER, HAZARD, CYAN, RED, box, panel  # noqa: E402

BLADE = [(18, 18, 20), (44, 46, 52)]


def build(sensor):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))

    # casco 8x3x8: paneles, franja de peligro en el canto delantero, luces de posición que brillan en los lados
    def hull(face, x, y, w, h):
        if face == "front" and y == 1:
            return HAZARD[(x // 2) % 2]
        if face in ("left", "right") and y == 1 and x in (1, w - 2):
            return sensor[1], True
        if face == "top" and (x, y) in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):
            return COPPER[2]
        if face == "bottom" and 2 <= x <= w - 3 and 2 <= y <= h - 3:
            return STEEL[0] if (x + y) % 2 else STEEL[1]                     # rejilla de ventilación
        return panel(face, x, y, w, h)
    box(img, glow, 0, 0, 8, 3, 8, hull)
    # cúpula 5x2x5: cristal oscuro con reflejo y un ojo central que brilla
    def dome(face, x, y, w, h):
        if face == "top":
            if (x, y) == (2, 2):
                return sensor[2], True
            if abs(x - 2) + abs(y - 2) == 1:
                return sensor[0], True
            return (30, 44, 52) if (x + y) % 3 else (60, 84, 96)
        return STEEL[2] if y == 0 else STEEL[1]
    box(img, glow, 0, 12, 5, 2, 5, dome)
    # sensor frontal 3x2x1
    box(img, glow, 24, 12, 3, 2, 1, lambda f, x, y, w, h: ((sensor[2] if (x, y) == (1, 0) else sensor[1]), True)
        if f == "front" else STEEL[0])
    # antena 1x4x1 con punta luminosa
    box(img, glow, 40, 16, 1, 4, 1, lambda f, x, y, w, h: (sensor[1], True) if y == 0 else STEEL[2])
    # brazos 1x1x8: acero con anillos de cobre
    box(img, glow, 34, 0, 1, 1, 8, lambda f, x, y, w, h: COPPER[1] if ((y if f in ("top", "bottom") else x) % 3 == 0)
        else STEEL[1])
    # motor 2x2x2 de cobre y aspas casi negras con punta clara
    box(img, glow, 34, 10, 2, 2, 2, lambda f, x, y, w, h: COPPER[2] if f == "top" else COPPER[0] if y else COPPER[1])
    box(img, glow, 44, 10, 8, 1, 1, lambda f, x, y, w, h: HAZARD[0] if f in ("top", "bottom", "front", "back")
        and x in (0, w - 1) else BLADE[(x + y) % 2])
    box(img, glow, 0, 20, 1, 1, 8, lambda f, x, y, w, h: HAZARD[0] if ((f in ("top", "bottom") and y in (0, h - 1))
        or (f in ("left", "right") and x in (0, w - 1))) else BLADE[(x + y) % 2])
    # soporte del cañón 3x2x3 y cañón 1x1x6 (boca negra)
    box(img, glow, 24, 16, 3, 2, 3, lambda f, x, y, w, h: panel(f, x, y, w, h) if w > 2 and h > 2 else STEEL[1])
    box(img, glow, 0, 30, 1, 1, 6, lambda f, x, y, w, h: (8, 8, 10) if f == "front"
        else COPPER[2] if ((y if f in ("top", "bottom") else x) == 1) else STEEL[0])
    return img, glow


if __name__ == "__main__":
    ROOT.mkdir(parents=True, exist_ok=True)
    img, glow = build(CYAN)
    img.save(ROOT / "drone.png")
    glow.save(ROOT / "drone_glow.png")
    _, alert = build(RED)
    alert.save(ROOT / "drone_alert.png")
    if len(sys.argv) > 1:
        prev = Image.new("RGBA", (512, 512), (48, 50, 56, 255))
        prev.alpha_composite(img.resize((512, 512), Image.NEAREST))
        prev.save(sys.argv[1])
    print("ok: dron")
