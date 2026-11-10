"""Plomo, estaño y titanio + polvos del Refinador + bloques de las máquinas eléctricas (fase 1).

python tools/metals_gen.py         (desde forge-1.20.1-47.4.10-mdk; luego python tools/sync_lang.py)

Genera:
- texturas: capas de mena (sobre piedra/pizarra vanilla), mena en bruto y lingote (recoloreando los de inercita),
  pepitas, polvos (10), bloques de metal y frentes de las máquinas (64 px);
- modelos de bloque/objeto y estados; botín; etiquetas forge (ores, raw_materials, ingots, nuggets, dusts,
  storage_blocks) y de herramienta; generación en el mundo; recetas de horno, alto horno, pepitas, bloques y máquinas.
"""
import json
import math
import zlib
import random
import sys
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))

MOD = Path(__file__).resolve().parent.parent / "src/main/resources"
A = MOD / "assets/tecnocinetico"
D = MOD / "data"
T = A / "textures"


def jw(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def tag_add(path, values):
    data = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"replace": False, "values": []}
    for v in values:
        if v not in data["values"]:
            data["values"].append(v)
    jw(path, data)


# =================================================================== paletas (oscuro -> brillante)
RAMPS = {
    "lead": [(22, 24, 34), (52, 56, 74), (88, 94, 118), (132, 140, 166), (190, 198, 220)],          # gris azulado mate
    "tin": [(40, 40, 40), (98, 100, 98), (160, 164, 160), (212, 216, 210), (248, 250, 244)],         # plata clara
    "titanium": [(26, 26, 34), (70, 72, 88), (128, 128, 150), (186, 182, 206), (236, 232, 250)],    # gris frío con brillo violáceo
    "uranium": [(14, 26, 10), (40, 74, 26), (90, 150, 40), (160, 220, 70), (225, 255, 160)],        # pecblenda verde que brilla
    "iron": [(40, 34, 30), (110, 100, 94), (170, 160, 152), (214, 206, 198), (245, 240, 236)],
    "gold": [(70, 40, 6), (160, 110, 20), (225, 175, 40), (250, 220, 90), (255, 248, 190)],
    "copper": [(60, 24, 14), (140, 64, 36), (200, 108, 66), (230, 150, 100), (250, 200, 160)],
    "cinetrite": [(6, 30, 40), (20, 90, 110), (50, 170, 200), (120, 225, 245), (220, 255, 255)],
    "inercita": [(20, 24, 40), (52, 62, 90), (90, 104, 140), (140, 154, 190), (200, 210, 235)],
    "piroxite": [(20, 8, 6), (90, 24, 12), (190, 60, 20), (250, 140, 40), (255, 220, 140)],
    "nullite": [(30, 20, 50), (90, 70, 140), (170, 150, 220), (225, 218, 248), (255, 255, 255)],
}
METALS = {  # nombre: (capa de mena: núcleo, medio, borde), dureza de herramienta, venas por chunk, tamaño, altura
    "lead": {"tool": "stone", "count": 7, "size": 8, "min": -48, "max": 48},
    "tin": {"tool": "stone", "count": 10, "size": 9, "min": -16, "max": 96},
    "titanium": {"tool": "iron", "count": 4, "size": 5, "min": -64, "max": -8},
    "uranium": {"tool": "iron", "count": 3, "size": 4, "min": -64, "max": -24},
}
DUSTS = ["iron", "gold", "copper", "lead", "tin", "titanium", "cinetrite", "inercita", "piroxite", "nullite"]
DUST_RESULT = {"iron": "minecraft:iron_ingot", "gold": "minecraft:gold_ingot", "copper": "minecraft:copper_ingot",
               "lead": "tecnocinetico:lead_ingot", "tin": "tecnocinetico:tin_ingot", "titanium": "tecnocinetico:titanium_ingot",
               "cinetrite": "tecnocinetico:cinetrite_ingot", "inercita": "tecnocinetico:inercita_ingot",
               "piroxite": "tecnocinetico:piroxite_ingot", "nullite": "tecnocinetico:nullite_ingot"}


