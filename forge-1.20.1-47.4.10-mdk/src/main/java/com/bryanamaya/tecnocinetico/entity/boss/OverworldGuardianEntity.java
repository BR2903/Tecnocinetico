package com.bryanamaya.tecnocinetico.entity.boss;

import com.bryanamaya.tecnocinetico.block.entity.CoreConsoleBlockEntity;
import com.bryanamaya.tecnocinetico.entity.ModEntities;
import com.bryanamaya.tecnocinetico.entity.custom.CoreShots;
import com.bryanamaya.tecnocinetico.entity.custom.DroneEntity;
import com.bryanamaya.tecnocinetico.entity.custom.SwarmBodyEntity;
import com.bryanamaya.tecnocinetico.entity.swarm.HiveNodeEntity;
import com.bryanamaya.tecnocinetico.entity.swarm.SwarmWaspEntity;
import com.bryanamaya.tecnocinetico.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;

/** Guardianes locales, no la IA final. Vigía aéreo (rayos/pulsos) y Matriarca terrestre (embestidas/bombardeo). */
public class OverworldGuardianEntity extends Monster {
    private static final EntityDataAccessor<Byte> MODE = SynchedEntityData.defineId(OverworldGuardianEntity.class, EntityDataSerializers.BYTE);
    private final ServerBossEvent bar;
    @Nullable private BlockPos console;
    @Nullable private BlockPos home;
    private int cooldown = 40, charge, attacks, lonely, exposed, rushing;
    private Vec3 lock = Vec3.ZERO;

