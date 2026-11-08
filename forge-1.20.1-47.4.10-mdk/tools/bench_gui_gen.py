"""Textura de la interfaz del Banco de Armas Cinético (256x256, se usan 176x186).
Las posiciones de las ranuras deben coincidir con WeaponBenchMenu.java.
"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw

OUT = Path(sys.argv[1])
W, H = 176, 190
img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
d = ImageDraw.Draw(img)

BG = (198, 198, 198, 255)
LIGHT = (255, 255, 255, 255)
SHADOW = (85, 85, 85, 255)
BLACK = (0, 0, 0, 255)
SLOT_DARK = (55, 55, 55, 255)
SLOT_IN = (139, 139, 139, 255)
PANEL = (38, 42, 50, 255)
PANEL_EDGE = (22, 24, 30, 255)
GRID = (46, 51, 60, 255)
SIL = (74, 80, 94, 255)
SIL_EDGE = (110, 118, 136, 255)
CYAN = (70, 215, 235, 255)
CYAN_DIM = (40, 130, 150, 255)

# --- Panel principal estilo vanilla (bordes redondeados) ---
d.rectangle([1, 1, W - 2, H - 2], fill=BG)
d.line([(2, 0), (W - 3, 0)], fill=BLACK); d.line([(2, H - 1), (W - 3, H - 1)], fill=BLACK)
d.line([(0, 2), (0, H - 3)], fill=BLACK); d.line([(W - 1, 2), (W - 1, H - 3)], fill=BLACK)
for p in [(1, 1), (W - 2, 1), (1, H - 2), (W - 2, H - 2)]:
    d.point(p, fill=BLACK)
d.line([(1, 2), (1, H - 3)], fill=LIGHT); d.line([(2, 1), (W - 3, 1)], fill=LIGHT)
d.line([(2, 2), (3, 2)], fill=LIGHT); d.point((2, 3), fill=LIGHT)
d.line([(W - 2, 2), (W - 2, H - 3)], fill=SHADOW); d.line([(2, H - 2), (W - 3, H - 2)], fill=SHADOW)
d.line([(W - 4, H - 3), (W - 3, H - 3)], fill=SHADOW); d.point((W - 3, H - 4), fill=SHADOW)


def slot(x, y):
    """Ranura vanilla de 18x18; (x, y) es donde se dibuja el objeto."""
    x0, y0 = x - 1, y - 1
    d.rectangle([x0, y0, x0 + 17, y0 + 17], fill=SLOT_IN)
    d.line([(x0, y0), (x0 + 16, y0)], fill=SLOT_DARK)
    d.line([(x0, y0), (x0, y0 + 16)], fill=SLOT_DARK)
    d.line([(x0 + 1, y0 + 17), (x0 + 17, y0 + 17)], fill=LIGHT)
    d.line([(x0 + 17, y0 + 1), (x0 + 17, y0 + 17)], fill=LIGHT)


# --- Esquema del arma (panel oscuro con rejilla) ---
AX0, AY0, AX1, AY1 = 36, 14, 170, 95
d.rectangle([AX0, AY0, AX1, AY1], fill=PANEL, outline=PANEL_EDGE)
for gx in range(AX0 + 6, AX1, 8):
    d.line([(gx, AY0 + 1), (gx, AY1 - 1)], fill=GRID)
for gy in range(AY0 + 6, AY1, 8):
    d.line([(AX0 + 1, gy), (AX1 - 1, gy)], fill=GRID)

# Silueta de la pistola (de perfil, apuntando a la derecha)
slide = [62, 37, 140, 48]
d.rectangle(slide, fill=SIL, outline=SIL_EDGE)
d.rectangle([140, 40, 148, 45], fill=SIL, outline=SIL_EDGE)               # cañón / compensador
d.rectangle([70, 48, 132, 52], fill=SIL, outline=SIL_EDGE)                # armazón
grip = [(66, 52), (84, 52), (80, 74), (61, 74)]
d.polygon(grip, fill=SIL, outline=SIL_EDGE)                               # empuñadura inclinada
d.line([(92, 52), (92, 60), (104, 60), (106, 52)], fill=SIL_EDGE)         # guardamonte
d.line([(97, 52), (96, 57)], fill=SIL_EDGE)                               # gatillo
for sx in range(64, 76, 2):
    d.line([(sx, 39), (sx, 46)], fill=PANEL)                              # estrías
d.line([(110, 42), (130, 42)], fill=CYAN_DIM)                             # ventana de bobinas

# Ranuras de módulos y líneas de conexión (cian) hacia su pieza
MODULES = {  # posición del objeto en la ranura -> punto de la silueta
    "optic": ((93, 17), (101, 37)),
    "extended_mag": ((66, 76), (71, 74)),
    "inercita_barrel": ((150, 36), (148, 42)),
    "quick_reload": ((40, 70), (61, 72)),
    "seeker": ((120, 72), (126, 52)),
}
for (sx, sy), (tx, ty) in MODULES.values():
    cx, cy = sx + 8, sy + 8
    # línea en L desde el borde de la ranura hasta la pieza
    if abs(cx - tx) > abs(cy - ty):
        d.line([(cx, cy), (tx, cy), (tx, ty)], fill=CYAN_DIM)
    else:
        d.line([(cx, cy), (cx, ty), (tx, ty)], fill=CYAN_DIM)
    d.rectangle([tx - 1, ty - 1, tx + 1, ty + 1], fill=CYAN)              # punto de anclaje
for (sx, sy), _ in MODULES.values():
    slot(sx, sy)
    d.rectangle([sx - 2, sy - 2, sx + 17, sy + 17], outline=CYAN_DIM)     # marco cian de ranura de módulo

# Ranura del arma (más destacada)
WX, WY = 20, 46
d.rectangle([WX - 5, WY - 5, WX + 20, WY + 20], fill=PANEL, outline=PANEL_EDGE)
slot(WX, WY)
d.rectangle([WX - 3, WY - 3, WX + 18, WY + 18], outline=CYAN)

# Inventario del jugador
INV_Y = 108
for row in range(3):
    for col in range(9):
        slot(8 + col * 18, INV_Y + row * 18)
for col in range(9):
    slot(8 + col * 18, INV_Y + 58)

OUT.parent.mkdir(parents=True, exist_ok=True)
img.save(OUT)
img.crop((0, 0, W, H)).resize((W * 3, H * 3), Image.NEAREST).save(Path(sys.argv[2]))
print("ok")
