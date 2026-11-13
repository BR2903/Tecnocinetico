"""Objetos raros y Mk II: inventario, mano/suelo y equipo puesto, frente/espalda."""
import json
from pathlib import Path
from PIL import Image,ImageDraw
import render_zbuffer as rz
from nether_gear_art import A,ITEMS
from nether_gear_rig import layout
from preview_nether_gear import skin,append_cube,flat_model
from preview_combustion_drill import raster,font
from abyss_foundry_gen import OUT


def mannequin(level):
    m={'textures':{},'elements':[]};skin(m)
    for item,part in ITEMS.items():
        bones=('body',) if part=='cooler' else ('head',) if part=='filter' else ('right_leg','left_leg')
        for bone in bones:
            for e in layout()[part+('_mk2' if level==2 else '')]:append_cube(m,e,'tecnocinetico:entity/worn_gear',bone)
    return m


def main():
    OUT.mkdir(parents=True,exist_ok=True);old=rz.tex_path
    rz.tex_path=lambda ref,tex:Path(ref[5:]) if ref.startswith('file:') else old(ref,tex)
    names=('nether_coupler','abyssal_component',*(n+'_mk2' for n in ITEMS))
    sheet=Image.new('RGB',(1500,1500),(30,35,39));d=ImageDraw.Draw(sheet)
    for i,name in enumerate(names):
        m=flat_model(name) if name=='abyssal_component' else json.loads((A/f'models/item/{name}.json').read_text())
        for j,context in enumerate(('gui','firstperson_righthand','ground')):
            im=raster(m,context);im.save(OUT/f'{name}_{context}.png')
            im.thumbnail((490,255));sheet.paste(im,(j*500+(500-im.width)//2,i*300+35))
        d.text((15,i*300+8),name,font=font(19),fill='#eddfc4')
    sheet.save(OUT/'objetos.png')
    worn=Image.new('RGB',(1800,800),(30,35,39));d=ImageDraw.Draw(worn)
    for i,(level,yaw,label) in enumerate(((1,190,'Mk I · frente'),(2,190,'Mk II · frente'),(1,10,'Mk I · espalda'),(2,10,'Mk II · espalda'))):
        m=mannequin(level);im=rz.render(m,yaw,8,17,size=760);im.save(OUT/f'equipo_mk{level}_{"frente" if yaw==190 else "espalda"}.png')
        worn.paste(im.resize((450,450)),(i*450,140));d.text((i*450+20,50),label,font=font(24),fill='#e6d7b8')
    worn.save(OUT/'equipo_puesto.png')
    print('Vistas: build/fundidor/objetos.png y equipo_puesto.png')


if __name__=='__main__':main()
