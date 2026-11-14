"""Arte original de rayo/impacto de la Lanza Térmica. Solo PNG y vistas previas.
python tools/thermal_lance_fx.py
No modifica el arma, la boca, registros, Java ni JSON de tipos de partícula.
"""
import json,math,random
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw
from preview_combustion_drill import font

ROOT=Path(__file__).resolve().parent.parent
A=ROOT/'src/main/resources/assets/tecnocinetico'
OUT=ROOT/'build/lanza_fx'
NAMES=('heat_spark','molten_drop','heat_flash','heat_haze')
LABELS=('Chispa de plasma','Gota fundida','Destello de impacto','Calima / hollín')


def clamp(v):return max(0,min(1,v))


def blend(a,b,t):return tuple(round(x*(1-t)+y*t) for x,y in zip(a,b))


def beam():
    """U = anchura, V = longitud y desplazamiento. RGB premultiplicado, listo para ONE/ONE."""
    im=Image.new('RGBA',(32,128))
    for y in range(128):
        phase=math.tau*y/127
        centre=15.5+.85*math.sin(phase*3)+.3*math.sin(phase*7+.8)
        width=1.25+.18*math.sin(phase*2+.4)
        halo=5.7+.9*math.sin(phase*3+1.1)
        for x in range(2,30):
            d=abs(x-centre)
            core=math.exp(-(max(0,d-.55)/width)**4)
            inner=.82*math.exp(-(d/3.2)**2)
            sheath=.34*math.exp(-(d/halo)**2)
            # Filamentos desplazados: pequeñas ondulaciones que viajan al desplazar V.
            ribbon=math.exp(-((d-(4.8+1.0*math.sin(phase*4))) / .65)**2)*.16
            strength=clamp(max(core,inner,sheath+ribbon))
            if strength<.012:continue
            hot=clamp(core+.28*math.exp(-(d/3.8)**2))
            warm=blend((222,38,3),(255,147,18),clamp(1-d/7))
            color=blend(warm,(255,254,202),hot)
            alpha=round(255*strength)
            im.putpixel((x,y),tuple(min(alpha,round(c*strength)) for c in color)+(alpha,))
    # Cierre exacto de la repetición V; ambos bordes U permanecen negros y transparentes.
    for x in range(32):im.putpixel((x,127),im.getpixel((x,0)))
    return im


def particle(name,frame):
    im=Image.new('RGBA',(16,16));rng=random.Random(114)
    noise=[[rng.uniform(.84,1.08) for _ in range(16)] for _ in range(16)]
    for y in range(1,15):
        for x in range(1,15):
            dx=x-7.5;dy=y-7.5
            if name=='heat_spark':
                width=(1.02,1.1,.88,.68)[frame];length=(4.8,4.3,3.4,2.5)[frame]
                bent=dx+.12*dy+.15*math.sin(dy*.8)
                shape=math.exp(-((bent/width)**2+(dy/length)**2)*1.4)
                halo=.24*math.exp(-((bent/2)**2+(dy/(length+1))**2)*1.4)
                energy=max(shape,halo)*(1,.84,.51,.16)[frame]
                base=((255,213,67),(255,167,34),(237,88,11),(141,38,7))[frame]
                color=blend(base,(255,255,204),shape*(1,.65,.08,0)[frame])
            elif name=='molten_drop':
                # Gota con bulbo inferior y cola afilada, nunca una llama vanilla.
                width=(2.55,2.65,2.25,1.8)[frame];height=(3.4,3.5,3,2.4)[frame]
                bulb=math.sqrt((dx/width)**2+((dy-.9)/height)**2)
                body=clamp((1-bulb)*3)
                tail=clamp((1-abs(dx)/max(.25,(dy+5.5)*.32))*2) if -5.5<dy<.9 else 0
                energy=max(body,tail*.83)*(1,.93,.7,.3)[frame]
                base=((255,133,18),(248,91,10),(178,44,8),(79,21,10))[frame]
                spec=math.exp(-((dx+.65)**2/.7+(dy-.5)**2/2))*(1,.55,.12,0)[frame]
                color=blend(base,(255,240,142),spec)
            elif name=='heat_flash':
                r=math.sqrt(dx*dx+dy*dy);a=math.atan2(dy,dx)
                radius=(1.9,3.25,4.75,5.9)[frame]
                uneven=radius*(1+.055*math.sin(a*3+.6)+.035*math.cos(a*5))
                ring=math.exp(-((r-uneven)/(.75 if frame<2 else .65))**2)
                ring*=.65+.35*math.sin(a*3+.9)**2
                star=math.exp(-dx*dx/.28-dy*dy/(radius*radius*1.8))+math.exp(-dy*dy/.28-dx*dx/(radius*radius*1.8))
                centre=math.exp(-r*r/(1.7 if frame==0 else .9))
                energy=clamp(max(ring,star*(.9,.65,.22,.04)[frame],centre*(1,.45,.06,0)[frame]))*(1,.84,.46,.13)[frame]
                base=((255,217,86),(255,167,37),(243,101,17),(180,53,10))[frame]
                color=blend(base,(255,255,218),max(centre,ring*.5)*(1,.42,.02,0)[frame])
            else:
                # Tres lóbulos desiguales de hollín: alfa normal, sin una estrella o silueta repetida.
                scale=(.64,.81,.98,1.12)[frame]
                puffs=0
                for cx,cy,rx,ry in ((-1.8,.9,3.5,3.3),(1.5,-1.5,3.1,3.5),(1.1,1.8,3.3,2.6),(-2.3,-1.9,2.5,2.6)):
                    q=((dx-cx*scale)/(rx*scale))**2+((dy-cy*scale)/(ry*scale))**2
                    puffs=max(puffs,clamp(1-q)*.9)
                energy=puffs*(.64,.49,.29,.10)[frame]*noise[y][x]
                warm=clamp((dy+2)/7)*(.45,.18,.04,0)[frame]
                color=blend((65+frame*5,59+frame*4,53+frame*4),(155,87,42),warm)
            if energy<.025:continue
            im.putpixel((x,y),color+(round(255*clamp(energy)),))
    return im


