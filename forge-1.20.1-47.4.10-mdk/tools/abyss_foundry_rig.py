"""Fundidor: geometría a media escala y poses compartidas por Java y las vistas.
El renderer aplica 2x. Y positiva hacia abajo; suelo = 24, frente = -Z.
"""
import math
from nether_remodel_rigs import Rig

COLORS={'steel':(67,72,68),'edge':(116,115,98),'rust':(105,57,34),
        'dark':(27,30,27),'copper':(157,99,49),'ceramic':(155,143,110),
        'hazard':(175,131,47),'ember':(255,107,20),'core':(255,213,104),'slag':(74,47,35)}


def geometry():
    r=Rig();r.part('body',at=(0,2,0))
    def b(p,mat,xyz,size):r.box(p,mat,xyz,size)
    # Carcasa abierta, revestimiento refractario y núcleo profundo.
    for x in (-8,5.5):
        b('body','steel',(x,-6,-5),(2.5,11,11))
        b('body','ceramic',(x+.25,-4.8,-5.25),(2,8,.5))
    b('body','steel',(-5.5,-6,4),(11,12,2))
    b('body','edge',(-8,-8,-6),(16,2,12))
    b('body','dark',(-6,4,-5),(12,3,10))
    b('body','rust',(-7,4,-7),(14,2,2))
    b('body','slag',(-5,-5,-4),(10,9,6))
    b('body','ember',(-4,-4,-4.3),(8,7,.5))
    b('body','core',(-2,-3,-4.65),(4,5,.4))
    for x in (-4,3):b('body','core',(x,1,-4.65),(1,2,.4))
    # Rejillas batientes: núcleo visible también cerrado, marcos de hierro.
    for name,x,sgn in (('door_left',5.5,-1),('door_right',-5.5,1)):
        r.part(name,'body',(x,-5,-6.3));lo=-5.5 if sgn<0 else 0
        for xx in (lo,lo+4.7):b(name,'steel',(xx,0,0),(.8,9,1))
        for yy in (0,8.2):b(name,'edge',(lo+.8,yy,0),(3.9,.8,1))
        for yy in (2,4,6):b(name,'dark',(lo+.8,yy,.1),(3.9,.6,.8))
        b(name,'copper',(lo+2.1,3.4,-.4),(1.2,2,.4))
    # Placas remachadas, faldón y protectores de hombros.
    for s in (-1,1):
        b('body','rust',(s*7-.7,-5,-5.45),(1.4,8,.6))
        for yy in (-4,0,3):b('body','edge',(s*7-.3,yy,-5.65),(.6,.6,.25))
        b('body','steel',(s*5-1.5,6,-4),(3,3,1))
        b('body','hazard',(s*5-1.2,6.3,-4.2),(2.4,1,.25))
        # Aletas y tapa lateral articulada.
        name='vent_right' if s<0 else 'vent_left';r.part(name,'body',(s*8,-4,1))
        b(name,'steel',(-.35,0,0),(.7,7,4))
        for yy in (1,3,5):b(name,'dark',(-.55,yy,.5),(1.1,.8,3))
        # Dos chimeneas huecas, tapas con bisagra trasera.
        top=-15 if s<0 else -13;x=s*4.5
        for xx in (-1.5,1):b('body','dark',(x+xx,top,2),(.5,-7-top,3))
        for zz in (2,4.5):b('body','rust',(x-1,top,zz),(2,-7-top,.5))
        for yy in (top+.5,-8):b('body','edge',(x-1.7,yy,1.8),(3.4,.6,3.4))
        name='lid_right' if s<0 else 'lid_left';r.part(name,'body',(x,top,5.3))
        b(name,'steel',(-1.8,-.5,-3.6),(3.6,.5,3.6))
        b('body','copper',(s*6-.5,-5,6), (1,8,1))
        for yy in (-4,1):b('body','edge',(s*6-.8,yy,5.8),(1.6,.6,1.4))
    b('body','steel',(-3,-4,6),(6,8,2))
    for yy in (-3,-1,1,3):b('body','dark',(-2.5,yy,8.05),(5,.6,.25))
    r.part('head','body',(0,-8.5,-2))
    b('head','dark',(-3.5,-2,-3),(7,2,5))
    b('head','edge',(-4,-2.5,-3.5),(8,.7,6))
    b('head','ember',(-2.5,-1.3,-3.25),(5,.5,.3))
    for x in (-1.8,.9):b('head','steel',(x,-1.8,-3.4),(.9,1.5,.3))
    # Tren de rodaje: piernas, pistones externos, dedos y talones de acero.
    for side,s in (('right',-1),('left',1)):
        leg=side+'_leg';knee=side+'_knee';foot=side+'_foot'
        r.part(leg,at=(s*4.5,12,0));b(leg,'dark',(-2.2,0,-2.2),(4.4,5.5,4.4))
        b(leg,'steel',(-2.5,.4,-2.7),(5,4,.7))
        b(leg,'copper',(s*2.1-.4,.4,-.4),(.8,5,.8))
        r.part(knee,leg,(0,5.5,0));b(knee,'steel',(-2.4,-.8,-2.5),(4.8,5.5,5))
        b(knee,'edge',(-1.8,-.4,-3),(3.6,2,.7))
        b(knee,'dark',(-.7,0,2.6),(1.4,5,1.4))
        b(knee,'copper',(-.35,1,4),(.7,4,.5))
        r.part(foot,knee,(0,5.5,0));b(foot,'dark',(-2.8,0,-4.5),(5.6,1,8))
        b(foot,'steel',(-2.5,-1.6,-4.2),(5,1.6,7.4))
        for x in (-2,0,2):b(foot,'edge',(x-.5,-1.3,-4.7),(1,1.3,1))
    # Brazos; pivotes separados para pose legible de herramientas pesadas.
    for side,s in (('right',-1),('left',1)):
        arm=side+'_arm';elbow=side+'_elbow'
        r.part(arm,'body',(s*9,-4.5,0));b(arm,'dark',(-1.8,0,-1.8),(3.6,8,3.6))
        b(arm,'steel',(-2.5,-1.2,-2.5),(5,3.4,5))
        b(arm,'rust',(-2.6,-1.4,-2.65),(5.2,.7,5.3))
        for yy in (2.5,5.5):b(arm,'edge',(-2,yy,-2.1),(4,.7,4.2))
        b(arm,'copper',(s*2-.4,1,-.5),(.8,6,.8))
        r.part(elbow,arm,(0,8,0));b(elbow,'dark',(-1.5,0,-1.5),(3,7,3))
        b(elbow,'steel',(-2,0,-2),(4,2,4));b(elbow,'copper',(-.55,2,-2), (1.1,5,.8))
    r.part('hammer','right_elbow',(0,8,0))
    b('hammer','steel',(-4.5,-1.5,-4.5),(9,4,9))
    b('hammer','dark',(-3.5,2.5,-3.5),(7,1,7))
    b('hammer','edge',(-4,3.5,-4),(8,1,8))
    b('hammer','ember',(-3.8,3.1,-3.8),(7.6,.3,7.6))
    for x in (-3,2):b('hammer','edge',(x,-2,-4.7),(1,4.5,.5))
    for z in (-2.5,1.5):b('hammer','hazard',(-4.7,-.8,z),(.4,2.5,1))
    for x in (-2.5,1.5):b('hammer','dark',(x,4.5,-3),(1,.6,6))
    r.part('crucible','left_elbow',(0,7,0))
    b('crucible','dark',(-4,5,-4),(8,1.3,8))
    for x in (-4,3):b('crucible','rust',(x,-1,-4),(1,6,8))
    for z in (-4,3):b('crucible','rust',(-3,-1,z),(6,6,1))
    b('crucible','ember',(-3,0,-3),(6,.4,6))
    b('crucible','core',(-1.5,-.1,-2),(3,.2,3))
    for x in (-4.25,3.25):b('crucible','edge',(x,-1.5,-4.25),(1,.6,8.5))
    for z in (-4.25,3.25):b('crucible','edge',(-3.25,-1.5,z),(6.5,.6,1))
    b('crucible','ceramic',(-1.5,-.8,-5.5),(3,.6,1.4))
    for x in (-2,1.5):b('crucible','dark',(x,-1.6,-5.5),(.5,1.4,1.4))
    r.part('slag_stream','crucible',(0,-.2,-5.2))
    b('slag_stream','ember',(-.5,0,-.5),(1,4,1));b('slag_stream','core',(-.3,3,-.3),(.6,3,.6))
    # Holgura microscópica distinta por caja: las uniones no comparten planos
    # visibles. CubeDeformation conserva las dimensiones UV y el encaje del atlas.
    for i,c in enumerate(c for p in r.parts for c in p['cubes']):c['inset']=round((i+1)*.0002,6)
    return r


