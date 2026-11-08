"""Texturas de los Corruptos (supervivientes infectados por la cinetrita), formato de piel de jugador 64x64,
con una textura de brillo aparte (_glow: cristales y ojos) que se pinta a plena luz:

textures/entity/corrupted/{wanderer,worker,crystal,raider,soldier}.png (+ _glow.png)
  wanderer/worker/crystal -> Corrupto (cuerpo a cuerpo); raider/soldier -> Tirador Corrupto.

Uso (desde forge-1.20.1-47.4.10-mdk):  python tools/corrupted_skin_gen.py [preview.png]
Estilo: el de survivor_skin_gen.py (bandas de luz, poco ruido), pero piel gris azulada con venas oscuras,
ropa rota y cristales de cinetrita cian que asoman de la carne (capa exterior = relieve).
"""
import random
import sys
from pathlib import Path
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from survivor_skin_gen import (Skin, ramp_of, banded, preview, HEAD, HAT, BODY, JACKET, R_ARM, R_SLEEVE,  # noqa: E402
                               L_ARM, L_SLEEVE, R_LEG, R_PANTS, L_LEG, L_PANTS, BOOT, LEATHER, COPPER)

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tecnocinetico/textures/entity/corrupted"

FLESH = [(70, 82, 80), (92, 106, 102), (112, 126, 120), (130, 144, 136)]   # piel infectada: sombra .. luz
VEIN = (44, 58, 64)
CRYS = [(26, 110, 124), (52, 190, 206), (150, 240, 250), (220, 255, 255)]  # cinetrita
GORE = (74, 40, 40)


class CSkin(Skin):
    """Piel con capa de brillo: los píxeles marcados con glow se copian también a self.glow."""

    def __init__(self, seed):
        super().__init__()
        self.glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.r = random.Random(seed)

    def lit(self, x, y, c):
        self.px(x, y, c)
        self.glow.putpixel((x, y), (*c[:3], 255))

    def paint_g(self, part, fn):
        """Como paint, pero fn puede devolver (color, True) para que brille."""
        for name, (x0, y0, fw, fh) in self.faces(*part).items():
            for y in range(fh):
                for x in range(fw):
                    c = fn(name, x, y, fw, fh)
                    if c is None:
                        continue
                    if isinstance(c[0], tuple):
                        self.lit(x0 + x, y0 + y, c[0])
                    else:
                        self.px(x0 + x, y0 + y, c)

    def crystal_patch(self, part, face, cx, cy, size, outer=False):
        """Racimo de cristales: núcleo brillante, borde oscuro; en la capa exterior forma relieve."""
        x0, y0, w, h = self.faces(*part)[face]
        for dy in range(-size, size + 1):
            for dx in range(-size, size + 1):
                x, y = cx + dx, cy + dy
                if not (0 <= x < w and 0 <= y < h):
                    continue
                d = abs(dx) + abs(dy)
                if d > size:
                    continue
                if d == size and self.r.random() < 0.5:
                    continue
                c = CRYS[3] if d == 0 else CRYS[2] if d == 1 else CRYS[1] if (dx + dy) % 2 else CRYS[0]
                if d >= size and not outer:
                    self.px(x0 + x, y0 + y, VEIN)                          # carne ennegrecida alrededor
                else:
                    self.lit(x0 + x, y0 + y, c)


def flesh(face, x, y, w, h, veins=None):
    """Piel infectada con venas oscuras (patrón fijo por cara)."""
    if veins and (x, y) in veins:
        return VEIN
    return banded(FLESH, face, y, h)


def vein_set(r, w, h, n=3):
    out = set()
    for _ in range(n):
        x, y = r.randrange(w), r.randrange(h)
        for _ in range(r.randint(2, 4)):
            out.add((x, y))
            x = max(0, min(w - 1, x + r.choice((-1, 0, 1))))
            y = min(h - 1, y + 1)
    return out


