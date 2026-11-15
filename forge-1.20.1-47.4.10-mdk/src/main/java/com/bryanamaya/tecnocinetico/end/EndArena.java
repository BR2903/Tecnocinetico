package com.bryanamaya.tecnocinetico.end;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.effect.ModEffects;
import com.bryanamaya.tecnocinetico.entity.ModEntities;
import com.bryanamaya.tecnocinetico.gear.NetherGadgetItem;
import com.bryanamaya.tecnocinetico.gear.TacticalGear;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.network.EndQuakePacket;
import com.bryanamaya.tecnocinetico.network.ModNetwork;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * End, parte 2: <b>llegada y Dragón modificado</b> (diseño del usuario, ver {@code docs/PLAN_END.md}).
 * <ol>
 *   <li><b>Dormido</b>: al llegar no hay Dragón ni barra de jefe (la pelea vanilla se marca como "matado" mientras tanto)
 *       y las torres de obsidiana están <b>rotas</b>: se les corta la punta, sin cristales ni jaulas, con escombros.</li>
 *   <li><b>Despertar</b> ({@link #AWAKEN_TICKS}): cuando un jugador en supervivencia se acerca a menos de
 *       {@link #TRIGGER_RADIUS} bloques del centro, la isla tiembla (pantalla), la Piedra del End se corrompe en círculos
 *       crecientes y suena el rugido.</li>
 *   <li><b>Pelea</b>: el Dragón del End modificado sale de debajo de la isla y en cada torre aparece una
 *       <b>Torreta-repetidor</b> ({@link EndRelayTurretEntity}) que lo cura como un cristal y dispara a los jugadores.
 *       Ataques nuevos: aliento corrupto (nubes de corrupción de datos), Pulso EMP al aterrizar, Lluvia de datos
 *       desde el aire y refuerzos a mitad de vida (endermen corruptos y dos torretas reparadas).</li>
 *   <li><b>Derrotado</b>: botín junto al portal, uno por jugador que luchó (máx. 4).</li>
 * </ol>
 * Todo se decide en el servidor; los efectos los ven todos (partículas de servidor y paquete de temblor).
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID)
public final class EndArena {

    public static final String CORRUPTED = "TecnoCorruptedDragon", LAST_SITTING = "TecnoDragonSitting";
    public static final double TRIGGER_RADIUS = 50.0D, ARENA_RADIUS = 160.0D;
    public static final int AWAKEN_TICKS = 300;
    public static final float DRAGON_HEALTH = 400.0F;
    public static final int RAIN_EVERY = 280, RAIN_WARNING = 30;
    public static final float RAIN_DAMAGE = 10.0F, RAIN_RADIUS = 2.6F;
    public static final double EMP_RADIUS = 14.0D;
    public static final int EMP_TICKS = 120;
    public static final int CORRUPTION_TICKS = 100;

    private EndArena() {}

    public static boolean isCorrupted(Entity e) {
        return e instanceof EnderDragon && e.getPersistentData().getBoolean(CORRUPTED);
    }

    /** Jugadores (no espectadores) en la arena central. */
    public static List<ServerPlayer> arenaPlayers(ServerLevel end) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : end.players()) {
            if (p.isAlive() && !p.isSpectator() && p.position().horizontalDistanceSqr() < ARENA_RADIUS * ARENA_RADIUS) {
                out.add(p);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ tick del End

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel end) || end.dimension() != Level.END) {
            return;
        }
        EndDragonFight fight = end.getDragonFight();
        if (fight == null) {
            return;
        }
        EndArenaData data = EndArenaData.get(end);
        if (end.getGameTime() % 20 == 0) {
            ruinTowers(end, data);
        }
        switch (data.state) {
            case DORMANT -> dormant(end, fight, data);
            case AWAKENING -> awakening(end, fight, data);
            case FIGHT -> fight(end, fight, data);
            default -> { }
        }
    }

    private static void dormant(ServerLevel end, EndDragonFight fight, EndArenaData data) {
        fight.dragonKilled = true;                                            // sin Dragón ni barra hasta el despertar
        if (end.getGameTime() % 20 == 0) {
            for (EnderDragon d : end.getDragons()) {
                if (!isCorrupted(d)) {
                    d.discard();                                              // un Dragón vanilla de antes de este cambio
                }
            }
        }
        for (ServerPlayer p : end.players()) {
            if (p.isAlive() && !p.isCreative() && !p.isSpectator()
                    && p.position().horizontalDistanceSqr() < TRIGGER_RADIUS * TRIGGER_RADIUS) {
                startAwakening(end, data);
                return;
            }
        }
    }

    public static void startAwakening(ServerLevel end, EndArenaData data) {
        data.set(EndArenaData.State.AWAKENING);
        for (ServerPlayer p : end.players()) {
            p.displayClientMessage(Component.translatable("message.tecnocinetico.end_awakening").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        }
        end.playSound(null, 0, 70, 0, SoundEvents.ENDER_DRAGON_AMBIENT, SoundSource.HOSTILE, 6.0F, 0.5F);
    }

    private static void awakening(ServerLevel end, EndDragonFight fight, EndArenaData data) {
        fight.dragonKilled = true;
        int t = ++data.timer;
        data.setDirty();
        if (t % 20 == 1) {                                                    // temblor: pantalla y sonido
            float strength = 0.4F + 0.6F * t / AWAKEN_TICKS;
            for (ServerPlayer p : arenaPlayers(end)) {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new EndQuakePacket(24, strength));
            }
            end.playSound(null, 0, 64, 0, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 8.0F, 0.5F);
            end.playSound(null, 0, 64, 0, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 3.0F, 0.4F);
        }
        corruptGround(end, 8.0D + 52.0D * t / AWAKEN_TICKS, 6);
        if (t == AWAKEN_TICKS * 2 / 3) {
            end.playSound(null, 0, 64, 0, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 10.0F, 0.6F);
        }
        if (t >= AWAKEN_TICKS) {
            EnderDragon dragon = spawnDragon(end, fight, data);
            if (dragon != null) {
                spawnTurrets(end);
                data.set(EndArenaData.State.FIGHT);
            }
        }
    }

    /** La red se extiende: Piedra del End de la superficie dentro del radio pasa a corrupta, con glitches. */
    public static void corruptGround(ServerLevel end, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double a = end.random.nextDouble() * Math.PI * 2, r = Math.sqrt(end.random.nextDouble()) * radius;
            int x = Mth.floor(Math.cos(a) * r), z = Mth.floor(Math.sin(a) * r);
            if (!end.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;
            }
            BlockPos top = end.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(x, 0, z)).below();
            if (end.getBlockState(top).is(Blocks.END_STONE)) {
                end.setBlock(top, ModBlocks.CORRUPTED_END_STONE.get().defaultBlockState(), 3);
                end.sendParticles(ModParticles.DATA_GLITCH.get(), x + 0.5, top.getY() + 1.1, z + 0.5, 4, 0.4, 0.2, 0.4, 0.02);
            }
        }
    }

    // ------------------------------------------------------------------ torres rotas

    /** Rompe las torres cuyo chunk ya está cargado (una vez por torre y mundo). */
    public static void ruinTowers(ServerLevel end, EndArenaData data) {
        List<SpikeFeature.EndSpike> spikes = SpikeFeature.getSpikesForLevel(end);
        for (int i = 0; i < spikes.size(); i++) {
            if ((data.ruined & (1 << i)) != 0) {
                continue;
            }
            SpikeFeature.EndSpike s = spikes.get(i);
            int r = s.getRadius() + 2;
            if (!end.hasChunkAt(new BlockPos(s.getCenterX() - r, 0, s.getCenterZ() - r)) || !end.hasChunkAt(new BlockPos(s.getCenterX() + r, 0, s.getCenterZ() + r))
                    || !end.hasChunkAt(new BlockPos(s.getCenterX() - r, 0, s.getCenterZ() + r)) || !end.hasChunkAt(new BlockPos(s.getCenterX() + r, 0, s.getCenterZ() - r))) {
                continue;
            }
            ruinSpike(end, s, i);
            data.ruined |= 1 << i;
            data.setDirty();
        }
    }

    /** Corta la punta de una torre a una altura irregular, quita cristal y jaula y deja escombros al pie. */
    public static void ruinSpike(ServerLevel end, SpikeFeature.EndSpike s, int index) {
        int cx = s.getCenterX(), cz = s.getCenterZ(), r = s.getRadius() + 2;
        int cut = s.getHeight() - 9 - Math.floorMod(index * 7 + cx, 12);
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                int col = cut + Math.floorMod((x * 31 + z * 17) ^ index, 5) - 2;
                for (int y = col; y <= s.getHeight() + 12; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState st = end.getBlockState(p);
                    if (st.is(Blocks.OBSIDIAN) || st.is(Blocks.IRON_BARS) || st.is(Blocks.BEDROCK) || st.is(Blocks.FIRE)) {
                        end.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        for (var crystal : end.getEntitiesOfClass(net.minecraft.world.entity.boss.enderdragon.EndCrystal.class,
                new AABB(cx - r, cut - 4, cz - r, cx + r + 1, s.getHeight() + 14, cz + r + 1))) {
            crystal.discard();
        }
        var rand = net.minecraft.util.RandomSource.create(index * 9157L + cx);
        for (int i = 0; i < 14; i++) {                                         // escombros de obsidiana al pie
            double a = rand.nextDouble() * Math.PI * 2, d = s.getRadius() + 1 + rand.nextDouble() * 6;
            BlockPos ground = end.getHeightmapPos(Heightmap.Types.WORLD_SURFACE,
                    new BlockPos(cx + (int) Math.round(Math.cos(a) * d), 0, cz + (int) Math.round(Math.sin(a) * d)));
            if (ground.getY() > end.getMinBuildHeight() + 1 && end.getBlockState(ground.below()).is(Blocks.END_STONE)) {
                end.setBlock(ground, (rand.nextInt(4) == 0 ? Blocks.CRYING_OBSIDIAN : Blocks.OBSIDIAN).defaultBlockState(), 2);
            }
        }
    }

    /** Altura del bloque más alto de la torre rota (para poner la torreta encima). */
    public static int towerTop(ServerLevel end, SpikeFeature.EndSpike s) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(s.getCenterX(), s.getHeight() + 12, s.getCenterZ());
        while (p.getY() > end.getMinBuildHeight() && !end.getBlockState(p).is(Blocks.OBSIDIAN)) {
            p.move(0, -1, 0);
        }
        return p.getY();
    }

    // ------------------------------------------------------------------ Dragón y torretas

    @Nullable
    public static EnderDragon spawnDragon(ServerLevel end, EndDragonFight fight, EndArenaData data) {
        end.getChunkAt(new BlockPos(0, 128, 0));                              // como el vanilla: el chunk del centro cargado
        EnderDragon d = EntityType.ENDER_DRAGON.create(end);
        if (d == null) {
            return null;
        }
        corrupt(d);
        d.setDragonFight(fight);
        d.setFightOrigin(BlockPos.ZERO);
        d.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
        Vec3 from = emergePoint(end);
        d.moveTo(from.x, from.y, from.z, end.random.nextFloat() * 360.0F, 0.0F);   // sube desde el vacío junto a la isla
        end.addFreshEntity(d);
        fight.dragonKilled = false;
        fight.dragonUUID = d.getUUID();
        data.dragon = d.getUUID();
        data.reinforced = false;
        data.fighters.clear();
        data.setDirty();
        end.playSound(null, 0, 60, 0, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 16.0F, 0.8F);
        end.sendParticles(ModParticles.DATA_GLITCH.get(), 0, 60, 0, 200, 6, 6, 6, 0.1);
        return d;
    }

    /**
     * Dónde aparece: en el vacío junto al borde de la isla (una columna sin suelo a 70-140 bloques del centro), por debajo
     * del nivel de la isla, para que suba a la vista sin atravesar el suelo. Si no encuentra hueco, encima del centro.
     */
    public static Vec3 emergePoint(ServerLevel end) {
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4;
            for (int r = 70; r <= 140; r += 10) {                  // dentro de la zona que carga la pelea (9 chunks)
                BlockPos col = BlockPos.containing(Math.cos(a) * r, 0, Math.sin(a) * r);
                end.getChunkAt(col);
                if (end.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, col).getY() <= end.getMinBuildHeight() + 1) {
                    return new Vec3(col.getX() + 0.5, 30, col.getZ() + 0.5);
                }
            }
        }
        return new Vec3(0, 120, 0);
    }

    // ------------------------------------------------------------------ esquivar la isla y las torres

    /** Distancias (bloques) a las que mira por delante, y empuje hacia arriba por tick al ver un obstáculo. */
    private static final double[] LOOKAHEAD = {3.0, 6.0, 10.0};
    public static final double AVOID_LIFT = 0.25D, UNSTUCK_STEP = 1.0D;

    /**
     * El Dragón vanilla no choca con nada (atraviesa la piedra del End y la obsidiana). El modificado <b>esquiva</b>: si
     * alguna parte de su cuerpo (cabeza, cuello, cuerpo, alas, cola) va a meterse en bloques en los próximos metros de
     * vuelo, sube para pasar por encima; si ya está dentro, sale hacia arriba. No se aplica al aterrizar en el portal,
     * posado ni muriendo (ahí tiene que tocar el suelo).
     */
    @SubscribeEvent
    public static void steer(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof EnderDragon d) || !(d.level() instanceof ServerLevel level) || !isCorrupted(d)) {
            return;
        }
        var phase = d.getPhaseManager().getCurrentPhase();
        var type = phase.getPhase();
        if (phase.isSitting() || type == EnderDragonPhase.LANDING || type == EnderDragonPhase.DYING || type == EnderDragonPhase.HOVERING) {
            return;
        }
        List<AABB> body = new ArrayList<>();
        for (var part : d.getSubEntities()) {
            body.add(part.getBoundingBox());
        }
        Vec3 v = d.getDeltaMovement();
        if (obstructed(level, body, Vec3.ZERO)) {                             // ya dentro: sale por arriba
            d.setPos(d.getX(), d.getY() + UNSTUCK_STEP, d.getZ());
            d.setDeltaMovement(v.x * 0.5, Math.max(v.y, 0.3), v.z * 0.5);
            return;
        }
        Vec3 facing = new Vec3(Mth.sin(d.getYRot() * Mth.DEG_TO_RAD), 0, -Mth.cos(d.getYRot() * Mth.DEG_TO_RAD));
        Vec3 heading = facing.add(0, Mth.clamp(v.y * 2.0, -0.6, 0.6), 0).normalize();
        for (double dist : LOOKAHEAD) {
            if (obstructed(level, body, heading.scale(dist))) {
                d.setDeltaMovement(avoid(v, dist));
                return;
            }
        }
    }

    /** Velocidad para esquivar un obstáculo a {@code dist} bloques: frena en horizontal y sube (más cuanto más cerca). */
    public static Vec3 avoid(Vec3 v, double dist) {
        double lift = AVOID_LIFT * (dist <= 3.0 ? 1.6 : dist <= 6.0 ? 1.2 : 1.0);
        return new Vec3(v.x * 0.85, Math.max(v.y, 0.0) + lift, v.z * 0.85);
    }

    /** ¿Alguna de las cajas, desplazada {@code offset}, toca un bloque sólido? (no carga chunks) */
    public static boolean obstructed(ServerLevel level, List<AABB> boxes, Vec3 offset) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (AABB raw : boxes) {
            AABB b = raw.move(offset).deflate(0.15);
            for (int x = Mth.floor(b.minX); x <= Mth.floor(b.maxX); x++) {
                for (int z = Mth.floor(b.minZ); z <= Mth.floor(b.maxZ); z++) {
                    if (!level.hasChunkAt(p.set(x, 0, z))) {
                        continue;
                    }
                    for (int y = Mth.floor(b.minY); y <= Mth.floor(b.maxY); y++) {
                        p.set(x, y, z);
                        BlockState st = level.getBlockState(p);
                        if (!st.isAir() && !st.getCollisionShape(level, p).isEmpty()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /** Marca y mejora un Dragón (vida y nombre). */
    public static void corrupt(EnderDragon d) {
        d.getPersistentData().putBoolean(CORRUPTED, true);
        d.getAttribute(Attributes.MAX_HEALTH).setBaseValue(DRAGON_HEALTH);
        d.setHealth(DRAGON_HEALTH);
        d.setCustomName(Component.translatable("entity.tecnocinetico.corrupted_dragon"));
    }

    /** Una Torreta-repetidor en lo alto de cada torre rota. */
    public static int spawnTurrets(ServerLevel end) {
        int n = 0;
        for (SpikeFeature.EndSpike s : SpikeFeature.getSpikesForLevel(end)) {
            if (spawnTurret(end, s)) {
                n++;
            }
        }
        return n;
    }

    public static boolean spawnTurret(ServerLevel end, SpikeFeature.EndSpike s) {
        int top = towerTop(end, s);
        if (top <= end.getMinBuildHeight()) {
            return false;
        }
        EndRelayTurretEntity t = ModEntities.END_RELAY_TURRET.get().create(end);
        if (t == null) {
            return false;
        }
        t.moveTo(s.getCenterX() + 0.5, top + 1, s.getCenterZ() + 0.5, 0, 0);
        end.addFreshEntity(t);
        end.sendParticles(ModParticles.DATA_GLITCH.get(), t.getX(), t.getY() + 0.8, t.getZ(), 40, 0.6, 0.8, 0.6, 0.05);
        return true;
    }

    @Nullable
    public static EnderDragon dragon(ServerLevel end) {
        EndArenaData data = EndArenaData.get(end);
        return data.dragon != null && end.getEntity(data.dragon) instanceof EnderDragon d ? d : null;
    }

    // ------------------------------------------------------------------ pelea

    private static void fight(ServerLevel end, EndDragonFight fight, EndArenaData data) {
        EnderDragon d = dragon(end);
        if (d == null) {
            if (fight.dragonKilled && end.getDragons().isEmpty()) {           // terminó la animación de muerte
                defeated(end, data);
            }
            return;
        }
        boolean dying = d.getPhaseManager().getCurrentPhase().getPhase() == EnderDragonPhase.DYING;
        if (dying) {
            return;
        }
        long t = end.getGameTime();
        // Pulso EMP al posarse
        boolean sitting = d.getPhaseManager().getCurrentPhase().isSitting();
        if (sitting && !d.getPersistentData().getBoolean(LAST_SITTING)) {
            empPulse(end, d.position());
        }
        d.getPersistentData().putBoolean(LAST_SITTING, sitting);
        // Lluvia de datos desde el aire
        if (!sitting && t % RAIN_EVERY == 0) {
            dataRain(end, data, t);
        }
        tickStrikes(end, data, t);
        // refuerzos a mitad de vida
        if (!data.reinforced && d.getHealth() <= d.getMaxHealth() * 0.5F) {
            reinforce(end, data, d);
        }
    }

    /** Onda del aterrizaje: empuja, daña algo y apaga los gadgets (salvo Mk III) unos segundos. */
    public static void empPulse(ServerLevel end, Vec3 at) {
        end.playSound(null, at.x, at.y, at.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 4.0F, 0.6F);
        com.bryanamaya.tecnocinetico.util.ParticleShapes.disk(end, ModParticles.DATA_GLITCH.get(), at.add(0, 0.5, 0), 4, 4, 1, 48, 2, 0.5, 0.1, 0.5, 0.2);
        for (Player p : end.getEntitiesOfClass(Player.class, new AABB(at, at).inflate(EMP_RADIUS),
                p -> !p.isSpectator() && !p.isCreative() && p.distanceToSqr(at) < EMP_RADIUS * EMP_RADIUS)) {
            p.addEffect(new MobEffectInstance(ModEffects.EMP.get(), EMP_TICKS, 0));
            Vec3 push = p.position().subtract(at).multiply(1, 0, 1).normalize().scale(1.3);
            p.push(push.x, 0.45, push.z);
            p.hurtMarked = true;
            p.hurt(end.damageSources().magic(), 4.0F);
        }
    }

    /** Elige hasta 3 jugadores y marca el suelo bajo ellos (aviso); los impactos llegan en {@link #RAIN_WARNING} ticks. */
    public static void dataRain(ServerLevel end, EndArenaData data, long now) {
        List<ServerPlayer> players = new ArrayList<>(arenaPlayers(end));
        players.removeIf(Player::isCreative);
        java.util.Collections.shuffle(players, new java.util.Random(end.random.nextLong()));
        for (int i = 0; i < Math.min(3, players.size()); i++) {
            BlockPos at = players.get(i).blockPosition();
            data.strikes.add(new EndArenaData.Strike(at, now + RAIN_WARNING));
            end.playSound(null, at, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.0F, 1.8F);
        }
    }

    public static void tickStrikes(ServerLevel end, EndArenaData data, long now) {
        var it = data.strikes.iterator();
        while (it.hasNext()) {
            var s = it.next();
            Vec3 c = Vec3.atBottomCenterOf(s.at());
            if (now < s.when()) {
                if (now % 4 == 0) {                                           // círculo de aviso en el suelo
                    com.bryanamaya.tecnocinetico.util.ParticleShapes.ring(end, ModParticles.DATA_GLITCH.get(), c.add(0, 0.1, 0), RAIN_RADIUS, 16);
                    double fall = (s.when() - now) * 0.8;                     // el proyectil cayendo
                    end.sendParticles(ModParticles.DATA_GLITCH.get(), c.x, c.y + 2 + fall, c.z, 6, 0.15, 0.4, 0.15, 0.0);
                }
                continue;
            }
            it.remove();
            end.sendParticles(ModParticles.DATA_GLITCH.get(), c.x, c.y + 0.5, c.z, 60, RAIN_RADIUS * 0.4, 0.5, RAIN_RADIUS * 0.4, 0.15);
            end.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.4F, 1.4F);
            for (LivingEntity e : end.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(RAIN_RADIUS, 2.5, RAIN_RADIUS),
                    e -> !(e instanceof EnderDragon) && !(e instanceof EndRelayTurretEntity) && !e.isSpectator()
                            && e.position().distanceToSqr(c) < RAIN_RADIUS * RAIN_RADIUS + 4)) {
                if (e instanceof Player p && p.isCreative()) {
                    continue;
                }
                e.hurt(end.damageSources().explosion(null, null), RAIN_DAMAGE);   // la armadura (y Protección contra explosiones) cuenta
                e.push(0, 0.5, 0);
                e.hurtMarked = true;
            }
        }
    }

    /** A mitad de vida: endermen corruptos junto a los jugadores y dos torretas reparadas. */
    public static void reinforce(ServerLevel end, EndArenaData data, EnderDragon d) {
        data.reinforced = true;
        data.setDirty();
        end.playSound(null, d.getX(), d.getY(), d.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 12.0F, 0.5F);
        int spawned = 0;
        for (ServerPlayer p : arenaPlayers(end)) {
            p.displayClientMessage(Component.translatable("message.tecnocinetico.end_reinforcements").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            for (int i = 0; i < 2 && spawned < 6; i++) {
                EnderMan e = EntityType.ENDERMAN.create(end);
                if (e != null && e.randomTeleport(p.getX() + end.random.nextInt(9) - 4, p.getY(), p.getZ() + end.random.nextInt(9) - 4, true)) {
                    end.addFreshEntity(e);
                    e.setTarget(p);
                    EndMobEvents.glitch(end, e, 20);
                    spawned++;
                }
            }
        }
        int repaired = 0;
        for (SpikeFeature.EndSpike s : SpikeFeature.getSpikesForLevel(end)) {
            if (repaired >= 2) {
                break;
            }
            boolean alive = !end.getEntitiesOfClass(EndRelayTurretEntity.class,
                    new AABB(s.getCenterX() - 4, end.getMinBuildHeight(), s.getCenterZ() - 4, s.getCenterX() + 5, end.getMaxBuildHeight(), s.getCenterZ() + 5)).isEmpty();
            if (!alive && spawnTurret(end, s)) {
                repaired++;
            }
        }
    }

    // ------------------------------------------------------------------ derrota y botín

    private static void defeated(ServerLevel end, EndArenaData data) {
        BlockPos top = end.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, BlockPos.ZERO);
        dropLoot(end, Vec3.atBottomCenterOf(top).add(0, 1, 0), data.fighters.size());
        for (EndRelayTurretEntity t : end.getEntitiesOfClass(EndRelayTurretEntity.class, new AABB(BlockPos.ZERO).inflate(ARENA_RADIUS, 256, ARENA_RADIUS))) {
            EndMobEvents.glitch(end, t, 30);
            t.discard();                                                      // la red se apaga
        }
        data.set(EndArenaData.State.DEFEATED);
        for (ServerPlayer p : end.players()) {
            p.displayClientMessage(Component.translatable("message.tecnocinetico.end_defeated").withStyle(ChatFormatting.GOLD), false);
        }
    }

    /** Botín del Dragón modificado: por cada jugador que luchó (1-4) un Acoplador, un Núcleo de Datos y un Núcleo de Navegación, y 16 de nulita. */
    public static List<ItemEntity> dropLoot(ServerLevel end, Vec3 at, int fighters) {
        int n = Math.max(1, Math.min(4, fighters));
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            stacks.add(new ItemStack(ModItems.END_COUPLER.get()));
            stacks.add(new ItemStack(ModItems.DATA_CORE.get()));
            stacks.add(new ItemStack(ModItems.NAVIGATION_CORE.get()));
            stacks.add(new ItemStack(ModItems.NULLITE_INGOT.get(), 16));
        }
        List<ItemEntity> out = new ArrayList<>();
        for (ItemStack s : stacks) {
            ItemEntity item = new ItemEntity(end, at.x, at.y, at.z, s, end.random.nextGaussian() * 0.05, 0.2, end.random.nextGaussian() * 0.05);
            item.setInvulnerable(true);
            item.setUnlimitedLifetime();
            end.addFreshEntity(item);
            out.add(item);
        }
        return out;
    }

    // ------------------------------------------------------------------ eventos del Dragón

    /** Mientras duerme no hay Dragón vanilla; durante y después del evento todo Dragón está modificado. */
    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getLevel() instanceof ServerLevel end) || end.dimension() != Level.END) {
            return;
        }
        if (event.getEntity() instanceof EnderDragon d && !isCorrupted(d)) {
            EndArenaData data = EndArenaData.get(end);
            if (data.state == EndArenaData.State.DORMANT || data.state == EndArenaData.State.AWAKENING) {
                event.setCanceled(true);
            } else {
                corrupt(d);                                                   // p. ej. reaparecido con cristales
            }
        } else if (event.getEntity() instanceof AreaEffectCloud cloud && cloud.getOwner() != null && isCorrupted(cloud.getOwner())) {
            cloud.setParticle(ModParticles.DATA_GLITCH.get());               // aliento corrupto
            cloud.addEffect(new MobEffectInstance(ModEffects.DATA_CORRUPTION.get(), CORRUPTION_TICKS, 0));
        }
    }

    /** Dureza en grupo (como los jefes del Nether) y registro de quién luchó. */
    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (!isCorrupted(event.getEntity()) || !(event.getEntity().level() instanceof ServerLevel end)) {
            return;
        }
        int players = Math.max(1, arenaPlayers(end).size());
        event.setAmount(event.getAmount() / Math.min(3.0F, 1.0F + 0.5F * (players - 1)));
        if (event.getSource().getEntity() instanceof Player p) {
            EndArenaData data = EndArenaData.get(end);
            if (!data.fighters.contains(p.getUUID())) {
                data.fighters.add(p.getUUID());
                data.setDirty();
            }
        }
    }

    /** El Filtro de Ceniza Mk III, encendido y con batería, filtra la corrupción de datos. */
    @SubscribeEvent
    public static void filterCorruption(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect() == ModEffects.DATA_CORRUPTION.get() && event.getEntity() instanceof Player p
                && TacticalGear.of(p).map(g -> g.active(p, TacticalGear.FACE)).filter(s -> s.is(ModItems.ASH_FILTER.get())
                && NetherGadgetItem.level(s) >= 3 && com.bryanamaya.tecnocinetico.gear.GadgetItem.hasCharge(s)).isPresent()) {
            event.setResult(Event.Result.DENY);
        }
    }

    /** Para las pruebas: vuelve al estado inicial (sin tocar las torres). */
    public static void reset(ServerLevel end) {
        EndArenaData data = EndArenaData.get(end);
        data.set(EndArenaData.State.DORMANT);
        data.dragon = null;
        data.strikes.clear();
        data.fighters.clear();
        data.reinforced = false;
    }

    public static UUID dragonId(ServerLevel end) {
        return EndArenaData.get(end).dragon;
    }
}
