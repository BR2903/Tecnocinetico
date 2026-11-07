"""Copia las texturas originales a un respaldo y reescala las que no son potencia de 2 / son enormes."""
import shutil
import sys
from pathlib import Path
from PIL import Image

root = Path(sys.argv[1])          # .../assets/tecnocinetico/textures
backup = Path(sys.argv[2])        # carpeta de respaldo
TARGET = 128

def is_pow2(n):
    return n > 0 and (n & (n - 1)) == 0

for png in sorted(root.rglob("*.png")):
    img = Image.open(png)
    w, h = img.size
    if w <= 16 and h <= 16 and is_pow2(w) and is_pow2(h):
        continue  # ya correcta (pixel art 16x16)
    dest = backup / png.relative_to(root)
    dest.parent.mkdir(parents=True, exist_ok=True)
    if not dest.exists():
        shutil.copy2(png, dest)
    img = img.convert("RGBA")
    if w < 32:
        # Pixel art pequeño (22x22): se centra en un lienzo de 32x32 sin reescalar para no deformar los píxeles
        canvas = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
        canvas.paste(img, ((32 - w) // 2, (32 - h) // 2))
        out = canvas
    else:
        out = img.resize((TARGET, TARGET), Image.LANCZOS)
    out.save(png, optimize=True)
    print(f"{png.name}: {w}x{h} -> {out.size[0]}x{out.size[1]}")
