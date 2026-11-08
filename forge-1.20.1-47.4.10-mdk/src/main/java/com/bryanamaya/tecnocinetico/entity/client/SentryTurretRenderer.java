package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.entity.custom.SentryTurretEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Torreta Centinela. El cuerpo no gira (está atornillada): se fuerza la rotación del cuerpo a 0 y la cabeza
 * usa la rotación absoluta. Capa luminosa: sensor cian, que pasa a rojo mientras carga el disparo.
 */
public class SentryTurretRenderer extends MobRenderer<SentryTurretEntity, SentryTurretModel> {

    private static final ResourceLocation TEXTURE = SwarmWaspRenderer.tex("sentry_turret");
    private static final RenderType GLOW = RenderType.eyes(SwarmWaspRenderer.tex("sentry_turret_glow"));
    private static final RenderType GLOW_CHARGE = RenderType.eyes(SwarmWaspRenderer.tex("sentry_turret_charge"));

    public SentryTurretRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SentryTurretModel(ctx.bakeLayer(SentryTurretModel.LAYER)), 0.6F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, SentryTurretEntity e,
                               float limbSwing, float limbSwingAmount, float partialTick, float age, float headYaw, float headPitch) {
                var vc = buffers.getBuffer(e.isCharging() && (e.tickCount / 2) % 2 == 0 ? GLOW_CHARGE : GLOW);
                getParentModel().renderToBuffer(pose, vc, 15728640, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            }

            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(SentryTurretEntity e) {
        return TEXTURE;
    }

    @Override
    public void render(SentryTurretEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        // base fija: sin rotación del cuerpo; la cabeza gira con su rotación absoluta (netHeadYaw = cabeza - cuerpo)
        e.yBodyRot = 0.0F;
        e.yBodyRotO = 0.0F;
        super.render(e, 0.0F, partialTick, pose, buffers, light);
    }
}
