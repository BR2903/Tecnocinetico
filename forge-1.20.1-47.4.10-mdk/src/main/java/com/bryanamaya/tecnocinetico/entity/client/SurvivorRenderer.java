package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.custom.SurvivorEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Superviviente: modelo de jugador (con capas exteriores: capucha, chaqueta, mangas) y una textura de 64x64
 * por oficio en textures/entity/remodel/survivor/ (generadas por tools/survivor_skin_gen.py).
 */
public class SurvivorRenderer extends HumanoidMobRenderer<SurvivorEntity, SurvivorModel> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "survivor"), "main");

    public SurvivorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SurvivorModel(ctx.bakeLayer(LAYER)), 0.5F);
    }

    public static LayerDefinition createLayer() {
        return RemodeledMobLayers.survivor();
    }

    @Override
    public ResourceLocation getTextureLocation(SurvivorEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID,
                "textures/entity/remodel/survivor/" + entity.getRole().id + ".png");
    }
}
