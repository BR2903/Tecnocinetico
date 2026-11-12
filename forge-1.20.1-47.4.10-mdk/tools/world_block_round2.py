"""Arte de segunda ronda. Siluetas vegetales dibujadas, perfiles I/H y píxel 16x16.
Entrada pública: world_blocks_gen.py --remodel-only --variant A|B [--block <id>].
"""
import copy,json,math,random,shutil
from pathlib import Path
from PIL import Image,ImageDraw

ROOT=Path(__file__).resolve().parent.parent
A=ROOT/'src/main/resources/assets/tecnocinetico'
OUT=ROOT/'build/remodelar_bloques'
R2=OUT/'ronda2'
WEIGHTS=(4,3,2,1)
# Cuatro contornos pintados: derrames asimétricos, ventanas de hormigón y matas bajas aisladas.
MATS=(
('######......####','########...#####','#######.....####','######.......###','#######.......##',
 '#####...........','######..........','.####...........','..##............','..###...........',
 '...#............','................','.............#..','............###.','...........####.','............##..'),
('..#######.......','.##########.....','...#########....','.....########...','......######....',
 '......#######...','.......#####....','........####....','........###.....','.........##.....',
 '..........#.....','................','##..............','###.............','##..............','#...............'),
('##.........#####','####.......#####','###.........####','.##.........###.','..#........#####',
 '............###.','.............##.','.............#..','................','................',
 '......###.......','....#######.....','...#########....','...#######......','....#####.......','.....##.........'),
('..........######','........########','.......#########','.......########.','........######..',
 '........#####...','.........####...','...........#....','................','................',
 '#...............','###.............','#####...........','######..........','#######.........','#####...........'))


