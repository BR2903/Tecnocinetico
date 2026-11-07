"""Genera el modelo 3D + textura del Martillo Cinético (la pistola tiene su propio pistol_gen.py).

Cada elemento del modelo recibe su propio trozo de textura (UV empaquetada automáticamente),
así la textura siempre coincide con la geometría. Uso:  python tools_gen.py <assets/tecnocinetico>
"""
import json
import math
import random
import sys
from pathlib import Path
from PIL import Image

ASSETS = Path(sys.argv[1])
PREVIEW_DIR = Path(sys.argv[2]) if len(sys.argv) > 2 else None
PPU = 2  # píxeles de textura por unidad de modelo (el doble que vanilla)

# ---------------------------------------------------------------- paleta
GUN = (52, 57, 68)
GUN_DARK = (34, 37, 46)
GUN_LIGHT = (92, 100, 116)
STEEL = (186, 195, 206)
STEEL_DARK = (128, 137, 150)
COPPER = (184, 104, 60)
COPPER_DARK = (122, 64, 36)
CYAN = (70, 227, 245)
CYAN_BRIGHT = (200, 253, 255)
CYAN_DARK = (24, 128, 150)
RUBBER = (30, 31, 37)
RUBBER_LIGHT = (50, 52, 60)
INERCITA = (64, 72, 90)
INERCITA_LIGHT = (104, 114, 136)
INERCITA_DARK = (38, 43, 56)
LEATHER = (82, 54, 38)
LEATHER_DARK = (52, 33, 24)
OUTLINE = (18, 20, 26)


def clamp(v):
    return max(0, min(255, int(v)))


def rgba(c, k=1.0):
    return (clamp(c[0] * k), clamp(c[1] * k), clamp(c[2] * k), 255)


def jitter(c, amt=0.06):
    return rgba(c, 1.0 + random.uniform(-amt, amt))


# ---------------------------------------------------------------- pintores de materiales
# Cada pintor recibe (cara, ancho, alto, ejes) y devuelve una función (x, y) -> RGBA.
# ejes = (eje_u, eje_v): qué eje del modelo recorre la textura en horizontal y en vertical.

def bevel(base, light, dark):
    def paint(face, w, h, axes):
        def px(x, y):
            if w > 2 and h > 2:
                if x == 0 or y == 0:
                    return rgba(light)
                if x == w - 1 or y == h - 1:
                    return rgba(dark)
            return jitter(base)
        return px
    return paint


def p_gun(face, w, h, axes):
    return bevel(GUN, GUN_LIGHT, GUN_DARK)(face, w, h, axes)


def p_gun_dark(face, w, h, axes):
    return bevel(GUN_DARK, GUN, OUTLINE)(face, w, h, axes)


def p_steel(face, w, h, axes):
    return bevel(STEEL, (230, 236, 242), STEEL_DARK)(face, w, h, axes)


def p_slide(face, w, h, axes):
    base = p_gun(face, w, h, axes)
    au, av = axes

    def px(x, y):
        if face in ("east", "west") and 1 <= y <= h - 2:
            # rejillas de ventilación en la mitad delantera (z pequeña)
            u = x if face == "west" else w - 1 - x
            if u < w * 0.45 and u % 3 == 1:
                return rgba(GUN_DARK)
            # línea de energía cian a lo largo del cuerpo
            if y == h // 2 and u >= w * 0.45:
                return rgba(CYAN_DARK)
        if face == "up" and w >= 3 and x == w // 2:
            return rgba(CYAN, 0.85)  # riel de energía superior
        return base(x, y)
    return px


def p_coil(face, w, h, axes):
    """Bobinas: anillos de cobre y cinetrita alrededor del cañón (a lo largo de z)."""
    def px(x, y):
        if face in ("north", "south"):
            cx, cy = (w - 1) / 2, (h - 1) / 2
            if abs(x - cx) <= 0.6 and abs(y - cy) <= 0.6:
                return rgba(OUTLINE)
            return jitter(COPPER)
        along = x if axes[0] == "z" else y
        return rgba(CYAN) if along % 3 == 0 else jitter(COPPER if along % 3 == 1 else COPPER_DARK, 0.04)
    return px


def p_muzzle(face, w, h, axes):
    base = p_gun_dark(face, w, h, axes)

    def px(x, y):
        if face == "north":
            cx, cy = (w - 1) / 2, (h - 1) / 2
            d = max(abs(x - cx), abs(y - cy))
            if d <= 0.6:
                return rgba((5, 5, 8))
            if d <= 1.6:
                return rgba(CYAN_DARK)
        return base(x, y)
    return px


