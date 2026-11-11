package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.entity.swarm.HiveNodeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Nodo de Colmena: el modelo de la avispa a gran escala, con aleteo más lento y textura de reina (cobre y núcleo). */
public class HiveNodeRenderer extends MobRenderer<HiveNodeEntity, HiveNodeModel> {

    private static final float SCALE = 1.0F;
    private static final ResourceLocation TEXTURE = SwarmWaspRenderer.tex("remodel/hive_node");
    private static final RenderType GLOW = RenderType.eyes(SwarmWaspRenderer.tex("remodel/hive_node_glow"));

    public HiveNodeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HiveNodeModel(ctx.bakeLayer(HiveNodeModel.LAYER)), 1.1F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(HiveNodeEntity e) {
        return TEXTURE;
    }

    @Override
    protected void scale(HiveNodeEntity e, PoseStack pose, float partialTick) {
        pose.scale(SCALE, SCALE, SCALE);
    }
}
