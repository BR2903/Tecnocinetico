"""Poses del mismo rig/UV/curvas que Java; profundidad real y jugador de 1.8 m.
python tools/preview_abyss_foundry.py
"""
import copy,json,math
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw
import render_zbuffer as rz
from nether_remodel_gen import pack,key,faces,transform,verify_surfaces
from abyss_foundry_rig import geometry,pose
from abyss_foundry_gen import A,OUT
from preview_combustion_drill import font


def scene(attack=0,t=0,exposed=False):
    rig=geometry();uv,_=pack(rig);channels=pose(attack,t,exposed)
    root={'at':[0,channels.get('root.y',0),0],'rot':[0,0,0]}
    matrices={'root':transform(root)};m={'textures':{'all':'tecnocinetico:entity/abyss_foundry'},'elements':[]}
    for source in rig.parts:
        p=copy.deepcopy(source)
        p['at']=list(p['at']);p['rot']=list(p['rot'])
        for i,k in enumerate(('x','y','z')):
            p['at'][i]+=channels.get(p['name']+'.'+k,0)
            p['rot'][i]+=channels.get(p['name']+'.'+k+'Rot',0)
        if p['name']=='slag_stream':
            if attack!=3 or t<=.84 or exposed:continue
            p['rot'][0]=-sum(channels.get(b+'.xRot',0) for b in ('body','left_arm','left_elbow','crucible'))
        matrix=matrices[p['parent']]@transform(p);matrices[p['name']]=matrix
        for c in p['cubes']:
            xyz=c['xyz'];ow,oh,od=c['size'];inset=c['inset']
            w,h,d=[s-2*inset for s in c['size']];x,y,z=[v+inset for v in xyz];verts={}
            # Construct v=0 on the TOP face edge, then articulate in Java's Y-down frame.
            for f in ('north','south','east','west','up','down'):
                pts=rz.corners([x,-y-h,z],[x+w,-y,z+d],f);arr=[]
                for q in pts:
                    v=matrix@np.array([q[0],-q[1],q[2],1]);arr.append([float(v[0])*2,(24-float(v[1]))*2,float(v[2])*2])
                verts[f]=arr
            allp=np.array([q for pts in verts.values() for q in pts]);u,v=uv[key(c)]
            m['elements'].append({'part':p['name'],'from':allp.min(0).tolist(),'to':allp.max(0).tolist(),'preview_vertices':verts,
                                 'faces':{f:{'texture':'#all','uv':[a/8 for a in rect]} for f,rect in faces(u,v,ow,oh,od).items()}})
    player(m)
    return m


def player(m):
    # Figura Minecraft de 1.8 bloques: la comparación usa la misma escala métrica, sin perspectiva forzada.
    pic=Image.new('RGBA',(16,16),(119,134,139,255));pic.save(OUT/'jugador.png')
    def box(f,t,color):
        path=OUT/f'jugador_{color}.png'
        if not path.exists():Image.new('RGBA',(16,16),'#'+color).save(path)
        m['elements'].append({'part':'player','from':f,'to':t,'faces':{face:{'texture':'file:'+str(path.resolve()),'uv':[0,0,16,16]} for face in ('north','south','east','west','up','down')}})
    # 28.8 px de altura = 1.8 bloques.
    x=51;s=.9
    for f,t,c in [([-4,24,-4],[4,32,4],'b59577'),([-4,12,-2],[4,24,2],'526e78'),
                   ([-8,12,-2],[-4,24,2],'526e78'),([4,12,-2],[8,24,2],'526e78'),
                   ([-4,0,-2],[-.15,12,2],'383e42'),([.15,0,-2],[4,12,2],'383e42'),
                   ([-2.5,28,-4.1],[-1.3,29,-3.9],'303130'),([1.3,28,-4.1],[2.5,29,-3.9],'303130')]:
        box([f[0]*s+x,f[1]*s,f[2]*s],[t[0]*s+x,t[1]*s,t[2]*s],c)


def main():
    OUT.mkdir(parents=True,exist_ok=True);original=rz.tex_path
    def path(ref,tex):return Path(ref[5:]) if ref.startswith('file:') else original(ref,tex)
    rz.tex_path=path
    cases=[('reposo','Reposo',0,0,False)]
    for attack,label in ((1,'Martillo'),(2,'Fractura'),(3,'Escoria'),(4,'Sobrecalentamiento')):
        cases.extend([(f'ataque_{attack}_aviso',label+' · aviso',attack,.78,False),(f'ataque_{attack}_impacto',label+' · impacto',attack,1,False)])
    cases.append(('expuesto','Expuesto · núcleo abierto',0,0,True))
    models=[scene(a,t,e) for _,_,a,t,e in cases]
    for m in models:verify_surfaces([e for e in m['elements'] if e['part']!='player'])
    allp=np.array([q for m in models for e in m['elements'] for q in (e['from'],e['to'])]);lo=allp.min(0);hi=allp.max(0)
    sheet=Image.new('RGB',(2000,1100),(31,35,37));draw=ImageDraw.Draw(sheet)
    for i,((name,label,*_),m) in enumerate(zip(cases,models)):
        # Mismo encuadre y escala para todas las poses, incluyendo herramientas elevadas.
        m['elements'].append({'part':'camera_bounds','from':lo.tolist(),'to':hi.tolist(),'faces':{}})
        (OUT/f'{name}.json').write_text(json.dumps(m),encoding='utf-8')
        im=rz.render(m,205,12,6,size=1100);d=ImageDraw.Draw(im)
        d.text((25,25),'FUNDIDOR DEL ABISMO',font=font(28),fill='#efe6ce')
        d.text((25,65),label+' · jugador 1,8 m',font=font(23),fill='#dbac68');im.save(OUT/f'{name}.png')
        x,y=i%5*400,i//5*550;sheet.paste(im.resize((400,400)),(x,y+40))
        draw.text((x+12,y+12),label,font=font(18),fill='#eddfc2')
    sheet.save(OUT/'comparativa.png')
    back=scene();rz.render(back,35,16,9,size=1000).save(OUT/'espalda.png')
    print('Vistas Fundidor: build/fundidor/comparativa.png (10 poses) y espalda.png')


if __name__=='__main__':main()
