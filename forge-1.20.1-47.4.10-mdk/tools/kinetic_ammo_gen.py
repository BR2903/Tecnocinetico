"""Munición cinética v2 y Equipo Táctico 176x248, sin registros ni cambios de lógica.
python tools/kinetic_ammo_gen.py
Usa geometría/UV/display aprobados y materiales editables de ammo_packages_gen/gunkit.
No regenera ni sobrescribe las municiones normales. Ejecutar después de generadores antiguos de GUI.
"""
import copy,json,random,zlib,math
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw
import gunkit
from ammo_packages_gen import rifle_pile,shells,rifle_belt
from tactical_gui_gen import panel,slot,GHOSTS,GHOST,CYAN_DIM,CYAN,GREEN
from preview_combustion_drill import raster,font

ROOT=Path(__file__).resolve().parent.parent
A=ROOT/'src/main/resources/assets/tecnocinetico'
OUT=ROOT/'build/municion_cinetica'
PAIRS=[('rifle_round','kinetic_rifle_round',rifle_pile,5),('shotgun_shell','kinetic_shotgun_shell',shells,6),('rifle_belt','kinetic_rifle_belt',rifle_belt,15)]
FILES=[]

def ammo():
    report={}
    for normal,name,builder,count in PAIRS:
        original=json.loads((A/f'models/item/{normal}.json').read_text(encoding='utf-8'));model=copy.deepcopy(original)
        source=A/f'textures/item/gun/{normal}.png';image=Image.open(source).convert('RGBA');before=np.array(image)
        assembly=builder();parts=gunkit.resolve_zfight(assembly.parts)
        assert assembly.rounds==count and len(parts)==len(original['elements']),normal
        changes=0;tip_boxes=0
        for i,(part,element) in enumerate(zip(parts,original['elements'])):
            # Evitar que un futuro cambio de generador pinte el material de otra pieza.
            assert part['from']==element['from'] and part['to']==element['to'],(normal,i,'geometría fuente diferente')
            assert part.get('rotation')==element.get('rotation'),(normal,i,'rotación diferente')
            material=part['_mats']['north'];sx,sy,sz=[part['to'][j]-part['from'][j] for j in range(3)]
            bullet_tip=normal!='shotgun_shell' and material in ('copper','steel_hi')
            shell_cap=normal=='shotgun_shell' and material=='shell_red' and sy<.5
            shell_body=normal=='shotgun_shell' and material=='shell_red' and sy>2
            if not (bullet_tip or shell_cap or shell_body):continue
            tip_boxes+=int(bullet_tip or shell_cap)
            for face,fd in element['faces'].items():
                u,v,U,V=fd['uv'];x,y,X,Y=[round(q*image.width/16) for q in (u,v,U,V)];w,h=X-x,Y-y
                assert w>0 and h>0
                rng=random.Random(zlib.crc32(f'{normal}|{i}|{face}'.encode()));f=gunkit.Face(face,w,h,None)
                for yy in range(h):
                    for xx in range(w):
                        # Escopeta: conserva el cuerpo rojo; núcleo/tapa y collar superior cian.
                        if shell_body and not (face=='up' or face not in ('up','down') and yy in (1,2)):continue
                        c=gunkit.PAINTERS['cyan'](f,xx,yy,rng);image.putpixel((x+xx,y+yy),c)
        changes=int(np.any(before!=np.array(image),axis=2).sum());assert changes>0 and tip_boxes>=count
        assert max(image.size)<=128 and all(v&(v-1)==0 for v in image.size)
        image.save(A/f'textures/item/gun/{name}.png')
        ref=f'tecnocinetico:item/gun/{normal}';newref=f'tecnocinetico:item/gun/{name}'
        model['textures']={k:newref if val==ref else val for k,val in model['textures'].items()}
        assert model['elements']==original['elements'] and model['display']==original['display']
        (A/f'models/item/{name}.json').write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')
        FILES.extend([f'src/main/resources/assets/tecnocinetico/models/item/{name}.json',f'src/main/resources/assets/tecnocinetico/textures/item/gun/{name}.png'])
        report[name]={'source':normal,'visible_cartridges':count,'elements':len(model['elements']),'texture_size':list(image.size),'changed_texture_pixels':changes,'geometry_UV_display':'idénticos a la normal','kinetic_material':'gunkit.PAINTERS[cyan], mismo que kinetic_round'}
    return report

