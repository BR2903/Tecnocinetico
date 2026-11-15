"""End parte 4: bloques de la red y élitros de placas. Arte propio, sin píxeles vanilla.
python tools/end_part4_gen.py
Solo escribe los recursos de estos seis ids y las vistas en build/end_part4/.
"""
import copy,json,math,random
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw
from nether_remodel_gen import transform
from end_part2_gen import native_polygons
from preview_combustion_drill import font
import render_zbuffer as rz

ROOT=Path(__file__).resolve().parent.parent
A=ROOT/'src/main/resources/assets/tecnocinetico'
OUT=ROOT/'build/end_part4'
IDS=('network_plating','server_rack','network_terminal','data_crystal','data_cable')
MAG=(192,76,255,255);PINK=(255,79,216,255);LIT=(239,176,255,255)
FILES=[]

def save(im,path):
    path.parent.mkdir(parents=True,exist_ok=True);im.save(path);FILES.append(str(path.relative_to(ROOT)).replace('\\','/'))

def write(path,data):
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');FILES.append(str(path.relative_to(ROOT)).replace('\\','/'))

def metal(seed,base=(48,47,54)):
    rng=random.Random(seed);im=Image.new('RGBA',(16,16))
    for y in range(16):
        for x in range(16):
            k=rng.choice((-3,-2,-1,0,0,1,2,3));im.putpixel((x,y),tuple(v+k for v in base)+(255,))
    return im

def textures():
    im=metal(140);d=ImageDraw.Draw(im)
    d.rectangle((0,0,15,15),outline=(24,23,29));d.line((1,1,14,1),fill=(82,78,89));d.line((1,1,1,14),fill=(70,67,76))
    d.line((14,3,14,14),fill=(30,29,36));d.line((3,14,14,14),fill=(31,29,37))
    for x,y in ((2,2),(13,2),(2,13),(13,13)):
        d.point((x,y),fill=(101,94,110));d.point((x+1,y+1),fill=(21,20,26))
    d.line((5,14,9,14),fill=(87,43,99));d.point((10,14),fill=(111,53,128))
    d.line((5,3,9,3),fill=(43,41,49));d.point((11,10),fill=(69,64,76))
    save(im,A/'textures/block/network_plating.png')
    front=metal(141,(38,36,44));d=ImageDraw.Draw(front)
    d.rectangle((0,0,15,15),outline=(18,16,23));d.line((1,1,1,14),fill=(103,94,115));d.line((14,1,14,14),fill=(77,69,87))
    for i,y in enumerate((2,5,8,11)):
        d.rectangle((2,y,13,y+2),fill=(19,16,25));d.line((3,y,11,y),fill=(70,63,81));d.line((4,y+1,8,y+1),fill=(92,83,104));d.point((12,y+1),fill=MAG if i%2 else PINK)
    d.line((4,14,10,14),fill=(18,15,24));d.point((12,14),fill=(111,48,139))
    save(front,A/'textures/block/server_rack_front.png')
    side=metal(142);d=ImageDraw.Draw(side);d.rectangle((2,2,13,13),fill=(27,25,33),outline=(75,68,85))
    for y in (4,6,8,10,12):d.line((4,y,11,y),fill=(11,10,15));d.line((4,y+1,11,y+1),fill=(55,49,64))
    d.point((2,2),fill=(123,110,133));d.point((13,13),fill=(99,88,111));save(side,A/'textures/block/server_rack_side.png')
    top=metal(143,(49,47,55));d=ImageDraw.Draw(top);d.rectangle((1,1,14,14),outline=(34,31,40));d.line((3,2,12,2),fill=(79,72,89));save(top,A/'textures/block/server_rack_top.png')
    back=metal(144,(40,38,46));d=ImageDraw.Draw(back);d.rectangle((3,2,12,13),outline=(82,71,96));d.rectangle((5,4,10,6),fill=(15,12,21));d.line((6,9,9,9),fill=(104,87,119));d.line((6,10,9,10),fill=(22,18,30));d.line((8,11,8,15),fill=(18,15,24));save(back,A/'textures/block/server_rack_back.png')
    term=metal(145,(45,41,51));d=ImageDraw.Draw(term);d.rectangle((1,1,14,11),fill=(100,87,112));d.rectangle((2,2,13,10),fill=(12,8,20));d.line((3,3,7,3),fill=MAG)
    for x,y,w,c in ((3,5,3,PINK),(7,5,4,MAG),(3,7,2,MAG),(6,7,4,MAG),(4,9,5,PINK)):
        d.line((x,y,x+w-1,y),fill=c)
    d.point((11,9),fill=LIT);d.rectangle((2,12,13,14),fill=(26,22,32))
    for x in (3,5,7,9,11):d.point((x,12),fill=(132,115,145));d.point((x,14),fill=(78,65,91))
    d.line((5,15,10,15),fill=(18,15,24));save(term,A/'textures/block/network_terminal_front.png')
    crystal=Image.new('RGBA',(16,16));d=ImageDraw.Draw(crystal)
    def shard(poly,shade,highlight):
        d.polygon(poly,fill=shade);xs=[p[0] for p in poly];ys=[p[1] for p in poly];x=(min(xs)+max(xs))//2
        d.line((x,min(ys)+1,x,max(ys)-1),fill=highlight)
    shard([(4,6),(6,8),(6,14),(4,15),(2,12),(2,8)],(102,39,140),MAG)
    shard([(11,4),(14,7),(13,13),(10,15),(9,12),(9,7)],(121,41,161),PINK)
    shard([(7,0),(10,3),(10,13),(7,15),(5,12),(5,3)],(163,53,211),LIT)
    d.polygon([(7,1),(9,3),(8,11),(7,13)],fill=MAG);d.point((7,2),fill=(255,210,255));d.line((3,15,12,15),fill=(70,31,90));save(crystal,A/'textures/block/data_crystal.png')
    cable=Image.new('RGBA',(16,16));d=ImageDraw.Draw(cable)
    for y in range(16):
        for i,x in enumerate((4,7,10)):
            d.line((x,y,x+1,y),fill=(21,19,27));d.point((x+((y+i)%3==0),y),fill=(66,60,75))
        d.point((8,y),fill=MAG if y%4 else PINK)
    for y in (3,11):d.line((3,y,12,y),fill=(36,31,44));d.point((8,y),fill=(157,67,197))
    save(cable,A/'textures/block/data_cable.png')

