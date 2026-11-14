"""Inventario/manos/suelo, generador montado y armadura articulada, con UV y profundidad reales.
python tools/preview_lote_inercita.py
Los modelos de herramienta heredan handheld vanilla; no se abre Minecraft.
"""
import copy,json,math
from pathlib import Path
from PIL import Image,ImageDraw
import numpy as np
import render_zbuffer as rz
import preview_nether_gear as ng
from armor3d_gen import PIVOT
from nether_remodel_gen import transform,verify_surfaces
from preview_weapon_remodel import prepared
from preview_combustion_drill import raster,font,euler
from lote_inercita_gen import A,OUT,TOOLS,PIECES

HANDHELD={'gui':{'rotation':[0,0,0],'scale':[1]*3},'ground':{'translation':[0,2,0],'scale':[.5]*3},
          'fixed':{'rotation':[0,180,0],'scale':[1]*3},
          'firstperson_righthand':{'rotation':[0,-90,25],'translation':[1.13,3.2,1.13],'scale':[.68]*3},
          'firstperson_lefthand':{'rotation':[0,90,-25],'translation':[1.13,3.2,1.13],'scale':[.68]*3},
          'thirdperson_righthand':{'rotation':[0,-90,55],'translation':[0,4,.5],'scale':[.85]*3},
          'thirdperson_lefthand':{'rotation':[0,90,-55],'translation':[0,4,.5],'scale':[.85]*3}}


def load(name):
    m=json.loads((A/f'models/item/{name}.json').read_text())
    if 'elements' in m:return m
    tex=m['textures']['layer0'];im=Image.open(rz.tex_path(tex,{})).convert('RGBA');elements=[]
    # Dos caras de la lámina y su borde de un píxel, igual al espesor de item/generated.
    elements.append({'from':[0,0,7.5],'to':[16,16,8.5], 'faces':{f:{'texture':'#0','uv':[0,0,16,16]} for f in ('north','south')}})
    for y in range(16):
        for x in range(16):
            if im.getpixel((x,y))[3]<128:continue
            faces={}
            for dx,dy,f in ((-1,0,'west'),(1,0,'east'),(0,-1,'up'),(0,1,'down')):
                xx,yy=x+dx,y+dy
                if not(0<=xx<16 and 0<=yy<16) or im.getpixel((xx,yy))[3]<128:faces[f]={'texture':'#0','uv':[x,y,x+1,y+1]}
            if faces:elements.append({'from':[x,15-y,7.5],'to':[x+1,16-y,8.5],'faces':faces})
    return {'textures':{'0':tex},'elements':elements,'display':copy.deepcopy(HANDHELD)}


def avatar(old=False,walk=False):
    ng.OUT=OUT;m={'textures':{},'elements':[]};ng.skin(m,walk)
    path=OUT/'antes/armor_models/inercita.json' if old else A/'armor_models/inercita.json'
    data=json.loads(path.read_text());texture='file:'+str((OUT/'antes/textures/models/armor/3d/inercita.png').resolve()) if old else 'tecnocinetico:models/armor/3d/inercita'
    for boxes in data['slots'].values():
        for e in boxes:ng.append_cube(m,e,texture,e['bone'],walk)
    return m


def third(model,left=False):
    m=avatar();ctx='thirdperson_'+('lefthand' if left else 'righthand');disp=model['display'][ctx]
    arm=transform({'at':PIVOT['left_arm' if left else 'right_arm'],'rot':(0,0,0)})
    def convert(p):
        angles=list(disp.get('rotation',[0]*3));tr=np.array(disp.get('translation',[0]*3),dtype=float)
        if left:angles[1]*=-1;angles[2]*=-1;tr[0]*=-1
        q=euler((np.array(p)-8)*np.array(disp.get('scale',[1]*3)),angles)+tr
        q=euler(euler(q+[-1 if left else 1,2,-10],[0,180,0]),[-90,0,0]);v=arm@np.array([*q,1])
        return [float(v[0]),-float(v[1]),float(v[2])]
    for e in prepared(model)['elements']:
        n=copy.deepcopy(e);n['preview_vertices']={f:[convert(v) for v in points] for f,points in e['preview_vertices'].items()}
        for fd in n['faces'].values():
            ref=fd['texture']
            while ref.startswith('#'):ref=model['textures'][ref[1:]]
            fd['texture']=ref
        p=np.array([v for face in n['preview_vertices'].values() for v in face]);n['from']=p.min(0).tolist();n['to']=p.max(0).tolist();m['elements'].append(n)
    return m


