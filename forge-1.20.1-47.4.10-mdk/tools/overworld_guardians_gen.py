"""Texturas propias 128 px y modelos de previsualización de Vigía/Matriarca (geometría de OverworldGuardianModel).
Ejecutar desde el proyecto. Las previsualizaciones van a build/guardian_previews, no al juego.
"""
import json
from pathlib import Path
from PIL import Image
from turret_texture import box

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/tecnocinetico"
OUT = ROOT / "build/guardian_previews"


def build(name, watcher):
    img = Image.new("RGBA", (128, 128), (35, 40, 48, 255))
    glow = Image.new("RGBA", (128, 128))
    cyan = (65, 220, 245) if watcher else (115, 235, 150)

    def armor(face, x, y, w, h):
        if y == 0 or x == 0: return (115, 130, 144)
        if y == h - 1 or x % 7 == 0: return (30, 37, 45)
        if y == 2 and x % 5 == 2: return cyan, True
        if y == h - 3 and face in ("left", "right"): return (210, 157, 35) if x % 6 < 3 else (24, 28, 33)
        return (65, 79, 93) if watcher else (59, 78, 68)

    def copper(f, x, y, w, h): return (157, 93, 52) if y % 3 else (75, 48, 34)
    box(img, glow, 0, 0, *( (28, 10, 24) if watcher else (22, 14, 28) ), armor)
    if watcher:
        box(img, glow, 48, 40, 2, 2, 20, copper)
        box(img, glow, 0, 60, 16, 1, 2, lambda f, x, y, w, h: (157, 165, 166))
    else:
        box(img, glow, 0, 44, 14, 10, 14, armor)
        box(img, glow, 0, 80, 2, 3, 17, copper)
        box(img, glow, 40, 80, 2, 14, 2, armor)
    box(img, glow, 64, 44, 2, 3, 12, lambda f, x, y, w, h: (25, 30, 36) if y else (116, 125, 133))
    box(img, glow, 0, 104, 6, 4, 2, lambda f, x, y, w, h: (cyan, True))
    tex = ASSETS / "textures/entity"
    tex.mkdir(parents=True, exist_ok=True)
    img.save(tex / f"{name}.png")
    glow.save(tex / f"{name}_glow.png")
    for suffix, color in [("charge", (255, 80, 40)), ("exposed", (240, 250, 255))]:
        layer = glow.copy()
        layer.putdata([(*color, p[3]) if p[3] else p for p in glow.getdata()])
        layer.save(tex / f"{name}_{suffix}.png")
    # Vista estática fiel a las piezas principales, con UV del atlas Java.
    elements = []

    def cube(x, y, z, w, h, d, u, v):
        faces = {
            "west": [u, v+d, u+d, v+d+h], "north": [u+d, v+d, u+d+w, v+d+h],
            "east": [u+d+w, v+d, u+2*d+w, v+d+h], "south": [u+2*d+w, v+d, u+2*d+2*w, v+d+h],
            "up": [u+d, v, u+d+w, v+d], "down": [u+d+w, v, u+d+2*w, v+d],
        }
        elements.append({"from": [x+8, 24-y-h, z+8], "to": [x+w+8, 24-y, z+d+8],
                         "faces": {f: {"texture": "#all", "uv": [n/8 for n in uv]} for f, uv in faces.items()}})
    if watcher:
        cube(-14, 5, -12, 28, 10, 24, 0, 0)
        cube(-3, 8, -14, 6, 4, 2, 0, 104)
        cube(-1, 15, -16, 2, 3, 12, 64, 44)
        for x in (-18, 18):
            for z in (-18, 18):
                cube(min(x, 0), 9, z, abs(x)+1, 2, 2, 48, 40)
                cube(x-8, 7, z-1, 16, 1, 2, 0, 60)
    else:
        cube(-11, 1, -10, 22, 14, 28, 0, 0)
        cube(-7, 4, -18, 14, 10, 14, 0, 44)
        cube(-3, 6, -20, 6, 4, 2, 0, 104)
        for x in (-6, 6): cube(x-1, 11, -28, 2, 3, 12, 64, 44)
        for side in (-1, 1):
            for z in (-6, 2, 10):
                cube(-25 if side < 0 else 8, 9, z, 17, 3, 2, 0, 80)
                cube(side*25-1, 10, z, 2, 14, 2, 40, 80)
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / f"{name}.json").write_text(json.dumps({"textures": {"all": f"tecnocinetico:entity/{name}"}, "elements": elements}), encoding="utf-8")
    (ASSETS / f"models/item/{name}_spawn_egg.json").write_text('{"parent":"minecraft:item/template_spawn_egg"}\n', encoding="utf-8")


def main():
    build("network_watcher", True)
    build("swarm_matriarch", False)
    for locale, values in {
        "es_es": {"entity.tecnocinetico.network_watcher": "Vigía de la Red", "entity.tecnocinetico.swarm_matriarch": "Matriarca del Enjambre",
                  "item.tecnocinetico.network_watcher_spawn_egg": "Huevo de Vigía de la Red", "item.tecnocinetico.swarm_matriarch_spawn_egg": "Huevo de Matriarca del Enjambre",
                  "message.tecnocinetico.guardian_wake": "%s ha despertado."},
        "en_us": {"entity.tecnocinetico.network_watcher": "Network Watcher", "entity.tecnocinetico.swarm_matriarch": "Swarm Matriarch",
                  "item.tecnocinetico.network_watcher_spawn_egg": "Network Watcher Spawn Egg", "item.tecnocinetico.swarm_matriarch_spawn_egg": "Swarm Matriarch Spawn Egg",
                  "message.tecnocinetico.guardian_wake": "%s has awakened."},
    }.items():
        p = ASSETS / f"lang/{locale}.json"
        data = json.loads(p.read_text(encoding="utf-8"))
        data.update(values)
        p.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
