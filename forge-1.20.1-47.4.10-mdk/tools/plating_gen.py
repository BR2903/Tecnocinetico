"""Chapa de Aleación Antigua (textures/block/ancient_alloy_plating.png, 128 px): 4 placas de acero oscuro con bisel,
remaches en las esquinas, arañazos y óxido en los bordes. Sin símbolos (el mod es tecnológico, no mágico).
La usan el Bloque de Aleación Antigua y los laterales del Ensamblador de Armas y de las máquinas eléctricas.

python tools/plating_gen.py
"""
import math
import random
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tecnocinetico/textures/block/ancient_alloy_plating.png"
S, P = 128, 64                                       # textura y tamaño de cada placa


def smooth_noise(r, cells):
    """Ruido de valor suave (sin ruido píxel a píxel): rejilla de cells x cells interpolada."""
    g = [[r.random() for _ in range(cells + 1)] for _ in range(cells + 1)]

    def at(x, y):
        fx, fy = (x % S) / S * cells, (y % S) / S * cells
        i, j = int(fx), int(fy)
        tx, ty = fx - i, fy - j
        tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
        a = g[j][i] + (g[j][i + 1] - g[j][i]) * tx
        b = g[j + 1][i] + (g[j + 1][i + 1] - g[j + 1][i]) * tx
        return a + (b - a) * ty
    return at


def main():
    r = random.Random(1701)
    n1, n2 = smooth_noise(r, 6), smooth_noise(r, 18)
    img = Image.new("RGBA", (S, S))
    base = (40, 42, 47)
    rust = (92, 52, 30)
    for y in range(S):
        for x in range(S):
            k = 0.9 + 0.16 * n1(x, y) + 0.06 * n2(x, y)
            c = [v * k for v in base]
            px, py = x % P, y % P
            # bisel de cada placa: claro arriba/izquierda, oscuro abajo/derecha, junta negra
            if px in (0, P - 1) or py in (0, P - 1):
                c = [8, 8, 11]
            elif px == 1 or py == 1:
                c = [v * 1.45 for v in c]
            elif px == P - 2 or py == P - 2:
                c = [v * 0.6 for v in c]
            # óxido que nace en las juntas
            edge = min(px, py, P - 1 - px, P - 1 - py)
            if 2 <= edge <= 4 and n2(x * 1.7, y * 1.3) > 0.62:
                t = (n2(x * 1.7, y * 1.3) - 0.62) * 2.2
                c = [c[i] + (rust[i] - c[i]) * min(1, t) for i in range(3)]
            img.putpixel((x, y), tuple(int(max(0, min(255, v))) for v in c) + (255,))
    # arañazos rectos y finos (pocos, sin simetría)
    for _ in range(9):
        x0, y0 = r.randint(6, S - 6), r.randint(6, S - 6)
        a = r.uniform(0, math.pi)
        length = r.randint(6, 16)
        for t in range(length):
            x, y = int(x0 + math.cos(a) * t), int(y0 + math.sin(a) * t)
            if 0 <= x < S and 0 <= y < S and 3 < x % P < P - 4 and 3 < y % P < P - 4:
                p = img.getpixel((x, y))
                img.putpixel((x, y), tuple(min(255, int(v * 1.35)) for v in p[:3]) + (255,))
    # remaches en las esquinas de cada placa
    for (ox, oy) in ((0, 0), (P, 0), (0, P), (P, P)):
        for (cx, cy) in ((6, 6), (P - 7, 6), (6, P - 7), (P - 7, P - 7)):
            for dy in range(-2, 3):
                for dx in range(-2, 3):
                    d = dx * dx + dy * dy
                    if d > 5:
                        continue
                    v = 150 if (dx, dy) in ((-1, -1), (0, -1), (-1, 0)) else 92 if d < 3 else 30
                    img.putpixel((ox + cx + dx, oy + cy + dy), (v, v + 2, v + 6, 255))
            img.putpixel((ox + cx + 1, oy + cy + 2), (74, 42, 26, 255))      # chorrete de óxido bajo el remache
    img.save(OUT)
    print("ok:", OUT.name)


if __name__ == "__main__":
    main()