def facing():return {'variants':{f'facing={n}':dict(model='',y=y) for n,y in [('north',0),('east',90),('south',180),('west',270)]}}

def blocks():
    for name in IDS:
        if name in ('server_rack','network_terminal'):
            state=facing()
            for v in state['variants'].values():v['model']='tecnocinetico:block/'+name
            model={'parent':'minecraft:block/cube','textures':{
                'particle':'tecnocinetico:block/'+name+'_front','north':'tecnocinetico:block/'+name+'_front',
                'south':'tecnocinetico:block/server_rack_back','east':'tecnocinetico:block/server_rack_side','west':'tecnocinetico:block/server_rack_side',
                'up':'tecnocinetico:block/server_rack_top','down':'tecnocinetico:block/server_rack_top'}}
        else:
            state={'variants':{'':{'model':'tecnocinetico:block/'+name}}}
            if name=='network_plating':model={'parent':'minecraft:block/cube_all','textures':{'all':'tecnocinetico:block/network_plating'}}
            elif name=='data_crystal':model={'parent':'minecraft:block/cross','render_type':'minecraft:cutout','textures':{'cross':'tecnocinetico:block/data_crystal'}}
            else:
                # Solo los extremos north/south cortan los conductores; a los
                # lados east/west no hay cable y dibujar caras crearía tiras flotantes.
                fs={f:{'texture':'#all','uv':[0,0,16,16] if f in ('up','down') else [0,15,16,16]} for f in ('north','south','up','down')}
                fs['down']['cullface']='down'
                model={'parent':'minecraft:block/block','render_type':'minecraft:cutout','textures':{'all':'tecnocinetico:block/data_cable','particle':'tecnocinetico:block/data_cable'},'elements':[{'from':[0,0,0],'to':[16,1,16],'faces':fs}]}
        write(A/f'blockstates/{name}.json',state);write(A/f'models/block/{name}.json',model);write(A/f'models/item/{name}.json',{'parent':f'tecnocinetico:block/{name}'})

