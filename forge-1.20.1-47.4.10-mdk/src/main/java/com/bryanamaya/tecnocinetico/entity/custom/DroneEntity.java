package com.bryanamaya.tecnocinetico.entity.custom;

import com.bryanamaya.tecnocinetico.particle.ModParticles;
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
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
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
    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(1.0F, 0.15F, 0.1F), 0.6F);
    private static final DustParticleOptions SCAN = new DustParticleOptions(new Vector3f(0.3F, 0.9F, 1.0F), 0.5F);

    private static final double SIGHT = 24.0D, ALERT_RADIUS = 32.0D, ORBIT = 9.0D;
    private static final int CHARGE = 12, BURST = 3, BURST_GAP = 3, COOLDOWN = 50;

    @Nullable
    private BlockPos home;
    @Nullable
    private Vec3 waypoint;
    private int charge, burstLeft, burstTimer, cooldown = 30, lostTicks;
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

    /** Fija un objetivo (pruebas automáticas o alarma de otro dron). */
    public void engage(LivingEntity t) {
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

    private void patrol() {
        if (waypoint == null || position().distanceToSqr(waypoint) < 4 || tickCount % 120 == 0) {
            double a = phase + tickCount * 0.01 + random.nextDouble() * 1.5;
            double r = 6 + random.nextDouble() * 10;
            double x = home.getX() + 0.5 + Math.cos(a) * r, z = home.getZ() + 0.5 + Math.sin(a) * r;
            int ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
            waypoint = new Vec3(x, ground + 7 + random.nextDouble() * 5, z);
        }
        steer(waypoint, 0.22D);
        faceTowards(getDeltaMovement());
        setXRot(getXRot() * 0.9F + 4.0F);       // en patrulla el cañón mira algo abajo
        if (tickCount % 20 == 0 && level() instanceof ServerLevel server) {          // haz de escaneo hacia el suelo
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

        // ráfagas: carga (línea roja) → 3 disparos → enfriamiento
        if (burstLeft > 0) {
            if (--burstTimer <= 0) {
                fire(t);
                burstLeft--;
                burstTimer = BURST_GAP;
                if (burstLeft == 0) {
                    cooldown = COOLDOWN;
                    setMode(1);
                }
            }
        } else if (charge > 0) {
            charge++;
            if (charge % 2 == 0) {
                laser(t);
            }
            if (charge >= CHARGE) {
                charge = 0;
                burstLeft = BURST;
                burstTimer = 0;
            }
        } else if (cooldown <= 0 && hasLineOfSight(t) && distanceToSqr(t) < 28 * 28) {
            charge = 1;
            setMode(2);
            playSound(ModSounds.SENTRY_TURRET_CHARGE.get(), 0.7F, 1.6F);
        }
    }

    private void laser(LivingEntity t) {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        Vec3 from = getEyePosition(), to = t.getEyePosition().subtract(0, 0.3, 0);
        Vec3 path = to.subtract(from);
        int steps = (int) (path.length() * 1.5);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(path.scale(i / (double) Math.max(1, steps)));
            server.sendParticles(RED, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    private void fire(LivingEntity t) {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        Vec3 from = getEyePosition().subtract(0, 0.2, 0);
        Vec3 aim = t.position().add(0, t.getBbHeight() * 0.6, 0).subtract(from).normalize();
        double s = Math.toRadians(2.5);                                        // dispersión
        aim = aim.add(random.nextGaussian() * s, random.nextGaussian() * s, random.nextGaussian() * s).normalize();
        Vec3 end = from.add(aim.scale(32));
        BlockHitResult block = level().clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level(), this, from, end, new AABB(from, end).inflate(1),
                e -> e instanceof LivingEntity && !(e instanceof DroneEntity) && !e.isSpectator(), 0.2F);
        if (hit != null) {
            end = hit.getEntity().getBoundingBox().clip(from, end).orElse(hit.getEntity().getBoundingBox().getCenter());
            doHurtTarget(hit.getEntity());
        }
        Vec3 path = end.subtract(from);
        int steps = (int) (path.length() * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(path.scale(i / (double) Math.max(1, steps)));
            server.sendParticles(ModParticles.KINETIC_TRACER.get(), p.x, p.y, p.z, 0, 0, 0, 0, 0);
        }
        server.sendParticles(ModParticles.KINETIC_SPARK.get(), end.x, end.y, end.z, 4, 0.04, 0.04, 0.04, 0.15);
        playSound(ModSounds.PISTOL_FIRE.get(), 1.0F, 1.5F);
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
        boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof DroneEntity)) {
            engage(attacker);
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Home")) {
            home = NbtUtils.readBlockPos(tag.getCompound("Home"));
        }
    }
}
