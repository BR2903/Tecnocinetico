"""Arsenal diseñado desde cero: cada función construye toda la geometría desde una lista vacía.

Solo comparte materiales, coordenadas de agarre y contratos de estados con el juego.
No importa geometrías anteriores, ni añade piezas sobre modelos antiguos. La pistola queda fuera.
"""
import math
from gunkit import el, scaled

EXTRA_WEAPONS={"scrap_machete","kinetic_blade","kinetic_grenade"}
GRIP={"axis":"x","angle":-22.5,"origin":[8,11,11]}
DIAGONAL={"axis":"z","angle":-45,"origin":[8,8,8]}


class Assembly:
    def __init__(self): self.parts=[]

    def box(self,x,y,z,w,h,d,mat="steel_dark",rot=None,front=None):
        self.parts.append(el([x,y,z],[x+w,y+h,z+d],mat,rot=rot,front=front))

    def pin(self,x,y,z,rot=None): self.box(x,y,z,.23,.23,.23,"steel_hi",rot)

    def tube(self,x,y,z,length,r=.6,mat="steel",rot=None):
        """Tubo hueco. Los grupos basculantes usan cuatro caras y una única rotación de bisagra."""
        if rot:
            for dx,dy,w,h in ((-r,-r,.18,2*r),(r-.18,-r,.18,2*r),(-r,-r,2*r,.18),(-r,r-.18,2*r,.18)):
                self.box(x+dx,y+dy,z,w,h,length,mat,rot)
            return
        face=2*r*math.tan(math.pi/8)+.035
        for i in range(8):
            a=i*math.pi/4; px=x+math.cos(a)*r; py=y+math.sin(a)*r
            if i%2:
                pose={"axis":"z","angle":45 if i in (1,5) else -45,"origin":[px,py,z]}
                w,h=.16,face
            else:
                pose=None; w,h=(.16,face) if i%4==0 else (face,.16)
            self.box(px-w/2,py-h/2,z,w,h,length,mat,pose)

    def cylinder(self,x,y,z,length,r,mat,rot=None):
        """Sección biselada de tres volúmenes, útil en piezas pequeñas sin gastar ocho caras por aro."""
        self.box(x-r*.7,y-r,z,r*1.4,r*2,length,mat,rot,front="black")
        self.box(x-r,y-r*.7,z,r*2,r*1.4,length,mat,rot,front="black")

    def handle(self,wood=False,z=10.4):
        self.box(7.28,7.7,z,1.44,3.8,2.1,"wood_dark" if wood else "rubber",GRIP)
        for y in (8.2,9.1,10): self.box(7.19,y,z-.05,1.62,.18,2.2,"tape" if wood else "graphite",GRIP)
        self.box(7.15,7.45,z-.1,1.7,.4,2.3,"steel",GRIP)
        # Guardamonte abierto y gatillo, ligados al mismo cajón.
        self.box(7.7,9.35,7.4,.6,.3,3.2)
        self.box(7.7,9.6,7.4,.6,1.45,.3)
        self.box(7.85,9.8,8.8,.3,1.1,.28,"steel_hi")

    def shell(self,z,length=6,mat="inercita",width=2.2,y=11.2,height=2.6):
        self.box(8-width/2,y,z,width,height,length,mat)
        for x in (8-width/2-.12,8+width/2):
            self.box(x,y+.35,z+.4,.12,height-.7,length-.8,"steel_dark")
            for pz in (z+.65,z+length-.9): self.pin(x,y+.7,pz)

    def sights(self,front,back,top=14.1):
        self.box(7.75,13,front,.5,top-13,.5,"steel_dark")
        self.box(7.8,13,back,.4,top-13,.7,"steel_dark")
        self.box(7.5,top-.15,back,1,.15,.7,"steel_dark")
        self.box(7.86,top,front,.28,.75,.5,"steel_hi")
        self.box(7.5,top,back,.25,.65,.7)
        self.box(8.25,top,back,.25,.65,.7)

    def rail(self,z,length,y=14):
        self.box(7.5,y,z,1,.25,length)
        for i in range(int(length/1.5)):
            self.box(7.35,y+.22,z+i*1.5,1.3,.2,.55,"steel")

    def stock(self,z,end,skeleton=True,wood=False):
        if skeleton:
            self.box(7.55,12,z,.9,.7,end-z,"steel")
            self.box(7.55,9.8,z+1,.9,.55,end-z-1,"wood_dark" if wood else "graphite")
            self.box(7.55,10.2,z+1,.9,1.9,.55,"wood_dark" if wood else "steel")
            self.box(7.45,10.3,end-1.3,1.1,2.3,1.1,"wood" if wood else "inercita")
        else:
            self.box(7.25,10,z,1.5,2.8,(end-z)*.55,"wood" if wood else "graphite")
            self.box(7,9.1,z+(end-z)*.45,2,3.7,(end-z)*.55,"wood_dark" if wood else "inercita")
        self.box(6.9,8.9,end,2.2,4.2,.7,"rubber")
        self.box(7.2,12.65,end-4.5,1.6,.45,3.7,"leather" if wood else "rubber")

    def magazine(self,z,y=6.8,length=3,mat="graphite"):
        self.box(7.35,y,z,1.3,11.3-y,length,mat)
        for zz in (z+.35,z+length-.6): self.box(7.25,y+.5,zz,1.5,10.8-y,.22,"steel_dark")
        self.box(7.15,y-.3,z-.1,1.7,.45,length+.2,"steel")

    def scope(self,z,length=8,y=15.3):
        for zz in (z+2,z+length-2): self.box(7.6,13.7,zz,.8,y-13.7,.55,"steel")
        self.cylinder(8,y,z,length,.55,"graphite")
        self.tube(8,y,z-1.2,1.6,.95,"steel")
        self.box(7.35,y-.65,z-.08,1.3,1.3,.1,"cyan")
        self.box(7.7,y+.45,z+length*.6,.6,.7,.7,"steel_hi")

    def cage(self,z,length,r=1.1,y=13,mat="inercita"):
        for x in (8-r,8+r-.2):
            for yy in (y-r,y+r-.2): self.box(x,yy,z,.2,.2,length,mat)
        for zz in (z,z+length*.5,z+length-.35):
            self.box(8-r,y-r,zz,2*r,.2,.35,"steel")
            self.box(8-r,y+r-.2,zz,2*r,.2,.35,"steel")
            for x in (8-r,8+r-.2): self.box(x,y-r+.1,zz,.2,2*r-.2,.35,"steel_dark")


