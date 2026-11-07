"""Ajusta la vista de inventario ("gui") de modelos 3D para que llenen la casilla, como los mods de armas realistas.

python gui_fit.py <assets/tecnocinetico> <rx,ry,rz> <modelo_ref> [otros modelos que usan el mismo ajuste...]
El ajuste (escala y desplazamiento) se calcula con el primer modelo y se aplica a todos,
para que los fotogramas de animación no cambien de tamaño en la casilla.
"""
import json
import math
import sys
from pathlib import Path

ASSETS = Path(sys.argv[1])
NAMES = sys.argv[3:]
FILL = 15.0  # unidades de las 16 de la casilla (deja 0,5 px de margen por lado)
if NAMES and NAMES[0].startswith("fill="):
    FILL = float(NAMES.pop(0)[5:])


def euler_from_steps(spec):
    """'y-90;z20;x15' = girar en pantalla primero en Y -90, luego Z 20, luego X 15.
    Devuelve los ángulos XYZ equivalentes para el JSON (Minecraft aplica Rx·Ry·Rz)."""
    def rot(axis, deg):
        a = math.radians(deg)
        c, s = math.cos(a), math.sin(a)
        return {"x": [[1, 0, 0], [0, c, -s], [0, s, c]],
                "y": [[c, 0, s], [0, 1, 0], [-s, 0, c]],
                "z": [[c, -s, 0], [s, c, 0], [0, 0, 1]]}[axis]

    def mul(a, b):
        return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
    m = [[1, 0, 0], [0, 1, 0], [0, 0, 1]]
    for step in spec.split(";"):
        m = mul(rot(step[0], float(step[1:])), m)
    b = math.asin(max(-1.0, min(1.0, m[0][2])))
    c = math.atan2(-m[0][1], m[0][0])
    a = math.atan2(-m[1][2], m[2][2])
    return [round(math.degrees(v), 3) for v in (a, b, c)]


ROT = euler_from_steps(sys.argv[2]) if ";" in sys.argv[2] or sys.argv[2][0] in "xyz" \
    else [float(v) for v in sys.argv[2].split(",")]


def rot_el(p, el):
    r = el.get("rotation")
    if not r or not r.get("angle"):
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


def rot_xyz(p, rx, ry, rz):
    """Igual que Quaternionf.rotationXYZ de Minecraft: se aplica Z, luego Y, luego X."""
    x, y, z = p
    a = math.radians(rz)
    x, y = x * math.cos(a) - y * math.sin(a), x * math.sin(a) + y * math.cos(a)
    a = math.radians(ry)
    x, z = x * math.cos(a) + z * math.sin(a), -x * math.sin(a) + z * math.cos(a)
    a = math.radians(rx)
    y, z = y * math.cos(a) - z * math.sin(a), y * math.sin(a) + z * math.cos(a)
    return x, y, z


def path(name):
    return ASSETS / "models/item" / f"{name}.json"


ref = json.loads(path(NAMES[0]).read_text(encoding="utf-8"))
xs, ys = [], []
for el in ref["elements"]:
    f, t = el["from"], el["to"]
    for cx in (f[0], t[0]):
        for cy in (f[1], t[1]):
            for cz in (f[2], t[2]):
                p = rot_el((cx, cy, cz), el)
                q = rot_xyz((p[0] - 8, p[1] - 8, p[2] - 8), *ROT)
                xs.append(q[0])
                ys.append(q[1])
w, h = max(xs) - min(xs), max(ys) - min(ys)
scale = min(4.0, FILL / max(w, h))
mid_x, mid_y = (max(xs) + min(xs)) / 2, (max(ys) + min(ys)) / 2
gui = {"rotation": ROT, "translation": [round(-mid_x * scale, 3), round(-mid_y * scale, 3), 0],
       "scale": [round(scale, 3)] * 3}

for name in NAMES:
    m = json.loads(path(name).read_text(encoding="utf-8"))
    m.setdefault("display", {})["gui"] = gui
    m["gui_light"] = "front"
    path(name).write_text(json.dumps(m, indent=1), encoding="utf-8")
print(f"{NAMES[0]}: tamaño {w:.2f}x{h:.2f} -> escala {scale:.3f}, gui = {gui}")
