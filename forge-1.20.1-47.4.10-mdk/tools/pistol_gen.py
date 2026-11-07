"""Pistola Cinética realista: modelo 3D por piezas, textura con sombreado, icono y fotogramas de recarga.

python pistol_gen.py <assets/tecnocinetico> [carpeta_preview]
Genera:
  textures/item/kinetic_pistol.png, kinetic_pistol_icon.png
  models/item/kinetic_pistol.json (+ overrides de recarga)
  models/item/kinetic_pistol_3d.json, kinetic_pistol_reload_{1..4}(_3d).json
"""
import copy
import json
import math
import random
import sys
from pathlib import Path
from PIL import Image

ASSETS = Path(sys.argv[1])
PREVIEW = Path(sys.argv[2]) if len(sys.argv) > 2 else None
PPU = 4  # 4 píxeles por unidad: detalle fino, como los mods de armas realistas
random.seed(2024)

# ---------------------------------------------------------------- paleta realista
STEEL_BLK = (40, 42, 47)       # acero pavonado de la corredera
STEEL_HI = (78, 82, 90)
STEEL_LO = (24, 25, 29)
POLY = (33, 34, 37)            # polímero del armazón
POLY_HI = (52, 54, 58)
POLY_LO = (20, 21, 23)
BARREL = (108, 112, 119)       # acero del cañón (sin pavonar)
BARREL_LO = (70, 73, 79)
MAG = (58, 60, 65)
COPPER = (150, 92, 56)
CYAN = (64, 206, 228)
CYAN_HI = (176, 246, 255)
BLACK = (10, 10, 12)
OUTLINE = (14, 15, 18)


def clamp(v):
    return max(0, min(255, int(round(v))))


def col(c, k=1.0, a=255):
    return (clamp(c[0] * k), clamp(c[1] * k), clamp(c[2] * k), a)


def mix(c1, c2, t):
    return tuple(c1[i] + (c2[i] - c1[i]) * t for i in range(3))


# ---------------------------------------------------------------- utilidades de cara
class Face:
    """Información de una cara: tamaño en píxeles y cómo se mapean u/v a ejes del modelo."""

    def __init__(self, name, w, h):
        self.name, self.w, self.h = name, w, h

    def uf(self, x):
        return (x + 0.5) / self.w

    def vf(self, y):
        return (y + 0.5) / self.h

    def zf(self, x, y):
        """Fracción a lo largo de z (0 = delante/boca del cañón, 1 = detrás)."""
        if self.name == "east":
            return 1 - self.uf(x)
        if self.name == "west":
            return self.uf(x)
        if self.name == "up":
            return self.vf(y)
        if self.name == "down":
            return 1 - self.vf(y)
        return 0.5

    @property
    def side(self):
        return self.name in ("east", "west")


def shaded(base, hi, lo, f, x, y, noise=0.035, scratches=0.0):
    """Sombreado realista: luz desde arriba, bisel en los bordes, ruido fino y arañazos."""
    c = base
    if f.side or f.name in ("north", "south"):
        c = mix(base, hi, max(0.0, 0.35 - f.vf(y)) * 0.9)          # degradado: arriba más claro
        c = mix(c, lo, max(0.0, f.vf(y) - 0.7) * 0.8)              # abajo más oscuro
    elif f.name == "up":
        c = mix(base, hi, 0.25)
    else:
        c = mix(base, lo, 0.4)
    if f.w > 2 and f.h > 2:
        if y == 0:
            c = mix(c, hi, 0.6)                                       # arista superior iluminada
        elif y == f.h - 1:
            c = mix(c, lo, 0.6)
        if x == 0 or x == f.w - 1:
            c = mix(c, lo, 0.25)
    k = 1 + random.uniform(-noise, noise)
    if scratches and random.random() < scratches:
        return col(mix(c, hi, 0.8))
    return col(c, k)


