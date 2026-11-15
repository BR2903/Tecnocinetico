"""End abandonado: Enderman y Shulker corrompidos por datos, sin magia ni cian.
python tools/end_remodel_gen.py
Solo crea EndRemodelLayers.java, cuatro atlas, cuatro partículas, su JSON y vistas en build/.
Las texturas vanilla se leen del Minecraft local SOLO para la comparación, nunca para el arte del mod.
"""
import argparse,copy,json,math,random,zipfile
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw
from nether_remodel_rigs import Rig
from nether_remodel_gen import pack,key,faces,number,transform,verify_surfaces
import render_zbuffer as rz
from preview_combustion_drill import font

ROOT=Path(__file__).resolve().parent.parent
A=ROOT/'src/main/resources/assets/tecnocinetico'
JAVA=ROOT/'src/main/java/com/bryanamaya/tecnocinetico/entity/client/EndRemodelLayers.java'
OUT=ROOT/'build/end_remodel'
PALETTE={'skin':(23,21,28),'jaw':(19,17,24),'armor':(53,52,60),'edge':(105,103,113),
         'rubber':(27,25,32),'dark':(12,10,17),'pcb':(32,26,40),'worn':(73,67,79),
         'violet':(192,76,255),'magenta':(255,79,216),'screen':(255,79,216),'lens':(192,76,255)}
END_PIVOTS={'head':(0,-13,0),'hat':(0,-13,0),'body':(0,-14,0),'right_arm':(-5,-12,0),
            'left_arm':(5,-12,0),'right_leg':(-2,-5,0),'left_leg':(2,-5,0)}
SHULKER_PIVOTS={'lid':(0,24,0),'base':(0,24,0),'head':(0,12,0)}


