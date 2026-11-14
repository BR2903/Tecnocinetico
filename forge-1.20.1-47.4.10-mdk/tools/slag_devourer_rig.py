"""Devorador: siete segmentos y corona de ocho mandíbulas. Media escala; renderer 2x.
Y de Minecraft hacia abajo, suelo 24, frente -Z. Poses compartidas con la vista previa.
"""
import math
from nether_remodel_rigs import Rig

PALETTE={'armor':(65,68,61),'edge':(117,114,94),'slag':(86,55,37),'dark':(25,29,26),
         'sinew':(98,52,36),'copper':(151,89,43),'ceramic':(170,151,112),'carbide':(46,51,49),
         'ember':(239,94,22),'hot':(255,193,75),'hazard':(165,123,46)}
TAPER=(1,1.08,1,.92,.84,.73,.57)


def geometry():
    r=Rig()
    def box(p,m,f,s):r.box(p,m,f,s)
    # Mandíbula principal: túnel hueco, collar octogonal y núcleo de triturado al fondo.
    r.part('head',at=(0,13.2,-7));r.parts[-1]['scale']=(1.12,1,1)
    for i in range(8):
        a=i*math.pi/4
        ring=f'collar_{i}';r.part(ring,'head',(8.2*math.sin(a),-6.4*math.cos(a),-.2),(0,0,a))
        box(ring,'armor',(-2.7,-1.2,-2.3),(5.4,1.8,5.2))
        box(ring,'slag',(-2.35,-1.65,-1.7),(4.7,.6,3.8))
        box(ring,'edge',(-2.8,-1.25,-2.9),(5.6,.6,.7))
        box(ring,'copper',(-.65,-1.9,-.4),(1.3,.4,1.5))
        mount=f'mount_{i}';jaw=f'jaw_{i}'
        r.part(mount,'head',(8.0*math.sin(a),-6.1*math.cos(a),-2.8),(0,0,a))
        r.part(jaw,mount,rot=(.27,0,0))
        box(jaw,'dark',(-1.55,-.4,-1),(3.1,1.6,2))
        box(jaw,'edge',(-2.1,-.8,-4.0),(4.2,1.3,4.1))
        box(jaw,'armor',(-1.65,-1.3,-5.7),(3.3,1.6,3.5))
        box(jaw,'slag',(-1.3,-1.55,-5.3),(2.6,.4,3.2))
        box(jaw,'carbide',(-1.05,-.55,-7.1),(2.1,1.5,2))
        box(jaw,'edge',(-.65,.8,-7.1),(1.3,1.2,1))
        for x in (-1.35,.8):box(jaw,'ceramic',(x,.65,-5.9),(.55,1.0,1.3))
        box(jaw,'ember',(-.5,-1.63,-4.8),(1,.15,2.6))
        # Revestimiento de garganta: el anillo deja un conducto oscuro real, no una tapa plana.
        throat=f'throat_{i}';r.part(throat,'head',(3.5*math.sin(a),-3.5*math.cos(a),0),(0,0,a))
        box(throat,'dark',(-1.5,-.55,-4.8),(3,1.1,6.8))
        box(throat,'ember',(-1.15,-.7,-4.7),(2.3,.2,4.9))
        box(throat,'carbide',(-.5,.4,-3.5),(1,.8,1.8))
    box('head','slag',(-3,-3,1.8),(6,6,1.5))
    box('head','hot',(-1.8,-1.8,1.35),(3.6,3.6,.5))
    box('head','dark',(-.7,-2.5,1.0),(1.4,5,.4))
    box('head','dark',(-2.5,-.7,.95),(5,1.4,.4))
    for s in (-1,1):
        box('head','dark',(s*4.8-.9,-4.6,-3.0),(1.8,1.5,1.4))
        box('head','hot',(s*4.8-.6,-4.3,-3.22),(1.2,.45,.25))
        box('head','edge',(s*5.2-.5,-5.0,-2.9),(1,.4,2))
        box('head','dark',(s*3.2-.8,-1.0,2.2),(1.6,3,5.8))
        for z in (3,5.5):box('head','copper',(s*3.2-.95,-1.15,z),(1.9,3.3,.6))
    # Cada hull comparte UV pero se estrecha hacia la cola; garras con pivotes independientes.
    for i,taper in enumerate(TAPER):
        seg=f'segment_{i}';hull=f'hull_{i}'
        height=(16,15.7,15.9,16.2,16.6,17,17.4)[i]
        r.part(seg,at=(0,height,-2+5.75*i));r.part(hull,seg);r.parts[-1]['scale']=(taper,taper,1)
        box(hull,'dark',(-4,-3.5,-2.5),(8,7,5))
        box(hull,'sinew',(-4.35,-1.3,-2.2),(8.7,3.5,4.4))
        box(hull,'armor',(-4.3,2.3,-2.6),(8.6,1.4,5.2))
        box(hull,'hot',(-2.3,-3.7,-2.1),(4.6,.5,4.2))
        # Uniones luminosas, rodeadas por manguitos y costillas metálicas.
        for s in (-1,1):
            box(hull,'dark',(s*3.8-.8,-2.0,2.4),(1.6,4.3,1.4))
            box(hull,'ember',(s*3.8-.55,-1.6,2.5),(1.1,3.5,1.4))
            box(hull,'edge',(s*3.8-.85,-2.3,2.7),(1.7,.5,1))
            box(hull,'copper',(s*4.5-.25,.5,-2.0),(.5,.6,4))
            plate=f'plate_{i}_{"left" if s>0 else "right"}'
            r.part(plate,hull,(s*4.3,-3.8,0))
            start=-4.35 if s>0 else 0
            box(plate,'slag' if i%3==1 else 'armor',(start,-2.1,-2.65),(4.35,1.4,5.3))
            box(plate,'edge',(start,-.7,-2.8),(4.35,.7,.7))
            box(plate,'slag',(start+.35,-2.5,-1.8),(3.65,.5,3.5))
            box(plate,'copper',(start+1.8,-2.65,-.8),(.75,.2,1.6))
            shoulder=f'shoulder_{i}_{"left" if s>0 else "right"}'
            r.part(shoulder,hull,(s*4.5,-3.0,0),(0,0,-s*math.pi/4))
            box(shoulder,'armor',(-.7,-1.4,-2.6),(1.4,2.8,5.2))
            side=f'vent_{i}_{"left" if s>0 else "right"}';r.part(side,hull,(s*5.0,-2.2,0))
            box(side,'armor',(-.6,0,-2.5),(1.2,4.6,5))
            for y in (.7,2.2,3.7):box(side,'dark',(-.75,y,-1.8),(1.5,.55,3.6))
        # Aleta dorsal en sierra y placa de identificación industrial (sin runas).
        box(hull,'carbide',(-.7,-7,-1.8),(1.4,1.3,3.6))
        box(hull,'hazard',(-.75,-7.15,-1.5),(1.5,.2,1.3))
        for s in (-1,1):
            leg=f'leg_{i}_{"left" if s>0 else "right"}'
            r.part(leg,seg,(s*(4.5*taper),3.0+16-height,0),(0,0,-s*.2))
            lo=0 if s>0 else -3.2
            box(leg,'armor',(lo,-.7,-1.3),(3.2,1.8,2.6))
            box(leg,'edge',(lo+.4,-.95,-1.0),(2.4,.35,2))
            knee=f'claw_{i}_{"left" if s>0 else "right"}';r.part(knee,leg,(s*3.0,1.0,0),(0,0,s*.35))
            box(knee,'dark',(-.65,0,-.8),(1.3,3.6,1.6))
            box(knee,'copper',(-.3,.6,1.0),(.6,2.4,.5))
            box(knee,'edge',(-1.0,3.6,-2.5),(2,1.0,3.7))
            box(knee,'carbide',(-.8,4.4,-3.1),(1.6,.3,1.3))
    # Cono caudal y herramienta de arrastre, conectados al último segmento.
    r.part('tail','segment_6',(0,0,2.5))
    box('tail','armor',(-2,-2.5,0),(4,5,1.6));box('tail','slag',(-1.3,-1.7,1.6),(2.6,3.4,1.3))
    box('tail','carbide',(-.8,-.8,2.9),(1.6,1.6,1.1))
    for i,c in enumerate(c for p in r.parts for c in p['cubes']):c['inset']=round((i+1)*.00012,6)
    return r


