package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.entity.custom.SwarmBodyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Comparte el modelo y las texturas propias del Enjambre a escala de cría. */
public class MatriarchMinionRenderer extends SwarmBodyRenderer {
    public MatriarchMinionRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = .3F;
    }
    @Override protected void scale(SwarmBodyEntity e, PoseStack pose, float partial) { pose.scale(.45F, .45F, .45F); }
}
