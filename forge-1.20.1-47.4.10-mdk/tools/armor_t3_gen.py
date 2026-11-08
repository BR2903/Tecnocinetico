"""Armaduras tier 3 (una por dimensión) y materiales del Nether/End:
- textures/models/armor/{core,piroxite,nullite}_layer_1/2.png + iconos 16x16 de las piezas;
- item/raw_piroxite, piroxite_ingot, raw_nullite, nullite_ingot: recoloreados de los de inercita (mismo estilo
  pintado a 128 px, cambiando la rampa de color por luminancia);
- block/piroxite_ore_overlay, nullite_ore_overlay (16x16): cristales sobre la piedra vanilla (el modelo de la mena
  pinta netherrack / end stone debajo y esta capa encima).

Uso (desde forge-1.20.1-47.4.10-mdk):  python tools/armor_t3_gen.py [preview.png]
- Núcleo (Overworld): placas grafito con circuitos cian de la IA, visor de franja, emblema del procesador.
- Piroxita (Nether): placas de basalto con aletas disipadoras y grietas al rojo vivo.
- Nulita (End): placas lisas blanco-lavanda, sin remaches (sin fricción), juntas violeta.
"""
import random
import sys
from pathlib import Path
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from armor_gen import ASSETS, DARK, CYAN, paint, plate, ICONS  # noqa: E402
from armor_t2_gen import icon, preview  # noqa: E402

GRAPH = [(24, 26, 30), (38, 40, 46), (56, 58, 66), (80, 84, 94)]
BASALT = [(30, 26, 26), (48, 42, 42), (66, 58, 56), (88, 78, 74)]
HEAT = [(150, 40, 16), (230, 96, 30), (255, 190, 80)]
NULL = [(150, 140, 176), (190, 182, 214), (222, 216, 240), (246, 244, 255)]
VIOLET = [(70, 40, 120), (120, 70, 190), (180, 140, 240)]


def lit(pal, y, h):
    return pal[3] if y == 0 else pal[0] if y == h - 1 else pal[2] if y < h // 2 else pal[1]


# =================================================================== Núcleo (Overworld)
def core():
    l1 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    def helmet(face, x, y, w, h):
        if face == "bottom":
            return None
        if face == "front":
            if y == 3 and 1 <= x <= 6:
                return CYAN[2] if x in (3, 4) else CYAN[1]                 # visor de franja
            if y in (2, 4) and 1 <= x <= 6:
                return GRAPH[0]
            if y >= 5:
                return GRAPH[1] if x in (0, 7) or y == 7 else (GRAPH[2] if (x + y) % 2 else GRAPH[1])
            return GRAPH[3] if y == 0 else GRAPH[2]
        if face == "top":
            return CYAN[0] if x in (3, 4) else GRAPH[2]
        if face in ("left", "right") and y == 3:
            return CYAN[0] if x % 3 == 0 else GRAPH[1]                      # circuito lateral
        return plate(GRAPH, x, y % 4, w, 4, rivets=False)
    paint(l1, 0, 0, 8, 8, 8, helmet)

    def chest(face, x, y, w, h):
        if face == "bottom":
            return None
        if face == "top":
            return GRAPH[2]
        if face == "front":
            if 3 <= x <= 4 and 3 <= y <= 4:
                return CYAN[2]                                             # emblema del procesador
            if 2 <= x <= 5 and 2 <= y <= 5:
                return CYAN[0] if (x in (2, 5) or y in (2, 5)) else GRAPH[0]
            if (x, y) in ((1, 4), (6, 4), (3, 7), (4, 8), (4, 9)):
                return CYAN[1]                                             # pistas de circuito
            if y in (6, 10):
                return GRAPH[0]
            return lit(GRAPH, y % 4, 4)
        if face == "back":
            if x in (3, 4) and 1 <= y <= 9:
                return CYAN[1] if y % 2 else GRAPH[0]
            return plate(GRAPH, x % 4, y % 6, 4, 6, rivets=False)
        return GRAPH[1] if y % 4 else GRAPH[2]
    paint(l1, 16, 16, 8, 12, 4, chest)

    def arm(face, x, y, w, h):
        if face == "bottom":
            return None
        if y <= 3:
            return GRAPH[3] if face == "top" or y == 0 else GRAPH[2]
        if y == 4 and face != "back":
            return CYAN[1]                                                 # borde luminoso de la hombrera
        if 7 <= y <= 9:
            return plate(GRAPH, x, y - 7, w, 3, rivets=False)
        return GRAPH[1]
    paint(l1, 40, 16, 4, 12, 4, arm)

    def boots(face, x, y, w, h):
        if face == "top" or y < 8:
            return None
        if face == "bottom":
            return DARK
        if y == 8:
            return CYAN[0]
        return GRAPH[2] if y < 11 else GRAPH[0]
    paint(l1, 0, 16, 4, 12, 4, boots)

    l2 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    paint(l2, 16, 16, 8, 12, 4, lambda f, x, y, w, h: None if f in ("top", "bottom") or y < 8
          else (CYAN[0] if y == 8 else GRAPH[1]))

    def legs(face, x, y, w, h):
        if face == "bottom":
            return None
        if face == "top":
            return GRAPH[1]
        if face == "front" and x in (1, 2) and y % 3 == 1:
            return CYAN[1]
        if 5 <= y <= 6:
            return GRAPH[3] if y == 5 else GRAPH[2]
        return GRAPH[2] if face == "front" else GRAPH[1]
    paint(l2, 0, 16, 4, 12, 4, legs)
    return l1, l2


