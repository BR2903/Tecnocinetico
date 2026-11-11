"""Galería de las geometrías finales de los mobs remodelados, con sus variantes y atlas reales."""
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageChops, ImageOps
from render_zbuffer import render

ROOT=Path(__file__).resolve().parent.parent
OUT=ROOT/"build/mob_remodel"
ENTRIES=[
    ("siege_spider","Araña de Asedio"),("drone","Dron del Núcleo"),("sentry_turret","Torreta Centinela"),
    ("anchor","Anclador"),("swarm_body","Cuerpo de Enjambre"),("hive_node","Nodo de Colmena"),
    ("swarm_wasp/scout","Avispa exploradora"),("swarm_wasp/stinger","Avispa aguijón"),("swarm_wasp/guard","Avispa escolta"),
    ("corrupted_spider","Arañas corruptas"),("corrupted_creeper","Creeper Corrupto"),
    ("corrupted/wanderer","Corrupto errante"),("corrupted/worker","Corrupto trabajador"),
    ("corrupted/crystal","Corrupto cristalizado"),("corrupted/raider","Tirador saqueador"),
    ("corrupted/soldier","Tirador soldado"),("corrupted/drowned","Ahogado Corrupto"),
    ("survivor/scrapper","Superviviente chatarrero"),("survivor/gunsmith","Superviviente armero"),("survivor/farmer","Superviviente agricultor"),
]


def main():
    OUT.mkdir(parents=True,exist_ok=True)
    sheet=Image.new("RGB",(1800,1440),(34,39,45))
    try: font=ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf",19)
    except OSError: font=ImageFont.load_default()
    for i,(name,label) in enumerate(ENTRIES):
        model=json.loads((ROOT/f"build/guardian_previews/remodel/{name}.json").read_text(encoding="utf-8"))
        pic=render(model,205,18,9 if "spider" not in name else 7)
        bounds=ImageChops.difference(pic,Image.new("RGB",pic.size,(63,63,63))).getbbox()
        if bounds: pic=ImageOps.expand(pic.crop(bounds),border=8,fill=(63,63,63))
        pic.save(OUT/(name.replace("/","_")+".png"))
        factor=min(340/pic.width,300/pic.height)
        pic=pic.resize((int(pic.width*factor),int(pic.height*factor)),Image.Resampling.LANCZOS)
        tile=Image.new("RGB",(360,360),(34,39,45))
        tile.paste(pic,((360-pic.width)//2,42+(300-pic.height)//2))
        ImageDraw.Draw(tile).text((12,10),label,font=font,fill="#dbe4e5")
        sheet.paste(tile,((i%5)*360,(i//5)*360))
    sheet.save(OUT/"todos_los_mobs.png")
    print(OUT/"todos_los_mobs.png")


if __name__=="__main__": main()