def p_cell(face, w, h, axes):
    """Célula de cinetrita: ventana brillante con marco."""
    def px(x, y):
        if w > 2 and h > 2 and (x == 0 or y == 0 or x == w - 1 or y == h - 1):
            return rgba(GUN_DARK)
        cx, cy = (w - 1) / 2, (h - 1) / 2
        d = math.hypot((x - cx) / max(cx, 1), (y - cy) / max(cy, 1))
        k = max(0.0, 1.0 - d)
        return (clamp(CYAN[0] + (CYAN_BRIGHT[0] - CYAN[0]) * k),
                clamp(CYAN[1] + (CYAN_BRIGHT[1] - CYAN[1]) * k),
                clamp(CYAN[2] + (CYAN_BRIGHT[2] - CYAN[2]) * k), 255)
    return px


def p_grip(face, w, h, axes):
    def px(x, y):
        along = y if axes[1] == "y" else x
        if along % 2 == 0:
            return rgba(RUBBER_LIGHT)
        return jitter(RUBBER, 0.1)
    return px


def p_inercita(face, w, h, axes):
    base = bevel(INERCITA, INERCITA_LIGHT, INERCITA_DARK)(face, w, h, axes)

    def px(x, y):
        # remaches en las esquinas
        if w >= 6 and h >= 6 and x in (1, w - 2) and y in (1, h - 2):
            return rgba(INERCITA_LIGHT, 1.15)
        return base(x, y)
    return px


def p_strike(face, w, h, axes):
    """Caras de golpeo: acero con un anillo de cinetrita en la cara exterior."""
    base = p_steel(face, w, h, axes)

    def px(x, y):
        if face in ("east", "west"):
            cx, cy = (w - 1) / 2, (h - 1) / 2
            d = math.hypot(x - cx, y - cy)
            if d <= 1.0:
                return rgba(CYAN_BRIGHT)
            if 2.0 <= d <= 3.0:
                return rgba(CYAN)
        return base(x, y)
    return px


def p_copper(face, w, h, axes):
    return bevel(COPPER, (222, 146, 96), COPPER_DARK)(face, w, h, axes)


def p_wrap(face, w, h, axes):
    """Mango envuelto en cuero con vendas diagonales."""
    def px(x, y):
        return rgba(LEATHER_DARK) if (x + y) % 4 == 0 else jitter(LEATHER, 0.08)
    return px


PAINTERS = {
    "gun": p_gun, "gun_dark": p_gun_dark, "steel": p_steel, "slide": p_slide, "coil": p_coil,
    "muzzle": p_muzzle, "cell": p_cell, "grip": p_grip, "inercita": p_inercita, "strike": p_strike,
    "copper": p_copper, "wrap": p_wrap, "core": p_cell,
}

FACES = ["north", "south", "east", "west", "up", "down"]


def face_info(frm, to, face):
    dx, dy, dz = (to[i] - frm[i] for i in range(3))
    if face in ("north", "south"):
        return dx, dy, ("x", "y")
    if face in ("east", "west"):
        return dz, dy, ("z", "y")
    return dx, dz, ("x", "z")


def build(name, elements, display):
    """Empaqueta las caras en una textura, la pinta y escribe el modelo 3D."""
    rects = []
    for ei, el in enumerate(elements):
        for face in FACES:
            w, h, axes = face_info(el["from"], el["to"], face)
            rects.append([ei, face, max(1, round(w * PPU)), max(1, round(h * PPU)), axes])

    for size in (32, 64, 128):
        x = y = row_h = 0
        ok = True
        for r in sorted(rects, key=lambda r: -r[3]):
            if x + r[2] > size:
                x, y, row_h = 0, y + row_h, 0
            if y + r[3] > size or r[2] > size:
                ok = False
                break
            r.append((x, y))
            x += r[2]
            row_h = max(row_h, r[3])
        if ok:
            break
        for r in rects:
            del r[5:]

    tex = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out_elements = [{"name": el["name"], "from": el["from"], "to": el["to"], "faces": {}} for el in elements]
    for el, out in zip(elements, out_elements):
        if "rotation" in el:
            out["rotation"] = el["rotation"]

    scale = 16 / size
    for ei, face, w, h, axes, (x0, y0) in rects:
        painter = PAINTERS[elements[ei]["mat"]](face, w, h, axes)
        for yy in range(h):
            for xx in range(w):
                tex.putpixel((x0 + xx, y0 + yy), painter(xx, yy))
        out_elements[ei]["faces"][face] = {
            "uv": [round(x0 * scale, 4), round(y0 * scale, 4),
                   round((x0 + w) * scale, 4), round((y0 + h) * scale, 4)],
            "texture": "#0",
        }

    tex_path = ASSETS / "textures" / "item" / f"{name}.png"
    tex.save(tex_path)
    model = {
        "credit": "Tecnocinetico - generado por tools_gen.py (editable en Blockbench)",
        "texture_size": [size, size],
        "textures": {"0": f"tecnocinetico:item/{name}", "particle": f"tecnocinetico:item/{name}"},
        "elements": out_elements,
        "display": display,
    }
    (ASSETS / "models" / "item" / f"{name}_3d.json").write_text(json.dumps(model, indent=2), encoding="utf-8")
    return tex


