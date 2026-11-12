"""Taladro de gasolina original y dos componentes planos. Solo arte, sin registros ni lógica.
python tools/combustion_drill_gen.py
La variante se deriva del JSON base: cuerpo, UV y transforms idénticos; solo gira la broca.
"""
import copy
import json
import math
import subprocess
import sys
from pathlib import Path
from PIL import Image,ImageDraw
from gunkit import el,model,flush

ROOT=Path(__file__).resolve().parent.parent
A=ROOT/'src/main/resources/assets/tecnocinetico'
OUT=ROOT/'build/combustion_drill'
AXIS=[8,12,0]
DISPLAY={
    'firstperson_righthand':{'rotation':[8,22,0],'translation':[-1,-.3,-3.25],'scale':[.85,.85,.85]},
    'firstperson_lefthand':{'rotation':[8,22,0],'translation':[-1,-.3,-3.25],'scale':[.85,.85,.85]},
    'thirdperson_righthand':{'rotation':[0,0,0],'translation':[0,-1.5,0],'scale':[.85,.85,.85]},
    'thirdperson_lefthand':{'rotation':[0,0,0],'translation':[0,-1.5,0],'scale':[.85,.85,.85]},
    'ground':{'rotation':[0,0,0],'translation':[0,-2,0],'scale':[.45,.45,.45]},
    'fixed':{'rotation':[0,90,0],'translation':[4.7,-2.75,0],'scale':[.5,.5,.5]},
}

def box(dst,x,y,z,w,h,d,mat='steel_dark',rot=None,front=None):
    dst.append(el([x,y,z],[x+w,y+h,z+d],mat,rot=rot,front=front))

