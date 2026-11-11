"""Geometría del arsenal remodelado. Se aplica antes de empaquetar el atlas en weapons_gen.

Conserva los pivotes de agarre, máscaras de módulos y estados móviles de los modelos originales.
No genera la Pistola Cinética ni cambia su kit de materiales compartido.
"""
import math
from gunkit import el, scaled

MELEE = {"scrap_machete", "kinetic_blade"}
EXTRA_WEAPONS = MELEE | {"kinetic_grenade"}


def box(a, b, mat="steel_dark", rot=None):
    return el(a, b, mat, rot=rot)


def sideplates(x0, x1, y0, y1, z0, z1, mat="inercita", rot=None):
    """Placas a ambos costados, con espesor propio y unión al cuerpo."""
    return [box([x0-.18,y0,z0],[x0+.12,y1,z1],mat,rot),
            box([x1-.12,y0,z0],[x1+.18,y1,z1],mat,rot)]


def ribs(x0, x1, y0, y1, zs, mat="steel", rot=None):
    return [box([x0,y0,z],[x1,y1,z+.3],mat,rot) for z in zs]


def ring(cx, cy, z, radius, material="steel", depth=.45):
    """Collar octogonal hueco; cada cara diagonal usa una rotación JSON válida."""
    out=[]
    width=2*radius*math.tan(math.pi/8)+.06
    for i in range(8):
        a=i*math.pi/4; x=cx+math.cos(a)*radius; y=cy+math.sin(a)*radius
        if i%2==0:
            sx,sy=(.22,width) if i%4==0 else (width,.22)
            rot=None
        else:
            sx,sy=(.22,width)
            rot={"axis":"z","angle":45 if i in (1,5) else -45,"origin":[x,y,z]}
        out.append(box([x-sx/2,y-sy/2,z],[x+sx/2,y+sy/2,z+depth],material,rot))
    return out


def receiver(x0,x1,y0,y1,z0,z1,mat="steel"):
    out=sideplates(x0,x1,y0,y1,z0,z1,mat)
    # Pasadores de servicio y selector: piezas sólidas encastradas, no pegatinas flotantes.
    for x in (x0-.24,x1+.1):
        for z in (z0+.4,z1-.65): out.append(box([x,y0+.3,z],[x+.14,y0+.6,z+.25],"steel_hi"))
    out.append(box([x1+.15,y0+.7,z1-1.8],[x1+.38,y0+.95,z1-.8],"copper"))
    return out


def vented(x0,x1,y0,y1,z0,z1):
    """Guardamanos abierto: dos largueros por lado y nervios separados con huecos reales."""
    out=[]
    for a,b in ((x0,x0+.3),(x1-.3,x1)):
        out += [box([a,y0,z0],[b,y0+.35,z1],"graphite"),
                box([a,y1-.35,z0],[b,y1,z1],"inercita")]
        for i in range(5):
            z=z0+i*(z1-z0-.5)/4
            out.append(box([a,y0+.25,z],[b,y1-.25,z+.45],"steel_dark"))
    return out


def remove_box(els, start, end):
    return [e for e in els if e["from"]!=start or e["to"]!=end]


