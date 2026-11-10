"""Armaduras en 3D (estilo Techguns): un modelo por pieza hecho de cubos que sobresalen del cuerpo + su textura.

python tools/armor3d_gen.py [carpeta_vista_previa]      (desde forge-1.20.1-47.4.10-mdk)

Escribe assets/tecnocinetico/armor_models/<set>.json (lo lee client/Armor3DModels) y
textures/models/armor/3d/<set>.png. Coordenadas de modelo de Minecraft (y hacia ABAJO, el frente es -z, la derecha
del personaje es -x), locales a cada hueso: cabeza (-4,-8,-4)+8, cuerpo (-4,0,-2)+(8,12,4), brazo derecho
(-3,-2,-2)+(4,12,4), pierna (-2,0,-2)+(4,12,4). Las piezas de los brazos y piernas se definen para el lado derecho
y se reflejan solas. Cada cara se pinta según su material (1 texel por unidad, como las armaduras vanilla).
Con una carpeta de vista previa dibuja cada set puesto en un maniquí (tools/render_zbuffer.py).
"""
import json
import math
import random
import sys
import zlib
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tecnocinetico"
TEX = 128
BONES = ("head", "body", "right_arm", "left_arm", "right_leg", "left_leg")
PIVOT = {"head": (0, 0, 0), "body": (0, 0, 0), "right_arm": (-5, 2, 0), "left_arm": (5, 2, 0), "right_leg": (-1.9, 12, 0), "left_leg": (1.9, 12, 0)}


# =================================================================== pintado
def clamp(v):
    return max(0, min(255, int(v)))


def shade(c, k):
    return tuple(clamp(x * k) for x in c[:3])


