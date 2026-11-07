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
# Reglas (estilo vanilla): paletas cortas (rampas de 4-6 tonos), GRUPOS de píxeles (no ruido suelto),
# luz desde arriba-izquierda (bisel: borde claro arriba/izquierda, sombra abajo/derecha) y formas reconocibles.

def ramp(dark, light, n):
    """Rampa de n tonos de oscuro a claro."""
    return [lerp(dark, light, i / (n - 1)) for i in range(n)]


def put(img, x, y, c, a=255):
    img.putpixel((x % S, y % S), (*c[:3], a))


def tone_field(seed, scale, levels, bias=0.0):
    """Campo de tonos en grupos (ruido suave cuantizado)."""
    vn = value_noise(seed, scale)
    return [[max(0, min(levels - 1, int((vn[y][x] + bias) * levels))) for x in range(S)] for y in range(S)]


def cells(seed, n):
    """Partición en n 'piedras' (Voronoi que se repite sin cortes): id de celda y si el píxel es junta."""
    r = random.Random(seed)
    pts = [(r.uniform(0, S), r.uniform(0, S)) for _ in range(n)]
    cid = [[0] * S for _ in range(S)]
    edge = [[False] * S for _ in range(S)]
    for y in range(S):
        for x in range(S):
            ds = sorted((min(abs(x + .5 - px), S - abs(x + .5 - px)) ** 2 + min(abs(y + .5 - py), S - abs(y + .5 - py)) ** 2, i)
                        for i, (px, py) in enumerate(pts))
            cid[y][x] = ds[0][1]
            edge[y][x] = math.sqrt(ds[1][0]) - math.sqrt(ds[0][0]) < 1.1
    return cid, edge


def bevel_stones(img, cid, edge, palette_for, seed, gap):
    """Cada piedra con su luz arriba-izquierda y su sombra abajo-derecha; juntas oscuras."""
    r = random.Random(seed)
    for y in range(S):
        for x in range(S):
            if edge[y][x]:
                put(img, x, y, gap)
                continue
            c = cid[y][x]
            pal = palette_for(c)
            up = cid[(y - 1) % S][x] != c or edge[(y - 1) % S][x]
            left = cid[y][(x - 1) % S] != c or edge[y][(x - 1) % S]
            down = cid[(y + 1) % S][x] != c or edge[(y + 1) % S][x]
            right = cid[y][(x + 1) % S] != c or edge[y][(x + 1) % S]
            i = 2
            if up or left:
                i = 3
            if down or right:
                i = 1
            if r.random() < 0.12:
                i = max(0, min(len(pal) - 1, i + r.choice([-1, 1])))
            put(img, x, y, pal[i])


