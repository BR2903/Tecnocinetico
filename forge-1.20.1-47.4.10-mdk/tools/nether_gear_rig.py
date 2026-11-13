"""Geometría local a cuerpo/cabeza/pierna y extensión UV del equipo puesto.
No registra renderizadores. Las raíces cooler/filter/anchor siguen el contrato de GearLayer.
"""
import copy,math,re
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parent.parent
JAVA=ROOT/'src/main/java/com/bryanamaya/tecnocinetico/client/WornGearModel.java'
COLORS={'steel':(71,79,79),'dark':(31,37,37),'tank':(167,180,177),'coolant':(57,129,143),
        'copper':(145,87,46),'rubber':(34,39,37),'ceramic':(159,153,124),'vent':(63,69,64),
        'lead':(70,69,77),'hazard':(169,134,55),'led':(85,194,203),'amber':(218,145,58),
        'slag_plate':(83,59,43),'forge_edge':(129,122,99),'pyroxite':(226,117,46)}


def geometry():
    parts={key:[] for key in ('cooler','filter','anchor')}
    def b(part,x,y,z,w,h,d,mat):parts[part].append({'from':[x,y,z],'size':[w,h,d],'mat':mat})
    # Mochila: soporte que une la espalda desnuda y los raíles de la armadura, depósitos y bomba.
    for x in (-2.8,2.2):
        for y in (2.2,8.4):b('cooler',x,y,2.12,.6,.65,3.8,'dark')
    b('cooler',-4.2,.7,5.82,8.4,9.8,.45,'steel')
    for x in (-4.05,1.45):
        b('cooler',x,1.25,6.3,2.6,8.1,2.7,'tank')
        b('cooler',x+.45,.55,6.7,1.7,.75,1.8,'steel')
        b('cooler',x+.25,.25,6.5,2.1,.4,2.2,'dark')
        b('cooler',x+.82,2.05,9.02,.95,5.9,.22,'coolant')
        for y in (3.1,7.6):b('cooler',x-.08,y,9.04,2.76,.4,.22,'rubber')
        b('cooler',x+.2,9.28,6.5,2.2,.55,2.3,'steel')
    b('cooler',-1.35,1.8,6.35,2.7,6.4,1.7,'dark')
    for i in range(7):b('cooler',-1.7,2+i*.8,7.85,3.4,.23,.85,'steel')
    # Serpentín continuo, con codos entre tramos y sin cajas coplanares superpuestas.
    for i in range(5):
        y=2.1+i*1.15;b('cooler',-1.5,y,8.78,3,.38,.38,'copper')
        if i<4:b('cooler',1.12 if i%2==0 else -1.5,y+.38,8.78,.38,.77,.38,'copper')
    b('cooler',-1.45,8.35,6.45,2.9,2,2.7,'dark')
    b('cooler',-.75,8.65,9.18,1.5,1.2,.3,'steel')
    b('cooler',-.45,8.88,9.51,.9,.55,.12,'led')
    b('cooler',-4.25,10.35,6.45,8.5,.45,2.6,'steel')
    # Respirador: sello facial, válvula, dos cartuchos y arnés compatible con casco grueso.
    b('filter',-2.15,-3.2,-6.35,4.3,3.25,2.5,'rubber')
    b('filter',-1.8,-2.95,-6.83,3.6,2.6,.5,'steel')
    b('filter',-.78,-2.2,-7.18,1.56,1.45,.36,'vent')
    b('filter',-.3,-1.83,-7.31,.6,.6,.14,'dark')
    for x in (-4.05,2.05):
        b('filter',x,-2.7,-7.85,2,2.5,2,'ceramic')
        b('filter',x-.12,-2.55,-8.22,2.24,2.2,.38,'dark')
        b('filter',x+.18,-2.25,-8.48,1.64,1.6,.27,'vent')
        b('filter',x+.18,-.18,-7.7,1.6,.32,1.7,'steel')
        b('filter',x+.18,-2.95,-7.7,1.6,.3,1.7,'steel')
    b('filter',3.35,-1.2,-8.62,.35,.38,.14,'amber')
    for x in (-5.18,4.86):b('filter',x,-3.2,-5.15,.32,.6,10.3,'rubber')
    b('filter',-4.86,-3.2,4.86,9.72,.6,.32,'rubber')
    for x in (-4.86,3.95):b('filter',x,-2.95,-5.5,.9,.9,.55,'steel')
    # Una bota, simétrica: los pistones se sitúan detrás, para no invadir la otra pierna.
    b('anchor',-2.22,7.1,-3.73,4.44,3.8,.55,'steel')
    b('anchor',-2.2,7.2,2.85,4.4,3.8,.55,'dark')
    b('anchor',-2.35,11.02,-3.9,4.7,1.6,7.45,'lead')
    b('anchor',-1.95,10.45,-3.96,3.9,.55,2.4,'steel')
    b('anchor',-.65,6.0,3.5,1.3,4.55,1.15,'dark')
    b('anchor',-.35,6.35,4.68,.7,3.55,.55,'steel')
    b('anchor',-.9,10.0,3.62,1.8,1.2,1.5,'copper')
    for x in (-1.95,1.4):b('anchor',x,8.1,3.42,.55,2.5,.6,'steel')
    for x in (-1.75,1.05):b('anchor',x,12.58,-3.8,.7,.22,1,'dark')
    b('anchor',-.75,8.45,-3.9,1.5,.55,.18,'hazard')
    b('anchor',-.25,9.4,-3.92,.5,.35,.2,'amber')
    # Anclaje como sobrebota/abrazadera: deja una separación real entre las dos piernas quietas.
    for e in parts['anchor']:
        e['from'][0]=round(e['from'][0]*.74,4);e['size'][0]=round(e['size'][0]*.74,4)
    return parts


