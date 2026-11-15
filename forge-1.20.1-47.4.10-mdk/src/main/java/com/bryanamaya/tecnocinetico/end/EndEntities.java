package com.bryanamaya.tecnocinetico.end;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Enemigos nuevos del End (parte 3): restos de la red de la IA en las islas exteriores. Solo aparecen lejos de la isla
 * central (la lista de biomas está en data/.../forge/biome_modifier/end_spawns.json).
 */
public final class EndEntities {

    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Tecnocinetico.MOD_ID);

    /** Gusano de Datos: excava la piedra del End y sale en arco; segmentos que se rompen por separado. */
    public static final RegistryObject<EntityType<DataWormEntity>> DATA_WORM = TYPES.register("data_worm",
            () -> EntityType.Builder.<DataWormEntity>of(DataWormEntity::new, MobCategory.MONSTER).sized(1.2F, 1.2F).fireImmune()
                    .clientTrackingRange(10).updateInterval(1).build("data_worm"));

    /** Espectro de Datos: proyección de luz sólida, intangible hasta que se materializa para atacar. */
    public static final RegistryObject<EntityType<DataSpecterEntity>> DATA_SPECTER = TYPES.register("data_specter",
            () -> EntityType.Builder.<DataSpecterEntity>of(DataSpecterEntity::new, MobCategory.MONSTER).sized(0.9F, 2.2F).fireImmune()
                    .clientTrackingRange(10).build("data_specter"));

    /** Distancia mínima al centro del End para aparecer (la isla central queda para el Dragón). */
    public static final int OUTER_ISLANDS = 600;

    private EndEntities() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        bus.addListener(EndEntities::attributes);
        bus.addListener(EndEntities::placements);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(DATA_WORM.get(), DataWormEntity.attributes().build());
        event.put(DATA_SPECTER.get(), DataSpecterEntity.attributes().build());
    }

    public static boolean outer(BlockPos pos) {
        return (long) pos.getX() * pos.getX() + (long) pos.getZ() * pos.getZ() >= (long) OUTER_ISLANDS * OUTER_ISLANDS;
    }

    private static void placements(SpawnPlacementRegisterEvent event) {
        event.register(DATA_WORM.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getLevel().dimension() == Level.END && outer(pos)
                        && level.getBlockState(pos.below()).is(Blocks.END_STONE)
                        && Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random),
                SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(DATA_SPECTER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getLevel().dimension() == Level.END && outer(pos)
                        && Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random),
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
