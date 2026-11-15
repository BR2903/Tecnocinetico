package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.end.EndRelayTurretEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Torreta-repetidor del End (modelo de Codex, {@link EndRelayTurretModel}). La base no gira; la cabeza apunta con su
 * rotación absoluta. El iris se abre con la carga del disparo y el brillo sube con ella. Mientras cura al Dragón, dibuja
 * el haz de los cristales del End desde el emisor hasta él (el mismo dibujo vanilla del cristal).
 */
public class EndRelayTurretRenderer extends MobRenderer<EndRelayTurretEntity, EndRelayTurretModel<EndRelayTurretEntity>> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "end_relay_turret"), "main");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/entity/end_relay_turret.png");
    private static final RenderType GLOW = RenderType.eyes(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/entity/end_relay_turret_glow.png"));
    /** Altura del emisor sobre los pies (Codex). */
    private static final float MUZZLE_Y = 1.0F;

    public EndRelayTurretRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new EndRelayTurretModel<>(ctx.bakeLayer(LAYER)), 0.7F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, EndRelayTurretEntity e, float swing, float amount,
                               float partial, float age, float yaw, float pitch) {
                float b = 0.6F + 0.4F * e.charge();
                getParentModel().renderToBuffer(pose, buffers.getBuffer(GLOW), 15728640, OverlayTexture.NO_OVERLAY, b, b, b, 1.0F);
            }

            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(EndRelayTurretEntity e) {
        return TEXTURE;
    }

    @Override
    public void render(EndRelayTurretEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        e.yBodyRot = 0.0F;                                                    // base atornillada
        e.yBodyRotO = 0.0F;
        model.charge = e.charge();
        super.render(e, 0.0F, partial, pose, buffers, light);
        Entity target = e.level().getEntity(e.healTarget());
        if (target != null) {                                                 // haz de curación hacia el Dragón
            float tx = (float) (Mth.lerp(partial, target.xo, target.getX()) - Mth.lerp(partial, e.xo, e.getX()));
            float ty = (float) (Mth.lerp(partial, target.yo, target.getY()) + 1.5 - Mth.lerp(partial, e.yo, e.getY()) - MUZZLE_Y);
            float tz = (float) (Mth.lerp(partial, target.zo, target.getZ()) - Mth.lerp(partial, e.zo, e.getZ()));
            pose.pushPose();
            pose.translate(0, MUZZLE_Y, 0);
            EnderDragonRenderer.renderCrystalBeams(tx, ty, tz, partial, e.tickCount, pose, buffers, light);
            pose.popPose();
        }
    }

    /** Se dibuja aunque esté fuera de la vista si su haz cruza la pantalla. */
    @Override
    public boolean shouldRender(EndRelayTurretEntity e, Frustum frustum, double x, double y, double z) {
        return super.shouldRender(e, frustum, x, y, z) || e.healTarget() >= 0;
    }
}
