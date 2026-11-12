"""Diez modelos de munición/cargadores. Solo arte; no toca los cargadores de pistola.

python tools/ammo_packages_gen.py
Ejecutar DESPUÉS de ammo_gen.py / weapons_gen.py si se usan esos generadores antiguos.
Las capacidades son referencias visuales, no predicados ni lógica de inventario.
"""
import json
import copy
import math
import subprocess
import sys
from pathlib import Path
from PIL import Image,ImageDraw
import gunkit
from gunkit import el,model,flush

ROOT=Path(__file__).resolve().parent.parent
A=ROOT/'src/main/resources/assets/tecnocinetico'
OUT=ROOT/'build/municion'
NAMES=('kinetic_round','guided_round','rifle_round','shotgun_shell','rifle_belt',
       'stripper_clip','short_rifle_magazine','rifle_magazine','belt_box','minigun_box')
COUNTS=(6,6,5,6,15,5,10,30,60,180)
DISPLAY={
    'firstperson_righthand':{'rotation':[0,-55,10],'translation':[-3,4,-3],'scale':[.6,.6,.6]},
    'firstperson_lefthand':{'rotation':[0,-55,10],'translation':[-3,4,-3],'scale':[.6,.6,.6]},
    'thirdperson_righthand':{'rotation':[0,0,0],'translation':[0,2,1],'scale':[.5,.5,.5]},
    'thirdperson_lefthand':{'rotation':[0,0,0],'translation':[0,2,1],'scale':[.5,.5,.5]},
    'ground':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[.5,.5,.5]},
    'fixed':{'rotation':[0,90,0],'translation':[0,0,0],'scale':[.65,.65,.65]},
}

def paper(face,x,y,rng):
    n=rng.randrange(-6,7);v=(126,108,76)
    if x==0 or y==0:n-=19
    return tuple(max(0,min(255,c+n)) for c in v)+(255,)

def guide(face,x,y,rng):
    c=(184,76,215) if (x+y)%4 else (226,145,248)
    return (*c,255)

DIGITS={'0':('111','101','101','101','111'),'1':('010','110','010','010','111'),
        '6':('111','100','111','101','111'),'8':('111','101','111','101','111')}
def label(text):
    def paint(face,x,y,rng):
        n=rng.randrange(-5,6);base=(38+n,43+n,44+n,255)
        if face.name!='north':return base
        width=len(text)*4-1;u=int((x+.5)/face.w*(width+2))-1;v=int((y+.5)/face.h*7)-1
        if 0<=u<width and 0<=v<5:
            ch=u//4;col=u%4
            if ch<len(text) and col<3 and DIGITS[text[ch]][v][col]=='1':return (211,198,141,255)
        return base
    return paint

gunkit.PAINTERS.update(ammo_paper=paper,ammo_guide=guide,label60=label('60'),label180=label('180'))

class Assembly:
    def __init__(self):self.parts=[];self.rounds=0;self.labels={}
    def box(self,x,y,z,w,h,d,mat='steel_dark',rot=None):self.parts.append(el([x,y,z],[x+w,y+h,z+d],mat,rot=rot))
    def cartridge(self,x,y,z,kind='rifle',lying=False,angle=0,scale=1):
        """Cartucho biselado, con culote, hombro, cuello y punta; un solo cartucho por llamada."""
        self.rounds+=1
        rotation={'axis':'y','angle':angle,'origin':[x,y,z+3*scale]} if angle else None
        def box(dx,dy,dz,w,h,d,mat):
            if lying:origin=[x+dx*scale,y+dz*scale,z+dy*scale];size=[w*scale,d*scale,h*scale]
            else:origin=[x+dx*scale,y+dy*scale,z+dz*scale];size=[w*scale,h*scale,d*scale]
            self.box(*origin,*size,mat,rotation)
        def octagon(radius,y0,height,material):
            box(-radius*.72,y0,-radius,radius*1.44,height,radius*2,material)
            box(-radius,y0,-radius*.72,radius*2,height,radius*1.44,material)
        shell=kind=='shell';kinetic=kind in ('kinetic','guided');guided=kind=='guided'
        r=1 if shell else .85 if kinetic else .6
        octagon(r*1.08,0,.24,'brass' if not guided else 'steel')
        octagon(r,.26,3.8 if not shell else 3.9,'shell_red' if shell else 'steel' if guided else 'copper' if kinetic else 'brass')
        if shell:
            octagon(r*1.01,.25,.65,'brass');octagon(r*.9,4.12,.22,'shell_red')
            box(-.45,4.33,-.12,.9,.08,.24,'graphite');return
        octagon(r*.72,4.07,.55,'steel' if guided else 'brass' if not kinetic else 'copper')
        octagon(r*.54,4.6,1.15,'cyan' if kinetic else 'copper')
        box(-r*.28,5.7,-r*.28,r*.56,.65,r*.56,'ammo_guide' if guided else 'cyan' if kinetic else 'steel_hi')
        if guided:octagon(r*1.04,3.05,.32,'ammo_guide')

