"""Armaduras tier 2: texturas puestas (textures/models/armor/{inercita,tactical,exo}_layer_1/2.png, 64x32,
formato humanoide vanilla) e iconos 16x16; también los iconos 32x32 de las piezas de prensa (placa de inercita,
mecanismo de precisión, servo cinético, tela balística).

Uso (desde forge-1.20.1-47.4.10-mdk):  python tools/armor_t2_gen.py [preview.png]
- Inercita: placas pesadas azul-gris superpuestas sobre tela oscura, yelmo cerrado con ranura en T cian.
- Táctica: tela color coyote con correaje negro, bolsillos, coderas/rodilleras y casco con gafas cian.
- Exoesqueleto: armazón grafito con servos de cobre en las articulaciones y líneas de energía cian.
"""
import sys
from pathlib import Path
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from armor_gen import ASSETS, DARK, RIVET, CYAN, COPPER, LEATHER, paint, plate, ICONS  # noqa: E402

INER = [(40, 46, 60), (64, 72, 90), (88, 98, 120), (118, 130, 154)]
CLOTH = [(26, 28, 30), (38, 40, 44), (52, 54, 58), (66, 68, 72)]
COYOTE = [(78, 68, 46), (104, 92, 64), (128, 116, 84), (152, 140, 106)]
WEB = [(18, 18, 20), (32, 32, 34), (46, 46, 48)]
FRAME = [(26, 28, 32), (42, 44, 50), (62, 64, 72), (88, 90, 100)]


def lit(pal, y, h):
    return pal[3] if y == 0 else pal[0] if y == h - 1 else pal[2] if y < h // 2 else pal[1]


# =================================================================== Placas de inercita
def inercita():
    l1 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    def helmet(face, x, y, w, h):
        if face == "bottom":
            return None
        if face == "front":
            if y == 3 and 1 <= x <= 6:
                return CYAN[1] if 2 <= x <= 5 else DARK                    # ranura horizontal de la T
            if x in (3, 4) and 4 <= y <= 6:
                return CYAN[0] if y == 4 else DARK                        # ranura vertical
            if y == 7:
                return INER[0]
            return INER[3] if y == 0 else INER[2] if (x in (0, 7) or y < 3) else INER[1]
        if face == "top":
            return INER[3] if x in (3, 4) else INER[2]                     # cresta
        return plate(INER, x, y % 4, w, 4, rivets=y < 4)
    paint(l1, 0, 0, 8, 8, 8, helmet)

    def chest(face, x, y, w, h):
        if face in ("top", "bottom"):
            return INER[2] if face == "top" else None
        if face == "front":
            if (x, y) in ((3, 4), (4, 4)):
                return CYAN[2]
            if 2 <= x <= 5 and 3 <= y <= 5:
                return INER[3] if y == 3 else INER[2]                      # placa pectoral
            if y in (6, 8, 10):
                return INER[0]                                             # juntas de las láminas
            return lit(INER, y % 2, 2) if y > 6 else INER[1] if x in (0, 7) else INER[2]
        if face == "back":
            if x in (3, 4) and y < 8:
                return CLOTH[2]
            return plate(INER, x % 4, y % 6, 4, 6)
        return CLOTH[1] if 4 <= y <= 8 else INER[1]
    paint(l1, 16, 16, 8, 12, 4, chest)

    def arm(face, x, y, w, h):
        if face == "bottom":
            return None
        if y <= 4:                                                         # hombrera grande
            return INER[3] if face == "top" or y == 0 else INER[2] if y < 4 else INER[0]
        if 7 <= y <= 9:
            return plate(INER, x, y - 7, w, 3, rivets=False)              # brazal
        return CLOTH[1] if (x + y) % 4 else CLOTH[0]
    paint(l1, 40, 16, 4, 12, 4, arm)

    def boots(face, x, y, w, h):
        if face == "top" or y < 7:
            return None
        if face == "bottom":
            return DARK
        if y == 7:
            return INER[3]
        if face == "front" and y >= 9:
            return INER[2] if y < 11 else INER[0]                          # escarpe
        return INER[1]
    paint(l1, 0, 16, 4, 12, 4, boots)

    l2 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    def waist(face, x, y, w, h):
        if face in ("top", "bottom") or y < 8:
            return None
        if y == 8:
            return INER[3]
        return INER[1] if face in ("front", "back") and y >= 10 else CLOTH[1]    # faldar
    paint(l2, 16, 16, 8, 12, 4, waist)

    def legs(face, x, y, w, h):
        if face == "bottom":
            return None
        if face == "top":
            return CLOTH[1]
        if face in ("front", "right", "left") and 1 <= y <= 4:
            return plate(INER, x, y - 1, w, 4, rivets=False)              # quijote
        if face == "front" and 5 <= y <= 6:
            return INER[3] if y == 5 else INER[2]                          # rodillera
        return CLOTH[1] if (x + y) % 5 else CLOTH[0]
    paint(l2, 0, 16, 4, 12, 4, legs)
    return l1, l2


