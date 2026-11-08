package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.custom.DroneEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Dron del Núcleo: casco plano con cúpula, sensor frontal (brilla), cuatro brazos en X con rotores que giran,
 * cañón colgado bajo el casco (apunta con el pitch) y antena. Textura 64x64 de tools/drone_texture.py. Mira hacia -Z.
 */
public class DroneModel extends HierarchicalModel<DroneEntity> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "drone"), "main");

    private final ModelPart root, body, gun;
    private final ModelPart[] rotors = new ModelPart[4];

    public DroneModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.gun = body.getChild("gun");
        for (int i = 0; i < 4; i++) {
            rotors[i] = body.getChild("arm" + i).getChild("rotor");
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -1.5F, -4.0F, 8, 3, 8)                 // casco
                        .texOffs(0, 12).addBox(-2.5F, -3.5F, -2.5F, 5, 2, 5)                // cúpula
                        .texOffs(24, 12).addBox(-1.5F, -1.0F, -4.6F, 3, 2, 1)               // sensor (brilla)
                        .texOffs(40, 16).addBox(-0.5F, -6.5F, 2.0F, 1, 4, 1),               // antena
                PartPose.offset(0.0F, 17.0F, 0.0F));
        for (int i = 0; i < 4; i++) {                                                     // brazos en X
            PartDefinition arm = body.addOrReplaceChild("arm" + i, CubeListBuilder.create()
                            .texOffs(34, 0).addBox(-0.5F, -0.5F, -9.0F, 1, 1, 8),
                    PartPose.rotation(0.0F, Mth.PI / 4 + i * Mth.HALF_PI, 0.0F));
            arm.addOrReplaceChild("rotor", CubeListBuilder.create()
                            .texOffs(34, 10).addBox(-1.0F, 0.0F, -1.0F, 2, 2, 2)              // motor
                            .texOffs(44, 10).addBox(-4.0F, -0.6F, -0.5F, 8, 1, 1)             // aspas
                            .texOffs(0, 20).addBox(-0.5F, -0.6F, -4.0F, 1, 1, 8),
                    PartPose.offset(0.0F, -1.5F, -9.0F));
        }
        body.addOrReplaceChild("gun", CubeListBuilder.create()
                        .texOffs(24, 16).addBox(-1.5F, 0.0F, -1.5F, 3, 2, 3)                // soporte
                        .texOffs(0, 30).addBox(-0.5F, 0.5F, -7.0F, 1, 1, 6),                // cañón
                PartPose.offset(0.0F, 1.5F, -1.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(DroneEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        body.y += Mth.sin(age * 0.15F) * 0.6F;                                          // flota
        body.xRot = Mth.clamp(limbSwingAmount, 0.0F, 1.0F) * 0.35F;                    // se inclina al avanzar
        body.zRot = Mth.sin(age * 0.11F) * 0.05F;
        gun.xRot = Mth.clamp(headPitch, -60.0F, 80.0F) * Mth.DEG_TO_RAD - body.xRot;
        float spin = age * (e.mode() == 0 ? 1.6F : 2.4F);
        for (int i = 0; i < 4; i++) {
            rotors[i].yRot = (i % 2 == 0 ? spin : -spin);
        }
    }
}
