package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.BlazeModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.GhastModel;
import net.minecraft.client.model.PiglinModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.BlazeRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.GhastRenderer;
import net.minecraft.client.renderer.entity.PiglinRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.WitherSkeletonRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Mobs vanilla del Nether remodelados (arte de Codex: {@link NetherRemodelLayers}, texturas en
 * {@code textures/entity/nether_remodel/}). Se ven así <b>solo dentro del Nether</b>: fuera (p. ej. un piglin que
 * cruza un portal) usan su modelo y textura vanilla, igual que su comportamiento (ver {@code nether/NetherMobEvents}).
 * <p>
 * Cada renderizador hereda del vanilla (escala, bebés, armadura, objeto en la mano, cabeza del ghast cargando...) y
 * solo cambia el modelo, la textura y añade la capa emisiva {@code <nombre>_glow.png}. Los modelos nuevos usan los
 * mismos huesos que los vanilla, así que las animaciones vanilla les valen.
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NetherRemodelRenderers {

    public static final ModelLayerLocation PIGLIN = layer("piglin"), PIGLIN_BRUTE = layer("piglin_brute"),
            ZOMBIFIED_PIGLIN = layer("zombified_piglin"), GHAST = layer("ghast"), BLAZE = layer("blaze"),
            WITHER_SKELETON = layer("wither_skeleton");

    private NetherRemodelRenderers() {}

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "nether_" + name), "main");
    }

    static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/entity/nether_remodel/" + name + ".png");
    }

    /** El aspecto remodelado se usa solo en el Nether. */
    public static boolean inNether(Entity e) {
        return e.level() != null && e.level().dimension() == Level.NETHER;
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PIGLIN, NetherRemodelLayers::piglin);
        event.registerLayerDefinition(PIGLIN_BRUTE, NetherRemodelLayers::piglin_brute);
        event.registerLayerDefinition(ZOMBIFIED_PIGLIN, NetherRemodelLayers::zombified_piglin);
        event.registerLayerDefinition(GHAST, NetherRemodelLayers::ghast);
        event.registerLayerDefinition(BLAZE, NetherRemodelLayers::blaze);
        event.registerLayerDefinition(WITHER_SKELETON, NetherRemodelLayers::wither_skeleton);
    }

    /** Sustituye los renderizadores vanilla (mismos parámetros que {@code EntityRenderers}). */
    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityType.PIGLIN, c -> new Piglins(c, ModelLayers.PIGLIN, ModelLayers.PIGLIN_INNER_ARMOR,
                ModelLayers.PIGLIN_OUTER_ARMOR, false, PIGLIN, "piglin"));
        event.registerEntityRenderer(EntityType.PIGLIN_BRUTE, c -> new Piglins(c, ModelLayers.PIGLIN_BRUTE, ModelLayers.PIGLIN_BRUTE_INNER_ARMOR,
                ModelLayers.PIGLIN_BRUTE_OUTER_ARMOR, false, PIGLIN_BRUTE, "piglin_brute"));
        event.registerEntityRenderer(EntityType.ZOMBIFIED_PIGLIN, c -> new Piglins(c, ModelLayers.ZOMBIFIED_PIGLIN,
                ModelLayers.ZOMBIFIED_PIGLIN_INNER_ARMOR, ModelLayers.ZOMBIFIED_PIGLIN_OUTER_ARMOR, true, ZOMBIFIED_PIGLIN, "zombified_piglin"));
        event.registerEntityRenderer(EntityType.GHAST, Ghasts::new);
        event.registerEntityRenderer(EntityType.BLAZE, Blazes::new);
        event.registerEntityRenderer(EntityType.WITHER_SKELETON, WitherSkeletons::new);
    }

    // ------------------------------------------------------------------ capa emisiva

    /** Brillo ({@code eyes}) con la textura _glow sobre el modelo remodelado; nada fuera del Nether. */
    private static final class Glow<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
        private final RenderType type;
        private final Function<T, Float> brightness;

        Glow(RenderLayerParent<T, M> parent, String name, Function<T, Float> brightness) {
            super(parent);
            this.type = RenderType.eyes(tex(name + "_glow"));
            this.brightness = brightness;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, T e, float swing, float amount, float partial,
                           float age, float yaw, float pitch) {
            if (!inNether(e)) {
                return;
            }
            float b = brightness.apply(e);
            getParentModel().renderToBuffer(pose, buffers.getBuffer(type), 15728640, OverlayTexture.NO_OVERLAY, b, b, b, 1.0F);
        }
    }

    // ------------------------------------------------------------------ renderizadores

    /** Piglin, bruto y zombificado (el vanilla usa la misma clase para los tres). */
    public static final class Piglins extends PiglinRenderer {
        private final PiglinModel<Mob> vanilla, remodel;
        private final ResourceLocation texture;

        Piglins(EntityRendererProvider.Context ctx, ModelLayerLocation vanillaLayer, ModelLayerLocation inner, ModelLayerLocation outer,
                boolean noRightEar, ModelLayerLocation remodelLayer, String name) {
            super(ctx, vanillaLayer, inner, outer, noRightEar);
            this.vanilla = model;
            this.remodel = new PiglinModel<>(ctx.bakeLayer(remodelLayer));
            this.texture = tex(name);
            addLayer(new Glow<>(this, name, e -> 1.0F));
        }

        @Override
        public void render(Mob e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            model = inNether(e) ? remodel : vanilla;
            super.render(e, yaw, partial, pose, buffers, light);
        }

        @Override
        public ResourceLocation getTextureLocation(Mob e) {
            return inNether(e) ? texture : super.getTextureLocation(e);
        }
    }

    /** Ghast: horno flotante; las rejillas brillan más mientras carga el disparo. */
    public static final class Ghasts extends GhastRenderer {
        private final GhastModel<Ghast> vanilla, remodel;
        private final ResourceLocation texture = tex("ghast");

        Ghasts(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.vanilla = model;
            this.remodel = new GhastModel<>(ctx.bakeLayer(GHAST));
            addLayer(new Glow<>(this, "ghast", g -> g.isCharging() ? 1.0F : 0.6F));
        }

        @Override
        public void render(Ghast e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            model = inNether(e) ? remodel : vanilla;
            super.render(e, yaw, partial, pose, buffers, light);
        }

        @Override
        public ResourceLocation getTextureLocation(Ghast e) {
            return inNether(e) ? texture : super.getTextureLocation(e);       // la misma textura también al cargar
        }
    }

    /** Blaze: inyectores orbitales; más brillo cuando arde (está a punto de disparar). */
    public static final class Blazes extends BlazeRenderer {
        private final BlazeModel<Blaze> vanilla, remodel;
        private final ResourceLocation texture = tex("blaze");

        Blazes(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.vanilla = model;
            this.remodel = new BlazeModel<>(ctx.bakeLayer(BLAZE));
            addLayer(new Glow<>(this, "blaze", b -> b.isOnFire() ? 1.0F : 0.65F));
        }

        @Override
        public void render(Blaze e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            model = inNether(e) ? remodel : vanilla;
            super.render(e, yaw, partial, pose, buffers, light);
        }

        @Override
        public ResourceLocation getTextureLocation(Blaze e) {
            return inNether(e) ? texture : super.getTextureLocation(e);
        }
    }

    /** Esqueleto wither mecanizado (el vanilla ya lo dibuja a escala 1,2). */
    public static final class WitherSkeletons extends WitherSkeletonRenderer {
        private final SkeletonModel<AbstractSkeleton> vanilla, remodel;
        private final ResourceLocation texture = tex("wither_skeleton");

        WitherSkeletons(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.vanilla = model;
            this.remodel = new SkeletonModel<>(ctx.bakeLayer(WITHER_SKELETON));
            addLayer(new Glow<>(this, "wither_skeleton", e -> 1.0F));
        }

        @Override
        public void render(AbstractSkeleton e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            model = inNether(e) ? remodel : vanilla;
            super.render(e, yaw, partial, pose, buffers, light);
        }

        @Override
        public ResourceLocation getTextureLocation(AbstractSkeleton e) {
            return inNether(e) ? texture : super.getTextureLocation(e);
        }
    }

    /** Para las pruebas de modelos: las capas que hay que construir. */
    public static java.util.Map<ModelLayerLocation, Supplier<net.minecraft.client.model.geom.builders.LayerDefinition>> all() {
        return java.util.Map.of(PIGLIN, NetherRemodelLayers::piglin, PIGLIN_BRUTE, NetherRemodelLayers::piglin_brute,
                ZOMBIFIED_PIGLIN, NetherRemodelLayers::zombified_piglin, GHAST, NetherRemodelLayers::ghast,
                BLAZE, NetherRemodelLayers::blaze, WITHER_SKELETON, NetherRemodelLayers::wither_skeleton);
    }
}
