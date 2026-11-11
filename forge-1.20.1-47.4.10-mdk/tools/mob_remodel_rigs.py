"""Geometría articulada propia de los mobs anteriores a Vigía/Matriarca. Comparte rig Java y vistas."""
import math
from guardian_rig import Rig, UV


def panel(r, name, parent, at, size, rot=(0,0,0), material="armor"):
    r.part(name, parent, at, rot)
    w,h,d = size
    r.cube(name, material, (-w/2,-h/2,-d/2), size)
    return name


def siege():
    r = Rig(); r.part("body", "root", (0,8,0))
    r.cube("body", "dark", (-7,-4,-9), (14,7,18))
    for i, z in enumerate((-6,0,6)):
        panel(r,f"dorsal{i}","body",(0,-6,z),(15-i,3,7),(.06*i,0,0))
    for side in (-1,1):
        panel(r,f"flank{side+1}","body",(side*7,-2,0),(3,7,18),(0,0,side*-.23))
        for j in range(3):
            panel(r,f"vent{side+1}_{j}","body",(side*8.5,-2,j*4-4),(1,3,2),material="copper")
    r.part("missiles","body",(0,-9,5))
    r.cube("missiles","armor",(-5,-3,-4),(10,5,8))
    for x in (-3,0,3):
        for z in (-2,2):
            r.cube("missiles","dark",(x-1,-4,z-1),(2,1,2))
            r.cube("missiles","glow",(x-.5,-4.3,z-.5),(1,1,1))
    r.part("reactor","body",(0,-2,10))
    r.cube("reactor","dark",(-4,-3,0),(8,6,2))
    r.cube("reactor","glow",(-2,-2,1),(4,4,2))
    for x in (-3,0,3): r.cube("reactor","copper",(x-.4,-3,3),(1,6,1))
    r.part("turret","body",(0,-3,-9))
    r.cube("turret","armor",(-5,-4,-7),(10,7,8))
    panel(r,"visor","turret",(0,-2,-8),(6,3,2),material="dark")
    r.cube("visor","glow",(-2,-.5,-1.6),(4,1,1))
    for i,side in enumerate((-1,1)):
        panel(r,f"mantlet{i}","turret",(side*5,-1,-3),(2,6,7),(0,0,side*.18))
        r.part(f"gun{i}","turret",(side*3,-1,-6))
        r.cube(f"gun{i}","dark",(-1,-1,-15),(2,2,15))
        for z in (-12,-6): r.cube(f"gun{i}","copper",(-1.5,-1.5,z),(3,3,1))
        r.cube(f"gun{i}","ceramic",(-1.5,-1.5,-16),(3,3,2))
    # Anclajes, longitud y elevación EXACTOS de SiegeSpiderEntity: las seis rodillas siguen en el mismo sitio.
    anchors = [(8,-7,.45),(8,0,0),(8,7,-.45),(-8,-7,-.45),(-8,0,0),(-8,7,.45)]
    for i,(x,z,yaw) in enumerate(anchors):
        side = 1 if x>0 else -1
        leg=r.part(f"leg{i}","root",(x,6,z),(0,yaw,-side*.5))
        r.cube(leg,"dark",(0 if side>0 else -14,-1,-1),(14,2,2))
        r.cube(leg,"armor",(1 if side>0 else -12,-2.5,-2),(11,1.5,4))
        r.cube(leg,"copper",(2 if side>0 else -10,1,-.5),(8,1,1))
        shin=r.part(f"shin{i}",leg,(side*14,0,0),(0,0,side*.5))
        r.cube(shin,"copper",(-2,-2,-2),(4,4,4))
        r.cube(shin,"glow",(-1,-1,-2.6),(2,2,1))
        r.cube(shin,"dark",(-1,0,-1),(2,25,2))
        panel(r,f"shinArmor{i}",shin,(0,12,-1.7),(4,15,2),(.08,0,0))
        r.cube(shin,"copper",(-1.5,18,-1.5),(3,2,3))
        r.cube(shin,"armor",(-3,23,-3),(6,2,6))
        for dx in (-2,1): r.cube(shin,"ceramic",(dx,23,-5),(1,2,3))
    return r