def pipe_shotgun(mods,reload,spin):
    a=Assembly(); front=-4.8 if mods&2 else -12.6
    hinge={"axis":"x","angle":-22.5,"origin":[8,11.7,6.2]} if reload else None
    # Cañones superpuestos y bastidor basculante; culata de tubos y tablón recuperado.
    for yy in (11.8,13.15): a.tube(8,yy,front,6.2-front,.58,"rust",hinge)
    a.box(7.25,10.5,-1,1.5,.7,6.8,"wood_dark",hinge)
    for z in (front+1,1.4,5.5): a.box(7.18,11.1,z,1.64,2.9,.4,"steel",hinge)
    a.box(7.87,13.75,front+.2,.26,.25,6-front,"steel",hinge)
    if mods&1:
        for yy in (11.8,13.15): a.tube(8,yy,front-.6,.65,.65,"copper",hinge)
    a.shell(6.1,4.6,"steel",2.1,11,2.7); a.handle(True)
    a.stock(11.1,20.2,True,True)
    a.box(7.15,11.7,14,1.7,1.2,5.5,"wood")
    if mods&4:
        a.box(9,10,14,.35,2.1,4.9,"leather")
        for z in (14.2,15.8,17.4): a.cylinder(9.55,11,z,1.1,.32,"shell_red")
    return a.parts


def bolt_rifle(mods,reload,spin):
    a=Assembly(); a.tube(8,13,-15.5,18,.4,"inercita" if mods&4 else "steel_dark")
    a.box(7.1,11.15,-8,1.8,1.3,10.8,"wood")
    a.box(7.45,12.5,-5.5,1.1,.8,6,"wood_dark")
    for z in (-7,-.5): a.box(7,11.15,z,2,2.4,.4,"steel")
    a.shell(2.5,8.5,"steel",1.9,11.3,2.4); a.handle(True)
    # Culata con hueco para el pulgar: largueros de madera y cantonera ancha.
    a.stock(11.2,23,True,True)
    a.box(7.1,9,18.7,1.8,3.6,4.1,"wood")
    shift=2.6 if reload else 0
    a.cylinder(8.35,13.5,6+shift,4,.25,"brass" if mods&2 else "steel_hi")
    a.box(8.35,12.9,8.8+shift,1.7,.3,.4,"steel_hi")
    a.box(9.6,12.4,8.6+shift,.6,.7,.7,"copper" if mods&2 else "steel")
    a.sights(-13.4,8.8,13.7)
    if mods&1: a.scope(2.8,8,15)
    if mods&4: a.tube(8,13,-16,.9,.65,"steel_hi")
    return a.parts


