"""Cámara de Reacción 2x2x2 (un solo modelo dibujado por el núcleo, que es la esquina delantera izquierda de abajo).

python tools/chamber_model_gen.py      (desde forge-1.20.1-47.4.10-mdk)
Usa el mismo sistema que tools/reactor_model_gen.py (coordenadas locales u, v, w; UV sin costuras) con su propio
atlas textures/block/reaction_chamber.png. Escribe reaction_chamber(_on).json (luz de estado), su estado, el
modelo del haz (reaction_beam.json) y el modelo del kit.
"""
import json
import math
import random
import sys
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
import reactor_model_gen as rm  # noqa: E402
from gunkit import resolve_zfight  # noqa: E402

A = rm.A
ZONE = 32
MATS = {"wall": (0, 0, 1, False), "frame": (1, 0, 1, False), "hazard": (2, 0, 1, False), "roof": (3, 0, 1, False),
        "glass": (0, 1, 0, True), "copper": (1, 1, 1, False), "screen": (2, 1, 0, True), "interior": (3, 1, 1, False),
        "cyan": (0, 2, 1, False), "base": (1, 2, 1, False), "coil": (2, 2, 1, False), "emitter": (3, 2, 0, True)}


def paint():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r = random.Random(9)

    def fill(name, base, noise=0.025, alpha=255):
        c, rr = MATS[name][:2]
        x0, y0 = c * ZONE, rr * ZONE
        for y in range(ZONE):
            for x in range(ZONE):
                k = 1 + r.uniform(-noise, noise)
                img.putpixel((x0 + x, y0 + y), tuple(max(0, min(255, int(v * k))) for v in base) + (alpha,))
        return x0, y0

    x0, y0 = fill("wall", (176, 182, 186))                                    # panel claro de laboratorio
    d.rectangle([x0, y0, x0 + 31, y0 + 31], outline=(120, 126, 132))
    d.line([(x0 + 1, y0 + 1), (x0 + 30, y0 + 1)], fill=(208, 212, 216))
    for (px, py) in ((3, 3), (28, 3), (3, 28), (28, 28)):
        d.point((x0 + px, y0 + py), fill=(120, 126, 132))
    fill("frame", (54, 58, 66))
    x0, y0 = MATS["hazard"][0] * ZONE, MATS["hazard"][1] * ZONE
    for y in range(ZONE):
        for x in range(ZONE):
            img.putpixel((x0 + x, y0 + y), ((222, 182, 42) if ((x + y) // 6) % 2 == 0 else (34, 34, 36)) + (255,))
    x0, y0 = fill("roof", (96, 100, 108))
    d.rectangle([x0 + 4, y0 + 4, x0 + 27, y0 + 27], outline=(70, 74, 80))
    x0, y0 = fill("glass", (150, 210, 225), 0.01, 70)
    d.line([(x0 + 4, y0 + 4), (x0 + 14, y0 + 4)], fill=(235, 250, 255, 180)); d.line([(x0 + 4, y0 + 4), (x0 + 4, y0 + 14)], fill=(235, 250, 255, 180))
    x0, y0 = MATS["copper"][0] * ZONE, MATS["copper"][1] * ZONE
    for y in range(ZONE):
        for x in range(ZONE):
            t = abs((y % 8) - 3.5) / 3.5
            img.putpixel((x0 + x, y0 + y), (int(206 - 80 * t), int(124 - 50 * t), int(72 - 34 * t), 255))
    x0, y0 = fill("screen", (46, 50, 56))
    d.rectangle([x0 + 2, y0 + 3, x0 + 20, y0 + 28], fill=(8, 24, 28), outline=(110, 116, 126))
    for i in range(16):                                                         # resonancia en pantalla
        d.point((x0 + 3 + i, y0 + 16 + int(8 * math.sin(i * 0.7) * math.exp(-i * 0.08))), fill=(90, 230, 120))
    for i, c in enumerate(((70, 215, 235), (230, 200, 60), (220, 60, 50))):
        d.ellipse([x0 + 23, y0 + 4 + i * 9, x0 + 29, y0 + 10 + i * 9], fill=c, outline=(30, 30, 30))   # diales
    fill("interior", (34, 38, 44))
    x0, y0 = MATS["cyan"][0] * ZONE, MATS["cyan"][1] * ZONE
    for y in range(ZONE):
        for x in range(ZONE):
            img.putpixel((x0 + x, y0 + y), (120, 240, 255, 255))
    fill("base", (60, 62, 66), 0.04)
    x0, y0 = MATS["coil"][0] * ZONE, MATS["coil"][1] * ZONE                    # bobinado
    for y in range(ZONE):
        for x in range(ZONE):
            t = abs((x % 4) - 1.5) / 1.5
            img.putpixel((x0 + x, y0 + y), (int(220 - 90 * t), int(140 - 60 * t), int(80 - 40 * t), 255))
    x0, y0 = fill("emitter", (40, 44, 52))
    d.ellipse([x0 + 6, y0 + 6, x0 + 25, y0 + 25], outline=(120, 240, 255), width=2)
    d.ellipse([x0 + 12, y0 + 12, x0 + 19, y0 + 19], fill=(200, 255, 255))
    img.save(A / "textures/block/reaction_chamber.png")


def geometry():
    b, wall = rm.box, rm.wall_with_openings
    g = [b([0, 0, 0], [2, 0.25, 2], "base"),
         b([0.05, 0.25, 0.05], [1.95, 0.33, 1.95], "hazard", up="frame")]
    for (u0, w0) in ((0, 0), (1.7, 0), (0, 1.7), (1.7, 1.7)):                     # columnas
        g.append(b([u0, 0.33, w0], [u0 + 0.3, 1.55, w0 + 0.3], "frame"))
    g += wall("u", 0.12, 0.3, 0.3, 1.7, 0.33, 1.55, [(0.55, 1.45, 0.62, 1.32)], "wall", "back", "front")   # frente con mirilla
    g += wall("u", 1.7, 1.88, 0.3, 1.7, 0.33, 1.55, [], "wall", "front", "back")
    g += wall("w", 0.12, 0.3, 0.3, 1.7, 0.33, 1.55, [], "wall", "right", "left")
    g += wall("w", 1.7, 1.88, 0.3, 1.7, 0.33, 1.55, [], "wall", "left", "right")
    g += [b([0.55, 0.62, 0.19], [1.45, 1.32, 0.23], "glass"),                  # mirilla y su marco
          b([0.5, 0.57, 0.06], [1.5, 0.62, 0.3], "frame"), b([0.5, 1.32, 0.06], [1.5, 1.37, 0.3], "frame"),
          b([0.5, 0.62, 0.06], [0.55, 1.32, 0.3], "frame"), b([1.45, 0.62, 0.06], [1.5, 1.32, 0.3], "frame"),
          b([0.45, 0.36, 0.05], [1.2, 0.54, 0.12], "frame", front="screen"),    # panel de control
          b([0.3, 0.33, 0.3], [1.7, 0.4, 1.7], "interior"),                   # suelo interior
          b([0.85, 0.4, 0.85], [1.15, 0.62, 1.15], "frame", up="cyan"),       # soporte de la muestra
          b([0.92, 1.38, 0.92], [1.08, 1.55, 1.08], "cyan"),                  # punta del emisor (se ve dentro)
          b([0, 1.55, 0], [2, 1.68, 2], "frame", up="roof"),                  # techo
          b([0.6, 1.68, 0.6], [1.4, 1.82, 1.4], "frame", up="emitter"),        # emisor
          b([0.5, 1.7, 0.5], [1.5, 1.76, 1.5], "coil"),                        # bobinas
          b([0.55, 1.78, 0.55], [1.45, 1.81, 1.45], "coil")]
    for u0 in (0.0, 1.9):                                                     # tuberías laterales
        g.append(b([u0, 0.5, 0.45], [u0 + 0.1, 1.5, 0.55], "copper"))
        g.append(b([u0, 0.5, 1.45], [u0 + 0.1, 1.5, 1.55], "copper"))
    return g


def elements(geom):
    out = []
    for bx in geom:
        f = [max(bx["f"][a], 0) for a in range(3)]
        t = [min(bx["t"][a], 2) for a in range(3)]
        cuts = [[f[a]] + [x for x in range(math.floor(f[a]) + 1, math.ceil(t[a])) if f[a] < x < t[a]] + [t[a]] for a in range(3)]
        for a0, a1 in zip(cuts[0], cuts[0][1:]):
            for b0, b1 in zip(cuts[1], cuts[1][1:]):
                for c0, c1 in zip(cuts[2], cuts[2][1:]):
                    e = rm.element(bx, (a0, b0, c0), (a1, b1, c1), 0, 0, 0)
                    if e:
                        out.append(e)
    return out


def main():
    paint()
    rm.MATS.clear()
    rm.MATS.update(MATS)                                                      # el sistema UV del reactor con este atlas
    els = resolve_zfight(elements(geometry()))
    for suffix, lamp in (("", "press_lamp_off"), ("_on", "press_lamp_on")):
        lamp_el = {"from": [-12.0, 6.0, 0.7], "to": [-9.6, 8.4, 1.0],         # luz de estado junto al panel (u 1.25..1.4)
                   "faces": {f: {"uv": [0, 0, 16, 16], "texture": "#lamp"} for f in ("north", "east", "west", "up")}}
        rm.jw(A / f"models/block/reaction_chamber{suffix}.json", {
            "parent": "minecraft:block/block", "render_type": "minecraft:translucent", "ambientocclusion": False,
            "textures": {"0": "tecnocinetico:block/reaction_chamber", "lamp": f"tecnocinetico:block/{lamp}",
                         "particle": "tecnocinetico:block/reaction_chamber"},
            "elements": els + [lamp_el],
            "display": {"gui": {"rotation": [30, 225, 0], "translation": [-2, -2, 0], "scale": [0.32, 0.32, 0.32]},
                        "ground": {"translation": [0, 3, 0], "scale": [0.15, 0.15, 0.15]},
                        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.2, 0.2, 0.2]},
                        "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.25, 0.25, 0.25]}}})
    variants = {}
    for facing, rot in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for powered in ("false", "true"):
            v = {"model": f"tecnocinetico:block/reaction_chamber{'_on' if powered == 'true' else ''}"}
            if rot:
                v["y"] = rot
            variants[f"facing={facing},powered={powered}"] = v
    rm.jw(A / "blockstates/reaction_chamber.json", {"variants": variants})
    rm.jw(A / "models/item/reaction_chamber.json", {"parent": "tecnocinetico:block/reaction_chamber"})
    rm.jw(A / "models/block/reaction_chamber_part.json", {"textures": {"particle": "tecnocinetico:block/reaction_chamber"}})
    rm.jw(A / "blockstates/reaction_chamber_part.json", {"variants": {f"facing={f}": {"model": "tecnocinetico:block/reaction_chamber_part"}
                                                                      for f in ("north", "east", "south", "west")}})
    # haz: columna fina cian (el renderizador la estira)
    c = MATS["cyan"]
    rm.jw(A / "models/block/reaction_beam.json", {"textures": {"0": "tecnocinetico:block/reaction_chamber", "particle": "tecnocinetico:block/reaction_chamber"},
                                                 "elements": [{"from": [7.2, 0, 7.2], "to": [8.8, 16, 8.8],
                                                               "faces": {f: {"uv": [c[0] * 4, c[1] * 4, c[0] * 4 + 1, c[1] * 4 + 4], "texture": "#0"}
                                                                         for f in ("north", "south", "east", "west")}}]})
    print(f"ok: cámara de reacción 2x2x2 ({len(els)} elementos)")


if __name__ == "__main__":
    main()
