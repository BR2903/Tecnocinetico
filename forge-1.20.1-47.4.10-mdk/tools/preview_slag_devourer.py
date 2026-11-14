"""Rig, UV y poses del Devorador con profundidad real y jugador a escala.
python tools/preview_slag_devourer.py
Incluye estados invisibles de EMERGE, marcha y objeto en inventario/mano/suelo.
"""
import copy,json,math
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw
import render_zbuffer as rz
from nether_remodel_gen import pack,key,faces,transform,verify_surfaces
from slag_devourer_rig import geometry,animation
from slag_devourer_gen import A,OUT
from preview_weapon_remodel import prepared
from preview_combustion_drill import font,raster


def filetex(name,color):
    path=OUT/(name+'.png')
    if not path.exists():Image.new('RGBA',(16,16),color).save(path)
    return 'file:'+str(path.resolve())


def box(scene,f,t,ref,part):
    scene['elements'].append({'part':part,'from':f,'to':t,'faces':{face:{'texture':ref,'uv':[0,0,16,16]} for face in ('north','south','east','west','up','down')}})


def player(scene):
    # Altura 28.8 px = 1.8 bloques. En el mismo espacio 3D que la criatura.
    for f,t,c in [([-4,24,-4],[4,32,4],'b59577'),([-4,12,-2],[4,24,2],'526e78'),
                  ([-8,12,-2],[-4,24,2],'526e78'),([4,12,-2],[8,24,2],'526e78'),
                  ([-4,0,-2],[-.15,12,2],'383e42'),([.15,0,-2],[4,12,2],'383e42'),
                  ([-2.5,28,-4.1],[-1.3,29,-3.9],'303130'),([1.3,28,-4.1],[2.5,29,-3.9],'303130')]:
        box(scene,[f[0]*.9+56,f[1]*.9,f[2]*.9-38],[t[0]*.9+56,t[1]*.9,t[2]*.9-38],filetex('jugador_'+c,'#'+c),'player')


def scene(attack=0,t=0,exposed=False,age=0,limb=0,amount=0,burrowed=False):
    rig=geometry();uv,_=pack(rig);ch=animation(attack,t,exposed,age,limb,amount)
    root={'at':[0,ch.get('root.y',0),0],'rot':[0,0,0]};matrices={'root':transform(root)}
    m={'textures':{'all':'tecnocinetico:entity/slag_devourer'},'elements':[]}
    if not burrowed:
        for source in rig.parts:
            p=copy.deepcopy(source);p['at']=list(p['at']);p['rot']=list(p['rot'])
            for i,axis in enumerate(('x','y','z')):
                p['at'][i]+=ch.get(p['name']+'.'+axis,0);p['rot'][i]+=ch.get(p['name']+'.'+axis+'Rot',0)
            scale=np.eye(4);scale[0,0],scale[1,1],scale[2,2]=p.get('scale',(1,1,1))
            matrix=matrices[p['parent']]@transform(p)@scale;matrices[p['name']]=matrix
            for c in p['cubes']:
                inset=c['inset'];x,y,z=[v+inset for v in c['xyz']];w,h,d=[v-2*inset for v in c['size']]
                verts={}
                for f in ('north','south','east','west','up','down'):
                    arr=[]
                    for q in rz.corners([x,-y-h,z],[x+w,-y,z+d],f):
                        v=matrix@np.array([q[0],-q[1],q[2],1]);arr.append([float(v[0])*2,(24-float(v[1]))*2,float(v[2])*2])
                    verts[f]=arr
                pts=np.array([v for face in verts.values() for v in face]);u,v=uv[key(c)]
                m['elements'].append({'part':p['name'],'from':pts.min(0).tolist(),'to':pts.max(0).tolist(),'preview_vertices':verts,
                                     'faces':{f:{'texture':'#all','uv':[n/8 for n in rect]} for f,rect in faces(u,v,*c['size']).items()}})
    player(m)
    # El suelo tiene geometría real pero no altera el encuadre del modelo.
    m['elements'].append({'part':'ground','from':[0,0,0],'to':[0,0,0],
        'preview_vertices':{'up':rz.corners([-120,-.3,-100],[130,-.2,140],'up')},
        'faces':{'up':{'texture':filetex('suelo','#37332d'),'uv':[0,0,16,16]}}})
    return m