def swarm():
    r=Rig(); r.part("body","root",(0,13,0))
    r.cube("body","fiber",(-4,-3,-6),(8,6,13))
    for i,z in enumerate((-4,1,5)):
        panel(r,f"shell{i}","body",(0,-4,z),(10-i,3,5),(-.08*i,0,0))
    r.part("core","body",(0,1,-7))
    r.cube("core","glow",(-2,-2,-1),(4,4,2))
    for x in (-2,0,2): r.cube("body","copper",(x-.3,-2,-8.2),(1,6,1))
    r.part("head","root",(0,10,-6))
    r.cube("head","armor",(-3,-3,-5),(6,5,6))
    r.cube("head","dark",(-2,0,-6),(4,2,1))
    for side in (-1,1):
        r.cube("head","glow",(side*1.5-.5,-1,-6),(1,1,1))
        panel(r,f"jaw{side+1}","head",(side*2,1,-5),(1,3,4),(.3,side*.3,0),"ceramic")
    for side,label in [(-1,"right"),(1,"left")]:
        front=r.part("front_"+label,"root",(side*6,12,-5))
        r.cube(front,"fiber",(-1.5,-1,-1.5),(3,7,3))
        r.cube(front,"armor",(-2,0,-2),(4,4,2))
        r.cube(front,"copper",(-1.5,6,-1.5),(3,2,3))
        panel(r,"blade_"+label,front,(0,9,-2),(2,7,6),(.35,0,side*.1),"ceramic")
        mid=r.part("mid_"+label,"root",(side*5,14,1))
        r.cube(mid,"dark",(-1,0,-1),(2,10,2))
        r.cube(mid,"copper",(-1.5,3,-1.5),(3,2,3))
        r.cube(mid,"armor",(-2,9,-2),(4,1,4))
        rear=r.part("rear_"+label,"root",(side*4.5,14,5))
        r.cube(rear,"fiber",(-2,-1,-2),(4,6,4))
        r.cube(rear,"armor",(-2.5,-1,-2.5),(5,3,5))
        r.cube(rear,"copper",(-1,5,-1),(2,4,2))
        r.cube(rear,"dark",(-2,9,-3),(4,1,6))
    return r


def wasp(node=False):
    r=Rig(); r.part("body","root",(0,17,0))
    r.cube("body","fiber",(-2,-2,-2),(4,4,5))
    panel(r,"thoraxPlate","body",(0,-2.5,0),(5,1,5))
    r.part("head","body",(0,-.5,-2.5))
    r.cube("head","armor",(-1.5,-1.5,-3),(3,3,3))
    for side in (-1,1):
        r.cube("head","glow",(side*1.5-.5,-1,-3.5),(1,2,1))
        panel(r,"antenna"+str(side+1),"head",(side, -2,-2),(1,1,4),(.6,side*.35,0),"copper")
    r.part("abdomen","body",(0,.6,3.4),(-.35,0,0))
    r.cube("abdomen","fiber",(-2,-2,0),(4,4,7))
    for i in range(3): panel(r,f"abdomenPlate{i}","abdomen",(0,-2,i*2+1),(5-i,1,3))
    panel(r,"sting","abdomen",(0,0,8),(1,1,4),(.2,0,0),"ceramic")
    for side,label in [(1,"left"),(-1,"right")]:
        for j,part in enumerate(("front","back")):
            wing=r.part(f"wing_{label}_{part}","body",(side*1.2,-2,-1+j*2),(0,side*(-.25+j*.55),side*-.15))
            r.cube(wing,"ceramic",(0 if side>0 else -7,-.1,-1.5),(7,.2,3))
            r.cube(wing,"copper",(0 if side>0 else -7,-.3,-.2),(7,.5,.5))
        for j in range(3):
            leg=r.part(f"leg_{label}_{j}","body",(side*1.5,1.5,j*1.5-1.5),(.4,0,side*-.5))
            r.cube(leg,"dark",(-.5,0,-.5),(1,4,1))
            r.cube(leg,"ceramic",(-.5,3,-1.5),(1,1,2))
    # Placas de la escolta y antena larga de la exploradora: visibles según el papel de la avispa.
    for side in (-1,1): panel(r,f"guard{side+1}","body",(side*2,0,0),(1,4,5),(0,0,side*.25))
    panel(r,"scoutSensor","head",(0,-2,-4),(1,1,5),(.3,0,0),"glow")
    return r


