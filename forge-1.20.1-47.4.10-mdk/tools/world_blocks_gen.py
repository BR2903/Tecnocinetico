"""Genera los recursos de los bloques del mundo post-apocalíptico (fase 1 de docs/PLAN_MUNDO.md).

python tools/world_blocks_gen.py [preview.png]
(ejecutar desde forge-1.20.1-47.4.10-mdk/)

Crea: texturas 16x16, blockstates, modelos de bloque/objeto, loot tables, tags de herramienta, recetas y textos.
Los bloques se registran en Java en block/ModWorldBlocks.java (mismos nombres).
Semillas fijas: el resultado es reproducible.
"""
import json
import math
import random
import sys
from pathlib import Path
from PIL import Image

RES = Path("src/main/resources")
ASSETS = RES / "assets/tecnocinetico"
DATA = RES / "data"
NS = "tecnocinetico"
PREVIEW = Path(sys.argv[1]) if len(sys.argv) > 1 else None
S = 16


# =================================================================== utilidades de píxel
def clamp(v):
    return max(0, min(255, int(round(v))))


def shade(c, k):
    return tuple(clamp(v * k) for v in c[:3])


def lerp(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def new():
    return Image.new("RGBA", (S, S), (0, 0, 0, 0))


def value_noise(seed, scale=4):
    """Ruido suave (bloques interpolados) que se repite sin cortes en 16x16."""
    r = random.Random(seed)
    g = [[r.random() for _ in range(scale)] for _ in range(scale)]
    out = [[0.0] * S for _ in range(S)]
    for y in range(S):
        for x in range(S):
            gx, gy = x / S * scale, y / S * scale
            x0, y0 = int(gx) % scale, int(gy) % scale
            x1, y1 = (x0 + 1) % scale, (y0 + 1) % scale
            fx, fy = gx - int(gx), gy - int(gy)
            fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
            a = g[y0][x0] * (1 - fx) + g[y0][x1] * fx
            b = g[y1][x0] * (1 - fx) + g[y1][x1] * fx
            out[y][x] = a * (1 - fy) + b * fy
    return out


def fill_noise(img, base, seed, fine=0.07, coarse=0.10, scale=4):
    r = random.Random(seed)
    vn = value_noise(seed + 1, scale)
    for y in range(S):
        for x in range(S):
            k = 1 + (vn[y][x] - 0.5) * 2 * coarse + r.uniform(-fine, fine)
            img.putpixel((x, y), (*shade(base, k), 255))
    return img


def crack(img, seed, start, steps, color, highlight=None, wrap=True, branch=True):
    """Grieta orgánica: avanza hacia abajo serpenteando 1 px y puede ramificarse."""
    r = random.Random(seed)
    x, y = start
    for i in range(steps):
        px, py = x % S, y % S
        img.putpixel((px, py), (*color, 255))
        if highlight:
            hx = (px + 1) % S
            c = img.getpixel((hx, py))
            if c[:3] != tuple(color):
                img.putpixel((hx, py), (*lerp(c, highlight, 0.45), 255))
        y += 1
        x += r.choice([-1, 0, 0, 0, 1]) if r.random() < 0.6 else 0
        if branch and i == steps // 2:
            bx, by, bd = x, y, r.choice([-1, 1])
            for _ in range(r.randrange(3, 6)):                                    # rama corta en diagonal
                bx += bd
                by += r.choice([0, 1])
                img.putpixel((bx % S, by % S), (*lerp(color, (128, 126, 120), 0.3), 255))
        if not wrap and not (0 <= x < S and 0 <= y < S):
            break


def specks(img, seed, count, colors):
    r = random.Random(seed)
    for _ in range(count):
        x, y = r.randrange(S), r.randrange(S)
        img.putpixel((x, y), (*r.choice(colors), 255))


# =================================================================== texturas
CONCRETE = (146, 144, 138)
TEX = {}


def t_concrete(seed=10):
    img = fill_noise(new(), CONCRETE, seed, fine=0.05, coarse=0.08)
    specks(img, seed + 2, 10, [shade(CONCRETE, 0.78), shade(CONCRETE, 0.85)])      # poros
    specks(img, seed + 3, 6, [shade(CONCRETE, 1.1)])                                # árido claro
    return img


def t_cracked_concrete():
    img = t_concrete(20)
    crack(img, 21, (4, 0), 16, (72, 70, 66), highlight=(178, 176, 170))
    crack(img, 22, (11, 3), 10, (80, 78, 74), highlight=(178, 176, 170), branch=False)
    specks(img, 23, 4, [(96, 94, 90)])                                              # desconchones
    return img


def t_mossy_concrete():
    img = t_cracked_concrete()
    vn = value_noise(31, 4)
    r = random.Random(32)
    moss = [(64, 92, 42), (82, 112, 50), (98, 128, 58)]
    for y in range(S):
        for x in range(S):
            bias = (1 - y / S) * 0.25                                              # más musgo arriba
            if vn[y][x] + bias + r.uniform(-0.08, 0.08) > 0.72:
                img.putpixel((x, y), (*r.choice(moss), 255))
    return img


def t_rubble():
    img = new()
    r = random.Random(40)
    pts = [(r.uniform(0, S), r.uniform(0, S)) for _ in range(9)]
    cols = [shade(CONCRETE, k) for k in (0.95, 1.05, 0.88, 1.12)] + [(124, 72, 56), (110, 104, 98)]
    pc = [r.choice(cols) for _ in pts]
    for y in range(S):
        for x in range(S):
            ds = sorted((min(abs(x - px), S - abs(x - px)) ** 2 + min(abs(y - py), S - abs(y - py)) ** 2, i)
                        for i, (px, py) in enumerate(pts))
            if math.sqrt(ds[1][0]) - math.sqrt(ds[0][0]) < 1.0:
                img.putpixel((x, y), (58, 55, 52, 255))                              # huecos entre trozos
            else:
                img.putpixel((x, y), (*shade(pc[ds[0][1]], 1 + r.uniform(-0.07, 0.07)), 255))
    return img


ASPHALT = (56, 56, 60)


def t_asphalt(seed=50):
    img = fill_noise(new(), ASPHALT, seed, fine=0.08, coarse=0.06)
    specks(img, seed + 1, 22, [(92, 92, 96), (104, 102, 100), (40, 40, 43)])      # árido del asfalto
    return img


def t_cracked_asphalt():
    img = t_asphalt(60)
    crack(img, 61, (5, 0), 16, (28, 28, 30), highlight=(84, 84, 88))
    crack(img, 62, (12, 5), 9, (30, 30, 32), branch=False)
    for p in [(6, 9), (7, 9), (13, 4)]:                                             # brotes en la grieta
        img.putpixel(p, (78, 112, 52, 255))
    return img


def t_road_line():
    img = t_asphalt(70)
    r = random.Random(71)
    for y in range(S):
        for x in (7, 8):
            if r.random() > 0.15:                                                   # pintura desgastada
                img.putpixel((x, y), (*shade((206, 172, 54), r.uniform(0.85, 1.05)), 255))
    return img


RUST = [(148, 78, 40), (122, 60, 30), (176, 100, 52), (98, 50, 28)]
STEEL = (112, 110, 106)


def rust_over(img, seed, amount=0.5):
    vn = value_noise(seed, 4)
    r = random.Random(seed + 1)
    for y in range(S):
        for x in range(S):
            if vn[y][x] + r.uniform(-0.12, 0.12) > 1 - amount:
                img.putpixel((x, y), (*r.choice(RUST), 255))
    return img


def t_metal_sheet():
    img = new()
    r = random.Random(80)
    for y in range(S):
        for x in range(S):
            ridge = [1.12, 1.0, 0.86, 0.94][x % 4]                                   # chapa ondulada
            img.putpixel((x, y), (*shade(STEEL, ridge * (1 + r.uniform(-0.05, 0.05))), 255))
    rust_over(img, 81, 0.48)
    for x in range(S):                                                              # chorretones de óxido
        if r.random() < 0.25:
            for y in range(r.randrange(4, 12)):
                img.putpixel((x, y), (*r.choice(RUST[:2]), 255))
    for p in [(1, 1), (14, 1), (1, 14), (14, 14)]:
        img.putpixel(p, (70, 66, 62, 255))                                          # remaches
    return img


def t_beam_side():
    img = new()
    r = random.Random(90)
    for y in range(S):
        for x in range(S):
            if x <= 2 or x >= 13:
                c = shade(STEEL, 1.08)                                               # alas de la viga
            elif x in (3, 12):
                c = shade(STEEL, 0.7)
            else:
                c = shade(STEEL, 0.86)                                               # alma (hundida)
            img.putpixel((x, y), (*shade(c, 1 + r.uniform(-0.05, 0.05)), 255))
    rust_over(img, 91, 0.4)
    for y in (3, 12):
        for x in (1, 14):
            img.putpixel((x, y), (64, 60, 56, 255))                                  # pernos
    return img


def t_beam_end():
    img = new()
    for y in range(S):
        for x in range(S):
            img.putpixel((x, y), (38, 36, 34, 255))
    for y in range(S):
        for x in range(S):
            flange = (y <= 2 or y >= 13) and 2 <= x <= 13
            web = 7 <= x <= 8
            if flange or (web and 2 < y < 13):
                img.putpixel((x, y), (*shade(STEEL, 1.0 if flange else 0.9), 255))   # sección en "I"
    rust_over(img, 92, 0.25)
    return img


def t_grate():
    img = new()
    r = random.Random(100)
    for y in range(S):
        for x in range(S):
            if x % 5 == 0 or y % 5 == 0 or x == 15 or y == 15:
                c = r.choice(RUST) if r.random() < 0.45 else shade(STEEL, r.uniform(0.85, 1.05))
                img.putpixel((x, y), (*c, 255))
    return img


def t_scrap_pile():
    img = new()
    r = random.Random(110)
    pts = [(r.uniform(0, S), r.uniform(0, S)) for _ in range(11)]
    cols = RUST + [shade(STEEL, 1.1), shade(STEEL, 0.8), (168, 104, 62), (176, 150, 58), (84, 104, 74)]
    pc = [r.choice(cols) for _ in pts]
    for y in range(S):
        for x in range(S):
            ds = sorted((min(abs(x - px), S - abs(x - px)) ** 2 + min(abs(y - py), S - abs(y - py)) ** 2, i)
                        for i, (px, py) in enumerate(pts))
            gap = math.sqrt(ds[1][0]) - math.sqrt(ds[0][0])
            if gap < 0.9:
                img.putpixel((x, y), (34, 30, 28, 255))
            else:
                k = 1.15 if gap < 1.8 else 1.0                                       # bordes brillantes del metal
                img.putpixel((x, y), (*shade(pc[ds[0][1]], k * (1 + r.uniform(-0.06, 0.06))), 255))
    return img


ASH = (172, 170, 166)


def t_ash():
    img = fill_noise(new(), ASH, 120, fine=0.06, coarse=0.06)
    specks(img, 121, 14, [(132, 130, 126), (120, 118, 116), (196, 194, 190)])
    specks(img, 122, 2, [(70, 66, 64)])                                             # carboncillo
    return img


def t_vitrified_sand():
    img = fill_noise(new(), (190, 152, 104), 130, fine=0.05, coarse=0.12)
    r = random.Random(131)
    for _ in range(5):                                                              # vetas brillantes de vidrio
        x, y = r.randrange(S), r.randrange(S)
        for k in range(r.randrange(3, 7)):
            img.putpixel(((x + k) % S, (y + k // 2) % S), (232, 206, 156, 255))
    crack(img, 132, (5, 0), 16, (120, 88, 56))
    crack(img, 133, (12, 2), 12, (126, 92, 60), branch=False)
    return img


def t_vitrified_glass():
    img = new()
    r = random.Random(140)
    for y in range(S):
        for x in range(S):
            if x in (0, 15) or y in (0, 15):
                img.putpixel((x, y), (126, 92, 58, 230))                             # borde
            else:
                img.putpixel((x, y), (190, 140, 80, 90 + r.randrange(0, 20)))        # ámbar ahumado translúcido
    for (x, y) in [(3, 4), (10, 7), (6, 11), (12, 12), (4, 9)]:
        img.putpixel((x, y), (236, 214, 170, 170))                                   # burbujas
    for k in range(5):
        img.putpixel((2 + k, 2 + k), (246, 232, 200, 150))                           # reflejo
    return img


def t_cinetrite_cluster():
    """Racimo de cristales separados (como la amatista): punta, cara iluminada, cara en sombra y contorno."""
    img = new()
    light, mid, dark, edge = (196, 250, 255), (110, 226, 245), (52, 180, 208), (20, 104, 126)
    crystals = [(3, 2, 7), (12, 2, 5), (6, 2, 3), (9, 3, 0)]                      # (centro x, medio ancho, y de la punta)
    for cx, hw_max, top in crystals:                                              # de atrás hacia delante
        for y in range(top, 15):
            hw = min(hw_max, (y - top + 1) // 2)
            for x in range(cx - hw, cx + hw + 1):
                if not (0 <= x < S):
                    continue
                if hw > 0 and x in (cx - hw, cx + hw):
                    c = edge
                elif x < cx:
                    c = light if y < top + (15 - top) * 0.6 else mid
                elif x == cx:
                    c = mid
                else:
                    c = dark
                img.putpixel((x, y), (*c, 255))
        img.putpixel((cx, top), (*light, 255))                                    # brillo en la punta
    return img


DEAD = (98, 90, 82)


def t_dead_log_side():
    img = new()
    r = random.Random(150)
    cols = [r.random() for _ in range(S)]
    for y in range(S):
        for x in range(S):
            k = 0.86 + 0.24 * cols[x] + r.uniform(-0.04, 0.04)
            img.putpixel((x, y), (*shade(DEAD, k), 255))
    for x in (3, 8, 12):                                                            # grietas verticales de la corteza
        for y in range(S):
            if r.random() < 0.8:
                img.putpixel((x, y), (*shade(DEAD, 0.55), 255))
    return img


def t_dead_log_top():
    img = new()
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 7.0:
                c = shade(DEAD, 0.85)                                               # corteza
            else:
                ring = 1.0 if int(d * 1.3) % 2 == 0 else 0.9
                c = shade((150, 140, 125), ring)
            img.putpixel((x, y), (*c, 255))
    for k in range(6):
        img.putpixel((7, 4 + k), (92, 84, 74, 255))                                 # grieta de secado
    return img


def t_dead_planks():
    img = new()
    r = random.Random(160)
    base = (128, 122, 112)
    for y in range(S):
        plank = y // 4
        tone = [1.0, 0.92, 1.04, 0.96][plank]
        for x in range(S):
            k = tone * (1 + r.uniform(-0.05, 0.05))
            if y % 4 == 3:
                k = 0.62                                                            # junta entre tablones
            elif (x + plank * 5) % 7 == 0 and r.random() < 0.6:
                k *= 0.85                                                           # veta
            img.putpixel((x, y), (*shade(base, k), 255))
    for p in [(1, 1), (14, 5), (2, 9), (13, 13)]:
        img.putpixel(p, (70, 66, 62, 255))                                           # clavos
    return img


def t_scrap_item():
    img = new()
    shape = ["................",
             "................",
             "........##......",
             "......####......",
             "....######.##...",
             "...#######.###..",
             "..########.####.",
             "..#######..###..",
             ".######...###...",
             ".#####...###....",
             "..###...###.....",
             "...#...###......",
             "......###.......",
             ".....###........",
             "................",
             "................"]
    r = random.Random(170)
    for y, row in enumerate(shape):
        for x, ch in enumerate(row):
            if ch == "#":
                bar = x >= 8 and y >= 4
                c = shade(STEEL, 1.1) if bar else r.choice(RUST)
                img.putpixel((x, y), (*shade(c, 1 + r.uniform(-0.08, 0.08)), 255))
    out = img.copy()                                                                # contorno oscuro
    for y in range(S):
        for x in range(S):
            if img.getpixel((x, y))[3]:
                continue
            if any(0 <= x + dx < S and 0 <= y + dy < S and img.getpixel((x + dx, y + dy))[3]
                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                out.putpixel((x, y), (30, 24, 20, 255))
    return out


TEXTURES = {
    "block/concrete": t_concrete, "block/cracked_concrete": t_cracked_concrete, "block/mossy_concrete": t_mossy_concrete,
    "block/rubble": t_rubble, "block/asphalt": t_asphalt, "block/cracked_asphalt": t_cracked_asphalt,
    "block/asphalt_road_line": t_road_line, "block/rusted_metal_sheet": t_metal_sheet,
    "block/rusted_metal_beam": t_beam_side, "block/rusted_metal_beam_top": t_beam_end, "block/rusted_grate": t_grate,
    "block/scrap_pile": t_scrap_pile, "block/ash": t_ash, "block/vitrified_sand": t_vitrified_sand,
    "block/vitrified_glass": t_vitrified_glass, "block/cinetrite_cluster": t_cinetrite_cluster,
    "block/dead_log": t_dead_log_side, "block/dead_log_top": t_dead_log_top, "block/dead_planks": t_dead_planks,
    "item/scrap_metal": t_scrap_item,
}


# =================================================================== JSON helpers
def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")


def rl(path):
    return f"{NS}:{path}"


def blockstate(name, data):
    write(ASSETS / "blockstates" / f"{name}.json", data)


def model(name, data, kind="block"):
    write(ASSETS / "models" / kind / f"{name}.json", data)


def item_model(name, parent=None, layer=None):
    data = {"parent": parent} if parent else {"parent": "minecraft:item/generated", "textures": {"layer0": layer}}
    model(name, data, "item")


def simple(name, tex=None, render_type=None):
    tex = tex or name
    m = {"parent": "minecraft:block/cube_all", "textures": {"all": rl(f"block/{tex}")}}
    if render_type:
        m["render_type"] = render_type
    model(name, m)
    blockstate(name, {"variants": {"": {"model": rl(f"block/{name}")}}})
    item_model(name, rl(f"block/{name}"))


def pillar(name, side, end):
    model(name, {"parent": "minecraft:block/cube_column", "textures": {"side": rl(f"block/{side}"), "end": rl(f"block/{end}")}})
    m = rl(f"block/{name}")
    blockstate(name, {"variants": {"axis=y": {"model": m}, "axis=z": {"model": m, "x": 90},
                                   "axis=x": {"model": m, "x": 90, "y": 90}}})
    item_model(name, m)


def family(base):
    """Losa, escalera y muro con la textura del bloque base (plantillas vanilla)."""
    t = rl(f"block/{base}")
    # losa
    model(f"{base}_slab", {"parent": "minecraft:block/slab", "textures": {"bottom": t, "top": t, "side": t}})
    model(f"{base}_slab_top", {"parent": "minecraft:block/slab_top", "textures": {"bottom": t, "top": t, "side": t}})
    blockstate(f"{base}_slab", {"variants": {
        "type=bottom": {"model": rl(f"block/{base}_slab")},
        "type=top": {"model": rl(f"block/{base}_slab_top")},
        "type=double": {"model": rl(f"block/{base}")}}})
    item_model(f"{base}_slab", rl(f"block/{base}_slab"))
    # escalera
    for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        model(f"{base}_stairs{suffix}", {"parent": f"minecraft:block/{parent}",
                                          "textures": {"bottom": t, "top": t, "side": t}})
    variants = {}
    rot = {"east": 0, "south": 90, "west": 180, "north": 270}
    for facing, y0 in rot.items():
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                m = f"{base}_stairs" + ("_inner" if "inner" in shape else "_outer" if "outer" in shape else "")
                y = y0
                if shape in ("inner_left", "outer_left"):
                    y = (y - 90) % 360
                x = 180 if half == "top" else 0
                if half == "top" and shape != "straight":
                    y = (y + 90) % 360
                v = {"model": rl(f"block/{m}")}
                if x:
                    v["x"] = x
                if y:
                    v["y"] = y
                if x or y:
                    v["uvlock"] = True
                variants[f"facing={facing},half={half},shape={shape}"] = v
    blockstate(f"{base}_stairs", {"variants": variants})
    item_model(f"{base}_stairs", rl(f"block/{base}_stairs"))
    # muro
    model(f"{base}_wall_post", {"parent": "minecraft:block/template_wall_post", "textures": {"wall": t}})
    model(f"{base}_wall_side", {"parent": "minecraft:block/template_wall_side", "textures": {"wall": t}})
    model(f"{base}_wall_side_tall", {"parent": "minecraft:block/template_wall_side_tall", "textures": {"wall": t}})
    model(f"{base}_wall_inventory", {"parent": "minecraft:block/wall_inventory", "textures": {"wall": t}})
    parts = [{"when": {"up": "true"}, "apply": {"model": rl(f"block/{base}_wall_post")}}]
    for d, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for h, m in (("low", "side"), ("tall", "side_tall")):
            a = {"model": rl(f"block/{base}_wall_{m}"), "uvlock": True}
            if y:
                a["y"] = y
            parts.append({"when": {d: h}, "apply": a})
    blockstate(f"{base}_wall", {"multipart": parts})
    item_model(f"{base}_wall", rl(f"block/{base}_wall_inventory"))


# =================================================================== loot tables
def loot(name, pools):
    write(DATA / NS / "loot_tables/blocks" / f"{name}.json", {"type": "minecraft:block", "pools": pools})


def loot_self(name):
    loot(name, [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": rl(name)}],
                 "conditions": [{"condition": "minecraft:survives_explosion"}]}])


SILK = {"condition": "minecraft:match_tool",
        "predicate": {"enchantments": [{"enchantment": "minecraft:silk_touch", "levels": {"min": 1}}]}}


def loot_slab(name):
    loot(name, [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": rl(name), "functions": [
        {"function": "minecraft:set_count", "count": 2, "add": False,
         "conditions": [{"condition": "minecraft:block_state_property", "block": rl(name),
                         "properties": {"type": "double"}}]},
        {"function": "minecraft:explosion_decay"}]}]}])


# =================================================================== recetas
def recipe(name, data):
    write(DATA / NS / "recipes" / f"{name}.json", data)


def shaped(name, pattern, key, result, count=1):
    recipe(name, {"type": "minecraft:crafting_shaped", "category": "building", "pattern": pattern,
                  "key": {k: {"item": v} for k, v in key.items()}, "result": {"item": result, "count": count}})


def shapeless(name, items, result, count=1):
    recipe(name, {"type": "minecraft:crafting_shapeless", "category": "building",
                  "ingredients": [{"item": i} for i in items], "result": {"item": result, "count": count}})


def smelt(name, ingredient, result, xp=0.1, blasting=False):
    recipe(name, {"type": "minecraft:smelting", "category": "blocks", "ingredient": {"item": ingredient},
                  "result": result, "experience": xp, "cookingtime": 200})
    if blasting:
        recipe(name + "_blasting", {"type": "minecraft:blasting", "category": "misc", "ingredient": {"item": ingredient},
                                    "result": result, "experience": xp, "cookingtime": 100})


def cutting(name, ingredient, result, count=1):
    recipe(name, {"type": "minecraft:stonecutting", "ingredient": {"item": ingredient}, "result": result, "count": count})


# =================================================================== construcción
def main():
    # --- texturas
    images = {}
    for path, fn in TEXTURES.items():
        img = fn()
        p = ASSETS / "textures" / f"{path}.png"
        p.parent.mkdir(parents=True, exist_ok=True)
        img.save(p)
        images[path] = img

    # --- modelos y blockstates
    for name in ("concrete", "cracked_concrete", "mossy_concrete", "rubble", "asphalt", "cracked_asphalt",
                 "rusted_metal_sheet", "scrap_pile", "vitrified_sand", "dead_planks"):
        simple(name)
    simple("ash_block", "ash")
    simple("vitrified_glass", render_type="minecraft:translucent")
    simple("rusted_grate", render_type="minecraft:cutout")
    for base in ("concrete", "cracked_concrete", "mossy_concrete"):
        family(base)
    pillar("rusted_metal_beam", "rusted_metal_beam", "rusted_metal_beam_top")
    pillar("dead_log", "dead_log", "dead_log_top")

    # asfalto con línea: la cara superior gira con la dirección
    model("asphalt_road_line", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": rl("block/asphalt_road_line"), "bottom": rl("block/asphalt"), "side": rl("block/asphalt")}})
    blockstate("asphalt_road_line", {"variants": {f"facing={f}": ({"model": rl("block/asphalt_road_line"), "y": y} if y
                                                                   else {"model": rl("block/asphalt_road_line")})
                                                  for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    item_model("asphalt_road_line", rl("block/asphalt_road_line"))

    # capa de escombros (como una alfombra)
    model("debris_layer", {"parent": "minecraft:block/carpet", "textures": {"wool": rl("block/rubble")}})
    blockstate("debris_layer", {"variants": {"": {"model": rl("block/debris_layer")}}})
    item_model("debris_layer", rl("block/debris_layer"))

    # capa de ceniza (1-8 capas, plantillas de nieve)
    tex = rl("block/ash")
    variants = {}
    for layers in range(1, 9):
        if layers == 8:
            variants["layers=8"] = {"model": rl("block/ash_block")}
        else:
            model(f"ash_layer_height{layers * 2}", {"parent": f"minecraft:block/snow_height{layers * 2}",
                                                    "textures": {"texture": tex, "particle": tex}})
            variants[f"layers={layers}"] = {"model": rl(f"block/ash_layer_height{layers * 2}")}
    blockstate("ash_layer", {"variants": variants})
    item_model("ash_layer", rl("block/ash_layer_height2"))

    # racimo de cinetrita (como la amatista: modelo en cruz, 6 orientaciones)
    model("cinetrite_cluster", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                "textures": {"cross": rl("block/cinetrite_cluster")}})
    m = rl("block/cinetrite_cluster")
    blockstate("cinetrite_cluster", {"variants": {
        "facing=up": {"model": m}, "facing=down": {"model": m, "x": 180},
        "facing=north": {"model": m, "x": 90}, "facing=south": {"model": m, "x": 90, "y": 180},
        "facing=east": {"model": m, "x": 90, "y": 90}, "facing=west": {"model": m, "x": 90, "y": 270}}})
    item_model("cinetrite_cluster", layer=rl("block/cinetrite_cluster"))
    item_model("scrap_metal", layer=rl("item/scrap_metal"))

    # --- loot tables
    for name in ("concrete", "cracked_concrete", "mossy_concrete", "rubble", "debris_layer", "asphalt", "cracked_asphalt",
                 "asphalt_road_line", "rusted_metal_sheet", "rusted_metal_beam", "rusted_grate", "ash_block",
                 "vitrified_sand", "dead_log", "dead_planks"):
        loot_self(name)
    for base in ("concrete", "cracked_concrete", "mossy_concrete"):
        loot_slab(f"{base}_slab")
        loot_self(f"{base}_stairs")
        loot_self(f"{base}_wall")
    loot("vitrified_glass", [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": rl("vitrified_glass")}],
                              "conditions": [SILK]}])
    loot("scrap_pile", [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "name": rl("scrap_pile"), "conditions": [SILK]},
        {"type": "minecraft:item", "name": rl("scrap_metal"), "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2, "max": 4}},
            {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:uniform_bonus_count",
             "parameters": {"bonusMultiplier": 1}},
            {"function": "minecraft:explosion_decay"}]}]}]}])
    loot("cinetrite_cluster", [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "name": rl("cinetrite_cluster"), "conditions": [SILK]},
        {"type": "minecraft:item", "name": rl("cinetrite_shard"), "conditions": [
            {"condition": "minecraft:match_tool", "predicate": {"tag": "minecraft:cluster_max_harvestables"}}],
         "functions": [{"function": "minecraft:set_count", "count": 3},
                       {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune",
                        "formula": "minecraft:ore_drops"}]},
        {"type": "minecraft:item", "name": rl("cinetrite_shard"), "functions": [
            {"function": "minecraft:set_count", "count": 1}, {"function": "minecraft:explosion_decay"}]}]}]}])
    loot("ash_layer", [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": rl("ash_layer"), "functions": [
        {"function": "minecraft:set_count", "count": n, "add": False, "conditions": [
            {"condition": "minecraft:block_state_property", "block": rl("ash_layer"), "properties": {"layers": str(n)}}]}
        for n in range(1, 9)]}], "conditions": [{"condition": "minecraft:survives_explosion"}]}])

    # --- tags
    def add_tag(path, values):
        p = DATA / path
        cur = json.loads(p.read_text(encoding="utf-8")) if p.exists() else {"replace": False, "values": []}
        for v in values:
            if v not in cur["values"]:
                cur["values"].append(v)
        write(p, cur)

    fam = [f"{b}{s}" for b in ("concrete", "cracked_concrete", "mossy_concrete") for s in ("", "_slab", "_stairs", "_wall")]
    add_tag("minecraft/tags/blocks/mineable/pickaxe.json", [rl(n) for n in fam + [
        "asphalt", "cracked_asphalt", "asphalt_road_line", "rusted_metal_sheet", "rusted_metal_beam", "rusted_grate",
        "scrap_pile", "vitrified_sand", "cinetrite_cluster"]])
    add_tag("minecraft/tags/blocks/mineable/shovel.json", [rl(n) for n in ("rubble", "debris_layer", "ash_block", "ash_layer")])
    add_tag("minecraft/tags/blocks/mineable/axe.json", [rl("dead_log"), rl("dead_planks")])
    walls = [rl(f"{b}_wall") for b in ("concrete", "cracked_concrete", "mossy_concrete")]
    slabs = [rl(f"{b}_slab") for b in ("concrete", "cracked_concrete", "mossy_concrete")]
    stairs = [rl(f"{b}_stairs") for b in ("concrete", "cracked_concrete", "mossy_concrete")]
    for kind in ("blocks", "items"):
        add_tag(f"minecraft/tags/{kind}/walls.json", walls)
        add_tag(f"minecraft/tags/{kind}/slabs.json", slabs)
        add_tag(f"minecraft/tags/{kind}/stairs.json", stairs)
        add_tag(f"minecraft/tags/{kind}/logs.json", [rl("dead_log")])
        add_tag(f"minecraft/tags/{kind}/planks.json", [rl("dead_planks")])

    # --- recetas
    shapeless("concrete", ["minecraft:sand", "minecraft:sand", "minecraft:gravel", "minecraft:gravel", "minecraft:clay_ball"],
              rl("concrete"), 4)
    smelt("cracked_concrete", rl("concrete"), rl("cracked_concrete"))
    shapeless("mossy_concrete", [rl("concrete"), "minecraft:vine"], rl("mossy_concrete"))
    shapeless("mossy_concrete_from_moss", [rl("concrete"), "minecraft:moss_block"], rl("mossy_concrete"))
    for b in ("concrete", "cracked_concrete", "mossy_concrete"):
        shaped(f"{b}_slab", ["###"], {"#": rl(b)}, rl(f"{b}_slab"), 6)
        shaped(f"{b}_stairs", ["#  ", "## ", "###"], {"#": rl(b)}, rl(f"{b}_stairs"), 4)
        shaped(f"{b}_wall", ["###", "###"], {"#": rl(b)}, rl(f"{b}_wall"), 6)
        cutting(f"{b}_slab_stonecutting", rl(b), rl(f"{b}_slab"), 2)
        cutting(f"{b}_stairs_stonecutting", rl(b), rl(f"{b}_stairs"))
        cutting(f"{b}_wall_stonecutting", rl(b), rl(f"{b}_wall"))
    shaped("debris_layer", ["##"], {"#": rl("rubble")}, rl("debris_layer"), 3)
    shapeless("asphalt", ["minecraft:gravel", "minecraft:gravel", "minecraft:gravel", "minecraft:gravel", "minecraft:coal"],
              rl("asphalt"), 4)
    smelt("cracked_asphalt", rl("asphalt"), rl("cracked_asphalt"))
    shapeless("asphalt_road_line", [rl("asphalt"), "minecraft:yellow_dye"], rl("asphalt_road_line"))
    shaped("rusted_metal_sheet", ["##", "##"], {"#": rl("scrap_metal")}, rl("rusted_metal_sheet"))
    shaped("rusted_metal_beam", ["#", "#", "#"], {"#": rl("rusted_metal_sheet")}, rl("rusted_metal_beam"), 3)
    shaped("rusted_grate", [" B ", "BSB", " B "], {"B": "minecraft:iron_bars", "S": rl("scrap_metal")}, rl("rusted_grate"), 4)
    smelt("iron_nugget_from_scrap", rl("scrap_metal"), "minecraft:iron_nugget", 0.1, blasting=True)
    shaped("ash_block", ["##", "##"], {"#": rl("ash_layer")}, rl("ash_block"))
    shaped("ash_layer", ["###"], {"#": rl("ash_block")}, rl("ash_layer"), 6)
    smelt("vitrified_glass", rl("vitrified_sand"), rl("vitrified_glass"))
    shapeless("dead_planks", [rl("dead_log")], rl("dead_planks"), 4)

    # --- textos
    names = {
        "concrete": ("Hormigón", "Concrete"), "cracked_concrete": ("Hormigón Agrietado", "Cracked Concrete"),
        "mossy_concrete": ("Hormigón Musgoso", "Mossy Concrete"),
        "rubble": ("Escombros", "Rubble"), "debris_layer": ("Capa de Escombros", "Debris"),
        "asphalt": ("Asfalto", "Asphalt"), "cracked_asphalt": ("Asfalto Agrietado", "Cracked Asphalt"),
        "asphalt_road_line": ("Asfalto con Línea", "Road Line Asphalt"),
        "rusted_metal_sheet": ("Chapa Oxidada", "Rusted Metal Sheet"), "rusted_metal_beam": ("Viga Oxidada", "Rusted Beam"),
        "rusted_grate": ("Rejilla Oxidada", "Rusted Grate"), "scrap_pile": ("Montón de Chatarra", "Scrap Pile"),
        "ash_block": ("Bloque de Ceniza", "Ash Block"), "ash_layer": ("Capa de Ceniza", "Ash"),
        "vitrified_sand": ("Arena Vitrificada", "Vitrified Sand"), "vitrified_glass": ("Vidrio Vitrificado", "Vitrified Glass"),
        "cinetrite_cluster": ("Racimo de Cinetrita", "Cinetrite Cluster"),
        "dead_log": ("Tronco Muerto", "Dead Log"), "dead_planks": ("Tablones Muertos", "Dead Planks"),
    }
    es, en = {}, {}
    for key, (e1, e2) in names.items():
        es[f"block.{NS}.{key}"], en[f"block.{NS}.{key}"] = e1, e2
    for b in ("concrete", "cracked_concrete", "mossy_concrete"):
        e1, e2 = names[b]
        for suf, (s1, s2) in {"_slab": ("Losa de", "Slab"), "_stairs": ("Escaleras de", "Stairs"), "_wall": ("Muro de", "Wall")}.items():
            es[f"block.{NS}.{b}{suf}"] = f"{s1} {e1}"
            en[f"block.{NS}.{b}{suf}"] = f"{e2} {s2}"
    es[f"item.{NS}.scrap_metal"], en[f"item.{NS}.scrap_metal"] = "Chatarra", "Scrap Metal"
    es[f"creativetab.{NS}_world_tab"], en[f"creativetab.{NS}_world_tab"] = "Tecnocinético: Mundo", "Tecnocinetico: World"
    for f, entries in (("es_es.json", es), ("en_us.json", en)):
        p = ASSETS / "lang" / f
        d = json.loads(p.read_text(encoding="utf-8"))
        d.update(entries)
        p.write_text(json.dumps(d, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    # --- vista previa (texturas ampliadas en una lámina)
    if PREVIEW:
        cols = 7
        rows = (len(images) + cols - 1) // cols
        sheet = Image.new("RGBA", (cols * 104, rows * 104), (40, 42, 48, 255))
        for i, (path, img) in enumerate(images.items()):
            big = img.resize((96, 96), Image.NEAREST)
            sheet.paste(big, ((i % cols) * 104 + 4, (i // cols) * 104 + 4), big)
        sheet.save(PREVIEW)
    print(f"ok: {len(images)} texturas")


if __name__ == "__main__":
    main()