def kinetic_pack(guided=False):
    a=Assembly()
    for x in (5,8,11):
        for z in (6,9.5):a.cartridge(x,4.1,z,'guided' if guided else 'kinetic')
    a.box(3.6,3.5,4.6,8.8,.65,6.4,'ammo_paper')
    for x in (3.6,12.15):a.box(x,4.1,4.6,.25,2.25,6.4,'ammo_paper')
    a.box(3.6,4.1,10.75,8.8,2.25,.25,'ammo_paper')
    a.box(3.6,4.1,4.6,8.8,.9,.25,'ammo_paper')
    a.box(4.6,4.32,4.39,6.8,.42,.25,'ammo_guide' if guided else 'cyan')
    return a

def rifle_pile():
    a=Assembly()
    # Cinco balas sueltas en dos alturas, ninguna caja ni peine oculto.
    for x,y,z,angle in ((6.3,5.16,4.7,-22.5),(8,5.1,5.4,0),(9.7,5.23,4.8,22.5),
                        (7.15,6.2,4.4,22.5),(9,6.25,5.0,-22.5)):
        a.cartridge(x,y,z,lying=True,angle=angle)
    return a

def shells():
    a=Assembly()
    for row,z in enumerate((6.5,9.2)):
        for x in (5.6,8,10.4):a.cartridge(x,4.4+row*.45,z,'shell')
    # Una faja baja deja visibles los seis cuerpos rojos y sus culotes.
    for z in (5.35,10.25):a.box(4.45,5.4,z,7.1,.7,.2,'ammo_paper')
    for x in (4.45,11.35):a.box(x,5.4,5.35,.2,.7,5.1,'ammo_paper')
    return a

def link(a,x,y,z,previous=None):
    a.box(x-.72,y+2.1,z-.72,1.44,.5,.25,'steel_dark')
    a.box(x-.72,y+2.1,z+.47,1.44,.5,.25,'steel_dark')
    if previous:
        px,py,pz=previous
        a.box(min(px,x)-.08,min(py,y)+2.28,min(pz,z)+.45,abs(px-x)+.16,abs(py-y)+.22,abs(pz-z)+.23,'steel')

def rifle_belt():
    a=Assembly();previous=None
    for i in range(15):
        x=-1.1+i*1.3;y=4.2+.65*math.cos(i*.32);z=8+1.6*math.sin(i*.45)
        a.cartridge(x,y,z);link(a,x,y,z,previous);previous=(x,y,z)
    return a

def stripper_clip():
    a=Assembly()
    for x in (4.8,6.4,8,9.6,11.2):a.cartridge(x,4.6,8)
    a.box(3.85,4.15,7.08,8.3,.35,1.84,'steel')
    for z in (7.08,8.72):a.box(3.85,4.45,z,8.3,.6,.2,'steel_dark')
    for x in (3.85,11.8):a.box(x,4.43,7.05,.35,.9,1.9,'steel_hi')
    return a

def magazine(long=False):
    a=Assembly()
    if not long:
        a.box(6.4,4.1,5.8,3.2,6.8,4.4,'steel_dark')
        for x in (6.28,9.48):
            for z in (6.3,8.9):a.box(x,4.9,z,.24,5.2,.42,'steel')
        a.box(6.15,3.5,5.55,3.7,.7,4.9,'graphite')
        top=10.9;z0=5.8
    else:
        for i,(cy,cz,angle) in enumerate(((12,7,0),(7.9,7.8,-22.5),(3.85,9.35,-22.5))):
            width=3.2-i*.06;rot={'axis':'x','angle':angle,'origin':[8,cy,cz]} if angle else None
            a.box(8-width/2,cy-2.25,cz-2.25,width,4.5,4.5,'steel_dark',rot)
            for x in (8-width/2-.12,8+width/2-.12):
                for z in (cz-1.65,cz+1.1):a.box(x,cy-1.75,z,.24,3.5,.5,'steel',rot)
            if i==2:a.box(6.15,cy-2.45,cz-2.5,3.7,.65,5,'graphite',rot)
        top=14.25;z0=4.75
    a.box(6.48,top-.25,z0+.15,3.04,.3,4.2,'black')
    for x in (6.25,9.35):a.box(x,top-.1,z0,.4,.65,4.5,'steel_hi')
    a.box(7.25,top-.05,z0+1.0,1.5,.2,2.5,'graphite')
    return a