def pump_shotgun(mods,reload,spin):
    a=Assembly(); front=-4.6 if mods&2 else -12.8
    # Bullpup compacto: recámara y cartuchos detrás del agarre.
    a.tube(8,13,front,14-front,.55,"steel_dark")
    a.cylinder(8,11.65,front+1,12-front,.42,"steel")
    a.shell(3.8,13.1,"graphite",2.4,10.7,3.4)
    a.box(6.7,10.4,14.5,2.6,3.9,3.1,"inercita")
    a.box(6.6,10.2,17.6,2.8,4.1,.7,"rubber")
    a.handle(False); dz=2.5 if reload else 0
    a.box(7.05,10.6,-5.8+dz,1.9,1.5,4.2,"rubber")
    for z in (-5.5,-4.4,-3.3): a.box(6.95,10.55,z+dz,2.1,1.6,.28,"graphite")
    a.cage(max(front+1,-9.5),8.8 if not mods&2 else 3,1.05,13.2)
    a.rail(2,12,14.1); a.sights(front+1,12.3,14.35)
    if mods&1: a.tube(8,13,front-.7,.8,.72,"copper")
    if mods&4:
        for z in (9.5,11.2,12.9): a.cylinder(6.5,12,z,1,.32,"shell_red")
    return a.parts


def semi_rifle(mods,reload,spin):
    a=Assembly(); a.tube(8,13,-14.6,18.1,.43,"inercita" if mods&4 else "steel")
    a.cylinder(8,14,-10,13,.25,"steel_dark")
    a.cage(-9,10,1.05,12.8,"olive")
    a.shell(1.2,9.7,"olive",2.2,11.3,2.8); a.handle(False)
    a.stock(11.2,20,True)
    a.box(7.15,11.65,15,1.7,1.1,4.7,"olive")
    if not reload: a.magazine(3.6,8.1,3.8,"steel")
    a.box(8.9,13.2,6.2,.8,.35,1,"brass" if mods&2 else "steel_hi")
    a.rail(2.3,7.5,14.15); a.sights(-9.7,8.5,14.4)
    if mods&1: a.scope(.3,8.2,15.45)
    if mods&4: a.tube(8,13,-15.8,1.4,.65,"steel_dark")
    return a.parts


def kinetic_smg(mods,reload,spin):
    a=Assembly(); a.tube(8,13,-6.2,6,.45,"steel_dark")
    a.shell(-.8,11.5,"inercita",2.2,11.1,2.7); a.handle(False)
    a.cage(-5.6,5.5,.85,13,"copper")
    a.box(7.2,9.9,-.6,1.6,1.6,2.2,"graphite")
    if not reload: a.magazine(.2,6.1,2.5,"steel")
    # Dos barras telescópicas y cantonera plegada, silueta de PDW.
    for x in (6.75,8.9): a.box(x,12,7,.35,.45,10.4,"steel_hi")
    a.box(6.7,10.7,17.1,2.6,2.8,.65,"rubber")
    a.box(7.4,13.8,5.5,1.2,.55,2.4,"graphite")
    a.box(7.65,14,5.3,.7,.25,.2,"cyan")
    a.sights(-4.5,8.2,13.85)
    return a.parts


def assault_rifle(mods,reload,spin):
    a=Assembly(); a.tube(8,13,-15,19,.4,"steel_dark")
    a.tube(8,13,-15.7,1.3,.65,"steel")
    a.cage(-10.2,11.2,1.15,13,"graphite")
    a.shell(1,9.8,"inercita",2.25,11.2,2.8)
    a.box(7.5,10.2,2.8,1,1.1,3.3,"steel"); a.handle(False)
    a.stock(11.4,21.4,True)
    a.box(7.1,12.3,16.6,1.8,.9,4.1,"graphite")
    if not reload:
        a.magazine(3.1,7.5,2.9)
        a.box(7.3,6,4.1,1.4,1.8,2.9,"graphite",{"axis":"x","angle":-22.5,"origin":[8,8,4]})
    a.rail(-10,19.5,14.2); a.sights(-9,8.2,14.65)
    a.box(8.95,12.3,3,.24,.6,2.7,"black")
    a.box(8.95,12.25,7,.6,.3,1.1,"steel_hi")
    return a.parts


