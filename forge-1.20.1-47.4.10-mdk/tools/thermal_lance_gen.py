"""Lanza Térmica de Plasma Ígneo: tres estados y emisor metálico, sin lente.
python tools/thermal_lance_gen.py
Solo arte; sin Java, registros, datos, partículas ni cambios en otras armas.
"""
import copy,json,math,random,subprocess,sys
from pathlib import Path
from PIL import Image,ImageDraw
import numpy as np
import gunkit
from combustion_drill_gen import DISPLAY as DRILL_DISPLAY
from preview_combustion_drill import euler

ROOT=Path(__file__).resolve().parent.parent
A=ROOT/'src/main/resources/assets/tecnocinetico'
OUT=ROOT/'build/lanza'
NAMES=('thermal_lance','thermal_lance_firing','thermal_lance_overheated')
MUZZLE=(8,13,-14.18)
SMOKE=(8,19.15,10.8)
PALETTE={'tl_dark':(32,37,36),'tl_armor':(64,71,69),'tl_edge':(130,131,110),'tl_slag':(97,62,40),
         'tl_ceramic':(163,158,129),'tl_plasma':(255,132,25),'tl_copper':(152,93,49),
         'tl_cold':(124,62,29),'tl_fire':(255,174,58),'tl_red':(255,83,24),
         'tl_fin':(74,81,77),'tl_rubber':(35,38,33)}


def save(path,data):
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')


def painters():
    for mat,col in PALETTE.items():
        def painter(face,x,y,rng,m=mat,c=col):
            k=rng.uniform(.94,1.05)
            if x==0 or y==0:k+=.12
            elif x==face.w-1 or y==face.h-1:k*=.62
            color=c
            if m in ('tl_armor','tl_edge','tl_slag') and rng.random()<.06:color=(110,72,45)
            if m=='tl_plasma':
                if x in (0,1):color=(255,225,128)
                elif y%7==0:k*=.83
            return tuple(min(255,max(0,round(a*k))) for a in color)+(255,)
        gunkit.PAINTERS[mat]=painter