# =================================================================== Táctica
def tactical():
    l1 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    def helmet(face, x, y, w, h):
        if face == "bottom":
            return None
        if face == "front":
            if y == 2 and 1 <= x <= 6:                                     # gafas cian sobre el casco
                return CYAN[2] if x in (2, 5) else CYAN[1] if x in (1, 6) else WEB[1]
            if y in (1, 3) and 1 <= x <= 6:
                return WEB[1]
            if y >= 4:
                return None                                                # cara descubierta
            return COYOTE[3] if y == 0 else COYOTE[2]
        if face == "top":
            return COYOTE[2] if (x + y) % 5 else COYOTE[1]
        if y >= 5:
            return WEB[0] if y == 5 and face != "back" else None           # barboquejo
        if face == "back" and y in (2, 3) and 2 <= x <= 5:
            return WEB[2] if y == 2 else WEB[1]                            # contrapeso / batería
        return COYOTE[2] if y < 4 else COYOTE[1]
    paint(l1, 0, 0, 8, 8, 8, helmet)

    def chest(face, x, y, w, h):
        if face == "bottom":
            return None
        if face == "top":
            return WEB[1]
        if face == "front":
            if y in (6, 7, 8) and x in (1, 2, 5, 6):                       # portacargadores
                return COYOTE[3] if y == 6 else COYOTE[1]
            if y == 6 and x in (3, 4):
                return CYAN[1]                                             # luz de identificación
            if x in (1, 6) and y < 6:
                return WEB[1]                                              # tirantes
            if y in (9, 10):
                return WEB[2] if y == 9 else WEB[1]                        # cinturón
            return lit(COYOTE, y, 6) if y < 6 else COYOTE[1]
        if face == "back":
            if 2 <= x <= 5 and 1 <= y <= 7:
                return COYOTE[2] if (x in (2, 5) or y in (1, 7)) else COYOTE[1]   # mochila de hidratación
            return WEB[1] if y in (9, 10) else COYOTE[1]
        return COYOTE[1] if y < 9 else WEB[1]
    paint(l1, 16, 16, 8, 12, 4, chest)

    def arm(face, x, y, w, h):
        if face == "bottom":
            return None
        if y <= 2:
            return COYOTE[2] if face != "top" else COYOTE[3]
        if face in ("front", "right", "left") and 6 <= y <= 7:
            return WEB[2] if y == 6 else WEB[1]                            # codera
        if y >= 10:
            return WEB[1]                                                  # guantes
        return COYOTE[1] if (x + y) % 6 else COYOTE[0]
    paint(l1, 40, 16, 4, 12, 4, arm)

    def boots(face, x, y, w, h):
        if face == "top" or y < 8:
            return None
        if face == "bottom":
            return WEB[0]
        if y == 8:
            return COYOTE[2]
        return WEB[1] if y < 11 else WEB[0]
    paint(l1, 0, 16, 4, 12, 4, boots)

    l2 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    def waist(face, x, y, w, h):
        if face in ("top", "bottom") or y < 9:
            return None
        return COYOTE[1]
    paint(l2, 16, 16, 8, 12, 4, waist)

    def legs(face, x, y, w, h):
        if face == "bottom":
            return None
        if face == "top":
            return COYOTE[1]
        if face in ("front", "right", "left") and 5 <= y <= 6:
            return WEB[2] if y == 5 else WEB[1]                            # rodillera
        if face in ("right", "left") and 1 <= y <= 3:
            return COYOTE[2] if y == 1 else COYOTE[0]                      # bolsillo de muslo
        return COYOTE[1] if (x * 3 + y) % 7 else COYOTE[0]
    paint(l2, 0, 16, 4, 12, 4, legs)
    return l1, l2


