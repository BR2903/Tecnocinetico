package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedSkin;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Monster;

import java.util.HashMap;
import java.util.Map;

/**
 * Corruptos humanos (Corrupto y Tirador Corrupto): textura por variante en textures/entity/remodel/corrupted/ y capa
 * luminosa (_glow: ojos y cristales de cinetrita) a plena luz. Muestra armadura y el arma en la mano.
 */
public class CorruptedHumanRenderer<T extends Monster & CorruptedSkin> extends HumanoidMobRenderer<T, CorruptedHumanModel<T>> {

    private static final Map<String, ResourceLocation> TEX = new HashMap<>();
    private static final Map<String, RenderType> GLOW = new HashMap<>();

    public CorruptedHumanRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CorruptedHumanModel<>(ctx.bakeLayer(CorruptedHumanModel.LAYER)), 0.5F);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
        addLayer(new RenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, T e, float limbSwing, float limbSwingAmount,
                               float partialTick, float age, float headYaw, float headPitch) {
                RenderType rt = GLOW.computeIfAbsent(e.skinName(), n -> RenderType.eyes(tex(n + "_glow")));
                getParentModel().renderToBuffer(pose, buffers.getBuffer(rt), 15728640, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            }
        });
    }

    private static ResourceLocation tex(String name) {
        return TEX.computeIfAbsent(name, n -> ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID,
                "textures/entity/remodel/corrupted/" + n + ".png"));
    }

    @Override
    public ResourceLocation getTextureLocation(T e) {
        return tex(e.skinName());
    }
}