def enderman(corrupt=True):
    r=Rig()
    for n,at in END_PIVOTS.items():r.part(n,at=at)
    def bb(p,m,f,s,uv,inf=0,mirror=False):
        next(q for q in r.parts if q['name']==p)['cubes'].append(dict(material=m,xyz=f,size=s,vanilla_uv=uv,deform=inf,mirror=mirror,base=True))
    bb('head','skin',(-4,-8,-4),(8,8,8),(0,0))
    bb('hat','jaw',(-4,-8,-4),(8,8,8),(0,16),-.5)
    bb('body','skin',(-4,0,-2),(8,12,4),(32,16))
    for n in ('right_arm','left_arm'):bb(n,'skin',(-1,-2,-1),(2,30,2),(56,0),mirror=n.startswith('left'))
    for n in ('right_leg','left_leg'):bb(n,'skin',(-1,0,-1),(2,30,2),(56,0),mirror=n.startswith('left'))
    if not corrupt:return r
    def panel(n,p,f,s,m='armor',rot=(0,0,0)):r.panel(n,p,f,s,m,rot)
    # Implantes del cráneo; los ojos conservan la lectura inmediata del Enderman.
    for i,x in enumerate((-3.45,.65)):
        panel(f'eye_frame_{i}','head',(x-.18,-5.45,-4.12),(3.0,1.35,.28),'dark')
        panel(f'eye_{i}','head',(x,-5.08,-4.43),(2.65,.56,.12),'magenta' if i==0 else 'violet')
        panel(f'eye_data_{i}','head',(x+.55,-4.97,-4.58),(.55,.25,.1),'screen')
    panel('cranial_patch','head',(3.9,-7.2,-2.8),(.5,4.8,4.2))
    panel('temple_socket','head',(4.39,-5.9,-1.7),(.22,2.3,2.4),'dark')
    panel('temple_status','head',(4.63,-5.5,-1.5),(.1,.45,1.4),'violet')
    panel('broken_aerial_base','head',(2.7,-8.3,1.2),(1.0,.5,1.3),'edge')
    panel('broken_aerial','head',(3.02,-9.25,1.55),(.35,1.05,.45),'worn',(0,0,.15))
    panel('brow_plate','head',(-4.1,-7.6,-4.17),(5.3,.65,.25),'armor')
    panel('head_rear_port','head',(-2.7,-6.9,3.96),(5.4,3.7,.38),'pcb')
    panel('head_rear_trace','head',(-2.25,-6.3,4.35),(2.1,.45,.12),'screen')
    # La mandíbula pertenece a hat: permanece abajo cuando head sube 5 px por creepy.
    panel('jaw_guard','hat',(-3.4,-2.15,-3.73),(6.8,1.45,.5),'armor')
    panel('jaw_edge','hat',(-3.5,-.8,-3.85),(7,.35,.65),'edge')
    for i,x in enumerate((-2.5,-.8,1.5)):panel(f'jaw_vent_{i}','hat',(x,-1.85,-4.25),(.65,.65,.1),'dark')
    # Módulo de red abandonado y cables pegados a sus huesos: nada cruza las articulaciones.
    panel('back_frame','body',(-2.5,.6,1.95),(5,9.7,1.25),'armor')
    panel('back_socket','body',(-1.85,1.5,3.21),(3.7,7.9,.25),'dark')
    for i in range(4):
        panel(f'back_port_{i}','body',(-1.4,2.1+i*1.65,3.47),(2.8,.65,.2),'pcb')
        panel(f'back_error_{i}','body',(-1.12,2.25+i*1.65,3.7),(1.4 if i%2==0 else .6,.28,.1),'violet' if i%2==0 else 'magenta')
    panel('chest_patch','body',(-3.9,2,-2.17),(3.5,5.6,.3),'armor')
    panel('chest_screen','body',(-3.6,2.6,-2.5),(2.9,2.4,.18),'screen')
    panel('chest_wound','body',(.2,4.1,-2.2),(2.2,4.1,.25),'dark')
    for i in range(3):panel(f'chest_broken_trace_{i}','body',(.55+i*.5,4.6+i*.75,-2.48),(.35,1.3,.1),'violet')
    for side,s in (('right',-1),('left',1)):
        arm=side+'_arm';leg=side+'_leg'
        panel('shoulder_socket_'+side,'body',(s*3.8-.45,1.0,.5),(.9,1.1,1.9),'rubber')
        panel('arm_collar_'+side,arm,(-1.2,-1.2,-1.15),(2.4,2.6,2.3),'worn')
        panel('arm_cable_'+side,arm,(s*.7-.16,1.3,.95),(.32,6.2,.38),'rubber')
        panel('arm_connector_'+side,arm,(s*.8-.3,6.8,.9),(.6,1.0,.65),'edge')
        panel('forearm_implant_'+side,arm,(-1.17,13.0,-1.2),(2.34,4.6,.4),'armor')
        panel('forearm_data_'+side,arm,(-.78,13.8,-1.63),(1.2,.32,.12),'screen')
        panel('shin_collar_'+side,leg,(-1.18,14,-1.17),(2.36,2.6,2.34),'worn')
        panel('shin_wire_'+side,leg,(s*.8-.14,16.7,.8),(.28,6,.42),'rubber')
        panel('ankle_plate_'+side,leg,(-1.15,25.2,-1.25),(2.3,2.0,.45),'armor')
        panel('ankle_diagnostic_'+side,leg,(-.75,25.65,-1.73),(.8,.35,.12),'violet')
    return r


