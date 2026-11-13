"""Objetos de preparación del Nether. Solo modelos, texturas y WornGearModel autorizado.
python tools/nether_gear_art.py
python tools/preview_nether_gear.py
"""
import copy,json,math,subprocess,sys
from pathlib import Path
from types import SimpleNamespace
from PIL import Image,ImageDraw
import gunkit
from nether_gear_rig import layout,paint_color,write_java
from worn_gear_texture import build
from armor3d_gen import Cube,item_models

ROOT=Path(__file__).resolve().parent.parent
A=ROOT/'src/main/resources/assets/tecnocinetico'
OUT=ROOT/'build/equipo_nether'
ITEMS={'thermal_cooler':'cooler','ash_filter':'filter','anchor_boots':'anchor'}
REFILLS=('coolant_pack','filter_cell','anchor_cell')


def save(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes((json.dumps(data,ensure_ascii=False,indent=2)+'\n').encode('utf-8'))


def display(back=False):
    angle=35 if back else 145
    return {'firstperson_righthand':{'rotation':[-5,angle,5],'translation':[-2,5.5,0],'scale':[.52]*3},
            'firstperson_lefthand':{'rotation':[-5,angle,5],'translation':[-2,5.5,0],'scale':[.52]*3},
            'thirdperson_righthand':{'translation':[0,2,1],'scale':[.5]*3},
            'thirdperson_lefthand':{'translation':[0,2,1],'scale':[.5]*3},
            'ground':{'translation':[0,2,0],'scale':[.5]*3},
            'fixed':{'rotation':[0,0 if back else 180,0],'scale':[.8]*3}}


def item_boxes(boxes,pair=False,fold_filter=False):
    out=[]
    for off in ((-2.6,2.6) if pair else (0,)):
        for original in boxes:
            e=copy.deepcopy(original);f,s=e['from'],e['size']
            # El arnés se guarda plegado en el objeto; puesto conserva la longitud para rodear el casco.
            if fold_filter and e['mat']=='rubber':
                if s[2]>8:s[2]=3.3
                elif s[0]>9:f[2]=-2.17
            low=[f[0]+off,-f[1]-s[1],f[2]];high=[f[0]+s[0]+off,-f[1],f[2]+s[2]]
            out.append(gunkit.el(low,high,'ng_'+e['mat']))
    low=[min(e['from'][i] for e in out) for i in range(3)];high=[max(e['to'][i] for e in out) for i in range(3)]
    shift=[8-(low[i]+high[i])/2 for i in range(3)]
    for e in out:
        for key in ('from','to'):e[key]=[round(e[key][i]+shift[i],4) for i in range(3)]
    return out


def refills():
    outline=(31,39,39);steel=(106,121,122);light=(196,207,197);shadow=(63,76,76)
    for name in REFILLS:
        im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
        if name=='coolant_pack':
            d.rectangle((6,1,9,3),fill=outline);d.line((6,1,9,1),fill=steel)
            d.polygon([(5,3),(10,3),(12,5),(12,13),(10,14),(4,14),(3,12),(3,5)],fill=outline)
            d.polygon([(5,4),(9,4),(10,6),(10,12),(4,12),(4,6)],fill=light)
            d.polygon([(10,5),(11,6),(11,12),(10,13)],fill=shadow)
            d.rectangle((6,6,8,11),fill=(47,139,153));d.line((6,6,6,10),fill=(133,217,216))
            d.line((4,13,9,13),fill=steel);d.point((8,9),fill=(211,236,222))
        elif name=='filter_cell':
            d.ellipse((3,2,12,14),fill=outline);d.rectangle((3,5,12,11),fill=outline)
            d.rectangle((4,5,11,11),fill=(163,154,113));d.ellipse((4,2,11,6),fill=steel)
            d.ellipse((5,3,10,5),fill=(58,69,65))
            for x in (5,7,9):d.line((x,6,x,11),fill=(92,96,70))
            d.line((4,12,11,12),fill=steel);d.line((5,13,10,13),fill=shadow)
            d.point((11,5),fill=(202,132,54))
        else:
            d.rectangle((5,1,6,3),fill=(177,116,56));d.rectangle((9,1,10,3),fill=(177,116,56))
            d.polygon([(4,3),(11,3),(13,5),(13,12),(11,14),(3,14),(2,12),(2,5)],fill=outline)
            d.rectangle((3,5,11,12),fill=(84,84,94));d.rectangle((11,5,12,12),fill=(49,55,61))
            d.polygon([(4,6),(9,6),(10,7),(5,7)],fill=(173,137,60))
            d.polygon([(4,8),(9,8),(10,9),(5,9)],fill=(173,137,60))
            d.rectangle((6,10,9,11),fill=(42,48,51));d.point((7,10),fill=(216,141,58))
            for x,y in ((3,5),(10,5),(3,12),(10,12)):d.point((x,y),fill=(157,160,156))
        im.save(A/f'textures/item/{name}.png')
        save(A/f'models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'tecnocinetico:item/{name}'}})
        im.resize((256,256),Image.Resampling.NEAREST).save(OUT/f'{name}_plano.png')


def armor():
    data=json.loads((ROOT/'tools/nether_refined_armor_source.json').read_text(encoding='utf-8'))
    # Las botas importadas se solapaban al estar quieto. Se desplazan hacia fuera sin mover los pivotes.
    for e in data['slots']['feet']:
        right=e['bone']=='right_leg'
        e['from'][0]=-2.6 if right else -1.4
        e['size'][0]=4
        if e['size'][1]==5:e['inflate']=.3 if right else .28
    save(A/'armor_models/refined_piroxite.json',data)
    slots={}
    for slot,entries in data['slots'].items():
        slots[slot]=[]
        for e in entries:
            c=Cube(e['bone'],e['from'],e['size'],'plate',e['inflate'],mirror=e['mirror']);c.uv=e['uv'];slots[slot].append(c)
    item_models(SimpleNamespace(name='refined_piroxite',slots=slots))
    for piece in ('helmet','chestplate','leggings','boots'):
        p=A/f'models/item/refined_piroxite_{piece}.json';m=json.loads(p.read_text())
        for side in ('righthand','lefthand'):
            m['display']['firstperson_'+side]={'rotation':[0,135,0],'translation':[-1.7,5.1,0],'scale':[.32]*3}
        save(p,m)
    return data


def main():
    OUT.mkdir(parents=True,exist_ok=True);rig=write_java();img,glow=build()
    img.save(A/'textures/entity/worn_gear.png');glow.save(A/'textures/entity/worn_gear_glow.png')
    for mat in {e['mat'] for boxes in rig.values() for e in boxes}:
        def painter(face,x,y,rnd,material=mat):
            c=paint_color(material,face.name,x,y,face.w,face.h)
            if min(face.w,face.h)>3 and rnd.random()<.035:return tuple(max(0,v-15) for v in c[:3])+(255,)
            return c
        gunkit.PAINTERS['ng_'+mat]=painter
    for name,part in ITEMS.items():gunkit.model(name,item_boxes(rig[part],part=='anchor',part=='filter'),display(part=='cooler'),group='nether_'+name)
    gunkit.flush();refills();armor()
    for name in (*ITEMS,*(f'refined_piroxite_{p}' for p in ('helmet','chestplate','leggings','boots'))):
        rotation='22,35,0' if name=='thermal_cooler' else '22,145,0' if name in ITEMS else '18,200,0'
        subprocess.run([sys.executable,str(ROOT/'tools/gui_fit.py'),str(A),rotation,'fill=14',name],check=True)
    save(OUT/'rig.json',rig)
    save(OUT/'integracion.json',{'texture':[128,128],
        'root_parts':{'cooler':'Coordenadas locales m.body, como jetpack','filter':'Coordenadas locales m.head, como visor',
                      'anchor':'Una bota; dibujar con m.rightLeg y m.leftLeg, como propulsor'},
        'pose':'PartPose.ZERO en las tres raíces. GearLayer aplica translateAndRotate del hueso del PlayerModel.',
        'legacy_uv':'Cuadrante superior izquierdo 64x64 de base y glow idéntico; texOffs y cajas anteriores intactos.',
        'glow':'Mismos modelos/pose para entityCutoutNoCull y eyes, como el equipo actual; solo indicadores y refrigerante de control.',
        'armor':'Solo se estrecharon/desplazaron hacia fuera los cubos de pies de refined_piroxite; pivotes, textura y otras piezas conservados.',
        'refills':'coolant_pack/filter_cell/anchor_cell son item/generated de 16x16, sin predicados.'})
    files=['src/main/java/com/bryanamaya/tecnocinetico/client/WornGearModel.java',
           'src/main/resources/assets/tecnocinetico/armor_models/refined_piroxite.json']
    files += [f'src/main/resources/assets/tecnocinetico/models/item/{name}.json' for name in (*ITEMS,*REFILLS,*(f'refined_piroxite_{p}' for p in ('helmet','chestplate','leggings','boots')))]
    files += [f'src/main/resources/assets/tecnocinetico/textures/item/gun/nether_{name}.png' for name in ITEMS]
    files += [f'src/main/resources/assets/tecnocinetico/textures/item/{name}.png' for name in REFILLS]
    files += [f'src/main/resources/assets/tecnocinetico/textures/entity/{name}.png' for name in ('worn_gear','worn_gear_glow')]
    files += ['tools/nether_gear_art.py','tools/nether_gear_rig.py','tools/preview_nether_gear.py','tools/nether_refined_armor_source.json','tools/worn_gear_texture.py','docs/HERRAMIENTAS.md']
    save(OUT/'archivos.json',files)
    print('Equipo Nether generado: objetos, recargas planas, armadura ajustada y tres raíces de equipo puesto.')


if __name__=='__main__':main()