def draw_crack(img, seed, start, length, dark, light, branch=True):
    """Grieta vanilla: línea oscura de 1 px con borde iluminado a la derecha (como cracked_stone_bricks)."""
    r = random.Random(seed)
    x, y = start
    path = []
    for i in range(length):
        path.append((x, y))
        y += 1
        if r.random() < 0.45:
            x += r.choice([-1, 1])
    if branch:
        bx, by = path[length // 2]
        d = r.choice([-1, 1])
        for _ in range(r.randrange(3, 5)):
            bx += d
            by += r.choice([0, 1])
            path.append((bx, by))
    for (cx, cy) in path:
        put(img, cx + 1, cy, light)
    for (cx, cy) in path:
        put(img, cx, cy, dark)


# ---------------------------------------------------------------- hormigón
CONC = ramp((92, 90, 86), (176, 174, 168), 6)        # 0 sombra profunda ... 5 luz


def concrete_base(seed=10):
    img = new()
    field = tone_field(seed, 3, 3)                    # manchas grandes y suaves (3 tonos medios)
    for y in range(S):
        for x in range(S):
            put(img, x, y, CONC[2 + field[y][x]])
    for x in range(S):                                 # junta del encofrado (borde del panel)
        put(img, x, 0, CONC[5])
        put(img, x, 15, CONC[1])
    for y in range(S):
        put(img, 0, y, CONC[5])
        put(img, 15, y, CONC[1])
    for (hx, hy) in [(4, 4), (11, 4), (4, 11), (11, 11)]:   # agujeros de los tensores del encofrado
        put(img, hx, hy, CONC[0])
        put(img, hx + 1, hy + 1, CONC[5])
    r = random.Random(seed + 5)
    for (hx, hy) in [(4, 4), (11, 11)]:                # chorretón de humedad bajo un agujero
        for k in range(1, r.randrange(3, 5)):
            put(img, hx, hy + k, CONC[1] if k < 2 else CONC[2])
    return img


def t_concrete():
    return concrete_base(10)


def t_cracked_concrete():
    img = concrete_base(20)
    draw_crack(img, 21, (6, 0), 15, CONC[0], CONC[5])
    draw_crack(img, 22, (12, 6), 7, CONC[0], CONC[5], branch=False)
    for (x, y) in [(1, 13), (2, 13), (1, 14), (2, 14), (3, 14)]:   # esquina desconchada (se ve el árido)
        put(img, x, y, (112, 104, 94))
    put(img, 1, 12, CONC[1])
    put(img, 3, 13, CONC[1])
    return img


MOSS = [(52, 74, 34), (72, 100, 44), (94, 126, 54), (122, 152, 70)]


def t_mossy_concrete():
    img = t_cracked_concrete()
    vn = value_noise(31, 4)
    for y in range(S):
        for x in range(S):
            v = vn[y][x] + (1 - y / S) * 0.32           # el musgo cuelga desde arriba
            if v > 0.78:
                put(img, x, y, MOSS[3] if v > 0.98 else MOSS[2])
            elif v > 0.70:
                put(img, x, y, MOSS[1])
            elif v > 0.66 and (x + y) % 2 == 0:
                put(img, x, y, MOSS[0])                 # borde irregular de la mata
    return img


# ---------------------------------------------------------------- escombros
BRICK = ramp((84, 44, 34), (176, 98, 72), 5)



# ---------------------------------------------------------------- asfalto
ASPH = ramp((30, 30, 33), (104, 102, 100), 5)




def t_road_line():
    img = t_asphalt(70)
    r = random.Random(71)
    paint = [(170, 140, 44), (214, 182, 66), (236, 210, 102)]
    for y in range(S):
        worn = r.random() < 0.18
        if not worn:
            put(img, 7, y, paint[2])
            put(img, 8, y, paint[1])
            put(img, 9, y, paint[0])                    # borde de la pintura
    return img


# ---------------------------------------------------------------- metal oxidado
STEEL = ramp((52, 52, 54), (176, 176, 172), 6)
RUSTR = [(84, 40, 22), (124, 60, 30), (162, 86, 42), (196, 118, 62)]


def rust_clumps(img, seed, amount, streaks=True):
    """Óxido irregular: mezcla de dos escalas de ruido, borde desgastado y chorretones."""
    big, small = value_noise(seed, 4), value_noise(seed + 7, 8)
    r = random.Random(seed + 1)
    mask = [[(big[y][x] * 0.6 + small[y][x] * 0.4) + r.uniform(-0.06, 0.06) > 1 - amount
             for x in range(S)] for y in range(S)]
    for y in range(S):
        for x in range(S):
            if mask[y][x]:
                up = not mask[(y - 1) % S][x]
                down = not mask[(y + 1) % S][x]
                if up:
                    c = RUSTR[3]
                elif down:
                    c = RUSTR[0]
                else:
                    c = RUSTR[2] if small[y][x] > 0.5 else RUSTR[1]
                put(img, x, y, c)
            elif any(mask[(y + dy) % S][(x + dx) % S] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))) and r.random() < 0.35:
                put(img, x, y, RUSTR[1])                       # puntitos de óxido alrededor de la mancha
    if streaks:
        for x in range(S):                                     # chorretones bajo el óxido
            for y in range(S):
                if mask[y][x] and not mask[(y + 1) % S][x] and r.random() < 0.4:
                    for k in range(1, r.randrange(2, 4)):
                        if not mask[(y + k) % S][x]:
                            put(img, x, y + k, RUSTR[1])
    return img







# ---------------------------------------------------------------- ceniza
ASHR = ramp((112, 110, 106), (198, 196, 192), 5)



# ---------------------------------------------------------------- vitrificado
SANDR = ramp((136, 100, 64), (214, 178, 124), 5)
GLAZE = [(96, 58, 30), (150, 96, 48), (198, 142, 74), (240, 204, 140)]




# ---------------------------------------------------------------- cinetrita
CLUSTER_MAP = [
    "................",
    "........e.......",
    ".......eLe......",
    ".......eLMe.....",
    "..e....eLMe.....",
    ".eLe...eLMDe..e.",
    ".eLMe..eLMDe.eLe",
    ".eLMe.eLLMDe.eLM",
    ".eLMDeeLLMDe.eLM",
    "..eLMDeLLMDeeLMD",
    "..eLMDeLLMMDeLMD",
    "...eLMeLLMMDeLMe",
    "...eLMDLLMMDLMDe",
    "....eLMLLMMDMDe.",
    ".....eeeeeeeee..",
    "................",
]


