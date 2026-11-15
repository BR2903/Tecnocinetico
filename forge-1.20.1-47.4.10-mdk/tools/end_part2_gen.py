"""Arte original End parte 2. python tools/end_part2_gen.py

Solo escribe los recursos encargados y build/end_part2. La geometría/UV del
dragón reproduce el contrato vanilla 1.20.1; ningún píxel procede de Mojang.
Requiere Pillow, numpy y los módulos de previsualización existentes en tools/.
"""
import copy,json,math,random
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw
from nether_remodel_rigs import Rig
from nether_remodel_gen import pack,key,faces,number,transform
from end_remodel_gen import color,model as rig_model,PALETTE
from preview_combustion_drill import font
import render_zbuffer as rz

ROOT=Path(__file__).resolve().parent.parent
A=ROOT/'src/main/resources/assets/tecnocinetico'
M=ROOT/'src/main/resources/assets/minecraft'
OUT=ROOT/'build/end_part2'
JAVA=ROOT/'src/main/java/com/bryanamaya/tecnocinetico/entity/client/EndRelayTurretModel.java'
MA=(192,76,255,255);PINK=(255,79,216,255);PALE=(248,205,255,255)
FILES=[]

def save(im,path):
    path.parent.mkdir(parents=True,exist_ok=True);im.save(path);FILES.append(str(path.relative_to(ROOT)).replace('\\','/'))

def write(path,data):
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n',encoding='utf-8');FILES.append(str(path.relative_to(ROOT)).replace('\\','/'))

def turret_rig():
    r=Rig();r.part('base',at=(0,24,0));r.part('head',at=(0,8,0))
    def box(n,p,xyz,size,mat='armor',rot=(0,0,0)):r.panel(n,p,xyz,size,mat,rot)
    box('anchor_plinth','base',(-8,-2,-8),(16,2,16),'dark')
    box('anchor_plate','base',(-7.6,-3.2,-7.6),(15.2,1.1,15.2),'worn')
    for x in (-6.5,5.5):
        for z in (-6.5,5.5):box(f'bolt_{x}_{z}'.replace('-','m').replace('.','_'),'base',(x,-3.8,z),(1,.6,1),'edge')
    box('lower_server','base',(-5.5,-10,-4.4),(11,6.7,8.8))
    box('front_service','base',(-4.5,-8.9,-4.65),(9,4.7,.3),'dark')
    for i in range(3):
        box(f'rack_slot_{i}','base',(-3.8,-8.2+i*1.3,-5),(5.8,.55,.3),'worn')
        box(f'rack_fault_{i}','base',(3,-8.2+i*1.3,-5),(1,.5,.35),'magenta' if i==1 else 'violet')
    for side in (-1,1):
        for i in range(3):box(f'base_fin_{side}_{i}'.replace('-','m'),'base',(side*5.4-.25,-9+i*1.7,-3.5),(.5,.6,7),'edge')
    box('bearing_pedestal','base',(-3.5,-14.8,-3.5),(7,4.6,7),'rubber')
    box('bearing_ring','base',(-4.4,-15.8,-4.4),(8.8,.8,8.8),'edge')
    box('receiver_housing','head',(-5.4,-5.1,-3.4),(10.8,9.7,8.8))
    box('rear_bus_cover','head',(-4.8,-4.5,5.35),(9.6,8,.6),'dark')
    for i in range(4):box(f'rear_bus_{i}','head',(-3.6,-3.4+i*1.6,6),(7.2,.7,.25),'worn')
    box('top_shield','head',(-6,-5.9,-4),(12,1,9.8),'worn')
    box('front_shroud','head',(-4.8,-4.7,-6.7),(9.6,9.4,3.2),'dark')
    # Emisor cuadrado octogonal; biseles girados, hueco oscuro y lente al fondo.
    box('emitter_throat','head',(-3,-3,-10.1),(6,6,3.4),'rubber')
    for i in range(8):
        ang=i*math.pi/4;x=3.5*math.sin(ang);y=3.5*math.cos(ang)
        box(f'focus_ring_{i}','head',(x-.7,y-1,-11.5),(1.4,2,2.1),'edge',(0,0,-ang))
    box('lens_socket','head',(-2.7,-2.7,-11.7),(5.4,5.4,.6),'dark')
    box('lens','head',(-2.15,-2.15,-12),(4.3,4.3,.25),'lens')
    for n,at,size in [('iris_top',(-2.7,-3.15,-11.9),(5.4,.65,.5)),('iris_bottom',(-2.7,2.5,-11.9),(5.4,.65,.5)),
                      ('iris_left',(-3.15,-2.45,-11.9),(.65,4.9,.5)),('iris_right',(2.5,-2.45,-11.9),(.65,4.9,.5))]:box(n,'head',at,size,'worn')
    for side,s in (('left',-1),('right',1)):
        box('cheek_'+side,'head',(s*5.3-.35,-3.8,-2.8),(.7,7.5,7.4),'dark')
        for i in range(4):box(f'cooling_{side}_{i}','head',(s*5.6-.3,-3+i*1.7,-2.3),(.6,.65,6.3),'worn')
        box('signal_'+side,'head',(s*6-.1,-2.4,-2),(.2,.5,4.1),'magenta')
    box('aerial_socket','head',(3.3,-7.2,1.4),(1.4,1.1,1.4),'edge')
    box('aerial','head',(3.8,-9.7,1.9),(.4,2.4,.4),'worn')
    box('aerial_fault','head',(3.72,-9.7,1.82),(.56,.45,.56),'violet')
    box('capacitor_slide','head',(-3.6,-6.6,-2.7),(5.7,.6,5.2),'pcb')
    box('capacitor_data','capacitor_slide',(.6,-.15,.8),(4.5,.2,.5),'screen')
    # Holguras microscópicas entre piezas; ninguna cara nueva coincide en el mismo plano.
    for i,p in enumerate(r.parts):
        for c in p['cubes']:c['deform']=-(i+1)*.00009
    return r