def hive():
    r=Rig(); r.part("body","root",(0,12,0))
    r.cube("body","fiber",(-5,-5,-5),(10,10,10))
    r.cube("body","dark",(-3,-8,-3),(6,3,6))
    r.cube("body","glow",(-3,-4,-6),(6,7,2))
    for x in (-3,0,3): r.cube("body","copper",(x-.4,-5,-7),(1,9,1))
    for i in range(6):
        a=i*math.pi/3
        p=panel(r,f"petal{i}","body",(math.sin(a)*7,0,math.cos(a)*7),(5,12,2),(.1,a,0))
        r.cube(p,"copper",(-2,3,-1.7),(4,1,1))
        r.cube(p,"glow",(-.5,-3,-1.7),(1,3,1))
    panel(r,"crown","body",(0,-9,0),(9,2,9),material="copper")
    for side in (-1,1):
        panel(r,f"receiver{side+1}","body",(side*4,-12,0),(1,7,1),(0,0,side*.3),"ceramic")
        panel(r,f"claw{side+1}","body",(side*3,8,0),(2,6,3),(0,0,side*-.35),"dark")
    return r


def drone():
    r=Rig(); r.part("body","root",(0,17,0))
    r.cube("body","armor",(-4,-1.5,-4),(8,3,8))
    panel(r,"top","body",(0,-3,0),(6,2,7))
    panel(r,"sensor","body",(0,-.5,-5),(4,2,2),material="dark")
    r.cube("sensor","glow",(-1,-.5,-1.5),(2,1,1))
    for i in range(4):
        arm=r.part(f"arm{i}","body",rot=(0,math.pi/4+i*math.pi/2,0))
        r.cube(arm,"dark",(-1,-1,-9),(2,2,8))
        for j in range(8):
            a=j*math.pi/4
            panel(r,f"duct{i}_{j}",arm,(math.sin(a)*3,-1, -9+math.cos(a)*3),(2.5,2,1),(0,a,0))
        rotor=r.part(f"rotor{i}",arm,(0,-1.5,-9))
        r.cube(rotor,"copper",(-.5,-1,-.5),(1,3,1))
        r.cube(rotor,"ceramic",(-2.5,0,-.5),(5,.4,1))
        r.cube(rotor,"ceramic",(-.5,0,-2.5),(1,.4,5))
    r.part("gun","body",(0,1.5,-1))
    r.cube("gun","dark",(-1,-.5,-8),(2,2,8))
    r.cube("gun","copper",(-1.5,-1,-5),(3,3,1))
    r.cube("gun","glow",(-.5,0,-8.5),(1,1,1))
    panel(r,"antenna","body",(0,-5,2),(1,5,1),material="copper")
    return r


def turret():
    r=Rig(); r.part("base","root",(0,24,0))
    r.cube("base","armor",(-5,-2,-5),(10,2,10))
    r.cube("base","copper",(-2,-8,-2),(4,6,4))
    for i in range(3):
        a=i*math.pi*2/3
        p=panel(r,f"stabilizer{i}","base",(math.sin(a)*5,-1,math.cos(a)*5),(3,2,7),(0,a,0),"dark")
        r.cube(p,"copper",(-1,-1,-3),(2,2,1))
    r.part("head","root",(0,12,0))
    r.cube("head","armor",(-4,-3,-4),(8,6,9))
    for i,side in enumerate((-1,1)):
        panel(r,f"shield{i}","head",(side*4.5,0,-1),(2,7,8),(0,0,side*-.15))
        r.cube("head","dark",(side*2-1,-1,-13),(2,2,10))
        for z in (-10,-6): r.cube("head","copper",(side*2-1.5,-1.5,z),(3,3,1))
        r.cube("head","ceramic",(side*2-1.5,-1.5,-14),(3,3,2))
    r.cube("head","dark",(-2,-3,-5),(4,2,1))
    r.cube("head","glow",(-1,-2.5,-5.6),(2,1,1))
    panel(r,"radar","head",(0,-6,2),(6,2,2),material="ceramic")
    return r


