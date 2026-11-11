"""Genera los rigs Java, texturas 128 px, vistas fieles y componentes exclusivos del caza."""
import json
from pathlib import Path
from PIL import Image, ImageDraw
from guardian_rig import generate_model

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/tecnocinetico"


def texture(name, watcher):
    img = Image.new("RGBA", (128, 128), (30, 34, 40, 255))
    glow = Image.new("RGBA", (128, 128))
    color = (60, 205, 235) if watcher else (95, 190, 125)
    for y in range(128):
        for x in range(128):
            if y < 38:
                base = (61, 79, 98) if watcher else (57, 68, 56)
                edge = x % 9 == 0 or y % 7 == 0
                c = (29, 36, 44) if edge else tuple(v + ((x*13+y*7)%5-2) for v in base)
                if x % 9 == 1 or y % 7 == 1: c = (110, 124, 134) if watcher else (101, 113, 91)
                if x % 9 == 2 and y % 7 == 2: c = (163, 129, 75)
            elif y < 64:
                c = (24, 29, 35) if x % 4 else (58, 67, 75)
            elif y < 88 and x < 64:
                c = (159, 94, 48) if y % 3 else (76, 47, 31)
            elif y < 88 and x >= 96:
                c = color if (x+y)%3 else tuple(min(255, v+40) for v in color)
                glow.putpixel((x, y), (*c, 255))
            elif y >= 88 and x < 64:
                c = (158, 167, 160) if watcher else (153, 155, 116)
                if y % 5 == 0: c = (90, 104, 105)
            elif y >= 88:
                c = (37, 47, 40) if (x+y)%4 else (75, 91, 59)
            else: c = (27, 32, 36)
            img.putpixel((x, y), (*c, 255))
    tex = ASSETS / "textures/entity"
    tex.mkdir(parents=True, exist_ok=True)
    img.save(tex / f"{name}.png"); glow.save(tex / f"{name}_glow.png")
    for suffix, color in [("charge", (255, 85, 40)), ("exposed", (235, 245, 255))]:
        layer = glow.copy()
        layer.putdata([(*color, p[3]) if p[3] else p for p in glow.get_flattened_data()])
        layer.save(tex / f"{name}_{suffix}.png")
    (ASSETS / f"models/item/{name}_spawn_egg.json").write_text('{"parent":"minecraft:item/template_spawn_egg"}\n', encoding="utf-8")


def components():
    for name in ("vector_thruster", "adaptive_flight_core"):
        im = Image.new("RGBA", (32, 32)); d = ImageDraw.Draw(im)
        if name == "vector_thruster":
            d.polygon([(8,5),(21,3),(27,11),(26,22),(16,28),(5,21),(4,12)], fill="#2f414e", outline="#a9bcc2")
            d.ellipse((7,7,25,25), fill="#18232e", outline="#c08049", width=2)
            d.ellipse((10,10,22,22), fill="#216a86", outline="#69ddec", width=2)
            for p in [((16,10),(18,15),(16,16)), ((22,16),(17,18),(16,16)), ((16,22),(14,17),(16,16)), ((10,16),(15,14),(16,16))]:
                d.polygon(p, fill="#a7d2d4")
            d.ellipse((14,14,18,18), fill="#e0f4ec")
            d.rectangle((3,12,6,20), fill="#a56b3d"); d.rectangle((25,9,28,17), fill="#a56b3d")
        else:
            d.polygon([(8,3),(24,3),(28,8),(28,24),(23,29),(7,29),(3,24),(3,8)], fill="#3f4c3b", outline="#a3ae8a")
            d.rectangle((7,7,24,25), fill="#172b26", outline="#ac824a", width=2)
            for x in (10,16,22):
                d.line((x,5,x,27), fill="#c39e5b")
                for y in (11,17,23): d.ellipse((x-2,y-2,x+2,y+2), fill="#4d8e64", outline="#90d3a0")
            d.rectangle((11,12,21,20), fill="#263b2c", outline="#b9d39b")
            d.line((13,16,15,14,17,18,19,15), fill="#aeeeb4", width=1)
        p = ASSETS / f"textures/item/{name}.png"; p.parent.mkdir(parents=True, exist_ok=True); im.save(p)
        (ASSETS / f"models/item/{name}.json").write_text(json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": f"tecnocinetico:item/{name}"}}), encoding="utf-8")


def main():
    texture("network_watcher", True); texture("swarm_matriarch", False)
    generate_model((ROOT / "tools/guardian_model.java.template").read_text(encoding="utf-8"))
    components()
    for locale, values in {
        "es_es": {"entity.tecnocinetico.network_watcher": "Vigía de la Red", "entity.tecnocinetico.swarm_matriarch": "Matriarca del Enjambre",
                  "item.tecnocinetico.network_watcher_spawn_egg": "Huevo de Vigía de la Red", "item.tecnocinetico.swarm_matriarch_spawn_egg": "Huevo de Matriarca del Enjambre",
                  "message.tecnocinetico.guardian_wake": "%s ha despertado.",
                  "item.tecnocinetico.vector_thruster": "Propulsor Vectorial", "item.tecnocinetico.adaptive_flight_core": "Núcleo de Vuelo Adaptativo",
                  "tooltip.tecnocinetico.vector_thruster": "Componente del Caza del Núcleo. Recuperado del Vigía de la Red.",
                  "tooltip.tecnocinetico.adaptive_flight_core": "Componente del Caza del Núcleo. Recuperado de la Matriarca del Enjambre."},
        "en_us": {"entity.tecnocinetico.network_watcher": "Network Watcher", "entity.tecnocinetico.swarm_matriarch": "Swarm Matriarch",
                  "item.tecnocinetico.network_watcher_spawn_egg": "Network Watcher Spawn Egg", "item.tecnocinetico.swarm_matriarch_spawn_egg": "Swarm Matriarch Spawn Egg",
                  "message.tecnocinetico.guardian_wake": "%s has awakened.",
                  "item.tecnocinetico.vector_thruster": "Vector Thruster", "item.tecnocinetico.adaptive_flight_core": "Adaptive Flight Core",
                  "tooltip.tecnocinetico.vector_thruster": "Core Fighter component. Recovered from the Network Watcher.",
                  "tooltip.tecnocinetico.adaptive_flight_core": "Core Fighter component. Recovered from the Swarm Matriarch."},
    }.items():
        p = ASSETS / f"lang/{locale}.json"
        data = json.loads(p.read_text(encoding="utf-8")); data.update(values)
        p.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
