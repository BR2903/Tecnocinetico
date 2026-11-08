package com.bryanamaya.tecnocinetico.entity.custom;

import com.bryanamaya.tecnocinetico.sound.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Torreta Centinela de las instalaciones del Núcleo Directriz. Anclada (no se mueve ni se empuja); gira la
 * cabeza hacia el jugador con velocidad limitada y alterna dos ataques (ver {@link CoreShots}): ráfagas rápidas
 * de poco daño y un disparo cargado muy fuerte con línea roja que se fija poco antes de disparar.
 */
public class SentryTurretEntity extends Monster {

    private static final EntityDataAccessor<Boolean> CHARGING =
            SynchedEntityData.defineId(SentryTurretEntity.class, EntityDataSerializers.BOOLEAN);
    private static final double RANGE = 22.0D;
    // ráfaga: 8 tiros de 2,5 cada 2 ticks; cargado: 1,1 s de carga, se fija 0,4 s antes y hace 14 de daño
    private static final int BURST = 8, BURST_GAP = 2, AFTER_BURST = 12, CHARGE_TICKS = 22, LOCK_TICKS = 8, AFTER_HEAVY = 18;
    private static final float BURST_DAMAGE = 2.5F, HEAVY_DAMAGE = 14.0F;
    private static final double BURST_SPREAD = 3.5D;
    private static final float TURN_SPEED = 7.0F;                      // grados por tick

    private int charge, cooldown, burstLeft, burstTimer, bursts;
    private Vec3 lockPoint = Vec3.ZERO;

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
        if (charge > 0) {                                                // disparo cargado
            charge++;
            if (target != null && charge <= CHARGE_TICKS - LOCK_TICKS) {
                lockPoint = CoreShots.aimPoint(target);                  // sigue al objetivo...
            }                                                            // ...y se fija los últimos ticks
            turnTowards(lockPoint.subtract(getEyePosition()), TURN_SPEED * 1.5F);
            Vec3 from = muzzle();
            if (charge % 2 == 0) {
                CoreShots.laser(this, from, lockPoint.add(lockPoint.subtract(from).normalize().scale(4)));
            }
            if (charge >= CHARGE_TICKS) {
                CoreShots.fire(this, from, lockPoint.subtract(from), RANGE, HEAVY_DAMAGE, 0.0, SentryTurretEntity.class, true);
                playSound(ModSounds.PISTOL_FIRE.get(), 1.6F, 0.55F);
                charge = 0;
                bursts = 0;
                cooldown = AFTER_HEAVY;
                entityData.set(CHARGING, false);
            }
            return;
        }
        if (target == null) {
            burstLeft = 0;
            // barrido lento de vigilancia
            setYHeadRot(yHeadRot + 1.2F);
            setXRot(Mth.approach(getXRot(), 0.0F, 2.0F));
            return;
        }
        boolean aimed = turnTowards(target.getEyePosition().subtract(getEyePosition()), TURN_SPEED);
        if (burstLeft > 0) {                                             // ráfaga: dispara hacia donde mira
            if (--burstTimer <= 0) {
                CoreShots.fire(this, muzzle(), Vec3.directionFromRotation(getXRot(), yHeadRot), RANGE, BURST_DAMAGE,
                        BURST_SPREAD, SentryTurretEntity.class, false);
                playSound(ModSounds.PISTOL_FIRE.get(), 0.9F, 1.5F + random.nextFloat() * 0.2F);
                burstLeft--;
                burstTimer = BURST_GAP;
                if (burstLeft == 0) {
                    cooldown = AFTER_BURST;
                }
            }
            return;
        }
        if (aimed && cooldown <= 0) {
            if (bursts >= 2 || random.nextFloat() < 0.35F) {
                charge = 1;
                lockPoint = CoreShots.aimPoint(target);
                entityData.set(CHARGING, true);
                playSound(ModSounds.SENTRY_TURRET_CHARGE.get(), 1.2F, 1.0F);
            } else {
                burstLeft = BURST;
                burstTimer = 0;
                bursts++;
            }
        }
    }

    /** Gira la cabeza hacia {@code to} con velocidad limitada; true si ya apunta. */
    private boolean turnTowards(Vec3 to, float speed) {
        float wantYaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0F;
        float wantPitch = (float) -(Mth.atan2(to.y, to.horizontalDistance()) * Mth.RAD_TO_DEG);
        setYHeadRot(Mth.approachDegrees(yHeadRot, wantYaw, speed));
        setXRot(Mth.approach(getXRot(), wantPitch, speed));
        return Math.abs(Mth.degreesDifference(yHeadRot, wantYaw)) < 4.0F && Math.abs(getXRot() - wantPitch) < 4.0F;
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
