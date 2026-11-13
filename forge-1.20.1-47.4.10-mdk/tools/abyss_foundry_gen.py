"""Genera exclusivamente el modelo, renderer visual y atlas originales del Fundidor.
python tools/abyss_foundry_gen.py
"""
import json,math,random
from pathlib import Path
from PIL import Image
from abyss_foundry_rig import geometry,COLORS,POSES,EXPOSED
from nether_remodel_gen import pack,key,faces,number

ROOT=Path(__file__).resolve().parent.parent
A=ROOT/'src/main/resources/assets/tecnocinetico'
JAVA=ROOT/'src/main/java/com/bryanamaya/tecnocinetico/entity/client'
OUT=ROOT/'build/fundidor'


def texture(rig,uv):
    im=Image.new('RGBA',(128,128));glow=Image.new('RGBA',(128,128))
    for spec,(u,v) in uv.items():
        mat,w,h,d=spec;rng=random.Random(str(spec))
        for face,(x0,y0,x1,y1) in faces(u,v,w,h,d).items():
            for y in range(math.floor(y0),math.ceil(y1)):
                for x in range(math.floor(x0),math.ceil(x1)):
                    xx,yy=x-math.floor(x0),y-math.floor(y0);k=rng.uniform(.91,1.09)
                    if mat in ('ember','core'):
                        k=rng.uniform(.8,1.05)
                        if mat=='ember' and rng.random()<.12:k=.42
                    elif mat=='hazard':
                        k=.22 if (xx+yy)//2%2 else 1
                    else:
                        if xx==0 or yy==0:k+=.17
                        if x==math.ceil(x1)-1 or y==math.ceil(y1)-1:k*=.65
                        if rng.random()<.09:k*=.6
                    c=tuple(max(0,min(255,round(a*k))) for a in COLORS[mat])+(255,)
                    if mat in ('steel','edge') and rng.random()<.10:c=(105,62,37,255)
                    im.putpixel((x,y),c)
                    if mat in ('ember','core'):glow.putpixel((x,y),c)
    im.save(A/'textures/entity/abyss_foundry.png');glow.save(A/'textures/entity/abyss_foundry_glow.png')


