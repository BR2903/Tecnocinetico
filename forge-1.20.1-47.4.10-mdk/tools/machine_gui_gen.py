"""Texturas de interfaz de las máquinas eléctricas (256x256: panel 176x166 + flecha y barra llenas a la derecha).

python tools/machine_gui_gen.py      (desde forge-1.20.1-47.4.10-mdk)
Las posiciones deben coincidir con machine/MachineKind.java (mismo estilo que tools/press_gui_gen.py).
"""
from pathlib import Path
from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tecnocinetico/textures/gui"
W, H = 176, 166
BG = (198, 198, 198, 255); LIGHT = (255, 255, 255, 255); SHADOW = (85, 85, 85, 255); BLACK = (0, 0, 0, 255)
SLOT_DARK = (55, 55, 55, 255); SLOT_IN = (139, 139, 139, 255)
CYAN = (70, 215, 235, 255); CYAN_HI = (190, 250, 255, 255); CYAN_LO = (30, 120, 140, 255)
BAR_BG = (36, 40, 48, 255)

# id: (entradas, salidas (la primera grande), flecha)   = MachineKind
KINDS = {
    "electric_refiner": ([(44, 35)], [(100, 35), (130, 35)], (68, 35)),
    "cell_charger": ([(44, 35)], [(104, 35)], (68, 35)),
    "ammo_fabricator": ([(26, 35), (44, 35), (62, 35)], [(116, 35)], (85, 35)),
}


def make(inputs, outputs, arrow_pos):
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([1, 1, W - 2, H - 2], fill=BG)
    d.line([(2, 0), (W - 3, 0)], fill=BLACK); d.line([(2, H - 1), (W - 3, H - 1)], fill=BLACK)
    d.line([(0, 2), (0, H - 3)], fill=BLACK); d.line([(W - 1, 2), (W - 1, H - 3)], fill=BLACK)
    for p in [(1, 1), (W - 2, 1), (1, H - 2), (W - 2, H - 2)]:
        d.point(p, fill=BLACK)
    d.line([(1, 2), (1, H - 3)], fill=LIGHT); d.line([(2, 1), (W - 3, 1)], fill=LIGHT)
    d.line([(W - 2, 2), (W - 2, H - 3)], fill=SHADOW); d.line([(2, H - 2), (W - 3, H - 2)], fill=SHADOW)

    def slot(x, y, big=False):
        pad = 5 if big else 1
        x0, y0 = x - pad, y - pad
        s = 16 + pad * 2
        d.rectangle([x0, y0, x0 + s - 1, y0 + s - 1], fill=SLOT_IN)
        d.line([(x0, y0), (x0 + s - 2, y0)], fill=SLOT_DARK); d.line([(x0, y0), (x0, y0 + s - 2)], fill=SLOT_DARK)
        d.line([(x0 + 1, y0 + s - 1), (x0 + s - 1, y0 + s - 1)], fill=LIGHT)
        d.line([(x0 + s - 1, y0 + 1), (x0 + s - 1, y0 + s - 1)], fill=LIGHT)

    def arrow(x, y, fill, outline):
        body = [(x, y + 5), (x + 14, y + 5), (x + 14, y), (x + 21, y + 7), (x + 21, y + 8),
                (x + 14, y + 15), (x + 14, y + 10), (x, y + 10)]
        d.polygon(body, fill=fill, outline=outline)

    for (x, y) in inputs:
        slot(x, y)
    for i, (x, y) in enumerate(outputs):
        slot(x, y, big=(i == 0))
    for row in range(3):
        for col in range(9):
            slot(8 + col * 18, 84 + row * 18)
    for col in range(9):
        slot(8 + col * 18, 142)
    arrow(*arrow_pos, (139, 139, 139, 255), SHADOW)
    arrow(176, 0, CYAN, CYAN_LO)
    d.line([(177, 6), (189, 6)], fill=CYAN_HI)
    EX, EY, EW, EH = 152, 17, 14, 52
    d.rectangle([EX - 1, EY - 1, EX + EW, EY + EH], fill=BAR_BG)
    d.line([(EX - 1, EY - 1), (EX + EW - 1, EY - 1)], fill=SLOT_DARK); d.line([(EX - 1, EY - 1), (EX - 1, EY + EH - 1)], fill=SLOT_DARK)
    d.line([(EX, EY + EH), (EX + EW, EY + EH)], fill=LIGHT); d.line([(EX + EW, EY), (EX + EW, EY + EH)], fill=LIGHT)
    for gy in range(EY + 4, EY + EH, 6):
        d.line([(EX, gy), (EX + 2, gy)], fill=(70, 76, 88, 255))
    for yy in range(EH):
        t = yy / (EH - 1)
        c = tuple(int(CYAN_HI[i] + (CYAN_LO[i] - CYAN_HI[i]) * t) for i in range(3)) + (255,)
        d.line([(176, 17 + yy), (176 + EW - 1, 17 + yy)], fill=c)
        if yy % 6 == 4:
            d.line([(176, 17 + yy), (178, 17 + yy)], fill=CYAN_LO)
    d.line([(176, 17), (176, 17 + EH - 1)], fill=CYAN_HI)
    d.polygon([(158, 6), (156, 10), (158, 10), (156, 14), (161, 9), (159, 9), (161, 6)], fill=CYAN, outline=CYAN_LO)
    return img


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    for kid, (ins, outs, arr) in KINDS.items():
        make(ins, outs, arr).save(OUT / f"{kid}.png")
    print("ok: interfaces de las máquinas")
