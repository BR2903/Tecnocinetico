package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.boss.OverworldGuardianEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Dos siluetas propias: plataforma aérea de cuatro rotores y organismo acorazado de seis patas. */
public class OverworldGuardianModel extends HierarchicalModel<OverworldGuardianEntity> {
    public static final ModelLayerLocation WATCHER = layer("network_watcher"), MATRIARCH = layer("swarm_matriarch");
    private final ModelPart root, body;
    private final boolean watcher;
    private static ModelLayerLocation layer(String id) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, id), "main");
    }
    public OverworldGuardianModel(ModelPart root, boolean watcher) {
        this.root = root; this.body = root.getChild("body"); this.watcher = watcher;
    }
    public static LayerDefinition watcherLayer() { return create(true); }
    public static LayerDefinition matriarchLayer() { return create(false); }
    private static LayerDefinition create(boolean watcher) {
        var mesh = new MeshDefinition();
        var body = mesh.getRoot().addOrReplaceChild("body", watcher
                ? CubeListBuilder.create().texOffs(0, 0).addBox(-14, -5, -12, 28, 10, 24)
                : CubeListBuilder.create().texOffs(0, 0).addBox(-11, -7, -10, 22, 14, 28),
                PartPose.offset(0, watcher ? 10 : 8, 0));
        body.addOrReplaceChild("sensor", CubeListBuilder.create().texOffs(0, 104).addBox(-3, -2, -2, 6, 4, 2),
                PartPose.offset(0, 0, watcher ? -12 : -18));
        if (watcher) {
            body.addOrReplaceChild("gun", CubeListBuilder.create().texOffs(64, 44)
                    .addBox(-1, -1, -12, 2, 3, 12), PartPose.offset(0, 6, -4));
            for (int i = 0; i < 4; i++) {
                var arm = body.addOrReplaceChild("arm" + i, CubeListBuilder.create().texOffs(48, 40).addBox(-1, -1, -20, 2, 2, 20),
                        PartPose.rotation(0, Mth.PI / 4 + i * Mth.HALF_PI, 0));
                arm.addOrReplaceChild("rotor", CubeListBuilder.create().texOffs(0, 60).addBox(-8, -1, -1, 16, 1, 2), PartPose.offset(0, -2, -20));
            }
        } else {
            body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 44).addBox(-7, -5, -14, 14, 10, 14), PartPose.offset(0, 1, -4));
            for (int side : new int[]{-1, 1}) {
                body.addOrReplaceChild("jaw" + side, CubeListBuilder.create().texOffs(64, 44).addBox(-1, -1, -12, 2, 3, 12),
                        PartPose.offsetAndRotation(side * 6, 4, -16, 0, side * .25F, 0));
            }
            for (int i = 0; i < 6; i++) {
                int side = i < 3 ? 1 : -1;
                var leg = body.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(0, 80).addBox(-1, -1, -17, 2, 3, 17),
                        PartPose.offsetAndRotation(side * 8, 2, (i % 3) * 8 - 6, 0, -side * (Mth.HALF_PI + (i % 3 - 1) * .4F), 0));
                leg.addOrReplaceChild("shin", CubeListBuilder.create().texOffs(40, 80).addBox(-1, 0, -1, 2, 14, 2), PartPose.offset(0, 0, -17));
            }
        }
        return LayerDefinition.create(mesh, 128, 128);
    }
    @Override public ModelPart root() { return root; }
    @Override public void setupAnim(OverworldGuardianEntity e, float swing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        if (watcher) {
            body.y += Mth.sin(age * .12F) * .7F;
            for (int i = 0; i < 4; i++) body.getChild("arm" + i).getChild("rotor").yRot = age * (i % 2 == 0 ? 1.4F : -1.4F);
            body.getChild("gun").xRot = pitch * Mth.DEG_TO_RAD;
        } else {
            for (int i = 0; i < 6; i++) {
                var leg = body.getChild("leg" + i);
                leg.yRot += Mth.sin(swing * .6F + i * Mth.PI) * amount * .3F;
                leg.y -= Math.max(0, Mth.cos(swing * .6F + i * Mth.PI)) * amount * 2;
            }
            body.getChild("head").yRot = yaw * Mth.DEG_TO_RAD * .3F;
        }
        if (e.mode() == 1 || e.mode() == 2) body.x += Mth.sin(age * 3) * .18F;
    }
}
