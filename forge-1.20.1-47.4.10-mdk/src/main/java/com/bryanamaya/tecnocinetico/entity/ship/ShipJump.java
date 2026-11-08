package com.bryanamaya.tecnocinetico.entity.ship;

import com.bryanamaya.tecnocinetico.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Motor de salto del Caza del Núcleo: lleva la nave (y a su piloto) a otra dimensión, al punto equivalente
 * (escala del Nether 1:8) y siempre en el aire y a salvo: encima del terreno, o en una bolsa de aire del Nether.
 */
public final class ShipJump {

    private ShipJump() {}

    /** Caja que ocupa la nave (con margen) si su base está en {@code p}. */
    private static AABB room(Vec3 p) {
        double r = 1.6 * CoreFighterEntity.SCALE, hgt = 1.6 * CoreFighterEntity.SCALE;
        return new AABB(p.x - r, p.y, p.z - r, p.x + r, p.y + hgt, p.z + r);
    }

    private static boolean free(ServerLevel to, Vec3 p) {
        AABB box = room(p);
        return to.noCollision(box) && !to.containsAnyLiquid(box);
    }

    /**
     * Destino seguro en la dimensión {@code to} para la posición {@code from} de {@code fromLevel}: misma posición
     * (escala del Nether), 20 bloques sobre el terreno; en el Nether, el hueco más alto donde quepa la nave (buscando
     * también alrededor). Si no hay ninguno, el motor de salto abre uno (sin tocar la roca madre).
     */
    public static Vec3 arrival(ServerLevel fromLevel, Vec3 from, ServerLevel to) {
        double scale = fromLevel.dimensionType().coordinateScale() / to.dimensionType().coordinateScale();
        double x = Mth.clamp(from.x * scale, -29_000_000, 29_000_000), z = Mth.clamp(from.z * scale, -29_000_000, 29_000_000);
        BlockPos col = BlockPos.containing(x, 0, z);
        if (!to.dimensionType().hasCeiling()) {
            to.getChunk(col.getX() >> 4, col.getZ() >> 4);
            int ground = to.getHeight(Heightmap.Types.MOTION_BLOCKING, col.getX(), col.getZ());
            double y = ground <= to.getMinBuildHeight() + 1 ? 70 : ground + 20;    // End sin isla: flota a 70
            y = Math.min(y, to.getMaxBuildHeight() - 10);
            Vec3 p = new Vec3(col.getX() + 0.5, y, col.getZ() + 0.5);
            while (!free(to, p) && p.y < to.getMaxBuildHeight() - 4) {
                p = p.add(0, 2, 0);
            }
            return p;
        }
        // Nether: busca un hueco en espiral alrededor del punto equivalente
        int top = Math.min(to.getMinBuildHeight() + to.getLogicalHeight() - 6, 118);
        for (int r = 0; r <= 24; r += 4) {
            for (int dx = -r; dx <= r; dx += 4) {
                for (int dz = -r; dz <= r; dz += 4) {
                    if (r > 0 && Math.abs(dx) != r && Math.abs(dz) != r) {
                        continue;                                                  // solo el borde del anillo
                    }
                    BlockPos c = col.offset(dx, 0, dz);
                    to.getChunk(c.getX() >> 4, c.getZ() >> 4);
                    for (int yy = top; yy > to.getMinBuildHeight() + 6; yy--) {
                        Vec3 p = new Vec3(c.getX() + 0.5, yy, c.getZ() + 0.5);
                        if (free(to, p)) {
                            return p;
                        }
                    }
                }
            }
        }
        // sin hueco: lo abre
        Vec3 p = new Vec3(col.getX() + 0.5, 64, col.getZ() + 0.5);
        AABB box = room(p).inflate(0.5);
        for (BlockPos b : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            if (!to.getBlockState(b).is(net.minecraft.world.level.block.Blocks.BEDROCK)) {
                to.setBlock(b, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
        }
        return p;
    }

    private static ITeleporter to(Vec3 pos, float yaw) {
        return new ITeleporter() {
            @Override
            public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float y, Function<Boolean, Entity> reposition) {
                return reposition.apply(false);
            }

            @Override
            public @Nullable PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                return new PortalInfo(pos, Vec3.ZERO, yaw, 0.0F);
            }

            @Override
            public boolean isVanilla() {
                return false;
            }

            @Override
            public boolean playTeleportSound(ServerPlayer player, ServerLevel sourceWorld, ServerLevel destWorld) {
                return false;
            }
        };
    }

    /** Salta: nave y piloto aparecen en {@code dest}; el piloto vuelve a subir en cuanto llega. Devuelve la nave nueva. */
    @Nullable
    public static Entity perform(CoreFighterEntity ship, ResourceKey<Level> dest) {
        if (!(ship.level() instanceof ServerLevel from)) {
            return null;
        }
        ServerLevel to = from.getServer().getLevel(dest);
        if (to == null || to == from) {
            return null;
        }
        Vec3 pos = arrival(from, ship.position(), to);
        from.sendParticles(ParticleTypes.FLASH, ship.getX(), ship.getY() + CoreFighterEntity.PIVOT, ship.getZ(), 1, 0, 0, 0, 0);
        from.playSound(null, ship.getX(), ship.getY(), ship.getZ(), ModSounds.SHIP_JUMP.get(), SoundSource.PLAYERS, 3.0F, 1.0F);
        ServerPlayer pilot = ship.getControllingPassenger() instanceof ServerPlayer p ? p : null;
        if (pilot != null) {
            pilot.stopRiding();
        }
        Entity moved = ship.changeDimension(to, to(pos, ship.getYRot()));
        if (pilot != null) {
            pilot.changeDimension(to, to(pos.add(0, 0.5, 0), ship.getYRot()));
            pilot.fallDistance = 0;
            if (moved instanceof CoreFighterEntity s) {
                s.pendingPilot = pilot.getUUID();
                s.pendingTicks = 0;
            }
        }
        to.sendParticles(ParticleTypes.FLASH, pos.x, pos.y + CoreFighterEntity.PIVOT, pos.z, 1, 0, 0, 0, 0);
        to.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + CoreFighterEntity.PIVOT, pos.z, 100, 4.0, 1.5, 4.0, 0.3);
        to.playSound(null, pos.x, pos.y, pos.z, ModSounds.SHIP_JUMP.get(), SoundSource.PLAYERS, 3.0F, 0.8F);
        return moved;
    }
}