# =================================================================== Exoesqueleto
def exo():
    l1 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    def helmet(face, x, y, w, h):
        if face == "bottom":
            return None
        if face == "front":
            if y == 3 and x in (1, 2, 5, 6):
                return CYAN[2] if x in (2, 5) else CYAN[0]                 # lentes
            if y in (2, 4) and x in (1, 2, 5, 6):
                return COPPER[1]                                           # aros de las lentes
            if y >= 5 and 2 <= x <= 5:
                return FRAME[1] if (x + y) % 2 else FRAME[0]               # rejilla de respiración
            if 3 <= y <= 6 and 2 < x < 5:
                return FRAME[0]
            if y >= 5:
                return None
            return FRAME[3] if y == 0 else FRAME[2]
        if face == "top":
            return CYAN[1] if x in (3, 4) and 1 <= y <= 6 else FRAME[2]    # línea de energía
        if y == 4 and face in ("left", "right"):
            return COPPER[2] if x in (3, 4) else FRAME[1]                  # servo del cuello
        return FRAME[2] if y < 4 else FRAME[1] if y < 7 else None
    paint(l1, 0, 0, 8, 8, 8, helmet)

    def chest(face, x, y, w, h):
        if face == "bottom":
            return None
        if face == "top":
            return FRAME[2]
        if face == "front":
            if 3 <= x <= 4 and 3 <= y <= 4:
                return CYAN[2] if (x, y) == (3, 3) else CYAN[1]           # núcleo
            if (x in (2, 5) and 2 <= y <= 5) or (y in (2, 5) and 2 <= x <= 5):
                return COPPER[1]
            if x in (3, 4) and y > 5:
                return FRAME[0] if y % 2 else FRAME[1]                     # abdominales del armazón
            if y in (1, 8) or x in (0, 7):
                return FRAME[2]
            return None if 7 < y < 11 and 1 <= x <= 6 and x not in (3, 4) else FRAME[1]
        if face == "back":
            if x in (3, 4):
                return CYAN[1] if y % 3 == 1 else FRAME[0]                 # columna con energía
            if 1 <= y <= 5 and x in (1, 2, 5, 6):
                return FRAME[2] if y == 1 else FRAME[1]                    # batería dorsal
            return FRAME[1] if y in (0, 6, 11) else None
        return FRAME[1] if y in (0, 5, 11) else None
    paint(l1, 16, 16, 8, 12, 4, chest)

    def arm(face, x, y, w, h):
        if face == "bottom":
            return None
        if y <= 2:
            return COPPER[2] if (face == "top" or y == 1) else COPPER[1]   # servo del hombro
        if y in (6, 7):
            return COPPER[1] if face != "back" else COPPER[0]              # servo del codo
        if x in (0, 3) or face == "front" and x in (1, 2):
            return FRAME[2] if face == "front" else FRAME[1]               # barras del armazón
        return None
    paint(l1, 40, 16, 4, 12, 4, arm)

    def boots(face, x, y, w, h):
        if face == "top" or y < 8:
            return None
        if face == "bottom":
            return FRAME[0]
        if y == 8:
            return COPPER[1]
        return FRAME[2] if y == 9 else FRAME[1]
    paint(l1, 0, 16, 4, 12, 4, boots)

    l2 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    def waist(face, x, y, w, h):
        if face in ("top", "bottom") or y < 9:
            return None
        if face in ("left", "right") and y == 10:
            return COPPER[2]                                               # servo de la cadera
        return FRAME[2] if y == 9 else FRAME[1]
    paint(l2, 16, 16, 8, 12, 4, waist)

    def legs(face, x, y, w, h):
        if face == "bottom":
            return None
        if face == "top":
            return FRAME[1]
        if y in (5, 6):
            return COPPER[2] if y == 5 else COPPER[1]                      # servo de la rodilla
        if face in ("front", "back") and x in (1, 2):
            return CYAN[1] if face == "front" and x == 1 and y % 4 == 2 else FRAME[2]
        if x in (0, 3) and face in ("left", "right"):
            return FRAME[1]
        return None
    paint(l2, 0, 16, 4, 12, 4, legs)
    return l1, l2