# ---------------------------------------------------------------- materiales
def m_slide(f, x, y):
    z, v = f.zf(x, y), f.vf(y)
    if f.side:
        # estrías traseras de agarre
        if z > 0.8 and 0.12 < v < 0.88:
            return col(STEEL_LO) if x % 2 == 0 else shaded(STEEL_BLK, STEEL_HI, STEEL_LO, f, x, y)
        # ventanas delanteras: se ven las bobinas de cinetrita/cobre del cañón
        if 0.07 < z < 0.4 and 0.32 < v < 0.72:
            if z < 0.1 or z > 0.37 or v < 0.38 or v > 0.66:
                return col(BLACK)
            phase = int(z * f.w) % 3
            return col(CYAN, 1.0 if phase == 0 else 0.0) if phase == 0 else col(COPPER, 0.9 if phase == 1 else 0.6)
        # ventana de expulsión (solo lado derecho)
        if f.name == "east" and 0.5 < z < 0.72 and v < 0.62:
            if v < 0.1:
                return col(STEEL_LO)
            return shaded(BARREL, (150, 154, 160), BARREL_LO, f, x, y, 0.02)
        # glifo rúnico grabado (lado izquierdo), como los de la Aleación Antigua
        # (rombo fino con trazo central, 1 px de grosor)
        if f.name == "west" and 0.53 < z < 0.67 and 0.22 < v < 0.78:
            # en la cara oeste la u avanza con z, así que x local = x - inicio de la zona
            gx0, gw = int(0.53 * f.w) + 1, int(0.14 * f.w) - 1
            gy0, gh = int(0.22 * f.h) + 1, int(0.56 * f.h) - 1
            lx, ly = x - gx0, y - gy0
            cx, cy = gw // 2, gh // 2
            if 0 <= lx < gw and 0 <= ly < gh:
                diamond = abs(abs(lx - cx) + abs(ly - cy) * cx / max(cy, 1) - cx) < 0.6
                if diamond or (lx == cx and abs(ly - cy) <= cy - 1):
                    return col(CYAN, 0.65)
        # linea de mecanizado
        if abs(v - 0.84) < 0.5 / f.h:
            return col(STEEL_HI, 0.9)
        return shaded(STEEL_BLK, STEEL_HI, STEEL_LO, f, x, y, scratches=0.012)
    if f.name == "up":
        cx = (f.w - 1) / 2
        if z > 0.82:
            return col(STEEL_LO) if y % 2 == 0 else shaded(STEEL_BLK, STEEL_HI, STEEL_LO, f, x, y)
        if abs(x - cx) < 0.6:
            if 0.66 < z < 0.74:
                return col(CYAN_HI)                                  # indicador de carga
            return col(STEEL_LO)                                     # ranura de puntería
        return shaded(STEEL_BLK, STEEL_HI, STEEL_LO, f, x, y, scratches=0.012)
    return shaded(STEEL_BLK, STEEL_HI, STEEL_LO, f, x, y)


def m_frame(f, x, y):
    z, v = f.zf(x, y), f.vf(y)
    if f.side and z < 0.35 and v > 0.45 and x % 4 == 0:
        return col(POLY_LO)                                          # raíl de accesorios
    return shaded(POLY, POLY_HI, POLY_LO, f, x, y, noise=0.05)


def m_grip(f, x, y):
    if f.name == "north" and y % 7 == 3:
        return col(POLY_LO)                                          # acanaladuras para los dedos
    if f.w > 3 and f.h > 3 and (x == 0 or x == f.w - 1):
        return col(POLY_HI, 0.85)
    # punteado antideslizante: grano fino y de poco contraste, como el polímero real
    c = mix(POLY, POLY_HI, 0.15) if (x + y * 2) % 3 == 0 else POLY
    return col(c, 1 + random.uniform(-0.08, 0.08))


def m_barrel(f, x, y):
    if f.name == "north":
        cx, cy = (f.w - 1) / 2, (f.h - 1) / 2
        d = math.hypot(x - cx, y - cy)
        if d < f.w * 0.28:
            return col(BLACK)                                        # ánima
        if d < f.w * 0.38:
            return col(CYAN, 0.55)                                   # brillo residual de la bobina
    return shaded(BARREL, (150, 154, 160), BARREL_LO, f, x, y, 0.03)


def m_mag(f, x, y):
    if f.side and x % 5 == 2 and 0.15 < f.vf(y) < 0.85 and y % 4 == 1:
        return col(BLACK)                                            # agujeros testigo
    return shaded(MAG, (96, 99, 106), (36, 37, 41), f, x, y)


def m_poly(f, x, y):
    return shaded(POLY, POLY_HI, POLY_LO, f, x, y, noise=0.05)


def m_steel(f, x, y):
    return shaded(STEEL_BLK, STEEL_HI, STEEL_LO, f, x, y)


