"""Conjuntos de postes construidos con los blockstates y vértices reales del arte.
python tools/preview_world_props.py
Las rotaciones de estado se aplican como Minecraft: X y después Y, alrededor de (8,8,8).
No crea Java ni recursos de juego; solo JSON y PNG en build/postes/.
"""
import copy
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
from world_props_gen import A, OUT, IDS, box, definition
from render_zbuffer import render, corners
from preview_weapon_remodel import rotated
import render_model as legacy


def font(size):
    try:return ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',size)
    except OSError:return ImageFont.load_default()


def place(scene,name,pos=(0,0,0),**state):
    states=json.loads((A/f'blockstates/{name}.json').read_text())['variants']
    variant=next(v for key,v in states.items() if dict(pair.split('=') for pair in key.split(','))==
                 {k:str(v).lower() if isinstance(v,bool) else str(v) for k,v in state.items()})
    path=variant['model'].split(':')[1]
    model=json.loads((A/f'models/{path}.json').read_text())
    for original in model['elements']:
        e=copy.deepcopy(original);verts={}
        for face in e['faces']:
            points=[]
            for p in corners(e['from'],e['to'],face):
                p=rotated(p,e.get('rotation'))
                for axis in ('x','y'):
                    if variant.get(axis):p=rotated(p,{'axis':axis,'angle':-variant[axis],'origin':[8,8,8]})
                points.append([p[i]+pos[i] for i in range(3)])
            verts[face]=points
        e['preview_vertices']=verts
        allp=[p for pts in verts.values() for p in pts]
        e['from']=[min(p[i] for p in allp) for i in range(3)]
        e['to']=[max(p[i] for p in allp) for i in range(3)]
        scene['elements'].append(e)


def base(xmax=48,zmax=32):
    m=definition('preview',[])
    m['textures']['ground']='tecnocinetico:block/cracked_asphalt'
    m['textures']['debris']='tecnocinetico:block/rubble'
    for x in range(-16,xmax,16):
        for z in range(-16,zmax,16):
            e=box(m['elements'],(x,-2,z),(min(x+16,xmax),0,min(z+16,zmax)),'iron')
            for f in e['faces'].values():f.update(texture='#ground',uv=[0,0,16,16])
    return m


def prop(scene,lo,hi,mat='rust'):
    e=box(scene['elements'],lo,hi,mat)
    for f in e['faces'].values():f.update(texture='#debris',uv=[0,0,16,16])


def scenes():
    result=[]
    m=base(30,25)
    for y in range(3):place(m,'street_lamp_pole',(0,16*y,0),axis='y')
    place(m,'street_lamp_head',(0,48,0),facing='north')
    result.append(('farola_de_pie','Farola de pie · 3 fustes + cabeza',m))
    m=base(30,25)
    for y in range(3):place(m,'street_lamp_pole',(0,16*y,0),axis='y')
    place(m,'street_lamp_bent',(0,48,0),facing='north')
    result.append(('farola_doblada','Farola doblada · pliegue de 45°',m))
    m=base(78,30)
    for x in range(3):place(m,'street_lamp_pole',(x*16,0,0),axis='x')
    # Cabeza con un estado real: sin inventar una rotación que el contrato no permite.
    place(m,'street_lamp_head',(48,0,0),facing='west')
    for x in (5,31):prop(m,(x,0,4),(x+8,6.01,12),'rust')
    result.append(('farola_caida','Farola caída · apoyada en escombros',m))
    m=base(48,107)
    # Cruceta sustituye al quinto bloque, no se suma a un poste de cinco.
    for z in (0,80):
        for y in range(4):place(m,'utility_pole',(0,y*16,z),axis='y')
        place(m,'utility_crossarm',(0,64,z),axis='x')
    for z in range(16,80,16):place(m,'power_cable',(0,64,z),axis='z',hanging=True)
    result.append(('postes_y_cables','Postes de 5 bloques · crucetas y tres cables',m))
    m=base(91,49)
    for x in range(5):place(m,'utility_pole',(16*x,0,0),axis='x')
    for x in (8,53):prop(m,(x,0,4),(x+9,5.01,12),'rust')
    for x in range(0,64,16):place(m,'power_cable',(x,0,17),axis='x',hanging=False)
    result.append(('poste_caido','Poste caído · cables enredados en el suelo',m))
    return result