def save(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes((json.dumps(data,ensure_ascii=False,indent=2)+'\n').encode('utf-8'))


def field(base,seed,noise=3):
    im=Image.new('RGBA',(16,16));r=random.Random(seed)
    for y in range(16):
        for x in range(16):
            k=r.randrange(-noise,noise+1);im.putpixel((x,y),tuple(max(0,min(255,c+k)) for c in base)+(255,))
    return im


def moss(v,n=0,top=False):
    im=field((133,134,125) if v=='A' else (119,124,114),920+n);d=ImageDraw.Draw(im)
    for pts,col in (([(1,6),(3,5),(4,7),(2,8)],(114,115,106)),
                    ([(10,11),(14,10),(15,13),(12,14)],(145,144,130)),
                    ([(7,2),(10,3),(9,5),(7,4)],(121,122,112))):d.polygon(pts,fill=col)
    r=random.Random(1201+n*97+(v=='B')*42)
    d.line([(12,0),(11,3),(12,5),(10,7),(11,9)],fill=(89,94,81))
    colors=((35,58,26),(48,77,31),(65,96,37),(85,115,45),(113,136,60)) if v=='A' else (
            (47,69,34),(65,90,40),(89,117,48),(116,140,59),(143,158,78))
    mask=[list(row) for row in MATS[n]]
    if v=='B':
        mask=[list(reversed(row)) for row in MATS[(n+2)%4]]
        for yy in range(2,9):
            for xx in range(1,15):
                if mask[yy][xx]=='#' and r.random()<.3:mask[yy][xx-1]='#'
    if top:
        other=MATS[(n+1)%4]
        mask=[['#' if mask[y][x]=='#' or other[15-x][y]=='#' else '.' for x in range(16)] for y in range(16)]
    for y in range(16):
        for x in range(16):
            if mask[y][x]!='#':continue
            below=y==15 or mask[y+1][x]!='#';above=y==0 or mask[y-1][x]!='#'
            tone=0 if below else 3 if above else r.choice((1,2,2,3,4))
            if not below and (x//2+y//3+n)%4==0:tone=min(4,tone+1)
            d.point((x,y),fill=colors[tone])
            if below and y<15 and r.random()<.32:d.point((x,y+1),fill=(70,79,53))
    return im


def beam(v,end=False):
    im=field((86,92,89) if v=='A' else (63,77,66),2102);d=ImageDraw.Draw(im)
    if end:
        d.rectangle((0,0,15,3),fill=(111,104,86));d.rectangle((0,12,15,15),fill=(89,83,66))
        d.line((1,1,14,1),fill=(151,148,128));d.line((1,14,14,14),fill=(50,50,41));d.line((7,3,7,12),fill=(117,118,103))
    else:
        d.rectangle((0,0,1,15),fill=(125,132,122));d.rectangle((14,0,15,15),fill=(49,55,49))
        d.polygon([(0,0),(6,0),(5,3),(7,5),(4,6),(4,9),(1,12),(0,11)],fill=(103,60,34))
        d.line([(0,1),(3,1),(3,4),(5,5),(3,7),(2,10)],fill=(153,93,47))
        d.polygon([(11,10),(15,8),(15,15),(8,15),(10,13)],fill=(85,49,29))
        d.line([(12,11),(14,10),(14,14),(10,15)],fill=(130,77,36));d.line((7,0,7,15),fill=(107,114,103) if v=='A' else (88,100,77))
    if v=='B':
        d.polygon([(7,1),(13,0),(13,4),(10,5),(9,8),(6,7)],fill=(140,75,39));d.line([(8,1),(11,2),(10,4),(8,5)],fill=(178,105,48))
    return im


def log(v,end=False):
    if end:
        im=field((164,155,128) if v=='A' else (51,49,42),3231,4);d=ImageDraw.Draw(im)
        for y in range(16):
            for x in range(16):
                dx,dy=x-6.5,y-8.5;r=math.sqrt(dx*dx*.87+dy*dy)+.55*math.sin(x*1.3+y*.7)
                if v=='A':
                    if int(r*1.35)%4==0:im.putpixel((x,y),(117,108,87,255))
                    if r>8.2:im.putpixel((x,y),(82,80,66,255))
                else:
                    if r<3.2:im.putpixel((x,y),(23,25,22,255))
                    elif int(r*1.6)%3==0:im.putpixel((x,y),(91,88,73,255))
        for pts in ([(6,8),(9,9),(11,12),(15,14)],[(6,8),(4,6),(2,2)],[(6,8),(4,11),(3,15)]):d.line(pts,fill=(49,48,38) if v=='A' else (17,20,17))
        if v=='A':d.line([(6,9),(8,10),(9,12)],fill=(206,193,154))
        return im
    if v=='A':
        im=field((148,145,127),3121,4);d=ImageDraw.Draw(im)
        d.polygon([(0,0),(3,0),(3,4),(2,6),(3,10),(1,12),(2,15),(0,15)],fill=(76,78,65))
        d.polygon([(14,0),(15,0),(15,15),(12,15),(13,11),(12,8),(14,5)],fill=(92,92,76))
        d.polygon([(5,0),(8,0),(7,4),(9,7),(8,11),(9,15),(6,15),(6,10),(4,7)],fill=(183,174,144))
        d.line([(6,0),(5,4),(6,7),(5,9),(6,13),(6,15)],fill=(104,104,86));d.line([(11,0),(10,5),(11,9),(10,13)],fill=(113,114,93))
        d.ellipse((7,6,11,11),outline=(96,95,76));d.point((9,9),fill=(63,66,53));d.line([(2,1),(1,4),(2,7)],fill=(128,128,107))
    else:
        im=field((39,43,38),3127);d=ImageDraw.Draw(im)
        for x,y,h in ((1,0,5),(6,3,6),(12,0,8),(3,9,6),(10,11,4)):
            d.line([(x,y),(x+1,y+2),(x,y+h)],fill=(16,22,18));d.line((x+1,y+1,x+2,y+2),fill=(79,81,66))
        d.line([(0,7),(4,6),(7,8),(10,7),(15,9)],fill=(23,28,23))
        d.polygon([(7,0),(10,0),(9,4),(11,7),(9,9),(7,6),(8,3)],fill=(132,123,96));d.line([(9,0),(8,3),(10,6),(9,8)],fill=(77,76,59))
    return im


def sand(v):
    if v=='A':
        im=field((58,65,64),4211,3);d=ImageDraw.Draw(im)
        # Flujo vítreo continuo y escamas con reflejo; arena residual solo en los bordes.
        d.polygon([(0,0),(4,0),(3,2),(1,3),(0,2)],fill=(151,130,91))
        d.polygon([(8,15),(12,13),(15,14),(15,15)],fill=(119,105,77))
        d.polygon([(0,8),(5,5),(12,6),(15,3),(15,8),(9,11),(3,12)],fill=(31,42,43))
        d.line([(0,8),(5,5),(11,6),(15,3)],fill=(103,124,123))
        d.line([(3,9),(7,7),(10,8)],fill=(64,86,86))
        d.line([(7,1),(10,2),(13,1),(15,2)],fill=(81,99,97))
        d.line((4,5,6,5),fill=(168,178,165));d.line((11,5,12,5),fill=(136,155,149))
        for x,y in ((1,1),(3,0),(12,14),(14,15)):d.point((x,y),fill=(184,159,111))
        for x,y in ((1,12),(12,3),(13,8)):d.point((x,y),fill=(20,31,31));d.point((x,y+1),fill=(85,103,98))
    else:
        im=field((162,154,126),4217,4);d=ImageDraw.Draw(im)
        d.polygon([(1,2),(5,0),(8,2),(7,5),(3,6),(1,4)],fill=(104,77,40));d.polygon([(3,2),(5,1),(7,2),(6,4),(3,4)],fill=(166,117,53))
        d.line([(3,2),(5,1),(6,2)],fill=(210,177,101))
        d.polygon([(10,10),(14,8),(15,10),(14,14),(10,15),(8,13)],fill=(114,91,48));d.line([(10,11),(13,9),(14,10)],fill=(206,174,95))
        d.line([(0,10),(4,8),(7,9),(9,7),(10,3),(15,2)],fill=(104,107,86));d.line([(1,11),(4,9),(6,10)],fill=(191,185,150))
        for x,y in ((2,13),(9,1),(12,5),(6,12)):d.point((x,y),fill=(71,81,66));d.point((x+1,y),fill=(205,196,154))
    return im


def textures(v='A'):
    result={'mossy_concrete':moss(v),'rusted_metal_beam':beam(v),'rusted_metal_beam_top':beam(v,True),
            'dead_log':log(v),'dead_log_top':log(v,True),'vitrified_sand':sand(v)}
    for n in range(4):result[f'mossy_concrete_side_{n}']=moss(v,n);result[f'mossy_concrete_top_{n}']=moss(v,n,True)
    return result


def beam_boxes(v):
    return ([[2,0,2,14,16,4.5],[7,0,4.5,9,16,11.5],[2,0,11.5,14,16,14]] if v=='A' else
            [[1,0,1,15,16,4],[6.5,0,4,9.5,16,12],[1,0,12,15,16,15]])


def box(bounds,textures):
    f,t=bounds[:3],bounds[3:];faces={}
    for face in ('north','south','west','east','up','down'):
        uv=([f[0],f[2],t[0],t[2]] if face in ('up','down') else [f[2],16-t[1],t[2],16-f[1]] if face in ('east','west') else [f[0],16-t[1],t[0],16-f[1]])
        faces[face]={'texture':textures[face] if isinstance(textures,dict) else textures,'uv':uv}
    return {'from':f,'to':t,'faces':faces}


def beam_model(v='A'):
    surfaces={f:'#end' if f in ('up','down') else '#side' for f in ('north','south','east','west','up','down')}
    els=[box(b,surfaces) for b in beam_boxes(v)]
    front,back=(2,14) if v=='A' else (1,15)
    els.append(box([5.5,5,front-.25,10.5,11,front+.1],surfaces))
    for x in (6.15,9.15):
        for y in (6,9.4):els.append(box([x,y,front-.65,x+.7,y+.7,front-.24],surfaces))
    els.append(box([5.5,5,back-.1,10.5,11,back+.25],surfaces))
    for e in els[:3]:
        for f in ('up','down'):e['faces'][f]['cullface']=f
    return {'parent':'minecraft:block/block','textures':{'side':'tecnocinetico:block/rusted_metal_beam','end':'tecnocinetico:block/rusted_metal_beam_top','particle':'tecnocinetico:block/rusted_metal_beam'},'elements':els}


def family():
    models={};states={}
    def suffix(n):return '' if n==0 else f'_m{n}'
    def choices(name,**rotate):return [dict({'model':'tecnocinetico:block/'+name+suffix(n),'weight':WEIGHTS[n]},**rotate) for n in range(4)]
    for n in range(4):
        s=suffix(n);side=f'tecnocinetico:block/mossy_concrete_side_{n}';top=f'tecnocinetico:block/mossy_concrete_top_{n}'
        tex={'side':side,'top':top,'bottom':'tecnocinetico:block/concrete','particle':side}
        cube={**tex,'up':top,'down':tex['bottom'],**{f:f'tecnocinetico:block/mossy_concrete_side_{(n+k)%4}' for k,f in enumerate(('north','east','south','west'))}}
        models['mossy_concrete'+s]={'parent':'minecraft:block/cube','textures':cube}
        for end,parent in (('_slab','slab'),('_slab_top','slab_top'),('_stairs','stairs'),('_stairs_inner','inner_stairs'),('_stairs_outer','outer_stairs')):
            models['mossy_concrete'+end+s]={'parent':'minecraft:block/'+parent,'textures':tex}
            if 'stairs' in end:
                models['mossy_concrete'+end+'_upper'+s]={'parent':'minecraft:block/'+parent,'textures':{**tex,'top':tex['bottom'],'bottom':top}}
        shapes={'_wall_post':[[4,0,4,12,16,12]],'_wall_side':[[5,0,0,11,14,8]],'_wall_side_tall':[[5,0,0,11,16,8]],
                '_wall_inventory':[[0,0,6,4,16,10],[12,0,6,16,16,10],[4,3,7,12,13,9]]}
        for end,boxes in shapes.items():
            refs={f:'#top' if f=='up' else '#bottom' if f=='down' else '#side' for f in ('north','south','east','west','up','down')}
            els=[box(b,refs) for b in boxes]
            for e in els:
                if e['from'][1]==0:e['faces']['down']['cullface']='down'
            # Coronación común: evita dos colores coplanares donde se solapan brazo y poste.
            models['mossy_concrete'+end+s]={'parent':'minecraft:block/block','textures':{**tex,'top':'tecnocinetico:block/mossy_concrete_top_0'},'elements':els}
    states['mossy_concrete']={'variants':{'':[dict({'model':'tecnocinetico:block/mossy_concrete'+suffix(n),'weight':WEIGHTS[n]},**({'y':yaw} if yaw else {})) for n in range(4) for yaw in (0,90,180,270)]}}
    states['mossy_concrete_slab']={'variants':{'type=bottom':choices('mossy_concrete_slab'),'type=top':choices('mossy_concrete_slab_top'),'type=double':choices('mossy_concrete')}}
    variants={}
    for facing,y0 in (('east',0),('south',90),('west',180),('north',270)):
        for half in ('bottom','top'):
            for shape in ('straight','inner_left','inner_right','outer_left','outer_right'):
                end='_inner' if 'inner' in shape else '_outer' if 'outer' in shape else ''
                yaw=(y0+(-90 if shape.endswith('left') else 0)+(90 if half=='top' and shape!='straight' else 0))%360;r={}
                if half=='top':r['x']=180
                if yaw:r['y']=yaw
                if r:r['uvlock']=True
                variants[f'facing={facing},half={half},shape={shape}']=choices('mossy_concrete_stairs'+end+('_upper' if half=='top' else ''),**r)
    states['mossy_concrete_stairs']={'variants':variants}
    parts=[{'when':{'up':'true'},'apply':choices('mossy_concrete_wall_post')}]
    for face,yaw in (('north',0),('east',90),('south',180),('west',270)):
        for height,end in (('low','side'),('tall','side_tall')):
            parts.append({'when':{face:height},'apply':choices('mossy_concrete_wall_'+end,**({'y':yaw,'uvlock':True} if yaw else {'uvlock':True}))})
    states['mossy_concrete_wall']={'multipart':parts}
    return models,states


def snapshot():
    if (R2/'actual').exists():return
    for folder,patterns in {'textures/block':['mossy_concrete*.png','rusted_metal_beam*.png','dead_log*.png','vitrified_sand*.png'],
                            'models/block':['mossy_concrete*.json','rusted_metal_beam.json','dead_log.json','vitrified_sand.json'],
                            'blockstates':['mossy_concrete*.json','rusted_metal_beam.json','dead_log.json','vitrified_sand.json']}.items():
        for pattern in patterns:
            for src in (A/folder).glob(pattern):
                p=R2/'actual'/src.relative_to(A);p.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,p)


def install(v='A',block='all'):
    snapshot();images=textures(v);models,states=family()
    for name,im in images.items():
        if block=='all' or name==block or name.startswith(block+'_'):im.save(A/f'textures/block/{name}.png')
    if block in ('all','mossy_concrete'):
        for name,m in models.items():save(A/f'models/block/{name}.json',m)
        for name,s in states.items():save(A/f'blockstates/{name}.json',s)
    if block in ('all','rusted_metal_beam'):save(A/'models/block/rusted_metal_beam.json',beam_model(v))
    for option in ('A','B'):
        dst=R2/option
        for name,im in textures(option).items():
            p=dst/f'textures/block/{name}.png';p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
        for name,m in {**models,'rusted_metal_beam':beam_model(option)}.items():save(dst/f'models/block/{name}.json',m)
        for name,s in states.items():save(dst/f'blockstates/{name}.json',s)
    shapes={}
    for option in ('A','B'):
        front,back=(2,14) if option=='A' else (1,15)
        shapes[option]=beam_boxes(option)+[[5.5,5,front-.65,10.5,11,front],[5.5,5,back,10.5,11,back+.25]]
    save(OUT/'integracion_bloques.json',{'rusted_metal_beam':{'axis':'x|y|z (sin cambios)','shapes_pixels_axis_y':shapes,
         'code_needed':'noOcclusion(); unión de las cinco cajas y giro con axis. Cajas 4/5 son envolventes de placa y remaches. Codex no modifica Java.'},
         'mossy_concrete':'Cuatro texturas laterales, cuatro superiores; 16 alternativas ponderadas en el bloque y cuatro por losa/escalera/pieza de muro. Estados y colisión conservados.'})
    files=[f'src/main/resources/assets/tecnocinetico/textures/block/{name}.png' for name in images]
    files += [f'src/main/resources/assets/tecnocinetico/models/block/{name}.json' for name in (*models,'rusted_metal_beam')]
    files += [f'src/main/resources/assets/tecnocinetico/blockstates/{name}.json' for name in states]
    files += ['tools/world_blocks_gen.py','tools/world_block_remodel.py','tools/world_block_round2.py',
              'tools/preview_world_block_remodel.py','tools/preview_world_block_round2.py','docs/HERRAMIENTAS.md']
    save(OUT/'archivos_ronda2.json',files)
    print('Segunda ronda instalada:',v,block,'(solo arte, texturas 16x16)')
