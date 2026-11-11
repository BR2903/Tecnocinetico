"""Comparación geométrica: posición anterior en rojo y boca actual en cian al volver del retroceso."""
import json,math
import numpy as np
from PIL import Image,ImageDraw,ImageFont,ImageChops,ImageOps
from preview_weapon_remodel import ROOT,ASSETS,prepared
from weapon_blueprints import muzzle_point
from render_zbuffer import render


def main():
    entries=["pipe_shotgun","pump_shotgun","bolt_rifle","semi_rifle","rail_rifle","kinetic_sniper",
             "revolver","grenade_launcher","rocket_launcher"]
    sheet=Image.new("RGB",(1350,735),(34,39,45))
    font=ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf",17)
    ImageDraw.Draw(sheet).text((12,8),"Simulación de retorno: rojo = fotograma anterior; cian = salida actual",font=font,fill="#dbe4e5")
    for i,name in enumerate(entries):
        model=json.loads((ASSETS/f"models/item/{name}.json").read_text(encoding="utf-8"))
        pic=render(prepared(model),105,14,14,size=760)
        points=np.array([p for e in model["elements"] for p in (e["from"],e["to"])])
        center=(points.min(0)+points.max(0))/2
        def project(p):
            x,y,z=np.array(p)-center; c,s=math.cos(math.radians(105)),math.sin(math.radians(105))
            x,z=x*c-z*s,x*s+z*c
            c,s=math.cos(math.radians(14)),math.sin(math.radians(14)); y=y*c-z*s
            return 380+x*14,380-y*14
        raw=np.array(muzzle_point(name),dtype=float); old=raw-np.array([8,8,8])
        c,s=math.cos(math.radians(23.4)),math.sin(math.radians(23.4))
        old[1],old[2]=old[1]*c-old[2]*s,old[1]*s+old[2]*c
        old+=np.array([8,8.65,8]); p=project(raw); q=project(old)
        d=ImageDraw.Draw(pic); d.line([p,q],fill="#92736c",width=1)
        for point,color in ((q,"#ff6655"),(p,"#43eaff")):
            x,y=point; d.ellipse((x-4,y-4,x+4,y+4),fill=color)
        bounds=ImageChops.difference(pic,Image.new("RGB",pic.size,(63,63,63))).getbbox()
        pic=ImageOps.expand(pic.crop(bounds),border=8,fill=(63,63,63)); pic.thumbnail((425,180))
        x=i%3*450; y=i//3*230+35
        sheet.paste(pic,(x+(450-pic.width)//2,y+30+(180-pic.height)//2))
        ImageDraw.Draw(sheet).text((x+10,y+5),name,font=font,fill="#dbe4e5")
    out=ROOT/"build/weapon_muzzles/retroceso_corregido.png"; out.parent.mkdir(parents=True,exist_ok=True)
    sheet.save(out); print(out)


if __name__=="__main__": main()