def ramp_at(ramp, t):
    t = max(0.0, min(1.0, t))
    k = t * (len(ramp) - 1)
    i = min(int(k), len(ramp) - 2)
    f = k - i
    return tuple(int(ramp[i][j] + (ramp[i + 1][j] - ramp[i][j]) * f) for j in range(3))


# =================================================================== texturas
def ore_overlay(name):
    """Capa 16x16: vetas redondeadas de metal con brillo y borde oscuro (la piedra se ve debajo)."""
    ramp = RAMPS[name]
    r = random.Random({"lead": 11, "tin": 12, "titanium": 13, "uranium": 14}[name])
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    spots = [(3, 3), (10, 2), (12, 9), (5, 9), (8, 13), (1, 13)]
    r.shuffle(spots)
    for (cx, cy) in spots[:5]:
        cx += r.randint(-1, 1)
        cy += r.randint(-1, 1)
        w, h = r.choice(((2, 1), (2, 2), (3, 2), (1, 2)))
        pts = [(cx + dx, cy + dy) for dx in range(w) for dy in range(h)]
        for (x, y) in pts:
            if 0 <= x < 16 and 0 <= y < 16:
                img.putpixel((x, y), (*ramp_at(ramp, 0.55 + 0.1 * r.random()), 255))
        for (x, y) in pts:
            for nx, ny in ((x + 1, y), (x, y + 1)):
                if 0 <= nx < 16 and 0 <= ny < 16 and img.getpixel((nx, ny))[3] == 0:
                    img.putpixel((nx, ny), (*ramp[0], 255))                    # sombra abajo-derecha
            for nx, ny in ((x - 1, y), (x, y - 1)):
                if 0 <= nx < 16 and 0 <= ny < 16 and img.getpixel((nx, ny))[3] == 0:
                    img.putpixel((nx, ny), (*ramp_at(ramp, 0.3), 255))
        if 0 <= cx < 16 and 0 <= cy < 16:
            img.putpixel((cx, cy), (*ramp[4], 255))                            # brillo
    return img


def ingot_tex(name):
    """Lingote en pixel art (estilo vanilla): prisma con aristas de pendiente 1:2; cara superior (3 px),
    arista frontal brillante, cara frontal (2 px), extremo derecho y contorno oscuro."""
    ramp = RAMPS[name]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))

    def put(x, y, t):
        if 0 <= x < 16 and 0 <= y < 16:
            img.putpixel((x, y), (*ramp_at(ramp, t), 255))

    yh = lambda x: 12 - x // 2                       # arista frontal superior
    for x in range(2, 14):
        k = (x - 2) / 11
        put(x, yh(x) - 4, 0.05)                      # arista trasera (contorno)
        for y in range(yh(x) - 3, yh(x)):
            put(x, y, 0.66 + 0.16 * k + (0.06 if y == yh(x) - 3 else 0))   # cara superior
        put(x, yh(x), 0.97)                          # brillo de la arista
        put(x, yh(x) + 1, 0.48 - 0.08 * k)           # cara frontal
        put(x, yh(x) + 2, 0.36 - 0.08 * k)
        put(x, yh(x) + 3, 0.05)                      # contorno inferior
    for y in range(yh(2) - 4, yh(2) + 4):
        put(1, y, 0.05)                              # extremo izquierdo
    for y in range(yh(13) - 3, yh(13) + 3):
        put(14, y, 0.56 if y < yh(13) else 0.4)      # extremo derecho
    for y in range(yh(13) - 4, yh(13) + 4):
        put(15, y, 0.05)
    put(14, yh(13) - 4, 0.05)
    put(14, yh(13) + 3, 0.05)
    return img


