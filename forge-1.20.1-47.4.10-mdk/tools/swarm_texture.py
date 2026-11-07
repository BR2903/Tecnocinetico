"""Genera swarm_body.png y swarm_body_glow.png (64x64) siguiendo el mapa UV de SwarmBodyModel."""
import random
import sys
from pathlib import Path
from PIL import Image

out_dir = Path(sys.argv[1])
random.seed(1337)

W = H = 64
tex = Image.new("RGBA", (W, H), (0, 0, 0, 0))
glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
px = tex.load()
gx = glow.load()


def clamp(c):
    return max(0, min(255, int(c)))


def shade(rgb, k):
    return tuple(clamp(c * k) for c in rgb) + (255,)


def noisy(rgb, amount=0.12):
    return shade(rgb, 1.0 + random.uniform(-amount, amount))


def faces(u, v, w, h, d):
    """Devuelve los rectángulos (x, y, ancho, alto) de cada cara del cubo con el layout UV de Minecraft."""
    return {
        "up": (u + d, v, w, d),
        "down": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + 2 * d + w, v + d, w, h),
    }


def fill(rect, painter):
    x0, y0, fw, fh = rect
    for y in range(fh):
        for x in range(fw):
            px[x0 + x, y0 + y] = painter(x, y, fw, fh)


# --- Materiales ---
FLESH = (88, 34, 52)        # carne corrompida
FLESH_DARK = (52, 20, 34)
VEIN = (160, 40, 60)
METAL = (78, 86, 102)       # Inercita
METAL_DARK = (44, 48, 60)
METAL_LIGHT = (130, 140, 156)
STEEL = (196, 205, 214)
CHITIN = (40, 30, 40)
CYAN = (60, 220, 240)


def flesh(x, y, fw, fh):
    base = FLESH if (x * 7 + y * 3) % 11 else FLESH_DARK
    if random.random() < 0.08:
        return noisy(VEIN, 0.15)
    return noisy(base, 0.15)


def metal(x, y, fw, fh):
    if x == 0 or y == 0:
        return noisy(METAL_LIGHT, 0.05)          # bisel iluminado
    if x == fw - 1 or y == fh - 1:
        return noisy(METAL_DARK, 0.05)           # bisel en sombra
    if (x % 4 == 2 and y % 4 == 2):
        return shade(METAL_LIGHT, 1.1)           # remaches
    return noisy(METAL, 0.08)


def plates(x, y, fw, fh):
    if x % 4 == 0:
        return noisy(METAL_DARK, 0.05)           # juntas entre placas
    return metal(x, y, fw, fh)


def chitin(x, y, fw, fh):
    return noisy(CHITIN if y % 3 else FLESH_DARK, 0.15)


def steel(x, y, fw, fh):
    return noisy(STEEL if x != fw - 1 else METAL_LIGHT, 0.05)


def paint_box(u, v, w, h, d, painter, overrides=None):
    for name, rect in faces(u, v, w, h, d).items():
        fill(rect, (overrides or {}).get(name, painter))


# Torso (0,0) 10x7x14: carne, con una banda metálica en el lomo
def torso_top(x, y, fw, fh):
    return metal(x, y, fw, fh) if 3 <= x <= 6 else flesh(x, y, fw, fh)


paint_box(0, 0, 10, 7, 14, flesh, {"up": torso_top})

# Núcleo de Cinetrita (48,0) 5x5x2: cian brillante con centro casi blanco
def core(x, y, fw, fh):
    cx, cy = (fw - 1) / 2, (fh - 1) / 2
    dist = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5 / max(cx, cy, 1)
    k = 1.0 - 0.45 * dist
    return (clamp(120 + 135 * k), clamp(230 + 25 * k), 255, 255)


paint_box(48, 0, 5, 5, 2, core)

# Barrotes (48,8) 1x6x1
paint_box(48, 8, 1, 6, 1, steel)

# Placas dorsales (0,21) 11x2x12
paint_box(0, 21, 11, 2, 12, plates)

# Cabeza (0,35) 6x5x5: máscara metálica con ojos
def head_front(x, y, fw, fh):
    if y == 1 and x in (0, 1, 4, 5):
        return (90, 240, 255, 255)               # ojos (4)
    if y == 3 and 1 <= x <= 4:
        return noisy(VEIN, 0.1)                  # ranura de la boca
    return metal(x, y, fw, fh)


paint_box(0, 35, 6, 5, 5, metal, {"front": head_front})

# Patas delanteras (22,35) 2x12x2 + cuchilla (30,35) 1x3x5
def front_leg(x, y, fw, fh):
    if y in (4, 5):
        return noisy(FLESH, 0.1)                 # articulación de carne
    return metal(x, y, fw, fh)


paint_box(22, 35, 2, 12, 2, front_leg)
paint_box(30, 35, 1, 3, 5, steel)

# Patas traseras: muslo (42,35) 3x6x3 de carne con banda metálica, pie (54,35) 2x5x2
def thigh(x, y, fw, fh):
    return metal(x, y, fw, fh) if y in (2, 3) else flesh(x, y, fw, fh)


paint_box(42, 35, 3, 6, 3, thigh)
paint_box(54, 35, 2, 5, 2, metal)

# Patas medias (0,45) 1x12x1
paint_box(0, 45, 1, 12, 1, chitin)

# --- Capa luminosa: núcleo y ojos ---
for name, (x0, y0, fw, fh) in faces(48, 0, 5, 5, 2).items():
    for y in range(fh):
        for x in range(fw):
            r, g, b, _ = px[x0 + x, y0 + y]
            gx[x0 + x, y0 + y] = (r, g, b, 255)
fx, fy, _, _ = faces(0, 35, 6, 5, 5)["front"]
for ex in (0, 1, 4, 5):
    gx[fx + ex, fy + 1] = (90, 240, 255, 255)

out_dir.mkdir(parents=True, exist_ok=True)
tex.save(out_dir / "swarm_body.png")
glow.save(out_dir / "swarm_body_glow.png")
# Vista previa ampliada para revisarla
tex.resize((512, 512), Image.NEAREST).save(out_dir.parent.parent.parent.parent.parent.parent.parent / "swarm_body_preview.png") if False else None
print("texturas generadas en", out_dir)