def geometry(state):
    e=[];heat='tl_fire' if state==1 else 'tl_red' if state==2 else 'tl_cold'
    def b(f,s,mat='tl_armor',rot=None,glow=False):
        el=gunkit.el(f,[f[i]+s[i] for i in range(3)],mat,rot=rot)
        if glow:el['_emissive']=True
        e.append(el)
    def ring(z,r,d,mat,glow=False):
        for i in range(8):
            a=i*math.pi/4;x=8+math.cos(a)*r;y=13+math.sin(a)*r
            if i%2:
                w,h=r*.96,.65;rot={'axis':'z','angle':45 if i in (3,7) else -45,'origin':[x,y,z+d/2]}
            elif i in (0,4):w,h=.65,r*1.02;rot=None
            else:w,h=r*1.02,.65;rot=None
            b([x-w/2,y-h/2,z],[w,h,d],mat,rot,glow)
    # Cuna abierta del núcleo: dos marcos, esquinas soldadas y rejillas de barras reales.
    for y in (9.8,16.4):b([4.4,y,1.8],[7.2,.75,8.5],'tl_dark')
    for z in (1.7,9.8):
        for x in (4.5,10.75):b([x,10.45,z],[.75,5.95,.75],'tl_armor')
        b([5.25,10.2,z],[5.5,.55,.75],'tl_edge')
        b([5.25,15.85,z],[5.5,.55,.75],'tl_edge')
    for x in (4.28,11.3):
        for z in (2.75,4.45,6.15,7.85,9.55):b([x,10.55,z],[.42,5.15,.4],'tl_armor')
        for y in (10.35,15.65):b([x-.05,y,2.45],[.52,.42,7.5],'tl_edge')
    b([5.0,10.4,10.6],[6.0,5.5,1.2],'tl_slag')
    for x in (5.1,10.3):
        for z in (2,10):b([x,16.95,z],[.65,.38,.65],'tl_edge')
    # Depósito de Plasma Ígneo: ventana naranja incandescente, única recarga del arma.
    b([5.7,5.75,3.6],[4.6,3.9,5.9],'tl_dark')
    for y in (5.5,9.3):b([5.45,y,3.35],[5.1,.6,6.4],'tl_armor')
    for x in (5.45,10.25):b([x,6.35,4.3],[.35,2.3,4.5],'tl_plasma',glow=True)
    b([6.45,6.1,3.1],[3.1,2.8,.35],'tl_slag')
    for z in (4.55,7.9):
        b([5.25,5.95,z],[.4,3.5,.35],'tl_rubber');b([10.35,5.95,z],[.4,3.5,.35],'tl_rubber')
    b([5.1,8.8,8.2],[.5,2.6,.5],'tl_copper');b([5.1,10.9,7.0],[.5,.5,1.7],'tl_copper')
    b([10.3,7.1,9.8],[.8,1.1,.8],'tl_edge')
    # Disipadores con luz propia por cara en los estados activos, no iluminan toda el arma.
    for i in range(9):
        z=2.0+i*.88;h=1.6+.3*(i%3)
        b([4.2,17.05,z],[7.6,h,.24],heat if state==2 else 'tl_fin',glow=state==2)
        b([4.12,17.05+h-.20,z-.04],[7.76,.22,.32],heat,glow=state in (1,2))
    # Chimenea de alivio y válvula: punto para humo de sobrecalentamiento en el contrato.
    b([7.1,17.15,10.0],[1.8,1.45,1.6],'tl_dark')
    for x in (7.0,8.65):b([x,18.6,9.9],[.35,.5,1.8],'tl_edge')
    for z in (9.9,11.35):b([7.35,18.6,z],[1.3,.5,.35],'tl_edge')
    # Intercambiador y tubos hacia el tren de enfoque, con aisladores cerámicos.
    b([5.4,10.4,.3],[5.2,5.2,1.5],'tl_armor')
    for x in (5.1,10.3):
        b([x,11.7,-2.4],[.6,.6,4.5],'tl_copper')
        b([x,11.7,-2.4],[.6,2.2,.6],'tl_copper')
        b([x-.15,13.3,-2.6],[.9,.65,1.05],'tl_ceramic')
    # Tubo hueco con ocho caras: tres anillos de enfoque y barras de protección.
    ring(-11.8,2.08,12.2,'tl_dark')
    for z in (-3,-7.2,-11.1):
        ring(z,2.7,1.05,'tl_edge')
        ring(z-.24,2.52,.24,heat,glow=state in (1,2))
    for x in (4.8,10.6):b([x,12.6,-11.4],[.6,.8,11.8],'tl_ceramic')
    ring(-14.0,3.12,1.1,'tl_armor');ring(-14.1,2.9,.35,'tl_edge')
    # Emisor metálico permanente: conducto abierto, aro inyector y anillo de enfoque.
    b([6.7,11.7,.5],[2.6,2.6,.45],'tl_dark')
    ring(-14.05,2.35,.5,'tl_edge')
    ring(-14.08,1.8,.65,heat if state in (1,2) else 'tl_armor',glow=state in (1,2))
    for x,y in ((5.0,12.6),(10.45,12.6),(7.65,10.0),(7.65,15.45)):
        b([x,y,-14.35],[.6,.8,.7],'tl_slag')
    # Asa delantera térmicamente aislada y empuñadura principal.
    b([6.1,8.8,-3.8],[3.8,1.4,4.5],'tl_dark')
    b([6.4,8.5,-3.55],[3.2,.6,3.8],'tl_rubber')
    for z in (-3.1,-2.2,-1.3,-.4):b([6.25,8.4,z],[3.5,.3,.2],'tl_edge')
    rot={'axis':'x','angle':-22.5,'origin':[8,11,11.4]}
    b([7.1,6.9,10.7],[1.8,4.4,2.1],'tl_rubber',rot)
    for y in (7.2,8.2,9.2):b([7.0,y,10.65],[2,.2,2.25],'tl_dark',rot)
    b([7.65,9.9,9.8],[.7,1.2,.4],'tl_slag')
    b([7.1,7.6,9.3],[1.8,.35,2.4],'tl_edge');b([7.1,7.95,9.3],[1.8,1.5,.3],'tl_edge')
    # Culata de dos largueros, hueco central y cantonera ancha; sin piezas flotantes.
    b([6.5,13.8,11.6],[3,1.4,8.0],'tl_armor')
    b([6.9,8.0,12.5],[2.2,1.1,7.1],'tl_armor')
    b([6.25,8.0,18.3],[3.5,7.4,1.4],'tl_slag')
    b([5.6,7.7,19.6],[4.8,7.8,1.1],'tl_rubber')
    for y in (8.3,9.8,11.3,12.8,14.3):b([5.5,y,20.55],[5,.3,.35],'tl_dark')
    b([6.25,15.2,12.3],[3.5,.65,6.9],'tl_rubber')
    # Testigo de temperatura y soporte lateral del núcleo.
    b([11.75,13.3,8.7],[.25,2.2,1.1],'tl_dark')
    b([12.02,13.6,8.9],[.15,1.5,.55],heat,glow=state in (1,2))
    # Conductos del intercambiador: el estado también se lee por la rejilla trasera del núcleo.
    for z in (4.1,6.25,8.4):b([10.15,11.65,z],[.22,3.25,.32],heat,glow=state in (1,2))
    return e


