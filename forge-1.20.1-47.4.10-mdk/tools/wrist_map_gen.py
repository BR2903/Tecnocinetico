"""Reloj de Navegación: iconos, cinco huecos de GUI y brazalete con radar ámbar.
python tools/wrist_map_gen.py
Aplicar después de regenerar worn_gear_texture.py / tactical_gui_gen.py.
No modifica sus generadores; conserva todos los píxeles fuera de las zonas nuevas.
"""
import copy,json,math,re
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw
from tactical_gui_gen import slot,GHOSTS,GHOST,BG,CYAN_DIM
from nether_remodel_gen import faces,transform,number,verify_surfaces
from end_part2_gen import native_polygons
from preview_combustion_drill import font
import render_zbuffer as rz

ROOT=Path(__file__).resolve().parent.parent
A=ROOT/'src/main/resources/assets/tecnocinetico'
JAVA=ROOT/'src/main/java/com/bryanamaya/tecnocinetico/client/WornGearModel.java'
OUT=ROOT/'build/wrist_map'
FILES=[]
BEGIN='        // BEGIN WRIST MAP GENERATED: tools/wrist_map_gen.py'
END='        // END WRIST MAP GENERATED'
AMBER=(232,166,57,255);HOT=(255,218,124,255)

def save(im,path):
    path.parent.mkdir(parents=True,exist_ok=True);im.save(path);FILES.append(str(path.relative_to(ROOT)).replace('\\','/'))

def icons():
    clock=Image.new('RGBA',(16,16));d=ImageDraw.Draw(clock)
    d.rectangle((6,0,9,15),fill=(43,42,43));d.line((6,0,6,15),fill=(95,92,86))
    for y in (1,14):d.line((7,y,8,y),fill=(24,23,25))
    d.polygon([(4,2),(11,2),(13,4),(13,11),(11,13),(4,13),(2,11),(2,4)],fill=(32,32,35),outline=(103,101,99))
    d.line((4,3,10,3),fill=(148,144,132));d.rectangle((4,4,11,10),fill=(24,23,16))
    for x in (5,8,11):d.line((x,4,x,10),fill=(67,53,25))
    for y in (5,8,10):d.line((4,y,11,y),fill=(67,53,25))
    d.line([(5,9),(7,9),(7,6),(10,6)],fill=(166,117,44));d.point((9,8),fill=HOT);d.point((8,9),fill=AMBER);d.point((10,9),fill=AMBER)
    d.point((5,12),fill=(137,133,120));d.point((10,12),fill=(137,133,120));d.line((14,6,14,8),fill=(133,126,110))
    save(clock,A/'textures/item/wrist_map.png')
    satellite=Image.new('RGBA',(16,16));d=ImageDraw.Draw(satellite)
    for x in (0,11):
        d.rectangle((x,5,x+4,11),fill=(27,33,56),outline=(124,127,137))
        d.line((x+2,6,x+2,10),fill=(76,82,107));d.line((x+1,8,x+3,8),fill=(76,82,107));d.point((x+1,6),fill=(114,121,145))
    d.line((4,8,11,8),fill=(166,157,128));d.rectangle((6,4,9,12),fill=(76,75,73),outline=(32,32,35));d.line((7,4,8,4),fill=(204,191,150));d.line((7,5,7,10),fill=(142,134,110));d.point((8,8),fill=AMBER);d.line((7,12,8,12),fill=(37,36,38))
    d.line((7,1,7,4),fill=(153,150,138));d.line((5,1,9,1),fill=(148,144,132));d.point((5,0),fill=(203,199,181));d.point((9,0),fill=(203,199,181))
    save(satellite,A/'textures/item/satellite.png')
    for name in ('wrist_map','satellite'):
        path=A/f'models/item/{name}.json';path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':f'tecnocinetico:item/{name}'}},indent=2)+'\n',encoding='utf-8');FILES.append(str(path.relative_to(ROOT)).replace('\\','/'))

