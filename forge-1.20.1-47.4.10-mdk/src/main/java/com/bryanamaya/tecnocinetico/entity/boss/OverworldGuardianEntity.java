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
    private static final EntityDataAccessor<Byte> BROKEN = SynchedEntityData.defineId(OverworldGuardianEntity.class, EntityDataSerializers.BYTE);
    private final ServerBossEvent bar;
    @Nullable private BlockPos console;
    @Nullable private BlockPos home;
    private int cooldown = 40, charge, attacks, lonely, stun, rushing, volley, volleyTimer;
    private int retreatTicks, summonCooldown = 80;
    private final java.util.Set<java.util.UUID> minions = new java.util.HashSet<>();
    private final float[] capacitorDamage = new float[2];
    private final java.util.Set<java.util.UUID> rushed = new java.util.HashSet<>();
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
    public boolean capacitorBroken(int i) { return (entityData.get(BROKEN) & (1 << i)) != 0; }
    public boolean stunned() { return isWatcher() && stun > 0; }
    public boolean ownsMinion(java.util.UUID id) { return minions.contains(id); }
    public int phase() { return getHealth() > getMaxHealth() * .6F ? 1 : getHealth() > getMaxHealth() * .3F ? 2 : 3; }
    public void setConsole(BlockPos p) { console = p; home = blockPosition(); }

    public static AttributeSupplier.Builder attributes(boolean watcher) {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, watcher ? 700 : 1000).add(Attributes.ARMOR, watcher ? 18 : 22)
                .add(Attributes.ARMOR_TOUGHNESS, watcher ? 8 : 12)
                .add(Attributes.ATTACK_DAMAGE, watcher ? 22 : 28).add(Attributes.MOVEMENT_SPEED, watcher ? .3 : .36)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.FOLLOW_RANGE, 64);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 40));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
    @Override protected void defineSynchedData() {
        super.defineSynchedData(); entityData.define(MODE, (byte) 0); entityData.define(BROKEN, (byte) 0);
    }
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
        int capacitor = isWatcher() && !stunned() ? capacitorHit(source) : -1;
        boolean hurt = super.hurt(source, stunned() ? amount * 1.5F : amount);
        if (hurt && isWatcher() && !stunned() && source.getEntity() instanceof Player player && enemy(player)) {
            setTarget(player);
            if (distanceTo(player) < 6) { retreatTicks = 50; cooldown = Math.min(cooldown, 10); }
        }
        if (hurt && capacitor >= 0 && isAlive()) {
            capacitorDamage[capacitor] += amount;
            if (capacitorDamage[capacitor] >= 24) {
                entityData.set(BROKEN, (byte) (entityData.get(BROKEN) | (1 << capacitor)));
                stun = 60; charge = 0; volley = 0; rushing = 0;
                entityData.set(MODE, (byte) 3);
                cooldown = 32;
                if (level() instanceof ServerLevel server) {
                    Vec3 p = capacitorPosition(capacitor);
                    server.sendParticles(ParticleTypes.EXPLOSION, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                    server.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 24, .25, .25, .25, .1);
                }
                playSound(ModSounds.ANCHOR_STOMP.get(), 3, .8F);
            }
        }
        return hurt;
    }

    /** Centros de los dos condensadores traseros; misma escala y giro que OverworldGuardianModel. */
    public Vec3 capacitorPosition(int i) {
        double k = 1.4 / 16, x = -(i == 0 ? -9 : 9) * k, z = 12 * k;
        double a = Math.toRadians(180 - yBodyRot);
        return position().add(x * Math.cos(a) + z * Math.sin(a), 11 * k, -x * Math.sin(a) + z * Math.cos(a));
    }

    private int capacitorHit(DamageSource source) {
        if (!(source.getEntity() instanceof Player player)) return -1;
        Entity direct = source.getDirectEntity();
        Vec3 impact = direct != null && direct != player ? direct.position() : null;
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        // El blindaje frontal tapa los condensadores: hay que flanquear de verdad, no acertar a través del casco.
        Vec3 rear = capacitorPosition(0).add(capacitorPosition(1)).scale(.5).subtract(position()).multiply(1, 0, 1).normalize();
        Vec3 incoming = (impact != null ? impact : eye).subtract(position()).multiply(1, 0, 1).normalize();
        if (rear.dot(incoming) < .25) return -1;
        int best = -1;
        double distance = .48;
        for (int i = 0; i < 2; i++) {
            if (capacitorBroken(i)) continue;
            Vec3 p = capacitorPosition(i);
            double d = impact != null ? impact.distanceTo(p)
                    : eye.add(look.scale(Math.max(0, p.subtract(eye).dot(look)))).distanceTo(p);
            if (d < distance) { distance = d; best = i; }
        }
        return best;
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
        if (stun > 0) {
            stun--; entityData.set(MODE, (byte) (stun > 0 ? 3 : 0));
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().scale(.5));
            if (tickCount % 4 == 0) server.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1, getZ(), 3, .3, .3, .3, .02);
            return;
        }
        if (t == null) {
            charge = 0; rushing = 0; volley = 0;
            entityData.set(MODE, (byte) 0);
            getNavigation().stop();
            if (isWatcher()) watcherFlight(null);
            return;
        }
        getLookControl().setLookAt(t, 25, 25);
        if (isWatcher()) { watcherFlight(t); watcherCombat(server, t); return; }
        tickMinions(server);
        boolean special = attacks % 2 == 1;
        if (rushing > 0) {
            rushing--;
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(.8), this::enemy))
                if (rushed.add(e.getUUID())) e.hurt(damageSources().mobAttack(this), 24 + phase() * 3);
            if (rushing == 0 || horizontalCollision) {
                rushing = 0; setDeltaMovement(Vec3.ZERO); finishAttack();
            }
            return;
        }
        if (charge > 0) {
            charge++;
            getNavigation().stop();
            int preparation = 24 - phase() * 2;
            if (charge <= preparation - 10) lock = CoreShots.aimPoint(t);
            if (!special) CoreShots.laser(this, getEyePosition(), lock);
            else if (charge % 2 == 0) {
                for (Vec3 p : bombardmentCenters()) ring(server, p, 5.5, ParticleTypes.ELECTRIC_SPARK);
            }
            if (charge >= preparation) {
                if (special) {
                    // Tres impactos simultáneos, cada uno marcado durante la preparación.
                    for (Vec3 p : bombardmentCenters()) {
                        ring(server, p, 5.5, ParticleTypes.EXPLOSION);
                        areaDamage(p, 5.5, 26 + phase() * 3);
                    }
                    finishAttack();
                } else {
                    Vec3 dir = lock.subtract(position()).multiply(1, 0, 1).normalize();
                    setDeltaMovement(dir.scale(1.5).add(0, .1, 0));
                    rushed.clear(); rushing = 14; charge = 0;
                }
                playSound(ModSounds.ANCHOR_STOMP.get(), 2.5F, .65F);
            }
            return;
        }
        if (cooldown > 0) cooldown--;
        if (distanceTo(t) > 4) getNavigation().moveTo(t, 1.25);
        else getNavigation().stop();
        if (cooldown <= 0 && hasLineOfSight(t) && distanceTo(t) < 20) {
            charge = 1; lock = CoreShots.aimPoint(t);
            entityData.set(MODE, (byte) (special ? 2 : 1));
            playSound(ModSounds.SENTRY_TURRET_CHARGE.get(), 2, .65F);
        }
    }

    /** Mantiene altura y separación también durante cargas y salvas: nunca se queda a esperar junto al jugador. */
    private void watcherFlight(@Nullable LivingEntity target) {
        Vec3 anchor = Vec3.atCenterOf(home);
        double floor = home.getY() - (console == null ? 0 : 4);
        Vec3 want;
        if (target == null) {
            want = new Vec3(anchor.x, floor + 8, anchor.z);
        } else {
            if (retreatTicks > 0) retreatTicks--;
            Vec3 away = position().subtract(target.position()).multiply(1, 0, 1);
            double angle = away.lengthSqr() < .1 ? tickCount * .04 : Math.atan2(away.z, away.x) + .12;
            double radius = retreatTicks > 0 || away.lengthSqr() < 100 ? 16 : 12;
            Vec3 horizontal = target.position().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius)
                    .subtract(anchor).multiply(1, 0, 1);
            if (horizontal.length() > 28) horizontal = horizontal.normalize().scale(28);
            want = new Vec3(anchor.x + horizontal.x, Math.max(floor + 8, target.getY() + 7) + Math.sin(tickCount * .06),
                    anchor.z + horizontal.z);
        }
        Vec3 delta = want.subtract(position());
        Vec3 velocity = delta.lengthSqr() < .01 ? Vec3.ZERO : delta.normalize().scale(Math.min(.65, delta.length() * .15));
        if (!level().noCollision(this, getBoundingBox().move(velocity.scale(3)))) {
            Vec3 evade = new Vec3(-velocity.z, .55, velocity.x);
            velocity = level().noCollision(this, getBoundingBox().move(evade.scale(3))) ? evade
                    : new Vec3(velocity.z, .55, -velocity.x);
        }
        setDeltaMovement(getDeltaMovement().lerp(velocity, .25));
    }

    /** Ciclo visible de cuatro ataques distintos; los objetivos se fijan antes de impactar para poder esquivarlos. */
    private void watcherCombat(ServerLevel server, LivingEntity target) {
        int kind = Math.floorMod(attacks, 4); // ráfaga, bombardeo, barrido y EMP
        if (volley > 0) {
            if (kind == 0 && volleyTimer > 5) lock = CoreShots.aimPoint(target);
            Vec3 aim = kind == 2 ? sweepPoint() : lock;
            if (tickCount % 2 == 0) CoreShots.laser(this, getEyePosition(), aim);
            if (--volleyTimer <= 0) {
                CoreShots.fire(this, getEyePosition(), aim.subtract(getEyePosition()), 56, 14 + phase() * 2, 0,
                        OverworldGuardianEntity.class, true);
                playSound(ModSounds.PISTOL_FIRE.get(), 2, kind == 2 ? 1.3F : .7F);
                volley--; volleyTimer = kind == 2 ? 3 : 10;
                if (volley == 0) finishAttack();
            }
            return;
        }
        if (charge > 0) {
            charge++;
            int preparation = kind == 3 ? 30 : 26 - phase() * 2;
            if (kind != 3 && charge <= (kind == 1 ? 6 : preparation - 10)) lock = CoreShots.aimPoint(target);
            if (tickCount % 2 == 0) {
                if (kind == 0 || kind == 2) {
                    CoreShots.laser(this, getEyePosition(), lock);
                    if (kind == 2) for (int side : new int[]{-1, 1})
                        CoreShots.laser(this, getEyePosition(), getEyePosition().add(lock.subtract(getEyePosition()).yRot(side * .52F)));
                } else if (kind == 1) {
                    for (Vec3 p : watcherBombardment()) ring(server, p, 3, ParticleTypes.FLAME);
                } else ring(server, lock, 6, ParticleTypes.ELECTRIC_SPARK);
            }
            if (charge >= preparation) {
                if (kind == 0 || kind == 2) {
                    volley = kind == 2 ? (phase() == 3 ? 9 : 7) : (phase() == 3 ? 4 : 3);
                    volleyTimer = 8; charge = 0;
                } else {
                    if (kind == 1) for (Vec3 p : watcherBombardment()) {
                        ring(server, p, 3, ParticleTypes.EXPLOSION);
                        areaDamage(p, 3, 18 + phase() * 2);
                    }
                    else {
                        ring(server, lock, 6, ParticleTypes.EXPLOSION);
                        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(lock, lock).inflate(6), this::enemy))
                            if (e.distanceToSqr(lock) < 36 && hasLineOfSight(e)
                                    && e.hurt(damageSources().mobAttack(this), 16 + phase() * 2))
                                e.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                        com.bryanamaya.tecnocinetico.effect.ModEffects.INERTIA_ANCHOR.get(), 40), this);
                    }
                    playSound(ModSounds.ANCHOR_STOMP.get(), 3, kind == 3 ? 1.4F : .8F);
                    finishAttack();
                }
            }
            return;
        }
        if (cooldown > 0) cooldown--;
        if (cooldown <= 0 && hasLineOfSight(target)) {
            charge = 1; lock = CoreShots.aimPoint(target);
            entityData.set(MODE, (byte) (kind == 0 ? 1 : kind == 1 ? 2 : kind == 2 ? 4 : 5));
            playSound(ModSounds.SENTRY_TURRET_CHARGE.get(), 2, kind == 3 ? .6F : 1.1F);
        }
    }
    private Vec3 sweepPoint() {
        int total = phase() == 3 ? 9 : 7;
        float angle = -.52F + 1.04F * (total - volley) / (total - 1);
        return getEyePosition().add(lock.subtract(getEyePosition()).normalize().yRot(angle).scale(56));
    }
    private java.util.List<Vec3> watcherBombardment() {
        return java.util.List.of(lock.add(-3, 0, -2), lock.add(3, 0, -2), lock.add(0, 0, 3));
    }

    private void tickMinions(ServerLevel server) {
        minions.removeIf(id -> !(server.getEntity(id) instanceof MatriarchMinionEntity m) || !m.isAlive());
        if (--summonCooldown > 0) {
            if (summonCooldown <= 20 && summonCooldown % 4 == 0)
                ring(server, position(), 4, ParticleTypes.ELECTRIC_SPARK);
            return;
        }
        int remaining = Math.min(phase() + 1, 6 - minions.size());
        for (int attempt = 0; attempt < 32 && remaining > 0; attempt++) {
            double angle = attempt * Math.PI / 8 + random.nextDouble() * .2;
            int x = (int) Math.floor(getX() + Math.cos(angle) * 4), z = (int) Math.floor(getZ() + Math.sin(angle) * 4);
            for (int dy : new int[]{0, 1, -1, 2, -2}) {
                BlockPos p = new BlockPos(x, (int) Math.floor(getY()) + dy, z);
                if (!level().hasChunkAt(p) || !level().getWorldBorder().isWithinBounds(p)
                        || !level().getBlockState(p.below()).isFaceSturdy(level(), p.below(), net.minecraft.core.Direction.UP)) continue;
                var minion = ModEntities.MATRIARCH_MINION.get().create(level());
                if (minion == null) continue;
                minion.moveTo(x + .5, p.getY(), z + .5, random.nextFloat() * 360, 0);
                if (!level().noCollision(minion) || !level().getEntities(minion, minion.getBoundingBox(), e -> e.isAlive()
                        && !(e instanceof net.minecraft.world.entity.item.ItemEntity)
                        && !(e instanceof net.minecraft.world.entity.ExperienceOrb)).isEmpty()) continue;
                minion.finalizeSpawn(server, server.getCurrentDifficultyAt(p), net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null, null);
                minion.bind(this);
                if (server.addFreshEntity(minion)) {
                    minions.add(minion.getUUID()); remaining--;
                    server.sendParticles(ParticleTypes.SMOKE, x + .5, p.getY() + .3, z + .5, 12, .3, .3, .3, .04);
                }
                break;
            }
        }
        summonCooldown = 280 - phase() * 40; // 12 / 10 / 8 s, sin tocar los ataques actuales de la Matriarca
        playSound(ModSounds.SWARM_BODY_AMBIENT.get(), 2, 1.4F);
    }
    private void clearMinions() {
        if (level() instanceof ServerLevel server) for (var id : minions) {
            Entity e = server.getEntity(id);
            if (e instanceof MatriarchMinionEntity) e.discard();
        }
        minions.clear();
    }

    private void finishAttack() {
        charge = 0; attacks++;
        cooldown = isWatcher() ? 50 - phase() * 8 : 40 - phase() * 8;
        entityData.set(MODE, (byte) 0);
    }
    private java.util.List<Vec3> bombardmentCenters() {
        return java.util.List.of(lock, lock.add(5, 0, 0), lock.add(-5, 0, 0));
    }
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
        clearMinions();
        super.die(source);
        if (!level().isClientSide && console != null && level().getBlockEntity(console) instanceof CoreConsoleBlockEntity c) c.defeated();
    }
    @Override public void remove(RemovalReason reason) {
        if (reason == RemovalReason.DISCARDED || reason == RemovalReason.KILLED) clearMinions();
        if (reason == RemovalReason.DISCARDED && !level().isClientSide && console != null
                && level().getBlockEntity(console) instanceof CoreConsoleBlockEntity c && c.isActive()) c.reset();
        super.remove(reason);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (console != null) tag.put("Console", NbtUtils.writeBlockPos(console));
        if (home != null) tag.put("Home", NbtUtils.writeBlockPos(home));
        tag.putInt("Attacks", attacks);
        tag.putByte("BrokenCapacitors", entityData.get(BROKEN));
        tag.putFloat("Capacitor0", capacitorDamage[0]); tag.putFloat("Capacitor1", capacitorDamage[1]);
        tag.putInt("Stun", stun);
        tag.putInt("GuardianBalance", 2);
        tag.putInt("SummonCooldown", summonCooldown);
        tag.putInt("MinionCount", minions.size());
        int i = 0;
        for (var id : minions) tag.putUUID("Minion" + i++, id);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getInt("GuardianBalance") < 2) {
            float fraction = getHealth() / getMaxHealth();
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(isWatcher() ? 700 : 1000);
            getAttribute(Attributes.ARMOR).setBaseValue(isWatcher() ? 18 : 22);
            getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(isWatcher() ? 8 : 12);
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(isWatcher() ? 22 : 28);
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(isWatcher() ? .3 : .36);
            getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(64);
            setHealth(getMaxHealth() * fraction);
        }
        if (tag.contains("Console")) console = NbtUtils.readBlockPos(tag.getCompound("Console"));
        if (tag.contains("Home")) home = NbtUtils.readBlockPos(tag.getCompound("Home"));
        attacks = tag.getInt("Attacks");
        summonCooldown = tag.contains("SummonCooldown") ? Math.max(1, Math.min(240, tag.getInt("SummonCooldown"))) : 80;
        minions.clear();
        for (int i = 0; i < Math.min(6, tag.getInt("MinionCount")); i++)
            if (tag.hasUUID("Minion" + i)) minions.add(tag.getUUID("Minion" + i));
        entityData.set(BROKEN, isWatcher() ? tag.getByte("BrokenCapacitors") : 0);
        capacitorDamage[0] = tag.getFloat("Capacitor0"); capacitorDamage[1] = tag.getFloat("Capacitor1");
        stun = isWatcher() ? Math.max(0, Math.min(60, tag.getInt("Stun"))) : 0;
        bar.setName(getDisplayName());
    }
}
