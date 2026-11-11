package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Creeper;

/** Conserva marcha, hinchado y destello vanilla con un cuerpo biomecánico y depósitos propios. */
public class RemodeledCreeperModel extends CreeperModel<Creeper> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "corrupted_creeper"), "main");
    public RemodeledCreeperModel(ModelPart root) { super(root); }
    public static LayerDefinition createLayer() { return RemodeledMobLayers.creeper(); }
}
