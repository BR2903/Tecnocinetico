package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.entity.ship.CoreFighterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Caza del Núcleo: rumbo, cabeceo y alabeo (se inclina al girar), capa luminosa (toberas, líneas cian, luces de
 * navegación) y la animación del salto: tiembla cada vez más mientras carga y, en el último medio segundo, se
 * estira hacia delante como si saliera disparada.
 */
public class CoreFighterRenderer extends EntityRenderer<CoreFighterEntity> {

    private static final ResourceLocation TEXTURE = SwarmWaspRenderer.tex("core_fighter");
    private static final RenderType GLOW = RenderType.eyes(SwarmWaspRenderer.tex("core_fighter_glow"));
    private final CoreFighterModel model;

    public CoreFighterRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new CoreFighterModel(ctx.bakeLayer(CoreFighterModel.LAYER));
        this.shadowRadius = 1.6F;
    }

    @Override
    public void render(CoreFighterEntity e, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        int j = e.jumpTicks();
        if (j > 0) {                                                                 // vibración del motor de salto
            float k = j / (float) CoreFighterEntity.JUMP_TICKS;
            float t = e.tickCount + pt;
            pose.translate(Mth.sin(t * 2.9F) * 0.05F * k, Mth.cos(t * 3.7F) * 0.04F * k, Mth.sin(t * 4.3F) * 0.03F * k);
        }
        pose.translate(0.0D, 0.75D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - Mth.rotLerp(pt, e.yRotO, e.getYRot())));
        pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(pt, e.xRotO, e.getXRot())));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(pt, e.rollO, e.roll)));
        if (j > CoreFighterEntity.JUMP_TICKS - 10) {                                // estirón final
            float s = (j - (CoreFighterEntity.JUMP_TICKS - 10) + pt) / 10.0F;
            pose.scale(1.0F - 0.3F * s, 1.0F - 0.3F * s, 1.0F + 2.5F * s * s);
        }
        pose.translate(0.0D, -0.75D, 0.0D);
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(0.0D, -1.501D, 0.0D);
        Minecraft mc = Minecraft.getInstance();
        boolean pilotView = e.hasPassenger(mc.player) && mc.options.getCameraType().isFirstPerson();
        model.configure(e.onGround(), !pilotView);
        model.renderToBuffer(pose, buffers.getBuffer(model.renderType(TEXTURE)), light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        model.renderToBuffer(pose, buffers.getBuffer(GLOW), 15728640, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();
        super.render(e, yaw, pt, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(CoreFighterEntity e) {
        return TEXTURE;
    }
}