def smooth(t):
    t=max(0,min(1,t));return t*t*(3-2*t)


def channels(attack,t,exposed=False):
    """Canales [A preparándose, B impacto]; interpolación idéntica en el modelo Java."""
    prepare={};impact={}
    def both(k,a,b):prepare[k]=a;impact[k]=b
    if exposed:
        both('root.y',1.1,1.1);both('head.y',2,2);both('head.xRot',.12,.12)
        for i in range(7):
            both(f'segment_{i}.y',1.3,1.3);both(f'segment_{i}.zRot',.035*math.sin(i*1.5),.035*math.sin(i*1.5))
    elif attack==1:
        both('head.y',-.8,.9);both('head.z',1,-2.7);both('head.xRot',-.13,.18)
    elif attack==2:
        both('head.y',-2.2,-2.2);both('head.z',1.5,1.5);both('head.xRot',-.30,-.30)
        for i in range(7):both(f'segment_{i}.z',-.5*i,-.5*i);both(f'segment_{i}.y',-.18*i,-.18*i)
    elif attack==3:
        both('head.y',1.1,1.1);both('head.xRot',.12,.12);both('head.z',-1.8,-1.8)
    elif attack==4:
        both('head.y',-1.6,-3.0);both('head.z',1.8,-3.0);both('head.xRot',-.48,-.15)
        for i in range(3):both(f'segment_{i}.y',-.45*(3-i),-.5*(3-i))
    elif attack==5:
        both('head.xRot',.48,.78);both('head.y',1.0,5.8);both('root.y',0,4.5)
        for i in range(7):both(f'segment_{i}.y',-math.sin(i/6*math.pi)*1.8,-math.sin(i/6*math.pi)*1.2)
    elif attack==7:
        both('head.xRot',-.12,.05)
        for i in range(7):both(f'segment_{i}.y',-.3*math.sin(i),.4*math.sin(i))
    # Mandíbulas abren hacia fuera (rotación negativa); cerradas convergen al conducto.
    opening={0:(0,0),1:(-.85,.42),2:(-.12,-.12),3:(.05,.05),4:(-.6,-.75),5:(.18,.38),6:(0,0),7:(-.2,-.3)}
    ja,jb=opening.get(attack,(0,0))
    for i in range(8):
        # Caído: las mandíbulas inferiores descansan recogidas sobre el suelo.
        rest=-.30-.22*math.cos(i*math.pi/4)
        both(f'jaw_{i}.xRot',rest if exposed else ja,rest if exposed else jb)
    for i in range(7):
        amount=.95 if exposed else .85 if attack==7 else .10 if attack==4 else 0
        for side,s in (('left',1),('right',-1)):
            both(f'plate_{i}_{side}.zRot',s*amount*.45,s*amount)
            both(f'vent_{i}_{side}.zRot',-s*amount*.2,-s*amount*.55)
            if exposed:
                both(f'leg_{i}_{side}.zRot',-s*.62,-s*.62)
                both(f'leg_{i}_{side}.y',-1.5,-1.5)
                both(f'claw_{i}_{side}.zRot',s*.36,s*.36)
    if exposed:return {k:v for k,v in impact.items()}
    if attack==3:return impact
    charge=smooth(t/.76);hit=smooth((t-.76)/.24)
    return {k:prepare[k]*charge*(1-hit)+impact[k]*hit for k in prepare}