def anchor():
    r=Rig(); r.part("body","root",(0,9,0))
    r.cube("body","armor",(-5,-3,-5),(10,6,10))
    r.cube("body","glow",(-2,-1.5,-5.8),(4,3,1))
    for side in (-1,1): panel(r,f"shoulder{side+1}","body",(side*5,-1,0),(2,6,9),(0,0,side*-.18))
    r.part("head","body",(0,-4,0))
    r.cube("head","dark",(-2,-2,-3),(4,3,5))
    r.cube("head","copper",(-1,-1,-8),(2,2,6))
    r.cube("head","glow",(-1,-1,-9),(2,2,1))
    for x in (-2,2):
        r.cube("head","ceramic",(x-.5,-5,-3),(1,5,5))
    r.cube("head","glow",(-1,-5,-1),(2,3,2))
    for i,a in enumerate((math.pi/3,math.pi,-math.pi/3)):
        leg=r.part(f"leg{i}","root",(0,10,0),(0,a,0))
        r.cube(leg,"dark",(-1,-1,-9),(2,2,8))
        r.cube(leg,"armor",(-1.5,-2.5,-8),(3,1.5,6))
        shin=r.part(f"shin{i}",leg,(0,0,-9))
        r.cube(shin,"copper",(-2,-2,-2),(4,4,4))
        r.cube(shin,"dark",(-1,0,-1),(2,14,2))
        r.cube(shin,"armor",(-1.5,3,-2),(3,8,1.5))
        r.cube(shin,"armor",(-2,12,-3),(4,2,6))
    return r


def corrupted_spider():
    r=Rig(); r.part("body","root",(0,17,3))
    r.cube("body","fiber",(-5,-3,-2),(10,6,12))
    for i,z in enumerate((0,4,8)): panel(r,f"shell{i}","body",(0,-3.5,z),(11-i,2,5),(-.05*i,0,0))
    r.part("head","root",(0,17,-5))
    r.cube("head","fiber",(-3,-2.5,-4),(6,5,6))
    for x in (-2,0,2): r.cube("head","glow",(x-.5,-1,-4.7),(1,1,1))
    for side in (-1,1): panel(r,f"fang{side+1}","head",(side*2,2,-4),(1,4,2),(.4,0,side*.15),"ceramic")
    for i in range(8):
        side=-1 if i<4 else 1
        leg=r.part(f"leg{i}","root",(side*4,17,(i%4)*3-5),(0,-side*(math.pi/2+(i%4-1.5)*.35),0))
        r.cube(leg,"fiber",(-1,-1,-10),(2,2,10))
        r.cube(leg,"armor",(-1,-2.5,-8),(2,1.5,6))
        shin=r.part(f"shin{i}",leg,(0,0,-10),(-.3,0,0))
        r.cube(shin,"copper",(-1.5,-1.5,-1.5),(3,3,3))
        r.cube(shin,"dark",(-.5,0,-.5),(1,7,1))
    return r


def creeper():
    r=Rig(); r.part("body","root",(0,6,0))
    r.cube("body","fiber",(-3,0,-2),(6,12,4))
    for i in range(3): panel(r,f"rib{i}","body",(0,3+i*3,-2.5),(7,1,2),material="armor")
    r.cube("body","glow",(-1,3,-3.7),(2,6,1))
    r.part("head","root",(0,6,0))
    r.cube("head","fiber",(-4,-8,-4),(8,8,8))
    panel(r,"halfMask","head",(-2,-4,-4),(4,7,2),(0,0,-.1))
    for x in (-2,2): r.cube("head","glow",(x-1,-6,-5.3),(2,1,1))
    r.cube("head","dark",(-1,-4,-4.7),(2,3,1))
    for side in (-1,1):
        panel(r,f"canister{side+1}","body",(side*4,6,2),(3,9,3),(0,0,side*.1),"dark")
        panel(r,f"valve{side+1}","body",(side*4,1,2),(3,1,3),material="copper")
    names=["right_hind_leg","left_hind_leg","right_front_leg","left_front_leg"]
    for i,name in enumerate(names):
        side=-1 if i%2==0 else 1
        leg=r.part(name,"root",(side*2,18,2 if i<2 else -2))
        r.cube(leg,"fiber",(-1.5,0,-1.5),(3,6,3))
        r.cube(leg,"armor",(-2,4,-2),(4,2,4))
    return r
