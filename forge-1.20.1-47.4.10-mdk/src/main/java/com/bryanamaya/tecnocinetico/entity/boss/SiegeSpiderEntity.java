package com.bryanamaya.tecnocinetico.entity.boss;

import com.bryanamaya.tecnocinetico.block.entity.CoreConsoleBlockEntity;
import com.bryanamaya.tecnocinetico.effect.ModEffects;
import com.bryanamaya.tecnocinetico.entity.ModEntities;
import com.bryanamaya.tecnocinetico.entity.custom.AnchorEntity;
import com.bryanamaya.tecnocinetico.entity.custom.CoreShots;
import com.bryanamaya.tecnocinetico.entity.custom.DroneEntity;
import com.bryanamaya.tecnocinetico.entity.ship.ShipMissileEntity;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
import com.bryanamaya.tecnocinetico.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Araña de Asedio: guardián de la Instalación de la IA (la IA ya no está en este mundo; esta máquina guarda lo que
 * dejó). Seis patas, torreta doble al frente, lanzamisiles en el lomo y núcleo de inercita detrás. Aparece al
 * activar la Consola del Núcleo en la arena bajo la cámara del Núcleo.
 *
 * Fases (barra de jefe):
 * 1. (100-60 %) ráfagas de la torreta, disparo cargado con línea roja (se fija al final) y pisotón.
 * 2. (60-30 %) + haz de arrastre (te atrae hacia ella; si vuelas, además Lastre de Inercia) + llama 2 drones.
 * 3. (<30 %) enfurecida: más rápida, enfriamientos x0,65 y salvas de 4 misiles guiados.
 * Si se queda sin jugadores cerca 60 s, se reinicia (la consola vuelve a abrir la arena).
 * Ayuda al jugador: cada rodilla es un punto débil. Si un jugador le acierta en una (bala, granada, o el golpe o
 * disparo que pasa junto a ella), esa pata se rompe y la araña se desploma 4 s (no ataca y recibe +50 % de daño).
 * Una vez por pata: con las 6 rotas ya no se cae más. Al morir suelta siempre el Motor Dimensional.
 */
public class SiegeSpiderEntity extends Monster {

    public static final float MAX_HEALTH = 450.0F;
    private static final EntityDataAccessor<Byte> MODE = SynchedEntityData.defineId(SiegeSpiderEntity.class, EntityDataSerializers.BYTE);
    /** Patas rotas (bit i = pata i). */
    private static final EntityDataAccessor<Byte> LEGS = SynchedEntityData.defineId(SiegeSpiderEntity.class, EntityDataSerializers.BYTE);
    /** Geometría de las patas (la misma que dibuja SiegeSpiderModel): {x del anclaje, z del anclaje, giro en Y}. */
    public static final float[][] LEG_ANCHORS = {{8, -7, 0.45F}, {8, 0, 0.0F}, {8, 7, -0.45F}, {-8, -7, -0.45F}, {-8, 0, 0.0F}, {-8, 7, 0.45F}};
    public static final float LEG_LIFT = 0.5F, MODEL_SCALE = 2.0F;
    public static final int STUN_TICKS = 80;
    private static final DustParticleOptions BEAM = new DustParticleOptions(new Vector3f(0.45F, 0.5F, 1.0F), 1.2F);
    private static final UUID ENRAGE_SPEED = UUID.fromString("5b2a1e44-7c3d-4f8e-9a61-0d3c2b1e9f77");

    private final ServerBossEvent bossBar = new ServerBossEvent(Component.translatable("entity.tecnocinetico.siege_spider"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    @Nullable
    private BlockPos console;
    private int phase = 1, lonely, stun;

    public SiegeSpiderEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 500;
        setMaxUpStep(1.5F);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ARMOR, 14.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new SiegeAttackGoal());
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(MODE, (byte) 0);
        entityData.define(LEGS, (byte) 0);
    }

    public boolean legBroken(int i) {
        return (entityData.get(LEGS) & (1 << i)) != 0;
    }

    public boolean stunned() {
        return mode() == 4;
    }

    /** Posición en el mundo de la rodilla de la pata i (misma geometría y giro que el modelo x2). */
    public Vec3 knee(int i) {
        float[] l = LEG_ANCHORS[i];
        float sign = l[0] > 0 ? 1 : -1;
        double reach = 14 * Math.cos(LEG_LIFT);
        double mx = l[0] + sign * reach * Math.cos(l[2]);
        double mz = l[1] - sign * reach * Math.sin(l[2]);
        double my = 6 - 14 * Math.sin(LEG_LIFT) + (stunned() ? 6 : 0);
        double k = MODEL_SCALE / 16.0;
        double lx = -mx * k, lz = mz * k, h = (24 - my) * k;
        double th = Math.toRadians(180.0F - yBodyRot);
        return position().add(lx * Math.cos(th) + lz * Math.sin(th), h, -lx * Math.sin(th) + lz * Math.cos(th));
    }

