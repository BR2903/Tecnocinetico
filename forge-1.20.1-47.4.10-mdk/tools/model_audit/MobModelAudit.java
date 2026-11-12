package com.bryanamaya.tecnocinetico.audit;

import com.bryanamaya.tecnocinetico.entity.client.*;
import net.minecraft.client.model.geom.ModelPart;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;

/** Auditoría local sin ventana, servidor ni contexto OpenGL: construye los modelos y comprueba sus enlaces reales. */
public final class MobModelAudit {
    private static int baked;
    private static ModelPart require(ModelPart root, String path) {
        for (String name : path.split("/")) root = root.getChild(name);
        return root;
    }
    private static void check(ModelPart root, String... paths) {
        for (String path : paths) require(root,path);
        baked++;
    }
    private static void close(float a, float b, String message) {
        if (Math.abs(a-b) > .0001F) throw new IllegalStateException(message+": "+a+" / "+b);
    }
    /** Contrato de los seis modelos del Nether con los esqueletos vanilla, sin renderizadores. */
    private static void netherRemodel(Path project) throws Exception {
        String[] humanoid={"head","hat","body","right_arm","left_arm","right_leg","left_leg"};
        String[] player={"ear","cloak","left_sleeve","right_sleeve","left_pants","right_pants","jacket"};
        String[] names={"piglin","piglin_brute","zombified_piglin","ghast","blaze","wither_skeleton"};
        var layers=java.util.List.of(NetherRemodelLayers.piglin(),NetherRemodelLayers.piglin_brute(),
                NetherRemodelLayers.zombified_piglin(),NetherRemodelLayers.ghast(),NetherRemodelLayers.blaze(),NetherRemodelLayers.wither_skeleton());
        for(int i=0;i<names.length;i++) {
            ModelPart actual=layers.get(i).bakeRoot(),expected;
            java.util.List<String> bones=new java.util.ArrayList<>();
            if(i<3){
                expected=net.minecraft.client.model.geom.builders.LayerDefinition.create(
                        net.minecraft.client.model.PiglinModel.createMesh(net.minecraft.client.model.geom.builders.CubeDeformation.NONE),64,64).bakeRoot();
                new net.minecraft.client.model.PiglinModel<>(actual);
                bones.addAll(java.util.List.of(humanoid));bones.addAll(java.util.List.of(player));
                bones.add("head/left_ear");bones.add("head/right_ear");
                for(String empty:player)if(!actual.getChild(empty).isEmpty())throw new IllegalStateException("Segunda piel inesperada: "+empty);
            }else if(i==3){
                expected=net.minecraft.client.model.GhastModel.createBodyLayer().bakeRoot();
                new net.minecraft.client.model.GhastModel<>(actual);bones.add("body");
                for(int j=0;j<9;j++)bones.add("tentacle"+j);
            }else if(i==4){
                expected=net.minecraft.client.model.BlazeModel.createBodyLayer().bakeRoot();
                new net.minecraft.client.model.BlazeModel<>(actual);bones.add("head");
                for(int j=0;j<12;j++)bones.add("part"+j);
            }else{
                expected=net.minecraft.client.model.SkeletonModel.createBodyLayer().bakeRoot();
                new net.minecraft.client.model.SkeletonModel<>(actual);bones.addAll(java.util.List.of(humanoid));
            }
            for(String path:bones){
                ModelPart a=require(actual,path),b=require(expected,path);
                close(a.x,b.x,names[i]+" "+path+" X");close(a.y,b.y,names[i]+" "+path+" Y");close(a.z,b.z,names[i]+" "+path+" Z");
                close(a.xRot,b.xRot,names[i]+" "+path+" giro X");close(a.yRot,b.yRot,names[i]+" "+path+" giro Y");close(a.zRot,b.zRot,names[i]+" "+path+" giro Z");
            }
            for(String suffix:new String[]{"","_glow"}){
                Path path=project.resolve("src/main/resources/assets/tecnocinetico/textures/entity/nether_remodel/"+names[i]+suffix+".png");
                var im=ImageIO.read(path.toFile());if(im.getWidth()!=128||im.getHeight()!=128)throw new IllegalStateException("Atlas no es 128x128: "+path);
                if(suffix.isEmpty())continue;
                int lit=0,clear=0;for(int y=0;y<128;y++)for(int x=0;x<128;x++){if((im.getRGB(x,y)>>>24)==0)clear++;else lit++;}
                if(lit==0||clear==0)throw new IllegalStateException("Máscara emisiva sin luz o sin transparencia: "+path);
            }
            baked++;
        }
        System.out.println("NETHER CORRECTO: seis clases vanilla construidas; pivotes, ropa vacía, orejas y doce atlas verificados.");
    }
    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        if(args.length>1&&args[1].equals("--nether-only")){netherRemodel(Path.of(args[0]));return;}
        ModelPart root = SiegeSpiderModel.createLayer().bakeRoot(); new SiegeSpiderModel(root);
        check(root,"body/turret","body/reactor","body/missiles");
        float[][] anchors = {{8,-7,.45F},{8,0,0},{8,7,-.45F},{-8,-7,-.45F},{-8,0,0},{-8,7,.45F}};
        for (int i=0;i<6;i++) {
            ModelPart leg=require(root,"leg"+i), shin=require(leg,"shin"+i);
            close(leg.x,anchors[i][0],"Anclaje X de rodilla "+i);
            close(leg.y,6,"Anclaje Y de rodilla "+i); close(leg.z,anchors[i][1],"Anclaje Z de rodilla "+i);
            close(leg.yRot,anchors[i][2],"Giro Y de rodilla "+i);
            close(leg.zRot,i<3?-.5F:.5F,"Elevación de rodilla "+i);
            close(shin.x,i<3?14:-14,"Longitud hasta rodilla "+i);
            close(shin.zRot,i<3?.5F:-.5F,"Compensación de espinilla "+i);
        }
        root=SwarmBodyModel.createBodyLayer().bakeRoot(); new SwarmBodyModel(root);
        check(root,"body/core","head","front_right","front_left","mid_right","mid_left","rear_right","rear_left");
        root=SwarmWaspModel.createLayer().bakeRoot(); new SwarmWaspModel<>(root);
        check(root,"body/wing_left_front","body/wing_right_front","body/wing_left_back","body/wing_right_back",
                "body/head/scoutSensor","body/guard0","body/guard2","body/abdomen");
        root=HiveNodeModel.createLayer().bakeRoot(); new HiveNodeModel(root);
        check(root,"body/crown","body/petal0","body/petal5");
        root=DroneModel.createLayer().bakeRoot(); new DroneModel(root); check(root,"body/gun");
        for(int i=0;i<4;i++) require(root,"body/arm"+i+"/rotor"+i);
        root=SentryTurretModel.createLayer().bakeRoot(); new SentryTurretModel(root); check(root,"head","base");
        root=AnchorModel.createLayer().bakeRoot(); new AnchorModel(root); check(root,"body/head");
        for(int i=0;i<3;i++) require(root,"leg"+i+"/shin"+i);
        root=CorruptedSpiderModel.createLayer().bakeRoot(); new CorruptedSpiderModel(root); check(root,"body","head");
        for(int i=0;i<8;i++) require(root,"leg"+i+"/shin"+i);
        root=RemodeledCreeperModel.createLayer().bakeRoot(); new RemodeledCreeperModel(root);
        check(root,"head","body/canister0","body/canister2","right_hind_leg","left_hind_leg","right_front_leg","left_front_leg");
        root=CorruptedHumanModel.createLayer().bakeRoot(); new CorruptedHumanModel<>(root);
        check(root,"head/soldier_helmet","head/worker_helmet","head/raider_mask","head/drowned_mask",
                "body/worker_pack","body/soldier_chest","body/drowned_tank","left_arm/crystal_growth","right_arm/raider_pad");
        root=SurvivorRenderer.createLayer().bakeRoot(); new SurvivorModel(root);
        check(root,"head/farmer_hat","head/scrapper_goggles","body/farmer_basket","body/scrapper_pack","body/gunsmith_apron","body/gunsmith_holster");
        root=OverworldGuardianModel.watcherLayer().bakeRoot(); new OverworldGuardianModel(root,true); check(root,"body/capacitor0","body/capacitor1");
        root=OverworldGuardianModel.matriarchLayer().bakeRoot(); new OverworldGuardianModel(root,false); check(root,"body/abdomen","body/head/jaw0","body/head/jaw1");
        netherRemodel(Path.of(args[0]));
        Path textures=Path.of(args[0],"src/main/resources/assets/tecnocinetico/textures/entity/remodel");
        int count=0;
        try(var files=Files.walk(textures)) {
            for(Path p:files.filter(p->p.toString().endsWith(".png")).toList()) {
                var image=ImageIO.read(p.toFile());
                if(image==null || image.getWidth()!=128 || image.getHeight()!=128) throw new IllegalStateException("Atlas inválido: "+p);
                count++;
            }
        }
        System.out.println("MODELOS CORRECTOS: "+baked+" modelos construidos, seis rodillas conservadas y "+count+" texturas de 128 px.");
        WeaponMuzzleAudit.run(Path.of(args[0]));
    }
}
