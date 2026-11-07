"""Renderizador ortográfico mínimo de modelos JSON de Minecraft (para previsualizar sin abrir el juego).
python render_model.py <assets> <out.png> <modelo1> [modelo2 ...]   (modelo = nombre en models/item sin .json)
"""
import json
import math
import sys
from pathlib import Path
from PIL import Image, ImageDraw

ASSETS = Path(sys.argv[1]) if len(sys.argv) > 1 else None
S = 34  # píxeles por unidad
BG = (64, 66, 72, 255)


def load(name):
    return json.loads((ASSETS / "models/item" / f"{name}.json").read_text(encoding="utf-8"))


def corners(el, face):
    (fx, fy, fz), (tx, ty, tz) = el["from"], el["to"]
    return {
        "north": [(tx, ty, fz), (fx, ty, fz), (fx, fy, fz), (tx, fy, fz)],
        "south": [(fx, ty, tz), (tx, ty, tz), (tx, fy, tz), (fx, fy, tz)],
        "east": [(tx, ty, tz), (tx, ty, fz), (tx, fy, fz), (tx, fy, tz)],
        "west": [(fx, ty, fz), (fx, ty, tz), (fx, fy, tz), (fx, fy, fz)],
        "up": [(fx, ty, fz), (tx, ty, fz), (tx, ty, tz), (fx, ty, tz)],
        "down": [(fx, fy, tz), (tx, fy, tz), (tx, fy, fz), (fx, fy, fz)],
    }[face]


def rot_el(p, el):
    r = el.get("rotation")
    if not r or not r["angle"]:
        return p
    a = math.radians(r["angle"])
    o = r["origin"]
    x, y, z = p[0] - o[0], p[1] - o[1], p[2] - o[2]
    c, s = math.cos(a), math.sin(a)
    if r["axis"] == "x":
        y, z = y * c - z * s, y * s + z * c
    elif r["axis"] == "y":
        x, z = x * c + z * s, -x * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    return (x + o[0], y + o[1], z + o[2])


def unrot_el(p, el):
    r = el.get("rotation")
    if not r or not r["angle"]:
        return p
    inv = dict(el)
    inv["rotation"] = dict(r, angle=-r["angle"])
    return rot_el(p, inv)


def hidden(el, face, elements):
    """La cara está dentro de otra pieza (p. ej. el cañón dentro de la corredera)."""
    pts = [rot_el(p, el) for p in corners(el, face)]
    c = tuple(sum(p[k] for p in pts) / 4 for k in range(3))
    for other in elements:
        if other is el:
            continue
        q = unrot_el(c, other)
        if all(other["from"][k] + 1e-3 < q[k] < other["to"][k] - 1e-3 for k in range(3)):
            return True
    return False


def view(p, yaw, pitch):
    x, y, z = p[0] - 8, p[1] - 8, p[2] - 8
    c, s = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    x, z = x * c + z * s, -x * s + z * c
    c, s = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    y, z = y * c - z * s, y * s + z * c
    return x, y, z


def gui_view(model):
    """Proyección igual a la del inventario: aplica display.gui y mira desde +z."""
    g = model.get("display", {}).get("gui", {})
    rx, ry, rz = g.get("rotation", [0, 0, 0])
    t = g.get("translation", [0, 0, 0])
    s = g.get("scale", [1, 1, 1])

    def f(p, yaw=None, pitch=None):
        x, y, z = p[0] - 8, p[1] - 8, p[2] - 8
        x, y, z = x * s[0], y * s[1], z * s[2]
        a = math.radians(rz); x, y = x * math.cos(a) - y * math.sin(a), x * math.sin(a) + y * math.cos(a)
        a = math.radians(ry); x, z = x * math.cos(a) + z * math.sin(a), -x * math.sin(a) + z * math.cos(a)
        a = math.radians(rx); y, z = y * math.cos(a) - z * math.sin(a), y * math.sin(a) + z * math.cos(a)
        return x + t[0], y + t[1], -(z + t[2])
    return f


def render(model, yaw, pitch, size=560, projector=None):
    global view
    proj = projector or view
    tex_ref = model["textures"]["0"].split(":")[1]
    tex = Image.open(ASSETS / "textures" / f"{tex_ref}.png").convert("RGBA")
    tw, th = tex.size
    img = Image.new("RGBA", (size, size), BG)
    draw = ImageDraw.Draw(img)
    quads = []
    for el in model["elements"]:
        for face, fd in el["faces"].items():
            if hidden(el, face, model["elements"]):
                continue
            pts = [proj(rot_el(p, el), yaw, pitch) for p in corners(el, face)]
            # normal para descartar caras traseras
            ax, ay = pts[1][0] - pts[0][0], pts[1][1] - pts[0][1]
            bx, by = pts[3][0] - pts[0][0], pts[3][1] - pts[0][1]
            if ax * by - ay * bx > 0:
                continue
            depth = sum(p[2] for p in pts) / 4
            quads.append((depth, pts, fd["uv"]))
    shade = {"up": 1.0}
    for depth, pts, uv in sorted(quads, key=lambda q: -q[0]):
        u0, v0, u1, v1 = [c * tw / 16 for c in uv]
        w, h = max(1, round(abs(u1 - u0))), max(1, round(abs(v1 - v0)))
        sub = tex.crop((min(u0, u1), min(v0, v1), min(u0, u1) + w, min(v0, v1) + h))
        def at(fu, fv):
            top = [pts[0][k] + (pts[1][k] - pts[0][k]) * fu for k in range(2)]
            bot = [pts[3][k] + (pts[2][k] - pts[3][k]) * fu for k in range(2)]
            x = top[0] + (bot[0] - top[0]) * fv
            y = top[1] + (bot[1] - top[1]) * fv
            return (size / 2 + x * S, size / 2 - y * S)
        for j in range(h):
            for i in range(w):
                c = sub.getpixel((i, j))
                if c[3] == 0:
                    continue
                poly = [at(i / w, j / h), at((i + 1) / w, j / h), at((i + 1) / w, (j + 1) / h), at(i / w, (j + 1) / h)]
                draw.polygon(poly, fill=c)
    return img


def main():
    OUT = Path(sys.argv[2])
    NAMES = sys.argv[3:]
    views = [(90, 0), (35, 20), (-90, 0)]
    rows = []
    for name in NAMES:
        if name.endswith("@gui"):
            # Vista de inventario: casilla de 16x16 dibujada con fondo de slot y su borde
            m = load(name[:-4])
            im = render(m, 0, 0, size=560, projector=gui_view(m))
            d = ImageDraw.Draw(im)
            half = 8 * S
            d.rectangle([280 - half, 280 - half, 280 + half, 280 + half], outline=(200, 200, 200, 255), width=2)
            rows.append([im])
            continue
        m = load(name)
        rows.append([render(m, y, p) for y, p in views])
    cols = max(len(r) for r in rows)
    views = [None] * cols
    sheet = Image.new("RGBA", (560 * len(views), 560 * len(rows)), (0, 0, 0, 255))
    for r, row in enumerate(rows):
        for c, im in enumerate(row):
            sheet.paste(im, (c * 560, r * 560))
    sheet = sheet.resize((sheet.width // 2, sheet.height // 2), Image.LANCZOS)
    sheet.save(OUT)
    print("ok", OUT)


if __name__ == "__main__":
    main()
