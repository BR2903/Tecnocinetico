"""Generador de Combustible, herramientas/armadura de Inercita y paquete de bala normal.
python tools/lote_inercita_gen.py
Solo arte. No modifica Java, registros, recetas, estadísticas ni munición aprobada.
"""
import copy,json,math,subprocess,sys
from pathlib import Path
from PIL import Image,ImageDraw
import gunkit
from inercita_equipment_rig import make_armor,write_armor
from armor3d_gen import item_models
from petroleum_art import painters

ROOT=Path(__file__).resolve().parent.parent
A=ROOT/'src/main/resources/assets/tecnocinetico'
OUT=ROOT/'build/lote_inercita'
TOOLS=('inercita_pickaxe','inercita_axe','inercita_shovel','inercita_hoe','inercita_sword')
PIECES=('helmet','chestplate','leggings','boots')


def save(path,data):
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')


def fit(names,angles='22,145,0'):
    subprocess.run([sys.executable,str(ROOT/'tools/gui_fit.py'),str(A),angles,'fill=14',*names],check=True)


def machine(on):
    e=[]
    def b(f,s,mat='steel_dark',rot=None):e.append(gunkit.el(f,[f[i]+s[i] for i in range(3)],mat,rot=rot))
    b([.4,.15,.4],[15.2,1.8,15.2],'steel_dark')
    for x in (1,12.7):
        for z in (1,12.7):
            b([x,1.95,z],[2.3,1.0,2.3],'rubber')
            b([x+.2,2.95,z+.2],[1.9,.45,1.9],'steel')
    # Cárter y cilindro aleteado: máquina de motor visible, sin carcasa de cubo genérica.
    b([5.2,3.1,4.5],[5.7,5.6,7.4],'graphite')
    b([5.8,8.7,5.1],[4.5,3,5.5],'steel_dark')
    for i in range(7):b([5.35,9.05+i*.55,4.8],[5.4,.23,6.1],'steel')
    b([5.85,12.65,5.3],[4.4,.65,5.2],'steel_dark')
    for x,z in ((6.2,5.7),(9.4,5.7),(6.2,9.8),(9.4,9.8)):b([x,13.3,z],[.55,.35,.55],'steel_hi')
    b([7.6,13.3,7.7],[.8,1.0,.8],'ceramic')
    b([7.85,14.25,7.95],[2.5,.3,.3],'rubber')
    b([10.05,10.7,7.95],[.3,3.7,.3],'rubber')
    # Radiador frontal, columna lateral y mangueras conectadas al motor.
    b([1.2,3.2,1.0],[3.1,8.0,3.2],'steel_dark')
    b([1.55,3.6,.75],[2.4,7.2,.25],'graphite')
    for y in (4,5,6,7,8,9,10):b([1.4,y,.45],[2.7,.3,.35],'steel')
    b([1.3,11.2,1.1],[2.9,.7,3.0],'steel')
    b([2.2,11.9,2.0],[1.1,.5,1.1],'rubber')
    b([3.1,10.3,3.9],[2.8,.75,.75],'rubber');b([5.15,8.2,3.9],[.75,2.1,.75],'rubber')
    # Volante: aro octogonal estático y cuatro radios que cambian de posición al encender.
    cx,cy=7.8,6.3
    for i in range(8):
        a=math.pi*i/4;x=cx+2.0*math.cos(a);y=cy+2.0*math.sin(a)
        w,h=(.65,2.2) if i%4==0 else (2.2,.65)
        rot=None
        if i%2:w,h=2.1,.65;rot={'axis':'z','angle':45 if i in (3,7) else -45,'origin':[x,y,3.55]}
        b([x-w/2,y-h/2,3.2],[w,h,.7],'rust',rot)
    rot={'axis':'z','angle':22.5 if on else 0,'origin':[cx,cy,3.5]}
    b([cx-1.8,cy-.28,3.1],[3.6,.56,.8],'steel',rot)
    b([cx-.28,cy-1.8,3.1],[.56,3.6,.8],'steel',rot)
    b([cx-.55,cy-.55,2.9],[1.1,1.1,1.5],'steel_hi')
    # Depósito rojo con bandas, tapón, conexión de gasolina y manómetro mecánico.
    b([11.35,3.2,2.7],[3.6,4.2,5.6],'shell_red')
    for z in (3.2,7.2):
        b([11.18,3.1,z],[.3,4.5,.55],'steel_dark');b([14.9,3.1,z],[.3,4.5,.55],'steel_dark')
        b([11.3,7.45,z],[3.6,.3,.55],'steel_dark')
    b([12.3,7.4,4.8],[1.8,.7,1.8],'steel');b([12.45,8.1,4.95],[1.5,.3,1.5],'rubber')
    b([10.75,4.7,6.3],[.65,.6,.6],'copper');b([10.75,4.7,6.3],[.6,.6,3.4],'copper')
    b([10.2,7.2,5.1],[1.4,1.8,.6],'steel');b([10.45,7.45,4.85],[.9,1.3,.3],'petro_gauge')
    # Colector / escape hueco: salida a +Y en la esquina posterior derecha.
    b([10.6,8.2,10.9],[1.8,1.5,1.7],'rust')
    b([11.4,8.9,11.3],[1,3.6,1],'steel_dark')
    b([10.75,12.0,10.65],[2.3,1.0,2.3],'steel')
    for x in (11.0,12.3):b([x,13,10.9],[.35,3.0,1.75],'steel_dark')
    for z in (10.9,12.3):b([11.35,13,z],[.95,3.0,.35],'steel_dark')
    # Bornes: aisladores cerámicos, cobre y cubiertas roja/negra de polaridad.
    b([1.4,3.2,11.3],[3.4,2.8,3.5],'steel_dark')
    for x,mat in ((1.6,'shell_red'),(3.4,'black')):
        b([x,6.0,11.8],[1.0,.5,1.7],'ceramic');b([x+.25,6.5,12.2],[.5,.8,.9],'copper')
        b([x-.1,5.0,11.45],[1.2,.8,.3],mat)
    b([4.0,4.0,10.0],[.5,.6,2.1],'copper');b([4.0,4.0,10.0],[1.2,.6,.5],'copper')
    # Panel en el frente: testigo operativo y pulsador, común a las otras máquinas eléctricas.
    b([10.3,2.1,.7],[4.5,2.3,1.7],'steel')
    b([10.8,2.6,.43],[2.6,1.1,.3],'black')
    b([11.1,2.85,.12],[1.4,.55,.32],'petro_green' if on else 'graphite')
    b([13.6,2.8,.4],[.6,.6,.35],'shell_red')
    return e