# =================================================================== Piroxita (Nether)
def piroxite():
    r = random.Random(7)
    cracks = {(r.randrange(8), r.randrange(12)) for _ in range(10)}
    l1 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    def helmet(face, x, y, w, h):
        if face == "bottom":
            return None
        if face == "front":
            if y == 3 and 1 <= x <= 6:
                return HEAT[2] if x in (2, 5) else DARK                     # ojos al rojo
            if y >= 5 and x in (1, 3, 4, 6):
                return BASALT[0] if y % 2 else HEAT[0]                      # rejilla de ventilación
            return BASALT[3] if y == 0 else BASALT[2]
        if face == "top":
            return HEAT[1] if x in (3, 4) and y % 2 == 0 else BASALT[3] if x in (3, 4) else BASALT[2]   # cresta-aleta
        if face in ("left", "right") and y in (2, 4, 6):
            return BASALT[0]                                               # aletas
        return BASALT[2] if y < 4 else BASALT[1]
    paint(l1, 0, 0, 8, 8, 8, helmet)

    def chest(face, x, y, w, h):
        if face == "bottom":
            return None
        if face == "top":
            return BASALT[2]
        if face == "front":
            if 2 <= x <= 5 and 2 <= y <= 7 and y % 2 == 0:
                return BASALT[0]                                           # aletas disipadoras
            if 2 <= x <= 5 and 2 <= y <= 7:
                return HEAT[0] if x in (2, 5) else HEAT[1]                 # calor entre aletas
            if (x, y) in cracks:
                return HEAT[1]
            return lit(BASALT, y % 4, 4)
        if (x, y) in cracks:
            return HEAT[0]
        if face == "back" and 2 <= x <= 5 and y < 9:
            return BASALT[0] if y % 2 else BASALT[2]
        return BASALT[1]
    paint(l1, 16, 16, 8, 12, 4, chest)

    def arm(face, x, y, w, h):
        if face == "bottom":
            return None
        if y <= 3:
            return BASALT[3] if face == "top" or y == 0 else (BASALT[0] if y == 2 else BASALT[2])
        if (x + y) % 7 == 0:
            return HEAT[0]
        return BASALT[1] if (x + y) % 3 else BASALT[2]
    paint(l1, 40, 16, 4, 12, 4, arm)

    def boots(face, x, y, w, h):
        if face == "top" or y < 8:
            return None
        if face == "bottom":
            return DARK
        if y == 11:
            return HEAT[0] if x % 2 else BASALT[0]
        return BASALT[2] if y == 8 else BASALT[1]
    paint(l1, 0, 16, 4, 12, 4, boots)

    l2 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    paint(l2, 16, 16, 8, 12, 4, lambda f, x, y, w, h: None if f in ("top", "bottom") or y < 8
          else (BASALT[3] if y == 8 else BASALT[1]))

    def legs(face, x, y, w, h):
        if face == "bottom":
            return None
        if face == "top":
            return BASALT[1]
        if face == "front" and 5 <= y <= 6:
            return HEAT[1] if y == 6 and x in (1, 2) else BASALT[3]
        if (x * 3 + y) % 9 == 0:
            return HEAT[0]
        return BASALT[2] if face == "front" else BASALT[1]
    paint(l2, 0, 16, 4, 12, 4, legs)
    return l1, l2