def rail_rifle(mods,reload,spin):
    a=Assembly(); a.shell(2,10.3,"inercita",2.4,11.1,3.2); a.handle(False)
    # Acelerador abierto en horquilla: carriles laterales y bus inferior.
    for x in (6.5,8.9):
        a.box(x,12,-15.4,.6,2,17.7,"inercita")
        a.box(x+.15,12.4,-15.2,.3,1.2,17.4,"cyan")
    a.box(7.2,10.8,-12.5,1.6,.5,15,"steel")
    for z in (-12,-7.2,-2.4):
        a.box(6.35,10.7,z,3.3,.45,.65,"ceramic")
        a.box(6.35,10.8,z,.3,3.4,.65,"steel_dark")
        a.box(9.35,10.8,z,.3,3.4,.65,"steel_dark")
    # Banco de condensadores a un solo costado, con puentes al receptor.
    a.box(9,11.3,-1.5,1.4,2.7,8,"graphite")
    for z in (-1,1.2,3.4):
        a.box(9.2,11.5,z,1,2.2,1.4,"copper")
        a.box(9.15,12.2,z-.1,1.1,.35,1.6,"ceramic")
    a.stock(12.3,20.5,True); a.box(7,12.2,16.5,2,1.5,3.6,"inercita")
    if not reload: a.magazine(13,8.5,3.4,"copper")
    a.scope(3,5.5,15.25)
    return a.parts


def grenade_launcher(mods,reload,spin):
    a=Assembly(); hinge={"axis":"x","angle":-22.5,"origin":[8,10.1,1]} if reload else None
    a.tube(8,13.1,-10,10.2,.95,"steel_dark",hinge)
    a.box(6.8,11.8,-5,2.4,.5,5.7,"inercita",hinge)
    a.box(7.35,9.6,-4.6,1.3,2.3,1.7,"rubber",hinge)
    # Tambor de seis recámaras, caras delanteras y eje central visibles al abrir.
    for i in range(6):
        t=i*math.pi/3; x=8+math.cos(t)*1.5; y=11.65+math.sin(t)*1.5
        a.cylinder(x,y,.8,5.2,.58,"olive")
    a.cylinder(8,11.65,.7,5.4,.4,"steel_hi")
    a.tube(8,11.65,1, .45,2.12,"steel"); a.tube(8,11.65,5.35,.45,2.12,"steel")
    a.box(7.35,13.9,.4,1.3,.6,6,"inercita")
    a.shell(6.1,5,"steel",2.2,11.1,2.8); a.handle(False)
    a.stock(11.3,18.3,True)
    for y in (13.3,14.1,14.9): a.box(6.2,y,-1.6,.25,.15,2.1,"brass",hinge)
    for z in (-1.6,.3): a.box(6.2,12.9,z,.25,2.3,.2,"steel",hinge)
    return a.parts


def pulse_cannon(mods,reload,spin):
    a=Assembly(); a.shell(1.3,10,"graphite",2.5,10.8,3.6); a.handle(False)
    # Emisor de campana: tres anillos abiertos alrededor del canal de enfoque.
    for z,r in ((-12.2,2.55),(-9.8,2.1),(-7.7,1.65)):
        a.tube(8,13,z,.65,r,"copper")
        a.box(8-r,12.86,z+.25,r*2,.28,.3,"inercita")
        a.box(7.86,13-r,z+.25,.28,r*2,.3,"inercita")
    for x in (6.7,9): a.box(x,12,-11.6,.3,2,13.4,"inercita")
    a.cylinder(8,13,-7.5,9,.65,"ceramic")
    a.box(7.5,12.5,-11.8,1,1,5,"cyan")
    for z in (-5,-3,-1): a.box(6.8,14,z,2.4,.6,.4,"steel")
    a.stock(11.5,17.6,False)
    for x in (6.4,8.9): a.cylinder(x,12.2,12,4.5,.5,"copper")
    if not reload: a.magazine(2.8,8.3,3.7,"cyan")
    a.box(7.5,14.4,5,1,.8,3.2,"steel")
    return a.parts


