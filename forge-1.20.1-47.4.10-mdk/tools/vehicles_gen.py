"""Vehículos abandonados: geometría original, superficies recortadas por celda y UV globales.
python tools/vehicles_gen.py
Solo arte. Sin Java, registros, datos, botín ni colisiones ejecutables.
"""
import copy
import itertools
import json
import math
import random
from pathlib import Path
from PIL import Image, ImageDraw
from check_zfight import check
from render_zbuffer import corners

ROOT=Path(__file__).resolve().parent.parent
A=ROOT/'src/main/resources/assets/tecnocinetico'
OUT=ROOT/'build/vehiculos'
SPECS={'car_wreck':(4,2,2),'truck_wreck':(6,2,3),'bus_wreck':(8,3,3)}
MATS=('paint','burnt','rust','steel','dark','rubber','rim','glass',
      'cloth','foam','wood','light','red','amber','stripe','grille')
FACES={'west':(0,0),'east':(0,1),'down':(1,0),'up':(1,1),'north':(2,0),'south':(2,1)}
EPS=1e-7


def dump(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes((json.dumps(data,ensure_ascii=False,indent=1)+'\n').encode('utf-8'))


def atlas(name):
    color={'car_wreck':(69,105,100),'truck_wreck':(139,111,65),'bus_wreck':(167,164,136)}[name]
    colors=(color,(49,51,45),(115,66,39),(87,93,87),(31,35,33),(26,29,27),
            (112,116,102),(84,108,105),(62,68,59),(137,121,79),(97,76,49),
            (150,154,125),(113,44,31),(155,107,39),(49,91,88),(44,48,44))
    image=Image.new('RGBA',(128,128))
    for n,(mat,base) in enumerate(zip(MATS,colors)):
        tile=Image.new('RGBA',(32,32));rng=random.Random(651+n)
        grain=[rng.randrange(-12,13) for _ in range(32)]
        for y in range(32):
            for x in range(32):
                k=rng.randrange(-5,6)
                if mat=='wood':k+=grain[x]
                if mat=='rubber':k+=-7 if (x+y//4)%7<2 else 2
                if mat=='cloth':k+=4 if (x+y)%4==0 else -2
                if mat in ('light','red','amber'):k+=12 if x%5<2 else -5
                if mat=='grille':k+=-15 if y%4<2 else 5
                tile.putpixel((x,y),tuple(max(0,min(255,c+k)) for c in base)+(255,))
        d=ImageDraw.Draw(tile)
        if mat in ('paint','stripe','steel','burnt','rim'):
            for _ in range(15 if mat!='burnt' else 33):
                x,y=rng.randrange(32),rng.randrange(32)
                for j in range(rng.randrange(4,18)):
                    tile.putpixel((x%32,y%32),rng.choice(((87,57,37,255),(118,72,40,255),(70,68,51,255))))
                    x+=rng.choice((-1,0,1));y+=rng.choice((0,1))
            for _ in range(8):
                x,y=rng.randrange(30),rng.randrange(32)
                d.line((x,y,x+2,y),fill=(137,142,124))
        if mat=='glass':
            for pts in ([(0,2),(13,13),(18,29)],[(13,13),(27,5),(31,6)],[(13,13),(4,30)]):
                d.line(pts,fill=(155,171,157),width=1)
            d.line((2,8,29,29),fill=(41,63,58))
        if mat=='cloth':
            d.line([(5,3),(9,10),(7,13),(12,21)],fill=(28,32,27),width=2)
            d.line((20,19,27,25),fill=(143,122,77),width=2)
        if mat=='foam':
            for _ in range(25):
                x,y=rng.randrange(32),rng.randrange(32);d.point((x,y),fill=(76,69,45))
        if mat=='wood':
            for x in (5,17,27):d.line([(x,1),(x+1,11),(x-1,23),(x,31)],fill=(40,36,27))
        if mat=='rust':
            for _ in range(50):
                x,y=rng.randrange(32),rng.randrange(32);d.rectangle((x,y,x+1,y+1),fill=rng.choice(((73,48,31),(144,89,48))))
        image.paste(tile,((n%4)*32,(n//4)*32))
    p=A/f'textures/block/{name}.png';p.parent.mkdir(parents=True,exist_ok=True);image.save(p)


def box(a,x,y,z,w,h,d,mat='paint'):
    assert min(w,h,d)>0
    a.append({'from':[x,y,z],'to':[x+w,y+h,z+d],'mat':mat})


def wheel(a,x,z,r=4.6,missing=False,flat=False):
    """Perfil octogonal escalonado; aro, buje y cuatro pernos reales, sin planos superpuestos."""
    cy=r+.18;th=2.5
    if not missing:
        ry=r*.70 if flat else r
        cy=ry+.18
        for hh,dd in ((ry,.56*r),(.78*ry,.86*r),(.46*ry,r)):
            box(a,x,cy-hh,z-dd,th,2*hh,2*dd,'rubber')
    side=x-.12 if x<10 else x+th-.55
    rr=r*.60
    for hh,dd in ((rr,.60*rr),(.73*rr,.91*rr),(.43*rr,rr)):
        box(a,side,cy-hh,z-dd,.7,2*hh,2*dd,'rust' if missing else 'rim')
    box(a,side-.05,cy-.85,z-.85,.8,1.7,1.7,'dark')
    for dy,dz in ((-1.7,0),(1.7,0),(0,-1.7),(0,1.7)):
        box(a,side-.08,cy+dy-.23,z+dz-.23,.86,.46,.46,'steel')


def seat(a,x,z,w=6,h=8,damaged=False):
    box(a,x+1,7.8,z+1,w-2,3,4,'rust')
    box(a,x,10.5,z,w,2.2,6,'cloth')
    box(a,x,12.6,z+4.7,w, h,1.8,'cloth')
    if damaged:
        box(a,x+.5,12.82,z+1,w*.55,.35,2.6,'foam')
        box(a,x+1,14,z+4.45,w*.45,3,.4,'foam')
    else:
        box(a,x+w/2-.3,h+12.4,z+5.2,.6,.9,.6,'steel')
        box(a,x+1,h+13,z+4.9,w-2,2,1.6,'cloth')


def engine(a,x,y,z,w=12):
    box(a,x,y,z,w,2.5,9,'dark')
    box(a,x+1,y+2,z+1,w-2,3.7,6,'steel')
    for xx in range(int(x+2),int(x+w-1),2):box(a,xx,y+5.6,z+1,.6,.45,6,'rust')
    box(a,x+w-.5,y+1,z+2,1.2,2.2,4,'grille')
    box(a,x+2,y+2,z-1,2.4,2.4,2,'dark')
    for zz in (z+1,z+5):box(a,x-.6,y+3,zz,1,1,2,'rubber')


def car():
    a=[]
    box(a,4,5.5,5,24,1.8,54,'burnt')
    for x in (4.7,25.3):box(a,x,3.4,9,2,2.1,46,'rust')
    for z in (14,49):box(a,2.8,4,z-.6,26.4,1.2,1.2,'steel')
    for x in (2,27.5):
        for z in (14,49):wheel(a,x,z,4.4,missing=(x>10 and z==49),flat=(x<10 and z==14))
    for x in (3.6,27):
        box(a,x,7.2,5,1.4,2.2,54,'rust')
        for z,d in ((5,4),(19,23),(54,6)):box(a,x,9.3,z,1.4,3.5,d,'paint')
        for z in (9,44):box(a,x,12.1,z,1.4,1,10,'paint')
    box(a,4.3,8,3.7,23.4,4,2,'paint')
    box(a,2.8,6.7,2,26.4,1.7,1.5,'steel')
    box(a,10.5,9,3.25,11,1.9,.5,'grille')
    for x in (5,23):box(a,x,9.4,3.05,4,2.6,.8,'light')
    box(a,5,9,58.7,22,4,1.8,'burnt')
    box(a,3.1,6.9,60.5,25.8,1.5,1.4,'rust')
    for x in (5,23.4):box(a,x,10,60.5,3.4,1.8,.5,'red')
    box(a,9,12.6,46,14,1.3,12,'paint')
    engine(a,9,8.2,9,14)
    # Capó abierto: chapas escalonadas, bisagras en el cortafuegos.
    for z,y in ((8,24),(11,22),(14,20),(17,18),(20,16)):
        box(a,5.5,y,z,21,2.1,3.2,'burnt' if z==14 else 'paint')
    box(a,6.4,12,10,.5,10.5,.5,'steel')
    box(a,5.5,13.4,22,21,1.5,3.1,'dark')
    for x in (5.2,25.8):
        box(a,x,12.7,23,1,9.7,1.1,'steel')
        box(a,x,12.7,44.5,1,9.7,1.2,'paint')
        box(a,x,13,36,1,9.6,.9,'burnt')
        box(a,x,21.8,24,1,1.3,21,'paint')
    for z in (24,43):box(a,6.2,22,z,19.6,1.3,2,'paint')
    box(a,6.2,22,26,7.5,1.3,17,'burnt')
    box(a,18,22,26,7.8,1.3,17,'paint')
    # Parabrisas quebrado: huecos abiertos y unos pocos fragmentos restantes.
    box(a,6.3,15,23.15,4.8,3.7,.25,'glass')
    box(a,20.8,18,23.15,4.8,3.7,.25,'glass')
    box(a,6.4,16.3,45.4,3,5.1,.25,'glass')
    box(a,19,19,45.4,6.4,2.4,.25,'glass')
    seat(a,7,28,6,6,True);seat(a,19,28,6,6)
    seat(a,7,39,18,4,True)
    box(a,14.8,8,25,2.1,5.2,13,'dark')
    box(a,15.6,13.1,28.5,.45,2,.45,'steel')
    # Volante cuadrangular y columna, visibles por la puerta arrancada.
    box(a,9.5,12,25.5,.8,3,2,'dark')
    for x in (7.8,11.7):box(a,x,15,26,.6,.6,3,'rubber')
    for z in (26,28.5):box(a,7.8,15,z,4.5,.6,.6,'rubber')
    # Puerta derecha descuadrada, izquierda ausente.
    box(a,28.6,9.6,26,1.3,6.3,11,'paint')
    for z in (26,36):box(a,28.6,15.5,z,.7,5.6,.7,'rust')
    box(a,28.6,20.6,26,.7,.6,10.7,'steel')
    box(a,29.95,14.4,33,.3,.5,2,'steel')
    box(a,5,7.5,34,4.8,.7,6,'burnt')
    return a


def truck():
    a=[]
    for x in (7,23):box(a,x,5,8,2,4,84,'rust')
    box(a,4,9,3,24,2,28,'burnt')
    for z in (15,69,83):
        box(a,2,5,z-.8,28,1.6,1.6,'steel')
        for x in (1.5,28):wheel(a,x,z,5.2,missing=(z==69 and x<10),flat=(z==15 and x>10))
    box(a,3.4,10.8,3,25.2,3,2,'paint')
    box(a,3,8,1.8,26,2,1.7,'steel')
    engine(a,9,11,5,14)
    for x in (4,24):box(a,x,14.4,3.1,4,3,.7,'light')
    for x in (4,27):
        box(a,x,11,6,1.1,10.5,24,'paint')
        for z in (5,28.5):box(a,x,21,z,1.1,10.8,1.3,'burnt')
        box(a,x,30.3,6,1.1,1.1,23,'steel')
    # Cabina: techo parcial, parabrisas sin el panel central, puerta rota.
    box(a,5,30.5,7,9,1.2,22,'paint')
    box(a,17,30.5,7,10,1.2,22,'burnt')
    box(a,5,30.5,5,22,1.2,2,'paint')
    box(a,5,20.5,5,22,1.2,1,'dark')
    box(a,5.3,21.8,5.3,4.5,6,.25,'glass')
    box(a,22.5,26.5,5.3,4.2,3.5,.25,'glass')
    box(a,5,11,29,22,18,1.2,'burnt')
    for x in (7,19):seat(a,x,19,6,10,x==7)
    box(a,6,18.7,8,20,2,3,'dark')
    box(a,8,21,12,5,.8,4,'rubber')
    box(a,10,18.5,13,.8,2.5,.8,'steel')
    box(a,28.7,12,19,1.4,9,9,'paint')
    box(a,29,21,26,.7,7,.7,'rust')
    # Caja de carga abierta en un costado y en la trasera.
    box(a,3.5,12,33,25,2.5,60,'steel')
    for x in (3.5,27):
        for z in (34,48,62,76,90):box(a,x,14.5,z,1.5,25,1.3,'steel')
        for z,d in ((34,13),(49,12),(77,14)):
            box(a,x+.15,15.5,z,1,21,d,'paint')
        for y in (18,23,28,33):
            for z,d in ((34,27),(77,14)):box(a,x-.15,y,z,1.8,.5,d,'rust')
    box(a,5,38.8,34,22,1.3,29,'paint')
    box(a,5,38.8,63,9,1.3,28,'burnt')
    box(a,16,38.8,63,11,1.3,14,'paint')
    for y in (14.5,36.5):box(a,5,y,92,22,1.2,1.3,'rust')
    box(a,5,15.7,92.4,8,20.8,.8,'paint')
    for y in (19,25,31):box(a,5,y,93.2,8,.7,.45,'steel')
    # Palés, cajones rotos y tablones, todo dentro de la caja.
    for z in (47,66,80):
        for x in (8,20):box(a,x,14.5,z,2,2,11,'wood')
        for zz in (z,z+4,z+8):box(a,7,16.5,zz,17,1,2.4,'wood')
    for x in (7,20):
        for y in (18,23,28):box(a,x,y,68,1,3,12,'wood')
    for z in (68,79):box(a,7,18,z,14,13,1,'wood')
    box(a,29,8,32,1.2,24,1.3,'rust')
    box(a,28.8,31.8,31.8,1.6,1,1.7,'dark')
    for x in (5,24.5):box(a,x,10,93.5,2.5,2,.5,'red')
    return a


def bus():
    a=[]
    box(a,4,7.4,3,40,2,122,'burnt')
    for x in (9,36):box(a,x,4.3,9,2.5,3,110,'rust')
    for z in (20,102):
        box(a,2,5.5,z-.8,44,1.6,1.6,'steel')
        for x in (1.4,44):wheel(a,x,z,5.6,missing=(z==102 and x>10),flat=(z==20 and x<10))
    # Paneles inferiores interrumpidos, pasos de rueda y puerta de acceso ausente.
    for x in (3.4,43):
        for z,d in ((3,10),(27,38),(86,10),(109,16)):
            if x>20 and z==3:continue
            box(a,x,9.4,z,1.5,11.4,d,'paint')
            box(a,x-.08,16.7,z,1.66,2.8,d,'stripe')
        for z in (13,96):box(a,x,17.6,z,1.5,3.2,14,'paint')
        box(a,x,20.7,3,1.5,1.1,122,'steel')
        for z in (3,26,43,60,77,94,111,124):box(a,x,21.8,z,1.3,16.9,1.1,'paint')
        box(a,x,38,3,1.5,1.6,122,'paint')
        # Fragmentos pequeños de cristal en marcos vacíos; sin láminas transparentes falsas.
        for i,z in enumerate((28,45,62,79,96,113)):
            if i%2==0:box(a,x+.3,23,z,1,4.7,4,'glass')
            else:box(a,x+.3,33,z+7,1,4.5,5,'glass')
    for y,h in ((9.5,11.5),(37.5,2)):
        box(a,5,y,1.9,38,h,1.6,'paint')
        box(a,5,y,124,38,h,1.6,'burnt')
    for x in (5,23.5,41.5):box(a,x,21,2,1.3,16.5,1.2,'steel')
    box(a,6.3,22.2,2.3,5.5,9,.45,'glass')
    box(a,30,31,2.3,10,5.5,.45,'glass')
    # Techo dañado: nervios, dos grandes zonas abiertas y una chapa arrancada.
    for z in (4,26,48,70,92,123):box(a,5,39.2,z,38,1.1,1.2,'steel')
    for x,z,w,d in ((5,4,38,22),(5,27,12,41),(30,27,13,41),(5,71,38,20),(5,94,18,29)):
        box(a,x,39.4,z,w,1.2,d,'burnt' if z>90 else 'paint')
    box(a,16,40.6,12,16,1.7,9,'grille')
    for x in (7,37):box(a,x,20,6,4,3,1.5,'light')
    box(a,4,7,1,40,2,1.3,'rust')
    box(a,17,12.5,1.3,14,4,.65,'grille')
    box(a,6,21,5,35,2.2,4,'dark')
    seat(a,7,12,7,11,True)
    box(a,8,23.5,10,6,.8,4,'rubber')
    # Asientos traseros: pasillo real en medio, espuma expuesta y hueco de asiento arrancado.
    for i,z in enumerate((32,49,66,83,106)):
        for x in (7,32):
            if i==2 and x==7:continue
            seat(a,x,z,9,10,(i+(x==7))%2==0)
        if i!=4:
            for x in (20.6,27):box(a,x,10,z+7,.65,27,.65,'steel')
    for x in (20.6,27):box(a,x,36.5,27,.65,.65,91,'rust')
    # Escalones y restos del plegado de la puerta delantera.
    for x,y in ((39,4),(37,6),(35,8)):box(a,x,y,9,5,1.7,14,'steel')
    for z in (10,21):box(a,41.2,10,z,.9,23,1,'burnt')
    box(a,41.2,22,10,.9,1,12,'steel')
    for y in (14,17,20):box(a,16,y,125.5,16,.7,.8,'grille')
    for x in (6,39):box(a,x,11,125.5,3,3,.5,'red')
    box(a,7,29,124.8,7,6,.6,'glass')
    return a


def subtract(rect,cut):
    x0,y0,x1,y1=rect;a0,b0,a1,b1=cut
    a0=max(x0,a0);b0=max(y0,b0);a1=min(x1,a1);b1=min(y1,b1)
    if a1-a0<EPS or b1-b0<EPS:return [rect]
    return [r for r in ((x0,y0,a0,y1),(a1,y0,x1,y1),(a0,y0,a1,b0),(a0,b1,a1,y1)) if r[2]-r[0]>EPS and r[3]-r[1]>EPS]


def surfaces(boxes):
    """Unión superficial exacta de AABB: quitar caras interiores y coincidencias, conservando UV globales."""
    result=[]
    for i,e in enumerate(boxes):
        for face,(axis,side) in FACES.items():
            plane=e['to' if side else 'from'][axis];axes=[k for k in range(3) if k!=axis]
            rect=[e['from'][k] for k in axes]+[e['to'][k] for k in axes];rects=[rect]
            for j,o in enumerate(boxes):
                if i==j:continue
                lo,hi=o['from'][axis],o['to'][axis]
                occluded=(lo<=plane+EPS and hi>plane+EPS) if side else (lo<plane-EPS and hi>=plane-EPS)
                duplicate=j<i and abs((hi if side else lo)-plane)<EPS
                if not (occluded or duplicate):continue
                cut=[o['from'][k] for k in axes]+[o['to'][k] for k in axes]
                rects=[piece for r in rects for piece in subtract(r,cut)]
                if not rects:break
            for r in rects:
                lo=[plane]*3;hi=[plane]*3
                for k,ax in enumerate(axes):lo[ax]=r[k];hi[ax]=r[k+2]
                result.append({'from':lo,'to':hi,'face':face,'mat':e['mat']})
    return result


def uv_coords(p,face):
    x,y,z=p
    return {'north':(-x,-y),'south':(x,-y),'east':(-z,-y),'west':(z,-y),'up':(x,z),'down':(x,-z)}[face]


def mesh(boxes,spec):
    L,W,H=spec;parts=[[] for _ in range(L*W*H)];surfs=surfaces(boxes)
    for s in surfs:
        axis,side=FACES[s['face']]
        axes=[i for i in range(3) if i!=axis];plane=s['from'][axis]
        # Una cara en un límite pertenece a la celda que contiene su volumen, no a la vecina vacía.
        index=math.floor((plane+(-EPS if side else EPS))/16)
        for h,w,l in itertools.product(range(H),range(W),range(L)):
            cell=(w,h,l)
            if cell[axis]!=index:continue
            low=[max(s['from'][i],cell[i]*16) for i in range(3)]
            high=[min(s['to'][i],(cell[i]+1)*16) for i in range(3)]
            if any(high[i]-low[i]<EPS for i in axes):continue
            points=corners(low,high,s['face']);q0=uv_coords(points[0],s['face']);q2=uv_coords(points[2],s['face'])
            n=MATS.index(s['mat']);u,v=n%4*4,n//4*4
            ubase=math.floor((min(q0[0],q2[0])+EPS)/16)*16
            vbase=math.floor((min(q0[1],q2[1])+EPS)/16)*16
            uv=[u+(q0[0]-ubase)/4,v+(q0[1]-vbase)/4,u+(q2[0]-ubase)/4,v+(q2[1]-vbase)/4]
            e={'from':[round(low[i]-cell[i]*16,6) for i in range(3)],
               'to':[round(high[i]-cell[i]*16,6) for i in range(3)],
               'faces':{s['face']:{'texture':'#0','uv':[round(t,6) for t in uv]}}}
            parts[(h*W+w)*L+l].append(e)
    return parts


def collision(boxes,spec):
    L,W,H=spec;result=[]
    for h,w,l in itertools.product(range(H),range(W),range(L)):
        offset=(16*w,16*h,16*l);hits=[]
        for b in boxes:
            lo=[max(b['from'][i],offset[i]) for i in range(3)]
            hi=[min(b['to'][i],offset[i]+16) for i in range(3)]
            if all(hi[i]-lo[i]>EPS for i in range(3)):hits.append((lo,hi))
        result.append(None if not hits else [round(min(t[0][i] for t in hits)-offset[i],6) for i in range(3)]+
                      [round(max(t[1][i] for t in hits)-offset[i],6) for i in range(3)])
    return result


def model(name,els):
    return {'parent':'minecraft:block/block','ambientocclusion':True,
            'textures':{'0':f'tecnocinetico:block/{name}','particle':f'tecnocinetico:block/{name}'},'elements':els}


def miniature(name,parts,spec):
    L,W,H=spec;ratio=26/(L*16);els=[]
    for part,items in enumerate(parts):
        l=part%L;w=(part//L)%W;h=part//(L*W)
        shift=(w*16-W*8,h*16-H*8,l*16-L*8)
        for original in items:
            e=copy.deepcopy(original)
            for key in ('from','to'):e[key]=[round((e[key][i]+shift[i])*ratio+8,6) for i in range(3)]
            els.append(e)
    m=model(name,els)
    m['display']={'gui':{'rotation':[25,225,0],'translation':[0,0,0],'scale':[.61]*3},
                  'ground':{'rotation':[0,0,0],'translation':[0,1,0],'scale':[.48]*3},
                  'fixed':{'rotation':[0,90,0],'translation':[0,0,0],'scale':[.6]*3},
                  'firstperson_righthand':{'rotation':[0,35,0],'translation':[0,0,0],'scale':[.45]*3},
                  'firstperson_lefthand':{'rotation':[0,35,0],'translation':[0,0,0],'scale':[.45]*3},
                  'thirdperson_righthand':{'rotation':[70,25,0],'translation':[0,1,0],'scale':[.4]*3},
                  'thirdperson_lefthand':{'rotation':[70,25,0],'translation':[0,1,0],'scale':[.4]*3}}
    return m


def main():
    OUT.mkdir(parents=True,exist_ok=True);integration={'part_formula':'(h*W+w)*L+l','units':'pixels, 16 per cell','vehicles':{}}
    files=[]
    for name,build in zip(SPECS,(car,truck,bus)):
        spec=SPECS[name];L,W,H=spec;boxes=build();atlas(name)
        assert all(0<=e['from'][i]<e['to'][i]<=16*(W,H,L)[i] for e in boxes for i in range(3)),name
        parts=mesh(boxes,spec);bounds=collision(boxes,spec)
        variants={}
        for n,els in enumerate(parts):
            path=A/f'models/block/{name}_part_{n}.json';dump(path,model(name,els));files.append(path)
            assert not check(path),f'{name} part {n}: z-fighting {check(path)[:4]}'
            assert all(0<=t<=16 for e in els for k in ('from','to') for t in e[k])
            assert bounds[n] is not None or not els
            for facing,yaw in (('north',0),('east',90),('south',180),('west',270)):
                variants[f'facing={facing},part={n}']=dict({'model':f'tecnocinetico:block/{name}_part_{n}'},**({'y':yaw} if yaw else {}))
        state=A/f'blockstates/{name}.json';dump(state,{'variants':variants});files.append(state)
        item=A/f'models/item/{name}.json';dump(item,miniature(name,parts,spec));files.append(item)
        # El icono es una copia escalada de TODAS las celdas, no una celda ni un sprite.
        assert not check(item),f'{name}: z-fighting en miniatura'
        integration['vehicles'][name]={'L':L,'W':W,'H':H,'part_count':len(parts),'collision':bounds,
                                      'empty_parts':[i for i,b in enumerate(bounds) if b is None]}
        dump(OUT/f'{name}_source.json',{'boxes':boxes,'dimensions':spec})
        files.append(A/f'textures/block/{name}.png')
        print(name,len(boxes),'primitivas;',sum(map(len,parts)),'caras;',len(parts),'celdas;',sum(b is None for b in bounds),'vacías',flush=True)
    integration['notes']=['NORTH: frontal z=0, ancho +X, largo +Z; todos los elementos locales dentro de [0,16].',
                          'Rotaciones: north=0, east=90, south=180, west=270. Girar coordenadas de celda Y y geometría alrededor del centro de la celda, como el blockstate.',
                          'Las cajas son AABB envolventes por celda, no una unión exacta de chapa/asientos. Pueden cerrar huecos dentro de una celda.',
                          'Recomendado noOcclusion: los modelos tienen huecos reales y caras de corte ausentes; no usar oclusión de cubo completo.',
                          'Cristales opacos rotos por geometría; sin emisivo ni render transparente especial.',
                          'No eliminar partes vacías del rango de PART: todos los índices tienen su modelo, incluso si elements está vacío.']
    dump(OUT/'integracion.json',integration)
    dump(OUT/'archivos.json',[str(p.relative_to(ROOT)).replace('\\','/') for p in files]+['tools/vehicles_gen.py','tools/preview_vehicles.py','docs/HERRAMIENTAS.md'])


if __name__=='__main__':main()
