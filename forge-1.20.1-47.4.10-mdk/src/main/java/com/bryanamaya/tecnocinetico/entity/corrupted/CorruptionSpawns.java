package com.bryanamaya.tecnocinetico.entity.corrupted;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.function.Supplier;

/**
 * En el Overworld, zombis/husks, esqueletos/strays y arañas se sustituyen por sus versiones corruptas: al
 * inicializarse cualquier aparición (natural, generador de mazmorra, generación de chunks, refuerzos...) se
 * cancela y se crea el corrupto en el mismo sitio. Los huevos y /summon siguen creando el mob vanilla.
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID)
public final class CorruptionSpawns {

    private static final Map<EntityType<?>, Supplier<? extends EntityType<? extends Mob>>> REPLACE = Map.of(
            EntityType.ZOMBIE, ModEntities.CORRUPTED,
            EntityType.HUSK, ModEntities.CORRUPTED,
            EntityType.SKELETON, ModEntities.CORRUPTED_GUNNER,
            EntityType.STRAY, ModEntities.CORRUPTED_GUNNER,
            EntityType.SPIDER, ModEntities.CORRUPTED_SPIDER);

    private CorruptionSpawns() {}

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        Mob vanilla = event.getEntity();
        var to = REPLACE.get(vanilla.getType());
        MobSpawnType type = event.getSpawnType();
        if (to == null || type == MobSpawnType.SPAWN_EGG || type == MobSpawnType.COMMAND || type == MobSpawnType.JOCKEY
                || type == MobSpawnType.CONVERSION || vanilla.isPassenger()
                || event.getLevel().getLevel().dimension() != Level.OVERWORLD) {
            return;
        }
        event.setSpawnCancelled(true);                                          // el vanilla no entra al mundo
        event.setCanceled(true);                                                // ni se inicializa (sin jinetes)
        Mob mob = to.get().create(event.getLevel().getLevel());
        if (mob == null) {
            return;
        }
        mob.moveTo(vanilla.getX(), vanilla.getY(), vanilla.getZ(), vanilla.getYRot(), 0.0F);
        mob.finalizeSpawn(event.getLevel(), event.getDifficulty(), type, null, null);
        if (vanilla.isPersistenceRequired()) {
            mob.setPersistenceRequired();
        }
        event.getLevel().addFreshEntityWithPassengers(mob);
    }
}