def animation(attack=0,t=0,exposed=False,age=0,limb=0,amount=0):
    d=channels(attack,t,exposed)
    def add(k,v):d[k]=d.get(k,0)+v
    walk=max(0,min(1,amount*6))*(0 if exposed else 1 if attack in (0,3) else .35)
    if attack==3 and not exposed:walk=1
    speed=age*.95 if attack==3 else limb*.8
    for i in range(7):
        phase=speed-i*.85
        add(f'segment_{i}.x',math.sin(phase)*(.22 if attack==3 else .9*walk)+math.sin(age*.11-i*.65)*.08)
        add(f'segment_{i}.yRot',math.sin(phase)*(.035 if attack==3 else .11*walk))
        add(f'segment_{i}.y',math.sin(age*.12-i*.7)*(.03 if exposed else .09))
        for side,s in (('left',1),('right',-1)):
            step=phase+(math.pi if s<0 else 0)
            add(f'leg_{i}_{side}.xRot',math.sin(step)*.38*walk)
            add(f'leg_{i}_{side}.y',-max(0,math.cos(step))*.95*walk)
            add(f'claw_{i}_{side}.xRot',-math.sin(step)*.22*walk)
    for i in range(8):add(f'jaw_{i}.xRot',math.sin(age*.10+i*.7)*.025)
    return d
