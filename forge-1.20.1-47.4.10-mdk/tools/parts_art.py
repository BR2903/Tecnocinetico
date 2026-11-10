"""Arte plano (16x16, estilo vanilla) de componentes y materiales pequeños: cinetrita (en bruto, cristal refinado y
fragmento), Resistencia 330 Ω y Transductor Piezo-Cúprico. La cinetrita es un CRISTAL, no un metal: nada de lingotes. El usuario prefiere arte normal (no 3D) en estos objetos.

python tools/parts_art.py      (desde forge-1.20.1-47.4.10-mdk)
"""
import json
from pathlib import Path

from PIL import Image

A = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tecnocinetico"


def draw(rows, pal):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), pal[ch] + (255,))
    return img


# fragmento de cinetrita: cristal facetado en diagonal (como la esquirla de amatista), cian con brillo
SHARD = [
    "................",
    ".............o..",
    "............oWo.",
    "...........oWho.",
    "..........oWhmo.",
    ".........oWhmo..",
    "...o....oWhmo...",
    "..oWo..oWhmdo...",
    "..oWho.oWhmdo...",
    "..ohmdohhmdo....",
    "...omdhhmdo.....",
    "...ommhmdo......",
    "....ommdo.......",
    ".....odo........",
    "......o.........",
    "................",
]
SHARD_PAL = {"o": (14, 70, 86), "W": (230, 255, 255), "h": (150, 240, 252), "m": (70, 200, 228), "d": (34, 130, 160)}

# cinetrita en bruto: trozo de roca con puntas de cristal cian saliendo (como una geoda partida)
RAW_CINETRITE = [
    "................",
    "................",
    "........o.......",
    "...o...oWo......",
    "..oWo..oWho..o..",
    "..ohdo.oWhdooWo.",
    "..ohdo.ohmdoohdo",
    "..ohmdoohmdoohdo",
    "..rohmdohmdohdr.",
    ".rrRohmdmdohdRr.",
    ".rRRRoddddoRRRr.",
    ".rRgRRRoooRRgRr.",
    ".rRRRgRRRRRRRRr.",
    "..rrRRRRgRRRrr..",
    "....rrrrrrrrr...",
    "................",
]
RAW_CINETRITE_PAL = dict(SHARD_PAL, r=(38, 38, 44), R=(92, 92, 100), g=(128, 128, 138))

# cristal de cinetrita (procesado): cristal tallado, prisma hexagonal con dos puntas, facetas y destello
CINETRITE_CRYSTAL = [
    "................",
    ".......o........",
    "......oWo...+...",
    ".....oWWho.+*+..",
    "....oWWhhmo.+...",
    "....oWhhmdo.....",
    "....oWhmmdo.....",
    "....oWhmmdo.....",
    "....oWhmmdo.....",
    "....oWhmmdo.....",
    "....ohhmmdo.....",
    "....ohmmddo.....",
    ".....ohmdo......",
    "......omo.......",
    ".......o........",
    "................",
]
CINETRITE_CRYSTAL_PAL = dict(SHARD_PAL, **{"+": (120, 230, 250), "*": (240, 255, 255)})

# resistencia de 330 Ω (dibujada por cálculo): cuerpo beige en diagonal con extremos abombados, bandas
# naranja-naranja-marrón-dorado bien marcadas y patas de alambre
def resistor_img():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    body, band = (226, 206, 160), {-3: (230, 128, 30), -1: (230, 128, 30), 1: (110, 58, 28), 3: (220, 180, 60)}
    cells = {}
    for y in range(16):
        for x in range(16):
            a, t = x - y, x + y - 15                                     # a = a lo largo, t = a lo ancho
            half = 2 if abs(a) in (5, 6) else 1                           # extremos abombados
            if abs(a) <= 6 and abs(t) <= half:
                c = band.get(a, body) if abs(t) <= 1 else body
                if t == -half or (abs(a) in (5, 6) and t == -2):
                    c = tuple(min(255, int(v * 1.12)) for v in c)         # luz
                elif t == half:
                    c = tuple(int(v * 0.75) for v in c)                  # sombra
                cells[(x, y)] = c
            elif 7 <= abs(a) <= 14 and t in (0, 1) and (x + y) % 2 == 1:
                cells[(x, y)] = (190, 192, 198)                           # patas
    for (x, y), c in cells.items():
        img.putpixel((x, y), c + (255,))
    for (x, y) in list(cells):                                            # contorno del cuerpo
        if abs(x - y) <= 6:
            for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                if 0 <= nx < 16 and 0 <= ny < 16 and (nx, ny) not in cells:
                    img.putpixel((nx, ny), (70, 54, 36, 255))
    return img


# transductor piezo-cúprico: disco de cobre con la cerámica blanca, electrodo plateado, punto de cinetrita y dos cables
PIEZO = [
    "................",
    ".....oooooo.....",
    "...ooccccccoo...",
    "..occCCCCCCcco..",
    "..ocCwwwwwwCco..",
    ".ocCwwssssswCco.",
    ".ocCwssssssswCo.",
    ".ocCwssskssswCo.",
    ".ocCwsssssswCco.",
    ".ocCwwssssswCco.",
    "..ocCwwwwwwCco..",
    "..occCCCCCCcco..",
    "...ooccccccRR...",
    ".....oooooo.RR..",
    "......KK.....RR.",
    ".......KK.......",
]
PIEZO_PAL = {"o": (86, 46, 24), "c": (176, 100, 56), "C": (214, 140, 84), "w": (232, 230, 220), "s": (180, 186, 196),
             "k": (90, 225, 245), "R": (200, 40, 34), "K": (30, 30, 32)}


def main():
    for name, img in (("raw_cinetrite", draw(RAW_CINETRITE, RAW_CINETRITE_PAL)),
                      ("cinetrite_ingot", draw(CINETRITE_CRYSTAL, CINETRITE_CRYSTAL_PAL)),   # id antiguo; es el Cristal de Cinetrita
                      ("cinetrite_shard", draw(SHARD, SHARD_PAL)), ("resistor_330_ohm", resistor_img()),
                      ("piezo_transducer", draw(PIEZO, PIEZO_PAL))):
        img.save(A / f"textures/item/{name}.png")
        (A / f"models/item/{name}.json").write_text(json.dumps(
            {"parent": "minecraft:item/generated", "textures": {"layer0": f"tecnocinetico:item/{name}"}}, indent=2) + "\n", encoding="utf-8")
    print("ok: arte plano de componentes")


if __name__ == "__main__":
    main()