def gui():
    path=A/'textures/gui/tactical_gear.png';old=Image.open(path).convert('RGBA');assert old.size==(256,256)
    im=Image.new('RGBA',(256,256));im.paste(old.crop((176,0,256,256)),(176,0))
    d=ImageDraw.Draw(im);panel(d,0,0,175,247)
    x0,y0,x1,y1=28,16,94,124
    d.rectangle((x0,y0,x1,y1),fill=(18,20,24),outline=(40,44,50))
    for x in range(x0+6,x1,8):d.line((x,y0+1,x,y1-1),fill=(26,34,40))
    for y in range(y0+6,y1,8):d.line((x0+1,y,x1-1,y),fill=(26,34,40))
    d.line((x0+4,y1-6,x1-4,y1-6),fill=CYAN_DIM)
    gadgets=[(8,y) for y in (18,40,62,84,106)]
    for (x,y),name in zip(gadgets,('face','back','feet','belt','wrist')):
        slot(d,x,y,CYAN_DIM)
        if name!='wrist':
            for yy,row in enumerate(GHOSTS[name]):
                for xx,ch in enumerate(row):
                    if ch=='#':d.point((x+xx,y+yy),fill=GHOST)
        else:
            d.rectangle((14,y+1,17,y+14),fill=GHOST);d.rectangle((11,y+4,20,y+11),outline=GHOST,width=2);d.rectangle((13,y+6,18,y+9),fill=(139,139,139));d.point((16,y+8),fill=GHOST)
    ammo=[(x,y) for y in (18,36,54,72) for x in (98,116,134,152)]
    food=[(8+18*c,134) for c in range(9)]
    inventory=[(8+18*c,y) for y in (166,184,202) for c in range(9)]
    hotbar=[(8+18*c,224) for c in range(9)]
    for x,y in ammo:slot(d,x,y,CYAN_DIM)
    for x,y in food:slot(d,x,y,GREEN)
    for x,y in inventory+hotbar:slot(d,x,y)
    assert im.crop((176,0,256,256)).tobytes()==old.crop((176,0,256,256)).tobytes()
    # El marco y los iconos de las cinco ranuras no cambian respecto al arte aprobado.
    column_equal=im.crop((6,16,26,125)).tobytes()==old.crop((6,16,26,125)).tobytes()
    im.save(path);FILES.append(str(path.relative_to(ROOT)).replace('\\','/'))
    im.crop((0,0,176,248)).resize((1056,1488),Image.Resampling.NEAREST).save(OUT/'equipo_tactico_16_ranuras.png')
    info={'texture_size':[256,256],'window_size':[176,248],'character_window':[28,16,94,124],'gadgets':gadgets,'ammo':ammo,'food':food,'inventory':inventory,'hotbar':hotbar,'labels_drawn_by_code':{'ammo_y':6,'food_y':124,'inventory_y':155},'buttons':'x=27,y=ranura+2; sin botones pintados','gadget_column_matches_previous':column_equal,'tabs':'píxeles existentes conservados a partir de x=176'}
    (OUT/'gui_layout.json').write_text(json.dumps(info,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')

def previews():
    sheet=Image.new('RGB',(1200,1740),(27,33,37));d=ImageDraw.Draw(sheet)
    labels=('Rifle · 5 balas','Escopeta · 6 cartuchos','Cinta de rifle · 15 balas')
    for row,((normal,kinetic,_,_),label) in enumerate(zip(PAIRS,labels)):
        for col,name in enumerate((normal,kinetic)):
            model=json.loads((A/f'models/item/{name}.json').read_text(encoding='utf-8'));pic=raster(model,'gui');pic.save(OUT/f'{name}_gui.png');sheet.paste(pic,(col*600+44,row*580+48));d.text((col*600+24,row*580+12),label+' · '+('cinética' if col else 'normal'),font=font(25),fill='#dbe3df')
    sheet.save(OUT/'comparativa_gui.png')

def main():
    OUT.mkdir(parents=True,exist_ok=True);report=ammo();gui();previews()
    (OUT/'auditoria.json').write_text(json.dumps(report,indent=2,ensure_ascii=False)+'\n',encoding='utf-8');(OUT/'archivos.json').write_text(json.dumps(FILES+['tools/kinetic_ammo_gen.py'],indent=2)+'\n',encoding='utf-8')
    print('Tres municiones cineticas, misma geometria y display; GUI 176x248 con 16 huecos de municion. Vistas: build/municion_cinetica/.')

if __name__=='__main__':main()
