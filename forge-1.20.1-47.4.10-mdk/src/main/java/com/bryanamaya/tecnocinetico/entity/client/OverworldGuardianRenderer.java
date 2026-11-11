package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.boss.OverworldGuardianEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class OverworldGuardianRenderer extends MobRenderer<OverworldGuardianEntity, OverworldGuardianModel> {
    private final ResourceLocation texture;
    public OverworldGuardianRenderer(EntityRendererProvider.Context ctx, boolean watcher) {
        super(ctx, new OverworldGuardianModel(ctx.bakeLayer(watcher ? OverworldGuardianModel.WATCHER : OverworldGuardianModel.MATRIARCH), watcher), 1.8F);
        String id = watcher ? "network_watcher" : "swarm_matriarch";
        texture = tex(id);
        addLayer(new RenderLayer<>(this) {
            @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, OverworldGuardianEntity e,
                    float swing, float amount, float partial, float age, float yaw, float pitch) {
                String suffix = e.mode() == 3 ? "_exposed" : (e.mode() == 1 || e.mode() == 2) && e.tickCount % 4 < 2 ? "_charge" : "_glow";
                getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(tex(id + suffix))), 15728640,
                        OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
            }
        });
    }
    private static ResourceLocation tex(String id) {
        return ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/entity/" + id + ".png");
    }
    @Override protected void scale(OverworldGuardianEntity e, PoseStack pose, float partial) { pose.scale(1.4F, 1.4F, 1.4F); }
    @Override public ResourceLocation getTextureLocation(OverworldGuardianEntity e) { return texture; }
}
