"""Remodela todos los mobs anteriores a Vigía/Matriarca. Arte propio, rigs compartidos con las vistas.
No cambia atributos, IA, botín ni puntos débiles. Reejecutar después de regenerar las pieles originales.
"""
import copy
import json
from pathlib import Path
from PIL import Image, ImageDraw
from guardian_rig import ROOT, UV, Rig, java_parts, preview
from mob_remodel_rigs import siege, swarm, wasp, hive, drone, turret, anchor, corrupted_spider, creeper
from mob_humanoid_rigs import human, BASE_PARTS

CLIENT=ROOT / "src/main/java/com/bryanamaya/tecnocinetico/entity/client"
TEXTURES=ROOT / "src/main/resources/assets/tecnocinetico/textures/entity"
MODELS={
    "siege_spider":("SiegeSpiderModel","SiegeSpiderEntity","boss",siege,"createLayer"),
    "swarm_body":("SwarmBodyModel","SwarmBodyEntity","custom",swarm,"createBodyLayer"),
    "swarm_wasp":("SwarmWaspModel",None,None,wasp,"createLayer"),
    "hive_node":("HiveNodeModel","HiveNodeEntity","swarm",hive,"createLayer"),
    "drone":("DroneModel","DroneEntity","custom",drone,"createLayer"),
    "sentry_turret":("SentryTurretModel","SentryTurretEntity","custom",turret,"createLayer"),
    "anchor":("AnchorModel","AnchorEntity","custom",anchor,"createLayer"),
    "corrupted_spider":("CorruptedSpiderModel","Spider",None,corrupted_spider,"createLayer"),
}


def atlas(name, family="machine", base=None, source_glow=None, accent=(65,210,230)):
    im=Image.new("RGBA",(128,128),(28,32,38,255)); glow=Image.new("RGBA",(128,128))
    for y in range(128):
        for x in range(128):
            if base is not None:
                if y < 64: c=(40,42,46)
                elif y < 90 and x < 64: c=(67,77,83) if x%7 and y%6 else (127,133,128)
                elif y < 90 and x < 96: c=(135,87,48) if y%3 else (61,48,33)
                elif y < 90:
                    c=accent; glow.putpixel((x,y),(*c,255))
                elif x < 64: c=(99,79,53) if family=="survivor" else (52,60,65)
                else: c=(79,72,65) if (x+y)%4 else (120,113,83)
            elif y < 38:
                c=(61,77,92) if family=="machine" else (53,66,64)
                if x%9==0 or y%7==0: c=(25,32,39)
                elif x%9==1 or y%7==1: c=(112,124,133) if family=="machine" else (96,113,98)
                elif x%9==2 and y%7==2: c=(150,115,63)
            elif y < 64: c=(24,29,36) if x%4 else (55,63,72)
            elif y < 88 and x < 64: c=(156,94,50) if y%3 else (65,43,28)
            elif y < 88 and x >= 96:
                c=accent if (x+y)%3 else tuple(min(255,n+35) for n in accent)
                glow.putpixel((x,y),(*c,255))
            elif y >= 88 and x < 64: c=(156,165,156) if y%5 else (90,109,112)
            elif y >= 88: c=(65,60,53) if (x+y)%4 else (103,87,69)
            else: c=(26,31,38)
            im.putpixel((x,y),(*c,255))
    if base is not None:
        original=Image.open(base).convert("RGBA")
        im.paste(original,(0,0))
        if source_glow and source_glow.exists(): glow.paste(Image.open(source_glow).convert("RGBA"),(0,0))
    target=TEXTURES / "remodel" / f"{name}.png"; target.parent.mkdir(parents=True,exist_ok=True)
    im.save(target); glow.save(target.with_name(target.stem+"_glow.png"))
    for suffix,color in [("charge",(235,243,255)),("rage",(255,65,48)),("alert",(245,75,38))]:
        g=glow.copy(); g.putdata([(*color,p[3]) if p[3] else p for p in glow.get_flattened_data()])
        g.save(target.with_name(target.stem+"_"+suffix+".png"))


def validate(r):
    known={"root"}
    for p in r.parts:
        if p["parent"] not in known or p["name"] in known: raise ValueError("Jerarquía inválida: "+p["name"])
        known.add(p["name"])
        for material,xyz,size in p["cubes"]:
            u,v=UV[material]; w,h,d=size
            if min(size)<0 or u+2*(w+d)>128 or v+d+h>128: raise ValueError(f"UV inválida: {p['name']} / {size}")


