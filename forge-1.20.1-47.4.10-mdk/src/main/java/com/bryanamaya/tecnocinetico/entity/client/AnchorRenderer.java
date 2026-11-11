package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.entity.custom.AnchorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Anclador: núcleo y cristal azul-violeta que brillan; parpadean blancos mientras carga el haz o el pisotón. */
public class AnchorRenderer extends MobRenderer<AnchorEntity, AnchorModel> {

    private static final ResourceLocation TEXTURE = SwarmWaspRenderer.tex("remodel/anchor");
    private static final RenderType GLOW = RenderType.eyes(SwarmWaspRenderer.tex("remodel/anchor_glow"));
    private static final RenderType GLOW_CHARGE = RenderType.eyes(SwarmWaspRenderer.tex("remodel/anchor_charge"));

    public AnchorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new AnchorModel(ctx.bakeLayer(AnchorModel.LAYER)), 1.0F);
        addLayer(new RenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, AnchorEntity e, float limbSwing, float limbSwingAmount,
                               float partialTick, float age, float headYaw, float headPitch) {
                boolean flash = e.mode() != 0 && (e.tickCount / 2) % 2 == 0;
                getParentModel().renderToBuffer(pose, buffers.getBuffer(flash ? GLOW_CHARGE : GLOW), 15728640,
                        OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(AnchorEntity e) {
        return TEXTURE;
    }
}