def generator():
    painters();stage=OUT/'staging';(stage/'models/item').mkdir(parents=True,exist_ok=True)
    display={'firstperson_righthand':{'rotation':[0,45,0],'translation':[0,1,0],'scale':[.45]*3},
             'firstperson_lefthand':{'rotation':[0,45,0],'translation':[0,1,0],'scale':[.45]*3},
             'thirdperson_righthand':{'rotation':[75,45,0],'translation':[0,2.5,0],'scale':[.375]*3},
             'thirdperson_lefthand':{'rotation':[75,45,0],'translation':[0,2.5,0],'scale':[.375]*3},
             'ground':{'translation':[0,3,0],'scale':[.3]*3},'fixed':{'scale':[.6]*3}}
    original=gunkit.ROOT;gunkit.ROOT=stage
    try:
        for on in (False,True):gunkit.model('fuel_generator'+('_on' if on else ''),machine(on),display,group='fuel_generator')
        gunkit.flush()
    finally:gunkit.ROOT=original
    Image.open(stage/'textures/item/gun/fuel_generator.png').save(A/'textures/block/fuel_generator.png')
    for on in (False,True):
        name='fuel_generator'+('_on' if on else '');m=json.loads((stage/f'models/item/{name}.json').read_text())
        m.update(parent='minecraft:block/block',render_type='minecraft:cutout')
        m['textures']={k:'tecnocinetico:block/fuel_generator' for k in m['textures']}
        save(A/f'models/block/{name}.json',m)
        if not on:save(A/'models/item/fuel_generator.json',m)
    save(A/'blockstates/fuel_generator.json',{'variants':{
        f'facing={facing},powered={str(on).lower()}':{'model':'tecnocinetico:block/fuel_generator'+('_on' if on else ''),'y':rot}
        for facing,rot in (('north',0),('east',90),('south',180),('west',270)) for on in (False,True)}})
    fit(['fuel_generator'],'25,225,0')


