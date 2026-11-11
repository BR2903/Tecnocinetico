package com.bryanamaya.tecnocinetico.entity.ship;

import com.bryanamaya.tecnocinetico.entity.ModEntities;
import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Misil del Caza del Núcleo: acelera y, si salió con un objetivo fijado, lo persigue girando poco a poco (se le
 * puede esquivar con un quiebro). Estalla al tocar algo (radio 2,5, no rompe bloques) o a los 6 s.
 */
public class ShipMissileEntity extends ThrowableItemProjectile {

    private static final double SPEED = 4.5D;                       // más rápido que la nave
    @Nullable
    private LivingEntity target;

    public ShipMissileEntity(EntityType<? extends ShipMissileEntity> type, Level level) {
        super(type, level);
    }

    public ShipMissileEntity(Level level, LivingEntity owner, @Nullable LivingEntity target) {
        super(ModEntities.SHIP_MISSILE.get(), owner, level);
        this.target = target;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.CORE_MISSILE.get();
    }

    @Override
    protected float getGravity() {
        return 0.0F;
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        return super.canHitEntity(e) && !(e instanceof CoreFighterEntity) && !(e instanceof ShipMissileEntity)
                && (getOwner() == null || !getOwner().isAlliedTo(e));
    }

    @Override
    public void tick() {
        Vec3 v = getDeltaMovement();
        double sp = Math.min(SPEED, v.length() + 0.25D);
        Vec3 dir = v.lengthSqr() > 1e-6 ? v.normalize() : Vec3.directionFromRotation(getXRot(), getYRot());
        if (target != null && target.isAlive() && tickCount > 4) {
            Vec3 want = target.getBoundingBox().getCenter().subtract(position()).normalize();
            dir = dir.lerp(want, 0.16D).normalize();
        }
        setDeltaMovement(dir.scale(sp));
        super.tick();
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SMOKE, xo, yo, zo, 2, 0.02, 0.02, 0.02, 0.0);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, xo, yo, zo, 1, 0, 0, 0, 0.0);
            if (tickCount > 120) {
                explode();
            }
        }
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
        level().explode(this, getX(), getY(), getZ(), 2.5F, Level.ExplosionInteraction.NONE);
        discard();
    }
}
