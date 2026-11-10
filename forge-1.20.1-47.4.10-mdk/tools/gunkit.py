"""Kit de modelos de armas con textura propia por arma (como la Pistola Cinética de pistol_gen.py).

Cada arma (y todas sus variantes: módulos, fotogramas de recarga) comparte una textura `textures/item/gun/<grupo>.png`
donde CADA CARA de cada pieza tiene su trozo, pintado por el "pintor" de su material a 2-4 px por unidad de modelo:
acero pavonado con bisel y arañazos, madera con veta, polímero punteado, fibra de carbono, pintura oliva desconchada,
cobre cepillado, franjas de peligro, cinetrita luminosa, bocas de cañón con ánima... Así cada arma tiene detalle de
verdad en lugar de cuadros de color plano.

Uso desde un generador:
    from gunkit import el, model, flush
    model("mi_arma", [el([x0,y0,z0],[x1,y1,z1], "steel_dark", front="black"), ...], DISPLAY, overrides, group="mi_arma")
    flush()      # al final: empaqueta, pinta y escribe texturas y modelos (conserva display.gui)
"""
import json
import math
import random
import zlib
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tecnocinetico"
FACES = ("north", "south", "east", "west", "up", "down")


def clamp(v):
    return max(0, min(255, int(round(v))))


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def rgba(c, k=1.0, a=255):
    return (clamp(c[0] * k), clamp(c[1] * k), clamp(c[2] * k), a)


# ===================================================================== caras
class Face:
    """Una cara de un elemento en la textura: tamaño en px y orientación."""

    def __init__(self, name, w, h, size):
        self.name, self.w, self.h = name, w, h
        self.size = size                               # (dx, dy, dz) del elemento en unidades

    @property
    def side(self):
        return self.name in ("east", "west", "north", "south")

    def uf(self, x):
        return (x + 0.5) / self.w

    def vf(self, y):
        return (y + 0.5) / self.h

    def along(self, x, y):
        """Coordenada a lo largo del lado largo de la cara (px) y la transversal."""
        return (x, y) if self.w >= self.h else (y, x)

    def long_len(self):
        return max(self.w, self.h)


# ===================================================================== sombreado común
def shade(ramp, f, x, y, rnd, noise=0.035, bevel=True):
    """ramp = (sombra, base, luz, brillo). Luz desde arriba, bisel en las aristas, ruido fino."""
    lo, base, hi, hi2 = ramp
    if f.name == "up":
        c = mix(base, hi, 0.55)
    elif f.name == "down":
        c = mix(base, lo, 0.6)
    else:
        v = f.vf(y)
        c = mix(hi, base, min(1.0, v / 0.35)) if v < 0.35 else mix(base, lo, (v - 0.6) / 0.4) if v > 0.6 else base
    if bevel and f.w > 2 and f.h > 2:
        if y == 0:
            c = mix(c, hi2, 0.7)
        elif y == f.h - 1:
            c = mix(c, lo, 0.7)
        if x == 0 or x == f.w - 1:
            c = mix(c, lo, 0.35)
    return rgba(c, 1 + rnd.uniform(-noise, noise))


