"""Petróleo: fluidos, materiales, dos máquinas y motosierra. Solo arte original.
python tools/petroleum_art.py
No cambia Java, registros, recetas, interfaces ni otros modelos del arsenal.
"""
import copy,json,math,random,subprocess,sys
from pathlib import Path
from PIL import Image,ImageDraw
import gunkit
from combustion_drill_gen import DISPLAY as DRILL_DISPLAY

ROOT=Path(__file__).resolve().parent.parent
A=ROOT/'src/main/resources/assets/tecnocinetico'
OUT=ROOT/'build/petroleo'
FILES=set()
FLUIDS={'crude_oil':{'rgb':(27,23,19),'alpha':255,'still_time':16,'flow_time':12},
        'gasoline':{'rgb':(220,188,97),'alpha':165,'still_time':5,'flow_time':4},
        'kinetic_fuel':{'rgb':(35,153,161),'alpha':215,'still_time':5,'flow_time':3}}


def track(path):
    FILES.add(str(path.relative_to(ROOT)).replace('\\','/'));path.parent.mkdir(parents=True,exist_ok=True);return path


def save(path,data):track(path).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')


def fluids():
    """Ruido suave y advección periódicos: manchas irregulares, sin bandas sinusoidales."""
    for name,cfg in FLUIDS.items():
        rng=random.Random(name)
        grids={n:[[rng.random() for _ in range(n)] for _ in range(n)] for n in (2,3,4,7,11)}
        def noise(n,x,y):
            x*=n;y*=n;ix,iy=math.floor(x),math.floor(y);fx,fy=x-ix,y-iy
            fx=fx*fx*(3-2*fx);fy=fy*fy*(3-2*fy);g=grids[n]
            top=g[iy%n][ix%n]*(1-fx)+g[iy%n][(ix+1)%n]*fx
            bottom=g[(iy+1)%n][ix%n]*(1-fx)+g[(iy+1)%n][(ix+1)%n]*fx
            return top*(1-fy)+bottom*fy
        for flowing,n,count in ((False,16,8),(True,32,4)):
            im=Image.new('RGBA',(n,n*count));kind='flow' if flowing else 'still'
            for frame in range(count):
                phase=2*math.pi*frame/count
                for y in range(n):
                    for x in range(n):
                        xx=x/n+.075*math.sin(phase);yy=y/n+(.06*math.cos(phase) if not flowing else -frame/count)
                        xx+=noise(3,xx,yy)*.13;yy+=noise(2,xx,yy)*.12
                        wave=2*(noise(2,xx,yy)*.35+noise(4,xx,yy)*.4+noise(7,xx,yy)*.25)-1
                        grain=(noise(11,xx,yy)-.5)*.045
                        k=1+wave*.18+grain
                        rgb=[round(v*k) for v in cfg['rgb']]
                        if name=='crude_oil':
                            sheen=max(0,noise(3,xx,yy)-.5)
                            rgb=[v+round(sheen*p) for v,p in zip(rgb,(24,19,30))]
                            if wave>.25:rgb=[v+round((wave-.25)*p) for v,p in zip(rgb,(22,15,9))]
                        elif name=='kinetic_fuel':
                            glint=max(0,noise(11,xx,yy)-.70)*3.2
                            rgb=[round(v+(240-v)*glint*.65) for v in rgb]
                        im.putpixel((x,frame*n+y),tuple(min(255,max(0,v)) for v in rgb)+(cfg['alpha'],))
            path=A/f'textures/block/{name}_{kind}.png';im.save(track(path))
            save(Path(str(path)+'.mcmeta'),{'animation':{'width':n,'height':n,'frametime':cfg[kind+'_time'],'interpolate':True}})


