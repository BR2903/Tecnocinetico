package com.bryanamaya.tecnocinetico.entity.custom;

import com.bryanamaya.tecnocinetico.effect.ModEffects;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
import com.bryanamaya.tecnocinetico.sound.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;

/**
 * Anclador: trípode pesado del Núcleo Directriz con un núcleo de inercita. Es el antiaéreo de las instalaciones:
 * - si su objetivo está en el aire (jetpack, élitros, un salto alto), carga 1 s un haz azul-violeta y le aplica
 *   Lastre de Inercia (3 s): cae a plomo y el jetpack se apaga. Hay que cortar la línea de visión para evitarlo;
 * - si lo tiene cerca y en el suelo, da un pisotón: onda de inercia que hiere, ralentiza y empuja.
 * Lento y blindado (40 de vida, 8 de armadura, casi no se le puede empujar).
 */
public class AnchorEntity extends Monster {

    /** 0 = en reposo, 1 = cargando el haz, 2 = preparando el pisotón (brillo y animación). */
    private static final EntityDataAccessor<Byte> MODE = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.BYTE);
    private static final DustParticleOptions BEAM = new DustParticleOptions(new Vector3f(0.45F, 0.5F, 1.0F), 0.9F);
    private static final DustParticleOptions BEAM_CORE = new DustParticleOptions(new Vector3f(0.85F, 0.85F, 1.0F), 0.6F);

    private static final int BEAM_CHARGE = 20, BEAM_COOLDOWN = 80, STOMP_WINDUP = 12, STOMP_COOLDOWN = 60;
    private static final double BEAM_RANGE = 28.0D, STOMP_RADIUS = 4.5D;

    public AnchorEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 20;
        setMaxUpStep(1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 2.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new AnchorAttackGoal());
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(MODE, (byte) 0);
    }

    public int mode() {
        return entityData.get(MODE);
    }

    private void setMode(int m) {
        entityData.set(MODE, (byte) m);
    }

    /** ¿Está en el aire de verdad? (más de 2,5 bloques sin suelo debajo, o con élitros) */
    public static boolean airborne(LivingEntity t) {
        if (t.isFallFlying()) {
            return true;
        }
        if (t.onGround() || t.isInWater() || t.isPassenger()) {
            return false;
        }
        Vec3 from = t.position();
        HitResult hit = t.level().clip(new ClipContext(from, from.subtract(0, 2.5D, 0), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY, t));
        return hit.getType() == HitResult.Type.MISS;
    }

    private Vec3 emitter() {
        return position().add(0, getBbHeight() + 0.1D, 0);
    }

    // ------------------------------------------------------------------ ataques

    class AnchorAttackGoal extends Goal {
        private int beam, stomp, beamCd = 20, stompCd = 20;

        AnchorAttackGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public void stop() {
            beam = 0;
            stomp = 0;
            setMode(0);
            getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) {
                return;
            }
            if (beamCd > 0) {
                beamCd--;
            }
            if (stompCd > 0) {
                stompCd--;
            }
            getLookControl().setLookAt(t, 20.0F, 40.0F);
            boolean see = getSensing().hasLineOfSight(t);
            double dist = distanceTo(t);

            if (beam > 0) {                                                      // cargando el haz: quieto
                getNavigation().stop();
                beam++;
                if (beam % 2 == 0) {
                    beamParticles(t, false);
                }
                if (beam >= BEAM_CHARGE) {
                    if (hasLineOfSight(t) && dist < BEAM_RANGE + 4) {
                        anchor(t);
                    }
                    beam = 0;
                    beamCd = BEAM_COOLDOWN;
                    setMode(0);
                }
                return;
            }
            if (stomp > 0) {                                                     // preparando el pisotón
                getNavigation().stop();
                if (++stomp >= STOMP_WINDUP) {
                    stompNow();
                    stomp = 0;
                    stompCd = STOMP_COOLDOWN;
                    setMode(0);
                }
                return;
            }
            if (beamCd <= 0 && see && dist < BEAM_RANGE && airborne(t)) {
                beam = 1;
                setMode(1);
                playSound(ModSounds.ANCHOR_CHARGE.get(), 1.4F, 1.0F);
                return;
            }
            if (stompCd <= 0 && dist < STOMP_RADIUS - 0.5 && onGround()) {
                stomp = 1;
                setMode(2);
                playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.2F, 0.6F);
                return;
            }
            if (!see || dist > 10.0D) {
                getNavigation().moveTo(t, 1.0D);
            } else if (dist > 3.0D) {
                getNavigation().moveTo(t, 0.8D);
            } else {
                getNavigation().stop();
            }
        }
    }

    private void beamParticles(LivingEntity t, boolean fire) {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        Vec3 from = emitter(), to = t.position().add(0, t.getBbHeight() * 0.5, 0);
        Vec3 path = to.subtract(from);
        int steps = (int) (path.length() * (fire ? 4 : 2));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(path.scale(i / (double) Math.max(1, steps)));
            server.sendParticles(fire ? BEAM_CORE : BEAM, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        if (fire) {
            server.sendParticles(BEAM, to.x, to.y, to.z, 30, 0.4, 0.6, 0.4, 0.0);
        }
    }

    /** El haz alcanza: Lastre de Inercia 3 s y un golpe. */
    private void anchor(LivingEntity t) {
        beamParticles(t, true);
        t.addEffect(new MobEffectInstance(ModEffects.INERTIA_ANCHOR.get(), 60, 0), this);
        t.hurt(damageSources().indirectMagic(this, this), 3.0F);
        level().playSound(null, t.getX(), t.getY(), t.getZ(), ModSounds.ANCHOR_PULL.get(), getSoundSource(), 1.6F, 1.0F);
        playSound(ModSounds.ANCHOR_PULL.get(), 1.0F, 0.8F);
    }

    /** Pisotón: onda de inercia alrededor. */
    private void stompNow() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(STOMP_RADIUS, 1.5D, STOMP_RADIUS),
                e -> e != this && !(e instanceof AnchorEntity) && e.isAlive() && e.distanceTo(this) <= STOMP_RADIUS + 0.5)) {
            if (e.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE))) {
                Vec3 away = e.position().subtract(position()).multiply(1, 0, 1).normalize();
                e.knockback(1.2D, -away.x, -away.z);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), this);
            }
        }
        BlockState below = level().getBlockState(blockPosition().below());
        for (int i = 0; i < 32; i++) {
            double a = i / 32.0 * Math.PI * 2;
            for (double r = 1.0; r <= STOMP_RADIUS; r += 1.2) {
                server.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, below),
                        getX() + Math.cos(a) * r, getY() + 0.1, getZ() + Math.sin(a) * r, 1, 0, 0.05, 0, 0.1);
            }
            server.sendParticles(BEAM, getX() + Math.cos(a) * STOMP_RADIUS, getY() + 0.3, getZ() + Math.sin(a) * STOMP_RADIUS, 1, 0, 0, 0, 0);
        }
        server.sendParticles(ModParticles.KINETIC_SPARK.get(), getX(), getY() + 0.5, getZ(), 20, 0.6, 0.2, 0.6, 0.2);
        playSound(ModSounds.ANCHOR_STOMP.get(), 1.8F, 1.0F);
    }

    // ------------------------------------------------------------------ física y sonidos

    @Override
    public boolean causeFallDamage(float distance, float mult, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ANCHOR_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 0.6F, 1.3F);
    }

    @Override
    public float getVoicePitch() {
        return 0.8F;
    }
}
