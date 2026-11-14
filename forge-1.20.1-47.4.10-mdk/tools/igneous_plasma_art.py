"""Plasma Ígneo original: remolinos térmicos animados, cubo y célula plana.
python tools/igneous_plasma_art.py
thermal_lance_gen.py lo invoca automáticamente. No modifica otros fluidos ni el modelo del cubo.
"""
import json,math,random
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parent.parent
A=ROOT/'src/main/resources/assets/tecnocinetico'
STILL_TIME=3
FLOW_TIME=2


def generate():
    files=[]
    def record(path):files.append(str(path.relative_to(ROOT)).replace('\\','/'));return path
    rng=random.Random(74155);grids={n:[[rng.random() for _ in range(n)] for _ in range(n)] for n in (2,3,5,9)}
    def noise(n,x,y):
        x*=n;y*=n;ix,iy=math.floor(x),math.floor(y);fx,fy=x-ix,y-iy
        fx=fx*fx*(3-2*fx);fy=fy*fy*(3-2*fy);g=grids[n]
        a=g[iy%n][ix%n]*(1-fx)+g[iy%n][(ix+1)%n]*fx
        b=g[(iy+1)%n][ix%n]*(1-fx)+g[(iy+1)%n][(ix+1)%n]*fx
        return a*(1-fy)+b*fy
    for flowing,n,count,time in ((False,16,8,STILL_TIME),(True,32,4,FLOW_TIME)):
        im=Image.new('RGBA',(n,n*count));kind='flow' if flowing else 'still'
        for frame in range(count):
            phase=math.tau*frame/count
            for y in range(n):
                for x in range(n):
                    u=x/n;v=y/n-(frame/count if flowing else 0)
                    # Campos periódicos deformados: corrientes anchas y filamentos al rojo.
                    wx=u+.12*math.sin(math.tau*v+phase)+.08*(noise(3,u,v)-.5)
                    wy=v+.10*math.sin(math.tau*u-phase)+.08*(noise(2,u,v)-.5)
                    field=.60*noise(3,wx,wy)+.40*noise(5,wx,wy)
                    ridge=math.exp(-((field-.52)/.065)**2)
                    fleck=noise(9,wx+.03*math.sin(phase),wy)
                    heat=max(0,min(1,.15+.7*ridge+.2*fleck))
                    red=round(231+24*heat);green=round(62+175*heat);blue=round(9+105*heat**2)
                    im.putpixel((x,frame*n+y),(red,green,blue,255))
        path=A/f'textures/block/igneous_plasma_{kind}.png';im.save(record(path))
        record(Path(str(path)+'.mcmeta')).write_text(json.dumps({'animation':{'width':n,'height':n,'frametime':time,'interpolate':True}},indent=2)+'\n',encoding='utf-8')
    # Mantener exactamente la silueta y el metal de los otros cubos, recoloreando solo el líquido.
    bucket=Image.open(A/'textures/item/gasoline_bucket.png').convert('RGBA')
    for y in range(4,8):
        for x in range(2,14):
            r,g,b,a=bucket.getpixel((x,y))
            if a and r>150 and r>g and g>b*1.2:
                heat=max(0,min(1,(g-160)/95));bucket.putpixel((x,y),(255,round(116+121*heat),round(21+100*heat),a))
    bucket.save(record(A/'textures/item/igneous_plasma_bucket.png'))
    # Hermana de la célula cinética: carcasa y contactos intactos, ventana ahora de plasma naranja.
    cell=Image.open(A/'textures/item/kinetic_fuel_cell.png').convert('RGBA')
    for y in range(16):
        for x in range(16):
            r,g,b,a=cell.getpixel((x,y))
            if a and g-r>45 and b-r>35:
                heat=max(0,min(1,(g-80)/165));cell.putpixel((x,y),(round(245+10*heat),round(80+155*heat),round(10+115*heat),a))
    cell.save(record(A/'textures/item/plasma_cell.png'))
    record(A/'models/item/plasma_cell.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'tecnocinetico:item/plasma_cell'}},indent=2)+'\n',encoding='utf-8')
    print('Plasma: still 16x128, flow 32x128, remolinos sin costura, cubo y célula planos 16x16.')
    return files


if __name__=='__main__':generate()
