package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import com.bryanamaya.tecnocinetico.entity.boss.SiegeSpiderEntity;

/** Rig propio generado por tools/remodel_mobs.py; animaciones en RemodeledMobModel. */
public class SiegeSpiderModel extends RemodeledMobModel<SiegeSpiderEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "siege_spider"), "main");
    public SiegeSpiderModel(ModelPart root) { super(root, "siege_spider"); }

    public static LayerDefinition createLayer() { return RemodeledMobLayers.siege_spider(); }
}
