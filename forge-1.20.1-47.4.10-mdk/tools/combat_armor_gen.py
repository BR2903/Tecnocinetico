"""Armaduras de combate (sin habilidades; resistencia y movilidad): textures/models/armor/combat_mk{1,2,3}_layer_1/2.png
e iconos 16x16. Un mismo diseño militar de placas con distintos materiales por tier:
- Mk I (tier 1): verde oliva y acero, correaje de cuero; visor de rejilla.
- Mk II (tier 2): gris urbano con placas de inercita y visor cian.
- Mk III (tier 3): negro mate con filos de piroxita (naranja) y juntas de nulita (blanco); visor ámbar.

Uso (desde forge-1.20.1-47.4.10-mdk):  python tools/combat_armor_gen.py [preview.png]
"""
import sys
from pathlib import Path
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from armor_gen import ASSETS, DARK, paint, ICONS  # noqa: E402
from armor_t2_gen import icon, preview  # noqa: E402

SETS = {
    "combat_mk1": {"base": [(46, 52, 34), (66, 74, 48), (86, 96, 62), (108, 118, 80)],
                   "plate": [(64, 66, 70), (92, 96, 102), (124, 128, 132), (160, 166, 166)],
                   "trim": [(70, 46, 30), (98, 66, 42), (128, 88, 56)], "visor": [(30, 34, 30), (60, 70, 60), (90, 100, 90)]},
    "combat_mk2": {"base": [(44, 46, 50), (62, 64, 70), (82, 86, 92), (104, 108, 116)],
                   "plate": [(40, 46, 60), (64, 72, 90), (88, 98, 120), (118, 130, 154)],
                   "trim": [(30, 130, 146), (70, 210, 226), (180, 248, 255)], "visor": [(30, 130, 146), (70, 210, 226), (180, 248, 255)]},
    "combat_mk3": {"base": [(16, 16, 18), (26, 26, 30), (38, 38, 44), (54, 54, 62)],
                   "plate": [(24, 24, 28), (36, 36, 42), (52, 52, 60), (72, 72, 82)],
                   "trim": [(200, 80, 24), (240, 130, 40), (255, 200, 120)], "visor": [(180, 110, 20), (240, 170, 40), (255, 230, 150)],
                   "joint": [(170, 162, 196), (220, 214, 240), (246, 244, 255)]},
}


def layers(c):
    base, plate, trim, visor = c["base"], c["plate"], c["trim"], c["visor"]
    joint = c.get("joint", plate)
    l1 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    def helmet(face, x, y, w, h):
        if face == "bottom":
            return None
        if face == "front":
            if y in (3, 4) and 1 <= x <= 6:
                return visor[2] if (y == 3 and x in (2, 5)) else visor[1] if y == 3 else visor[0]   # visor ancho
            if y >= 5:
                return plate[1] if x in (0, 7) or y == 7 else (plate[2] if (x + y) % 2 else plate[1])   # mandíbula
            return plate[3] if y == 0 else plate[2]
        if face == "top":
            return trim[1] if x in (3, 4) else plate[2]                     # cresta
        if y == 4 and face in ("left", "right"):
            return joint[1]
        return plate[2] if y < 4 else plate[1] if y < 7 else None
    paint(l1, 0, 0, 8, 8, 8, helmet)

    def chest(face, x, y, w, h):
        if face == "bottom":
            return None
        if face == "top":
            return plate[2]
        if face == "front":
            if 1 <= x <= 6 and 1 <= y <= 6:                                 # placa pectoral
                if y == 1:
                    return plate[3]
                if x in (1, 6):
                    return plate[1]
                if y == 4 and 2 <= x <= 5:
                    return trim[1]
                return plate[2]
            if y in (8, 9) and x in (1, 2, 5, 6):
                return trim[0] if y == 9 else base[3]                       # bolsillos
            if y == 10:
                return trim[0]                                              # cinturón
            return base[2] if y < 6 else base[1]
        if face == "back":
            if 2 <= x <= 5 and 1 <= y <= 7:
                return plate[2] if y == 1 else plate[1]
            return base[1] if y != 10 else trim[0]
        return joint[1] if y == 3 else base[1] if y != 10 else trim[0]
    paint(l1, 16, 16, 8, 12, 4, chest)

    def arm(face, x, y, w, h):
        if face == "bottom":
            return None
        if y <= 3:
            return plate[3] if face == "top" or y == 0 else plate[2] if y < 3 else trim[1]   # hombrera con filo
        if face in ("front", "right", "left") and 6 <= y <= 8:
            return plate[2] if y == 6 else plate[1]                         # brazal
        if y == 5:
            return joint[1]
        return base[1] if y < 10 else base[0]
    paint(l1, 40, 16, 4, 12, 4, arm)

    def boots(face, x, y, w, h):
        if face == "top" or y < 8:
            return None
        if face == "bottom":
            return DARK
        if y == 8:
            return trim[1]
        return plate[2] if face == "front" and y >= 10 else plate[1]
    paint(l1, 0, 16, 4, 12, 4, boots)

    l2 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    paint(l2, 16, 16, 8, 12, 4, lambda f, x, y, w, h: None if f in ("top", "bottom") or y < 9 else (trim[0] if y == 9 else base[1]))

    def legs(face, x, y, w, h):
        if face == "bottom":
            return None
        if face == "top":
            return base[1]
        if face in ("front", "right", "left") and 1 <= y <= 3:
            return plate[2] if y == 1 else plate[1]                         # quijote
        if face in ("front", "right", "left") and 5 <= y <= 6:
            return plate[3] if y == 5 else joint[1]                         # rodillera
        return base[2] if face == "front" else base[1]
    paint(l2, 0, 16, 4, 12, 4, legs)
    return l1, l2


def icon_palette(c):
    return {"#": DARK, "R": c["plate"][2], "r": c["trim"][2], "G": c["base"][2], "D": c["base"][0], "c": c["visor"][1],
            "L": c["trim"][1], "M": c["plate"][1], "C": c["visor"][1], "P": c["base"][1], "K": c["trim"][1]}


if __name__ == "__main__":
    all_layers, icons = [], []
    for sid, c in SETS.items():
        l1, l2 = layers(c)
        l1.save(ASSETS / f"models/armor/{sid}_layer_1.png")
        l2.save(ASSETS / f"models/armor/{sid}_layer_2.png")
        all_layers += [l1, l2]
        for piece in ("helmet", "chestplate", "leggings", "boots"):
            img = icon(ICONS[f"scrap_{piece}"], icon_palette(c))
            img.save(ASSETS / f"item/{sid}_{piece}.png")
            icons.append(img)
    if len(sys.argv) > 1:
        preview(all_layers, icons, sys.argv[1])
    print("ok: armaduras de combate")
