package com.bryanamaya.tecnocinetico.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Creeper;

/**
 * Creeper Corrupto: el renderizador vanilla (hinchado, destello, capa de carga eléctrica) con textura propia
 * (tools/corrupted_creeper_texture.py) y grietas de cinetrita que brillan más cuanto más cerca está de estallar.
 */
public class CorruptedCreeperRenderer extends CreeperRenderer {

    private static final ResourceLocation TEXTURE = SwarmWaspRenderer.tex("remodel/corrupted_creeper");
    private static final RenderType GLOW = RenderType.eyes(SwarmWaspRenderer.tex("remodel/corrupted_creeper_glow"));

    public CorruptedCreeperRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new RemodeledCreeperModel(ctx.bakeLayer(RemodeledCreeperModel.LAYER));
        addLayer(new RenderLayer<Creeper, CreeperModel<Creeper>>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, Creeper e, float limbSwing, float limbSwingAmount,
                               float partialTick, float age, float headYaw, float headPitch) {
                float swell = e.getSwelling(partialTick);
                float b = 0.45F + 0.15F * (float) Math.sin(age * 0.12F) + 0.55F * Math.min(1.0F, swell * 1.5F);
                getParentModel().renderToBuffer(pose, buffers.getBuffer(GLOW), 15728640, OverlayTexture.NO_OVERLAY, b, b, b, 1.0F);
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(Creeper e) {
        return TEXTURE;
    }
}
