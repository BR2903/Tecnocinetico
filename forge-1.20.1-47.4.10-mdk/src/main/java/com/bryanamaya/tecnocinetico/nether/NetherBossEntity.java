package com.bryanamaya.tecnocinetico.nether;

import com.bryanamaya.tecnocinetico.block.entity.CoreConsoleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Base de los jefes del Nether (Fundidor del Abismo, Devorador de Escoria): lo que comparten.
 * <ul>
 *   <li>Contrato con el modelo: {@link #attack()}, {@link #windup()} (0 = empieza el aviso, 1 = golpe),
 *       {@link #exposed()} (recibe {@link #exposedMultiplier()} de daño) y {@link #enraged()} (bajo el 50 %).</li>
 *   <li>Consola: se despierta con una Consola del Núcleo; al morir la deja vencida y al desaparecer la reinicia.</li>
 *   <li>Multijugador: aguanta un 50 % más por cada jugador extra en la arena al despertar (máx. ×3; divide el daño,
 *       porque la vida de Minecraft no pasa de 1024) y el botín se reparte según los jugadores que le pegaron.</li>
 *   <li>Reinicio: sin jugadores en {@link #ARENA} bloques durante un minuto, o en Pacífico, desaparece.</li>
 *   <li>Utilidades: barra de jefe, daño en área, avisos con anillos de partículas, bolas de escoria en arco y charcos.</li>
 * </ul>
 */
public abstract class NetherBossEntity extends Monster {

    public static final int ARENA = 40;
    /** Ticks que tarda en volar cada bola de escoria. */
    public static final int SLAG_FLIGHT = 14;

    private static final EntityDataAccessor<Integer> ATTACK = SynchedEntityData.defineId(NetherBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WINDUP = SynchedEntityData.defineId(NetherBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WINDUP_MAX = SynchedEntityData.defineId(NetherBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> EXPOSED = SynchedEntityData.defineId(NetherBossEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ENRAGED = SynchedEntityData.defineId(NetherBossEntity.class, EntityDataSerializers.BOOLEAN);

    protected final ServerBossEvent bar;
    @Nullable
    protected BlockPos console, home;
    private int lonely;
    /** Aguante por jugadores: el daño recibido se divide por esto (1 a 3). */
    private float toughness = 1.0F;
    /** Jugadores que le hicieron daño (para repartir el botín). */
    private final Set<UUID> fighters = new HashSet<>();
    /** Bolas de escoria en vuelo: origen, destino, tick (negativo = aún no sale), daño, radio. */
    private final List<double[]> flights = new ArrayList<>();
    /** Charcos de escoria: x, y, z, ticks que quedan. */
    private final List<double[]> puddles = new ArrayList<>();

    protected NetherBossEntity(EntityType<? extends Monster> type, Level level, BossEvent.BossBarColor color) {
        super(type, level);
        bar = new ServerBossEvent(getDisplayName(), color, BossEvent.BossBarOverlay.NOTCHED_10);
        setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ATTACK, 0);
        entityData.define(WINDUP, 0);
        entityData.define(WINDUP_MAX, 1);
        entityData.define(EXPOSED, false);
        entityData.define(ENRAGED, false);
    }

    // ------------------------------------------------------------------ contrato con el modelo

    public int attack() {
        return entityData.get(ATTACK);
    }

    public float windup() {
        int left = entityData.get(WINDUP), max = Math.max(1, entityData.get(WINDUP_MAX));
        return attack() == 0 ? 0.0F : 1.0F - left / (float) max;
    }

    public boolean exposed() {
        return entityData.get(EXPOSED);
    }

    public boolean enraged() {
        return entityData.get(ENRAGED);
    }

    protected void setAttack(int attack, int windupTicks) {
        entityData.set(ATTACK, attack);
        entityData.set(WINDUP, windupTicks);
        entityData.set(WINDUP_MAX, Math.max(1, windupTicks));
    }

    protected int windupLeft() {
        return entityData.get(WINDUP);
    }

    protected void setWindupLeft(int ticks) {
        entityData.set(WINDUP, ticks);
    }

    protected void setExposed(boolean on) {
        entityData.set(EXPOSED, on);
    }

    /** Multiplicador de daño mientras está expuesto. */
    protected abstract float exposedMultiplier();

    /** Lógica propia de cada jefe (después de lo común: barra, enfado, bolas, charcos, reinicio). */
    protected abstract void bossTick(ServerLevel server);

    /** Clave del mensaje que se anuncia a todos al vencerlo. */
    protected abstract String defeatMessage();

    /** Botín según los jugadores que lucharon (1-4). */
    protected abstract void loot(int players);

    // ------------------------------------------------------------------ consola y multijugador

    /** La consola que lo despertó: su casa (la arena) es donde aparece. Ajusta el aguante al número de jugadores cerca. */
    public void setConsole(BlockPos pos) {
        console = pos;
        home = blockPosition();
        int players = level().getEntitiesOfClass(Player.class, new AABB(home).inflate(ARENA), p -> !p.isSpectator() && !p.isCreative()).size();
        toughness = Math.min(3.0F, 1.0F + 0.5F * Math.max(0, players - 1));
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bar.removePlayer(player);
    }

    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    // ------------------------------------------------------------------ tick común

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        bar.setProgress(getHealth() / getMaxHealth());
        if (!enraged() && getHealth() < getMaxHealth() / 2) {
            entityData.set(ENRAGED, true);
            bar.setColor(BossEvent.BossBarColor.YELLOW);
            server.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 3.0F, 0.4F);
        }
        tickFlights(server);
        tickPuddles(server);
        if (checkReset()) {
            return;
        }
        bossTick(server);
    }

    /** El objetivo actual si sigue siendo válido y en la arena; si no, el jugador más cercano. */
    @Nullable
    protected LivingEntity pickTarget() {
        LivingEntity t = getTarget();
        if (t != null && (!t.isAlive() || t instanceof Player p && (p.isCreative() || p.isSpectator())
                || home != null && t.distanceToSqr(Vec3.atCenterOf(home)) > (ARENA + 8) * (ARENA + 8))) {
            setTarget(null);
            t = null;
        }
        if (t == null) {
            t = level().getNearestPlayer(this, ARENA);
            if (t instanceof Player p && (p.isCreative() || p.isSpectator())) {
                t = null;
            }
            setTarget(t);
        }
        return t;
    }

    private boolean checkReset() {
        BlockPos c = home != null ? home : blockPosition();
        boolean anyone = !level().getEntitiesOfClass(Player.class, new AABB(c).inflate(ARENA), p -> !p.isSpectator() && !p.isCreative()).isEmpty();
        lonely = anyone ? 0 : lonely + 1;
        if (lonely > 1200 || level().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            discard();
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ ataques compartidos

    /** Lanza una bola de escoria en arco de `from` a `to` (sale tras `delay` ticks); al caer hace daño y deja charco. */
    protected void launchSlag(Vec3 from, Vec3 to, int delay, float damage, double radius) {
        flights.add(new double[]{from.x, from.y, from.z, to.x, to.y, to.z, -delay, damage, radius});
    }

    private void tickFlights(ServerLevel server) {
        var it = flights.iterator();
        while (it.hasNext()) {
            double[] f = it.next();
            if (++f[6] < 0) {
                continue;
            }
            Vec3 a = new Vec3(f[0], f[1], f[2]), b = new Vec3(f[3], f[4], f[5]);
            double t = f[6] / SLAG_FLIGHT, arc = 3.0 + a.distanceTo(b) * 0.2;
            Vec3 at = a.lerp(b, t).add(0, 4 * arc * t * (1 - t), 0);
            server.sendParticles(ParticleTypes.LAVA, at.x, at.y, at.z, 2, 0.15, 0.15, 0.15, 0.0);
            server.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 4, 0.2, 0.2, 0.2, 0.01);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 1, 0.1, 0.1, 0.1, 0.0);
            if (f[6] % 3 == 0) {
                ring(server, b, f[8] * 0.8, ParticleTypes.LAVA);                       // el punto de caída sigue marcado
            }
            if (f[6] >= SLAG_FLIGHT) {
                server.playSound(null, BlockPos.containing(b), SoundEvents.LAVA_EXTINGUISH, SoundSource.HOSTILE, 2.0F, 0.6F);
                server.sendParticles(ParticleTypes.LAVA, b.x, b.y + 0.5, b.z, 14, 0.8, 0.3, 0.8, 0.1);
                server.sendParticles(ParticleTypes.EXPLOSION, b.x, b.y + 0.5, b.z, 1, 0, 0, 0, 0);
                hurtArea(server, b, f[8], (float) f[7], 0.4, 6);
                puddles.add(new double[]{b.x, b.y, b.z, 100});
                it.remove();
            }
        }
    }

    /** Charcos de escoria: queman a quien los pisa cada 10 ticks. */
    private void tickPuddles(ServerLevel server) {
        puddles.removeIf(p -> --p[3] <= 0);
        for (double[] p : puddles) {
            if (tickCount % 5 == 0) {
                server.sendParticles(ParticleTypes.LAVA, p[0], p[1] + 0.2, p[2], 1, 0.8, 0.0, 0.8, 0.0);
                server.sendParticles(ParticleTypes.SMOKE, p[0], p[1] + 0.3, p[2], 2, 0.8, 0.1, 0.8, 0.01);
            }
            if (tickCount % 10 == 0) {
                hurtArea(server, new Vec3(p[0], p[1], p[2]), 1.8, 6.0F, 0.0, 5);
            }
        }
    }

    /** Daño a los jugadores (y mobs que no sean jefes del Nether ni sus ácaros aliados) en un radio; empuje y fuego. */
    protected void hurtArea(ServerLevel server, Vec3 c, double radius, float damage, double push, int fireSeconds) {
        AABB box = new AABB(c, c).inflate(radius, 2.0, radius);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive()
                && !(e instanceof NetherBossEntity) && !(e instanceof InfernalEntity))) {
            if (e instanceof Player p && (p.isSpectator() || p.isCreative())) {
                continue;
            }
            if (e.position().distanceToSqr(c.x, e.getY(), c.z) > radius * radius) {
                continue;
            }
            e.hurt(damageSources().mobAttack(this), damage);
            if (fireSeconds > 0) {
                e.setSecondsOnFire(fireSeconds);
            }
            if (push > 0) {
                Vec3 d = flat(e.position().subtract(c)).scale(push);
                e.push(d.x, 0.35, d.z);
                e.hurtMarked = true;
            }
        }
    }

    protected void ring(ServerLevel server, Vec3 c, double r, SimpleParticleType type) {
        int n = Math.max(6, (int) (r * 8));
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            server.sendParticles(type, c.x + Math.cos(a) * r, c.y + 0.1, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    protected static Vec3 flat(Vec3 v) {
        Vec3 h = new Vec3(v.x, 0, v.z);
        return h.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : h.normalize();
    }

    // ------------------------------------------------------------------ daño y final

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.FALL) || source.is(DamageTypes.DROWN)) {
            return false;
        }
        if (exposed()) {
            amount *= exposedMultiplier();
        }
        amount /= toughness;
        if (source.getEntity() instanceof Player p) {
            fighters.add(p.getUUID());
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server) {
            consoleEntity(server, true);
            bar.removeAllPlayers();
            server.getServer().getPlayerList().broadcastSystemMessage(Component.translatable(defeatMessage())
                    .withStyle(net.minecraft.ChatFormatting.GOLD), false);
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hit) {
        super.dropCustomDeathLoot(source, looting, hit);
        loot(Math.max(1, Math.min(4, fighters.size())));
    }

    /** Suelta un objeto que no se quema en la lava. */
    protected void drop(ItemStack stack) {
        ItemEntity item = new ItemEntity(level(), getX(), getY() + 1.0, getZ(), stack);
        item.setInvulnerable(true);
        item.setDeltaMovement((random.nextDouble() - 0.5) * 0.3, 0.4, (random.nextDouble() - 0.5) * 0.3);
        level().addFreshEntity(item);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason == RemovalReason.DISCARDED && !isDeadOrDying() && level() instanceof ServerLevel server) {
            consoleEntity(server, false);
        }
        bar.removeAllPlayers();
        super.remove(reason);
    }

    private void consoleEntity(ServerLevel server, boolean beaten) {
        if (console != null && server.getBlockEntity(console) instanceof CoreConsoleBlockEntity c) {
            if (beaten) {
                c.defeated();
            } else {
                c.reset();
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (console != null) {
            tag.put("Console", NbtUtils.writeBlockPos(console));
        }
        if (home != null) {
            tag.put("Home", NbtUtils.writeBlockPos(home));
        }
        tag.putFloat("Toughness", toughness);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Console")) {
            console = NbtUtils.readBlockPos(tag.getCompound("Console"));
        }
        if (tag.contains("Home")) {
            home = NbtUtils.readBlockPos(tag.getCompound("Home"));
        }
        toughness = tag.contains("Toughness") ? Math.max(1.0F, tag.getFloat("Toughness")) : 1.0F;
        if (hasCustomName()) {
            bar.setName(getDisplayName());
        }
    }
}
