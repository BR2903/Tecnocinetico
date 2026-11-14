"""Genera exclusivamente arte del Devorador: modelo, renderer, atlas y forge_heart 3D.
python tools/slag_devourer_gen.py
No registra capas, entidades u objetos; no modifica lógica de combate.
"""
import json,math,random,subprocess,sys
from pathlib import Path
from PIL import Image
import gunkit
from slag_devourer_rig import geometry,channels,PALETTE
from nether_remodel_gen import pack,key,faces,number

ROOT=Path(__file__).resolve().parent.parent
A=ROOT/'src/main/resources/assets/tecnocinetico'
JAVA=ROOT/'src/main/java/com/bryanamaya/tecnocinetico/entity/client'
OUT=ROOT/'build/devorador'


def pixel(mat,x,y,w,h,rng):
    k=rng.uniform(.92,1.06);color=PALETTE[mat]
    if mat in ('ember','hot'):
        k=rng.uniform(.82,1.06)
        if mat=='ember' and rng.random()<.16:k*=.45
    elif mat=='hazard':k=.23 if (x+y)//2%2 else 1
    else:
        if x==0 or y==0:k+=.18
        elif x==w-1 or y==h-1:k*=.63
        if mat in ('armor','edge','slag') and rng.random()<.11:color=(100,62,36)
        if mat=='sinew' and y%3==0:k*=.63
        if rng.random()<.045:k*=.65
    return tuple(min(255,max(0,round(v*k))) for v in color)+(255,)


def texture(rig,uv):
    im=Image.new('RGBA',(128,128));glow=Image.new('RGBA',(128,128))
    for spec,(u,v) in uv.items():
        mat,w,h,d=spec;rng=random.Random(str(spec)+'devorador')
        for rect in faces(u,v,w,h,d).values():
            x0,y0,x1,y1=map(math.floor,rect)
            for y in range(y0,math.ceil(rect[3])):
                for x in range(x0,math.ceil(rect[2])):
                    c=pixel(mat,x-x0,y-y0,math.ceil(rect[2]-rect[0]),math.ceil(rect[3]-rect[1]),rng)
                    im.putpixel((x,y),c)
                    if mat in ('hot','ember'):glow.putpixel((x,y),c)
    im.save(A/'textures/entity/slag_devourer.png');glow.save(A/'textures/entity/slag_devourer_glow.png')