def item_sheet(names,path):
    sheet=Image.new('RGB',(1800,len(names)*350),(28,33,37));d=ImageDraw.Draw(sheet)
    for i,name in enumerate(names):
        m=load(name)
        for j,ctx in enumerate(('gui','firstperson_righthand','firstperson_lefthand')):
            pic=raster(m,ctx);pic.save(OUT/f'{name}_{ctx}.png')
            pic.thumbnail((280,280) if j==0 else (730,300))
            x=0 if j==0 else 300+(j-1)*750
            sheet.paste(pic,(x+(300-pic.width)//2 if j==0 else x,i*350+32))
        d.text((12,i*350+5),name,font=font(19),fill='#d8d8c0')
        raster(m,'ground').save(OUT/f'{name}_ground.png')
        for left in (False,True):
            posed=third(m,left);rz.render(posed,205 if not left else 155,10,18,size=850).save(OUT/f'{name}_tercera_{"izquierda" if left else "derecha"}.png')
    sheet.save(path)


def armor_views():
    sheet=Image.new('RGB',(2000,1000),(28,33,37));d=ImageDraw.Draw(sheet)
    cases=((True,False,195,'Antes · frente'),(False,False,195,'Nueva · frente'),(False,False,15,'Nueva · espalda'),(False,True,210,'Nueva · marcha'))
    for i,(old,walk,yaw,label) in enumerate(cases):
        m=avatar(old,walk);im=rz.render(m,yaw,8,18,size=900);im.save(OUT/f'armadura_{i}.png')
        sheet.paste(im.resize((500,500)),(i*500,180));d.text((i*500+20,100),label,font=font(24),fill='#dcd9c1')
    sheet.save(OUT/'armadura_puesta.png')
    # Referencia de ajuste de la manga, separada de la vista real de objetos en mano.
    # El cliente actual solo tiene armadura de tercera persona, sin hook RenderArmEvent.
    source=json.loads((A/'armor_models/inercita.json').read_text());refs=Image.new('RGB',(1920,620),(28,33,37));d=ImageDraw.Draw(refs)
    for i,left in enumerate((False,True)):
        m={'textures':{'0':'tecnocinetico:models/armor/3d/inercita'},'elements':[]}
        for e in source['slots']['chest']:
            if e['bone']!=('left_arm' if left else 'right_arm'):continue
            ng.append_cube(m,e,'#0','body')
        for el in m['elements']:
            for f,points in el['preview_vertices'].items():el['preview_vertices'][f]=[[q[0]+8,q[1]+12,q[2]+8] for q in points]
            points=np.array([p for face in el['preview_vertices'].values() for p in face]);el['from']=points.min(0).tolist();el['to']=points.max(0).tolist()
        m['display']={'firstperson_righthand':{'rotation':[-18,-25,24],'translation':[0,-1,-1],'scale':[.8]*3},
                      'firstperson_lefthand':{'rotation':[-18,-25,24],'translation':[0,-1,-1],'scale':[.8]*3}}
        pic=raster(m,'firstperson_'+('lefthand' if left else 'righthand'));refs.paste(pic,(i*960,60))
        d.text((i*960+15,15),'Referencia de manga · '+('izquierda' if left else 'derecha'),font=font(23),fill='#dcd9c1')
    d.text((20,590),'Referencia geométrica: requiere conectar el render del brazo en primera persona desde Java.',font=font(21),fill='#e2bd81')
    refs.save(OUT/'mangas_referencia_primera.png')


def main():
    OUT.mkdir(parents=True,exist_ok=True);old=rz.tex_path
    def path(ref,tex):
        while ref.startswith('#'):ref=tex[ref[1:]]
        return Path(ref[5:]) if ref.startswith('file:') else old(ref,{})
    rz.tex_path=path
    armor_views()
    item_sheet(TOOLS,OUT/'herramientas.png')
    item_sheet(tuple('inercita_'+p for p in PIECES),OUT/'armadura_objetos.png')
    item_sheet(('fuel_generator','standard_round'),OUT/'objetos_maquina_municion.png')
    # Cinco sprites nuevos a tamaño legible y sin convertir el inventario real a imágenes de alta resolución.
    sprites=Image.new('RGB',(1500,360),(28,33,37));d=ImageDraw.Draw(sprites)
    for i,name in enumerate(TOOLS):
        im=Image.open(A/f'textures/item/{name}.png').resize((240,240),Image.Resampling.NEAREST)
        sprites.paste(im,(i*300+30,40),im);d.text((i*300+10,305),name,font=font(18),fill='#dbd7bd')
    sprites.save(OUT/'herramientas_inventario.png')
    machine_sheet=Image.new('RGB',(1600,900),(28,33,37));d=ImageDraw.Draw(machine_sheet)
    for i,on in enumerate((False,True)):
        model=json.loads((A/f'models/block/fuel_generator{"_on" if on else ""}.json').read_text())
        pic=rz.render(prepared(model),205,20,37,size=850);pic.save(OUT/f'generador_{"encendido" if on else "apagado"}.png')
        machine_sheet.paste(pic.resize((800,800)),(i*800,55));d.text((i*800+15,15),'Generador · '+('encendido' if on else 'apagado'),font=font(25),fill='#dbd7bd')
    machine_sheet.save(OUT/'generador.png')
    ammo_sheet=Image.new('RGB',(1200,700),(28,33,37));d=ImageDraw.Draw(ammo_sheet)
    for i,name in enumerate(('kinetic_round','standard_round')):
        pic=rz.render(prepared(load(name)),205,20,36,size=750);ammo_sheet.paste(pic.resize((600,600)),(i*600,60))
        d.text((i*600+20,15),name+' · 6 balas',font=font(24),fill='#dbd7bd')
    ammo_sheet.save(OUT/'municion.png')
    combined=Image.new('RGB',(1600,2440),(28,33,37))
    combined.paste(machine_sheet,(0,0));combined.paste(Image.open(OUT/'armadura_puesta.png').resize((1600,800)),(0,900))
    combined.paste(sprites.resize((1500,360)),(50,1710));combined.paste(ammo_sheet.resize((600,350)),(500,2080));combined.save(OUT/'comparativa.png')
    print('Vistas: build/lote_inercita/comparativa.png, herramientas.png, armadura_puesta.png y objetos/ambas manos/tercera/suelo.')


if __name__=='__main__':main()
