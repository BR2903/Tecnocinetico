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
 * Cabeza: caja (-4,-8,-4)..(4,0,4), cara en z = -4, ojos hacia y = -4. Torso: (-4,0,-2)..(4,12,2), espalda z = +2.
 * Pierna: (-2,0,-2)..(2,12,2), pie en y = 12.
 * Textura 64x64 generada por tools/worn_gear_texture.py (la capa _glow lleva lentes, toberas y luces).
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
        // GAFAS DE VISIÓN NOCTURNA: binocular delante de los ojos colgado de un soporte en la frente
        root.addOrReplaceChild("visor", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.7F, -5.4F, -9.5F, 3, 3, 3)                     // tubo izq.
                .texOffs(0, 0).addBox(0.7F, -5.4F, -9.5F, 3, 3, 3)                      // tubo der.
                .texOffs(12, 0).addBox(-3.2F, -4.9F, -9.8F, 2, 2, 0.5F)                 // lente izq. (verde, brilla)
                .texOffs(12, 0).addBox(1.2F, -4.9F, -9.8F, 2, 2, 0.5F)                  // lente der.
                .texOffs(0, 6).addBox(-2.5F, -5.7F, -6.6F, 5, 2, 2)                     // cuerpo binocular
                .texOffs(22, 6).addBox(-0.5F, -6.8F, -5.6F, 1, 2, 1)                    // brazo articulado
                .texOffs(14, 6).addBox(-1.5F, -7.6F, -5.0F, 3, 2, 1)                    // soporte de la frente
                .texOffs(18, 0).addBox(-1.5F, -6.6F, 4.2F, 3, 2, 1)                     // batería en la nuca
                .texOffs(0, 10).addBox(-4.4F, -6.6F, -4.4F, 8.8F, 1, 0.4F)              // correa: delante
                .texOffs(0, 10).addBox(-4.4F, -6.6F, 4.0F, 8.8F, 1, 0.4F)               // detrás
                .texOffs(0, 13).addBox(-4.4F, -6.6F, -4.0F, 0.4F, 1, 8)                 // izquierda
                .texOffs(0, 13).addBox(4.0F, -6.6F, -4.0F, 0.4F, 1, 8)                  // derecha
                .texOffs(32, 30).addBox(-1.0F, -8.4F, -4.4F, 2, 0.4F, 8.8F),             // por encima de la cabeza
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
        // BOTAS PROPULSORAS (pierna): caña blindada con puntera y suela, turbina en el talón, aletas
        root.addOrReplaceChild("propulsor", CubeListBuilder.create()
                .texOffs(0, 24).addBox(-2.5F, 7.0F, -3.0F, 5, 5, 5)                     // caña y puntera
                .texOffs(0, 34).addBox(-2.5F, 11.5F, -3.5F, 5, 1, 6)                    // suela
                .texOffs(20, 24).addBox(-1.5F, 6.0F, 2.0F, 3, 4, 3)                     // turbina del talón
                .texOffs(22, 34).addBox(-3.0F, 8.0F, 2.5F, 1, 3, 2)                     // aleta izq.
                .texOffs(22, 34).addBox(2.0F, 8.0F, 2.5F, 1, 3, 2),                     // aleta der.
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
