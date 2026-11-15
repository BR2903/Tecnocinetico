"""Arte del End: Gusano y Espectro de Datos corruptos, sin dependencias de entidades.
Regenerar: python tools/end_part3_gen.py (Pillow + numpy).
Solo escribe los dos modelos, cuatro texturas y build/end_part3/.
"""
import copy,json,math,random
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw
from nether_remodel_rigs import Rig
from nether_remodel_gen import pack,key,faces,number,transform,verify_surfaces
from end_remodel_gen import color,PALETTE
from end_part2_gen import native_polygons
from preview_combustion_drill import font
import render_zbuffer as rz

ROOT=Path(__file__).resolve().parent.parent
OUT=ROOT/'build/end_part3'
CLIENT=ROOT/'src/main/java/com/bryanamaya/tecnocinetico/entity/client'
TEX=ROOT/'src/main/resources/assets/tecnocinetico/textures/entity'

def panel(r,n,p,at,size,mat='armor',rot=(0,0,0)):
    r.panel(n,p,at,size,mat,rot)

def finalize(r):
    for i,p in enumerate(r.parts):
        for c in p['cubes']:c['deform']=-(i+1)*.00011
    return r

def worm_head():
    r=Rig();p=lambda n,parent,at,size,mat='armor',rot=(0,0,0):panel(r,n,parent,at,size,mat,rot)
    p('head_chassis','root',(-5.8,-5.8,-9.6),(11.6,11.6,16),'dark')
    p('rear_coupling','root',(-3.9,-3.9,-12),(7.8,7.8,2.3),'rubber')
    p('rear_collar','root',(-5,-5,-10.5),(10,10,1.2),'edge')
    for i in range(6):
        a=i*math.pi/3
        # Seis costillas y seis mandíbulas independientes, con bisagra radial.
        r.part(f'shroud_{i}',at=(6.1*math.sin(a),6.1*math.cos(a),-7),rot=(0,0,-a))
        p(f'shroud_plate_{i}',f'shroud_{i}',(-2.55,-.5,0),(5.1,1.8,11.2),'armor')
        p(f'shroud_edge_{i}',f'shroud_{i}',(-2.3,1.35,.6),(4.6,.35,9.9),'worn')
        p(f'shroud_port_{i}',f'shroud_{i}',(-1.65,1.72,2.1),(3.3,.23,3.9),'dark')
        p(f'shroud_data_{i}',f'shroud_{i}',(-1.1,1.98,2.9),(2.2,.13,.65),'magenta' if i%2 else 'violet')
        r.part(f'jaw_mount_{i}',at=(5.8*math.sin(a),5.8*math.cos(a),3.1),rot=(0,0,-a))
        p(f'jaw_bolt_{i}',f'jaw_mount_{i}',(-2,-.6,-1.3),(4,1.2,1.6),'edge')
        r.part(f'jaw_{i}',f'jaw_mount_{i}',rot=(.35,0,0))
        p(f'jaw_back_{i}',f'jaw_{i}',(-1.75,-.75,0),(3.5,1.9,4.6),'worn')
        p(f'jaw_blade_{i}',f'jaw_{i}',(-1.3,-.9,4.6),(2.6,1.65,4.1),'armor')
        p(f'jaw_tooth_{i}',f'jaw_{i}',(-.9,-2.05,6.2),(1.8,1.1,2.5),'edge')
        p(f'jaw_signal_{i}',f'jaw_{i}',(-.65,.8,4.8),(1.3,.18,2.1),'violet')
    # Fondo oscuro de la boca y sensor central; las fauces lo dejan ver al abrir.
    p('mouth_recess','root',(-3.7,-3.7,5.3),(7.4,7.4,.6),'rubber')
    p('mouth_socket','root',(-2.25,-2.25,5.95),(4.5,4.5,.45),'dark')
    p('mouth_sensor','root',(-1.3,-1.3,6.45),(2.6,2.6,.2),'lens')
    for side,s in (('left',-1),('right',1)):
        p('optic_frame_'+side,'root',(s*6.5-.55,-3.3,-2),(1.1,2.4,4.6),'dark')
        p('optic_'+side,'root',(s*7-.2,-2.8,-.7),(.4,1.2,2.5),'magenta')
        p('jaw_feed_'+side,'root',(s*4.8-.3,3,-8),(.6,.7,10.8),'rubber')
    return finalize(r)