def revolver(mods,reload,spin):
    a=Assembly(); shift=2.5 if reload else 0
    a.tube(8,13,-6.8,9.3,.52,"inercita")
    a.box(7.5,13.6,-6.3,1,.35,10.8,"steel")
    a.cylinder(8,12.2,-5.6,7.5,.3,"steel_dark")
    for i in range(6):
        t=i*math.pi/3
        a.cylinder(8+shift+math.cos(t)*.7,12.5+math.sin(t)*.7,2.6,3.5,.42,"steel")
    a.box(7.8,11.4,3.5,shift+.4,.35,2.9,"steel_hi")
    a.shell(6.3,4.4,"inercita",1.9,11,2.9); a.handle(True)
    a.box(7.7,13.9,9,.6,.7,1,"steel_hi")
    a.sights(-5.7,8.5,13.95)
    return scaled(a.parts,.76,[8,11.5,10])


def flamethrower(mods,reload,spin):
    a=Assembly(); a.tube(8,13,-13,15,.53,"steel_dark")
    a.tube(8,13,-14,.9,.85,"copper")
    a.cage(-10.5,8,1.2,13,"steel")
    a.box(7.78,11.5,-13.7,.44,.4,3.5,"copper")
    a.shell(1.5,10,"olive",2.2,11,3); a.handle(False)
    a.stock(11.8,17.4,True)
    # Dos botellas verticales sustituibles, cada una con casquete y brida.
    if not reload:
        for x in (6.65,8.35):
            a.box(x,6.1,2.1,1,4.9,4.5,"shell_red")
            for y in (6.1,9.7): a.box(x-.1,y,2,1.2,.4,4.7,"steel")
    a.box(6.4,10.7,2,.4,.4,6.3,"copper")
    a.box(6.4,10.9,7.7,.4,2.3,.4,"copper")
    a.box(6.25,12.4,7.1,.7,1.5,1.5,"brass")
    a.box(6.1,12.6,7.3,.15,1.1,1.1,"ceramic")
    a.box(6.03,13,7.5,.1,.2,.65,"shell_red")
    a.box(7.6,14,4,.8,.7,.8,"brass"); a.box(7.1,14.7,3.7,1.8,.25,1.4,"shell_red")
    return a.parts


def light_machine_gun(mods,reload,spin):
    a=Assembly(); a.tube(8,13,-15.4,18.2,.45,"steel_dark")
    a.cage(-11.8,10.3,1.1,13,"steel")
    a.shell(-1.3,12.7,"olive",2.6,11,3); a.handle(False)
    a.box(6.8,13.9,.2,2.4,.5,8.8,"steel")
    for z in (1.5,6.7): a.box(7.65,14.3,z,.7,1.4,.4,"steel")
    a.box(7.55,15.4,1.5,.9,.45,5.6,"rubber")
    a.stock(11.5,22,True)
    # Alimentación lateral izquierda, caja extraíble y eslabones visibles.
    if not reload:
        a.box(4.6,7.6,1.8,2.1,4.8,5,"olive")
        a.box(4.45,8.2,2.2,.2,3.4,4.2,"graphite")
        for x in (5,5.6,6.2): a.box(x,12,2.7,.35,.35,1.8,"brass")
    for x in (6.8,8.8):
        a.box(x,10.8,-12,.4,.45,8,"steel")
        a.box(x-.15,10.5,-4.6,.7,.45,1.1,"rubber")
    a.box(6.65,11.1,-10.8,2.7,1,.55,"steel_dark")
    a.sights(-10.5,9.4,14.1)
    return a.parts


def kinetic_sniper(mods,reload,spin):
    a=Assembly(); a.tube(8,13,-15.3,18.8,.42,"inercita")
    a.tube(8,13,-16,1.3,.8,"steel")
    a.cage(-7,9.8,1.05,12.6,"graphite")
    a.shell(3.2,8,"inercita",2.1,11.3,2.6); a.handle(False)
    a.stock(11.5,23,True)
    a.box(7.1,13,18,1.8,.65,4.2,"rubber")
    for z in (18.3,21): a.box(7.65,12.5,z,.7,.65,.4,"steel_hi")
    a.scope(-.5,12.4,15.7)
    for x in (6.7,8.8): a.box(x,10.9,-10.6,.5,.5,6.2,"steel")
    a.box(6.65,11.2,-5.5,2.7,.6,.55,"steel_dark")
    if not reload: a.magazine(5,8.8,2.6,"graphite")
    dz=2.3 if reload else 0
    a.box(8.9,12.75,7+dz,1.2,.3,.45,"steel_hi")
    a.box(9.75,12.2,6.85+dz,.6,.75,.75,"copper")
    return a.parts


