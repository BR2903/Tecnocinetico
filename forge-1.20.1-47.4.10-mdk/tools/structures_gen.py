"""Estructuras grandes: Ensamblador Industrial (modelo 3x3x3 dibujado por el núcleo + brazo aparte) y Reactor de
Fisión 9x9x6 (texturas de las piezas, núcleo y barras de combustible), kits y estados.

python tools/structures_gen.py      (desde forge-1.20.1-47.4.10-mdk)
El modelo del ensamblador usa coordenadas -16..32: el núcleo es el bloque central de la estructura.
"""
import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

A = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tecnocinetico"
D = Path(__file__).resolve().parent.parent / "src/main/resources/data"
STEEL = [(24, 26, 31), (40, 43, 50), (58, 62, 70), (86, 92, 102), (130, 136, 146)]
CYAN = (70, 215, 235)


def jw(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def ramp(base, k):
    return tuple(max(0, min(255, int(v * k))) for v in base)


# =================================================================== Ensamblador Industrial
Z = 16
AZ = {"casing": (0, 0), "dark": (1, 0), "deck": (2, 0), "hazard": (3, 0), "table": (0, 1), "panel": (1, 1),
      "screen": (2, 1), "copper": (3, 1), "cyan": (0, 2), "light": (1, 2)}


def assembler_atlas():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r = random.Random(5)

    def zone(name):
        c, rr = AZ[name]
        return c * Z, rr * Z

    def plate(name, base, rivets=True, bevel=True):
        x0, y0 = zone(name)
        for y in range(Z):
            for x in range(Z):
                img.putpixel((x0 + x, y0 + y), ramp(base, 1 + r.uniform(-0.03, 0.03)) + (255,))
        if bevel:
            d.rectangle([x0, y0, x0 + Z - 1, y0 + Z - 1], outline=STEEL[0])
            d.line([(x0 + 1, y0 + 1), (x0 + Z - 2, y0 + 1)], fill=ramp(base, 1.4))
        if rivets:
            for (px, py) in ((2, 2), (Z - 3, 2), (2, Z - 3), (Z - 3, Z - 3)):
                d.point((x0 + px, y0 + py), fill=STEEL[4])

    plate("casing", (66, 72, 82))
    plate("dark", STEEL[1], rivets=False)
    # suelo: chapa estriada
    x0, y0 = zone("deck")
    plate("deck", (78, 82, 90), rivets=False, bevel=False)
    for y in range(0, Z, 4):
        for x in range(0, Z, 4):
            d.line([(x0 + x + (y // 4) % 2 * 2, y0 + y + 1), (x0 + x + (y // 4) % 2 * 2 + 1, y0 + y + 2)], fill=(110, 116, 124))
    # franjas de peligro
    x0, y0 = zone("hazard")
    for y in range(Z):
        for x in range(Z):
            img.putpixel((x0 + x, y0 + y), ((220, 180, 40) if ((x + y) // 4) % 2 == 0 else (32, 32, 34)) + (255,))
    # mesa de trabajo: plancha con marcas de posición
    x0, y0 = zone("table")
    plate("table", (92, 98, 108), rivets=False)
    d.rectangle([x0 + 4, y0 + 4, x0 + 11, y0 + 11], outline=(220, 180, 40))
    d.line([(x0 + 7, y0 + 2), (x0 + 7, y0 + 13)], fill=(150, 156, 166))
    d.line([(x0 + 2, y0 + 7), (x0 + 13, y0 + 7)], fill=(150, 156, 166))
    # pared de maquinaria: rejillas, armarios y luces
    x0, y0 = zone("panel")
    plate("panel", (56, 60, 68))
    for yy in range(3, 8):
        d.line([(x0 + 2, y0 + yy * 2 - 3), (x0 + 7, y0 + yy * 2 - 3)], fill=STEEL[0])
    d.rectangle([x0 + 9, y0 + 2, x0 + 13, y0 + 9], fill=STEEL[1], outline=STEEL[3])
    d.point((x0 + 10, y0 + 4), fill=(90, 230, 120)); d.point((x0 + 12, y0 + 4), fill=(230, 80, 60))
    d.rectangle([x0 + 2, y0 + 11, x0 + 13, y0 + 13], fill=STEEL[0])
    # pantalla de la consola
    x0, y0 = zone("screen")
    plate("screen", STEEL[1], rivets=False)
    d.rectangle([x0 + 2, y0 + 2, x0 + 13, y0 + 10], fill=(8, 24, 28), outline=STEEL[3])
    for i in range(9):
        d.point((x0 + 3 + i, y0 + 8 - int(3 * abs(math.sin(i * 0.8)))), fill=CYAN)
    for i, c in enumerate(((70, 215, 235), (220, 60, 50), (230, 200, 60), (90, 230, 120))):
        d.rectangle([x0 + 2 + i * 3, y0 + 12, x0 + 3 + i * 3, y0 + 13], fill=c)
    # cobre (tuberías)
    x0, y0 = zone("copper")
    for y in range(Z):
        for x in range(Z):
            t = abs((y % 4) - 1.5) / 1.5
            img.putpixel((x0 + x, y0 + y), (int(200 - 70 * t), int(120 - 45 * t), int(70 - 30 * t), 255))
    # cinetrita luminosa y luz blanca de trabajo
    x0, y0 = zone("cyan")
    for y in range(Z):
        for x in range(Z):
            dd = max(abs(x - 7.5), abs(y - 7.5)) / 8
            img.putpixel((x0 + x, y0 + y), (int(70 + 150 * (1 - dd)), int(215 + 40 * (1 - dd)), 240, 255))
    x0, y0 = zone("light")
    d.rectangle([x0, y0, x0 + Z - 1, y0 + Z - 1], fill=(240, 240, 220))
    d.rectangle([x0, y0, x0 + Z - 1, y0 + Z - 1], outline=(150, 150, 140))
    img.save(A / "textures/block/industrial_assembler.png")


def uv(name):
    c, r = AZ[name]
    return [c * 4, r * 4, c * 4 + 4, r * 4 + 4]


def box(fr, to, mat, **over):
    faces = {}
    for f in ("north", "south", "east", "west", "up", "down"):
        m = over.get(f, mat)
        if m == "#lamp":
            faces[f] = {"uv": [0, 0, 16, 16], "texture": "#lamp"}
        elif m:
            faces[f] = {"uv": uv(m), "texture": "#0"}
    return {"from": fr, "to": to, "faces": faces}


def assembler_elements():
    e = [box([-16, -16, -16], [32, -13, 32], "casing", up="deck"),                          # base
         box([-16, -13, -16], [32, -12, -14], "hazard", up="hazard"),                       # borde delantero
         box([-6, -13, -6], [22, -6, 22], "casing", up="table"),                             # mesa de trabajo
         box([-13, -13, 26], [29, 24, 29], "panel"),                                         # pared de maquinaria
         box([-12, 20, 24], [28, 22, 26], "copper"), box([-12, 15, 24], [28, 17, 26], "copper"),   # tuberías
         box([-15, -13, -13], [-9, 6, -3], "casing", north="screen", up="screen"),          # consola
         box([-14, 6, -12], [-12, 8, -10], "dark", up="#lamp", north="#lamp"),              # luz de estado
         box([-13, 26, 6], [29, 29, 10], "dark"),                                            # raíl de los brazos
         box([2, 29, 2], [14, 30, 14], "light")]                                             # foco de trabajo (bajo el pórtico)
    for (x, z) in ((-16, -16), (29, -16), (-16, 29), (29, 29)):                              # columnas
        e.append(box([x, -13, z], [x + 3, 32, z + 3], "casing", up="dark"))
    for z in (-16, 29):                                                                      # pórtico
        e.append(box([-13, 29, z], [29, 32, z + 3], "dark"))
    for x in (-16, 29):
        e.append(box([x, 29, -13], [x + 3, 32, 29], "dark"))
    e.append(box([-6, 29, 6], [22, 32, 10], "dark"))                                        # viga central del raíl
    for z in (-6, 21):                                                                       # topes de la mesa
        e.append(box([-6, -6, z], [22, -5, z + 1], "hazard"))
    return e


def arm_elements():
    """Brazo robótico (bloque 0..16, se cuelga del raíl): carro, segmento, articulación, cabezal y punta luminosa."""
    return [box([5, 12, 5], [11, 16, 11], "dark"),
            box([7, 5, 7], [9, 12, 9], "casing"),
            box([6.5, 3.5, 6.5], [9.5, 5.5, 9.5], "dark"),
            box([7.25, 0.5, 7.25], [8.75, 3.5, 8.75], "copper"),
            box([7.5, -0.5, 7.5], [8.5, 0.5, 8.5], "cyan")]


def machine_states(name, model_base):
    variants = {}
    for facing, rot in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for powered in ("false", "true"):
            v = {"model": f"tecnocinetico:block/{model_base}{'_on' if powered == 'true' else ''}"}
            if rot:
                v["y"] = rot
            variants[f"facing={facing},powered={powered}"] = v
    jw(A / f"blockstates/{name}.json", {"variants": variants})


def part_states(name, model):
    jw(A / f"blockstates/{name}.json", {"variants": {f"facing={f}": {"model": f"tecnocinetico:block/{model}"}
                                                    for f in ("north", "east", "south", "west")}})


# =================================================================== Reactor
def tex32(name, painter):
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    painter(img, ImageDraw.Draw(img))
    img.save(A / f"textures/block/{name}.png")


def casing(img, d, base=(92, 96, 100)):
    r = random.Random(11)
    for y in range(32):
        for x in range(32):
            img.putpixel((x, y), ramp(base, 1 + r.uniform(-0.03, 0.03) - 0.08 * (y / 32)) + (255,))
    d.rectangle([0, 0, 31, 31], outline=(40, 42, 46))
    d.line([(1, 1), (30, 1)], fill=ramp(base, 1.35)); d.line([(1, 1), (1, 30)], fill=ramp(base, 1.2))
    d.line([(2, 30), (30, 30)], fill=ramp(base, 0.6)); d.line([(30, 2), (30, 30)], fill=ramp(base, 0.6))
    for (x, y) in ((4, 4), (27, 4), (4, 27), (27, 27)):
        d.rectangle([x - 1, y - 1, x, y], fill=ramp(base, 1.5))
        d.point((x + 1, y + 1), fill=ramp(base, 0.5))
    d.line([(4, 16), (27, 16)], fill=ramp(base, 0.75))                                       # junta central


def port(img, d):
    casing(img, d)
    d.rectangle([7, 7, 24, 24], fill=(60, 62, 66), outline=(30, 30, 32))                     # escotilla
    d.rectangle([8, 8, 23, 9], fill=(110, 114, 120))
    d.ellipse([11, 11, 20, 20], fill=(220, 180, 40))                                         # radiación
    d.ellipse([14, 14, 17, 17], fill=(30, 30, 30))
    for a in (90, 210, 330):
        cx = 15.5 + 3.2 * math.cos(math.radians(a)); cy = 15.5 + 3.2 * math.sin(math.radians(a))
        d.ellipse([cx - 1.5, cy - 1.5, cx + 1.5, cy + 1.5], fill=(30, 30, 30))


def window(img, d):
    for y in range(32):
        for x in range(32):
            img.putpixel((x, y), (150, 200, 215, 80))
    d.rectangle([0, 0, 31, 31], outline=(70, 74, 80, 255), width=3)                           # marco grueso
    d.line([(6, 6), (12, 6)], fill=(235, 250, 255, 200)); d.line([(6, 6), (6, 12)], fill=(235, 250, 255, 200))


def vessel(img, d):
    for y in range(32):
        for x in range(32):
            img.putpixel((x, y), (90, 220, 170, 70))
    d.rectangle([0, 0, 31, 31], outline=(60, 90, 80, 255), width=2)
    for y in (10, 21):
        d.line([(2, y), (29, y)], fill=(60, 90, 80, 200))                                     # aros de la vasija
    d.line([(5, 4), (5, 27)], fill=(220, 255, 240, 170))


def vent(img, d):
    casing(img, d)
    d.ellipse([5, 5, 26, 26], fill=(30, 32, 36), outline=(130, 136, 146))
    for a in range(0, 360, 60):
        x = 15.5 + 9 * math.cos(math.radians(a)); y = 15.5 + 9 * math.sin(math.radians(a))
        d.line([(15.5, 15.5), (x, y)], fill=(100, 106, 116), width=2)                         # aspas del ventilador
    d.ellipse([13, 13, 18, 18], fill=(130, 136, 146))


def rod(img, d, glow):
    for y in range(32):
        for x in range(32):
            img.putpixel((x, y), (150, 152, 166, 255))                                       # revestimiento de titanio
    for y in range(4, 28, 6):
        d.rectangle([0, y, 31, y + 3], fill=glow)                                             # ventanas del combustible
    d.line([(8, 0), (8, 31)], fill=(210, 212, 226))


def reactor():
    tex32("reactor_casing", casing)
    tex32("reactor_port", port)
    tex32("reactor_window", window)
    tex32("reactor_vessel_glass", vessel)
    tex32("reactor_vent", vent)
    tex32("reactor_rod_active", lambda i, d: rod(i, d, (140, 245, 100)))
    tex32("reactor_rod_idle", lambda i, d: rod(i, d, (60, 80, 60)))
    for name, rt in (("reactor_casing", None), ("reactor_port", None), ("reactor_vent", None),
                     ("reactor_window", "minecraft:translucent"), ("reactor_vessel_glass", "minecraft:translucent")):
        m = {"parent": "minecraft:block/cube_all", "textures": {"all": f"tecnocinetico:block/{name}"}}
        if rt:
            m["render_type"] = rt
        jw(A / f"models/block/{name}.json", m)
        part_states(name, name)
    # barras (bloque de alto; el renderizador las estira)
    for name, tex in (("reactor_fuel_rod", "reactor_rod_active"), ("reactor_fuel_rod_idle", "reactor_rod_idle")):
        t = f"tecnocinetico:block/{tex}"
        jw(A / f"models/block/{name}.json", {"textures": {"0": t, "particle": t}, "elements": [
            {"from": [6.5, 0, 6.5], "to": [9.5, 16, 9.5], "faces": {f: {"uv": [0, 0, 6, 16], "texture": "#0"}
                                                                    for f in ("north", "south", "east", "west")}},
            {"from": [6, 15, 6], "to": [10, 16, 10], "faces": {f: {"uv": [0, 0, 4, 4], "texture": "#0"} for f in ("north", "south", "east", "west", "up")}}]})
    # núcleo: soporte de las barras con anillo luminoso (luz de estado)
    for suffix, lamp in (("", "press_lamp_off"), ("_on", "press_lamp_on")):
        jw(A / f"models/block/fission_reactor{suffix}.json", {
            "parent": "minecraft:block/block", "textures": {"0": "tecnocinetico:block/reactor_casing", "lamp": f"tecnocinetico:block/{lamp}",
                                                            "particle": "tecnocinetico:block/reactor_casing"},
            "elements": [
                {"from": [0, 0, 0], "to": [16, 12, 16], "faces": {f: {"uv": [0, 0, 16, 12], "texture": "#0"} for f in ("north", "south", "east", "west")}
                 | {"up": {"uv": [0, 0, 16, 16], "texture": "#0"}, "down": {"uv": [0, 0, 16, 16], "texture": "#0"}}},
                {"from": [5, 12, 5], "to": [11, 16, 11], "faces": {f: {"uv": [0, 0, 16, 16], "texture": "#lamp"} for f in ("north", "south", "east", "west", "up")}}]})
    machine_states("fission_reactor", "fission_reactor")
    # kit del reactor: caja de transporte con el símbolo
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([1, 3, 14, 14], fill=(110, 100, 60), outline=(50, 44, 26))
    d.line([(1, 8), (14, 8)], fill=(70, 62, 36))
    d.rectangle([5, 5, 10, 11], fill=(220, 180, 40))
    d.point((7, 8), fill=(30, 30, 30)); d.point((8, 8), fill=(30, 30, 30))
    d.point((7, 6), fill=(30, 30, 30)); d.point((6, 10), fill=(30, 30, 30)); d.point((9, 10), fill=(30, 30, 30))
    d.rectangle([2, 1, 13, 3], fill=(140, 130, 80), outline=(50, 44, 26))
    img.save(A / "textures/item/fission_reactor_kit.png")
    jw(A / "models/item/fission_reactor_kit.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "tecnocinetico:item/fission_reactor_kit"}})


def main():
    assembler_atlas()
    for suffix, lamp in (("", "press_lamp_off"), ("_on", "press_lamp_on")):
        jw(A / f"models/block/industrial_assembler{suffix}.json", {
            "parent": "minecraft:block/block", "texture_size": [64, 64],
            "textures": {"0": "tecnocinetico:block/industrial_assembler", "lamp": f"tecnocinetico:block/{lamp}",
                         "particle": "tecnocinetico:block/industrial_assembler"},
            "elements": assembler_elements(),
            "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.2, 0.2, 0.2]},
                        "ground": {"translation": [0, 3, 0], "scale": [0.1, 0.1, 0.1]},
                        "fixed": {"scale": [0.25, 0.25, 0.25]},
                        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.12, 0.12, 0.12]},
                        "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.15, 0.15, 0.15]}}})
    machine_states("industrial_assembler", "industrial_assembler")
    jw(A / "models/block/industrial_assembler_arm.json", {"textures": {"0": "tecnocinetico:block/industrial_assembler",
                                                                      "particle": "tecnocinetico:block/industrial_assembler"},
                                                          "texture_size": [64, 64], "elements": arm_elements()})
    jw(A / "models/item/industrial_assembler.json", {"parent": "tecnocinetico:block/industrial_assembler"})
    jw(A / "models/block/assembler_part.json", {"textures": {"particle": "tecnocinetico:block/industrial_assembler"}})
    part_states("assembler_part", "assembler_part")
    reactor()
    (D / "tecnocinetico/loot_tables/blocks/fission_reactor.json").unlink(missing_ok=True)   # el núcleo suelta el kit (en código)
    (A / "models/item/fission_reactor.json").unlink(missing_ok=True)
    (A / "textures/block/fission_reactor_front.png").unlink(missing_ok=True)
    print("ok: ensamblador industrial y reactor")


if __name__ == "__main__":
    main()