def worm_segment():
    r=Rig();p=lambda n,parent,at,size,mat='armor',rot=(0,0,0):panel(r,n,parent,at,size,mat,rot)
    p('segment_hub','root',(-4,-4,-6.4),(8,8,12.8),'dark')
    for z,n in [(-8.8,'rear'),(6.4,'front')]:
        p('joint_'+n,'root',(-3.4,-3.4,z),(6.8,6.8,2.4),'rubber')
        p('joint_ring_'+n,'root',(-3.8,-3.8,z+.5),(7.6,7.6,.65),'edge')
    for i in range(4):
        a=i*math.pi/2
        r.part(f'segment_plate_{i}',at=(6.1*math.sin(a),6.1*math.cos(a),0),rot=(0,0,-a))
        p(f'vertebra_{i}',f'segment_plate_{i}',(-3.7,-.9,-5.3),(7.4,1.65,10.6),'armor')
        p(f'rib_{i}',f'segment_plate_{i}',(-3.9,.8,-1.25),(7.8,.8,2.5),'worn')
        p(f'vertebra_vent_{i}',f'segment_plate_{i}',(-2.8,.8,-4.4),(5.6,.3,1.4),'dark')
        p(f'vertebra_fault_{i}',f'segment_plate_{i}',(-1.75,1.12,-4),(3.5,.16,.45),'magenta' if i%2 else 'violet')
    for side,s in (('left',-1),('right',1)):
        p('body_wire_'+side,'root',(s*4.9-.35,-4.9,-8.4),(.7,.8,16.8),'rubber')
        p('wire_collar_'+side,'root',(s*4.9-.5,-5.1,-3.1),(1,1.1,1.2),'edge')
    p('packet_0','root',(-1.8,-7.85,-1.6),(1.15,.22,1),'violet')
    p('packet_1','root',(.65,-7.85,.7),(1.15,.22,.6),'magenta')
    return finalize(r)

def worm_tail():
    r=Rig();p=lambda n,parent,at,size,mat='armor',rot=(0,0,0):panel(r,n,parent,at,size,mat,rot)
    p('tail_joint','root',(-3.4,-3.4,7.9),(6.8,6.8,2.5),'rubber')
    p('tail_collar','root',(-4.3,-4.3,6.7),(8.6,8.6,1.2),'edge')
    p('tail_body','root',(-4.9,-4.9,1),(9.8,9.8,5.6))
    p('tail_taper','root',(-3.3,-3.3,-5.3),(6.6,6.6,6.2),'worn')
    r.part('tail_tip',at=(0,0,-5.4))
    p('tail_tip_shell','tail_tip',(-1.9,-1.9,-3.7),(3.8,3.8,3.6),'armor')
    p('tail_tip_point','tail_tip',(-.7,-.7,-5),(1.4,1.4,1.2),'edge')
    p('tail_bus','root',(-1.6,-5.15,1.5),(3.2,.3,4.3),'dark')
    p('tail_status','root',(-1,-5.52,2.2),(2,.2,2.1),'violet')
    for side,s in (('left',-1),('right',1)):
        p('tail_cable_'+side,'root',(s*2.8-.3,-2.9,-3.8),(.6,.6,9.9),'rubber')
    return finalize(r)

