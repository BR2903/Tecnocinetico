"""Busca parpadeo de texturas (z-fighting) en modelos de objetos: dos caras en el mismo plano, mirando al mismo
lado y que se solapan (Minecraft dibuja las dos y el color "titila" al mover la cámara).

python tools/check_zfight.py [modelo ...]     (sin argumentos: todos los modelos de models/item con elementos)
Solo compara elementos con la misma rotación (las caras de piezas giradas distinto no pueden coincidir exactamente).
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tecnocinetico/models/item"
EPS = 1e-4
# cara -> (eje, lado: 0 = from, 1 = to)
FACES = {"west": (0, 0), "east": (0, 1), "down": (1, 0), "up": (1, 1), "north": (2, 0), "south": (2, 1)}


def overlap(a0, a1, b0, b1):
    return min(a1, b1) - max(a0, b0)


def check(path):
    data = json.loads(path.read_text(encoding="utf-8"))
    els = data.get("elements") or []
    found = []
    for i, a in enumerate(els):
        for j in range(i + 1, len(els)):
            b = els[j]
            if json.dumps(a.get("rotation"), sort_keys=True) != json.dumps(b.get("rotation"), sort_keys=True):
                continue
            for face, (ax, side) in FACES.items():
                if face not in a.get("faces", {}) or face not in b.get("faces", {}):
                    continue
                pa = (a["from"], a["to"])[side][ax]
                pb = (b["from"], b["to"])[side][ax]
                if abs(pa - pb) > EPS:
                    continue
                o = [overlap(a["from"][k], a["to"][k], b["from"][k], b["to"][k]) for k in range(3) if k != ax]
                if all(v > EPS for v in o):
                    found.append((i, j, face, round(o[0] * o[1], 3)))
    return found


def main():
    names = sys.argv[1:] or sorted(p.stem for p in ROOT.glob("*.json"))
    bad = 0
    for n in names:
        res = check(ROOT / f"{n}.json")
        if res:
            bad += 1
            print(f"{n}: " + ", ".join(f"#{i}/#{j} {f} ({area})" for i, j, f, area in res))
    print("ok: sin caras coplanares" if not bad else f"{bad} modelos con caras coplanares")


if __name__ == "__main__":
    main()
