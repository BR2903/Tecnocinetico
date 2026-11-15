package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.end.DataSpecterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;

/**
 * Espectro de Datos (modelo de Codex, {@link DataSpecterModel}). Translúcido: desfasado es una proyección tenue que
 * parpadea; al materializarse gana opacidad hasta ser sólido. Brillo emisivo aparte.
 */
public class DataSpecterRenderer extends MobRenderer<DataSpecterEntity, DataSpecterModel<DataSpecterEntity>> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/entity/data_specter.png");
    private static final RenderType GLOW = RenderType.eyes(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/entity/data_specter_glow.png"));

    /** Opacidad del cuerpo en este fotograma (la lee el modelo al dibujarse). */
    private float alpha = 1.0F;

    public DataSpecterRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, null, 0.0F);
        this.model = new DataSpecterModel<>(ctx.bakeLayer(DataSpecterModel.LAYER)) {
            @Override
            public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
                var core = root().getChild("core");                          // el emisor es físico: siempre opaco
                core.visible = false;
                super.renderToBuffer(pose, vc, light, overlay, r, g, b, a * alpha);
                core.visible = true;
                pose.pushPose();
                root().translateAndRotate(pose);
                core.render(pose, vc, light, overlay, r, g, b, a);
                pose.popPose();
            }
        };
        addLayer(new EyesLayer<>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, DataSpecterEntity e, float swing, float amount,
                               float partial, float age, float yaw, float pitch) {
                float m = e.materialize(partial);
                float k = Mth.clamp(0.35F + 0.65F * Math.max(m, alpha), 0.0F, 1.0F);
                getParentModel().renderToBuffer(pose, buffers.getBuffer(GLOW), 15728640, OverlayTexture.NO_OVERLAY, k, k, k, 1.0F);
            }

            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    public void render(DataSpecterEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float m = e.materialize(partial), age = e.tickCount + partial;
        float flicker = Mth.sin(age * 1.7F + e.getId()) > 0.85F || Mth.sin(age * 0.53F) > 0.97F ? 0.12F : 0.4F;   // proyección inestable
        alpha = Mth.lerp(m, flicker, 1.0F);
        model.materialize = m;
        model.attack = e.attackAnim(partial);
        model.pulse = e.pulseAnim(partial);
        super.render(e, yaw, partial, pose, buffers, light);
    }

    @Nullable
    @Override
    protected RenderType getRenderType(DataSpecterEntity e, boolean visible, boolean translucent, boolean glowing) {
        return RenderType.entityTranslucent(TEXTURE);
    }

    @Override
    public ResourceLocation getTextureLocation(DataSpecterEntity e) {
        return TEXTURE;
    }
}