    public OverworldGuardianEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        bar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
        xpReward = 400;
        setPersistenceRequired();
        setMaxUpStep(1.5F);
        setNoGravity(isWatcher());
    }

    public boolean isWatcher() { return getType() == ModEntities.NETWORK_WATCHER.get(); }
    public int mode() { return entityData.get(MODE); }
    public int phase() { return getHealth() > getMaxHealth() * .6F ? 1 : getHealth() > getMaxHealth() * .3F ? 2 : 3; }
    public void setConsole(BlockPos p) { console = p; home = blockPosition(); }

    public static AttributeSupplier.Builder attributes(double health) {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, health).add(Attributes.ARMOR, 10)
                .add(Attributes.ATTACK_DAMAGE, 14).add(Attributes.MOVEMENT_SPEED, .29)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 40));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(MODE, (byte) 0); }
    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bar.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bar.removePlayer(p); }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean causeFallDamage(float distance, float mult, DamageSource source) { return false; }

    @Override public boolean isAlliedTo(Entity other) {
        return other instanceof OverworldGuardianEntity || other instanceof SiegeSpiderEntity || other instanceof DroneEntity
                || other instanceof SwarmBodyEntity || other instanceof SwarmWaspEntity || other instanceof HiveNodeEntity || super.isAlliedTo(other);
    }
    @Override public void setTarget(@Nullable LivingEntity t) {
        if (t == null || !isAlliedTo(t)) super.setTarget(t);
    }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() != null && isAlliedTo(source.getEntity())) return false;
        // El núcleo queda expuesto después del ataque: ventana para responder, sin inmunidades arbitrarias.
        return super.hurt(source, exposed > 0 ? amount * 1.5F : amount);
    }

    private boolean enemy(LivingEntity e) {
        return e != this && e.isAlive() && !isAlliedTo(e) && !(e instanceof Player p && (p.isCreative() || p.isSpectator()));
    }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel server)) return;
        if (home == null) home = blockPosition();
        bar.setProgress(getHealth() / getMaxHealth());
        bar.setColor(phase() == 3 ? BossEvent.BossBarColor.RED : isWatcher() ? BossEvent.BossBarColor.BLUE : BossEvent.BossBarColor.GREEN);
        boolean players = level().players().stream().anyMatch(p -> p.isAlive() && !p.isCreative() && !p.isSpectator()
                && p.distanceToSqr(Vec3.atCenterOf(home)) < 48 * 48);
        lonely = players ? 0 : lonely + 1;
        if (lonely > 1200) { discard(); return; }
        LivingEntity t = getTarget();
        if (t != null && (!enemy(t) || t.distanceToSqr(Vec3.atCenterOf(home)) > 40 * 40)) {
            setTarget(null); t = null;
        }
        if (exposed > 0) {
            exposed--; entityData.set(MODE, (byte) 3);
            if (exposed == 0) entityData.set(MODE, (byte) 0);
        }
        if (t == null) {
            charge = 0; rushing = 0;
            entityData.set(MODE, (byte) 0);
            getNavigation().stop();
            if (isWatcher()) setDeltaMovement(Vec3.atCenterOf(home).subtract(position()).scale(.035));
            return;
        }
        getLookControl().setLookAt(t, 25, 25);
        boolean special = attacks % 3 == 2;
        if (rushing > 0) {
            rushing--;
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(.8), this::enemy))
                e.hurt(damageSources().mobAttack(this), 14 + phase() * 2);
            if (rushing == 0 || horizontalCollision) {
                rushing = 0; setDeltaMovement(Vec3.ZERO); finishAttack();
            }
            return;
        }
        if (charge > 0) {
            charge++;
            getNavigation().stop();
            if (isWatcher()) setDeltaMovement(getDeltaMovement().scale(.7));
            if (charge <= 16) lock = CoreShots.aimPoint(t);
            if (!special) CoreShots.laser(this, getEyePosition(), lock);
            else if (charge % 2 == 0) ring(server, isWatcher() ? pulseCenter() : special ? lock : position(),
                    isWatcher() ? 8 : special ? 4.5 : 2, ParticleTypes.ELECTRIC_SPARK);
            if (charge >= 30) {
                if (isWatcher()) {
                    if (special) {
                        // Pulso: castiga estar cerca; la zona se avisa antes de descargar.
                        ring(server, pulseCenter(), 8, ParticleTypes.EXPLOSION);
                        areaDamage(pulseCenter(), 8, 12 + phase() * 2);
                    } else CoreShots.fire(this, getEyePosition(), lock.subtract(getEyePosition()), 44, 14 + phase() * 2, 0, OverworldGuardianEntity.class, true);
                    finishAttack();
                } else if (special) {
                    ring(server, lock, 4.5, ParticleTypes.EXPLOSION);
                    areaDamage(lock, 4.5, 18 + phase() * 2);
                    finishAttack();
                } else {
                    Vec3 dir = lock.subtract(position()).multiply(1, 0, 1).normalize();
                    setDeltaMovement(dir.scale(1.15).add(0, .1, 0));
                    rushing = 12; charge = 0;
                }
                playSound(ModSounds.ANCHOR_STOMP.get(), 2.5F, isWatcher() ? 1.4F : .65F);
            }
            return;
        }
        if (cooldown > 0) cooldown--;
        if (isWatcher()) {
            Vec3 anchor = Vec3.atCenterOf(home);
            double angle = tickCount * .025;
            Vec3 want = anchor.add(Math.cos(angle) * 8, 2 + Math.sin(tickCount * .06), Math.sin(angle) * 8);
            setDeltaMovement(getDeltaMovement().lerp(want.subtract(position()).scale(.06), .15));
        } else if (distanceTo(t) > 6) getNavigation().moveTo(t, 1.0);
        else getNavigation().stop();
        if (cooldown <= 0 && hasLineOfSight(t) && (isWatcher() || distanceTo(t) < 20)) {
            charge = 1; lock = CoreShots.aimPoint(t);
            entityData.set(MODE, (byte) (special ? 2 : 1));
            playSound(ModSounds.SENTRY_TURRET_CHARGE.get(), 2, isWatcher() ? 1 : .65F);
        }
    }

    private void finishAttack() {
        charge = 0; attacks++; exposed = 24;
        cooldown = phase() == 3 ? 38 : phase() == 2 ? 52 : 70;
        entityData.set(MODE, (byte) 3);
    }
    private Vec3 pulseCenter() { return new Vec3(getX(), home.getY() - 4, getZ()); }
    private void areaDamage(Vec3 center, double radius, float damage) {
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius), this::enemy)) {
            if (e.distanceToSqr(center) < radius * radius && hasLineOfSight(e)) e.hurt(damageSources().mobAttack(this), damage);
        }
    }
    private void ring(ServerLevel server, Vec3 center, double r, net.minecraft.core.particles.SimpleParticleType particle) {
        for (int i = 0; i < 40; i++) {
            double a = i * Math.PI / 20;
            server.sendParticles(particle, center.x + Math.cos(a) * r, center.y + .1, center.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }
    @Override public void travel(Vec3 input) {
        if (isWatcher()) move(MoverType.SELF, getDeltaMovement()); else super.travel(input);
    }
    @Override public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide && console != null && level().getBlockEntity(console) instanceof CoreConsoleBlockEntity c) c.defeated();
    }
    @Override public void remove(RemovalReason reason) {
        if (reason == RemovalReason.DISCARDED && !level().isClientSide && console != null
                && level().getBlockEntity(console) instanceof CoreConsoleBlockEntity c && c.isActive()) c.reset();
        super.remove(reason);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (console != null) tag.put("Console", NbtUtils.writeBlockPos(console));
        if (home != null) tag.put("Home", NbtUtils.writeBlockPos(home));
        tag.putInt("Attacks", attacks);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Console")) console = NbtUtils.readBlockPos(tag.getCompound("Console"));
        if (tag.contains("Home")) home = NbtUtils.readBlockPos(tag.getCompound("Home"));
        attacks = tag.getInt("Attacks");
        bar.setName(getDisplayName());
    }
}