def java(rig,uv):
    names=[p['name'] for p in rig.parts]
    lines=['package com.bryanamaya.tecnocinetico.entity.client;',
        'import com.bryanamaya.tecnocinetico.Tecnocinetico;',
        'import com.bryanamaya.tecnocinetico.nether.AbyssFoundryEntity;',
        'import com.mojang.blaze3d.vertex.*;',
        'import net.minecraft.client.model.EntityModel;',
        'import net.minecraft.client.model.geom.*;',
        'import net.minecraft.client.model.geom.builders.*;',
        'import net.minecraft.resources.ResourceLocation;',
        'import net.minecraft.util.Mth;',
        'import java.util.WeakHashMap;',
        '/** Arte generado por tools/abyss_foundry_gen.py; sin lógica de combate. Renderer a escala 2. */',
        'public class AbyssFoundryModel extends EntityModel<AbyssFoundryEntity> {',
        '    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "abyss_foundry"), "main");',
        '    public final ModelPart root;',
        '    private final ModelPart '+', '.join(names)+';',
        '    private final WeakHashMap<AbyssFoundryEntity, Visual> visuals = new WeakHashMap<>();',
        '    private static class Visual { int attack; float progress, stopped = -1000; }',
        '    public AbyssFoundryModel(ModelPart root) {', '        this.root = root;']
    for p in rig.parts:lines.append(f'        this.{p["name"]} = {p["parent"]}.getChild("{p["name"]}");')
    lines+=['    }','    public static LayerDefinition createBodyLayer() {','        var mesh = new MeshDefinition();','        var root = mesh.getRoot();']
    for p in rig.parts:
        b='CubeListBuilder.create()'
        for c in p['cubes']:
            u,v=uv[key(c)];b+=f'.texOffs({u}, {v}).addBox('+', '.join(number(n) for n in (*c['xyz'],*c['size']))+', new CubeDeformation('+number(-c['inset'])+'))'
        pp='PartPose.offsetAndRotation('+', '.join(number(n) for n in (*p['at'],*p['rot']))+')'
        lines.append(f'        var {p["name"]} = {p["parent"]}.addOrReplaceChild("{p["name"]}", {b}, {pp});')
    lines+=['        return LayerDefinition.create(mesh, 128, 128);','    }',
        '    private static float smooth(float t) { t = Mth.clamp(t, 0, 1); return t*t*(3-2*t); }',
        '    @Override public void setupAnim(AbyssFoundryEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {',
        '        root.getAllParts().forEach(ModelPart::resetPose);',
        '        Visual v = visuals.computeIfAbsent(e, key -> new Visual());',
        '        int attack = e.attack(); float progress = e.windup(), weight = 1;',
        '        if (attack != 0) { v.attack = attack; v.progress = progress; v.stopped = age; }',
        '        else if (v.attack != 0 && age-v.stopped < 10 && !e.exposed()) {',
        '            attack = v.attack; progress = Mth.lerp(Mth.clamp((age-v.stopped)/2, 0, 1), v.progress, 1);',
        '            weight = 1-smooth((age-v.stopped-2)/8);',
        '        }',
        '        float walk = limbSwingAmount * (attack == 0 && !e.exposed() ? 1 : 0.1F);',
        '        right_leg.xRot += Mth.cos(limbSwing*0.66F)*0.32F*walk;',
        '        left_leg.xRot += Mth.cos(limbSwing*0.66F+Mth.PI)*0.32F*walk;',
        '        right_knee.xRot += Math.max(0, -Mth.sin(limbSwing*0.66F))*0.35F*walk;',
        '        left_knee.xRot += Math.max(0, Mth.sin(limbSwing*0.66F))*0.35F*walk;',
        '        body.y += Mth.sin(age*0.12F)*0.035F;',
        '        body.zRot += Mth.sin(age*(e.enraged() ? 0.7F : 0.18F))*(e.enraged() ? 0.008F : 0.003F);',
        '        head.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.15F;',
        '        head.xRot += headPitch * Mth.DEG_TO_RAD * 0.15F;',
        '        float charge = smooth(progress/0.78F), hit = smooth((progress-0.78F)/0.22F);',
        '        if (e.exposed()) {']
    for ch,val in EXPOSED.items():lines.append(f'            {ch} += {number(val)};')
    lines+=['        } else switch (attack) {']
    for attack,(a,b) in POSES.items():
        lines.append(f'            case {attack}:')
        for ch in sorted(a.keys()|b.keys()):lines.append(f'                {ch} += Mth.lerp(hit, {number(a.get(ch,0))}*charge, {number(b.get(ch,0))})*weight;')
        lines.append('                break;')
    lines+=['            default: break;','        }',
        '        slag_stream.visible = attack == 3 && progress > 0.84F && weight > 0.5F && !e.exposed();',
        '        slag_stream.xRot = -(body.xRot+left_arm.xRot+left_elbow.xRot+crucible.xRot);',
        '    }',
        '    @Override public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float alpha) {',
        '        root.render(pose, buffer, light, overlay, r, g, b, alpha);','    }','}']
    (JAVA/'AbyssFoundryModel.java').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    (JAVA/'AbyssFoundryRenderer.java').write_text('''package com.bryanamaya.tecnocinetico.entity.client;
import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.nether.AbyssFoundryEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
/** Render exclusivamente visual. Generado por tools/abyss_foundry_gen.py. */
public class AbyssFoundryRenderer extends MobRenderer<AbyssFoundryEntity, AbyssFoundryModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/entity/abyss_foundry.png");
    private static final RenderType GLOW = RenderType.eyes(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/entity/abyss_foundry_glow.png"));
    public AbyssFoundryRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new AbyssFoundryModel(ctx.bakeLayer(AbyssFoundryModel.LAYER)), 1.5F);
        addLayer(new RenderLayer<AbyssFoundryEntity, AbyssFoundryModel>(this) {
            @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, AbyssFoundryEntity e,
                    float swing, float amount, float partial, float age, float yaw, float pitch) {
                float brightness = e.exposed() ? 1.0F : e.enraged() ? 0.85F : 0.58F;
                if (!e.exposed()) brightness += 0.05F*(float)Math.sin(age*0.3F) + 0.1F*e.windup();
                getParentModel().renderToBuffer(pose, buffers.getBuffer(GLOW), 15728640, OverlayTexture.NO_OVERLAY,
                        brightness, brightness, brightness, 1.0F);
            }
        });
    }
    @Override public ResourceLocation getTextureLocation(AbyssFoundryEntity e) { return TEXTURE; }
    @Override protected void scale(AbyssFoundryEntity e, PoseStack pose, float partial) { pose.scale(2.0F, 2.0F, 2.0F); }
}
''',encoding='utf-8')


