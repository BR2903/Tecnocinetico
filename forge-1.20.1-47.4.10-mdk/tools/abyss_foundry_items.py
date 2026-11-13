"""Acoplador, componente plano y mejoras Mk II del Fundidor; solo arte.
python tools/abyss_foundry_items.py
"""
import json,subprocess,sys
from PIL import Image,ImageDraw
import gunkit
from nether_gear_art import A,display,item_boxes,save,ITEMS
from nether_gear_rig import write_java,paint_color
from worn_gear_texture import build
from abyss_foundry_gen import ROOT,OUT


def component():
    im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
    # Cojinete de horno roto: material pequeño, dibujado píxel a píxel.
    d.polygon([(4,2),(11,2),(14,5),(14,11),(11,14),(4,14),(1,11),(1,5)],fill='#262b28')
    d.polygon([(4,3),(10,3),(13,5),(13,10),(10,13),(4,13),(2,10),(2,6)],fill='#76664d')
    d.polygon([(4,4),(9,4),(10,6),(12,7),(11,11),(5,12),(3,9),(3,6)],fill='#433e32')
    d.polygon([(6,5),(10,6),(11,9),(9,11),(5,10),(4,7)],fill='#a64c29')
    d.polygon([(6,6),(9,6),(10,8),(8,10),(5,8)],fill='#e17d32')
    d.line((6,6,8,6,9,7),fill='#f6cc7a');d.point((8,8),fill='#ffe5a5')
    d.line((3,5,5,3,10,3),fill='#b2ac8c');d.line((3,11,5,13,10,13),fill='#4b493b')
    for x,y in ((3,5),(11,4),(12,11),(4,12)):
        d.point((x,y),fill='#d2c19a');d.point((x+1,y+1),fill='#302d25')
    im.save(A/'textures/item/abyssal_component.png')
    save(A/'models/item/abyssal_component.json',{'parent':'minecraft:item/generated','textures':{'layer0':'tecnocinetico:item/abyssal_component'}})


def coupler():
    els=[]
    def b(f,t,m):els.append(gunkit.el(f,t,'ng_'+m))
    # Acoplamiento mecánico con paso central abierto, guías y ocho contactos.
    b([3,3,5],[13,5,11],'slag_plate');b([3,11,5],[13,13,11],'slag_plate')
    b([3,5,5],[5,11,11],'slag_plate');b([11,5,5],[13,11,11],'slag_plate')
    for x in (2.7,10.7):
        for y in (2.7,10.7):
            b([x,y,4.55],[x+2.6,y+2.6,5.25],'forge_edge')
            b([x+.8,y+.8,4.15],[x+1.8,y+1.8,4.55],'dark')
    for y in (5.8,8.6):
        b([4.7,y,5.6],[5.5,y+1.5,10.4],'copper')
        b([10.5,y,5.6],[11.3,y+1.5,10.4],'copper')
    for x in (5.8,8.6):
        b([x,4.7,5.6],[x+1.5,5.5,10.4],'copper')
        b([x,10.5,5.6],[x+1.5,11.3,10.4],'copper')
    for x in (5.2,9.5):
        b([x,3.5,4.65],[x+1.3,4.2,4.95],'pyroxite')
    b([6,12.8,6.3],[10,14.5,9.7],'dark')
    b([6.8,14.5,7],[7.5,15.5,9],'copper');b([8.5,14.5,7],[9.2,15.5,9],'copper')
    gunkit.model('nether_coupler',els,display(False),group='nether_coupler')


def main():
    OUT.mkdir(parents=True,exist_ok=True);rig=write_java();im,glow=build()
    im.save(A/'textures/entity/worn_gear.png');glow.save(A/'textures/entity/worn_gear_glow.png')
    for mat in {e['mat'] for boxes in rig.values() for e in boxes}:
        def painter(face,x,y,rnd,material=mat):return paint_color(material,face.name,x,y,face.w,face.h)
        gunkit.PAINTERS['ng_'+mat]=painter
    for name,part in ITEMS.items():
        gunkit.model(name+'_mk2',item_boxes(rig[part+'_mk2'],part=='anchor',part=='filter'),display(part=='cooler'),group='nether_'+name+'_mk2')
    coupler();gunkit.flush();component()
    for name in ITEMS:
        path=A/f'models/item/{name}.json';m=json.loads(path.read_text(encoding='utf-8'))
        overrides=[o for o in m.get('overrides',[]) if 'tecnocinetico:level' not in o['predicate']]
        m['overrides']=overrides+[{'predicate':{'tecnocinetico:level':2},'model':f'tecnocinetico:item/{name}_mk2'}]
        # Mantener geometría, textura y todas las vistas de Mk I.
        path.write_text(json.dumps(m,indent=2),encoding='utf-8')
    for name in (*(n+'_mk2' for n in ITEMS),'nether_coupler'):
        rotation='22,35,0' if name.startswith('thermal') else '22,145,0'
        subprocess.run([sys.executable,str(ROOT/'tools/gui_fit.py'),str(A),rotation,'fill=14',name],check=True)
    save(OUT/'equipo_mk2_rig.json',rig)
    save(OUT/'integracion_equipo.json',{
        'item_property':'tecnocinetico:level debe devolver 2 para NBT Level=2, sin normalizar.',
        'item_models':{n:n+'_mk2' for n in ITEMS},
        'worn_roots':{'cooler_mk2':'Raíz completa: transformar con PlayerModel.body, como cooler.',
                      'filter_mk2':'Raíz completa: transformar con PlayerModel.head, como filter.',
                      'anchor_mk2':'Una sobrebota completa: dibujar una vez con cada pierna.'},
        'selection':'Level >= 2: dibujar solo la raíz _mk2; Level < 2: solo Mk I. No superponer ambas.',
        'textures':['textures/entity/worn_gear.png','textures/entity/worn_gear_glow.png'],
        'glow':'Misma raíz y pose en la capa eyes existente. Indicadores de piroxita incluidos.',
        'other_items':'nether_coupler (3D pequeño) y abyssal_component (item/generated 16x16), sin predicados.',
        'preservation':'Mk I: solo overrides de objeto modificados. Raíces, cubos, UV y píxeles anteriores conservados.'})
    print('Acoplador 3D, componente 16x16, tres objetos Mk II y tres raíces puestas; predicado level >= 2.')


if __name__=='__main__':main()