def tools():
    outline='#242e3b';metal='#617b98';light='#a1b7c6';shade='#3d526b';rubber='#30383d';cyan='#65b9c8'
    for name in TOOLS:
        im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
        if name=='inercita_sword':
            d.line([(3,13),(8,8)],fill=outline,width=3);d.line([(3,13),(8,8)],fill=rubber,width=1)
            d.polygon([(6,8),(10,3),(14,1),(13,5),(9,10)],fill=outline)
            d.polygon([(7,8),(11,3),(13,2),(12,5),(9,9)],fill=metal)
            d.line([(13,2),(12,5),(9,9)],fill=light);d.line([(7,8),(10,5)],fill=shade)
            d.line((5,8,9,12),fill=outline,width=2);d.line((5,8,9,12),fill=metal,width=1)
            d.point((6,9),fill=cyan);d.point((3,13),fill=light)
        else:
            d.line([(2,14),(10,6)],fill=outline,width=3);d.line([(2,14),(10,6)],fill=metal,width=1)
            for x,y in ((3,13),(4,12),(5,11),(6,10)):
                d.line((x-1,y,x+1,y),fill=rubber)
            d.point((2,14),fill=light)
            if name=='inercita_pickaxe':
                d.polygon([(2,3),(5,1),(10,2),(13,4),(14,8),(12,7),(10,5),(5,4),(2,6)],fill=outline)
                d.polygon([(3,3),(5,2),(10,3),(12,4),(13,6),(10,4),(5,3),(3,4)],fill=metal)
                d.line((5,2,10,3,12,4),fill=light);d.line((3,4,5,3),fill=shade)
                d.point((9,4),fill=cyan);d.point((8,4),fill=light)
            elif name=='inercita_axe':
                d.polygon([(7,2),(11,1),(14,3),(14,7),(11,9),(8,7),(7,5),(5,5),(5,3)],fill=outline)
                d.polygon([(8,3),(11,2),(13,4),(13,7),(11,8),(9,6),(8,5)],fill=metal)
                d.line((13,4,13,7,11,8),fill=light);d.line((9,4,11,3),fill=shade);d.point((7,4),fill=cyan)
            elif name=='inercita_shovel':
                d.polygon([(10,1),(14,5),(13,8),(9,10),(6,7),(7,3)],fill=outline)
                d.polygon([(10,2),(13,5),(12,8),(9,9),(7,7),(8,4)],fill=metal)
                d.line((10,2,13,5,12,8),fill=light);d.line((8,4,8,7),fill=shade);d.point((8,8),fill=cyan)
            else:
                d.polygon([(4,2),(12,2),(14,4),(13,7),(11,6),(10,4),(6,4),(4,5)],fill=outline)
                d.polygon([(5,3),(12,3),(13,4),(12,5),(10,3)],fill=metal)
                d.line((5,2,11,2),fill=light);d.point((9,4),fill=cyan)
        im.save(A/f'textures/item/{name}.png')
        save(A/f'models/item/{name}.json',{'parent':'minecraft:item/handheld','textures':{'layer0':f'tecnocinetico:item/{name}'}})


def armor():
    s=make_armor();write_armor(s);item_models(s)
    return s


