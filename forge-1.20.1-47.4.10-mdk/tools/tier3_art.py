"""Arte del tier 3 que no es un modelo de arma:
- textures/block/weapon_assembler_front.png (128x128): frente del Ensamblador de Armas (marco de aleación, ventana
  con brazos robóticos y un fusil a medio montar, franjas de peligro abajo).
- textures/item/core_processor.png (32x32): Procesador del Núcleo (chip con patas de cobre y núcleo cian).

Uso (desde forge-1.20.1-47.4.10-mdk):  python tools/tier3_art.py [preview.png]
"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tecnocinetico/textures"
STEEL = [(22, 23, 27), (36, 38, 44), (56, 60, 68), (86, 92, 104), (120, 126, 138)]
CYAN = [(30, 130, 146), (70, 210, 226), (180, 248, 255)]
COPPER = [(110, 64, 36), (168, 106, 62), (214, 150, 98)]
HAZ = [(214, 168, 40), (30, 30, 32)]


def assembler_front():
    img = Image.new("RGBA", (128, 128), (*STEEL[2], 255))
    d = ImageDraw.Draw(img)
    # marco con bisel y remaches
    d.rectangle([0, 0, 127, 127], outline=STEEL[0], width=4)
    d.line([(4, 4), (123, 4)], fill=STEEL[4], width=2)
    d.line([(4, 4), (4, 123)], fill=STEEL[3], width=2)
    for x, y in ((10, 10), (117, 10), (10, 117), (117, 117)):
        d.ellipse([x - 3, y - 3, x + 3, y + 3], fill=COPPER[1], outline=COPPER[0])
    # ventana oscura
    d.rectangle([16, 16, 111, 88], fill=(14, 16, 20), outline=STEEL[0], width=3)
    d.line([(19, 19), (108, 19)], fill=(40, 46, 56))
    # brazos robóticos desde arriba
    for ax in (38, 88):
        d.rectangle([ax - 3, 19, ax + 3, 40], fill=STEEL[3])
        d.rectangle([ax - 6, 40, ax + 6, 46], fill=COPPER[1])
        d.line([(ax, 46), (ax + (8 if ax < 64 else -8), 58)], fill=STEEL[4], width=3)
        d.rectangle([ax + (6 if ax < 64 else -10), 56, ax + (10 if ax < 64 else -6), 62], fill=CYAN[1])
    # fusil a medio montar (silueta) con luz cian
    d.rectangle([28, 66, 100, 72], fill=STEEL[1])                       # cañón y cajón
    d.rectangle([60, 64, 84, 76], fill=STEEL[3])
    d.polygon([(84, 72), (100, 72), (104, 84), (94, 84)], fill=STEEL[1])  # culata
    d.polygon([(66, 76), (72, 76), (70, 84), (64, 84)], fill=STEEL[3])   # empuñadura
    d.line([(30, 69), (58, 69)], fill=CYAN[1], width=1)
    # chispa de soldadura
    for (x, y, c) in ((49, 62, CYAN[2]), (47, 60, CYAN[1]), (51, 60, CYAN[1]), (50, 64, CYAN[0])):
        d.point((x, y), fill=c)
    # panel de control: luces y rejilla
    d.rectangle([16, 94, 111, 108], fill=STEEL[1], outline=STEEL[0])
    for i, c in enumerate((CYAN[1], CYAN[1], (200, 60, 40), CYAN[0])):
        d.rectangle([22 + i * 10, 98, 27 + i * 10, 103], fill=c)
    for x in range(66, 108, 4):
        d.line([(x, 97), (x, 105)], fill=STEEL[0])
    # franjas de peligro
    for x in range(4, 124):
        for y in range(112, 124):
            img.putpixel((x, y), (*HAZ[((x + y) // 8) % 2], 255))
    return img


def core_processor():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for i in range(6):                                                    # patas de cobre
        o = 8 + i * 3
        d.line([(o, 3), (o, 6)], fill=COPPER[2]); d.line([(o, 25), (o, 28)], fill=COPPER[1])
        d.line([(3, o), (6, o)], fill=COPPER[2]); d.line([(25, o), (28, o)], fill=COPPER[1])
    d.rectangle([6, 6, 25, 25], fill=STEEL[1], outline=STEEL[0])        # encapsulado
    d.line([(7, 7), (24, 7)], fill=STEEL[3]); d.line([(7, 7), (7, 24)], fill=STEEL[3])
    d.rectangle([11, 11, 20, 20], fill=(20, 24, 30), outline=CYAN[0])   # núcleo
    d.rectangle([13, 13, 18, 18], fill=CYAN[1])
    d.rectangle([14, 14, 15, 15], fill=CYAN[2])
    for (x, y) in ((9, 9), (22, 9), (9, 22), (22, 22)):                   # marcas de esquina
        d.point((x, y), fill=CYAN[1])
    d.line([(16, 8), (16, 11)], fill=CYAN[0]); d.line([(16, 20), (16, 23)], fill=CYAN[0])
    d.line([(8, 16), (11, 16)], fill=CYAN[0]); d.line([(20, 16), (23, 16)], fill=CYAN[0])
    return img


if __name__ == "__main__":
    a = assembler_front()
    a.save(ROOT / "block/weapon_assembler_front.png")
    c = core_processor()
    c.save(ROOT / "item/core_processor.png")
    if len(sys.argv) > 1:
        sheet = Image.new("RGBA", (128 + 8 + 128, 128), (48, 50, 56, 255))
        sheet.alpha_composite(a, (0, 0))
        sheet.alpha_composite(c.resize((128, 128), Image.NEAREST), (136, 0))
        sheet.save(sys.argv[1])
    print("ok: arte tier 3")
