"""Objetos y equipo articulado sobre maniquí/armadura, con geometría y UV reales.
python tools/preview_nether_gear.py
No abre Minecraft ni modifica recursos del mod.
"""
import copy,json,math,re
from pathlib import Path
from PIL import Image,ImageDraw
import numpy as np
from nether_gear_art import A,OUT,ITEMS,REFILLS
from nether_gear_rig import JAVA,layout,regions
from armor3d_gen import PIVOT
from render_zbuffer import render
import render_zbuffer as rz
from preview_combustion_drill import raster,font
from preview_weapon_remodel import rotated

FACES={'top':'up','bottom':'down','front':'north','back':'south','right':'west','left':'east'}


def append_cube(scene,e,texture,bone='body',walk=False):
    inflate=e.get('inflate',0);f=[v-inflate for v in e['from']];s=[v+2*inflate for v in e['size']]
    pose=PIVOT[bone];faces={};verts={}
    for name,(u,v,w,h) in regions(e).items():
        face=FACES[name];uv=[u/8,v/8,(u+w)/8,(v+h)/8]
        if e.get('mirror'):uv=[uv[2],uv[1],uv[0],uv[3]]
        faces[face]={'texture':texture,'uv':uv}
        # UV de Java: v=0 en la parte superior. Construir primero en Y arriba y articular en Y abajo.
        pts=rz.corners([f[0],-f[1]-s[1],f[2]],[f[0]+s[0],-f[1],f[2]+s[2]],face)
        points=[]
        for p in pts:
            p=[p[0],-p[1],p[2]]
            if walk and 'leg' in bone:p=rotated(p,{'axis':'x','angle':-22 if bone=='left_leg' else 22,'origin':[0,0,0]})
            if walk and 'arm' in bone:p=rotated(p,{'axis':'x','angle':15 if bone=='left_arm' else -15,'origin':[0,0,0]})
            p=[p[i]+pose[i] for i in range(3)];points.append([p[0],-p[1],p[2]])
        verts[face]=points
    allp=[p for pts in verts.values() for p in pts]
    scene['elements'].append({'from':[min(p[i] for p in allp) for i in range(3)],'to':[max(p[i] for p in allp) for i in range(3)],
                              'faces':faces,'preview_vertices':verts})


def skin(scene,walk=False):
    path=OUT/'maniqui.png'
    im=Image.new('RGBA',(128,128),(60,68,70,255));im.save(path)
    body=[('head',[-4,-8,-4],[8,8,8]),('body',[-4,0,-2],[8,12,4]),('right_arm',[-3,-2,-2],[4,12,4]),
          ('left_arm',[-1,-2,-2],[4,12,4]),('right_leg',[-2,0,-2],[4,12,4]),('left_leg',[-2,0,-2],[4,12,4])]
    for bone,f,s in body:append_cube(scene,{'from':f,'size':s,'uv':[0,0]},'file:'+str(path.resolve()),bone,walk)


def worn(armored=False,walk=False,legacy=None,old=False):
    m={'textures':{},'elements':[]};skin(m,walk)
    if armored:
        armor=json.loads((A/'armor_models/refined_piroxite.json').read_text())
        for boxes in armor['slots'].values():
            for e in boxes:append_cube(m,e,'tecnocinetico:models/armor/3d/refined_piroxite',e['bone'],walk)
    if legacy:
        source=(OUT/'antes/WornGearModel.java').read_text(encoding='utf-8') if old else JAVA.read_text(encoding='utf-8')
        # Encontrar raíces sin interpretar Java de lógica ni modificarlo.
        for match in re.finditer(r'root.addOrReplaceChild\("(\w+)", CubeListBuilder.create\(\)(.*?)PartPose.ZERO\);',source,re.S):
            name,block=match.groups()
            if name not in legacy:continue
            bone='head' if name=='visor' else 'right_leg' if name=='propulsor' else 'body'
            for v in re.finditer(r'\.texOffs\((\d+),\s*(\d+)\)\.addBox\(([^)]+)\)',block):
                nums=[float(t.strip().rstrip('F')) for t in v[3].split(',')]
                e={'from':nums[:3],'size':nums[3:],'uv':[int(v[1]),int(v[2])]}
                if old:e['uv']=[q*2 for q in e['uv']];e['size']=[q for q in e['size']]
                # La función normaliza a 128; para atlas anterior de 64 las regiones se escalan aparte.
                texture='tecnocinetico:entity/worn_gear'
                if old:
                    # Expandir solo en build/ equivale al mismo muestreo, con UV sin cambiar.
                    pic=Image.new('RGBA',(128,128));pic.paste(Image.open(OUT/'antes/worn_gear.png'),(0,0));pic.save(OUT/'legacy_128.png')
                    e['uv']=[q/2 for q in e['uv']];texture='file:'+str((OUT/'legacy_128.png').resolve())
                append_cube(m,e,texture,bone,walk)
                if name=='propulsor':append_cube(m,e,texture,'left_leg',walk)
    else:
        for part,boxes in layout().items():
            bones=('body',) if part=='cooler' else ('head',) if part=='filter' else ('right_leg','left_leg')
            for bone in bones:
                for e in boxes:append_cube(m,e,'tecnocinetico:entity/worn_gear',bone,walk)
    return m