def specter():
    r=Rig();p=lambda n,parent,at,size,mat='holo',rot=(0,0,0):panel(r,n,parent,at,size,mat,rot)
    # Raíces exigidas; el proyector opaco queda separado de la figura translúcida.
    for n,at in [('core',(0,2.8,0)),('body',(0,2,0)),('head',(0,-6.7,0)),('left_arm',(5.1,-2.2,0)),('right_arm',(-5.1,-2.2,0)),('rings',(0,3.2,0))]:r.part(n,at=at)
    p('projector_case','core',(-2.9,-3.3,-2.4),(5.8,6.6,4.8),'armor')
    p('projector_back','core',(-2.3,-2.8,2.45),(4.6,5.6,.5),'dark')
    p('projector_face','core',(-2.45,-2.7,-2.8),(4.9,5.4,.3),'edge')
    p('projector_socket','core',(-1.9,-1.9,-3.2),(3.8,3.8,.35),'dark')
    p('projector_lens','core',(-1.2,-1.2,-3.6),(2.4,2.4,.3),'lens')
    for i in range(3):p(f'projector_vent_{i}','core',(-1.8,-1.9+i*1.6,3),(3.6,.55,.2),'worn')
    p('emitter_top','core',(-1.7,-4,-1.5),(3.4,.6,3),'dark')
    p('emitter_projection','core',(-1.1,-4.45,-.9),(2.2,.3,1.8),'magenta')
    # Busto proyectado, sin piernas ni tela: bandas de datos y fragmentos sólidos.
    p('shoulder_image','body',(-4.6,-5,-1.7),(9.2,1.5,3.4),'holo_edge')
    p('chest_left','body',(-4.25,-3.4,-1.8),(1.25,5.3,3.6),'holo')
    p('chest_right','body',(3,-3.4,-1.8),(1.25,5.3,3.6),'holo')
    p('rear_rib','body',(-2.9,-3.4,1.8),(5.8,6,.25),'holo_dim')
    p('waist_image','body',(-3.5,3.1,-1.8),(7,1.2,3.6),'holo_edge')
    for i,(x,y,z,w,h) in enumerate([(-3.5,4.8,.4,1.1,11.9),(-1.8,5.2,-.6,.9,8.9),(.1,4.9,1.0,1.2,10.8),(2.1,5.3,.4,.8,11.2),(-2.6,5.8,-1.1,.65,7.3)]):
        r.part(f'stream_{i}','body',at=(x,y,z))
        p(f'stream_band_{i}',f'stream_{i}',(0,0,0),(w,h,.24),'holo_dim')
        for j in range(2):p(f'stream_packet_{i}_{j}',f'stream_{i}',(-.2,2.2+j*4.1,-.2),(w+.4,.45,.35),'data')
    p('head_image','head',(-3.3,-3.9,-2.45),(6.6,6,4.9),'holo_dim')
    p('crown_frag_a','head',(-3.5,-4.5,-1.4),(2.6,.45,2.8),'holo_edge')
    p('crown_frag_b','head',(.4,-4,-1.3),(2.8,.45,2.6),'holo_edge')
    p('face_window','head',(-2.9,-2.4,-2.8),(5.8,2.4,.2),'holo_dark')
    p('broken_visor_a','head',(-2.7,-1.9,-3.05),(2.1,.65,.15),'data')
    p('broken_visor_b','head',(.1,-1.25,-3.05),(2.5,.5,.15),'data_pink')
    p('head_lower_fragment','head',(-2.1,2.3,-1.7),(3.8,.6,3.5),'holo')
    for side,s in [('left',1),('right',-1)]:
        arm=side+'_arm'
        p(side+'_upper_image',arm,(-.9,0,-1),(1.8,8.4,2),'holo')
        p(side+'_arm_line',arm,(-1.1,1.2,-1.2),(.35,5.4,.25),'data')
        forearm=side+'_forearm';r.part(forearm,arm,at=(0,8.5,0),rot=(0,0,-s*.11))
        p(side+'_forearm_image',forearm,(-1.05,0,-1.2),(2.1,5.7,2.4),'holo_edge')
        p(side+'_wrist_fragment',forearm,(-1.25,6,-1.1),(2.5,.5,2.2),'holo')
        for i,x in enumerate((-1.45,-.2,1.05)):
            r.part(f'{side}_claw_{i}',forearm,at=(x,6.9,-.9),rot=(-.22-i*.06,0,0))
            p(f'{side}_claw_fragment_{i}',f'{side}_claw_{i}',(-.24,0,0),(.48,2.4+i*.4,.5),'holo_edge')
            p(f'{side}_claw_tip_{i}',f'{side}_claw_{i}',(-.2,2.7+i*.4,-.5),(.4,.9,.4),'data_pink')
    # Arcos de escaneo incompletos, nacidos del emisor, en dos alturas distintas.
    for n,y in [('scan_upper',-2.6),('scan_lower',5.2)]:
        r.part(n,'rings',at=(0,y,0))
        for i in (0,1,3,4,6):
            a=i*math.pi/4
            p(f'{n}_arc_{i}',n,(6.0*math.sin(a)-1.3,-.12,6.0*math.cos(a)-.2),(2.6,.24,.4),'data' if i%2 else 'data_pink',rot=(0,a,0))
    return finalize(r)

def atlas(name,rig):
    uv,_=pack(rig);im=Image.new('RGBA',(128,128));glow=Image.new('RGBA',(128,128))
    for spec,(u,v) in uv.items():
        mat,w,h,d=spec;rng=random.Random(name+str(spec))
        for rect in faces(u,v,w,h,d).values():
            X,Y,XX,YY=map(math.floor,rect)
            for y in range(Y,math.ceil(rect[3])):
                for x in range(X,math.ceil(rect[2])):
                    xx,yy=x-X,y-Y;ww,hh=math.ceil(rect[2])-X,math.ceil(rect[3])-Y
                    emissive=False
                    if mat.startswith('holo') or mat.startswith('data'):
                        k=rng.randrange(-7,8)
                        if mat=='holo_dark':c=(36,14,50,155)
                        elif mat.startswith('data'):
                            c=((255,79,216) if mat=='data_pink' else (192,76,255))+(220,);emissive=True
                        else:
                            edge=xx in (0,ww-1) or yy in (0,hh-1)
                            c=((191+k,76+k,244+k,190) if edge or mat=='holo_edge' else (108+k,40+k,156+k,112 if mat=='holo_dim' else 148))
                            if yy%4==2:c=c[:3]+(max(40,c[3]-65),)
                            emissive=True
                        if (xx*7+yy*3)%19==0 and mat not in ('holo_dark',):c=(239,166,255,c[3]);emissive=True
                    else:
                        c=color(mat,xx,yy,ww,hh,rng)
                        emissive=mat in ('magenta','violet') or mat in ('lens','screen') and c[:3] not in (PALETTE['dark'],PALETTE['pcb'])
                    c=tuple(max(0,min(255,int(v))) for v in c);im.putpixel((x,y),c)
                    if emissive:glow.putpixel((x,y),c)
    TEX.mkdir(parents=True,exist_ok=True);im.save(TEX/f'{name}.png');glow.save(TEX/f'{name}_glow.png');return uv

