package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Banco de rendimiento (solo con la variable de entorno {@code TECNO_BENCH=1}; si no, pasa sin hacer nada): mide cuánto
 * cuesta un tick de cada mob del mod (16 a la vez, con un jugador cerca para que su IA trabaje) y de cada máquina, y
 * lo compara con mobs vanilla. Escribe la tabla en el log ("[Banco]") ordenada de más cara a más barata.
 */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class BenchmarkGameTests {

    private static final int COUNT = 16, WARMUP = 20, TICKS = 100, BE_TICKS = 2000;

    private static boolean enabled() {
        return "1".equals(System.getenv("TECNO_BENCH"));
    }

    private record Result(String name, double micros) {}

    /** Mobs cuyo tick se muestrea (pila del hilo del servidor cada ~1 ms) para ver dónde se va el tiempo. */
    private static final java.util.Set<String> SAMPLED = java.util.Set.of("slag_devourer", "anchor", "siege_spider", "swarm_matriarch");

    private static final class Sampler extends Thread {
        private final Thread target;
        private volatile boolean running = true;
        private final java.util.Map<String, Integer> mod = new java.util.HashMap<>(), top = new java.util.HashMap<>();
        private int samples;

        private Sampler(Thread target) {
            this.target = target;
            setDaemon(true);
        }

        static Sampler start(Thread target) {
            Sampler s = new Sampler(target);
            s.start();
            return s;
        }

        @Override
        public void run() {
            while (running) {
                for (StackTraceElement[] st : stacks()) {
                    sample(st);
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    return;
                }
            }
        }

        /** El hilo del servidor o, si es null, los trabajadores de generación ("Worker-Main"). */
        private java.util.List<StackTraceElement[]> stacks() {
            if (target != null) {
                return java.util.Collections.singletonList(target.getStackTrace());
            }
            java.util.List<StackTraceElement[]> out = new java.util.ArrayList<>();
            for (var e : Thread.getAllStackTraces().entrySet()) {
                StackTraceElement[] st = e.getValue();
                if (e.getKey().getName().startsWith("Worker-Main") && st.length > 0 && !st[0].getMethodName().equals("park")) {
                    out.add(st);                                                  // solo los que trabajan
                }
            }
            return out;
        }

        private void sample(StackTraceElement[] st) {
            {
                if (st.length > 0) {
                    samples++;
                    top.merge(st[0].getClassName() + "." + st[0].getMethodName(), 1, Integer::sum);
                    for (StackTraceElement e : st) {                              // el marco del mod más profundo
                        if (e.getClassName().startsWith("com.bryanamaya") && !e.getClassName().contains("gametest")) {
                            mod.merge(e.getClassName().replace("com.bryanamaya.tecnocinetico.", "") + "." + e.getMethodName() + ":" + e.getLineNumber(), 1, Integer::sum);
                            break;
                        }
                    }
                }
            }
        }

        void stopAndLog(String id) {
            running = false;
            try {
                join(200);
            } catch (InterruptedException ignored) {
            }
            Tecnocinetico.LOGGER.info("[Banco] --- muestras de {} ({}): marco del mod más profundo ---", id, samples);
            mod.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(12)
                    .forEach(e -> Tecnocinetico.LOGGER.info("[Banco]   {}% {}", e.getValue() * 100 / Math.max(1, samples), e.getKey()));
            Tecnocinetico.LOGGER.info("[Banco] --- {}: cima de la pila ---", id);
            top.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(6)
                    .forEach(e -> Tecnocinetico.LOGGER.info("[Banco]   {}% {}", e.getValue() * 100 / Math.max(1, samples), e.getKey()));
        }
    }

    @GameTest(template = "empty", batch = "banco_rendimiento", timeoutTicks = 100)
    public static void benchmarkMobsAndMachines(GameTestHelper h) {
        if (!enabled()) {
            h.succeed();
            return;
        }
        ServerLevel level = h.getLevel();
        BlockPos base = h.absolutePos(new BlockPos(1, 2, 1)).above(40);
        // suelo y caja de cristal para que no se vayan
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-8, -1, -8), base.offset(8, 6, 8))) {
            boolean wall = Math.abs(p.getX() - base.getX()) == 8 || Math.abs(p.getZ() - base.getZ()) == 8 || p.getY() == base.getY() - 1
                    || p.getY() == base.getY() + 6;
            level.setBlock(p, wall ? Blocks.GLASS.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
        }
        FakePlayer player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "banco"));
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
        level.addNewPlayer(player);
        List<Result> mobs = new ArrayList<>();
        try {
            List<EntityType<?>> types = new ArrayList<>();
            for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
                if (BuiltInRegistries.ENTITY_TYPE.getKey(t).getNamespace().equals(Tecnocinetico.MOD_ID)) {
                    types.add(t);
                }
            }
            types.add(EntityType.ZOMBIE);                                         // referencias vanilla
            types.add(EntityType.SKELETON);
            types.add(EntityType.ENDERMAN);
            types.add(EntityType.PIGLIN);
            for (EntityType<?> t : types) {
                List<Mob> list = new ArrayList<>();
                for (int i = 0; i < COUNT; i++) {
                    Entity e = t.create(level);
                    if (!(e instanceof Mob mob)) {
                        if (e != null) {
                            e.discard();
                        }
                        break;
                    }
                    mob.moveTo(base.getX() - 6 + (i % 4) * 4 + 0.5, base.getY(), base.getZ() - 6 + (i / 4) * 4 + 0.5, 0, 0);
                    mob.finalizeSpawn(level, level.getCurrentDifficultyAt(base), MobSpawnType.SPAWN_EGG, null, null);
                    level.addFreshEntity(mob);
                    list.add(mob);
                }
                if (list.isEmpty()) {
                    continue;
                }
                for (int k = 0; k < WARMUP; k++) {
                    list.forEach(Entity::tick);
                }
                String id = BuiltInRegistries.ENTITY_TYPE.getKey(t).getPath();
                boolean sample = SAMPLED.contains(id);
                Sampler sampler = sample ? Sampler.start(Thread.currentThread()) : null;
                long t0 = System.nanoTime();
                for (int k = 0; k < TICKS; k++) {
                    for (Mob m : list) {
                        if (!m.isRemoved()) {
                            m.tick();
                        }
                    }
                }
                double us = (System.nanoTime() - t0) / 1000.0 / (TICKS * (double) list.size());
                if (sampler != null) {
                    sampler.stopAndLog(id);
                }
                mobs.add(new Result(BuiltInRegistries.ENTITY_TYPE.getKey(t).toString(), us));
                list.forEach(Entity::discard);
                level.getEntitiesOfClass(Entity.class, new net.minecraft.world.phys.AABB(base).inflate(40),
                        e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);   // proyectiles, crías...
            }
        } finally {
            player.discard();
        }
        // máquinas: el ticker de cada entidad de bloque del mod, en reposo y con energía
        List<Result> machines = new ArrayList<>();
        BlockPos at = h.absolutePos(new BlockPos(1, 2, 1));
        for (Block b : BuiltInRegistries.BLOCK) {
            if (!BuiltInRegistries.BLOCK.getKey(b).getNamespace().equals(Tecnocinetico.MOD_ID) || !(b instanceof EntityBlock eb)) {
                continue;
            }
            var state = b.defaultBlockState();
            level.setBlock(at, state, 2);
            BlockEntity be = level.getBlockEntity(at);
            if (be == null) {
                continue;
            }
            @SuppressWarnings("unchecked")
            BlockEntityTicker<BlockEntity> ticker = (BlockEntityTicker<BlockEntity>) eb.getTicker(level, state, be.getType());
            if (ticker == null) {
                continue;
            }
            be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY).ifPresent(e -> e.receiveEnergy(Integer.MAX_VALUE, false));
            try {
                for (int k = 0; k < 50; k++) {
                    ticker.tick(level, at, level.getBlockState(at), be);
                }
                long t0 = System.nanoTime();
                for (int k = 0; k < BE_TICKS; k++) {
                    ticker.tick(level, at, level.getBlockState(at), be);
                }
                machines.add(new Result(BuiltInRegistries.BLOCK.getKey(b).toString(), (System.nanoTime() - t0) / 1000.0 / BE_TICKS));
            } catch (RuntimeException ex) {
                machines.add(new Result(BuiltInRegistries.BLOCK.getKey(b) + " (error: " + ex + ")", -1));
            }
            level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
        }
        mobs.sort((a, b) -> Double.compare(b.micros, a.micros));
        machines.sort((a, b) -> Double.compare(b.micros, a.micros));
        Tecnocinetico.LOGGER.info("[Banco] ===== mobs: microsegundos por tick y por mob ({} a la vez) =====", COUNT);
        mobs.forEach(r -> Tecnocinetico.LOGGER.info("[Banco] {} {}", String.format("%9.2f", r.micros), r.name));
        Tecnocinetico.LOGGER.info("[Banco] ===== máquinas: microsegundos por tick =====");
        machines.forEach(r -> Tecnocinetico.LOGGER.info("[Banco] {} {}", String.format("%9.2f", r.micros), r.name));
        h.succeed();
    }

    /** Generación: carga chunks nuevos en cada dimensión (en el hilo del servidor) y muestrea dónde se va el tiempo. */
    @GameTest(template = "empty", batch = "banco_generacion", timeoutTicks = 100)
    public static void benchmarkWorldgen(GameTestHelper h) {
        if (!enabled()) {
            h.succeed();
            return;
        }
        var server = h.getLevel().getServer();
        for (var key : List.of(net.minecraft.world.level.Level.OVERWORLD, net.minecraft.world.level.Level.NETHER, net.minecraft.world.level.Level.END)) {
            ServerLevel level = server.getLevel(key);
            int base = 20000 + level.getRandom().nextInt(4000) * 64;          // zona nueva cada vez
            int side = 8;
            Sampler sampler = Sampler.start(null);
            long t0 = System.nanoTime();
            for (int x = 0; x < side; x++) {
                for (int z = 0; z < side; z++) {
                    level.getChunk((base >> 4) + x, (base >> 4) + z);
                }
            }
            double ms = (System.nanoTime() - t0) / 1e6 / (side * side);
            Tecnocinetico.LOGGER.info("[Banco] generación {}: {} ms por chunk ({} chunks)", key.location(), String.format("%.2f", ms), side * side);
            sampler.stopAndLog("gen " + key.location().getPath());
        }
        h.succeed();
    }
}
