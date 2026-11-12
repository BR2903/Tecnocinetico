"""Geometría original de seis mobs vanilla del Nether. Coordenadas y pivotes Mojang 1.20.1.

Solo arte: todos los accesorios cuelgan de huesos animados vanilla; no desplazan manos,
cabezas ni las raíces usadas por el escalado de los bebés.
"""
import math
import struct

NAMES=('piglin','piglin_brute','zombified_piglin','ghast','blaze','wither_skeleton')

class Rig:
    def __init__(self): self.parts=[]
    def part(self,name,parent='root',at=(0,0,0),rot=(0,0,0)):
        self.parts.append(dict(name=name,parent=parent,at=at,rot=rot,cubes=[]));return name
    def box(self,part,material,xyz,size):
        next(p for p in self.parts if p['name']==part)['cubes'].append(dict(material=material,xyz=xyz,size=size))
    def panel(self,name,parent,xyz,size,material='armor',rot=(0,0,0)):
        self.part(name,parent,xyz,rot);self.box(name,material,(0,0,0),size);return name

HUMANOID={'head':(0,0,0),'hat':(0,0,0),'body':(0,0,0),
          'right_arm':(-5,2,0),'left_arm':(5,2,0),
          'right_leg':(-1.9,12,0),'left_leg':(1.9,12,0)}
PLAYER_EMPTY={'ear':(0,0,0),'cloak':(0,0,0),'left_sleeve':(5,2,0),
              'right_sleeve':(-5,2,0),'left_pants':(1.9,12,0),
              'right_pants':(-1.9,12,0),'jacket':(0,0,0)}