CROUCH={'root.y':1.1,'body.y':3.1,'right_leg.xRot':-.45,'left_leg.xRot':-.45,
        'right_knee.xRot':.75,'left_knee.xRot':.75,'right_foot.xRot':-.3,'left_foot.xRot':-.3}


def crouch(amount,**channels):return {**{k:v*amount for k,v in CROUCH.items()},**channels}


POSES={
 1:( {'right_arm.xRot':-2.15,'right_elbow.xRot':-.6,'body.yRot':-.16,'left_arm.zRot':-.15},
      crouch(1,**{'body.xRot':.32,'right_arm.xRot':-.6,'right_elbow.xRot':.1,'body.yRot':.1})),
 2:( {'right_arm.xRot':-1.45,'right_elbow.xRot':-.5,'body.xRot':-.12,'left_arm.zRot':-.25},
      crouch(1,**{'body.xRot':.25,'right_arm.xRot':-.5,'right_elbow.xRot':.1})),
 3:( {'left_arm.xRot':.65,'left_elbow.xRot':-.8,'crucible.xRot':.15,'body.yRot':.15},
      {'body.xRot':.2,'body.yRot':-.12,'left_arm.xRot':-.9,'left_elbow.xRot':-.4,'crucible.xRot':2.0}),
 4:( crouch(.7,**{'body.xRot':.12,'door_left.yRot':-.3,'door_right.yRot':.3,'vent_left.zRot':-.2,'vent_right.zRot':.2}),
      crouch(1,**{'body.xRot':.3,'door_left.yRot':-1.5,'door_right.yRot':1.5,'vent_left.zRot':-1.1,'vent_right.zRot':1.1,
                 'lid_left.xRot':-1.2,'lid_right.xRot':-1.2,'right_arm.zRot':.32,'left_arm.zRot':-.32,'left_arm.xRot':-.3}))
}
EXPOSED=crouch(1,**{'body.xRot':.38,'head.xRot':.25,'door_left.yRot':-1.5,'door_right.yRot':1.5,
                  'vent_left.zRot':-1.1,'vent_right.zRot':1.1,'lid_left.xRot':-1.2,'lid_right.xRot':-1.2,
                  'right_arm.zRot':.15,'left_arm.zRot':-.2,'left_arm.xRot':-.38})


def smooth(t):
    t=max(0,min(1,t));return t*t*(3-2*t)


def pose(attack=0,t=0,exposed=False):
    if exposed:return EXPOSED.copy()
    if attack not in POSES:return {}
    a,b=POSES[attack];charge=smooth(t/.78);hit=smooth((t-.78)/.22)
    return {k:a.get(k,0)*charge*(1-hit)+b.get(k,0)*hit for k in a.keys()|b.keys()}