def java(rig,uv):
    names=[p['name'] for p in rig.parts]
    lines=['package com.bryanamaya.tecnocinetico.entity.client;',
      'import com.bryanamaya.tecnocinetico.Tecnocinetico;',
      'import com.bryanamaya.tecnocinetico.nether.SlagDevourerEntity;',
      'import com.mojang.blaze3d.vertex.*;',
      'import net.minecraft.client.model.EntityModel;',
      'import net.minecraft.client.model.geom.*;',
      'import net.minecraft.client.model.geom.builders.*;',
      'import net.minecraft.resources.ResourceLocation;',
      'import net.minecraft.util.Mth;',
      'import java.util.WeakHashMap;',
      '/** Arte generado por tools/slag_devourer_gen.py. Sin lógica de juego; renderer a escala 2. */',
      'public class SlagDevourerModel extends EntityModel<SlagDevourerEntity> {',
      '    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "slag_devourer"), "main");',
      '    public final ModelPart root;',
      '    private final ModelPart '+', '.join(names)+';',
      '    private final ModelPart[] segments, jaws, legsLeft, legsRight, clawsLeft, clawsRight;',
      '    private final WeakHashMap<SlagDevourerEntity, Visual> visuals = new WeakHashMap<>();',
      '    private static class Visual { int attack; float progress, stopped = -1000; }',
      '    public SlagDevourerModel(ModelPart root) {', '        this.root = root;']
    for p in rig.parts:lines.append(f'        this.{p["name"]} = {p["parent"]}.getChild("{p["name"]}");')
    for field,pattern,count in (('segments','segment_{}',7),('jaws','jaw_{}',8),('legsLeft','leg_{}_left',7),('legsRight','leg_{}_right',7),('clawsLeft','claw_{}_left',7),('clawsRight','claw_{}_right',7)):
        lines.append(f'        {field} = new ModelPart[] {{'+', '.join(pattern.format(i) for i in range(count))+'};')
    lines+=['    }','    public static LayerDefinition createBodyLayer() {','        var mesh = new MeshDefinition();','        var root = mesh.getRoot();']
    for p in rig.parts:
        builder='CubeListBuilder.create()'
        for c in p['cubes']:
            u,v=uv[key(c)];builder+=f'.texOffs({u}, {v}).addBox('+', '.join(number(n) for n in (*c['xyz'],*c['size']))+', new CubeDeformation('+number(-c['inset'])+'))'
        pp='PartPose.offsetAndRotation('+', '.join(number(n) for n in (*p['at'],*p['rot']))+')'
        lines.append(f'        var {p["name"]} = {p["parent"]}.addOrReplaceChild("{p["name"]}", {builder}, {pp});')
    lines+=['        return LayerDefinition.create(mesh, 128, 128);','    }',
      '    private static float smooth(float t) { t = Mth.clamp(t, 0, 1); return t*t*(3-2*t); }',
      '    @Override public void setupAnim(SlagDevourerEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {',
      '        root.getAllParts().forEach(p -> { p.resetPose(); p.xScale = p.yScale = p.zScale = 1; });',
      '        root.visible = !e.burrowed();',
      '        if (e.burrowed()) return;']
    for p in rig.parts:
        for i,scale in enumerate(p.get('scale',(1,1,1))):
            if scale!=1:lines.append(f'        {p["name"]}.{("xScale","yScale","zScale")[i]} = {number(scale)};')
    lines+=['        Visual v = visuals.computeIfAbsent(e, key -> new Visual());',
      '        int attack = e.attack(); float progress = e.windup(), weight = 1;',
      '        if (attack != 0) { v.attack = attack; v.progress = progress; v.stopped = age; }',
      '        else if (v.attack != 0 && age-v.stopped < 9 && !e.exposed()) {',
      '            attack = v.attack; progress = Mth.lerp(Mth.clamp((age-v.stopped)/2, 0, 1), v.progress, 1);',
      '            weight = 1-smooth((age-v.stopped-2)/7);',
      '        }',
      '        applyPose(attack, progress, weight, e.exposed());',
      '        float walk = Mth.clamp(limbSwingAmount*6, 0, 1) * (e.exposed() ? 0 : attack == 0 || attack == 3 ? 1 : 0.35F);',
      '        float phaseBase = attack == 3 ? age*0.95F : limbSwing*0.8F;',
      '        if (attack == 3 && !e.exposed()) walk = Math.max(walk, weight);',
      '        for (int i=0; i<7; i++) {',
      '            float phase = phaseBase-i*0.85F;',
      '            segments[i].x += Mth.sin(phase)*(attack == 3 ? 0.22F : 0.9F*walk)+Mth.sin(age*0.11F-i*0.65F)*0.08F;',
      '            segments[i].yRot += Mth.sin(phase)*(attack == 3 ? 0.035F : 0.11F*walk);',
      '            segments[i].y += Mth.sin(age*0.12F-i*0.7F)*(e.exposed() ? 0.03F : 0.09F);',
      '            for (int side=0; side<2; side++) {',
      '                ModelPart leg = side == 0 ? legsLeft[i] : legsRight[i];',
      '                ModelPart claw = side == 0 ? clawsLeft[i] : clawsRight[i];',
      '                float step = phase+(side == 0 ? 0 : Mth.PI);',
      '                leg.xRot += Mth.sin(step)*0.38F*walk;',
      '                leg.y -= Math.max(0, Mth.cos(step))*0.95F*walk;',
      '                claw.xRot -= Mth.sin(step)*0.22F*walk;',
      '            }',
      '        }',
      '        head.yRot += Mth.clamp(netHeadYaw, -30, 30)*Mth.DEG_TO_RAD*0.25F;',
      '        head.xRot += Mth.clamp(headPitch, -15, 15)*Mth.DEG_TO_RAD*0.1F;',
      '        for (int i=0; i<8; i++) jaws[i].xRot += Mth.sin(age*0.10F+i*0.7F)*0.025F;',
      '    }',
      '    private void applyPose(int attack, float progress, float weight, boolean exposed) {',
      '        float charge = smooth(progress/0.76F), hit = smooth((progress-0.76F)/0.24F);',
      '        if (exposed) {']
    for ch,val in channels(0,1,True).items():
        if val:lines.append(f'            {ch} += {number(val)};')
    lines+=['            return;','        }','        switch (attack) {']
    # El endpoint .76 es exactamente la pose de aviso máxima (charge=1, hit=0).
    for attack in (1,2,3,4,5,7):
        a=channels(attack,.76);b=channels(attack,1)
        lines.append(f'            case {attack}:')
        for ch in sorted(a.keys()|b.keys()):
            if not a.get(ch,0) and not b.get(ch,0):continue
            expr=number(b[ch]) if attack==3 else f'Mth.lerp(hit, {number(a[ch])}*charge, {number(b[ch])})'
            lines.append(f'                {ch} += {expr}*weight;')
        lines.append('                break;')
    lines+=['            default: break;','        }','    }',
      '    @Override public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float alpha) {',
      '        root.render(pose, buffer, light, overlay, r, g, b, alpha);','    }','}']
    (JAVA/'SlagDevourerModel.java').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    (JAVA/'SlagDevourerRenderer.java').write_text('''package com.bryanamaya.tecnocinetico.entity.client;
import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.nether.SlagDevourerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
/** Solo presentación. Generado por tools/slag_devourer_gen.py. */
public class SlagDevourerRenderer extends MobRenderer<SlagDevourerEntity, SlagDevourerModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/entity/slag_devourer.png");
    private static final RenderType GLOW = RenderType.eyes(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/entity/slag_devourer_glow.png"));
    public SlagDevourerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SlagDevourerModel(ctx.bakeLayer(SlagDevourerModel.LAYER)), 1.4F);
        addLayer(new RenderLayer<SlagDevourerEntity, SlagDevourerModel>(this) {
            @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, SlagDevourerEntity e,
                    float swing, float amount, float partial, float age, float yaw, float pitch) {
                if (e.burrowed()) return;
                float brightness = e.exposed() ? 1.0F : e.enraged() ? 0.85F : 0.55F;
                if (!e.exposed()) brightness += 0.04F*(float)Math.sin(age*0.25F)+0.1F*e.windup();
                getParentModel().renderToBuffer(pose, buffers.getBuffer(GLOW), 15728640, OverlayTexture.NO_OVERLAY,
                        brightness, brightness, brightness, 1.0F);
            }
        });
    }
    @Override public void render(SlagDevourerEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        shadowRadius = e.burrowed() ? 0 : 1.4F;
        if (!e.burrowed()) super.render(e, yaw, partial, pose, buffers, light);
    }
    @Override public boolean shouldRender(SlagDevourerEntity e, Frustum frustum, double x, double y, double z) {
        return !e.burrowed() && e.shouldRender(x, y, z) && frustum.isVisible(e.getBoundingBox().inflate(7.5, 2.5, 7.5));
    }
    @Override public ResourceLocation getTextureLocation(SlagDevourerEntity e) { return TEXTURE; }
    @Override protected void scale(SlagDevourerEntity e, PoseStack pose, float partial) { pose.scale(2, 2, 2); }
}
''',encoding='utf-8')