def m_sight(f, x, y):
    if f.name == "south":
        cx, cy = (f.w - 1) / 2, (f.h - 1) / 2
        if abs(x - cx) < 0.6 and abs(y - cy) < 0.6:
            return col(CYAN_HI)                                      # punto luminoso (tritio cinético)
    return col(BLACK, 1.4)


def m_cell(f, x, y):
    """Célula de cinetrita en la tapa del armazón: ventana con brillo."""
    if f.w > 2 and f.h > 1 and (x == 0 or x == f.w - 1):
        return col(POLY_LO)
    t = abs((x + 0.5) / f.w - 0.5) * 2
    return col(mix(CYAN_HI, CYAN, t))


INERCITA = (64, 72, 90)
INERCITA_HI = (104, 114, 136)
INERCITA_LO = (38, 43, 56)
DEEPSLATE = (52, 54, 62)


def window(f, x, y, u0, u1, v0, v1):
    """¿El píxel está dentro de una ventana (en fracciones u/v de la cara)? Devuelve 0 fuera, 1 borde, 2 dentro."""
    u, v = f.uf(x), f.vf(y)
    if not (u0 <= u <= u1 and v0 <= v <= v1):
        return 0
    edge_u = min(u - u0, u1 - u) * f.w
    edge_v = min(v - v0, v1 - v) * f.h
    return 1 if min(edge_u, edge_v) < 1.0 else 2


def glow(f, x, y):
    """Brillo cian con centro más claro."""
    t = min(1.0, abs(f.vf(y) - 0.5) * 2.2)
    return col(mix(CYAN_HI, CYAN, t))


def m_comp(f, x, y):
    """Compensador / Transductor Piezo-Cúprico: ranuras de escape que brillan en cian."""
    if f.name == "north":
        cx, cy = (f.w - 1) / 2, (f.h - 1) / 2
        d = math.hypot(x - cx, y - cy)
        if d < f.w * 0.2:
            return col(BLACK)
        if d < f.w * 0.3:
            return col(CYAN)
        return shaded(STEEL_BLK, STEEL_HI, STEEL_LO, f, x, y)
    if f.side or f.name == "up":
        z = f.zf(x, y)
        across = f.vf(y) if f.side else f.uf(x)
        for z0 in (0.12, 0.42, 0.72):
            if z0 <= z <= z0 + 0.18 and 0.28 <= across <= 0.72:
                edge = min(z - z0, z0 + 0.18 - z) < 0.05
                return col(STEEL_LO) if edge else glow(f, x, y)
    return shaded(STEEL_BLK, STEEL_HI, STEEL_LO, f, x, y, scratches=0.01)


def m_coupler(f, x, y):
    """Acoplador Cinético: carcasa de cobre con tapas de inercita y ventana de energía."""
    if f.name in ("up", "down"):
        return shaded(INERCITA, INERCITA_HI, INERCITA_LO, f, x, y)
    w = window(f, x, y, 0.22, 0.78, 0.2, 0.8)
    if w == 1:
        return col(BLACK, 1.5)
    if w == 2:
        return glow(f, x, y)
    if f.w > 4 and f.h > 3 and x in (1, f.w - 2) and y in (1, f.h - 2):
        return col(INERCITA_HI)                                      # remaches
    return shaded(COPPER, (196, 128, 84), (96, 56, 32), f, x, y)


def m_substrate(f, x, y):
    """Sustrato Lógico de Pizarra: losetas de pizarra con pistas de circuito cian."""
    if not f.side:
        return shaded(DEEPSLATE, (80, 83, 94), (30, 31, 37), f, x, y)
    if x == 0 or y == 0 or x == f.w - 1 or y == f.h - 1:
        return col(POLY_LO)                                          # marco de la cacha
    traces, nodes = circuit(f.w, f.h)
    if (x, y) in nodes:
        return col(CYAN_HI)                                          # nodos / soldaduras
    if (x, y) in traces:
        return col(CYAN, 0.6)                                        # pistas finas y tenues
    rnd = random.Random(x * 31 + y * 17 + f.w)
    return col(DEEPSLATE, 1 + rnd.uniform(-0.07, 0.07))


_CIRCUITS = {}