def corrupt_head(s, hair=True, mouth_gore=True):
    """Cabeza: piel gris azulada, ojos hundidos que brillan cian, boca oscura, pelo ralo."""
    veins = vein_set(s.r, 8, 8, 1)
    hair_c = [(30, 28, 26), (44, 40, 36)]

    def fn(face, x, y, w, h):
        if face == "top":
            if hair and s.r.random() < 0.7:
                return hair_c[(x + y) % 2]
            return FLESH[1]
        if face == "bottom":
            return FLESH[0]
        if face == "back":
            return hair_c[1] if hair and y < 5 and (x + y) % 4 else flesh(face, x, y, w, h, veins)
        if face in ("right", "left"):
            if hair and y < 2:
                return hair_c[0]
            return flesh(face, x, y, w, h, veins)
        # frente
        if hair and y == 0:
            return hair_c[0] if x % 3 else FLESH[1]
        if y == 3 and x in (1, 2, 5, 6):
            return FLESH[0]                                          # cuencas hundidas
        if y == 4 and x in (1, 2, 5, 6):
            return (CRYS[2], True) if x in (2, 5) else (VEIN)        # ojos cian
        if y == 5 and x in (3, 4):
            return FLESH[0]
        if y == 6 and 2 <= x <= 5:
            return (20, 16, 16) if mouth_gore and x in (3, 4) else GORE
        if y == 7 and x in (3, 4) and mouth_gore:
            return GORE
        return flesh(face, x, y, w, h, veins)
    s.paint_g(HEAD, fn)


_HOLES = {}


def holes_of(key, w, h, amount):
    """Desgarros agrupados (1-3 por cara) en vez de puntos sueltos: bordes oscuros y piel dentro."""
    if key not in _HOLES:
        r = random.Random(str(key))
        out = {}
        for _ in range(max(0, round(amount * w * h / 5))):
            cx, cy = r.randrange(w), r.randrange(h)
            for dx, dy in ((0, 0), (1, 0), (0, 1), (r.choice((-1, 1)), 0)):
                out[(cx + dx, cy + dy)] = FLESH[1]
            out.setdefault((cx, cy - 1), VEIN)
        _HOLES[key] = out
    return _HOLES[key]


def torn(face, x, y, w, h, base_ramp, r_seed, holes=0.12, rows=None):
    """Tela rota: color de la tela con desgarros por los que se ve la piel."""
    c = holes_of((r_seed, face, w, h), w, rows or h, holes).get((x, y))
    if c is not None:
        return c
    return banded(base_ramp, face, y, rows or h)


def limbs(s, sleeve=None, sleeve_rows=6, pants=(46, 50, 58), boots=True, seed=0):
    """Brazos (manga rota hasta sleeve_rows, luego piel con venas y manos oscurecidas) y piernas."""
    sr = ramp_of(sleeve) if sleeve else None
    pr = ramp_of(pants)
    vs = vein_set(s.r, 4, 12, 1)

    def arm(face, x, y, w, h):
        if face == "top":
            return sr[1] if sr else FLESH[2]
        if face == "bottom":
            return FLESH[0]
        if sr and y < sleeve_rows:
            if y == sleeve_rows - 1 and (x + seed) % 2:
                return FLESH[1]                                          # borde deshilachado
            return torn(face, x, y, w, h, sr, seed, 0.06, sleeve_rows)
        if y >= 10:
            return FLESH[0] if (x + y) % 3 else VEIN                    # manos ennegrecidas
        return flesh(face, x, y, w, h, vs)
    s.paint_g(R_ARM, arm)
    s.paint_g(L_ARM, arm)

    def leg(face, x, y, w, h):
        if boots and y >= h - 3:
            return BOOT[2] if face == "front" and y == h - 3 else BOOT[1] if face != "bottom" else BOOT[0]
        if face == "front" and 5 <= y <= 6 and (x + seed) % 4 in (0, 1):
            return FLESH[1] if y == 6 else VEIN                          # rodilla rota
        return torn(face, x, y, w, h, pr, seed + 3, 0.05, h - 3)
    s.paint_g(R_LEG, leg)
    s.paint_g(L_LEG, leg)


# --------------------------------------------------------------------------- variantes

