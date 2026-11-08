"""Munición, cargadores y gadgets del Equipo Táctico en 3D: textura atlas 32x32 (textures/item/ammo.png) y modelos de
kinetic_round, guided_round, magazine y extended_magazine (con variantes vacío / bala normal / bala guiada).

Uso (desde forge-1.20.1-47.4.10-mdk):
    python tools/ammo_gen.py
    python tools/gui_fit.py src/main/resources/assets/tecnocinetico "y-135;x30" kinetic_round
    (igual para guided_round, magazine, magazine_loaded, magazine_guided, extended_magazine, ...)
Misma paleta que la pistola (tools/pistol_gen.py): acero pavonado, polímero, cobre, cian de cinetrita, inercita.
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gunkit import resolve_zfight  # noqa: E402  (evita el parpadeo de caras coplanares)
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
         "inercita": (0, 2), "guide": (1, 2), "follower": (2, 2), "steel_hi": (3, 2),
         "leather": (0, 3), "teal": (1, 3), "lens": (2, 3), "copper_dark": (3, 3)}


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
    zone("leather", lambda x, y: (96, 64, 42) if y in (0, 7) else (70, 46, 30) if (x + y) % 4 else (60, 40, 26))
    zone("teal", lambda x, y: (60, 120, 110) if y < 2 else (42, 92, 86) if x % 3 else (30, 70, 66))   # aletas
    zone("lens", lambda x, y: CYAN[2] if (x, y) in ((1, 1), (2, 1), (1, 2)) else CYAN[1] if x + y < 9 else CYAN[0])
    zone("copper_dark", lambda x, y: COPPER[0] if y % 3 else COPPER[1])
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
            "textures": {"0": TEX, "particle": TEX}, "elements": resolve_zfight(elements),
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


# ---------------------------------------------------------------- gadgets del Equipo Táctico
def visor_elements():
    """Visor de visión nocturna: dos tubos con lente cian, puente, placa frontal y correa de cuero."""
    return [el([4.5, 6, 6], [11.5, 9.5, 7.5], "steel", "steel", "steel"),                 # placa frontal
            el([4.6, 6.4, 3.4], [7.4, 9.2, 6], "steel_hi", "steel"),                       # tubo izq.
            el([8.6, 6.4, 3.4], [11.4, 9.2, 6], "steel_hi", "steel"),                      # tubo der.
            el([5.0, 6.8, 3.2], [7.0, 8.8, 3.4], "lens"),                                  # lente izq.
            el([9.0, 6.8, 3.2], [11.0, 8.8, 3.4], "lens"),                                 # lente der.
            el([7.4, 7.2, 4.6], [8.6, 8.4, 6], "copper"),                                  # puente
            el([3.0, 6.8, 6.4], [4.5, 8.6, 12.5], "leather"),                              # correa izq.
            el([11.5, 6.8, 6.4], [13.0, 8.6, 12.5], "leather"),                            # correa der.
            el([3.0, 6.8, 12.5], [13.0, 8.6, 13.6], "leather")]                            # correa trasera


def jetpack_elements():
    """Jetpack: placa de espalda, dos depósitos de cobre con tapa, acoplador central, toberas con brillo cian."""
    els = [el([4.5, 3, 10], [11.5, 13, 11.5], "steel", "steel"),                           # placa de espalda
           el([7.2, 6, 8.2], [8.8, 10, 10], "copper_dark")]                                # acoplador
    for x0 in (3.5, 8.6):
        els += [el([x0, 3.5, 6.6], [x0 + 3.9, 12, 10], "copper"),                          # depósito
                el([x0 + 0.5, 12, 7.1], [x0 + 3.4, 13, 9.5], "steel_hi"),                  # tapa
                el([x0 + 0.8, 1.6, 7.4], [x0 + 3.1, 3.5, 9.2], "steel"),                   # tobera
                el([x0 + 1.0, 1.4, 7.6], [x0 + 2.9, 1.6, 9.0], "lens")]                    # llama cian
    els += [el([4.5, 11, 5.6], [5.6, 12, 10], "leather"), el([10.4, 11, 5.6], [11.5, 12, 10], "leather")]  # tirantes
    return els


def jetpack_level_elements(level):
    """Mk II: propulsores laterales; Mk III además aleta dorsal y alerones."""
    els = jetpack_elements()
    if level >= 2:
        els += [el([1.6, 5, 7.2], [3.5, 10, 9.2], "copper_dark"), el([12.5, 5, 7.2], [14.4, 10, 9.2], "copper_dark"),
                el([1.8, 4.6, 7.4], [3.3, 5, 9.0], "lens"), el([12.7, 4.6, 7.4], [14.2, 5, 9.0], "lens")]
    if level >= 3:
        els += [el([7.5, 4, 11.5], [8.5, 12, 13.5], "steel_hi"),
                el([0.5, 11.5, 7.5], [3.5, 12.5, 9.5], "steel_hi"), el([12.5, 11.5, 7.5], [15.5, 12.5, 9.5], "steel_hi")]
    return els


def battery_elements():
    """Batería de Visor: cuerpo de acero con franja cian y bornes de cobre."""
    return [el([5.5, 2, 6.5], [10.5, 11, 9.5], "steel", "steel"), el([5.4, 5, 6.4], [10.6, 6.5, 9.6], "lens"),
            el([6.5, 11, 7.3], [7.7, 12, 8.7], "copper"), el([8.3, 11, 7.3], [9.5, 12, 8.7], "copper")]


def fuel_elements():
    """Cartucho de Propelente: bombona de cobre con aros, válvula de acero."""
    return [el([5.5, 1, 5.5], [10.5, 11, 10.5], "copper"), el([5.4, 3, 5.4], [10.6, 3.6, 10.6], "copper_dark"),
            el([5.4, 8.5, 5.4], [10.6, 9.1, 10.6], "copper_dark"), el([7, 11, 7], [9, 13, 9], "steel_hi"),
            el([6, 12.5, 7.6], [10, 13.2, 8.4], "steel")]


def cell_elements():
    """Célula de Propulsión: cápsula verde azulada con ventana cian y extremos de acero."""
    return [el([6, 3, 6], [10, 11, 10], "teal"), el([5.6, 2, 5.6], [10.4, 3, 10.4], "steel"),
            el([5.6, 11, 5.6], [10.4, 12, 10.4], "steel"), el([7, 5, 5.8], [9, 9, 6], "lens")]


def kit_elements(level):
    """Kit de mejora: maletín de acero con cierres y una placa (cobre Mk II, cian Mk III)."""
    accent = "copper" if level == 2 else "lens"
    return [el([3, 2, 5], [13, 9, 11], "steel", "steel"), el([3.2, 9, 5.2], [12.8, 9.6, 10.8], "steel_hi"),
            el([6, 9.6, 7.5], [10, 10.6, 8.5], "poly"), el([4, 5, 4.8], [6, 7, 5], accent), el([10, 5, 4.8], [12, 7, 5], accent),
            el([6.5, 4, 4.8], [9.5, 8, 5], "copper_dark" if level == 2 else "guide")]


def propulsor_elements():
    """Propulsor acuático: dos turbinas de tobillo con rotor cian y aleta."""
    els = []
    for x0 in (2.5, 9.0):
        els += [el([x0, 4, 6], [x0 + 4.5, 8.5, 10], "steel", "steel"),                     # carcasa
                el([x0 + 0.8, 4.8, 5.8], [x0 + 3.7, 7.7, 6.0], "lens"),                    # rotor
                el([x0 + 0.5, 3.4, 10], [x0 + 4.0, 4.2, 14.5], "teal"),                    # aleta
                el([x0 - 0.3, 5.6, 6.6], [x0 + 4.8, 6.6, 9.4], "leather")]                 # correa
    return els


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
    model("night_vision_visor", visor_elements())
    model("jetpack_2", jetpack_level_elements(2))
    model("jetpack_3", jetpack_level_elements(3))
    jet = model("jetpack", jetpack_elements())
    jet["overrides"] = [{"predicate": {"tecnocinetico:level": 2}, "model": "tecnocinetico:item/jetpack_2"},
                        {"predicate": {"tecnocinetico:level": 3}, "model": "tecnocinetico:item/jetpack_3"}]
    (ROOT / "models/item/jetpack.json").write_text(json.dumps(jet, indent=2), encoding="utf-8")
    model("visor_battery", battery_elements())
    model("jetpack_fuel", fuel_elements())
    model("propulsor_cell", cell_elements())
    model("jetpack_kit_2", kit_elements(2))
    model("jetpack_kit_3", kit_elements(3))
    model("aqua_propulsor", propulsor_elements())
    print("ok: munición, cargadores y gadgets")
