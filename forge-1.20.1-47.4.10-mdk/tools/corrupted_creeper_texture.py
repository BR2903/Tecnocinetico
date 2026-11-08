"""Texturas del Creeper Corrupto (modelo vanilla de creeper, 64x32):
textures/entity/corrupted_creeper.png y corrupted_creeper_glow.png (grietas y ojos de cinetrita; el
renderizador las hace brillar más cuanto más hinchado está).

Uso (desde forge-1.20.1-47.4.10-mdk):  python tools/corrupted_creeper_texture.py [preview.png]
Estilo: carne gris verdosa moteada y agrietada, una red de grietas cian que la recorre, dos ojos-cristal
alargados y una boca rota.
"""
import random
import sys
from pathlib import Path
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from turret_texture import ROOT, box  # noqa: E402

FLESH = [(38, 46, 40), (56, 68, 58), (74, 88, 74), (92, 106, 88)]
CRACK = [(26, 110, 124), (52, 190, 206), (150, 240, 250)]
DARK = (14, 16, 16)


def cracks(seed, w, h, n):
    """Grietas: caminos irregulares que bajan por la cara (núcleo brillante + borde oscuro)."""
    r = random.Random(seed)
    core, edge = set(), set()
    for _ in range(n):
        x, y = r.randrange(w), r.randrange(max(1, h // 3))
        for _ in range(r.randint(3, h)):
            core.add((x, y))
            x = max(0, min(w - 1, x + r.choice((-1, 0, 0, 1))))
            y += 1
            if y >= h:
                break
    for (x, y) in core:
        for dx, dy in ((1, 0), (-1, 0)):
            if (x + dx, y + dy) not in core:
                edge.add((x + dx, y + dy))
    return core, edge


def mottled(face, x, y, h, seed):
    k = (x * 5 + y * 7 + seed * 13 + len(face) * 3) % 9
    base = 2 if y <= 1 else 1 if y < h - 1 else 0
    if face == "top":
        base = 2
    if face == "bottom":
        base = 0
    if k == 0:
        return FLESH[max(0, base - 1)]
    if k == 4:
        return FLESH[min(3, base + 1)]
    return FLESH[base]


def build():
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    glow = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    face_cracks = {f: cracks(i + 1, 8, 8, 2) for i, f in enumerate(("right", "left", "back", "top"))}

    # cabeza 8x8x8: ojos-cristal alargados, boca rota con astillas
    def head(face, x, y, w, h):
        if face == "front":
            if x in (1, 2, 5, 6) and 2 <= y <= 3:
                if x in (2, 5):
                    return CRACK[2 if y == 2 else 1], True                # ojo: cristal brillante
                return DARK
            if y == 4 and x in (2, 5):
                return CRACK[0], True
            if y == 5 and 2 <= x <= 5:
                return DARK                                              # boca
            if y == 6 and x in (2, 3, 5):
                return DARK if x != 3 else (CRACK[1], True)
            if y == 7 and x == 4:
                return DARK
            return mottled(face, x, y, h, 1)
        core, edge = face_cracks.get(face, (set(), set()))
        if (x, y) in core:
            return CRACK[1], True
        if (x, y) in edge:
            return DARK
        return mottled(face, x, y, h, 2)
    box(img, glow, 0, 0, 8, 8, 8, head)

    # cuerpo 8x12x4: grietas grandes en frente y espalda, bulto de cristal en el pecho
    body_cracks = {f: cracks(10 + i, w, 12, n) for i, (f, w, n) in
                   enumerate((("front", 8, 3), ("back", 8, 3), ("left", 4, 1), ("right", 4, 1)))}

    def body(face, x, y, w, h):
        if face == "front" and (x, y) in ((3, 4), (4, 4), (3, 5), (4, 5)):
            return CRACK[2 if (x + y) % 2 else 1], True
        core, edge = body_cracks.get(face, (set(), set()))
        if (x, y) in core:
            return CRACK[1 if y % 3 else 2], True
        if (x, y) in edge:
            return DARK
        return mottled(face, x, y, h, 3)
    box(img, glow, 16, 16, 8, 12, 4, body)

    # patas 4x6x4: más oscuras, puntas ennegrecidas
    def leg(face, x, y, w, h):
        if face == "bottom" or y >= h - 1:
            return DARK
        if face == "front" and (x, y) == (1, 2):
            return CRACK[0], True
        return mottled(face, x, y, h, 4) if y < h - 2 else FLESH[0]
    box(img, glow, 0, 16, 4, 6, 4, leg)
    return img, glow


if __name__ == "__main__":
    img, glow = build()
    img.save(ROOT / "corrupted_creeper.png")
    glow.save(ROOT / "corrupted_creeper_glow.png")
    if len(sys.argv) > 1:
        prev = Image.new("RGBA", (512, 256), (48, 50, 56, 255))
        prev.alpha_composite(img.resize((512, 256), Image.NEAREST))
        prev.save(sys.argv[1])
    print("ok: creeper corrupto")