    /** Pata cuya rodilla recibió el impacto (o -1). */
    private int kneeHit(DamageSource source) {
        net.minecraft.world.entity.Entity direct = source.getDirectEntity(), attacker = source.getEntity();
        if (!(attacker instanceof Player)) {
            return -1;
        }
        Vec3 impact = direct != null && direct != attacker ? direct.position() : null;
        Vec3 eye = ((Player) attacker).getEyePosition(), look = ((Player) attacker).getLookAngle();
        int best = -1;
        double bestD = 1.2;
        for (int i = 0; i < 6; i++) {
            if (legBroken(i)) {
                continue;
            }
            Vec3 kp = knee(i);
            double d;
            if (impact != null) {
                d = impact.distanceTo(kp);
            } else {
                double t = Math.max(0, kp.subtract(eye).dot(look));
                d = eye.add(look.scale(t)).distanceTo(kp);                       // distancia de la rodilla a la línea de tiro
            }
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) {
            return super.hurt(source, amount);
        }
        if (stun > 0) {
            amount *= 1.5F;                                                       // tumbada: más vulnerable
        }
        int leg = kneeHit(source);
        boolean hurt = super.hurt(source, amount);
        if (hurt && leg >= 0 && isAlive()) {
            entityData.set(LEGS, (byte) (entityData.get(LEGS) | (1 << leg)));
            stun = STUN_TICKS;
            setMode(4);
            getNavigation().stop();
            if (level() instanceof ServerLevel server) {
                Vec3 kp = knee(leg);
                server.sendParticles(ParticleTypes.EXPLOSION, kp.x, kp.y, kp.z, 1, 0, 0, 0, 0);
                server.sendParticles(ModParticles.KINETIC_SPARK.get(), kp.x, kp.y, kp.z, 30, 0.3, 0.3, 0.3, 0.3);
            }
            playSound(SoundEvents.IRON_GOLEM_DAMAGE, 3.0F, 0.5F);
            playSound(ModSounds.SIEGE_ROAR.get(), 3.0F, 1.4F);
        }
        return hurt;
    }

    /** 0 nada, 1 cargando disparo, 2 preparando pisotón, 3 cargando haz de arrastre, 4 desplomada (pata rota). */
    public int mode() {
        return entityData.get(MODE);
    }

    private void setMode(int m) {
        entityData.set(MODE, (byte) m);
    }

    public int phase() {
        return phase;
    }

    public void setConsole(@Nullable BlockPos pos) {
        this.console = pos;
    }

