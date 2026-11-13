"""Petróleo: vistas texturizadas con profundidad, animaciones reales y manos a FOV 70.
python tools/preview_petroleum.py
No abre Minecraft. La cámara simula la pose estable del taladro; ver integracion.json.
"""
import copy,json,math,random
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw
import render_zbuffer as rz
from petroleum_art import A,OUT,FLUIDS
from preview_weapon_remodel import prepared
from preview_combustion_drill import font,euler


def load(name,block=False):return json.loads((A/f'models/{"block" if block else "item"}/{name}.json').read_text(encoding='utf-8'))


def hand(model,left=False):
    """Cámara perspectiva con z-buffer, atlas múltiples y bloque delante a 2 m."""
    w,h=960,540;canvas=np.zeros((h,w,3))+np.array([.11,.13,.15]);depth=np.full((h,w),-1e9)
    focal=h/2/math.tan(math.radians(70)/2);textures={}
    d=model['display']['firstperson_'+('lefthand' if left else 'righthand')]
    scale=np.array(d.get('scale',[1]*3));tr=np.array(d.get('translation',[0]*3),dtype=float);angles=d.get('rotation',[0]*3).copy()
    if left:tr[0]*=-1;angles[1]*=-1;angles[2]*=-1
    def project(p):
        distance=-p[2]
        return [w/2+p[0]*focal/distance,h/2-p[1]*focal/distance,1/distance]
    # Bloque de madera independiente en el mundo, centrado en la cruz (cara frontal a 2 m).
    wood=Image.new('RGBA',(16,16));pixels=wood.load()
    for y in range(16):
        for x in range(16):pixels[x,y]=(86+(x*7+y*3)%19,68+(x*3+y)%13,43+(x+y)%10,255)
    wood.save(OUT/'madera_preview.png')
    faces=[]
    for f in ('north','south','east','west','up','down'):
        faces.append((f,[project(q) for q in rz.corners([-.5,-.5,-3],[.5,.5,-2],f)],wood,[0,0,16,16]))
    for e in prepared(model)['elements']:
        for f,fd in e['faces'].items():
            path=rz.tex_path(fd['texture'],model['textures'])
            if path not in textures:
                pic=Image.open(path).convert('RGBA');textures[path]=pic.crop((0,0,pic.width,pic.width)) if pic.height>pic.width else pic
            pts=[]
            for p in e['preview_vertices'][f]:
                q=euler((np.array(p)-8)*scale,angles)/16+tr/16+np.array([-.5 if left else .5,-.5,-.66])
                pts.append(project(q))
            faces.append((f,pts,textures[path],fd['uv']))
    for face,points,pic,rect in faces:
        tx=np.array(pic)/255;th,tw=tx.shape[:2];u0,v0,u1,v1=rect
        uv=np.array([(u0,v0),(u1,v0),(u1,v1),(u0,v1)])
        for ids in ((0,1,2),(0,2,3)):
            p=np.array(points)[list(ids)];t=uv[list(ids)]
            minx=max(0,int(p[:,0].min()));maxx=min(w-1,int(p[:,0].max())+1)
            miny=max(0,int(p[:,1].min()));maxy=min(h-1,int(p[:,1].max())+1)
            if minx>maxx or miny>maxy:continue
            (x1,y1,z1),(x2,y2,z2),(x3,y3,z3)=p
            den=(y2-y3)*(x1-x3)+(x3-x2)*(y1-y3)
            if abs(den)<1e-10:continue
            x,y=np.meshgrid(np.arange(minx,maxx+1)+.5,np.arange(miny,maxy+1)+.5)
            a=((y2-y3)*(x-x3)+(x3-x2)*(y-y3))/den;b=((y3-y1)*(x-x3)+(x1-x3)*(y-y3))/den;c=1-a-b
            z=a*z1+b*z2+c*z3
            u=(a*t[0,0]*z1+b*t[1,0]*z2+c*t[2,0]*z3)/z;v=(a*t[0,1]*z1+b*t[1,1]*z2+c*t[2,1]*z3)/z
            uu=np.clip((u/16*tw).astype(int),0,tw-1);vv=np.clip((v/16*th).astype(int),0,th-1);color=tx[vv,uu]
            old=depth[miny:maxy+1,minx:maxx+1];valid=(a>=0)&(b>=0)&(c>=0)&(color[:,:,3]>.5)&(z>old)
            old[valid]=z[valid];canvas[miny:maxy+1,minx:maxx+1][valid]=color[:,:,:3][valid]*max(.7,rz.SHADE[face])
    im=Image.fromarray((np.clip(canvas,0,1)*255).astype('uint8'));draw=ImageDraw.Draw(im)
    draw.line((472,270,488,270),fill='#b7c2b4');draw.line((480,262,480,278),fill='#b7c2b4')
    draw.text((15,15),'FOV 70 · '+('izquierda' if left else 'derecha')+' · madera a 2 bloques',font=font(18),fill='#d1d8d2')
    return im


