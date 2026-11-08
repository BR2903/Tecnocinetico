package com.bryanamaya.tecnocinetico;

import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.block.ModWorldBlocks;
import com.bryanamaya.tecnocinetico.block.entity.ModBlockEntities;
import com.bryanamaya.tecnocinetico.entity.ModEntities;
import com.bryanamaya.tecnocinetico.entity.client.SwarmBodyModel;
import com.bryanamaya.tecnocinetico.entity.client.SwarmBodyRenderer;
import com.bryanamaya.tecnocinetico.entity.custom.SwarmBodyEntity;
import com.bryanamaya.tecnocinetico.item.ModCreativeModeTabs;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.network.ModNetwork;
import com.bryanamaya.tecnocinetico.client.KineticParticle;
import com.bryanamaya.tecnocinetico.item.custom.GunAnimState;
import com.bryanamaya.tecnocinetico.item.custom.KineticPistolItem;
import net.minecraft.client.Minecraft;
import com.bryanamaya.tecnocinetico.item.custom.WeaponModule;
import com.bryanamaya.tecnocinetico.client.AutoKineticPressScreen;
import com.bryanamaya.tecnocinetico.client.WeaponBenchScreen;
import com.bryanamaya.tecnocinetico.menu.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
import com.bryanamaya.tecnocinetico.sound.ModSounds;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Tecnocinetico.MOD_ID)
public class Tecnocinetico {

    public static final String MOD_ID = "tecnocinetico";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Tecnocinetico(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // --- EL ÚNICO REGISTRO CIVIL DE TECNOCINÉTICO ---
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModWorldBlocks.init();   // bloques del mundo post-apocalíptico
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModParticles.register(modEventBus);
        ModSounds.register(modEventBus);
        ModMenus.register(modEventBus);
        com.bryanamaya.tecnocinetico.worldgen.ModWorldgen.register(modEventBus);
        com.bryanamaya.tecnocinetico.item.ModRecipes.register(modEventBus);
        com.bryanamaya.tecnocinetico.worldgen.structure.ModStructures.register(modEventBus);
        ModNetwork.register();
    }

