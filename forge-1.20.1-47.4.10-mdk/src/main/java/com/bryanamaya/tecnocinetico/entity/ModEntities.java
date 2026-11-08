package com.bryanamaya.tecnocinetico.entity;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.custom.DroneEntity;
import com.bryanamaya.tecnocinetico.entity.custom.SentryTurretEntity;
import com.bryanamaya.tecnocinetico.entity.custom.SurvivorEntity;
import com.bryanamaya.tecnocinetico.entity.swarm.HiveNodeEntity;
import com.bryanamaya.tecnocinetico.entity.swarm.SwarmWaspEntity;
import com.bryanamaya.tecnocinetico.entity.custom.SwarmBodyEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Tecnocinetico.MOD_ID);

    // Cuerpo de Enjambre: criatura biomecánica con núcleo de Cinetrita (ver lore)
    public static final RegistryObject<EntityType<SwarmBodyEntity>> SWARM_BODY =
            ENTITY_TYPES.register("swarm_body", () -> EntityType.Builder.of(SwarmBodyEntity::new, MobCategory.MONSTER)
                    .sized(1.2f, 1.2f)
                    .clientTrackingRange(10)
                    .build("swarm_body"));

    // Superviviente: NPC comerciante de los Asentamientos (MISC: no cuenta para los límites de aparición)
    public static final RegistryObject<EntityType<SurvivorEntity>> SURVIVOR =
            ENTITY_TYPES.register("survivor", () -> EntityType.Builder.<SurvivorEntity>of(SurvivorEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(10)
                    .build("survivor"));

    // Enjambre de avispas biomecánicas (entity/swarm): avispas y su Nodo de Colmena
    public static final RegistryObject<EntityType<SwarmWaspEntity>> SWARM_WASP =
            ENTITY_TYPES.register("swarm_wasp", () -> EntityType.Builder.<SwarmWaspEntity>of(SwarmWaspEntity::new, MobCategory.MONSTER)
                    .sized(0.7f, 0.6f)
                    .clientTrackingRange(10)
                    .build("swarm_wasp"));
    public static final RegistryObject<EntityType<HiveNodeEntity>> HIVE_NODE =
            ENTITY_TYPES.register("hive_node", () -> EntityType.Builder.<HiveNodeEntity>of(HiveNodeEntity::new, MobCategory.MONSTER)
                    .sized(1.8f, 1.6f)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("hive_node"));

    // Torreta Centinela de las instalaciones de la IA
    public static final RegistryObject<EntityType<SentryTurretEntity>> SENTRY_TURRET =
            ENTITY_TYPES.register("sentry_turret", () -> EntityType.Builder.<SentryTurretEntity>of(SentryTurretEntity::new, MobCategory.MONSTER)
                    .sized(0.9f, 1.2f)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("sentry_turret"));

    // Dron del Núcleo Directriz: patrulla instalaciones y antenas, escanea y dispara ráfagas
    public static final RegistryObject<EntityType<DroneEntity>> DRONE =
            ENTITY_TYPES.register("drone", () -> EntityType.Builder.<DroneEntity>of(DroneEntity::new, MobCategory.MONSTER)
                    .sized(1.0f, 0.7f)
                    .clientTrackingRange(10)
                    .build("drone"));

    // Corruptos: sustituyen a zombis, esqueletos y arañas en el Overworld (entity/corrupted/CorruptionSpawns)
    public static final RegistryObject<EntityType<com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedEntity>> CORRUPTED =
            ENTITY_TYPES.register("corrupted", () -> EntityType.Builder.<com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedEntity>of(com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(8)
                    .build("corrupted"));
    public static final RegistryObject<EntityType<com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedGunnerEntity>> CORRUPTED_GUNNER =
            ENTITY_TYPES.register("corrupted_gunner", () -> EntityType.Builder.<com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedGunnerEntity>of(com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedGunnerEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(8)
                    .build("corrupted_gunner"));
    public static final RegistryObject<EntityType<com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedSpiderEntity>> CORRUPTED_SPIDER =
            ENTITY_TYPES.register("corrupted_spider", () -> EntityType.Builder.<com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedSpiderEntity>of(com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedSpiderEntity::new, MobCategory.MONSTER)
                    .sized(1.6f, 1.0f)
                    .clientTrackingRange(8)
                    .build("corrupted_spider"));

    public static final RegistryObject<EntityType<com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedCaveSpiderEntity>> CORRUPTED_CAVE_SPIDER =
            ENTITY_TYPES.register("corrupted_cave_spider", () -> EntityType.Builder.<com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedCaveSpiderEntity>of(com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedCaveSpiderEntity::new, MobCategory.MONSTER)
                    .sized(0.8f, 0.55f)
                    .clientTrackingRange(8)
                    .build("corrupted_cave_spider"));
    public static final RegistryObject<EntityType<com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedDrownedEntity>> CORRUPTED_DROWNED =
            ENTITY_TYPES.register("corrupted_drowned", () -> EntityType.Builder.<com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedDrownedEntity>of(com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedDrownedEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(8)
                    .build("corrupted_drowned"));
    public static final RegistryObject<EntityType<com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedCreeperEntity>> CORRUPTED_CREEPER =
            ENTITY_TYPES.register("corrupted_creeper", () -> EntityType.Builder.<com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedCreeperEntity>of(com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedCreeperEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.7f)
                    .clientTrackingRange(8)
                    .build("corrupted_creeper"));

    // Anclador: trípode antiaéreo del Núcleo (arrastra al suelo a quien vuela)
    public static final RegistryObject<EntityType<com.bryanamaya.tecnocinetico.entity.custom.AnchorEntity>> ANCHOR =
            ENTITY_TYPES.register("anchor", () -> EntityType.Builder.<com.bryanamaya.tecnocinetico.entity.custom.AnchorEntity>of(com.bryanamaya.tecnocinetico.entity.custom.AnchorEntity::new, MobCategory.MONSTER)
                    .sized(1.3f, 1.6f)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("anchor"));

    // Granada Cinética (Lanzagranadas)
    public static final RegistryObject<EntityType<com.bryanamaya.tecnocinetico.entity.custom.KineticGrenadeEntity>> KINETIC_GRENADE =
            ENTITY_TYPES.register("kinetic_grenade", () -> EntityType.Builder.<com.bryanamaya.tecnocinetico.entity.custom.KineticGrenadeEntity>of(
                            com.bryanamaya.tecnocinetico.entity.custom.KineticGrenadeEntity::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f)
                    .clientTrackingRange(6)
                    .updateInterval(5)
                    .build("kinetic_grenade"));

    // Caza del Núcleo (nave de fin de juego) y su misil
    public static final RegistryObject<EntityType<com.bryanamaya.tecnocinetico.entity.ship.CoreFighterEntity>> CORE_FIGHTER =
            ENTITY_TYPES.register("core_fighter", () -> EntityType.Builder.<com.bryanamaya.tecnocinetico.entity.ship.CoreFighterEntity>of(com.bryanamaya.tecnocinetico.entity.ship.CoreFighterEntity::new, MobCategory.MISC)
                    .sized(6.0f, 3.6f)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("core_fighter"));
    public static final RegistryObject<EntityType<com.bryanamaya.tecnocinetico.entity.ship.ShipMissileEntity>> SHIP_MISSILE =
            ENTITY_TYPES.register("ship_missile", () -> EntityType.Builder.<com.bryanamaya.tecnocinetico.entity.ship.ShipMissileEntity>of(com.bryanamaya.tecnocinetico.entity.ship.ShipMissileEntity::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("ship_missile"));

    // Araña de Asedio: guardián de la Instalación de la IA (jefe)
    public static final RegistryObject<EntityType<com.bryanamaya.tecnocinetico.entity.boss.SiegeSpiderEntity>> SIEGE_SPIDER =
            ENTITY_TYPES.register("siege_spider", () -> EntityType.Builder.<com.bryanamaya.tecnocinetico.entity.boss.SiegeSpiderEntity>of(com.bryanamaya.tecnocinetico.entity.boss.SiegeSpiderEntity::new, MobCategory.MONSTER)
                    .sized(5.4f, 3.4f)                                     // incluye las rodillas (puntos débiles)
                    .fireImmune()
                    .clientTrackingRange(12)
                    .build("siege_spider"));

    // Cohete guiado (Lanzacohetes)
    public static final RegistryObject<EntityType<com.bryanamaya.tecnocinetico.entity.custom.GuidedRocketEntity>> GUIDED_ROCKET =
            ENTITY_TYPES.register("guided_rocket", () -> EntityType.Builder.<com.bryanamaya.tecnocinetico.entity.custom.GuidedRocketEntity>of(
                            com.bryanamaya.tecnocinetico.entity.custom.GuidedRocketEntity::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("guided_rocket"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
