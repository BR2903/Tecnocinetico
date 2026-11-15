package com.bryanamaya.tecnocinetico.entity.client;
import com.bryanamaya.tecnocinetico.client.GlowMask;
import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.nether.SlagDevourerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
/** Solo presentación. Generado por tools/slag_devourer_gen.py. */
public class SlagDevourerRenderer extends MobRenderer<SlagDevourerEntity, SlagDevourerModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/entity/slag_devourer.png");
    private static final RenderType GLOW = GlowMask.eyes(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/entity/slag_devourer_glow.png"));
    public SlagDevourerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SlagDevourerModel(ctx.bakeLayer(SlagDevourerModel.LAYER)), 1.4F);
        addLayer(new RenderLayer<SlagDevourerEntity, SlagDevourerModel>(this) {
            @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, SlagDevourerEntity e,
                    float swing, float amount, float partial, float age, float yaw, float pitch) {
                if (e.burrowed()) return;
                float brightness = e.exposed() ? 1.0F : e.enraged() ? 0.85F : 0.55F;
                if (!e.exposed()) brightness += 0.04F*(float)Math.sin(age*0.25F)+0.1F*e.windup();
                GlowMask.render(getParentModel(), GLOW, pose, buffers, brightness, brightness, brightness, 1.0F);
            }
        });
    }
    @Override public void render(SlagDevourerEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        shadowRadius = e.burrowed() ? 0 : 1.4F;
        if (!e.burrowed()) super.render(e, yaw, partial, pose, buffers, light);
    }
    @Override public boolean shouldRender(SlagDevourerEntity e, Frustum frustum, double x, double y, double z) {
        return !e.burrowed() && e.shouldRender(x, y, z) && frustum.isVisible(e.getBoundingBox().inflate(7.5, 2.5, 7.5));
    }
    @Override public ResourceLocation getTextureLocation(SlagDevourerEntity e) { return TEXTURE; }
    @Override protected void scale(SlagDevourerEntity e, PoseStack pose, float partial) { pose.scale(2, 2, 2); }
}
