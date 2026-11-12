"""Genera únicamente arte: capas Java, seis atlas y geometría de previsualización.

python tools/nether_remodel_gen.py
Las UV se empaquetan por caja/material, con margen y sin solapamientos. No importa
modelos ni imágenes de otros mods; el pintado es propio y reproducible.
"""
import json
import math
import random
import zlib
from pathlib import Path
import numpy as np
from PIL import Image
from nether_remodel_rigs import all_rigs, HUMANOID, PLAYER_EMPTY
from render_zbuffer import corners

ROOT=Path(__file__).resolve().parent.parent
CLIENT=ROOT/'src/main/java/com/bryanamaya/tecnocinetico/entity/client'
TEXTURES=ROOT/'src/main/resources/assets/tecnocinetico/textures/entity/nether_remodel'
OUT=ROOT/'build/nether_remodel'
COLORS={
    'skin':(169,119,95),'pig_head':(175,125,100),'snout':(193,139,119),
    'rot':(119,119,77),'rot_head':(131,129,90),'snout_rot':(149,131,100),
    'armor':(51,52,52),'armor_light':(87,84,75),'dark':(27,29,30),
    'copper':(160,91,43),'brass':(180,136,63),'rust':(98,61,38),
    'ivory':(220,202,156),'bone_light':(183,175,142),'bone':(85,81,70),
    'leather':(92,57,37),'cloth':(58,45,37),'flesh':(114,44,38),
    'ghast_skin':(180,170,155),'ghast_tube':(116,111,99),
    'blaze_face':(167,99,39),'wither_skull':(77,75,67),
    'ember':(255,126,29),'ember_red':(245,77,25),'ember_hot':(255,194,66),
}

def key(c):return (c['material'],*c['size'])
def faces(u,v,w,h,d):
    return {'west':(u,v+d,u+d,v+d+h),'north':(u+d,v+d,u+d+w,v+d+h),
            'east':(u+d+w,v+d,u+2*d+w,v+d+h),'south':(u+2*d+w,v+d,u+2*d+2*w,v+d+h),
            'up':(u+d,v,u+d+w,v+d),'down':(u+d+w,v,u+d+2*w,v+d)}

def pack(rig):
    specs={key(c):(math.ceil(2*(c['size'][0]+c['size'][2]))+2,
                   math.ceil(c['size'][1]+c['size'][2])+2) for p in rig.parts for c in p['cubes']}
    for strategy in range(3):
        free=[(0,0,128,128)];result={}
        ordered=sorted(specs,key=lambda k:(max(specs[k]) if strategy==0 else specs[k][1] if strategy==1 else specs[k][0]*specs[k][1],specs[k][0]*specs[k][1]),reverse=True)
        for k in ordered:
            w,h=specs[k];fits=[(min(fw-w,fh-h),fw*fh-w*h,i) for i,(x,y,fw,fh) in enumerate(free) if w<=fw and h<=fh]
            if not fits:break
            _,_,i=min(fits);x,y,fw,fh=free.pop(i);result[k]=(x+1,y+1)
            if fw-w>fh-h:pieces=[(x+w,y,fw-w,fh),(x,y+h,w,fh-h)]
            else:pieces=[(x+w,y,fw-w,h),(x,y+h,fw,fh-h)]
            free.extend(p for p in pieces if p[2]>0 and p[3]>0)
        else:return result,specs
    raise ValueError(f'No cabe el atlas: {len(specs)} redes, área {sum(w*h for w,h in specs.values())}/16384')