def main():
    OUT.mkdir(parents=True,exist_ok=True);rig=geometry();uv,_=pack(rig)
    texture(rig,uv);java(rig,uv)
    (OUT/'rig.json').write_text(json.dumps(rig.parts,indent=2),encoding='utf-8')
    (OUT/'integracion_modelo.json').write_text(json.dumps({
        'model':'com.bryanamaya.tecnocinetico.entity.client.AbyssFoundryModel',
        'renderer':'com.bryanamaya.tecnocinetico.entity.client.AbyssFoundryRenderer',
        'layer':'AbyssFoundryModel.LAYER (tecnocinetico:abyss_foundry#main)',
        'layer_factory':'AbyssFoundryModel::createBodyLayer',
        'scale':'2.0 ya aplicado por AbyssFoundryRenderer; no duplicar',
        'rest_size_blocks':[3.375,4.6875],
        'collision':'Sin cambios; conservar 3.2 x 4.6. Herramientas sobresalen durante ataques.',
        'texture_size':[128,128],
        'animations':'Andar, vibración, HAMMER=1, FRACTURE=2, SLAG=3, OVERHEAT=4, exposed y enraged. Sin cambios de IA.',
        'glow':'RenderType.eyes integrado en el renderer; más intenso durante exposed/enraged.',
        'recovery':'Estado visual por entidad en WeakHashMap; al reiniciar attack, completa impacto 2 ticks y recoge herramienta en 8.',
        'registration':'Registrar LAYER y renderer en el bus cliente; este generador no registra nada.'
    },ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    files=[str((JAVA/f'{n}.java').relative_to(ROOT)).replace('\\','/') for n in ('AbyssFoundryModel','AbyssFoundryRenderer')]
    files+=['src/main/java/com/bryanamaya/tecnocinetico/client/WornGearModel.java']
    files+=['src/main/resources/assets/tecnocinetico/'+p for p in
        ['textures/entity/'+n+'.png' for n in ('abyss_foundry','abyss_foundry_glow','worn_gear','worn_gear_glow')]
        +['models/item/'+n+'.json' for n in ('nether_coupler','abyssal_component','thermal_cooler_mk2','ash_filter_mk2','anchor_boots_mk2','thermal_cooler','ash_filter','anchor_boots')]
        +['textures/item/gun/'+n+'.png' for n in ('nether_coupler','nether_thermal_cooler_mk2','nether_ash_filter_mk2','nether_anchor_boots_mk2')]
        +['textures/item/abyssal_component.png']]
    files+=['tools/'+n+'.py' for n in ('abyss_foundry_rig','abyss_foundry_gen','abyss_foundry_items','preview_abyss_foundry','preview_foundry_equipment','nether_gear_rig','nether_gear_art','preview_nether_gear')]
    files+=['docs/HERRAMIENTAS.md']
    (OUT/'archivos.json').write_text(json.dumps(files,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(f'Fundidor: {len(rig.parts)} huesos, {sum(len(p["cubes"]) for p in rig.parts)} cubos, atlas 128x128.')


if __name__=='__main__':main()
