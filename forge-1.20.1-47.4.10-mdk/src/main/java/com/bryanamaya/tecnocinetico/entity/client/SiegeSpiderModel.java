package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.boss.SiegeSpiderEntity;
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
 * Araña de Asedio (modelo de diseño; el renderizador lo escala x2: ~7 bloques entre puntas de patas, 5,5 de largo
 * y 3,5 de alto): tórax blindado, placas dorsales, torreta doble giratoria al frente con sensor, lanzamisiles en el
 * lomo, ventana del núcleo detrás y seis patas (muslo que sube, rodilla de cobre, espinilla hasta el suelo y garra).
 * Textura 128x128 de tools/siege_spider_texture.py. Frente hacia -Z.
 */
public class SiegeSpiderModel extends HierarchicalModel<SiegeSpiderEntity> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "siege_spider"), "main");
    private static final float LIFT = 0.5F;                                       // ángulo del muslo hacia arriba
    /** {x del anclaje, z del anclaje, giro en Y} de cada pata (derecha = +x). */
    private static final float[][] LEGS = {{8, -7, 0.45F}, {8, 0, 0.0F}, {8, 7, -0.45F}, {-8, -7, -0.45F}, {-8, 0, 0.0F}, {-8, 7, 0.45F}};

    private final ModelPart root, body, turret;
    private final ModelPart[] legs = new ModelPart[6], shins = new ModelPart[6];

    public SiegeSpiderModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.turret = body.getChild("turret");
        for (int i = 0; i < 6; i++) {
            legs[i] = root.getChild("leg" + i);
            shins[i] = legs[i].getChild("shin");
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-8.0F, -6.0F, -10.0F, 16, 8, 20)                // tórax
                        .texOffs(0, 28).addBox(-6.0F, -8.0F, -8.0F, 12, 2, 16)                // placas dorsales
                        .texOffs(56, 30).addBox(-5.0F, -12.0F, 2.0F, 10, 4, 8)                // lanzamisiles
                        .texOffs(100, 20).addBox(-3.0F, -4.0F, 9.6F, 6, 4, 1),                // ventana del núcleo (brilla)
                PartPose.offset(0.0F, 8.0F, 0.0F));
        body.addOrReplaceChild("turret", CubeListBuilder.create()
                        .texOffs(72, 0).addBox(-5.0F, -4.0F, -8.0F, 10, 7, 8)                 // torreta
                        .texOffs(72, 15).addBox(-4.0F, -2.0F, -20.0F, 2, 2, 12)               // cañón izq.
                        .texOffs(72, 15).addBox(2.0F, -2.0F, -20.0F, 2, 2, 12)                // cañón der.
                        .texOffs(100, 15).addBox(-2.0F, -3.0F, -8.6F, 4, 2, 1),               // sensor (brilla)
                PartPose.offset(0.0F, -3.0F, -9.0F));
        for (int i = 0; i < 6; i++) {
            boolean right = LEGS[i][0] > 0;
            PartDefinition leg = root.addOrReplaceChild("leg" + i, CubeListBuilder.create()
                            .texOffs(0, 46).addBox(right ? 0.0F : -14.0F, -1.0F, -1.0F, 14, 2, 2),   // muslo
                    PartPose.offsetAndRotation(LEGS[i][0], 6.0F, LEGS[i][1], 0.0F, LEGS[i][2], right ? -LIFT : LIFT));
            leg.addOrReplaceChild("shin", CubeListBuilder.create()
                            .texOffs(40, 46).addBox(-1.5F, -1.5F, -1.5F, 3, 3, 3)               // rodilla
                            .texOffs(32, 46).addBox(-1.0F, 0.0F, -1.0F, 2, 25, 2)               // espinilla
                            .texOffs(40, 52).addBox(-2.0F, 23.0F, -2.0F, 4, 2, 4),              // garra
                    PartPose.offsetAndRotation(right ? 14.0F : -14.0F, 0.0F, 0.0F, 0.0F, 0.0F, right ? LIFT : -LIFT));
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(SiegeSpiderEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        turret.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        turret.xRot = Mth.clamp(headPitch, -30.0F, 30.0F) * Mth.DEG_TO_RAD;
        body.y += Mth.sin(age * 0.07F) * 0.3F;
        float amount = Math.min(limbSwingAmount, 1.0F);
        for (int i = 0; i < 6; i++) {                                             // marcha en trípode alterno
            float ph = limbSwing * 0.5F + (i % 2 == (i < 3 ? 0 : 1) ? 0 : Mth.PI);
            float lift = Math.max(0.0F, Mth.sin(ph)) * 0.35F * amount;
            boolean right = i < 3;
            legs[i].zRot += right ? -lift : lift;
            shins[i].zRot += right ? lift : -lift;
            legs[i].yRot += Mth.cos(ph) * 0.25F * amount;
        }
        switch (e.mode()) {
            case 1 -> turret.x += Mth.sin(age * 3.1F) * 0.25F;                    // cargando el disparo
            case 2 -> {                                                           // preparando el pisotón: se agacha
                body.y += 2.5F;
                for (ModelPart l : legs) {
                    l.y += 2.0F;
                }
            }
            case 3 -> turret.xRot -= 0.15F + Mth.sin(age * 2.0F) * 0.05F;         // haz de arrastre
            default -> {
            }
        }
    }
}
