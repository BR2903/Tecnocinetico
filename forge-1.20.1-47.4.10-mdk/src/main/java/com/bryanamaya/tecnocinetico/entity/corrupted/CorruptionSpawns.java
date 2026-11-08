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
 * En el Overworld, zombis/husks, esqueletos/strays, arañas (y de cueva), ahogados y creepers se sustituyen por sus versiones corruptas: al
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
            EntityType.SPIDER, ModEntities.CORRUPTED_SPIDER,
            EntityType.CAVE_SPIDER, ModEntities.CORRUPTED_CAVE_SPIDER,
            EntityType.DROWNED, ModEntities.CORRUPTED_DROWNED,
            EntityType.CREEPER, ModEntities.CORRUPTED_CREEPER);

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

    /** Creeper Corrupto: al estallar deja una nube de polvo de cinetrita (debilidad y náusea). */
    @SubscribeEvent
    public static void onDetonate(net.minecraftforge.event.level.ExplosionEvent.Detonate event) {
        if (!(event.getExplosion().getExploder() instanceof CorruptedCreeperEntity creeper)) {
            return;
        }
        var cloud = new net.minecraft.world.entity.AreaEffectCloud(event.getLevel(), creeper.getX(), creeper.getY(), creeper.getZ());
        cloud.setRadius(3.0F);
        cloud.setRadiusOnUse(-0.3F);
        cloud.setWaitTime(5);
        cloud.setDuration(140);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.setFixedColor(0x46D2E6);
        cloud.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 200, 0));
        cloud.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION, 140, 0));
        event.getLevel().addFreshEntity(cloud);
    }
}