# ---------------------------------------------------------------- iconos planos 16x16
def icon(name, classify):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            c = classify(x, y)
            if c:
                img.putpixel((x, y), c)
    # contorno oscuro alrededor de la silueta
    out = img.copy()
    for y in range(16):
        for x in range(16):
            if img.getpixel((x, y))[3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and img.getpixel((nx, ny))[3]:
                    out.putpixel((x, y), rgba(OUTLINE))
                    break
    out.save(ASSETS / "textures" / "item" / f"{name}_icon.png")
    return out


def hammer_icon(x, y):
    px, py = x + 0.5, y + 0.5
    s2 = math.sqrt(2)
    d = (1 / s2, -1 / s2)   # dirección del mango (hacia arriba a la derecha)
    p = (1 / s2, 1 / s2)    # perpendicular (dirección de la cabeza)
    # cabeza
    cx, cy = 11.0, 5.0
    a = (px - cx) * p[0] + (py - cy) * p[1]
    b = (px - cx) * d[0] + (py - cy) * d[1]
    if abs(a) <= 5.4 and abs(b) <= 2.2:
        if abs(a) > 4.0:
            return rgba(STEEL) if b < 0.5 else rgba(STEEL_DARK)
        if abs(a) <= 1.0 and abs(b) <= 1.0:
            return rgba(CYAN_BRIGHT) if abs(a) < 0.5 else rgba(CYAN)
        if abs(abs(a) - 2.3) < 0.5:
            return rgba(COPPER)
        return rgba(INERCITA_LIGHT) if b < -1.0 else rgba(INERCITA)
    # mango
    ax, ay = 2.0, 14.0
    t = (px - ax) * d[0] + (py - ay) * d[1]
    s = (px - ax) * p[0] + (py - ay) * p[1]
    if -0.3 <= t <= 10.5 and abs(s) <= 0.75:
        if t < 1.2:
            return rgba(GUN_DARK)
        if t > 9.0:
            return rgba(COPPER)
        return rgba(LEATHER_DARK) if int(t * 1.4) % 2 == 0 else rgba(LEATHER)
    return None


def pistol_icon(x, y):
    grip_rows = {8: (3, 5), 9: (3, 5), 10: (2, 4), 11: (2, 4), 12: (1, 3)}
    if (x, y) == (3, 3):
        return rgba(GUN_DARK)                       # mira trasera
    if (x, y) == (11, 3):
        return rgba(STEEL)                          # mira delantera
    if 4 <= y <= 6 and 13 <= x <= 15:               # cañón con bobinas
        if y == 5 and x == 15:
            return rgba(OUTLINE)
        return rgba(CYAN) if x == 14 else rgba(COPPER)
    if 4 <= y <= 6 and 2 <= x <= 12:                # corredera
        if y == 5 and 6 <= x <= 8:
            return rgba(CYAN_BRIGHT) if x == 7 else rgba(CYAN)
        if y == 5 and x in (10, 12):
            return rgba(GUN_DARK)
        return rgba(GUN_LIGHT) if y == 4 else rgba(GUN)
    if y == 7 and 3 <= x <= 11:
        return rgba(GUN_DARK)                       # armazón
    if y == 13 and 1 <= x <= 3:
        return rgba(GUN_DARK)                       # base del cargador
    if y in grip_rows and grip_rows[y][0] <= x <= grip_rows[y][1]:
        if x == grip_rows[y][0]:
            return rgba((84, 88, 102))              # brillo en el borde de la empuñadura
        return rgba((66, 68, 80)) if y % 2 == 0 else rgba((48, 50, 60))
    if (x, y) == (7, 8):
        return rgba(STEEL)                          # gatillo
    if (x, y) in ((6, 9), (7, 9), (8, 9), (9, 8)):
        return rgba(GUN_DARK)                       # guardamonte
    return None


# ---------------------------------------------------------------- definición de los modelos
random.seed(42)

hammer_elements = [
    {"name": "pommel", "from": [6.5, -1, 6.5], "to": [9.5, 1, 9.5], "mat": "gun_dark"},
    {"name": "handle", "from": [7, 1, 7], "to": [9, 9.5, 9], "mat": "wrap"},
    {"name": "collar", "from": [6.5, 9.5, 6.5], "to": [9.5, 11, 9.5], "mat": "copper"},
    {"name": "head", "from": [3, 11, 5.5], "to": [13, 15, 10.5], "mat": "inercita"},
    {"name": "band_left", "from": [4.75, 10.75, 5.25], "to": [5.5, 15.25, 10.75], "mat": "copper"},
    {"name": "band_right", "from": [10.5, 10.75, 5.25], "to": [11.25, 15.25, 10.75], "mat": "copper"},
    {"name": "core_front", "from": [6.5, 12, 5.25], "to": [9.5, 14, 5.5], "mat": "core"},
    {"name": "core_back", "from": [6.5, 12, 10.5], "to": [9.5, 14, 10.75], "mat": "core"},
    {"name": "core_top", "from": [6.5, 15, 6.5], "to": [9.5, 15.75, 9.5], "mat": "core"},
    {"name": "strike_left", "from": [1.5, 10.5, 5], "to": [3, 15.5, 11], "mat": "strike"},
    {"name": "strike_right", "from": [13, 10.5, 5], "to": [14.5, 15.5, 11], "mat": "strike"},
]

hammer_display = {
    "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [0, 1, 0.5], "scale": [0.9, 0.9, 0.9]},
    "thirdperson_lefthand": {"rotation": [0, -90, 0], "translation": [0, 1, 0.5], "scale": [0.9, 0.9, 0.9]},
    "firstperson_righthand": {"rotation": [0, 90, 10], "translation": [1, 2, 0], "scale": [0.75, 0.75, 0.75]},
    "firstperson_lefthand": {"rotation": [0, -90, -10], "translation": [1, 2, 0], "scale": [0.75, 0.75, 0.75]},
    "ground": {"translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 90, 0], "scale": [0.8, 0.8, 0.8]},
    "head": {"rotation": [0, 90, 0], "translation": [0, 6, 0]},
}

grip_rot = {"angle": -22.5, "axis": "x", "origin": [8, 11, 11.25]}
pistol_elements = [
    {"name": "slide", "from": [6.5, 12, 2.5], "to": [9.5, 15, 13], "mat": "slide"},
    {"name": "barrel_coil", "from": [7, 12.5, 0], "to": [9, 14.5, 2.5], "mat": "coil"},
    {"name": "muzzle", "from": [6.75, 12.25, -0.75], "to": [9.25, 14.75, 0], "mat": "muzzle"},
    {"name": "cinetrite_cell", "from": [6.25, 12.75, 6], "to": [9.75, 14.25, 9], "mat": "cell"},
    {"name": "frame", "from": [7, 10.75, 3.5], "to": [9, 12, 13], "mat": "gun_dark"},
    {"name": "grip", "from": [6.75, 5.5, 9.75], "to": [9.25, 10.75, 12.75], "mat": "grip", "rotation": grip_rot},
    {"name": "mag_plate", "from": [6.5, 5, 9.5], "to": [9.5, 5.5, 13], "mat": "gun_dark", "rotation": grip_rot},
    {"name": "guard_bottom", "from": [7.5, 9.5, 6.5], "to": [8.5, 10, 9.75], "mat": "gun_dark"},
    {"name": "guard_front", "from": [7.5, 10, 6.5], "to": [8.5, 10.75, 7], "mat": "gun_dark"},
    {"name": "trigger", "from": [7.75, 10, 8], "to": [8.25, 10.75, 8.5], "mat": "steel"},
    {"name": "rear_sight", "from": [7, 15, 11.5], "to": [9, 15.75, 12.5], "mat": "gun_dark"},
    {"name": "front_sight", "from": [7.75, 15, 3], "to": [8.25, 15.5, 3.75], "mat": "steel"},
    {"name": "hammer_spur", "from": [7.25, 13, 13], "to": [8.75, 14.5, 14], "mat": "steel"},
]

pistol_display = {
    "thirdperson_righthand": {"translation": [0, -1.5, 0]},
    "thirdperson_lefthand": {"translation": [0, -1.5, 0]},
    "firstperson_righthand": {"translation": [-1, 0, -3.25]},
    "firstperson_lefthand": {"translation": [-0.5, 0, -3]},
    "ground": {"translation": [0, -2, 0], "scale": [0.6, 0.6, 0.6]},
    "fixed": {"rotation": [0, 90, 0], "translation": [0, -3, 0]},
}

ht = build("kinetic_hammer", hammer_elements, hammer_display)

# El inventario muestra el modelo 3D (no un icono plano). Después de ejecutar este script:
#   python tools/gui_fit.py <assets> "0,25,-45" kinetic_hammer_3d
(ASSETS / "models" / "item" / "kinetic_hammer.json").write_text(
    json.dumps({"parent": "tecnocinetico:item/kinetic_hammer_3d"}, indent=2), encoding="utf-8")
print("ok", ht.size)
