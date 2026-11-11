package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.swarm.SwarmWaspEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Avispa del Enjambre: textura según su papel y capa luminosa (ojos y bandas de cinetrita). */
public class SwarmWaspRenderer extends MobRenderer<SwarmWaspEntity, SwarmWaspModel<SwarmWaspEntity>> {

    private static final RenderType GLOW = RenderType.eyes(tex("remodel/swarm_wasp/glow"));

    public SwarmWaspRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SwarmWaspModel<>(ctx.bakeLayer(SwarmWaspModel.LAYER)), 0.3F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    static ResourceLocation tex(String path) {
        return ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/entity/" + path + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(SwarmWaspEntity e) {
        return tex("remodel/swarm_wasp/" + e.getRole().id);
    }

    @Override
    protected void scale(SwarmWaspEntity e, PoseStack pose, float partialTick) {
        float s = e.getRole().scale;
        pose.scale(s, s, s);
    }
}
