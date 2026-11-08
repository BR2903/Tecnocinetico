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

# El panel queda vacío: la pantalla dibuja encima el modelo 3D del arma que haya en el banco
# y las ranuras de módulo de ESE arma (sprite del marco en (200,0), 22x22; ver WeaponBenchScreen).
FX, FY = 200, 0
d.rectangle([FX, FY, FX + 21, FY + 21], fill=PANEL)
d.rectangle([FX + 1, FY + 1, FX + 20, FY + 20], outline=CYAN_DIM)
slot(FX + 3, FY + 3)

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