def material_sheet(frame=0):
    sheet=Image.new('RGB',(1500,930),(31,36,39));draw=ImageDraw.Draw(sheet)
    for col,(name,cfg) in enumerate(FLUIDS.items()):
        draw.text((col*500+25,15),name,font=font(24),fill='#d9d6bd')
        source=Image.open(A/f'textures/block/{name}_still.png');tile=source.crop((0,frame%8*16,16,frame%8*16+16))
        plane=Image.new('RGBA',(80,80))
        for y in range(5):
            for x in range(5):plane.paste(tile,(x*16,y*16))
        back=Image.new('RGBA',(80,80));p=back.load();rng=random.Random(103)
        for y in range(80):
            for x in range(80):
                k=rng.uniform(.86,1.13);p[x,y]=tuple(round(c*k) for c in (100,99,88))+(255,)
        back.alpha_composite(plane);sheet.paste(back.convert('RGB').resize((400,400),Image.Resampling.NEAREST),(col*500+45,65))
        draw.text((col*500+45,480),'5 × 5 · transparencia sobre fondo',font=font(18),fill='#b3bbb2')
        flow=Image.open(A/f'textures/block/{name}_flow.png');f=frame%4
        flow.crop((0,f*32,32,f*32+32)).resize((128,128),Image.Resampling.NEAREST).save(OUT/f'{name}_flow_frame.png')
    names=('crude_oil_bucket','gasoline_bucket','kinetic_fuel_bucket','tar','fuel_canister','kinetic_fuel_cell')
    for i,name in enumerate(names):
        im=Image.open(A/f'textures/item/{name}.png').resize((176,176),Image.Resampling.NEAREST)
        x=i%6*250+35;sheet.paste(im,(x,600),im)
        draw.text((i*250+12,805),name,font=font(15),fill='#ddd8bc')
    return sheet