def circuit(w, h):
    """Pistas de circuito: caminos de 1 px que avanzan y giran 90°, con nodos en los extremos."""
    if (w, h) in _CIRCUITS:
        return _CIRCUITS[(w, h)]
    rnd = random.Random(w * 1000 + h)
    traces, nodes = set(), set()
    for _ in range(3):
        x, y = rnd.randrange(2, w - 2), rnd.randrange(2, h - 2)
        nodes.add((x, y))
        dx, dy = (0, 1) if rnd.random() < 0.6 else (1, 0)
        for _ in range(rnd.randint(2, 4)):
            for _ in range(rnd.randint(2, 5)):
                nx, ny = x + dx, y + dy
                if not (2 <= nx < w - 2 and 2 <= ny < h - 2):
                    break
                x, y = nx, ny
                traces.add((x, y))
            dx, dy = (dy, dx) if rnd.random() < 0.5 else (-dy, -dx)
        nodes.add((x, y))
    _CIRCUITS[(w, h)] = (traces, nodes)
    return traces, nodes


def m_copper(f, x, y):
    return shaded(COPPER, (210, 140, 92), (96, 56, 32), f, x, y, 0.03)


def m_inercita(f, x, y):
    if f.w > 4 and f.h > 2 and x in (1, f.w - 2) and y == f.h // 2:
        return col(INERCITA_HI, 1.1)
    return shaded(INERCITA, INERCITA_HI, INERCITA_LO, f, x, y)


def m_rearplate(f, x, y):
    """Placa trasera con tres luces de carga."""
    if f.name == "south" and abs(f.vf(y) - 0.4) < 0.12:
        if any(abs(f.uf(x) - c) < 0.09 for c in (0.25, 0.5, 0.75)):
            return col(CYAN_HI)
    return shaded(STEEL_BLK, STEEL_HI, STEEL_LO, f, x, y)


def m_mag_window(f, x, y):
    """Cargador con ventana: se ven los Fragmentos de Cinetrita apilados."""
    if f.side:
        w = window(f, x, y, 0.26, 0.74, 0.1, 0.9)
        if w == 1:
            return col(BLACK)
        if w == 2:
            return col(CYAN_HI) if y % 3 == 0 else col(CYAN, 0.8)
    return m_mag(f, x, y)


def m_optic(f, x, y):
    """Mira de Puntería Cinética: carcasa oscura, lente cian y retícula luminosa por detrás."""
    if f.name in ("north", "south"):
        w = window(f, x, y, 0.12, 0.88, 0.14, 0.86)
        if w == 1:
            return col(STEEL_LO)
        if w == 2:
            cx, cy = (f.w - 1) / 2, (f.h - 1) / 2
            if f.name == "south":
                d = math.hypot(x - cx, y - cy)
                if d < 0.8:
                    return col(CYAN_HI)                              # punto de la retícula
                if abs(d - 2.6) < 0.5:
                    return col(CYAN, 0.9)                            # anillo de la retícula
            if (x - y) % 7 == 0:
                return col((120, 200, 215))                          # reflejo en el cristal
            return col((26, 78, 92))                                 # cristal tintado
    if f.name == "up":
        return shaded(INERCITA, INERCITA_HI, INERCITA_LO, f, x, y)
    if f.side and abs(f.vf(y) - 0.5) < 0.12 and abs(f.uf(x) - 0.5) < 0.12:
        return col(CYAN)                                             # botón de calibración
    return shaded(STEEL_BLK, STEEL_HI, STEEL_LO, f, x, y)


def m_comp_inercita(f, x, y):
    """Cañón de Inercita: compensador largo con cuatro ranuras de energía."""
    if f.name == "north":
        cx, cy = (f.w - 1) / 2, (f.h - 1) / 2
        d = math.hypot(x - cx, y - cy)
        if d < f.w * 0.2:
            return col(BLACK)
        if d < f.w * 0.3:
            return col(CYAN)
    if f.side or f.name == "up":
        z = f.zf(x, y)
        across = f.vf(y) if f.side else f.uf(x)
        for z0 in (0.08, 0.31, 0.54, 0.77):
            if z0 <= z <= z0 + 0.14 and 0.3 <= across <= 0.7:
                edge = min(z - z0, z0 + 0.14 - z) < 0.035
                return col(INERCITA_LO) if edge else glow(f, x, y)
    return shaded(INERCITA, INERCITA_HI, INERCITA_LO, f, x, y, scratches=0.01)