# =================================================================== iconos 16x16 (siluetas de la chatarra, colores propios)
ICON_PALETTES = {
    "inercita": {"#": DARK, "R": INER[2], "r": INER[3], "G": INER[1], "D": DARK, "c": CYAN[1], "L": CLOTH[2],
                 "M": INER[1], "C": CYAN[2], "P": INER[2], "K": INER[3]},
    "tactical": {"#": WEB[0], "R": COYOTE[2], "r": COYOTE[3], "G": COYOTE[1], "D": WEB[1], "c": CYAN[1], "L": WEB[2],
                 "M": WEB[1], "C": CYAN[1], "P": COYOTE[1], "K": WEB[2]},
    "exo": {"#": DARK, "R": FRAME[2], "r": COPPER[2], "G": FRAME[1], "D": FRAME[0], "c": CYAN[2], "L": COPPER[1],
            "M": FRAME[2], "C": CYAN[2], "P": FRAME[1], "K": COPPER[2]},
}


def icon(rows, pal):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x + 1, y + 2), (*pal[ch], 255))
    return img


# =================================================================== piezas de prensa (32x32)
def part_icons():
    out = {}

    def canvas():
        return Image.new("RGBA", (32, 32), (0, 0, 0, 0))

    # placa de inercita: placa biselada con remaches y reflejo
    img = canvas()
    for y in range(7, 25):
        for x in range(5, 27):
            edge = x in (5, 26) or y in (7, 24)
            c = DARK if edge else INER[3] if y == 8 or x == 6 else INER[0] if y == 23 or x == 25 else INER[2] if (x + y) % 9 else INER[1]
            img.putpixel((x, y), (*c, 255))
    for (x, y) in ((8, 10), (23, 10), (8, 21), (23, 21)):
        img.putpixel((x, y), (*RIVET, 255))
    for i in range(10, 22):
        img.putpixel((i, 30 - i), (*INER[3], 255))                            # reflejo diagonal
    out["inercita_plate"] = img

    # mecanismo de precisión: engranaje de latón con muelle y eje de acero
    img = canvas()
    import math
    for y in range(32):
        for x in range(32):
            dx, dy = x - 15.5, y - 15.5
            r = math.hypot(dx, dy)
            a = math.atan2(dy, dx)
            tooth = r < 12.5 and (math.cos(a * 10) > 0.2) and r > 10
            if r < 10 or tooth:
                if r < 3:
                    c = (60, 64, 70) if r > 1.5 else (150, 154, 160)
                elif 5 < r < 6.2:
                    c = (110, 80, 40)
                else:
                    c = (236, 204, 120) if dy < -3 else (196, 160, 80) if dy < 4 else (150, 116, 54)
                if r > 9.4 and not tooth:
                    c = (110, 80, 40)
                img.putpixel((x, y), (*c, 255))
    for i in range(6):                                                         # muelle cian
        img.putpixel((24 + (i % 2), 22 + i), (*CYAN[1], 255))
    out["precision_mechanism"] = img

    # servo cinético: cuerpo de grafito con brida de cobre y núcleo cian
    img = canvas()
    for y in range(6, 27):
        for x in range(7, 25):
            edge = x in (7, 24) or y in (6, 26)
            c = DARK if edge else FRAME[3] if y == 7 else FRAME[2] if x < 16 else FRAME[1]
            if 11 <= y <= 13:
                c = COPPER[2] if y == 11 else COPPER[1]
            if 19 <= y <= 22 and 13 <= x <= 18:
                c = CYAN[2] if (x, y) in ((15, 20), (16, 20)) else CYAN[1]
            img.putpixel((x, y), (*c, 255))
    for x in range(13, 19):                                                    # eje
        for y in range(2, 6):
            img.putpixel((x, y), (*(COPPER[0] if x in (13, 18) else (160, 166, 166)), 255))
    out["kinetic_servo"] = img

    # tela balística: rollo de tejido gris oscuro con trama y hebras cian
    img = canvas()
    for y in range(8, 25):
        for x in range(4, 28):
            if x in (4, 27) or y in (8, 24):
                c = DARK
            else:
                c = CLOTH[3] if (x + y) % 4 == 0 else CLOTH[2] if (x - y) % 4 == 0 else CLOTH[1]
                if y % 6 == 2:
                    c = CYAN[0]
            img.putpixel((x, y), (*c, 255))
    for y in range(8, 25):                                                     # canto del rollo
        for x in range(24, 28):
            img.putpixel((x, y), (*(CLOTH[0] if (y + x) % 2 else CLOTH[1]), 255))
    out["ballistic_fabric"] = img
    return out