def selected(r, hidden):
    r=copy.deepcopy(r); blocked=set(hidden); parts=[]
    for p in r.parts:
        if p["name"] in blocked or p["parent"] in blocked: blocked.add(p["name"])
        else: parts.append(p)
    r.parts=parts; return r


def layer_method(name,r,humanoid=False):
    if humanoid:
        start="var mesh = net.minecraft.client.model.PlayerModel.createMesh(CubeDeformation.NONE, false);"
        bindings="\n".join(f'        var {p} = root.getChild("{p}");' for p in sorted(BASE_PARTS))
        r=copy.deepcopy(r); r.parts=[p for p in r.parts if p["name"] not in BASE_PARTS]
    else: start="var mesh = new MeshDefinition();"; bindings=""
    return f'''    public static LayerDefinition {name}() {{
        {start}
        var root = mesh.getRoot();
{bindings}
{java_parts(r)}
        return LayerDefinition.create(mesh, 128, 128);
    }}
'''


def wrapper(kind,meta):
    cls,entity,package,fn,method=meta
    generic=entity is None
    imports="import net.minecraft.world.entity.Mob;" if generic else "import net.minecraft.world.entity.monster.Spider;" if entity=="Spider" else f"import com.bryanamaya.tecnocinetico.entity.{package}.{entity};"
    declaration=cls+"<T extends Mob>" if generic else cls
    typ="T" if generic else entity
    extra=f"    public {cls}(ModelPart root, float speed) {{ this(root); this.flapSpeed = speed; }}" if generic else ""
    return f'''package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
{imports}

/** Rig propio generado por tools/remodel_mobs.py; animaciones en RemodeledMobModel. */
public class {declaration} extends RemodeledMobModel<{typ}> {{
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "{kind}"), "main");
    public {cls}(ModelPart root) {{ super(root, "{kind}"); }}
{extra}
    public static LayerDefinition {method}() {{ return RemodeledMobLayers.{kind}(); }}
}}
'''


def update(path,pairs):
    text=path.read_text(encoding="utf-8")
    for old,new in pairs: text=text.replace(old,new)
    path.write_text(text,encoding="utf-8")


