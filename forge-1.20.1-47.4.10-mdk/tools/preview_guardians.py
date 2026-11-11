"""Comparativa de los guardianes rediseñados y sus piezas exclusivas; no abre el cliente."""
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageChops
from render_zbuffer import render

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "build/guardian_previews"


def main():
    sheet = Image.new("RGB", (1500, 650), (35, 39, 45))
    d = ImageDraw.Draw(sheet)
    try:
        title = ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 25)
        small = ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 18)
    except OSError:
        title = small = ImageFont.load_default()
    panels = [
        ("network_watcher", 205, "Vigía de la Red · 700 PV", "Propulsor Vectorial", "vector_thruster"),
        ("network_watcher", 25, "Dos condensadores traseros", "Cada uno: 24 daño → 3 s de aturdimiento", None),
        ("swarm_matriarch", 205, "Matriarca · 1.000 PV", "Núcleo de Vuelo Adaptativo", "adaptive_flight_core"),
    ]
    for i, (name, yaw, heading, caption, part) in enumerate(panels):
        model = json.loads((OUT / f"{name}.json").read_text(encoding="utf-8"))
        picture = render(model, yaw, 20, 10)
        bounds = ImageChops.difference(picture, Image.new("RGB", picture.size, (63,63,63))).getbbox()
        if bounds: picture = picture.crop(bounds)
        picture.thumbnail((470, 400))
        x = i * 500
        sheet.paste(picture, (x+(500-picture.width)//2, 65+(410-picture.height)//2))
        d.text((x+18, 18), heading, font=title, fill="#dce7e9")
        if part:
            icon = Image.open(ROOT / f"src/main/resources/assets/tecnocinetico/textures/item/{part}.png").resize((80,80), Image.Resampling.NEAREST)
            sheet.paste(icon, (x+205, 490), icon)
        d.text((x+15, 580), caption, font=small, fill="#c3d7ce")
        if i == 2: d.text((x+15, 611), "Sin puntos débiles ni exposición", font=small, fill="#b5c3bc")
    sheet.save(OUT / "guardianes_redisenados.png")
    print(OUT / "guardianes_redisenados.png")


if __name__ == "__main__":
    main()
