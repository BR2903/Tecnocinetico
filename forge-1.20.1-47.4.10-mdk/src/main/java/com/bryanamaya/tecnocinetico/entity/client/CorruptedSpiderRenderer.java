package com.bryanamaya.tecnocinetico.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Spider;

/**
 * Arañas Corruptas (normal x1,15 y de cueva x0,8): modelo vanilla de araña con textura propia
 * (tools/corrupted_spider_texture.py) y capa luminosa de ojos y cristales.
 */
public class CorruptedSpiderRenderer extends MobRenderer<Spider, CorruptedSpiderModel> {

    private static final ResourceLocation TEXTURE = SwarmWaspRenderer.tex("remodel/corrupted_spider");
    private static final RenderType GLOW = RenderType.eyes(SwarmWaspRenderer.tex("remodel/corrupted_spider_glow"));

    private final float scale;

    public CorruptedSpiderRenderer(EntityRendererProvider.Context ctx, float scale) {
        super(ctx, new CorruptedSpiderModel(ctx.bakeLayer(CorruptedSpiderModel.LAYER)), 0.9F * scale);
        this.scale = scale;
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    protected float getFlipDegrees(Spider e) {
        return 180.0F;
    }

    @Override
    protected void scale(Spider e, PoseStack pose, float partialTick) {
        pose.scale(scale, scale, scale);
    }

    @Override
    public ResourceLocation getTextureLocation(Spider e) {
        return TEXTURE;
    }
}
