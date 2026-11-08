package com.bryanamaya.tecnocinetico.entity.swarm;

import com.bryanamaya.tecnocinetico.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Avispa del Enjambre: insecto biomecánico volador. NO usa búsqueda de rutas (sería caro para un enjambre):
 * cada tick calcula un punto deseado según lo que ordena la {@link HiveMind} de su Nodo y vuela hacia él con
 * "steering" (llegada suave + separación entre avispas + esquiva de obstáculos), como un enjambre real.
 *
 * Tácticas que ejecuta: patrulla/órbita, vigilancia, ataque por turnos desde ángulos repartidos, esquiva
 * lateral cuando el jugador la apunta, rodeo por la espalda si se cubre con escudo, retirada a curarse.
 * Sin Nodo (huérfana) solo persigue de forma torpe y puede desaparecer.
 */
public class SwarmWaspEntity extends Monster {

    private static final EntityDataAccessor<Integer> ROLE =
            SynchedEntityData.defineId(SwarmWaspEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID hiveId;
    @Nullable
    private HiveNodeEntity node;
    private boolean orphan;
    private int lostTicks;

    private boolean striking;
    private int strikeTicks, recoverTicks;
    private boolean retreating;
    private int jinkSign = 1;
    private final float phase;
    @Nullable
    private Vec3 wanderPoint;

    public SwarmWaspEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
        this.xpReward = 5;
        this.phase = random.nextFloat() * Mth.TWO_PI;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, WaspRole.STINGER.health)
                .add(Attributes.ATTACK_DAMAGE, WaspRole.STINGER.damage)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.FLYING_SPEED, 0.6D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        // sin objetivos vanilla: lo decide la mente colmena (ver computeDesired)
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ROLE, WaspRole.STINGER.ordinal());
    }

    // ------------------------------------------------------------------ papel y colmena

    public WaspRole getRole() {
        return WaspRole.byId(entityData.get(ROLE));
    }

    public void setRole(WaspRole role) {
        entityData.set(ROLE, role.ordinal());
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(role.health);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(role.damage);
        setHealth(getMaxHealth());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (ROLE.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(getRole().scale);
    }

    public void joinHive(HiveNodeEntity hive, WaspRole role) {
        this.hiveId = hive.getUUID();
        this.node = hive;
        this.orphan = false;
        setRole(role);
        setPersistenceRequired();
        hive.mind().checkIn(this);
    }

    /** El Nodo murió o se perdió: sin tácticas, sin curación; puede desaparecer como un monstruo normal. */
    public void orphan() {
        orphan = true;
        node = null;
        striking = false;
    }

    public boolean isStriking() {
        return striking;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return hiveId != null && !orphan;
    }

    private void resolveNode() {
        if (hiveId == null || orphan) {
            return;
        }
        if (node != null && (node.isRemoved() || !node.isAlive())) {
            node = null;
        }
        if (node == null && tickCount % 20 == 0 && level() instanceof ServerLevel server) {
            Entity e = server.getEntity(hiveId);
            if (e instanceof HiveNodeEntity n && n.isAlive()) {
                node = n;
                lostTicks = 0;
            } else if ((lostTicks += 20) > 600) {
                orphan();
            }
        }
    }

    // ------------------------------------------------------------------ cerebro (servidor)

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        resolveNode();
        if (node != null) {
            node.mind().checkIn(this);
        }
        if (tickCount % 12 == 0) {
            jinkSign = random.nextBoolean() ? 1 : -1;
        }
        LivingEntity target = currentTarget();
        Vec3 desired = computeDesired(target);
        double speed = getRole().speed * (striking ? 1.35D : 1.0D) * (retreating ? 0.8D : 1.0D);
        steer(desired, speed);
        face(target);
        if (striking && target != null && closeTo(target)) {
            sting(target);
        }
    }

    @Nullable
    private LivingEntity currentTarget() {
        if (node != null) {
            HiveMind m = node.mind();
            return m.state() == HiveMind.State.PATROL ? null : m.target();
        }
        // huérfana: persigue torpemente al jugador más cercano
        if (tickCount % 20 == 0) {
            Player p = level().getNearestPlayer(this, 12.0D);
            setTarget(p != null && !p.isCreative() && !p.isSpectator() ? p : null);
        }
        return getTarget();
    }

    private Vec3 computeDesired(@Nullable LivingEntity t) {
        if (node == null) {
            if (t != null) {
                striking = recoverTicks <= 0;
                if (recoverTicks > 0) {
                    recoverTicks--;
                }
                return t.position().add(0, t.getBbHeight() * 0.6D, 0);
            }
            striking = false;
            if (wanderPoint == null || tickCount % 100 == 0 || position().distanceToSqr(wanderPoint) < 2) {
                wanderPoint = position().add(random.nextGaussian() * 6, random.nextGaussian() * 2, random.nextGaussian() * 6);
            }
            return wanderPoint;
        }

        HiveMind mind = node.mind();
        WaspRole role = getRole();
        int slot = mind.slotOf(this);
        Vec3 home = node.position();
        float time = tickCount;

        // retirada a curarse junto al Nodo
        if (getHealth() < getMaxHealth() * 0.35F) {
            retreating = true;
        } else if (retreating && getHealth() > getMaxHealth() * 0.8F) {
            retreating = false;
        }
        if (retreating) {
            striking = false;
            double a = phase + time * 0.05D;
            return home.add(Math.cos(a) * 1.6D, 1.2D, Math.sin(a) * 1.6D);
        }

        HiveMind.State state = mind.state();
        if (state == HiveMind.State.PATROL || t == null) {
            striking = false;
            if (role == WaspRole.SCOUT && tickCount % 10 == 0) {
                scout(mind);
            }
            double r = role == WaspRole.SCOUT ? 13 + (slot % 3) * 3 : role == WaspRole.GUARD ? 2.6 : 5 + (slot % 2) * 1.5;
            double a = phase + time * (role == WaspRole.SCOUT ? 0.012D : 0.03D) * (slot % 2 == 0 ? 1 : -1);
            double y = role == WaspRole.SCOUT ? 5 + Math.sin(time * 0.03D + phase) * 1.5D : 1.5D + Math.sin(time * 0.05D + phase) * 0.7D;
            return home.add(Math.cos(a) * r, y, Math.sin(a) * r);
        }

        Vec3 toTarget = t.position().subtract(home);
        Vec3 dirT = toTarget.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : toTarget.normalize();
        if (state == HiveMind.State.ALERT) {
            striking = false;
            if (role == WaspRole.SCOUT) {
                // se planta delante del intruso, a la vista, y zumba de aviso
                if (random.nextInt(50) == 0) {
                    playSound(ModSounds.SWARM_WASP_ALARM.get(), 1.0F, 1.0F + random.nextFloat() * 0.2F);
                }
                Vec3 fromT = position().subtract(t.position());
                fromT = fromT.lengthSqr() < 1e-4 ? dirT.reverse() : fromT.normalize();
                return t.getEyePosition().add(fromT.scale(6)).add(0, 1, 0);
            }
            Vec3 side = new Vec3(-dirT.z, 0, dirT.x).scale(((slot % 5) - 2) * 1.5D);
            double dist = role == WaspRole.GUARD ? 2.0D : 6.0D;
            return home.add(dirT.scale(dist)).add(side).add(0, 2, 0);
        }

        // ---- ATAQUE
        if (role == WaspRole.GUARD && t.distanceToSqr(node) > 8 * 8) {
            striking = false;                                         // las escoltas no se alejan del Nodo
            return home.add(dirT.scale(3)).add(0, 1.5D, 0);
        }
        boolean rangedAdapted = mind.rangedBias() > 0.25F;
        boolean meleeAdapted = mind.rangedBias() < -0.25F;
        int group = Math.max(3, mind.members().size());
        double r = rangedAdapted ? 7.5D : 5.0D;
        double a = mind.orbitAngle() + slot * (Mth.TWO_PI / group) + (role == WaspRole.SCOUT ? Math.PI / 3 : 0);
        Vec3 orbit = t.position().add(Math.cos(a) * r, t.getBbHeight() * 0.7D + 1.0D + Math.sin(time * 0.1D + phase), Math.sin(a) * r);
        boolean shielded = t.isBlocking() && inFrontOf(t);
        Vec3 desired;

        if (recoverTicks > 0) {
            recoverTicks--;
            desired = orbit;
        } else if (striking) {
            if (++strikeTicks > 60) {                                 // no lo alcanzó: vuelve a orbitar
                striking = false;
                recoverTicks = 30;
            }
            desired = shielded ? behind(t) : t.position().add(0, t.getBbHeight() * 0.6D, 0);
        } else {
            boolean canStrike = !shielded && (!meleeAdapted || !lookedAtBy(t, 0.3D));
            if (canStrike && distanceToSqr(orbit) < 9 && mind.requestStrike(this)) {
                striking = true;
                strikeTicks = 0;
                playSound(ModSounds.SWARM_WASP_ALARM.get(), 0.8F, 1.6F);
            }
            desired = shielded ? behind(t) : orbit;
        }
        // si el jugador la apunta, esquiva de lado (más fuerte si aprendió que le disparan de lejos)
        if (t instanceof Player && lookedAtBy(t, 0.985D)) {
            Vec3 look = t.getViewVector(1.0F);
            Vec3 side = new Vec3(-look.z, 0, look.x).normalize().scale(jinkSign * (rangedAdapted ? 3.0D : 2.0D));
            desired = desired.add(side).add(0, jinkSign * 0.6D, 0);
        }
        return desired;
    }

    /** Las exploradoras detectan intrusos con línea de visión y dan la alarma a la colmena. */
    private void scout(HiveMind mind) {
        Player p = level().getNearestPlayer(this, 24.0D);
        if (p != null && !p.isCreative() && !p.isSpectator() && hasLineOfSight(p)) {
            mind.report(p);
            playSound(ModSounds.SWARM_WASP_ALARM.get(), 1.2F, 1.0F);
        }
    }

    // ------------------------------------------------------------------ vuelo

    private void steer(Vec3 desired, double speed) {
        Vec3 pos = position();
        Vec3 to = desired.subtract(pos);
        double dist = to.length();
        Vec3 want = dist > 1e-3 ? to.scale(Math.min(speed, dist * 0.25D + 0.04D) / dist) : Vec3.ZERO;
        // separación: no amontonarse
        if (node != null) {
            for (SwarmWaspEntity o : node.mind().members()) {
                if (o == this) {
                    continue;
                }
                Vec3 away = pos.subtract(o.position());
                double d2 = away.lengthSqr();
                if (d2 < 2.5D && d2 > 1e-4) {
                    want = want.add(away.scale(0.06D / d2));
                }
            }
        }
        // esquivar obstáculos: si delante hay un bloque sólido, subir
        Vec3 vel = getDeltaMovement();
        if (vel.lengthSqr() > 0.0025D) {
            Vec3 probe = pos.add(0, getBbHeight() * 0.5D, 0).add(vel.normalize().scale(1.4D));
            BlockPos bp = BlockPos.containing(probe);
            if (!level().getBlockState(bp).getCollisionShape(level(), bp).isEmpty()) {
                want = want.add(0, speed * 0.9D, 0);
            }
        }
        // no rozar el suelo salvo al atacar
        BlockPos below = BlockPos.containing(pos.x, pos.y - 1.0D, pos.z);
        if (!striking && !level().getBlockState(below).getCollisionShape(level(), below).isEmpty()) {
            want = want.add(0, 0.04D, 0);
        }
        setDeltaMovement(vel.add(want.subtract(vel).scale(0.2D)));
    }

    @Override
    public void travel(Vec3 input) {
        move(MoverType.SELF, getDeltaMovement());
    }

    private void face(@Nullable LivingEntity t) {
        Vec3 dir = t != null && (striking || getRole() == WaspRole.SCOUT) ? t.getEyePosition().subtract(getEyePosition())
                : getDeltaMovement();
        if (dir.horizontalDistanceSqr() > 1e-4) {
            float yaw = (float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90.0F;
            setYRot(yaw);
            yBodyRot = yaw;
            yHeadRot = yaw;
        }
    }

    // ------------------------------------------------------------------ ataque

    private boolean closeTo(LivingEntity t) {
        return getBoundingBox().inflate(0.35D).intersects(t.getBoundingBox());
    }

    private void sting(LivingEntity t) {
        if (doHurtTarget(t)) {
            if (getRole() == WaspRole.STINGER && level().getDifficulty() != Difficulty.EASY) {
                t.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), this);
            }
            playSound(ModSounds.SWARM_WASP_STING.get(), 1.0F, 0.9F + random.nextFloat() * 0.2F);
        }
        striking = false;
        recoverTicks = 25 + random.nextInt(20);
        Vec3 away = position().subtract(t.position()).normalize().scale(0.5D);
        setDeltaMovement(away.add(0, 0.25D, 0));
    }

    /** ¿El objetivo la está mirando/apuntando? (umbral = coseno del ángulo). */
    private boolean lookedAtBy(LivingEntity t, double threshold) {
        Vec3 to = position().add(0, getBbHeight() * 0.5D, 0).subtract(t.getEyePosition());
        return to.lengthSqr() > 1e-4 && t.getViewVector(1.0F).dot(to.normalize()) > threshold;
    }

    private boolean inFrontOf(LivingEntity t) {
        Vec3 look = t.getViewVector(1.0F);
        Vec3 to = position().subtract(t.position());
        return new Vec3(look.x, 0, look.z).dot(new Vec3(to.x, 0, to.z)) > 0.0D;
    }

    /** Punto a la espalda del objetivo (para rodear un escudo). */
    private static Vec3 behind(LivingEntity t) {
        Vec3 look = t.getViewVector(1.0F);
        Vec3 h = new Vec3(look.x, 0, look.z);
        h = h.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : h.normalize();
        return t.position().subtract(h.scale(2.5D)).add(0, 1.2D, 0);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() instanceof LivingEntity attacker) {
            if (node != null) {
                node.mind().provoked(attacker, attacker.distanceTo(this));
            } else {
                setTarget(attacker);
            }
        }
        return hurt;
    }

    @Override
    public boolean causeFallDamage(float distance, float mult, DamageSource source) {
        return false;
    }

    // ------------------------------------------------------------------ sonidos

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SWARM_WASP_BUZZ.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 30;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SWARM_WASP_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SWARM_WASP_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.7F;
    }

    // ------------------------------------------------------------------ guardado

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Role", getRole().ordinal());
        tag.putBoolean("Orphan", orphan);
        if (hiveId != null) {
            tag.putUUID("Hive", hiveId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ROLE, tag.getInt("Role"));
        orphan = tag.getBoolean("Orphan");
        if (tag.hasUUID("Hive")) {
            hiveId = tag.getUUID("Hive");
        }
    }
}
