"""Armas tier 1 en 3D: textura atlas textures/item/weapons_t1.png (64x64, zonas de 8x8) y modelos de
pipe_shotgun (+ _open), bolt_rifle (+ _bolt), scrap_machete, shotgun_shell y rifle_round; modelos planos de la
armadura de chatarra (iconos de tools/armor_gen.py).

Uso (desde forge-1.20.1-47.4.10-mdk):
    python tools/weapons_gen.py
    python tools/gui_fit.py $A "y-90;z25" pipe_shotgun pipe_shotgun_open      (armas largas, como la pistola)
    python tools/gui_fit.py $A "y-90;z25" bolt_rifle bolt_rifle_bolt
    python tools/gui_fit.py $A "y-135;x30" shotgun_shell ;  ... rifle_round
    python tools/weapons_gen.py   (otra vez: conserva display.gui)
Las armas largas usan las coordenadas de empuñadura y miras de la pistola (cañón hacia -Z, empuñadura en
z≈9..12, miras a y≈14-15) para compartir la animación en primera persona.
"""
import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tecnocinetico"
TEX = "tecnocinetico:item/weapons_t1"

ZONES = {"steel_dark": (0, 0), "steel": (1, 0), "steel_hi": (2, 0), "rust": (3, 0), "wood": (4, 0), "wood_dark": (5, 0),
         "leather": (6, 0), "copper": (7, 0), "brass": (0, 1), "shell_red": (1, 1), "cyan": (2, 1), "rubber": (3, 1),
         "blade": (4, 1), "edge": (5, 1), "tape": (6, 1), "black": (7, 1)}


def paint():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))

    def zone(name, fn):
        cx, cy = ZONES[name]
        for y in range(8):
            for x in range(8):
                img.putpixel((cx * 8 + x, cy * 8 + y), (*fn(x, y), 255))

    zone("steel_dark", lambda x, y: (52, 54, 60) if y == 0 else (34, 36, 40) if y < 7 else (22, 23, 26))
    zone("steel", lambda x, y: (96, 100, 108) if y == 0 else (70, 74, 80) if (x + y) % 6 else (60, 64, 70))
    zone("steel_hi", lambda x, y: (176, 180, 186) if x in (2, 3) else (130, 134, 140))
    zone("rust", lambda x, y: (160, 88, 46) if (x * 3 + y) % 5 == 0 else (124, 62, 32) if (x + y) % 3 else (96, 48, 26))
    zone("wood", lambda x, y: (128, 86, 52) if y % 3 == 0 else (110, 72, 42) if (x + 2 * y) % 7 else (92, 60, 34))
    zone("wood_dark", lambda x, y: (84, 54, 32) if y % 2 else (70, 44, 26))
    zone("leather", lambda x, y: (82, 56, 36) if y % 3 else (58, 38, 24))
    zone("copper", lambda x, y: (214, 150, 98) if y == 0 else (168, 106, 62) if y < 6 else (110, 64, 36))
    zone("brass", lambda x, y: (236, 204, 120) if x in (2, 3) else (196, 160, 80) if x < 6 else (150, 116, 54))
    zone("shell_red", lambda x, y: (190, 52, 40) if x in (2, 3) else (150, 36, 30) if x < 6 else (110, 26, 22))
    zone("cyan", lambda x, y: (180, 248, 255) if (x, y) == (2, 2) else (70, 210, 226) if x + y < 9 else (30, 130, 146))
    zone("rubber", lambda x, y: (30, 30, 32) if (x + y) % 2 else (22, 22, 24))
    zone("blade", lambda x, y: (150, 154, 160) if y % 4 == 0 else (120, 124, 130))
    zone("edge", lambda x, y: (220, 224, 228) if x < 4 else (190, 194, 200))
    zone("tape", lambda x, y: (120, 120, 116) if y % 2 else (96, 96, 92))
    zone("black", lambda x, y: (8, 8, 10))
    return img


def uv(name):
    cx, cy = ZONES[name]
    u, v = cx * 2, cy * 2                                     # textura 64 px, UV 0..16: 1 unidad = 4 px
    return [u, v, u + 2, v + 2]


def el(fr, to, mat, side=None, rot=None, front=None):
    side = side or mat
    e = {"from": fr, "to": to, "faces": {f: {"uv": uv(mat if f in ("up", "down") else side), "texture": "#0"}
                                         for f in ("north", "south", "east", "west", "up", "down")}}
    if front:
        e["faces"]["north"] = {"uv": uv(front), "texture": "#0"}
    if rot:
        e["rotation"] = rot
    return e


GRIP_ROT = {"angle": -22.5, "axis": "x", "origin": [8, 11, 11]}


def trigger_group():
    return [el([7.6, 9.4, 7.5], [8.4, 9.8, 10.6], "steel_dark"), el([7.6, 9.4, 7.3], [8.4, 11.0, 7.7], "steel_dark"),
            el([7.85, 9.9, 8.6], [8.15, 11.0, 9.0], "steel_hi")]


