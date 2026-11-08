package com.bryanamaya.tecnocinetico.entity;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
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

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
