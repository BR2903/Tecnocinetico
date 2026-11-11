package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import com.bryanamaya.tecnocinetico.entity.custom.AnchorEntity;

/** Rig propio generado por tools/remodel_mobs.py; animaciones en RemodeledMobModel. */
public class AnchorModel extends RemodeledMobModel<AnchorEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "anchor"), "main");
    public AnchorModel(ModelPart root) { super(root, "anchor"); }

    public static LayerDefinition createLayer() { return RemodeledMobLayers.anchor(); }
}
