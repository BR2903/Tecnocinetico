package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedSpiderEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Araña Corrupta: modelo vanilla de araña, algo más grande, con textura propia (tools/corrupted_spider_texture.py)
 * y capa luminosa de ojos y cristales.
 */
public class CorruptedSpiderRenderer extends MobRenderer<CorruptedSpiderEntity, SpiderModel<CorruptedSpiderEntity>> {

    private static final ResourceLocation TEXTURE = SwarmWaspRenderer.tex("corrupted_spider");
    private static final RenderType GLOW = RenderType.eyes(SwarmWaspRenderer.tex("corrupted_spider_glow"));

    public CorruptedSpiderRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SpiderModel<>(ctx.bakeLayer(ModelLayers.SPIDER)), 1.0F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    protected float getFlipDegrees(CorruptedSpiderEntity e) {
        return 180.0F;
    }

    @Override
    protected void scale(CorruptedSpiderEntity e, PoseStack pose, float partialTick) {
        pose.scale(1.15F, 1.15F, 1.15F);
    }

    @Override
    public ResourceLocation getTextureLocation(CorruptedSpiderEntity e) {
        return TEXTURE;
    }
}
