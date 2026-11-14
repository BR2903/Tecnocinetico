"""Tres estados sin lente, Plasma Ígneo, manos FOV 70 y boca marcada.
python tools/preview_thermal_lance.py
Geometría y UV reales; muestras con profundidad. No abre Minecraft.
"""
import copy,json,math
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw
import render_zbuffer as rz
import preview_petroleum as pp
from thermal_lance_gen import A,OUT,NAMES,MUZZLE,display_point
from preview_weapon_remodel import prepared
from preview_combustion_drill import euler,font,raster
from nether_remodel_gen import transform

LABELS=('Apagada','Disparando','Sobrecalentada')


def load(state):
    m=json.loads((A/f'models/item/{NAMES[state]}.json').read_text())
    # Reproduce el color Forge de la pieza importada sin tocar ninguna textura de producción.
    tint=('856D50','FFFFFF','FF936D')[state]
    core=np.array(Image.open(A/'textures/item/gun/forge_heart.png').convert('RGBA'))
    core[:,:,:3]=(core[:,:,:3].astype(float)*np.array([int(tint[i:i+2],16)/255 for i in (0,2,4)])).astype('uint8')
    path=OUT/f'core_estado_{state}.png';Image.fromarray(core).save(path)
    m['textures']['heart']='file:'+str(path.resolve())
    return m


def projected(m,p,yaw,pitch,scale,size):
    allp=np.array([q for e in m['elements'] for q in (e['from'],e['to'])]);center=(allp.min(0)+allp.max(0))/2
    x,y,z=np.array(p)-center;cy,sy,cp,sp=math.cos(math.radians(yaw)),math.sin(math.radians(yaw)),math.cos(math.radians(pitch)),math.sin(math.radians(pitch))
    x,z=x*cy-z*sy,x*sy+z*cy;y=y*cp-z*sp
    return (size/2+x*scale,size/2-y*scale)


def third_person(model,left=False):
    scene={'textures':{},'elements':[]}
    main='left_arm' if left else 'right_arm'
    for bone,f,s,col in [('head',[-4,-8,-4],[8,8,8],'aa8d70'),('body',[-4,0,-2],[8,12,4],'505c5c'),
                         ('right_arm',[-3,-2,-2],[4,12,4],'505c5c'),('left_arm',[-1,-2,-2],[4,12,4],'505c5c'),
                         ('right_leg',[-2,0,-2],[4,12,4],'343c3e'),('left_leg',[-2,0,-2],[4,12,4],'343c3e')]:
        pos={'head':(0,0,0),'body':(0,0,0),'right_arm':(-5,2,0),'left_arm':(5,2,0),'right_leg':(-1.9,12,0),'left_leg':(1.9,12,0)}[bone]
        rot=(0,0,0)
        if 'arm' in bone:rot=(-math.pi/2+.1,-.3,0) if bone==main and not left else (-math.pi/2+.1,.3,0) if bone==main else (-1.5,-.6 if left else .6,0)
        matrix=transform({'at':pos,'rot':rot})
        path=OUT/f'jugador_{col}.png'
        if not path.exists():Image.new('RGBA',(16,16),'#'+col).save(path)
        vertices={}
        for face in ('north','south','east','west','up','down'):
            pts=rz.corners([f[0],-f[1]-s[1],f[2]],[f[0]+s[0],-f[1],f[2]+s[2]],face);vv=[]
            for q in pts:
                v=matrix@np.array([q[0],-q[1],q[2],1]);vv.append([v[0],24-v[1],v[2]])
            vertices[face]=vv
        points=np.array([p for face in vertices.values() for p in face]);scene['elements'].append({'from':points.min(0).tolist(),'to':points.max(0).tolist(),'preview_vertices':vertices,
            'faces':{f:{'texture':'file:'+str(path.resolve()),'uv':[0,0,16,16]} for f in vertices}})
    # Geometría de ItemInHandLayer verificada en el JAR local de Forge 1.20.1.
    ctx='thirdperson_'+('lefthand' if left else 'righthand');disp=model['display'][ctx]
    arm=transform({'at':(5 if left else -5,2,0),'rot':(-math.pi/2+.1,.3 if left else -.3,0)})
    def convert(p):
        q=display_point(p,disp,left);q=euler(euler(q+[-1 if left else 1,2,-10],[0,180,0]),[-90,0,0]);v=arm@np.array([*q,1])
        return [float(v[0]),24-float(v[1]),float(v[2])]
    for e in prepared(model)['elements']:
        n=copy.deepcopy(e)
        n['preview_vertices']={f:[convert(p) for p in points] for f,points in e['preview_vertices'].items()}
        for f,fd in n['faces'].items():
            ref=fd['texture']
            while ref.startswith('#'):ref=model['textures'][ref[1:]]
            fd['texture']=ref
        pts=np.array([p for face in n['preview_vertices'].values() for p in face]);n['from']=pts.min(0).tolist();n['to']=pts.max(0).tolist();scene['elements'].append(n)
    return scene,convert(MUZZLE)