def heart_elements(state):
    """Reutiliza el núcleo aprobado, reducido al 48% y orientado hacia la rejilla izquierda."""
    src=json.loads((A/'models/item/forge_heart.json').read_text());out=[]
    im=Image.open(A/'textures/item/gun/forge_heart.png').convert('RGB')
    def point(p):return [8+(p[2]-8)*.48,13.5+(p[1]-8)*.48,6-(p[0]-8)*.48]
    face_map={'north':'west','south':'east','east':'north','west':'south','up':'up','down':'down'}
    tint=('FF856D50','FFFFFFFF','FFFF936D')[state]
    for e in src['elements']:
        corners=np.array([point(p) for p in (e['from'],e['to'])]);n={'from':corners.min(0).tolist(),'to':corners.max(0).tolist(),'faces':{}}
        if 'rotation' in e:
            r=e['rotation'];axis,sign={'x':('z',-1),'y':('y',1),'z':('x',1)}[r['axis']]
            n['rotation']={'axis':axis,'angle':r['angle']*sign,'origin':point(r['origin'])}
        for f,fd in e['faces'].items():
            q=copy.deepcopy(fd);q['texture']='#heart';q['forge_data']={'color':tint}
            u0,v0,u1,v1=fd['uv'];sx,sy=im.size
            crop=im.crop((int(min(u0,u1)*sx/16),int(min(v0,v1)*sy/16),max(1,math.ceil(max(u0,u1)*sx/16)),max(1,math.ceil(max(v0,v1)*sy/16))))
            avg=np.asarray(crop).reshape(-1,3).mean(0)
            if state in (1,2) and avg[0]>180 and avg[1]>75 and avg[2]<135:
                q['forge_data'].update(block_light=15,sky_light=15,ambient_occlusion=False)
                n['shade']=False
            if f in ('up','down'):q['rotation']=90 if f=='up' else 270
            n['faces'][face_map[f]]=q
        out.append(n)
    return out


def cleanup_obsolete():
    """Solo los tres recursos retirados expresamente y sus vistas antiguas, sin borrado recursivo."""
    deleted=['models/item/piroxite_lens.json','textures/item/piroxite_lens.png','models/item/thermal_lance_no_lens.json']
    for name in deleted:
        path=(A/name).resolve();assert A.resolve() in path.parents;path.unlink(missing_ok=True)
    for pattern in ('thermal_lance_no_lens*','piroxite_lens*','core_estado_3.png','validacion.json'):
        for path in OUT.glob(pattern):
            assert path.resolve().parent==OUT.resolve()
            if path.is_file():path.unlink()
    save(OUT/'borrados.json',['src/main/resources/assets/tecnocinetico/'+n for n in deleted])


