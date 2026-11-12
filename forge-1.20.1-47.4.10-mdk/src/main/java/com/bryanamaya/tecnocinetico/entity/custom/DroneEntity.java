package com.bryanamaya.tecnocinetico.entity.custom;

import com.bryanamaya.tecnocinetico.entity.boss.SiegeSpiderEntity;
import com.bryanamaya.tecnocinetico.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * Dron del Núcleo Directriz. Vuela con "steering" (sin búsqueda de rutas, como las avispas): patrulla en círculos
 * sobre su zona barriendo el suelo con un haz de escaneo; si ve a un jugador da la alarma (y avisa a los drones
 * cercanos), lo orbita a distancia, esquiva cuando le apuntan y dispara ráfagas de 3 tiros tras medio segundo de
 * carga con línea roja de aviso. Si pierde al objetivo un rato, vuelve a patrullar.
 */
public class DroneEntity extends Monster {

    /** 0 = patrulla (ojo cian), 1 = ataque (ojo rojo), 2 = cargando la ráfaga (rojo parpadeante). */
    private static final EntityDataAccessor<Byte> MODE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.BYTE);
    private static final DustParticleOptions SCAN = new DustParticleOptions(new Vector3f(0.3F, 0.9F, 1.0F), 0.5F);

    private static final double SIGHT = 24.0D, ALERT_RADIUS = 32.0D, ORBIT = 9.0D;
    // ráfaga: 6 tiros de 2 cada 2 ticks; cargado: 1 s de carga, se fija 0,35 s antes y hace 10 de daño
    private static final int BURST = 6, BURST_GAP = 2, AFTER_BURST = 14, CHARGE = 20, LOCK = 7, AFTER_HEAVY = 20;
    private static final float BURST_DAMAGE = 2.0F, HEAVY_DAMAGE = 10.0F;
    private static final double BURST_SPREAD = 4.0D;

    @Nullable
    private BlockPos home;
    @Nullable
    private Vec3 waypoint;
    private int charge, burstLeft, burstTimer, bursts, cooldown = 15, lostTicks;
    private Vec3 lockPoint = Vec3.ZERO;
    // ---- patrulla nocturna (entity/custom/NightPatrols): cruza la zona en grupo y se va al amanecer
    /** Dirección horizontal de la patrulla (null = dron normal que vigila su sitio). */
    @Nullable
    private Vec3 patrolHeading;
    private int patrolTicks;
    private boolean leaving;
    /** Velocidad a la que avanza el punto de patrulla (bloques/tick). Duración máxima sin encontrar a nadie. */
    public static final double PATROL_SPEED = 0.12D;
    public static final int PATROL_LIFETIME = 3600, LEAVE_TICKS = 200;
    private int jinkSign = 1;
    private final float phase;

    public DroneEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
        this.xpReward = 10;
        this.phase = random.nextFloat() * Mth.TWO_PI;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FLYING_SPEED, 0.5D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected void registerGoals() {
        // sin objetivos vanilla: todo en customServerAiStep
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

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        home = blockPosition();
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    /** Convierte el dron en patrulla nocturna que avanza hacia `heading` (horizontal). */
    public void startPatrol(Vec3 heading) {
        Vec3 h = new Vec3(heading.x, 0, heading.z);
        patrolHeading = h.lengthSqr() > 1e-6 ? h.normalize() : new Vec3(1, 0, 0);
        patrolTicks = 0;
        leaving = false;
        home = blockPosition();
    }

    public boolean isNightPatrol() {
        return patrolHeading != null;
    }

    public boolean isLeaving() {
        return leaving;
    }

    /** Las patrullas desaparecen lejos de los jugadores; los drones de estructuras se quedan. */
    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
        return isNightPatrol() || super.removeWhenFarAway(distanceSqr);
    }

    /** Fija un objetivo (pruebas automáticas o alarma de otro dron). */
    public void engage(LivingEntity t) {
        if (isAlliedTo(t)) {
            return;
        }
        if (getTarget() == null) {
            playSound(ModSounds.DRONE_ALERT.get(), 1.4F, 1.0F);
        }
        setTarget(t);
        setMode(1);
        lostTicks = 0;
    }

    // ------------------------------------------------------------------ cerebro

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (home == null) {
            home = blockPosition();
        }
        if (cooldown > 0) {
            cooldown--;
        }
        if (tickCount % 15 == 0) {
            jinkSign = random.nextBoolean() ? 1 : -1;
        }
        LivingEntity t = getTarget();
        if (t != null && (!t.isAlive() || t.distanceToSqr(this) > 48 * 48
                || (t instanceof Player p && (p.isCreative() || p.isSpectator())))) {
            t = null;
        }
        if (t != null && !hasLineOfSight(t) && ++lostTicks > 200) {
            t = null;                                                          // lo perdió: vuelve a patrullar
        }
        if (t == null) {
            if (getTarget() != null) {
                setTarget(null);
            }
            setMode(0);
            charge = 0;
            burstLeft = 0;
            if (isNightPatrol() && patrolStep()) {
                return;                                                        // se está yendo
            }
            patrol();
            if (tickCount % 10 == 0) {
                scanForPlayers();
            }
            return;
        }
        if (hasLineOfSight(t)) {
            lostTicks = 0;
        }
        attack(t);
    }

    /**
     * Patrulla nocturna sin objetivo: el punto de patrulla avanza en línea (el grupo cruza la zona) y al amanecer o
     * pasado {@link #PATROL_LIFETIME} sube y se va. Devuelve true mientras se está yendo (no patrulla).
     */
    private boolean patrolStep() {
        patrolTicks++;
        if (!leaving && (level().isDay() || patrolTicks > PATROL_LIFETIME)) {
            leaving = true;
            patrolTicks = 0;
        }
        if (leaving) {
            steer(position().add(patrolHeading.scale(4)).add(0, 3, 0), 0.3D);   // sube y se aleja
            faceTowards(getDeltaMovement());
            if (patrolTicks > LEAVE_TICKS) {
                discard();
            }
            return true;
        }
        if (patrolTicks % 20 == 0) {
            home = BlockPos.containing(Vec3.atCenterOf(home).add(patrolHeading.scale(PATROL_SPEED * 20)));
        }
        return false;
    }

    private void patrol() {
        if (waypoint == null || position().distanceToSqr(waypoint) < 4 || tickCount % 120 == 0) {
            double a = phase + tickCount * 0.01 + random.nextDouble() * 1.5;
            double r = isNightPatrol() ? 2 + random.nextDouble() * 4 : 6 + random.nextDouble() * 10;   // en patrulla, en formación
            double x = home.getX() + 0.5 + Math.cos(a) * r, z = home.getZ() + 0.5 + Math.sin(a) * r;
            int ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
            waypoint = new Vec3(x, ground + (isNightPatrol() ? 10 : 7) + random.nextDouble() * 5, z);
        }
        steer(waypoint, 0.22D);
        faceTowards(getDeltaMovement());
        setXRot(getXRot() * 0.9F + 4.0F);       // en patrulla el cañón mira algo abajo
        if (tickCount % (isNightPatrol() ? 8 : 20) == 0 && level() instanceof ServerLevel server) {   // haz de escaneo hacia el suelo
            Vec3 eye = getEyePosition();
            Vec3 dir = Vec3.directionFromRotation(55.0F, getYRot());
            BlockHitResult hit = level().clip(new ClipContext(eye, eye.add(dir.scale(16)), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, this));
            Vec3 end = hit.getLocation();
            for (int i = 0; i <= 12; i++) {
                Vec3 p = eye.add(end.subtract(eye).scale(i / 12.0));
                server.sendParticles(SCAN, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
            if (tickCount % 60 == 0) {
                playSound(ModSounds.DRONE_SCAN.get(), 0.6F, 1.0F);
            }
        }
    }

    /** Ve a un jugador si está cerca, o algo más lejos pero dentro de su cono de visión, con línea de visión. */
    private void scanForPlayers() {
        Player p = level().getNearestPlayer(this, SIGHT);
        if (p == null || p.isCreative() || p.isSpectator() || !hasLineOfSight(p)) {
            return;
        }
        Vec3 to = p.position().subtract(position());
        Vec3 facing = Vec3.directionFromRotation(30.0F, getYRot());
        boolean inCone = to.normalize().dot(facing) > 0.5D;
        if (to.length() < 10.0D || inCone) {
            engage(p);
            for (DroneEntity other : level().getEntitiesOfClass(DroneEntity.class, getBoundingBox().inflate(ALERT_RADIUS),
                    d -> d != this && d.getTarget() == null)) {
                other.engage(p);                                               // alarma: avisa a los cercanos
            }
        }
    }

    private void attack(LivingEntity t) {
        // orbitar a distancia, por encima del objetivo; esquivar si le apuntan
        double a = phase + tickCount * 0.03;
        Vec3 desired = t.position().add(Math.cos(a) * ORBIT, t.getBbHeight() + 3.0 + Math.sin(tickCount * 0.07) * 0.8, Math.sin(a) * ORBIT);
        if (t instanceof Player && lookedAtBy(t)) {
            Vec3 look = t.getViewVector(1.0F);
            desired = desired.add(new Vec3(-look.z, 0, look.x).normalize().scale(jinkSign * 3.0D)).add(0, jinkSign * 0.8D, 0);
        }
        steer(desired, 0.32D);
        Vec3 aimDir = t.getEyePosition().subtract(getEyePosition());
        faceTowards(aimDir);
        setXRot((float) (-Mth.atan2(aimDir.y, aimDir.horizontalDistance()) * Mth.RAD_TO_DEG));

        // dos ataques: ráfaga rápida (flojita, casi sin espera) o disparo cargado (fuerte, castiga quedarse quieto)
        Vec3 muzzle = getEyePosition().subtract(0, 0.2, 0);
        if (burstLeft > 0) {
            if (--burstTimer <= 0) {
                CoreShots.fire(this, muzzle, CoreShots.aimPoint(t).subtract(muzzle), 32, BURST_DAMAGE, BURST_SPREAD, DroneEntity.class, false);
                playSound(ModSounds.PISTOL_FIRE.get(), 0.7F, 1.8F + random.nextFloat() * 0.2F);
                burstLeft--;
                burstTimer = BURST_GAP;
                if (burstLeft == 0) {
                    cooldown = AFTER_BURST;
                }
            }
        } else if (charge > 0) {
            charge++;
            if (charge <= CHARGE - LOCK) {
                lockPoint = CoreShots.aimPoint(t);                            // sigue al objetivo...
            }                                                                  // ...y se fija los últimos ticks
            Vec3 dir = lockPoint.subtract(muzzle).normalize();
            if (charge % 2 == 0) {
                CoreShots.laser(this, muzzle, lockPoint.add(dir.scale(4)));
            }
            if (charge >= CHARGE) {
                CoreShots.fire(this, muzzle, dir, 32, HEAVY_DAMAGE, 0.0, DroneEntity.class, true);
                playSound(ModSounds.PISTOL_FIRE.get(), 1.5F, 0.6F);
                charge = 0;
                bursts = 0;
                cooldown = AFTER_HEAVY;
                setMode(1);
            }
        } else if (cooldown <= 0 && hasLineOfSight(t) && distanceToSqr(t) < 28 * 28) {
            if (bursts >= 2 || random.nextFloat() < 0.35F) {
                charge = 1;
                lockPoint = CoreShots.aimPoint(t);
                setMode(2);
                playSound(ModSounds.SENTRY_TURRET_CHARGE.get(), 0.9F, 1.4F);
            } else {
                burstLeft = BURST;
                burstTimer = 0;
                bursts++;
            }
        }
    }

    // ------------------------------------------------------------------ vuelo

    private void steer(Vec3 desired, double speed) {
        Vec3 pos = position();
        Vec3 to = desired.subtract(pos);
        double dist = to.length();
        Vec3 want = dist > 1e-3 ? to.scale(Math.min(speed, dist * 0.2D + 0.03D) / dist) : Vec3.ZERO;
        for (DroneEntity o : level().getEntitiesOfClass(DroneEntity.class, getBoundingBox().inflate(2.0D), d -> d != this)) {
            Vec3 away = pos.subtract(o.position());
            double d2 = away.lengthSqr();
            if (d2 > 1e-4) {
                want = want.add(away.scale(0.08D / d2));                       // no chocar entre drones
            }
        }
        Vec3 vel = getDeltaMovement();
        if (vel.lengthSqr() > 0.002D) {                                         // subir ante un obstáculo
            BlockPos ahead = BlockPos.containing(pos.add(0, getBbHeight() * 0.5, 0).add(vel.normalize().scale(1.6)));
            if (!level().getBlockState(ahead).getCollisionShape(level(), ahead).isEmpty()) {
                want = want.add(0, speed, 0);
            }
        }
        int ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, getBlockX(), getBlockZ());
        if (pos.y < ground + 2.5D) {
            want = want.add(0, 0.08D, 0);                                       // no bajar del todo
        }
        setDeltaMovement(vel.add(want.subtract(vel).scale(0.15D)));
    }

    @Override
    public void travel(Vec3 input) {
        move(MoverType.SELF, getDeltaMovement());
    }

    private void faceTowards(Vec3 dir) {
        if (dir.horizontalDistanceSqr() > 1e-4) {
            float yaw = (float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90.0F;
            setYRot(Mth.approachDegrees(getYRot(), yaw, 12.0F));
            yBodyRot = getYRot();
            yHeadRot = getYRot();
        }
    }

    private boolean lookedAtBy(LivingEntity t) {
        Vec3 to = position().subtract(t.getEyePosition());
        return to.lengthSqr() > 1e-4 && t.getViewVector(1.0F).dot(to.normalize()) > 0.985D;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Incluye proyectiles y explosiones: getEntity devuelve al responsable del ataque.
        if (source.getEntity() instanceof SiegeSpiderEntity) {
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof DroneEntity)) {
            engage(attacker);
        }
        return hurt;
    }

    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity other) {
        return other instanceof SiegeSpiderEntity || super.isAlliedTo(other);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target == null || !isAlliedTo(target)) {
            super.setTarget(target);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float mult, DamageSource source) {
        return false;
    }

    // ------------------------------------------------------------------ sonidos

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.DRONE_HUM.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 40;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SWARM_WASP_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SWARM_BODY_DEATH.get();
    }

    // ------------------------------------------------------------------ guardado

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) {
            tag.put("Home", NbtUtils.writeBlockPos(home));
        }
        if (patrolHeading != null) {
            tag.putDouble("PatrolX", patrolHeading.x);
            tag.putDouble("PatrolZ", patrolHeading.z);
            tag.putInt("PatrolTicks", patrolTicks);
            tag.putBoolean("Leaving", leaving);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Home")) {
            home = NbtUtils.readBlockPos(tag.getCompound("Home"));
        }
        if (tag.contains("PatrolX")) {
            patrolHeading = new Vec3(tag.getDouble("PatrolX"), 0, tag.getDouble("PatrolZ"));
            patrolTicks = tag.getInt("PatrolTicks");
            leaving = tag.getBoolean("Leaving");
        }
    }
}
