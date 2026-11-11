package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import com.bryanamaya.tecnocinetico.entity.swarm.HiveNodeEntity;

/** Rig propio generado por tools/remodel_mobs.py; animaciones en RemodeledMobModel. */
public class HiveNodeModel extends RemodeledMobModel<HiveNodeEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "hive_node"), "main");
    public HiveNodeModel(ModelPart root) { super(root, "hive_node"); }

    public static LayerDefinition createLayer() { return RemodeledMobLayers.hive_node(); }
}
