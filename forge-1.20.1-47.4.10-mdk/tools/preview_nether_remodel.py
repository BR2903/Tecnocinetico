"""Vistas frontales/3-4 con profundidad real, más comprobación de brazos y piernas en movimiento.
python tools/preview_nether_remodel.py
"""
import json
from pathlib import Path
from PIL import Image, ImageDraw
from nether_remodel_rigs import NAMES
from render_zbuffer import render

ROOT=Path(__file__).resolve().parent.parent
OUT=ROOT/'build/nether_remodel'
LABELS={'piglin':'PIGLIN / INFANTERIA','piglin_brute':'BRUTO / BLINDAJE PESADO',
        'zombified_piglin':'ZOMBIFICADO / DETERIORADO','ghast':'GHAST / HORNO FLOTANTE',
        'blaze':'BLAZE / INYECTORES ORBITALES','wither_skeleton':'WITHER / ESQUELETO MECANIZADO'}

def frame(name,suffix,yaw,pitch,size=720):
    model=json.loads((OUT/f'{name}{suffix}.json').read_text(encoding='utf-8'))
    bounds=[e[k] for e in model['elements'] for k in ('from','to')]
    span=max(max(v[i] for v in bounds)-min(v[i] for v in bounds) for i in range(3))
    image=render(model,yaw,pitch,size*.72/span,size=size)
    d=ImageDraw.Draw(image);d.text((18,16),LABELS[name],fill=(235,223,204));return image

def main():
    sheet=Image.new('RGB',(1200,6*390),(30,30,32));draw=ImageDraw.Draw(sheet)
    for row,name in enumerate(NAMES):
        for col,(view,yaw,pitch) in enumerate((('front',180,0),('three_quarter',210,12))):
            im=frame(name,'',yaw,pitch);im.save(OUT/f'{name}_{view}.png')
            sheet.paste(im.resize((390,390)),(col*400,row*390))
        walk=OUT/f'{name}_walk.json'
        if walk.exists():im=frame(name,'_walk',210,12)
        else:im=frame(name,'',145,15)
        im.save(OUT/f'{name}_articulation.png');sheet.paste(im.resize((390,390)),(800,row*390))
    sheet.save(OUT/'comparativa.png');print('Vistas listas: build/nether_remodel/comparativa.png')

if __name__=='__main__':main()
