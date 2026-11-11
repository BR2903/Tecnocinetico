"""Previsualizaciones recortadas de los volcados reales de GameTest; interiores sin techo y ciudad sin agua."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import render_dump

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "build/overworld_previews"
SITES = [
    ("relay_antenna", "Antena repetidora", 34),
    ("data_center", "Centro de datos (sin techo)", 11),
    ("survivor_bunker", "Búnker (sin techo)", 9),
    ("swarm_laboratory", "Laboratorio del Enjambre (sin techo)", 15),
    ("sunken_city", "Ciudad sumergida (agua oculta)", 21),
]


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    render_dump.COLORS.update({"core_console": (45, 190, 205), "red_wool": (155, 40, 35),
                              "copper_block": (178, 103, 70), "white_concrete": (200, 204, 207)})
    tiles = []
    for name, title, ceiling in SITES:
        source = ROOT / f"run/gametest_dump/{name}.txt"
        if not source.exists(): continue
        rows = []
        xmax, zmax = (27, 32) if name == "survivor_bunker" else (34, 42)
        xmin, zmin = (3, 3) if name == "survivor_bunker" else (2, 2)
        for line in source.read_text(encoding="utf-8").splitlines():
            x, y, z, block = line.split()
            x, y, z = int(x), int(y), int(z)
            if not (xmin <= x <= xmax and zmin <= z <= zmax and 6 <= y <= ceiling): continue
            if block in {"minecraft:water", "minecraft:kelp", "minecraft:kelp_plant", "minecraft:seagrass", "minecraft:tall_seagrass"}: continue
            # El y original del dump parte de la base de la pieza - 2 (suelo local = y6).
            rows.append(f"{x-2} {y-6} {z-2} {block}")
        cropped = OUT / f"{name}.txt"
        cropped.write_text("\n".join(rows), encoding="utf-8")
        picture = render_dump.render(cropped)
        picture.thumbnail((620, 540))
        tile = Image.new("RGB", (650, 600), (40, 40, 44))
        tile.paste(picture, ((650-picture.width)//2, 45+(540-picture.height)//2))
        d = ImageDraw.Draw(tile)
        try: font = ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 23)
        except OSError: font = ImageFont.load_default()
        d.text((18, 10), title, font=font, fill=(225, 231, 234))
        tile.save(OUT / f"{name}.png")
        tiles.append(tile)
    sheet = Image.new("RGB", (650*3, 600*2), (40, 40, 44))
    for i, tile in enumerate(tiles): sheet.paste(tile, ((i % 3)*650, (i//3)*600))
    sheet.save(OUT / "estructuras.png")
    print(OUT / "estructuras.png")


if __name__ == "__main__":
    main()
