"""Arena vitrificada, tercera ronda: arena original de 16x16 con fusión muy discreta.
python tools/vitrified_sand_art.py --variant A|B
No lee, copia ni remuestrea el PNG de Minecraft. La referencia vanilla solo la usa la vista previa.
"""
import argparse,hashlib,json,random,shutil
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parent.parent
A=ROOT/'src/main/resources/assets/tecnocinetico'
OUT=ROOT/'build/remodelar_bloques/arena3'
NAME='vitrified_sand'
# Paleta pintada para arena clara: bajo contraste y grano beige, sin tonos de roca o mena.
PALETTE=((210,188,140),(214,197,152),(219,208,164),(228,220,177),(232,229,190),(237,235,201))
COUNTS=(8,70,106,56,10,6)
WEIGHTS=(4,3,2,2)


def save(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes((json.dumps(data,ensure_ascii=False,indent=2)+'\n').encode('utf-8'))


def sand_base(n):
    r=random.Random(7871+n*307)
    # Histograma estable entre variantes para evitar parches claros/oscuros al cubrir el suelo.
    pixels=[(*color,255) for color,count in zip(PALETTE,COUNTS) for _ in range(count)]
    r.shuffle(pixels)
    # Unas pocas parejas de grano. Solo permuta píxeles originales; no crea manchas de baja frecuencia.
    for _ in range(24):
        x,y=r.randrange(15),r.randrange(16);i=y*16+x;j=i+1
        target=pixels[i];candidates=[k for k,c in enumerate(pixels) if c==target and k!=i]
        k=r.choice(candidates);pixels[j],pixels[k]=pixels[k],pixels[j]
    im=Image.new('RGBA',(16,16));im.putdata(pixels);return im


def texture(option='A',n=0):
    im=sand_base(n);r=random.Random(9719+n*193)
    x,y=r.randrange(3,12),r.randrange(3,12)
    # Gotita pequeña: vidrio aún del color de la arena, con un reflejo y una sombra suave.
    im.putpixel((x,y),(213,218,187,255))
    im.putpixel((x+1,y),(244,240,214,255))
    im.putpixel((x,y+1),(187,185,152,255))
    # Un grano tostado; nunca una mancha negra ni un borde de baldosa.
    bx,by=(x+6)%14+1,(y+7)%14+1
    im.putpixel((bx,by),(174,157,120,255))
    if option=='B':
        x2,y2=(x+7)%11+2,(y+5)%11+2
        im.putpixel((x2,y2),(206,215,181,255))
        im.putpixel((x2+1,y2),(240,239,211,255))
        im.putpixel((x2,y2+1),(190,191,155,255))
        im.putpixel((x2+1,y2+1),(216,222,190,255))
        # Pequeña costra translúcida y una fisura de dos granos, no una veta que cruce el bloque.
        im.putpixel(((x+3)%14+1,(y+8)%14+1),(232,231,200,255))
        im.putpixel(((x+9)%14+1,(y+4)%14+1),(180,165,127,255))
        im.putpixel(((x+10)%14+1,(y+5)%14+1),(194,180,139,255))
    return im


def suffix(n):return '' if n==0 else f'_v{n}'


def model(n):
    return {'parent':'minecraft:block/cube_all','textures':{'all':f'tecnocinetico:block/{NAME}{suffix(n)}'}}


def states():
    return {'variants':{'':[dict({'model':f'tecnocinetico:block/{NAME}{suffix(n)}','weight':WEIGHTS[n]},**({'y':yaw} if yaw else {}))
                            for n in range(4) for yaw in (0,90,180,270)]}}


def snapshot():
    if (OUT/'actual').exists():return
    for folder,ext in (('textures/block','png'),('models/block','json'),('blockstates','json')):
        p=A/f'{folder}/{NAME}.{ext}';dst=OUT/'actual'/p.relative_to(A)
        dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,dst)
    save(OUT/'antes_hashes.json',{str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in (ROOT/'src').rglob('*') if p.is_file()})


def install(option='A'):
    snapshot();files=[];report={}
    for choice in ('A','B'):
        for n in range(4):
            im=texture(choice,n);path=OUT/choice/f'textures/block/{NAME}{suffix(n)}.png'
            path.parent.mkdir(parents=True,exist_ok=True);im.save(path)
            save(OUT/choice/f'models/block/{NAME}{suffix(n)}.json',model(n))
            base=sand_base(n);count=sum(base.getpixel((x,y))!=im.getpixel((x,y)) for y in range(16) for x in range(16))
            report[f'{choice}_{n}']={'detail_pixels':count,'sand_unchanged_percent':round((256-count)/256*100,2)}
            if choice==option:
                target=A/f'textures/block/{NAME}{suffix(n)}.png';im.save(target);files.append(target.relative_to(ROOT).as_posix())
                target=A/f'models/block/{NAME}{suffix(n)}.json';save(target,model(n));files.append(target.relative_to(ROOT).as_posix())
        save(OUT/choice/f'blockstates/{NAME}.json',states())
    path=A/f'blockstates/{NAME}.json';save(path,states());files.append(path.relative_to(ROOT).as_posix())
    save(OUT/'detalle.json',report)
    save(OUT/'archivos.json',files+['tools/vitrified_sand_art.py','tools/preview_vitrified_sand.py','tools/world_block_round2.py','docs/HERRAMIENTAS.md'])
    print('Arena vitrificada instalada:',option,'(4 texturas originales de 16x16; sin tocar los otros bloques)')


def main():
    p=argparse.ArgumentParser();p.add_argument('--variant',choices=('A','B'),default='A');install(p.parse_args().variant)


if __name__=='__main__':main()
