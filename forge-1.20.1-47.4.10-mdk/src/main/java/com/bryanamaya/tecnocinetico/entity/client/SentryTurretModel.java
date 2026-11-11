package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import com.bryanamaya.tecnocinetico.entity.custom.SentryTurretEntity;

/** Rig propio generado por tools/remodel_mobs.py; animaciones en RemodeledMobModel. */
public class SentryTurretModel extends RemodeledMobModel<SentryTurretEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "sentry_turret"), "main");
    public SentryTurretModel(ModelPart root) { super(root, "sentry_turret"); }

    public static LayerDefinition createLayer() { return RemodeledMobLayers.sentry_turret(); }
}
