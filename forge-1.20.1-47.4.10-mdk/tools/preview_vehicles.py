"""Montaje real por PART + rotación de blockstates; imágenes con profundidad y UV de producción.
python tools/preview_vehicles.py
"""
import copy
import json
import math
from PIL import Image, ImageDraw, ImageFont
from vehicles_gen import A, OUT, SPECS, FACES, EPS
from render_zbuffer import corners, render
from preview_combustion_drill import raster


def font(size):
    try:return ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',size)
    except OSError:return ImageFont.load_default()


def assembled(name,facing='north'):
    L,W,H=SPECS[name];state=json.loads((A/f'blockstates/{name}.json').read_text())['variants']
    els=[];textures={}
    for part in range(L*W*H):
        v=state[f'facing={facing},part={part}'];path=v['model'].split(':')[1]
        m=json.loads((A/f'models/{path}.json').read_text());textures=m['textures']
        l=part%L;w=(part//L)%W;h=part//(L*W)
        angle=-math.radians(v.get('y',0));c,s=math.cos(angle),math.sin(angle)
        for original in m['elements']:
            e=copy.deepcopy(original);verts={}
            for face in e['faces']:
                pts=[]
                for p in corners(e['from'],e['to'],face):
                    x,y,z=p[0]+16*w-8,p[1]+16*h-8,p[2]+16*l-8
                    pts.append([round(x*c+z*s+8,6),y+8,round(-x*s+z*c+8,6)])
                verts[face]=pts
            e['preview_vertices']=verts
            pts=[p for quad in verts.values() for p in quad]
            e['from']=[min(p[i] for p in pts) for i in range(3)]
            e['to']=[max(p[i] for p in pts) for i in range(3)]
            els.append(e)
    return {'textures':textures,'elements':els}


def picture(model,yaw=215,pitch=19,size=1000):
    points=[p for e in model['elements'] for f in e['faces'] for p in e.get('preview_vertices',{}).get(f,corners(e['from'],e['to'],f))]
    cy,sy=math.cos(math.radians(yaw)),math.sin(math.radians(yaw))
    cp,sp=math.cos(math.radians(pitch)),math.sin(math.radians(pitch))
    xy=[(p[0]*cy-p[2]*sy,p[1]*cp-(p[0]*sy+p[2]*cy)*sp) for p in points]
    width=max(p[0] for p in xy)-min(p[0] for p in xy);height=max(p[1] for p in xy)-min(p[1] for p in xy)
    return render(model,yaw,pitch,(size-110)/max(width,height),size)


def audit(name,model,collision):
    """Conservación de área exterior y coordenadas al recomponer todos los recortes."""
    from vehicles_gen import surfaces
    source=json.loads((OUT/f'{name}_source.json').read_text())['boxes']
    original=surfaces(source)
    def area(e,face):
        ax=FACES[face][0]
        return math.prod(e['to'][i]-e['from'][i] for i in range(3) if i!=ax)
    source_area=sum(area(e,e['face']) for e in original)
    rebuilt_area=sum(area(e,f) for e in model['elements'] for f in e['faces'])
    assert abs(source_area-rebuilt_area)<.02,(name,source_area,rebuilt_area)
    L,W,H=SPECS[name]
    for part in range(L*W*H):
        m=json.loads((A/f'models/block/{name}_part_{part}.json').read_text())
        b=collision[part]
        if b is None:assert not m['elements']
        for e in m['elements']:
            assert b is not None
            assert all(b[i]-EPS<=e['from'][i]<=e['to'][i]<=b[i+3]+EPS for i in range(3))
    return {'source_surface_area':round(source_area,5),'assembled_surface_area':round(rebuilt_area,5),'cells':L*W*H,'cut_faces_removed':True}


def main():
    OUT.mkdir(parents=True,exist_ok=True)
    integration=json.loads((OUT/'integracion.json').read_text());report={}
    sheet=Image.new('RGB',(1800,3*590),(35,40,41))
    labels=('Turismo · capó abierto y puerta arrancada','Camión · caja abierta y motor expuesto','Autobús · interior quemado y ventanillas rotas')
    inventory=Image.new('RGB',(900,300),(35,40,41))
    for row,(name,label) in enumerate(zip(SPECS,labels)):
        model=assembled(name);report[name]=audit(name,model,integration['vehicles'][name]['collision'])
        (OUT/f'{name}_assembled.json').write_text(json.dumps(model),encoding='utf-8')
        front=picture(model);front.save(OUT/f'{name}_tres_cuartos.png')
        side=picture(model,270,0);side.save(OUT/f'{name}_lateral.png')
        other=picture(model,35,16);other.save(OUT/f'{name}_trasera.png')
        for col,im in enumerate((front,side,other)):
            sheet.paste(im.resize((600,550),Image.Resampling.LANCZOS),(col*600,row*590+40))
        ImageDraw.Draw(sheet).text((18,row*590+8),label,font=font(23),fill='#e8e4d7')
        orient=Image.new('RGB',(1600,1600),(35,40,41))
        for i,facing in enumerate(('north','east','south','west')):
            im=picture(assembled(name,facing),215,23,size=800)
            ImageDraw.Draw(im).text((20,18),f'{name} · facing={facing}',font=font(23),fill='#e8e4d7')
            im.save(OUT/f'{name}_{facing}.png');orient.paste(im,(i%2*800,i//2*800))
        orient.save(OUT/f'{name}_orientaciones.png')
        item=json.loads((A/f'models/item/{name}.json').read_text())
        im=raster(item,'gui')
        im.save(OUT/f'{name}_inventario.png')
        inventory.paste(im.convert('RGB').resize((290,290),Image.Resampling.LANCZOS),(row*300,0))
        print(name,'montado y auditado en 4 orientaciones',flush=True)
    sheet.save(OUT/'comparativa.png');inventory.save(OUT/'inventario.png')
    (OUT/'auditoria.json').write_text(json.dumps(report,indent=2),encoding='utf-8')


if __name__=='__main__':main()