def elytra():
    im=Image.new('RGBA',(64,32));tile=Image.new('RGBA',(10,20));d=ImageDraw.Draw(tile)
    mask=Image.new('L',(10,20));md=ImageDraw.Draw(mask);md.polygon([(4,0),(9,0),(9,16),(7,19),(1,17),(0,12),(0,6)],fill=255)
    rng=random.Random(456)
    for y in range(20):
        for x in range(10):
            k=rng.randrange(-3,4);tile.putpixel((x,y),(53+k,49+k,62+k,255))
    d.line((8,0,8,16),fill=(111,96,125));d.line((9,0,9,16),fill=(23,19,30))
    for y in (3,7,11,15):
        d.line((0,y,7,y),fill=(18,15,24));d.line((1,y+1,7,y+1),fill=(98,83,112))
    for y in (4,8,12):d.line((2,y,4,y),fill=(138,68,173));d.point((6,y+1),fill=(155,140,165))
    d.line((7,2,7,11),fill=MAG);d.rectangle((4,13,7,17),fill=(22,18,29));d.rectangle((5,13,6,15),fill=(116,102,129));d.line((5,16,6,16),fill=(13,9,20));d.line((5,17,6,17),fill=PINK)
    d.point((6,18),fill=LIT);tile.putalpha(mask)
    # UV vanilla: box10x20x2, offset22,0. Back is reversed relative to front.
    im.paste(tile,(24,2));im.paste(tile.transpose(Image.Transpose.FLIP_LEFT_RIGHT),(36,2))
    for y in range(20):
        for x in range(2):
            im.putpixel((22+x,2+y),(60,51,70,int(mask.getpixel((0,y)))))
            im.putpixel((34+x,2+y),(91,77,105,int(mask.getpixel((9,y)))))
    for x in range(10):
        for y in range(2):
            im.putpixel((24+x,y),(89,76,102,int(mask.getpixel((x,0)))))
            im.putpixel((34+x,y),(42,34,52,int(mask.getpixel((x,19)))))
    save(im,A/'textures/entity/thruster_elytra.png')
    icon=Image.new('RGBA',(16,16));d=ImageDraw.Draw(icon)
    d.polygon([(5,2),(7,3),(6,9),(4,13),(0,14),(1,7)],fill=(59,53,69),outline=(23,19,30))
    d.polygon([(10,2),(8,3),(9,9),(11,13),(15,14),(14,7)],fill=(73,63,85),outline=(23,19,30))
    for y in (6,9,12):
        d.line((2,y,5,y-1),fill=(121,104,136));d.line((10,y-1,13,y),fill=(136,116,150))
    d.line((6,3,4,9),fill=MAG);d.line((9,3,11,9),fill=PINK)
    for x in (4,10):
        d.rectangle((x,10,x+1,13),fill=(17,13,23));d.line((x,10,x+1,10),fill=(153,136,167));d.point((x,13),fill=PINK)
    save(icon,A/'textures/item/thruster_elytra.png');write(A/'models/item/thruster_elytra.json',{'parent':'minecraft:item/generated','textures':{'layer0':'tecnocinetico:item/thruster_elytra'}})

def block_mesh(name):
    raw=json.loads((A/f'models/block/{name}.json').read_text())
    if name=='data_cable':return raw
    if name=='data_crystal':
        tex={'all':'tecnocinetico:block/data_crystal'};els=[]
        for i,v in enumerate([[(1,16,1),(15,16,15),(15,0,15),(1,0,1)],[(15,16,1),(1,16,15),(1,0,15),(15,0,1)]]):
            els.append({'part':'cross_'+str(i),'from':[1,0,1],'to':[15,16,15],'preview_vertices':{'north':v},'faces':{'north':{'texture':'#all','uv':[0,0,16,16]}}})
        return {'textures':tex,'elements':els}
    tex=raw['textures'];fs={f:{'texture':'#all' if name=='network_plating' else '#'+f,'uv':[0,0,16,16]} for f in rz.SHADE}
    return {'textures':tex,'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':fs}]}

