"""Intrusor de Red y dos muñecas. python tools/network_intruder_gen.py
Solo arte: ejecutar después de generadores anteriores de atlas/reloj/GUI.
Conserva la GUI salvo el nuevo marco de 20x20 y todos los píxeles antiguos del atlas.
"""
import copy,json,math,re
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw
from wrist_map_gen import geometry as clock_geometry,player as clock_player,regions
from tactical_gui_gen import slot,BG,GHOST,CYAN_DIM
from nether_remodel_gen import number,verify_surfaces
from end_part2_gen import native_polygons
from preview_combustion_drill import font
import render_zbuffer as rz

ROOT=Path(__file__).resolve().parent.parent
A=ROOT/'src/main/resources/assets/tecnocinetico'
JAVA=ROOT/'src/main/java/com/bryanamaya/tecnocinetico/client/WornGearModel.java'
OUT=ROOT/'build/intrusor'
BEGIN='        // BEGIN NETWORK INTRUDER GENERATED: tools/network_intruder_gen.py'
END='        // END NETWORK INTRUDER GENERATED'
GREEN=(124,216,96,255);HOT=(210,255,170,255)
FILES=[]

def save(im,path):
    path.parent.mkdir(parents=True,exist_ok=True);im.save(path);FILES.append(str(path.relative_to(ROOT)).replace('\\','/'))

def icon():
    im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
    d.rectangle((6,0,9,15),fill=(43,41,46));d.line((6,0,6,15),fill=(91,85,100))
    d.polygon([(4,3),(10,3),(12,5),(12,12),(10,13),(3,13),(2,11),(2,5)],fill=(63,58,73),outline=(119,110,135))
    d.line((4,4,9,4),fill=(161,151,175));d.rectangle((4,5,10,10),fill=(17,29,20))
    d.line([(5,6),(7,6),(7,9),(9,9)],fill=GREEN);d.line((9,6,9,7),fill=HOT)
    d.rectangle((12,6,14,11),fill=(32,29,39),outline=(112,103,128));d.point((13,8),fill=GREEN);d.point((13,10),fill=(152,144,163))
    d.line((10,1,10,3),fill=(116,107,132));d.point((10,0),fill=GREEN)
    d.point((4,12),fill=(159,152,169));d.line((6,14,8,14),fill=(26,24,29))
    save(im,A/'textures/item/network_intruder.png')
    p=A/'models/item/network_intruder.json';p.write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'tecnocinetico:item/network_intruder'}},indent=2)+'\n',encoding='utf-8');FILES.append(str(p.relative_to(ROOT)).replace('\\','/'))

def geometry():
    pieces=copy.deepcopy(clock_geometry()[:4])
    # Correillas compartidas: reutilizan sus UV aprobadas sin volver a pintarlas.
    for e in pieces:e['reuse']=True
    def box(n,xyz,size,uv,mat='nulite',scale=None):
        e=dict(name=n,xyz=xyz,size=size,uv=uv,mat=mat)
        if scale:e['texscale']=scale
        pieces.append(e)
    box('module',(3.42,6.2,-2.05),(.9,3.7,4.1),(0,82))
    box('rim',(4.34,6.6,-1.7),(.18,2.9,3.4),(13,82),'edge')
    box('display',(4.54,6.85,-1.45),(.12,2.35,2.8),(25,24),'hack_screen',.25)
    box('core_cage',(3.5,6.6,2.08),(1.02,2.9,.68),(23,82),'dark')
    box('data_core',(3.68,7,2.79),(.7,2,.18),(29,82),'nulite')
    box('core_contacts',(3.78,7.3,2.99),(.5,.22,.12),(33,82),'green')
    box('wire',(3.6,9.4,-2.1),(.35,.4,4.9),(36,82),'rubber')
    box('aerial_base',(3.6,5.7,-.8),(.65,.4,.65),(49,82),'edge')
    box('aerial',(3.78,4.6,-.64),(.28,1.1,.28),(54,82),'nulite')
    box('aerial_tip',(3.73,4.45,-.69),(.38,.23,.38),(57,82),'green')
    box('status_a',(4.54,9.3,-.9),(.12,.2,.55),(60,82),'green')
    box('status_b',(4.54,9.3,.35),(.12,.2,.55),(60,82),'green')
    return pieces

