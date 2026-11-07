package com.bryanamaya.tecnocinetico.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.bryanamaya.tecnocinetico.sound.ModSounds;

import java.util.EnumSet;

/**
 * Cuerpo de Enjambre.
 * "No es completamente máquina. Tampoco es completamente organismo."
 * Ataca con las patas delanteras (cuchillas) y usa las traseras para lanzar saltos violentos.
 */
public class SwarmBodyEntity extends Monster {

    public SwarmBodyEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 34.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.ARMOR, 6.0D)              // placas de Inercita
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4D)
                .add(Attributes.FOLLOW_RANGE, 28.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new KineticLeapGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    // Es en parte máquina: no recibe daño por caída (aterriza sobre sus patas de impulso)
    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return false;
    }

    // --- Sonidos propios (sintetizados con tools/sound_gen.py): mezcla orgánica y mecánica ---
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SWARM_BODY_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return ModSounds.SWARM_BODY_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SWARM_BODY_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pPos, BlockState pState) {
        this.playSound(ModSounds.SWARM_BODY_STEP.get(), 0.35F, 0.9F + this.random.nextFloat() * 0.2F);
    }

    @Override
    public float getVoicePitch() {
        return 0.7F + this.random.nextFloat() * 0.2F;
    }

    /** Salto cinético: cuando el objetivo está a media distancia, se lanza contra él y le golpea al alcanzarlo. */
    static class KineticLeapGoal extends Goal {
        private static final int COOLDOWN_TICKS = 70;
        private final SwarmBodyEntity mob;
        private LivingEntity target;
        private int nextLeapTick;
        private boolean hasHit;
        private int airTicks;

        KineticLeapGoal(SwarmBodyEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (mob.tickCount < nextLeapTick || !mob.onGround()) {
                return false;
            }
            target = mob.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            double distSq = mob.distanceToSqr(target);
            return distSq > 9.0D && distSq < 100.0D
                    && mob.hasLineOfSight(target)
                    && mob.getRandom().nextInt(reducedTickDelay(8)) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            // Dura mientras esté en el aire (con un límite por seguridad)
            return airTicks < 40 && (airTicks < 3 || !mob.onGround());
        }

        @Override
        public boolean isInterruptable() {
            return false;
        }

        @Override
        public void start() {
            hasHit = false;
            airTicks = 0;
            nextLeapTick = mob.tickCount + COOLDOWN_TICKS;

            Vec3 toTarget = new Vec3(target.getX() - mob.getX(), 0, target.getZ() - mob.getZ());
            double horizontal = Math.min(1.4D, 0.35D + toTarget.length() * 0.12D);
            Vec3 dir = toTarget.normalize().scale(horizontal);
            mob.setDeltaMovement(dir.x, 0.55D, dir.z);
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                    ModSounds.SWARM_BODY_LEAP.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            if (mob.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, mob.getX(), mob.getY() + 0.2, mob.getZ(),
                        12, 0.4, 0.1, 0.4, 0.15);
            }
        }

        @Override
        public void tick() {
            airTicks++;
            if (!hasHit && target != null && target.isAlive()
                    && mob.getBoundingBox().inflate(0.4D).intersects(target.getBoundingBox())) {
                hasHit = true;
                float damage = (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.25F;
                if (target.hurt(mob.damageSources().mobAttack(mob), damage)) {
                    Vec3 push = mob.getDeltaMovement().multiply(1, 0, 1).normalize().scale(0.6D);
                    target.push(push.x, 0.25D, push.z);
                }
                mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
        }

        @Override
        public void stop() {
            target = null;
        }
    }
}