def display():
    d=copy.deepcopy(DRILL_DISPLAY)
    for side in ('righthand','lefthand'):d['firstperson_'+side]={'rotation':[0,8,0],'translation':[0,.5,-2],'scale':[.57]*3}
    d['ground']={'rotation':[0,0,0],'translation':[0,-1.5,0],'scale':[.4]*3}
    d['fixed']={'rotation':[0,90,0],'translation':[3.0,-2.4,0],'scale':[.45]*3}
    return d


def display_point(point,d,left=False):
    tr=np.array(d.get('translation',[0]*3),dtype=float);ang=d.get('rotation',[0]*3).copy()
    if left:tr[0]*=-1;ang[1]*=-1;ang[2]*=-1
    return euler((np.array(point)-8)*np.array(d.get('scale',[1]*3)),ang)+tr


def muzzle_contract():
    model=json.loads((A/'models/item/thermal_lance.json').read_text());contexts={}
    for context in ('firstperson_righthand','firstperson_lefthand','thirdperson_righthand','thirdperson_lefthand'):
        left=context.endswith('lefthand');p=display_point(MUZZLE,model['display'][context],left)
        q=display_point(np.array(MUZZLE)+[0,0,-1],model['display'][context],left)-p;q/=np.linalg.norm(q)
        entry={'after_display_px':np.round(p,6).tolist(),'after_display_direction':np.round(q,6).tolist()}
        if context.startswith('first'):
            entry['rest_camera_px']=np.round(p+[-8 if left else 8,-8,-10.56],6).tolist()
        else:
            # ItemInHandLayer 1.20.1: translateToHand -> Rx(-90) Ry(180) T(±1,2,-10) -> display.
            arm=euler(euler(p+[-1 if left else 1,2,-10],[0,180,0]),[-90,0,0])
            entry['arm_bone_local_px']=np.round(arm,6).tolist()
            entry['from_palm_reference_px']=np.round(arm-[0,10,0],6).tolist()
        contexts[context]=entry
    save(OUT/'boca.json',{'raw_model_px':MUZZLE,'raw_forward':[0,0,-1],'emitter_front_z':-14.08,'smoke_vent_raw_px':SMOKE,
        'units':'Todos los puntos en píxeles (16 = 1 bloque). Raw absoluto; after_display centrado en 8,8,8.',
        'contexts':contexts,
        'first_person_pose':'Traslación de reposo (±0.5,-0.5,-0.66), sin rotaciones antes del display. Las cifras rest_camera_px dependen de esta pose.',
        'third_person_pose':'arm_bone_local_px: después de translateToHand, en el marco del hueso de brazo (origen del hombro); aplicar pose del brazo para llegar al cuerpo. from_palm_reference_px resta la referencia (0,10,0); no es una coordenada universal del mundo.',
        'integration':'Preferir HandMuzzleGeometry.tip(pose.last().pose(), modelo.getTransforms().getTransform(context), left, new Vec3(8,13,-14.18)). La pose debe capturarse justo ANTES de renderItem, con las transformaciones de mano ya aplicadas; evita aplicar el display dos veces.',
        'world_projection':'HandMuzzleGeometry.tip devuelve espacio de vista de la mano. Para el rayo en mundo, convertir con MuzzleProjection.toWorldView y la rotación inversa de vista, como ClientWeaponEffects.capture; después sumar a la posición de cámara.',
        'endpoint':'Dibujar desde esta salida visual al punto de impacto calculado por el servidor. La dirección del display no sustituye el apuntado del jugador.',
        'states':'Misma boca y display en los tres estados; punto y coordenadas transformadas conservados respecto al diseño anterior. Solo estado 1 emite rayo.'})