def t_cinetrite_cluster():
    img = new()
    pal = {"e": (20, 100, 124), "L": (196, 250, 255), "M": (96, 222, 242), "D": (44, 168, 198)}
    for y, row in enumerate(CLUSTER_MAP):
        for x, ch in enumerate(row[:S]):
            if ch in pal:
                put(img, x, y, pal[ch])
    return img


# ---------------------------------------------------------------- madera muerta
BARK = ramp((44, 40, 36), (138, 128, 116), 5)



def t_dead_log_top():
    img = new()
    rings = ramp((92, 84, 74), (170, 158, 140), 4)
    for y in range(S):
        for x in range(S):
            d = max(abs(x - 7.5), abs(y - 7.5))           # anillos "cuadrados" como el roble vanilla
            if d >= 7:
                c = BARK[1]
            elif d >= 6:
                c = BARK[3]
            else:
                c = rings[3] if int(d) % 2 == 0 else rings[1]
            put(img, x, y, c)
    for k in range(5):
        put(img, 8, 3 + k, rings[0])                      # grieta de secado
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
                c = STEEL[4] if bar else r.choice(RUSTR)
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


# ================================================================ texturas v3 (lenguaje visual inspirado en Create)
# Formas limpias, contornos oscuros de 1 px, bisel (luz arriba-izquierda), poco ruido y acentos con intención.

OUTLINE = (34, 33, 32)


def frame(img, dark, light, shadow):
    """Marco de panel: contorno oscuro exterior + bisel interior (luz arriba/izquierda, sombra abajo/derecha)."""
    for i in range(S):
        put(img, i, 0, dark)
        put(img, i, 15, dark)
        put(img, 0, i, dark)
        put(img, 15, i, dark)
    for i in range(1, 15):
        put(img, i, 1, light)
        put(img, 1, i, light)
        put(img, i, 14, shadow)
        put(img, 14, i, shadow)


def bolt(img, x, y, light, dark):
    put(img, x, y, light)
    put(img, x + 1, y + 1, dark)


def t_metal_sheet():
    img = new()
    for y in range(S):
        for x in range(S):
            put(img, x, y, STEEL[[3, 2, 2, 1][x % 4]])            # chapa ondulada limpia (3 tonos)
    frame(img, OUTLINE, STEEL[4], STEEL[0])
    for (x, y) in [(2, 2), (12, 2), (2, 12), (12, 12)]:
        bolt(img, x, y, STEEL[5], STEEL[0])
    r = random.Random(81)
    for x in range(2, 14):                                       # óxido donde se acumula el agua: borde inferior
        h = r.choice([0, 1, 1, 2, 3])
        for k in range(h):
            put(img, x, 13 - k, RUSTR[1] if k == h - 1 else RUSTR[2])
        if h:
            put(img, x, 13 - h, RUSTR[3])
    for (x, y) in [(2, 2), (12, 2)]:                             # chorretón bajo los tornillos de arriba
        for k in range(2, r.randrange(4, 7)):
            put(img, x, y + k, RUSTR[2] if k < 4 else RUSTR[1])
    return img


def t_beam_side():
    img = new()
    flange = [OUTLINE, STEEL[4], STEEL[3], STEEL[1]]              # ala: contorno, canto iluminado, cara, sombra
    for y in range(S):
        for i, c in enumerate(flange):
            put(img, i, y, c)
            put(img, 15 - i, y, flange[3 - i] if i in (1, 2) else c)
        for x in range(4, 12):
            put(img, x, y, STEEL[1] if x in (4, 11) else STEEL[2])  # alma hundida
    for y0 in (3, 11):                                           # pares de tornillos en el alma
        for x0 in (6, 9):
            bolt(img, x0, y0, STEEL[5], OUTLINE)
    r = random.Random(91)
    for (x0, y0) in [(6, 3), (9, 11)]:
        for k in range(2, r.randrange(3, 6)):
            put(img, x0, y0 + k, RUSTR[2] if k < 3 else RUSTR[1])
    for y in range(12, 16):                                      # óxido al pie del alma
        for x in range(5, 11):
            if r.random() < (y - 11) * 0.22:
                put(img, x, y, r.choice(RUSTR[1:3]))
    return img