def layer(method,r,uv):
    lines=[f'    public static LayerDefinition {method}() {{','        var mesh = new MeshDefinition();','        var root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.ZERO);']
    for p in r.parts:
        builder='CubeListBuilder.create()'
        for c in p['cubes']:
            u,v=uv[key(c)];builder+=f'.texOffs({u}, {v}).addBox('+', '.join(number(n) for n in (*c['xyz'],*c['size']))+', new CubeDeformation('+number(c['deform'])+'))'
        pose='PartPose.offsetAndRotation('+', '.join(number(n) for n in (*p['at'],*p['rot']))+')'
        lines.append(f'        var {p["name"]} = {p["parent"]}.addOrReplaceChild("{p["name"]}", {builder}, {pose});')
    return '\n'.join(lines+['        return LayerDefinition.create(mesh, 128, 128);','    }'])

IMPORTS='''package com.bryanamaya.tecnocinetico.entity.client;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
'''

def java_worm(rigs,uv):
    code=IMPORTS+'''import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
/** Arte generado por tools/end_part3_gen.py. Todas las piezas miran a +Z. */
public class DataWormModel {
    public static final ModelLayerLocation HEAD_LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("tecnocinetico", "data_worm"), "head");
    public static final ModelLayerLocation SEGMENT_LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("tecnocinetico", "data_worm"), "segment");
    public static final ModelLayerLocation TAIL_LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("tecnocinetico", "data_worm"), "tail");
    private final ModelPart head, segment, tail;
    private final ModelPart[] jaws = new ModelPart[6];
    public DataWormModel(ModelPart head, ModelPart segment, ModelPart tail) {
        this.head = head.getChild("root"); this.segment = segment.getChild("root"); this.tail = tail.getChild("root");
        for (int i=0; i<6; i++) jaws[i] = this.head.getChild("jaw_mount_"+i).getChild("jaw_"+i);
    }
    private static void draw(ModelPart part, PoseStack pose, VertexConsumer vc, int light, int overlay) {
        pose.pushPose();
        // ModelPart convierte internamente sus píxeles a bloques (/16).
        // Solo invertir X/Y: arriba = +Y, boca = +Z; no duplicar scale(1/16).
        pose.scale(-1F, -1F, 1F);
        part.render(pose, vc, light, overlay);
        pose.popPose();
    }
    public void renderHead(PoseStack pose, VertexConsumer vc, int light, int overlay, float jawOpen, float age) {
        head.getAllParts().forEach(ModelPart::resetPose);
        float open = Math.max(0F, Math.min(1F, jawOpen));
        for (int i=0; i<6; i++) jaws[i].xRot += -.61F * open + (float)Math.sin(age*.13F+i*.4F)*.012F;
        draw(head, pose, vc, light, overlay);
    }
    public void renderSegment(PoseStack pose, VertexConsumer vc, int light, int overlay, int index, float age) {
        segment.getAllParts().forEach(ModelPart::resetPose);
        float wave = (float)Math.sin(age*.16F-index*.85F)*.13F;
        for (int i=0; i<4; i++) {
            ModelPart plate = segment.getChild("segment_plate_"+i);
            plate.x += (float)Math.sin(i*Math.PI*.5)*wave;
            plate.y += (float)Math.cos(i*Math.PI*.5)*wave;
        }
        segment.getChild("packet_0").z += (float)Math.sin(age*.22F-index*.85F)*1.4F;
        segment.getChild("packet_1").z += (float)Math.sin(age*.22F-index*.85F+.7F)*1.4F;
        draw(segment, pose, vc, light, overlay);
    }
    public void renderTail(PoseStack pose, VertexConsumer vc, int light, int overlay, float age) {
        tail.getAllParts().forEach(ModelPart::resetPose);
        tail.getChild("tail_tip").yRot = (float)Math.sin(age*.12F)*.035F;
        draw(tail, pose, vc, light, overlay);
    }
'''
    for method,r in zip(('createHead','createSegment','createTail'),rigs):code+=layer(method,r,uv)+'\n'
    (CLIENT/'DataWormModel.java').write_text(code+'}\n',encoding='utf-8')