def m_magwell(f, x, y):
    """Acelerador de Recarga: brocal de cobre con una línea de energía."""
    if f.side and abs(f.vf(y) - 0.5) < 0.5 / f.h + 0.05:
        return col(CYAN, 0.85)
    return shaded(COPPER, (210, 140, 92), (96, 56, 32), f, x, y, 0.03)


MATERIALS = {"slide": m_slide, "frame": m_frame, "grip": m_grip, "barrel": m_barrel, "mag": m_mag,
             "poly": m_poly, "steel": m_steel, "sight": m_sight, "cell": m_cell,
             "comp": m_comp, "coupler": m_coupler, "substrate": m_substrate, "copper": m_copper,
             "inercita": m_inercita, "rearplate": m_rearplate, "mag_window": m_mag_window,
             "optic": m_optic, "comp_inercita": m_comp_inercita, "magwell": m_magwell}

# Bits de los módulos (deben coincidir con el orden de WeaponModule.java)
OPTIC, EXTENDED_MAG, INERCITA_BARREL, QUICK_RELOAD = 1, 2, 4, 8
MODULE_IDS = {OPTIC: "optic", EXTENDED_MAG: "extended_mag", INERCITA_BARREL: "inercita_barrel", QUICK_RELOAD: "quick_reload"}

# ---------------------------------------------------------------- geometría (unidades de modelo)
GRIP_ROT = {"angle": -22.5, "axis": "x", "origin": [8, 11, 11]}

ELEMENTS = [
    # --- corredera (grupo "slide": se desplaza hacia atrás al montar el arma) ---
    {"name": "slide", "group": "slide", "mat": "slide", "from": [7, 12, 2.25], "to": [9, 14.5, 13]},
    {"name": "slide_nose", "group": "slide", "mat": "steel", "from": [7.15, 12.2, 1.6], "to": [8.85, 14.3, 2.25]},
    {"name": "rear_plate", "group": "slide", "mat": "rearplate", "from": [7.3, 12.3, 13], "to": [8.7, 14.2, 13.15]},
    {"name": "rear_sight_l", "group": "slide", "mat": "sight", "from": [7.05, 14.5, 12.1], "to": [7.6, 15, 12.8]},
    {"name": "rear_sight_r", "group": "slide", "mat": "sight", "from": [8.4, 14.5, 12.1], "to": [8.95, 15, 12.8]},
    {"name": "front_sight", "group": "slide", "mat": "sight", "from": [7.75, 14.5, 2.6], "to": [8.25, 14.95, 3.2]},
    # --- cañón (fijo) ---
    {"name": "barrel", "mat": "barrel", "from": [7.45, 12.7, 1.35], "to": [8.55, 13.8, 12]},
    # --- armazón de polímero ---
    {"name": "frame", "mat": "frame", "from": [7.05, 11, 2.6], "to": [8.95, 12, 12.6]},
    {"name": "kinetic_cell", "mat": "cell", "from": [7.0, 11.2, 3.2], "to": [9.0, 11.6, 5.6]},
    {"name": "beavertail", "mat": "inercita", "from": [7.25, 11.1, 12.6], "to": [8.75, 11.8, 13.6]},
    # --- piezas de la receta (detalles del mod) ---
    {"name": "compensator", "mat": "comp", "from": [7.15, 12.2, 0.4], "to": [8.85, 14.3, 1.6], "unless": INERCITA_BARREL},
    {"name": "kinetic_coupler", "mat": "coupler", "from": [6.75, 10.95, 6.4], "to": [9.25, 11.95, 8.3]},
    {"name": "conduit_l", "mat": "copper", "from": [6.85, 11.45, 5.4], "to": [7.05, 11.7, 6.4]},
    {"name": "conduit_r", "mat": "copper", "from": [8.95, 11.45, 5.4], "to": [9.15, 11.7, 6.4]},
    {"name": "grip_panel_l", "mat": "substrate", "from": [6.88, 6.4, 9.45], "to": [7.0, 10.3, 12.25], "rotation": GRIP_ROT},
    {"name": "grip_panel_r", "mat": "substrate", "from": [9.0, 6.4, 9.45], "to": [9.12, 10.3, 12.25], "rotation": GRIP_ROT},
    {"name": "slide_stop", "mat": "steel", "from": [6.9, 11.65, 6.6], "to": [7.05, 12.0, 8.6]},
    {"name": "guard_front", "mat": "poly", "from": [7.6, 9.6, 5.9], "to": [8.4, 11.0, 6.35]},
    {"name": "guard_bottom", "mat": "poly", "from": [7.6, 9.45, 5.9], "to": [8.4, 9.85, 9.6]},
    {"name": "trigger_top", "mat": "steel", "from": [7.85, 10.4, 8.0], "to": [8.15, 11.0, 8.35]},
    {"name": "trigger_low", "mat": "steel", "from": [7.85, 9.95, 7.8], "to": [8.15, 10.45, 8.15]},
    # --- empuñadura inclinada ---
    {"name": "grip", "mat": "grip", "from": [7.0, 5.6, 9.1], "to": [9.0, 11.0, 12.6], "rotation": GRIP_ROT},
    # --- cargador (grupo "mag": cae y vuelve a entrar al recargar) ---
    {"name": "mag_body", "group": "mag", "mat": "mag_window", "from": [7.25, 5.65, 9.5], "to": [8.75, 10.5, 12.0], "rotation": GRIP_ROT},
    {"name": "mag_base", "group": "mag", "mat": "poly", "from": [7.1, 5.0, 9.15], "to": [8.9, 5.6, 12.65], "rotation": GRIP_ROT, "unless": EXTENDED_MAG},
    # --- MÓDULOS DE MEJORA (solo aparecen si están instalados) ---
    {"name": "optic_mount", "group": "slide", "mat": "steel", "from": [7.35, 14.5, 8.6], "to": [8.65, 14.75, 10.9], "if": OPTIC},
    {"name": "optic_body", "group": "slide", "mat": "optic", "from": [7.15, 14.75, 8.9], "to": [8.85, 16.35, 10.6], "if": OPTIC},
    {"name": "mag_ext", "group": "mag", "mat": "mag_window", "from": [7.25, 2.9, 9.5], "to": [8.75, 5.65, 12.0], "rotation": GRIP_ROT, "if": EXTENDED_MAG},
    {"name": "mag_base_ext", "group": "mag", "mat": "inercita", "from": [7.1, 2.3, 9.15], "to": [8.9, 2.9, 12.65], "rotation": GRIP_ROT, "if": EXTENDED_MAG},
    {"name": "inercita_barrel", "mat": "comp_inercita", "from": [7.1, 12.15, -1.8], "to": [8.9, 14.35, 1.6], "if": INERCITA_BARREL},
    {"name": "magwell", "mat": "magwell", "from": [6.75, 5.15, 8.85], "to": [9.25, 6.2, 12.95], "rotation": GRIP_ROT, "if": QUICK_RELOAD},
]