def melee(kinetic):
    d={"angle":-45,"axis":"z","origin":[8,8,8]}
    def b(a,c,m): return box(a,c,m,d)
    out=[b([7.35,.5,7.4],[8.65,4.6,8.6],"rubber" if kinetic else "wood_dark"),
         b([7.15,.1,7.25],[8.85,.75,8.75],"steel"),
         b([7.4,.75,7.85],[8.6,4.8,8.15],"steel_dark")]
    for y in (1.2,2.3,3.4):
        out.append(b([7.25,y,7.35],[8.75,y+.25,8.65],"copper" if kinetic else "tape"))
        out.append(b([7.8,y,7.25],[8.2,y+.22,8.75],"brass"))
    if kinetic:
        out += [b([5.7,4.4,7.35],[10.3,5.1,8.65],"inercita"),
                b([7.1,4.7,7.15],[8.9,6.3,8.85],"steel_dark"),
                b([7.65,5.3,7.05],[8.35,6.1,8.95],"cyan")]
        # Hoja segmentada con filos escalonados y núcleo visible entre las mordazas.
        for i in range(4):
            y=6+i*2.25; half=1.2-i*.14
            out += [b([8-half,y,7.7],[7.72,y+2.1,8.3],"inercita"),
                    b([8.28,y,7.7],[8+half,y+2.1,8.3],"inercita"),
                    b([8-half-.2,y+.15,7.8],[8-half,y+2.05,8.2],"edge"),
                    b([8+half,y+.15,7.8],[8+half+.2,y+2.05,8.2],"edge"),
                    b([7.75,y-.1,7.76],[8.25,y+2.25,8.24],"cyan")]
        out += [b([7.4,15,7.74],[8.6,16,8.26],"inercita"),
                b([7.75,16,7.82],[8.25,16.7,8.18],"edge")]
        for x in (5.6,9.8):
            out += [b([x,4.8,7.3],[x+.6,6.3,8.7],"copper"),
                    b([x+.1,5.6,7.2],[x+.5,6.1,8.8],"ceramic")]
    else:
        out += [b([6.9,4.5,7.3],[9.1,5.25,8.7],"steel"),
                b([7.45,5.1,7.73],[8.8,9,8.27],"blade"),
                b([6.9,9,7.73],[8.8,13.3,8.27],"blade"),
                b([6.3,13.3,7.73],[8.6,15.5,8.27],"blade"),
                b([6.3,15.5,7.73],[8,16.2,8.27],"blade"),
                b([7.1,16.2,7.8],[7.85,16.7,8.2],"edge"),
                b([7.23,5.4,7.85],[7.45,9,8.15],"edge"),
                b([6.68,9,7.85],[6.9,13.3,8.15],"edge"),
                b([6.08,13.3,7.85],[6.3,15.8,8.15],"edge"),
                b([8.55,5.2,7.68],[8.9,13.3,8.32],"steel_dark"),
                b([8.25,6.2,7.64],[8.5,11.6,8.36],"copper")]
        for y in (6.2,7.4,8.6): out.append(b([8.55,y,7.6],[9,y+.45,8.4],"steel_dark"))
    return out


