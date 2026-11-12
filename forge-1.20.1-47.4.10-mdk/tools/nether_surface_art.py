"""Escoria y azufre originales de 32x32. Solo arte; no toca otros bloques del Nether.
python tools/nether_surface_art.py --variant A|B [--block compacted_slag|sulfur_crust]
python tools/preview_nether_surface_art.py
"""
import argparse,hashlib,json,math,random,shutil
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw

ROOT=Path(__file__).resolve().parent.parent
A=ROOT/'src/main/resources/assets/tecnocinetico'
OUT=ROOT/'build/nether_bloques'
IDS=('compacted_slag','sulfur_crust')
WEIGHTS=(4,3,2,2)


def save(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes((json.dumps(data,ensure_ascii=False,indent=2)+'\n').encode('utf-8'))


def noise(seed,n):
    """Campo periódico interpolado; píxeles agrupados, sin bordes de tesela dibujados."""
    r=random.Random(seed);g=np.array([[r.random() for _ in range(n)] for _ in range(n)])
    result=np.zeros((32,32))
    for y in range(32):
        for x in range(32):
            xx,yy=x*n/32,y*n/32;ix,iy=int(xx),int(yy);fx,fy=xx-ix,yy-iy
            fx=fx*fx*(3-2*fx);fy=fy*fy*(3-2*fy)
            result[y,x]=(g[iy%n,ix%n]*(1-fx)+g[iy%n,(ix+1)%n]*fx)*(1-fy)+(g[(iy+1)%n,ix%n]*(1-fx)+g[(iy+1)%n,(ix+1)%n]*fx)*fy
    return result


def base(palette,seed):
    f=.65*noise(seed,5)+.35*noise(seed+1,13)
    f=(f-f.mean())/(f.std()+1e-6)
    r=random.Random(seed+2);im=Image.new('RGBA',(32,32))
    for y in range(32):
        for x in range(32):
            k=max(0,min(4,int(2+f[y,x]*.8)));d=r.choice((-2,-1,0,0,1,2))
            im.putpixel((x,y),tuple(max(0,min(255,c+d)) for c in palette[k])+(255,))
    return im


def slag(option,n):
    seed=8127+n*179+(option=='B')*319;r=random.Random(seed)
    palette=(((56,38,33),(66,44,37),(76,50,40),(87,58,45),(98,67,49)) if option=='A' else
             ((40,37,34),(49,43,37),(60,49,40),(72,56,42),(85,65,45)))
    im=base(palette,seed);d=ImageDraw.Draw(im)
    # Burbujas de gas colapsadas: contornos orgánicos, tamaños y separaciones distintos.
    for _ in range(18 if option=='A' else 13):
        cx,cy=r.randrange(1,31),r.randrange(1,31);rx=r.uniform(1,2.5);ry=r.uniform(.8,2.2)
        if option=='B':rx*=1.25;ry*=1.2
        points=[]
        for j in range(9):
            a=j*math.tau/9;k=r.uniform(.77,1.18)
            points.append((round(cx+math.cos(a)*rx*k),round(cy+math.sin(a)*ry*k)))
        d.polygon(points,fill=(43,31,28) if option=='A' else (31,29,26))
        d.line(points[4:8],fill=(100,64,43) if option=='A' else (81,61,42),width=1)
        if rx>1.6:d.point((cx,cy),fill=(32,26,24))
    # Pequeños restos metálicos mates, incrustados, no vetas de mena regulares.
    for _ in range(3 if option=='A' else 5):
        x,y=r.randrange(3,28),r.randrange(3,28);w=r.randrange(2,4)
        d.polygon([(x,y),(x+w,y-1),(x+w-1,y+1),(x+1,y+2)],fill=(89,86,71))
        d.line((x+1,y,x+w-1,y),fill=(122,113,88))
        d.point((x+1,y+2),fill=(48,42,34))
    # Calor residual localizado; la mayor parte de la superficie está apagada.
    if n!=2:
        x,y=r.randrange(7,23),r.randrange(7,23)
        for i in range(5 if option=='A' else 8):
            d.point((x,y),fill=(101,39,26));d.point((x,y+1),fill=(136,49,25) if i%3==0 else (111,40,24))
            x+=r.choice((-1,0,1));y+=r.choice((0,1))
    return im


def sulfur(option,n):
    seed=1981+n*227+(option=='B')*413;r=random.Random(seed)
    palette=(((119,119,48),(146,136,48),(173,153,56),(190,174,77),(205,192,104)) if option=='A' else
             ((122,128,60),(148,148,70),(173,167,85),(192,185,106),(211,204,136)))
    im=base(palette,seed);d=ImageDraw.Draw(im)
    # Surcos cortos fracturados: jamás se trazan filas/columnas ni marcos del bloque.
    for _ in range(4):
        x,y=r.randrange(2,29),r.randrange(2,29);points=[(x,y)]
        for j in range(r.randrange(4,9)):
            x=max(1,min(30,x+r.choice((-1,0,1))));y=max(1,min(30,y+r.choice((-1,0,1))))
            points.append((x,y))
        d.line(points,fill=(110,108,47) if option=='A' else (109,117,60))
    # Racimos cristalinos nacen en bolsas: no un reparto uniforme de puntitos amarillos.
    for _ in range(5):
        cx,cy=r.randrange(3,29),r.randrange(3,29)
        for j in range(r.randrange(3,7)):
            x,y=cx+r.randrange(-4,5),cy+r.randrange(-4,5)
            w=r.choice((1,2,2,3)) if option=='A' else r.choice((2,3,3,4))
            h=r.randrange(2,5)
            points=[(x,y),(x+w,y-1),(x+w+1,y+1),(x+1,y+h)]
            d.polygon(points,fill=(179,157,61) if option=='A' else (178,178,94))
            d.polygon([(x,y),(x+w,y-1),(x+1,y+h-1)],fill=(219,203,113) if option=='A' else (228,219,151))
            d.line([(x+1,y+h),(x+w+1,y+1)],fill=(133,129,48) if option=='A' else (131,145,72))
    for _ in range(10):
        x,y=r.randrange(32),r.randrange(32)
        d.point((x,y),fill=(225,208,122) if option=='A' else (226,219,163))
    return im


def compatible_edges(images,seed):
    """Bordes comunes de grano fino, palindrómicos: compatibles entre variantes y giros de 90°.
    Su color medio coincide con el material; no dibuja líneas de contorno ni un marco oscuro.
    """
    samples=np.concatenate([np.array(im)[:,:,:3].reshape(-1,3) for im in images]);mean=samples.mean(0)
    r=random.Random(seed);edge=[]
    # Los poros y cristales también cruzan juntas; un borde solo de color medio produciría un marco liso.
    for i in range(16):
        sample=samples[r.randrange(len(samples))].astype(float)
        if i and r.random()<.28:sample=edge[-1]+r.randrange(-5,6)
        edge.append(sample)
    edge=np.array(edge);edge+=mean-edge.mean(0)
    for im in images:
        pixels=np.array(im,dtype=float)
        for y in range(32):
            for x in range(32):
                dist=min(x,y,31-x,31-y)
                if dist>=2:continue
                along=min(x,31-x) if min(y,31-y)<=min(x,31-x) else min(y,31-y)
                pixels[y,x,:3]=pixels[y,x,:3]*(dist/2)+edge[along]*(1-dist/2)
        im.paste(Image.fromarray(np.clip(np.rint(pixels),0,255).astype('uint8')))
    return images


def textures(name,option):
    return compatible_edges([(slag if name=='compacted_slag' else sulfur)(option,n) for n in range(4)],
                            1509 if name=='compacted_slag' else 6901)


def suffix(n):return '' if n==0 else f'_v{n}'


def model(name,n):
    return {'parent':'minecraft:block/cube_all','textures':{'all':f'tecnocinetico:block/{name}{suffix(n)}'}}


def states(name):
    return {'variants':{'':[dict({'model':f'tecnocinetico:block/{name}{suffix(n)}','weight':WEIGHTS[n]},**({'y':yaw} if yaw else {}))
                            for n in range(4) for yaw in (0,90,180,270)]}}


def snapshot():
    if (OUT/'actual').exists():return
    for name in IDS:
        for folder,ext in (('textures/block','png'),('models/block','json'),('blockstates','json')):
            src=A/f'{folder}/{name}.{ext}';dst=OUT/'actual'/src.relative_to(A)
            dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dst)
    save(OUT/'hashes_antes.json',{str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in (ROOT/'src').rglob('*') if p.is_file()})


