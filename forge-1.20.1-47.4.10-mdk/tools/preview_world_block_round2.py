"""Lámina de segunda ronda: modelos/variantes reales junto a texturas vanilla locales.
python tools/preview_world_block_round2.py
Minecraft se lee de su caché instalada, solo para imágenes de build/, nunca se copia al mod.
"""
import copy,json,math,random,zipfile
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import render_zbuffer as rz
from preview_weapon_remodel import rotated
from world_block_round2 import A,OUT,R2


def font(n):
    try:return ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',n)
    except OSError:return ImageFont.load_default()


class Models:
    def __init__(self):
        self.jar=zipfile.ZipFile(Path.home()/'.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client-extra.jar')
        self.cache={}

    def read(self,ref,option):
        key=(ref,option)
        if key in self.cache:return copy.deepcopy(self.cache[key])
        ns,name=ref.split(':') if ':' in ref else ('minecraft',ref)
        if ns=='minecraft':data=json.loads(self.jar.read(f'assets/minecraft/models/{name}.json'))
        else:
            p=R2/option/f'models/{name}.json'
            if not p.exists():p=A/f'models/{name}.json'
            data=json.loads(p.read_text(encoding='utf-8'))
        parent=data.get('parent')
        if parent and not parent.startswith('builtin/'):
            base=self.read(parent,option);textures={**base.get('textures',{}),**data.get('textures',{})}
            base.update(data);base['textures']=textures;data=base
        self.cache[key]=data;return copy.deepcopy(data)

    def texture(self,ref,textures,option):
        while ref.startswith('#'):ref=textures[ref[1:]]
        ns,name=ref.split(':') if ':' in ref else ('minecraft',ref)
        if ns=='minecraft':
            p=R2/f'vanilla_preview/textures/{name}.png'
            if not p.exists():p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(self.jar.read(f'assets/minecraft/textures/{name}.png'))
            return p
        p=R2/option/f'textures/{name}.png'
        return p if p.exists() else A/f'textures/{name}.png'

    def put(self,scene,name,pos=(0,0,0),option='A',axis='y',selection=None,rng=None):
        rotate={}
        if selection is not None:
            variants=selection if isinstance(selection,list) else [selection]
            choice=(rng or random.Random(12)).choices(variants,weights=[v.get('weight',1) for v in variants])[0]
            name=choice['model'];rotate=choice
        m=self.read(name,option)
        for e in m.get('elements',[]):
            verts={}
            for f,fd in e['faces'].items():
                fd['texture']='file:'+str(self.texture(fd['texture'],m.get('textures',{}),option).resolve())
                if 'uv' not in fd:fd['uv']=default_uv(e,f)
                points=[]
                for p in rz.corners(e['from'],e['to'],f):
                    p=rotated(p,e.get('rotation'))
                    if axis!='y':
                        p=rotated(p,{'axis':'x','angle':-90,'origin':[8,8,8]})
                        if axis=='x':p=rotated(p,{'axis':'y','angle':-90,'origin':[8,8,8]})
                    for ax in ('x','y'):
                        if rotate.get(ax):p=rotated(p,{'axis':ax,'angle':-rotate[ax],'origin':[8,8,8]})
                    points.append([p[i]+pos[i] for i in range(3)])
                # Las plantillas vanilla pueden girar sus UV por cara.
                turn=fd.get('rotation',0)//90
                if turn:points=points[-turn:]+points[:-turn]
                verts[f]=points
            e['preview_vertices']=verts
            pts=[v for vs in verts.values() for v in vs]
            e['from']=[min(v[i] for v in pts) for i in range(3)];e['to']=[max(v[i] for v in pts) for i in range(3)]
            scene['elements'].append(e)


def default_uv(e,f):
    x,y,z=e['from'];a,b,c=e['to']
    return {'down':[x,16-c,a,16-z],'up':[x,z,a,c],'north':[16-a,16-b,16-x,16-y],
            'south':[x,16-b,a,16-y],'west':[z,16-b,c,16-y],'east':[16-c,16-b,16-z,16-y]}[f]