def raw_chunk(name):
    """Mena en bruto en pixel art: trozo grumoso con grietas, luz arriba-izquierda."""
    ramp = RAMPS[name]
    r = random.Random(sum(map(ord, name)))
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    lumps = [(7.5, 8.5, 5.2), (5.0, 6.5, 3.2), (10.5, 6.0, 3.4), (10.0, 10.5, 3.2), (5.0, 10.5, 3.0)]
    inside = {}
    for y in range(16):
        for x in range(16):
            best = None
            for (cx, cy, rad) in lumps:
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy) / rad
                if d < 1 and (best is None or d < best[0]):
                    best = (d, cx, cy)
            if best:
                inside[(x, y)] = best
    for (x, y), (d, cx, cy) in inside.items():
        edge = any((x + dx, y + dy) not in inside for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if edge:
            t = 0.05
        else:
            t = 0.62 - 0.35 * ((x + 0.5 - cx) + (y + 0.5 - cy)) / 4 - 0.2 * d + r.uniform(-0.06, 0.06)
            if r.random() < 0.12:
                t -= 0.25                                                       # grietas
        img.putpixel((x, y), (*ramp_at(ramp, t), 255))
    return img


def nugget(name):
    """Tres grumos de metal fundido, redondeados, con luz arriba-izquierda y contorno oscuro."""
    ramp = RAMPS[name]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for (cx, cy, rad) in ((6.0, 7.5, 2.8), (10.0, 9.5, 2.4), (6.5, 11.5, 2.2)):
        for y in range(16):
            for x in range(16):
                dx, dy = (x + 0.5 - cx) / rad, (y + 0.5 - cy) / (rad * 0.85)
                d2 = dx * dx + dy * dy
                if d2 > 1.0:
                    continue
                t = 0.62 - 0.32 * dy - 0.18 * dx - 0.25 * d2
                if d2 > 0.72:
                    t = 0.12                                                   # contorno
                img.putpixel((x, y), (*ramp_at(ramp, t), 255))
        img.putpixel((int(cx - rad * 0.4), int(cy - rad * 0.45)), (*ramp[4], 255))   # brillo
    return img


def dust(name):
    """Montoncito de polvo: granos claros y oscuros, más denso en el centro."""
    ramp = RAMPS[name]
    r = random.Random(zlib.crc32(name.encode()))
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            # silueta de montículo
            dx = (x - 7.5) / 6.5
            top = 6 + 7 * dx * dx
            if y < top or y > 13 or abs(dx) > 1:
                continue
            t = 0.35 + 0.5 * (1 - (y - top) / max(1, 13 - top)) + r.uniform(-0.18, 0.18)
            img.putpixel((x, y), (*ramp_at(ramp, t), 255))
    for _ in range(6):                                                         # granos sueltos
        x, y = r.randint(2, 13), r.randint(11, 14)
        img.putpixel((x, y), (*ramp_at(ramp, 0.6), 255))
    return img


def metal_block(name):
    """Bloque de metal 16x16: placas con biseles, juntas y remaches."""
    ramp = RAMPS[name]
    r = random.Random(len(name) * 31)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            px, py = x % 8, y % 8
            t = 0.55 + r.uniform(-0.04, 0.04)
            if px == 0 or py == 0:
                t = 0.78                                                       # bisel claro
            if px == 7 or py == 7:
                t = 0.22                                                       # junta oscura
            if (px, py) in ((2, 2), (5, 5)):
                t = 0.9
            img.putpixel((x, y), (*ramp_at(ramp, t), 255))
    return img


def machine_front(kind):
    """Frente 64x64 de cada máquina sobre chapa de acero oscuro con marco."""
    S = 64
    img = Image.new("RGBA", (S, S))
    d = ImageDraw.Draw(img)
    r = random.Random(len(kind))
    steel = [(22, 24, 30), (40, 44, 52), (62, 66, 76), (92, 98, 110), (140, 146, 158)]
    for y in range(S):
        for x in range(S):
            t = 0.45 + r.uniform(-0.03, 0.03) - 0.1 * (y / S)
            img.putpixel((x, y), (*ramp_at(steel, t), 255))
    d.rectangle([0, 0, S - 1, S - 1], outline=steel[0])
    d.rectangle([1, 1, S - 2, S - 2], outline=steel[3])
    d.rectangle([3, 3, S - 4, S - 4], outline=steel[1])
    for (x, y) in ((5, 5), (S - 7, 5), (5, S - 7), (S - 7, S - 7)):
        d.rectangle([x, y, x + 1, y + 1], fill=steel[4])
    cyan = (70, 215, 235)
    if kind == "electric_refiner":                                             # tambor de molienda tras una ventana
        d.rectangle([14, 12, 49, 43], fill=steel[0], outline=steel[3])
        for i in range(6):
            y0 = 14 + i * 5
            d.rectangle([16, y0, 47, y0 + 2], fill=steel[2])
            for x in range(16, 48, 4):
                d.polygon([(x, y0 + 3), (x + 2, y0 + 3), (x + 1, y0 + 4)], fill=steel[3])
        d.line([(15, 13), (48, 13)], fill=(200, 210, 220))
        d.rectangle([20, 48, 43, 57], fill=steel[0], outline=steel[3])          # tolva de salida
        d.rectangle([22, 50, 41, 51], fill=(150, 120, 90))
        d.rectangle([52, 14, 56, 40], fill=steel[1], outline=steel[3])          # indicador
        d.rectangle([53, 30, 55, 39], fill=cyan)
    elif kind == "cell_charger":                                               # bahía de carga con bobinas
        d.rectangle([12, 10, 51, 46], fill=steel[0], outline=steel[3])
        d.rectangle([24, 16, 39, 40], fill=(30, 40, 46), outline=steel[3])      # célula en la bahía
        d.rectangle([27, 20, 36, 36], fill=cyan)
        d.rectangle([29, 22, 34, 34], fill=(200, 250, 255))
        for x0 in (14, 42):
            for y in range(13, 44, 3):
                d.line([(x0, y), (x0 + 6, y)], fill=(190, 120, 70))             # bobinas de cobre
                d.line([(x0, y + 1), (x0 + 6, y + 1)], fill=(120, 70, 40))
        for i in range(8):                                                     # barra de carga
            c = cyan if i < 5 else steel[1]
            d.rectangle([14 + i * 5, 52, 17 + i * 5, 56], fill=c)
    elif kind == "fission_reactor":                                            # vasija con mirilla al núcleo
        d.rectangle([10, 6, 53, 46], fill=steel[0], outline=steel[3])
        d.ellipse([16, 9, 47, 40], fill=(20, 26, 30), outline=steel[3])
        d.ellipse([19, 12, 44, 37], fill=(12, 40, 60))
        for i, x in enumerate(range(22, 42, 4)):                                # barras de combustible brillando
            d.rectangle([x, 15, x + 2, 34], fill=(120, 230, 90) if i % 2 == 0 else (90, 200, 240))
        d.ellipse([19, 12, 44, 37], outline=(60, 150, 200))
        for (gx, col) in ((14, (230, 70, 50)), (31, (230, 200, 60))):          # manómetros
            d.ellipse([gx, 49, gx + 9, 58], fill=steel[1], outline=steel[4])
            d.line([(gx + 4, 53), (gx + 7, 50)], fill=col)
        d.rectangle([46, 49, 55, 58], fill=(220, 180, 40))                      # radiación
        d.ellipse([49, 52, 52, 55], fill=(30, 30, 30))
        for a in (90, 210, 330):
            x = 50.5 + 3.5 * math.cos(math.radians(a))
            y = 53.5 + 3.5 * math.sin(math.radians(a))
            d.rectangle([x - 1, y - 1, x, y], fill=(30, 30, 30))
    elif kind == "chemical_lab":                                               # vitrina con matraces y tuberías
        d.rectangle([9, 8, 54, 44], fill=(14, 22, 28), outline=steel[3])
        d.line([(10, 9), (53, 9)], fill=(200, 215, 225))
        flasks = ((16, (215, 214, 90)), (31, (80, 200, 240)), (45, (120, 230, 120)))
        for (cx, col) in flasks:
            d.rectangle([cx - 1, 14, cx + 1, 24], outline=(170, 190, 200))     # cuello
            d.ellipse([cx - 6, 23, cx + 6, 40], fill=(40, 50, 58), outline=(170, 190, 200))
            d.ellipse([cx - 5, 30, cx + 5, 39], fill=col)
            d.point((cx - 3, 27), fill=(230, 240, 245))
        d.line([(16, 12), (31, 12), (45, 12)], fill=(190, 120, 70), width=2)   # tubería de cobre
        for (cx, col) in flasks:
            d.line([(cx, 12), (cx, 14)], fill=(190, 120, 70))
        for i, col in enumerate(((215, 214, 90), (80, 200, 240))):           # indicadores de los tanques
            x0 = 11 + i * 15
            d.rectangle([x0, 49, x0 + 12, 57], fill=steel[0], outline=steel[3])
            d.rectangle([x0 + 2, 52, x0 + 10, 55], fill=col)
        d.rectangle([44, 49, 51, 57], fill=(220, 180, 40))                      # peligro químico
        d.polygon([(47, 50), (50, 56), (44, 56)], fill=(30, 30, 30))
    elif kind == "reaction_chamber":                                           # mirilla con el haz y el núcleo
        d.ellipse([12, 8, 51, 47], fill=steel[0], outline=steel[3])
        d.ellipse([15, 11, 48, 44], fill=(10, 30, 36), outline=steel[1])
        d.ellipse([25, 21, 38, 34], fill=(120, 230, 80))                        # carga que resuena
        d.ellipse([28, 24, 35, 31], fill=(230, 255, 180))
        d.line([(31, 11), (31, 21)], fill=cyan, width=2)                        # haz
        d.line([(31, 34), (31, 44)], fill=cyan, width=2)
        for a in range(0, 360, 45):                                             # pernos del anillo
            x = 31.5 + 21 * math.cos(math.radians(a))
            y = 27.5 + 21 * math.sin(math.radians(a))
            d.rectangle([x - 1, y - 1, x, y], fill=steel[4])
        for i in range(2):                                                      # diales de frecuencia e intensidad
            x0 = 12 + i * 15
            d.ellipse([x0, 50, x0 + 9, 59], fill=steel[1], outline=steel[4])
            d.line([(x0 + 4, 54), (x0 + 7, 51)], fill=cyan)
        d.rectangle([44, 51, 51, 58], fill=(220, 180, 40))                      # radiación
        d.rectangle([46, 53, 49, 56], fill=(30, 30, 30))
    else:                                                                      # fabricador: troquel y cinta
        d.rectangle([12, 8, 51, 28], fill=steel[0], outline=steel[3])
        d.rectangle([26, 10, 37, 24], fill=steel[3], outline=steel[4])          # troquel
        d.rectangle([29, 24, 34, 30], fill=steel[2])
        d.rectangle([10, 34, 53, 44], fill=(30, 30, 32), outline=steel[3])      # cinta
        for x in range(12, 52, 4):
            d.line([(x, 35), (x, 43)], fill=(50, 50, 54))
        for x in (16, 26, 36, 46):                                             # balas en la cinta
            d.rectangle([x, 37, x + 3, 41], fill=(200, 140, 70))
            d.rectangle([x + 3, 38, x + 4, 40], fill=(150, 150, 160))
        d.rectangle([12, 50, 51, 57], fill=(220, 180, 40))
        for x in range(12, 52, 6):
            d.polygon([(x, 57), (x + 3, 50), (x + 6, 50), (x + 3, 57)], fill=(30, 30, 30))
    return img


# =================================================================== modelos y estados
def cube_all(name, tex):
    jw(A / f"models/block/{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": tex}})