# =================================================================== Nulita (End)
def nullite():
    l1 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    def helmet(face, x, y, w, h):
        if face == "bottom":
            return None
        if face == "front":
            if y == 3 and 2 <= x <= 5:
                return VIOLET[2] if x in (3, 4) else VIOLET[1]             # rendija violeta
            if y in (2, 4) and 2 <= x <= 5:
                return NULL[0]
            return NULL[3] if y == 0 else NULL[2] if y < 6 else NULL[1]
        if face == "top":
            return NULL[3] if (x + y) % 5 else NULL[2]                       # cúpula lisa con brillo
        return NULL[2] if y < 4 else NULL[1] if y < 7 else NULL[0]
    paint(l1, 0, 0, 8, 8, 8, helmet)

    def chest(face, x, y, w, h):
        if face == "bottom":
            return None
        if face == "top":
            return NULL[2]
        if face == "front":
            if x in (3, 4) and y < 9:
                return VIOLET[1] if y % 3 == 1 else NULL[1]                 # junta central
            if y == 9:
                return VIOLET[0]
            return NULL[3] if y == 0 else NULL[2] if y < 6 else NULL[1]
        if face == "back":
            return VIOLET[0] if x in (3, 4) else NULL[1]
        return NULL[1] if y % 5 else VIOLET[0]
    paint(l1, 16, 16, 8, 12, 4, chest)

    def arm(face, x, y, w, h):
        if face == "bottom":
            return None
        if y <= 3:
            return NULL[3] if face == "top" or y == 0 else NULL[2]
        if y == 4:
            return VIOLET[1]
        return NULL[1] if y < 10 else NULL[0]
    paint(l1, 40, 16, 4, 12, 4, arm)

    def boots(face, x, y, w, h):
        if face == "top" or y < 8:
            return None
        if face == "bottom":
            return VIOLET[0]
        return VIOLET[1] if y == 8 else NULL[2] if y < 11 else NULL[0]
    paint(l1, 0, 16, 4, 12, 4, boots)

    l2 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    paint(l2, 16, 16, 8, 12, 4, lambda f, x, y, w, h: None if f in ("top", "bottom") or y < 8
          else (VIOLET[1] if y == 8 else NULL[1]))

    def legs(face, x, y, w, h):
        if face == "bottom":
            return None
        if face == "top":
            return NULL[1]
        if 5 <= y <= 6:
            return VIOLET[1] if y == 5 else NULL[2]
        return NULL[2] if face == "front" else NULL[1]
    paint(l2, 0, 16, 4, 12, 4, legs)
    return l1, l2