def gui():
    path=A/'textures/gui/tactical_gear.png';im=Image.open(path).convert('RGBA');before=np.array(im)
    assert im.size==(256,256)
    # Únicamente la columna vieja: el personaje comienza en x=28 y queda intacto.
    d=ImageDraw.Draw(im);d.rectangle((6,16,27,124),fill=BG+(255,))
    for y,name in zip((18,40,62,84,106),('face','back','feet','belt','wrist')):
        slot(d,8,y,CYAN_DIM)
        if name!='wrist':
            for yy,row in enumerate(GHOSTS[name]):
                for xx,char in enumerate(row):
                    if char=='#':d.point((8+xx,y+yy),fill=GHOST)
        else:
            d.rectangle((14,y+1,17,y+14),fill=GHOST)
            d.rectangle((11,y+4,20,y+11),outline=GHOST,width=2)
            d.rectangle((13,y+6,18,y+9),fill=(139,139,139))
            d.point((16,y+8),fill=GHOST)
    changed=np.any(before!=np.array(im),axis=2);allowed=np.zeros((256,256),bool);allowed[16:125,6:28]=True
    assert not np.any(changed&~allowed),'GUI: se ha tocado algo fuera de la columna'
    save(im,path);im.crop((0,0,176,218)).resize((1056,1308),Image.Resampling.NEAREST).save(OUT/'gui_cinco_ranuras.png')
    return int(changed.sum())

def geometry():
    # Hueso leftArm: normal x=-1..3, slim=-1..2; y=-2..10.
    return [
        dict(name='strap_front',xyz=(-1.15,6.5,-2.26),size=(4.3,2.8,.26),uv=(1,96),mat='strap'),
        dict(name='strap_back',xyz=(-1.15,6.5,2.0),size=(4.3,2.8,.26),uv=(1,96),mat='strap'),
        dict(name='strap_inner',xyz=(-1.41,6.5,-2),size=(.26,2.8,4),uv=(14,96),mat='strap'),
        dict(name='strap_outer',xyz=(3.15,6.5,-2),size=(.26,2.8,4),uv=(14,96),mat='strap'),
        dict(name='case',xyz=(3.42,6.4,-1.9),size=(.72,3.2,3.8),uv=(27,96),mat='steel'),
        dict(name='bezel',xyz=(4.16,6.6,-1.65),size=(.18,2.8,3.3),uv=(40,96),mat='edge'),
        dict(name='screen',xyz=(4.36,6.8,-1.4),size=(.14,2.4,2.8),uv=(18,24),mat='screen',texscale=.25),
        dict(name='knob',xyz=(3.85,6,-.3),size=(.4,.35,.6),uv=(54,96),mat='steel')]

def regions(e):
    scale=e.get('texscale',1)
    return {f:tuple(v/scale for v in rect) for f,rect in faces(*e['uv'],*e['size']).items()}

def used_before(text):
    # Réserver aussi les zones inutilisées de chaque boîte ancienne, pas seulement son alpha.
    occupied=np.zeros((128,128),bool)
    text=re.sub(re.escape(BEGIN)+'.*?'+re.escape(END)+'\n','',text,flags=re.S)
    for match in re.finditer(r'texOffs\((\d+),\s*(\d+)\)\.addBox\(([^)]+)\)',text):
        u,v=int(match[1]),int(match[2]);args=match[3].split(',')
        try:w,h,d=[float(n.strip().rstrip('Ff')) for n in args[3:6]]
        except ValueError:raise ValueError('No se pudo comprobar una UV antigua: '+match[0])
        occupied[v:min(128,math.ceil(v+h+d)),u:min(128,math.ceil(u+2*(w+d)))]=True
    return occupied

