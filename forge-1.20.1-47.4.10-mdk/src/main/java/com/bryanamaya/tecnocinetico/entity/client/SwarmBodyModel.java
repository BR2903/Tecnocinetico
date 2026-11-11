package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import com.bryanamaya.tecnocinetico.entity.custom.SwarmBodyEntity;

/** Rig propio generado por tools/remodel_mobs.py; animaciones en RemodeledMobModel. */
public class SwarmBodyModel extends RemodeledMobModel<SwarmBodyEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "swarm_body"), "main");
    public SwarmBodyModel(ModelPart root) { super(root, "swarm_body"); }

    public static LayerDefinition createBodyLayer() { return RemodeledMobLayers.swarm_body(); }
}