def rocket_launcher(mods,reload,spin):
    a=Assembly(); a.tube(8,12.8,-12,27,1.3,"olive")
    for z in (-12.3,14.8): a.tube(8,12.8,z,.9,1.65,"steel")
    for z in (-7,3,11): a.tube(8,12.8,z,.45,1.4,"steel_dark")
    a.box(7,10.8,9.5,2,.6,4.6,"rubber"); a.handle(False)
    for z in (9.8,13): a.box(7.5,11.3,z,1,.5,.55,"steel_dark")
    a.box(7.5,9.5,-1.5,1,2.3,1.7,"rubber")
    a.box(6,13.8,1.4,1.1,1.5,6,"graphite")
    a.box(6.05,14,1.15,1,1.1,.25,"cyan")
    a.box(6.1,13.2,3.4,.7,.6,2,"steel")
    a.box(7.5,14,2.5,1,1.5,.4,"steel"); a.box(7.5,14,7.5,1,1.5,.4,"steel")
    a.box(7.5,15.1,2.5,1,.5,5.4,"rubber")
    if not reload:
        a.cylinder(8,12.8,-12.4,1.4,.85,"shell_red")
        a.cylinder(8,12.8,-13,.65,.5,"steel_hi")
    return a.parts


def arc_caster(mods,reload,spin):
    a=Assembly(); a.shell(2,9.2,"inercita",2.3,11,3); a.handle(False)
    a.stock(11.5,17,True)
    # Dos electrodos opuestos y bobina visible en una jaula industrial.
    a.cage(-7,9,1.3,13,"copper")
    a.cylinder(8,13,-6.8,8.6,.7,"glass")
    for z in (-6.5,-5.2,-3.9,-2.6,-1.3,0): a.tube(8,13,z,.25,.84,"copper")
    for yy in (11.4,14.3):
        a.box(7.5,yy,-12.3,1,.45,5.6,"copper")
        a.box(7.35,yy-.2,-8.4,1.3,.85,1.2,"ceramic")
        a.box(7.6,yy-.05,-12.65,.8,.55,.4,"cyan")
    a.box(7.7,12.75,-10.8,.6,.5,3.8,"cyan")
    if not reload: a.magazine(3.3,8.1,3.5,"cyan")
    for z in (3,7):
        a.box(7.6,14,z,.8,1.1,.7,"ceramic")
        a.box(7.45,15,z-.15,1.1,.25,1,"brass")
    a.box(7.85,15.2,3,.3,.2,4.8,"copper")
    return a.parts


def minigun(mods,reload,spin):
    a=Assembly(); rotation=None if spin==0 else {"axis":"z","angle":22.5 if spin==1 else -22.5,"origin":[8,5.1,-5]}
    # Modelo construido directamente a la altura de porte, sin trasladar una geometría antigua.
    for i in range(6):
        t=i*math.pi/3
        a.cylinder(8+math.cos(t)*1.3,5.1+math.sin(t)*1.3,-12.5,12,.38,"steel_dark",rotation)
    a.tube(8,5.1,-11.7,.6,1.9,"steel"); a.tube(8,5.1,-2.1,.7,1.9,"copper")
    a.shell(-.4,9.8,"inercita",3.4,3,4.2)
    for x in (6.1,9.7):
        for z in (.5,2,3.5,5,6.5): a.box(x,3.4,z,.2,3.3,.4,"steel")
    a.box(6.6,3.5,9.4,2.8,3.2,4,"olive")
    for z in (1.5,7): a.box(7.5,7,z,1,2.3,.7,"steel")
    a.box(7.35,9,1.2,1.3,.8,6.8,"rubber")
    a.box(7.8,8,5.4,.4,1.1,1,"shell_red")
    if not reload:
        a.box(6.1,.3,1.6,3.8,2.8,5.6,"olive")
        a.box(6,.9,2,4,.4,4.8,"steel")
        for z in (.1,.8,1.5): a.box(7.5,2.8,z,1,.4,.45,"brass")
    return a.parts