def install(option='A',block='all'):
    snapshot();files=[]
    for choice in ('A','B'):
        for name in IDS:
            images=textures(name,choice)
            for n,im in enumerate(images):
                dst=OUT/choice/f'textures/block/{name}{suffix(n)}.png';dst.parent.mkdir(parents=True,exist_ok=True);im.save(dst)
                save(OUT/choice/f'models/block/{name}{suffix(n)}.json',model(name,n))
                if choice==option and block in ('all',name):
                    target=A/f'textures/block/{name}{suffix(n)}.png';im.save(target);files.append(target.relative_to(ROOT).as_posix())
                    target=A/f'models/block/{name}{suffix(n)}.json';save(target,model(name,n));files.append(target.relative_to(ROOT).as_posix())
            save(OUT/choice/f'blockstates/{name}.json',states(name))
            if choice==option and block in ('all',name):
                target=A/f'blockstates/{name}.json';save(target,states(name));files.append(target.relative_to(ROOT).as_posix())
    save(OUT/'archivos.json',files+['tools/nether_surface_art.py','tools/preview_nether_surface_art.py','docs/HERRAMIENTAS.md'])
    print('Arte Nether instalado:',option,block,'(escoria/azufre; 4 variantes de 32x32 por bloque)')


def main():
    p=argparse.ArgumentParser();p.add_argument('--variant',choices=('A','B'),default='A');p.add_argument('--block',choices=('all',*IDS),default='all')
    args=p.parse_args();install(args.variant,args.block)


if __name__=='__main__':main()
