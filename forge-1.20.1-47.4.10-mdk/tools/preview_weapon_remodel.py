"""Vistas del arsenal con texturas reales, rotaciones JSON y búfer de profundidad."""
import copy
import json
import math
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont,ImageChops,ImageOps
from render_zbuffer import render,corners

ROOT=Path(__file__).resolve().parent.parent
ASSETS=ROOT/"src/main/resources/assets/tecnocinetico"
OUT=ROOT/"build/weapon_remodel"
ENTRIES=[("pipe_shotgun","Escopeta de caño"),("bolt_rifle","Rifle de cerrojo"),
         ("revolver","Revólver"),("scrap_machete","Machete de chatarra"),
         ("pump_shotgun","Escopeta de corredera"),("semi_rifle","Rifle semiautomático"),
         ("kinetic_smg","Subfusil cinético"),("light_machine_gun","Ametralladora ligera"),
         ("assault_rifle","Fusil de asalto"),("kinetic_sniper","Francotirador cinético"),
         ("rail_rifle","Rifle de riel"),("kinetic_blade","Cuchilla cinética"),
         ("grenade_launcher","Lanzagranadas"),("flamethrower","Lanzallamas"),
         ("rocket_launcher","Lanzacohetes"),("minigun","Minigun"),
         ("pulse_cannon","Cañón de impulso"),("arc_caster","Arco voltaico"),
         ("kinetic_grenade","Granada cinética")]


def rotated(p,r):
    if not r: return p
    o=r["origin"]; x,y,z=[p[i]-o[i] for i in range(3)]
    c,s=math.cos(math.radians(r["angle"])),math.sin(math.radians(r["angle"]))
    if r["axis"]=="x": y,z=y*c-z*s,y*s+z*c
    elif r["axis"]=="y": x,z=x*c+z*s,-x*s+z*c
    else: x,y=x*c-y*s,x*s+y*c
    return [x+o[0],y+o[1],z+o[2]]


def prepared(model):
    model=copy.deepcopy(model)
    for e in model["elements"]:
        e["preview_vertices"]={face:[rotated(p,e.get("rotation")) for p in corners(e["from"],e["to"],face)] for face in e["faces"]}
    return model


def picture(name,yaw=105,pitch=14):
    model=json.loads((ASSETS/f"models/item/{name}.json").read_text(encoding="utf-8"))
    pic=render(prepared(model),yaw,pitch,14,size=760)
    bounds=ImageChops.difference(pic,Image.new("RGB",pic.size,(63,63,63))).getbbox()
    return ImageOps.expand(pic.crop(bounds),border=10,fill=(63,63,63)) if bounds else pic


def gallery(entries,path,cols=4):
    sheet=Image.new("RGB",(cols*450,math.ceil(len(entries)/cols)*250),(34,39,45))
    try: font=ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf",19)
    except OSError: font=ImageFont.load_default()
    for i,(name,label) in enumerate(entries):
        yaw=20 if name in {"kinetic_blade","scrap_machete"} else 35 if name=="kinetic_grenade" else 105
        pic=picture(name,yaw)
        pic.save(OUT/f"{name}.png")
        pic.thumbnail((425,205),Image.Resampling.LANCZOS)
        x=(i%cols)*450; y=(i//cols)*250
        sheet.paste(pic,(x+(450-pic.width)//2,y+35+(205-pic.height)//2))
        ImageDraw.Draw(sheet).text((x+12,y+8),label,font=font,fill="#dbe4e5")
    sheet.save(path)


def main():
    OUT.mkdir(parents=True,exist_ok=True)
    gallery(ENTRIES,OUT/"arsenal_remodelado.png")
    variants=[("pipe_shotgun_m7_open","Escopeta: módulos y apertura"),
              ("bolt_rifle_m7_bolt","Rifle: mira y cerrojo atrás"),
              ("pump_shotgun_m7_pumped","Corredera: módulos y recarga"),
              ("semi_rifle_m7_mag_out","Semiautomático: cargador fuera"),
              ("revolver_open","Revólver: tambor abierto"),
              ("grenade_launcher_open","Lanzagranadas abierto"),
              ("minigun_s1","Minigun: giro"),("minigun_box_out","Minigun: sin caja")]
    gallery(variants,OUT/"variantes_y_recargas.png")
    print(OUT/"arsenal_remodelado.png")


if __name__=="__main__": main()