def scrap_machete(mods,reload,spin):
    a=Assembly(); r=DIAGONAL
    a.box(7.35,.3,7.45,1.3,4.8,1.1,"wood_dark",r)
    a.box(7.1,.1,7.3,1.8,.6,1.4,"steel",r)
    for y in (1,2.2,3.4): a.box(7.25,y,7.35,1.5,.4,1.3,"tape",r)
    a.box(7.8,1.1,7.25,.4,.3,1.5,"brass",r)
    a.box(6.8,4.8,7.3,2.4,.6,1.4,"steel",r)
    # Hoja de despiece ancha, punta recortada y lomo dentado; cambia toda la silueta.
    for x,y,w,h in ((7.1,5.4,1.8,3.7),(6.4,9.1,2.5,3.9),(5.9,13,3,2.7),(6.7,15.7,2.2,.7)):
        a.box(x,y,7.76,w,h,.48,"blade",r)
        a.box(x-.22,y+.1,7.86,.22,h-.2,.28,"edge",r)
    a.box(8.6,5.4,7.68,.4,9.8,.64,"steel_dark",r)
    for y in (6.4,7.7,9,10.3): a.box(8.85,y,7.64,.45,.5,.72,"steel_dark",r)
    a.box(7.25,11.7,7.7,.9,.4,.6,"rust",r)
    return a.parts


def kinetic_blade(mods,reload,spin):
    a=Assembly(); r=DIAGONAL
    a.box(7.45,.5,7.4,1.1,4.2,1.2,"rubber",r)
    a.box(7.1,.1,7.1,1.8,.65,1.8,"inercita",r)
    for y in (1.2,2.4,3.6): a.box(7.3,y,7.25,1.4,.22,1.5,"copper",r)
    a.box(6.25,4.5,7.25,3.5,.75,1.5,"inercita",r)
    a.box(6,4.8,7.4,.5,1.5,1.2,"ceramic",r)
    a.box(7.3,4.9,7.1,1.4,1.4,1.8,"copper",r)
    # Sable asimétrico: espina de inercita y filo de energía con soportes separados.
    a.box(8.15,5.7,7.7,.65,9.5,.6,"inercita",r)
    a.box(7.15,5.8,7.78,1,9.3,.44,"cyan",r)
    for y in (6.2,8.8,11.4,14):
        a.box(7.8,y,7.6,1.1,.5,.8,"steel",r)
    a.box(6.9,6,7.9,.25,8.8,.2,"edge",r)
    a.box(7.35,15.1,7.75,1.45,1,.5,"inercita",r)
    a.box(7.65,16.1,7.84,.75,.8,.32,"edge",r)
    return a.parts


def kinetic_grenade(mods,reload,spin):
    a=Assembly(); a.box(6.8,3.2,6.8,2.4,4.6,2.4,"steel_dark")
    for y in (3.4,4.9,6.4):
        for x in (6.5,8):
            for z in (6.5,8): a.box(x,y,z,1.35,1.15,1.35,"olive")
    a.box(7.2,7.7,7.2,1.6,1.4,1.6,"brass")
    a.box(7.5,9,7.5,1,.5,1,"cyan")
    a.box(7.6,9.4,7.4,2.2,.35,1.2,"steel")
    a.box(9.4,4.5,7.5,.4,5.1,1,"steel")
    a.box(6.3,8.3,7.8,1.2,.25,.4,"steel_hi")
    for x in (5.2,6.1): a.box(x,8,7.6,.25,1.3,.8,"steel")
    for y in (7.8,9.3): a.box(5.2,y,7.6,1.15,.25,.8,"steel")
    return a.parts


BLUEPRINTS={name:globals()[name] for name in (
    "pipe_shotgun","bolt_rifle","pump_shotgun","semi_rifle","kinetic_smg","assault_rifle",
    "rail_rifle","grenade_launcher","pulse_cannon","revolver","flamethrower","light_machine_gun",
    "kinetic_sniper","rocket_launcher","arc_caster","minigun","scrap_machete","kinetic_blade","kinetic_grenade")}


def build_weapon(name,mods=0,reload=False,spin=0):
    return BLUEPRINTS[name](mods,reload,spin)
