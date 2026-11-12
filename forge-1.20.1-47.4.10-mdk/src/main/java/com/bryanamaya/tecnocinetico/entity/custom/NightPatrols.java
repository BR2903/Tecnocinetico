package com.bryanamaya.tecnocinetico.entity.custom;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Patrullas nocturnas de drones del Núcleo Directriz (Overworld). De noche, cerca de cada jugador en supervivencia
 * que esté en la superficie, a veces aparece un grupo de 2-3 drones a {@link #DISTANCE} bloques que cruza la zona
 * pasando cerca de él, barriendo el suelo con su haz. Si lo ven, atacan y avisan a los demás (DroneEntity). Al
 * amanecer, o a los 3 minutos sin encontrar a nadie, suben y se van.
 * <p>
 * Multijugador: cada jugador tiene su propia espera (datos persistentes) y se cuenta a los drones de patrulla que ya
 * hay alrededor ({@link #MAX_NEARBY}), así varios jugadores juntos no multiplican las patrullas.
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID)
public final class NightPatrols {

    public static final int CHECK_EVERY = 200, COOLDOWN = 600, MAX_NEARBY = 5, DISTANCE = 56;
    public static final float CHANCE = 0.35F;
    private static final String NEXT = "TecnocineticoNextPatrol";

    private NightPatrols() {}

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD
                || level.getGameTime() % CHECK_EVERY != 0 || level.isDay() || level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        long now = level.getGameTime();
        for (ServerPlayer player : level.players()) {
            if (player.isCreative() || player.isSpectator() || !player.isAlive() || now < player.getPersistentData().getLong(NEXT)) {
                continue;
            }
            if (!outdoors(level, player) || level.random.nextFloat() > CHANCE) {
                continue;
            }
            if (!spawnPatrol(level, player).isEmpty()) {
                player.getPersistentData().putLong(NEXT, now + COOLDOWN);
            }
        }
    }

    /** En la superficie (no en cuevas ni bajo techo grueso). */
    public static boolean outdoors(ServerLevel level, Player player) {
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, player.getBlockX(), player.getBlockZ());
        return player.getY() >= surface - 6;
    }

    /** Hace aparecer un grupo de 2-3 drones lejos del jugador, rumbo a pasar cerca de él. Devuelve los drones creados. */
    public static List<DroneEntity> spawnPatrol(ServerLevel level, Player player) {
        List<DroneEntity> out = new ArrayList<>();
        long nearby = level.getEntitiesOfClass(DroneEntity.class, player.getBoundingBox().inflate(96), DroneEntity::isNightPatrol).size();
        if (nearby >= MAX_NEARBY) {
            return out;
        }
        double angle = level.random.nextDouble() * Math.PI * 2;
        Vec3 start = player.position().add(Math.cos(angle) * DISTANCE, 0, Math.sin(angle) * DISTANCE);
        if (!level.hasChunkAt(net.minecraft.core.BlockPos.containing(start))) {
            return out;
        }
        // rumbo: hacia un punto a pocos bloques del jugador (pasa cerca, no siempre encima)
        Vec3 aim = player.position().add(level.random.nextGaussian() * 10, 0, level.random.nextGaussian() * 10);
        Vec3 heading = aim.subtract(start);
        int size = 2 + level.random.nextInt(2);
        for (int i = 0; i < size && nearby + i < MAX_NEARBY; i++) {
            DroneEntity drone = ModEntities.DRONE.get().create(level);
            if (drone == null) {
                break;
            }
            double sx = start.x + (i - 1) * 3, sz = start.z + (i % 2) * 3;
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(sx), (int) Math.floor(sz));
            drone.moveTo(sx, ground + 12 + i, sz, 0, 0);
            if (!level.noCollision(drone)) {
                continue;
            }
            drone.finalizeSpawn(level, level.getCurrentDifficultyAt(drone.blockPosition()), MobSpawnType.EVENT, null, null);
            drone.startPatrol(heading);
            level.addFreshEntity(drone);
            out.add(drone);
        }
        return out;
    }
}