def java_specter(r,uv):
    code=IMPORTS+'''import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.world.entity.Entity;
/** Proyección corrupta de un emisor físico. Sin dependencias de entidades del mod. */
public class DataSpecterModel<T extends Entity> extends HierarchicalModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("tecnocinetico", "data_specter"), "main");
    public float materialize, attack, pulse;
    private final ModelPart root, core, body, head, leftArm, rightArm, rings;
    public DataSpecterModel(ModelPart root) {
        this.root = root.getChild("root"); core = this.root.getChild("core"); body = this.root.getChild("body");
        head = this.root.getChild("head"); leftArm = this.root.getChild("left_arm"); rightArm = this.root.getChild("right_arm"); rings = this.root.getChild("rings");
    }
    @Override public ModelPart root() { return root; }
    private static float clamp(float v) { return Math.max(0F, Math.min(1F, v)); }
    @Override public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float t=ageInTicks, m=clamp(materialize), a=clamp(attack), p=clamp(pulse), glitch=1F-m;
        root.y += (float)Math.sin(t*.075F)*.35F;
        head.yRot = netHeadYaw * ((float)Math.PI/180F); head.xRot = headPitch * ((float)Math.PI/180F);
        head.x += (float)Math.sin(t*.63F)*glitch*.6F;
        body.x += (float)Math.sin(t*.43F+1.1F)*glitch*.4F;
        body.z += (float)Math.cos(t*.37F)*glitch*.25F;
        core.y += (float)Math.cos(t*.11F)*.1F;
        float wind=Math.min(1F,a/.55F), cut=Math.max(0F,(a-.55F)/.45F);
        for (int i=0; i<2; i++) {
            ModelPart arm=i==0?leftArm:rightArm; float side=i==0?1F:-1F;
            arm.xRot = .08F + wind*.85F-cut*2.2F-p*.25F;
            arm.zRot = -side*(.08F+wind*.22F-cut*.5F+p*1.15F);
            arm.x += side*glitch*(float)Math.sin(t*.49F+i)*.3F;
            ModelPart forearm=arm.getChild(i==0?"left_forearm":"right_forearm");
            forearm.xRot = -wind*.3F+cut*.7F;
            for(int j=0;j<3;j++)forearm.getChild((i==0?"left":"right")+"_claw_"+j).xRot += wind*.2F-cut*.4F;
        }
        rings.xScale=1F+p*.65F; rings.zScale=1F+p*.65F;
        rings.getChild("scan_upper").yRot=t*.025F+p*.5F;
        rings.getChild("scan_lower").yRot=-t*.019F-p*.4F;
        for(int i=0;i<5;i++) {
            ModelPart strip=body.getChild("stream_"+i);
            strip.xRot=(float)Math.sin(t*.095F+i*.8F)*.065F;
            strip.zRot=(float)Math.cos(t*.12F+i)*.055F;
            strip.x += (float)Math.sin(t*.71F+i*2F)*glitch*.45F;
            strip.y += (float)Math.cos(t*.52F+i)*glitch*.4F;
        }
    }
'''
    (CLIENT/'DataSpecterModel.java').write_text(code+layer('createBodyLayer',r,uv)+'\n}\n',encoding='utf-8')

def worm_pose(r,kind,age=12,jaw=0,index=0):
    r=copy.deepcopy(r)
    for p in r.parts:
        n=p['name'];at=list(p['at']);rot=list(p['rot'])
        if kind=='head' and n.startswith('jaw_') and n[4:].isdigit():rot[0]+=-.61*jaw+math.sin(age*.13+int(n[4:])*.4)*.012
        if kind=='segment':
            if n.startswith('segment_plate_'):
                i=int(n.rsplit('_',1)[1]);wave=math.sin(age*.16-index*.85)*.13;at[0]+=math.sin(i*math.pi*.5)*wave;at[1]+=math.cos(i*math.pi*.5)*wave
            if n.startswith('packet_'):at[2]+=math.sin(age*.22-index*.85+(0 if n=='packet_0' else .7))*1.4
        if kind=='tail' and n=='tail_tip':rot[1]=math.sin(age*.12)*.035
        p['at']=at;p['rot']=rot
    return r

def specter_pose(r,m=1,a=0,pulse=0,age=18):
    r=copy.deepcopy(r);glitch=1-m;wind=min(1,a/.55);cut=max(0,(a-.55)/.45)
    for q in r.parts:
        n=q['name'];at=list(q['at']);rot=list(q['rot'])
        if n=='head':at[0]+=math.sin(age*.63)*glitch*.6
        if n=='body':at[0]+=math.sin(age*.43+1.1)*glitch*.4;at[2]+=math.cos(age*.37)*glitch*.25
        if n=='core':at[1]+=math.cos(age*.11)*.1
        if n in ('left_arm','right_arm'):
            side=1 if n=='left_arm' else -1;i=0 if side==1 else 1
            rot[0]=.08+wind*.85-cut*2.2-pulse*.25;rot[2]=-side*(.08+wind*.22-cut*.5+pulse*1.15);at[0]+=side*glitch*math.sin(age*.49+i)*.3
        if n in ('left_forearm','right_forearm'):rot[0]=-wind*.3+cut*.7
        if n.startswith(('left_claw_','right_claw_')) and n.rsplit('_',1)[1].isdigit():rot[0]+=wind*.2-cut*.4
        if n=='scan_upper':rot[1]=age*.025+pulse*.5
        if n=='scan_lower':rot[1]=-age*.019-pulse*.4
        if n.startswith('stream_') and n[7:].isdigit():
            i=int(n[7:]);rot[0]=math.sin(age*.095+i*.8)*.065;rot[2]=math.cos(age*.12+i)*.055;at[0]+=math.sin(age*.71+i*2)*glitch*.45;at[1]+=math.cos(age*.52+i)*glitch*.4
        if n=='rings':q['scale']=(1+pulse*.65,1,1+pulse*.65)
        q['at']=at;q['rot']=rot
    r.preview_root_y=math.sin(age*.075)*.35
    return r

