package com.bryanamaya.tecnocinetico.entity.custom;

import com.bryanamaya.tecnocinetico.entity.ModEntities;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.item.weapon.RocketLauncherItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Cohete Guiado (Lanzacohetes): acelera hasta 2,2 bloques/tick; mientras su dueño lleve el lanzacohetes en la mano,
 * gira hacia el punto que mira (guía láser, 128 bloques). Estalla al tocar algo (potencia 4, no rompe bloques) o a
 * los 8 s.
 */
public class GuidedRocketEntity extends ThrowableItemProjectile {

    private static final double MAX_SPEED = 2.2D;

    public GuidedRocketEntity(EntityType<? extends GuidedRocketEntity> type, Level level) {
        super(type, level);
    }

    public GuidedRocketEntity(Level level, LivingEntity owner) {
        super(ModEntities.GUIDED_ROCKET.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.GUIDED_ROCKET.get();
    }

    @Override
    protected float getGravity() {
        return 0.0F;
    }

    @Override
    public void tick() {
        Vec3 v = getDeltaMovement();
        double sp = Math.min(MAX_SPEED, v.length() + 0.12);
        Vec3 dir = v.lengthSqr() > 1e-6 ? v.normalize() : Vec3.directionFromRotation(getXRot(), getYRot());
        if (!level().isClientSide && getOwner() instanceof Player p && p.isAlive()
                && p.getMainHandItem().getItem() instanceof RocketLauncherItem && tickCount > 2) {
            Vec3 eye = p.getEyePosition(), end = eye.add(p.getLookAngle().scale(128));
            HitResult aim = level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            Vec3 target = aim.getType() == HitResult.Type.MISS ? end : aim.getLocation();
            dir = dir.lerp(target.subtract(position()).normalize(), 0.2).normalize();      // guía láser
        }
        setDeltaMovement(dir.scale(sp));
        super.tick();
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.FLAME, xo, yo, zo, 1, 0.02, 0.02, 0.02, 0.0);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, xo, yo, zo, 1, 0.05, 0.05, 0.05, 0.0);
            if (tickCount > 160) {
                explode();
            }
        }
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        return super.canHitEntity(e) && !(e instanceof GuidedRocketEntity);
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide) {
            explode();
        }
    }

    private void explode() {
        if (isRemoved()) {
            return;
        }
        level().explode(this, getX(), getY(), getZ(), 4.0F, Level.ExplosionInteraction.NONE);
        discard();
    }
}
