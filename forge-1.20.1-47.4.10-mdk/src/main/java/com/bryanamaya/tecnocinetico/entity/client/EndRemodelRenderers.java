package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EndermanModel;
import net.minecraft.client.model.ShulkerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.ShulkerRenderer;
import net.minecraft.client.renderer.entity.layers.EnderEyesLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.ShulkerHeadLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Mobs vanilla del End corruptos por la red de la IA (arte de Codex: {@link EndRemodelLayers}, texturas en
 * {@code textures/entity/end_remodel/}). Se ven así <b>solo dentro del End</b>, como su comportamiento
 * ({@code end/EndMobEvents}); fuera usan el modelo y la textura vanilla. Mismo esquema que {@link NetherRemodelRenderers}:
 * se hereda el renderizador vanilla (animaciones, bloque en brazos, apertura de la tapa) y se cambian modelo, textura
 * y la capa emisiva {@code <nombre>_glow.png}.
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class EndRemodelRenderers {

    public static final ModelLayerLocation ENDERMAN = layer("enderman"), SHULKER = layer("shulker");

    private EndRemodelRenderers() {}

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "end_" + name), "main");
    }

    static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/entity/end_remodel/" + name + ".png");
    }

    /** El aspecto corrupto se usa solo en el End. */
    public static boolean inEnd(Entity e) {
        return e.level() != null && e.level().dimension() == Level.END;
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ENDERMAN, EndRemodelLayers::enderman);
        event.registerLayerDefinition(SHULKER, EndRemodelLayers::shulker);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityType.ENDERMAN, Endermen::new);
        event.registerEntityRenderer(EntityType.SHULKER, Shulkers::new);
    }

    // ------------------------------------------------------------------ enderman

    /** Enderman: los ojos vanilla solo fuera del End; dentro, el brillo magenta de sus ojos y líneas de datos. */
    public static final class Endermen extends EndermanRenderer {
        private final EndermanModel<EnderMan> vanilla, remodel;
        private final ResourceLocation texture = tex("enderman");

        Endermen(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.vanilla = model;
            this.remodel = new EndermanModel<>(ctx.bakeLayer(ENDERMAN));
            layers.replaceAll(l -> l instanceof EnderEyesLayer<?> eyes ? new OutsideEnd<>(this, castEyes(eyes)) : l);
            addLayer(new Glow<>(this, RenderType.eyes(tex("enderman_glow")), e -> e.isCreepy() ? 1.0F : 0.75F));
        }

        @SuppressWarnings("unchecked")
        private static RenderLayer<EnderMan, EndermanModel<EnderMan>> castEyes(EnderEyesLayer<?> eyes) {
            return (RenderLayer<EnderMan, EndermanModel<EnderMan>>) (RenderLayer<?, ?>) eyes;
        }

        @Override
        public void render(EnderMan e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            model = inEnd(e) ? remodel : vanilla;                             // antes de que el vanilla marque carrying/creepy
            super.render(e, yaw, partial, pose, buffers, light);
        }

        @Override
        public ResourceLocation getTextureLocation(EnderMan e) {
            return inEnd(e) ? texture : super.getTextureLocation(e);
        }
    }

    // ------------------------------------------------------------------ shulker

    /** Shulker: cápsula-torreta de servidor, una sola textura (sin los 16 colores) también en la cabeza. */
    public static final class Shulkers extends ShulkerRenderer {
        private final ShulkerModel<Shulker> vanilla, remodel;
        private static final ResourceLocation TEXTURE = tex("shulker");
        private static final RenderType GLOW = RenderType.eyes(tex("shulker_glow"));

        Shulkers(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.vanilla = model;
            this.remodel = new ShulkerModel<>(ctx.bakeLayer(SHULKER));
            layers.removeIf(l -> l instanceof ShulkerHeadLayer);
            addLayer(new Head(this));
            addLayer(new RenderLayer<>(this) {                                 // brillo de tapa, base y lente de la cabeza
                @Override
                public void render(PoseStack pose, MultiBufferSource buffers, int light, Shulker e, float swing, float amount,
                                   float partial, float age, float yaw, float pitch) {
                    if (!inEnd(e) || e.isInvisible()) {
                        return;
                    }
                    var buffer = buffers.getBuffer(GLOW);
                    getParentModel().renderToBuffer(pose, buffer, 15728640, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
                    getParentModel().getHead().render(pose, buffer, 15728640, OverlayTexture.NO_OVERLAY);
                }
            });
        }

        @Override
        public void render(Shulker e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            model = inEnd(e) ? remodel : vanilla;
            super.render(e, yaw, partial, pose, buffers, light);
        }

        @Override
        public ResourceLocation getTextureLocation(Shulker e) {
            return inEnd(e) ? TEXTURE : super.getTextureLocation(e);
        }

        /** Como {@link ShulkerHeadLayer}, pero con la textura corrupta dentro del End. */
        private static final class Head extends RenderLayer<Shulker, ShulkerModel<Shulker>> {
            Head(RenderLayerParent<Shulker, ShulkerModel<Shulker>> parent) {
                super(parent);
            }

            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, Shulker e, float swing, float amount,
                               float partial, float age, float yaw, float pitch) {
                ResourceLocation texture = inEnd(e) ? TEXTURE : ShulkerRenderer.getTextureLocation(e.getColor());
                getParentModel().getHead().render(pose, buffers.getBuffer(RenderType.entitySolid(texture)), light,
                        LivingEntityRenderer.getOverlayCoords(e, 0.0F));
            }
        }
    }

    // ------------------------------------------------------------------ capas

    /** Capa emisiva solo dentro del End. */
    private static final class Glow<T extends EnderMan, M extends EndermanModel<T>> extends RenderLayer<T, M> {
        private final RenderType type;
        private final java.util.function.Function<T, Float> brightness;

        Glow(RenderLayerParent<T, M> parent, RenderType type, java.util.function.Function<T, Float> brightness) {
            super(parent);
            this.type = type;
            this.brightness = brightness;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, T e, float swing, float amount, float partial,
                           float age, float yaw, float pitch) {
            if (!inEnd(e) || e.isInvisible()) {
                return;
            }
            float b = brightness.apply(e);
            getParentModel().renderToBuffer(pose, buffers.getBuffer(type), 15728640, OverlayTexture.NO_OVERLAY, b, b, b, 1.0F);
        }
    }

    /** Envuelve una capa vanilla para que solo se dibuje fuera del End. */
    private static final class OutsideEnd<T extends EnderMan, M extends EndermanModel<T>> extends RenderLayer<T, M> {
        private final RenderLayer<T, M> inner;

        OutsideEnd(RenderLayerParent<T, M> parent, RenderLayer<T, M> inner) {
            super(parent);
            this.inner = inner;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, T e, float swing, float amount, float partial,
                           float age, float yaw, float pitch) {
            if (!inEnd(e)) {
                inner.render(pose, buffers, light, e, swing, amount, partial, age, yaw, pitch);
            }
        }
    }
}