ICON_PALETTES = {
    "core": {"#": DARK, "R": GRAPH[2], "r": CYAN[2], "G": GRAPH[1], "D": GRAPH[0], "c": CYAN[2], "L": CYAN[0],
             "M": GRAPH[2], "C": CYAN[2], "P": GRAPH[1], "K": CYAN[1]},
    "piroxite": {"#": DARK, "R": BASALT[2], "r": HEAT[2], "G": BASALT[1], "D": BASALT[0], "c": HEAT[2], "L": HEAT[0],
                 "M": BASALT[2], "C": HEAT[1], "P": BASALT[1], "K": HEAT[1]},
    "nullite": {"#": VIOLET[0], "R": NULL[2], "r": NULL[3], "G": NULL[1], "D": VIOLET[0], "c": VIOLET[2], "L": VIOLET[1],
                "M": NULL[1], "C": VIOLET[2], "P": NULL[1], "K": VIOLET[1]},
}


# =================================================================== materiales recoloreados
def recolor(src, ramp):
    """Cambia los colores conservando la luminancia (y la transparencia) del dibujo original."""
    img = Image.open(src).convert("RGBA")
    out = Image.new("RGBA", img.size)
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            if a == 0:
                continue
            t = min(1.0, (0.299 * r + 0.587 * g + 0.114 * b) / 255 * 1.6)
            k = t * (len(ramp) - 1)
            i = min(int(k), len(ramp) - 2)
            f = k - i
            c = tuple(int(ramp[i][j] + (ramp[i + 1][j] - ramp[i][j]) * f) for j in range(3))
            out.putpixel((x, y), (*c, a))
    return out


PIRO_RAMP = [(20, 8, 6), (90, 24, 12), (190, 60, 20), (250, 140, 40), (255, 220, 140)]
NULL_RAMP = [(30, 20, 50), (90, 70, 140), (170, 150, 220), (225, 218, 248), (255, 255, 255)]


def ore_overlay(seed, core, mid, edge):
    """Capa 16x16 de cristales (transparente donde se ve la piedra)."""
    r = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for (cx, cy) in ((4, 4), (11, 6), (6, 11), (12, 12)):
        cx += r.randint(-1, 1)
        cy += r.randint(-1, 1)
        size = r.choice((1, 2))
        pts = [(cx + dx, cy + dy) for dx in range(-size, size + 1) for dy in range(-size, size + 1) if abs(dx) + abs(dy) <= size]
        for (x, y) in pts:
            if 0 <= x < 16 and 0 <= y < 16:
                img.putpixel((x, y), (*mid, 255))
        for (x, y) in pts:                                                 # borde oscuro
            for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                if 0 <= nx < 16 and 0 <= ny < 16 and img.getpixel((nx, ny))[3] == 0:
                    img.putpixel((nx, ny), (*edge, 255))
        img.putpixel((cx, cy), (*core, 255))
        if 0 <= cx - 1 < 16 and 0 <= cy - 1 < 16:
            img.putpixel((cx - 1, cy - 1), (*core, 255))
    return img


SETS = {"core": core, "piroxite": piroxite, "nullite": nullite}

if __name__ == "__main__":
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
    mats = {}
    for name, src, ramp in (("raw_piroxite", "raw_inercita", PIRO_RAMP), ("piroxite_ingot", "inercita_ingot", PIRO_RAMP),
                            ("raw_nullite", "raw_inercita", NULL_RAMP), ("nullite_ingot", "inercita_ingot", NULL_RAMP)):
        mats[name] = recolor(ASSETS / f"item/{src}.png", ramp)
        mats[name].save(ASSETS / f"item/{name}.png")
    ore_overlay(3, HEAT[2], HEAT[1], (90, 20, 10)).save(ASSETS / "block/piroxite_ore_overlay.png")
    ore_overlay(5, NULL[3], VIOLET[2], VIOLET[0]).save(ASSETS / "block/nullite_ore_overlay.png")
    if len(sys.argv) > 1:
        preview(layers, icons + [m.resize((32, 32)) for m in mats.values()], sys.argv[1])
    print("ok: armaduras tier 3")