def over(im,bg,size=None):
    if size:im=im.resize(size,Image.Resampling.NEAREST)
    canvas=Image.new('RGBA',im.size,bg);canvas.alpha_composite(im);return canvas.convert('RGB')


def additive(im,bg):
    # El RGB del rayo ya está premultiplicado: ONE,ONE, sin multiplicarlo por alfa otra vez.
    arr=np.array(im.convert('RGBA')).astype(float)
    return Image.fromarray(np.minimum(255,arr[:,:,:3]+np.array(bg)).astype('uint8'))


def ray_preview(texture,shift=0):
    sheet=Image.new('RGB',(1800,720),(27,31,34));d=ImageDraw.Draw(sheet)
    arr=np.roll(np.array(texture),shift,axis=0);tile=Image.fromarray(arr).rotate(90,expand=True)
    d.text((25,18),'Lanza Térmica · rayo de plasma propio · mezcla aditiva ONE / ONE',font=font(26),fill='#e9d8b5')
    d.text((25,57),'U = anchura · V = longitud / desplazamiento · original RGBA 32 × 128',font=font(19),fill='#aeb8b5')
    # Textura vertical, ampliada sin suavizar, al lado de su uso estirado.
    sheet.paste(over(texture,(38,42,44),(128,512)),(35,125))
    d.text((25,660),'Textura',font=font(19),fill='#c8c8b8')
    for i,(height,bg,label) in enumerate(((40,(24,27,30),'Estrecho / fondo oscuro'),(80,(39,34,29),'Grueso / roca oscura'),(64,(115,94,70),'Fondo claro'))):
        strip=Image.new('RGBA',(128*4,32))
        for j in range(4):strip.paste(tile,(128*j,0))
        strip=strip.resize((1490,height),Image.Resampling.BILINEAR)
        x,y=240,155+i*175
        d.rectangle((x,y-25,x+1490,y+110),fill=bg)
        sheet.paste(additive(strip,bg),(x,y+5))
        d.text((x,y-51),label+' · 4 períodos a lo largo',font=font(18),fill='#ccbda6')
    return sheet


def previews(ray,particles):
    sheet=Image.new('RGB',(1460,1120),(27,31,34));d=ImageDraw.Draw(sheet)
    d.text((20,15),'Partículas propias · fotogramas originales 16 × 16 · ampliación sin suavizado',font=font(25),fill='#e6d5b3')
    for row,(name,label) in enumerate(zip(NAMES,LABELS)):
        y=70+row*255;d.text((20,y+4),label,font=font(20),fill='#d6cdb7')
        for f in range(4):
            im=particles[name][f].resize((176,176),Image.Resampling.NEAREST)
            # Dos fondos para leer alfa y hollín oscuro, especialmente la última fase.
            canvas=Image.new('RGBA',(256,192),(35,40,43,255));dd=ImageDraw.Draw(canvas)
            dd.rectangle((128,0,256,192),fill=(137,116,91,255));canvas.alpha_composite(im,(40,8))
            x=310+f*280;sheet.paste(canvas.convert('RGB'),(x,y+8))
            d.text((x,y+208),f'{name}_{f}',font=font(17),fill='#c8c6b4')
        frames=[over(im,(44,48,51),(256,256)) for im in particles[name]]
        frames[0].save(OUT/f'{name}.gif',save_all=True,append_images=frames[1:],duration=110,loop=0)
    sheet.save(OUT/'particulas.png')
    ray_preview(ray).save(OUT/'rayo.png')
    frames=[ray_preview(ray,f*16).resize((1200,480)) for f in range(8)]
    frames[0].save(OUT/'rayo_flujo.gif',save_all=True,append_images=frames[1:],duration=70,loop=0)
    combined=Image.new('RGB',(1800,2100),(27,31,34));combined.paste(ray_preview(ray),(0,0))
    combined.paste(sheet.resize((1800,1380)),(0,720));combined.save(OUT/'comparativa.png')