def shulker(corrupt=True):
    r=Rig()
    for n,at in SHULKER_PIVOTS.items():r.part(n,at=at)
    def b(p,mat,f,s):next(q for q in r.parts if q['name']==p)['cubes'].append(dict(material=mat,xyz=f,size=s))
    if not corrupt:
        for p,f,s,uv in (('lid',(-8,-16,-8),(16,12,16),(0,0)),('base',(-8,-8,-8),(16,8,16),(0,28)),('head',(-3,0,-3),(6,6,6),(0,52))):
            b(p,'skin',f,s);next(q for q in r.parts if q['name']==p)['cubes'][-1]['vanilla_uv']=uv
        return r
    def panel(n,p,f,s,m='armor',rot=(0,0,0)):r.panel(n,p,f,s,m,rot)
    # Tapa en forma de cápsula hueca: conserva exactamente la envolvente vanilla.
    b('lid','armor',(-8,-16,-8),(16,1.25,16))
    for x in (-8,6.75):b('lid','armor',(x,-14.75,-8),(1.25,10.75,16))
    for z in (-8,6.75):b('lid','armor',(-6.75,-14.75,z),(13.5,10.75,1.25))
    b('base','dark',(-8,-1.25,-8),(16,1.25,16))
    for x in (-8,6.75):b('base','armor',(x,-8,-8),(1.25,6.75,16))
    for z in (-8,6.75):b('base','armor',(-6.75,-8,z),(13.5,6.75,1.25))
    for x in (-7.85,6.65):
        for z in (-7.85,6.65):
            panel(f'lid_corner_{x}_{z}'.replace('.','_').replace('-','m'),'lid',(x,-15.7,z),(1.2,11.0,1.2),'worn')
            panel(f'base_corner_{x}_{z}'.replace('.','_').replace('-','m'),'base',(x,-7.85,z),(1.2,7.7,1.2),'edge')
    panel('lid_top_access','lid',(-5.1,-16.18,-4.8),(10.2,.25,9.6),'dark')
    panel('lid_top_plate','lid',(-4.4,-16.44,-4.1),(8.8,.28,8.2),'armor')
    for i in range(3):panel(f'top_contact_{i}','lid',(-3.2+i*2.2,-16.75,1.7),(1.1,.2,1.6),'violet' if i!=1 else 'dark')
    panel('lid_front_socket','lid',(-5.65,-13,-8.08),(11.3,6.7,.22),'dark')
    panel('lid_front_access','lid',(-5.15,-12.4,-8.34),(6.2,4.9,.28),'pcb')
    panel('lid_damaged_display','lid',(-4.7,-11.9,-8.65),(5.25,2.2,.15),'screen')
    for i in range(3):panel(f'lid_front_vent_{i}','lid',(2.1,-12.2+i*1.7,-8.37),(2.7,.65,.25),'worn')
    for side,s in (('right',-1),('left',1)):
        x=s*8.0
        panel('lid_side_panel_'+side,'lid',(x-.1,-13.6,-5.2),(.23,6.1,10.4),'dark')
        for i in range(4):panel(f'lid_side_vent_{side}_{i}','lid',(x-.18,-12.8+i*1.35,-4.4),(.4,.65,8.8),'worn')
        panel('base_side_service_'+side,'base',(x-.1,-5.8,-4.3),(.23,3.8,8.6),'dark')
    for i,x in enumerate((-5.5,-2.4,.7,3.8)):
        panel(f'base_latch_{i}','base',(x,-7.75,-8.12),(1.5,1.7,.3),'worn')
        panel(f'base_status_{i}','base',(x+.15,-3.1,-8.16),(1.2,.4,.2),'violet' if i%2 else 'magenta')
    panel('rear_circuit','lid',(-5.3,-12.8,7.96),(10.6,5.4,.3),'pcb')
    for i in range(3):panel(f'rear_fault_{i}','lid',(-4.5+i*3,-11.7,8.28),(1.7,1.1,.15),'screen')
    panel('sensor_pedestal','base',(-2.4,-7.8,-2.4),(4.8,5.7,4.8),'dark')
    for i,x in enumerate((-4.6,3.6)):
        panel(f'interior_board_{i}','base',(x,-6.6,-5.4),(1,4.6,10.8),'pcb')
        panel(f'interior_status_{i}','base',(x-.1,-6.9,-4.0),(1.2,.3,2),'magenta')
    # Cabeza de seis píxeles: no cuelga de la tapa, sigue el root head que gira vanilla.
    b('head','dark',(-3,0,-3),(6,6,6))
    panel('sensor_frame','head',(-2.5,.6,-3.12),(5,4.8,.35),'edge')
    panel('sensor_recess','head',(-2.1,1,-3.5),(4.2,4,.35),'dark')
    panel('sensor_lens','head',(-1.25,1.75,-3.88),(2.5,2.5,.16),'lens')
    for i,x in enumerate((-2.35,1.8)):panel(f'sensor_screw_{i}','head',(x,1.1,-3.55),(.55,.55,.25),'worn')
    panel('sensor_top_bus','head',(-2.0,-.15,-1.9),(4,.25,3.8),'pcb')
    return r


