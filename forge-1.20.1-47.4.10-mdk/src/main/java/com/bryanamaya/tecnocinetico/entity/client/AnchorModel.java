package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.custom.AnchorEntity;
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
 * Anclador: casco de placas de inercita con ventana del núcleo (brilla), emisor giratorio arriba (cristal que
 * brilla y cañón del haz) y tres patas en trípode (muslo horizontal, rodilla de cobre, espinilla hasta el suelo).
 * Textura 64x64 de tools/anchor_texture.py. Mira hacia -Z.
 */
public class AnchorModel extends HierarchicalModel<AnchorEntity> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "anchor"), "main");
    private static final float[] LEG_YAW = {Mth.PI / 3, Mth.PI, -Mth.PI / 3};       // dos delante, una detrás

    private final ModelPart root, body, head;
    private final ModelPart[] legs = new ModelPart[3], shins = new ModelPart[3];

    public AnchorModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        for (int i = 0; i < 3; i++) {
            legs[i] = root.getChild("leg" + i);
            shins[i] = legs[i].getChild("shin");
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.0F, -4.0F, -5.0F, 10, 7, 10)               // casco
                        .texOffs(40, 0).addBox(-2.0F, -2.0F, -5.6F, 4, 3, 1),               // ventana del núcleo (brilla)
                PartPose.offset(0.0F, 9.0F, 0.0F));
        body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(28, 18).addBox(-2.0F, -3.0F, -2.0F, 4, 3, 4)               // base del emisor
                        .texOffs(44, 18).addBox(-1.0F, -2.5F, -7.0F, 2, 2, 5)               // cañón del haz
                        .texOffs(28, 26).addBox(-1.5F, -6.0F, -1.5F, 3, 3, 3),              // cristal (brilla)
                PartPose.offset(0.0F, -4.0F, 0.0F));
        for (int i = 0; i < 3; i++) {
            PartDefinition leg = root.addOrReplaceChild("leg" + i, CubeListBuilder.create()
                            .texOffs(0, 18).addBox(-1.0F, -1.0F, -9.0F, 2, 2, 8),           // muslo
                    PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, 0.0F, LEG_YAW[i], 0.0F));
            leg.addOrReplaceChild("shin", CubeListBuilder.create()
                            .texOffs(40, 26).addBox(-1.5F, -1.5F, -1.5F, 3, 3, 3)           // rodilla de cobre
                            .texOffs(20, 18).addBox(-1.0F, 0.0F, -1.0F, 2, 14, 2),          // espinilla
                    PartPose.offset(0.0F, 0.0F, -9.0F));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(AnchorEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(headPitch, -60.0F, 30.0F) * Mth.DEG_TO_RAD;
        body.y += Mth.sin(age * 0.08F) * 0.3F;
        float amount = Math.min(limbSwingAmount, 1.0F);
        for (int i = 0; i < 3; i++) {                                                 // paso de trípode
            float ph = limbSwing * 0.55F + i * 2.1F;
            legs[i].yRot += Mth.sin(ph) * 0.3F * amount;
            shins[i].xRot = Mth.cos(ph) * 0.25F * amount;
            legs[i].y -= Math.max(0.0F, Mth.sin(ph)) * 1.2F * amount;                 // levanta la pata
        }
        if (e.mode() == 1) {                                                          // cargando: el emisor vibra
            head.x += Mth.sin(age * 2.7F) * 0.2F;
        } else if (e.mode() == 2) {                                                   // pisotón: se agacha
            body.y += 1.5F;
            for (ModelPart leg : legs) {
                leg.y += 1.0F;
            }
        }
    }
}