def piglin(kind='piglin'):
    r=Rig();brute=kind=='piglin_brute';dead=kind=='zombified_piglin'
    for name,at in HUMANOID.items():r.part(name,at=at)
    for name,at in PLAYER_EMPTY.items():r.part(name,at=at)
    skin='rot' if dead else 'skin';head='rot_head' if dead else 'pig_head'
    r.box('head',head,(-5,-8,-4),(10,8,8))
    r.box('head','snout_rot' if dead else 'snout',(-2,-4,-5),(4,4,1))
    for i,s in enumerate((-1,1)):
        r.box('head','ivory',(s*2.5-.5,-2,-5.15),(1,2,1.15))
        name='right_ear' if s<0 else 'left_ear'
        r.part(name,'head',(s*4.5,-6,0),(0,0,-s*math.pi/6))
        r.box(name,skin,(-1 if s<0 else 0,0,-2),(1,5,4))
        r.panel(f'ear_clip{i}',name,(-1.15 if s<0 else -.15,3,-2.15),(1.3,.8,4.3),'copper' if not dead else 'rust')
    # Casquete abierto: orejas, hocico y colmillos siguen siendo reconocibles.
    if not dead:
        r.panel('brow','head',(-5.1,-8.15,-4.15),(10.2,1.5,8.3),'armor')
        r.panel('brow_edge','head',(-5.15,-6.8,-4.3),(10.3,.5,.45),'copper')
        r.panel('sensor','head',(2.2,-5.1,-4.2),(1.7,.55,.25),'ember')
    else:
        r.panel('skull_patch','head',(1,-7.8,-4.16),(3.4,2.4,.25),'bone_light')
        r.panel('eye_implant','head',(2.1,-5.1,-4.2),(1.8,.55,.25),'ember_red')
        r.panel('broken_temple','head',(-5.1,-7,0),(1.1,3,3),'rust')
    r.box('body','rot' if dead else 'leather',(-4,0,-2),(8,12,4))
    if dead:
        # Abertura al ras del pecho: nada colgando de las mejillas.
        r.panel('open_cavity','body',(0,1.5,-2.25),(3.7,7.6,.4),'dark')
        r.panel('spinal_wire','body',(1,2,-2.6),(.7,6,.45),'flesh')
        for i in range(3):r.panel(f'exposed_rib{i}','body',(.1,2.1+i*2.2,-2.8),(3.5,.65,.6),'bone_light')
        r.panel('torn_plate','body',(-4.15,1,-2.4),(3.5,4,.65),'rust')
        r.panel('retained_belt','body',(-4.1,9.5,-2.15),(8.2,1.4,4.3),'leather')
        r.panel('sternum_ember','body',(2.3,8,-2.4),(.9,1.2,.25),'ember_red')
    else:
        r.panel('chest','body',(-4.15,.5,-2.55),(8.3,7.6,.75),'armor')
        r.panel('chest_rim','body',(-4.2,7.3,-2.65),(8.4,.9,.9),'copper')
        for i in range(3):r.panel(f'front_vent{i}','body',(-2.9,2+i*1.4,-2.7),(3.2,.55,.25),'dark')
        r.panel('thermal_gauge','body',(1.5,2.2,-2.8),(1.1,2.3,.3),'ember')
        r.panel('belt','body',(-4.2,9.5,-2.2),(8.4,1.5,4.4),'leather')
        r.panel('belt_buckle','body',(-1,9.6,-2.65),(2,1.3,.5),'brass')
        r.panel('back_mount','body',(-3.5,.8,1.8),(7,8,.82),'armor')
        for i,x in enumerate((-2.5,1)):
            r.panel(f'back_pipe{i}','body',(x,1.15,2.5),(1.5,7,1.3),'dark')
            for j,y in enumerate((1,4,7)):r.panel(f'pipe_collar{i}_{j}','body',(x-.2,y,2.4),(1.9,.7,1.5),'copper')
    for i,(name,side) in enumerate((('right_arm',-1),('left_arm',1))):
        x=-3 if side<0 else -1
        if dead and side>0:
            r.box(name,'bone_light',(x+.65,-2,-1.1),(2.6,4,2.2))
            r.box(name,'dark',(x+1,1,-.8),(2,6,1.6))
            r.panel('exposed_arm_rod',name,(x+.6,1.08,-1.55),(.6,5.7,1),'copper')
            r.box(name,'rot',(x,7,-2),(4,3,4))
        else:r.box(name,skin,(x,-2,-2),(4,12,4))
        if not dead or side<0:
            r.panel(f'pauldron{i}',name,(x-.35,-2.2,-2.35),(4.7,3.2,4.7),'rust' if dead else 'armor')
            r.panel(f'bracer{i}',name,(x-.12,4.5,-2.15),(4.24,3,4.3),'rust' if dead else 'dark')
            r.panel(f'bracer_clasp{i}',name,(x+.3,5.5,-2.45),(3.4,.6,.4),'copper')
        if brute:
            r.panel(f'upper_guard{i}',name,(x-.65,-2.5,-2.65),(5.3,2.3,5.3),'armor_light')
            r.panel(f'guard_rim{i}',name,(x-.7,-.5,-2.7),(5.4,.6,5.4),'brass')
            r.panel(f'elbow_cap{i}',name,(x+.3,2,-2.45),(3.4,2.3,.6),'armor')
    for i,name in enumerate(('right_leg','left_leg')):
        r.box(name,skin if dead and i==0 else 'cloth',(-1.8,0,-1.9),(3.6,11.8,3.8))
        r.panel(f'knee{i}',name,(-1.6,3.8,-2.25),(3.2,2,.5),'rust' if dead else 'armor')
        r.panel(f'boot{i}',name,(-1.85,8.1,-2.3),(3.7,3.9,4.45),'dark')
        r.panel(f'boot_toe{i}',name,(-1.86,10.4,-2.55),(3.72,1.55,.45),'copper' if brute else 'armor')
    if brute:
        r.panel('left_lamella','body',(-4.5,7.9,-2.4),(3.5,3.6,.7),'armor_light')
        r.panel('right_lamella','body',(1,7.9,-2.4),(3.5,3.6,.7),'armor_light')
        r.panel('reinforced_sternum','body',(-.8,.8,-2.9),(1.6,6.7,.65),'brass')
        r.panel('jaw_guard','head',(-2.1,-.9,-4.7),(4.2,1,.65),'armor_light')
        for i,z in enumerate((-1.8,.2,2.2)):r.panel(f'helmet_rib{i}','head',(-4.7,-8.6,z),(9.4,.65,.6),'armor_light')
    return r