def body():
    a=[]
    # Reductora frontal octogonal, unida al cigüeñal, sin cañón ni miras de arma.
    box(a,6.15,10.05,1.3,3.7,3.9,4.2,'steel')
    box(a,5.7,10.7,1.5,4.6,2.6,3.9,'steel')
    box(a,6.4,10.4,.6,3.2,3.2,.9,'graphite')
    box(a,6.8,10.8,.1,2.4,2.4,.85,'steel_hi')
    for x,y in ((6.3,10.4),(9.45,10.4),(6.3,13.25),(9.45,13.25)):
        box(a,x,y,1.17,.3,.3,.25,'steel_hi')
    # Cárter y cilindro de dos tiempos con aletas reales separadas.
    box(a,5.75,10.2,5.3,4.5,4.1,6.3,'graphite')
    box(a,6.05,13.8,5.8,3.9,3.2,4.7,'steel_dark')
    for i in range(6):
        box(a,5.55,14.05+i*.48,5.4,4.9,.2,5.5,'steel')
    box(a,6.05,16.98,5.8,3.9,.5,4.7,'steel_dark')
    # Bujía, aislador cerámico y cable que vuelve a la bobina.
    box(a,7.6,17.4,8,.8,.45,.8,'steel_hi')
    box(a,7.72,17.8,8.12,.56,1.05,.56,'ceramic')
    for y in (18,18.3):box(a,7.65,y,8.05,.7,.12,.7,'ceramic')
    box(a,7.78,18.82,8.18,.44,.4,.44,'rubber')
    box(a,8.15,18.87,8.2,2.55,.28,.32,'rubber')
    box(a,10.4,15.2,8.2,.32,3.9,.32,'rubber')
    box(a,9.95,14.4,7.65,1.3,1.25,1.4,'rubber')
    # Arrancador lateral con tapa y tirador de cuerda.
    box(a,4.9,10.75,6.2,1,3.2,4.6,'steel_dark')
    box(a,4.55,11.25,6.65,.55,2.2,3.7,'shell_red')
    for z in (7,7.9,8.8,9.7):box(a,4.4,11.65,z,.25,1.3,.26,'black')
    box(a,4.5,14,9.9,.35,1.1,.4,'leather')
    box(a,3.9,14.9,9.65,1.6,.45,.9,'rubber')
    # Silenciador protegido a la derecha; salida dirigida al costado.
    box(a,10.15,11.2,5.7,1.8,3.2,4.5,'rust')
    for z in (6.2,7.3,8.4,9.5):box(a,11.9,11.7,z,.22,2,.28,'steel_dark')
    box(a,11.1,11.65,10.1,1,1,1.6,'steel_dark')
    box(a,11.85,11.65,10.7,1.7,1,1,'steel_dark')
    # Boca hueca del escape; cuatro paredes, sin tapón metálico.
    for y,z,h,d in ((11.55,10.6,.18,1.2),(12.57,10.6,.18,1.2),(11.7,10.6,.9,.18),(11.7,11.62,.9,.18)):
        box(a,13.35,y,z,.7,h,d,'steel')
    # Depósito visible de gasolina, situado bajo el motor y delante de la mano.
    box(a,5.75,7.1,4.6,4.5,3.1,5.5,'shell_red')
    box(a,6.05,6.8,4.9,3.9,.6,4.9,'shell_red')
    for z in (5.15,9.15):
        box(a,5.6,7.05,z,.2,3.65,.5,'steel_dark')
        box(a,10.2,7.05,z,.2,3.65,.5,'steel_dark')
        box(a,5.65,6.9,z,4.7,.25,.5,'steel_dark')
    box(a,10.15,8.1,7,1,.9,1.1,'steel')
    box(a,11.05,8.15,7.1,.25,.8,.9,'rubber')
    box(a,5.42,8.3,6.4,.25,1.4,1.8,'hazard')
    # Manguito de gasolina conectado al cárter; no atraviesa el hueco de la empuñadura.
    box(a,5.15,9.4,9.2,.4,1.8,.4,'rubber')
    box(a,5.15,10.85,8.5,.4,.35,.95,'rubber')
    box(a,5.7,10.65,8.4,.4,.75,.65,'copper')
    # Asa transversal delantera: mano auxiliar separada del cilindro caliente.
    for x in (4.2,11.1):
        box(a,x,11.5,2.75,.65,5.2,.85,'steel_dark')
        box(a,x-.12,11.65,2.6,.9,1,1.15,'steel')
    box(a,4.2,16.45,2.65,7.55,.85,1,'steel')
    box(a,5.05,16.35,2.48,5.85,1.1,1.35,'rubber')
    for x in (5.3,6.4,7.5,8.6,9.7):box(a,x,16.3,2.43,.22,1.2,1.45,'graphite')
    # Agarre principal en las coordenadas del arsenal; gatillo protegido.
    grip={'axis':'x','angle':-22.5,'origin':[8,11,11]}
    box(a,7.15,7.7,10.5,1.7,4.15,2.1,'rubber',grip)
    for y in (8.1,9.05,10):box(a,7.08,y,10.42,1.84,.22,2.26,'graphite',grip)
    box(a,7,11.4,10.3,2,.75,2.55,'steel_dark')
    box(a,7.65,9.7,9.5,.7,1.2,.35,'shell_red')
    box(a,7.45,8.95,8.7,1.1,.3,2.1,'steel')
    box(a,7.45,9.1,8.7,1.1,1.35,.3,'steel')
    box(a,6.3,10.6,11.3,3.4,2.7,.65,'steel_dark')
    box(a,6.5,11.05,11.92,3,.7,.3,'hazard')
    return a

def bit():
    a=[]
    box(a,7.42,11.42,-11.95,1.16,1.16,12.8,'steel')
    # Dos vuelos opuestos, escalonados alrededor del alma de titanio.
    # Todas las cajas son axiales: el fotograma de giro conserva exactamente sus UV.
    for i in range(14):
        z=-.1-i*.86;r=1.75-.04*i;angle=math.radians(i*30)
        for side in (-1,1):
            x=math.cos(angle)*r*side;y=math.sin(angle)*r*side
            box(a,8+min(0,x)-.18,12+min(0,y)-.18,z-.95,abs(x)+.36,abs(y)+.36,1.15,'steel_hi')
            box(a,8+x-.24,12+y-.24,z-1.06,.48,.48,.42,'ceramic')
    # Nariz escalonada y dos labios de carburo; asimetría visible también en proyección frontal.
    box(a,7.35,11.35,-12.7,1.3,1.3,1.35,'steel_hi')
    box(a,7.6,11.6,-13.6,.8,.8,1,'steel')
    box(a,7.8,11.8,-14.3,.4,.4,.8,'ceramic')
    box(a,7.15,11.76,-12.95,.7,.48,.55,'ceramic')
    box(a,8.2,11.76,-12.95,.65,.48,.55,'ceramic')
    return a