def main():
    methods=[]
    for kind,meta in MODELS.items():
        rig=meta[3](); validate(rig)
        methods.append(layer_method(kind,rig))
        (CLIENT/f"{meta[0]}.java").write_text(wrapper(kind,meta),encoding="utf-8")
        if kind=="swarm_wasp":
            for role,color in [("scout",(85,220,220)),("stinger",(230,177,65)),("guard",(100,161,240))]:
                atlas("swarm_wasp/"+role,"swarm",accent=color)
                hidden={"guard0","guard2"} if role!="guard" else set()
                if role!="scout": hidden.add("scoutSensor")
                preview(selected(rig,hidden),"remodel/swarm_wasp/"+role)
            glow=TEXTURES/"remodel/swarm_wasp/guard_glow.png"
            (glow.parent/"glow.png").write_bytes(glow.read_bytes())
        else:
            atlas(kind,"machine" if kind in {"siege_spider","drone","sentry_turret","anchor"} else "swarm",
                  accent=(120,205,155) if kind=="corrupted_spider" else (65,210,230))
            preview(rig,"remodel/"+kind)
    cr=creeper(); validate(cr); methods.append(layer_method("creeper",cr)); atlas("corrupted_creeper","swarm"); preview(cr,"remodel/corrupted_creeper")
    for corrupted in (True,False):
        rig=human(corrupted); validate(rig); methods.append(layer_method("corruptedHuman" if corrupted else "survivor",rig,True))
        names=("wanderer","worker","crystal","raider","soldier","drowned") if corrupted else ("scrapper","gunsmith","farmer")
        folder="corrupted" if corrupted else "survivor"
        for name in names:
            source=TEXTURES/f"{folder}/{name}.png"
            atlas(f"{folder}/{name}",folder,source,source.with_name(name+"_glow.png"))
            if corrupted:
                hidden={"worker_pack","worker_helmet","crystal_growth","raider_pad","raider_mask","soldier_helmet","soldier_chest","drowned_tank","drowned_mask"}
                visible={"worker":{"worker_pack","worker_helmet"},"crystal":{"crystal_growth"},"raider":{"raider_pad","raider_mask"},
                         "soldier":{"soldier_helmet","soldier_chest","raider_pad"},"drowned":{"drowned_tank","drowned_mask"}}.get(name,set())
            else:
                hidden={"scrapper_pack","scrapper_goggles","gunsmith_apron","gunsmith_holster","farmer_hat","farmer_basket"}
                visible={p for p in hidden if p.startswith(name+"_")}
            preview(selected(rig,hidden-visible),f"remodel/{folder}/{name}")
    header='''package com.bryanamaya.tecnocinetico.entity.client;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
/** Generado por tools/remodel_mobs.py. Fuente: mob_remodel_rigs.py y mob_humanoid_rigs.py. */
public final class RemodeledMobLayers {
    private RemodeledMobLayers() {}
'''
    (CLIENT/"RemodeledMobLayers.java").write_text(header+"\n".join(methods)+"}\n",encoding="utf-8")
    # Recursos nuevos en su carpeta: regenerar los scripts antiguos no deshace las geometrías ni sus atlas.
    for filename,ids in {
        "SiegeSpiderRenderer.java":["siege_spider"],"DroneRenderer.java":["drone"],"AnchorRenderer.java":["anchor"],
        "SentryTurretRenderer.java":["sentry_turret"],"HiveNodeRenderer.java":["hive_node"],
        "CorruptedSpiderRenderer.java":["corrupted_spider"],"CorruptedCreeperRenderer.java":["corrupted_creeper"],
        "SwarmWaspRenderer.java":["swarm_wasp/"],
    }.items():
        update(CLIENT/filename,[(f'tex("{i}',f'tex("remodel/{i}') for i in ids])
    update(CLIENT/"SwarmBodyRenderer.java",[("textures/entity/swarm_body","textures/entity/remodel/swarm_body")])
    update(CLIENT/"CorruptedHumanRenderer.java",[("textures/entity/corrupted/","textures/entity/remodel/corrupted/")])
    update(CLIENT/"SurvivorRenderer.java",[("textures/entity/survivor/","textures/entity/remodel/survivor/"),
        ("HumanoidMobRenderer<SurvivorEntity, PlayerModel<SurvivorEntity>>","HumanoidMobRenderer<SurvivorEntity, SurvivorModel>"),
        ("new PlayerModel<>(ctx.bakeLayer(LAYER), false)","new SurvivorModel(ctx.bakeLayer(LAYER))"),
        ("return LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64);","return RemodeledMobLayers.survivor();")])
    update(CLIENT/"HiveNodeRenderer.java",[("SwarmWaspModel<HiveNodeEntity>","HiveNodeModel"),
        ("new SwarmWaspModel<>(ctx.bakeLayer(SwarmWaspModel.LAYER), 0.9F)","new HiveNodeModel(ctx.bakeLayer(HiveNodeModel.LAYER))"),
        ("SCALE = 2.6F","SCALE = 1.0F")])
    update(CLIENT/"CorruptedSpiderRenderer.java",[("MobRenderer<Spider, SpiderModel<Spider>>","MobRenderer<Spider, CorruptedSpiderModel>"),
        ("new SpiderModel<>(ctx.bakeLayer(ModelLayers.SPIDER))","new CorruptedSpiderModel(ctx.bakeLayer(CorruptedSpiderModel.LAYER))")])
    update(CLIENT/"CorruptedCreeperRenderer.java",[("        super(ctx);\n        addLayer", "        super(ctx);\n        this.model = new RemodeledCreeperModel(ctx.bakeLayer(RemodeledCreeperModel.LAYER));\n        addLayer")])
    update(CLIENT/"CorruptedHumanModel.java",[("return LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64);","return RemodeledMobLayers.corruptedHuman();")])
    common=ROOT/"src/main/java/com/bryanamaya/tecnocinetico/Tecnocinetico.java"
    text=common.read_text(encoding="utf-8")
    if "HiveNodeModel.LAYER" not in text:
        line="            event.registerLayerDefinition(SwarmBodyModel.LAYER, SwarmBodyModel::createBodyLayer);"
        added="\n".join(f"            event.registerLayerDefinition(com.bryanamaya.tecnocinetico.entity.client.{cls}.LAYER, com.bryanamaya.tecnocinetico.entity.client.{cls}::createLayer);" for cls in ("HiveNodeModel","CorruptedSpiderModel","RemodeledCreeperModel"))
        common.write_text(text.replace(line,added+"\n"+line),encoding="utf-8")
    print("Modelos, variantes y texturas remodelados; vistas en build/guardian_previews/remodel/")


if __name__=="__main__": main()
