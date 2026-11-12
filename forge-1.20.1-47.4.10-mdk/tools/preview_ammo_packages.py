"""Comparativas 3D de los diez modelos de munición, inventario y mano vanilla, sin abrir Minecraft.
python tools/preview_ammo_packages.py
"""
import json
from pathlib import Path
from PIL import Image,ImageDraw
from ammo_packages_gen import NAMES
from preview_combustion_drill import raster,font
from preview_weapon_remodel import prepared
from render_zbuffer import render

ROOT=Path(__file__).resolve().parent.parent
A=ROOT/'src/main/resources/assets/tecnocinetico'
OUT=ROOT/'build/municion'
LABELS=('Cinéticas · paquete de 6','Guiadas · paquete de 6','Rifle · 5 balas sueltas',
        'Escopeta · 6 cartuchos','Cinta · 15 balas','Peine · 5 balas','Cargador corto · 10',
        'Cargador curvo · 30','Caja de cinta · 60','Caja industrial · 180')

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    combined=Image.new('RGB',(1600,5*320),(27,33,37))
    gui_sheet=Image.new('RGB',(5*300,2*340),(27,33,37))
    hand_sheet=Image.new('RGB',(2*600,5*365),(27,33,37))
    models={}
    for i,(name,label) in enumerate(zip(NAMES,LABELS)):
        model=json.loads((A/f'models/item/{name}.json').read_text(encoding='utf-8'));models[name]=model
        gui=raster(model,'gui');gui.save(OUT/f'{name}_gui.png')
        hand=raster(model,'firstperson_righthand');hand.save(OUT/f'{name}_mano.png')
        left=raster(model,'firstperson_lefthand');left.save(OUT/f'{name}_mano_izquierda.png')
        render(prepared(model),215,20,24,size=720).save(OUT/f'{name}_tres_cuartos.png')
        x=i%2*800;y=i//2*320
        combined.paste(gui.resize((270,270)),(x,y+34));combined.paste(hand.resize((510,287)),(x+280,y+30))
        ImageDraw.Draw(combined).text((x+14,y+6),label,font=font(19),fill='#e4dfd2')
        x=i%5*300;y=i//5*340;gui_sheet.paste(gui.resize((290,290)),(x,y+35))
        ImageDraw.Draw(gui_sheet).text((x+10,y+8),label,font=font(17),fill='#e4dfd2')
        x=i%2*600;y=i//2*365;hand_sheet.paste(hand.resize((600,338)),(x,y+27))
        ImageDraw.Draw(hand_sheet).text((x+12,y+5),label,font=font(18),fill='#e4dfd2')
    combined.save(OUT/'comparativa.png');gui_sheet.save(OUT/'comparativa_gui.png');hand_sheet.save(OUT/'comparativa_mano.png')
    # Misma escala física: así se compara la abundancia sin que gui_fit iguale el tamaño de los iconos.
    same=Image.new('RGB',(1600,650),(63,63,63));d=ImageDraw.Draw(same)
    for i,name in enumerate(('rifle_round','rifle_belt')):
        im=render(prepared(models[name]),215,20,32,size=800);same.paste(im.crop((0,90,800,710)),(i*800,30))
        d.text((i*800+20,10),'5 balas sueltas' if i==0 else '15 balas enlazadas · misma escala',font=font(22),fill='#eee8da')
    same.save(OUT/'rifle_cinco_vs_cinta_quince.png')
    print('Vistas: build/municion/comparativa.png, comparativa_gui.png y comparativa_mano.png')

if __name__=='__main__':main()