def scene(lib,name,option):
    m={'textures':{},'elements':[]};rng=random.Random(552)
    # Suelo discreto de piedra/tierra vanilla, con la misma composición en las tres columnas.
    for x in range(-16,97,16):
        for z in range(-16,33,16):lib.put(m,'minecraft:block/dirt',(x,-16,z))
    lib.put(m,'minecraft:block/stone',(-16,0,8));lib.put(m,'minecraft:block/stone',(-16,16,8))
    lib.put(m,'minecraft:block/oak_log',(80,0,8));lib.put(m,'minecraft:block/oak_log',(80,16,8))
    if name=='mossy_concrete':
        path=R2/option/'blockstates/mossy_concrete.json'
        state=json.loads(path.read_text())['variants']['']
        for y in range(3):
            for x in range(4):lib.put(m,'tecnocinetico:block/mossy_concrete',(x*16,y*16,0),option,selection=state,rng=rng)
        lib.put(m,'tecnocinetico:block/concrete',(64,0,0));lib.put(m,'tecnocinetico:block/cracked_concrete',(64,16,0))
    elif name in ('rusted_metal_beam','dead_log'):
        for y in range(3):lib.put(m,'tecnocinetico:block/'+name,(0,y*16,0),option)
        for x in (24,40,56):lib.put(m,'tecnocinetico:block/'+name,(x,0,0),option,axis='x')
        lib.put(m,'tecnocinetico:block/rusted_metal_sheet',(48,0,16));lib.put(m,'tecnocinetico:block/concrete',(64,0,16))
    else:
        for x in (0,16,32,48):
            lib.put(m,'tecnocinetico:block/'+name,(x,0,0),option)
        for x in (16,32):lib.put(m,'tecnocinetico:block/'+name,(x,16,0),option)
        lib.put(m,'tecnocinetico:block/ash_block',(64,0,0));lib.put(m,'tecnocinetico:block/vitrified_glass',(64,16,0))
    return m


def main():
    lib=Models();old=rz.tex_path
    def paths(ref,textures):return Path(ref[5:]) if ref.startswith('file:') else old(ref,textures)
    rz.tex_path=paths
    sheet=Image.new('RGB',(2160,4*610),(29,34,35))
    rows=(('mossy_concrete','Musgo · A: derrames de bosque / B: almohadillas claras · pared 4 × 3'),
          ('rusted_metal_beam','Acero · A: viga I remachada / B: perfil H de imprimación vieja'),
          ('dead_log','Tronco · A: madera expuesta y astillada / B: corteza carbonizada'),
          ('vitrified_sand','Arena · A: fulgurita ahumada / B: costra con lentes de vidrio miel'))
    for row,(name,label) in enumerate(rows):
        for col,option in enumerate(('actual','A','B')):
            m=scene(lib,name,option)
            yaw=195 if name=='mossy_concrete' else 210
            im=rz.render(m,yaw,17 if name=='mossy_concrete' else 24,6.6,size=1000)
            ImageDraw.Draw(im).text((25,24),option.upper(),font=font(33),fill='#e9e4d6')
            im.save(OUT/f'{name}_ronda2_{option}.png')
            sheet.paste(im.resize((720,580),Image.Resampling.LANCZOS),(col*720,row*610+30))
        ImageDraw.Draw(sheet).text((15,row*610+5),label,font=font(21),fill='#e8e3d5')
    sheet.save(OUT/'comparativa_2.png')
    # Detalle cercano del musgo y derivados, legible sin reducir la lámina completa.
    for option in ('A','B'):
        m={'textures':{},'elements':[]}
        for i,name in enumerate(('mossy_concrete','mossy_concrete_slab','mossy_concrete_stairs','mossy_concrete_wall_inventory')):
            lib.put(m,'tecnocinetico:block/'+name,(i*20,0,0),option)
        rz.render(m,210,28,10,size=1000).save(OUT/f'familia_musgosa_2_{option}.png')
        m={'textures':{},'elements':[]}
        for y in (0,16):lib.put(m,'tecnocinetico:block/rusted_metal_beam',(0,y,0),option)
        for x in (24,40):lib.put(m,'tecnocinetico:block/rusted_metal_beam',(x,0,0),option,axis='x')
        for z in (0,16):lib.put(m,'tecnocinetico:block/rusted_metal_beam',(72,0,z),option,axis='z')
        im=rz.render(m,215,26,8,size=1000)
        ImageDraw.Draw(im).text((20,20),f'{option}: tres ejes · Y vertical; X y Z horizontales',font=font(22),fill='#eee8d9')
        im.save(OUT/f'viga_tres_ejes_2_{option}.png')
    lib.jar.close();print('Lámina: build/remodelar_bloques/comparativa_2.png (vanilla auténtico, solo en build/)')


if __name__=='__main__':main()