DISPLAY = {
    "thirdperson_righthand": {"translation": [0, -1.5, 0]},
    "thirdperson_lefthand": {"translation": [0, -1.5, 0]},
    "firstperson_righthand": {"translation": [-1, 0, -3.25]},
    "firstperson_lefthand": {"translation": [-0.5, 0, -3]},
    "ground": {"translation": [0, -2, 0], "scale": [0.6, 0.6, 0.6]},
    "fixed": {"rotation": [0, 90, 0], "translation": [0, -3, 0]},
}

FACES = ["north", "south", "east", "west", "up", "down"]


def face_size(el, face):
    d = [el["to"][i] - el["from"][i] for i in range(3)]
    w, h = {"north": (d[0], d[1]), "south": (d[0], d[1]), "east": (d[2], d[1]),
            "west": (d[2], d[1]), "up": (d[0], d[2]), "down": (d[0], d[2])}[face]
    return max(1, round(w * PPU)), max(1, round(h * PPU))


# ---------------------------------------------------------------- empaquetado de textura
rects = [(i, f, *face_size(el, f)) for i, el in enumerate(ELEMENTS) for f in FACES]
for SIZE in (64, 128, 256):
    placed, x, y, row = {}, 0, 0, 0
    ok = True
    for i, f, w, h in sorted(rects, key=lambda r: (-r[3], -r[2])):
        if x + w > SIZE:
            x, y, row = 0, y + row, 0
        if y + h > SIZE:
            ok = False
            break
        placed[(i, f)] = (x, y, w, h)
        x += w
        row = max(row, h)
    if ok:
        break

