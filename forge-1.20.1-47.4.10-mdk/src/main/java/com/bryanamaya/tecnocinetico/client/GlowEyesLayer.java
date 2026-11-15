package com.bryanamaya.tecnocinetico.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.world.entity.Entity;

/** EyesLayer que solo dibuja las piezas que brillan ({@link GlowMask}); su renderType() debe venir de GlowMask.eyes. */
public abstract class GlowEyesLayer<T extends Entity, M extends EntityModel<T>> extends EyesLayer<T, M> {

    protected GlowEyesLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float swing, float amount, float partial,
                       float age, float yaw, float pitch) {
        GlowMask.render(getParentModel(), renderType(), pose, buffers, 1.0F, 1.0F, 1.0F, 1.0F);
    }
}