def remodel(group, name, original):
    if group in MELEE: return melee(group=="kinetic_blade")
    els=list(original); add=[]
    if group in {"assault_rifle","light_machine_gun"}:
        # El guardamanos ahora tiene huecos reales; quitar las antiguas ranuras pintadas.
        els=[e for e in els if not (e["_mats"]["east"]=="black" and e["to"][2]<0)]
    if group=="pipe_shotgun":
        hinge=next((e.get("rotation") for e in els if e["from"][2]<0 and e.get("rotation")),None)
        front=-4 if "_m2" in name or "_m3" in name or "_m6" in name or "_m7" in name else -12
        add += sideplates(6.85,9.15,11.05,13.1,6.1,8.9,"steel")
        add += ribs(6.7,9.3,11.1,11.55,[-3.5,-1.8,0,1.8,3.6],"wood",hinge)
        for x in (6.5,9.1):
            add += [box([x,11.5,5.9],[x+.4,12.5,6.6],"brass"),
                    box([x,9.3,13.5],[x+.4,11.5,14],"steel")]
        add += ribs(7.2,8.8,13.3,13.75,[front+1,front+3],"steel_dark",hinge)
        add += sideplates(7,9,9,11.8,15,20,"wood_dark")
    elif group=="bolt_rifle":
        add += receiver(7.25,8.75,12.3,13.7,4.3,7,"steel")
        add += sideplates(7.05,8.95,11.55,12.4,-8,-3.8,"wood_dark")
        add += ribs(7.1,8.9,11.1,11.65,[-7,-5.7,-4.4],"wood")
        add += sideplates(7.1,8.9,9.4,12.1,16,21,"leather")
        for z in (-13,-11): add += sideplates(7.5,8.5,12.6,13.6,z,z+.35,"steel")
    elif group=="pump_shotgun":
        front=-5 if any("_m"+str(m) in name for m in (2,3,6,7)) else -14
        add += vented(6.95,9.05,12.85,13.95,max(front+1,-11),-2)
        add += receiver(7.15,8.85,11.25,12.5,4.4,9.2)
        dz=3 if name.endswith("_pumped") else 0
        add += ribs(6.95,9.05,11.1,11.5,[z+dz for z in (-7.4,-5.8,-4.2)],"rubber")
        add += sideplates(7.1,8.9,10,11.7,15,19.7,"inercita")
    elif group=="semi_rifle":
        add += receiver(7.25,8.75,12.1,13.6,4.3,7.6,"inercita")
        add += ribs(6.9,9.1,11.5,12.1,[-7,-5.5,-4,-2.5],"wood_dark")
        add += sideplates(7.1,8.9,9.5,12.2,15.5,20,"leather")
        add += [box([7.65,13.5,-9],[8.35,13.9,-2],"steel"),
                box([8.8,12.7,7.8],[9.25,13,8.5],"brass")]
    elif group=="kinetic_smg":
        els=remove_box(els,[7.2,11.4,-2],[8.8,14,4])
        add += vented(7.05,8.95,11.4,14,-2,4)
        add += [box([7.6,12.2,-2],[8.4,13.3,4],"copper")]
        add += receiver(7.25,8.75,11.4,13.65,5,8.5,"graphite")
        add += sideplates(7.55,8.45,11.2,13,15.5,18.8,"inercita")
    elif group=="assault_rifle":
        els=remove_box(els,[7.1,12,-9],[8.9,14.1,1])
        add += vented(7,9,11.85,14.1,-9,1)
        add += receiver(7.25,8.75,12.3,13.8,2,7.2,"inercita")
        add += sideplates(7.25,8.75,10.4,12.8,16,19,"graphite")
        add += [box([7.4,10.8,-2],[8.6,12,-.6],"rubber")]
    elif group=="rail_rifle":
        els=remove_box(els,[7,10.6,2],[9,14.4,21])
        add += [box([7.2,10.6,2],[8.8,14.4,12],"graphite"),
                box([7.25,13.1,11],[8.75,14.4,21],"inercita"),
                box([7.35,10.6,12],[8.65,11.4,21],"steel_dark"),
                box([7.25,11.3,19],[8.75,13.2,21],"graphite")]
        add += receiver(7,9,11.4,13.7,3,9,"inercita")
        for z in (-12.8,-9.5,-5.5,-1.5):
            add += ribs(6.8,9.2,10.8,11.25,[z],"ceramic")
        for x0,x1 in ((6.1,6.95),(9.05,9.9)):
            add += ribs(x0,x1,11.45,13.75,[-6,-3.5,-1],"ceramic")
    elif group=="grenade_launcher":
        els=remove_box(els,[6.2,9.8,0],[9.8,13.4,6])
        els=remove_box(els,[6.6,9.4,.4],[9.4,13.8,5.6])
        els=remove_box(els,[6.15,11.3,1],[9.85,11.9,5])
        # Seis recámaras individuales: el tambor deja de ser un bloque macizo.
        for i in range(6):
            a=i*math.pi/3; x=8+math.cos(a)*1.25; y=11.6+math.sin(a)*1.25
            add.append(el([x-.52,y-.52,.1],[x+.52,y+.52,5.95],"olive",front="black"))
        add += ring(8,11.6,.2,1.85,"steel_dark")+ring(8,11.6,5.35,1.85,"steel_dark")
        add += receiver(7.2,8.8,11.5,13.6,7,9.5,"inercita")
    elif group=="pulse_cannon":
        add += ring(8,12.9,-12.6,2.6,"inercita")
        for z in (-7.8,-3.5,.5):
            add += sideplates(6.35,9.65,11.5,13.5,z,z+.5,"ceramic")
        add += receiver(7.2,8.8,11.1,13.7,5,9,"inercita")
        add += [box([7.45,10.15,-5],[8.55,11.5,-2],"rubber")]
    elif group=="revolver":
        # Misma escala compacta y desplazamiento del tambor que el modelo original.
        shift=1.9 if name.endswith("_open") else 0
        add += ring(8+shift,12.75,2.45,1.05,"steel_dark",.3)
        add += ring(8+shift,12.75,5.55,1.05,"steel_dark",.3)
        add += sideplates(7.25,8.75,11.5,13.5,6.3,8.6,"inercita")
        add += ribs(7.35,8.65,12.35,13.75,[-4.3,-2.6,-.9],"steel")
        add=scaled(add,.72,[8,11.5,10])
    elif group=="flamethrower":
        els=remove_box(els,[7,12,-10],[9,14,-5])
        add += vented(6.9,9.1,12,14,-10,-5)
        add += receiver(7,9,11.2,13.6,3,9,"steel")
        add += ring(8,13,-13.7,1.15,"copper")
        # Manómetro mecánico y conducción fijados al cajón, presentes al cambiar el depósito.
        add += [box([6.45,12,5.5],[7.1,13.5,7],"brass"),
                box([6.35,12.2,5.7],[6.45,13.3,6.8],"ceramic"),
                box([6.3,12.6,6],[6.35,12.75,6.7],"shell_red")]
    elif group=="light_machine_gun":
        els=remove_box(els,[7.2,12.3,-11],[8.8,13.9,-2])
        add += vented(7.05,8.95,12.2,14,-11,-2)
        add += receiver(7.15,8.85,11.6,14,0,9,"inercita")
        add += ribs(7.1,8.9,14.05,14.5,[.4,2,3.6,5.2,6.8,8.4],"steel")
        if not name.endswith("_mag_out"):
            add += sideplates(9,11.2,9.3,12.3,1.2,5.8,"graphite")
    elif group=="kinetic_sniper":
        add += receiver(7.25,8.75,11.9,13.6,4.2,7,"inercita")
        add += sideplates(7.15,8.85,11.7,12.7,-5.8,2.5,"inercita")
        for z in (-12.5,-10.5,-8.5): add += ring(8,13.15,z,.55,"steel_dark",.3)
        add += ribs(7,9,9.4,12.1,[14.5,18.5],"steel")
        add += sideplates(7.05,8.95,9.4,11.7,15,20.5,"graphite")
        add += ring(8,15.4,1,1,"steel",.35)+ring(8,15.4,11.8,1,"steel",.35)
    elif group=="rocket_launcher":
        add += ring(8,12.8,-12.65,1.65,"steel_dark")+ring(8,12.8,16,1.65,"steel_dark")
        add += sideplates(6.8,9.2,11.8,13.8,4,8,"inercita")
        for z in (-7,-5,-3,11.5,13.5): add += ring(8,12.8,z,1.35,"olive",.35)
        add += [box([6,13.6,3],[6.8,15.4,6.5],"graphite"),
                el([6.05,13.8,2.8],[6.75,15.2,3.1],"steel",front="cyan")]
    elif group=="arc_caster":
        # Jaula conductora alrededor del tubo y faldones de porcelana delante.
        for z in (-7.8,-7.2,-6.6): add += ring(8,13,z,.85,"ceramic",.25)
        for x in (6.8,9): add += [box([x,12.6,-5.8],[x+.2,13.4,1],"copper")]
        add += receiver(7,9,11.5,13.5,3,8.8,"inercita")
        add += sideplates(6.45,9.55,11.8,12.6,-10.5,-8.8,"ceramic")
    elif group=="minigun":
        add += ring(8,13.1,-13.6,2.35,"inercita")
        add += ring(8,13.1,-3,2.35,"steel_dark")
        for z in (.4,1.8,3.2,4.6,6): add += sideplates(6.35,9.65,11.9,14.8,z,z+.3,"steel")
        # Se traslada junto al arma; las nuevas carcasas son fijas mientras giran los seis cañones.
        for e in add:
            for key in ("from","to"): e[key]=[v+d for v,d in zip(e[key],(0,-8.4,6))]
            if "rotation" in e: e["rotation"]["origin"]=[v+d for v,d in zip(e["rotation"]["origin"],(0,-8.4,6))]
    elif group=="kinetic_grenade":
        # Granada de mano: segmentos de carcasa, espoleta, palanca y anilla de seguridad.
        els=[box([6.7,3.2,6.7],[9.3,8.3,9.3],"steel_dark"),
             box([6.8,2.8,6.8],[9.2,3.4,9.2],"inercita"),
             box([7.3,8.1,7.3],[8.7,9.5,8.7],"brass"),
             box([7.5,9.4,7.5],[8.5,9.9,8.5],"cyan"),
             box([7.6,9.7,7.1],[9.8,10.1,8.9],"steel"),
             box([9.4,4.5,7.4],[9.8,9.9,8.6],"steel")]
        for y in (3.5,5.1,6.7):
            for z in (6.55,8.05): add += sideplates(6.5,9.5,y,y+1.25,z,z+1.35,"olive")
        add += [box([6.4,8.8,7.7],[7.6,9.05,8.3],"steel_hi"),
                box([6.1,8.6,7.5],[6.4,9.6,8.5],"steel"),
                box([5.3,8.6,7.5],[5.6,9.6,8.5],"steel"),
                box([5.3,8.35,7.5],[6.4,8.6,8.5],"steel"),
                box([5.3,9.6,7.5],[6.4,9.85,8.5],"steel")]
    return els+add