def main():
    OUT.mkdir(parents=True,exist_ok=True);cleanup_obsolete();painters()
    sources=[geometry(i) for i in range(3)]
    overrides=[{'predicate':{'tecnocinetico:lance_state':i},'model':'tecnocinetico:item/'+NAMES[i]} for i in (1,2)]
    for i,name in enumerate(NAMES):gunkit.model(name,sources[i],display(),overrides if i==0 else None,group='thermal_lance')
    gunkit.flush()
    for i,name in enumerate(NAMES):
        p=A/f'models/item/{name}.json';m=json.loads(p.read_text());m['textures']['heart']='tecnocinetico:item/gun/forge_heart'
        for e,source in zip(m['elements'],sources[i]):
            if source.get('_emissive'):
                e['shade']=False
                for fd in e['faces'].values():fd['forge_data']={'block_light':15,'sky_light':15,'ambient_occlusion':False}
        # Importación de la pieza aprobada sin tocar su archivo ni su textura.
        m['elements']+=heart_elements(i);save(p,m)
    subprocess.run([sys.executable,str(ROOT/'tools/gui_fit.py'),str(A),'y-65;x25','fill=14',*NAMES],check=True)
    from igneous_plasma_art import generate as plasma_art
    plasma_files=plasma_art();muzzle_contract()
    save(OUT/'integracion.json',{'models':dict(enumerate(NAMES)),'predicate':'tecnocinetico:lance_state: entero 0/1/2; eliminado estado 3',
        'emissive':'ForgeFaceData por cara (forge_data block_light=15, sky_light=15, ambient_occlusion=false). Compatible con el Forge 1.20.1 local, sin renderer propio. No produce luz de bloque.',
        'core':'Reutiliza models/item/forge_heart.json y su textura aprobados, a escala .48, orientado hacia la rejilla izquierda. Se mantienen intactos.',
        'fuel':'Solo Plasma Ígneo, depósito naranja incandescente; sin lente ni otros consumibles.',
        'plasma':'block/igneous_plasma_still (16x128, 8 cuadros), _flow (32x128, 4 cuadros), .mcmeta incluidos. Tinte blanco 0xFFFFFFFF; luz 8 sugerida en FluidType, a cargo del código.',
        'bucket':'PNG item/igneous_plasma_bucket.png (16x16); Claude cambia el modelo existente a item/generated con layer0=tecnocinetico:item/igneous_plasma_bucket. Este generador no toca el JSON del cubo.',
        'cell':'plasma_cell: modelo item/generated y PNG item/plasma_cell.png, 16x16, hermana de kinetic_fuel_cell.',
        'firstperson':'Pose estable del taladro (±.5,-.5,-.66), sin golpe vanilla y sin inclinación hacia la cámara. Clic derecho mantenido sin bloquear la vista con pose vanilla de arco.',
        'thirdperson':'Display del taladro aprobado; recomendar CROSSBOW_HOLD para dos manos.',
        'muzzle':'build/lanza/boca.json: punto original, direcciones y coordenadas transformadas de ambas manos y ambos contextos.',
        'smoke':'Sobrecalentada: humo desde el respiradero raw (8,19.15,10.8), dirección (0,1,0). Partículas a cargo del código; no hay humo plano pegado al arma.',
        'emitter':'Aros metálicos permanentes con conducto hueco. Boca conservada (8,13,-14.18); no cambia el contrato del rayo.'})
    files=['src/main/resources/assets/tecnocinetico/models/item/'+n+'.json' for n in NAMES]
    files+=['src/main/resources/assets/tecnocinetico/textures/item/gun/thermal_lance.png']+plasma_files
    files+=['tools/thermal_lance_gen.py','tools/igneous_plasma_art.py','tools/preview_thermal_lance.py','docs/HERRAMIENTAS.md']
    save(OUT/'archivos.json',files)
    print('Lanza: tres estados sin lente, depósito naranja, Plasma Ígneo animado, cubo y célula 16x16. Boca conservada:',MUZZLE)


if __name__=='__main__':main()
