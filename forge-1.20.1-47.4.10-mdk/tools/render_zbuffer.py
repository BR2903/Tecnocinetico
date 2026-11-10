"""Vista previa con búfer de profundidad (para modelos grandes donde render_model.py se confunde).

python tools/render_zbuffer.py <salida.png> <modelo.json> [yaw pitch escala]
Ortográfica, sin rotaciones de elemento, texturas con transparencia (alfa < 0.5 deja ver lo de detrás).
"""
import json
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets"


def tex_path(ref, textures):
    while ref.startswith("#"):
        ref = textures[ref[1:]]
    ns, path = ref.split(":") if ":" in ref else ("minecraft", ref)
    return ASSETS / ns / "textures" / f"{path}.png"


def corners(f, t, face):
    (x0, y0, z0), (x1, y1, z1) = f, t
    return {"north": [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
            "south": [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
            "east": [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
            "west": [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
            "up": [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
            "down": [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)]}[face]


SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6}


def render(model, yaw=35, pitch=25, scale=4, size=900):
    texs = {}
    W = H = size
    img = np.zeros((H, W, 3)) + 0.25
    depth = np.full((H, W), -1e9)
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    els = model["elements"]
    allp = np.array([p for e in els for p in (e["from"], e["to"])], dtype=float)
    center = (allp.min(0) + allp.max(0)) / 2

    def proj(p):
        x, y, z = p[0] - center[0], p[1] - center[1], p[2] - center[2]
        x, z = x * cy - z * sy, x * sy + z * cy
        y, z = y * cp - z * sp, y * sp + z * cp
        return W / 2 + x * scale, H / 2 - y * scale, z

    for e in els:
        for face, fd in e["faces"].items():
            ref = fd["texture"]
            path = tex_path(ref, model["textures"])
            if path not in texs:
                texs[path] = np.asarray(Image.open(path).convert("RGBA"), dtype=float) / 255
            tx = texs[path]
            th, tw = tx.shape[:2]
            u0, v0, u1, v1 = fd.get("uv", [0, 0, 16, 16])
            q = [proj(p) for p in corners(e["from"], e["to"], face)]
            # rasterizar el cuadrilátero como dos triángulos con coordenadas (s, t) bilineales
            uvq = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
            for tri in ((0, 1, 2), (0, 2, 3)):
                P = [q[i] for i in tri]
                UV = [uvq[i] for i in tri]
                xs, ys = [p[0] for p in P], [p[1] for p in P]
                minx, maxx = max(0, int(min(xs))), min(W - 1, int(max(xs)) + 1)
                miny, maxy = max(0, int(min(ys))), min(H - 1, int(max(ys)) + 1)
                if minx > maxx or miny > maxy:
                    continue
                (x1, y1, z1), (x2, y2, z2), (x3, y3, z3) = P
                den = (y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3)
                if abs(den) < 1e-9:
                    continue
                gx, gy = np.meshgrid(np.arange(minx, maxx + 1) + 0.5, np.arange(miny, maxy + 1) + 0.5)
                a = ((y2 - y3) * (gx - x3) + (x3 - x2) * (gy - y3)) / den
                b = ((y3 - y1) * (gx - x3) + (x1 - x3) * (gy - y3)) / den
                c = 1 - a - b
                inside = (a >= -1e-6) & (b >= -1e-6) & (c >= -1e-6)
                if not inside.any():
                    continue
                zz = a * z1 + b * z2 + c * z3
                uu = a * UV[0][0] + b * UV[1][0] + c * UV[2][0]
                vv = a * UV[0][1] + b * UV[1][1] + c * UV[2][1]
                px = np.clip((uu / 16 * tw).astype(int), 0, tw - 1)
                py = np.clip((vv / 16 * th).astype(int), 0, th - 1)
                col = tx[py, px]
                sub = depth[miny:maxy + 1, minx:maxx + 1]
                ok = inside & (col[..., 3] > 0.5) & (zz > sub)
                sub[ok] = zz[ok]
                img[miny:maxy + 1, minx:maxx + 1][ok] = col[..., :3][ok] * SHADE[face]
    return Image.fromarray((img * 255).astype(np.uint8))


if __name__ == "__main__":
    out, mp = sys.argv[1], sys.argv[2]
    yaw = float(sys.argv[3]) if len(sys.argv) > 3 else 35
    pitch = float(sys.argv[4]) if len(sys.argv) > 4 else 25
    scale = float(sys.argv[5]) if len(sys.argv) > 5 else 4
    render(json.loads(Path(mp).read_text(encoding="utf-8")), yaw, pitch, scale).save(out)
    print("ok", out)
