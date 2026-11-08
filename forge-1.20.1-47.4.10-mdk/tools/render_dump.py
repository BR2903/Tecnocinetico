"""Renderiza en isométrico los volcados de bloques que escribe el GameTest de estructuras
(run/gametest_dump/<nombre>.txt, líneas "x y z bloque") para revisar el aspecto sin abrir el juego.

Uso: python tools/render_dump.py <salida.png> [--maxy=N] run/gametest_dump/a.txt [b.txt ...]
     --maxy=N corta todo lo que esté por encima de esa altura (para ver el interior sin el techo).
Colores aproximados por bloque (los desconocidos en magenta); caras con luz arriba/izquierda/derecha.
"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw

COLORS = {
    "concrete": (170, 168, 162), "cracked_concrete": (150, 148, 142), "mossy_concrete": (112, 136, 92),
    "rusted_metal_beam": (150, 84, 44), "rusted_grate": (130, 90, 60), "rubble": (128, 124, 118),
    "scrap_pile": (140, 96, 64), "debris_layer": (120, 116, 110), "grass_block": (96, 140, 64),
    "dirt": (122, 88, 60), "coarse_dirt": (112, 84, 60), "stone": (120, 120, 120), "gravel": (128, 122, 118),
    "sand": (214, 200, 150), "snow": (240, 244, 248), "snow_block": (240, 244, 248), "moss_carpet": (90, 120, 50),
    "moss_block": (90, 120, 50), "vine": (60, 110, 40), "chest": (170, 120, 50), "water": (60, 90, 200),
    "oak_log": (110, 86, 52), "oak_leaves": (60, 110, 40), "short_grass": (90, 140, 60), "grass": (90, 140, 60),
    "tall_grass": (90, 140, 60), "dead_log": (100, 92, 82), "ash_block": (160, 158, 154), "ash_layer": (170, 168, 164),
    "cobblestone": (110, 110, 110), "podzol": (100, 72, 40), "asphalt": (60, 60, 64), "cracked_asphalt": (70, 70, 72),
    "cracked_concrete_slab": (150, 148, 142), "spruce_log": (80, 60, 40), "spruce_leaves": (50, 80, 50),
    "birch_log": (210, 210, 200), "birch_leaves": (110, 150, 70), "mud": (70, 60, 56), "clay": (150, 155, 170),
    "dead_planks": (132, 118, 100), "rusted_metal_sheet": (146, 92, 58), "lantern": (250, 200, 90),
    "campfire": (230, 120, 40), "farmland": (96, 66, 40), "wheat": (200, 180, 70), "potatoes": (110, 150, 60),
    "carrots": (110, 150, 60), "beetroots": (110, 150, 60), "red_bed": (180, 40, 40), "spruce_door": (100, 76, 50),
    "crafting_table": (150, 110, 70), "furnace": (100, 100, 100), "chipped_anvil": (70, 70, 74), "barrel": (130, 96, 60),
    "hay_block": (200, 170, 60), "ladder": (150, 120, 80), "iron_bars": (150, 150, 155), "dirt_path": (150, 124, 80),
    "polished_andesite": (140, 142, 138), "polished_andesite_slab": (140, 142, 138), "blast_furnace": (90, 90, 96),
    "smooth_stone": (160, 160, 160), "hopper": (70, 70, 74), "chain": (60, 64, 70), "weathered_copper": (100, 150, 120),
    "weathered_cut_copper": (100, 150, 120), "exposed_cut_copper_slab": (160, 120, 90), "vitrified_glass": (200, 150, 90),
    "heavy_weighted_pressure_plate": (190, 190, 190), "lectern": (150, 110, 70), "grindstone": (130, 130, 130),
    "swarm_comb": (44, 46, 54), "cinetrite_cluster": (70, 210, 226), "sandstone": (200, 186, 140), "red_sand": (180, 100, 50), "powder_snow": (240, 244, 248), "packed_ice": (150, 180, 230), "ice": (150, 180, 230), "terracotta": (150, 92, 66),
}
SKIP = {"short_grass", "grass", "tall_grass", "fern", "large_fern", "dandelion", "poppy"}
U = 6  # píxeles por media celda


def color(block):
    name = block.split(":")[1]
    return COLORS.get(name, (220, 0, 220))


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


MAXY = None


def render(path):
    blocks = {}
    for line in Path(path).read_text().splitlines():
        x, y, z, b = line.split()
        if b.split(":")[1] in SKIP or (MAXY is not None and int(y) > MAXY):
            continue
        blocks[(int(x), int(y), int(z))] = b
    if not blocks:
        return Image.new("RGB", (100, 100), (40, 40, 44))
    xs, ys, zs = zip(*blocks)
    W = (max(xs) + max(zs) + 2) * 2 * U
    H = (max(xs) + max(zs) + 2) * U + (max(ys) + 2) * 2 * U
    img = Image.new("RGB", (W, H), (40, 40, 44))
    d = ImageDraw.Draw(img)
    ox, oy = (max(zs) + 1) * 2 * U, (max(ys) + 1) * 2 * U
    # pintar de atrás hacia delante (algoritmo del pintor)
    for (x, y, z) in sorted(blocks, key=lambda p: (p[0] + p[2], p[1])):
        c = color(blocks[(x, y, z)])
        sx = ox + (x - z) * 2 * U
        sy = oy + (x + z) * U - y * 2 * U
        top = [(sx, sy - 2 * U), (sx + 2 * U, sy - U), (sx, sy), (sx - 2 * U, sy - U)]
        left = [(sx - 2 * U, sy - U), (sx, sy), (sx, sy + 2 * U), (sx - 2 * U, sy + U)]
        right = [(sx, sy), (sx + 2 * U, sy - U), (sx + 2 * U, sy + U), (sx, sy + 2 * U)]
        if (x, y + 1, z) not in blocks:
            d.polygon(top, fill=shade(c, 1.1))
        if (x, y, z + 1) not in blocks:
            d.polygon(left, fill=shade(c, 0.8))
        if (x + 1, y, z) not in blocks:
            d.polygon(right, fill=shade(c, 0.62))
    return img


if __name__ == "__main__":
    args = sys.argv[2:]
    for a in [a for a in args if a.startswith("--maxy=")]:
        MAXY = int(a.split("=")[1])
        args.remove(a)
    imgs = [render(p) for p in args]
    W = sum(i.width for i in imgs) + 10 * (len(imgs) - 1)
    H = max(i.height for i in imgs)
    sheet = Image.new("RGB", (W, H), (40, 40, 44))
    x = 0
    for i in imgs:
        sheet.paste(i, (x, H - i.height))
        x += i.width + 10
    sheet.thumbnail((2400, 1400))
    sheet.save(sys.argv[1])
    print("ok", sys.argv[1])