def skeleton():
    r=Rig()
    for name,at in HUMANOID.items():r.part(name,at=(-2 if name=='right_leg' else 2,12,0) if name.endswith('_leg') else at)
    r.box('head','wither_skull',(-4,-8,-4),(8,8,8))
    r.panel('skull_band','head',(-4.15,-7.9,-4.15),(8.3,1.2,8.3),'armor')
    for i,x in enumerate((-2.6,1.2)):r.panel(f'eye_coal{i}','head',(x,-5.1,-4.18),(1.4,.5,.25),'ember_red')
    r.panel('neck','body',(-1.05,-.5,-1.05),(2.1,2.5,2.1),'bone')
    r.box('body','dark',(-1,1,-1),(2,10,2))
    for i,y in enumerate((2,4.5,7)):
        r.panel(f'rib{i}','body',(-3.4,y,-1.85),(6.8,.9,1.1),'bone')
        for j,s in enumerate((-1,1)):r.panel(f'rib_side{i}_{j}','body',(s*3.1-.4,y+.1,-1.2),(.8,1.1,3.1),'bone')
    r.panel('pelvis','body',(-3.4,10,-1.7),(6.8,2,3.4),'armor')
    r.panel('spine_conduit','body',(.9,1,1),(.7,9,.6),'copper')
    r.panel('core_latch','body',(-.6,4.6,-2.1),(1.2,1.3,.4),'ember')
    for i,name in enumerate(('right_arm','left_arm')):
        r.box(name,'bone',(-1,-2,-1),(2,12,2))
        r.panel(f'shoulder{i}',name,(-1.6,-2.1,-1.6),(3.2,2.1,3.2),'armor')
        r.panel(f'forearm{i}',name,(-1.3,4,-1.3),(2.6,3,2.6),'armor')
        r.panel(f'arm_piston{i}',name,(1,1,-.5),(.6,5.8,1),'copper')
    for i,name in enumerate(('right_leg','left_leg')):
        r.box(name,'bone',(-1,0,-1),(2,11.8,2))
        r.panel(f'kneecap{i}',name,(-1.4,4,-1.6),(2.8,2.1,.75),'armor')
        r.panel(f'ankle{i}',name,(-1.3,9,-1.3),(2.6,3,2.6),'dark')
        r.panel(f'heel_collar{i}',name,(-1.45,10,-1.4),(2.9,.8,2.8),'copper')
    return r

class JavaRandom:
    def __init__(self,seed):self.state=(seed^0x5DEECE66D)&((1<<48)-1)
    def next_int(self,n):
        while True:
            self.state=(self.state*0x5DEECE66D+11)&((1<<48)-1);bits=self.state>>17;v=bits%n
            if bits-v+n-1<2147483648:return v

