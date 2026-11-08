"""Texturas del enjambre (modelo SwarmWaspModel, 64x32) y el icono del Núcleo de Enjambre.

Uso (desde forge-1.20.1-47.4.10-mdk):  python tools/swarm_wasp_texture.py [preview.png]
Genera textures/entity/swarm_wasp/{scout,stinger,guard,glow}.png, textures/entity/hive_node(_glow).png
y textures/item/swarm_core.png.

Estilo: caparazón de placas de metal oscuro con cantos iluminados, juntas de cobre, ojos compuestos y bandas
de cinetrita cian (que además brillan: van también en la textura *glow*), alas de membrana translúcida.
"""
import sys
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tecnocinetico/textures"
CYAN = [(30, 130, 146), (70, 210, 226), (180, 248, 255)]
COPPER = [(96, 52, 30), (150, 88, 52), (204, 138, 88)]

PALETTES = {
    # (caparazón: sombra, base, canto, brillo), color de las juntas
    "scout": ([(44, 50, 58), (66, 74, 86), (96, 106, 120), (140, 152, 166)], COPPER),
    "stinger": ([(24, 25, 30), (38, 40, 48), (58, 62, 72), (92, 98, 112)], COPPER),
    "guard": ([(70, 40, 24), (112, 66, 38), (160, 100, 60), (206, 146, 96)], [(36, 38, 44), (56, 60, 70), (88, 94, 108)]),
    "hive_node": ([(52, 30, 20), (92, 54, 32), (146, 92, 54), (214, 160, 96)], [(28, 29, 34), (44, 46, 54), (70, 74, 86)]),
}