def main():
    OUT.mkdir(parents=True,exist_ok=True)
    machines=Image.new('RGB',(1600,900),(30,35,39));draw=ImageDraw.Draw(machines)
    for col,name in enumerate(('oil_pump','refinery')):
        frames=[]
        for on in (False,True):
            m=prepared(load(name+('_on' if on else ''),True));im=rz.render(m,205,22,32,size=800)
            im.save(OUT/f'{name}{"_on" if on else ""}.png');frames.append(im)
        machines.paste(frames[0].resize((400,400)),(col*800,45));machines.paste(frames[1].resize((400,400)),(col*800+400,45))
        draw.text((col*800+20,15),name+' · apagada / encendida',font=font(23),fill='#dcd8bc')
        back=rz.render(prepared(load(name,True)),25,20,25,size=600);back.save(OUT/f'{name}_espalda.png')
        machines.paste(back.resize((370,370)),(col*800+215,500))
        frames[0].save(OUT/f'{name}_estados.gif',save_all=True,append_images=[frames[1]],duration=1000,loop=0)
    machines.save(OUT/'maquinas.png')
    saw=load('chainsaw');preview=Image.new('RGB',(1800,1150),(30,35,39));draw=ImageDraw.Draw(preview)
    im=rz.render(prepared(saw),110,20,24,size=1000);im.save(OUT/'chainsaw_3cuartos.png')
    preview.paste(im.resize((900,900)),(0,100));draw.text((20,25),'Motosierra · modelo 3D',font=font(26),fill='#ddd6b8')
    for i,left in enumerate((False,True)):
        pic=hand(saw,left);pic.save(OUT/f'chainsaw_primera_{"izquierda" if left else "derecha"}.png')
        preview.paste(pic.resize((900,506)),(900,i*550+25))
    preview.save(OUT/'motosierra.png')
    rz.render(prepared(saw),-90,0,25,size=1000).save(OUT/'chainsaw_lateral.png')
    # Inventario 3D, transforms reales de gui_fit y múltiples texturas.
    from preview_combustion_drill import euler as rotate
    inv=copy.deepcopy(prepared(saw));disp=saw['display']['gui']
    for e in inv['elements']:
        for f,points in e['preview_vertices'].items():
            e['preview_vertices'][f]=[(rotate((np.array(p)-8)*np.array(disp['scale']),disp['rotation'])+np.array(disp['translation'])).tolist() for p in points]
        pts=np.array([p for face in e['preview_vertices'].values() for p in face]);e['from']=pts.min(0).tolist();e['to']=pts.max(0).tolist()
    rz.render(inv,0,0,35,size=700).save(OUT/'chainsaw_inventario.png')
    # Animación aislada con la textura de cadena real y el paso de dientes de ambos JSON.
    frames=[];running=load('chainsaw_running');chain=Image.open(A/'textures/item/gun/chainsaw_chain_running.png')
    original=rz.tex_path
    for f in range(4):
        path=OUT/f'cadena_frame_{f}.png';chain.crop((0,f*16,16,f*16+16)).save(path)
        def resolve(ref,tex,frame_path=path):
            target=original(ref,tex)
            return frame_path if target.name=='chainsaw_chain_running.png' else target
        rz.tex_path=resolve
        frames.append(rz.render(prepared(running),110,20,24,size=900))
    rz.tex_path=original
    frames[0].save(OUT/'chainsaw_en_marcha.gif',save_all=True,append_images=frames[1:],duration=50,loop=0)
    materials=[material_sheet(f) for f in range(8)];materials[0].save(OUT/'fluidos_materiales.png')
    materials[0].save(OUT/'fluidos.gif',save_all=True,append_images=materials[1:],duration=250,loop=0)
    for name,cfg in FLUIDS.items():
        for kind,n,count in (('still',16,8),('flow',32,4)):
            source=Image.open(A/f'textures/block/{name}_{kind}.png');frames=[]
            for i in range(count):
                frame=source.crop((0,i*n,n,i*n+n));bg=Image.new('RGBA',(n,n),(80,78,70,255));bg.alpha_composite(frame)
                frames.append(bg.convert('RGB').resize((256,256),Image.Resampling.NEAREST))
            frames[0].save(OUT/f'{name}_{kind}.gif',save_all=True,append_images=frames[1:],duration=cfg[kind+'_time']*50,loop=0)
    # Lámina única de acceso rápido, conservando las imágenes grandes individuales.
    combined=Image.new('RGB',(1800,2200),(30,35,39))
    combined.paste(machines.resize((1600,900)),(100,0));combined.paste(preview.resize((1800,1150)),(0,900))
    draw=ImageDraw.Draw(combined);draw.text((25,2090),'Fluidos, cubos y materiales: fluidos_materiales.png · Animaciones: *.gif',font=font(26),fill='#dcd6b9')
    combined.save(OUT/'comparativa.png')
    print('Vistas en build/petroleo/: maquinas.png, motosierra.png, fluidos_materiales.png y GIF.')


if __name__=='__main__':main()