def heart():
    elements=[]
    def b(f,s,mat,rot=None):elements.append(gunkit.el(f,[f[i]+s[i] for i in range(3)],'dev_'+mat,rot=rot))
    for mat,col in PALETTE.items():
        def paint(face,x,y,rng,m=mat):return pixel(m,x,y,face.w,face.h,rng)
        gunkit.PAINTERS['dev_'+mat]=paint
    b([5.2,5.2,6.5],[5.6,5.6,4.5],'ember');b([6.2,6.2,6.0],[3.6,3.6,.6],'hot')
    for x in (3.3,11.2):b([x,4,6.2],[1.5,8,5],'armor')
    for y in (3.3,11.2):b([4,y,6.2],[8,1.5,5],'armor')
    # Engranaje frontal con hueco central: ocho dientes y cuatro secciones del aro.
    for f,s in (([4,3.8,5.3],[8,1.4,1]),([4,10.8,5.3],[8,1.4,1]),([3.8,5.2,5.3],[1.4,5.6,1]),([10.8,5.2,5.3],[1.4,5.6,1])):b(f,s,'edge')
    for i in range(8):
        a=i*math.pi/4;x=8+4.9*math.sin(a);y=8+4.9*math.cos(a)
        b([x-1,y-.9,5.05],[2,1.8,1.5],'edge',{'origin':[x,y,5.8],'axis':'z','angle':45 if i%2 else 0})
    for x,y in ((5,5),(10.3,5),(5,10.3),(10.3,10.3)):b([x,y,4.9],[.7,.7,.5],'carbide')
    b([5.3,5.3,11.2],[5.4,5.4,1.1],'dark')
    for x in (5.9,8.8):
        b([x,11.9,7],[1.3,2.5,1.4],'copper');b([x,13.5,8.4],[1.3,.9,2],'copper')
    b([6,1.6,7],[4,1.7,2.5],'slag')
    for x in (6.3,8.5):b([x,.7,7.5],[1.2,.9,1.5],'copper')
    display={'firstperson_righthand':{'rotation':[0,145,0],'translation':[-1,3,0],'scale':[.5]*3},
             'firstperson_lefthand':{'rotation':[0,145,0],'translation':[-1,3,0],'scale':[.5]*3},
             'thirdperson_righthand':{'rotation':[0,0,0],'translation':[0,2,1],'scale':[.45]*3},
             'thirdperson_lefthand':{'rotation':[0,0,0],'translation':[0,2,1],'scale':[.45]*3},
             'ground':{'translation':[0,2,0],'scale':[.4]*3},'fixed':{'rotation':[0,180,0],'scale':[.75]*3}}
    gunkit.model('forge_heart',elements,display,group='forge_heart');gunkit.flush()
    subprocess.run([sys.executable,str(ROOT/'tools/gui_fit.py'),str(A),'22,155,0','fill=14','forge_heart'],check=True)


