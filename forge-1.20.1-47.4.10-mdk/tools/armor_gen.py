"""Armadura de Chatarra (tier 1): texturas puestas (textures/models/armor/scrap_layer_1/2.png, 64x32, formato de
armadura humanoide vanilla) e iconos 16x16 de los objetos.

Uso (desde forge-1.20.1-47.4.10-mdk):  python tools/armor_gen.py [preview.png]
Estilo: chapas de chatarra de distintos metales remachadas sobre cuero, contorno oscuro, una célula de cinetrita
cian en el pecho y en el visor. Sin ruido: tonos planos con canto claro y sombra.
"""
import sys
from pathlib import Path
from PIL import Image

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tecnocinetico/textures"
RUST = [(84, 40, 22), (124, 62, 32), (160, 88, 46), (196, 124, 70)]
GALV = [(60, 64, 68), (92, 98, 102), (124, 130, 132), (160, 166, 166)]
PAINT = [(40, 62, 58), (56, 86, 80), (74, 108, 98), (104, 136, 124)]
LEATHER = [(46, 30, 20), (68, 44, 28), (90, 60, 38), (114, 78, 50)]
DARK = (20, 20, 22)
RIVET = (206, 196, 180)
CYAN = [(30, 130, 146), (70, 210, 226), (180, 248, 255)]
COPPER = [(110, 64, 36), (168, 106, 62), (214, 150, 98)]