class Painter:
    """Pinta una cara (w x h texeles) de un material. `pal` = paleta del set."""

    def __init__(self, pal):
        self.pal = pal

    def __call__(self, mat, face, w, h, rnd):
        base = self.pal[mat]
        px = {}
        for y in range(h):
            for x in range(w):
                k = 1.0 + rnd.uniform(-0.02, 0.02)
                # bisel: luz arriba-izquierda, sombra abajo-derecha (solo en caras de 3 o más)
                if w >= 3 and h >= 3:
                    if y == 0 or x == 0:
                        k += 0.14
                    elif y == h - 1 or x == w - 1:
                        k -= 0.22
                c = shade(base, k)
                kind = MATKIND.get(mat, "plate")
                if kind == "fabric" and (x + y) % 3 == 0 and 0 < x < w - 1 and 0 < y < h - 1:
                    c = shade(base, 0.9)                                         # trama
                elif kind == "plate" and w >= 5 and h >= 5 and (x, y) in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):
                    c = shade(base, 1.35)                                        # remaches
                elif kind == "pouch" and h >= 3 and y == max(1, h // 3):
                    c = shade(base, 0.7)                                         # solapa
                elif kind == "strap" and w >= 3 and x in (0, w - 1):
                    c = shade(base, 1.25)                                        # costura
                elif kind == "visor":
                    t = y / max(1, h - 1)
                    c = shade(base, 1.25 - 0.5 * t)
                    if y == 0 and x < w - 1:
                        c = shade(self.pal.get("visor_hi", (180, 220, 230)), 1.0)
                elif kind == "glow":
                    d = max(abs(x - (w - 1) / 2) / max(1, w / 2), abs(y - (h - 1) / 2) / max(1, h / 2))
                    c = shade(base, 1.25 - 0.45 * d)
                elif kind == "rust" and rnd.random() < 0.18:
                    c = shade(self.pal.get("rust2", (90, 50, 30)), 1.0 + rnd.uniform(-0.1, 0.1))
                elif kind == "vent" and x % 2 == 1 and 0 < y < h - 1:
                    c = shade(base, 0.45)                                        # rejilla
                px[(x, y)] = c
        return px


MATKIND = {}


# =================================================================== cubos y empaquetado
class Cube:
    def __init__(self, bone, f, s, mat, inflate=0.0, faces=None, mirror=False):
        self.bone, self.f, self.s, self.mat, self.inflate, self.mirror = bone, list(f), [int(round(v)) for v in s], mat, inflate, mirror
        self.faces = faces or {}                                                 # front/back/top/bottom/right/left -> material
        self.uv = None

    def mat_of(self, face):
        return self.faces.get(face, self.mat)

    def mirrored(self):
        """Copia para el otro lado del cuerpo (x reflejada, misma UV en espejo)."""
        other = {"right_arm": "left_arm", "right_leg": "left_leg"}.get(self.bone, self.bone)
        c = Cube(other, [-(self.f[0] + self.s[0])] + self.f[1:], self.s, self.mat, self.inflate,
                 {{"right": "left", "left": "right"}.get(k, k): v for k, v in self.faces.items()}, True)
        c.twin = self
        return c


def pack(cubes):
    """Estantes en una textura de TEX x TEX (sin contar los reflejados, que comparten UV)."""
    x = y = shelf = 0
    for c in sorted([c for c in cubes if not c.mirror], key=lambda c: -(c.s[2] + c.s[1])):
        w, h, d = c.s
        bw, bh = 2 * (d + w), d + h
        if x + bw > TEX:
            x, y, shelf = 0, y + shelf, 0
        if y + bh > TEX - 4:
            raise ValueError("no cabe la textura de la armadura")
        c.uv = (x, y)
        x += bw
        shelf = max(shelf, bh)
    for c in cubes:
        if c.mirror:
            c.uv = c.twin.uv


def regions(c):
    u, v = c.uv
    w, h, d = c.s
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def paint(cubes, painter, img, setname):
    for i, c in enumerate(cubes):
        if c.mirror:
            continue
        for face, (x0, y0, w, h) in regions(c).items():
            if w <= 0 or h <= 0:
                continue
            rnd = random.Random(zlib.crc32(f"{setname}|{i}|{face}".encode()))
            for (x, y), col in painter(c.mat_of(face), face, w, h, rnd).items():
                img.putpixel((x0 + x, y0 + y), col + (255,))
    for x in range(TEX - 4, TEX):                                                # gris del maniquí (vista previa)
        for y in range(TEX - 4, TEX):
            img.putpixel((x, y), (150, 150, 150, 255))


# =================================================================== ayudas de diseño
class Set:
    def __init__(self, name, pal, kinds):
        self.name, self.pal = name, pal
        MATKIND.clear()
        MATKIND.update(kinds)
        self.slots = {"head": [], "chest": [], "legs": [], "feet": []}

    def add(self, slot, bone, f, s, mat, inflate=0.0, **faces):
        c = Cube(bone, f, s, mat, inflate, faces)
        self.slots[slot].append(c)
        if bone in ("right_arm", "right_leg"):
            self.slots[slot].append(c.mirrored())
        return c

    # piezas base que siguen el cuerpo
    def helmet_shell(self, mat, inflate=1.0, **faces):
        self.add("head", "head", (-4, -8, -4), (8, 8, 8), mat, inflate, **faces)

    def torso(self, mat, inflate=1.0, **faces):
        self.add("chest", "body", (-4, 0, -2), (8, 12, 4), mat, inflate, **faces)

    def sleeves(self, mat, inflate=1.0, length=12, **faces):
        self.add("chest", "right_arm", (-3, -2, -2), (4, length, 4), mat, inflate, **faces)

    def pants(self, mat, inflate=0.5, **faces):
        self.add("legs", "body", (-4, 7, -2), (8, 5, 4), mat, inflate, **faces)
        self.add("legs", "right_leg", (-2, 0, -2), (4, 9, 4), mat, inflate, **faces)

    def boots(self, mat, inflate=1.0, height=4, **faces):
        self.add("feet", "right_leg", (-2, 12 - height, -2), (4, height, 4), mat, inflate, **faces)


# =================================================================== diseños
def combat(mk):
    """Militar realista: Mk I ligera (chaleco portaplacas), Mk II pesada (hombreras, visor), Mk III casi exoesqueleto."""
    pal = {1: dict(cloth=(132, 122, 92), plate=(74, 84, 52), strap=(44, 46, 36), pouch=(156, 132, 92), metal=(70, 72, 78),
                   boot=(96, 74, 50), knee=(34, 34, 36), visor=(28, 40, 44), glow=(90, 220, 240), helmet=(82, 92, 58), sole=(30, 26, 22)),
           2: dict(cloth=(74, 82, 62), plate=(80, 84, 92), strap=(40, 42, 40), pouch=(82, 90, 66), metal=(60, 62, 68),
                   boot=(44, 40, 36), knee=(36, 38, 42), visor=(18, 28, 34), glow=(90, 220, 240), helmet=(76, 80, 88), sole=(24, 24, 24)),
           3: dict(cloth=(44, 46, 52), plate=(70, 74, 84), strap=(30, 32, 36), pouch=(56, 60, 68), metal=(120, 126, 136),
                   boot=(52, 56, 64), knee=(40, 42, 48), visor=(20, 60, 70), glow=(110, 235, 250), helmet=(66, 70, 80), sole=(22, 22, 24))}[mk]
    kinds = dict(cloth="fabric", plate="plate", strap="strap", pouch="pouch", metal="plate", boot="plate", knee="plate",
                 visor="visor", glow="glow", helmet="plate", sole="plate")
    s = Set(f"combat_mk{mk}", pal, kinds)
    # --- casco
    s.add("head", "head", (-4.5, -9.2, -4.5), (9, 4, 9), "helmet")                     # cúpula
    s.add("head", "head", (-4.6, -5.2, -4.0), (1, 3, 8), "helmet")                     # orejeras
    s.add("head", "head", (3.6, -5.2, -4.0), (1, 3, 8), "helmet")
    s.add("head", "head", (-4.0, -5.2, 3.6), (8, 3, 1), "helmet")                      # nuca
    if mk == 1:
        s.add("head", "head", (-4.6, -6.0, -4.8), (9, 1, 1), "strap")                  # cinta de las gafas
        s.add("head", "head", (-3.6, -6.4, -5.2), (3, 2, 1), "visor")                  # gafas
        s.add("head", "head", (0.6, -6.4, -5.2), (3, 2, 1), "visor")
    if mk >= 2:
        s.add("head", "head", (-4.0, -6.0, -5.3), (8, 3, 1), "visor")                  # visor abatible
        s.add("head", "head", (-5.4, -5.6, -1.5), (1, 3, 3), "metal")                  # cascos de radio
        s.add("head", "head", (4.4, -5.6, -1.5), (1, 3, 3), "metal")
        s.add("head", "head", (-0.5, -10.0, -1.5), (1, 1, 3), "metal")                 # raíl
    if mk == 3:
        s.add("head", "head", (-4.0, -3.0, -5.0), (8, 3, 1), "helmet")                 # mentonera cerrada
        s.add("head", "head", (-0.5, -6.0, -5.4), (1, 4, 1), "glow")                   # visor en T
        s.add("head", "head", (-3.5, -5.4, -5.45), (7, 1, 1), "glow")
    # --- pecho y brazos
    s.torso("cloth", 0.6)
    s.add("chest", "body", (-3.5, 1.0, -3.4), (7, 7, 1), "plate")                      # placa frontal
    s.add("chest", "body", (-3.5, 1.0, 2.4), (7, 8, 1), "plate")                       # placa trasera
    s.add("chest", "body", (-4.0, -0.6, -2.4), (2, 1, 5), "strap")                     # hombreras del chaleco
    s.add("chest", "body", (2.0, -0.6, -2.4), (2, 1, 5), "strap")
    s.add("chest", "body", (-4.7, 5.0, -2.5), (1, 4, 5), "strap")                      # faja lateral
    s.add("chest", "body", (3.7, 5.0, -2.5), (1, 4, 5), "strap")
    for x in (-3.5, -1.0, 1.5):                                                        # portacargadores
        s.add("chest", "body", (x, 6.0, -4.4), (2, 3, 1), "pouch")
    s.sleeves("cloth", 0.5)
    if mk == 1:
        s.add("chest", "right_arm", (-3.6, -2.4, -2.6), (4, 2, 5), "plate")            # protector de hombro
    if mk >= 2:
        s.add("chest", "right_arm", (-4.2, -3.0, -3.0), (5, 3, 6), "plate")            # hombrera
        s.add("chest", "right_arm", (-4.0, -0.2, -2.8), (4, 2, 6), "plate")            # segunda lámina
        s.add("chest", "right_arm", (-3.4, 5.0, -2.6), (4, 4, 5), "metal")             # antebrazo
        s.add("chest", "body", (-1.5, 1.6, -3.9), (3, 2, 1), "glow" if mk == 3 else "metal")   # parche / indicador
    if mk == 3:
        s.add("chest", "body", (-3.0, 1.0, 3.2), (6, 9, 3), "metal")                   # mochila de energía
        s.add("chest", "body", (-2.0, 2.0, 6.0), (4, 6, 1), "glow")                    # célula
        s.add("chest", "body", (-3.5, 0.0, 3.0), (1, 4, 3), "metal")                   # conductos
        s.add("chest", "body", (2.5, 0.0, 3.0), (1, 4, 3), "metal")
        s.add("chest", "right_arm", (-4.4, -4.0, -3.2), (5, 2, 6), "plate")            # tercera lámina
        s.add("chest", "right_arm", (-3.6, 8.0, -2.6), (4, 3, 5), "knee")              # guantelete
        s.add("chest", "right_arm", (-3.5, 2.0, -2.7), (1, 1, 1), "glow")
    # --- pantalones
    s.pants("cloth", 0.5)
    s.add("legs", "body", (-4.0, 10.0, -2.6), (8, 2, 5), "strap")                      # cinturón
    s.add("legs", "right_leg", (-2.0, 4.0, -3.0), (4, 3, 1), "knee")                   # rodillera
    if mk == 1:
        s.add("legs", "right_leg", (-3.0, 1.0, -1.5), (1, 3, 3), "pouch")              # bolsillo del muslo
    if mk >= 2:
        s.add("legs", "body", (-1.5, 11.0, -3.0), (3, 3, 1), "plate")                  # protección inguinal
        s.add("legs", "right_leg", (-2.6, 0.0, -2.8), (4, 4, 1), "plate")              # placa del muslo
        s.add("legs", "right_leg", (-3.0, 0.5, -2.0), (1, 4, 4), "plate")
    if mk == 3:
        s.add("legs", "right_leg", (-2.5, 4.5, -3.3), (1, 1, 1), "glow")               # articulación
        s.add("legs", "right_leg", (-3.2, 4.0, -1.0), (1, 3, 2), "metal")              # pistón de rodilla
    # --- botas
    s.boots("boot", 0.6, 4)
    s.add("feet", "right_leg", (-2.4, 11.0, -3.2), (5, 1, 6), "sole")                  # suela
    s.add("feet", "right_leg", (-2.0, 9.0, -3.0), (4, 2, 1), "boot")                   # puntera
    if mk >= 2:
        s.add("feet", "right_leg", (-2.0, 5.0, -2.9), (4, 4, 1), "plate")              # espinillera
    if mk == 3:
        s.add("feet", "right_leg", (-3.0, 7.0, -1.0), (1, 3, 2), "metal")              # pistón del tobillo
    return s


def scrap():
    pal = dict(rust=(132, 78, 44), rust2=(92, 52, 30), cloth=(112, 98, 72), rubber=(32, 32, 34), rope=(150, 122, 72),
               metal=(116, 118, 122), visor=(40, 70, 40), visor_hi=(150, 200, 150))
    s = Set("scrap", pal, dict(rust="rust", cloth="fabric", rubber="plate", rope="strap", metal="plate", visor="visor"))
    s.add("head", "head", (-4.6, -9.0, -4.6), (9, 4, 9), "metal")                      # olla
    s.add("head", "head", (-5.5, -6.0, -1.0), (1, 2, 2), "rust")                       # asas de la olla
    s.add("head", "head", (4.5, -6.0, -1.0), (1, 2, 2), "rust")
    s.add("head", "head", (-4.4, -6.2, -4.8), (9, 1, 1), "rubber")                     # goma de las gafas de soldar
    s.add("head", "head", (-3.5, -6.6, -5.4), (3, 2, 1), "visor")
    s.add("head", "head", (0.5, -6.6, -5.4), (3, 2, 1), "visor")
    s.add("head", "head", (-4.2, -3.0, -4.7), (8, 3, 1), "cloth")                      # pañuelo
    s.torso("cloth", 0.6)
    s.add("chest", "body", (-3.8, 0.5, -3.3), (5, 6, 1), "rust")                       # chapas desiguales
    s.add("chest", "body", (0.6, 2.5, -3.2), (3, 5, 1), "metal")
    s.add("chest", "body", (-3.6, 7.0, -3.2), (7, 3, 1), "rubber")                     # trozo de neumático
    s.add("chest", "body", (-4.6, -0.5, -2.6), (9, 1, 1), "rope")                      # cuerdas
    s.add("chest", "body", (-3.5, 1.0, 2.4), (7, 8, 1), "rust")
    s.sleeves("cloth", 0.5, 10)
    s.add("chest", "right_arm", (-3.8, -2.6, -2.6), (4, 3, 5), "rubber")               # neumático de hombro
    s.add("chest", "right_arm", (-3.5, 5.0, -2.6), (4, 1, 5), "rope")
    s.pants("cloth", 0.5)
    s.add("legs", "right_leg", (-2.2, 3.5, -3.0), (4, 4, 1), "rust")                   # rodillera de chapa
    s.add("legs", "body", (-4.0, 10.0, -2.6), (8, 1, 5), "rope")
    s.boots("rubber", 0.6, 4)
    s.add("feet", "right_leg", (-2.4, 11.0, -3.0), (5, 1, 5), "rubber")
    s.add("feet", "right_leg", (-2.0, 9.0, -2.8), (4, 1, 1), "rope")
    return s


def inercita():
    pal = dict(plate=(92, 104, 132), dark=(52, 60, 78), cloth=(60, 64, 76), glow=(90, 220, 240), visor=(16, 20, 28))
    s = Set("inercita", pal, dict(plate="plate", dark="plate", cloth="fabric", glow="glow", visor="visor"))
    s.helmet_shell("plate", 1.0)
    s.add("head", "head", (-3.5, -6.0, -5.4), (7, 1, 1), "visor")                      # rendija
    s.add("head", "head", (-0.5, -10.2, -4.0), (1, 2, 8), "dark")                      # cresta
    s.add("head", "head", (-4.0, -3.0, -5.3), (8, 3, 1), "dark")                       # mentonera
    s.torso("plate", 1.0)
    s.add("chest", "body", (-4.0, 0.5, -4.0), (8, 6, 1), "plate")                      # coraza en capas
    s.add("chest", "body", (-3.5, 6.5, -3.8), (7, 4, 1), "dark")
    s.add("chest", "body", (-1.0, 2.0, -4.6), (2, 2, 1), "glow")
    s.sleeves("cloth", 0.6)
    s.add("chest", "right_arm", (-4.5, -3.2, -3.2), (6, 4, 6), "plate")                # hombrera maciza
    s.add("chest", "right_arm", (-3.6, 4.0, -2.8), (4, 6, 6), "dark")                  # brazal
    s.pants("cloth", 0.5)
    s.add("legs", "right_leg", (-2.6, 0.0, -2.8), (5, 6, 1), "plate")
    s.add("legs", "right_leg", (-2.4, 6.0, -2.9), (4, 3, 1), "dark")
    s.add("legs", "body", (-4.0, 9.5, -2.7), (8, 3, 5), "dark")
    s.boots("plate", 1.0, 5)
    s.add("feet", "right_leg", (-2.5, 11.0, -3.4), (5, 1, 6), "dark")
    return s


def tactical():
    pal = dict(cloth=(40, 42, 46), plate=(52, 54, 58), strap=(28, 28, 30), pouch=(48, 50, 54), metal=(70, 72, 78),
               visor=(20, 40, 30), visor_hi=(140, 220, 160), lens=(40, 120, 60), knee=(24, 24, 26))
    s = Set("tactical", pal, dict(cloth="fabric", plate="plate", strap="strap", pouch="pouch", metal="plate", visor="visor", lens="glow", knee="plate"))
    s.add("head", "head", (-4.4, -9.0, -4.4), (9, 3, 9), "plate")                      # casco alto corte
    s.add("head", "head", (-1.5, -8.6, -5.4), (3, 2, 1), "metal")                      # soporte de visión nocturna
    s.add("head", "head", (-2.5, -7.2, -6.4), (2, 2, 1), "lens")
    s.add("head", "head", (0.5, -7.2, -6.4), (2, 2, 1), "lens")
    s.add("head", "head", (-5.2, -6.0, -1.5), (1, 3, 3), "strap")                      # auriculares
    s.add("head", "head", (4.2, -6.0, -1.5), (1, 3, 3), "strap")
    s.torso("cloth", 0.5)
    s.add("chest", "body", (-3.8, 1.0, -3.0), (8, 9, 1), "plate")                      # chaleco
    for x in (-3.5, -1.5, 0.5, 2.5):
        s.add("chest", "body", (x, 6.0, -4.0), (1, 3, 1), "pouch")                     # cargadores finos
    s.add("chest", "body", (-3.0, 2.0, -3.8), (3, 2, 1), "pouch")                      # radio
    s.add("chest", "body", (-3.5, 1.0, 2.2), (7, 7, 1), "plate")
    s.sleeves("cloth", 0.4)
    s.add("chest", "right_arm", (-3.5, 8.0, -2.5), (4, 2, 5), "knee")                  # guantes
    s.pants("cloth", 0.4)
    s.add("legs", "right_leg", (-2.0, 4.0, -2.9), (4, 3, 1), "knee")
    s.add("legs", "right_leg", (-3.0, 1.0, -1.0), (1, 4, 3), "pouch")                  # funda de la pierna
    s.boots("strap", 0.5, 4)
    s.add("feet", "right_leg", (-2.3, 11.0, -3.0), (5, 1, 5), "knee")
    return s


def exo():
    pal = dict(frame=(60, 62, 70), strut=(150, 154, 162), joint=(194, 116, 64), glow=(90, 220, 240), cloth=(46, 48, 54), visor=(20, 50, 60))
    s = Set("exo", pal, dict(frame="plate", strut="plate", joint="plate", glow="glow", cloth="fabric", visor="visor"))
    s.add("head", "head", (-4.5, -8.6, -4.5), (9, 2, 9), "frame")                      # aro superior
    s.add("head", "head", (-4.0, -6.0, -5.0), (8, 2, 1), "visor")
    s.add("head", "head", (-5.0, -7.0, -0.5), (1, 5, 1), "strut")                      # montantes
    s.add("head", "head", (4.0, -7.0, -0.5), (1, 5, 1), "strut")
    s.torso("cloth", 0.4)
    s.add("chest", "body", (-0.5, -1.0, 2.6), (1, 12, 2), "strut")                     # columna
    for y in (1.0, 4.0, 7.0):
        s.add("chest", "body", (-3.5, y, 2.6), (7, 1, 1), "frame")                     # costillas
    s.add("chest", "body", (-1.5, 4.0, 3.6), (3, 3, 1), "joint")
    s.add("chest", "body", (-1.0, 4.5, 4.4), (2, 2, 1), "glow")
    s.add("chest", "body", (-3.5, 0.5, -3.0), (7, 2, 1), "frame")
    s.sleeves("cloth", 0.3)
    s.add("chest", "right_arm", (-4.0, -2.5, -0.5), (1, 11, 1), "strut")               # barra exterior
    s.add("chest", "right_arm", (-4.4, 3.0, -1.5), (2, 2, 3), "joint")                 # servo del codo
    s.add("chest", "right_arm", (-4.5, 3.5, -0.5), (1, 1, 1), "glow")
    s.add("chest", "right_arm", (-3.5, -2.8, -2.5), (4, 2, 5), "frame")
    s.pants("cloth", 0.3)
    s.add("legs", "right_leg", (-3.0, 0.0, -0.5), (1, 12, 1), "strut")
    s.add("legs", "right_leg", (-3.4, 4.0, -1.5), (2, 3, 3), "joint")                  # servo de la rodilla
    s.add("legs", "right_leg", (-2.0, 4.0, -2.8), (4, 1, 1), "frame")
    s.add("legs", "body", (-4.0, 10.0, -2.5), (8, 2, 5), "frame")
    s.boots("frame", 0.6, 3)
    s.add("feet", "right_leg", (-2.4, 11.0, -3.4), (5, 1, 6), "frame")
    s.add("feet", "right_leg", (-3.0, 8.0, -0.5), (1, 4, 1), "strut")
    return s


def core():
    pal = dict(white=(224, 228, 232), grey=(150, 156, 166), dark=(60, 64, 72), glow=(110, 235, 250), visor=(30, 90, 110), visor_hi=(190, 250, 255))
    s = Set("core", pal, dict(white="plate", grey="plate", dark="plate", glow="glow", visor="visor"))
    s.helmet_shell("white", 1.0)
    s.add("head", "head", (-4.0, -6.0, -5.3), (8, 2, 1), "visor")
    s.add("head", "head", (-4.6, -9.6, -0.5), (9, 1, 1), "glow")                       # halo
    s.add("head", "head", (-5.3, -6.0, -2.0), (1, 2, 4), "grey")
    s.add("head", "head", (4.3, -6.0, -2.0), (1, 2, 4), "grey")
    s.torso("white", 1.0)
    s.add("chest", "body", (-2.0, 2.0, -3.6), (4, 4, 1), "dark")                       # núcleo
    s.add("chest", "body", (-1.0, 3.0, -4.2), (2, 2, 1), "glow")
    s.add("chest", "body", (-4.0, 7.0, -3.2), (8, 1, 1), "grey")
    s.add("chest", "body", (-2.5, 1.0, 3.2), (5, 7, 2), "grey")                        # generador del escudo
    s.add("chest", "body", (-1.5, 2.0, 5.2), (3, 5, 1), "glow")
    s.sleeves("white", 0.8)
    s.add("chest", "right_arm", (-4.2, -3.0, -2.8), (5, 3, 6), "white")
    s.add("chest", "right_arm", (-4.3, -1.0, -0.5), (1, 6, 1), "glow")                 # línea de energía
    s.pants("grey", 0.5)
    s.add("legs", "right_leg", (-2.4, 0.0, -2.8), (4, 6, 1), "white")
    s.add("legs", "right_leg", (-2.6, 2.0, -0.5), (1, 6, 1), "glow")
    s.boots("white", 1.0, 4)
    s.add("feet", "right_leg", (-2.4, 11.0, -3.2), (5, 1, 6), "dark")
    return s


def piroxite():
    pal = dict(char=(46, 34, 30), plate=(150, 58, 28), glow=(255, 150, 50), fin=(80, 44, 30), visor=(60, 20, 10), visor_hi=(255, 180, 90))
    s = Set("piroxite", pal, dict(char="plate", plate="plate", glow="glow", fin="vent", visor="visor"))
    s.helmet_shell("char", 1.0)
    s.add("head", "head", (-3.5, -6.0, -5.3), (7, 2, 1), "visor")
    for z in (-3.0, 0.0, 2.0):                                                         # aletas de calor
        s.add("head", "head", (-0.5, -11.0, z), (1, 3, 2), "fin")
    s.add("head", "head", (-4.4, -3.4, -4.6), (9, 1, 1), "glow")
    s.torso("char", 1.0)
    s.add("chest", "body", (-3.5, 1.0, -3.5), (7, 6, 1), "plate")
    s.add("chest", "body", (-0.5, 1.5, -4.0), (1, 5, 1), "glow")                       # grieta incandescente
    for x in (-3.5, 2.5):                                                              # disipadores de la espalda
        s.add("chest", "body", (x, 1.0, 2.6), (1, 8, 3), "fin")
    s.sleeves("char", 0.8)
    s.add("chest", "right_arm", (-4.4, -3.0, -3.0), (5, 3, 6), "plate")
    s.add("chest", "right_arm", (-4.6, -4.4, -1.0), (1, 2, 2), "fin")
    s.pants("char", 0.5)
    s.add("legs", "right_leg", (-2.4, 1.0, -2.8), (4, 5, 1), "plate")
    s.add("legs", "right_leg", (-0.5, 2.0, -3.3), (1, 3, 1), "glow")
    s.boots("char", 1.0, 5)
    s.add("feet", "right_leg", (-2.4, 11.0, -3.3), (5, 1, 6), "plate")
    return s


def nullite():
    pal = dict(dark=(36, 28, 50), plate=(108, 82, 162), glow=(210, 160, 255), visor=(40, 20, 70), visor_hi=(220, 180, 255))
    s = Set("nullite", pal, dict(dark="plate", plate="plate", glow="glow", visor="visor"))
    s.helmet_shell("dark", 1.0)
    s.add("head", "head", (-4.0, -6.2, -5.3), (8, 2, 1), "visor")
    s.add("head", "head", (-4.8, -10.0, -3.0), (1, 4, 2), "plate")                     # cuernos afilados
    s.add("head", "head", (3.8, -10.0, -3.0), (1, 4, 2), "plate")
    s.add("head", "head", (-0.5, -9.6, -4.6), (1, 1, 1), "glow")
    s.torso("dark", 1.0)
    s.add("chest", "body", (-3.0, 1.0, -3.5), (2, 8, 1), "plate")                      # nervios
    s.add("chest", "body", (1.0, 1.0, -3.5), (2, 8, 1), "plate")
    s.add("chest", "body", (-1.0, 3.0, -3.6), (2, 3, 1), "glow")
    s.add("chest", "body", (-3.0, 0.0, 2.6), (6, 2, 2), "plate")
    s.sleeves("dark", 0.8)
    s.add("chest", "right_arm", (-4.6, -3.6, -2.6), (4, 3, 5), "plate")                # hombrera en punta
    s.add("chest", "right_arm", (-5.0, -5.0, -0.5), (1, 2, 1), "plate")
    s.add("chest", "right_arm", (-3.6, 6.0, -2.7), (4, 1, 5), "glow")
    s.pants("dark", 0.5)
    s.add("legs", "right_leg", (-2.4, 0.5, -2.8), (2, 6, 1), "plate")
    s.boots("dark", 1.0, 4)
    s.add("feet", "right_leg", (-2.0, 11.0, -3.6), (4, 1, 6), "plate")
    return s


def hazmat():
    pal = dict(suit=(226, 190, 40), fold=(196, 162, 30), rubber=(34, 34, 38), visor=(30, 60, 70), visor_hi=(190, 230, 240),
               stripe=(190, 196, 200), metal=(80, 84, 90))
    s = Set("hazmat", pal, dict(suit="fabric", fold="fabric", rubber="plate", visor="visor", stripe="plate", metal="plate"))
    s.add("head", "head", (-4.8, -9.2, -4.8), (10, 9, 10), "suit", 0.0)                # capucha abultada
    s.add("head", "head", (-3.5, -7.0, -5.4), (7, 4, 1), "visor")                      # visor grande
    s.add("head", "head", (-1.5, -3.0, -6.4), (3, 3, 2), "metal")                      # filtro
    s.add("head", "head", (-1.0, -2.5, -7.0), (2, 2, 1), "rubber")
    s.torso("suit", 1.3)
    s.add("chest", "body", (-4.0, 6.0, -3.5), (8, 1, 1), "stripe")                     # banda reflectante
    s.add("chest", "body", (-3.0, 1.0, 3.3), (6, 7, 2), "metal")                       # botella de aire
    s.add("chest", "body", (2.0, 2.0, -3.4), (1, 2, 1), "rubber")                      # dosímetro
    s.sleeves("suit", 1.1)
    s.add("chest", "right_arm", (-3.6, 4.0, -2.8), (4, 1, 5), "stripe")
    s.add("chest", "right_arm", (-3.6, 7.0, -2.6), (4, 3, 5), "rubber")                # guantes
    s.pants("suit", 0.9)
    s.add("legs", "right_leg", (-2.6, 4.0, -2.9), (5, 1, 5), "stripe")
    s.boots("rubber", 1.1, 4)
    s.add("feet", "right_leg", (-2.6, 11.0, -3.4), (5, 1, 6), "rubber")
    return s


SETS = [lambda: combat(1), lambda: combat(2), lambda: combat(3), scrap, inercita, tactical, exo, core, piroxite, nullite, hazmat]


# =================================================================== salida y vista previa
def write(s):
    cubes = [c for slot in s.slots.values() for c in slot]
    pack(cubes)
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    paint(cubes, Painter(s.pal), img, s.name)
    (ROOT / "textures/models/armor/3d").mkdir(parents=True, exist_ok=True)
    img.save(ROOT / f"textures/models/armor/3d/{s.name}.png")
    data = {"texture": [TEX, TEX], "slots": {slot: [{"bone": c.bone, "uv": list(c.uv), "from": [round(v, 3) for v in c.f], "size": c.s,
                                                     "inflate": c.inflate, "mirror": c.mirror} for c in cubes_]
                                            for slot, cubes_ in s.slots.items()}}
    (ROOT / "armor_models").mkdir(parents=True, exist_ok=True)
    (ROOT / f"armor_models/{s.name}.json").write_text(json.dumps(data, indent=1) + "\n", encoding="utf-8")
    return cubes


def preview_model(s, cubes):
    """Modelo JSON (y hacia arriba) del set puesto en un maniquí gris, para render_zbuffer.py."""
    els = []
    k = 16.0 / TEX

    def element(bone, f, size, inflate, uvreg, mirror):
        px, py, pz = PIVOT[bone]
        x0, y0, z0 = f[0] - inflate + px, f[1] - inflate + py, f[2] - inflate + pz
        x1, y1, z1 = f[0] + size[0] + inflate + px, f[1] + size[1] + inflate + py, f[2] + size[2] + inflate + pz
        fr, to = [x0, -y1, z0], [x1, -y0, z1]
        faces = {}
        names = {"front": "north", "back": "south", "top": "up", "bottom": "down", "right": "west", "left": "east"}
        for face, reg in uvreg.items():
            u0, v0, w, h = reg
            uv = [u0 * k, v0 * k, (u0 + w) * k, (v0 + h) * k]
            if mirror:
                uv = [uv[2], uv[1], uv[0], uv[3]]
            faces[names[face]] = {"uv": uv, "texture": "#0"}
        els.append({"from": fr, "to": to, "faces": faces})

    grey = (TEX - 4, TEX - 4, 4, 4)
    body = [("head", (-4, -8, -4), (8, 8, 8)), ("body", (-4, 0, -2), (8, 12, 4)), ("right_arm", (-3, -2, -2), (4, 12, 4)),
            ("left_arm", (-1, -2, -2), (4, 12, 4)), ("right_leg", (-2, 0, -2), (4, 12, 4)), ("left_leg", (-2, 0, -2), (4, 12, 4))]
    for bone, f, size in body:
        element(bone, f, size, 0, {n: grey for n in ("front", "back", "top", "bottom", "right", "left")}, False)
    for c in cubes:
        element(c.bone, c.f, c.s, c.inflate, regions(c), c.mirror)
    return {"textures": {"0": f"tecnocinetico:models/armor/3d/{s.name}"}, "elements": els}


def main():
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else None
    names = []
    for make in SETS:
        s = make()
        cubes = write(s)
        names.append(s.name)
        if out:
            (out / f"{s.name}_preview.json").write_text(json.dumps(preview_model(s, cubes)), encoding="utf-8")
    print("ok: armaduras 3D:", ", ".join(names))


if __name__ == "__main__":
    main()