def t_beam_end():
    img = new()
    hollow = (24, 23, 22)
    for y in range(S):
        for x in range(S):
            put(img, x, y, hollow)
    for x in range(1, 15):                                       # alas (arriba y abajo) con bisel
        put(img, x, 1, STEEL[4]); put(img, x, 2, STEEL[2]); put(img, x, 3, STEEL[1])
        put(img, x, 12, STEEL[4]); put(img, x, 13, STEEL[2]); put(img, x, 14, STEEL[1])
    for y in range(4, 12):                                       # alma
        put(img, 7, y, STEEL[3]); put(img, 8, y, STEEL[1])
    for x in range(S):
        put(img, x, 0, OUTLINE); put(img, x, 15, OUTLINE)
    return img


def t_grate():
    img = new()
    r = random.Random(100)
    for y in range(S):
        for x in range(S):
            bx, by = x % 8, y % 8
            if by in (0, 1) or bx in (0, 1):
                if by == 0 or (bx == 0 and by > 1):
                    c = STEEL[4]                                  # cara iluminada de la barra
                elif by == 1 or bx == 1:
                    c = STEEL[2]
                else:
                    c = STEEL[3]
                if r.random() < 0.18:
                    c = RUSTR[2] if c == STEEL[4] else RUSTR[1]
                put(img, x, y, c)
            elif bx == 2 or by == 2:
                put(img, x, y, OUTLINE, 160)                       # sombra de la barra sobre el hueco
    for (x, y) in [(0, 0), (8, 0), (0, 8), (8, 8)]:
        bolt(img, x, y, STEEL[5], STEEL[0])                       # remache en cada cruce
    return img


def polygon_chunks(seed, n):
    """Trozos angulosos grandes (para escombros/chatarra): celdas Voronoi con pocos puntos."""
    return cells(seed, n)


def t_rubble():
    img = new()
    cid, edge = polygon_chunks(40, 5)
    r = random.Random(41)
    pal = {c: (BRICK[1:5] if r.random() < 0.25 else CONC[1:5]) for c in range(5)}
    for y in range(S):
        for x in range(S):
            if edge[y][x]:
                put(img, x, y, OUTLINE)
                continue
            c = cid[y][x]
            up = edge[(y - 1) % S][x] or cid[(y - 1) % S][x] != c
            left = edge[y][(x - 1) % S] or cid[y][(x - 1) % S] != c
            down = edge[(y + 1) % S][x] or cid[(y + 1) % S][x] != c
            right = edge[y][(x + 1) % S] or cid[y][(x + 1) % S] != c
            i = 3 if (up or left) else 0 if (down or right) else 2
            if i == 2 and (x * 7 + y * 3) % 11 == 0:
                i = 1                                             # poros del hormigón
            put(img, x, y, pal[c][i])
    for k in range(7):                                            # varilla de acero oxidada asomando
        x, y = 3 + k, 12 - k
        put(img, x, y, RUSTR[3]); put(img, x + 1, y, RUSTR[1]); put(img, x, y + 1, OUTLINE)
    return img


def t_scrap_pile():
    img = new()
    for y in range(S):
        for x in range(S):
            put(img, x, y, (28, 26, 24))
    r = random.Random(110)
    plates = [STEEL[1:5], RUSTR, [(96, 58, 34), (150, 92, 54), (196, 128, 80), (226, 164, 112)],
              [(46, 74, 70), (64, 104, 98), (88, 136, 126), (130, 172, 160)],
              [(122, 96, 30), (168, 136, 44), (206, 176, 70), (232, 210, 110)], RUSTR, STEEL[1:5]]
    for i, pal in enumerate(plates):                              # placas superpuestas (de atrás hacia delante)
        w, h = r.randint(6, 10), r.randint(4, 7)
        x0, y0 = r.randrange(S), r.randrange(S)
        skew = r.choice([-1, 0, 1])
        for yy in range(h):
            for xx in range(w):
                x, y = x0 + xx + (yy * skew) // 3, y0 + yy
                edge_ = xx in (0, w - 1) or yy in (0, h - 1)
                if edge_:
                    c = OUTLINE
                elif yy == 1:
                    c = pal[3]                                    # canto superior iluminado
                elif yy == h - 2:
                    c = pal[0]
                else:
                    c = pal[2] if (xx + yy) % 5 else pal[1]
                put(img, x, y, c)
        put(img, x0 + 2, y0 + 2, (220, 220, 214))                  # remache/brillo
    return img