def mk2_additions():
    """Blindaje independiente: reutiliza las bases Mk I sin mover una sola UV anterior."""
    parts={key:[] for key in ('cooler_mk2','filter_mk2','anchor_mk2')}
    def b(p,f,s,mat):parts[p].append({'from':list(f),'size':list(s),'mat':mat})
    for x in (-4.25,1.35):
        b('cooler_mk2',(x,.95,9.35),(2.9,2.0,.65),'slag_plate')
        b('cooler_mk2',(x,7.95,9.35),(2.9,2.0,.65),'slag_plate')
        b('cooler_mk2',(x+.25,1.3,10.02),(2.4,.45,.2),'forge_edge')
        b('cooler_mk2',(x+.95,8.5,10.02),(.9,.75,.2),'pyroxite')
    b('cooler_mk2',(-4.6,10.85,6.1),(9.2,.65,3.7),'slag_plate')
    b('cooler_mk2',(-1.3,8.15,9.55),(2.6,2.25,.45),'forge_edge')
    for x in (-4.38,1.8):
        b('filter_mk2',(x,-3.15,-8.9),(2.6,.65,2.4),'slag_plate')
        b('filter_mk2',(x,-.4,-8.9),(2.6,.65,2.4),'slag_plate')
        b('filter_mk2',(x+.85,-1.95,-8.9),(.9,.8,.4),'pyroxite')
    b('filter_mk2',(-1.3,-3.35,-7.15),(2.6,.55,1.0),'forge_edge')
    for x in (-1.85,1.3):
        b('anchor_mk2',(x,7.5,-4.18),(.55,3,.4),'slag_plate')
        b('anchor_mk2',(x,7.8,3.98),(.55,3.2,.6),'slag_plate')
    b('anchor_mk2',(-1.65,10.1,-4.35),(3.3,.45,1.9),'forge_edge')
    b('anchor_mk2',(-.55,8.95,-4.18),(1.1,.6,.3),'pyroxite')
    b('anchor_mk2',(-1.75,12.6,-3.7),(3.5,.55,7.0),'slag_plate')
    return parts


def layout():
    parts=geometry();occupied=[[False]*128 for _ in range(128)]
    for y in range(64):
        for x in range(64):occupied[y][x]=True
    def allocate(boxes):
        for e in sorted(boxes,key=lambda e:-math.ceil(e['size'][1]+e['size'][2])):
            w,h,d=e['size'];rw=math.ceil(2*(w+d))+2;rh=math.ceil(h+d)+2;found=None
            for y in range(129-rh):
                for x in range(129-rw):
                    if not any(occupied[yy][xx] for yy in range(y,y+rh) for xx in range(x,x+rw)):
                        found=(x,y);break
                if found:break
            assert found,'Las UV nuevas no caben en 128x128'
            x,y=found;e['uv']=[x+1,y+1]
            for yy in range(y,y+rh):
                for xx in range(x,x+rw):occupied[yy][xx]=True
    allocate([e for boxes in parts.values() for e in boxes])
    extra=mk2_additions();allocate([e for boxes in extra.values() for e in boxes])
    for name,boxes in extra.items():parts[name]=copy.deepcopy(parts[name[:-4]])+boxes
    return parts


