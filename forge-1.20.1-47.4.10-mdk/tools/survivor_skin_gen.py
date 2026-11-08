"""Genera las texturas (64x64, formato de piel de jugador) de los Supervivientes:
textures/entity/survivor/{scrapper,gunsmith,farmer}.png y una vista previa (frente y espalda).

Uso (desde forge-1.20.1-47.4.10-mdk):  python tools/survivor_skin_gen.py [preview.png]

Estilo: ropa gastada post-apocalíptica con acentos del mod (cian de cinetrita, cobre), formas limpias,
sombreado por bandas (luz arriba, sombra abajo) y poco ruido.
"""
import random
import sys
from pathlib import Path
from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tecnocinetico/textures/entity/survivor"

# --------------------------------------------------------------------------- paletas
SKIN = [(150, 98, 70), (178, 122, 90), (200, 144, 108), (220, 166, 128)]   # sombra .. luz
HAIR = [(40, 30, 24), (58, 44, 34), (78, 60, 46)]
CYAN = [(30, 120, 132), (60, 200, 214), (160, 240, 248)]
COPPER = [(110, 60, 36), (168, 98, 58), (214, 146, 96)]
BOOT = [(30, 26, 24), (44, 38, 34), (62, 54, 48)]
LEATHER = [(70, 44, 30), (98, 64, 42), (128, 86, 56)]
EYE_W, EYE_P = (232, 232, 226), (36, 40, 46)


def ramp_of(base):
    """4 tonos a partir de un color base (sombra, base, luz, brillo)."""
    def k(c, f):
        return tuple(max(0, min(255, int(v * f))) for v in c)
    return [k(base, 0.72), base, k(base, 1.16), k(base, 1.32)]


class Skin:
    def __init__(self):
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))

    def px(self, x, y, c, a=255):
        if c is None:
            return
        self.img.putpixel((x, y), (*c[:3], a))

    def faces(self, u, v, w, h, d):
        """Rectángulos de las 6 caras de una caja con UV en (u, v): nombre -> (x0, y0, ancho, alto)."""
        return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
                "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}

    def paint(self, u, v, w, h, d, fn):
        """fn(cara, x, y, ancho, alto) -> color o None, para cada píxel de cada cara."""
        for name, (x0, y0, fw, fh) in self.faces(u, v, w, h, d).items():
            for y in range(fh):
                for x in range(fw):
                    c = fn(name, x, y, fw, fh)
                    if c is not None:
                        self.px(x0 + x, y0 + y, c)


# piezas del modelo de jugador (u, v, ancho, alto, fondo)
HEAD, HAT = (0, 0, 8, 8, 8), (32, 0, 8, 8, 8)
BODY, JACKET = (16, 16, 8, 12, 4), (16, 32, 8, 12, 4)
R_ARM, R_SLEEVE = (40, 16, 4, 12, 4), (40, 32, 4, 12, 4)
L_ARM, L_SLEEVE = (32, 48, 4, 12, 4), (48, 48, 4, 12, 4)
R_LEG, R_PANTS = (0, 16, 4, 12, 4), (0, 32, 4, 12, 4)
L_LEG, L_PANTS = (16, 48, 4, 12, 4), (0, 48, 4, 12, 4)


def banded(r, face, y, h):
    """Sombreado por bandas: arriba algo más claro, abajo más oscuro, caras laterales un poco más oscuras."""
    i = 2 if y == 0 else 0 if y >= h - 1 else 1
    if face in ("right", "left", "back") and i > 0:
        i -= 0 if face == "back" else 0
    if face == "bottom":
        i = 0
    return r[i]


# --------------------------------------------------------------------------- partes comunes

def head(s, hair_rows=2, beard=False, skin=SKIN):
    """Cara: piel, pelo arriba y detrás, ojos con blanco y pupila, cejas, boca."""
    def fn(face, x, y, w, h):
        if face == "top":
            return HAIR[1] if (x + y) % 5 else HAIR[2]
        if face == "bottom":
            return skin[0]
        if face == "back":
            return HAIR[1] if y < 6 else HAIR[0] if y == 6 else skin[1]
        if face in ("right", "left"):
            if y < hair_rows + 1 or (y < 6 and (face == "right" and x < 3 or face == "left" and x > 4)):
                return HAIR[1]
            return skin[1] if y < 7 else skin[0]
        # frente
        if y < hair_rows:
            return HAIR[1] if y == 0 else HAIR[2] if x % 3 else HAIR[1]
        if y == 3 and x in (1, 2, 5, 6):
            return HAIR[0]                                          # cejas
        if y == 4:
            return {1: EYE_W, 2: EYE_P, 5: EYE_P, 6: EYE_W}.get(x, skin[2])
        if y == 5 and x in (3, 4):
            return skin[1]                                          # nariz (sombra)
        if y == 6 and x in (3, 4):
            return (120, 70, 56)                                    # boca
        if beard and y >= 6:
            return HAIR[1] if (x + y) % 2 else HAIR[0]
        return skin[3] if (y == 2 and 2 < x < 6) else skin[2] if y < 6 else skin[1]
    s.paint(*HEAD, fn)