def ammo_box(big=False):
    a=Assembly();x0=2 if big else 3.2;y0=1.7 if big else 3.1;z0=3.5 if big else 4.5
    w,h,d=(11.5,10,9) if big else (9.2,7,7)
    a.box(x0,y0,z0,w,h,d,'inercita' if big else 'olive')
    a.box(x0-.2,y0+h,z0-.2,w+.4,.55,d+.4,'steel_dark')
    a.box(x0-.12,y0-.4,z0-.12,w+.24,.5,d+.24,'steel_dark')
    for x in (x0+.65,x0+w-1.05):
        a.box(x,y0-.1,z0-.18,.4,h+.4,d+.36,'steel_dark')
        a.box(x-.12,y0+h-.6,z0-.4,.65,1.1,.4,'steel')
    for x in (x0+2.25,x0+w-2.85):a.box(x,y0+h+.45,z0+d/2-.35,.6,1.2,.7,'steel')
    a.box(x0+2.25,y0+h+1.3,z0+d/2-.45,w-4.5,.5,.9,'rubber')
    a.labels[len(a.parts)]='180' if big else '60'
    a.box(x0+2.2,y0+h*.42,z0-.25,4 if big else 3.1,1.8,.2,'label180' if big else 'label60')
    if big:
        for x in (x0+.1,x0+w-1.0):a.box(x,y0+.6,z0-.32,.9,2.5,.3,'hazard')
    # Ventana lateral y tramo de cinta saliente: las balas son transversales al avance.
    a.box(x0+w-.2,y0+h-2,z0+1.2,.5,1.5,4.8,'black')
    previous=None
    for i in range(7 if big else 5):
        x=x0+w+.25+(1.5*math.sin(i*.35) if big else i*.95)
        y=y0+h-1-(i*1.1 if big else -.6*math.sin(i*.65))
        z=z0+1.2;scale=.78
        a.cartridge(x,y,z,lying=True,scale=scale)
        # Eslabones que rodean el culote y enlazan la caída/curva del tramo.
        a.box(x-.58,y-.6,z+1.5,1.16,1.2,.28,'steel_dark')
        if previous:
            px,py=previous;a.box(min(px,x)-.18,min(py,y)-.1,z+1.67,abs(px-x)+.36,abs(py-y)+.2,.26,'steel')
        previous=(x,y)
    return a

def crisp_labels(name,assembly):
    """Reservar texeles para dígitos legibles aunque el resto del atlas use menor densidad."""
    if not assembly.labels:return
    texture=A/f'textures/item/gun/{name}.png';im=Image.open(texture).convert('RGBA');draw=ImageDraw.Draw(im)
    path=A/f'models/item/{name}.json';m=json.loads(path.read_text(encoding='utf-8'));size=im.width
    for index,text in assembly.labels.items():
        target=None
        for y in range(size-7):
            for x in range(size-15):
                if im.crop((x,y,x+16,y+8)).getchannel('A').getbbox() is None:target=(x,y);break
            if target:break
        if target is None:raise ValueError(f'Sin espacio de etiqueta en {name}')
        x,y=target;draw.rectangle((x,y,x+15,y+7),fill=(29,35,37,255));width=len(text)*4-1;left=x+(16-width)//2
        for i,ch in enumerate(text):
            for yy,row in enumerate(DIGITS[ch]):
                for xx,pixel in enumerate(row):
                    if pixel=='1':draw.point((left+i*4+xx,y+1+yy),fill=(223,211,162,255))
        m['elements'][index]['faces']['north']['uv']=[x*16/size,y*16/size,(x+16)*16/size,(y+8)*16/size]
    im.save(texture);path.write_text(json.dumps(m,indent=2)+'\n',encoding='utf-8')

def generate(names=NAMES):
    OUT.mkdir(parents=True,exist_ok=True)
    builders={'kinetic_round':lambda:kinetic_pack(False),'guided_round':lambda:kinetic_pack(True),
              'rifle_round':rifle_pile,'shotgun_shell':shells,'rifle_belt':rifle_belt,'stripper_clip':stripper_clip,
              'short_rifle_magazine':lambda:magazine(False),'rifle_magazine':lambda:magazine(True),
              'belt_box':lambda:ammo_box(False),'minigun_box':lambda:ammo_box(True)}
    report={};assemblies={}
    for name in names:
        a=builders[name]();assemblies[name]=a;display=copy.deepcopy(DISPLAY)
        if name=='minigun_box':
            for hand in ('firstperson_righthand','firstperson_lefthand'):display[hand]['translation'][1]=6.5
        if name in NAMES[:6]:assert a.rounds==COUNTS[NAMES.index(name)]
        model(name,a.parts,display,group=name)
        report[name]={'capacity_reference':COUNTS[NAMES.index(name)],'visible_cartridges':a.rounds,'elements':len(a.parts)}
    flush()
    for name in names:
        crisp_labels(name,assemblies[name])
        subprocess.run([sys.executable,str(ROOT/'tools/gui_fit.py'),str(A),'y-135;x30',name],check=True,stdout=subprocess.DEVNULL)
        m=json.loads((A/f'models/item/{name}.json').read_text(encoding='utf-8'))
        assert 'overrides' not in m
        from preview_weapon_remodel import rotated
        for e in m['elements']:
            assert all(-16<=n<=32 for k in ('from','to') for n in e[k])
            for x in (e['from'][0],e['to'][0]):
                for y in (e['from'][1],e['to'][1]):
                    for z in (e['from'][2],e['to'][2]):assert all(-16<=n<=32 for n in rotated([x,y,z],e.get('rotation')))
        im=Image.open(A/f'textures/item/gun/{name}.png');assert all(v<=128 and v&(v-1)==0 for v in im.size)
        report[name]['texture_size']=m['texture_size']
    (OUT/'manifest.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    print('Generados:',', '.join(names))

if __name__=='__main__':generate()