def mesh(r,uv,texture,worm=False,outer=None):
    matrices={'root':np.eye(4)};els=[]
    for p in r.parts:
        mat=transform(p)
        if 'scale' in p:mat=mat@np.diag([*p['scale'],1])
        matrix=matrices[p['parent']]@mat;matrices[p['name']]=matrix
        for c in p['cubes']:
            c=copy.deepcopy(c);c['vanilla_uv']=uv[key(c)];base_size=c['size'];dd=c.get('deform',0)
            # La holgura reduce geometría, nunca el rectángulo de textura.
            allfaces=list(native_polygons(c));vertices={};fd={}
            center=np.array(c['xyz'])+np.array(c['size'])/2
            for face,vv,rect in allfaces:
                points=[]
                for v in vv:
                    v=np.array(v,dtype=float);v+=np.sign(v-center)*dd
                    v=matrix@np.array([*v,1]);world=np.array([-v[0],-v[1],v[2],1])
                    if not worm:world[1]+=24-getattr(r,'preview_root_y',0)
                    if outer is not None:world=outer@world
                    points.append(world[:3].tolist())
                vertices[face]=points;fd[face]={'texture':'#all','uv':[v/8 for v in rect]}
            pts=np.array([v for vv in vertices.values() for v in vv]);els.append({'part':p['name'],'from':pts.min(0).tolist(),'to':pts.max(0).tolist(),'preview_vertices':vertices,'faces':fd})
    return {'textures':{'all':texture},'elements':els}

def alpha_render(model,yaw=205,pitch=12,scale=18,size=900,global_alpha=1):
    """Vista con transparencia alfa real; caras visibles ordenadas por profundidad."""
    cy,sy,cp,sp=map(float,(math.cos(math.radians(yaw)),math.sin(math.radians(yaw)),math.cos(math.radians(pitch)),math.sin(math.radians(pitch))))
    rot=np.array([[cy,0,-sy],[-sy*sp,cp,-cy*sp],[sy*cp,sp,cy*cp]])
    allp=np.array([v for e in model['elements'] for vv in e['preview_vertices'].values() for v in vv]);center=(allp.min(0)+allp.max(0))/2
    img=np.zeros((size,size,3))+np.array([27,30,35])/255;quads=[]
    tx=np.asarray(Image.open(rz.tex_path(model['textures']['all'],{})).convert('RGBA'),dtype=float)/255;th,tw=tx.shape[:2]
    for e in model['elements']:
        mid=np.mean([v for vv in e['preview_vertices'].values() for v in vv],axis=0)
        for face,vv in e['preview_vertices'].items():
            v=np.array(vv);n=np.cross(v[1]-v[0],v[3]-v[0]);hint=v.mean(0)-mid
            if n@hint<0:n=-n
            if (rot@n)[2]<=0:continue
            q=(v-center)@rot.T;quads.append((q[:,2].mean(),face,q,e['faces'][face]))
    for _,face,q,fd in sorted(quads,key=lambda v:v[0]):
        q[:,0]=size/2+q[:,0]*scale;q[:,1]=size/2-q[:,1]*scale
        u,v,U,V=fd['uv'];uv=[(u,v),(U,v),(U,V),(u,V)]
        for tri in ((0,1,2),(0,2,3)):
            P=q[list(tri)];coords=np.array(uv)[list(tri)];xmin=max(0,int(P[:,0].min()));xmax=min(size,int(P[:,0].max())+1);ymin=max(0,int(P[:,1].min()));ymax=min(size,int(P[:,1].max())+1)
            if xmax<=xmin or ymax<=ymin:continue
            gx,gy=np.meshgrid(np.arange(xmin,xmax)+.5,np.arange(ymin,ymax)+.5);(x1,y1,_),(x2,y2,_),(x3,y3,_)=P
            den=(y2-y3)*(x1-x3)+(x3-x2)*(y1-y3)
            if abs(den)<1e-9:continue
            a=((y2-y3)*(gx-x3)+(x3-x2)*(gy-y3))/den;b=((y3-y1)*(gx-x3)+(x1-x3)*(gy-y3))/den;c=1-a-b;inside=(a>=0)&(b>=0)&(c>=0)
            uu=a*coords[0,0]+b*coords[1,0]+c*coords[2,0];vv=a*coords[0,1]+b*coords[1,1]+c*coords[2,1]
            sample=tx[np.clip((vv/16*th).astype(int),0,th-1),np.clip((uu/16*tw).astype(int),0,tw-1)];alpha=sample[:,:,3]*inside
            # El renderer puede mantener el proyector opaco: simular esa opción.
            alpha*=np.where(sample[:,:,3]>.99,1,global_alpha)
            sub=img[ymin:ymax,xmin:xmax];rgb=sample[:,:,:3]*max(.75,rz.SHADE[face]);sub[:]=rgb*alpha[:,:,None]+sub*(1-alpha[:,:,None])
    return Image.fromarray(np.uint8(np.clip(img,0,1)*255))

