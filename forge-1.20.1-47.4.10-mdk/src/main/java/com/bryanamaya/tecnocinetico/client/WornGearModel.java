package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * Gadgets puestos sobre el jugador. Cada pieza está en coordenadas del hueso al que se engancha (cabeza,
 * torso, piernas del PlayerModel): GearLayer aplica la transformación del hueso y la dibuja.
 * Textura 64x64 generada por tools/worn_gear_texture.py (la capa _glow lleva lentes y toberas).
 */
public class WornGearModel {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "worn_gear"), "main");

    public final ModelPart visor, jetpack, jetpackMk2, jetpackMk3, propulsor;

    public WornGearModel(ModelPart root) {
        this.visor = root.getChild("visor");
        this.jetpack = root.getChild("jetpack");
        this.jetpackMk2 = root.getChild("jetpack_mk2");
        this.jetpackMk3 = root.getChild("jetpack_mk3");
        this.propulsor = root.getChild("propulsor");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // VISOR (cabeza: la cara está en z = -4, los ojos a y ≈ -4)
        root.addOrReplaceChild("visor", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.6F, -5.6F, -6.0F, 3, 3, 2)                     // tubo izq. (lente al frente)
                .texOffs(0, 0).addBox(0.6F, -5.6F, -6.0F, 3, 3, 2)                      // tubo der.
                .texOffs(10, 0).addBox(-1.0F, -5.0F, -5.0F, 2, 1, 1)                    // puente
                .texOffs(0, 10).addBox(-4.5F, -5.5F, -4.5F, 9, 1, 0.5F)                 // correa: delante
                .texOffs(0, 10).addBox(-4.5F, -5.5F, 4.0F, 9, 1, 0.5F)                  // detrás
                .texOffs(0, 13).addBox(-4.5F, -5.5F, -4.0F, 0.5F, 1, 8)                 // izquierda
                .texOffs(0, 13).addBox(4.0F, -5.5F, -4.0F, 0.5F, 1, 8),                 // derecha
                PartPose.ZERO);
        // JETPACK (torso: la espalda está en z = +2)
        root.addOrReplaceChild("jetpack", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-3.5F, 1.0F, 2.0F, 7, 9, 1)                      // placa
                .texOffs(32, 12).addBox(-4.0F, 0.0F, 3.0F, 3, 10, 3)                    // depósito izq.
                .texOffs(32, 12).addBox(1.0F, 0.0F, 3.0F, 3, 10, 3)                     // depósito der.
                .texOffs(48, 0).addBox(-3.5F, -1.0F, 3.5F, 2, 1, 2)                     // tapas
                .texOffs(48, 0).addBox(1.5F, -1.0F, 3.5F, 2, 1, 2)
                .texOffs(48, 4).addBox(-3.5F, 10.0F, 3.5F, 2, 2, 2)                     // toberas
                .texOffs(48, 4).addBox(1.5F, 10.0F, 3.5F, 2, 2, 2),
                PartPose.ZERO);
        root.addOrReplaceChild("jetpack_mk2", CubeListBuilder.create()
                .texOffs(44, 12).addBox(-6.0F, 3.0F, 3.5F, 2, 5, 2)                     // propulsores laterales
                .texOffs(44, 12).addBox(4.0F, 3.0F, 3.5F, 2, 5, 2),
                PartPose.ZERO);
        root.addOrReplaceChild("jetpack_mk3", CubeListBuilder.create()
                .texOffs(52, 12).addBox(-0.5F, 1.0F, 6.0F, 1, 8, 2)                     // aleta dorsal
                .texOffs(32, 26).addBox(-7.0F, 0.5F, 4.0F, 3, 1, 2)                     // alerones
                .texOffs(32, 26).addBox(4.0F, 0.5F, 4.0F, 3, 1, 2),
                PartPose.ZERO);
        // PROPULSOR (pierna: el tobillo está hacia y = 9..11)
        root.addOrReplaceChild("propulsor", CubeListBuilder.create()
                .texOffs(0, 24).addBox(-2.5F, 8.0F, -2.5F, 5, 3, 5)                     // carcasa
                .texOffs(20, 24).addBox(-1.0F, 8.5F, -3.0F, 2, 2, 1)                    // rotor (frente)
                .texOffs(0, 34).addBox(-1.5F, 10.0F, 2.5F, 3, 1, 4),                    // aleta
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