def main():
    OUT.mkdir(parents=True,exist_ok=True);rig=geometry();uv,specs=pack(rig)
    texture(rig,uv);java(rig,uv);heart()
    (OUT/'rig.json').write_text(json.dumps(rig.parts,indent=2)+'\n',encoding='utf-8')
    (OUT/'integracion.json').write_text(json.dumps({
        'model':'com.bryanamaya.tecnocinetico.entity.client.SlagDevourerModel',
        'renderer':'com.bryanamaya.tecnocinetico.entity.client.SlagDevourerRenderer',
        'layer':'SlagDevourerModel.LAYER (tecnocinetico:slag_devourer#main)',
        'factory':'SlagDevourerModel::createBodyLayer',
        'scale':'2x aplicado por el renderer. No duplicar. Longitud en reposo aproximadamente 7 bloques.',
        'geometry':{'segments':7,'radial_jaws':8,'legs':14,'cubes':sum(len(p['cubes']) for p in rig.parts),'atlas':[128,128]},
        'visibility':'burrowed() impide render base, glow, nombre y sombra. shouldRender amplía solo el culling visual por la cola larga; no cambia la colisión.',
        'attacks':'1 BITE, 2 CHARGE, 3 RUSH (no depende de la dirección de windup), 4 SPIT, 5 BURROW, 6 EMERGE oculto, 7 SUMMON; exposed prevalece en superficie.',
        'animation':'Ondulación y garras de limbSwing, respiración, corona y placas. Recuperación visual por entidad al terminar ataque; sin lógica.',
        'emerge':'Mientras burrowed=true no hay modelo, también en ambas vistas de EMERGE. Al reaparecer, la entidad existente pasa a exposed: se muestra la pose caída/abierta.',
        'glow':'RenderType.eyes incluido: juntas, garganta, señales de piroxita y núcleo; mayor brillo exposed/enraged.',
        'spit':'Boca durante SPIT cerca de 2.4 bloques delante y 1.9 de alto; compatible aproximadamente con origen actual (2.5,2.0). No cambié projectiles/IA.',
        'item':'forge_heart, models/item/forge_heart.json, textures/item/gun/forge_heart.png; 3D pequeño, sin predicados. Rareza épica y botín a cargo del código.'
    },ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    files=['src/main/java/com/bryanamaya/tecnocinetico/entity/client/'+n+'.java' for n in ('SlagDevourerModel','SlagDevourerRenderer')]
    files+=['src/main/resources/assets/tecnocinetico/'+n for n in ('textures/entity/slag_devourer.png','textures/entity/slag_devourer_glow.png','models/item/forge_heart.json','textures/item/gun/forge_heart.png')]
    files+=['tools/'+n+'.py' for n in ('slag_devourer_rig','slag_devourer_gen','preview_slag_devourer')]+['docs/HERRAMIENTAS.md']
    (OUT/'archivos.json').write_text(json.dumps(files,indent=2)+'\n',encoding='utf-8')
    print(f'Devorador: {len(rig.parts)} huesos, {sum(len(p["cubes"]) for p in rig.parts)} cubos, {len(specs)} redes UV en 128x128.')


if __name__=='__main__':main()