def regions(e):
    u,v=e['uv'];w,h,d=e['size']
    return {'top':(u+d,v,w,d),'bottom':(u+d+w,v,w,d),'right':(u,v+d,d,h),
            'front':(u+d,v+d,w,h),'left':(u+d+w,v+d,d,h),'back':(u+2*d+w,v+d,w,h)}


def paint_color(mat,face,x,y,w,h):
    color=COLORS[mat];k=1.12 if face in ('top','up') else .75 if face in ('bottom','down') else 1
    if min(w,h)>=3:
        if x==0 or y==0:k+=.1
        elif x==w-1 or y==h-1:k-=.18
    if mat=='vent' and x%2==1:k*=.48
    if mat=='lead' and face in ('bottom','down') and (x+y)%3==0:k*=.5
    if mat in ('steel','tank','copper') and w>=3 and h>=3 and (x*13+y*7)%41==0:k*=.7
    return tuple(max(0,min(255,round(c*k))) for c in color)+(255,)


def extend_texture(original,original_glow):
    image=Image.new('RGBA',(128,128));glow=Image.new('RGBA',(128,128))
    image.paste(original,(0,0));glow.paste(original_glow,(0,0))
    for boxes in layout().values():
        for e in boxes:
            for face,(u,v,w,h) in regions(e).items():
                for yy in range(math.floor(v),math.ceil(v+h)):
                    for xx in range(math.floor(u),math.ceil(u+w)):
                        color=paint_color(e['mat'],face,xx-math.floor(u),yy-math.floor(v),math.ceil(w),math.ceil(h))
                        image.putpixel((xx,yy),color)
                        if e['mat'] in ('led','amber','pyroxite'):glow.putpixel((xx,yy),color)
    assert image.crop((0,0,64,64)).tobytes()==original.tobytes()
    assert glow.crop((0,0,64,64)).tobytes()==original_glow.tobytes()
    return image,glow


def write_java():
    text=JAVA.read_text(encoding='utf-8')
    if 'public final ModelPart cooler_mk2, filter_mk2, anchor_mk2;' not in text:
        text=text.replace('    public WornGearModel(ModelPart root) {','    public final ModelPart cooler_mk2, filter_mk2, anchor_mk2;\n\n    public WornGearModel(ModelPart root) {')
        text=text.replace('        this.anchor = root.getChild("anchor");','        this.anchor = root.getChild("anchor");\n        this.cooler_mk2 = root.getChild("cooler_mk2");\n        this.filter_mk2 = root.getChild("filter_mk2");\n        this.anchor_mk2 = root.getChild("anchor_mk2");')
    if 'public final ModelPart cooler, filter, anchor;' not in text:
        text=text.replace('    public WornGearModel(ModelPart root) {','    public final ModelPart cooler, filter, anchor;\n\n    public WornGearModel(ModelPart root) {')
        text=text.replace('        this.propulsor = root.getChild("propulsor");','        this.propulsor = root.getChild("propulsor");\n        this.cooler = root.getChild("cooler");\n        this.filter = root.getChild("filter");\n        this.anchor = root.getChild("anchor");')
    def number(n):return f'{float(n):.4f}'.rstrip('0').rstrip('.')+'F'
    lines=['        // BEGIN NETHER GEAR GENERATED: tools/nether_gear_rig.py']
    for name,boxes in layout().items():
        lines.append(f'        root.addOrReplaceChild("{name}", CubeListBuilder.create()')
        for e in boxes:
            values=', '.join(number(n) for n in e['from']+e['size'])
            lines.append(f'                .texOffs({e["uv"][0]}, {e["uv"][1]}).addBox({values})')
        lines.append('                , PartPose.ZERO);')
    lines.append('        // END NETHER GEAR GENERATED')
    block='\n'.join(lines)+'\n'
    if '// BEGIN NETHER GEAR GENERATED' in text:
        text=re.sub(r'        // BEGIN NETHER GEAR GENERATED.*?        // END NETHER GEAR GENERATED\n',block,text,flags=re.S)
    else:text=text.replace('        return LayerDefinition.create(mesh, 64, 64);',block+'        return LayerDefinition.create(mesh, 64, 64);')
    text=text.replace('LayerDefinition.create(mesh, 64, 64)','LayerDefinition.create(mesh, 128, 128)')
    text=text.replace('Textura 64x64 generada','Textura 128x128 (zona anterior 64x64 intacta) generada')
    JAVA.write_bytes(text.encode('utf-8'))
    return layout()
