package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.entity.custom.DroneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Dron del Núcleo. Capa luminosa según el modo: sensor cian en patrulla, rojo al atacar y rojo parpadeante
 * mientras carga la ráfaga.
 */
public class DroneRenderer extends MobRenderer<DroneEntity, DroneModel> {

    private static final ResourceLocation TEXTURE = SwarmWaspRenderer.tex("remodel/drone");
    private static final RenderType GLOW = RenderType.eyes(SwarmWaspRenderer.tex("remodel/drone_glow"));
    private static final RenderType GLOW_ALERT = RenderType.eyes(SwarmWaspRenderer.tex("remodel/drone_alert"));

    public DroneRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DroneModel(ctx.bakeLayer(DroneModel.LAYER)), 0.5F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, DroneEntity e,
                               float limbSwing, float limbSwingAmount, float partialTick, float age, float headYaw, float headPitch) {
                int mode = e.mode();
                if (mode == 2 && (e.tickCount / 2) % 2 == 1) {
                    return;                                                     // parpadea mientras carga
                }
                var vc = buffers.getBuffer(mode == 0 ? GLOW : GLOW_ALERT);
                getParentModel().renderToBuffer(pose, vc, 15728640, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            }

            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(DroneEntity e) {
        return TEXTURE;
    }
}