def fitted(model,yaw,pitch,size=1000,padding=100):
    # Mismo encuadre para todas las piezas de un conjunto, sin alterar proporciones.
    pts=[p for e in model['elements'] for face in e['faces'] for p in e.get('preview_vertices',{}).get(face,corners(e['from'],e['to'],face))]
    cy,sy=math.cos(math.radians(yaw)),math.sin(math.radians(yaw))
    cp,sp=math.cos(math.radians(pitch)),math.sin(math.radians(pitch))
    projected=[(p[0]*cy-p[2]*sy,p[1]*cp-(p[0]*sy+p[2]*cy)*sp) for p in pts]
    width=max(p[0] for p in projected)-min(p[0] for p in projected)
    height=max(p[1] for p in projected)-min(p[1] for p in projected)
    return render(model,yaw,pitch,(size-padding)/max(width,height),size=size)


def main():
    OUT.mkdir(parents=True,exist_ok=True)
    sheet=Image.new('RGB',(1800,1200),(30,35,37))
    for i,(name,label,scene) in enumerate(scenes()):
        (OUT/f'{name}.json').write_text(json.dumps(scene,indent=2),encoding='utf-8')
        yaw=35 if name!='postes_y_cables' else 215
        pitch=20 if name not in ('farola_caida','poste_caido') else 32
        im=fitted(scene,yaw,pitch)
        ImageDraw.Draw(im).text((25,18),label,font=font(24),fill='#e9e4d7')
        im.save(OUT/f'{name}.png')
        tile=im.resize((600,600),Image.Resampling.LANCZOS)
        sheet.paste(tile,(i%3*600,i//3*600))
        fitted(scene,yaw+145,20).save(OUT/f'{name}_reverso.png')
    panel=Image.new('RGB',(600,600),(63,63,63));d=ImageDraw.Draw(panel)
    d.text((20,15),'Seis piezas · misma escala',font=font(23),fill='#e9e4d7')
    for i,name in enumerate(IDS):
        model=definition(name,[])
        state=({'axis':'y'} if name in ('street_lamp_pole','utility_pole') else
               {'facing':'north'} if name in ('street_lamp_head','street_lamp_bent') else
               {'axis':'x'} if name=='utility_crossarm' else {'axis':'x','hanging':False})
        place(model,name,**state)
        im=render(model,215 if name=='utility_crossarm' else 35,25,5.5,size=195)
        x,y=i%3*200,i//3*255+60;panel.paste(im,(x,y))
        d.text((x+6,y+198),name.replace('street_lamp','farola').replace('utility','poste'),font=font(13),fill='#ddd8ca')
    sheet.paste(panel,(1200,600));sheet.save(OUT/'comparativa.png')
    panel.save(OUT/'piezas.png')
    # Vista normal de objeto de bloque (sin convertirlo en icono plano).
    legacy.ASSETS=A
    inventory=Image.new('RGB',(900,600),(35,39,42))
    for i,name in enumerate(IDS):
        target='power_cable_ground' if name=='power_cable' else name
        model=json.loads((A/f'models/block/{target}.json').read_text())
        model['display']=json.loads((A/f'models/item/{name}.json').read_text())['display']
        im=legacy.render(model,0,0,560,projector=legacy.gui_view(model)).convert('RGB')
        im.resize((275,275),Image.Resampling.LANCZOS).save(OUT/f'{name}_inventario.png')
        inventory.paste(im.resize((275,275),Image.Resampling.LANCZOS),(i%3*300,i//3*300+20))
    inventory.save(OUT/'inventario.png')
    detail=Image.new('RGB',(1200,600),(63,63,63))
    for i,name in enumerate(('street_lamp_head','street_lamp_bent')):
        m=definition(name,[]);place(m,name,facing='north')
        detail.paste(fitted(m,215,-30,size=600),(i*600,0))
        ImageDraw.Draw(detail).text((i*600+15,15),'Cristal ausente y reflector · '+name,font=font(17),fill='#e9e4d7')
    detail.save(OUT/'detalle_luminarias.png')
    print('Vistas de los cinco conjuntos: build/postes/comparativa.png (profundidad real).')


if __name__=='__main__':main()