def turret(r):
    uv,_=pack(r);im=Image.new('RGBA',(128,128));glow=Image.new('RGBA',(128,128))
    for spec,(u,v) in uv.items():
        mat,w,h,d=spec;rng=random.Random(str(spec))
        for rect in faces(u,v,w,h,d).values():
            x0,y0,x1,y1=map(math.floor,rect)
            for y in range(y0,math.ceil(rect[3])):
                for x in range(x0,math.ceil(rect[2])):
                    c=color(mat,x-x0,y-y0,math.ceil(rect[2])-x0,math.ceil(rect[3])-y0,rng);im.putpixel((x,y),c)
                    if mat in ('magenta','violet') or mat in ('lens','screen') and c[:3] not in (PALETTE['dark'],PALETTE['pcb']):glow.putpixel((x,y),c)
    for suffix,img in [('',im),('_glow',glow)]:save(img,A/f'textures/entity/end_relay_turret{suffix}.png')
    code=['package com.bryanamaya.tecnocinetico.entity.client;',
          'import net.minecraft.client.model.HierarchicalModel;','import net.minecraft.client.model.geom.ModelPart;',
          'import net.minecraft.client.model.geom.PartPose;','import net.minecraft.client.model.geom.builders.*;',
          'import net.minecraft.world.entity.Entity;',
          '/** Arte generado por tools/end_part2_gen.py. Cabeza nativa hacia -Z; renderizado habitual: +Z. */',
          'public class EndRelayTurretModel<T extends Entity> extends HierarchicalModel<T> {',
          '    private final ModelPart root, head, irisTop, irisBottom, irisLeft, irisRight, capacitor;',
          '    /** Carga normalizada; el renderer debe asignarla antes de setupAnim en cada dibujo. */',
          '    public float charge;',
          '    public EndRelayTurretModel(ModelPart bakedRoot) {',
          '        root = bakedRoot.getChild("root"); head = root.getChild("head");',
          '        irisTop = head.getChild("iris_top"); irisBottom = head.getChild("iris_bottom");',
          '        irisLeft = head.getChild("iris_left"); irisRight = head.getChild("iris_right");',
          '        capacitor = head.getChild("capacitor_slide");','    }',
          '    @Override public ModelPart root() { return root; }',
          '    @Override public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {',
          '        root.getAllParts().forEach(ModelPart::resetPose);',
          '        head.yRot = netHeadYaw * ((float)Math.PI / 180F);',
          '        head.xRot = headPitch * ((float)Math.PI / 180F);',
          '        float c = Math.max(0F, Math.min(1F, charge));',
          '        irisTop.y -= c * .75F; irisBottom.y += c * .75F;',
          '        irisLeft.x -= c * .75F; irisRight.x += c * .75F;',
          '        capacitor.z += c * .8F;','    }',
          '    public static LayerDefinition createBodyLayer() {',
          '        var mesh = new MeshDefinition();',
          '        var root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.ZERO);']
    for p in r.parts:
        b='CubeListBuilder.create()'
        for c in p['cubes']:
            u,v=uv[key(c)];b+=f'.texOffs({u}, {v}).addBox('+', '.join(number(n) for n in (*c['xyz'],*c['size']))+', new CubeDeformation('+number(c['deform'])+'))'
        code.append(f'        var {p["name"]} = {p["parent"]}.addOrReplaceChild("{p["name"]}", {b}, PartPose.offsetAndRotation('+', '.join(number(n) for n in (*p['at'],*p['rot']))+'));')
    code+=['        return LayerDefinition.create(mesh, 128, 128);','    }','}']
    JAVA.write_text('\n'.join(code)+'\n',encoding='utf-8');FILES.append(str(JAVA.relative_to(ROOT)).replace('\\','/'))
    return uv