def finalize(rig):
    for i,c in enumerate(c for p in rig.parts for c in p['cubes']):
        c.setdefault('deform',0 if c.get('base') else -(i+1)*.00013)
    return rig


def color(mat,x,y,w,h,rng):
    # Las caras vanilla de cabeza y torso se solapan 1 px en reposo.
    # Conservar negro uniforme en ambas elimina cualquier diferencia visual en esa junta nativa.
    if mat=='skin':return PALETTE['skin']+(255,)
    if mat=='screen':
        on=(x*7+y*11)%5<3 and (y!=h-1 or x%3==0)
        return (PALETTE['magenta' if (x+y)%3 else 'violet'] if on else PALETTE['pcb'])+(255,)
    if mat=='lens':
        d=((x+.5)/max(w,1)*2-1)**2+((y+.5)/max(h,1)*2-1)**2
        c=(240,164,255) if d<.26 else PALETTE['violet'] if d<1 else PALETTE['dark']
        return c+(255,)
    base=PALETTE[mat];k=rng.uniform(.94,1.05)
    if mat in ('armor','edge','worn'):
        if x==0 or y==0:k+=.18
        elif x==w-1 or y==h-1:k*=.63
        if min(w,h)>3 and (x,y) in ((1,1),(w-2,h-2)):k+=.25
    return tuple(max(0,min(255,round(v*k))) for v in base)+(255,)


def atlas(name,rig,uv):
    im=Image.new('RGBA',(128,128));glow=Image.new('RGBA',(128,128))
    for spec,(u,v) in uv.items():
        mat,w,h,d=spec;rng=random.Random(name+str(spec))
        for x0,y0,x1,y1 in faces(u,v,w,h,d).values():
            for y in range(math.floor(y0),math.ceil(y1)):
                for x in range(math.floor(x0),math.ceil(x1)):
                    c=color(mat,x-math.floor(x0),y-math.floor(y0),math.ceil(x1-x0),math.ceil(y1-y0),rng);im.putpixel((x,y),c)
                    if mat in ('violet','magenta') or mat in ('screen','lens') and c[:3] not in (PALETTE['dark'],PALETTE['pcb']):glow.putpixel((x,y),c)
    path=A/'textures/entity/end_remodel';path.mkdir(parents=True,exist_ok=True)
    im.save(path/(name+'.png'));glow.save(path/(name+'_glow.png'))


def layer(name,rig,uv):
    mesh='net.minecraft.client.model.HumanoidModel.createMesh(CubeDeformation.NONE, -14.0F)' if name=='enderman' else 'new MeshDefinition()'
    lines=[f'    public static LayerDefinition {name}() {{',f'        var mesh = {mesh};','        var root = mesh.getRoot();']
    for p in rig.parts:
        builder='CubeListBuilder.create()'
        for c in p['cubes']:
            u,v=uv[key(c)];builder+=f'.mirror({str(c.get("mirror",False)).lower()}).texOffs({u}, {v}).addBox('+', '.join(number(n) for n in (*c['xyz'],*c['size']))+', new CubeDeformation('+number(c['deform'])+'))'
        pose='PartPose.offsetAndRotation('+', '.join(number(n) for n in (*p['at'],*p['rot']))+')'
        lines.append(f'        var {p["name"]} = {p["parent"]}.addOrReplaceChild("{p["name"]}", {builder}, {pose});')
    return '\n'.join(lines+['        return LayerDefinition.create(mesh, 128, 128);','    }'])


