package com.bryanamaya.tecnocinetico.entity.custom;

import com.bryanamaya.tecnocinetico.entity.ModEntities;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Granada Cinética (la dispara el Lanzagranadas): vuela en parábola, rebota en bloques perdiendo velocidad y
 * estalla al tocar a un ser vivo (que no sea quien la lanzó en el primer momento) o a los 2,5 s. La explosión
 * hiere y empuja en un radio de 3 bloques, pero no rompe bloques.
 */
public class KineticGrenadeEntity extends ThrowableItemProjectile {

    private static final int FUSE = 50;

    public KineticGrenadeEntity(EntityType<? extends KineticGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public KineticGrenadeEntity(Level level, LivingEntity shooter) {
        super(ModEntities.KINETIC_GRENADE.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.KINETIC_GRENADE.get();
    }

    @Override
    protected float getGravity() {
        return 0.045F;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server) {
            if (tickCount % 2 == 0) {
                server.sendParticles(ModParticles.KINETIC_SPARK.get(), getX(), getY(), getZ(), 1, 0, 0, 0, 0.01);
            }
            if (tickCount >= FUSE) {
                explode();
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (hit.getEntity() == getOwner() && tickCount < 10) {
            return;
        }
        if (hit.getEntity() instanceof LivingEntity && !level().isClientSide) {
            explode();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        Vec3 v = getDeltaMovement();
        Vec3 bounced = switch (hit.getDirection().getAxis()) {
            case X -> new Vec3(-v.x, v.y, v.z);
            case Y -> new Vec3(v.x, -v.y, v.z);
            case Z -> new Vec3(v.x, v.y, -v.z);
        };
        bounced = bounced.scale(0.45);
        if (bounced.lengthSqr() < 0.004) {
            bounced = Vec3.ZERO;
        }
        setDeltaMovement(bounced);
        Vec3 n = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
        setPos(hit.getLocation().add(n.scale(0.06)));
        if (v.lengthSqr() > 0.02) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_HIT, SoundSource.NEUTRAL, 0.6F, 1.4F);
        }
    }

    private void explode() {
        if (isRemoved()) {
            return;
        }
        level().explode(this, getX(), getY(0.0625), getZ(), 3.0F, Level.ExplosionInteraction.NONE);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ModParticles.KINETIC_SPARK.get(), getX(), getY(), getZ(), 40, 0.6, 0.6, 0.6, 0.4);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 20, 0.8, 0.8, 0.8, 0.2);
        }
        discard();
    }
}
