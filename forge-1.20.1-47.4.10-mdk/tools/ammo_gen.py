"""Munición y cargadores en 3D: textura atlas 32x32 (textures/item/ammo.png) y modelos de
kinetic_round, guided_round, magazine y extended_magazine (con variantes vacío / bala normal / bala guiada).

Uso (desde forge-1.20.1-47.4.10-mdk):
    python tools/ammo_gen.py
    python tools/gui_fit.py src/main/resources/assets/tecnocinetico "y-135;x30" kinetic_round
    (igual para guided_round, magazine, magazine_loaded, magazine_guided, extended_magazine, ...)
Misma paleta que la pistola (tools/pistol_gen.py): acero pavonado, polímero, cobre, cian de cinetrita, inercita.
"""
import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tecnocinetico"
TEX = "tecnocinetico:item/ammo"

STEEL = [(24, 25, 29), (40, 42, 47), (78, 82, 90)]
POLY = [(20, 21, 23), (33, 34, 37), (52, 54, 58)]
COPPER = [(96, 56, 32), (150, 92, 56), (206, 140, 92), (236, 186, 132)]
CYAN = [(30, 130, 146), (64, 206, 228), (176, 246, 255)]
INERCITA = [(38, 43, 56), (64, 72, 90), (104, 114, 136)]
RED = [(110, 22, 22), (190, 40, 40)]
GUIDE = [(120, 50, 170), (216, 108, 255)]

# zonas del atlas (8x8 px cada una) → nombre: (columna, fila)
ZONES = {"copper": (0, 0), "rim": (1, 0), "tip": (2, 0), "tip_hi": (3, 0),
         "steel": (0, 1), "red": (1, 1), "poly": (2, 1), "window": (3, 1),
         "inercita": (0, 2), "guide": (1, 2), "follower": (2, 2), "steel_hi": (3, 2)}


def paint():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))

    def zone(name, fn):
        cx, cy = ZONES[name]
        for y in range(8):
            for x in range(8):
                img.putpixel((cx * 8 + x, cy * 8 + y), (*fn(x, y), 255))

    # vaina de cobre: brillo vertical (cilindro) y canto superior claro
    zone("copper", lambda x, y: COPPER[3] if x in (2, 3) else COPPER[2] if x in (1, 4) else COPPER[1] if x < 7 else COPPER[0])
    zone("rim", lambda x, y: COPPER[0] if y > 4 else COPPER[1])
    zone("tip", lambda x, y: CYAN[2] if x in (2, 3) else CYAN[1] if x < 6 else CYAN[0])
    zone("tip_hi", lambda x, y: CYAN[2] if (x + y) % 3 else CYAN[1])
    zone("steel", lambda x, y: STEEL[2] if y == 0 else STEEL[1] if y < 7 else STEEL[0])
    zone("steel_hi", lambda x, y: STEEL[2] if x in (1, 2) else STEEL[1])
    zone("red", lambda x, y: RED[1] if y < 4 else RED[0])
    zone("poly", lambda x, y: POLY[2] if y == 0 else POLY[1] if y < 7 else POLY[0])
    # costado del cargador: cuerpo de acero con ventana que deja ver las balas (rayas cian)
    zone("window", lambda x, y: (STEEL[2] if y == 0 else STEEL[1]) if x in (0, 1, 2, 5, 6, 7)
         else (STEEL[0] if y % 3 == 2 else CYAN[0] if y % 3 == 0 else COPPER[1]))       # ventana estrecha
    zone("inercita", lambda x, y: INERCITA[2] if y == 0 else INERCITA[1] if y < 7 else INERCITA[0])
    zone("guide", lambda x, y: GUIDE[1] if (x + y) % 2 else GUIDE[0])
    zone("follower", lambda x, y: POLY[0])
    return img


def uv(name, w=4, h=4):
    cx, cy = ZONES[name]
    u, v = cx * 4, cy * 4                                         # 1 unidad UV = 2 px (textura 32, UV 0..16)
    return [u, v, u + w, v + h]


def el(fr, to, top, side=None, bottom=None, rot=None):
    side = side or top
    bottom = bottom or side
    e = {"from": fr, "to": to, "faces": {
        "up": {"uv": uv(top), "texture": "#0"}, "down": {"uv": uv(bottom), "texture": "#0"},
        "north": {"uv": uv(side), "texture": "#0"}, "south": {"uv": uv(side), "texture": "#0"},
        "east": {"uv": uv(side), "texture": "#0"}, "west": {"uv": uv(side), "texture": "#0"}}}
    if rot:
        e["rotation"] = rot
    return e


