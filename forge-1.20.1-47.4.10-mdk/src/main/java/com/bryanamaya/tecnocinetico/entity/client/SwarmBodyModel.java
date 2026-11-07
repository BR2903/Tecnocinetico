package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.custom.SwarmBodyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
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
 * Modelo del Cuerpo de Enjambre (textura 64x64).
 * Torso de carne corrompida con placas de Inercita, núcleo de Cinetrita enjaulado en el pecho,
 * patas delanteras-cuchilla, patas medias finas y patas traseras de impulso.
 */
public class SwarmBodyModel extends EntityModel<SwarmBodyEntity> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "swarm_body"), "main");

    private static final float FRONT_REST = -0.25F;
    private static final float MID_SPLAY = 0.6F;

    private final ModelPart body;
    private final ModelPart core;
    private final ModelPart head;
    private final ModelPart frontRight;
    private final ModelPart frontLeft;
    private final ModelPart midRight;
    private final ModelPart midLeft;
    private final ModelPart rearRight;
    private final ModelPart rearLeft;

    public SwarmBodyModel(ModelPart root) {
        this.body = root.getChild("body");
        this.core = body.getChild("core");
        this.head = root.getChild("head");
        this.frontRight = root.getChild("front_right");
        this.frontLeft = root.getChild("front_left");
        this.midRight = root.getChild("mid_right");
        this.midLeft = root.getChild("mid_left");
        this.rearRight = root.getChild("rear_right");
        this.rearLeft = root.getChild("rear_left");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Torso
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -3.5F, -7.0F, 10, 7, 14),
                PartPose.offset(0.0F, 13.0F, 0.0F));
        // Placas dorsales de Inercita
        body.addOrReplaceChild("plates",
                CubeListBuilder.create().texOffs(0, 21).addBox(-5.5F, -1.0F, -6.0F, 11, 2, 12),
                PartPose.offset(0.0F, -4.0F, 0.0F));
        // Núcleo de Cinetrita en el pecho
        body.addOrReplaceChild("core",
                CubeListBuilder.create().texOffs(48, 0).addBox(-2.5F, -2.5F, -1.0F, 5, 5, 2),
                PartPose.offset(0.0F, 2.0F, -7.0F));
        // Barrotes que encierran el núcleo
        body.addOrReplaceChild("bars",
                CubeListBuilder.create().texOffs(48, 8)
                        .addBox(-2.5F, -3.0F, -0.5F, 1, 6, 1)
                        .addBox(-0.5F, -3.0F, -0.5F, 1, 6, 1)
                        .addBox(1.5F, -3.0F, -0.5F, 1, 6, 1),
                PartPose.offset(0.0F, 2.0F, -8.3F));

        // Cabeza / máscara sensorial
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 35).addBox(-3.0F, -3.0F, -5.0F, 6, 5, 5),
                PartPose.offset(0.0F, 10.0F, -6.0F));

        // Patas delanteras: brazo + cuchilla
        root.addOrReplaceChild("front_right", frontLeg(false), PartPose.offset(-6.0F, 12.0F, -5.0F));
        root.addOrReplaceChild("front_left", frontLeg(true), PartPose.offset(6.0F, 12.0F, -5.0F));

        // Patas medias finas (aspecto de enjambre)
        root.addOrReplaceChild("mid_right",
                CubeListBuilder.create().texOffs(0, 45).addBox(-0.5F, 0.0F, -0.5F, 1, 12, 1),
                PartPose.offset(-5.0F, 14.0F, 1.0F));
        root.addOrReplaceChild("mid_left",
                CubeListBuilder.create().texOffs(0, 45).mirror().addBox(-0.5F, 0.0F, -0.5F, 1, 12, 1),
                PartPose.offset(5.0F, 14.0F, 1.0F));

        // Patas traseras de impulso: muslo de carne + pie metálico
        root.addOrReplaceChild("rear_right", rearLeg(false), PartPose.offset(-4.5F, 14.0F, 5.0F));
        root.addOrReplaceChild("rear_left", rearLeg(true), PartPose.offset(4.5F, 14.0F, 5.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    private static CubeListBuilder frontLeg(boolean mirror) {
        return CubeListBuilder.create().mirror(mirror)
                .texOffs(22, 35).addBox(-1.0F, -1.0F, -1.0F, 2, 12, 2)
                .texOffs(30, 35).addBox(-0.5F, 8.0F, -5.0F, 1, 3, 5);
    }

    private static CubeListBuilder rearLeg(boolean mirror) {
        return CubeListBuilder.create().mirror(mirror)
                .texOffs(42, 35).addBox(-1.5F, -1.0F, -1.5F, 3, 6, 3)
                .texOffs(54, 35).addBox(-1.0F, 5.0F, -1.0F, 2, 5, 2);
    }

    @Override
    public void setupAnim(SwarmBodyEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;

        float walk = Mth.cos(limbSwing * 0.6662F) * 1.1F * limbSwingAmount;
        float walkOpp = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.1F * limbSwingAmount;

        // Paso alterno en diagonal
        frontRight.xRot = FRONT_REST + walk * 0.6F;
        frontLeft.xRot = FRONT_REST + walkOpp * 0.6F;
        rearRight.xRot = walkOpp;
        rearLeft.xRot = walk;
        midRight.xRot = walk * 0.8F;
        midLeft.xRot = walkOpp * 0.8F;
        midRight.zRot = MID_SPLAY;
        midLeft.zRot = -MID_SPLAY;
        frontRight.zRot = 0.08F;
        frontLeft.zRot = -0.08F;

        // Ataque: alza las dos cuchillas y las baja de golpe
        if (attackTime > 0.0F) {
            float swing = Mth.sin(attackTime * Mth.PI);
            frontRight.xRot = FRONT_REST - swing * 1.7F;
            frontLeft.xRot = FRONT_REST - swing * 1.7F;
        }

        // En el aire (salto cinético): cuchillas al frente, patas traseras estiradas hacia atrás
        if (!entity.onGround() && !entity.isInWater()) {
            frontRight.xRot = -1.3F;
            frontLeft.xRot = -1.3F;
            rearRight.xRot = 0.9F;
            rearLeft.xRot = 0.9F;
            midRight.zRot = MID_SPLAY + 0.4F;
            midLeft.zRot = -MID_SPLAY - 0.4F;
        }

        // Respiración del torso y latido del núcleo
        body.y = 13.0F + Mth.sin(ageInTicks * 0.1F) * 0.3F;
        float pulse = 1.0F + Mth.sin(ageInTicks * 0.25F) * 0.06F;
        core.xScale = pulse;
        core.yScale = pulse;
        core.zScale = pulse;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        body.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        head.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        frontRight.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        frontLeft.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        midRight.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        midLeft.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        rearRight.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        rearLeft.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