def flat_items():
    for name,cfg in FLUIDS.items():
        im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
        # Cubo original: asa arqueada, borde grueso, costados de acero doblado.
        d.line([(3,5),(3,3),(5,1),(10,1),(12,3),(12,5)],fill='#424b51',width=1)
        d.line([(4,4),(4,3),(6,2),(9,2),(11,4)],fill='#bfc6c4')
        d.polygon([(1,5),(3,4),(12,4),(14,5),(13,12),(11,14),(4,14),(2,12)],fill='#30383e')
        d.polygon([(2,6),(13,6),(12,12),(10,13),(5,13),(3,11)],fill='#8c9697')
        d.polygon([(3,7),(6,8),(6,12),(4,11)],fill='#bbc0b7')
        d.polygon([(11,7),(13,6),(12,12),(10,13)],fill='#5d6c72')
        d.line((5,13,10,13),fill='#c9cbc0')
        liquid=cfg['rgb'];d.polygon([(3,5),(11,5),(13,6),(10,7),(5,7),(2,6)],fill=liquid)
        d.line((3,5,10,5),fill=tuple(min(255,v+48) for v in liquid))
        d.point((5,6),fill=tuple(min(255,v+30) for v in liquid))
        write_flat(name+'_bucket',im)
    im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
    for points in ([(1,11),(3,7),(7,7),(9,10),(8,14),(3,14)],[(6,7),(8,3),(12,3),(15,7),(13,11),(8,11)]):
        d.polygon(points,fill='#191a19');d.line(points+[points[0]],fill='#292b29')
    d.polygon([(8,5),(11,4),(13,6),(11,6),(10,8),(7,8)],fill='#4f514b')
    d.line((3,9,5,8,7,9),fill='#69695c');d.point((9,5),fill='#96957d')
    d.line((2,12,5,13,7,12),fill='#353831');write_flat('tar',im)
    # Recipientes pequeños = sprites: bidón rojo reconocible y célula tecnológica.
    # el fuel_canister conserva su modelo 3D original (weapons_gen.py): el usuario no pidió cambiarlo
    for name in ('kinetic_fuel_cell',):
        im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
        if name=='fuel_canister':
            d.rectangle((6,1,10,3),fill='#332e29');d.rectangle((7,2,9,3),fill=(0,0,0,0))
            d.rectangle((3,2,5,3),fill='#837c64');d.line((3,2,5,2),fill='#d0c3a0')
            d.polygon([(3,4),(11,3),(13,5),(13,13),(11,15),(3,15),(2,13),(2,6)],fill='#392923')
            d.polygon([(4,4),(10,4),(11,5),(11,13),(3,13),(3,6)],fill='#a34731')
            d.polygon([(11,5),(12,6),(12,13),(11,14)],fill='#64372c')
            d.line((4,5,10,12),fill='#d06c46');d.line((10,5,4,12),fill='#703426')
            d.rectangle((5,7,9,10),fill='#c3a35c');d.polygon([(7,7),(6,9),(7,10),(8,9)],fill='#42392d')
            d.line((3,14,10,14),fill='#754732');d.point((4,5),fill='#c18c5d')
        else:
            d.rectangle((5,1,10,3),fill='#3c4544');d.line((6,1,9,1),fill='#aab4a5')
            d.polygon([(4,3),(11,3),(13,5),(13,13),(11,15),(3,15),(2,13),(2,5)],fill='#253534')
            d.rectangle((3,5,11,12),fill='#6c7c76');d.rectangle((11,5,12,12),fill='#404f4d')
            d.rectangle((5,5,9,12),fill='#217f8b');d.line((5,5,5,11),fill='#78e0d4')
            d.line((6,6,8,6),fill='#4cb9b7');d.point((8,10),fill='#b5f1da')
            for y in (4,12):d.line((3,y,11,y),fill='#b5baa2')
            for x in (4,7,10):d.rectangle((x,13,x+1,14),fill='#b79045')
        write_flat(name,im)


