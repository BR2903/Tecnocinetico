"""Marca las bocas y ejes de disparo sobre la geometría aprobada, sin modificar recursos."""
import json,math
import numpy as np
from PIL import Image,ImageDraw,ImageFont,ImageChops,ImageOps
from preview_weapon_remodel import ROOT,ASSETS,prepared
from weapon_blueprints import MUZZLES,muzzle_point
from render_zbuffer import render


def main():
    out=ROOT/"build/weapon_muzzles"; out.mkdir(parents=True,exist_ok=True)
    entries=[(n,n,0) for n in MUZZLES]+[(n+f"_m{m}",n,m) for n,m in (("pipe_shotgun",2),("pump_shotgun",2),("bolt_rifle",4),("semi_rifle",4))]
    sheet=Image.new("RGB",(1800,1050),(34,39,45))
    font=ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf",16)
    for i,(name,base,mods) in enumerate(entries):
        model=json.loads((ASSETS/f"models/item/{name}.json").read_text(encoding="utf-8"))
        pic=render(prepared(model),105,14,14,size=760)
        points=np.array([p for e in model["elements"] for p in (e["from"],e["to"])])
        center=(points.min(0)+points.max(0))/2
        def project(p):
            x,y,z=np.array(p)-center; c,s=math.cos(math.radians(105)),math.sin(math.radians(105))
            x,z=x*c-z*s,x*s+z*c
            c,s=math.cos(math.radians(14)),math.sin(math.radians(14)); y=y*c-z*s
            return 380+x*14,380-y*14
        point=muzzle_point(base,mods); p=project(point); q=project((point[0],point[1],point[2]-2))
        draw=ImageDraw.Draw(pic); draw.line([p,q],fill="#ffcf43",width=3)
        draw.ellipse((p[0]-4,p[1]-4,p[0]+4,p[1]+4),fill="#ff6655")
        bounds=ImageChops.difference(pic,Image.new("RGB",pic.size,(63,63,63))).getbbox()
        pic=ImageOps.expand(pic.crop(bounds),border=8,fill=(63,63,63)); pic.thumbnail((425,165))
        x=i%4*450; y=i//4*210
        sheet.paste(pic,(x+(450-pic.width)//2,y+35+(165-pic.height)//2))
        ImageDraw.Draw(sheet).text((x+10,y+8),name,font=font,fill="#dbe4e5")
    sheet.save(out/"bocas_comprobadas.png")
    print(out/"bocas_comprobadas.png")


if __name__=="__main__": main()
