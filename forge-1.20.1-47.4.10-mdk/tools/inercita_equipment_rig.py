"""Armadura industrial de Inercita; contrato Armor3DModels intacto, sin Java.
Usada por lote_inercita_gen.py y por el set inercita de armor3d_gen.py.
"""
import json,math,random
from types import SimpleNamespace
from PIL import Image
from armor3d_gen import Cube,ROOT,fix_zfight,regions
from nether_remodel_rigs import Rig
from nether_remodel_gen import pack,key

COLORS={'plate':(81,100,128),'edge':(145,164,179),'dark':(35,45,58),'rubber':(39,44,46),
        'cloth':(51,61,69),'rod':(129,141,147),'cyan':(76,180,193),'visor':(19,31,40),'mark':(157,168,169)}


def make_armor():
    s=SimpleNamespace(name='inercita',pal=COLORS,slots={n:[] for n in ('head','chest','legs','feet')})
    def b(slot,bone,f,size,mat,pair=False):
        c=Cube(bone,f,[max(1,round(v)) for v in size],mat);c.s=list(size);s.slots[slot].append(c)
        if pair:
            other={'right_arm':'left_arm','right_leg':'left_leg'}[bone]
            b(slot,other,[-f[0]-size[0],f[1],f[2]],size,mat)
    # Casco envolvente, rendija protegida y cresta de refuerzo (no casco medieval).
    for f,size,mat in [([-4.7,-8.9,-4.7],[9.4,1.2,9.4],'plate'),([-4.3,-7.7,3.8],[8.6,7.0,1.15],'dark'),
                       ([-4.3,-7.7,-5.0],[8.6,1.55,1.1],'plate'),([-3.8,-6.05,-5.1],[7.6,1.15,.45],'visor'),
                       ([-3.8,-2.8,-5.0],[7.6,1.4,1.1],'plate'),([-.6,-10.1,-3.0],[1.2,1.2,6.6],'dark'),
                       ([-.35,-10.3,-2.7],[.7,.23,5.8],'edge'),([-1.4,-4.65,-5.25],[2.8,1.6,.6],'dark'),
                       ([-3.6,-6.18,-5.25],[7.2,.25,.35],'edge'),([1.9,-5.85,-5.59],[1.2,.25,.15],'cyan')]:b('head','head',f,size,mat)
    for side in (-1,1):
        b('head','head',[side*4.45-.4,-7.65,-4.25],[.8,6.95,8.6],'plate')
        b('head','head',[side*3.5-1.0,-4.85,-5.05],[2,2.0,.95],'dark')
        b('head','head',[side*4.7-.35,-5.3,-1.8],[.7,2.8,3.6],'rubber')
        b('head','head',[side*4.95-.22,-5.0,-1.3],[.44,2.2,2.6],'plate')
        for y in (-7.0,-1.6):b('head','head',[side*4.6-.24,y,-3.2],[.48,.48,.45],'edge')
    for x in (-.9,0,.9):b('head','head',[x-.15,-4.3,-5.9],[.3,.8,.1],'visor')
    # Coraza laminada, articulación de cintura y cinturón separado de los brazos.
    for f,size,mat in [([-4.2,.25,-2.3],[8.4,10.8,4.6],'cloth'),([-4.6,1,-3.8],[9.2,5.15,1.5],'plate'),
                       ([-4.4,6.45,-3.4],[8.8,1.85,1.2],'dark'),([-4.1,8.65,-3.2],[8.2,1.8,1.1],'plate'),
                       ([-4.5,1.1,-3.95],[9,.55,.2],'edge'),([-3.75,5.8,-3.95],[7.5,.4,.2],'edge'),
                       ([-1.7,2.45,-4.0],[3.4,1.9,.3],'dark'),([-.8,2.95,-4.34],[1.6,.4,.15],'cyan'),
                       ([-4.4,1.0,2.45],[8.8,8.7,1.1],'plate'),([-1.8,1.5,3.6],[3.6,6.8,.5],'dark')]:b('chest','body',f,size,mat)
    for side in (-1,1):
        x=side*2.8-.6
        b('chest','body',[x,2.0,3.65],[1.2,5.6,.9],'rubber')
        b('chest','body',[x+.3,2.6,4.5],[.6,4.2,.4],'rod')
        for y in (1.7,7.4):b('chest','body',[x-.15,y,3.5],[1.5,.65,1.3],'edge')
        b('chest','body',[side*4.35-.45,7.0,-1.6],[.9,2.8,3.2],'rubber')
    # Amortiguadores exteriores, codo libre y guantes sin cubrir los dedos de la mano.
    for f,size,mat in [([-3.15,-1.6,-2.2],[4.3,10.8,4.4],'cloth'),([-3.8,-2.8,-2.8],[5.2,3.6,5.6],'plate'),
                       ([-3.85,-3.2,-2.25],[5.3,.45,4.5],'dark'),([-3.85,-2.75,-2.96],[5.3,.6,.22],'edge'),
                       ([-3.35,4.4,-2.5],[4.7,4.1,5.0],'plate'),([-2.5,4.2,-3.15],[3.5,2.2,.7],'dark'),
                       ([-3.3,8.8,-2.4],[4.6,.6,4.8],'rubber'),([-4.1,.9,-.7],[.55,5,.9],'rubber'),
                       ([-4.25,1.3,-.4],[.35,3.6,.4],'rod'),([-2.7,6.5,-2.7],[1.0,.4,.22],'cyan')]:b('chest','right_arm',f,size,mat,True)
    # Perneras: se conserva espacio real en la unión central y detrás de las rodillas.
    b('legs','body',[-4.25,10.9,-2.55],[8.5,1.1,5.1],'dark')
    b('legs','body',[-1,10.7,-3.0],[2,1.5,.65],'edge')
    for f,size,mat in [([-2.05,.15,-2.05],[3.75,8.4,4.1],'cloth'),([-2.15,.55,-2.7],[3.8,4.8,.95],'plate'),
                       ([-2.2,5.7,-2.9],[3.9,2.25,1.2],'dark'),([-1.9,6.0,-3.05],[3.3,.55,.3],'edge'),
                       ([-2.65,1.0,-1.65],[.6,3.9,3.3],'plate'),([-2.7,1.7,-.4],[.32,2.6,.7],'rubber')]:b('legs','right_leg',f,size,mat,True)
    for f,size,mat in [([-2.35,8.45,-2.55],[3.95,3.3,5.35],'plate'),([-2.4,11.7,-3.5],[4,.85,6.4],'rubber'),
                       ([-2.15,10.45,-3.45],[3.5,1.2,1.05],'dark'),([-2.05,10.55,-3.65],[3.3,.35,.3],'edge'),
                       ([-2.4,8.95,2.85],[1.0,2.2,.6],'rubber'),([-2.15,9.3,3.45],[.5,1.5,.2],'rod')]:b('feet','right_leg',f,size,mat,True)
    for x in (-1.95,-.55,.85):b('feet','right_leg',[x,11.75,-3.85],[.65,.85,.5],'dark',True)
    return s


