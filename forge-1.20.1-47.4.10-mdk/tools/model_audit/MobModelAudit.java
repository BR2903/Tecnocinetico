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
    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
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