def wanderer():
    """Errante: sudadera gris rota, vaqueros oscuros; cristales en el hombro izquierdo y la mejilla."""
    s = CSkin(11)
    corrupt_head(s)
    hoodie = ramp_of((86, 88, 92))

    def body(face, x, y, w, h):
        if y >= 10:
            return (46, 50, 58) if face != "bottom" else (36, 40, 46)
        if face == "front" and 3 <= y <= 5 and 2 <= x <= 5:
            return hoodie[0] if y == 3 else hoodie[1]                     # bolsillo canguro
        if face == "front" and y == 7 and x == 6:
            return GORE
        return torn(face, x, y, w, h, hoodie, 1, 0.1, 10)
    s.paint_g(BODY, body)
    # capucha caída sobre la espalda (capa exterior del torso)
    s.paint_g(JACKET, lambda f, x, y, w, h: hoodie[0] if f == "back" and y <= 2 else None)
    limbs(s, sleeve=(86, 88, 92), sleeve_rows=7, seed=1)
    s.crystal_patch(HEAD, "right", 4, 5, 1)
    s.crystal_patch(L_SLEEVE, "top", 1, 1, 2, outer=True)
    s.crystal_patch(L_SLEEVE, "front", 2, 1, 2, outer=True)
    s.crystal_patch(L_SLEEVE, "left", 1, 1, 1, outer=True)
    s.crystal_patch(BODY, "back", 5, 3, 1)
    return s


def worker():
    """Obrero: casco amarillo abollado, mono azul oscuro con chaleco reflectante naranja; cristales en la espalda."""
    s = CSkin(23)
    corrupt_head(s, hair=False)
    yellow = ramp_of((196, 160, 40))
    s.paint_g(HAT, lambda f, x, y, w, h: (yellow[2] if y == 0 else yellow[1]) if f == "top"
              else (yellow[1] if y <= 1 else None) if f in ("front", "back", "left", "right") else None)
    coverall = ramp_of((48, 60, 82))
    vest = ramp_of((214, 104, 32))

    def body(face, x, y, w, h):
        if y >= 10:
            return coverall[0]
        if face in ("front", "back") and y in (4, 5):
            return (200, 200, 190) if (x + y) % 3 else (150, 150, 140)    # banda reflectante sucia
        if face in ("front", "back", "left", "right") and not (face == "front" and x in (3, 4)):
            return torn(face, x, y, w, h, vest, 2, 0.08, 10)
        return torn(face, x, y, w, h, coverall, 2, 0.08, 10)
    s.paint_g(BODY, body)
    limbs(s, sleeve=(48, 60, 82), sleeve_rows=9, pants=(48, 60, 82), seed=2)
    s.crystal_patch(JACKET, "back", 2, 6, 2, outer=True)
    s.crystal_patch(JACKET, "back", 6, 2, 1, outer=True)
    s.crystal_patch(JACKET, "top", 5, 2, 1, outer=True)
    s.crystal_patch(R_ARM, "front", 1, 8, 1)
    return s


def crystal():
    """Cristalizado: casi devorado por la cinetrita; torso desnudo con costillas, racimos grandes que brillan."""
    s = CSkin(37)
    corrupt_head(s, hair=False)

    def body(face, x, y, w, h):
        if y >= 10:
            return (38, 36, 34) if face != "bottom" else (30, 28, 26)    # pantalón hecho jirones
        if face == "front" and y in (3, 5, 7) and x not in (3, 4):
            return FLESH[0]                                              # costillas marcadas
        return flesh(face, x, y, w, h, {(3, 1), (4, 2), (3, 3)})
    s.paint_g(BODY, body)
    limbs(s, sleeve=None, pants=(38, 36, 34), boots=False, seed=3)
    s.crystal_patch(HAT, "top", 3, 3, 3, outer=True)
    s.crystal_patch(HAT, "left", 3, 2, 2, outer=True)
    s.crystal_patch(HEAD, "front", 6, 2, 1)
    s.crystal_patch(JACKET, "front", 5, 4, 2, outer=True)
    s.crystal_patch(JACKET, "back", 3, 5, 3, outer=True)
    s.crystal_patch(R_SLEEVE, "top", 1, 1, 2, outer=True)
    s.crystal_patch(R_SLEEVE, "right", 2, 2, 2, outer=True)
    s.crystal_patch(L_SLEEVE, "back", 2, 7, 2, outer=True)
    s.crystal_patch(L_PANTS, "front", 2, 4, 1, outer=True)
    return s


