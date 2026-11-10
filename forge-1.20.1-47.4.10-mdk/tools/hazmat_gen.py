"""Traje antirradiación: capas de armadura (64x32, cubre todo el cuerpo) e iconos.

python tools/hazmat_gen.py      (desde forge-1.20.1-47.4.10-mdk)
Disposición vanilla de las capas: cabeza (0,0)-(32,16), cuerpo (16,16)-(40,32), brazos (40,16)-(56,32),
piernas/botas (0,16)-(16,32). Capa 2 (pantalones): cuerpo y piernas.
"""
import random
import sys
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
from armor_t3_gen import recolor  # noqa: E402

A = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tecnocinetico/textures"
YELLOW = (226, 190, 40)
RUBBER = (34, 34, 38)
STRIPE = (190, 196, 200)


def suit(img, box, r):
    """Tela amarilla gruesa con pliegues verticales y algo de suciedad."""
    x0, y0, x1, y1 = box
    for y in range(y0, y1):
        for x in range(x0, x1):
            k = 1.0 + r.uniform(-0.015, 0.015) - (0.1 if (x * 7 + y * 3) % 11 == 0 else 0)
            if r.random() < 0.0:
                k -= 0.2                                                      # manchas
            img.putpixel((x, y), tuple(max(0, min(255, int(v * k))) for v in YELLOW) + (255,))


def layer1():
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r = random.Random(3)
    for box in ((0, 0, 32, 16), (16, 16, 40, 32), (40, 16, 56, 32), (0, 16, 16, 32)):
        suit(img, box, r)
    # cabeza: visor grande con marco, filtro de respiración y costura
    d.rectangle([8, 8, 15, 15], outline=(120, 100, 20))
    d.rectangle([9, 9, 14, 12], fill=(16, 34, 40), outline=(60, 62, 68))
    d.line([(10, 9), (12, 9)], fill=(160, 215, 225))
    d.rectangle([10, 13, 13, 15], fill=(56, 58, 62)); d.point((11, 14), fill=(20, 20, 22)); d.point((12, 14), fill=(20, 20, 22))
    for (x, y) in ((1, 13), (6, 13), (17, 13), (22, 13)):                     # filtros laterales
        d.rectangle([x, y - 1, x + 1, y], fill=(56, 58, 62))
    # cuerpo: cierre central, bandas reflectantes y dosímetro
    d.line([(24, 20), (24, 31)], fill=(120, 100, 20))
    for y in (26, 27):
        d.line([(16, y), (39, y)], fill=STRIPE)
    d.rectangle([21, 21, 22, 23], fill=(40, 44, 50)); d.point((21, 21), fill=(90, 230, 120))
    # brazos: banda reflectante y guantes de goma
    d.line([(40, 24), (55, 24)], fill=STRIPE)
    d.rectangle([40, 29, 55, 31], fill=RUBBER); d.rectangle([48, 16, 51, 19], fill=RUBBER)
    # botas: suela y puntera de goma
    d.rectangle([0, 27, 15, 31], fill=RUBBER); d.rectangle([8, 16, 11, 19], fill=RUBBER)
    return img


def layer2():
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r = random.Random(4)
    for box in ((16, 16, 40, 32), (0, 16, 16, 32)):
        suit(img, box, r)
    d.rectangle([16, 20, 39, 21], fill=(150, 124, 28))                         # cintura
    for y in (25, 26):
        d.line([(0, y), (15, y)], fill=STRIPE)                                 # bandas en las piernas
    return img


def main():
    layer1().save(A / "models/armor/hazmat_layer_1.png")
    layer2().save(A / "models/armor/hazmat_layer_2.png")
    ramp = [(40, 32, 6), (120, 96, 16), (200, 166, 30), (236, 204, 60), (255, 240, 150)]
    for p in ("helmet", "chestplate", "leggings", "boots"):
        img = recolor(A / f"item/tactical_{p}.png", ramp)
        d = ImageDraw.Draw(img)
        w, h = img.size
        if p == "helmet":
            d.rectangle([w * 5 // 16, h * 7 // 16, w * 11 // 16, h * 10 // 16], fill=(20, 40, 46, 255))
        elif p == "chestplate":
            d.line([(w * 3 // 16, h * 10 // 16), (w * 13 // 16, h * 10 // 16)], fill=STRIPE + (255,))
        img.save(A / f"item/hazmat_{p}.png")
    print("ok: traje antirradiación")


if __name__ == "__main__":
    main()
