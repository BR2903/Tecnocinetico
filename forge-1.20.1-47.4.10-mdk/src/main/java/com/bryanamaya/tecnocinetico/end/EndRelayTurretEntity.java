package com.bryanamaya.tecnocinetico.end;

import com.bryanamaya.tecnocinetico.particle.ModParticles;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * Torreta-repetidor (End, Dragón modificado): sustituye a cada Cristal del End en lo alto de las torres rotas.
 * <ul>
 *   <li><b>Cura</b> al Dragón como un cristal: solo la más cercana a él, si está a menos de {@link #HEAL_RANGE} bloques,
 *       1 de vida cada 10 ticks, con el haz visible ({@link #healTarget()}, lo dibuja su renderizador).</li>
 *   <li><b>Dispara</b> al jugador más cercano que vea: carga {@link #CHARGE_TICKS} con línea de aviso que se fija los
 *       últimos {@link #LOCK_TICKS} ticks (moverse lo esquiva) y descarga {@link #SHOT_DAMAGE}.</li>
 *   <li>Al morir <b>explota</b> (sin romper bloques) y, si la mató un jugador, el Dragón recibe 10 de daño (como al
 *       romper un cristal).</li>
 * </ul>
 */
public class EndRelayTurretEntity extends Monster {

    private static final EntityDataAccessor<Integer> HEAL_TARGET = SynchedEntityData.defineId(EndRelayTurretEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHARGE = SynchedEntityData.defineId(EndRelayTurretEntity.class, EntityDataSerializers.INT);
    public static final double HEAL_RANGE = 48.0D, RANGE = 40.0D;
    public static final int CHARGE_TICKS = 30, LOCK_TICKS = 8, COOLDOWN = 50;
    public static final float SHOT_DAMAGE = 8.0F;
    public static final DustParticleOptions AIM = new DustParticleOptions(new Vector3f(1.0F, 0.3F, 0.85F), 0.7F);

    private int cooldown = 20;
    private Vec3 lock = Vec3.ZERO;

    public EndRelayTurretEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 20;
        setPersistenceRequired();
        this.lookControl = new net.minecraft.world.entity.ai.control.LookControl(this) {
            @Override
            public void tick() {
            }
        };
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, RANGE);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(HEAL_TARGET, -1);
        entityData.define(CHARGE, 0);
    }

    /** Id de la entidad que cura (el Dragón) o -1. */
    public int healTarget() {
        return entityData.get(HEAL_TARGET);
    }

    /** Carga del disparo, 0..1 (para la animación del modelo). */
    public float charge() {
        return entityData.get(CHARGE) / (float) CHARGE_TICKS;
    }

    // ------------------------------------------------------------------ comportamiento

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        if (tickCount % 10 == 0) {
            EnderDragon d = EndArena.dragon(server);
            boolean heals = d != null && d.isAlive() && healStep(d);
            entityData.set(HEAL_TARGET, heals ? d.getId() : -1);
        }
        attack(server);
    }

    /** Cura 1 al Dragón si esta es la torreta viva más cercana a él y está en alcance. Devuelve si cura. */
    public boolean healStep(EnderDragon d) {
        if (distanceToSqr(d) > HEAL_RANGE * HEAL_RANGE) {
            return false;
        }
        for (EndRelayTurretEntity other : level().getEntitiesOfClass(EndRelayTurretEntity.class, d.getBoundingBox().inflate(HEAL_RANGE))) {
            if (other != this && other.isAlive() && other.distanceToSqr(d) < distanceToSqr(d)) {
                return false;
            }
        }
        if (d.getHealth() < d.getMaxHealth()) {
            d.heal(1.0F);
        }
        return true;
    }

    private void attack(ServerLevel server) {
        int charge = entityData.get(CHARGE);
        Player target = findTarget();
        if (charge > 0) {
            charge++;
            if (target != null && charge <= CHARGE_TICKS - LOCK_TICKS) {
                lock = target.position().add(0, target.getBbHeight() * 0.6, 0);
            }
            aimAt(lock);
            if (charge % 2 == 0) {
                line(server, muzzle(), lock, AIM, 2);
            }
            if (charge >= CHARGE_TICKS) {
                fire(server);
                charge = 0;
                cooldown = COOLDOWN + random.nextInt(20);
            }
            entityData.set(CHARGE, charge);
            return;
        }
        if (target == null) {
            setYHeadRot(yHeadRot + 1.5F);
            return;
        }
        aimAt(target.getEyePosition());
        if (--cooldown <= 0) {
            lock = target.position().add(0, target.getBbHeight() * 0.6, 0);
            entityData.set(CHARGE, 1);
            playSound(SoundEvents.BEACON_POWER_SELECT, 1.5F, 1.6F);
        }
    }

    private void fire(ServerLevel server) {
        Vec3 from = muzzle();
        Vec3 dir = lock.subtract(from).normalize();
        Vec3 end = from.add(dir.scale(RANGE));
        BlockHitResult block = level().clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        var hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level(), this, from, end,
                new AABB(from, end).inflate(1.0), e -> e instanceof LivingEntity && !(e instanceof EnderDragon)
                        && !(e instanceof EndRelayTurretEntity) && !e.isSpectator(), 0.35F);
        if (hit != null) {
            end = hit.getEntity().getBoundingBox().clip(from, end).orElse(hit.getEntity().getBoundingBox().getCenter());
            hit.getEntity().hurt(damageSources().mobProjectile(this, this), SHOT_DAMAGE);
        }
        line(server, from, end, new DustParticleOptions(new Vector3f(1.0F, 0.75F, 1.0F), 1.3F), 3);   // núcleo del rayo
        line(server, from, end, ModParticles.DATA_GLITCH.get(), 1);
        server.sendParticles(ModParticles.DATA_GLITCH.get(), end.x, end.y, end.z, 16, 0.2, 0.2, 0.2, 0.1);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 1.5F, 1.8F);
    }

    private static void line(ServerLevel server, Vec3 from, Vec3 to, net.minecraft.core.particles.ParticleOptions particle, int perBlock) {
        com.bryanamaya.tecnocinetico.util.ParticleShapes.line(server, particle, from, to, Math.max(1, (int) (to.distanceTo(from) * perBlock)));
    }

    private void aimAt(Vec3 to) {
        Vec3 d = to.subtract(getEyePosition());
        setYHeadRot(Mth.approachDegrees(yHeadRot, (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F, 10.0F));
        setXRot(Mth.approach(getXRot(), (float) -(Mth.atan2(d.y, d.horizontalDistance()) * Mth.RAD_TO_DEG), 10.0F));
    }

    @Nullable
    private Player findTarget() {
        if (tickCount % 4 != 0) {                                            // la vista se recalcula cada 4 ticks
            return getTarget() instanceof Player cur && cur.isAlive() && !cur.isCreative() && !cur.isSpectator()
                    && distanceToSqr(cur) < RANGE * RANGE ? cur : null;
        }
        Player p = level().getNearestPlayer(getX(), getY(), getZ(), RANGE,
                e -> e instanceof Player pl && !pl.isCreative() && !pl.isSpectator() && getSensing().hasLineOfSight(pl));
        setTarget(p);
        return p;
    }

    public Vec3 muzzle() {
        return getEyePosition().add(Vec3.directionFromRotation(getXRot(), yHeadRot).scale(0.75D));   // boca del emisor
    }

    // ------------------------------------------------------------------ aliada del Dragón

    /** El Dragón no daña a sus torretas: ni su cuerpo y alas al pasar, ni su aliento ni sus bolas de fuego. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (fromDragon(source)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    public static boolean fromDragon(DamageSource source) {
        Entity attacker = source.getEntity(), direct = source.getDirectEntity();
        return attacker instanceof EnderDragon || direct instanceof EnderDragon
                || direct instanceof net.minecraft.world.entity.boss.EnderDragonPart
                || direct instanceof net.minecraft.world.entity.projectile.DragonFireball
                || direct instanceof net.minecraft.world.entity.AreaEffectCloud cloud && cloud.getOwner() instanceof EnderDragon;
    }

    /** Es una máquina: las pociones y nubes (p. ej. el aliento del Dragón) no le hacen nada. */
    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) {
        return false;
    }

    // ------------------------------------------------------------------ muerte: explota como un cristal

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server) {
            server.explode(this, getX(), getY() + 0.5, getZ(), 3.0F, Level.ExplosionInteraction.NONE);
            server.sendParticles(ModParticles.DATA_GLITCH.get(), getX(), getY() + 0.8, getZ(), 40, 0.5, 0.6, 0.5, 0.15);
            EnderDragon d = EndArena.dragon(server);
            if (d != null && source.getEntity() instanceof Player) {
                d.hurt(d.head, damageSources().explosion(this, source.getEntity()), 10.0F);
            }
        }
    }

    // ------------------------------------------------------------------ anclada

    @Override
    public void travel(Vec3 input) {
        if (!onGround()) {
            super.travel(Vec3.ZERO);
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
    public void push(Entity e) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions dims) {
        return 1.0F;                                                     // pivote de la cabeza (modelo de Codex)
    }

    @Override
    public int getMaxHeadYRot() {
        return 180;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BEACON_DEACTIVATE;
    }
}