# ---------------------------------------------------------------- escopeta de caño (dos cañones, basculante)
def shotgun(open_=False):
    hinge = {"angle": -22.5, "axis": "x", "origin": [8, 11.7, 6]} if open_ else None     # los cañones bajan
    barrels = [el([7.0, 12.4, -12], [7.95, 13.4, 6], "steel", front="black", rot=hinge),
               el([8.05, 12.4, -12], [9.0, 13.4, 6], "steel", front="black", rot=hinge),
               el([7.8, 13.4, -12], [8.2, 13.6, 6], "steel_hi", rot=hinge),                   # banda superior
               el([7.9, 13.6, -11.8], [8.1, 13.9, -11.4], "brass", rot=hinge),               # punto de mira
               el([6.9, 12.3, -6.4], [9.1, 13.5, -5.4], "copper", rot=hinge),                # abrazaderas
               el([6.9, 12.3, 1.0], [9.1, 13.5, 2.0], "tape", rot=hinge),
               el([7.1, 11.3, -4.0], [8.9, 12.4, 4.0], "wood", rot=hinge)]                   # guardamanos
    action = [el([6.9, 11.0, 6.0], [9.1, 13.6, 9.5], "steel_dark"),                           # báscula
              el([6.8, 11.4, 5.6], [9.2, 12.0, 6.4], "steel_hi"),                             # eje de la bisagra
              el([7.6, 13.6, 8.6], [8.4, 14.2, 9.4], "steel_dark")]                           # llave de apertura
    stock = [el([7.1, 7.6, 9.4], [8.9, 11.0, 12.6], "wood", rot=GRIP_ROT),                    # empuñadura
             el([7.05, 8.6, 9.6], [8.95, 9.4, 12.4], "tape", rot=GRIP_ROT),                   # cinta en la empuñadura
             el([7.0, 8.6, 12.0], [9.0, 12.0, 21.0], "wood"),                                  # culata
             el([7.2, 12.0, 13.0], [8.8, 12.6, 20.0], "wood_dark"),                            # lomo
             el([6.9, 8.0, 21.0], [9.1, 12.2, 22.0], "rubber")]                                # cantonera
    return barrels + action + stock + trigger_group()


# ---------------------------------------------------------------- rifle de cerrojo
def rifle(bolt_back=False):
    els = [el([7.6, 12.7, -15.4], [8.4, 13.5, 4.0], "steel_dark", front="black"),              # cañón (límite de Minecraft: z >= -16)
           el([7.5, 12.6, -16.0], [8.5, 13.6, -15.3], "steel"),                                # brocal
           el([7.9, 13.5, -15.0], [8.1, 14.4, -14.5], "steel_hi"),                             # punto de mira
           el([7.5, 13.8, 3.0], [7.8, 14.5, 3.6], "steel"), el([8.2, 13.8, 3.0], [8.5, 14.5, 3.6], "steel"),  # alza
           el([7.3, 12.2, 4.0], [8.7, 14.0, 11.0], "steel_dark"),                              # cajón de mecanismos
           el([7.4, 11.0, 6.0], [8.6, 12.2, 9.0], "steel"),                                    # depósito
           el([7.1, 11.6, -9.0], [8.9, 13.0, 4.0], "wood"),                                    # guardamanos
           el([7.0, 11.9, -2.0], [9.0, 12.5, -1.4], "copper"),                                 # anilla
           el([7.2, 8.0, 10.5], [8.8, 12.2, 13.5], "wood", rot=GRIP_ROT),                      # empuñadura
           el([7.1, 9.0, 13.0], [8.9, 12.4, 23.0], "wood"),                                    # culata
           el([7.3, 12.4, 14.0], [8.7, 13.0, 21.0], "wood_dark"),                              # carrillera
           el([7.0, 8.2, 23.0], [9.0, 12.6, 23.6], "steel_dark"),                              # cantonera
           el([7.7, 11.35, -6.0], [8.3, 11.6, -2.0], "cyan")]                                  # bobina cinética (bajo el guardamanos)
    if bolt_back:                                                                             # cerrojo atrás y manilla arriba
        els += [el([8.7, 13.0, 9.0], [9.1, 13.6, 13.0], "steel_hi"),
                el([8.9, 13.0, 12.0], [9.4, 14.6, 12.6], "steel_hi"), el([8.8, 14.4, 11.9], [9.6, 15.1, 12.7], "steel")]
    else:
        els += [el([8.7, 13.0, 7.0], [9.1, 13.6, 11.0], "steel_hi"),
                el([9.1, 12.6, 10.0], [10.4, 13.0, 10.6], "steel_hi"), el([10.2, 12.2, 9.9], [10.8, 12.9, 10.7], "steel")]
    return els + trigger_group()