def particles():
    shapes=(((1,2,3,3),(5,4,6,4),(4,6,4,6)),((1,4,2,4),(4,3,6,3),(5,1,5,1)),
            ((2,4,3,5),(5,2,6,2),(1,1,1,1)),((4,4,5,4),(2,2,2,3),(6,6,6,6)))
    from PIL import ImageDraw
    for f,boxes in enumerate(shapes):
        im=Image.new('RGBA',(8,8));d=ImageDraw.Draw(im)
        for i,rect in enumerate(boxes):d.rectangle(rect,fill=(255,255,255,(255,230,180,120)[f] if i==0 else (200,180,130,80)[f]))
        im.save(A/f'textures/particle/data_glitch_{f}.png')
    (A/'particles/data_glitch.json').write_text(json.dumps({'textures':[f'tecnocinetico:data_glitch_{f}' for f in range(4)]},indent=2)+'\n',encoding='utf-8')


def pose_parts(rig,name,pose):
    r=copy.deepcopy(rig)
    for p in r.parts:
        p['at']=list(p['at']);p['rot']=list(p['rot'])
        if name=='enderman':
            if pose=='angry' and p['name']=='head':p['at'][1]-=5
            if pose=='carry' and p['name'] in ('right_arm','left_arm'):p['rot']=[-.5,0,.05 if p['name']=='right_arm' else -.05]
            if pose=='walk' and p['name'] in ('right_arm','left_leg'):p['rot'][0]=.35
            if pose=='walk' and p['name'] in ('left_arm','right_leg'):p['rot'][0]=-.35
        elif p['name']=='lid':
            peek=1 if pose=='open' else .7 if pose=='turn' else 0
            s=math.sin((.5+peek)*math.pi);p['at'][1]=16+s*8
            p['rot'][1]=(-1+s)**4*math.pi*.125 if peek>.3 else 0
        elif name=='shulker' and p['name']=='head' and pose=='turn':p['rot']=[-.16,.7,0]
    return r


def model(name,rig,uv,pose='rest',vanilla=False):
    matrices={'root':np.eye(4)};els=[]
    for p in pose_parts(rig,name,pose).parts:
        matrix=matrices[p['parent']]@transform(p);matrices[p['name']]=matrix
        for c in p['cubes']:
            deform=c.get('deform',0);x,y,z=[v-deform for v in c['xyz']];w,h,d=[v+2*deform for v in c['size']];verts={}
            for face in ('north','south','east','west','up','down'):
                vv=[]
                for q in rz.corners([x,-y-h,z],[x+w,-y,z+d],face):
                    v=matrix@np.array([q[0],-q[1],q[2],1]);vv.append([float(v[0]),25-float(v[1]) if name=='enderman' else 24-float(v[1]),float(v[2])])
                verts[face]=vv
            u,v=c['vanilla_uv'] if vanilla else uv[key(c)];tw,th=(64,32 if name=='enderman' else 64) if vanilla else (128,128)
            fd={}
            for face,rect in faces(u,v,*c['size']).items():
                u0,v0,u1,v1=rect
                if c.get('mirror'):u0,u1=u1,u0
                fd[face]={'texture':'#all','uv':[u0/tw*16,v0/th*16,u1/tw*16,v1/th*16]}
            pts=np.array([q for vv in verts.values() for q in vv]);els.append({'part':p['name'],'from':pts.min(0).tolist(),'to':pts.max(0).tolist(),'preview_vertices':verts,'faces':fd})
    ref='file:'+str((OUT/f'vanilla_{name}.png').resolve()) if vanilla else f'tecnocinetico:entity/end_remodel/{name}'
    return {'textures':{'all':ref},'elements':els}


def audit_surfaces(name,m):
    import ast
    try:verify_surfaces(m['elements'])
    except ValueError as error:
        hits=ast.literal_eval(str(error).split(': ',1)[1])
        allowed={('body','head','east'),('body','head','west')}
        if name!='enderman' or not set(hits)<=allowed:raise
        return 'Solo solapamiento cabeza/torso existente en Enderman vanilla; ambas caras son negro uniforme.'
    return 'Sin caras coplanares nuevas.'