def dragon_rig():
    """Cajas/UV oficiales comprobadas en EnderDragonRenderer.createBodyLayer 1.20.1.
    Solo en build para visualizar; nunca se sustituye la geometría del juego.
    El cuello vanilla se dibuja 5 veces delante y 12 detrás como cola.
    """
    r=Rig()
    def p(n,parent='root',at=(0,0,0),rot=(0,0,0)):r.part(n,parent,at,rot)
    def b(n,label,f,s,uv,mirror=False):
        next(q for q in r.parts if q['name']==n)['cubes'].append(dict(xyz=f,size=s,vanilla_uv=uv,material=label,deform=0,mirror=mirror))
    p('body',at=(0,4,8));b('body','body',(-12,0,-16),(24,24,64),(0,0))
    for z in (-10,10,30):b('body','dorsal',(-1,-6,z),(2,6,12),(220,53))
    # Pose de vuelo suave, con cinco módulos de cuello y doce módulos de cola.
    for i in range(5):
        n=f'neck{i}';p(n,at=(0,17-i*.65,-12-i*9.9),rot=(.065,0,0));b(n,'neck',(-5,-5,-5),(10,10,10),(192,104));b(n,'fin',(-1,-9,-3),(2,4,6),(48,0))
    for i in range(12):
        n=f'tail{i}';p(n,at=(math.sin(i*.2)*2,16+i*.55,62+i*9.8),rot=(-.055,math.pi,0));b(n,'neck',(-5,-5,-5),(10,10,10),(192,104));b(n,'fin',(-1,-9,-3),(2,4,6),(48,0))
    p('head',at=(0,13.6,-62));b('head','upperlip',(-6,-1,-24),(12,5,16),(176,44));b('head','skull',(-8,-8,-10),(16,16,16),(112,30))
    for x,mirror in [(-5,True),(3,True)]:
        b('head','horn',(x,-12,-4),(2,4,6),(0,0),mirror);b('head','nostril',(x,-3,-22),(2,2,4),(112,0),mirror)
    p('jaw','head',(0,4,-8),(.12,0,0));b('jaw','jaw',(-6,0,-16),(12,4,16),(176,65))
    for side,s in [('left',1),('right',-1)]:
        n=side+'_wing';p(n,at=(s*12,5,2),rot=(.125,-s*.25,s*.12))
        b(n,'wing_bone',(0 if s>0 else -56,-4,-4),(56,8,8),(112,88),s>0)
        b(n,'membrane',(0 if s>0 else -56,0,2),(56,0,56),(-56,88),s>0)
        tip=n+'_tip';p(tip,n,(s*56,0,0),rot=(0,-s*.08,0))
        b(tip,'wing_tip',(0 if s>0 else -56,-2,-2),(56,4,4),(112,136),s>0)
        b(tip,'membrane_tip',(0 if s>0 else -56,0,2),(56,0,56),(-56,144),s>0)
        for kind in ('front','hind'):
            n=side+'_'+kind+'_leg';p(n,at=(s*(12 if kind=='front' else 16),20 if kind=='front' else 16,2 if kind=='front' else 42),rot=(1.3 if kind=='front' else 1.0,0,s*.08))
            b(n,'limb',(-4,-4,-4) if kind=='front' else (-8,-4,-8),(8,24,8) if kind=='front' else (16,32,16),(112,104) if kind=='front' else (0,0))
            tip=n+'_tip';p(tip,n,(0,20,-1) if kind=='front' else (0,32,-4),(-.5 if kind=='front' else .5,0,0))
            b(tip,'piston',(-3,-1,-3) if kind=='front' else (-6,-2,0),(6,24,6) if kind=='front' else (12,32,12),(226,138) if kind=='front' else (196,0))
            foot=side+'_'+kind+'_foot';p(foot,tip,(0,23,0) if kind=='front' else (0,31,4),(-.8 if kind=='front' else -1.5,0,0))
            b(foot,'foot',(-4,0,-12) if kind=='front' else (-9,0,-20),(8,4,16) if kind=='front' else (18,6,24),(144,104) if kind=='front' else (112,0))
    return r