    /** Eventos comunes (cliente y servidor) del bus del mod. */
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class CommonModEvents {

        @SubscribeEvent
        public static void registerAttributes(EntityAttributeCreationEvent event) {
            event.put(ModEntities.SWARM_BODY.get(), SwarmBodyEntity.createAttributes().build());
            event.put(ModEntities.SWARM_WASP.get(), com.bryanamaya.tecnocinetico.entity.swarm.SwarmWaspEntity.createAttributes().build());
            event.put(ModEntities.HIVE_NODE.get(), com.bryanamaya.tecnocinetico.entity.swarm.HiveNodeEntity.createAttributes().build());
            event.put(ModEntities.SENTRY_TURRET.get(), com.bryanamaya.tecnocinetico.entity.custom.SentryTurretEntity.createAttributes().build());
            event.put(ModEntities.SURVIVOR.get(), com.bryanamaya.tecnocinetico.entity.custom.SurvivorEntity.createAttributes().build());
        }

        @SubscribeEvent
        public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
            // Aparece como un monstruo normal: en el suelo y en la oscuridad
            event.register(ModEntities.SWARM_BODY.get(),
                    SpawnPlacements.Type.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Monster::checkMonsterSpawnRules,
                    SpawnPlacementRegisterEvent.Operation.REPLACE);
        }
    }

    /** Eventos solo del cliente: modelos y renderizadores. */
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Fotogramas de recarga de la pistola: el modelo cambia según el progreso (0 = normal)
            event.enqueueWork(() -> {
                MenuScreens.register(ModMenus.WEAPON_BENCH.get(), WeaponBenchScreen::new);
                MenuScreens.register(ModMenus.AUTO_KINETIC_PRESS.get(), AutoKineticPressScreen::new);
                ItemProperties.register(ModItems.KINETIC_PISTOL.get(),
                        ResourceLocation.fromNamespaceAndPath(MOD_ID, "reload"),
                        (stack, level, entity, seed) -> {
                            float reload = KineticPistolItem.getReloadProgress(stack, 0.0F);
                            if (reload > 0.0F) {
                                return reload;
                            }
                            // Retroceso de la corredera: justo después de disparar se usa el
                            // fotograma "corredera atrás" de la recarga (umbral 0.72-0.9).
                            if (entity != null && entity == Minecraft.getInstance().player
                                    && entity.getMainHandItem() == stack
                                    && GunAnimState.ticksSinceShot(entity.level().getGameTime(), 0)
                                    < GunAnimState.SLIDE_BACK_TICKS) {
                                return 0.75F;
                            }
                            return 0.0F;
                        });
                // Cargadores: 0 vacío, 0.5 bala normal arriba, 1 bala guiada arriba (cambia el modelo)
                for (var mag : java.util.List.of(ModItems.MAGAZINE.get(), ModItems.EXTENDED_MAGAZINE.get())) {
                    ItemProperties.register(mag, ResourceLocation.fromNamespaceAndPath(MOD_ID, "loaded"),
                            (stack, level, entity, seed) -> com.bryanamaya.tecnocinetico.item.custom.MagazineItem.loadedProperty(stack));
                }
                // Combinación de módulos instalados (máscara de bits 0-15): cambia el modelo del arma
                ItemProperties.register(ModItems.KINETIC_PISTOL.get(),
                        ResourceLocation.fromNamespaceAndPath(MOD_ID, "modules"),
                        (stack, level, entity, seed) -> WeaponModule.getMask(stack));
            });
        }

        @SubscribeEvent
        public static void registerParticles(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ModParticles.KINETIC_TRACER.get(),
                    sprites -> new KineticParticle.Provider(sprites, KineticParticle.Kind.TRACER));
            event.registerSpriteSet(ModParticles.KINETIC_SPARK.get(),
                    sprites -> new KineticParticle.Provider(sprites, KineticParticle.Kind.SPARK));
            event.registerSpriteSet(ModParticles.KINETIC_BURST.get(),
                    sprites -> new KineticParticle.Provider(sprites, KineticParticle.Kind.BURST));
        }

        @SubscribeEvent
        public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(SwarmBodyModel.LAYER, SwarmBodyModel::createBodyLayer);
            event.registerLayerDefinition(com.bryanamaya.tecnocinetico.entity.client.SwarmWaspModel.LAYER,
                    com.bryanamaya.tecnocinetico.entity.client.SwarmWaspModel::createLayer);
            event.registerLayerDefinition(com.bryanamaya.tecnocinetico.entity.client.SentryTurretModel.LAYER,
                    com.bryanamaya.tecnocinetico.entity.client.SentryTurretModel::createLayer);
            event.registerLayerDefinition(com.bryanamaya.tecnocinetico.entity.client.SurvivorRenderer.LAYER,
                    com.bryanamaya.tecnocinetico.entity.client.SurvivorRenderer::createLayer);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.SWARM_BODY.get(), SwarmBodyRenderer::new);
            event.registerEntityRenderer(ModEntities.SWARM_WASP.get(), com.bryanamaya.tecnocinetico.entity.client.SwarmWaspRenderer::new);
            event.registerEntityRenderer(ModEntities.HIVE_NODE.get(), com.bryanamaya.tecnocinetico.entity.client.HiveNodeRenderer::new);
            event.registerEntityRenderer(ModEntities.SENTRY_TURRET.get(), com.bryanamaya.tecnocinetico.entity.client.SentryTurretRenderer::new);
            event.registerEntityRenderer(ModEntities.SURVIVOR.get(), com.bryanamaya.tecnocinetico.entity.client.SurvivorRenderer::new);
        }
    }
}