def preview(rigs,uvs,assets):
    with zipfile.ZipFile(assets) as z:
        for name,ref in (('enderman','assets/minecraft/textures/entity/enderman/enderman.png'),('shulker','assets/minecraft/textures/entity/shulker/shulker.png')):
            (OUT/f'vanilla_{name}.png').write_bytes(z.read(ref))
        # La capa de ojos vanilla no está necesariamente en la textura base.
        import io
        eye=Image.open(io.BytesIO(z.read('assets/minecraft/textures/entity/enderman/enderman_eyes.png'))).convert('RGBA')
        base=Image.open(OUT/'vanilla_enderman.png').convert('RGBA');b=np.array(base);e=np.array(eye)
        b[:,:,:3]=np.maximum(b[:,:,:3],(e[:,:,:3].astype(float)*e[:,:,3:4]/255).astype('uint8'))
        Image.fromarray(b).save(OUT/'vanilla_enderman.png')
    original=rz.tex_path
    rz.tex_path=lambda ref,tex:Path(ref[5:]) if ref.startswith('file:') else Path(tex[ref[1:]][5:]) if ref.startswith('#') and tex[ref[1:]].startswith('file:') else original(ref,tex)
    sheet=Image.new('RGB',(1800,1600),(27,30,35));draw=ImageDraw.Draw(sheet);report=[]
    for row,name in enumerate(('enderman','shulker')):
        for col,vanilla in enumerate((True,False)):
            r=enderman(False) if name=='enderman' and vanilla else shulker(False) if vanilla else rigs[name]
            m=model(name,r,uvs.get(name),vanilla=vanilla)
            if not vanilla:audit_surfaces(name,m)
            pic=rz.render(m,205,8,14 if name=='enderman' else 26,size=800)
            pic.save(OUT/f'{name}_{"vanilla" if vanilla else "corrompido"}.png');sheet.paste(pic,(col*900+50,row*800+45))
            draw.text((col*900+25,row*800+12),name+' · '+('vanilla' if vanilla else 'red corrompida'),font=font(25),fill='#e0d4e8')
        back=model(name,rigs[name],uvs[name])
        rz.render(back,25,10,15 if name=='enderman' else 25,size=900).save(OUT/f'{name}_espalda.png')
        poses=('angry','carry','walk') if name=='enderman' else ('open','turn')
        for pose in poses:
            for vanilla in (False,True):
                r=enderman(False) if name=='enderman' and vanilla else shulker(False) if vanilla else rigs[name]
                m=model(name,r,uvs[name],pose,vanilla)
                if not vanilla:report.append({'model':name,'pose':pose,'coplanarity':audit_surfaces(name,m)})
                pic=rz.render(m,205,10,13 if name=='enderman' else 22,size=900)
                ImageDraw.Draw(pic).text((18,15),f'{name} · {pose} · '+('vanilla' if vanilla else 'corrompido'),font=font(25),fill='#e1c6ef')
                pic.save(OUT/f'{name}_{pose}{"_vanilla" if vanilla else ""}.png')
                (OUT/f'{name}_{pose}{"_vanilla" if vanilla else ""}.json').write_text(json.dumps(m),encoding='utf-8')
    sheet.save(OUT/'comparativa.png')
    sprites=Image.new('RGB',(1000,300),(27,30,35));d=ImageDraw.Draw(sprites)
    for i in range(4):
        im=Image.open(A/f'textures/particle/data_glitch_{i}.png').resize((160,160),Image.Resampling.NEAREST)
        sprites.paste(im,(i*250+45,50),im);d.text((i*250+15,230),f'data_glitch_{i}',font=font(23),fill='#d9c4e5')
    sprites.save(OUT/'particulas.png')
    (OUT/'auditoria.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--minecraft-assets',type=Path)
    args=parser.parse_args();OUT.mkdir(parents=True,exist_ok=True)
    candidates=[args.minecraft_assets] if args.minecraft_assets else [p/'.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client-extra.jar' for p in (Path.home(),Path('C:/Users/Bryan26'))]
    assets=next((p for p in candidates if p and p.exists()),None)
    if assets is None:raise SystemExit('Indica --minecraft-assets <client-extra.jar> de Minecraft 1.20.1 para la comparación vanilla.')
    rigs={'enderman':finalize(enderman()),'shulker':finalize(shulker())};uvs={}
    for name,rig in rigs.items():
        expected=END_PIVOTS if name=='enderman' else SHULKER_PIVOTS
        roots={p['name']:tuple(p['at']) for p in rig.parts if p['parent']=='root'};assert roots==expected
        uvs[name],_=pack(rig);atlas(name,rig,uvs[name])
    code=['package com.bryanamaya.tecnocinetico.entity.client;',
          'import net.minecraft.client.model.geom.PartPose;',
          'import net.minecraft.client.model.geom.builders.*;',
          '/** Capas de arte; raíces y pivotes vanilla 1.20.1. Generado por tools/end_remodel_gen.py. */',
          'public final class EndRemodelLayers {','    private EndRemodelLayers() {}']
    for name,rig in rigs.items():code.append(layer(name,rig,uvs[name]))
    JAVA.write_text('\n'.join(code+['}'])+'\n',encoding='utf-8');particles();preview(rigs,uvs,assets)
    files=[str(JAVA.relative_to(ROOT)).replace('\\','/')]
    files+=['src/main/resources/assets/tecnocinetico/textures/entity/end_remodel/'+n+s+'.png' for n in rigs for s in ('','_glow')]
    files+=[f'src/main/resources/assets/tecnocinetico/textures/particle/data_glitch_{i}.png' for i in range(4)]
    files+=['src/main/resources/assets/tecnocinetico/particles/data_glitch.json','tools/end_remodel_gen.py']
    (OUT/'archivos.json').write_text(json.dumps(files,indent=2)+'\n',encoding='utf-8')
    (OUT/'integracion.json').write_text(json.dumps({
        'methods':['public static LayerDefinition EndRemodelLayers.enderman()','public static LayerDefinition EndRemodelLayers.shulker()'],
        'models':'Instanciar EndermanModel y ShulkerModel vanilla con estas capas. Sin subclases ni nuevas animaciones.',
        'texture_size':[128,128],'palette':['#C04CFF','#FF4FD8','negro/gris neutro; sin cian'],
        'enderman_roots':END_PIVOTS,'shulker_roots':SHULKER_PIVOTS,
        'enderman_hat':'En vanilla creepy sube head.y 5 px; hat mantiene y=-13 y actúa como mandíbula. jaw_guard/jaw_edge y rejillas son hijos de hat. No copiar otra vez head a hat después de setupAnim.',
        'enderman_eyes':'Sustituir o desactivar la EyesLayer vanilla (su UV 64x32 no coincide). Usar enderman_glow.png sobre el modelo nuevo.',
        'shulker_head':'IMPORTANTE: ShulkerModel.parts() solo devuelve base y lid. getHead() se dibuja aparte. La ShulkerHeadLayer vanilla elige textura por DyeColor mediante método estático de ShulkerRenderer, ignorando un simple override del renderer. Sustituir esa capa por una que use shulker.png y dibuje getHead(), sin tintes de color.',
        'shulker_glow':'Al dibujar shulker_glow.png, renderizar modelo (base+lid) Y getHead() con la misma pose. Así brilla también la lente del sensor.',
        'extra_bones':'Solo hijos decorativos, animados por sus padres. No hay cables rígidos que atraviesen huesos que se separan.',
        'particles':'particles/data_glitch.json contiene tecnocinetico:data_glitch_0..3. PNG 8x8 RGB blanco y alfa, para teñir desde código.',
        'vanilla_preview':'PNG originales leídos del client-extra.jar local SOLO en build/end_remodel/. No se copian a recursos de producción.',
        'scope':'Sin registros, renderizadores, mecánicas, documentación externa a build ni cambios en NetherRemodel.'
    },ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print('End: EndRemodelLayers.enderman()/shulker(), cuatro atlas 128x128 y cuatro sprites blancos 8x8. Vistas en build/end_remodel/.')


if __name__=='__main__':main()