def main():
    OUT.mkdir(parents=True,exist_ok=True);pp.OUT=OUT;original=rz.tex_path
    rz.tex_path=lambda ref,tex:Path(ref[5:]) if ref.startswith('file:') else Path(tex[ref[1:]][5:]) if ref.startswith('#') and tex[ref[1:]].startswith('file:') else original(ref,tex)
    models=[load(i) for i in range(3)]
    sheet=Image.new('RGB',(2700,750),(28,33,35));draw=ImageDraw.Draw(sheet)
    hands=Image.new('RGB',(1920,1740),(28,33,35));hd=ImageDraw.Draw(hands)
    for state,model in enumerate(models):
        m=prepared(model);im=rz.render(m,-110,18,23,size=1000);im.save(OUT/f'{NAMES[state]}_3cuartos.png')
        x,y=state*900,0;sheet.paste(im.resize((900,900)).crop((0,140,900,820)),(x,y+45))
        draw.text((x+22,y+12),f'{state} · '+LABELS[state],font=font(25),fill='#e2d9bd')
        rz.render(m,-110,12,23,size=1000).save(OUT/f'{NAMES[state]}_lado_nucleo.png')
        rz.render(m,110,12,23,size=1000).save(OUT/f'{NAMES[state]}_lado_intercambiador.png')
        rz.render(m,180,0,30,size=1000).save(OUT/f'{NAMES[state]}_boca.png')
        for col,left in enumerate((False,True)):
            im=pp.hand(model,left);name='izquierda' if left else 'derecha';im.save(OUT/f'{NAMES[state]}_mano_{name}.png')
            p=display_point(MUZZLE,model['display']['firstperson_'+('lefthand' if left else 'righthand')],left)/16+np.array([-.5 if left else .5,-.5,-.66])
            focal=270/math.tan(math.radians(70)/2);px=480+p[0]*focal/-p[2];py=270-p[1]*focal/-p[2]
            marked=im.copy();d=ImageDraw.Draw(marked);d.ellipse((px-5,py-5,px+5,py+5),outline='#89e29d',width=2)
            d.line((px,py,480,270),fill='#dbb45b',width=1);d.text((15,500),'Marcador de boca y línea de alineación · solo en esta vista',font=font(17),fill='#e4cf9b')
            marked.save(OUT/f'{NAMES[state]}_alineacion_{name}.png')
            hands.paste(im,(col*960,state*580+35));hd.text((col*960+16,state*580+7),LABELS[state]+' · '+name,font=font(20),fill='#e3d9bd')
    sheet.save(OUT/'estados.png');hands.save(OUT/'primera_persona.png')
    # Acercamiento para leer el núcleo real y la rejilla desde el lado izquierdo.
    view=rz.render(prepared(models[1]),-100,10,33,size=1300);view.save(OUT/'nucleo_encendido.png')
    thirds=Image.new('RGB',(1800,1000),(28,33,35));d=ImageDraw.Draw(thirds)
    for i,left in enumerate((False,True)):
        m,tip=third_person(models[1],left);im=rz.render(m,205 if not left else 155,12,18,size=1000)
        q=projected(m,tip,205 if not left else 155,12,18,1000);di=ImageDraw.Draw(im);di.ellipse((q[0]-5,q[1]-5,q[0]+5,q[1]+5),outline='#89e29d',width=2)
        name='izquierda' if left else 'derecha';im.save(OUT/f'tercera_persona_{name}.png')
        thirds.paste(im.resize((900,900)),(i*900,65));d.text((i*900+20,20),'Dos manos · principal '+name,font=font(24),fill='#e4d9bd')
    thirds.save(OUT/'tercera_persona.png')
    # Inventario según gui_fit, transformado sin aproximar el atlas ni sustituirlo por icono.
    m=prepared(models[0]);gui=models[0]['display']['gui']
    for e in m['elements']:
        e['preview_vertices']={f:[(euler((np.array(p)-8)*np.array(gui['scale']),gui['rotation'])+np.array(gui['translation'])).tolist() for p in points] for f,points in e['preview_vertices'].items()}
        p=np.array([v for face in e['preview_vertices'].values() for v in face]);e['from']=p.min(0).tolist();e['to']=p.max(0).tolist()
    rz.render(m,0,0,36,size=700).save(OUT/'inventario.png')
    plasma_previews()
    # Resumen de tres estados: no es animación del arma ni un predicado oscilante.
    frames=[Image.open(OUT/f'{n}_3cuartos.png').resize((700,700)) for n in NAMES]
    frames[0].save(OUT/'estados.gif',save_all=True,append_images=frames[1:],duration=1200,loop=0)
    print('Vistas: build/lanza/estados.png, primera_persona.png, tercera_persona.png, plasma.png y GIF de fluido.')


def plasma_previews():
    from igneous_plasma_art import STILL_TIME,FLOW_TIME
    still=Image.open(A/'textures/block/igneous_plasma_still.png');flow=Image.open(A/'textures/block/igneous_plasma_flow.png')
    frames=[]
    for f in range(8):
        tile=still.crop((0,f*16,16,f*16+16));surface=Image.new('RGBA',(80,80))
        for y in range(5):
            for x in range(5):surface.paste(tile,(x*16,y*16))
        frames.append(surface.convert('RGB').resize((480,480),Image.Resampling.NEAREST))
    frames[0].save(OUT/'igneous_plasma_still.gif',save_all=True,append_images=frames[1:],duration=STILL_TIME*50,loop=0)
    flow_frames=[flow.crop((0,f*32,32,f*32+32)).resize((320,320),Image.Resampling.NEAREST).convert('RGB') for f in range(4)]
    flow_frames[0].save(OUT/'igneous_plasma_flow.gif',save_all=True,append_images=flow_frames[1:],duration=FLOW_TIME*50,loop=0)
    sheet=Image.new('RGB',(1300,650),(28,33,35));d=ImageDraw.Draw(sheet)
    sheet.paste(frames[0],(20,70));d.text((20,18),'Plasma Ígneo · superficie 5 × 5',font=font(25),fill='#e8d4ac')
    for i,name in enumerate(('igneous_plasma_bucket','plasma_cell')):
        im=Image.open(A/f'textures/item/{name}.png').resize((240,240),Image.Resampling.NEAREST)
        sheet.paste(im,(585+i*345,165),im);d.text((555+i*345,430),name,font=font(21),fill='#e8d4ac')
    sheet.save(OUT/'plasma.png')


if __name__=='__main__':main()