def merge(scene,model,at,yaw=0):
    prefix='m'+str(len(scene['elements']))+'_';scene['textures'].update({prefix+k:v for k,v in model['textures'].items()})
    for element in model['elements']:
        e=copy.deepcopy(element);vertices={}
        for f,fd in e['faces'].items():
            fd['texture']='#'+prefix+fd['texture'][1:]
            vv=e.get('preview_vertices',{}).get(f,rz.corners(e['from'],e['to'],f));m=transform({'at':at,'rot':(0,math.radians(yaw),0)})
            vertices[f]=[(m@np.array([*v,1]))[:3].tolist() for v in vv]
        e['preview_vertices']=vertices;pts=np.array([v for vv in vertices.values() for v in vv]);e['from']=pts.min(0).tolist();e['to']=pts.max(0).tolist();scene['elements'].append(e)

def elytra_mesh(opened=False):
    els=[]
    def add(c,at,rot,texture,uvsize,inflate=0):
        mat=transform({'at':at,'rot':rot});vertices={};fs={};center=np.array(c['xyz'])+np.array(c['size'])/2
        for face,vv,rect in native_polygons(c):
            points=[]
            for q in vv:
                q=np.array(q,dtype=float)+np.sign(np.array(q)-center)*inflate;v=mat@np.array([*q,1]);points.append([-float(v[0]),24-float(v[1]),float(v[2])])
            vertices[face]=points;fs[face]={'texture':'#'+texture,'uv':[v/uvsize[i%2]*16 for i,v in enumerate(rect)]}
        pts=np.array([v for vv in vertices.values() for v in vv]);els.append({'part':texture,'from':pts.min(0).tolist(),'to':pts.max(0).tolist(),'preview_vertices':vertices,'faces':fs})
    # Maniquí de proporciones vanilla, textura propia solo de preview.
    for f,s,at in [((-4,-8,-4),(8,8,8),(0,0,0)),((-4,0,-2),(8,12,4),(0,0,0)),((-2,-2,-2),(4,12,4),(-6,2,0)),((-2,-2,-2),(4,12,4),(6,2,0)),((-2,0,-2),(4,12,4),(-2,12,0)),((-2,0,-2),(4,12,4),(2,12,0))]:
        add({'xyz':f,'size':s,'vanilla_uv':(0,0)},at,(0,0,0),'dummy',(32,32))
    for side in (-1,1):
        f=(-10,0,0) if side==1 else (0,0,0)
        add({'xyz':f,'size':(10,20,2),'vanilla_uv':(22,0),'mirror':side==-1},(side*5,0,2),(.34906584 if opened else .2617994,0,-side*(math.pi/2 if opened else .2617994)),'wings',(64,32),1)
    return {'textures':{'dummy':'tecnocinetico:entity/_end_part4_dummy_preview','wings':'tecnocinetico:entity/thruster_elytra'},'elements':els}