def components():
    # Pixel art propio a 16x16: titanio frío/carburo y motor de dos tiempos rojo.
    im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
    d.polygon([(1,13),(3,10),(4,6),(8,3),(12,1),(14,1),(14,4),(11,9),(7,12),(4,14)],fill='#29333b')
    d.polygon([(3,12),(5,7),(9,4),(13,2),(12,6),(8,10),(5,13)],fill='#91a6b1')
    for line in ((4,10,7,11),(6,7,10,8),(9,4,12,5)):
        d.line(line,fill='#354751',width=2);d.line((line[0],line[1]-1,line[2],line[3]-1),fill='#d9e3df')
    d.point((13,2),fill='#eee1a9');d.point((11,6),fill='#d8c592');d.point((7,10),fill='#d8c592')
    im.save(A/'textures/item/drill_head.png')
    motor=Image.new('RGBA',(16,16));d=ImageDraw.Draw(motor)
    d.rectangle((4,5,11,13),fill='#292d2e');d.rectangle((5,4,10,9),fill='#68767b')
    for y in (4,6,8):d.line((3,y,12,y),fill='#bbc4bd');d.line((4,y+1,11,y+1),fill='#252e33')
    d.rectangle((7,1,8,3),fill='#ded9bb');d.point((7,0),fill='#a6b7b6')
    d.rectangle((2,10,11,14),fill='#7e2d22');d.line((3,10,10,10),fill='#c8643b')
    d.rectangle((1,8,3,12),fill='#20282d');d.rectangle((12,8,14,10),fill='#685c4b')
    d.line((14,6,14,9),fill='#b8b7a4');d.rectangle((5,12,8,14),fill='#30383a');d.point((6,13),fill='#bdb99e')
    motor.save(A/'textures/item/combustion_engine.png')
    for name in ('drill_head','combustion_engine'):
        (A/f'models/item/{name}.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':f'tecnocinetico:item/{name}'}},indent=2)+'\n',encoding='utf-8')

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    fixed=body();rotor=bit();count=len(fixed)
    model('combustion_drill',fixed+rotor,DISPLAY,[{'predicate':{'tecnocinetico:drill_spin':1},'model':'tecnocinetico:item/combustion_drill_spin'}],group='combustion_drill')
    flush()
    path=A/'models/item/combustion_drill.json';base=json.loads(path.read_text(encoding='utf-8'));spin=copy.deepcopy(base);spin.pop('overrides',None)
    for e in spin['elements'][count:]:e['rotation']={'axis':'z','angle':45,'origin':AXIS}
    (A/'models/item/combustion_drill_spin.json').write_text(json.dumps(spin,indent=2)+'\n',encoding='utf-8')
    subprocess.run([sys.executable,str(ROOT/'tools/gui_fit.py'),str(A),'y-90;z25','combustion_drill','combustion_drill_spin'],check=True)
    components()
    base=json.loads(path.read_text(encoding='utf-8'));spin=json.loads((A/'models/item/combustion_drill_spin.json').read_text(encoding='utf-8'))
    assert base['display']==spin['display']
    assert base['elements'][:count]==spin['elements'][:count]
    for a,b in zip(base['elements'][count:],spin['elements'][count:]):
        c=copy.deepcopy(b);c.pop('rotation');assert c==a
    for m in (base,spin):
        for e in m['elements']:
            assert all(-16<=n<=32 for k in ('from','to') for n in e[k])
            # Comprobar también los vértices tras girar el agarre o la broca, no solo sus cajas declaradas.
            from preview_weapon_remodel import rotated
            for x in (e['from'][0],e['to'][0]):
                for y in (e['from'][1],e['to'][1]):
                    for z in (e['from'][2],e['to'][2]):assert all(-16<=n<=32 for n in rotated([x,y,z],e.get('rotation')))
    manifest={'fixed_elements':count,'rotating_elements':len(rotor),'axis':AXIS,'direction':'-Z',
              'spin_degrees':45,'predicate':'tecnocinetico:drill_spin','values':[0,1],
              'same_body_uv_and_transforms':True,'texture_size':base['texture_size']}
    (OUT/'contract.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
    print(json.dumps(manifest))

if __name__=='__main__':main()
