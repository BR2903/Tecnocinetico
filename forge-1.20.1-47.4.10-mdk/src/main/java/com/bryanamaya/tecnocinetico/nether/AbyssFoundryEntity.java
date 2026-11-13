package com.bryanamaya.tecnocinetico.nether;

import com.bryanamaya.tecnocinetico.block.entity.CoreConsoleBlockEntity;
import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.core.BlockPos;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Fundidor del Abismo: primer jefe del Nether. Horno-máquina de ~4,5 bloques con un martillo (brazo derecho) y un
 * crisol de escoria (brazo izquierdo). Se despierta con la consola de su fundición (CoreConsoleBlockEntity,
 * guardián "abyss_foundry"). Todo se decide en el servidor; cada ataque avisa antes (pose + partículas):
 * <ul>
 *   <li><b>Martillazo</b> (cerca): levanta el martillo (brazo derecho) y lo estrella delante y a su derecha.</li>
 *   <li><b>Fractura de lava</b> (media distancia): grieta recta hacia el punto fijado que suelta fuego.</li>
 *   <li><b>Escoria fundida</b>: marca 3 (5 enfadado) puntos alrededor del objetivo y lanza desde el crisol bolas de
 *       escoria que vuelan en arco y dejan charcos ardientes unos segundos.</li>
 *   <li><b>Sobrecalentamiento</b> (cada 4.º ataque): onda de calor muy fuerte (32 de daño, la armadura lo reduce; +45 de calor del Nether) y luego queda
 *       <b>expuesto</b> {@link #EXPOSED_TICKS} ticks recibiendo ×1,6 de daño sin atacar.</li>
 * </ul>
 * Por debajo de la mitad de vida se enfada: avisos y pausas más cortos. Multijugador: aguanta un 50 % más por cada
 * jugador extra cerca al despertar (máx. ×3; recibe menos daño, porque la vida no puede pasar de 1024) y al morir suelta un Acoplador del Nether por jugador que luchó (máx. 4).
 * Si no hay jugadores en {@link #ARENA} bloques durante un minuto, o el objetivo se aleja demasiado, se reinicia.
 * <p>
 * Contrato con el modelo: {@link #attack()}, {@link #windup()}, {@link #exposed()}, {@link #enraged()}.
 */
public class AbyssFoundryEntity extends Monster {

    public static final int HAMMER = 1, FRACTURE = 2, SLAG = 3, OVERHEAT = 4;
    public static final float BASE_HEALTH = 1000.0F;   // la vida máxima de Minecraft es 1024
    public static final int EXPOSED_TICKS = 80, ARENA = 40;
    public static final float EXPOSED_MULTIPLIER = 1.6F;
    /** Desplazamiento del martillazo hacia su derecha (bloques): el martillo está en el brazo derecho. */
    public static final double HAMMER_SIDE = 1.4;
    /** Daño del sobrecalentamiento (golpe normal: la armadura lo reduce). Sin armadura buena, mata. */
    public static final float OVERHEAT_DAMAGE = 32.0F;
    /** Ticks que tarda en volar cada bola de escoria. */
    public static final int SLAG_FLIGHT = 14;

    private static final EntityDataAccessor<Integer> ATTACK = SynchedEntityData.defineId(AbyssFoundryEntity.class, EntityDataSerializers.INT);
    /** Ticks que quedan de aviso y su total (para el progreso del aviso en el cliente). */
    private static final EntityDataAccessor<Integer> WINDUP = SynchedEntityData.defineId(AbyssFoundryEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WINDUP_MAX = SynchedEntityData.defineId(AbyssFoundryEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> EXPOSED = SynchedEntityData.defineId(AbyssFoundryEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ENRAGED = SynchedEntityData.defineId(AbyssFoundryEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    @Nullable
    private BlockPos console, home;
    private int cooldown = 40, sequence, exposedLeft, lonely;
    /** Aguante por jugadores: el daño recibido se divide por esto (1 a 3). */
    private float toughness = 1.0F;
    private Vec3 lock = Vec3.ZERO;
    private final List<Vec3> targets = new ArrayList<>();
    /** Bolas de escoria en vuelo: origen (x, y, z), destino (x, y, z), tick de vuelo (negativo = aún no sale). */
    private final List<double[]> flights = new ArrayList<>();
    /** Charcos de escoria: x, y, z, ticks que quedan. */
    private final List<double[]> puddles = new ArrayList<>();
    /** Jugadores que le hicieron daño (para repartir el botín). */
    private final java.util.Set<java.util.UUID> fighters = new java.util.HashSet<>();

    public AbyssFoundryEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 650;
        setMaxUpStep(1.5F);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, BASE_HEALTH)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0D)
                .add(Attributes.ATTACK_DAMAGE, 26.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
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

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 24.0F));
    }

    // ------------------------------------------------------------------ contrato con el modelo

    public int attack() {
        return entityData.get(ATTACK);
    }

    /** 0 = empieza el aviso, 1 = golpea. 0 también si no hay ataque. */
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

    private void setAttack(int attack, int windupTicks) {
        entityData.set(ATTACK, attack);
        entityData.set(WINDUP, windupTicks);
        entityData.set(WINDUP_MAX, Math.max(1, windupTicks));
    }

    // ------------------------------------------------------------------ consola y multijugador

    /** La consola que lo despertó: su casa (la arena) es donde aparece. Ajusta la vida al número de jugadores cerca. */
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

    // ------------------------------------------------------------------ combate

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
        if (exposedLeft > 0) {                                                       // abierto: no ataca ni anda
            getNavigation().stop();
            if (--exposedLeft == 0) {
                entityData.set(EXPOSED, false);
            } else if (exposedLeft % 4 == 0) {
                server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 3.0, getZ(), 3, 0.6, 0.4, 0.6, 0.02);
            }
            return;
        }
        LivingEntity t = pickTarget();
        if (t == null) {
            setAttack(0, 0);
            return;
        }
        getLookControl().setLookAt(t, 20.0F, 20.0F);
        if (attack() != 0) {
            tickWindup(server, t);
            return;
        }
        double dist = distanceTo(t);
        if (dist > 4.0) {
            getNavigation().moveTo(t, 1.0D);
        } else {
            getNavigation().stop();
        }
        if (--cooldown > 0) {
            return;
        }
        // elegir ataque
        sequence++;
        int attack = sequence % 4 == 0 ? OVERHEAT : dist < 6.0 ? HAMMER : dist < 18.0 && random.nextBoolean() ? FRACTURE : SLAG;
        int windup = switch (attack) {
            case OVERHEAT -> 40;
            case SLAG -> 26;
            default -> 30;
        };
        if (enraged()) {
            windup = Math.round(windup * 0.7F);
        }
        setAttack(attack, windup);
        getNavigation().stop();
        if (attack == HAMMER) {                                                      // el martillo va en la mano derecha:
            Vec3 dir = flat(t.position().subtract(position()));                      // cae delante y a su derecha
            Vec3 right = new Vec3(-dir.z, 0, dir.x);
            lock = position().add(dir.scale(Math.min(dist, 3.5))).add(right.scale(HAMMER_SIDE));
        } else {
            lock = t.position();
        }
        targets.clear();
        if (attack == SLAG) {
            int n = enraged() ? 5 : 3;
            targets.add(t.position());
            for (int i = 1; i < n; i++) {
                double a = random.nextDouble() * Math.PI * 2, r = 2.5 + random.nextDouble() * 3.0;
                targets.add(t.position().add(Math.cos(a) * r, 0, Math.sin(a) * r));
            }
        }
        server.playSound(null, blockPosition(), attack == OVERHEAT ? SoundEvents.FURNACE_FIRE_CRACKLE : SoundEvents.ANVIL_PLACE,
                SoundSource.HOSTILE, 3.0F, attack == OVERHEAT ? 0.5F : 0.6F);
    }

    /** Durante el aviso: partículas que marcan dónde va a pegar; al terminar, el golpe. */
    private void tickWindup(ServerLevel server, LivingEntity t) {
        int left = entityData.get(WINDUP) - 1;
        entityData.set(WINDUP, left);
        getNavigation().stop();
        if (left % 3 == 0) {
            switch (attack()) {
                case HAMMER -> ring(server, lock, 3.5, ParticleTypes.FLAME);
                case FRACTURE -> line(server, ParticleTypes.FLAME);
                case SLAG -> targets.forEach(p -> ring(server, p, 2.0, ParticleTypes.LAVA));
                case OVERHEAT -> {
                    ring(server, position(), 8.0, ParticleTypes.FLAME);
                    server.sendParticles(ParticleTypes.LAVA, getX(), getY() + 3.0, getZ(), 4, 1.0, 1.0, 1.0, 0.1);
                }
                default -> {}
            }
        }
        if (left > 0) {
            return;
        }
        switch (attack()) {
            case HAMMER -> hammer(server);
            case FRACTURE -> fracture(server);
            case SLAG -> slag(server);
            case OVERHEAT -> overheat(server);
            default -> {}
        }
        setAttack(0, 0);
        cooldown = enraged() ? 14 : 24;
    }

    private void hammer(ServerLevel server) {
        server.playSound(null, BlockPos.containing(lock), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.5F, 0.7F);
        server.sendParticles(ParticleTypes.EXPLOSION, lock.x, lock.y + 0.5, lock.z, 3, 1.0, 0.2, 1.0, 0.0);
        hurtArea(server, lock, 3.5, 36.0F, 1.8, 0);
    }

    private void fracture(ServerLevel server) {
        server.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 3.0F, 0.5F);
        Vec3 dir = flat(lock.subtract(position()));
        for (int i = 2; i <= 18; i++) {
            Vec3 p = position().add(dir.scale(i));
            server.sendParticles(ParticleTypes.LAVA, p.x, p.y + 0.3, p.z, 2, 0.3, 0.1, 0.3, 0.0);
            server.sendParticles(ParticleTypes.FLAME, p.x, p.y + 0.6, p.z, 4, 0.3, 0.4, 0.3, 0.02);
            hurtArea(server, p, 1.5, 24.0F, 0.5, 8);
        }
    }

    /** Lanza las bolas desde el crisol (mano izquierda, por encima del hombro); vuelan en arco y caen en los puntos marcados. */
    private void slag(ServerLevel server) {
        server.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 3.0F, 0.5F);
        Vec3 dir = targets.isEmpty() ? flat(getLookAngle()) : flat(targets.get(0).subtract(position()));
        Vec3 left = new Vec3(dir.z, 0, -dir.x);
        Vec3 hand = position().add(0, 5.5, 0).add(left.scale(1.6)).add(dir.scale(1.0));
        int i = 0;
        for (Vec3 p : targets) {
            flights.add(new double[]{hand.x, hand.y, hand.z, p.x, p.y, p.z, -2 * i++});
        }
    }

    /** Vuelo de las bolas de escoria (rastro de fuego y humo) y su impacto: daño, fuego y charco. */
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
                ring(server, b, 2.0, ParticleTypes.LAVA);                              // el punto de caída sigue marcado
            }
            if (f[6] >= SLAG_FLIGHT) {
                server.playSound(null, BlockPos.containing(b), SoundEvents.LAVA_EXTINGUISH, SoundSource.HOSTILE, 2.0F, 0.6F);
                server.sendParticles(ParticleTypes.LAVA, b.x, b.y + 0.5, b.z, 14, 0.8, 0.3, 0.8, 0.1);
                server.sendParticles(ParticleTypes.EXPLOSION, b.x, b.y + 0.5, b.z, 1, 0, 0, 0, 0);
                hurtArea(server, b, 2.5, 22.0F, 0.4, 6);
                puddles.add(new double[]{b.x, b.y, b.z, 100});
                it.remove();
            }
        }
    }

    private void overheat(ServerLevel server) {
        server.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 4.0F, 0.5F);
        server.sendParticles(ParticleTypes.FLAME, getX(), getY() + 2.0, getZ(), 120, 4.0, 1.5, 4.0, 0.15);
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(8.0))) {
            if (p.isSpectator() || p.isCreative()) {
                continue;
            }
            p.hurt(damageSources().mobAttack(this), OVERHEAT_DAMAGE);              // la armadura cuenta (progresión)
            float factor = NetherHazards.heatFactor(p);                              // la piroxita también protege aquí
            p.getPersistentData().putFloat(NetherHazards.HEAT, Math.min(NetherHazards.MAX, NetherHazards.heat(p) + 45.0F * factor));
            Vec3 push = flat(p.position().subtract(position())).scale(1.4);
            p.push(push.x, 0.5, push.z);
            p.hurtMarked = true;
        }
        exposedLeft = EXPOSED_TICKS;
        entityData.set(EXPOSED, true);
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

    /** Daño a los jugadores (y mobs que no sean él) dentro de un radio; empuje hacia fuera y fuego opcional. */
    private void hurtArea(ServerLevel server, Vec3 c, double radius, float damage, double push, int fireSeconds) {
        AABB box = new AABB(c, c).inflate(radius, 2.0, radius);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive())) {
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

    private void ring(ServerLevel server, Vec3 c, double r, net.minecraft.core.particles.SimpleParticleType type) {
        int n = (int) (r * 8);
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            server.sendParticles(type, c.x + Math.cos(a) * r, c.y + 0.1, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    private void line(ServerLevel server, net.minecraft.core.particles.SimpleParticleType type) {
        Vec3 dir = flat(lock.subtract(position()));
        for (int i = 2; i <= 18; i += 2) {
            Vec3 p = position().add(dir.scale(i));
            server.sendParticles(type, p.x, p.y + 0.1, p.z, 1, 0.2, 0, 0.2, 0);
        }
    }

    private static Vec3 flat(Vec3 v) {
        Vec3 h = new Vec3(v.x, 0, v.z);
        return h.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : h.normalize();
    }

    /** El objetivo actual si sigue siendo válido y en la arena; si no, el jugador más cercano que vea. */
    @Nullable
    private LivingEntity pickTarget() {
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

    /** Sin jugadores en la arena durante un minuto (o en Pacífico): desaparece y la consola queda lista otra vez. */
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

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.FALL) || source.is(DamageTypes.DROWN)) {
            return false;
        }
        if (exposed()) {
            amount *= EXPOSED_MULTIPLIER;
        }
        amount /= toughness;
        if (source.getEntity() instanceof Player p) {
            fighters.add(p.getUUID());
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return attack() == 0 && !exposed() && super.doHurtTarget(target);
    }

    // ------------------------------------------------------------------ final del combate

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server) {
            consoleEntity(server, true);
            bar.removeAllPlayers();
            server.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("message.tecnocinetico.foundry_defeated")
                    .withStyle(net.minecraft.ChatFormatting.GOLD), false);
        }
    }

    /** Botín: un Acoplador del Nether por jugador que luchó (1-4), componentes, aleación, piroxita y netherita. */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hit) {
        super.dropCustomDeathLoot(source, looting, hit);
        int players = Math.max(1, Math.min(4, fighters.size()));
        drop(new ItemStack(ModItems.NETHER_COUPLER.get(), players));
        drop(new ItemStack(ModItems.ABYSSAL_COMPONENT.get(), 3 + 2 * players));
        drop(new ItemStack(ModItems.SLAG_ALLOY.get(), 16 + random.nextInt(9)));
        drop(new ItemStack(ModItems.PIROXITE_INGOT.get(), 8 + random.nextInt(5)));
        drop(new ItemStack(Items.NETHERITE_INGOT, 1 + random.nextInt(2)));
    }

    private void drop(ItemStack stack) {
        ItemEntity item = new ItemEntity(level(), getX(), getY() + 1.0, getZ(), stack);
        item.setInvulnerable(true);                                                   // que no se quemen en la lava
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

    // pruebas
    public void testSetExposed(int ticks) {
        exposedLeft = ticks;
        entityData.set(EXPOSED, ticks > 0);
    }

    public int testCooldown() {
        return cooldown;
    }
}