def ore_model(name, base, overlay):
    faces = lambda t: {f: {"texture": t, "cullface": f} for f in ("down", "up", "north", "south", "west", "east")}
    jw(A / f"models/block/{name}.json", {
        "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
        "textures": {"base": base, "overlay": overlay, "particle": base},
        "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces("#base")},
                     {"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces("#overlay")}]})


def simple_state(name):
    jw(A / f"blockstates/{name}.json", {"variants": {"": {"model": f"tecnocinetico:block/{name}"}}})


def block_item(name):
    jw(A / f"models/item/{name}.json", {"parent": f"tecnocinetico:block/{name}"})


def vanilla_tinted(name, vanilla):
    """Modelo con la textura vanilla (capa 0 = tintindex 0): el color lo pone client/MetalColors."""
    jw(A / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"minecraft:item/{vanilla}"}})


OLD_INGOTS = ["cinetrite", "inercita", "piroxite", "nullite"]   # lingotes del mod que también pasan al lingote vanilla teñido


def gen_item(name):
    jw(A / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"tecnocinetico:item/{name}"}})


def machine_models(name):
    """Mismo cuerpo que el Ensamblador de Armas (chapa de aleación + luz de estado) con su propio frente."""
    for suffix in ("", "_on"):
        m = json.loads((A / f"models/block/weapon_assembler{suffix}.json").read_text(encoding="utf-8"))
        m["textures"]["front"] = f"tecnocinetico:block/{name}_front"
        jw(A / f"models/block/{name}{suffix}.json", m)
    variants = {}
    for facing, rot in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for powered in ("false", "true"):
            v = {"model": f"tecnocinetico:block/{name}{'_on' if powered == 'true' else ''}"}
            if rot:
                v["y"] = rot
            variants[f"facing={facing},powered={powered}"] = v
    jw(A / f"blockstates/{name}.json", {"variants": variants})
    block_item(name)