def write_flat(name,im):
    im.save(track(A/f'textures/item/{name}.png'))
    save(A/f'models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'tecnocinetico:item/{name}'}})


def machine(name,on=False):
    e=[]
    def b(x,y,z,w,h,d,mat='steel_dark',rot=None):e.append(gunkit.el([x,y,z],[x+w,y+h,z+d],mat,rot=rot))
    b(.5,.2,.5,15,2.3,15,'steel_dark')
    for x in (.7,13.3):
        for z in (.7,13.3):
            b(x,0,z,2,.35,2,'steel');b(x+.65,2.5,z+.65,.7,.35,.7,'steel_hi')
    b(9.5,2.5,.8,5,3.5,2.4,'steel')
    b(10,3, .58,3.2,1.6,.25,'black')
    b(10.3,3.3,.28,1.6,.75,.32,'petro_green' if on else 'graphite')
    b(12.5,3.5,.27,.5,.5,.33,'shell_red')
    b(9.8,2.65,.46,4,.3,.15,'hazard')
    if name=='oil_pump':
        # Entrada al pozo: tubo continuo hasta Y=0, junta, prensaestopas y vástago.
        angle=22.5 if on else -22.5
        rodx=8-4.8*math.cos(math.radians(angle));rody=12-4.8*math.sin(math.radians(angle))
        b(rodx-1.2,.1,7.25,2.4,3.9,2.4,'black');b(rodx-1.7,2.55,6.75,3.4,.6,3.4,'rust')
        b(rodx-.65,3.15,7.8,1.3,2.1,1.3,'steel')
        b(rodx-.26,5.1,8.18,.52,rody-5.1,.52,'steel_hi')
        for z in (6.4,10.2):
            b(7.1,2.5,z,1.8,9.3,1.2,'steel')
            b(6.6,2.5,z-.25,2.8,1.1,1.7,'rust')
            b(7.65,4,z-.2,.7,6.2,.25,'hazard')
        b(6.5,11.3,6,3,1.4,6,'steel_dark')
        rot={'origin':[8,12,8.5],'axis':'z','angle':angle}
        b(2.5,11.5,7.2,11,.95,2.6,'olive',rot)
        b(2.15,10.8,6.8,1.8,2.4,3.4,'rust',rot)
        b(11.6,10.7,6.7,2.1,2.5,3.6,'steel_dark',rot)
        b(5,12.46,7.6,5,.3,1.8,'hazard',rot)
        # Motor eléctrico, aletas, reductor y disco excéntrico.
        b(10.3,2.5,10.4,4.2,3.2,4.7,'graphite')
        for i in range(5):b(10.1,2.8,10.7+i*.75,4.6,3,.28,'steel')
        b(10.8,3.1,9.4,3.2,2.3,1,'rust')
        b(11.6,4.6,8.8,1.5,3.5,.55,'steel_dark')
        b(11.9,7.1,9.4,.8,2.5,.8,'copper')
        # Acumulador lateral con bandas y manómetro.
        b(1.5,2.5,1.8,3.3,4.8,3.5,'olive')
        for y in (3,6.5):b(1.3,y,1.6,3.7,.45,3.9,'steel_dark')
        b(2.1,7.3,2.4,2.1,.6,2.3,'steel')
        b(4.8,4.8,3.1,1.9,.7,.7,'copper');b(5.9,4.8,3.1,.8,1.9,.7,'copper')
        b(5.45,6.7,2.75,1.8,1.8,.6,'steel_hi');b(5.7,6.95,2.56,1.3,1.3,.23,'petro_gauge')
    else:
        # Columna octogonal escalonada, platos de destilación y calentador aislado.
        b(5.2,2.5,7.7,5.6,3,6.3,'graphite')
        b(5.6,5.5,8.1,4.8,9,5.5,'steel')
        b(5.1,5.7,8.65,5.8,8.6,4.4,'steel')
        for y in (5.3,8.2,11.1,14.3):b(4.95,y,7.8,6.1,.55,6.1,'steel_dark')
        for x in (5.25,10.35):b(x,6.2,7.65,.4,7.5,.4,'copper')
        b(6,14.85,8.8,4,1,4.1,'steel_dark')
        b(6.5,3.2,7.42,2.6,1.4,.32,'petro_heat' if on else 'black')
        for x,mat,h in ((1.2,'olive',5),(11.25,'steel',6.7)):
            b(x,2.5,2.7,3.8,h,4.0,mat)
            for y in (3.0,2.5+h-.7):b(x-.15,y,2.55,4.1,.45,4.3,'steel_dark')
            b(x+.5,2.5+h,3.3,2.8,.5,2.8,'steel')
            b(x+1.45,3.6,2.42,.9,h-1.6,.32,'petro_oil' if x<5 else 'petro_gas')
        # Cuello de cisne, intercambiador y tubos conectados en escuadra.
        b(8.7,15.05,10.0,5.9,.65,.65,'copper');b(13.95,10.2,10,.65,4.85,.65,'copper')
        for i in range(4):
            y=6.2+i*1.0;b(12.2,y,12,2.5,.45,.55,'copper')
            if i<3:b(12.2 if i%2==0 else 14.25,y+.45,12,.45,.55,.55,'copper')
        b(12.2,6.2,6.7,.55,.55,5.3,'copper');b(12.2,9.65,10.0,2.4,.55,2.2,'copper')
        b(3.15,7.6,4.2,.7,3.9,.7,'copper');b(3.15,10.8,4.2,2.7,.7,.7,'copper')
        b(5.15,10.8,4.2,.7,.7,3.9,'copper')
        b(3,9.0,3.9,1.3,.45,1.3,'shell_red')
        b(1,2.6,10.4,2.7,4,3.8,'steel_dark')
        for y in (3.1,4.1,5.1):b(.8,y,10.65,.3,.4,3.3,'steel')
    return e


def chainsaw(running=False):
    e=[];chain=[]
    def b(x,y,z,w,h,d,mat='steel_dark',rot=None,track_chain=False):
        e.append(gunkit.el([x,y,z],[x+w,y+h,z+d],mat,rot=rot))
        if track_chain:chain.append(len(e)-1)
    # Cárter, depósito y tapa lateral del arrancador. Espada hacia -Z.
    b(5.2,8.1,5.0,5.6,4.7,7,'graphite');b(5,7.2,6,6,1.3,6.1,'shell_red')
    b(5.15,8.8,5.4,5.7,4.6,5.6,'shell_red')
    b(4.4,9.1,6.0,.9,3.5,4.7,'steel_dark');b(4.1,9.6,6.45,.35,2.5,3.8,'shell_red')
    for z in (6.8,7.7,8.6,9.5):b(3.95,10,z,.2,1.6,.23,'black')
    b(4.3,12.3,9.4,.35,1.5,.35,'leather');b(3.6,13.5,9.05,1.8,.5,1.0,'rubber')
    b(6.1,12.6,7,3.8,2.5,4,'steel_dark')
    for y in (12.8,13.3,13.8,14.3):b(5.8,y,6.7,4.4,.2,4.5,'steel')
    b(7.75,15.1,8.8,.7,.9,.7,'ceramic');b(8,15.85,9.0,2,.3,.35,'rubber')
    b(10.1,10.8,6.2,1.5,2.8,4.6,'rust')
    for z in (6.7,7.7,8.7,9.7):b(11.65,11.2,z,.2,1.9,.3,'steel_dark')
    b(10.5,11,10.8,2.3,1,1.3,'black')
    b(10.4,8.15,10.4,.8,.9,1.1,'steel');b(11.15,8.25,10.5,.25,.7,.9,'rubber')
    # Agarre trasero hueco y gatillo, mano principal como en el taladro.
    b(6.6,10.0,11.7,2.8,1.6,5.2,'steel_dark')
    b(6.75,10.25,12.6,2.5,1.2,3.9,'rubber')
    b(6.6,7.3,15.7,2.8,2.7,1.2,'steel_dark');b(6.6,7.3,11.6,2.8,.6,4.1,'steel_dark')
    b(7.7,9.7,12,.6,.8,1.5,'shell_red')
    # Asa auxiliar envolvente y escudo de freno delante de los dedos.
    for x in (3.5,11.8):b(x,10,4.8,.7,6.5,.85,'steel_dark')
    b(3.5,15.9,4.65,9,.85,1.1,'steel')
    b(4.5,15.8,4.5,7,1.1,1.4,'rubber')
    b(4.7,12.7,3.5,6.6,3.2,.6,'steel_dark')
    b(5.1,13.2,3.35,5.8,1.2,.25,'hazard')
    # Cárter del piñón a la derecha, tuercas y espada larga de acero laminado.
    b(7,9.5,3.8,3.5,4.1,3.2,'steel_dark')
    b(7.65,10.25,-10.5,.7,3.2,15.1,'steel_hi')
    b(7.65,10.65,-11.8,.7,2.4,1.4,'steel_hi')
    b(7.65,11.15,-12.5,.7,1.4,.8,'steel_hi')
    for x in (7.48,8.35):
        b(x,11.4,-10.1,.17,.85,13.8,'steel')
        for z in (-9.3,-6.3,-3.3,-.3,2.7):b(x-.025,11.65,z,.22,.35,.65,'graphite')
    for y in (9.9,13.45):b(7.35,y,-10.55,1.3,.35,15.2,'steel_dark',track_chain=True)
    b(7.35,10.25,-12.05,1.3,3.2,.38,'steel_dark',track_chain=True)
    b(7.35,10.8,-12.75,1.3,2.1,.7,'steel_dark',track_chain=True)
    b(7.2,11.45,-13.2,.8,.7,.45,'steel_hi')
    for y,angle in ((10.25,-45),(12.8,45)):
        b(7.2,y,-12.55,.8,.5,.7,'steel_hi',{'axis':'x','angle':angle,'origin':[8,y+.25,-12.2]})
    # Dientes de corte alternos; el modelo en marcha desplaza el paso media cadena.
    for i in range(9):
        z=-10.2+i*1.7+(.65 if running else 0)
        if z>4:continue
        b(7.2 if i%2==0 else 8.0,13.8,z,.8,.5,.65,'steel_hi')
        b(8.0 if i%2==0 else 7.2,9.4,z+.25,.8,.5,.65,'steel_hi')
    for z in (4.3,6.0):b(10.52,10.6,z,.25,.65,.65,'steel_hi')
    return e,chain


def chain_texture():
    for running,count in ((False,1),(True,4)):
        im=Image.new('RGBA',(16,16*count))
        for frame in range(count):
            for y in range(16):
                for x in range(16):
                    u=(x+frame)%4;k=1 if u in (0,1) else .4
                    rgb=tuple(round(a*k) for a in (131,139,131))
                    if y%4==0:rgb=(41,45,42)
                    im.putpixel((x,frame*16+y),rgb+(255,))
        p=A/f'textures/item/gun/chainsaw_chain{"_running" if running else ""}.png';im.save(track(p))
        if running:save(Path(str(p)+'.mcmeta'),{'animation':{'frametime':1,'width':16,'height':16,'interpolate':False}})


def painters():
    palette={'petro_green':(91,204,132),'petro_heat':(246,121,40),'petro_oil':(36,29,24),'petro_gas':(213,175,85)}
    for mat,col in palette.items():
        def paint(face,x,y,rnd,c=col):
            k=1.15 if x==0 or y==0 else .85 if x==face.w-1 else 1
            return tuple(min(255,round(v*k)) for v in c)+(255,)
        gunkit.PAINTERS[mat]=paint
    def gauge(face,x,y,rnd):
        if x==y or (x==face.w//2 and y>face.h//2):return (47,45,36,255)
        return (194,188,155,255)
    gunkit.PAINTERS['petro_gauge']=gauge


def models():
    painters();stage=OUT/'staging';(stage/'models/item').mkdir(parents=True,exist_ok=True)
    original=gunkit.ROOT;gunkit.ROOT=stage
    blockdisplay={'gui':{'rotation':[25,225,0],'scale':[.7]*3},'ground':{'translation':[0,3,0],'scale':[.35]*3},
                  'fixed':{'scale':[.6]*3},'firstperson_righthand':{'rotation':[0,45,0],'scale':[.45]*3},
                  'firstperson_lefthand':{'rotation':[0,45,0],'scale':[.45]*3},
                  'thirdperson_righthand':{'rotation':[75,45,0],'translation':[0,2.5,0],'scale':[.375]*3},
                  'thirdperson_lefthand':{'rotation':[75,45,0],'translation':[0,2.5,0],'scale':[.375]*3}}
    try:
        for name in ('oil_pump','refinery'):
            for on in (False,True):gunkit.model(name+('_on' if on else ''),machine(name,on),blockdisplay,group=name)
        display=copy.deepcopy(DRILL_DISPLAY)
        for side in ('righthand','lefthand'):
            display['firstperson_'+side]={'rotation':[0,8,0],'translation':[0,.5,-2],'scale':[.65]*3}
        for running in (False,True):
            els,indices=chainsaw(running)
            gunkit.model('chainsaw_running' if running else 'chainsaw',els,display,
                None if running else [{'predicate':{'tecnocinetico:chainsaw_run':1},'model':'tecnocinetico:item/chainsaw_running'}],group='chainsaw')
        gunkit.flush()
    finally:gunkit.ROOT=original
    for name in ('oil_pump','refinery'):
        Image.open(stage/f'textures/item/gun/{name}.png').save(track(A/f'textures/block/{name}.png'))
        for on in (False,True):
            id=name+('_on' if on else '');m=json.loads((stage/f'models/item/{id}.json').read_text())
            m['parent']='minecraft:block/block';m['render_type']='minecraft:cutout'
            m['textures']={k:f'tecnocinetico:block/{name}' for k in m['textures']}
            save(A/f'models/block/{id}.json',m)
            if not on:
                # Objeto 3D completo: gui_fit opera sobre la geometría real.
                save(A/f'models/item/{name}.json',m)
        variants={}
        for face,rot in (('north',0),('east',90),('south',180),('west',270)):
            for powered in (False,True):
                variants[f'facing={face},powered={str(powered).lower()}']={'model':f'tecnocinetico:block/{name}'+('_on' if powered else ''),'y':rot}
        save(A/f'blockstates/{name}.json',{'variants':variants})
    for running in (False,True):
        id='chainsaw_running' if running else 'chainsaw';m=json.loads((stage/f'models/item/{id}.json').read_text())
        indices=chainsaw(running)[1];m['textures']['chain']='tecnocinetico:item/gun/chainsaw_chain'+('_running' if running else '')
        for i in indices:
            for f in m['elements'][i]['faces'].values():f.update(texture='#chain',uv=[0,0,16,2])
        save(A/f'models/item/{id}.json',m)
    Image.open(stage/'textures/item/gun/chainsaw.png').save(track(A/'textures/item/gun/chainsaw.png'))
    chain_texture()
    for args in [('25,225,0','oil_pump'),('25,225,0','refinery'),('y-65;x25','chainsaw','chainsaw_running')]:
        subprocess.run([sys.executable,str(ROOT/'tools/gui_fit.py'),str(A),args[0],'fill=14',*args[1:]],check=True)


def main():
    OUT.mkdir(parents=True,exist_ok=True);fluids();flat_items();models()
    save(OUT/'integracion.json',{
        'machines':{'ids':['oil_pump','refinery'],'properties':{'facing':['north','east','south','west'],'powered':[False,True]},
                    'models':'models/block/<id>.json y <id>_on.json','render_type':'cutout, noOcclusion',
                    'bounds_pixels':[0,0,0,16,16,16],'front':'north (-Z); rotaciones 0/90/180/270',
                    'collision':'Sugerencia: base [0,0,0,16,3,16] y cuerpo [1,3,1,15,16,15]. No precisa bloque entidad para dibujarse.',
                    'powered_visual':'Bomba: posición distinta de balancín y pistón + testigo verde. Refinería: calentador naranja y testigo verde.',
                    'gui':'Pendiente de menús; no se han creado interfaces.'},
        'fluids':{n:{'still':f'tecnocinetico:block/{n}_still','flow':f'tecnocinetico:block/{n}_flow',
                      'texture_size_still':[16,128],'texture_size_flow':[32,128],
                      'frames_still':8,'frames_flow':4,'tint':'0xFFFFFFFF: texturas ya coloreadas, no multiplicar otro tinte',
                      'render_layer':'translucent','suggested_light':3 if n=='kinetic_fuel' else 0} for n in FLUIDS},
        'buckets':'<fluid>_bucket: sprites item/generated 16x16 propios. Si el registro aplica Forge fluid_container, usar estos modelos sin sobreescribirlos con el loader dinámico.',
        'flat_items':['tar','kinetic_fuel_cell'],
        'chainsaw':{'base':'tecnocinetico:item/chainsaw','running':'tecnocinetico:item/chainsaw_running',
                    'predicate':'tecnocinetico:chainsaw_run, 0 parada, 1 en marcha. Mantener 1 mientras trabaja: la textura se anima sola.',
                    'animated_texture':'item/gun/chainsaw_chain_running, 4 frames de 1 tick, bucle sin interpolación',
                    'axis':'-Z, filo vertical; vistas niveladas con convergencia de 8 grados hacia dentro',
                    'firstperson':'Validada con traslación de reposo (±0.5,-0.5,-0.66), igual al taladro. El código debe usar esa pose estable y omitir el golpe vanilla al talar.',
                    'thirdperson':'Transforms del taladro: pose de dos manos CROSSBOW_HOLD recomendada; no cambia la lógica.'},
        'legacy_generator':'El fuel_canister es el 3D de weapons_gen.py; este generador ya no lo toca.'})
    FILES.add('tools/petroleum_art.py');FILES.add('tools/preview_petroleum.py');FILES.add('docs/HERRAMIENTAS.md')
    save(OUT/'archivos.json',sorted(FILES))
    print('Arte petróleo generado: fluidos, 6 sprites, 2 máquinas y motosierra parada/en marcha.')


if __name__=='__main__':main()