def main():
    OUT.mkdir(parents=True,exist_ok=True);(A/'textures/effect').mkdir(parents=True,exist_ok=True);(A/'textures/particle').mkdir(parents=True,exist_ok=True)
    ray=beam();ray.save(A/'textures/effect/thermal_beam.png');particles={}
    files=['src/main/resources/assets/tecnocinetico/textures/effect/thermal_beam.png']
    for name in NAMES:
        particles[name]=[]
        for f in range(4):
            im=particle(name,f);im.save(A/f'textures/particle/{name}_{f}.png');particles[name].append(im)
            files.append(f'src/main/resources/assets/tecnocinetico/textures/particle/{name}_{f}.png')
    previews(ray,particles)
    contract={'beam':{'texture':'tecnocinetico:textures/effect/thermal_beam.png','size':[32,128],
        'uv':'U transversal (0..1), V longitudinal. Repetir V y desplazarlo con el tiempo; no requiere .mcmeta.',
        'blend':'RGB premultiplicado + alfa de cobertura; recomendado ONE,ONE (aditivo). SRC_ALPHA,ONE también funciona, pero reduce el halo al multiplicar cobertura dos veces.',
        'sampling':'U clamp, V repeat. Filtrado lineal. V=0 y V=1 idénticos; bordes U completamente negros/transparentes.',
        'light':'Dibujar sin iluminación ambiental (fullbright), color de vértice blanco. La textura no incluye tapas ni impacto.'},
        'particles':{n:{'files':[f'textures/particle/{n}_{f}.png' for f in range(4)],
                        'sprite_ids':[f'tecnocinetico:{n}_{f}' for f in range(4)],
                        'format':'RGBA recto, 16x16 por fotograma, borde transparente',
                        'render':'Alfa normal, luz ambiental' if n=='heat_haze' else 'Alfa normal o aditivo; fullbright recomendado',
                        'sequence':'0 → 1 → 2 → 3; últimos fotogramas ya incluyen enfriamiento/desvanecimiento'} for n in NAMES},
        'haze':'La calima oscura necesita alfa normal (SRC_ALPHA, ONE_MINUS_SRC_ALPHA); la mezcla aditiva no puede oscurecer el fondo.',
        'motion':'Movimiento, orientación, gravedad, duración y emisión a cargo de Claude. La chispa tiene eje largo vertical; la gota tiene cola arriba y bulbo abajo. El flash crece dentro de la textura: mantener el quad estable si se quiere esa expansión exacta.',
        'registration':'No se han creado JSON de tipos de partícula, clases, registros ni cambios al rayo. Usar los sprite_ids en las listas textures del atlas de partículas.',
        'reload':'Se mantiene el arma actual; no se añade cartucho separado. No cambian modelos, combustible ni boca.'}
    (OUT/'integracion.json').write_text(json.dumps(contract,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    files+=['tools/thermal_lance_fx.py','docs/HERRAMIENTAS.md']
    (OUT/'archivos.json').write_text(json.dumps(files,indent=2)+'\n',encoding='utf-8')
    # Comprobar los riesgos visuales que motivaron el encargo: sprite blanco completo y cortes de atlas.
    arr=np.array(ray);assert np.array_equal(arr[0],arr[-1]);assert not arr[:,:2].any() and not arr[:,-2:].any()
    assert all(np.max(arr[y,:,3])>=250 for y in range(128))
    for sprites in particles.values():
        assert len({im.tobytes() for im in sprites})==4
        for im in sprites:
            a=np.array(im);assert im.size==(16,16) and np.any(a[:,:,3]>0)
            assert not a[0].any() and not a[-1].any() and not a[:,0].any() and not a[:,-1].any()
        assert np.array(sprites[-1])[:,:,3].sum()<np.array(sprites[0])[:,:,3].sum()
    (OUT/'validacion.json').write_text(json.dumps({'beam_size':[32,128],'particles':16,'particle_size':[16,16],
        'beam_V_seam':'exact','beam_edges':'black + alpha zero','continuous_bright_core':True,'distinct_frames_and_fading':True},indent=2)+'\n',encoding='utf-8')
    print('Rayo 32x128 y 16 partículas originales 16x16. Vistas: build/lanza_fx/comparativa.png y rayo_flujo.gif.')


if __name__=='__main__':main()