def ammo():
    source=A/'models/item/kinetic_round.json';m=json.loads(source.read_text())
    assert m['textures']['0']=='tecnocinetico:item/gun/kinetic_round'
    im=Image.open(A/'textures/item/gun/kinetic_round.png').convert('RGBA');changed=0
    for y in range(im.height):
        for x in range(im.width):
            r,g,b,a=im.getpixel((x,y))
            if a and g>r*1.1 and b>r*1.12:
                lum=(r*.21+g*.72+b*.07)/255;v=round(46+lum*132)
                im.putpixel((x,y),(v,v+2,v+6,a));changed+=1
    assert changed>0
    im.save(A/'textures/item/gun/standard_round.png')
    m['textures']={k:'tecnocinetico:item/gun/standard_round' if v=='tecnocinetico:item/gun/kinetic_round' else v for k,v in m['textures'].items()}
    save(A/'models/item/standard_round.json',m)
    return changed


def main():
    OUT.mkdir(parents=True,exist_ok=True);generator();tools();s=armor();changed=ammo()
    save(OUT/'integracion.json',{
        'generator':{'id':'fuel_generator','properties':{'facing':['north','east','south','west'],'powered':[False,True]},
                     'front':'north (-Z), rotaciones 0/90/180/270','render_type':'cutout + noOcclusion',
                     'collision':'Todo el modelo queda dentro de [0,0,0,16,16,16]; colisión de máquina de un bloque.',
                     'smoke_emitter_pixels':[12,16.2,11.8],'smoke_direction':[0,1,0],'smoke_facing':'Girar el punto alrededor de (8,8,8) con el facing del bloque.',
                     'powered':'Testigo verde y radios del volante girados 22.5 grados. Humo, energía y combustible a cargo del código.'},
        'tools':{'ids':TOOLS,'style':'Sprites propios 16x16, todos (incluida espada) usan minecraft:item/handheld y sus transforms vanilla.'},
        'armor':{'model':'armor_models/inercita.json','texture':'textures/models/armor/3d/inercita.png','size':[128,128],
                 'bones_and_slots':'Conservados; el modelo de tercera persona no requiere cambios de Java, pivotes ni registros.',
                 'first_person_worn':'Actualmente no existe RenderArmEvent para Armor3DModels. Para mostrar las mangas puestas en primera persona, Claude debe dibujar rightArm/leftArm del modelo CHEST siguiendo la pose del brazo del jugador. No se implementa aquí; mangas_referencia_primera.png es referencia geométrica, no una captura del juego.',
                 'cubes':sum(len(c) for c in s.slots.values()),'features':'Casco con rendija y cresta, placas macizas, cintura articulada, juntas de goma y amortiguadores; botas con separación real entre pies.'},
        'standard_round':{'kinetic_source_model':'models/item/kinetic_round.json','kinetic_source_texture':'textures/item/gun/kinetic_round.png',
                          'new_model':'models/item/standard_round.json','new_texture':'textures/item/gun/standard_round.png',
                          'package':'kinetic_round ya representa 6 balas. standard_round es su paquete equivalente de 6, misma geometría/UV/display.',
                          'separate_pack_id':None,'changed_pixels':changed,'glow':'Sin cian ni capa emisiva. Latón existente conservado y puntas grises de plomo.'},
        'regeneration':'lote_inercita_gen.py solo regenera este lote. armor3d_gen.py usa el nuevo rig en el set inercita; los demás sets no se cambian.'})
    files=['blockstates/fuel_generator.json','models/block/fuel_generator.json','models/block/fuel_generator_on.json','models/item/fuel_generator.json','textures/block/fuel_generator.png']
    files += [f'{folder}/{n}.{ext}' for n in TOOLS for folder,ext in (('models/item','json'),('textures/item','png'))]
    files += ['armor_models/inercita.json','textures/models/armor/3d/inercita.png']+[f'models/item/inercita_{p}.json' for p in PIECES]
    files += ['models/item/standard_round.json','textures/item/gun/standard_round.png']
    files=['src/main/resources/assets/tecnocinetico/'+n for n in files]
    files += ['tools/lote_inercita_gen.py','tools/preview_lote_inercita.py','tools/inercita_equipment_rig.py','tools/armor3d_gen.py','docs/HERRAMIENTAS.md']
    save(OUT/'archivos.json',files)
    print('Lote: generador de combustible, 5 herramientas planas, armadura 3D y paquete standard_round (6 balas).')


if __name__=='__main__':main()