def previews():
    sheet=Image.new('RGB',(1800,1200),(27,30,35));d=ImageDraw.Draw(sheet)
    for i,name in enumerate(IDS):
        pic=rz.render(block_mesh(name),205,24,21,size=600);ImageDraw.Draw(pic).text((18,15),name,font=font(25),fill='#dec5ef');pic.save(OUT/f'{name}.png');sheet.paste(pic,((i%3)*600,(i//3)*600))
    sheet.save(OUT/'bloques.png')
    for name in ('server_rack','network_terminal'):
        grid=Image.new('RGB',(1800,600))
        for i,(yaw,label) in enumerate([(205,'frente'),(25,'detras'),(115,'lateral')]):
            pic=rz.render(block_mesh(name),yaw,24,21,size=600);ImageDraw.Draw(pic).text((18,15),label,font=font(26),fill='#dec5ef');grid.paste(pic,(i*600,0))
        grid.save(OUT/f'{name}_caras.png')
    scene={'textures':{},'elements':[]}
    for x in range(5):
        for y in range(3):merge(scene,block_mesh('server_rack' if x in (1,2) else 'network_terminal' if x==3 and y==1 else 'network_plating'),(x*16,y*16,0))
    for x in range(5):
        for z in range(1,4):merge(scene,block_mesh('network_plating'),(x*16,-16,-z*16))
    for z in range(1,4):merge(scene,block_mesh('data_cable'),(2*16,0,-z*16))
    merge(scene,block_mesh('data_crystal'),(5,0,-32));merge(scene,block_mesh('data_crystal'),(62,0,-48));merge(scene,block_mesh('network_terminal'),(48,0,-32))
    rz.render(scene,205,20,9,size=1400).save(OUT/'escena_red.png');(OUT/'escena_red.json').write_text(json.dumps(scene),encoding='utf-8')
    dummy=Image.new('RGBA',(32,32),(111,115,124,255));dd=ImageDraw.Draw(dummy)
    for y in range(32):dd.line((0,y,31,y),fill=(112+y%3*4,115+y%3*4,125+y%3*4))
    dummy.save(OUT/'maniqui_preview.png');original=rz.tex_path
    def local(ref,tex):
        while ref.startswith('#'):ref=tex[ref[1:]]
        return OUT/'maniqui_preview.png' if ref.endswith('_end_part4_dummy_preview') else original(ref,tex)
    rz.tex_path=local
    try:
        wingsheet=Image.new('RGB',(1800,900),(27,30,35))
        for i,(opened,yaw,label) in enumerate([(False,25,'reposo · espalda'),(False,205,'reposo · frente'),(True,25,'vuelo · alas desplegadas')]):
            model=elytra_mesh(opened);pic=rz.render(model,yaw,12,21 if not opened else 15,size=900);ImageDraw.Draw(pic).text((20,18),label,font=font(28),fill='#dec5ef');pic.save(OUT/f'elitros_{i}.png');wingsheet.paste(pic.resize((600,600)),(i*600,0));(OUT/f'elitros_{i}.json').write_text(json.dumps(model),encoding='utf-8')
        icon=Image.open(A/'textures/item/thruster_elytra.png').resize((192,192),Image.Resampling.NEAREST);wingsheet.paste(icon,(50,650),icon);ImageDraw.Draw(wingsheet).text((270,720),'thruster_elytra · icono 16x16',font=font(25),fill='#dec5ef');wingsheet.save(OUT/'elitros.png')
    finally:rz.tex_path=original
    overview=Image.new('RGB',(1800,2100),(27,30,35));overview.paste(sheet,(0,0));overview.paste(Image.open(OUT/'escena_red.png').resize((900,900)),(0,1200));overview.paste(Image.open(OUT/'elitros_0.png'),(900,1200));overview.save(OUT/'lamina.png')

def main():
    OUT.mkdir(parents=True,exist_ok=True);textures();blocks();elytra();previews()
    for name in FILES:
        if name.endswith('.png'):
            size=Image.open(ROOT/name).size;assert size==((64,32) if name.endswith('textures/entity/thruster_elytra.png') else (16,16)),name
    info={'facing':'Frente nativo north (-Z). north=0, east=90, south=180, west=270; render rotado por blockstate.',
          'terminal_entry':'Variantes solo facing: entry 0..7 se ignora visualmente, sin crear 32 variantes.',
          'data_crystal':'Modelo minecraft:block/cross; render_type minecraft:cutout ya incluido. Requiere noOcclusion en la clase de bloque; forma sugerida [2,0,2,14,16,14] px o sin colisión según diseño.',
          'data_cable':'Un elemento [0,0,0,16,1,16] px, cutout incluido; noOcclusion; forma de alfombra [0,0,0,16,1,16]. Sin propiedades ni conexiones calculadas.',
          'elytra':'Textura 64x32, UV vanilla offset(22,0), caja 10x20x2 con CubeDeformation(1). Modelo vanilla intacto: una tobera pintada por ala, sin geometría extra. Seleccionar tecnocinetico:textures/entity/thruster_elytra.png en la capa de élitros para este objeto.',
          'glow':'No hay mapas emisivos nuevos ni animación; el código decide luz del cristal y propulsión.',
          'removed':['DataWormModel.java','textures/entity/data_worm.png','textures/entity/data_worm_glow.png'],'part3':'tools/end_part3_gen.py solo genera el espectro.'}
    (OUT/'integracion.json').write_text(json.dumps(info,indent=2,ensure_ascii=False)+'\n',encoding='utf-8');(OUT/'archivos.json').write_text(json.dumps(FILES+['tools/end_part4_gen.py'],indent=2)+'\n',encoding='utf-8')
    print('End parte 4: cinco bloques planos 16x16 y elitros 16x16/64x32. Vistas: build/end_part4/.')

if __name__=='__main__':main()
