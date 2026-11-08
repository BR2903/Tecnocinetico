package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.custom.SentryTurretEntity;
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
 * Torreta Centinela: base de trípode atornillada al suelo, columna, cabeza giratoria con cañón doble, sensor
 * (brilla) y blindaje lateral. Textura 64x64 generada por tools/turret_texture.py. Mira hacia -Z.
 */
public class SentryTurretModel extends HierarchicalModel<SentryTurretEntity> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "sentry_turret"), "main");

    private final ModelPart root, head;

    public SentryTurretModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("base", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-6.0F, -2.0F, -6.0F, 12, 2, 12)               // placa
                        .texOffs(0, 14).addBox(-3.0F, -9.0F, -3.0F, 6, 7, 6)                // columna
                        .texOffs(24, 14).addBox(-7.0F, -1.5F, -1.0F, 14, 1, 2)             // patas en cruz
                        .texOffs(32, 44).addBox(-1.0F, -1.5F, -7.0F, 2, 1, 14),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 27).addBox(-4.5F, -4.0F, -5.0F, 9, 7, 10)              // carcasa
                        .texOffs(38, 27).addBox(-5.5F, -3.0F, -3.0F, 1, 5, 6)              // blindaje izq.
                        .texOffs(38, 27).mirror().addBox(4.5F, -3.0F, -3.0F, 1, 5, 6)      // blindaje der.
                        .texOffs(0, 44).addBox(-2.5F, -1.5F, -12.0F, 2, 2, 7)              // cañón izq.
                        .texOffs(0, 44).addBox(0.5F, -1.5F, -12.0F, 2, 2, 7)               // cañón der.
                        .texOffs(18, 44).addBox(-1.5F, -3.5F, -5.6F, 3, 2, 1)              // sensor (brilla)
                        .texOffs(26, 44).addBox(-0.5F, -7.0F, 2.0F, 1, 3, 1),              // antena
                PartPose.offset(0.0F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(SentryTurretEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        if (e.isCharging()) {                                            // tiembla al cargar
            head.x += Mth.sin(age * 3.1F) * 0.15F;
        }
    }
}
