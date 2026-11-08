package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.ship.CoreFighterEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Caza del Núcleo (modelo de diseño de ~4,4 bloques; el renderizador lo escala x2,5: ~11 de largo, 10 de envergadura): fuselaje de grafito, morro afilado, cabina de cristal,
 * lomo, alas en flecha con soportes de misiles, dos góndolas de motor con toberas que brillan, derivas inclinadas,
 * estabilizadores, cañones en la raíz de las alas y tren de aterrizaje. Textura 128x256 de tools/ship_texture.py.
 * Morro hacia -Z (como los mobs).
 */
public class CoreFighterModel extends EntityModel<CoreFighterEntity> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "core_fighter"), "main");

    private final ModelPart body, glass, gear;

    public CoreFighterModel(ModelPart root) {
        this.body = root.getChild("body");
        this.glass = root.getChild("glass");
        this.gear = root.getChild("gear");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-6.0F, -7.0F, -28.0F, 12, 7, 50)                // fuselaje
                        .texOffs(0, 57).addBox(-4.0F, -5.0F, -40.0F, 8, 5, 12)                // morro
                        .texOffs(96, 85).addBox(-2.0F, -3.0F, -46.0F, 4, 3, 6)                // punta
                        .texOffs(0, 77).addBox(-3.0F, -10.0F, -8.0F, 6, 3, 20)                // lomo
                        .texOffs(88, 100).addBox(6.5F, -3.0F, -24.0F, 1, 1, 10)               // cañón izq.
                        .texOffs(88, 100).addBox(-7.5F, -3.0F, -24.0F, 1, 1, 10)              // cañón der.
                        .texOffs(52, 77).addBox(-11.0F, -6.0F, 8.0F, 6, 6, 16)                // motor izq.
                        .texOffs(52, 77).mirror().addBox(5.0F, -6.0F, 8.0F, 6, 6, 16)         // motor der.
                        .texOffs(96, 77).addBox(-10.0F, -5.0F, 24.0F, 4, 4, 1)                // tobera izq.
                        .texOffs(96, 77).addBox(6.0F, -5.0F, 24.0F, 4, 4, 1)                  // tobera der.
                        .texOffs(88, 111).addBox(6.0F, -3.0F, 14.0F, 10, 1, 6)                // estabilizador izq.
                        .texOffs(88, 111).mirror().addBox(-16.0F, -3.0F, 14.0F, 10, 1, 6),    // estabilizador der.
                PartPose.offset(0.0F, 12.0F, 0.0F));
        body.addOrReplaceChild("wing_l", CubeListBuilder.create()
                        .texOffs(0, 100).addBox(0.0F, -1.0F, -8.0F, 28, 2, 16)
                        .texOffs(0, 118).addBox(12.0F, 1.0F, -6.0F, 2, 2, 8),                 // misil bajo el ala
                PartPose.offsetAndRotation(6.0F, -3.0F, -2.0F, 0.0F, -0.45F, 0.0F));
        body.addOrReplaceChild("wing_r", CubeListBuilder.create()
                        .texOffs(0, 100).mirror().addBox(-28.0F, -1.0F, -8.0F, 28, 2, 16)
                        .texOffs(0, 118).mirror().addBox(-14.0F, 1.0F, -6.0F, 2, 2, 8),
                PartPose.offsetAndRotation(-6.0F, -3.0F, -2.0F, 0.0F, 0.45F, 0.0F));
        body.addOrReplaceChild("fin_l", CubeListBuilder.create()
                        .texOffs(88, 57).addBox(-1.0F, -10.0F, 0.0F, 2, 10, 10),
                PartPose.offsetAndRotation(4.0F, -7.0F, 12.0F, 0.0F, 0.0F, 0.3F));
        body.addOrReplaceChild("fin_r", CubeListBuilder.create()
                        .texOffs(88, 57).mirror().addBox(-1.0F, -10.0F, 0.0F, 2, 10, 10),
                PartPose.offsetAndRotation(-4.0F, -7.0F, 12.0F, 0.0F, 0.0F, -0.3F));
        root.addOrReplaceChild("glass", CubeListBuilder.create()
                        .texOffs(0, 128).addBox(-4.0F, -12.0F, -24.0F, 8, 5, 14),             // cabina
                PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition gear = root.addOrReplaceChild("gear", CubeListBuilder.create(), PartPose.ZERO);
        float[][] legs = {{0, -26}, {7, 8}, {-7, 8}};
        for (int i = 0; i < 3; i++) {
            gear.addOrReplaceChild("leg" + i, CubeListBuilder.create()
                            .texOffs(122, 57).addBox(-0.5F, 0.0F, -0.5F, 1, 11, 1)
                            .texOffs(114, 57).addBox(-1.0F, 10.0F, -1.0F, 2, 2, 2),
                    PartPose.offset(legs[i][0], 12.0F, legs[i][1]));
        }
        return LayerDefinition.create(mesh, 128, 256);
    }

    @Override
    public void setupAnim(CoreFighterEntity e, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch) {
    }

    /** El tren se recoge en vuelo; la cabina se oculta para el piloto en primera persona. */
    public void configure(boolean onGround, boolean showGlass) {
        gear.visible = onGround;
        glass.visible = showGlass;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        body.render(pose, vc, light, overlay, r, g, b, a);
        glass.render(pose, vc, light, overlay, r, g, b, a);
        gear.render(pose, vc, light, overlay, r, g, b, a);
    }
}
