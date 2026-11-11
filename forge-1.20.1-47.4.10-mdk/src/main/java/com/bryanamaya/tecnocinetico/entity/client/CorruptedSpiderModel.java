package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Spider;

/** Rig propio generado por tools/remodel_mobs.py; animaciones en RemodeledMobModel. */
public class CorruptedSpiderModel extends RemodeledMobModel<Spider> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "corrupted_spider"), "main");
    public CorruptedSpiderModel(ModelPart root) { super(root, "corrupted_spider"); }

    public static LayerDefinition createLayer() { return RemodeledMobLayers.corrupted_spider(); }
}
