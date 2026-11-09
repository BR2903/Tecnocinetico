"""Laboratorio Químico en 3D (no es un cubo): armario con panel de control, dos tanques de vidrio (ácido y
refrigerante), matraz central, tuberías de cobre con válvula y luz de estado.

python tools/lab_model_gen.py
Genera textures/block/chemical_lab.png (atlas 64x64 en zonas de 16x16), models/block/chemical_lab(_on).json,
blockstates/chemical_lab.json y models/item/chemical_lab.json. El bloque usa noOcclusion (ElectricMachineBlock con
fullCube = false).
"""
import json
import random
from pathlib import Path

from PIL import Image, ImageDraw

A = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tecnocinetico"
Z = 16                                              # px por zona; en UV de 16 = 4 unidades


def jw(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


# =================================================================== atlas
def paint():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r = random.Random(77)
    steel = [(24, 26, 31), (40, 43, 50), (58, 62, 70), (86, 92, 102), (130, 136, 146)]

    def zone(c, rr):
        return c * Z, rr * Z

    def plate(c, rr, base=2, rivets=True):
        x0, y0 = zone(c, rr)
        for y in range(Z):
            for x in range(Z):
                k = steel[base]
                v = 1.0 + r.uniform(-0.03, 0.03)
                img.putpixel((x0 + x, y0 + y), tuple(int(k[i] * v) for i in range(3)) + (255,))
        d.rectangle([x0, y0, x0 + Z - 1, y0 + Z - 1], outline=steel[0])
        d.line([(x0 + 1, y0 + 1), (x0 + Z - 2, y0 + 1)], fill=steel[4])
        d.line([(x0 + 1, y0 + 1), (x0 + 1, y0 + Z - 2)], fill=steel[3])
        if rivets:
            for (px, py) in ((3, 3), (Z - 4, 3), (3, Z - 4), (Z - 4, Z - 4)):
                d.point((x0 + px, y0 + py), fill=steel[4])
                d.point((x0 + px + 1, y0 + py + 1), fill=steel[0])

    plate(0, 0)                                                    # armario
    plate(1, 0, base=1, rivets=False)                              # acero oscuro (tapas, anillos)
    # panel de control frontal: pantalla con curva, botones y franja de peligro
    x0, y0 = zone(2, 0)
    plate(2, 0)
    d.rectangle([x0 + 2, y0 + 2, x0 + 9, y0 + 7], fill=(10, 26, 30), outline=steel[3])
    for i, yy in enumerate((6, 5, 5, 4, 3, 3)):
        d.point((x0 + 3 + i, y0 + yy), fill=(90, 230, 120))
    for i, c in enumerate(((70, 215, 235), (220, 60, 50), (230, 200, 60))):
        d.rectangle([x0 + 11, y0 + 2 + i * 2, x0 + 13, y0 + 3 + i * 2], fill=c)
    for x in range(Z - 2):
        c = (220, 180, 40) if ((x // 2) % 2 == 0) else (30, 30, 30)
        d.line([(x0 + 1 + x, y0 + 11), (x0 + 1 + x, y0 + 13)], fill=c)
    # rejilla de ventilación
    x0, y0 = zone(3, 0)
    plate(3, 0, rivets=False)
    for yy in range(3, Z - 2, 2):
        d.line([(x0 + 3, y0 + yy), (x0 + Z - 4, y0 + yy)], fill=steel[0])
    # vidrio: casi transparente, con reflejo vertical
    x0, y0 = zone(0, 1)
    for y in range(Z):
        for x in range(Z):
            a = 70
            c = (190, 225, 235)
            if x in (2, 3):
                c, a = (240, 252, 255), 150                        # reflejo
            if x in (0, Z - 1) or y in (0, Z - 1):
                c, a = (150, 175, 185), 170                        # borde
            img.putpixel((x0 + x, y0 + y), c + (a,))

    def liquid(c, rr, col):
        x0, y0 = zone(c, rr)
        for y in range(Z):
            for x in range(Z):
                k = 1.0 + (0.12 if y < 2 else 0) - 0.15 * (y / Z) + r.uniform(-0.04, 0.04)
                img.putpixel((x0 + x, y0 + y), tuple(min(255, int(col[i] * k)) for i in range(3)) + (225,))
        for _ in range(5):                                         # burbujas
            bx, by = r.randint(2, Z - 3), r.randint(3, Z - 3)
            img.putpixel((x0 + bx, y0 + by), (245, 255, 235, 240))

    liquid(1, 1, (205, 210, 70))                                   # ácido
    liquid(2, 1, (70, 190, 235))                                   # refrigerante
    liquid(3, 2, (110, 225, 110))                                  # mezcla del matraz
    # cobre
    x0, y0 = zone(3, 1)
    for y in range(Z):
        for x in range(Z):
            t = abs((y % 4) - 1.5) / 1.5
            c = (int(200 - 70 * t), int(120 - 45 * t), int(70 - 30 * t))
            img.putpixel((x0 + x, y0 + y), c + (255,))
    # tapa superior con escotilla
    x0, y0 = zone(0, 2)
    plate(0, 2)
    d.ellipse([x0 + 4, y0 + 4, x0 + 11, y0 + 11], outline=steel[0], fill=steel[1])
    d.line([(x0 + 5, y0 + 7), (x0 + 10, y0 + 7)], fill=steel[3])
    # válvula roja
    x0, y0 = zone(1, 2)
    d.rectangle([x0, y0, x0 + Z - 1, y0 + Z - 1], fill=(170, 40, 34))
    d.rectangle([x0 + 2, y0 + 2, x0 + Z - 3, y0 + Z - 3], outline=(220, 80, 60))
    img.save(A / "textures/block/chemical_lab.png")


# =================================================================== modelo
ZONES = {"casing": (0, 0), "dark": (1, 0), "panel": (2, 0), "vent": (3, 0), "glass": (0, 1), "acid": (1, 1),
         "coolant": (2, 1), "copper": (3, 1), "top": (0, 2), "valve": (1, 2), "mix": (3, 2)}
FACES = ("north", "south", "east", "west", "up", "down")


def uv(zone):
    c, r = ZONES[zone]
    return [c * 4, r * 4, c * 4 + 4, r * 4 + 4]


def el(fr, to, mat, **over):
    faces = {}
    for f in FACES:
        m = over.get(f, mat)
        if m is None:
            continue
        if m == "#lamp":
            faces[f] = {"uv": [0, 0, 16, 16], "texture": "#lamp"}
        else:
            faces[f] = {"uv": uv(m), "texture": "#0"}
    return {"from": fr, "to": to, "faces": faces}


def elements():
    e = [el([0, 0, 0], [16, 8, 16], "casing", north="panel", south="vent", up="top"),          # armario
         el([12.5, 8, 1], [14.5, 9, 3], "dark", up="#lamp", north="#lamp")]                     # luz de estado
    for (x0, liq) in ((1.5, "acid"), (9.5, "coolant")):                                       # tanques
        e += [el([x0 - 0.5, 8, 7.5], [x0 + 5.5, 8.8, 14.5], "dark"),                          # anillo inferior
              el([x0, 8.8, 8], [x0 + 5, 15, 14], "glass"),                                    # vidrio
              el([x0 + 0.4, 8.81, 8.4], [x0 + 4.6, 13, 13.6], liq, up=liq),                    # líquido
              el([x0 - 0.5, 15, 7.5], [x0 + 5.5, 16, 14.5], "dark")]                          # tapa
    e += [el([6.6, 8, 2.4], [9.4, 12, 5.6], "glass"),                                         # matraz
          el([6.9, 8.01, 2.7], [9.1, 10.2, 5.3], "mix"),
          el([7.4, 12, 3.4], [8.6, 14, 4.6], "glass"),                                        # cuello
          el([3.5, 16, 10.5], [12.5, 16.8, 11.5], "copper"),                                  # tubería entre tanques
          el([7.6, 16, 4.0], [8.4, 16.8, 10.5], "copper"),                                    # hacia el matraz
          el([7.6, 14, 3.6], [8.4, 16, 4.4], "copper"),
          el([7.2, 16.8, 6.6], [8.8, 17.2, 8.2], "valve")]                                    # válvula
    return e


def main():
    paint()
    for suffix, lamp in (("", "press_lamp_off"), ("_on", "press_lamp_on")):
        jw(A / f"models/block/chemical_lab{suffix}.json", {
            "parent": "minecraft:block/block", "render_type": "minecraft:translucent", "texture_size": [64, 64],
            "textures": {"0": "tecnocinetico:block/chemical_lab", "lamp": f"tecnocinetico:block/{lamp}",
                         "particle": "tecnocinetico:block/chemical_lab"},
            "elements": elements()})
    variants = {}
    for facing, rot in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for powered in ("false", "true"):
            v = {"model": f"tecnocinetico:block/chemical_lab{'_on' if powered == 'true' else ''}"}
            if rot:
                v["y"] = rot
            variants[f"facing={facing},powered={powered}"] = v
    jw(A / "blockstates/chemical_lab.json", {"variants": variants})
    jw(A / "models/item/chemical_lab.json", {"parent": "tecnocinetico:block/chemical_lab"})
    (A / "textures/block/chemical_lab_front.png").unlink(missing_ok=True)
    print("ok: laboratorio químico 3D")


if __name__ == "__main__":
    main()
