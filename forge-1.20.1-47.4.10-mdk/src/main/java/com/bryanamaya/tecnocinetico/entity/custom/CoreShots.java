package com.bryanamaya.tecnocinetico.entity.custom;

import com.bryanamaya.tecnocinetico.particle.ModParticles;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Disparos de las máquinas del Núcleo Directriz (torretas y drones), instantáneos con trazador:
 * - ráfaga: tiros rápidos y flojos con dispersión, apuntados a la posición actual del objetivo;
 * - cargado: un tiro fuerte hacia un PUNTO fijado poco antes de disparar (la línea roja deja de seguirte):
 *   si te quedas quieto te da, si te mueves en ese momento lo esquivas.
 */
public final class CoreShots {

    public static final DustParticleOptions LASER = new DustParticleOptions(new Vector3f(1.0F, 0.15F, 0.1F), 0.6F);

    private CoreShots() {}

    /** Punto al que apuntar en un objetivo (pecho). */
    public static Vec3 aimPoint(LivingEntity t) {
        return t.position().add(0, t.getBbHeight() * 0.6, 0);
    }

    /** Dispara desde {@code from} hacia {@code dir} y devuelve dónde impactó. */
    public static Vec3 fire(Mob shooter, Vec3 from, Vec3 dir, double range, float damage, double spreadDeg,
                            Class<?> ally, boolean heavy) {
        RandomSource r = shooter.getRandom();
        double s = Math.toRadians(spreadDeg);
        dir = dir.normalize();
        if (s > 0) {
            dir = dir.add(r.nextGaussian() * s, r.nextGaussian() * s, r.nextGaussian() * s).normalize();
        }
        Vec3 end = from.add(dir.scale(range));
        BlockHitResult block = shooter.level().clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(shooter.level(), shooter, from, end, new AABB(from, end).inflate(1.0D),
                e -> e instanceof LivingEntity && e != shooter && !ally.isInstance(e)
                        && !shooter.isAlliedTo(e) && !e.isSpectator(), heavy ? 0.35F : 0.15F);
        if (hit != null) {
            end = hit.getEntity().getBoundingBox().clip(from, end).orElse(hit.getEntity().getBoundingBox().getCenter());
            hit.getEntity().hurt(shooter.damageSources().mobProjectile(shooter, shooter), damage);   // bala: proyectil
        }
        if (shooter.level() instanceof ServerLevel server) {
            Vec3 path = end.subtract(from);
            int steps = (int) (path.length() * 3);
            for (int i = 0; i <= steps; i++) {
                Vec3 p = from.add(path.scale(i / (double) Math.max(1, steps)));
                server.sendParticles(ModParticles.KINETIC_TRACER.get(), p.x, p.y, p.z, 0, 0, 0, 0, 0);
            }
            server.sendParticles(ModParticles.KINETIC_SPARK.get(), end.x, end.y, end.z, heavy ? 14 : 3, 0.05, 0.05, 0.05, heavy ? 0.3 : 0.12);
            if (heavy) {
                server.sendParticles(ParticleTypes.SMALL_FLAME, end.x, end.y, end.z, 6, 0.1, 0.1, 0.1, 0.03);
            }
        }
        return end;
    }

    /** Línea roja de puntería (aviso del disparo cargado). */
    public static void laser(Mob shooter, Vec3 from, Vec3 to) {
        if (!(shooter.level() instanceof ServerLevel server)) {
            return;
        }
        BlockHitResult block = shooter.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
        Vec3 path = end.subtract(from);
        int steps = (int) (path.length() * 2);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(path.scale(i / (double) Math.max(1, steps)));
            server.sendParticles(LASER, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }
}