def round_elements(guided=False, y0=0.0, scale=1.0, cx=8.0, cz=8.0, lying=False):
    """Cartucho: culote (reborde), vaina, punta de cinetrita. Guiado: vaina de acero, banda roja y aletas."""
    def box(x0, a0, z0, x1, a1, z1, *mats):
        # a = altura a lo largo del cartucho; tumbado (lying) el eje largo pasa a ser Z
        if lying:
            return el([cx + x0 * scale, y0 + z0 * scale, cz + a0 * scale], [cx + x1 * scale, y0 + z1 * scale, cz + a1 * scale], *mats)
        return el([cx + x0 * scale, y0 + a0 * scale, cz + z0 * scale], [cx + x1 * scale, y0 + a1 * scale, cz + z1 * scale], *mats)
    casing = "steel" if guided else "copper"
    els = [box(-1.25, 0, -1.25, 1.25, 0.6, 1.25, "rim"),
           box(-1.0, 0.6, -1.0, 1.0, 6.5, 1.0, casing),
           box(-0.8, 6.5, -0.8, 0.8, 8.5, 0.8, "tip"),
           box(-0.45, 8.5, -0.45, 0.45, 9.4, 0.45, "tip_hi")]
    if guided:
        els += [box(-1.05, 4.6, -1.05, 1.05, 5.3, 1.05, "red"),
                box(-1.9, 0.6, -0.15, 1.9, 2.6, 0.15, "steel_hi"),     # aletas guía en cruz
                box(-0.15, 0.6, -1.9, 0.15, 2.6, 1.9, "steel_hi"),
                box(-0.5, 9.4, -0.5, 0.5, 9.6, 0.5, "guide")]          # sensor de guiado en la punta
    return els


def magazine_elements(extended=False, top=None):
    """Cargador recto: cuerpo de acero con ventana, labios arriba, base de polímero (inercita si es ampliado).
    top = None | "normal" | "guided": la bala que asoma por arriba cuando está cargado."""
    h = 13.0 if extended else 9.0
    y0 = 1.0
    els = [el([6.5, y0, 6.6], [9.5, y0 + h, 9.4], "steel", "window", "poly"),
           el([6.3, 0.0, 6.4], [9.7, y0, 9.6], "inercita" if extended else "poly"),          # base
           el([6.5, y0 + h, 6.6], [6.9, y0 + h + 0.5, 9.4], "steel_hi"),                      # labios
           el([9.1, y0 + h, 6.6], [9.5, y0 + h + 0.5, 9.4], "steel_hi")]
    if top is None:
        els.append(el([6.9, y0 + h - 0.4, 6.8], [9.1, y0 + h, 9.2], "follower"))            # empujador (vacío)
    else:
        els += round_elements(top == "guided", y0=y0 + h + 0.55, scale=0.42, lying=True, cz=6.4)
    return els


def model(name, elements):
    data = {"credit": "Tecnocinetico - ammo_gen.py", "texture_size": [32, 32],
            "textures": {"0": TEX, "particle": TEX}, "elements": elements,
            "display": {"thirdperson_righthand": {"translation": [0, 2, 1], "scale": [0.5, 0.5, 0.5]},
                        "thirdperson_lefthand": {"translation": [0, 2, 1], "scale": [0.5, 0.5, 0.5]},
                        "firstperson_righthand": {"rotation": [0, -90, 15], "translation": [1, 3, 0], "scale": [0.6, 0.6, 0.6]},
                        "firstperson_lefthand": {"rotation": [0, -90, 15], "translation": [1, 3, 0], "scale": [0.6, 0.6, 0.6]},
                        "ground": {"translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
                        "fixed": {"rotation": [0, 90, 0]}}}
    path = ROOT / "models/item" / f"{name}.json"
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    if "gui" in old.get("display", {}):
        data["display"]["gui"] = old["display"]["gui"]              # conserva el ajuste de gui_fit
    path.write_text(json.dumps(data, indent=2), encoding="utf-8")
    return data


if __name__ == "__main__":
    paint().save(ROOT / "textures/item/ammo.png")
    model("kinetic_round", round_elements(False, y0=3.3))
    model("guided_round", round_elements(True, y0=3.2))
    for mag, ext in (("magazine", False), ("extended_magazine", True)):
        model(f"{mag}_loaded", magazine_elements(ext, "normal"))
        model(f"{mag}_guided", magazine_elements(ext, "guided"))
        base = model(mag, magazine_elements(ext, None))
        base["overrides"] = [{"predicate": {"tecnocinetico:loaded": 0.5}, "model": f"tecnocinetico:item/{mag}_loaded"},
                             {"predicate": {"tecnocinetico:loaded": 1.0}, "model": f"tecnocinetico:item/{mag}_guided"}]
        (ROOT / "models/item" / f"{mag}.json").write_text(json.dumps(base, indent=2), encoding="utf-8")
    print("ok: munición y cargadores")