def preview(head,segment,tail,spec,wuv,suv):
    gallery=Image.new('RGB',(1800,1800),(27,30,35));draw=ImageDraw.Draw(gallery)
    for j,opening in enumerate((0,1)):
        m=mesh(worm_pose(head,'head',jaw=opening),wuv,'tecnocinetico:entity/data_worm',True)
        pic=rz.render(m,25,18,30,size=900);ImageDraw.Draw(pic).text((20,18),'Gusano · fauces '+('abiertas' if opening else 'cerradas'),font=font(27),fill='#e0c9f0');pic.save(OUT/f'cabeza_{"abierta" if opening else "cerrada"}.png');gallery.paste(pic,(j*900,0))
    chain={'textures':{'all':'tecnocinetico:entity/data_worm'},'elements':[]};position=np.array([0.,77.,40.])
    def outer(pos,yaw,pitch):return transform({'at':pos,'rot':(pitch,yaw,0)})
    chain['elements']+=mesh(worm_pose(head,'head',jaw=.75),wuv,'tecnocinetico:entity/data_worm',True,outer(position,-.2,-.05))['elements']
    for i in range(6):
        yaw=(-.16,-.06,.1,.28,.4,.55)[i];pitch=(-.15,-.3,-.48,-.65,-.82,-.98)[i]
        direction=np.array([math.sin(yaw)*math.cos(pitch),-math.sin(pitch),math.cos(yaw)*math.cos(pitch)])
        position-=direction*(20.8 if i==0 else 17.6)
        chain['elements']+=mesh(worm_pose(segment,'segment',index=i),wuv,'tecnocinetico:entity/data_worm',True,outer(position,yaw,pitch))['elements']
    position-=direction*19.2;chain['elements']+=mesh(worm_pose(tail,'tail'),wuv,'tecnocinetico:entity/data_worm',True,outer(position,.55,-.98))['elements']
    pic=rz.render(chain,38,10,11,size=1400);ImageDraw.Draw(pic).text((25,20),'Gusano de Datos · cabeza + 6 segmentos + cola · arco',font=font(32),fill='#e0c9f0');pic.save(OUT/'gusano_arco.png');(OUT/'gusano_arco.json').write_text(json.dumps(chain),encoding='utf-8')
    # Escena de llegada al suelo. La textura del suelo solo se escribe en build/.
    rng=random.Random(123);ground=Image.new('RGBA',(128,128))
    for y in range(128):
        for x in range(128):
            k=rng.choice((-12,-7,-3,0,3,7));ground.putpixel((x,y),(218+k,219+k,162+k,255))
    ground.save(OUT/'suelo_preview.png');scene=copy.deepcopy(chain);scene['textures']['ground']='tecnocinetico:entity/_end_part3_ground_preview'
    f=(-35,-7,-90);t=(35,0,57)
    scene['elements'].append({'part':'suelo_de_preview','from':f,'to':t,'preview_vertices':{face:rz.corners(f,t,face) for face in rz.SHADE},'faces':{face:{'texture':'#ground','uv':[0,0,16,16]} for face in rz.SHADE}})
    original=rz.tex_path
    def preview_path(ref,textures):
        while ref.startswith('#'):ref=textures[ref[1:]]
        return OUT/'suelo_preview.png' if ref.endswith('_end_part3_ground_preview') else original(ref,textures)
    rz.tex_path=preview_path
    try:pic=rz.render(scene,42,12,9,size=1400);pic.save(OUT/'gusano_emergiendo.png')
    finally:rz.tex_path=original
    sheet=Image.new('RGB',(1800,1800),(27,30,35))
    for i,(name,m,a,p,alpha) in enumerate([('desfasado',0,0,0,.48),('materializado',1,0,0,1),('garras',1,1,0,1),('pulso',1,0,1,1)]):
        posed=specter_pose(spec,m,a,p);model=mesh(posed,suv,'tecnocinetico:entity/data_specter')
        pic=alpha_render(model,205,10,20,900,alpha);ImageDraw.Draw(pic).text((20,18),'Espectro · '+name,font=font(28),fill='#e0c9f0');pic.save(OUT/f'espectro_{name}.png');sheet.paste(pic,((i%2)*900,(i//2)*900))
        (OUT/f'espectro_{name}.json').write_text(json.dumps(model),encoding='utf-8')
    sheet.save(OUT/'espectro_estados.png');gallery.paste(Image.open(OUT/'gusano_emergiendo.png').resize((900,900)),(0,900));gallery.paste(Image.open(OUT/'espectro_materializado.png'),(900,900));gallery.save(OUT/'lamina.png')

def main():
    OUT.mkdir(parents=True,exist_ok=True);head=worm_head();segment=worm_segment();tail=worm_tail();spec=specter()
    combined=Rig();combined.parts=head.parts+segment.parts+tail.parts;wuv=atlas('data_worm',combined);suv=atlas('data_specter',spec)
    java_worm((head,segment,tail),wuv);java_specter(spec,suv);preview(head,segment,tail,spec,wuv,suv)
    audits={}
    for n,r,uv in [('head',head,wuv),('segment',segment,wuv),('tail',tail,wuv),('specter',spec,suv)]:
        model=mesh(r,uv,'tecnocinetico:entity/'+('data_specter' if n=='specter' else 'data_worm'),n!='specter');verify_surfaces(model['elements']);pts=np.array([v for e in model['elements'] for vv in e['preview_vertices'].values() for v in vv]);audits[n]={'bounds_px':[pts.min(0).tolist(),pts.max(0).tolist()],'boxes':len(model['elements']),'new_coplanar_faces':0}
    for state,m,a,p in [('desfasado',0,0,0),('materializado',1,0,0),('garras',1,1,0),('pulso',1,0,1)]:
        verify_surfaces(mesh(specter_pose(spec,m,a,p),suv,'tecnocinetico:entity/data_specter')['elements'])
    verify_surfaces(mesh(worm_pose(head,'head',jaw=1),wuv,'tecnocinetico:entity/data_worm',True)['elements'])
    for name in ('data_worm','data_worm_glow','data_specter','data_specter_glow'):
        im=Image.open(TEX/f'{name}.png').convert('RGBA');assert im.size==(128,128)
        alpha=np.array(im)[:,:,3];assert alpha.min()==0 and alpha.max()>0
        if 'specter' in name:assert np.any((alpha>0)&(alpha<255))
    audits['poses_and_atlas']='OK: fauces abiertas y cuatro estados del espectro sin nuevas caras coplanares; atlas 128x128, alfa parcial del holograma conservado.'
    (OUT/'auditoria.json').write_text(json.dumps(audits,indent=2)+'\n',encoding='utf-8')
    files=[str(p.relative_to(ROOT)).replace('\\','/') for p in [CLIENT/'DataWormModel.java',CLIENT/'DataSpecterModel.java',TEX/'data_worm.png',TEX/'data_worm_glow.png',TEX/'data_specter.png',TEX/'data_specter_glow.png',ROOT/'tools/end_part3_gen.py']]
    (OUT/'archivos.json').write_text(json.dumps(files,indent=2)+'\n',encoding='utf-8')
    info={'worm_layers':['tecnocinetico:data_worm#head','tecnocinetico:data_worm#segment','tecnocinetico:data_worm#tail'],
          'worm_constructor':'new DataWormModel(bakeLayer(HEAD_LAYER), bakeLayer(SEGMENT_LAYER), bakeLayer(TAIL_LAYER)); pasar raíces horneadas completas, sin getChild adicional.',
          'worm_coordinates':'Cada pieza centrada en origen; +Z delante, +Y arriba, unidades de bloque. ModelPart ya convierte píxeles /16; draw solo aplica escala (-1,-1,1), sin traslación ni rotación extra. NO aplicar flip de LivingEntityRenderer ni escala /16 externa.',
          'worm_mouth_center_blocks':[0,0,.7],
          'worm_mouth_note':'Apertura hacia +Z; centro funcional a 11.2px (0.70 bloques) del origen. Los dientes se adelantan/retraen algo al abrir; no usar su punta animada como origen lógico.',
          'worm_segment_spacing_blocks':1.1,'worm_head_first_segment_spacing_suggested':1.3,'worm_tail_spacing_suggested':1.2,
          'worm_emissive':'Repetir renderHead/Segment/Tail con la textura _glow y los mismos age, index y jawOpen; las poses se restauran en cada llamada.',
          'specter_layer':'tecnocinetico:data_specter#main',
          'specter_root_bones':['core','body','head','left_arm','right_arm','rings'],
          'specter_fields':'Asignar materialize, attack y pulse (0..1) antes de setupAnim. attack 0..0.55 prepara; 0.55..1 corta. El renderer controla alfa de desfase; textura holo semitransparente, core metálico opaco.',
          'specter_alpha':'Para conservar emisor físico opaco, dibujar root.getChild("core") aparte con alfa 1 y omitir core de la pasada de alfa reducido del resto. La textura y hueso ya lo separan. _glow conserva alfa de la proyección: usar mezcla translúcida emisiva para que cuente, eyes si se prefiere más intenso.',
          'specter_animation':'Pose inicial restaurada en cada setupAnim, incluidos xScale/zScale de rings. El renderer debe ejecutar setupAnim una vez y reutilizar pose en pasada emisiva.',
          'eggs':'Colores propuestos por Claude: aprobados, 0x2A2430/0xC04CFF y 0x1A1022/0xFF4FD8.',
          'naming':'Corrupto; no se escribe lang ni se cambia lógica.'}
    (OUT/'integracion.json').write_text(json.dumps(info,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    print('End parte 3: dos modelos independientes, cuatro atlas 128x128; vistas en build/end_part3/.')

if __name__=='__main__':main()
