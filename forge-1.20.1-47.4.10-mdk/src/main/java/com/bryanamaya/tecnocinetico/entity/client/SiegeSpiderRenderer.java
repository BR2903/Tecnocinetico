package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.entity.boss.SiegeSpiderEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Araña de Asedio: modelo x2, sensor y núcleo que brillan (rojo en la fase 3, blanco al cargar un ataque). */
public class SiegeSpiderRenderer extends MobRenderer<SiegeSpiderEntity, SiegeSpiderModel> {

    private static final ResourceLocation TEXTURE = SwarmWaspRenderer.tex("siege_spider");
    private static final RenderType GLOW = RenderType.eyes(SwarmWaspRenderer.tex("siege_spider_glow"));
    private static final RenderType GLOW_RAGE = RenderType.eyes(SwarmWaspRenderer.tex("siege_spider_rage"));
    private static final RenderType GLOW_CHARGE = RenderType.eyes(SwarmWaspRenderer.tex("siege_spider_charge"));

    public SiegeSpiderRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SiegeSpiderModel(ctx.bakeLayer(SiegeSpiderModel.LAYER)), 3.0F);
        addLayer(new RenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, SiegeSpiderEntity e, float limbSwing,
                               float limbSwingAmount, float pt, float age, float headYaw, float headPitch) {
                RenderType rt = e.mode() != 0 && (e.tickCount / 2) % 2 == 0 ? GLOW_CHARGE
                        : e.getHealth() / e.getMaxHealth() < 0.3F ? GLOW_RAGE : GLOW;
                getParentModel().renderToBuffer(pose, buffers.getBuffer(rt), 15728640, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            }
        });
    }

    @Override
    protected void scale(SiegeSpiderEntity e, PoseStack pose, float pt) {
        pose.scale(2.0F, 2.0F, 2.0F);
    }

    @Override
    protected float getFlipDegrees(SiegeSpiderEntity e) {
        return 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(SiegeSpiderEntity e) {
        return TEXTURE;
    }
}