def details(f, x, y, ramp, rnd_seed):
    """Juntas y pasadores en caras laterales grandes (devuelve color o None)."""
    if not f.side or f.w < 18 or f.h < 4:
        return None
    lo, base, hi, hi2 = ramp
    # cada cara decide sus juntas y pasadores con una semilla propia (tamaño y orientación): no todas iguales
    r = random.Random(zlib.crc32(f"{f.name}{f.w}x{f.h}{ramp[1]}".encode()))
    seams = [r.uniform(0.2, 0.8) for _ in range(r.choice((0, 1, 1, 2)))]
    pins = [r.uniform(0.06, 0.94) for _ in range(r.choice((0, 1, 2, 2, 3)))]
    pin_y = r.choice((f.h // 2, max(1, f.h // 3), f.h - 2))
    for frac in seams:                                          # juntas de panel
        if x == int(f.w * frac) and 0 < y < f.h - 1:
            return rgba(lo, 0.8)
    for frac in pins:                                           # pasadores / tornillos
        px = int(f.w * frac)
        if (x, y) == (px, pin_y):
            return rgba(hi2)
        if abs(x - px) + abs(y - pin_y) == 1:
            return rgba(lo, 0.85)
    return None


# ===================================================================== paletas
P = {
    "steel_dark": ((20, 21, 25), (38, 40, 45), (62, 65, 72), (100, 104, 112)),
    "steel": ((46, 49, 55), (72, 76, 84), (100, 104, 112), (146, 150, 156)),
    "steel_hi": ((84, 88, 94), (124, 128, 134), (160, 164, 170), (210, 213, 216)),
    "inercita": ((38, 44, 58), (62, 70, 88), (88, 98, 120), (128, 140, 164)),
    "graphite": ((22, 23, 27), (36, 38, 44), (52, 54, 62), (78, 82, 92)),
    "rubber": ((16, 16, 18), (28, 29, 32), (40, 41, 45), (58, 60, 64)),
    "wood": ((74, 46, 26), (106, 70, 40), (132, 90, 54), (160, 116, 74)),
    "wood_dark": ((48, 30, 18), (72, 46, 28), (92, 60, 36), (116, 80, 50)),
    "leather": ((54, 34, 21), (80, 52, 33), (102, 68, 43), (126, 88, 58)),
    "copper": ((92, 54, 30), (146, 90, 54), (192, 128, 84), (232, 176, 124)),
    "brass": ((124, 96, 40), (182, 146, 70), (216, 182, 102), (246, 222, 152)),
    "shell_red": ((100, 22, 18), (146, 34, 28), (182, 52, 40), (214, 92, 72)),
    "rust": ((78, 40, 22), (116, 60, 32), (150, 84, 44), (182, 116, 66)),
    "blade": ((96, 100, 106), (134, 138, 144), (166, 170, 176), (210, 213, 218)),
    "edge": ((168, 172, 178), (204, 208, 212), (226, 229, 232), (246, 248, 250)),
    "tape": ((70, 70, 66), (96, 96, 90), (116, 116, 110), (140, 140, 134)),
    "olive": ((48, 54, 32), (70, 77, 46), (92, 100, 62), (118, 126, 84)),
    "black": ((6, 6, 8), (10, 10, 12), (16, 16, 18), (24, 24, 28)),
    "ceramic": ((150, 140, 124), (192, 184, 168), (220, 214, 200), (242, 238, 228)),
    "glass": ((24, 60, 70), (40, 96, 110), (70, 140, 156), (150, 210, 220)),
}
CYAN = ((24, 110, 126), (52, 190, 210), (120, 236, 248), (200, 252, 255))
HAZ_Y, HAZ_B = (214, 168, 40), (28, 28, 30)


# ===================================================================== pintores de material
def p_metal(mat):
    ramp = P[mat]

    def paint(f, x, y, rnd):
        d = details(f, x, y, ramp, 0)
        if d:
            return d
        c = shade(ramp, f, x, y, rnd)
        if mat == "steel_hi":                                    # acero pulido: estrías a lo largo
            a, b = f.along(x, y)
            if (b * 7 + a // 5) % 4 == 0:
                c = rgba(mix(c[:3], ramp[3], 0.35))
        if mat in ("steel_dark", "steel") and rnd.random() < 0.012:   # arañazos finos
            c = rgba(mix(c[:3], ramp[3], 0.6))
        return c
    return paint


def p_inercita(f, x, y, rnd):
    ramp = P["inercita"]
    d = details(f, x, y, ramp, 0)
    if d:
        return d
    c = shade(ramp, f, x, y, rnd, 0.025)
    a, b = f.along(x, y)
    if (a + 2 * b) % 9 == 0 and 0 < y < f.h - 1:
        c = rgba(mix(c[:3], ramp[3], 0.3))                       # cepillado
    return c


def p_graphite(f, x, y, rnd):
    """Fibra de carbono: tejido en damero 2x2 con brillo."""
    ramp = P["graphite"]
    d = details(f, x, y, ramp, 0)
    if d:
        return d
    c = shade(ramp, f, x, y, rnd, 0.02)
    if ((x // 2) + (y // 2)) % 2 == 0:
        c = rgba(mix(c[:3], ramp[2], 0.35))
    return c


def p_rubber(f, x, y, rnd):
    """Polímero punteado (agarre)."""
    c = shade(P["rubber"], f, x, y, rnd, 0.02)
    if rnd.random() < 0.25:
        c = rgba(mix(c[:3], P["rubber"][0], 0.6))
    return c


def p_wood(mat):
    ramp = P[mat]

    def paint(f, x, y, rnd):
        a, b = f.along(x, y)
        c = shade(ramp, f, x, y, rnd, 0.02)
        g = math.sin(b * 2.1 + 0.8 * math.sin(a * 0.13 + f.w * 0.7))
        if g > 0.82:
            c = rgba(mix(c[:3], ramp[0], 0.55))                   # vetas oscuras
        elif g < -0.9:
            c = rgba(mix(c[:3], ramp[3], 0.25))
        return c
    return paint


def p_leather(f, x, y, rnd):
    c = shade(P["leather"], f, x, y, rnd, 0.03)
    if f.w > 5 and f.h > 5 and (x in (1, f.w - 2) or y in (1, f.h - 2)) and (x + y) % 2 == 0:
        return rgba((170, 138, 92))                               # costura
    return c


def p_copper(mat):
    ramp = P[mat]

    def paint(f, x, y, rnd):
        c = shade(ramp, f, x, y, rnd, 0.025)
        a, b = f.along(x, y)
        if b % 3 == 0 and f.side:
            c = rgba(mix(c[:3], ramp[0], 0.25))                   # cepillado / anillos
        return c
    return paint


def p_shell(f, x, y, rnd):
    c = shade(P["shell_red"], f, x, y, rnd, 0.02)
    if f.side and f.w >= f.h and x % 3 == 0:
        c = rgba(mix(c[:3], P["shell_red"][0], 0.35))             # estrías del cartucho
    return c


def p_rust(f, x, y, rnd):
    ramp = P["rust"]
    c = shade(ramp, f, x, y, rnd, 0.06)
    r = rnd.random()
    if r < 0.12:
        return rgba(ramp[0], 0.85)                                # picaduras
    if r < 0.22:
        return rgba(mix(ramp[2], (150, 150, 150), 0.3))           # restos de metal
    return c


def p_blade(f, x, y, rnd):
    c = shade(P["blade"], f, x, y, rnd, 0.02)
    a, b = f.along(x, y)
    if f.side and b == 1:
        c = rgba(mix(c[:3], (160, 150, 190), 0.4))                # línea de templado
    return c


def p_tape(f, x, y, rnd):
    ramp = P["tape"]
    c = shade(ramp, f, x, y, rnd, 0.05, bevel=False)
    if ((x + y) // 2) % 3 == 0:
        c = rgba(mix(c[:3], ramp[0], 0.45))                       # vueltas de cinta
    return c


def p_olive(f, x, y, rnd):
    """Pintura oliva con desconchones que dejan ver el acero en las aristas."""
    ramp = P["olive"]
    edge = x in (0, f.w - 1) or y in (0, f.h - 1)
    if (edge and rnd.random() < 0.3) or rnd.random() < 0.015:
        return rgba(P["steel"][2], 1 + rnd.uniform(-0.05, 0.05))
    return shade(ramp, f, x, y, rnd, 0.03)


def p_hazard(f, x, y, rnd):
    c = HAZ_Y if ((x + y) // 3) % 2 == 0 else HAZ_B
    k = 1.1 if y == 0 else 0.8 if y == f.h - 1 else 1.0
    if rnd.random() < 0.05:
        c = mix(c, (60, 60, 60), 0.5)                             # desgaste
    return rgba(c, k)


def p_cyan(f, x, y, rnd):
    """Cinetrita luminosa: núcleo brillante, borde oscuro."""
    cx, cy = (f.w - 1) / 2, (f.h - 1) / 2
    d = max(abs(x - cx) / max(1, f.w / 2), abs(y - cy) / max(1, f.h / 2))
    if f.w > 2 and f.h > 2 and (x in (0, f.w - 1) or y in (0, f.h - 1)):
        return rgba(CYAN[0])
    c = mix(CYAN[3], CYAN[1], d) if d < 0.5 else mix(CYAN[1], CYAN[0], (d - 0.5) * 1.4)
    return rgba(c, 1 + rnd.uniform(-0.03, 0.03))


def p_bore(f, x, y, rnd):
    """Boca de cañón / ánima: aro de acero y negro dentro."""
    if f.w >= 3 and f.h >= 3 and (x in (0, f.w - 1) or y in (0, f.h - 1)):
        return rgba(P["steel_dark"][2])
    return rgba(P["black"][1])


def p_ceramic(f, x, y, rnd):
    """Aislador de porcelana: esmalte claro con anillos (faldones) más oscuros."""
    ramp = P["ceramic"]
    c = shade(ramp, f, x, y, rnd, 0.02)
    a, b = f.along(x, y)
    if f.side and b % 2 == 1:
        c = rgba(mix(c[:3], ramp[0], 0.35))
    return c


def p_glass(f, x, y, rnd):
    """Tubo de vidrio con bobinado de cobre dentro y reflejo."""
    ramp = P["glass"]
    c = shade(ramp, f, x, y, rnd, 0.02, bevel=False)
    a, b = f.along(x, y)
    if f.side and a % 2 == 0:
        c = rgba(mix(c[:3], P["copper"][2], 0.55))                # espiras de cobre vistas a través del vidrio
    if y == 1:
        c = rgba(mix(c[:3], (230, 250, 255), 0.5))                # reflejo
    return c


PAINTERS = {
    "ceramic": p_ceramic, "glass": p_glass,
    "steel_dark": p_metal("steel_dark"), "steel": p_metal("steel"), "steel_hi": p_metal("steel_hi"),
    "inercita": p_inercita, "graphite": p_graphite, "rubber": p_rubber,
    "wood": p_wood("wood"), "wood_dark": p_wood("wood_dark"), "leather": p_leather,
    "copper": p_copper("copper"), "brass": p_copper("brass"), "shell_red": p_shell, "rust": p_rust,
    "blade": p_blade, "edge": p_metal("steel_hi"), "tape": p_tape, "olive": p_olive, "hazard": p_hazard,
    "cyan": p_cyan, "black": p_bore,
}


# ===================================================================== elementos y modelos
def el(fr, to, mat, side=None, rot=None, front=None):
    """Elemento de modelo: `mat` arriba/abajo, `side` en los lados (por defecto `mat`), `front` en la cara norte."""
    side = side or mat
    mats = {f: (mat if f in ("up", "down") else side) for f in FACES}
    if front:
        mats["north"] = front
    e = {"from": [round(v, 4) for v in fr], "to": [round(v, 4) for v in to], "_mats": mats}
    if rot:
        e["rotation"] = rot
    return e


def scaled(els, k, origin):
    """Escala un grupo de elementos alrededor de `origin` (para hacer un arma más pequeña)."""
    out = []
    for e in els:
        e2 = dict(e)
        e2["from"] = [round(origin[i] + (e["from"][i] - origin[i]) * k, 4) for i in range(3)]
        e2["to"] = [round(origin[i] + (e["to"][i] - origin[i]) * k, 4) for i in range(3)]
        if "rotation" in e:
            r = dict(e["rotation"])
            r["origin"] = [round(origin[i] + (r["origin"][i] - origin[i]) * k, 4) for i in range(3)]
            e2["rotation"] = r
        out.append(e2)
    return out


_MODELS = []          # (name, elements, display, overrides, group)


def _same_rot(a, b):
    return json.dumps(a.get("rotation"), sort_keys=True) == json.dumps(b.get("rotation"), sort_keys=True)


def resolve_zfight(elements, eps=0.015):
    """Evita el parpadeo de texturas (z-fighting): si dos piezas tienen una cara en el mismo plano, mirando al mismo
    lado y solapadas, la pieza MÁS PEQUEÑA se adelanta `eps` por esa cara y queda siempre encima."""
    els = [dict(e, **{"from": list(e["from"]), "to": list(e["to"])}) for e in elements]

    def vol(e):
        return (e["to"][0] - e["from"][0]) * (e["to"][1] - e["from"][1]) * (e["to"][2] - e["from"][2])

    for _ in range(4):
        changed = False
        for i in range(len(els)):
            for j in range(i + 1, len(els)):
                a, b = els[i], els[j]
                if not _same_rot(a, b):
                    continue
                for ax in range(3):
                    for key, sign in (("from", -1), ("to", 1)):
                        if abs(a[key][ax] - b[key][ax]) > 1e-4:
                            continue
                        if all(min(a["to"][k], b["to"][k]) - max(a["from"][k], b["from"][k]) > 1e-4 for k in range(3) if k != ax):
                            small, big = (a, b) if vol(a) <= vol(b) else (b, a)
                            moved = round(small[key][ax] + sign * eps, 4)
                            if -16 <= moved <= 32:
                                small[key][ax] = moved                     # la pequeña sale un poco
                            else:                                          # en el límite del modelo: la grande entra un poco
                                big[key][ax] = round(big[key][ax] - sign * eps, 4)
                            changed = True
        if not changed:
            break
    return els


def model(name, elements, display, overrides=None, group=None):
    _MODELS.append((name, resolve_zfight(elements), display, overrides, group or name))


def _key(e):
    return (tuple(e["from"]), tuple(e["to"]), tuple(sorted(e["_mats"].items())), json.dumps(e.get("rotation"), sort_keys=True))


def _face_dims(e, face):
    dx, dy, dz = (abs(e["to"][i] - e["from"][i]) for i in range(3))
    return {"north": (dx, dy), "south": (dx, dy), "east": (dz, dy), "west": (dz, dy), "up": (dx, dz), "down": (dx, dz)}[face]


def _pack(rects, size):
    """Empaquetado en estantes. rects = [(id, w, h)] -> {id: (x, y)} o None si no cabe."""
    out, x, y, shelf = {}, 0, 0, 0
    for rid, w, h in sorted(rects, key=lambda r: (-r[2], -r[1])):
        if w > size:
            return None
        if x + w > size:
            x, y, shelf = 0, y + shelf, 0
        if y + h > size:
            return None
        out[rid] = (x, y)
        x += w
        shelf = max(shelf, h)
    return out


def flush():
    groups = {}
    for m in _MODELS:
        groups.setdefault(m[4], []).append(m)
    for group, models in groups.items():
        uniq = {}
        for _, els, *_ in models:
            for e in els:
                uniq.setdefault(_key(e), e)
        elements = list(uniq.values())
        layout = None
        for size in (64, 128):
            for ppu in (4, 3, 2.5, 2, 1.5, 1):
                rects = []
                for i, e in enumerate(elements):
                    for f in FACES:
                        a, b = _face_dims(e, f)
                        rects.append(((i, f), max(1, round(a * ppu)), max(1, round(b * ppu))))
                pos = _pack(rects, size)
                if pos and (size == 128 or ppu >= 3):
                    layout = (size, ppu, rects, pos)
                    break
            if layout:
                break
        size, ppu, rects, pos = layout
        img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        uvs = {}
        for (rid, w, h) in rects:
            i, fname = rid
            e = elements[i]
            px, py = pos[rid]
            mat = e["_mats"][fname]
            rnd = random.Random(zlib.crc32(f"{group}|{i}|{fname}".encode()))          # determinista
            face = Face(fname, w, h, None)
            painter = PAINTERS[mat]
            for yy in range(h):
                for xx in range(w):
                    img.putpixel((px + xx, py + yy), painter(face, xx, yy, rnd))
            k = 16.0 / size
            uvs[rid] = [round(px * k, 4), round(py * k, 4), round((px + w) * k, 4), round((py + h) * k, 4)]
        tex_rel = f"tecnocinetico:item/gun/{group}"
        (ROOT / "textures/item/gun").mkdir(parents=True, exist_ok=True)
        img.save(ROOT / "textures/item/gun" / f"{group}.png")
        index = {_key(e): i for i, e in enumerate(elements)}
        for name, els, display, overrides, _ in models:
            out = []
            for e in els:
                i = index[_key(e)]
                j = {"from": e["from"], "to": e["to"],
                     "faces": {f: {"uv": uvs[(i, f)], "texture": "#0"} for f in FACES}}
                if "rotation" in e:
                    j["rotation"] = e["rotation"]
                out.append(j)
            path = ROOT / "models/item" / f"{name}.json"
            old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
            data = {"credit": "Tecnocinetico - gunkit.py", "texture_size": [size, size],
                    "textures": {"0": tex_rel, "particle": tex_rel}, "elements": out, "display": dict(display)}
            if "gui" in old.get("display", {}):
                data["display"]["gui"] = old["display"]["gui"]
            if overrides:
                data["overrides"] = overrides
            path.write_text(json.dumps(data, indent=2), encoding="utf-8")
    _MODELS.clear()
