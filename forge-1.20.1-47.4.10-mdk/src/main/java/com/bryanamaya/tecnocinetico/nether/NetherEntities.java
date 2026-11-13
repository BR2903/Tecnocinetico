package com.bryanamaya.tecnocinetico.nether;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Enemigos propios del Nether (segunda parte del Nether por etapas): Ácaro de Escoria y Centinela de Ceniza.
 * Los dos jefes (Fundidor del Abismo y Devorador de Escoria) llegarán en sus propias entregas; sus modelos ya
 * están en {@code entity/client/InfernalModel} (tipos 0 y 1).
 */
public final class NetherEntities {

    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Tecnocinetico.MOD_ID);

    /** Ácaro de Escoria: pequeño, rápido, en grupo; vive en los túneles (por debajo de Y 80) y sale al excavar. */
    public static final RegistryObject<EntityType<InfernalEntity>> MITE = type("slag_mite", 0.65F, 0.4F);
    /** Centinela de Ceniza: máquina caminante que avisa con un láser y dispara; golpea si te acercas. */
    public static final RegistryObject<EntityType<InfernalEntity>> SENTINEL = type("ash_sentinel", 1.1F, 2.5F);

    /** Fundidor del Abismo (jefe): se despierta con la consola de su fundición. */
    public static final RegistryObject<EntityType<AbyssFoundryEntity>> FOUNDRY = TYPES.register("abyss_foundry",
            () -> EntityType.Builder.of(AbyssFoundryEntity::new, MobCategory.MONSTER).sized(3.2F, 4.6F).fireImmune()
                    .clientTrackingRange(16).build("abyss_foundry"));

    /** Altura máxima a la que aparecen los ácaros de forma natural o al excavar. */
    public static final int MITE_MAX_Y = 80;

    private NetherEntities() {}

    private static RegistryObject<EntityType<InfernalEntity>> type(String id, float width, float height) {
        return TYPES.register(id, () -> EntityType.Builder.of(InfernalEntity::new, MobCategory.MONSTER)
                .sized(width, height).fireImmune().clientTrackingRange(12).build(id));
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        bus.addListener(NetherEntities::attributes);
        bus.addListener(NetherEntities::placements);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(MITE.get(), InfernalEntity.attributes(18, 2, 4, 0.31).build());
        event.put(SENTINEL.get(), InfernalEntity.attributes(64, 10, 9, 0.25).build());
        event.put(FOUNDRY.get(), AbyssFoundryEntity.attributes().build());
    }

    /** Solo aparecen en el Nether (la lista de biomas está en data/.../forge/biome_modifier/infernal_spawns.json). */
    private static void placements(SpawnPlacementRegisterEvent event) {
        for (var type : java.util.List.of(MITE, SENTINEL)) {
            event.register(type.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (entity, level, reason, pos, random) -> level.getLevel().dimension() == Level.NETHER
                            && (entity != MITE.get() || pos.getY() < MITE_MAX_Y)
                            && Monster.checkAnyLightMonsterSpawnRules(entity, level, reason, pos, random),
                    SpawnPlacementRegisterEvent.Operation.REPLACE);
        }
    }
}