    // ------------------------------------------------------------------ jefe

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && tickCount % 4 == 0) {                         // patas rotas: chispas en la rodilla
            for (int i = 0; i < 6; i++) {
                if (legBroken(i)) {
                    Vec3 kp = knee(i);
                    level().addParticle(ParticleTypes.ELECTRIC_SPARK, kp.x, kp.y, kp.z, 0, -0.05, 0);
                }
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (stun > 0 && --stun == 0) {
            setMode(0);
            playSound(ModSounds.SIEGE_STEP.get(), 3.0F, 0.7F);                   // se levanta
        }
        float frac = getHealth() / getMaxHealth();
        bossBar.setProgress(frac);
        int want = frac > 0.6F ? 1 : frac > 0.3F ? 2 : 3;
        if (want > phase) {
            phase = want;
            playSound(ModSounds.SIEGE_ROAR.get(), 4.0F, phase == 3 ? 0.8F : 1.0F);
            bossBar.setColor(phase == 3 ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.PINK);
            if (phase == 3) {
                var speed = getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null && speed.getModifier(ENRAGE_SPEED) == null) {
                    speed.addPermanentModifier(new AttributeModifier(ENRAGE_SPEED, "Enfurecida", 0.3D, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }
        }
        // sin jugadores cerca un minuto: se reinicia el combate
        Player near = level().getNearestPlayer(this, 40.0D);
        if (near == null || near.isCreative() || near.isSpectator()) {
            if (++lonely > 1200) {
                resetFight();
            }
        } else {
            lonely = 0;
        }
    }

    private void resetFight() {
        if (console != null && level().getBlockEntity(console) instanceof CoreConsoleBlockEntity c) {
            c.reset();
        }
        discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 2, getZ(), 3, 1.5, 1, 1.5, 0);
            server.sendParticles(ModParticles.KINETIC_SPARK.get(), getX(), getY() + 2, getZ(), 120, 2.5, 1.5, 2.5, 0.5);
            playSound(SoundEvents.GENERIC_EXPLODE, 4.0F, 0.6F);
            if (console != null && server.getBlockEntity(console) instanceof CoreConsoleBlockEntity c) {
                c.defeated();
            }
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float mult, DamageSource source) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect() != ModEffects.INERTIA_ANCHOR.get() && effect.getEffect() != MobEffects.MOVEMENT_SLOWDOWN
                && super.canBeAffected(effect);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean canRide(net.minecraft.world.entity.Entity vehicle) {
        return false;
    }

    // ------------------------------------------------------------------ ataques

    private Vec3 muzzle() {
        Vec3 fwd = Vec3.directionFromRotation(0, getYHeadRot());
        return position().add(0, 2.4, 0).add(fwd.scale(3.2));
    }

    private float cdScale() {
        return phase == 3 ? 0.65F : 1.0F;
    }

    class SiegeAttackGoal extends Goal {
        private int burstLeft, burstTimer, charge, stomp, pull;
        private int gunCd = 30, chargeCd = 120, stompCd = 40, pullCd = 160, droneCd = 200, missileCd = 160;
        private Vec3 lockPoint = Vec3.ZERO;

        SiegeAttackGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public void stop() {
            charge = stomp = pull = burstLeft = 0;
            setMode(0);
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
            if (stun > 0) {                                                       // desplomada: ni se mueve ni ataca
                getNavigation().stop();
                charge = stomp = pull = burstLeft = 0;
                return;
            }
            gunCd--;
            chargeCd--;
            stompCd--;
            pullCd--;
            droneCd--;
            missileCd--;
            getLookControl().setLookAt(t, 15.0F, 30.0F);
            double dist = distanceTo(t);
            boolean see = getSensing().hasLineOfSight(t);
            Vec3 from = muzzle();

            // ---- acciones en curso (quieta mientras carga)
            if (stomp > 0) {
                getNavigation().stop();
                if (++stomp >= 16) {
                    stompNow();
                    stomp = 0;
                    stompCd = (int) (90 * cdScale());
                    setMode(0);
                }
                return;
            }
            if (charge > 0) {
                getNavigation().stop();
                charge++;
                if (charge <= 22) {
                    lockPoint = CoreShots.aimPoint(t);
                }
                if (charge % 2 == 0) {
                    CoreShots.laser(SiegeSpiderEntity.this, from, lockPoint.add(lockPoint.subtract(from).normalize().scale(4)));
                }
                if (charge >= 30) {
                    for (int k = 0; k < 2; k++) {
                        CoreShots.fire(SiegeSpiderEntity.this, from, lockPoint.subtract(from), 48, 18.0F, 0.0, SiegeSpiderEntity.class, true);
                    }
                    playSound(ModSounds.PISTOL_FIRE.get(), 3.0F, 0.4F);
                    charge = 0;
                    chargeCd = (int) (160 * cdScale());
                    setMode(0);
                }
                return;
            }
            if (pull > 0) {
                getNavigation().stop();
                pull++;
                if (pull % 2 == 0 && level() instanceof ServerLevel server) {
                    Vec3 to = t.position().add(0, t.getBbHeight() * 0.5, 0), path = to.subtract(from);
                    for (int i = 0, n = (int) (path.length() * 2); i <= n; i++) {
                        Vec3 p = from.add(path.scale(i / (double) Math.max(1, n)));
                        server.sendParticles(BEAM, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                    }
                }
                if (pull >= 22) {
                    if (hasLineOfSight(t)) {
                        Vec3 toward = position().subtract(t.position()).multiply(1, 0, 1).normalize();
                        t.setDeltaMovement(toward.scale(1.8).add(0, 0.45, 0));
                        t.hurtMarked = true;
                        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), SiegeSpiderEntity.this);
                        if (AnchorEntity.airborne(t)) {
                            t.addEffect(new MobEffectInstance(ModEffects.INERTIA_ANCHOR.get(), 60, 0), SiegeSpiderEntity.this);
                        }
                        playSound(ModSounds.ANCHOR_PULL.get(), 3.0F, 0.7F);
                        stompCd = Math.min(stompCd, 10);                       // combo: te atrae y te pisa
                    }
                    pull = 0;
                    pullCd = (int) (200 * cdScale());
                    setMode(0);
                }
                return;
            }
            if (burstLeft > 0) {
                if (--burstTimer <= 0) {
                    CoreShots.fire(SiegeSpiderEntity.this, from, CoreShots.aimPoint(t).subtract(from), 48, 4.0F, 3.5,
                            SiegeSpiderEntity.class, false);
                    playSound(ModSounds.PISTOL_FIRE.get(), 1.6F, 1.1F + random.nextFloat() * 0.2F);
                    burstLeft--;
                    burstTimer = 2;
                }
            }

            // ---- elegir acción
            if (stompCd <= 0 && dist < 6.5 && onGround()) {
                stomp = 1;
                setMode(2);
                playSound(ModSounds.SIEGE_ROAR.get(), 2.0F, 1.6F);
                return;
            }
            if (phase >= 2 && pullCd <= 0 && see && dist > 7 && dist < 28) {
                pull = 1;
                setMode(3);
                playSound(ModSounds.ANCHOR_CHARGE.get(), 2.5F, 0.7F);
                return;
            }
            if (chargeCd <= 0 && see && dist < 40) {
                charge = 1;
                lockPoint = CoreShots.aimPoint(t);
                setMode(1);
                playSound(ModSounds.SENTRY_TURRET_CHARGE.get(), 2.5F, 0.6F);
                return;
            }
            if (gunCd <= 0 && see && burstLeft <= 0 && dist < 40) {
                burstLeft = phase == 3 ? 10 : 7;
                burstTimer = 0;
                gunCd = (int) (45 * cdScale());
            }
            if (phase >= 2 && droneCd <= 0) {
                summonDrones(t);
                droneCd = (int) (500 * cdScale());
            }
            if (phase >= 3 && missileCd <= 0 && see) {
                for (int k = 0; k < 4; k++) {
                    ShipMissileEntity m = new ShipMissileEntity(level(), SiegeSpiderEntity.this, t);
                    m.setPos(getX() + (random.nextDouble() - 0.5) * 2, getY() + 3.6, getZ() + (random.nextDouble() - 0.5) * 2);
                    m.setDeltaMovement((random.nextDouble() - 0.5) * 0.6, 0.9 + random.nextDouble() * 0.3, (random.nextDouble() - 0.5) * 0.6);
                    level().addFreshEntity(m);
                }
                playSound(ModSounds.SHIP_MISSILE.get(), 3.0F, 0.8F);
                missileCd = (int) (200 * cdScale());
            }
            // ---- moverse: se acerca a media distancia
            if (!see || dist > 14) {
                getNavigation().moveTo(t, 1.0D);
            } else if (dist < 5) {
                getNavigation().stop();
            } else {
                getNavigation().moveTo(t, 0.7D);
            }
        }
    }

    private void stompNow() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        double r = 7.5;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r, 2, r),
                e -> e != this && e.isAlive() && !(e instanceof DroneEntity) && e.distanceTo(this) <= r + 1)) {
            if (e.hurt(damageSources().mobAttack(this), 12.0F)) {
                Vec3 away = e.position().subtract(position()).multiply(1, 0, 1).normalize();
                e.knockback(1.6D, -away.x, -away.z);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2), this);
            }
        }
        BlockState below = level().getBlockState(blockPosition().below());
        for (int i = 0; i < 48; i++) {
            double a = i / 48.0 * Math.PI * 2;
            for (double d = 1.5; d <= r; d += 1.5) {
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), getX() + Math.cos(a) * d, getY() + 0.1,
                        getZ() + Math.sin(a) * d, 1, 0, 0.05, 0, 0.15);
            }
            server.sendParticles(BEAM, getX() + Math.cos(a) * r, getY() + 0.4, getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
        playSound(ModSounds.ANCHOR_STOMP.get(), 4.0F, 0.6F);
    }

    private void summonDrones(LivingEntity target) {
        int have = level().getEntitiesOfClass(DroneEntity.class, getBoundingBox().inflate(32)).size();
        for (int k = have; k < Math.min(4, have + 2); k++) {
            DroneEntity d = ModEntities.DRONE.get().create(level());
            if (d == null) {
                continue;
            }
            d.moveTo(getX() + (random.nextDouble() - 0.5) * 4, getY() + 4, getZ() + (random.nextDouble() - 0.5) * 4, random.nextFloat() * 360, 0);
            d.finalizeSpawn((ServerLevel) level(), level().getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            level().addFreshEntity(d);
            d.engage(target);
            ((ServerLevel) level()).sendParticles(ParticleTypes.ELECTRIC_SPARK, d.getX(), d.getY(), d.getZ(), 20, 0.4, 0.4, 0.4, 0.2);
        }
    }

    // ------------------------------------------------------------------ sonidos y guardado

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ANCHOR_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SIEGE_ROAR.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.SIEGE_STEP.get(), 1.2F, 0.9F + random.nextFloat() * 0.2F);
    }

    @Override
    public float getVoicePitch() {
        return 0.6F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (console != null) {
            tag.put("Console", NbtUtils.writeBlockPos(console));
        }
        tag.putInt("Phase", phase);
        tag.putByte("Legs", entityData.get(LEGS));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Console")) {
            console = NbtUtils.readBlockPos(tag.getCompound("Console"));
        }
        phase = Math.max(1, tag.getInt("Phase"));
        entityData.set(LEGS, tag.getByte("Legs"));
        if (hasCustomName()) {
            bossBar.setName(getDisplayName());
        }
    }
}
