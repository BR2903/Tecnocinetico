package com.bryanamaya.tecnocinetico.nether;

import com.bryanamaya.tecnocinetico.entity.custom.CoreShots;
import com.bryanamaya.tecnocinetico.sound.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Enemigos industriales del Nether. Una sola clase para todos (el tipo de entidad decide el comportamiento), porque
 * comparten alianza, modelo ({@code InfernalModel}) y, más adelante, la lógica de los jefes.
 * <ul>
 *   <li><b>Ácaro de Escoria</b>: cuerpo a cuerpo, rápido y saltarín; ataca en grupo.</li>
 *   <li><b>Centinela de Ceniza</b>: fija un punto con un láser durante {@link #SENTINEL_WINDUP} ticks y dispara ahí
 *       (moverse lo esquiva). Si el objetivo se pega a él, usa el golpe cuerpo a cuerpo.</li>
 * </ul>
 * Todo se decide en el servidor; el cliente solo recibe {@link #attack()} para animar.
 */
public class InfernalEntity extends Monster {

    /** Ataque en curso (0 = ninguno, 1 = disparo del centinela cargando). Lo usa el modelo. */
    private static final EntityDataAccessor<Integer> ATTACK = SynchedEntityData.defineId(InfernalEntity.class, EntityDataSerializers.INT);
    /** Núcleo expuesto (recibe más daño). Reservado para los jefes; el modelo y el brillo ya lo leen. */
    private static final EntityDataAccessor<Boolean> EXPOSED = SynchedEntityData.defineId(InfernalEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int SENTINEL_WINDUP = 18, SENTINEL_COOLDOWN = 45;
    public static final double SENTINEL_RANGE = 36.0D;
    public static final float SENTINEL_SHOT_DAMAGE = 9.0F;
    /** Más cerca que esto el centinela no dispara: golpea. */
    private static final double SENTINEL_MELEE_SQR = 9.0D;

    private int cooldown = SENTINEL_COOLDOWN, windup;
    /** Punto fijado al empezar a cargar el disparo. */
    private Vec3 lock = Vec3.ZERO;

    public InfernalEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = mite() ? 4 : 24;
        setMaxUpStep(mite() ? 1.0F : 1.2F);
    }

    public boolean mite() {
        return getType() == NetherEntities.MITE.get();
    }

    public boolean sentinel() {
        return getType() == NetherEntities.SENTINEL.get();
    }

    /** Jefes (Fundidor y Devorador): todavía no están activos. El modelo ya pregunta por ellos. */
    public boolean boss() {
        return false;
    }

    public boolean foundry() {
        return false;
    }

    public int attack() {
        return entityData.get(ATTACK);
    }

    public boolean exposed() {
        return entityData.get(EXPOSED);
    }

    public static AttributeSupplier.Builder attributes(double health, double armor, double damage, double speed) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.ARMOR_TOUGHNESS, armor / 3)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.KNOCKBACK_RESISTANCE, armor > 15 ? 1 : 0.3);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ATTACK, 0);
        entityData.define(EXPOSED, false);
    }

    @Override
    protected void registerGoals() {
        if (getType() == NetherEntities.MITE.get()) {
            goalSelector.addGoal(1, new LeapAtTargetGoal(this, 0.38F));
        }
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 32.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, InfernalEntity.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ------------------------------------------------------------------ alianzas

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof InfernalEntity || super.isAlliedTo(other);
    }

    @Override
    public void setTarget(LivingEntity target) {
        if (target == null || !isAlliedTo(target)) {
            super.setTarget(target);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof InfernalEntity) {
            return false;                                                     // no se dañan entre sí
        }
        return super.hurt(source, exposed() ? amount * 1.6F : amount);
    }

    // ------------------------------------------------------------------ combate (servidor)

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server && sentinel()) {
            sentinelTick(server);
        }
    }

    private void sentinelTick(ServerLevel server) {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || !canAttack(target) || distanceToSqr(target) > SENTINEL_RANGE * SENTINEL_RANGE) {
            windup = 0;
            entityData.set(ATTACK, 0);
            return;
        }
        if (windup > 0) {
            getNavigation().stop();
            getLookControl().setLookAt(lock.x, lock.y, lock.z);
            if (tickCount % 4 == 0) {
                CoreShots.laser(this, getEyePosition(), lock);                // aviso: línea roja al punto fijado
            }
            if (--windup == 0) {
                CoreShots.fire(this, getEyePosition(), lock.subtract(getEyePosition()), SENTINEL_RANGE, SENTINEL_SHOT_DAMAGE, 2,
                        InfernalEntity.class, false);
                server.playSound(null, getX(), getY(), getZ(), ModSounds.AUTO_FIRE.get(), SoundSource.HOSTILE, 1.2F, 0.6F);
                entityData.set(ATTACK, 0);
                cooldown = SENTINEL_COOLDOWN;
            }
        } else if (--cooldown <= 0 && distanceToSqr(target) > SENTINEL_MELEE_SQR && hasLineOfSight(target)) {
            lock = CoreShots.aimPoint(target);
            windup = SENTINEL_WINDUP;
            entityData.set(ATTACK, 1);
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 0.5F, 1.9F);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return windup == 0 && super.doHurtTarget(target);                     // mientras apunta no golpea
    }

    // ------------------------------------------------------------------ sonidos

    @Override
    protected SoundEvent getAmbientSound() {
        return mite() ? SoundEvents.SILVERFISH_AMBIENT : SoundEvents.IRON_GOLEM_REPAIR;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return mite() ? SoundEvents.SILVERFISH_HURT : SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return mite() ? SoundEvents.SILVERFISH_DEATH : SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return mite() ? 0.6F + random.nextFloat() * 0.2F : 0.7F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return mite() ? 120 : 260;
    }
}