def legs(s, pants, boot_rows=3, knee_patch=None):
    pr = ramp_of(pants)

    def fn(face, x, y, w, h):
        if y >= h - boot_rows:
            if face == "front" and y == h - boot_rows:
                return BOOT[2]
            return BOOT[1] if face != "bottom" else BOOT[0]
        if knee_patch and face == "front" and 5 <= y <= 7 and 1 <= x <= 2:
            return knee_patch
        if face == "front" and x == 0:
            return pr[0]                                            # pliegue interior
        return banded(pr, face, y, h - boot_rows)
    s.paint(*R_LEG, fn)
    s.paint(*L_LEG, fn)


def arms(s, sleeve, hand=SKIN, sleeve_rows=12, glove=None, cuff=None):
    sr = ramp_of(sleeve)

    def fn(face, x, y, w, h):
        if face == "bottom":
            return glove[0] if glove else hand[1]
        if y >= sleeve_rows:
            if glove and y >= 10:
                return glove[1] if face != "back" else glove[0]
            return hand[2] if face == "front" else hand[1]
        if cuff and y == sleeve_rows - 1:
            return cuff
        return banded(sr, face, y, sleeve_rows)
    s.paint(*R_ARM, fn)
    s.paint(*L_ARM, fn)


# --------------------------------------------------------------------------- oficios

def scrapper():
    """Chatarrero: capucha verde oliva, gafas de cobre con cristales cian en la frente, pañuelo gris,
    chaqueta remendada con correa cruzada y riñonera, pantalón de carga."""
    s = Skin()
    olive = (86, 92, 56)
    orr = ramp_of(olive)
    head(s, hair_rows=2)

    # capucha (capa exterior de la cabeza): marco alrededor de la cara + gafas en la frente
    def hood(face, x, y, w, h):
        if face == "front":
            if y <= 1 or x == 0 or x == 7:
                return orr[1] if y == 0 or x in (0, 7) else orr[2]
            if y == 2:                                              # gafas
                if x in (1, 2, 5, 6):
                    return CYAN[1] if x in (1, 5) else CYAN[2]
                return COPPER[1]
            if y >= 5:                                              # pañuelo sobre boca y nariz
                return (112, 112, 104) if y == 5 else (94, 94, 88)
            return None
        if face == "bottom":
            return None
        return orr[1] if (x + y) % 6 else orr[0]
    s.paint(*HAT, hood)

    # torso: chaqueta oliva, correa cruzada de cuero, cinturón con hebilla de cobre
    def body(face, x, y, w, h):
        if y >= 10:
            return (52, 50, 44) if face != "bottom" else (44, 42, 38)  # pantalón bajo la chaqueta
        if y == 9:
            return COPPER[2] if face == "front" and x in (3, 4) else LEATHER[1]
        if face == "front":
            if x == y - 1 or x == y:                                # correa en diagonal
                return LEATHER[2] if x == y - 1 else LEATHER[0]
            if x in (3, 4) and y < 9:
                return orr[0]                                       # cremallera
            if 5 <= x <= 6 and 5 <= y <= 7:
                return (110, 96, 64)                                # remiendo
        if face == "back" and 2 <= x <= 5 and 1 <= y <= 7:
            return LEATHER[1] if (x in (2, 5) or y in (1, 7)) else LEATHER[2]  # mochila
        return banded(orr, face, y, 9)
    s.paint(*BODY, body)

    # capa exterior del torso: cuello del pañuelo
    def jacket(face, x, y, w, h):
        if y == 0 and face in ("front", "back", "left", "right"):
            return (106, 106, 98)
        return None
    s.paint(*JACKET, jacket)

    arms(s, olive, sleeve_rows=10, glove=[(40, 38, 34), (58, 54, 48)], cuff=orr[0])
    legs(s, (64, 62, 50), knee_patch=(96, 88, 64))
    # bolsillos laterales del pantalón de carga
    for (u, v, w, h, d) in (R_LEG, L_LEG):
        x0, y0, _, _ = s.faces(u, v, w, h, d)["right"]
        for yy in range(3, 6):
            for xx in range(1, 3):
                s.px(x0 + xx, y0 + yy, (78, 76, 60))
    return s.img