def raider():
    """Saqueador (tirador): máscara antigás con filtro de cobre, chaqueta de cuero, bandolera de cartuchos."""
    s = CSkin(41)
    corrupt_head(s, mouth_gore=False)
    mask = [(30, 30, 32), (48, 48, 52), (70, 70, 76)]

    def hat(face, x, y, w, h):
        if face == "front":
            if y == 4 and x in (1, 2, 5, 6):
                return (CRYS[1], True) if x in (2, 5) else mask[0]       # lentes: los ojos brillan detrás
            if y == 4 and x in (3, 4):
                return mask[1]
            if 5 <= y <= 7 and 2 <= x <= 5:
                return COPPER[2] if (y == 6 and x in (3, 4)) else COPPER[1] if y == 6 else mask[1]  # filtro
            if y in (3, 5) and 1 <= x <= 6:
                return mask[2] if y == 3 else mask[1]
            return None
        if face in ("left", "right") and y == 4:
            return LEATHER[0]                                            # correa
        if face == "back" and y == 4:
            return LEATHER[0]
        return None
    s.paint_g(HAT, hat)

    def body(face, x, y, w, h):
        if y >= 10:
            return (52, 46, 40)
        if face == "front" and (x == y - 1 or x == y):                    # bandolera
            return (170, 40, 36) if (x + y) % 2 else LEATHER[0]
        if face == "back" and (x == 8 - y or x == 9 - y):
            return LEATHER[0]
        return torn(face, x, y, w, h, LEATHER, 4, 0.07, 10)
    s.paint_g(BODY, body)
    limbs(s, sleeve=LEATHER[1], sleeve_rows=10, pants=(58, 52, 44), seed=4)
    s.crystal_patch(HEAD, "left", 3, 5, 1)
    s.crystal_patch(R_SLEEVE, "top", 2, 2, 1, outer=True)
    s.crystal_patch(JACKET, "back", 6, 7, 2, outer=True)
    return s


def soldier():
    """Soldado (tirador): casco oliva con el visor roto (cristal cian asomando), chaleco antibalas, pantalón de camuflaje."""
    s = CSkin(53)
    corrupt_head(s, hair=False)
    olive = ramp_of((76, 84, 56))

    def hat(face, x, y, w, h):
        if face == "top":
            return olive[2] if (x + y) % 4 else olive[1]
        if face == "bottom":
            return None
        if face == "front":
            if y <= 1:
                return olive[1] if y == 0 else olive[0]
            return None
        return olive[1] if y <= 3 else (olive[0] if y == 4 else None)
    s.paint_g(HAT, hat)
    vest = ramp_of((60, 64, 52))
    camo = [(70, 76, 52), (52, 58, 40), (92, 88, 62)]

    def body(face, x, y, w, h):
        if y >= 10:
            return camo[(x // 2 + y) % 3]
        if face == "front" and 6 <= y <= 8 and x in (1, 2, 5, 6):
            return vest[0] if y == 6 else (60, 52, 36)                   # bolsillos de cargador
        return torn(face, x, y, w, h, vest, 5, 0.05, 10)
    s.paint_g(BODY, body)
    limbs(s, sleeve=(70, 76, 52), sleeve_rows=8, pants=(70, 76, 52), seed=5)
    # camuflaje en las piernas
    for part in (R_LEG, L_LEG):
        for face in ("front", "back", "left", "right"):
            x0, y0, w, h = s.faces(*part)[face]
            for y in range(h - 3):
                for x in range(w):
                    if (x * 3 + y * 5 + x0) % 7 < 2:
                        s.px(x0 + x, y0 + y, camo[1 + (y % 2)])
    s.crystal_patch(HAT, "front", 2, 1, 2, outer=True)
    s.crystal_patch(JACKET, "left", 2, 3, 1, outer=True)
    s.crystal_patch(L_SLEEVE, "front", 1, 4, 1, outer=True)
    return s


SKINS = {"wanderer": wanderer, "worker": worker, "crystal": crystal, "raider": raider, "soldier": soldier}

if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    imgs = []
    for name, fn in SKINS.items():
        s = fn()
        s.img.save(OUT / f"{name}.png")
        s.glow.save(OUT / f"{name}_glow.png")
        imgs.append(s.img)
    if len(sys.argv) > 1:
        preview(imgs, sys.argv[1])
    print("ok: pieles de corruptos")