tex = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
uv = {}
for (i, f), (x0, y0, w, h) in placed.items():
    face = Face(f, w, h)
    paint = MATERIALS[ELEMENTS[i]["mat"]]
    for yy in range(h):
        for xx in range(w):
            tex.putpixel((x0 + xx, y0 + yy), paint(face, xx, yy))
    s = 16 / SIZE
    uv[(i, f)] = [round(x0 * s, 4), round(y0 * s, 4), round((x0 + w) * s, 4), round((y0 + h) * s, 4)]
tex.save(ASSETS / "textures/item/kinetic_pistol.png")


# ---------------------------------------------------------------- modelos (base + fotogramas de recarga)
def element_json(i, el, offset=(0, 0, 0)):
    out = {"name": el["name"],
           "from": [round(el["from"][k] + offset[k], 4) for k in range(3)],
           "to": [round(el["to"][k] + offset[k], 4) for k in range(3)],
           "faces": {f: {"uv": uv[(i, f)], "texture": "#0"} for f in FACES}}
    if "rotation" in el:
        out["rotation"] = el["rotation"]
    return out


def visible(el, mask):
    """¿Se muestra la pieza con esta combinación de módulos?"""
    if "if" in el and not (mask & el["if"]):
        return False
    if "unless" in el and (mask & el["unless"]):
        return False
    return True


def model(mag_offset=None, slide_back=0.0, display=None, mask=0):
    """mag_offset=None -> sin cargador; (dx,dy,dz) en el sistema de la empuñadura."""
    els = []
    for i, el in enumerate(ELEMENTS):
        if not visible(el, mask):
            continue
        g = el.get("group")
        if g == "mag":
            if mag_offset is None:
                continue
            els.append(element_json(i, el, mag_offset))
        elif g == "slide":
            els.append(element_json(i, el, (0, 0, slide_back)))
        else:
            els.append(element_json(i, el))
    return {"credit": "Tecnocinetico - pistol_gen.py", "texture_size": [SIZE, SIZE],
            "textures": {"0": "tecnocinetico:item/kinetic_pistol", "particle": "tecnocinetico:item/kinetic_pistol"},
            "elements": els, "display": display or DISPLAY}


def write(name, data):
    (ASSETS / "models/item" / f"{name}.json").write_text(json.dumps(data, indent=1), encoding="utf-8")


def wrapper(base_name, overrides=None):
    data = {"loader": "forge:separate_transforms", "gui_light": "front",
            "base": {"parent": f"tecnocinetico:item/{base_name}"},
            "perspectives": {"gui": {"parent": "minecraft:item/generated",
                                     "textures": {"layer0": "tecnocinetico:item/kinetic_pistol_icon"}}}}
    if overrides:
        data["overrides"] = overrides
    return data


FRAMES = [
    # (progreso mínimo, cargador, corredera atrás)
    (0.01, (0, -2.5, 0), 0.0),   # 1: el cargador empieza a caer
    (0.25, None, 0.0),           # 2: sin cargador
    (0.50, (0, -2.0, 0), 0.0),   # 3: cargador nuevo entrando
    (0.72, (0, 0, 0), 1.6),      # 4: cargador dentro, corredera atrás (montar)
    (0.90, (0, 0, 0), 0.0),      # 5: corredera vuelve
]

# El inventario usa el propio modelo 3D (como los mods de armas realistas); gui_fit.py añade la vista "gui".
# Un modelo por combinación de módulos (16) y fotograma de recarga (base + 4).
# Minecraft elige el ÚLTIMO override cuyos predicados se cumplen (valor >= umbral), así que se ordenan
# por máscara y luego por progreso de recarga: para la máscara M gana el bloque de M.
def pistol_name(mask, frame=0):
    prefix = "kinetic_pistol" if mask == 0 else f"kinetic_pistol_m{mask}"
    return f"{prefix}_3d" if frame == 0 else f"{prefix}_reload_{frame}_3d"


overrides = []
for mask in range(16):
    write(pistol_name(mask), model((0, 0, 0), mask=mask))
    if mask:
        overrides.append({"predicate": {"tecnocinetico:modules": mask, "tecnocinetico:reload": 0},
                          "model": f"tecnocinetico:item/{pistol_name(mask)}"})
    for n, (p, mag, slide) in enumerate(FRAMES[:4], start=1):
        write(pistol_name(mask, n), model(mag, slide, mask=mask))
        overrides.append({"predicate": {"tecnocinetico:modules": mask, "tecnocinetico:reload": p},
                          "model": f"tecnocinetico:item/{pistol_name(mask, n)}"})
    overrides.append({"predicate": {"tecnocinetico:modules": mask, "tecnocinetico:reload": FRAMES[4][0]},
                      "model": f"tecnocinetico:item/{pistol_name(mask)}"})
