"""Arte de infraestructura abandonada. No genera Java, datos, botín ni registros.

python tools/world_props_gen.py
python tools/preview_world_props.py
"""
import json
import math
import random
from pathlib import Path
from PIL import Image, ImageDraw
from check_zfight import check

ROOT = Path(__file__).resolve().parent.parent
A = ROOT / 'src/main/resources/assets/tecnocinetico'
OUT = ROOT / 'build/postes'
IDS = ('street_lamp_pole', 'street_lamp_head', 'street_lamp_bent',
       'utility_pole', 'utility_crossarm', 'power_cable')
MATERIALS = ('steel', 'paint', 'rust', 'wood', 'endgrain', 'iron',
             'ceramic', 'glass', 'reflector', 'rubber', 'copper', 'warning')
COLORS = ((111,122,119), (139,150,140), (111,64,39), (91,76,54),
          (107,89,61), (56,63,61), (151,157,138), (90,112,109),
          (126,132,123), (28,32,31), (147,94,49), (155,128,61))


def dump(rel, data):
    p = A / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_bytes((json.dumps(data, ensure_ascii=False, indent=2)+'\n').encode('utf-8'))


def atlas():
    """Atlas original: paleta apagada, manchas agrupadas, fibra y corrosión; semillas fijas."""
    img = Image.new('RGBA', (128,128), (45,48,44,255))
    for n, (name, base) in enumerate(zip(MATERIALS,COLORS)):
        tile = Image.new('RGBA',(32,32)); r = random.Random(821+n)
        columns = [r.randrange(-13,14) for _ in range(32)]
        for y in range(32):
            for x in range(32):
                v = r.randrange(-5,6)
                if name == 'wood': v += columns[x] + int(4*math.sin(y*.4+x))
                if name == 'rubber': v += (x%5==0)*7
                if name == 'ceramic': v += 10 if x%8<2 else -4
                tile.putpixel((x,y),tuple(max(0,min(255,c+v)) for c in base)+(255,))
        d = ImageDraw.Draw(tile)
        if name in ('steel','paint','iron','reflector'):
            # Picaduras pequeñas con bordes irregulares, sin grandes manchas cuadradas.
            for _ in range(11):
                x,y=r.randrange(32),r.randrange(32)
                for j in range(r.randrange(3,10)):
                    tile.putpixel((x%32,y%32),r.choice(((90,59,38,255),(126,77,43,255),(68,69,58,255))))
                    x+=r.choice((-1,0,1));y+=r.choice((0,0,1))
            for _ in range(7):
                x,y=r.randrange(32),r.randrange(32)
                d.line((x,y,x+2,y),fill=(165,168,152))
        if name == 'wood':
            for x in (3,11,19,27):
                y=r.randrange(3,12)
                d.line([(x,y),(x+1,y+5),(x,y+12),(x+1,y+18)],fill=(42,39,31))
                d.line((x+2,y+3,x+2,y+13),fill=(123,103,69))
            d.ellipse((20,18,27,27),outline=(52,46,34),width=2)
        if name == 'endgrain':
            for radius in (3,7,11,15,19):d.ellipse((16-radius,15-radius,16+radius,15+radius),outline=(68,60,43))
            d.line((16,15,25,30),fill=(40,38,29))
        if name == 'glass':
            d.line([(1,2),(13,10),(16,18),(30,27)],fill=(161,177,165),width=1)
            d.line([(13,10),(21,5),(29,7)],fill=(39,57,53))
            d.line([(16,18),(4,28)],fill=(40,57,51))
        if name == 'warning':
            for x in range(-32,64,12): d.polygon([(x,0),(x+5,0),(x-27,32),(x-32,32)],fill=(46,48,40))
        # Los lados de fustes se repiten sin discontinuidad vertical.
        if name in ('wood','paint','steel'):
            for x in range(32):tile.putpixel((x,31),tile.getpixel((x,0)))
        img.paste(tile,((n%4)*32,(n//4)*32))
    p=A/'textures/block';p.mkdir(parents=True,exist_ok=True)
    img.save(p/'world_props_atlas.png')
    for name,mat in (('metal','paint'),('wood','wood'),('cable','rubber')):
        n=MATERIALS.index(mat);x,y=n%4*32,n//4*32
        img.crop((x,y,x+32,y+32)).resize((16,16),Image.Resampling.NEAREST).save(p/f'world_props_{name}.png')


def box(els, lo, hi, mat='steel', rotation=None, ends=None, omit=()):
    e={'from':list(lo),'to':list(hi),'faces':{}}
    if rotation:e['rotation']=rotation
    for face in ('north','south','west','east','up','down'):
        if face in omit:continue
        material = ends if ends and face in ('up','down') else mat
        n=MATERIALS.index(material);u,v=(n%4)*4,(n//4)*4
        w,h=([hi[0]-lo[0],hi[2]-lo[2]] if face in ('up','down') else
             [hi[2]-lo[2],hi[1]-lo[1]] if face in ('east','west') else
             [hi[0]-lo[0],hi[1]-lo[1]])
        e['faces'][face]={'texture':'#0','uv':[u,v,u+min(4,w/4),v+min(4,h/4)]}
    els.append(e)
    return e


def rot(axis,angle,origin):return {'axis':axis,'angle':angle,'origin':list(origin)}


def vertex(p,r):
    if not r:return p
    a=math.radians(r['angle']);c,s=math.cos(a),math.sin(a)
    x,y,z=[p[i]-r['origin'][i] for i in range(3)]
    if r['axis']=='x':y,z=y*c-z*s,y*s+z*c
    elif r['axis']=='y':x,z=x*c+z*s,-x*s+z*c
    else:x,y=x*c-y*s,x*s+y*c
    return [v+o for v,o in zip((x,y,z),r['origin'])]


def shaft(wood=False):
    a=[];w=3 if wood else 2
    box(a,(8-w,0,8-w),(8+w,16,8+w),'wood' if wood else 'paint',ends='endgrain' if wood else 'steel')
    if wood:
        box(a,(4.78,5.3,6.6),(5.08,10.7,9.4),'iron')
        for y in (6.1,9.6):box(a,(4.53,y,7.6),(4.81,y+.6,8.3),'steel')
        box(a,(6.8,11.8,4.84),(9.2,13.5,5.04),'warning')
    else:
        box(a,(5.82,3.1,6.9),(6.06,9.2,9.1),'iron')
        for y in (3.7,8.1):box(a,(5.64,y,7.7),(5.85,y+.45,8.25),'rust')
    return a


def lamp_head():
    a=[]
    box(a,(6,0,6),(10,5.5,10),'paint')
    box(a,(6.8,4.3,-5.7),(9.2,6.2,7),'paint')
    box(a,(7.4,2,-1),(8.6,2.7,8),'iron',rot('x',22.5,(8,2.35,8)))
    # Carcasa escalonada, reflector profundo y marco. Los huecos son geometría real.
    box(a,(4.5,2.5,-14),(11.5,4.5,-3.5),'paint')
    box(a,(5.2,4.49,-13.2),(10.8,5.25,-4.2),'steel')
    box(a,(5.05,2.08,-13.35),(10.95,2.51,-4.15),'reflector')
    for x0,x1 in ((4.4,5),(11,11.6)):box(a,(x0,1.4,-14.1),(x1,2.49,-3.4),'iron')
    for z0,z1 in ((-14.1,-13.5),(-4,-3.4)):box(a,(5,1.4,z0),(11,2.49,z1),'iron')
    box(a,(5.1,1.6,-13.4),(7.65,1.85,-9.8),'glass')
    box(a,(7.9,1.55,-8.7),(10.9,1.8,-4.1),'glass')
    box(a,(8.9,1.48,-12.7),(10.8,1.73,-11.3),'glass',rot('y',22.5,(9.8,1.6,-12)))
    for z in (-10,-7):box(a,(7.7,1.95,z),(8.3,2.06,z+.9),'ceramic')
    for x in (4.55,11.1):
        for z in (-13.5,-4.6):box(a,(x,2.5,z),(x+.33,2.85,z+.4),'rust',omit=('down',))
    return a


def bent():
    a=[]
    box(a,(6,0,6),(10,3.5,10),'paint')
    box(a,(6.15,2.8,6.15),(9.85,16,9.85),'paint',rot('x',-45,(8,3,8)))
    # Acero retorcido y articulación desgarrada en el pliegue.
    for x in (5.8,9.65):box(a,(x,2.4,5.9),(x+.55,5.1,8.8),'rust',rot('x',-22.5,(8,3,8)))
    box(a,(6.75,11.2,-5.5),(9.25,13.3,-.6),'iron')
    # Luminaria desprendida por delante: su cara rota mira abajo y hacia el frente.
    rr=rot('x',22.5,(8,11.5,-5))
    box(a,(4.8,5.5,-7.5),(11.2,12.4,-4.4),'paint',rr)
    box(a,(5.3,6,-7.79),(10.7,11.7,-7.49),'reflector',rr)
    box(a,(5.5,6.2,-8.03),(7.7,8.3,-7.78),'glass',rr)
    box(a,(8.4,9.1,-8.03),(10.5,11.4,-7.78),'glass',rr)
    for x in (4.7,10.8):box(a,(x,5.4,-8.08),(x+.5,12.3,-7.7),'iron',rr)
    for y in (5.4,11.9):box(a,(5.21,y,-8.08),(10.79,y+.4,-7.7),'iron',rr)
    # Cable arrancado, cubierta negra y conductor de cobre en la punta.
    box(a,(9.6,5.9,-3.2),(9.96,10.9,-2.84),'rubber',rot('x',-22.5,(9.8,10.8,-3)))
    box(a,(9.65,5.45,-3.15),(9.88,6,-2.92),'copper',rot('x',-22.5,(9.8,10.8,-3)))
    return a


def crossarm():
    a=[]
    box(a,(5,0,5),(11,16,11),'wood',ends='endgrain')
    box(a,(-1,8,2.8),(17,10.4,5.3),'wood',ends='endgrain')
    box(a,(6.1,6.8,2.48),(9.9,11.6,2.85),'iron')
    for x in (6.7,8.8):
        for y in (7.3,10.7):box(a,(x,y,2.23),(x+.5,y+.5,2.5),'steel')
    for x,angle in ((2,45),(13,-45)):
        box(a,(x,4.5,3.5),(x+.6,9.2,4.3),'iron',rot('z',angle,(x+.3,9,4)))
    for x in (2,8,14):
        box(a,(x-.35,10.39,3.45),(x+.35,14.88,4.15),'iron')
        for y,w in ((10.9,1.25),(12.1,1.4),(13.3,1.1)):
            box(a,(x-w,y,3.8-w),(x+w,y+.48,3.8+w),'ceramic')
        box(a,(x-.7,13.77,3.1),(x+.7,14.45,4.5),'ceramic')
        # Conexión a ambos extremos del bloque, alineada con los cables vecinos.
        box(a,(x-.16,14.64,0),(x+.16,14.96,16),'rubber')
    return a


def cable(hanging):
    a=[]
    if hanging:
        # Axe X, trois conducteurs. Extrémités y=14.8, centre y=12.315.
        for z in (2,8,14):
            for lo,hi,angle,pivot in (
                ((0,14.64,z-.16),(6.50,14.96,z+.16),-22.5,(0,14.8,z)),
                ((6,12.155,z-.16),(10,12.475,z+.16),0,(8,12.315,z)),
                ((9.50,14.64,z-.16),(16,14.96,z+.16),22.5,(16,14.8,z))):
                box(a,lo,hi,'rubber',rot('z',angle,pivot) if angle else None)
    else:
        # Trois gaines posées au sol, boucles anguleuses et bouts de cuivre dénudés.
        paths=(((0,3),(4,3),(7,6),(10,6),(13,3),(16,3)),
               ((0,8),(3,8),(6,11),(9,11),(12,8),(16,8)),
               ((0,14),(3,14),(6,11),(9,11),(12,14),(16,14)))
        for i,path in enumerate(paths):
            y=.2+i*.34
            for (x0,z0),(x1,z1) in zip(path,path[1:]):
                dx,dz=x1-x0,z1-z0;length=math.hypot(dx,dz)
                angle=-math.degrees(math.atan2(dz,dx))
                box(a,(x0,y,z0-.16),(x0+length+.015,y+.3,z0+.16),'rubber',
                    rot('y',angle,(x0,y,z0)) if angle else None)
        box(a,(14.5,.7,6.1),(15.8,.9,6.3),'copper',rot('y',45,(14.5,.8,6.2)))
    return a


def definition(name,els):
    particle='wood' if name.startswith('utility') else 'cable' if name.startswith('power') else 'metal'
    return {'parent':'minecraft:block/block','ambientocclusion':True,
            'textures':{'0':'tecnocinetico:block/world_props_atlas','particle':f'tecnocinetico:block/world_props_{particle}'},
            'elements':els}


def axis_states(name):
    return {'variants':{'axis=y':{'model':f'tecnocinetico:block/{name}'},
                        'axis=x':{'model':f'tecnocinetico:block/{name}','x':90,'y':90},
                        'axis=z':{'model':f'tecnocinetico:block/{name}','x':90}}}


def inventory(els):
    # Encuadrar los voladizos: el centro geométrico no coincide con (8,8,8).
    pts=[]
    for e in els:
        for x in (e['from'][0],e['to'][0]):
            for y in (e['from'][1],e['to'][1]):
                for z in (e['from'][2],e['to'][2]):
                    p=vertex((x,y,z),e.get('rotation'));p=[v-8 for v in p]
                    p=vertex(vertex(p,rot('y',225,(0,0,0))),rot('x',30,(0,0,0)))
                    pts.append(p)
    lo=[min(p[i] for p in pts) for i in (0,1)];hi=[max(p[i] for p in pts) for i in (0,1)]
    s=min(.75,14/max(hi[i]-lo[i] for i in (0,1)))
    return {'rotation':[30,225,0],'translation':[round(-(lo[i]+hi[i])/2*s,4) for i in (0,1)]+[0],
            'scale':[round(s,4)]*3}


def main():
    OUT.mkdir(parents=True,exist_ok=True);atlas()
    geometry=dict(zip(IDS[:-1],(shaft(),lamp_head(),bent(),shaft(True),crossarm())))
    geometry.update(power_cable_hanging=cable(True),power_cable_ground=cable(False))
    report={}
    for name,els in geometry.items():
        m=definition(name,els);dump(f'models/block/{name}.json',m)
        path=A/f'models/block/{name}.json'
        assert not check(path),f'{name}: caras coplanares {check(path)}'
        vertices=[]
        for e in els:
            assert all(-16<=v<=32 for key in ('from','to') for v in e[key])
            for x in (e['from'][0],e['to'][0]):
                for y in (e['from'][1],e['to'][1]):
                    for z in (e['from'][2],e['to'][2]):
                        p=vertex((x,y,z),e.get('rotation'));vertices.append(p)
                        assert all(-16<=v<=32 for v in p),f'{name}: vértice girado fuera de límites {p}'
        report[name]={'elements':len(els),'rotated_bounds':[[round(min(p[i] for p in vertices),4) for i in range(3)],
                                                          [round(max(p[i] for p in vertices),4) for i in range(3)]]}
    for name in IDS:
        parent='power_cable_ground' if name=='power_cable' else name
        dump(f'models/item/{name}.json',{'parent':f'tecnocinetico:block/{parent}',
                                       'display':{'gui':inventory(geometry[parent])}})
        if name in ('street_lamp_pole','utility_pole'):state=axis_states(name)
        elif name in ('street_lamp_head','street_lamp_bent'):
            state={'variants':{f'facing={face}':dict({'model':f'tecnocinetico:block/{name}'},**({'y':yaw} if yaw else {}))
                               for face,yaw in (('north',0),('east',90),('south',180),('west',270))}}
        elif name=='utility_crossarm':
            state={'variants':{f'axis={axis}':dict({'model':f'tecnocinetico:block/{name}'},**({'y':yaw} if yaw else {})) for axis,yaw in (('x',0),('z',90))}}
        else:
            state={'variants':{f'axis={axis},hanging={str(hanging).lower()}':dict({'model':f'tecnocinetico:block/power_cable_{"hanging" if hanging else "ground"}'},**({'y':yaw} if yaw else {}))
                               for axis,yaw in (('x',0),('z',90)) for hanging in (True,False)}}
        dump(f'blockstates/{name}.json',state)
    (OUT/'model_report.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    contract={
        'units':'píxeles, 16 por bloque; formas antes de rotar el estado',
        'collision_suggestions':{
            'street_lamp_pole':[[6,0,6,10,16,10]],
            'utility_pole':[[5,0,5,11,16,11]],
            'street_lamp_head':[[6,0,6,10,5.5,10],[6.8,4.3,-5.7,9.2,6.2,7],[4.4,1.4,-14.1,11.6,5.25,-3.4]],
            'street_lamp_bent':[[6,0,6,10,4,10],[6,3,-2,10,13.5,9],[4.7,5,-10.2,11.3,13.5,-3]],
            'utility_crossarm':[[5,0,5,11,16,11],[-1,8,2.8,17,10.4,5.3]],
            'power_cable':[]},
        'notes':[
            'Propiedades exactamente como el encargo; sin propiedades ni predicados nuevos.',
            'Head y bent base miran NORTH (-Z). Facing indica el voladizo.',
            'Cruceta axis=X: travesaño X y conductores Z; axis=Z: travesaño Z y conductores X.',
            'Cruceta reemplaza al quinto bloque: cuatro utility_pole y una utility_crossarm.',
            'Cable hanging: eje X por defecto, tres conductores en z=2,8,14; extremos y=14.8, centro y=12.315. Rotar 90° para eje Z.',
            'Los cables de las crucetas llegan a ambos bordes del bloque en y=14.8, listos para vecinos hanging a la misma altura.',
            'Comba modular por bloque; tramos largos repiten la onda. Una única comba de varios bloques requiere geometría o estados adicionales del código.',
            'Fustes horizontales siguen centrados en el bloque: farola y=6..10, madera y=5..11. Apoyarlos en escombros o terreno para que no floten.',
            'Con facing solamente, la cabeza caída usa su orientación normal a ras de suelo, como en la vista previa. Una cabeza completamente tumbada en X necesita una orientación adicional del código.',
            'Colisión sugerida simplificada; accesorios pequeños y conductores no necesitan colisión. Los bounds visuales completos están en model_report.json.',
            'Recomendado noOcclusion para estas formas abiertas, sin emisión de luz. Los cristales rotos son fragmentos geométricos opacos; no requieren render translúcido.'
        ]}
    (OUT/'integracion.json').write_bytes((json.dumps(contract,ensure_ascii=False,indent=2)+'\n').encode('utf-8'))
    print('Generados 6 blockstates, 7 modelos de bloque, 6 de objeto y 4 PNG. Solo arte.')


if __name__=='__main__':main()