def paint_color(mat,face,x,y,w,h,rng):
    color=COLORS[mat];k=rng.uniform(.96,1.04)
    if min(w,h)>=3:
        if x==0 or y==0:k+=.18
        elif x==w-1 or y==h-1:k*=.66
    if mat in ('plate','dark') and w>=5 and h>=4:
        if (x,y) in ((1,1),(w-2,1),(1,h-2),(w-2,h-2)):color=COLORS['edge']
        if x==w-3 and y in (h//2,h//2+1):k*=.65
    if mat=='rubber' and y%3==1:k*=.73
    if mat=='cloth' and (x+y)%3==0:k*=.9
    if mat=='visor':k=1.18-y/max(1,h)*.55
    if mat=='cyan' and x==0:color=(143,221,224)
    return tuple(min(255,max(0,round(v*k))) for v in color)+(255,)


def write_armor(s):
    for cubes in s.slots.values():fix_zfight(cubes)
    allc=[c for cubes in s.slots.values() for c in cubes];rig=Rig();rig.part('mesh')
    for c in allc:rig.box('mesh',c.mat,c.f,c.s)
    uv,_=pack(rig)
    for c in allc:
        c.uv=uv[(c.mat,*c.s)]
        assert all(not(min(u+w,128)>max(u,124) and min(v+h,128)>max(v,124)) for u,v,w,h in regions(c).values()),'UV invade zona gris de referencia'
    image=Image.new('RGBA',(128,128))
    for c in allc:
        c.uv=uv[(c.mat,*c.s)];rng=random.Random(str((c.mat,*c.s)))
        for face,(u,v,w,h) in regions(c).items():
            for y in range(math.floor(v),math.ceil(v+h)):
                for x in range(math.floor(u),math.ceil(u+w)):
                    image.putpixel((x,y),paint_color(c.mat,face,x-math.floor(u),y-math.floor(v),math.ceil(w),math.ceil(h),rng))
    # Zona gris que usa la vista previa genérica de armor3d_gen para el maniquí.
    for y in range(124,128):
        for x in range(124,128):image.putpixel((x,y),(110,118,120,255))
    image.save(ROOT/'textures/models/armor/3d/inercita.png')
    data={'texture':[128,128],'slots':{slot:[{'bone':c.bone,'uv':list(c.uv),'from':c.f,'size':c.s,'inflate':c.inflate,'mirror':False} for c in cubes] for slot,cubes in s.slots.items()}}
    (ROOT/'armor_models/inercita.json').write_text(json.dumps(data,indent=1)+'\n',encoding='utf-8')
    return allc


def finish_items():
    """Mismo encuadre aunque se regenere desde el script global de armaduras."""
    import subprocess,sys
    from pathlib import Path
    project=Path(__file__).resolve().parent.parent
    for piece in ('helmet','chestplate','leggings','boots'):
        path=ROOT/f'models/item/inercita_{piece}.json';m=json.loads(path.read_text())
        for side in ('righthand','lefthand'):
            m['display']['firstperson_'+side]={'rotation':[0,135,0],'translation':[-1.7,5.1,0],'scale':[.32]*3}
        path.write_text(json.dumps(m,indent=2)+'\n',encoding='utf-8')
        subprocess.run([sys.executable,str(project/'tools/gui_fit.py'),str(ROOT),'18,200,0','fill=14','inercita_'+piece],check=True)