def ghast():
    r=Rig();r.part('body',at=(0,17.6,0));r.box('body','ghast_skin',(-8,-8,-8),(16,16,16))
    # Placas escalonadas estrechas; la mayor parte de la silueta permanece pálida.
    for i,s in enumerate((-1,1)):
        r.panel(f'cheek_plate{i}','body',(s*6.4-1.4,-5.8,-8.2),(2.8,8.6,.5),'armor_light')
        r.panel(f'eye_socket{i}','body',(s*3.2-1.5,-3.1,-8.25),(3,2.5,.5),'dark')
        r.panel(f'eye_ember{i}','body',(s*3.2-1,-2.4,-8.55),(2,.7,.25),'ember')
        r.panel(f'crown_side{i}','body',(s*6.5-1,-8.2,-5),(2,1,10),'armor')
        for j,y in enumerate((-4,-1,2)):r.panel(f'side_rivet{i}_{j}','body',(s*8.05-.15,y,-3),(.3,.6,.7),'copper')
    r.panel('furnace_mouth','body',(-2.2,1,-8.3),(4.4,4.7,.5),'dark')
    r.panel('mouth_heat','body',(-1.6,1.6,-8.65),(3.2,3.5,.25),'ember')
    for i,x in enumerate((-1.7,-.3,1.1)):r.panel(f'mouth_bar{i}','body',(x,.9,-8.95),(.6,5,.45),'armor')
    r.panel('lower_band','body',(-8.15,6.6,-8.15),(16.3,1.2,16.3),'armor')
    r.panel('back_exhaust','body',(-3.7,-4,7.9),(7.4,8,.6),'dark')
    for i,y in enumerate((-3.4,-1,1.4)):r.panel(f'back_radiator{i}','body',(-3.3,y,8.3),(6.6,.75,.5),'copper')
    random=JavaRandom(1660)
    for i in range(9):
        x=((i%3-(i//3%2)*.5+.25)-1)*5;z=(i//3-1)*5;length=random.next_int(7)+8
        name=r.part(f'tentacle{i}',at=(x,24.6,z));r.box(name,'ghast_tube',(-1,0,-1),(2,length,2))
        for j,y in enumerate((.4,length*.45,length-1)):
            r.panel(f'coupler{i}_{j}',name,(-1.25,y,-1.25),(2.5,.8,2.5),'armor' if j==0 else 'copper')
        r.panel(f'tip{i}',name,(-.6,length-2,-1.15),(1.2,1,.3),'ember')
    return r

def blaze():
    r=Rig();r.part('head');r.box('head','blaze_face',(-4,-4,-4),(8,8,8))
    r.panel('top_heatshield','head',(-4.15,-4.1,-4.1),(8.3,.8,8.2),'armor')
    for i,s in enumerate((-1,1)):
        r.panel(f'eye_socket{i}','head',(s*2-1,-1.7,-4.1),(2,1.5,.3),'dark')
        r.panel(f'burner_eye{i}','head',(s*2-.7,-1.3,-4.35),(1.4,.6,.2),'ember_hot')
        r.panel(f'face_rail{i}','head',(s*3.5-.4,-3,-4.25),(.8,6,.5),'copper')
    r.panel('mouth','head',(-1.6,1,-4.2),(3.2,1.8,.4),'dark')
    for i,x in enumerate((-1,0,1)):r.panel(f'breather{i}','head',(x-.2,1.1,-4.45),(.4,1.5,.25),'ember')
    r.panel('neck_burner','head',(-2,3.8,-2),(4,2.4,4),'dark')
    r.panel('burner_tip','head',(-1.5,5.8,-1.5),(3,.8,3),'ember_hot')
    for i in range(12):
        # Mth usa una tabla de seno, no math.cos directo. Reproducirla para la vista.
        f32=lambda n:struct.unpack('f',struct.pack('f',n))[0]
        def trig(angle,cos=False):
            value=f32(f32(angle)*f32(10430.378))
            if cos:value=f32(value+16384)
            return f32(math.sin((int(value)&65535)*math.pi*2/65536))
        a=f32(0 if i<4 else f32(math.pi)/4 if i<8 else .47123894)
        for _ in range(i if i<4 else i-4 if i<8 else i-8):a=f32(a+1)
        radius=9 if i<4 else 7 if i<8 else 5
        y=f32((-2 if i<4 else 2 if i<8 else 11)+trig(f32(i*.5 if i<8 else i*.75),True))
        name=r.part(f'part{i}',at=(f32(trig(a,True)*radius),y,f32(trig(a)*radius)))
        r.box(name,'dark',(0,0,0),(2,8,2))
        r.panel(f'injector{i}',name,(.25,1.4,-.18),(1.5,4.7,.35),'ember_hot' if i<4 else 'ember')
        for j,h in enumerate((.2,6.5)):r.panel(f'injector_cap{i}_{j}',name,(-.2,h,-.2),(2.4,1.1,2.4),'copper')
        r.panel(f'injector_spine{i}',name,(.65,1.2,1.8),(.7,5,.5),'armor')
    return r

def all_rigs():return {**{n:piglin(n) for n in NAMES[:3]},'ghast':ghast(),'blaze':blaze(),'wither_skeleton':skeleton()}