write("kinetic_pistol", {"parent": "tecnocinetico:item/kinetic_pistol_3d", "overrides": overrides})

# Objetos de los módulos: solo las piezas del módulo, centradas
MODULE_DISPLAY = {
    "thirdperson_righthand": {"translation": [0, 2, 1], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_lefthand": {"translation": [0, 2, 1], "scale": [0.5, 0.5, 0.5]},
    "firstperson_righthand": {"rotation": [0, -90, 0], "translation": [1, 3, 0], "scale": [0.6, 0.6, 0.6]},
    "firstperson_lefthand": {"rotation": [0, -90, 0], "translation": [1, 3, 0], "scale": [0.6, 0.6, 0.6]},
    "ground": {"translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 90, 0]},
}
for bit, mid in MODULE_IDS.items():
    parts = [(i, el) for i, el in enumerate(ELEMENTS) if el.get("if") == bit]
    lo = [min(el["from"][k] for _, el in parts) for k in range(3)]
    hi = [max(el["to"][k] for _, el in parts) for k in range(3)]
    delta = [8 - (lo[k] + hi[k]) / 2 for k in range(3)]
    els = []
    for i, el in parts:
        e = element_json(i, el, delta)
        if "rotation" in e:
            e["rotation"] = dict(e["rotation"], origin=[round(e["rotation"]["origin"][k] + delta[k], 4) for k in range(3)])
        els.append(e)
    write(f"module_{mid}", {"credit": "Tecnocinetico - pistol_gen.py", "texture_size": [SIZE, SIZE],
                            "textures": {"0": "tecnocinetico:item/kinetic_pistol",
                                         "particle": "tecnocinetico:item/kinetic_pistol"},
                            "elements": els, "display": MODULE_DISPLAY})
for old in ["kinetic_pistol_3d_wrapped"] + [f"kinetic_pistol_reload_{n}" for n in range(1, 5)]:
    (ASSETS / "models/item" / f"{old}.json").unlink(missing_ok=True)

# ---------------------------------------------------------------- icono 16x16 realista (de perfil, apuntando a la derecha)
ICON = [
    "................",
    "................",
    "................",
    "..d..........d..",
    ".sssssssssssssss",
    ".dSdSSSSSCcCSSSB",
    ".SSSSSSSSSSSSSS.",
    "..FFFFFFFFFFFF..",
    "..gGGG.T.d......",
    "..gGGG.ddd......",
    ".gGGG...........",
    ".gGGG...........",
    "gGGG............",
    "mmmm............",
    "................",
    "................",
]
ICON_PAL = {"s": (86, 90, 98), "S": (52, 55, 61), "d": (24, 25, 28), "F": (40, 41, 45), "G": (50, 52, 57),
            "g": (32, 33, 36), "T": (70, 72, 78), "C": CYAN, "c": CYAN_HI, "B": BARREL, "m": (62, 64, 70)}
icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for yy, row in enumerate(ICON):
    for xx, ch in enumerate(row):
        if ch in ICON_PAL:
            icon.putpixel((xx, yy), col(ICON_PAL[ch]))
outlined = icon.copy()
for yy in range(16):
    for xx in range(16):
        if icon.getpixel((xx, yy))[3]:
            continue
        if any(0 <= xx + dx < 16 and 0 <= yy + dy < 16 and icon.getpixel((xx + dx, yy + dy))[3]
               for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            outlined.putpixel((xx, yy), col(OUTLINE))
pass  # el icono plano ya no se usa

if PREVIEW:
    sheet = Image.new("RGBA", (800, 400), (70, 70, 74, 255))
    big = tex.resize((384, 384), Image.NEAREST)
    sheet.paste(big, (8, 8), big)
    ib = outlined.resize((320, 320), Image.NEAREST)
    sheet.paste(ib, (440, 40), ib)
    sheet.save(PREVIEW / "pistol_preview.png")
print("textura", SIZE, "elementos", len(ELEMENTS))