def flat_model(name):
    return {'textures':{'0':f'tecnocinetico:item/{name}'},
            'elements':[{'from':[0,0,7.5],'to':[16,16,8.5],'faces':{f:{'texture':'#0','uv':[0,0,16,16]} for f in ('north','south')}}],
            'display':{'gui':{'scale':[.8]*3},'firstperson_righthand':{'rotation':[0,-35,10],'translation':[0,2,0],'scale':[.5]*3},
                       'ground':{'rotation':[0,0,0],'scale':[.5]*3}}}


def main():
    OUT.mkdir(parents=True,exist_ok=True);original=rz.tex_path
    def paths(ref,tex):return Path(ref[5:]) if ref.startswith('file:') else original(ref,tex)
    rz.tex_path=paths
    sheet=Image.new('RGB',(2000,800),(29,34,38));hand_sheet=Image.new('RGB',(1500,7*300),(29,34,38))
    names=(*ITEMS,*REFILLS,*(f'refined_piroxite_{p}' for p in ('helmet','chestplate','leggings','boots')))
    j=0
    for i,name in enumerate(names):
        m=flat_model(name) if name in REFILLS else json.loads((A/f'models/item/{name}.json').read_text())
        im=raster(m,'gui');im.save(OUT/f'{name}_inventario.png')
        x,y=i%5*400,i//5*400;sheet.paste(im.resize((370,370)),(x,y+30))
        ImageDraw.Draw(sheet).text((x+8,y+5),name,font=font(19),fill='#e5e2d3')
        if name not in REFILLS:
            right=raster(m,'firstperson_righthand');left=raster(m,'firstperson_lefthand');ground=raster(m,'ground')
            right.save(OUT/f'{name}_mano.png');left.save(OUT/f'{name}_mano_izquierda.png');ground.save(OUT/f'{name}_suelo.png')
            hand_sheet.paste(im.resize((280,280)),(0,j*300+20));hand_sheet.paste(right.resize((640,360)).crop((0,60,640,340)),(300,j*300+20))
            hand_sheet.paste(ground.resize((280,280)),(1040,j*300+20));ImageDraw.Draw(hand_sheet).text((8,j*300+2),name,font=font(18),fill='#e5e2d3');j+=1
    sheet.save(OUT/'objetos.png');hand_sheet.save(OUT/'mano_suelo.png')
    players=Image.new('RGB',(2000,900),(63,63,63))
    for i,(armored,yaw,label) in enumerate(((False,180,'Equipo · frente'),(False,0,'Equipo · espalda'),(True,180,'Piroxita Abisal · frente'),(True,0,'Piroxita Abisal · espalda'))):
        model=worn(armored);pic=render(model,yaw,7,22,size=900)
        pic.save(OUT/f'jugador_{"armadura_" if armored else ""}{"frente" if yaw else "espalda"}.png')
        players.paste(pic.resize((500,800)),(i*500,70));ImageDraw.Draw(players).text((i*500+10,15),label,font=font(20),fill='#eee8d6')
    players.save(OUT/'jugador_frente_espalda.png')
    render(worn(True,True),205,12,21,size=1000).save(OUT/'jugador_marcha.png')
    report={}
    for level in (1,2,3):
        parts=['visor','jetpack','propulsor']+(['jetpack_mk2'] if level>=2 else [])+(['jetpack_mk3'] if level>=3 else [])
        first=render(worn(legacy=parts,old=True),215,16,20,size=850);second=render(worn(legacy=parts),215,16,20,size=850)
        count=int(np.any(np.asarray(first)!=np.asarray(second),axis=2).sum());assert count==0,(level,count)
        report[f'legacy_mk{level}_changed_pixels']=count;second.save(OUT/f'equipo_anterior_mk{level}.png')
    (OUT/'regresion_visual.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    print('Vistas: build/equipo_nether/objetos.png, jugador_frente_espalda.png y mano_suelo.png')


if __name__=='__main__':main()
