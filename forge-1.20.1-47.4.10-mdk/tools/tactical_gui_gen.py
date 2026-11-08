"""Interfaz del Equipo Táctico: textures/gui/tactical_gear.png (256x256).

Uso (desde forge-1.20.1-47.4.10-mdk):  python tools/tactical_gui_gen.py [preview.png]
Panel 176x218 en (0,0) con el estilo del inventario vanilla. Pestañas en (176,0) normal, (202,0) resaltada,
(176,24) activa (26x27, se funde con el panel). Las posiciones de las ranuras deben coincidir con
menu/TacticalMenu.POS (ranura = esquina del objeto; el marco de 18 px se dibuja 1 px antes).
"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tecnocinetico/textures/gui/tactical_gear.png"
W, H = 176, 218
BG, LIGHT, SHADOW, BLACK = (198, 198, 198), (255, 255, 255), (85, 85, 85), (0, 0, 0)
SLOT_DARK, SLOT_IN = (55, 55, 55), (139, 139, 139)
CYAN, CYAN_DIM, GREEN = (64, 206, 228), (40, 110, 124), (90, 170, 80)
GHOST = (118, 118, 118)

AMMO = [(116 + (i % 3) * 18, 16 + (i // 3) * 18) for i in range(6)]
FOOD = [(116 + (i % 3) * 18, 66 + (i // 3) * 18) for i in range(9)]
GADGETS = [(8, 20), (8, 56), (8, 92)]
INV_Y = 136


def panel(d, x0, y0, x1, y1, open_top=False):
    """Panel vanilla: contorno negro con esquinas redondeadas, luz arriba-izquierda, sombra abajo-derecha."""
    d.rectangle([x0 + 1, y0 + 1, x1 - 1, y1 - 1], fill=BG)
    if not open_top:
        d.line([(x0 + 2, y0), (x1 - 2, y0)], fill=BLACK)
    d.line([(x0 + 2, y1), (x1 - 2, y1)], fill=BLACK)
    d.line([(x0, y0 + 2), (x0, y1 - 2)], fill=BLACK)
    d.line([(x1, y0 + 2), (x1, y1 - 2)], fill=BLACK)
    for (cx, cy) in [(x0 + 1, y0 + 1), (x1 - 1, y0 + 1), (x0 + 1, y1 - 1), (x1 - 1, y1 - 1)]:
        d.point((cx, cy), fill=BLACK)
    if not open_top:
        d.line([(x0 + 2, y0 + 1), (x1 - 3, y0 + 1)], fill=LIGHT)
    d.line([(x0 + 1, y0 + 2), (x0 + 1, y1 - 3)], fill=LIGHT)
    d.line([(x0 + 3, y1 - 1), (x1 - 2, y1 - 1)], fill=SHADOW)
    d.line([(x1 - 1, y0 + 3), (x1 - 1, y1 - 2)], fill=SHADOW)


def slot(d, x, y, accent=None):
    x0, y0 = x - 1, y - 1
    d.rectangle([x0, y0, x0 + 17, y0 + 17], fill=SLOT_IN)
    d.line([(x0, y0), (x0 + 16, y0)], fill=SLOT_DARK)
    d.line([(x0, y0), (x0, y0 + 16)], fill=SLOT_DARK)
    d.line([(x0 + 1, y0 + 17), (x0 + 17, y0 + 17)], fill=LIGHT)
    d.line([(x0 + 17, y0 + 1), (x0 + 17, y0 + 17)], fill=LIGHT)
    if accent:
        d.rectangle([x0 - 1, y0 - 1, x0 + 18, y0 + 18], outline=accent)


GHOSTS = {  # siluetas 16x16 de las ranuras de gadgets (cara, espalda, pies)
    "face": ["................", "................", "................", "................", "..############..",
             ".##....##....##.", ".#.###.##.###.#.", ".#.###.##.###.#.", ".##....##....##.", "..############..",
             "................", "................", "................", "................", "................",
             "................"],
    "back": ["................", "....##....##....", "...####..####...", "...####..####...", "...####..####...",
             "...##########...", "...####..####...", "...####..####...", "...####..####...", "...####..####...",
             "...####..####...", "....##....##....", "....##....##....", "................", "................",
             "................"],
    "feet": ["................", "................", "................", "...###....###...", "...###....###...",
             "...###....###...", "...###....###...", "..####...####...", ".#####..#####...", "######.######...",
             "######.######...", "................", "................", "................", "................",
             "................"],
}


def build():
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    panel(d, 0, 0, W - 1, H - 1)

    # ventana del personaje: pantalla oscura con rejilla cian tenue
    x0, y0, x1, y1 = 28, 13, 104, 120
    d.rectangle([x0, y0, x1, y1], fill=(18, 20, 24), outline=(40, 44, 50))
    for gx in range(x0 + 6, x1, 8):
        d.line([(gx, y0 + 1), (gx, y1 - 1)], fill=(26, 34, 40))
    for gy in range(y0 + 6, y1, 8):
        d.line([(x0 + 1, gy), (x1 - 1, gy)], fill=(26, 34, 40))
    d.line([(x0 + 4, y1 - 6), (x1 - 4, y1 - 6)], fill=CYAN_DIM)       # "suelo" bajo el personaje

    for (x, y), name in zip(GADGETS, ("face", "back", "feet")):
        slot(d, x, y, CYAN_DIM)
        for yy, row in enumerate(GHOSTS[name]):
            for xx, ch in enumerate(row):
                if ch == "#":
                    d.point((x + xx, y + yy), fill=GHOST)
        d.line([(x + 17, y + 8), (x0 - 1, y + 8)], fill=CYAN_DIM)       # conexión con el personaje
    for (x, y) in AMMO:
        slot(d, x, y, CYAN_DIM)
    for (x, y) in FOOD:
        slot(d, x, y, GREEN)
    for row in range(3):
        for col in range(9):
            slot(d, 8 + col * 18, INV_Y + row * 18)
    for col in range(9):
        slot(d, 8 + col * 18, INV_Y + 58)

    # pestañas
    tab = Image.new("RGBA", (26, 27), (0, 0, 0, 0))
    td = ImageDraw.Draw(tab)
    panel(td, 0, 0, 25, 30, open_top=False)                           # se recorta: sin borde inferior
    for i, shade in enumerate([(160, 160, 160), (176, 176, 176)]):  # normal y resaltada (más oscuras que el panel)
        t = tab.crop((0, 0, 26, 24)).copy()
        px = t.load()
        for y in range(24):
            for x in range(26):
                if px[x, y][:3] == BG:
                    px[x, y] = (*shade, 255)
        img.alpha_composite(t, (176 + i * 26, 0))
    img.alpha_composite(tab, (176, 24))                               # activa: color del panel, se funde abajo
    return img


if __name__ == "__main__":
    img = build()
    OUT.parent.mkdir(parents=True, exist_ok=True)
    img.save(OUT)
    if len(sys.argv) > 1:
        img.crop((0, 0, 230, 218)).resize((690, 600), Image.NEAREST).save(sys.argv[1])
    print("ok: interfaz del equipo táctico")
