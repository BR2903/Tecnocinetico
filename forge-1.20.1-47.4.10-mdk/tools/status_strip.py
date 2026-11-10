"""Franja de estado común de las interfaces de máquinas: la interfaz pasa de 166 a 178 px de alto, el inventario baja
12 px y en el hueco (y 72..83) queda una pantallita oscura donde se escribe el estado (sin energía, trabajando...).

Lo usan machine_gui_gen.py, press_gui_gen.py y assembler_gui_gen.py antes de guardar. Coordenadas en las pantallas:
STRIP_Y (texto en y = STRIP_Y + 2), inventario del jugador desde y 96 y barra rápida en 154.
"""
from PIL import Image, ImageDraw

STRIP_Y, SHIFT, W = 72, 12, 176


def add_status_strip(img):
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.paste(img.crop((0, 0, img.width, STRIP_Y)), (0, 0))
    out.paste(img.crop((0, STRIP_Y, img.width, img.height - SHIFT)), (0, STRIP_Y + SHIFT))
    row = img.crop((0, STRIP_Y - 1, W, STRIP_Y))                 # la fila del panel (bordes incluidos) se repite
    for y in range(STRIP_Y, STRIP_Y + SHIFT):
        out.paste(row, (0, y))
    d = ImageDraw.Draw(out)
    d.rectangle([7, STRIP_Y, W - 8, STRIP_Y + 10], fill=(18, 22, 26, 255))
    d.line([(7, STRIP_Y), (W - 9, STRIP_Y)], fill=(55, 55, 55, 255))
    d.line([(7, STRIP_Y), (7, STRIP_Y + 9)], fill=(55, 55, 55, 255))
    d.line([(8, STRIP_Y + 10), (W - 8, STRIP_Y + 10)], fill=(255, 255, 255, 255))
    d.line([(W - 8, STRIP_Y + 1), (W - 8, STRIP_Y + 10)], fill=(255, 255, 255, 255))
    return out