def dragon_textures(r):
    im=Image.new('RGBA',(256,256),(22,19,28,255));g=Image.new('RGBA',(256,256))
    done=set()
    for part in r.parts:
        for c in part['cubes']:
            uv=c['vanilla_uv'];w,h,d=c['size'];tag=c['material']
            if (uv,tuple(c['size'])) in done:continue
            done.add((uv,tuple(c['size'])))
            for face,(x0,y0,x1,y1) in faces(*uv,w,h,d).items():
                x0,y0,x1,y1=map(int,(x0,y0,x1,y1));fw=x1-x0;fh=y1-y0
                if not fw or not fh:continue
                rng=random.Random(str((uv,face)));tile=Image.new('RGBA',(fw,fh));td=ImageDraw.Draw(tile);gl=Image.new('RGBA',(fw,fh));gd=ImageDraw.Draw(gl)
                membrane=tag.startswith('membrane');metal=tag in ('body','skull','jaw','piston','dorsal','wing_bone','wing_tip','fin','horn')
                for y in range(fh):
                    for x in range(fw):
                        k=rng.randrange(-3,4)
                        if membrane:
                            rib=min(abs(y-(x*.36+7)),abs(y-(x*.65+14)),abs(y-(x*1.03+3)))
                            rgb=(59,44,68) if rib<.8 else (33,25,40) if rib<2 else (24,20,29)
                        elif metal:
                            # Placas grandes y asimétricas, juntas oscuras y rebabas de acero.
                            cell=(x//max(6,fw//3)+y//max(5,fh//3))%3
                            rgb=((54,51,61),(42,39,49),(65,60,71))[cell]
                            if x in (0,fw-1) or y in (0,fh-1):rgb=(23,20,29)
                            elif x==1 or y==1:rgb=(99,91,106)
                            elif (x*11+y*17)%113==0:rgb=(125,112,128)
                        else:
                            rgb=(35,29,40) if (x//3+y//4)%3 else (46,38,52)
                        tile.putpixel((x,y),tuple(max(0,v+k) for v in rgb)+(255,))
                def trace(points,c=MA):td.line(points,fill=c,width=1);gd.line(points,fill=c,width=1)
                if membrane:
                    # Pistas impresas sobre la membrana y grupos de píxeles desconectados.
                    trace([(2,fh-4),(fw//3,fh-4),(fw//3,fh//2),(fw-8,fh//2),(fw-8,5)])
                    trace([(fw//2+1,fh-9),(fw//2+1,fh//2+5),(fw-5,fh//2+5)],PINK)
                    for x,y in [(8,11),(29,38),(43,20)]:
                        if x<fw-2 and y<fh-1:td.rectangle((x,y,x+2,y),fill=MA);gd.rectangle((x,y,x+2,y),fill=MA)
                elif min(fw,fh)>=7:
                    # Cable doble escalonado, con conectores; sin símbolos místicos.
                    y=fh//2;x=fw//3
                    td.line([(2,y+1),(x,y+1),(x,y-2),(fw-3,y-2)],fill=(15,12,20,255),width=3)
                    trace([(2,y),(x,y),(x,y-3),(fw-3,y-3)],PINK if tag in ('skull','neck') else MA)
                    for xx in (2,fw-4):
                        td.rectangle((xx,max(1,y-1),xx+1,min(fh-2,y+1)),fill=(125,108,138,255))
                    if fw>14 and fh>10:
                        td.rectangle((fw-8,2,fw-3,5),fill=(14,11,21,255))
                        for xx in range(fw-7,fw-3,2):trace([(xx,3),(xx,4)],MA)
                    if tag=='body':
                        # Puertos de servicio y rejillas del módulo dorsal/lateral.
                        for yy in range(7,fh-3,9):td.line((3,yy,min(11,fw-3),yy),fill=(16,13,21,255),width=2)
                elif tag in ('fin','horn','nostril','dorsal') and fw>1 and fh>1:trace([(fw//2,1),(fw//2,fh-2)])
                if tag=='skull' and face in ('east','west'):
                    ex=fw//2;ey=max(2,fh//3)
                    td.rectangle((ex-3,ey-1,ex+3,ey+2),fill=(13,10,18,255))
                    td.rectangle((ex-2,ey,ex+2,ey+1),fill=PINK);gd.rectangle((ex-2,ey,ex+2,ey+1),fill=PINK)
                    td.point((ex,ey),fill=PALE);gd.point((ex,ey),fill=PALE)
                if membrane:
                    # Bordes propios rasgados/festoneados: el modelo vanilla deja
                    # estas membranas como planos cuadrados y la textura da la silueta.
                    mask=Image.new('L',(fw,fh));md=ImageDraw.Draw(mask)
                    outline=([(0,0),(55,0),(55,42),(49,35),(41,41),(34,44),(26,43),(18,49),(9,54),(0,55)]
                             if tag=='membrane' else
                             [(0,0),(55,0),(55,55),(48,48),(41,45),(34,40),(28,33),(22,29),(16,20),(11,18),(5,8)])
                    md.polygon(outline,fill=255)
                    mask=mask.transpose(Image.Transpose.FLIP_TOP_BOTTOM)
                    tile.putalpha(mask);gl.putalpha(Image.fromarray(np.minimum(np.array(gl.getchannel('A')),np.array(mask))))
                # Coordenadas UV negativas de las alas son deliberadas; solo sus caras de área >0 entran al atlas.
                if x0>=0 and y0>=0 and x1<=256 and y1<=256:im.paste(tile,(x0,y0));g.paste(gl,(x0,y0))
    save(im,M/'textures/entity/enderdragon/dragon.png');save(g,M/'textures/entity/enderdragon/dragon_eyes.png')

def cube_model(tex):
    return {'textures':{'all':tex},'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':{f:{'texture':'#all'} for f in ('north','south','east','west','up','down')}}]}

def stone():
    rng=random.Random(2114);im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
    for y in range(16):
        for x in range(16):
            k=rng.choice((-14,-7,-3,0,3,5,8));im.putpixel((x,y),(max(0,219+k),max(0,219+k),max(0,161+k),255))
    for x,y in [(2,3),(9,1),(12,9),(4,12),(8,7)]:
        d.line((x,y,x+1,y),fill=(171,172,120));d.point((x,y+1),fill=(236,236,181))
    crack=[(0,10),(2,10),(3,9),(4,9),(5,8),(7,8),(8,7),(8,5),(10,4),(12,4),(13,3),(15,3)]
    d.line(crack,fill=(74,47,77),width=1)
    for a,b in [(1,5),(7,11)]:d.line(crack[a:b],fill=(177,69,201),width=1)
    d.line([(7,8),(8,10),(10,11),(11,13),(11,15)],fill=(110,61,124),width=1)
    d.point((8,7),fill=PALE);d.point((12,4),fill=PINK)
    save(im,A/'textures/block/corrupted_end_stone.png')
    write(A/'blockstates/corrupted_end_stone.json',{'variants':{'':{'model':'tecnocinetico:block/corrupted_end_stone'}}})
    write(A/'models/block/corrupted_end_stone.json',{'parent':'minecraft:block/cube_all','textures':{'all':'tecnocinetico:block/corrupted_end_stone'}})
    write(A/'models/item/corrupted_end_stone.json',{'parent':'tecnocinetico:block/corrupted_end_stone'})

def items():
    # Iconos dibujados a resolución nativa, con una silueta diferente por función.
    for name in ('end_coupler','data_core','navigation_core'):
        im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
        if name=='end_coupler':
            d.polygon([(2,5),(5,2),(11,2),(14,5),(14,11),(11,14),(5,14),(2,11)],fill=(22,19,29),outline=(98,86,114))
            d.line([(3,5),(6,3),(10,3),(13,5)],fill=(148,131,165))
            d.rectangle((5,5,10,10),fill=(73,48,92));d.rectangle((6,6,9,9),fill=MA);d.rectangle((7,7,8,8),fill=(26,19,38))
            for x,y in ((1,6),(1,9),(13,6),(13,9)):d.rectangle((x,y,x+1,y+1),fill=(151,135,163))
            d.point((5,12),fill=PINK);d.point((10,12),fill=MA)
        elif name=='data_core':
            d.polygon([(4,1),(11,1),(14,4),(14,11),(11,14),(4,14),(1,11),(1,4)],fill=(24,19,31),outline=(98,83,115))
            d.rectangle((4,3,11,12),fill=(50,36,64));d.rectangle((5,4,10,11),fill=(14,10,22))
            for y,x,w in [(4,5,4),(6,7,3),(8,5,2),(10,6,4)]:d.line((x,y,x+w-1,y),fill=MA if y%4 else PINK)
            for i in range(4):d.line((0,4+i*2,2,4+i*2),fill=(165,145,177));d.line((13,4+i*2,15,4+i*2),fill=(114,98,129))
            d.point((10,4),fill=PALE);d.line((4,2,10,2),fill=(143,124,159))
        else:
            d.ellipse((3,2,12,12),fill=(36,24,49),outline=(111,73,148))
            d.polygon([(7,2),(10,5),(11,9),(7,12),(4,9),(5,5)],fill=(106,54,145))
            d.polygon([(7,3),(9,6),(7,10),(5,7)],fill=MA);d.line((7,3,7,7),fill=PALE)
            d.ellipse((0,5,15,10),outline=(157,139,177));d.line((1,9,4,11),fill=(77,63,92));d.line((11,4,14,6),fill=(185,161,203))
            d.line((2,13,4,11),fill=PINK);d.point((13,2),fill=MA)
        save(im,A/f'textures/item/{name}.png');write(A/f'models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'tecnocinetico:item/{name}'}})

def effects():
    for name in ('data_corruption','emp'):
        im=Image.new('RGBA',(18,18));d=ImageDraw.Draw(im)
        if name=='data_corruption':
            d.rectangle((3,2,13,15),fill=(32,23,43),outline=(118,81,143));d.line((5,4,11,4),fill=(175,143,196))
            for box,c in [((2,5,7,6),MA),((9,6,15,7),PINK),((5,9,12,10),MA),((1,12,5,13),PINK),((10,14,15,14),MA)]:d.rectangle(box,fill=c)
            d.rectangle((7,6,8,8),fill=(0,0,0,0));d.rectangle((12,10,14,12),fill=(0,0,0,0));d.point((15,3),fill=PALE)
        else:
            d.arc((1,1,16,16),205,330,fill=MA,width=1);d.arc((3,3,14,14),205,330,fill=PINK,width=1)
            d.rectangle((5,6,12,13),fill=(38,31,46),outline=(159,139,174));d.rectangle((7,4,10,5),fill=(148,128,163))
            d.line((7,10,10,10),fill=(91,75,104));d.line((3,15,15,3),fill=(23,15,31),width=3);d.line((3,15,15,3),fill=PINK,width=1)
        save(im,A/f'textures/mob_effect/{name}.png')
    im=Image.new('RGBA',(128,128));d=ImageDraw.Draw(im);rng=random.Random(764)
    # Centro 36..91 completamente transparente; la mayor densidad vive en los primeros 12 px del borde.
    for i in range(145):
        side=rng.randrange(4);along=rng.randrange(128);depth=int(rng.triangular(0,29,0));length=rng.randrange(2,18)
        c=MA if i%3 else PINK;c=c[:3]+(rng.randrange(35,135),)
        if side==0:box=(along,depth,min(127,along+length),depth+rng.randrange(1,3))
        elif side==1:box=(along,127-depth-rng.randrange(1,3),min(127,along+length),127-depth)
        elif side==2:box=(depth,along,depth+rng.randrange(1,3),min(127,along+length))
        else:box=(127-depth-rng.randrange(1,3),along,127-depth,min(127,along+length))
        d.rectangle(box,fill=c)
    save(im,A/'textures/misc/corruption_overlay.png')

def dragon_model(r):
    matrices={'root':np.eye(4)};els=[]
    for p in r.parts:
        mat=matrices[p['parent']]@transform(p);matrices[p['name']]=mat
        for c in p['cubes']:
            x,y,z=c['xyz'];w,h,d=c['size'];vertices={};fd={}
            for f,local_vertices,rect in native_polygons(c):
                if (rect[2]-rect[0])*(rect[3]-rect[1])==0:continue
                vv=[]
                for q in local_vertices:
                    q=mat@np.array([*q,1]);vv.append([float(q[0]),-float(q[1]),float(q[2])])
                vertices[f]=vv;u0,v0,u1,v1=rect
                fd[f]={'texture':'#all','uv':[v/16 for v in (u0,v0,u1,v1)]}
            pts=np.array([q for vv in vertices.values() for q in vv]);els.append({'part':p['name'],'from':pts.min(0).tolist(),'to':pts.max(0).tolist(),'preview_vertices':vertices,'faces':fd})
    return {'textures':{'all':'minecraft:entity/enderdragon/dragon'},'elements':els}

def native_polygons(c):
    """Orden y orientación UV exactos de ModelPart.Cube, incluidos planos y mirror.
    Contraste por vértice con las cajas vanilla horneadas en la auditoría local.
    """
    x,y,z=c['xyz'];w,h,d=c['size'];X,Y,Z=x+w,y+h,z+d
    corners={
        'up':[(X,y,Z),(x,y,Z),(x,y,z),(X,y,z)],
        'down':[(X,Y,z),(x,Y,z),(x,Y,Z),(X,Y,Z)],
        'west':[(x,y,z),(x,y,Z),(x,Y,Z),(x,Y,z)],
        'north':[(X,y,z),(x,y,z),(x,Y,z),(X,Y,z)],
        'east':[(X,y,Z),(X,y,z),(X,Y,z),(X,Y,Z)],
        'south':[(x,y,Z),(X,y,Z),(X,Y,Z),(x,Y,Z)]}
    rects=faces(*c['vanilla_uv'],w,h,d)
    for face,vertices in corners.items():
        a,b,A,B=rects[face]
        if face=='down':rect=(A,B,a,b)
        else:rect=(A,b,a,B)
        if c.get('mirror'):vertices=[(x+X-q[0],q[1],q[2]) for q in vertices]
        yield face,vertices,rect

def previews(r,uv,dragon):
    for charge in (0,1):
        pose=copy.deepcopy(r)
        for p in pose.parts:
            if p['name']=='iris_top':p['at']=(p['at'][0],p['at'][1]-charge*.75,p['at'][2])
            if p['name']=='iris_bottom':p['at']=(p['at'][0],p['at'][1]+charge*.75,p['at'][2])
            if p['name']=='iris_left':p['at']=(p['at'][0]-charge*.75,*p['at'][1:])
            if p['name']=='iris_right':p['at']=(p['at'][0]+charge*.75,*p['at'][1:])
            if p['name']=='capacitor_slide':p['at']=(*p['at'][:2],p['at'][2]+charge*.8)
        m=rig_model('end_relay_turret',pose,uv);m['textures']={'all':'tecnocinetico:entity/end_relay_turret'}
        pic=rz.render(m,205,16,22,size=900);ImageDraw.Draw(pic).text((22,20),'Repetidor · '+('cargando' if charge else 'reposo'),font=font(29),fill='#e4c9ef');pic.save(OUT/f'torreta_{"cargando" if charge else "reposo"}.png')
        (OUT/f'torreta_{charge}.json').write_text(json.dumps(m),encoding='utf-8')
    dm=dragon_model(dragon)
    (OUT/'dragon_modelo_vanilla.json').write_text(json.dumps(dm),encoding='utf-8')
    sheet=Image.new('RGB',(1800,1200),(27,30,35))
    for i,(label,yaw,pitch,scale) in enumerate([('frente',205,18,3.7),('lateral',270,12,3.9),('dorsal',25,58,3.4)]):
        pic=rz.render(dm,yaw,pitch,scale,size=1100);ImageDraw.Draw(pic).text((20,20),'Dragón · '+label,font=font(30),fill='#e4c9ef');pic.save(OUT/f'dragon_{label}.png')
        sheet.paste(pic.resize((600,600)),(i*600,0))
    # Detalle de cabeza y cuerpo, mismos cubos/UV vanilla, mayor escala para revisar los implantes.
    head=copy.deepcopy(dm);head['elements']=[e for e in head['elements'] if e['part'] in ('head','jaw')]
    pic=rz.render(head,215,17,18,size=900);pic.save(OUT/'dragon_cabeza.png');sheet.paste(pic.resize((600,600)),(0,600))
    stone_pic=rz.render(cube_model('tecnocinetico:block/corrupted_end_stone'),205,25,22,size=600);stone_pic.save(OUT/'piedra.png');sheet.paste(stone_pic,(600,600))
    icons=Image.new('RGB',(600,600),(27,30,35));d=ImageDraw.Draw(icons)
    for i,name in enumerate(('end_coupler','data_core','navigation_core','data_corruption','emp')):
        path=A/f'textures/{"item" if i<3 else "mob_effect"}/{name}.png';im=Image.open(path).convert('RGBA').resize((112,112),Image.Resampling.NEAREST);x=20+(i%3)*195;y=30+(i//3)*230
        icons.paste(im,(x,y),im);d.text((x,y+125),name,font=font(16),fill='#e4c9ef')
    icons.save(OUT/'iconos.png');sheet.paste(icons,(1200,600));sheet.save(OUT/'lamina.png')
    overlay=Image.open(A/'textures/misc/corruption_overlay.png').resize((768,768),Image.Resampling.NEAREST);bg=Image.new('RGBA',overlay.size,(64,61,68,255));Image.alpha_composite(bg,overlay).convert('RGB').save(OUT/'overlay.png')
    tile=Image.open(A/'textures/block/corrupted_end_stone.png');wall=Image.new('RGBA',(80,80))
    for y in range(5):
        for x in range(5):wall.paste(tile,(x*16,y*16))
    wall.resize((800,800),Image.Resampling.NEAREST).save(OUT/'piedra_5x5.png')

def audit(r,uv,dragon):
    from nether_remodel_gen import verify_surfaces
    m=rig_model('turret',r,uv);verify_surfaces(m['elements'])
    pts=np.array([v for e in m['elements'] for vv in e['preview_vertices'].values() for v in vv]);bounds=[pts.min(0).tolist(),pts.max(0).tolist()]
    assert pts[:,1].min()>=0 and pts[:,1].max()<=26
    assert pts[:,0].max()-pts[:,0].min()<=20 and pts[:,2].max()-pts[:,2].min()<=20
    for name in FILES:
        if not name.endswith('.png'):continue
        im=Image.open(ROOT/name);w,h=im.size
        if '/mob_effect/' in name:assert (w,h)==(18,18)
        else:assert w==h and w in (16,128,256)
    assert Image.open(A/'textures/misc/corruption_overlay.png').crop((36,36,92,92)).getchannel('A').getbbox() is None
    report={'turret_coplanar':'OK; sin caras coplanares nuevas','turret_bounds_pixels':bounds,'texture_dimensions':'OK','overlay_center_transparent':'OK'}
    # Si existe la exportación de la auditoría Java local, contrastar TODAS las
    # cajas y UV usadas por la vista previa con el LayerDefinition real.
    ref=OUT/'dragon_vanilla_baked.json'
    if ref.exists():
        nodes={}
        def visit(n):
            for name,part in n['children'].items():nodes[name]=part;visit(part)
        visit(json.loads(ref.read_text()))
        count=0
        def sig(vertices):return sorted(tuple(round(float(v),4) for v in q) for q in vertices)
        for p in dragon.parts:
            n='neck' if p['name'].startswith(('neck','tail')) else p['name'];q=nodes[n]
            assert len(p['cubes'])==len(q['cubes']),n
            for i,c in enumerate(p['cubes']):
                expected=sorted(sig(vs) for vs in q['cubes'][i]);actual=[]
                for f,vv,rect in native_polygons(c):
                    u,v,U,V=rect;uvs=[(u,v),(U,v),(U,V),(u,V)];actual.append(sig([(*q,*t) for q,t in zip(vv,uvs)]))
                assert sorted(actual)==expected,(p['name'],i)
                count+=1
        report['dragon_boxes_and_UV']=f'OK; {count} cajas, seis caras por caja, vértices/UV contrastados con la capa vanilla real.'
    else:report['dragon_boxes_and_UV']='Contrato 1.20.1 incluido en el generador; sin exportación Java local para repetir el contraste.'
    (OUT/'auditoria.json').write_text(json.dumps(report,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')

def main():
    OUT.mkdir(parents=True,exist_ok=True);r=turret_rig();uv=turret(r);dr=dragon_rig();dragon_textures(dr);stone();items();effects();previews(r,uv,dr);audit(r,uv,dr)
    metadata={
        'class':'com.bryanamaya.tecnocinetico.entity.client.EndRelayTurretModel<T extends Entity>',
        'constructor':'EndRelayTurretModel(ModelPart bakedRoot)',
        'methods':['public static LayerDefinition createBodyLayer()','public ModelPart root()','public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)'],
        'bones':{'root':['base','head'],'head_charge':['iris_top','iris_bottom','iris_left','iris_right','capacitor_slide']},
        'charge':'Asignar public float charge 0..1 antes de setupAnim, incluso en reposo. Abre iris 0.75px y desplaza condensador 0.8px; lente y boca fijas.',
        'muzzle':{'feet_relative_blocks_facing_positive_Z':[0,1,.75],'head_pivot_feet_relative_blocks':[0,1,0],'head_local_model_pixels':[0,0,-12],'native_model_pixels':[0,8,-12]},
        'orientation':'Geometría nativa hacia -Z. Convención: rotación Y 180-bodyYaw, escala -1,-1,1 y traslación Y -1.5, anclando y=24 a los pies; a yaw=0 mira a +Z. Si se copia MobRenderer exactamente (-1.501), sumar 0.001 bloques a la altura indicada del haz.',
        'muzzle_formula':'Sin escala adicional: pies + (0,1,0) + 0.75 * (-sin(yaw)*cos(pitch), -sin(pitch), cos(yaw)*cos(pitch)); ángulos globales en radianes. setupAnim recibe yaw RELATIVO al cuerpo, igual que vanilla.',
        'textures':{'turret':'128x128; end_relay_turret.png y _glow.png','dragon':'256x256; minecraft:textures/entity/enderdragon/dragon.png y dragon_eyes.png, UV vanilla','items':'16x16, item/generated','stone':'16x16, cube_all','effects':'18x18 (tamaño nativo requerido)','overlay':'128x128; centro [36,36,92,92) totalmente transparente'},
        'dragon_preview':'Geometría reconstruida del LayerDefinition vanilla 1.20.1, UV y proporciones intactas; pose de vuelo de muestra con 5 módulos de cuello y 12 de cola. Ninguna textura original de Mojang se usa ni copia.',
        'renderer':'No se incluye registro ni renderer; usar _glow tipo eyes. La intensidad emisiva puede aumentar con charge. Dragon usa sus dos rutas vanilla; no requiere capa ni textura extra.'}
    (OUT/'integracion.json').write_text(json.dumps(metadata,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    files=FILES+['tools/end_part2_gen.py'];(OUT/'archivos.json').write_text(json.dumps(files,indent=2)+'\n',encoding='utf-8')
    print(f'End parte 2: {len(files)} archivos de arte/generador. Vistas e integración: build/end_part2/.')

if __name__=='__main__':main()