# ---------------------------------------------------------------- machete (como una espada: diagonal en el plano XY)
def machete():
    d = {"angle": -45, "axis": "z", "origin": [8, 8, 8]}                                      # mango abajo-izq., punta arriba-der.
    return [el([7.3, 0.5, 7.4], [8.7, 5.0, 8.6], "tape", rot=d),                               # mango encintado
            el([7.25, 1.2, 7.35], [8.75, 1.8, 8.65], "copper", rot=d), el([7.25, 3.4, 7.35], [8.75, 4.0, 8.65], "copper", rot=d),
            el([6.6, 5.0, 7.3], [9.6, 5.6, 8.7], "steel", rot=d),                              # guarda
            el([8.2, 5.6, 7.75], [9.3, 15.0, 8.25], "rust", rot=d),                            # lomo
            el([6.7, 5.6, 7.8], [8.2, 15.4, 8.2], "blade", rot=d),                             # hoja
            el([6.3, 6.0, 7.85], [6.7, 15.4, 8.15], "edge", rot=d),                            # filo
            el([6.7, 15.4, 7.8], [8.6, 16.0, 8.2], "blade", rot=d),                            # punta
            el([8.0, 8.0, 7.7], [8.2, 13.0, 8.3], "cyan", rot=d)]                              # vena de cinetrita


def shell():
    return [el([7.2, 2.0, 7.2], [8.8, 3.4, 8.8], "brass"), el([7.3, 3.4, 7.3], [8.7, 10.0, 8.7], "shell_red"),
            el([7.4, 10.0, 7.4], [8.6, 10.3, 8.6], "wood_dark")]


def rifle_round():
    return [el([7.4, 2.0, 7.4], [8.6, 2.5, 8.6], "brass"), el([7.5, 2.5, 7.5], [8.5, 9.0, 8.5], "brass"),
            el([7.6, 9.0, 7.6], [8.4, 10.6, 8.4], "copper"), el([7.75, 10.6, 7.75], [8.25, 11.6, 8.25], "copper"),
            el([7.45, 8.6, 7.45], [8.55, 9.0, 8.55], "cyan")]


GUN_DISPLAY = {"thirdperson_righthand": {"translation": [0, -1.5, 0]}, "thirdperson_lefthand": {"translation": [0, -1.5, 0]},
               "firstperson_righthand": {"translation": [-1, 0, -3.25]}, "firstperson_lefthand": {"translation": [-0.5, 0, -3]},
               "ground": {"translation": [0, -2, 0], "scale": [0.5, 0.5, 0.5]}, "fixed": {"rotation": [0, 90, 0], "scale": [0.6, 0.6, 0.6]}}
HANDHELD = {"thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4, 0.5], "scale": [0.85, 0.85, 0.85]},
            "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4, 0.5], "scale": [0.85, 0.85, 0.85]},
            "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
            "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
            "ground": {"translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]}, "fixed": {"rotation": [0, 180, 0]}}
SMALL = {"thirdperson_righthand": {"translation": [0, 2, 1], "scale": [0.5, 0.5, 0.5]},
         "firstperson_righthand": {"rotation": [0, -90, 15], "translation": [1, 3, 0], "scale": [0.6, 0.6, 0.6]},
         "ground": {"translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]}}


def model(name, elements, display, overrides=None):
    path = ROOT / "models/item" / f"{name}.json"
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    data = {"credit": "Tecnocinetico - weapons_gen.py", "texture_size": [64, 64],
            "textures": {"0": TEX, "particle": TEX}, "elements": elements, "display": dict(display)}
    if "gui" in old.get("display", {}):
        data["display"]["gui"] = old["display"]["gui"]
    if overrides:
        data["overrides"] = overrides
    path.write_text(json.dumps(data, indent=2), encoding="utf-8")


if __name__ == "__main__":
    paint().save(ROOT / "textures/item/weapons_t1.png")
    model("pipe_shotgun_open", shotgun(True), GUN_DISPLAY)
    model("pipe_shotgun", shotgun(False), GUN_DISPLAY,
          [{"predicate": {"tecnocinetico:reload": 1}, "model": "tecnocinetico:item/pipe_shotgun_open"}])
    model("bolt_rifle_bolt", rifle(True), GUN_DISPLAY)
    model("bolt_rifle", rifle(False), GUN_DISPLAY,
          [{"predicate": {"tecnocinetico:reload": 1}, "model": "tecnocinetico:item/bolt_rifle_bolt"}])
    model("scrap_machete", machete(), HANDHELD)
    model("shotgun_shell", shell(), SMALL)
    model("rifle_round", rifle_round(), SMALL)
    for piece in ("helmet", "chestplate", "leggings", "boots"):
        (ROOT / "models/item" / f"scrap_{piece}.json").write_text(json.dumps(
            {"parent": "minecraft:item/generated", "textures": {"layer0": f"tecnocinetico:item/scrap_{piece}"}}, indent=2), encoding="utf-8")
    print("ok: armas tier 1")
