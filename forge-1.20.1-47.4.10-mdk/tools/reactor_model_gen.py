"""Reactor de Fisión como UNA sola máquina de 9x9x6 bloques, troceada en 18 modelos de 3x3x3.

python tools/reactor_model_gen.py      (desde forge-1.20.1-47.4.10-mdk)

La geometría se define en coordenadas locales de la estructura (en bloques): u = derecha del jugador que la
despliega, v = arriba, w = hacia delante (el frente, w = 0, mira al jugador). Cada caja se recorta a los 18
bloques de 3x3x3 y se corta por bloques; solo se emiten las caras del borde de la caja original. Las texturas se
calculan con la posición real en el reactor (o relativas a la caja, para escotilla, rejillas y pantallas), así que
los trozos encajan sin costuras. Escribe textures/block/reactor.png (atlas 128x128, zonas de 32 px),
models/block/reactor_shell_N.json (N = 1..18), reactor_shell_empty.json, fission_reactor(_on).json (= trozo 5, el del
núcleo) y los estados de reactor_shell y fission_reactor.
"""
import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

A = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tecnocinetico"
ZONE = 32                                           # px por zona; atlas 128 -> 1 unidad UV = 8 px
# material: (columna, fila de la zona, periodo en bloques, anclado a la caja)
MATS = {"wall": (0, 0, 2, False), "frame": (1, 0, 1, False), "hazard": (2, 0, 1, False), "roof": (3, 0, 2, False),
        "glass": (0, 1, 0, True), "vessel": (1, 1, 1, False), "hatch": (2, 1, 0, True), "vent": (3, 1, 0, True),
        "copper": (0, 2, 1, False), "screen": (1, 2, 0, True), "interior": (2, 2, 1, False), "cyan": (3, 2, 1, False),
        "base": (0, 3, 1, False), "sign": (1, 3, 0, True)}


