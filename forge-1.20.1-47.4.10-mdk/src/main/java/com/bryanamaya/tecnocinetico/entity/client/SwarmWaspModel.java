package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** Rig propio generado por tools/remodel_mobs.py; animaciones en RemodeledMobModel. */
public class SwarmWaspModel<T extends Mob> extends RemodeledMobModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "swarm_wasp"), "main");
    public SwarmWaspModel(ModelPart root) { super(root, "swarm_wasp"); }
    public SwarmWaspModel(ModelPart root, float speed) { this(root); this.flapSpeed = speed; }
    public static LayerDefinition createLayer() { return RemodeledMobLayers.swarm_wasp(); }
}