def worn():
    text=JAVA.read_text(encoding='utf-8');occupied=used_before(text);mask=np.zeros((128,128),bool)
    im=Image.open(A/'textures/entity/worn_gear.png').convert('RGBA');glow=Image.open(A/'textures/entity/worn_gear_glow.png').convert('RGBA');old=np.array(im);oldg=np.array(glow)
    assert im.size==glow.size==(128,128)
    painted=set()
    for e in geometry():
        spec=(e['uv'],e['size'],e['mat'])
        if spec in painted:continue
        painted.add(spec)
        for face,rect in regions(e).items():
            x0,y0,x1,y1=rect;X,Y,XX,YY=math.floor(x0),math.floor(y0),math.ceil(x1),math.ceil(y1)
            assert 0<=X<XX<=128 and 0<=Y<YY<=128
            assert not occupied[Y:YY,X:XX].any(),'UV del reloj solapa un gadget existente'
            mask[Y:YY,X:XX]=True
            for y in range(Y,YY):
                for x in range(X,XX):
                    xx,yy=x-X,y-Y;w,h=XX-X,YY-Y;lit=False
                    if e['mat']=='screen':
                        c=(25,23,14,255)
                        if xx%3==0 or yy%3==0:c=(78,59,24,255)
                        if face=='east':
                            if (yy==h//2 and 1<=xx<=w-3) or (xx==w//3 and 1<=yy<=h//2):c=(183,122,38,255);lit=True
                            if (xx,yy) in ((w//2,h//2),(w//2-1,h//2+1),(w//2+1,h//2+1)):c=HOT;lit=True
                        elif xx%4==0:c=AMBER;lit=True
                    elif e['mat']=='strap':c=(49,48,46,255) if (xx+yy)%3 else (61,58,53,255)
                    elif e['mat']=='edge':c=(116,112,102,255) if xx==0 or yy==0 else (51,50,48,255)
                    else:c=(84,83,79,255) if yy%3 else (123,118,104,255)
                    im.putpixel((x,y),c);glow.putpixel((x,y),c if lit else (0,0,0,0))
    assert np.array_equal(np.array(im)[~mask],old[~mask]);assert np.array_equal(np.array(glow)[~mask],oldg[~mask])
    save(im,A/'textures/entity/worn_gear.png');save(glow,A/'textures/entity/worn_gear_glow.png')
    if 'public final ModelPart wristMap;' not in text:
        text=text.replace('    public WornGearModel(ModelPart root) {','    public final ModelPart wristMap;\n\n    public WornGearModel(ModelPart root) {')
    if 'this.wristMap = root.getChild("wrist_map");' not in text:
        text=text.replace('    public WornGearModel(ModelPart root) {','    public WornGearModel(ModelPart root) {\n        this.wristMap = root.getChild("wrist_map");')
    lines=[BEGIN,'        root.addOrReplaceChild("wrist_map", CubeListBuilder.create()']
    for e in geometry():
        u,v=e['uv'];args=', '.join(number(n) for n in (*e['xyz'],*e['size']))
        if 'texscale' in e:args+=', net.minecraft.client.model.geom.builders.CubeDeformation.NONE, 0.25F, 0.25F'
        lines.append(f'                .texOffs({u}, {v}).addBox({args})')
    lines+=['                , PartPose.ZERO);',END];block='\n'.join(lines)+'\n'
    if BEGIN in text:text=re.sub(re.escape(BEGIN)+'.*?'+re.escape(END)+'\n',lambda _:block,text,flags=re.S)
    else:text=text.replace('        return LayerDefinition.create(mesh, 128, 128);',block+'        return LayerDefinition.create(mesh, 128, 128);')
    JAVA.write_text(text,encoding='utf-8');FILES.append(str(JAVA.relative_to(ROOT)).replace('\\','/'))
    return int(mask.sum())

def player(slim=False,closeup=False):
    els=[]
    def add(c,at,texture,uvsize=(128,128),part='body',sx=1,dx=0):
        vertices={};fs={};scale=c.get('texscale',1)
        for f,vv,rect in native_polygons(c):
            points=[]
            for q in vv:
                x,y,z=q;x=x*sx+dx
                points.append([-(x+at[0]),24-y-at[1],z+at[2]])
            vertices[f]=points;fs[f]={'texture':'#'+texture,'uv':[v/scale/uvsize[i%2]*16 for i,v in enumerate(rect)]}
        pts=np.array([v for vv in vertices.values() for v in vv]);els.append({'part':part,'from':pts.min(0).tolist(),'to':pts.max(0).tolist(),'faces':fs,'preview_vertices':vertices})
    bones=[((-4,-8,-4),(8,8,8),(0,0,0),'head'),((-4,0,-2),(8,12,4),(0,0,0),'body'),(((-2 if slim else -3),-2,-2),(3 if slim else 4,12,4),(-5,2.5 if slim else 2,0),'right_arm'),((-1,-2,-2),(3 if slim else 4,12,4),(5,2.5 if slim else 2,0),'left_arm'),((-2,0,-2),(4,12,4),(-2,12,0),'leg'),((-2,0,-2),(4,12,4),(2,12,0),'leg')]
    for xyz,size,at,name in bones:
        if closeup and name!='left_arm':continue
        add(dict(xyz=xyz,size=size,vanilla_uv=(0,0)),at,'dummy',(32,32),name)
    for e in geometry():
        c=dict(e,vanilla_uv=e['uv']);add(c,(5,2.5 if slim else 2,0),'worn',part=e['name'],sx=.8 if slim else 1,dx=-.2 if slim else 0)
    return {'textures':{'dummy':'tecnocinetico:entity/_wrist_preview_dummy','worn':'tecnocinetico:entity/worn_gear'},'elements':els}

def preview():
    iconsheet=Image.new('RGB',(700,370),(27,30,35));d=ImageDraw.Draw(iconsheet)
    for i,name in enumerate(('wrist_map','satellite')):
        im=Image.open(A/f'textures/item/{name}.png').resize((256,256),Image.Resampling.NEAREST);iconsheet.paste(im,(i*350+45,35),im);d.text((i*350+50,310),name,font=font(27),fill='#dfd4b2')
    iconsheet.save(OUT/'iconos.png');Image.new('RGBA',(32,32),(113,112,106,255)).save(OUT/'maniqui.png')
    original=rz.tex_path
    def local(ref,tex):
        while ref.startswith('#'):ref=tex[ref[1:]]
        return OUT/'maniqui.png' if ref.endswith('_wrist_preview_dummy') else original(ref,tex)
    rz.tex_path=local
    try:
        sheet=Image.new('RGB',(1800,1800),(27,30,35))
        for row,slim in enumerate((False,True)):
            for col,yaw in enumerate((205,295)):
                m=player(slim);pic=rz.render(m,yaw,10,22,size=900);ImageDraw.Draw(pic).text((20,15),'Brazo '+('fino' if slim else 'normal')+f' · vista {col+1}',font=font(28),fill='#e3c990');pic.save(OUT/f'puesto_{"slim" if slim else "normal"}_{col}.png');sheet.paste(pic,(col*900,row*900))
            m=player(slim,True);pic=rz.render(m,270,8,52,size=900);pic.save(OUT/f'detalle_{"slim" if slim else "normal"}.png');(OUT/f'modelo_{"slim" if slim else "normal"}.json').write_text(json.dumps(m),encoding='utf-8')
        sheet.save(OUT/'reloj_puesto.png')
    finally:rz.tex_path=original

def main():
    OUT.mkdir(parents=True,exist_ok=True);icons();changed=gui();pixels=worn();preview()
    info={'field':'public final ModelPart wristMap;','bone':'wrist_map','attach_to':'PlayerModel.leftArm; coordenadas locales al hueso, PartPose.ZERO.',
          'normal':'wristMap.xScale=1F; wristMap.x=0F;',
          'slim':'Misma pieza: wristMap.xScale=0.8F; wristMap.x=-0.2F; (x en píxeles de ModelPart). Restaurar estos dos valores para cada jugador antes de dibujar, tanto pasada base como glow.',
          'arm_reference':'Vanilla leftArm normal x=-1..3, slim x=-1..2. y=6..9.6 local para el reloj. La posición 2/2.5 del hueso la aporta leftArm.translateAndRotate; no sumar otra corrección Y.',
          'texture':'worn_gear.png y worn_gear_glow.png existentes; solo UV nuevas a partir de y=96. La pantalla usa texScale=.25 para tener detalle de radar sin ampliar el atlas.',
          'gui_slots':[{'x':8,'y':y,'type':name} for y,name in zip((18,40,62,84,106),('face','back','feet','belt','wrist'))],
          'buttons':'Espacio reservado a la derecha; dibujar por código en x=27,y=ranura+2.',
          'regenerate':'python tools/wrist_map_gen.py; repetir después de regeneradores antiguos de GUI/atlas. No ejecuta otros generadores ni altera sus archivos.',
          'checks':{'gui_changed_pixels':changed,'gui_outside_column':'idéntica; solo x=6..27,y=16..124','atlas_added_area_px':pixels,'atlas_old_uv':'sin intersecciones; píxeles fuera de la nueva zona idénticos'}}
    (OUT/'integracion.json').write_text(json.dumps(info,indent=2,ensure_ascii=False)+'\n',encoding='utf-8');(OUT/'archivos.json').write_text(json.dumps(FILES+['tools/wrist_map_gen.py'],indent=2)+'\n',encoding='utf-8')
    print('Reloj y satelite: iconos 16x16, cinco ranuras, wristMap y pantalla ambar. Vistas en build/wrist_map/.')

if __name__=='__main__':main()