def faces(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


class Tex:
    def __init__(self):
        self.img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
        self.glow = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    def px(self, x, y, c, a=255, glow=False):
        self.img.putpixel((x, y), (*c[:3], a))
        if glow:
            self.glow.putpixel((x, y), (*c[:3], 255))

    def box(self, u, v, w, h, d, fn):
        for name, (x0, y0, fw, fh) in faces(u, v, w, h, d).items():
            for y in range(fh):
                for x in range(fw):
                    r = fn(name, x, y, fw, fh)
                    if r is None:
                        continue
                    c, g = (r if isinstance(r, tuple) and len(r) == 2 and isinstance(r[0], tuple) else (r, False))
                    self.px(x0 + x, y0 + y, c, glow=g)


def paint(kind):
    shell, joint = PALETTES[kind]
    node = kind == "hive_node"
    t = Tex()

    def plates(face, x, y, w, h, along_rows=True):
        """Placas: cada 2 filas (o columnas) una junta oscura y el canto siguiente iluminado."""
        k = y if along_rows else x
        if face == "bottom":
            return shell[0]
        if k % 2 == 0:
            return shell[2] if face == "top" else shell[1]
        return shell[1] if face == "top" else shell[0]

    # tórax 4x4x5 (u0,v0): placas dorsales, junta de cobre en los costados
    def thorax(face, x, y, w, h):
        if face in ("right", "left") and y == h - 1:
            return joint[1]
        if face == "top" and x in (0, w - 1):
            return shell[3]                                          # cantos iluminados
        return plates(face, x, y, w, h, along_rows=face == "top")
    t.box(0, 0, 4, 4, 5, thorax)

    # cintura 2x2x2 (10,17): cobre
    t.box(10, 17, 2, 2, 2, lambda f, x, y, w, h: joint[2] if y == 0 else joint[1])

    # cabeza 3x3x3 (0,10): ojos compuestos cian a los lados de la cara, mandíbulas abajo
    def headf(face, x, y, w, h):
        if face == "front":
            if y < 2 and x in (0, 2):
                return (CYAN[2] if y == 0 else CYAN[1]), True
            return joint[1] if y == 2 else shell[1]
        if face in ("left", "right") and y < 2 and x == 0:
            return CYAN[1], True                                    # el ojo envuelve el lateral
        if face == "top":
            return shell[2] if (x + y) % 2 == 0 else shell[1]
        return shell[1] if face != "bottom" else shell[0]
    t.box(0, 10, 3, 3, 3, headf)

    # mandíbulas 1x1x1 (36,13), antenas 1x1x3 (26,13), patas 1x4x1 (20,13)
    t.box(36, 13, 1, 1, 1, lambda f, x, y, w, h: COPPER[2])
    t.box(26, 13, 1, 1, 3, lambda f, x, y, w, h: shell[2] if x == w - 1 else shell[1])
    t.box(20, 13, 1, 4, 1, lambda f, x, y, w, h: joint[2] if y == 0 else joint[0] if y == h - 1 else (shell[1] if y % 2 else shell[2]))

    # abdomen 5x5x7 (20,0): segmentos a lo largo; bandas de cinetrita brillantes cada 3 segmentos
    def abdomen(face, x, y, w, h):
        if face == "front":
            return shell[0]
        if face == "back":                                          # punta: núcleo
            if 1 <= x <= w - 2 and 1 <= y <= h - 2:
                return (CYAN[2] if (x, y) == (w // 2, h // 2) else CYAN[1]), True
            return shell[1]
        seg = y if face in ("top", "bottom") else x                 # índice a lo largo del cuerpo
        if seg % 3 == 1 and face != "bottom":
            if node or (face == "top" and 1 <= x <= w - 2) or face in ("left", "right"):
                return (CYAN[1] if seg % 2 else CYAN[0]), True
        if face == "bottom":
            return joint[1] if seg % 2 == 0 else joint[0]
        edge = (face == "top" and x in (0, w - 1))
        return shell[3] if edge and seg % 3 == 0 else shell[2] if seg % 3 == 0 else shell[1]
    t.box(20, 0, 5, 5, 7, abdomen)

    # aguijón 1x1x3 (0,17): cobre que se aclara hacia la punta
    t.box(0, 17, 1, 1, 3, lambda f, x, y, w, h: COPPER[2] if f == "back" else COPPER[1])

    # alas (44,0) 7x0x3 y (44,4) 6x0x3: membrana translúcida con venas y borde más opaco
    for (u, v, w) in ((44, 0, 7), (44, 4, 6)):
        for name, (x0, y0, fw, fh) in faces(u, v, w, 0, 3).items():
            for y in range(fh):
                for x in range(fw):
                    edge = x == fw - 1 or y in (0, fh - 1)
                    vein = x % 3 == 0 or y == 1
                    c = (150, 200, 210) if node else (170, 210, 220)
                    a = 200 if edge else 150 if vein else 90
                    if vein and not edge:
                        c = (90, 120, 130)
                    t.px(x0 + x, y0 + y, c, a)
    return t


def swarm_core():
    """Icono 16x16: núcleo hexagonal de cinetrita en una jaula de cobre."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    hexa = ["......XXXX......", "....XXccccXX....", "...XccCCCCccX...", "..XcCCWWWWCCcX..", "..XcCWWWWWWCcX..",
            ".XcCWWWWWWWWCcX.", ".XcCWWWWWWWWCcX.", ".XcCCWWWWWWCCcX.", ".XccCCWWWWCCccX.", "..XccCCCCCCccX..",
            "..XXccccccccXX..", "...XXccccccXX...", "....XXXXXXXX....", "................", "................",
            "................"]
    col = {"X": COPPER[1], "c": CYAN[0], "C": CYAN[1], "W": CYAN[2]}
    for y, row in enumerate(hexa):
        for x, ch in enumerate(row):
            if ch in col:
                img.putpixel((x, y + 2 if y < 13 else y), (*col[ch], 255))
    for (x, y) in [(3, 7), (12, 7), (7, 2), (8, 14)]:                # remaches de la jaula
        img.putpixel((x, y), (*COPPER[2], 255))
    return img


def guided_round():
    """Icono 16x16 de la Munición Guiada: bala en diagonal, vaina de cobre, punta de cinetrita y aletas guía."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rows = ["................", "............WW..", "...........WCCc.", "..........WCCcc.", ".........RCCcc..",
            "........XrRcc...", ".......XXXr.....", "......XXXXX.....", ".....XXXXX......", "....XXXXX.......",
            "..FXXXXX........", ".FFXXXX.........", "..FFXX..........", "..FF............", "................",
            "................"]
    col = {"X": COPPER[1], "W": CYAN[2], "C": CYAN[1], "c": CYAN[0], "R": (190, 40, 40), "r": (120, 24, 24),
           "F": (70, 74, 86)}
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in col:
                img.putpixel((x, y), (*col[ch], 255))
    for (x, y) in [(6, 7), (5, 8), (4, 9)]:                         # brillo del cobre
        img.putpixel((x, y), (*COPPER[2], 255))
    return img


if __name__ == "__main__":
    (ROOT / "entity/swarm_wasp").mkdir(parents=True, exist_ok=True)
    glow = None
    previews = []
    for kind in ("scout", "stinger", "guard"):
        t = paint(kind)
        t.img.save(ROOT / f"entity/swarm_wasp/{kind}.png")
        glow = t.glow
        previews.append(t.img)
    glow.save(ROOT / "entity/swarm_wasp/glow.png")
    n = paint("hive_node")
    n.img.save(ROOT / "entity/hive_node.png")
    n.glow.save(ROOT / "entity/hive_node_glow.png")
    previews.append(n.img)
    swarm_core().save(ROOT / "item/swarm_core.png")
    guided_round().save(ROOT / "item/guided_round.png")
    if len(sys.argv) > 1:
        sheet = Image.new("RGBA", (64 * 8 * 2 + 8, 32 * 8 * 2 + 8), (48, 50, 56, 255))
        for i, p in enumerate(previews):
            sheet.alpha_composite(p.resize((512, 256), Image.NEAREST), ((i % 2) * 520, (i // 2) * 264))
        sheet.save(sys.argv[1])
    print("ok: texturas del enjambre")