def preview(layers, icons, path):
    """Frente y espalda de cada set sobre un maniquí gris (como se vería puesta), más los iconos."""
    from armor_gen import faces
    sc = 8
    tiles = []
    for i in range(0, len(layers), 2):
        l1, l2 = layers[i], layers[i + 1]
        for side in ("front", "back"):
            t = Image.new("RGBA", (16, 32), (0, 0, 0, 0))
            body = Image.new("RGBA", (16, 32), (0, 0, 0, 0))
            for (x0, y0, w, h) in ((4, 0, 8, 8), (4, 8, 8, 12), (0, 8, 4, 12), (12, 8, 4, 12), (4, 20, 4, 12), (8, 20, 4, 12)):
                for y in range(h):
                    for x in range(w):
                        body.putpixel((x0 + x, y0 + y), (120, 120, 124, 255))
            t.alpha_composite(body)

            def put(img, part, pos):
                x0, y0, w, h = faces(*part)[side]
                t.alpha_composite(img.crop((x0, y0, x0 + w, y0 + h)), pos)
            put(l2, (0, 16, 4, 12, 4), (4, 20))
            put(l2, (0, 16, 4, 12, 4), (8, 20))
            put(l2, (16, 16, 8, 12, 4), (4, 8))
            put(l1, (0, 16, 4, 12, 4), (4, 20))
            put(l1, (0, 16, 4, 12, 4), (8, 20))
            put(l1, (16, 16, 8, 12, 4), (4, 8))
            put(l1, (40, 16, 4, 12, 4), (0, 8))
            put(l1, (40, 16, 4, 12, 4), (12, 8))
            put(l1, (0, 0, 8, 8, 8), (4, 0))
            tiles.append(t.resize((16 * sc, 32 * sc), Image.NEAREST))
    W = len(tiles) * (16 * sc + 10)
    sheet = Image.new("RGBA", (max(W, len(icons) * 52), 32 * sc + 60), (48, 50, 56, 255))
    for i, t in enumerate(tiles):
        sheet.alpha_composite(t, (i * (16 * sc + 10), 0))
    for i, img in enumerate(icons):
        sheet.alpha_composite(img.resize((48, 48), Image.NEAREST), (i * 52, 32 * sc + 8))
    sheet.save(path)


SETS = {"inercita": inercita, "tactical": tactical, "exo": exo}

if __name__ == "__main__":
    (ASSETS / "models/armor").mkdir(parents=True, exist_ok=True)
    layers, icons = [], []
    for sid, fn in SETS.items():
        l1, l2 = fn()
        l1.save(ASSETS / f"models/armor/{sid}_layer_1.png")
        l2.save(ASSETS / f"models/armor/{sid}_layer_2.png")
        layers += [l1, l2]
        for piece in ("helmet", "chestplate", "leggings", "boots"):
            img = icon(ICONS[f"scrap_{piece}"], ICON_PALETTES[sid])
            img.save(ASSETS / f"item/{sid}_{piece}.png")
            icons.append(img)
    parts = part_icons()
    for name, img in parts.items():
        img.save(ASSETS / f"item/{name}.png")
    if len(sys.argv) > 1:
        preview(layers, icons + list(parts.values()), sys.argv[1])
    print("ok: armaduras tier 2")