def label(im,title,subtitle='Jugador de 1,8 m · escala y profundidad reales'):
    d=ImageDraw.Draw(im);d.text((22,18),'DEVORADOR DE ESCORIA',font=font(27),fill='#eee1bb')
    d.text((22,57),title,font=font(23),fill='#e1a36b');d.text((22,92),subtitle,font=font(17),fill='#b8c0b5')
    return im


def main():
    OUT.mkdir(parents=True,exist_ok=True);old=rz.tex_path
    rz.tex_path=lambda ref,tex:Path(ref[5:]) if ref.startswith('file:') else old(ref,tex)
    cases=[('reposo','Reposo',0,0,False,False)]
    for a,name in ((1,'Mordisco'),(2,'Preparación de carga'),(3,'Embestida'),(4,'Escoria'),(5,'Excavar'),(6,'Bajo tierra'),(7,'Soltar ácaros')):
        for impact,t in ((False,.76),(True,1)):
            phase='impacto' if impact else 'aviso'
            caption=phase if a not in (2,3,6) else ('fin del aviso' if impact else 'apuntando') if a==2 else ('final' if impact else 'inicio') if a==3 else 'modelo oculto'
            cases.append((f'ataque_{a}_{phase}',name+' · '+caption,a,t,False,a==6))
    cases.append(('expuesto','Expuesto · placas abiertas y cuerpo caído',0,1,True,False))
    models=[scene(a,t,e,age=12,burrowed=b) for _,_,a,t,e,b in cases]
    audit=[]
    for (name,*_),m in zip(cases,models):
        surfaces=[e for e in m['elements'] if e['part'] not in ('player','ground')]
        verify_surfaces(surfaces);audit.append({'pose':name,'cubes':len(surfaces),'coplanar_faces':0})
    pts=np.array([p for m in models for e in m['elements'] if e['part']!='ground' for p in (e['from'],e['to'])]);lo=pts.min(0);hi=pts.max(0)
    def framed(m):
        m=copy.deepcopy(m);m['elements'].append({'part':'camera','from':lo.tolist(),'to':hi.tolist(),'faces':{}});return m
    gallery=Image.new('RGB',(2200,1680),(29,33,33));draw=ImageDraw.Draw(gallery)
    for i,((name,title,*_),m) in enumerate(zip(cases,models)):
        m=framed(m);(OUT/f'{name}.json').write_text(json.dumps(m),encoding='utf-8')
        im=label(rz.render(m,235,17,7.2,size=1200),title);im.save(OUT/f'{name}.png')
        x,y=i%4*550,i//4*420;gallery.paste(im.crop((0,210,1200,1110)).resize((550,412)),(x,y+8))
        draw.rectangle((x,y,x+550,y+32),fill='#1d2121');draw.text((x+10,y+5),title,font=font(16),fill='#e2d4ae')
    gallery.save(OUT/'comparativa.png')
    for yaw,title in ((180,'frente'),(90,'lateral'),(25,'espalda')):
        label(rz.render(scene(age=12),yaw,12,8,size=1200),'Reposo · '+title).save(OUT/f'reposo_{title}.png')
    walking=[]
    for i in range(12):
        m=scene(age=i*2,limb=i/12*math.pi*2/.8,amount=.17)
        walking.append(label(rz.render(framed(m),235,17,6.5,size=1000),'Marcha · segmentos y garras'))
    walking[0].save(OUT/'marcha.gif',save_all=True,append_images=walking[1:],duration=90,loop=0)
    # Archivo de marcha a máxima extensión para examinar uniones y patas.
    walking[3].save(OUT/'marcha.png')
    item=json.loads((A/'models/item/forge_heart.json').read_text());items=Image.new('RGB',(1500,600),(29,33,33));d=ImageDraw.Draw(items)
    for i,context in enumerate(('gui','firstperson_righthand','ground')):
        im=raster(item,context);im.save(OUT/f'forge_heart_{context}.png');im.thumbnail((490,490))
        items.paste(im,(i*500+(500-im.width)//2,70));d.text((i*500+18,20),context,font=font(21),fill='#e1d5b4')
    items.save(OUT/'forge_heart.png')
    rz.render(prepared(item),205,18,35,size=800).save(OUT/'forge_heart_3cuartos.png')
    (OUT/'auditoria_poses.json').write_text(json.dumps(audit,indent=2)+'\n',encoding='utf-8')
    print('Vistas: build/devorador/comparativa.png, marcha.gif, forge_heart.png; 16 poses sin caras coplanares.')


if __name__=='__main__':main()
