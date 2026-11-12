"""Vistas con textura/profundidad reales y proyección de mano vanilla en reposo, FOV 70.
python tools/preview_combustion_drill.py
No abre Minecraft. La vista de mano no simula balanceo, equipamiento ni extensiones Java.
"""
import json
import math
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw,ImageFont
from preview_weapon_remodel import prepared
from render_zbuffer import render,tex_path,SHADE

ROOT=Path(__file__).resolve().parent.parent
A=ROOT/'src/main/resources/assets/tecnocinetico'
OUT=ROOT/'build/combustion_drill'

def font(size=20):
    try:return ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',size)
    except OSError:return ImageFont.load_default()

def euler(p,angles):
    x,y,z=p;rx,ry,rz=map(math.radians,angles)
    x,y=x*math.cos(rz)-y*math.sin(rz),x*math.sin(rz)+y*math.cos(rz)
    x,z=x*math.cos(ry)+z*math.sin(ry),-x*math.sin(ry)+z*math.cos(ry)
    y,z=y*math.cos(rx)-z*math.sin(rx),y*math.sin(rx)+z*math.cos(rx)
    return np.array([x,y,z])

def raster(model,context):
    perspective=context.startswith('firstperson');w,h=(960,540) if perspective else (512,512)
    canvas=np.zeros((h,w,3))+np.array([.11,.13,.15]);depth=np.full((h,w),-1e9)
    tex=np.array(Image.open(tex_path(model['textures']['0'],model['textures'])).convert('RGBA'))/255
    th,tw=tex.shape[:2];disp=model['display'][context];left=context.endswith('lefthand')
    scale=np.array(disp.get('scale',[1,1,1]));translation=np.array(disp.get('translation',[0,0,0]),dtype=float)
    angles=list(disp.get('rotation',[0,0,0]))
    if left:translation[0]*=-1;angles[1]*=-1;angles[2]*=-1
    focal=h/2/math.tan(math.radians(70)/2)
    def project(p):
        q=euler((np.array(p)-8)*scale,angles)
        if perspective:
            q=q/16+translation/16+np.array([-.56 if left else .56,-.52,-.72]);dist=-q[2]
            return np.array([w/2+q[0]*focal/dist,h/2-q[1]*focal/dist,1/dist])
        q+=translation;return np.array([w/2+q[0]*24,h/2-q[1]*24,q[2]])
    for e in prepared(model)['elements']:
        for face,fd in e['faces'].items():
            points=np.array([project(v) for v in e['preview_vertices'][face]])
            u0,v0,u1,v1=fd['uv'];uv=np.array([(u0,v0),(u1,v0),(u1,v1),(u0,v1)])
            for indices in ((0,1,2),(0,2,3)):
                p=points[list(indices)];t=uv[list(indices)]
                minx=max(0,int(p[:,0].min()));maxx=min(w-1,int(p[:,0].max())+1)
                miny=max(0,int(p[:,1].min()));maxy=min(h-1,int(p[:,1].max())+1)
                if minx>maxx or miny>maxy:continue
                (x1,y1,z1),(x2,y2,z2),(x3,y3,z3)=p
                den=(y2-y3)*(x1-x3)+(x3-x2)*(y1-y3)
                if abs(den)<1e-10:continue
                x,y=np.meshgrid(np.arange(minx,maxx+1)+.5,np.arange(miny,maxy+1)+.5)
                a=((y2-y3)*(x-x3)+(x3-x2)*(y-y3))/den
                b=((y3-y1)*(x-x3)+(x1-x3)*(y-y3))/den;c=1-a-b
                inside=(a>=0)&(b>=0)&(c>=0);z=a*z1+b*z2+c*z3
                if perspective:
                    uu=(a*t[0,0]*z1+b*t[1,0]*z2+c*t[2,0]*z3)/z
                    vv=(a*t[0,1]*z1+b*t[1,1]*z2+c*t[2,1]*z3)/z
                else:uu=a*t[0,0]+b*t[1,0]+c*t[2,0];vv=a*t[0,1]+b*t[1,1]+c*t[2,1]
                tx=np.clip((uu/16*tw).astype(int),0,tw-1);ty=np.clip((vv/16*th).astype(int),0,th-1)
                color=tex[ty,tx];old=depth[miny:maxy+1,minx:maxx+1]
                valid=inside&(color[:,:,3]>.5)&(z>old)
                old[valid]=z[valid];canvas[miny:maxy+1,minx:maxx+1][valid]=color[:,:,:3][valid]*max(.7,SHADE[face])
    result=Image.fromarray((np.clip(canvas,0,1)*255).astype('uint8'));d=ImageDraw.Draw(result)
    if perspective:
        d.line((w//2-8,h//2,w//2+8,h//2),fill='#8c9a9e');d.line((w//2,h//2-8,w//2,h//2+8),fill='#8c9a9e')
        d.text((18,14),'Mano vanilla en reposo · FOV 70 · '+('izquierda' if left else 'derecha'),font=font(18),fill='#dbe0df')
    else:d.rectangle((64,64,448,448),outline='#778184',width=2)
    return result

def main():
    OUT.mkdir(parents=True,exist_ok=True);frames=[];models=[]
    for phase,name in enumerate(('combustion_drill','combustion_drill_spin')):
        m=json.loads((A/f'models/item/{name}.json').read_text(encoding='utf-8'));models.append(m)
        im=render(prepared(m),110,18,22,size=900);im.save(OUT/f'three_quarter_{phase}.png');frames.append(im)
        render(prepared(m),-105,18,22,size=900).save(OUT/f'other_side_{phase}.png')
        render(prepared(m),180,0,22,size=900).save(OUT/f'bit_front_{phase}.png')
        raster(m,'firstperson_righthand').save(OUT/f'firstperson_{phase}.png')
    raster(models[0],'firstperson_lefthand').save(OUT/'firstperson_left.png')
    raster(models[0],'gui').save(OUT/'inventory.png')
    Image.open(OUT/'firstperson_0.png').save(OUT/'firstperson.png')
    frames[0].save(OUT/'three_quarter.png')
    # Alternancia exacta de dos ticks a 20 TPS: 100 ms por fotograma.
    frames[0].save(OUT/'spin.gif',save_all=True,append_images=[frames[1]],duration=[100,100],loop=0)
    sheet=Image.new('RGB',(1440,1180),(25,30,34));draw=ImageDraw.Draw(sheet)
    for i,im in enumerate(frames):
        sheet.paste(im.resize((700,700)),(i*720,35));draw.text((i*720+20,10),f'Broca {i*45}°',font=font(),fill='#e6ded1')
    for i in range(2):
        fp=Image.open(OUT/f'firstperson_{i}.png').resize((700,394));sheet.paste(fp,(i*720,750))
    sheet.save(OUT/'comparativa.png')
    parts=Image.new('RGB',(640,350),(30,35,40));d=ImageDraw.Draw(parts)
    for i,(name,label) in enumerate((('drill_head','Cabezal de Perforación'),('combustion_engine','Motor de Combustión'))):
        im=Image.open(A/f'textures/item/{name}.png').resize((224,224),Image.Resampling.NEAREST)
        parts.paste(im,(i*320+48,55),im);d.text((i*320+28,300),label,font=font(19),fill='#e3ded2')
    parts.save(OUT/'components.png');print('Vistas: build/combustion_drill/comparativa.png y components.png')

if __name__=='__main__':main()