# =================================================================== atlas
def paint_atlas():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r = random.Random(42)

    def z(name):
        c, rr = MATS[name][:2]
        return c * ZONE, rr * ZONE

    def fill(name, base, noise=0.025, alpha=255):
        x0, y0 = z(name)
        for y in range(ZONE):
            for x in range(ZONE):
                k = 1 + r.uniform(-noise, noise)
                img.putpixel((x0 + x, y0 + y), tuple(max(0, min(255, int(v * k))) for v in base) + (alpha,))
        return x0, y0

    # blindaje: grandes paneles claros (una losa cada 2 bloques), juntas finas y tornillos discretos
    x0, y0 = fill("wall", (150, 156, 160))
    d.line([(x0, y0), (x0 + 31, y0)], fill=(104, 110, 116)); d.line([(x0, y0), (x0, y0 + 31)], fill=(104, 110, 116))
    d.line([(x0, y0 + 1), (x0 + 31, y0 + 1)], fill=(184, 190, 194)); d.line([(x0 + 1, y0), (x0 + 1, y0 + 31)], fill=(176, 182, 186))
    for (px, py) in ((4, 4), (27, 4), (4, 27), (27, 27)):
        d.point((x0 + px, y0 + py), fill=(110, 116, 122))
    for x in range(6, 26, 5):                                                # nervios verticales
        d.line([(x0 + x, y0 + 6), (x0 + x, y0 + 25)], fill=(140, 146, 150))
    fill("frame", (58, 62, 70))
    x0, y0 = z("frame")
    d.line([(x0 + 2, y0), (x0 + 2, y0 + 31)], fill=(84, 90, 100)); d.line([(x0 + 29, y0), (x0 + 29, y0 + 31)], fill=(40, 42, 48))
    # franjas de peligro
    x0, y0 = z("hazard")
    for y in range(ZONE):
        for x in range(ZONE):
            img.putpixel((x0 + x, y0 + y), ((222, 182, 42) if ((x + y) // 8) % 2 == 0 else (34, 34, 36)) + (255,))
    # tejado: chapa estriada
    x0, y0 = fill("roof", (112, 116, 120))
    for y in range(0, ZONE, 6):
        for x in range(0, ZONE, 6):
            ox = 3 if (y // 6) % 2 else 0
            d.line([(x0 + x + ox, y0 + y + 2), (x0 + x + ox + 2, y0 + y)], fill=(140, 144, 148))
    # vidrios
    x0, y0 = fill("glass", (160, 205, 220), 0.01, 80)
    d.line([(x0 + 4, y0 + 3), (x0 + 12, y0 + 3)], fill=(235, 250, 255, 170)); d.line([(x0 + 4, y0 + 3), (x0 + 4, y0 + 11)], fill=(235, 250, 255, 170))
    x0, y0 = fill("vessel", (90, 220, 170), 0.01, 70)
    d.line([(x0 + 6, y0 + 2), (x0 + 6, y0 + 29)], fill=(220, 255, 240, 150))
    # escotilla con el símbolo de radiación (toda la puerta)
    x0, y0 = fill("hatch", (96, 100, 106))
    d.rectangle([x0, y0, x0 + 31, y0 + 31], outline=(40, 42, 46))
    d.rectangle([x0 + 2, y0 + 2, x0 + 29, y0 + 29], outline=(130, 134, 140))
    d.line([(x0 + 16, y0 + 3), (x0 + 16, y0 + 28)], fill=(60, 62, 66))           # dos hojas
    cx, cy = x0 + 16, y0 + 13
    d.ellipse([cx - 7, cy - 7, cx + 7, cy + 7], fill=(222, 182, 42), outline=(40, 40, 40))
    d.ellipse([cx - 1, cy - 1, cx + 1, cy + 1], fill=(30, 30, 30))
    for a in (90, 210, 330):
        for rad in range(2, 7):
            for da in range(-28, 29, 4):
                px = cx + rad * math.cos(math.radians(a + da)); py = cy + rad * math.sin(math.radians(a + da))
                img.putpixel((int(px), int(py)), (30, 30, 30, 255))
    for yy in (22, 25):
        d.line([(x0 + 6, y0 + yy), (x0 + 26, y0 + yy)], fill=(222, 182, 42))
    # respiradero (ventilador)
    x0, y0 = fill("vent", (60, 64, 70))
    d.ellipse([x0 + 3, y0 + 3, x0 + 28, y0 + 28], fill=(26, 28, 32), outline=(130, 136, 146))
    for a in range(0, 360, 45):
        px = x0 + 15.5 + 11 * math.cos(math.radians(a)); py = y0 + 15.5 + 11 * math.sin(math.radians(a))
        d.line([(x0 + 15.5, y0 + 15.5), (px, py)], fill=(96, 102, 112), width=2)
    d.ellipse([x0 + 13, y0 + 13, x0 + 18, y0 + 18], fill=(150, 156, 166))
    # tuberías de cobre
    x0, y0 = z("copper")
    for y in range(ZONE):
        for x in range(ZONE):
            t = abs((y % 8) - 3.5) / 3.5
            img.putpixel((x0 + x, y0 + y), (int(206 - 80 * t), int(124 - 50 * t), int(72 - 34 * t), 255))
    # pantalla de control
    x0, y0 = fill("screen", (50, 54, 60))
    d.rectangle([x0 + 3, y0 + 3, x0 + 28, y0 + 18], fill=(8, 24, 28), outline=(110, 116, 126))
    for i in range(24):
        d.point((x0 + 4 + i, y0 + 14 - int(6 * abs(math.sin(i * 0.45)))), fill=(90, 230, 120))
    for i, c in enumerate(((70, 215, 235), (220, 60, 50), (230, 200, 60), (90, 230, 120))):
        d.rectangle([x0 + 4 + i * 7, y0 + 22, x0 + 8 + i * 7, y0 + 27], fill=c)
    fill("interior", (40, 44, 50))
    x0, y0 = z("cyan")
    for y in range(ZONE):
        for x in range(ZONE):
            img.putpixel((x0 + x, y0 + y), (110, 235, 250, 255))
    fill("base", (66, 68, 70), 0.04)
    # cartel de radiación en los muros laterales
    x0, y0 = fill("sign", (222, 182, 42))
    d.rectangle([x0, y0, x0 + 31, y0 + 31], outline=(30, 30, 30), width=2)
    cx, cy = x0 + 16, y0 + 16
    d.ellipse([cx - 2, cy - 2, cx + 2, cy + 2], fill=(30, 30, 30))
    for a in (90, 210, 330):
        for rad in range(4, 12):
            for da in range(-28, 29, 3):
                px = cx + rad * math.cos(math.radians(a + da)); py = cy + rad * math.sin(math.radians(a + da))
                img.putpixel((int(px), int(py)), (30, 30, 30, 255))
    img.save(A / "textures/block/reactor.png")


# =================================================================== geometría (coordenadas locales en bloques)
FACES = ("front", "back", "left", "right", "up", "down")       # -w, +w, -u, +u, +v, -v


def box(f, t, mat, **over):
    return {"f": f, "t": t, "mats": {k: over.get(k, mat) for k in FACES}}


def wall_with_openings(axis, fixed0, fixed1, s0, s1, v0, v1, holes, outer, inner_face, outer_face, edge="frame"):
    """Muro (losa) a lo largo de `axis` ('u' o 'w') de s0..s1 x v0..v1, espesor fixed0..fixed1, con huecos
    rectangulares (s, v). Se trocea en celdas que no caen en ningún hueco."""
    ss = sorted({s0, s1, *[h[0] for h in holes], *[h[1] for h in holes]})
    vs = sorted({v0, v1, *[h[2] for h in holes], *[h[3] for h in holes]})
    out = []
    for a in range(len(ss) - 1):
        for b in range(len(vs) - 1):
            cs, ce, cv, cve = ss[a], ss[a + 1], vs[b], vs[b + 1]
            if any(h[0] <= cs and ce <= h[1] and h[2] <= cv and cve <= h[3] for h in holes):
                continue
            if axis == "u":
                bx = box([cs, cv, fixed0], [ce, cve, fixed1], edge, **{outer_face: outer, inner_face: "interior"})
            else:
                bx = box([fixed0, cv, cs], [fixed1, cve, ce], edge, **{outer_face: outer, inner_face: "interior"})
            out.append(bx)
    return out


def geometry():
    g = []
    g.append(box([0, 0, 0], [9, 0.5, 9], "base"))                                          # zócalo
    g.append(box([0.12, 0.5, 0.12], [8.88, 0.9, 8.88], "hazard", up="frame"))              # franja de peligro corrida
    g.append(box([1, 0.9, 1], [8, 1.0, 8], "interior"))                                    # suelo interior
    for (u0, w0) in ((0, 0), (8, 0), (0, 8), (8, 8)):                                      # columnas de las esquinas
        g.append(box([u0, 0.9, w0], [u0 + 1, 5.0, w0 + 1], "frame"))
    # muros: frente (escotilla y dos ventanas), fondo y lados (ventanal central y cartel)
    front_holes = [(3.5, 5.5, 0.9, 3.4), (1.3, 3.0, 2.2, 4.1), (6.0, 7.7, 2.2, 4.1)]
    g += wall_with_openings("u", 0.3, 0.7, 1, 8, 0.9, 5.0, front_holes, "wall", "back", "front")
    g += wall_with_openings("u", 8.3, 8.7, 1, 8, 0.9, 5.0, [(3, 6, 1.8, 4.2)], "wall", "front", "back")
    g += wall_with_openings("w", 0.3, 0.7, 1, 8, 0.9, 5.0, [(3, 6, 1.8, 4.2)], "wall", "right", "left")
    g += wall_with_openings("w", 8.3, 8.7, 1, 8, 0.9, 5.0, [(3, 6, 1.8, 4.2)], "wall", "left", "right")
    # escotilla (puerta grande) y ventanas del frente
    g.append(box([3.5, 0.9, 0.42], [5.5, 3.4, 0.6], "frame", front="hatch", back="interior"))
    g.append(box([3.35, 3.4, 0.2], [5.65, 3.6, 0.7], "frame"))                              # dintel
    for (a, b) in ((1.3, 3.0), (6.0, 7.7)):
        g.append(box([a, 2.2, 0.48], [b, 4.1, 0.52], "glass"))
        g.append(box([a - 0.1, 2.05, 0.2], [b + 0.1, 2.2, 0.7], "frame"))                   # alféizar
        g.append(box([a - 0.1, 4.1, 0.2], [b + 0.1, 4.25, 0.7], "frame"))
    g.append(box([1.4, 1.15, 0.18], [2.9, 1.95, 0.3], "frame", front="screen"))             # panel de control
    # ventanales del fondo y los lados (con marco)
    for side in ("back", "left", "right"):
        if side == "back":
            g.append(box([3, 1.8, 8.48], [6, 4.2, 8.52], "glass"))
            g.append(box([2.9, 1.65, 8.3], [6.1, 1.8, 8.8], "frame")); g.append(box([2.9, 4.2, 8.3], [6.1, 4.35, 8.8], "frame"))
        else:
            u = 0.48 if side == "left" else 8.48
            g.append(box([u, 1.8, 3], [u + 0.04, 4.2, 6], "glass"))
            u0, u1 = (0.2, 0.7) if side == "left" else (8.3, 8.8)
            g.append(box([u0, 1.65, 2.9], [u1, 1.8, 6.1], "frame")); g.append(box([u0, 4.2, 2.9], [u1, 4.35, 6.1], "frame"))
    # carteles de radiación en los lados, junto a los ventanales
    g.append(box([0.24, 2.3, 1.4], [0.3, 3.7, 2.6], "sign", left="sign"))
    g.append(box([8.7, 2.3, 6.4], [8.76, 3.7, 7.6], "sign", right="sign"))
    # tuberías de cobre por los costados y bajando por detrás
    for u0 in (0.05, 8.7):
        g.append(box([u0, 3.0, 1.0], [u0 + 0.25, 3.25, 8.0], "copper"))
        g.append(box([u0, 1.3, 1.0], [u0 + 0.25, 1.55, 8.0], "copper"))
    for u0 in (2.0, 6.75):
        g.append(box([u0, 0.9, 8.75], [u0 + 0.25, 5.0, 9.0], "copper"))
    # luces de servicio cian bajo el tejado
    g.append(box([1, 4.75, 0.24], [8, 4.85, 0.3], "cyan"))
    g.append(box([1, 4.75, 8.7], [8, 4.85, 8.76], "cyan"))
    # vasija de vidrio (las barras las dibuja el renderizador) con aros y base
    g.append(box([3, 1.0, 3], [6, 1.8, 6], "frame"))                                        # base de la vasija
    g.append(box([3, 1.8, 3], [6, 2.0, 6], "frame", up="interior"))
    g.append(box([3.05, 2.0, 3.05], [5.95, 5.0, 5.95], "vessel"))
    for vv in (2.0, 3.45, 4.8):
        g.append(box([2.95, vv, 2.95], [6.05, vv + 0.15, 6.05], "frame"))
    # tejado con claraboya sobre la vasija, cúpula y respiraderos
    g += [box([0, 5.0, 0], [9, 5.35, 3], "frame", up="roof"), box([0, 5.0, 6], [9, 5.35, 9], "frame", up="roof"),
          box([0, 5.0, 3], [3, 5.35, 6], "frame", up="roof"), box([6, 5.0, 3], [9, 5.35, 6], "frame", up="roof")]
    g.append(box([2.6, 5.35, 2.6], [6.4, 5.9, 3.0], "frame")); g.append(box([2.6, 5.35, 6.0], [6.4, 5.9, 6.4], "frame"))
    g.append(box([2.6, 5.35, 3.0], [3.0, 5.9, 6.0], "frame")); g.append(box([6.0, 5.35, 3.0], [6.4, 5.9, 6.0], "frame"))
    g.append(box([3.0, 5.7, 3.0], [6.0, 5.75, 6.0], "glass"))                               # claraboya
    for (u0, w0) in ((0.4, 0.4), (7.1, 0.4), (0.4, 7.1), (7.1, 7.1)):
        g.append(box([u0, 5.35, w0], [u0 + 1.5, 5.95, w0 + 1.5], "frame", up="vent"))
    g.append(box([0, 5.35, 0], [9, 5.45, 0.25], "hazard"))                                  # borde de peligro del tejado
    return g


# =================================================================== trocear y escribir modelos
def segment_elements(geom, i, j, k):
    cx, cy, cz = 3 * i + 1, 3 * j + 1, 3 * k + 1
    lo, hi = (3 * i, 3 * j, 3 * k), (3 * i + 3, 3 * j + 3, 3 * k + 3)
    out = []
    for b in geom:
        f = [max(b["f"][a], lo[a]) for a in range(3)]
        t = [min(b["t"][a], hi[a]) for a in range(3)]
        if any(t[a] - f[a] <= 1e-6 for a in range(3)):
            continue
        # cortar por bloques
        cuts = []
        for a in range(3):
            pts = [f[a]] + [x for x in range(math.floor(f[a]) + 1, math.ceil(t[a])) if f[a] < x < t[a]] + [t[a]]
            cuts.append(pts)
        for a0, a1 in zip(cuts[0], cuts[0][1:]):
            for b0, b1 in zip(cuts[1], cuts[1][1:]):
                for c0, c1 in zip(cuts[2], cuts[2][1:]):
                    e = element(b, (a0, b0, c0), (a1, b1, c1), cx, cy, cz)
                    if e:
                        out.append(e)
    return out


def element(b, pf, pt, cx, cy, cz):
    """Pieza local (u, v, w) -> elemento del modelo del trozo (X = 16*(1 + cx - u), Y = 16*(v - cy), Z = 16*(w - cz))."""
    X0, X1 = 16 * (1 + cx - pt[0]), 16 * (1 + cx - pf[0])
    Y0, Y1 = 16 * (pf[1] - cy), 16 * (pt[1] - cy)
    Z0, Z1 = 16 * (pf[2] - cz), 16 * (pt[2] - cz)
    # cara local -> (cara del modelo, ¿está en el borde de la caja original?)
    on = {"front": abs(pf[2] - b["f"][2]) < 1e-6, "back": abs(pt[2] - b["t"][2]) < 1e-6,
          "left": abs(pf[0] - b["f"][0]) < 1e-6, "right": abs(pt[0] - b["t"][0]) < 1e-6,
          "up": abs(pt[1] - b["t"][1]) < 1e-6, "down": abs(pf[1] - b["f"][1]) < 1e-6}
    mface = {"front": "north", "back": "south", "left": "east", "right": "west", "up": "up", "down": "down"}
    faces = {}
    for lf, mf in mface.items():
        if not on[lf] or b["mats"][lf] is None:
            continue
        faces[mf] = {"uv": face_uv(mf, b, b["mats"][lf], (X0, Y0, Z0), (X1, Y1, Z1), cx, cy, cz), "texture": "#0"}
    if not faces:
        return None
    r4 = lambda v: round(v, 4)
    return {"from": [r4(X0), r4(Y0), r4(Z0)], "to": [r4(X1), r4(Y1), r4(Z1)], "faces": faces, "shade": True}


def face_uv(mf, b, mat, mn, mx, cx, cy, cz):
    """UV de una cara: misma regla que Minecraft (north: u = -x, v = -y; south: u = x...) pero con coordenadas del
    reactor entero (sin costuras entre trozos) o relativas a la caja (materiales anclados)."""
    col, row, period, anchored = MATS[mat]
    # coordenadas "de mundo" del modelo: X crece al revés que u
    off = (-16 * (1 + cx), 16 * cy, 16 * cz)
    wmn = [mn[0] + off[0], mn[1] + off[1], mn[2] + off[2]]
    wmx = [mx[0] + off[0], mx[1] + off[1], mx[2] + off[2]]
    rules = {"north": ((0, -1), (1, -1)), "south": ((0, 1), (1, -1)), "east": ((2, -1), (1, -1)),
             "west": ((2, 1), (1, -1)), "up": ((0, 1), (2, 1)), "down": ((0, 1), (2, -1))}
    res = []
    for (axis, sign) in rules[mf]:
        a, c = sign * wmn[axis], sign * wmx[axis]
        lo_, hi_ = min(a, c), max(a, c)
        if anchored:
            bmn = 16 * (b["f"][[0, 1, 2][axis]]) if axis != 0 else -16 * b["t"][0]
            bmx = 16 * (b["t"][axis]) if axis != 0 else -16 * b["f"][0]
            ba, bc = sign * bmn, sign * bmx
            base, length = min(ba, bc), abs(bc - ba)
        else:
            length = 16 * period
            base = math.floor((lo_ + 1e-6) / length) * length
        res.append(((lo_ - base) / length, (hi_ - base) / length))
    (u0, u1), (v0, v1) = res
    zu, zv = col * 4, row * 4
    clamp = lambda x: max(0.0, min(1.0, x))
    return [round(zu + 4 * clamp(u0), 4), round(zv + 4 * clamp(v0), 4), round(zu + 4 * clamp(u1), 4), round(zv + 4 * clamp(v1), 4)]


def jw(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=1) + "\n", encoding="utf-8")


def main():
    paint_atlas()
    geom = geometry()
    total = 0
    for j in range(2):
        for k in range(3):
            for i in range(3):
                n = 1 + i + 3 * k + 9 * j
                els = segment_elements(geom, i, j, k)
                total += len(els)
                model = {"parent": "minecraft:block/block", "render_type": "minecraft:translucent", "ambientocclusion": False,
                         "textures": {"0": "tecnocinetico:block/reactor", "particle": "tecnocinetico:block/reactor"},
                         "elements": els}
                jw(A / f"models/block/reactor_shell_{n}.json", model)
                if n == 5:                                                      # el trozo del núcleo
                    jw(A / "models/block/fission_reactor.json", model)
                    jw(A / "models/block/fission_reactor_on.json", model)
    jw(A / "models/block/reactor_shell_empty.json", {"textures": {"particle": "tecnocinetico:block/reactor"}})
    variants = {}
    for facing, rot in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for seg in range(19):
            v = {"model": f"tecnocinetico:block/reactor_shell_{seg}" if seg else "tecnocinetico:block/reactor_shell_empty"}
            if rot:
                v["y"] = rot
            variants[f"facing={facing},segment={seg}"] = v
    jw(A / "blockstates/reactor_shell.json", {"variants": variants})
    print(f"ok: reactor en 18 trozos, {total} elementos")


if __name__ == "__main__":
    main()