def faces(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def plate(pal, x, y, w, h, rivets=True):
    """Chapa: canto superior claro, contorno inferior oscuro, remaches en las esquinas."""
    if rivets and (x, y) in ((1, 1), (w - 2, 1)) and w > 3 and h > 2:
        return RIVET
    if y == 0:
        return pal[3]
    if y == h - 1 or x == w - 1:
        return pal[0]
    return pal[2] if (x + y) % 5 else pal[1]


def paint(img, u, v, w, h, d, fn):
    for name, (x0, y0, fw, fh) in faces(u, v, w, h, d).items():
        for y in range(fh):
            for x in range(fw):
                c = fn(name, x, y, fw, fh)
                if c is not None:
                    img.putpixel((x0 + x, y0 + y), (*c, 255))


# ------------------------------------------------------------------ capa 1: casco, peto (con brazos), botas
def layer1():
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    def helmet(face, x, y, w, h):
        if face == "front":
            if y == 3 and 1 <= x <= 6:
                return CYAN[1] if x in (2, 5) else DARK                 # ranura de visión con brillo
            if y >= 5:                                                  # respirador con dos filtros
                if (x, y) in ((1, 6), (6, 6)):
                    return GALV[3]
                if x in (0, 1, 6, 7) and y >= 5:
                    return GALV[1] if y % 2 else GALV[2]
                return LEATHER[1] if y == 5 else GALV[0]
            return plate(RUST, x, y, w, 3)
        if face == "top":
            return RUST[2] if (x // 3 + y // 3) % 2 else GALV[2]        # chapas soldadas
        if face == "bottom":
            return None
        if y in (3, 4):
            return LEATHER[2] if face != "back" else LEATHER[1]         # correa
        return plate(GALV if face in ("left", "right") else RUST, x, y % 4, w, 4, rivets=y < 3)
    paint(img, 0, 0, 8, 8, 8, helmet)

    def chest(face, x, y, w, h):
        if face == "top":
            return GALV[2]
        if face == "bottom":
            return None
        if face == "front":
            if 3 <= x <= 4 and 4 <= y <= 6:                             # célula de cinetrita
                return CYAN[2] if (x, y) == (3, 5) else CYAN[1]
            if (x in (2, 5) and 3 <= y <= 7) or (y in (3, 7) and 2 <= x <= 5):
                return COPPER[1]                                        # marco de la célula
            if x == y - 2 or x == y - 3:
                return LEATHER[2]                                       # correa en diagonal
            if y >= 9:
                return plate(PAINT, x, y - 9, w, 3)
            return plate(RUST if x < 4 else GALV, x % 4, y, 4, 9)
        if face == "back":
            if x == y // 1.5 or w - 1 - x == y // 1.5:
                return LEATHER[2]                                       # correas en X
            return plate(RUST, x % 4, y % 6, 4, 6)
        return plate(GALV, x, y % 6, w, 6)
    paint(img, 16, 16, 8, 12, 4, chest)

    def arm(face, x, y, w, h):
        if face == "bottom":
            return None
        if y <= 3:                                                      # hombrera
            return plate(RUST, x, y, w, 4) if face != "top" else RUST[3]
        if y in (7, 10):
            return LEATHER[2]
        return LEATHER[1] if (x + y) % 4 else LEATHER[0]
    paint(img, 40, 16, 4, 12, 4, arm)

    def boots(face, x, y, w, h):
        if face == "top" or y < 8:
            return None                                                 # solo la parte baja de la pierna
        if face == "bottom":
            return DARK
        if y == 8:
            return GALV[3]
        if face == "front" and y >= 10:
            return GALV[2] if x % 2 else GALV[1]                        # puntera
        return LEATHER[1] if y == 9 else GALV[1]
    paint(img, 0, 16, 4, 12, 4, boots)
    return img


# ------------------------------------------------------------------ capa 2: pantalón
def layer2():
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    def waist(face, x, y, w, h):
        if face in ("top", "bottom") or y < 8:
            return None
        if y in (8, 9):                                                 # cinturón con hebilla
            return COPPER[2] if face == "front" and x in (3, 4) else LEATHER[3] if y == 8 else LEATHER[1]
        return LEATHER[1]
    paint(img, 16, 16, 8, 12, 4, waist)

    def legs(face, x, y, w, h):
        if face == "bottom":
            return None
        if face == "top":
            return LEATHER[1]
        if 4 <= y <= 6 and face in ("front", "right", "left"):          # rodillera remachada
            return plate(RUST, x, y - 4, w, 3)
        if y == 9:
            return LEATHER[3]
        return LEATHER[1] if (x + y) % 5 else LEATHER[0]
    paint(img, 0, 16, 4, 12, 4, legs)
    return img


# ------------------------------------------------------------------ iconos 16x16
ICONS = {
    "scrap_helmet": ["................", "................", "....######......", "...#RRRRRR#.....",
                     "..#RRrRRrRR#....", "..#GGGGGGGG#....", "..#DcDDDDcD#....", "..#GG####GG#....",
                     "..#LMMMMMML#....", "...#M#..#M#.....", "...###..###.....", "................",
                     "................", "................", "................", "................"],
    "scrap_chestplate": ["................", "..###....###....", ".#RRR#..#GGG#...", ".#RRRR##GGGG#...",
                         ".#RRRRLLGGGG#...", "..#RRRCCLGG#....", "..#RRRCCGLG#....", "..#RRRRGGGL#....",
                         "..#PPPPPPPP#....", "..#PPPPPPPP#....", "..##########....", "................",
                         "................", "................", "................", "................"],
    "scrap_leggings": ["................", "..##########....", "..#LLLKKLLL#....", "..#LLL##LLL#....",
                       "..#LLL##LLL#....", "..#RRR##RRR#....", "..#RrR##RrR#....", "..#LLL##LLL#....",
                       "..#LLL##LLL#....", "..#LLL##LLL#....", "..####..####....", "................",
                       "................", "................", "................", "................"],
    "scrap_boots": ["................", "................", "................", "................",
                    "................", "..###....###....", "..#L#....#L#....", "..#L#....#L#....",
                    "..#GG#...#GG#...", ".#GGGG#.#GGGG#..", ".######.######..", "................",
                    "................", "................", "................", "................"],
}
ICON_COL = {"#": DARK, "R": RUST[2], "r": RIVET, "G": GALV[2], "D": DARK, "c": CYAN[1], "L": LEATHER[2],
            "M": GALV[1], "C": CYAN[1], "P": PAINT[2], "K": COPPER[2]}


def icon(rows):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in ICON_COL:
                img.putpixel((x + 1, y + 2), (*ICON_COL[ch], 255))
    return img


if __name__ == "__main__":
    (ASSETS / "models/armor").mkdir(parents=True, exist_ok=True)
    l1, l2 = layer1(), layer2()
    l1.save(ASSETS / "models/armor/scrap_layer_1.png")
    l2.save(ASSETS / "models/armor/scrap_layer_2.png")
    for name, rows in ICONS.items():
        icon(rows).save(ASSETS / f"item/{name}.png")
    if len(sys.argv) > 1:
        sheet = Image.new("RGBA", (64 * 8, 64 * 8 + 64), (48, 50, 56, 255))
        sheet.alpha_composite(l1.resize((512, 256), Image.NEAREST), (0, 0))
        sheet.alpha_composite(l2.resize((512, 256), Image.NEAREST), (0, 256))
        for i, name in enumerate(ICONS):
            sheet.alpha_composite(icon(ICONS[name]).resize((64, 64), Image.NEAREST), (i * 70, 512))
        sheet.save(sys.argv[1])
    print("ok: armadura de chatarra")
