package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.custom.SwarmBodyEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

public class SwarmBodyRenderer extends MobRenderer<SwarmBodyEntity, SwarmBodyModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/entity/remodel/swarm_body.png");
    private static final RenderType GLOW =
            RenderType.eyes(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/entity/remodel/swarm_body_glow.png"));

    public SwarmBodyRenderer(EntityRendererProvider.Context context) {
        super(context, new SwarmBodyModel(context.bakeLayer(SwarmBodyModel.LAYER)), 0.75F);
        // Núcleo de Cinetrita y ojos: brillan siempre, incluso a oscuras
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(SwarmBodyEntity entity) {
        return TEXTURE;
    }
}