def mirrored(pieces):
    result=copy.deepcopy(pieces)
    for e in result:e['xyz']=(-e['xyz'][0]-e['size'][0],*e['xyz'][1:]);e['mirror']=True
    return result

def occupied(text):
    text=re.sub(re.escape(BEGIN)+'.*?'+re.escape(END)+'\n','',text,flags=re.S)
    mask=np.zeros((128,128),bool)
    for m in re.finditer(r'texOffs\((\d+),\s*(\d+)\)\.addBox\(([^)]+)\)',text):
        u,v=int(m[1]),int(m[2]);args=m[3].split(',');w,h,d=[float(n.strip().rstrip('Ff')) for n in args[3:6]]
        scale=float(args[-1].strip().rstrip('Ff')) if 'CubeDeformation.NONE' in m[3] else 1
        x,y,X,Y=u/scale,v/scale,(u+2*(w+d))/scale,(v+h+d)/scale
        mask[math.floor(y):min(128,math.ceil(Y)),math.floor(x):min(128,math.ceil(X))]=True
    return mask

def atlas():
    text=JAVA.read_text(encoding='utf-8');used=occupied(text);mask=np.zeros((128,128),bool)
    im=Image.open(A/'textures/entity/worn_gear.png').convert('RGBA');glow=Image.open(A/'textures/entity/worn_gear_glow.png').convert('RGBA');old=np.array(im);oldg=np.array(glow)
    painted=set()
    for e in geometry():
        if e.get('reuse'):continue
        spec=(e['uv'],e['size'],e['mat'])
        if spec in painted:continue
        painted.add(spec)
        for face,rect in regions(e).items():
            x,y,X,Y=math.floor(rect[0]),math.floor(rect[1]),math.ceil(rect[2]),math.ceil(rect[3]);assert 0<=x<X<=128 and 0<=y<Y<=128
            assert not used[y:Y,x:X].any(),'Intersección con UV antiguas: '+e['name'];mask[y:Y,x:X]=True
            for yy in range(y,Y):
                for xx in range(x,X):
                    a,b=xx-x,yy-y;w,h=X-x,Y-y;lit=False
                    if e['mat']=='hack_screen':
                        c=(17,30,20,255)
                        if a%3==0 or b%3==0:c=(33,64,35,255)
                        if face=='east' and ((b==2 and 1<=a<=w-3) or (a==w//2 and 2<=b<=h-3) or (b==h-3 and 2<=a<=w//2)):
                            c=GREEN;lit=True
                        if face=='east' and (a,b) in ((w-3,h-3),(w-4,h-3)):c=HOT;lit=True
                    elif e['mat']=='green':c=GREEN if (a+b)%3 else HOT;lit=True
                    elif e['mat']=='rubber':c=(37,34,42,255) if a%2 else (51,46,56,255)
                    elif e['mat']=='dark':c=(34,30,42,255) if b else (100,91,113,255)
                    elif e['mat']=='edge':c=(136,128,149,255) if a==0 or b==0 else (63,56,75,255)
                    else:
                        k=9 if b==0 else -14 if a==w-1 else 0;c=(99+k,91+k,112+k,255)
                        if w>3 and h>3 and (a,b)==(w-2,h-2):c=(162,152,173,255)
                    im.putpixel((xx,yy),c);glow.putpixel((xx,yy),c if lit else (0,0,0,0))
    assert np.array_equal(np.array(im)[~mask],old[~mask]);assert np.array_equal(np.array(glow)[~mask],oldg[~mask])
    save(im,A/'textures/entity/worn_gear.png');save(glow,A/'textures/entity/worn_gear_glow.png')
    return int(mask.sum())

def java():
    text=JAVA.read_text(encoding='utf-8')
    for field in ('intruder','intruderRight','wristMapRight'):
        if f'public final ModelPart {field};' not in text:text=text.replace('    public WornGearModel(ModelPart root) {',f'    public final ModelPart {field};\n\n    public WornGearModel(ModelPart root) {{')
    for field,bone in [('intruder','intruder'),('intruderRight','intruder_right'),('wristMapRight','wrist_map_right')]:
        assignment=f'        this.{field} = root.getChild("{bone}");'
        if assignment not in text:text=text.replace('    public WornGearModel(ModelPart root) {','    public WornGearModel(ModelPart root) {\n'+assignment)
    lines=[BEGIN]
    for name,pieces in [('intruder',geometry()),('intruder_right',mirrored(geometry())),('wrist_map_right',mirrored(clock_geometry()))]:
        lines.append(f'        root.addOrReplaceChild("{name}", CubeListBuilder.create()'+('.mirror(true)' if name.endswith('right') else ''))
        for e in pieces:
            args=', '.join(number(n) for n in (*e['xyz'],*e['size']))
            if 'texscale' in e:args+=', net.minecraft.client.model.geom.builders.CubeDeformation.NONE, 0.25F, 0.25F'
            lines.append(f'                .texOffs({e["uv"][0]}, {e["uv"][1]}).addBox({args})')
        lines.append('                , PartPose.ZERO);')
    lines.append(END);block='\n'.join(lines)+'\n'
    if BEGIN in text:text=re.sub(re.escape(BEGIN)+'.*?'+re.escape(END)+'\n',lambda _:block,text,flags=re.S)
    else:text=text.replace('        return LayerDefinition.create(mesh, 128, 128);',block+'        return LayerDefinition.create(mesh, 128, 128);')
    JAVA.write_text(text,encoding='utf-8');FILES.append(str(JAVA.relative_to(ROOT)).replace('\\','/'))

def gui():
    path=A/'textures/gui/tactical_gear.png';im=Image.open(path).convert('RGBA');old=np.array(im);assert im.size==(256,256)
    d=ImageDraw.Draw(im);d.rectangle((96,104,115,123),fill=BG+(255,));slot(d,98,106,CYAN_DIM)
    # Silueta de módulo con antena y cartucho lateral, distinta del reloj.
    d.rectangle((104,107,107,120),fill=GHOST);d.rectangle((100,110,110,117),outline=GHOST,width=2)
    d.rectangle((112,111,113,116),fill=GHOST);d.line((109,107,109,109),fill=GHOST)
    d.rectangle((102,112,108,115),fill=(139,139,139));d.line([(103,113),(105,113),(105,115),(107,115)],fill=GHOST)
    allowed=np.zeros((256,256),bool);allowed[104:124,96:116]=True;assert not np.any(np.any(np.array(im)!=old,axis=2)&~allowed)
    save(im,path);im.crop((0,0,176,248)).resize((1056,1488),Image.Resampling.NEAREST).save(OUT/'gui_seis_gadgets.png')

def model(slim,swapped=False,close=None):
    m=clock_player(slim);m['elements']=[e for e in m['elements'] if e['part'] in ('head','body','left_arm','right_arm','leg')]
    if close:m['elements']=[e for e in m['elements'] if e['part']==close+'_arm']
    for side in ('left','right'):
        if close and side!=close:continue
        is_intruder=(side=='left') if swapped else (side=='right');pieces=geometry() if is_intruder else clock_geometry()
        if side=='right':pieces=mirrored(pieces)
        sx=.8 if slim else 1;dx=(-.2 if side=='left' else .2) if slim else 0;at=(5 if side=='left' else -5,2.5 if slim else 2,0)
        for e in pieces:
            c=dict(e,vanilla_uv=e['uv']);vertices={};fs={}
            for f,vv,rect in native_polygons(c):
                vertices[f]=[[-(q[0]*sx+dx+at[0]),24-q[1]-at[1],q[2]+at[2]] for q in vv]
                fs[f]={'texture':'#worn','uv':[v/e.get('texscale',1)/8 for v in rect]}
            pts=np.array([q for vv in vertices.values() for q in vv]);m['elements'].append({'part':('intruder_' if is_intruder else 'clock_')+side+'_'+e['name'],'from':pts.min(0).tolist(),'to':pts.max(0).tolist(),'faces':fs,'preview_vertices':vertices})
    return m

def preview():
    iconim=Image.open(A/'textures/item/network_intruder.png').resize((320,320),Image.Resampling.NEAREST);sheet=Image.new('RGB',(500,410),(27,30,35));sheet.paste(iconim,(90,25),iconim);ImageDraw.Draw(sheet).text((110,360),'network_intruder',font=font(25),fill='#a6d69a');sheet.save(OUT/'icono.png')
    Image.new('RGBA',(32,32),(113,112,106,255)).save(OUT/'maniqui.png');original=rz.tex_path
    def local(ref,tex):
        while ref.startswith('#'):ref=tex[ref[1:]]
        return OUT/'maniqui.png' if ref.endswith('_wrist_preview_dummy') else original(ref,tex)
    rz.tex_path=local
    try:
        gallery=Image.new('RGB',(1800,3600),(27,30,35))
        for swapped in (False,True):
            for slim in (False,True):
                row=int(swapped)*2+int(slim)
                for col,yaw in enumerate((205,155)):
                    pic=rz.render(model(slim,swapped),yaw,10,22,size=900)
                    label=('Intrusor I · Reloj D' if swapped else 'Reloj I · Intrusor D')+' · '+('fino' if slim else 'normal')
                    ImageDraw.Draw(pic).text((20,15),label,font=font(26),fill='#a6d69a');pic.save(OUT/f'puesto_{row}_{col}.png');gallery.paste(pic,(col*900,row*900))
        gallery.save(OUT/'combinaciones.png')
        detail=Image.new('RGB',(1800,900),(27,30,35))
        for i,side in enumerate(('left','right')):
            m=model(False,side=='left',side);pic=rz.render(m,270 if side=='left' else 90,8,52,size=900);ImageDraw.Draw(pic).text((20,15),'Intrusor · '+side,font=font(28),fill='#a6d69a');pic.save(OUT/f'detalle_{side}.png');detail.paste(pic,(i*900,0));(OUT/f'modelo_{side}.json').write_text(json.dumps(m),encoding='utf-8')
        detail.save(OUT/'detalle_intrusor.png')
    finally:rz.tex_path=original

def main():
    OUT.mkdir(parents=True,exist_ok=True);icon();area=atlas();java();gui();preview()
    info={'fields':{'intruder':'intruder; PlayerModel.leftArm','intruderRight':'intruder_right; PlayerModel.rightArm','wristMapRight':'wrist_map_right; PlayerModel.rightArm','wristMap':'wrist_map anterior; PlayerModel.leftArm'},
          'normal':'part.xScale=1F; part.x=0F;',
          'slim_left':'part.xScale=0.8F; part.x=-0.2F;',
          'slim_right':'part.xScale=0.8F; part.x=+0.2F;',
          'slim_note':'x en píxeles de ModelPart; misma corrección para reloj e intrusor. Restaurar antes de dibujar cada jugador y reutilizar en pasada emisiva. La posición del hueso aporta y=2/2.5; sin corrección Y extra.',
          'color':'Verde ácido #7CD860, luces cálidas verdosas; nulita gris violácea y módulo de datos retenido mecánicamente. Ningún cian/magenta nuevo.',
          'gui':'Solo [96,104,116,124) cambia: hueco x=98,y=106; marco desde97,105; botón dinámico en117,108 sin pintar.',
          'atlas_added_area_px':area,'atlas':'128x128 existente; nuevas regiones en y=82 y sector x>=100,y>=96. Correillas y reloj derecho reutilizan UV existentes sin repintarlas.',
          'regenerate':'python tools/network_intruder_gen.py; ejecutar al final, después de regeneradores anteriores de gadgets/atlas/GUI.'}
    (OUT/'integracion.json').write_text(json.dumps(info,indent=2,ensure_ascii=False)+'\n',encoding='utf-8');(OUT/'archivos.json').write_text(json.dumps(FILES+['tools/network_intruder_gen.py'],indent=2)+'\n',encoding='utf-8')
    print('Intrusor: icono 16x16, tres piezas nuevas en WornGearModel, atlas verde y sexta ranura. Vistas en build/intrusor/.')

if __name__=='__main__':main()