def t_asphalt(seed=50):
    img = new()
    field = tone_field(seed, 3, 2)
    for y in range(S):
        for x in range(S):
            put(img, x, y, ASPH[1 + field[y][x]])
    r = random.Random(seed + 1)
    for _ in range(7):                                            # piedrecitas 2x2 con luz y sombra
        x, y = r.randrange(S), r.randrange(S)
        put(img, x, y, ASPH[4]); put(img, x + 1, y, ASPH[3])
        put(img, x, y + 1, ASPH[3]); put(img, x + 1, y + 1, ASPH[0])
    for _ in range(6):
        put(img, r.randrange(S), r.randrange(S), ASPH[3])
    return img


def t_cracked_asphalt():
    img = t_asphalt(60)
    r = random.Random(61)
    x = 6
    for y in range(S):                                            # grieta ancha (2 px) con borde iluminado
        put(img, x, y, (14, 14, 16)); put(img, x + 1, y, (22, 22, 24)); put(img, x + 2, y, ASPH[4])
        if r.random() < 0.4:
            x += r.choice([-1, 1])
    tuft = [(0, 0, 3), (-1, 1, 2), (1, 1, 2), (0, 1, 1), (-1, 2, 1), (1, 2, 0)]
    for dx, dy, i in tuft:                                        # mata de hierba saliendo de la grieta
        put(img, 7 + dx, 8 + dy, MOSS[i])
    return img


def t_ash():
    img = new()
    for y in range(S):
        for x in range(S):
            v = math.sin((x + y * 2) / 16 * 2 * math.pi * 2 + math.sin(y / 16 * 2 * math.pi) * 1.4)
            i = 3 if v > 0.55 else 2 if v > -0.2 else 1           # dunas de ceniza en bandas diagonales
            put(img, x, y, ASHR[i])
    r = random.Random(121)
    for _ in range(5):
        x, y = r.randrange(S), r.randrange(S)
        put(img, x, y, ASHR[0]); put(img, x + 1, y, ASHR[0])       # carboncillo
    put(img, 11, 5, (150, 64, 40)); put(img, 4, 12, (120, 52, 36))  # brasas
    return img


def t_vitrified_sand():
    img = new()
    shard = [(108, 66, 34), (164, 108, 56), (206, 150, 84), (240, 200, 140), (255, 236, 196)]
    for y in range(S):
        for x in range(S):
            d = (x + y) % 8                                       # facetas diagonales (como el cuarzo)
            band = ((x - y) // 4) % 3
            i = [1, 2, 3, 2, 2, 1, 1, 0][d]
            i = max(0, min(4, i + (band - 1)))
            put(img, x, y, shard[i])
    for k in range(4):                                            # reflejos en las aristas
        put(img, 2 + k, 1 + k, shard[4]); put(img, 10 + k, 9 + k, shard[4])
    for (x, y) in [(6, 13), (13, 4)]:
        put(img, x, y, (84, 52, 28))
    return img


def t_vitrified_glass():
    img = new()
    outer, inner_l, inner_d = (98, 64, 34), (226, 178, 112), (150, 104, 60)
    for y in range(S):
        for x in range(S):
            if x in (0, 15) or y in (0, 15):
                put(img, x, y, outer, 255)
            elif x == 1 or y == 1:
                put(img, x, y, inner_l, 255)
            elif x == 14 or y == 14:
                put(img, x, y, inner_d, 255)
            else:
                put(img, x, y, (196, 142, 82), 64)
    for k in range(3):                                            # brillos en esquinas (estilo vidrio enmarcado)
        put(img, 3 + k, 3, (252, 236, 200), 220)
        put(img, 3, 3 + k, (252, 236, 200), 220)
    put(img, 12, 12, (252, 236, 200), 200)
    put(img, 11, 12, (252, 236, 200), 160)
    return img


def t_dead_log_side():
    img = new()
    r = random.Random(150)
    x = 0
    while x < S:                                                  # placas de corteza de 2-3 px separadas por surcos
        w = r.choice([2, 3, 3])
        tone = r.choice([2, 3])
        breaks = {r.randrange(S) for _ in range(2)}
        for xx in range(w):
            for y in range(S):
                c = BARK[tone + 1] if xx == 0 else BARK[tone]
                if y in breaks:
                    c = BARK[1]
                put(img, x + xx, y, c)
        for y in range(S):
            put(img, x + w, y, BARK[0])                            # surco
        x += w + 1
    lichen = [(118, 130, 104), (146, 158, 128)]
    for (lx, ly) in [(4, 3), (5, 3), (4, 4), (11, 10), (12, 10), (12, 11)]:   # líquenes
        put(img, lx, ly, lichen[(lx + ly) % 2])
    return img


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
