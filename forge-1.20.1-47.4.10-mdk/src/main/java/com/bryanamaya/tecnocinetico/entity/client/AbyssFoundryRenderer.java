package com.bryanamaya.tecnocinetico.entity.client;
import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.nether.AbyssFoundryEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
/** Render exclusivamente visual. Generado por tools/abyss_foundry_gen.py. */
public class AbyssFoundryRenderer extends MobRenderer<AbyssFoundryEntity, AbyssFoundryModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/entity/abyss_foundry.png");
    private static final RenderType GLOW = RenderType.eyes(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/entity/abyss_foundry_glow.png"));
    public AbyssFoundryRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new AbyssFoundryModel(ctx.bakeLayer(AbyssFoundryModel.LAYER)), 1.5F);
        addLayer(new RenderLayer<AbyssFoundryEntity, AbyssFoundryModel>(this) {
            @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, AbyssFoundryEntity e,
                    float swing, float amount, float partial, float age, float yaw, float pitch) {
                float brightness = e.exposed() ? 1.0F : e.enraged() ? 0.85F : 0.58F;
                if (!e.exposed()) brightness += 0.05F*(float)Math.sin(age*0.3F) + 0.1F*e.windup();
                getParentModel().renderToBuffer(pose, buffers.getBuffer(GLOW), 15728640, OverlayTexture.NO_OVERLAY,
                        brightness, brightness, brightness, 1.0F);
            }
        });
    }
    @Override public ResourceLocation getTextureLocation(AbyssFoundryEntity e) { return TEXTURE; }
    @Override protected void scale(AbyssFoundryEntity e, PoseStack pose, float partial) { pose.scale(2.0F, 2.0F, 2.0F); }
}
