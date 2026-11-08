"""Texturas de la Araña de Asedio (modelo SiegeSpiderModel, 128x128) y de los bloques de su arena:
- textures/entity/siege_spider.png + _glow (cian), _rage (rojo, fase 3), _charge (blanco, cargando);
- textures/block/core_console_top.png / core_console_side.png (16x16) y core_barrier.png (16x16, translúcida).

Uso (desde forge-1.20.1-47.4.10-mdk):  python tools/siege_spider_texture.py [preview.png]
Estilo: máquina pesada del Núcleo: blindaje de inercita y grafito con franjas de peligro, articulaciones de cobre,
cañones de acero, sensor y núcleo que brillan.
"""
import sys
from pathlib import Path
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from turret_texture import ROOT, STEEL, COPPER, HAZARD, box  # noqa: E402

BLOCKS = ROOT.parent / "block"
INER = [(40, 46, 60), (64, 72, 90), (88, 98, 120), (118, 130, 154)]
GRAPH = [(24, 26, 30), (38, 40, 46), (54, 57, 64), (74, 78, 88)]
CYAN = [(30, 130, 146), (70, 210, 226), (180, 248, 255)]
RED = [(140, 20, 20), (240, 60, 50), (255, 170, 150)]
WHITE = [(170, 170, 210), (230, 230, 255), (255, 255, 255)]


def armor(face, x, y, w, h, step=6):
    if x % step == 0 or y == h - 1:
        return INER[0]
    if y == 0:
        return INER[3]
    return INER[2] if face == "top" else INER[1]


def build(glow_c):
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    glow = Image.new("RGBA", (128, 128), (0, 0, 0, 0))

    def thorax(face, x, y, w, h):
        if face in ("left", "right") and y == h - 2:
            return HAZARD[(x // 3) % 2]
        if face in ("left", "right") and y == 2 and x % 5 == 2:
            return glow_c[1], True                                           # luces laterales
        if face == "bottom":
            return GRAPH[1] if x % 4 else GRAPH[0]
        return armor(face, x, y, w, h)
    box(img, glow, 0, 0, 16, 8, 20, thorax)
    box(img, glow, 0, 28, 12, 2, 16, lambda f, x, y, w, h: INER[3] if y == 0 or (f == "top" and x % 4 == 0) else INER[2])
    # lanzamisiles: rejilla de tubos con puntas que brillan
    box(img, glow, 56, 30, 10, 4, 8, lambda f, x, y, w, h: ((glow_c[0] if (x + y) % 2 else glow_c[1]), True)
        if f == "top" and x % 3 == 1 and y % 3 == 1 else GRAPH[2] if y < 2 else GRAPH[1])
    box(img, glow, 100, 20, 6, 4, 1, lambda f, x, y, w, h: ((glow_c[2] if 2 <= x <= 3 and 1 <= y <= 2 else glow_c[1]), True)
        if f in ("front", "back") else GRAPH[0])
    # torreta, cañones, sensor
    box(img, glow, 72, 0, 10, 7, 8, lambda f, x, y, w, h: HAZARD[(x // 2) % 2] if f == "front" and y == h - 2 else armor(f, x, y, w, h, 5))
    box(img, glow, 72, 15, 2, 2, 12, lambda f, x, y, w, h: (8, 8, 10) if f == "front"
        else COPPER[1] if (x if f in ("left", "right") else y) % 6 == 0 else STEEL[1])
    box(img, glow, 100, 15, 4, 2, 1, lambda f, x, y, w, h: ((glow_c[2] if x in (1, 2) else glow_c[1]), True)
        if f in ("front", "back") else STEEL[0])
    # patas: muslo de grafito con banda, rodilla de cobre, espinilla de acero, garra negra
    box(img, glow, 0, 46, 14, 2, 2, lambda f, x, y, w, h: INER[2] if x % 5 == 0 else GRAPH[2])
    box(img, glow, 40, 46, 3, 3, 3, lambda f, x, y, w, h: COPPER[2] if (x, y) == (1, 1) else COPPER[1] if y < 2 else COPPER[0])
    box(img, glow, 32, 46, 2, 25, 2, lambda f, x, y, w, h: HAZARD[0] if y in (6, 7) else STEEL[2] if y % 6 else STEEL[1])
    box(img, glow, 40, 52, 4, 2, 4, lambda f, x, y, w, h: GRAPH[0] if y else STEEL[2])
    return img, glow


def blocks():
    top = Image.new("RGBA", (16, 16), (*GRAPH[1], 255))
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                top.putpixel((x, y), (*INER[0], 255))
            elif 3 <= x <= 12 and 3 <= y <= 12:
                c = CYAN[2] if (x, y) in ((6, 6), (9, 9)) else CYAN[1] if (x + y) % 4 == 0 else CYAN[0]
                top.putpixel((x, y), (*c, 255))                               # pantalla
            elif (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):
                top.putpixel((x, y), (*COPPER[1], 255))
    side = Image.new("RGBA", (16, 16), (*INER[1], 255))
    for y in range(16):
        for x in range(16):
            if y in (0, 15) or x in (0, 15):
                side.putpixel((x, y), (*INER[0], 255))
            elif y == 13:
                side.putpixel((x, y), (*HAZARD[(x // 2) % 2], 255))
            elif 4 <= y <= 9 and 3 <= x <= 12:
                side.putpixel((x, y), (*(GRAPH[0] if y in (4, 9) or x in (3, 12) else CYAN[0] if (x + y) % 3 else CYAN[1]), 255))
            elif y == 2:
                side.putpixel((x, y), (*INER[3], 255))
    barrier = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            grid = (x + y) % 4 == 0 or (x - y) % 4 == 0
            a = 200 if edge else 140 if grid else 70
            c = CYAN[2] if edge else CYAN[1] if grid else CYAN[0]
            barrier.putpixel((x, y), (*c, a))
    return top, side, barrier


if __name__ == "__main__":
    img, glow = build(CYAN)
    img.save(ROOT / "siege_spider.png")
    glow.save(ROOT / "siege_spider_glow.png")
    build(RED)[1].save(ROOT / "siege_spider_rage.png")
    build(WHITE)[1].save(ROOT / "siege_spider_charge.png")
    top, side, barrier = blocks()
    top.save(BLOCKS / "core_console_top.png")
    side.save(BLOCKS / "core_console_side.png")
    barrier.save(BLOCKS / "core_barrier.png")
    if len(sys.argv) > 1:
        prev = Image.new("RGBA", (512 + 8 + 3 * 72, 512), (48, 50, 56, 255))
        prev.alpha_composite(img.resize((512, 512), Image.NEAREST))
        for i, t in enumerate((top, side, barrier)):
            prev.alpha_composite(t.resize((64, 64), Image.NEAREST), (520 + i * 72, 0))
        prev.save(sys.argv[1])
    print("ok: araña de asedio")