# =================================================================== datos
def self_drop(name):
    jw(D / f"tecnocinetico/loot_tables/blocks/{name}.json", {
        "type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"tecnocinetico:{name}"}],
                                              "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def ore_drop(name, raw):
    jw(D / f"tecnocinetico/loot_tables/blocks/{name}.json", {
        "type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": f"tecnocinetico:{name}",
             "conditions": [{"condition": "minecraft:match_tool", "predicate": {"enchantments": [{"enchantment": "minecraft:silk_touch", "levels": {"min": 1}}]}}]},
            {"type": "minecraft:item", "name": f"tecnocinetico:{raw}",
             "functions": [{"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                           {"function": "minecraft:explosion_decay"}]}]}]}]})


def cook(rid, ingredient, result, xp=0.7):
    for kind, t in (("smelting", 200), ("blasting", 100)):
        jw(D / f"tecnocinetico/recipes/{kind}_{rid}.json", {"type": f"minecraft:{kind}", "ingredient": {"item": ingredient},
                                                              "result": result, "experience": xp, "cookingtime": t})


def pack(rid, small, big):
    jw(D / f"tecnocinetico/recipes/{rid}_from_{small.split(':')[1]}.json", {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["###", "###", "###"], "key": {"#": {"item": small}},
        "result": {"item": big, "count": 1}})
    jw(D / f"tecnocinetico/recipes/{small.split(':')[1]}_from_{rid}.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [{"item": big}], "result": {"item": small, "count": 9}})


def shaped(rid, pattern, key, result):
    jw(D / f"tecnocinetico/recipes/{rid}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
                                                 "key": {k: {"item": v} for k, v in key.items()}, "result": {"item": result, "count": 1}})


def worldgen(name, cfg):
    jw(D / f"tecnocinetico/worldgen/configured_feature/{name}_ore.json", {"type": "minecraft:ore", "config": {
        "discard_chance_on_air_exposure": 0.0, "size": cfg["size"], "targets": [
            {"state": {"Name": f"tecnocinetico:{name}_ore"}, "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"}},
            {"state": {"Name": f"tecnocinetico:deepslate_{name}_ore"}, "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"}}]}})
    jw(D / f"tecnocinetico/worldgen/placed_feature/{name}_ore_placed.json", {"feature": f"tecnocinetico:{name}_ore", "placement": [
        {"type": "minecraft:count", "count": cfg["count"]}, {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:trapezoid", "min_inclusive": {"absolute": cfg["min"]},
                                                       "max_inclusive": {"absolute": cfg["max"]}}},
        {"type": "minecraft:biome"}]})
    jw(D / f"tecnocinetico/forge/biome_modifier/add_{name}_ore.json", {"type": "forge:add_features", "biomes": "#minecraft:is_overworld",
                                                                       "features": f"tecnocinetico:{name}_ore_placed", "step": "underground_ores"})


def fuel_rods():
    """Barras de combustible (16 px, en diagonal como un palo): revestimiento de titanio con ventanas verdes;
    la agotada, oscura y apagada."""
    for name, glow, clad in (("uranium_fuel_rod", (140, 240, 90), (170, 168, 190)), ("depleted_fuel_rod", (70, 90, 60), (100, 96, 104))):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for i in range(12):                                                  # de abajo-izq. a arriba-der.
            x, y = 2 + i, 13 - i
            for dx, dy, c in ((0, 0, clad), (1, 0, tuple(int(v * 0.7) for v in clad)), (0, -1, tuple(min(255, int(v * 1.2)) for v in clad)),
                              (1, 1, tuple(int(v * 0.5) for v in clad)), (-1, -1, tuple(min(255, int(v * 1.3)) for v in clad))):
                if not (0 <= x + dx < 16 and 0 <= y + dy < 16):
                    continue
                img.putpixel((x + dx, y + dy), c + (255,))
            if 2 <= i <= 9 and i % 2 == 0:
                img.putpixel((x, y), glow + (255,))                          # ventana del combustible
        for (x, y) in ((1, 14), (2, 14), (1, 13), (14, 1), (14, 2), (13, 1)):  # tapones
            img.putpixel((x, y), (60, 62, 70, 255))
        img.save(T / f"item/{name}.png")
        gen_item(name)


MACHINES = ["electric_refiner", "cell_charger", "ammo_fabricator", "reaction_chamber", "chemical_lab", "fission_reactor"]
REACTOR_ONLY = ("titanium", "uranium")


def main():
    (T / "block").mkdir(parents=True, exist_ok=True)
    pickaxe, stone, iron = [], [], []
    for name, cfg in METALS.items():
        ramp = RAMPS[name]
        ore_overlay(name).save(T / f"block/{name}_ore_overlay.png")
        # lingote, pepita y en bruto: la textura de hierro vanilla teñida (client/MetalColors), forma idéntica
        for it, vanilla in ((f"raw_{name}", "raw_iron"), (f"{name}_ingot", "iron_ingot"), (f"{name}_nugget", "iron_nugget")):
            (T / f"item/{it}.png").unlink(missing_ok=True)
            vanilla_tinted(it, vanilla)
        metal_block(name).save(T / f"block/{name}_block.png")
        ore_model(f"{name}_ore", "minecraft:block/stone", f"tecnocinetico:block/{name}_ore_overlay")
        ore_model(f"deepslate_{name}_ore", "minecraft:block/deepslate", f"tecnocinetico:block/{name}_ore_overlay")
        cube_all(f"{name}_block", f"tecnocinetico:block/{name}_block")
        for b in (f"{name}_ore", f"deepslate_{name}_ore", f"{name}_block"):
            simple_state(b)
            block_item(b)
            pickaxe.append(f"tecnocinetico:{b}")
            (iron if cfg["tool"] == "iron" else stone).append(f"tecnocinetico:{b}")
        ore_drop(f"{name}_ore", f"raw_{name}")
        ore_drop(f"deepslate_{name}_ore", f"raw_{name}")
        self_drop(f"{name}_block")
        ingot = f"tecnocinetico:{name}_ingot"
        if name not in REACTOR_ONLY:                                           # titanio y uranio: solo Cámara de Reacción
            cook(f"{name}_from_raw", f"tecnocinetico:raw_{name}", ingot)
            cook(f"{name}_from_ore", f"tecnocinetico:{name}_ore", ingot)
            cook(f"{name}_from_deepslate_ore", f"tecnocinetico:deepslate_{name}_ore", ingot)
        pack(f"{name}_ingot", f"tecnocinetico:{name}_nugget", ingot)
        pack(f"{name}_block", ingot, f"tecnocinetico:{name}_block")
        worldgen(name, cfg)
        # etiquetas forge (compatibles con otros mods que usen plomo/estaño/titanio)
        for kind, vals in (("ores", [f"tecnocinetico:{name}_ore", f"tecnocinetico:deepslate_{name}_ore"]),
                           ("storage_blocks", [f"tecnocinetico:{name}_block"])):
            tag_add(D / f"forge/tags/blocks/{kind}/{name}.json", vals)
            tag_add(D / f"forge/tags/items/{kind}/{name}.json", vals)
            tag_add(D / f"forge/tags/blocks/{kind}.json", [f"#forge:{kind}/{name}"])
            tag_add(D / f"forge/tags/items/{kind}.json", [f"#forge:{kind}/{name}"])
        for kind, item in (("raw_materials", f"raw_{name}"), ("ingots", f"{name}_ingot"), ("nuggets", f"{name}_nugget")):
            tag_add(D / f"forge/tags/items/{kind}/{name}.json", [f"tecnocinetico:{item}"])
            tag_add(D / f"forge/tags/items/{kind}.json", [f"#forge:{kind}/{name}"])
    for name in OLD_INGOTS:
        vanilla_tinted(f"{name}_ingot", "iron_ingot")
    for name in DUSTS:
        dust(name).save(T / f"item/{name}_dust.png")
        gen_item(f"{name}_dust")
        if name not in REACTOR_ONLY:
            cook(f"{name}_from_dust", f"tecnocinetico:{name}_dust", DUST_RESULT[name], 0.3)
        tag_add(D / f"forge/tags/items/dusts/{name}.json", [f"tecnocinetico:{name}_dust"])
        tag_add(D / "forge/tags/items/dusts.json", [f"#forge:dusts/{name}"])
    for m in MACHINES:
        if m != "chemical_lab":                                                # el laboratorio es 3D: tools/lab_model_gen.py
            machine_front(m).save(T / f"block/{m}_front.png")
            machine_models(m)
        self_drop(m)
        pickaxe.append(f"tecnocinetico:{m}")
        iron.append(f"tecnocinetico:{m}")
    tag_add(D / "minecraft/tags/blocks/mineable/pickaxe.json", pickaxe)
    tag_add(D / "minecraft/tags/blocks/needs_stone_tool.json", stone)
    tag_add(D / "minecraft/tags/blocks/needs_iron_tool.json", iron)
    # recetas de las máquinas (tier 2: inercita y piezas de la prensa con energía)
    shaped("electric_refiner", ["IGI", "SPS", "ICI"], {"I": "minecraft:iron_ingot", "G": "minecraft:grindstone", "S": "tecnocinetico:kinetic_servo",
                                                       "P": "tecnocinetico:inercita_plate", "C": "tecnocinetico:crude_kinetic_coupler"},
           "tecnocinetico:electric_refiner")
    shaped("cell_charger", ["CRC", "LBL", "CPC"], {"C": "minecraft:copper_ingot", "R": "tecnocinetico:resistor_330_ohm", "L": "tecnocinetico:lead_ingot",
                                                   "B": "minecraft:redstone_block", "P": "tecnocinetico:inercita_plate"}, "tecnocinetico:cell_charger")
    shaped("ammo_fabricator", ["IPI", "MTM", "ILI"], {"I": "minecraft:iron_ingot", "P": "minecraft:piston", "M": "tecnocinetico:precision_mechanism",
                                                      "T": "minecraft:crafting_table", "L": "tecnocinetico:lead_ingot"}, "tecnocinetico:ammo_fabricator")
    shaped("reaction_chamber", ["PGP", "MRM", "LCL"], {"P": "tecnocinetico:inercita_plate", "G": "minecraft:tinted_glass",
                                                       "M": "tecnocinetico:precision_mechanism", "R": "minecraft:redstone_block",
                                                       "L": "tecnocinetico:lead_block", "C": "tecnocinetico:crude_kinetic_coupler"},
           "tecnocinetico:reaction_chamber")
    shaped("chemical_lab", ["TBT", "PCP", "TST"], {"T": "tecnocinetico:tin_ingot", "B": "minecraft:brewing_stand",
                                                   "P": "tecnocinetico:inercita_plate", "C": "minecraft:cauldron",
                                                   "S": "tecnocinetico:kinetic_servo"}, "tecnocinetico:chemical_lab")
    # el titanio ya no se funde: borrar recetas viejas de horno
    for old in ("titanium_from_raw", "titanium_from_ore", "titanium_from_deepslate_ore", "titanium_from_dust"):
        for kind in ("smelting", "blasting"):
            (D / f"tecnocinetico/recipes/{kind}_{old}.json").unlink(missing_ok=True)
    fuel_rods()
    print("ok: metales, polvos y máquinas")


if __name__ == "__main__":
    main()
