package com.bryanamaya.tecnocinetico.end;

import com.bryanamaya.tecnocinetico.effect.ModEffects;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
import com.bryanamaya.tecnocinetico.util.ParticleShapes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
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
 * Espectro de Datos (End, islas exteriores): proyección de luz sólida de la red de la IA alrededor de un pequeño
 * emisor. No es magia: mientras está <b>desfasado</b> ({@link #PHASED}) la proyección no tiene masa y nada le da
 * (las balas y flechas lo atraviesan); ronda al jugador y salta de sitio con glitches.
 * <ul>
 *   <li>Para atacar se <b>materializa</b> ({@link #CHARGE}, {@link #CHARGE_TICKS} ticks con aviso): desde ahí se le
 *       puede golpear.</li>
 *   <li><b>Garras</b> (lejos): línea de aviso que se fija al final y se lanza; {@link #CLAW_DAMAGE} y Corrupción de
 *       datos.</li>
 *   <li><b>Pulso</b> (cerca, &lt; {@link #PULSE_NEAR}): anillo de aviso y descarga de {@link #PULSE_DAMAGE} en
 *       {@link #PULSE_RADIUS} bloques, con Corrupción.</li>
 *   <li>Después queda <b>expuesto</b> ({@link #VULNERABLE}) {@link #VULNERABLE_TICKS} ticks, recibiendo
 *       ×{@link #EXPOSED_MULTIPLIER}.</li>
 * </ul>
 * Contrato con el renderizador: {@link #state()}, {@link #kind()}, {@link #materialize(float)},
 * {@link #attackAnim(float)}, {@link #pulseAnim(float)}.
 */
public class DataSpecterEntity extends Monster {

    public static final int PHASED = 0, CHARGE = 1, STRIKE = 2, VULNERABLE = 3;
    public static final int CLAW = 0, PULSE = 1;
    public static final int CHARGE_TICKS = 20, LOCK_TICKS = 6, STRIKE_TICKS = 10, VULNERABLE_TICKS = 40, CORRUPTION_TICKS = 80;
    public static final float CLAW_DAMAGE = 12.0F, PULSE_DAMAGE = 9.0F, EXPOSED_MULTIPLIER = 1.5F;
    public static final double PULSE_RADIUS = 4.5D, PULSE_NEAR = 5.0D, CLAW_RANGE = 14.0D, ORBIT = 7.0D, SENSE = 24.0D;

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(DataSpecterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(DataSpecterEntity.class, EntityDataSerializers.INT);

    private int timer, cooldown = 40, blink = 60, stateTick;
    private Vec3 lock = Vec3.ZERO, dash = Vec3.ZERO;
    private boolean struck;

    public DataSpecterEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
        xpReward = 25;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 70.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.ATTACK_DAMAGE, CLAW_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FLYING_SPEED, 0.4D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(STATE, PHASED);
        entityData.define(KIND, CLAW);
    }

    @Override
    protected void registerGoals() {
    }

    // ------------------------------------------------------------------ consultas (también el cliente)

    public int state() {
        return entityData.get(STATE);
    }

    public int kind() {
        return entityData.get(KIND);
    }

    private float since(float partial) {
        return tickCount - stateTick + partial;
    }

    /** 0 = holograma desfasado, 1 = sólido. */
    public float materialize(float partial) {
        return switch (state()) {
            case CHARGE -> Mth.clamp(since(partial) / CHARGE_TICKS, 0.0F, 1.0F);
            case STRIKE -> 1.0F;
            case VULNERABLE -> Mth.clamp(1.0F - (since(partial) - (VULNERABLE_TICKS - 8)) / 8.0F, 0.0F, 1.0F);
            default -> 0.0F;
        };
    }

    /** Golpe de garras 0..1 (hasta 0,55 se echa atrás; luego corta). */
    public float attackAnim(float partial) {
        if (kind() != CLAW) {
            return 0.0F;
        }
        return switch (state()) {
            case CHARGE -> 0.55F * Mth.clamp(since(partial) / CHARGE_TICKS, 0.0F, 1.0F);   // se echa atrás (modelo: 0..0,55)
            case STRIKE -> 0.55F + 0.45F * Mth.clamp(since(partial) / 4.0F, 0.0F, 1.0F);   // corte rápido
            default -> 0.0F;
        };
    }

    /** Descarga en anillo 0..1. */
    public float pulseAnim(float partial) {
        if (kind() != PULSE) {
            return 0.0F;
        }
        return switch (state()) {
            case CHARGE -> Mth.clamp(since(partial) / CHARGE_TICKS, 0.0F, 1.0F);
            case VULNERABLE -> Mth.clamp(1.0F - since(partial) / 8.0F, 0.0F, 1.0F);
            default -> 0.0F;
        };
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (STATE.equals(key)) {
            stateTick = tickCount;
        }
    }

    // ------------------------------------------------------------------ servidor

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        Player t = target();
        switch (state()) {
            case PHASED -> phased(server, t);
            case CHARGE -> charge(server, t);
            case STRIKE -> strike(server);
            case VULNERABLE -> vulnerable(server);
            default -> setState(PHASED);
        }
    }

    private void setState(int s) {
        entityData.set(STATE, s);
        stateTick = tickCount;
        timer = switch (s) {
            case CHARGE -> CHARGE_TICKS;
            case STRIKE -> STRIKE_TICKS;
            case VULNERABLE -> VULNERABLE_TICKS;
            default -> 0;
        };
    }

    private static boolean valid(Player p) {
        return p.isAlive() && !p.isCreative() && !p.isSpectator();
    }

    @Nullable
    private Player target() {
        if (getTarget() instanceof Player p && valid(p) && distanceToSqr(p) < 32 * 32) {
            return p;
        }
        if (tickCount % 10 != 0) {
            return null;
        }
        Player best = null;
        for (Player p : level().players()) {
            if (valid(p) && distanceToSqr(p) < SENSE * SENSE && getSensing().hasLineOfSight(p) && (best == null || distanceToSqr(p) < distanceToSqr(best))) {
                best = p;
            }
        }
        setTarget(best);
        return best;
    }

    private void phased(ServerLevel server, @Nullable Player t) {
        if (cooldown > 0) {
            cooldown--;
        }
        if (blink > 0) {
            blink--;
        }
        float bob = Mth.sin(tickCount * 0.08F) * 0.4F;
        if (t == null) {
            setDeltaMovement(getDeltaMovement().scale(0.8).add(0, bob * 0.01, 0));
            return;
        }
        double angle = tickCount * 0.025 + getId() * 1.7;
        Vec3 want = t.position().add(Math.cos(angle) * ORBIT, 1.5 + bob, Math.sin(angle) * ORBIT);
        steer(want, 0.28);
        lookAt(t, 30.0F, 30.0F);
        if (blink <= 0) {
            blinkNear(server, t);
        }
        double d = distanceTo(t);
        if (cooldown <= 0 && d < CLAW_RANGE && getSensing().hasLineOfSight(t)) {
            entityData.set(KIND, d < PULSE_NEAR ? PULSE : CLAW);
            lock = t.position().add(0, t.getBbHeight() * 0.5, 0);
            setState(CHARGE);
            server.playSound(null, getX(), getY(), getZ(), kind() == PULSE ? SoundEvents.WARDEN_SONIC_CHARGE : SoundEvents.BEACON_POWER_SELECT,
                    SoundSource.HOSTILE, 1.2F, 1.5F);
            EndMobEvents.glitch(server, this, 12);
        }
    }

    /** Salto de desfase: reaparece a 5-8 bloques del objetivo (si cabe y lo ve). */
    private void blinkNear(ServerLevel server, Player t) {
        blink = 60 + random.nextInt(30);
        for (int i = 0; i < 6; i++) {
            double a = random.nextDouble() * Math.PI * 2, r = 5 + random.nextDouble() * 3;
            Vec3 to = t.position().add(Math.cos(a) * r, 1 + random.nextDouble() * 2, Math.sin(a) * r);
            if (level().noCollision(this, getBoundingBox().move(to.subtract(position())))) {
                EndMobEvents.glitch(server, this, 16);
                teleportTo(to.x, to.y, to.z);
                setDeltaMovement(Vec3.ZERO);
                EndMobEvents.glitch(server, this, 16);
                server.playSound(null, to.x, to.y, to.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8F, 1.7F);
                return;
            }
        }
    }

    private void charge(ServerLevel server, @Nullable Player t) {
        setDeltaMovement(getDeltaMovement().scale(0.5));
        if (t != null && timer > LOCK_TICKS) {
            lock = t.position().add(0, t.getBbHeight() * 0.5, 0);
        }
        getLookControl().setLookAt(lock.x, lock.y, lock.z, 30.0F, 30.0F);
        if (timer % 2 == 0) {
            if (kind() == CLAW) {
                Vec3 from = position().add(0, getBbHeight() * 0.6, 0);
                ParticleShapes.line(server, ModParticles.DATA_GLITCH.get(), from, lock, Math.max(2, (int) from.distanceTo(lock) * 2));
            } else {
                ParticleShapes.ring(server, ModParticles.DATA_GLITCH.get(), position().add(0, 0.2, 0), PULSE_RADIUS, 28);
            }
        }
        if (--timer > 0) {
            return;
        }
        if (kind() == CLAW) {
            dash = lock.subtract(position().add(0, getBbHeight() * 0.5, 0)).normalize();
            struck = false;
            setState(STRIKE);
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5F, 0.6F);
        } else {
            pulse(server);
            setState(VULNERABLE);
        }
    }

    private void strike(ServerLevel server) {
        setDeltaMovement(dash.scale(1.1));
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.5), this::foe)) {
            if (!struck && e.hurt(damageSources().mobAttack(this), CLAW_DAMAGE)) {
                corrupt(e);
                e.push(dash.x * 0.6, 0.3, dash.z * 0.6);
                e.hurtMarked = true;
                struck = true;
            }
        }
        if (--timer <= 0 || struck || horizontalCollision || verticalCollision) {
            setDeltaMovement(Vec3.ZERO);
            setState(VULNERABLE);
        }
    }

    private void pulse(ServerLevel server) {
        Vec3 c = position().add(0, 0.5, 0);
        ParticleShapes.disk(server, ModParticles.DATA_GLITCH.get(), c, 1.0, PULSE_RADIUS, 1, 40, 2, 0.1, 0.1, 0.1, 0.15);
        server.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 0.8F, 1.6F);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(PULSE_RADIUS), this::foe)) {
            if (e.distanceTo(this) <= PULSE_RADIUS + 0.5 && e.hurt(damageSources().mobAttack(this), PULSE_DAMAGE)) {
                corrupt(e);
                Vec3 away = e.position().subtract(position()).multiply(1, 0, 1).normalize();
                e.push(away.x * 0.9, 0.35, away.z * 0.9);
                e.hurtMarked = true;
            }
        }
    }

    private void vulnerable(ServerLevel server) {
        setDeltaMovement(getDeltaMovement().scale(0.8).add(0, -0.005, 0));
        if (timer % 6 == 0) {
            server.sendParticles(ModParticles.DATA_GLITCH.get(), getX(), getY() + getBbHeight() * 0.5, getZ(), 2, 0.3, 0.5, 0.3, 0.02);
        }
        if (--timer <= 0) {
            cooldown = 50 + random.nextInt(30);
            setState(PHASED);
            EndMobEvents.glitch(server, this, 10);
        }
    }

    private boolean foe(LivingEntity e) {
        return e != this && e.isAlive() && !(e instanceof DataSpecterEntity) && !(e instanceof Player p && !valid(p));
    }

    private static void corrupt(LivingEntity e) {
        e.addEffect(new MobEffectInstance(ModEffects.DATA_CORRUPTION.get(), CORRUPTION_TICKS, 0));
    }

    private void steer(Vec3 want, double speed) {
        Vec3 to = want.subtract(position());
        double d = to.length();
        Vec3 v = d > 1e-3 ? to.scale(Math.min(speed, d * 0.15) / d) : Vec3.ZERO;
        setDeltaMovement(getDeltaMovement().scale(0.6).add(v.scale(0.4)));
    }

    // ------------------------------------------------------------------ daño

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (state() == PHASED && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;                                                     // la proyección no tiene masa
        }
        return super.hurt(source, state() == VULNERABLE ? amount * EXPOSED_MULTIPLIER : amount);
    }

    @Override
    public boolean isPickable() {
        return state() != PHASED && super.isPickable();
    }

    @Override
    public boolean isPushable() {
        return state() != PHASED && super.isPushable();
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(STATE, PHASED);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server) {
            EndMobEvents.glitch(server, this, 40);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BEACON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SHULKER_HURT_CLOSED;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BEACON_DEACTIVATE;
    }

    // ------------------------------------------------------------------ pruebas

    /** Fuerza un estado (pruebas). */
    public void testState(int s, int kind) {
        entityData.set(KIND, kind);
        setState(s);
    }
}
