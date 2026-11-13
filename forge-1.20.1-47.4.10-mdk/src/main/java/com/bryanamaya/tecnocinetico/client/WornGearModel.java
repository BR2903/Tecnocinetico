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
 * Textura 128x128 (zona anterior 64x64 intacta) generada por tools/worn_gear_texture.py (la capa _glow lleva lentes, toberas y luces).
 */
public class WornGearModel {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "worn_gear"), "main");

    public final ModelPart visor, jetpack, jetpackMk2, jetpackMk3, propulsor;

    public final ModelPart cooler, filter, anchor;

    public final ModelPart cooler_mk2, filter_mk2, anchor_mk2;

    public WornGearModel(ModelPart root) {
        this.visor = root.getChild("visor");
        this.jetpack = root.getChild("jetpack");
        this.jetpackMk2 = root.getChild("jetpack_mk2");
        this.jetpackMk3 = root.getChild("jetpack_mk3");
        this.propulsor = root.getChild("propulsor");
        this.cooler = root.getChild("cooler");
        this.filter = root.getChild("filter");
        this.anchor = root.getChild("anchor");
        this.cooler_mk2 = root.getChild("cooler_mk2");
        this.filter_mk2 = root.getChild("filter_mk2");
        this.anchor_mk2 = root.getChild("anchor_mk2");
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
        // BEGIN NETHER GEAR GENERATED: tools/nether_gear_rig.py
        root.addOrReplaceChild("cooler", CubeListBuilder.create()
                .texOffs(96, 27).addBox(-2.8F, 2.2F, 2.12F, 0.6F, 0.65F, 3.8F)
                .texOffs(107, 27).addBox(-2.8F, 8.4F, 2.12F, 0.6F, 0.65F, 3.8F)
                .texOffs(118, 27).addBox(2.2F, 2.2F, 2.12F, 0.6F, 0.65F, 3.8F)
                .texOffs(96, 34).addBox(2.2F, 8.4F, 2.12F, 0.6F, 0.65F, 3.8F)
                .texOffs(65, 1).addBox(-4.2F, 0.7F, 5.82F, 8.4F, 9.8F, 0.45F)
                .texOffs(85, 1).addBox(-4.05F, 1.25F, 6.3F, 2.6F, 8.1F, 2.7F)
                .texOffs(116, 46).addBox(-3.6F, 0.55F, 6.7F, 1.7F, 0.75F, 1.8F)
                .texOffs(105, 47).addBox(-3.8F, 0.25F, 6.5F, 2.1F, 0.4F, 2.2F)
                .texOffs(122, 1).addBox(-3.23F, 2.05F, 9.02F, 0.95F, 5.9F, 0.22F)
                .texOffs(86, 58).addBox(-4.13F, 3.1F, 9.04F, 2.76F, 0.4F, 0.22F)
                .texOffs(82, 65).addBox(-4.13F, 7.6F, 9.04F, 2.76F, 0.4F, 0.22F)
                .texOffs(90, 48).addBox(-3.85F, 9.28F, 6.5F, 2.2F, 0.55F, 2.3F)
                .texOffs(98, 1).addBox(1.45F, 1.25F, 6.3F, 2.6F, 8.1F, 2.7F)
                .texOffs(116, 51).addBox(1.9F, 0.55F, 6.7F, 1.7F, 0.75F, 1.8F)
                .texOffs(65, 52).addBox(1.7F, 0.25F, 6.5F, 2.1F, 0.4F, 2.2F)
                .texOffs(122, 10).addBox(2.27F, 2.05F, 9.02F, 0.95F, 5.9F, 0.22F)
                .texOffs(90, 65).addBox(1.37F, 3.1F, 9.04F, 2.76F, 0.4F, 0.22F)
                .texOffs(98, 65).addBox(1.37F, 7.6F, 9.04F, 2.76F, 0.4F, 0.22F)
                .texOffs(76, 52).addBox(1.65F, 9.28F, 6.5F, 2.2F, 0.55F, 2.3F)
                .texOffs(111, 1).addBox(-1.35F, 1.8F, 6.35F, 2.7F, 6.4F, 1.7F)
                .texOffs(96, 57).addBox(-1.7F, 2F, 7.85F, 3.4F, 0.23F, 0.85F)
                .texOffs(86, 61).addBox(-1.7F, 2.8F, 7.85F, 3.4F, 0.23F, 0.85F)
                .texOffs(97, 61).addBox(-1.7F, 3.6F, 7.85F, 3.4F, 0.23F, 0.85F)
                .texOffs(108, 61).addBox(-1.7F, 4.4F, 7.85F, 3.4F, 0.23F, 0.85F)
                .texOffs(65, 62).addBox(-1.7F, 5.2F, 7.85F, 3.4F, 0.23F, 0.85F)
                .texOffs(1, 65).addBox(-1.7F, 6F, 7.85F, 3.4F, 0.23F, 0.85F)
                .texOffs(12, 65).addBox(-1.7F, 6.8F, 7.85F, 3.4F, 0.23F, 0.85F)
                .texOffs(106, 65).addBox(-1.5F, 2.1F, 8.78F, 3F, 0.38F, 0.38F)
                .texOffs(118, 12).addBox(1.12F, 2.48F, 8.78F, 0.38F, 0.77F, 0.38F)
                .texOffs(115, 65).addBox(-1.5F, 3.25F, 8.78F, 3F, 0.38F, 0.38F)
                .texOffs(89, 35).addBox(-1.5F, 3.63F, 8.78F, 0.38F, 0.77F, 0.38F)
                .texOffs(64, 66).addBox(-1.5F, 4.4F, 8.78F, 3F, 0.38F, 0.38F)
                .texOffs(116, 41).addBox(1.12F, 4.78F, 8.78F, 0.38F, 0.77F, 0.38F)
                .texOffs(73, 66).addBox(-1.5F, 5.55F, 8.78F, 3F, 0.38F, 0.38F)
                .texOffs(125, 46).addBox(-1.5F, 5.93F, 8.78F, 0.38F, 0.77F, 0.38F)
                .texOffs(82, 68).addBox(-1.5F, 6.7F, 8.78F, 3F, 0.38F, 0.38F)
                .texOffs(107, 34).addBox(-1.45F, 8.35F, 6.45F, 2.9F, 2F, 2.7F)
                .texOffs(109, 52).addBox(-0.75F, 8.65F, 9.18F, 1.5F, 1.2F, 0.3F)
                .texOffs(124, 65).addBox(-0.45F, 8.88F, 9.51F, 0.9F, 0.55F, 0.12F)
                .texOffs(65, 46).addBox(-4.25F, 10.35F, 6.45F, 8.5F, 0.45F, 2.6F)
                , PartPose.ZERO);
        root.addOrReplaceChild("filter", CubeListBuilder.create()
                .texOffs(113, 19).addBox(-2.15F, -3.2F, -6.35F, 4.3F, 3.25F, 2.5F)
                .texOffs(105, 41).addBox(-1.8F, -2.95F, -6.83F, 3.6F, 2.6F, 0.5F)
                .texOffs(119, 61).addBox(-0.78F, -2.2F, -7.18F, 1.56F, 1.45F, 0.36F)
                .texOffs(118, 16).addBox(-0.3F, -1.83F, -7.31F, 0.6F, 0.6F, 0.14F)
                .texOffs(65, 39).addBox(-4.05F, -2.7F, -7.85F, 2F, 2.5F, 2F)
                .texOffs(101, 52).addBox(-4.17F, -2.55F, -8.22F, 2.24F, 2.2F, 0.38F)
                .texOffs(76, 62).addBox(-3.87F, -2.25F, -8.48F, 1.64F, 1.6F, 0.27F)
                .texOffs(87, 53).addBox(-3.87F, -0.18F, -7.7F, 1.6F, 0.32F, 1.7F)
                .texOffs(23, 65).addBox(-3.87F, -2.95F, -7.7F, 1.6F, 0.3F, 1.7F)
                .texOffs(75, 39).addBox(2.05F, -2.7F, -7.85F, 2F, 2.5F, 2F)
                .texOffs(109, 56).addBox(1.93F, -2.55F, -8.22F, 2.24F, 2.2F, 0.38F)
                .texOffs(32, 65).addBox(2.23F, -2.25F, -8.48F, 1.64F, 1.6F, 0.27F)
                .texOffs(117, 56).addBox(2.23F, -0.18F, -7.7F, 1.6F, 0.32F, 1.7F)
                .texOffs(38, 65).addBox(2.23F, -2.95F, -7.7F, 1.6F, 0.3F, 1.7F)
                .texOffs(126, 34).addBox(3.35F, -1.2F, -8.62F, 0.35F, 0.38F, 0.14F)
                .texOffs(65, 14).addBox(-5.18F, -3.2F, -5.15F, 0.32F, 0.6F, 10.3F)
                .texOffs(89, 14).addBox(4.86F, -3.2F, -5.15F, 0.32F, 0.6F, 10.3F)
                .texOffs(91, 68).addBox(-4.86F, -3.2F, 4.86F, 9.72F, 0.6F, 0.32F)
                .texOffs(96, 53).addBox(-4.86F, -2.95F, -5.5F, 0.9F, 0.9F, 0.55F)
                .texOffs(47, 65).addBox(3.95F, -2.95F, -5.5F, 0.9F, 0.9F, 0.55F)
                , PartPose.ZERO);
        root.addOrReplaceChild("anchor", CubeListBuilder.create()
                .texOffs(85, 39).addBox(-1.6428F, 7.1F, -3.73F, 3.2856F, 3.8F, 0.55F)
                .texOffs(95, 41).addBox(-1.628F, 7.2F, 2.85F, 3.256F, 3.8F, 0.55F)
                .texOffs(65, 27).addBox(-1.739F, 11.02F, -3.9F, 3.478F, 1.6F, 7.45F)
                .texOffs(65, 57).addBox(-1.443F, 10.45F, -3.96F, 2.886F, 0.55F, 2.4F)
                .texOffs(89, 27).addBox(-0.481F, 6F, 3.5F, 0.962F, 4.55F, 1.15F)
                .texOffs(113, 12).addBox(-0.259F, 6.35F, 4.68F, 0.518F, 3.55F, 0.55F)
                .texOffs(78, 57).addBox(-0.666F, 10F, 3.62F, 1.332F, 1.2F, 1.5F)
                .texOffs(121, 34).addBox(-1.443F, 8.1F, 3.42F, 0.407F, 2.5F, 0.6F)
                .texOffs(121, 40).addBox(1.036F, 8.1F, 3.42F, 0.407F, 2.5F, 0.6F)
                .texOffs(52, 65).addBox(-1.295F, 12.58F, -3.8F, 0.518F, 0.22F, 1F)
                .texOffs(58, 65).addBox(0.777F, 12.58F, -3.8F, 0.518F, 0.22F, 1F)
                .texOffs(114, 68).addBox(-0.555F, 8.45F, -3.9F, 1.11F, 0.55F, 0.18F)
                .texOffs(101, 48).addBox(-0.185F, 9.4F, -3.92F, 0.37F, 0.35F, 0.2F)
                , PartPose.ZERO);
        root.addOrReplaceChild("cooler_mk2", CubeListBuilder.create()
                .texOffs(96, 27).addBox(-2.8F, 2.2F, 2.12F, 0.6F, 0.65F, 3.8F)
                .texOffs(107, 27).addBox(-2.8F, 8.4F, 2.12F, 0.6F, 0.65F, 3.8F)
                .texOffs(118, 27).addBox(2.2F, 2.2F, 2.12F, 0.6F, 0.65F, 3.8F)
                .texOffs(96, 34).addBox(2.2F, 8.4F, 2.12F, 0.6F, 0.65F, 3.8F)
                .texOffs(65, 1).addBox(-4.2F, 0.7F, 5.82F, 8.4F, 9.8F, 0.45F)
                .texOffs(85, 1).addBox(-4.05F, 1.25F, 6.3F, 2.6F, 8.1F, 2.7F)
                .texOffs(116, 46).addBox(-3.6F, 0.55F, 6.7F, 1.7F, 0.75F, 1.8F)
                .texOffs(105, 47).addBox(-3.8F, 0.25F, 6.5F, 2.1F, 0.4F, 2.2F)
                .texOffs(122, 1).addBox(-3.23F, 2.05F, 9.02F, 0.95F, 5.9F, 0.22F)
                .texOffs(86, 58).addBox(-4.13F, 3.1F, 9.04F, 2.76F, 0.4F, 0.22F)
                .texOffs(82, 65).addBox(-4.13F, 7.6F, 9.04F, 2.76F, 0.4F, 0.22F)
                .texOffs(90, 48).addBox(-3.85F, 9.28F, 6.5F, 2.2F, 0.55F, 2.3F)
                .texOffs(98, 1).addBox(1.45F, 1.25F, 6.3F, 2.6F, 8.1F, 2.7F)
                .texOffs(116, 51).addBox(1.9F, 0.55F, 6.7F, 1.7F, 0.75F, 1.8F)
                .texOffs(65, 52).addBox(1.7F, 0.25F, 6.5F, 2.1F, 0.4F, 2.2F)
                .texOffs(122, 10).addBox(2.27F, 2.05F, 9.02F, 0.95F, 5.9F, 0.22F)
                .texOffs(90, 65).addBox(1.37F, 3.1F, 9.04F, 2.76F, 0.4F, 0.22F)
                .texOffs(98, 65).addBox(1.37F, 7.6F, 9.04F, 2.76F, 0.4F, 0.22F)
                .texOffs(76, 52).addBox(1.65F, 9.28F, 6.5F, 2.2F, 0.55F, 2.3F)
                .texOffs(111, 1).addBox(-1.35F, 1.8F, 6.35F, 2.7F, 6.4F, 1.7F)
                .texOffs(96, 57).addBox(-1.7F, 2F, 7.85F, 3.4F, 0.23F, 0.85F)
                .texOffs(86, 61).addBox(-1.7F, 2.8F, 7.85F, 3.4F, 0.23F, 0.85F)
                .texOffs(97, 61).addBox(-1.7F, 3.6F, 7.85F, 3.4F, 0.23F, 0.85F)
                .texOffs(108, 61).addBox(-1.7F, 4.4F, 7.85F, 3.4F, 0.23F, 0.85F)
                .texOffs(65, 62).addBox(-1.7F, 5.2F, 7.85F, 3.4F, 0.23F, 0.85F)
                .texOffs(1, 65).addBox(-1.7F, 6F, 7.85F, 3.4F, 0.23F, 0.85F)
                .texOffs(12, 65).addBox(-1.7F, 6.8F, 7.85F, 3.4F, 0.23F, 0.85F)
                .texOffs(106, 65).addBox(-1.5F, 2.1F, 8.78F, 3F, 0.38F, 0.38F)
                .texOffs(118, 12).addBox(1.12F, 2.48F, 8.78F, 0.38F, 0.77F, 0.38F)
                .texOffs(115, 65).addBox(-1.5F, 3.25F, 8.78F, 3F, 0.38F, 0.38F)
                .texOffs(89, 35).addBox(-1.5F, 3.63F, 8.78F, 0.38F, 0.77F, 0.38F)
                .texOffs(64, 66).addBox(-1.5F, 4.4F, 8.78F, 3F, 0.38F, 0.38F)
                .texOffs(116, 41).addBox(1.12F, 4.78F, 8.78F, 0.38F, 0.77F, 0.38F)
                .texOffs(73, 66).addBox(-1.5F, 5.55F, 8.78F, 3F, 0.38F, 0.38F)
                .texOffs(125, 46).addBox(-1.5F, 5.93F, 8.78F, 0.38F, 0.77F, 0.38F)
                .texOffs(82, 68).addBox(-1.5F, 6.7F, 8.78F, 3F, 0.38F, 0.38F)
                .texOffs(107, 34).addBox(-1.45F, 8.35F, 6.45F, 2.9F, 2F, 2.7F)
                .texOffs(109, 52).addBox(-0.75F, 8.65F, 9.18F, 1.5F, 1.2F, 0.3F)
                .texOffs(124, 65).addBox(-0.45F, 8.88F, 9.51F, 0.9F, 0.55F, 0.12F)
                .texOffs(65, 46).addBox(-4.25F, 10.35F, 6.45F, 8.5F, 0.45F, 2.6F)
                .texOffs(105, 71).addBox(-4.25F, 0.95F, 9.35F, 2.9F, 2F, 0.65F)
                .texOffs(115, 74).addBox(-4.25F, 7.95F, 9.35F, 2.9F, 2F, 0.65F)
                .texOffs(82, 77).addBox(-4F, 1.3F, 10.02F, 2.4F, 0.45F, 0.2F)
                .texOffs(110, 76).addBox(-3.3F, 8.5F, 10.02F, 0.9F, 0.75F, 0.2F)
                .texOffs(52, 75).addBox(1.35F, 0.95F, 9.35F, 2.9F, 2F, 0.65F)
                .texOffs(62, 75).addBox(1.35F, 7.95F, 9.35F, 2.9F, 2F, 0.65F)
                .texOffs(90, 77).addBox(1.6F, 1.3F, 10.02F, 2.4F, 0.45F, 0.2F)
                .texOffs(98, 77).addBox(2.3F, 8.5F, 10.02F, 0.9F, 0.75F, 0.2F)
                .texOffs(24, 69).addBox(-4.6F, 10.85F, 6.1F, 9.2F, 0.65F, 3.7F)
                .texOffs(24, 76).addBox(-1.3F, 8.15F, 9.55F, 2.6F, 2.25F, 0.45F)
                , PartPose.ZERO);
        root.addOrReplaceChild("filter_mk2", CubeListBuilder.create()
                .texOffs(113, 19).addBox(-2.15F, -3.2F, -6.35F, 4.3F, 3.25F, 2.5F)
                .texOffs(105, 41).addBox(-1.8F, -2.95F, -6.83F, 3.6F, 2.6F, 0.5F)
                .texOffs(119, 61).addBox(-0.78F, -2.2F, -7.18F, 1.56F, 1.45F, 0.36F)
                .texOffs(118, 16).addBox(-0.3F, -1.83F, -7.31F, 0.6F, 0.6F, 0.14F)
                .texOffs(65, 39).addBox(-4.05F, -2.7F, -7.85F, 2F, 2.5F, 2F)
                .texOffs(101, 52).addBox(-4.17F, -2.55F, -8.22F, 2.24F, 2.2F, 0.38F)
                .texOffs(76, 62).addBox(-3.87F, -2.25F, -8.48F, 1.64F, 1.6F, 0.27F)
                .texOffs(87, 53).addBox(-3.87F, -0.18F, -7.7F, 1.6F, 0.32F, 1.7F)
                .texOffs(23, 65).addBox(-3.87F, -2.95F, -7.7F, 1.6F, 0.3F, 1.7F)
                .texOffs(75, 39).addBox(2.05F, -2.7F, -7.85F, 2F, 2.5F, 2F)
                .texOffs(109, 56).addBox(1.93F, -2.55F, -8.22F, 2.24F, 2.2F, 0.38F)
                .texOffs(32, 65).addBox(2.23F, -2.25F, -8.48F, 1.64F, 1.6F, 0.27F)
                .texOffs(117, 56).addBox(2.23F, -0.18F, -7.7F, 1.6F, 0.32F, 1.7F)
                .texOffs(38, 65).addBox(2.23F, -2.95F, -7.7F, 1.6F, 0.3F, 1.7F)
                .texOffs(126, 34).addBox(3.35F, -1.2F, -8.62F, 0.35F, 0.38F, 0.14F)
                .texOffs(65, 14).addBox(-5.18F, -3.2F, -5.15F, 0.32F, 0.6F, 10.3F)
                .texOffs(89, 14).addBox(4.86F, -3.2F, -5.15F, 0.32F, 0.6F, 10.3F)
                .texOffs(91, 68).addBox(-4.86F, -3.2F, 4.86F, 9.72F, 0.6F, 0.32F)
                .texOffs(96, 53).addBox(-4.86F, -2.95F, -5.5F, 0.9F, 0.9F, 0.55F)
                .texOffs(47, 65).addBox(3.95F, -2.95F, -5.5F, 0.9F, 0.9F, 0.55F)
                .texOffs(52, 69).addBox(-4.38F, -3.15F, -8.9F, 2.6F, 0.65F, 2.4F)
                .texOffs(64, 69).addBox(-4.38F, -0.4F, -8.9F, 2.6F, 0.65F, 2.4F)
                .texOffs(46, 76).addBox(-3.53F, -1.95F, -8.9F, 0.9F, 0.8F, 0.4F)
                .texOffs(76, 71).addBox(1.8F, -3.15F, -8.9F, 2.6F, 0.65F, 2.4F)
                .texOffs(88, 71).addBox(1.8F, -0.4F, -8.9F, 2.6F, 0.65F, 2.4F)
                .texOffs(105, 76).addBox(2.65F, -1.95F, -8.9F, 0.9F, 0.8F, 0.4F)
                .texOffs(72, 77).addBox(-1.3F, -3.35F, -7.15F, 2.6F, 0.55F, 1F)
                , PartPose.ZERO);
        root.addOrReplaceChild("anchor_mk2", CubeListBuilder.create()
                .texOffs(85, 39).addBox(-1.6428F, 7.1F, -3.73F, 3.2856F, 3.8F, 0.55F)
                .texOffs(95, 41).addBox(-1.628F, 7.2F, 2.85F, 3.256F, 3.8F, 0.55F)
                .texOffs(65, 27).addBox(-1.739F, 11.02F, -3.9F, 3.478F, 1.6F, 7.45F)
                .texOffs(65, 57).addBox(-1.443F, 10.45F, -3.96F, 2.886F, 0.55F, 2.4F)
                .texOffs(89, 27).addBox(-0.481F, 6F, 3.5F, 0.962F, 4.55F, 1.15F)
                .texOffs(113, 12).addBox(-0.259F, 6.35F, 4.68F, 0.518F, 3.55F, 0.55F)
                .texOffs(78, 57).addBox(-0.666F, 10F, 3.62F, 1.332F, 1.2F, 1.5F)
                .texOffs(121, 34).addBox(-1.443F, 8.1F, 3.42F, 0.407F, 2.5F, 0.6F)
                .texOffs(121, 40).addBox(1.036F, 8.1F, 3.42F, 0.407F, 2.5F, 0.6F)
                .texOffs(52, 65).addBox(-1.295F, 12.58F, -3.8F, 0.518F, 0.22F, 1F)
                .texOffs(58, 65).addBox(0.777F, 12.58F, -3.8F, 0.518F, 0.22F, 1F)
                .texOffs(114, 68).addBox(-0.555F, 8.45F, -3.9F, 1.11F, 0.55F, 0.18F)
                .texOffs(101, 48).addBox(-0.185F, 9.4F, -3.92F, 0.37F, 0.35F, 0.2F)
                .texOffs(125, 50).addBox(-1.85F, 7.5F, -4.18F, 0.55F, 3F, 0.4F)
                .texOffs(119, 68).addBox(-1.85F, 7.8F, 3.98F, 0.55F, 3.2F, 0.6F)
                .texOffs(124, 68).addBox(1.3F, 7.5F, -4.18F, 0.55F, 3F, 0.4F)
                .texOffs(100, 71).addBox(1.3F, 7.8F, 3.98F, 0.55F, 3.2F, 0.6F)
                .texOffs(33, 76).addBox(-1.65F, 10.1F, -4.35F, 3.3F, 0.45F, 1.9F)
                .texOffs(1, 79).addBox(-0.55F, 8.95F, -4.18F, 1.1F, 0.6F, 0.3F)
                .texOffs(1, 69).addBox(-1.75F, 12.6F, -3.7F, 3.5F, 0.55F, 7F)
                , PartPose.ZERO);
        // END NETHER GEAR GENERATED
        return LayerDefinition.create(mesh, 128, 128);
    }
}
