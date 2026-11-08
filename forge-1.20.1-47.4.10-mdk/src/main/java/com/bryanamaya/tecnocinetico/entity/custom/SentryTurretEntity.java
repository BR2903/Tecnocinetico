package com.bryanamaya.tecnocinetico.entity.custom;

import com.bryanamaya.tecnocinetico.particle.ModParticles;
import com.bryanamaya.tecnocinetico.sound.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * Torreta Centinela de las instalaciones del Núcleo Directriz. Anclada (no se mueve ni se empuja); gira la
 * cabeza hacia el jugador con velocidad limitada, CARGA el disparo ~1,2 s mostrando la línea de puntería roja y
 * dispara hacia donde apuntaba al terminar la carga: si te apartas a tiempo, la esquivas.
 */
public class SentryTurretEntity extends Monster {

    private static final EntityDataAccessor<Boolean> CHARGING =
            SynchedEntityData.defineId(SentryTurretEntity.class, EntityDataSerializers.BOOLEAN);
    private static final double RANGE = 22.0D;
    private static final int CHARGE_TICKS = 24;
    private static final int COOLDOWN_TICKS = 30;
    private static final float TURN_SPEED = 5.0F;                      // grados por tick
    private static final DustParticleOptions LASER = new DustParticleOptions(new Vector3f(1.0F, 0.15F, 0.1F), 0.6F);

    private int charge, cooldown;
    @Nullable
    private Vec3 lockedAim;

    public SentryTurretEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 15;
        setPersistenceRequired();
        // la cabeza la gira customServerAiStep; el control vanilla la devolvería al cuerpo cada tick
        this.lookControl = new net.minecraft.world.entity.ai.control.LookControl(this) {
            @Override
            public void tick() {
            }
        };
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 34.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, RANGE);
    }

    @Override
    protected void registerGoals() {
        // sin objetivos vanilla: apunta y dispara en customServerAiStep
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CHARGING, false);
    }

    public boolean isCharging() {
        return entityData.get(CHARGING);
    }

    // ------------------------------------------------------------------ comportamiento

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (cooldown > 0) {
            cooldown--;
        }
        LivingEntity target = findTarget();
        if (charge > 0) {
            charge++;
            if (charge % 2 == 0) {
                laserSight(lockedAim);
            }
            if (charge >= CHARGE_TICKS) {
                fire(lockedAim);
                charge = 0;
                cooldown = COOLDOWN_TICKS;
                entityData.set(CHARGING, false);
            }
            return;
        }
        if (target == null) {
            // barrido lento de vigilancia
            setYHeadRot(yHeadRot + 1.2F);
            setXRot(Mth.approach(getXRot(), 0.0F, 2.0F));
            return;
        }
        Vec3 to = target.getEyePosition().subtract(getEyePosition());
        float wantYaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0F;
        float wantPitch = (float) -(Mth.atan2(to.y, to.horizontalDistance()) * Mth.RAD_TO_DEG);
        setYHeadRot(Mth.approachDegrees(yHeadRot, wantYaw, TURN_SPEED));
        setXRot(Mth.approach(getXRot(), wantPitch, TURN_SPEED));
        boolean aimed = Math.abs(Mth.degreesDifference(yHeadRot, wantYaw)) < 4.0F && Math.abs(getXRot() - wantPitch) < 4.0F;
        if (aimed && cooldown <= 0) {
            charge = 1;
            lockedAim = Vec3.directionFromRotation(getXRot(), yHeadRot);   // fija la dirección: esquivable
            entityData.set(CHARGING, true);
            playSound(ModSounds.SENTRY_TURRET_CHARGE.get(), 1.2F, 1.0F);
        }
    }

    @Nullable
    private LivingEntity findTarget() {
        Player p = level().getNearestPlayer(this, RANGE);
        if (p == null || p.isCreative() || p.isSpectator() || !hasLineOfSight(p)) {
            return null;
        }
        setTarget(p);
        return p;
    }

    private Vec3 muzzle() {
        return getEyePosition().add(Vec3.directionFromRotation(getXRot(), yHeadRot).scale(0.7D));
    }

    /** Línea de puntería roja durante la carga (aviso al jugador). */
    private void laserSight(@Nullable Vec3 dir) {
        if (dir == null || !(level() instanceof ServerLevel server)) {
            return;
        }
        Vec3 from = muzzle();
        Vec3 end = traceEnd(from, dir);
        Vec3 path = end.subtract(from);
        int steps = (int) (path.length() * 2);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(path.scale(i / (double) Math.max(1, steps)));
            server.sendParticles(LASER, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    private Vec3 traceEnd(Vec3 from, Vec3 dir) {
        Vec3 end = from.add(dir.scale(RANGE));
        BlockHitResult block = level().clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return block.getType() == HitResult.Type.MISS ? end : block.getLocation();
    }

    private void fire(@Nullable Vec3 dir) {
        if (dir == null || !(level() instanceof ServerLevel server)) {
            return;
        }
        Vec3 from = muzzle();
        Vec3 end = traceEnd(from, dir);
        AABB search = new AABB(from, end).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level(), this, from, end, search,
                e -> e instanceof LivingEntity && e != this && !(e instanceof SentryTurretEntity) && !e.isSpectator(), 0.3F);
        if (hit != null) {
            Vec3 at = hit.getEntity().getBoundingBox().clip(from, end).orElse(hit.getEntity().getBoundingBox().getCenter());
            doHurtTarget(hit.getEntity());
            end = at;
        }
        Vec3 path = end.subtract(from);
        int steps = (int) (path.length() * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(path.scale(i / (double) Math.max(1, steps)));
            server.sendParticles(ModParticles.KINETIC_TRACER.get(), p.x, p.y, p.z, 0, 0, 0, 0, 0);
        }
        server.sendParticles(ModParticles.KINETIC_SPARK.get(), end.x, end.y, end.z, 8, 0.05, 0.05, 0.05, 0.2);
        playSound(ModSounds.PISTOL_FIRE.get(), 1.4F, 0.7F);
    }

    // ------------------------------------------------------------------ física: anclada

    @Override
    public void travel(Vec3 input) {
        if (!onGround()) {
            super.travel(Vec3.ZERO);                                     // solo cae si no tiene suelo
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions dims) {
        return 0.78F;                                                    // a la altura de la cabeza giratoria
    }

    @Override
    public int getMaxHeadYRot() {
        return 180;
    }

    // ------------------------------------------------------------------ sonidos

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SWARM_BODY_DEATH.get();
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }
}
