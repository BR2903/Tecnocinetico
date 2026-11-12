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
        com.bryanamaya.tecnocinetico.machine.MachineBlockEntities.register(modEventBus);
        com.bryanamaya.tecnocinetico.fluid.ModFluids.register(modEventBus);
        ModEntities.register(modEventBus);
        com.bryanamaya.tecnocinetico.nether.NetherEntities.register(modEventBus);          // enemigos del Nether
        ModCreativeModeTabs.register(modEventBus);
        ModParticles.register(modEventBus);
        ModSounds.register(modEventBus);
        com.bryanamaya.tecnocinetico.effect.ModEffects.register(modEventBus);
        ModMenus.register(modEventBus);
        com.bryanamaya.tecnocinetico.worldgen.ModWorldgen.register(modEventBus);
        com.bryanamaya.tecnocinetico.item.ModRecipes.register(modEventBus);
        com.bryanamaya.tecnocinetico.worldgen.structure.ModStructures.register(modEventBus);
        ModNetwork.register();
        context.registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER, com.bryanamaya.tecnocinetico.rules.FriendlyFire.SPEC);
    }

    /** Eventos comunes (cliente y servidor) del bus del mod. */
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class CommonModEvents {

        @SubscribeEvent
        public static void registerAttributes(EntityAttributeCreationEvent event) {
            event.put(ModEntities.NETWORK_WATCHER.get(), com.bryanamaya.tecnocinetico.entity.boss.OverworldGuardianEntity.attributes(true).build());
            event.put(ModEntities.SWARM_MATRIARCH.get(), com.bryanamaya.tecnocinetico.entity.boss.OverworldGuardianEntity.attributes(false).build());
            event.put(ModEntities.MATRIARCH_MINION.get(), com.bryanamaya.tecnocinetico.entity.boss.MatriarchMinionEntity.attributes().build());
            event.put(ModEntities.SWARM_BODY.get(), SwarmBodyEntity.createAttributes().build());
            event.put(ModEntities.SWARM_WASP.get(), com.bryanamaya.tecnocinetico.entity.swarm.SwarmWaspEntity.createAttributes().build());
            event.put(ModEntities.HIVE_NODE.get(), com.bryanamaya.tecnocinetico.entity.swarm.HiveNodeEntity.createAttributes().build());
            event.put(ModEntities.SENTRY_TURRET.get(), com.bryanamaya.tecnocinetico.entity.custom.SentryTurretEntity.createAttributes().build());
            event.put(ModEntities.DRONE.get(), com.bryanamaya.tecnocinetico.entity.custom.DroneEntity.createAttributes().build());
            event.put(ModEntities.SIEGE_SPIDER.get(), com.bryanamaya.tecnocinetico.entity.boss.SiegeSpiderEntity.createAttributes().build());
            event.put(ModEntities.ANCHOR.get(), com.bryanamaya.tecnocinetico.entity.custom.AnchorEntity.createAttributes().build());
            event.put(ModEntities.CORRUPTED.get(), com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedEntity.createAttributes().build());
            event.put(ModEntities.CORRUPTED_GUNNER.get(), com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedGunnerEntity.createAttributes().build());
            event.put(ModEntities.CORRUPTED_SPIDER.get(), com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedSpiderEntity.createAttributes().build());
            event.put(ModEntities.CORRUPTED_CAVE_SPIDER.get(), com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedCaveSpiderEntity.createAttributes().build());
            event.put(ModEntities.CORRUPTED_DROWNED.get(), com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedDrownedEntity.createAttributes().build());
            event.put(ModEntities.CORRUPTED_CREEPER.get(), com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedCreeperEntity.createAttributes().build());
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
                MenuScreens.register(ModMenus.WEAPON_ASSEMBLER.get(), com.bryanamaya.tecnocinetico.client.WeaponAssemblerScreen::new);
                MenuScreens.register(ModMenus.TACTICAL_GEAR.get(), com.bryanamaya.tecnocinetico.client.TacticalScreen::new);
                for (var f : java.util.List.of(com.bryanamaya.tecnocinetico.fluid.ModFluids.SULFURIC_ACID, com.bryanamaya.tecnocinetico.fluid.ModFluids.COOLANT)) {
                    net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(f.source.get(), net.minecraft.client.renderer.RenderType.translucent());
                    net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(f.flowing.get(), net.minecraft.client.renderer.RenderType.translucent());
                }
                for (var kind : com.bryanamaya.tecnocinetico.machine.MachineKind.values()) {
                    if (kind == com.bryanamaya.tecnocinetico.machine.MachineKind.FISSION_REACTOR) {
                        MenuScreens.register(ModMenus.machine(kind).get(), com.bryanamaya.tecnocinetico.client.FissionReactorScreen::new);
                    } else if (kind == com.bryanamaya.tecnocinetico.machine.MachineKind.CHEMICAL_LAB) {
                        MenuScreens.register(ModMenus.machine(kind).get(), com.bryanamaya.tecnocinetico.client.ChemicalLabScreen::new);
                    } else if (kind == com.bryanamaya.tecnocinetico.machine.MachineKind.REACTION_CHAMBER) {
                        MenuScreens.register(ModMenus.machine(kind).get(), com.bryanamaya.tecnocinetico.client.ReactionChamberScreen::new);
                    } else {
                        MenuScreens.register(ModMenus.machine(kind).get(), com.bryanamaya.tecnocinetico.client.MachineScreen::new);
                    }
                }
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
                // Armas largas: 0 normal, 1 recargando (escopeta abierta / cerrojo atrás); "modules" = máscara local 0..7
                for (var gun : java.util.List.of(ModItems.PIPE_SHOTGUN.get(), ModItems.BOLT_RIFLE.get(), ModItems.PUMP_SHOTGUN.get(),
                        ModItems.SEMI_RIFLE.get(), ModItems.KINETIC_SMG.get(), ModItems.RAIL_RIFLE.get(), ModItems.ASSAULT_RIFLE.get(),
                        ModItems.GRENADE_LAUNCHER.get(), ModItems.PULSE_CANNON.get(), ModItems.REVOLVER.get(), ModItems.FLAMETHROWER.get(),
                        ModItems.LIGHT_MACHINE_GUN.get(), ModItems.KINETIC_SNIPER.get(), ModItems.ROCKET_LAUNCHER.get(),
                        ModItems.ARC_CASTER.get(), ModItems.MINIGUN.get())) {
                    ItemProperties.register(gun, ResourceLocation.fromNamespaceAndPath(MOD_ID, "modules"),
                            (stack, level, entity, seed) -> com.bryanamaya.tecnocinetico.item.custom.WeaponModule.localMask(stack));
                    ItemProperties.register(gun, ResourceLocation.fromNamespaceAndPath(MOD_ID, "reload"),
                            (stack, level, entity, seed) -> com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.isLoading(stack) ? 1.0F : 0.0F);
                }
                // Minigun: fotograma de giro de los cañones (0, 0.5, 1) mientras giran; más rápido a plena velocidad
                ItemProperties.register(ModItems.MINIGUN.get(), ResourceLocation.fromNamespaceAndPath(MOD_ID, "spin"),
                        (stack, level, entity, seed) -> {
                            float s = com.bryanamaya.tecnocinetico.item.weapon.MinigunItem.spin(stack);
                            if (s <= 0 || entity == null) {
                                return 0.0F;
                            }
                            long t = entity.level().getGameTime();
                            return ((t / (s >= 1.0F ? 1 : 2)) % 3) / 2.0F;
                        });
                // Taladro: alterna 0/1 cada 2 ticks mientras pica con combustible (modelo con la broca girada 45°)
                ItemProperties.register(ModItems.COMBUSTION_DRILL.get(), ResourceLocation.fromNamespaceAndPath(MOD_ID, "drill_spin"),
                        (stack, level, entity, seed) -> {
                            if (entity == null || entity.getMainHandItem() != stack
                                    || !com.bryanamaya.tecnocinetico.item.custom.CombustionDrillItem.hasFuel(stack)) {
                                return 0.0F;
                            }
                            var mc = net.minecraft.client.Minecraft.getInstance();
                            boolean mining = entity == mc.player ? mc.gameMode != null && mc.gameMode.isDestroying() : entity.swinging;
                            return mining ? (entity.level().getGameTime() / 2) % 2 : 0.0F;
                        });
                // Jetpack: nivel 1-3 (el modelo añade piezas en Mk II y Mk III)
                ItemProperties.register(ModItems.JETPACK.get(), ResourceLocation.fromNamespaceAndPath(MOD_ID, "level"),
                        (stack, level, entity, seed) -> com.bryanamaya.tecnocinetico.gear.JetpackItem.level(stack));
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
        public static void registerMachineRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            // máquinas grandes: se ven los objetos que procesan
            event.registerBlockEntityRenderer(com.bryanamaya.tecnocinetico.machine.MachineBlockEntities.INDUSTRIAL_ASSEMBLER.get(),
                    com.bryanamaya.tecnocinetico.client.IndustrialAssemblerRenderer::new);
            event.registerBlockEntityRenderer(com.bryanamaya.tecnocinetico.machine.MachineBlockEntities.FISSION_REACTOR.get(),
                    com.bryanamaya.tecnocinetico.client.FissionReactorRenderer::new);
            event.registerBlockEntityRenderer(com.bryanamaya.tecnocinetico.block.entity.ModBlockEntities.AUTO_KINETIC_PRESS_BE.get(),
                    com.bryanamaya.tecnocinetico.client.AutoPressRenderer::new);
            event.registerBlockEntityRenderer(com.bryanamaya.tecnocinetico.machine.MachineBlockEntities.REACTION_CHAMBER.get(),
                    com.bryanamaya.tecnocinetico.client.ReactionChamberRenderer::new);
        }

        @SubscribeEvent
        public static void registerReloadListeners(net.minecraftforge.client.event.RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) rm ->
                    com.bryanamaya.tecnocinetico.client.Armor3DModels.clear());          // armaduras 3D: recargar con F3+T
        }

        @SubscribeEvent
        public static void registerExtraModels(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event) {
            event.register(com.bryanamaya.tecnocinetico.client.IndustrialAssemblerRenderer.ARM);
            event.register(com.bryanamaya.tecnocinetico.client.FissionReactorRenderer.ROD);
            event.register(com.bryanamaya.tecnocinetico.client.FissionReactorRenderer.ROD_SPENT);
            event.register(com.bryanamaya.tecnocinetico.client.ReactionChamberRenderer.BEAM);
            event.register(com.bryanamaya.tecnocinetico.client.AutoPressRenderer.HEAD);
        }

        @SubscribeEvent
        public static void registerItemColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {
            com.bryanamaya.tecnocinetico.client.MetalColors.register(event);    // lingotes = el de hierro vanilla teñido
            for (var f : java.util.List.of(com.bryanamaya.tecnocinetico.fluid.ModFluids.SULFURIC_ACID, com.bryanamaya.tecnocinetico.fluid.ModFluids.COOLANT)) {
                event.register(new net.minecraftforge.client.model.DynamicFluidContainerModel.Colors(), f.bucket.get());   // cubos teñidos
            }
            event.register(new net.minecraftforge.client.model.DynamicFluidContainerModel.Colors(),
                    com.bryanamaya.tecnocinetico.fluid.ModFluids.UNIVERSAL_CELL.get());          // la celda: el líquido de dentro
        }

        @SubscribeEvent
        public static void registerParticles(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ModParticles.FLAME_STREAM.get(), com.bryanamaya.tecnocinetico.client.FlameStreamParticle.Provider::new);
            event.registerSpriteSet(ModParticles.KINETIC_TRACER.get(),
                    sprites -> new KineticParticle.Provider(sprites, KineticParticle.Kind.TRACER));
            event.registerSpriteSet(ModParticles.KINETIC_SPARK.get(),
                    sprites -> new KineticParticle.Provider(sprites, KineticParticle.Kind.SPARK));
            event.registerSpriteSet(ModParticles.KINETIC_BURST.get(),
                    sprites -> new KineticParticle.Provider(sprites, KineticParticle.Kind.BURST));
        }

        @SubscribeEvent
        public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(com.bryanamaya.tecnocinetico.entity.client.OverworldGuardianModel.WATCHER,
                    com.bryanamaya.tecnocinetico.entity.client.OverworldGuardianModel::watcherLayer);
            event.registerLayerDefinition(com.bryanamaya.tecnocinetico.entity.client.OverworldGuardianModel.MATRIARCH,
                    com.bryanamaya.tecnocinetico.entity.client.OverworldGuardianModel::matriarchLayer);
            event.registerLayerDefinition(com.bryanamaya.tecnocinetico.entity.client.HiveNodeModel.LAYER, com.bryanamaya.tecnocinetico.entity.client.HiveNodeModel::createLayer);
            event.registerLayerDefinition(com.bryanamaya.tecnocinetico.entity.client.CorruptedSpiderModel.LAYER, com.bryanamaya.tecnocinetico.entity.client.CorruptedSpiderModel::createLayer);
            event.registerLayerDefinition(com.bryanamaya.tecnocinetico.entity.client.RemodeledCreeperModel.LAYER, com.bryanamaya.tecnocinetico.entity.client.RemodeledCreeperModel::createLayer);
            event.registerLayerDefinition(SwarmBodyModel.LAYER, SwarmBodyModel::createBodyLayer);
            event.registerLayerDefinition(com.bryanamaya.tecnocinetico.entity.client.SwarmWaspModel.LAYER,
                    com.bryanamaya.tecnocinetico.entity.client.SwarmWaspModel::createLayer);
            event.registerLayerDefinition(com.bryanamaya.tecnocinetico.entity.client.CorruptedHumanModel.LAYER, com.bryanamaya.tecnocinetico.entity.client.CorruptedHumanModel::createLayer);
            event.registerLayerDefinition(com.bryanamaya.tecnocinetico.entity.client.AnchorModel.LAYER, com.bryanamaya.tecnocinetico.entity.client.AnchorModel::createLayer);
            event.registerLayerDefinition(com.bryanamaya.tecnocinetico.entity.client.CoreFighterModel.LAYER, com.bryanamaya.tecnocinetico.entity.client.CoreFighterModel::createLayer);
            event.registerLayerDefinition(com.bryanamaya.tecnocinetico.entity.client.SiegeSpiderModel.LAYER, com.bryanamaya.tecnocinetico.entity.client.SiegeSpiderModel::createLayer);
            event.registerLayerDefinition(com.bryanamaya.tecnocinetico.entity.client.DroneModel.LAYER,
                    com.bryanamaya.tecnocinetico.entity.client.DroneModel::createLayer);
            event.registerLayerDefinition(com.bryanamaya.tecnocinetico.entity.client.SentryTurretModel.LAYER,
                    com.bryanamaya.tecnocinetico.entity.client.SentryTurretModel::createLayer);
            event.registerLayerDefinition(com.bryanamaya.tecnocinetico.client.WornGearModel.LAYER,
                    com.bryanamaya.tecnocinetico.client.WornGearModel::createLayer);
            event.registerLayerDefinition(com.bryanamaya.tecnocinetico.entity.client.SurvivorRenderer.LAYER,
                    com.bryanamaya.tecnocinetico.entity.client.SurvivorRenderer::createLayer);
        }

        /** Gadgets del Equipo Táctico dibujados sobre el jugador (ambos tipos de brazos: "default" y "slim"). */
        @SubscribeEvent
        @SuppressWarnings("unchecked")
        public static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
            for (String skin : event.getSkins()) {
                var renderer = event.getSkin(skin);
                if (renderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer pr) {
                    pr.addLayer(new com.bryanamaya.tecnocinetico.client.GearLayer(pr, event.getEntityModels()));
                }
            }
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.SWARM_BODY.get(), SwarmBodyRenderer::new);
            event.registerEntityRenderer(ModEntities.SWARM_WASP.get(), com.bryanamaya.tecnocinetico.entity.client.SwarmWaspRenderer::new);
            event.registerEntityRenderer(ModEntities.HIVE_NODE.get(), com.bryanamaya.tecnocinetico.entity.client.HiveNodeRenderer::new);
            event.registerEntityRenderer(ModEntities.SENTRY_TURRET.get(), com.bryanamaya.tecnocinetico.entity.client.SentryTurretRenderer::new);
            event.registerEntityRenderer(ModEntities.DRONE.get(), com.bryanamaya.tecnocinetico.entity.client.DroneRenderer::new);
            event.registerEntityRenderer(com.bryanamaya.tecnocinetico.nether.NetherEntities.MITE.get(), c ->
                    new com.bryanamaya.tecnocinetico.entity.client.InfernalRenderer(c,
                            com.bryanamaya.tecnocinetico.entity.client.InfernalRenderer.MITE, "slag_mite", 0.6F, 0.3F));
            event.registerEntityRenderer(com.bryanamaya.tecnocinetico.nether.NetherEntities.SENTINEL.get(), c ->
                    new com.bryanamaya.tecnocinetico.entity.client.InfernalRenderer(c,
                            com.bryanamaya.tecnocinetico.entity.client.InfernalRenderer.SENTINEL, "ash_sentinel", 1.1F, 0.7F));
            event.registerEntityRenderer(ModEntities.KINETIC_GRENADE.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
            event.registerEntityRenderer(ModEntities.CORE_FIGHTER.get(), com.bryanamaya.tecnocinetico.entity.client.CoreFighterRenderer::new);
            event.registerEntityRenderer(ModEntities.SHIP_MISSILE.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
            event.registerEntityRenderer(ModEntities.SIEGE_SPIDER.get(), com.bryanamaya.tecnocinetico.entity.client.SiegeSpiderRenderer::new);
            event.registerEntityRenderer(ModEntities.NETWORK_WATCHER.get(), ctx -> new com.bryanamaya.tecnocinetico.entity.client.OverworldGuardianRenderer(ctx, true));
            event.registerEntityRenderer(ModEntities.SWARM_MATRIARCH.get(), ctx -> new com.bryanamaya.tecnocinetico.entity.client.OverworldGuardianRenderer(ctx, false));
            event.registerEntityRenderer(ModEntities.MATRIARCH_MINION.get(), com.bryanamaya.tecnocinetico.entity.client.MatriarchMinionRenderer::new);
            event.registerEntityRenderer(ModEntities.GUIDED_ROCKET.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
            event.registerEntityRenderer(ModEntities.ANCHOR.get(), com.bryanamaya.tecnocinetico.entity.client.AnchorRenderer::new);
            event.registerEntityRenderer(ModEntities.CORRUPTED.get(), com.bryanamaya.tecnocinetico.entity.client.CorruptedHumanRenderer::new);
            event.registerEntityRenderer(ModEntities.CORRUPTED_GUNNER.get(), com.bryanamaya.tecnocinetico.entity.client.CorruptedHumanRenderer::new);
            event.registerEntityRenderer(ModEntities.CORRUPTED_SPIDER.get(), ctx -> new com.bryanamaya.tecnocinetico.entity.client.CorruptedSpiderRenderer(ctx, 1.15F));
            event.registerEntityRenderer(ModEntities.CORRUPTED_CAVE_SPIDER.get(), ctx -> new com.bryanamaya.tecnocinetico.entity.client.CorruptedSpiderRenderer(ctx, 0.8F));
            event.registerEntityRenderer(ModEntities.CORRUPTED_DROWNED.get(), com.bryanamaya.tecnocinetico.entity.client.CorruptedHumanRenderer::new);
            event.registerEntityRenderer(ModEntities.CORRUPTED_CREEPER.get(), com.bryanamaya.tecnocinetico.entity.client.CorruptedCreeperRenderer::new);
            event.registerEntityRenderer(ModEntities.SURVIVOR.get(), com.bryanamaya.tecnocinetico.entity.client.SurvivorRenderer::new);
        }
    }
}
