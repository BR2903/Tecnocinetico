package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.nether.InfernalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Enemigos del Nether: modelo de {@link InfernalModel} (tipo 0 Fundidor, 1 Devorador, 2 Ácaro, 3 Centinela),
 * textura {@code textures/entity/<id>.png} y capa emisiva {@code <id>_glow.png} (se atenúa con el núcleo expuesto).
 */
public class InfernalRenderer extends MobRenderer<InfernalEntity, InfernalModel> {

    public static final int MITE = 2, SENTINEL = 3;

    private final ResourceLocation texture;
    private final float scale;

    public InfernalRenderer(EntityRendererProvider.Context ctx, int kind, String id, float scale, float shadow) {
        super(ctx, new InfernalModel(kind), shadow);
        this.scale = scale;
        this.texture = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/entity/" + id + ".png");
        RenderType glow = RenderType.eyes(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/entity/" + id + "_glow.png"));
        addLayer(new RenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, InfernalEntity e, float swing, float amount,
                               float partial, float age, float yaw, float pitch) {
                // cargando un ataque: el brillo late
                float b = e.attack() != 0 ? 0.75F + 0.25F * (float) Math.sin(age * 0.9F) : 1.0F;
                getParentModel().renderToBuffer(pose, buffers.getBuffer(glow), 15728640, OverlayTexture.NO_OVERLAY,
                        b, e.exposed() ? 0.4F * b : b, b, 1.0F);
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(InfernalEntity e) {
        return texture;
    }

    @Override
    protected void scale(InfernalEntity e, PoseStack pose, float partial) {
        pose.scale(scale, scale, scale);
    }
}