def gunsmith():
    """Armero: pelo corto, barba, gafas de soldar oscuras en la frente, camisa gris con mangas remangadas,
    delantal de cuero con bolsillo y una célula de cinetrita brillante, guantes gruesos."""
    s = Skin()
    shirt = (112, 114, 110)
    shr = ramp_of(shirt)
    head(s, hair_rows=1, beard=True)

    def goggles(face, x, y, w, h):
        if face == "front" and y == 2:
            return (24, 30, 34) if x in (1, 2, 5, 6) else LEATHER[0]
        if face in ("left", "right", "back") and y == 2:
            return LEATHER[0]                                       # correa alrededor de la cabeza
        return None
    s.paint(*HAT, goggles)

    def body(face, x, y, w, h):
        if y == 9:
            return LEATHER[0] if not (face == "front" and x in (3, 4)) else COPPER[1]
        if y >= 10:
            return (58, 56, 52)
        if face == "front" and 1 <= x <= 6 and y >= 2:              # delantal
            if y == 2 or x in (1, 6):
                return LEATHER[2] if y == 2 else LEATHER[0]
            if 2 <= x <= 5 and 5 <= y <= 7:                         # bolsillo con célula de cinetrita
                if y == 5:
                    return LEATHER[0]
                if x == 4 and y == 6:
                    return CYAN[2]
                if x == 4 and y == 7:
                    return CYAN[1]
                return LEATHER[1]
            return LEATHER[1]
        if face == "front" and x in (2, 5) and y < 2:
            return LEATHER[0]                                       # tirantes
        if face == "back" and x in (2, 5):
            return LEATHER[0]
        return banded(shr, face, y, 9)
    s.paint(*BODY, body)
    arms(s, shirt, sleeve_rows=5, glove=[(48, 40, 34), (72, 60, 50)], cuff=shr[2])
    legs(s, (54, 58, 64))
    return s.img


def farmer():
    """Granjero: pañuelo rojo desteñido en la cabeza, poncho de lana a rayas sobre camisa, pantalón verde
    oliva, botas."""
    s = Skin()
    cloth = (150, 74, 56)
    cr = ramp_of(cloth)
    head(s, hair_rows=2, skin=[(120, 76, 54), (146, 96, 68), (168, 116, 84), (186, 134, 100)])

    def scarf(face, x, y, w, h):
        if face == "top":
            return cr[1] if (x + y) % 4 else cr[2]
        if face == "bottom":
            return None
        if face == "front":
            return (cr[2] if y == 0 else cr[1]) if y <= 1 else None
        if face == "back":
            if y <= 2 or (y <= 4 and 3 <= x <= 4):
                return cr[1] if y != 2 else cr[0]                   # nudo
            return None
        return cr[1] if y <= 1 else None
    s.paint(*HAT, scarf)

    wool = (172, 156, 120)
    wr = ramp_of(wool)

    def body(face, x, y, w, h):
        if y >= 10:
            return (70, 82, 52)
        if y in (3, 7):
            return cr[1]                                            # rayas del poncho
        if y == 8 and face in ("front", "back") and x % 2 == 0:
            return wr[0]                                            # flecos
        if face == "front" and y < 2 and x in (3, 4):
            return SKIN[1]                                          # cuello abierto
        return banded(wr, face, y, 9)
    s.paint(*BODY, body)
    arms(s, wool, sleeve_rows=9, cuff=cr[1], hand=[(120, 76, 54), (146, 96, 68), (168, 116, 84), (186, 134, 100)])
    legs(s, (70, 82, 52), knee_patch=(92, 100, 66))
    return s.img


SKINS = {"scrapper": scrapper, "gunsmith": gunsmith, "farmer": farmer}


# --------------------------------------------------------------------------- vista previa

def preview(skins, path):
    """Frente y espalda de cada piel (cabeza + capa, torso + capa, brazos, piernas) a 8x."""
    sc = 8
    tiles = []
    for img in skins:
        for side in ("front", "back"):
            t = Image.new("RGBA", (16, 32), (48, 50, 56, 255))
            s = Skin()

            def face(part):
                x0, y0, w, h = s.faces(*part)[side]
                return img.crop((x0, y0, x0 + w, y0 + h))
            for base, over, pos in ((HEAD, HAT, (4, 0)), (BODY, JACKET, (4, 8)), (R_ARM, R_SLEEVE, (0, 8) if side == "front" else (12, 8)),
                                    (L_ARM, L_SLEEVE, (12, 8) if side == "front" else (0, 8)),
                                    (R_LEG, R_PANTS, (4, 20) if side == "front" else (8, 20)),
                                    (L_LEG, L_PANTS, (8, 20) if side == "front" else (4, 20))):
                b = face(base)
                t.alpha_composite(b, pos)
                t.alpha_composite(face(over), pos)
            tiles.append(t.resize((16 * sc, 32 * sc), Image.NEAREST))
    W = len(tiles) * (16 * sc + 12)
    sheet = Image.new("RGBA", (W, 32 * sc), (48, 50, 56, 255))
    for i, t in enumerate(tiles):
        sheet.paste(t, (i * (16 * sc + 12), 0))
    sheet.save(path)


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    imgs = []
    for name, fn in SKINS.items():
        img = fn()
        img.save(OUT / f"{name}.png")
        imgs.append(img)
    if len(sys.argv) > 1:
        preview(imgs, sys.argv[1])
    print("ok: pieles de supervivientes")