def color_pixel(mat,face,x,y,w,h,rng):
    base=COLORS[mat];delta=rng.randrange(-6,7)
    edge=x==0 or y==h-1 or x==w-1
    if mat.startswith('ember'):
        delta=18 if (x+y)%3==0 else -8
    elif mat in ('armor','armor_light','copper','brass','rust','dark'):
        if edge:delta-=19
        elif y==0:delta+=23
        elif w>=5 and h>=4 and x in (1,w-2) and y in (1,h-2):delta+=48
        elif mat=='rust' and rng.randrange(4)==0:return (144,77,35,255)
        elif mat in ('dark','copper') and y%4==0:delta-=12
    elif mat in ('rot','rot_head','snout_rot'):
        if rng.randrange(8)==0:return (83,86,55,255)
        if mat=='rot_head' and face=='north' and x>w*.56 and y<h*.45:return (160+delta,151+delta,120+delta,255)
    elif mat in ('skin','pig_head','snout'):
        if edge:delta-=9
        if y==0:delta+=8
    elif mat=='ghast_skin':
        if (x//3+y//4)%4==0:delta-=12
        if y>h*.65 and x%4==0:delta-=24
    elif mat=='ghast_tube':
        if y%3==0:delta-=24
    elif mat in ('cloth','leather'):
        if edge:delta-=15
        if x==1 and y%3==0:delta+=23
    if face=='north':
        if mat in ('pig_head','rot_head'):
            if y==3 and x in (1,2,7,8):return (239,216,170,255)
            if y==3 and x in (2,7):return (55,29,21,255)
        elif mat.startswith('snout'):
            if y==1 and x in (0,w-1):return (66,44,35,255)
        elif mat=='wither_skull':
            if 2<=y<=4 and x in (1,2,5,6):return (16,18,18,255)
            if y==4 and x in (3,4):return (27,26,24,255)
            if y==6 and 1<=x<=6:return (25,23,22,255) if x%2 else (120,112,91,255)
        elif mat=='blaze_face' and y>=h-2 and x%3==0:delta+=29
    return tuple(max(0,min(255,c+delta)) for c in base)+(255,)

def atlas(name,rig,uv):
    im=Image.new('RGBA',(128,128),(24,25,26,255));glow=Image.new('RGBA',(128,128))
    for k,(u,v) in uv.items():
        mat,w,h,d=k;rng=random.Random(zlib.crc32(str(k).encode()))
        for face,rect in faces(u,v,w,h,d).items():
            x0,y0,x1,y1=rect
            for y in range(math.floor(y0),math.ceil(y1)):
                for x in range(math.floor(x0),math.ceil(x1)):
                    c=color_pixel(mat,face,x-math.floor(x0),y-math.floor(y0),max(1,math.ceil(x1-x0)),max(1,math.ceil(y1-y0)),rng)
                    im.putpixel((x,y),c)
                    if mat.startswith('ember'):glow.putpixel((x,y),c)
    TEXTURES.mkdir(parents=True,exist_ok=True)
    im.save(TEXTURES/f'{name}.png');glow.save(TEXTURES/f'{name}_glow.png')

def number(n):return f'{float(n):.6f}F'
def layer(name,rig,uv):
    mesh='net.minecraft.client.model.PiglinModel.createMesh(CubeDeformation.NONE)' if name in ('piglin','piglin_brute','zombified_piglin') else 'net.minecraft.client.model.HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F)' if name=='wither_skeleton' else 'new MeshDefinition()'
    lines=[f'    public static LayerDefinition {name}() {{',f'        var mesh = {mesh};','        var root = mesh.getRoot();']
    for p in rig.parts:
        builder='CubeListBuilder.create()'
        for c in p['cubes']:
            u,v=uv[key(c)];builder+=f'.texOffs({u}, {v}).addBox('+', '.join(number(n) for n in (*c['xyz'],*c['size']))+')'
        pose='PartPose.offsetAndRotation('+', '.join(number(n) for n in (*p['at'],*p['rot']))+')'
        if name=='blaze' and p['parent']=='root' and p['name'].startswith('part'):pose=f'blazePivot({p["name"][4:]})'
        lines.append(f'        var {p["name"]} = {p["parent"]}.addOrReplaceChild("{p["name"]}", {builder}, {pose});')
    return '\n'.join(lines+['        return LayerDefinition.create(mesh, 128, 128);','    }'])

def transform(p):
    x,y,z=p['rot'];cx,sx,cy,sy,cz,sz=math.cos(x),math.sin(x),math.cos(y),math.sin(y),math.cos(z),math.sin(z)
    rx=np.array([[1,0,0],[0,cx,-sx],[0,sx,cx]]);ry=np.array([[cy,0,sy],[0,1,0],[-sy,0,cy]]);rz=np.array([[cz,-sz,0],[sz,cz,0],[0,0,1]])
    m=np.eye(4);m[:3,:3]=rz@ry@rx;m[:3,3]=p['at'];return m

def verify_surfaces(elements):
    """Caras orientadas iguales en el mismo plano: comprobar vértices reales, no AABB de piezas giradas."""
    def overlap(a,b):
        for polygon in (a,b):
            for first,second in zip(polygon,np.roll(polygon,-1,axis=0)):
                edge=second-first;axis=np.array([-edge[1],edge[0]]);axis/=np.linalg.norm(axis)
                ap=a@axis;bp=b@axis
                if min(ap.max(),bp.max())-max(ap.min(),bp.min())<=.0001:return False
        return True
    groups={};hits=[]
    for i,e in enumerate(elements):
        for face,points in e['preview_vertices'].items():
            p=np.array(points);n=np.cross(p[1]-p[0],p[3]-p[0]);n/=np.linalg.norm(n)
            k=(*np.round(n,4),round(float(n@p[0]),4));axes=[j for j in range(3) if j!=int(np.argmax(np.abs(n)))]
            quad=p[:,axes]
            for j,other in groups.get(k,[]):
                if overlap(quad,other):hits.append((e['part'],elements[j]['part'],face))
            groups.setdefault(k,[]).append((i,quad))
    if hits:raise ValueError(f'Caras coplanares solapadas: {hits}')

def preview(name,rig,uv,pose='rest'):
    transforms={'root':np.eye(4)};elements=[]
    for source in rig.parts:
        p=dict(source)
        if pose=='walk' and p['name'] in ('right_arm','left_leg'):p['rot']=(.6,0,0)
        elif pose=='walk' and p['name'] in ('left_arm','right_leg'):p['rot']=(-.6,0,0)
        m=transforms[p['parent']]@transform(p);transforms[p['name']]=m
        for c in p['cubes']:
            xyz,size=c['xyz'],c['size'];end=[a+b for a,b in zip(xyz,size)];verts={}
            for face in ('west','north','east','south','up','down'):
                verts[face]=[]
                for point in corners(xyz,end,face):
                    v=m@np.array([*point,1]);verts[face].append([float(v[0])+8,24-float(v[1]),float(v[2])+8])
            pts=np.array([v for face in verts.values() for v in face]);u,v=uv[key(c)]
            elements.append({'part':p['name'],'from':pts.min(0).tolist(),'to':pts.max(0).tolist(),'preview_vertices':verts,
                             'faces':{face:{'texture':'#all','uv':[n/8 for n in rect]} for face,rect in faces(u,v,*size).items()}})
    verify_surfaces(elements)
    target=OUT/f'{name}{"" if pose=="rest" else "_"+pose}.json'
    target.write_text(json.dumps({'textures':{'all':f'tecnocinetico:entity/nether_remodel/{name}'},'elements':elements}),encoding='utf-8')

def validate(name,rig):
    known={'root'}
    for p in rig.parts:
        if p['parent'] not in known or p['name'] in known:raise ValueError(f'{name}: jerarquía inválida {p["name"]}')
        known.add(p['name'])
        for c in p['cubes']:
            if min(c['size'])<=0:raise ValueError('Caja vacía')
    roots={p['name']:p for p in rig.parts if p['parent']=='root'}
    if name in ('piglin','piglin_brute','zombified_piglin'):
        assert set(roots)==set(HUMANOID)|set(PLAYER_EMPTY)
        for bone,at in {**HUMANOID,**PLAYER_EMPTY}.items():assert roots[bone]['at']==at
        for s,bone in ((-1,'right_ear'),(1,'left_ear')):
            p=next(p for p in rig.parts if p['name']==bone);assert p['parent']=='head' and p['at']==(s*4.5,-6,0)
    elif name=='wither_skeleton':assert set(roots)==set(HUMANOID)
    elif name=='ghast':assert set(roots)=={'body'}|{f'tentacle{i}' for i in range(9)}
    elif name=='blaze':assert set(roots)=={'head'}|{f'part{i}' for i in range(12)}

def main():
    OUT.mkdir(parents=True,exist_ok=True);methods=[];report={}
    for name,rig in all_rigs().items():
        validate(name,rig);uv,specs=pack(rig);atlas(name,rig,uv);methods.append(layer(name,rig,uv));preview(name,rig,uv)
        if name in ('piglin','piglin_brute','zombified_piglin','wither_skeleton'):preview(name,rig,uv,'walk')
        report[name]={'parts':len(rig.parts),'cubes':sum(len(p['cubes']) for p in rig.parts),'nets':len(uv),'coplanar_overlaps':0,
                      'atlas_area':sum(w*h for w,h in specs.values()),'roots':{p['name']:{'at':p['at'],'rot':p['rot']} for p in rig.parts if p['parent']=='root'}}
    header='''package com.bryanamaya.tecnocinetico.entity.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

/** Arte generado por tools/nether_remodel_gen.py; editar los rigs, no este archivo.
 * Conserva las raíces/pivotes vanilla. Accesorios e iluminación son hijos del esqueleto nativo.
 * No registra capas, entidades ni renderizadores.
 */
public final class NetherRemodelLayers {
    private NetherRemodelLayers() {}

    // Mismas operaciones float y trigonometría Mth que BlazeModel.createBodyLayer.
    private static PartPose blazePivot(int i) {
        int first=i<4?0:i<8?4:8;
        float angle=i<4?0.0F:i<8?(float)Math.PI/4.0F:0.47123894F;
        for(int j=first;j<i;j++)angle++;
        float radius=i<4?9.0F:i<8?7.0F:5.0F;
        float y=i<4?-2.0F+net.minecraft.util.Mth.cos((float)(i*2)*0.25F)
            :i<8?2.0F+net.minecraft.util.Mth.cos((float)(i*2)*0.25F)
            :11.0F+net.minecraft.util.Mth.cos((float)i*1.5F*0.5F);
        return PartPose.offset(net.minecraft.util.Mth.cos(angle)*radius,y,net.minecraft.util.Mth.sin(angle)*radius);
    }
'''
    (CLIENT/'NetherRemodelLayers.java').write_text(header+'\n\n'.join(methods)+'\n}\n',encoding='utf-8')
    (OUT/'rig_report.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    for name,v in report.items():print(f'{name}: {v["cubes"]} cajas; atlas {v["atlas_area"]}/16384 px; raíces vanilla verificadas')

if __name__=='__main__':main()
