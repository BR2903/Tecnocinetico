package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Pruebas automáticas (gradlew runGameTestServer). Generan de verdad las estructuras del mod en el mundo
 * del servidor de prueba (el Overworld del mod), lejos del área de tests, para detectar fallos de código antes de que el
 * usuario cree un mundo. No comprueban el aspecto (eso se mira en el juego).
 */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class WorldgenGameTests {

    private static final String[] STRUCTURES = {"ruined_building_megacity", "ruined_building_urban",
            "ruined_building_overgrown", "ruined_building_frozen", "survivor_settlement", "abandoned_factory", "swarm_hive", "ai_installation"};

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void structuresGenerate(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ChunkGenerator gen = level.getChunkSource().getGenerator();
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(2000, 0, 2000);

        for (int i = 0; i < STRUCTURES.length; i++) {
            ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE,
                    ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, STRUCTURES[i]));
            Structure structure = registry.get(key);
            helper.assertTrue(structure != null, "No existe la estructura " + key.location());
            // el mundo de prueba es el Overworld del mod con todos los biomas: se simula un bioma válido de la
            // estructura y se prueban chunks hasta encontrar terreno donde quepa (llano y sin agua), como en el juego
            var biome = structure.biomes().stream().findFirst().orElseThrow();
            StructureStart start = StructureStart.INVALID_START;
            for (int tries = 0; tries < 64 && !start.isValid(); tries++) {
                ChunkPos cp = new ChunkPos(base.offset(i * 1024 + (tries % 8) * 48, 0, (tries / 8) * 48));
                start = structure.generate(level.registryAccess(), gen, new FixedBiomeSource(biome),
                        level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(), cp, 0, level,
                        b -> true);
            }
            helper.assertTrue(start.isValid(), "No se pudo colocar " + key.location() + " en 64 intentos");
            StructureStart placed = start;
            BoundingBox bb = start.getBoundingBox();
            ChunkPos.rangeClosed(new ChunkPos(bb.minX() >> 4, bb.minZ() >> 4), new ChunkPos(bb.maxX() >> 4, bb.maxZ() >> 4))
                    .forEach(c -> placed.placeInChunk(level, level.structureManager(), gen, level.getRandom(),
                            new BoundingBox(c.getMinBlockX(), level.getMinBuildHeight(), c.getMinBlockZ(),
                                    c.getMaxBlockX(), level.getMaxBuildHeight(), c.getMaxBlockZ()), c));
            Tecnocinetico.LOGGER.info("[GameTest] {} generado en {}", key.location(), bb);
            dump(level, placed.getPieces().get(0).getBoundingBox().inflatedBy(2), STRUCTURES[i]);
            if (STRUCTURES[i].equals("survivor_settlement")) {
                var survivors = level.getEntitiesOfClass(com.bryanamaya.tecnocinetico.entity.custom.SurvivorEntity.class,
                        net.minecraft.world.phys.AABB.of(placed.getPieces().get(0).getBoundingBox()));
                Tecnocinetico.LOGGER.info("[GameTest] supervivientes en el asentamiento: {}", survivors.stream()
                        .map(s -> s.getRole().id + " (" + s.getOffers().size() + " ofertas)").toList());
                helper.assertTrue(!survivors.isEmpty(), "El asentamiento no tiene supervivientes");
            }
        }
        helper.succeed();
    }

    /**
     * El enjambre ataca de verdad: Nodo + 4 aguijones en el aire, un cerdo inmóvil como objetivo forzado.
     * Tras 10 s el cerdo debe estar herido o muerto (prueba el bucle de vuelo, turnos de ataque y picadura).
     */
    /**
     * Dron del Núcleo: se le asigna un cerdo como objetivo; en 10 s debe haberlo herido con sus ráfagas
     * (prueba el vuelo, la carga con aviso y el disparo con línea de visión).
     */
    /**
     * Corruptos: al aparecer un zombi, un esqueleto y una araña "naturales" en el Overworld, Forge debe cancelarlos
     * y en su lugar deben quedar un Corrupto, un Tirador Corrupto y una Araña Corrupta.
     */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void corruptedReplaceVanilla(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos col = helper.absolutePos(BlockPos.ZERO);
        level.getChunk(col);
        BlockPos base = new BlockPos(col.getX(), level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                col.getX(), col.getZ()), col.getZ());
        net.minecraft.world.entity.EntityType<?>[] vanilla = {net.minecraft.world.entity.EntityType.ZOMBIE,
                net.minecraft.world.entity.EntityType.SKELETON, net.minecraft.world.entity.EntityType.SPIDER,
                net.minecraft.world.entity.EntityType.CAVE_SPIDER, net.minecraft.world.entity.EntityType.DROWNED,
                net.minecraft.world.entity.EntityType.CREEPER};
        for (int i = 0; i < vanilla.length; i++) {
            vanilla[i].spawn(level, base.offset(i * 3, 0, 0), net.minecraft.world.entity.MobSpawnType.NATURAL);
        }
        helper.runAfterDelay(5, () -> {
            // solo junto a los 3 puntos de aparición (las otras pruebas corren a la vez y muy cerca)
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(base, base.offset(16, 1, 1)).inflate(0.5, 1.0, 0.5);
            var all = level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, box,
                    m -> !(m instanceof net.minecraft.world.entity.animal.Animal));
            long vanillaLeft = all.stream().filter(m -> java.util.Arrays.asList(vanilla).contains(m.getType())).count();
            long corrupted = all.stream().filter(m -> m.getType().getDescriptionId().startsWith("entity.tecnocinetico.corrupted")).count();
            Tecnocinetico.LOGGER.info("[GameTest] corruptos: {} vanilla restantes, {} corruptos: {}", vanillaLeft, corrupted,
                    all.stream().map(m -> m.getType().getDescriptionId() + (m.getMainHandItem().isEmpty() ? "" : "[" + m.getMainHandItem().getItem() + "]")).toList());
            all.forEach(net.minecraft.world.entity.Entity::discard);
            helper.assertTrue(vanillaLeft == 0, "Quedaron mobs vanilla sin sustituir");
            helper.assertTrue(corrupted == vanilla.length, "No aparecieron los " + vanilla.length + " corruptos (" + corrupted + ")");
            helper.succeed();
        });
    }

    /** Tirador Corrupto con pistola: en 10 s debe herir a un cerdo a 8 bloques. */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void gunnerShootsTarget(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos col = helper.absolutePos(BlockPos.ZERO);
        level.getChunk(col);
        BlockPos base = new BlockPos(col.getX(), level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                col.getX(), col.getZ()) + 8, col.getZ());
        var gunner = com.bryanamaya.tecnocinetico.entity.ModEntities.CORRUPTED_GUNNER.get().create(level);
        gunner.moveTo(base.getX() + 0.5D, base.getY(), base.getZ() + 0.5D, 0, 0);
        gunner.finalizeSpawn(level, level.getCurrentDifficultyAt(base), net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null, null);
        gunner.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                new net.minecraft.world.item.ItemStack(com.bryanamaya.tecnocinetico.item.ModItems.KINETIC_PISTOL.get()));
        gunner.setNoGravity(true);
        level.addFreshEntity(gunner);
        var pig = net.minecraft.world.entity.EntityType.PIG.create(level);
        pig.moveTo(base.getX() + 8.5D, base.getY(), base.getZ() + 0.5D, 0, 0);
        pig.setNoAi(true);
        pig.setNoGravity(true);
        level.addFreshEntity(pig);
        gunner.setTarget(pig);
        helper.runAfterDelay(200, () -> {
            boolean hit = (!pig.isAlive() || pig.getHealth() < pig.getMaxHealth())
                    && pig.getLastHurtByMob() instanceof com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedGunnerEntity;
            Tecnocinetico.LOGGER.info("[GameTest] tirador: cerdo {} (vida {}), tirador en {}", hit ? "atacado" : "INTACTO",
                    pig.getHealth(), gunner.blockPosition());
            gunner.discard();
            pig.discard();
            helper.assertTrue(hit, "El tirador no disparó al objetivo");
            helper.succeed();
        });
    }

    /** Anclador: un cerdo en el aire debe recibir el Lastre de Inercia (haz de 1 s) en menos de 5 s. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void anchorGroundsFlyer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos col = helper.absolutePos(BlockPos.ZERO);
        level.getChunk(col);
        int ground = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, col.getX(), col.getZ());
        var anchor = com.bryanamaya.tecnocinetico.entity.ModEntities.ANCHOR.get().create(level);
        anchor.moveTo(col.getX() + 0.5D, ground, col.getZ() + 0.5D, 0, 0);
        level.addFreshEntity(anchor);
        var pig = net.minecraft.world.entity.EntityType.PIG.create(level);
        pig.moveTo(col.getX() + 6.5D, ground + 7, col.getZ() + 0.5D, 0, 0);
        pig.setNoAi(true);
        pig.setNoGravity(true);
        level.addFreshEntity(pig);
        anchor.setTarget(pig);
        var seen = new java.util.concurrent.atomic.AtomicBoolean();               // el efecto dura 3 s: vigilar cada tick
        helper.onEachTick(() -> {
            if (pig.hasEffect(com.bryanamaya.tecnocinetico.effect.ModEffects.INERTIA_ANCHOR.get())) {
                seen.set(true);
            }
        });
        helper.runAfterDelay(100, () -> {
            boolean anchored = seen.get()
                    && pig.getLastHurtByMob() instanceof com.bryanamaya.tecnocinetico.entity.custom.AnchorEntity;
            Tecnocinetico.LOGGER.info("[GameTest] anclador: cerdo {} (vida {})", anchored ? "anclado" : "LIBRE", pig.getHealth());
            anchor.discard();
            pig.discard();
            helper.assertTrue(anchored, "El Anclador no ancló al objetivo en el aire");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void droneShootsTarget(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos col = helper.absolutePos(BlockPos.ZERO);
        level.getChunk(col);
        BlockPos base = new BlockPos(col.getX(), level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                col.getX(), col.getZ()) + 10, col.getZ());
        var drone = com.bryanamaya.tecnocinetico.entity.ModEntities.DRONE.get().create(level);
        drone.moveTo(base.getX() + 0.5D, base.getY() + 3, base.getZ() + 0.5D, 0, 0);
        drone.finalizeSpawn(level, level.getCurrentDifficultyAt(base), net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null, null);
        level.addFreshEntity(drone);
        var pig = net.minecraft.world.entity.EntityType.PIG.create(level);
        pig.moveTo(base.getX() + 6.5D, base.getY(), base.getZ() + 0.5D, 0, 0);
        pig.setNoAi(true);
        pig.setNoGravity(true);
        level.addFreshEntity(pig);
        drone.engage(pig);
        helper.runAfterDelay(200, () -> {
            boolean hit = (!pig.isAlive() || pig.getHealth() < pig.getMaxHealth())
                    && pig.getLastHurtByMob() instanceof com.bryanamaya.tecnocinetico.entity.custom.DroneEntity;
            boolean alive = drone.isAlive();
            Tecnocinetico.LOGGER.info("[GameTest] dron: cerdo {} (vida {}), dron vivo={} vida={} pos={}",
                    hit ? "atacado" : "INTACTO", pig.getHealth(), alive, drone.getHealth(), drone.blockPosition());
            drone.discard();
            pig.discard();
            helper.assertTrue(hit, "El dron no disparó al objetivo");
            helper.assertTrue(alive, "El dron murió sin que nadie lo atacara");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void swarmAttacksTarget(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // al aire libre: 12 bloques por encima del terreno (si no, mueren asfixiadas y la prueba miente)
        BlockPos col = helper.absolutePos(BlockPos.ZERO);                 // zona del test: chunk activo
        level.getChunk(col);
        BlockPos base = new BlockPos(col.getX(), level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                col.getX(), col.getZ()) + 12, col.getZ());
        var node = com.bryanamaya.tecnocinetico.entity.ModEntities.HIVE_NODE.get().create(level);
        node.moveTo(base.getX() + 0.5D, base.getY(), base.getZ() + 0.5D, 0, 0);
        node.finalizeSpawn(level, level.getCurrentDifficultyAt(base), net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null, null);
        level.addFreshEntity(node);
        java.util.List<net.minecraft.world.entity.Entity> spawned = new java.util.ArrayList<>(java.util.List.of(node));
        for (int i = 0; i < 4; i++) {
            var w = com.bryanamaya.tecnocinetico.entity.ModEntities.SWARM_WASP.get().create(level);
            w.moveTo(base.getX() + 0.5D + i, base.getY() + 1.5D, base.getZ() + 0.5D, 0, 0);
            w.finalizeSpawn(level, level.getCurrentDifficultyAt(base), net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null, null);
            w.joinHive(node, com.bryanamaya.tecnocinetico.entity.swarm.WaspRole.STINGER);
            level.addFreshEntity(w);
            spawned.add(w);
        }
        var pig = net.minecraft.world.entity.EntityType.PIG.create(level);
        pig.moveTo(base.getX() + 8.5D, base.getY(), base.getZ() + 0.5D, 0, 0);
        pig.setNoAi(true);
        pig.setNoGravity(true);
        level.addFreshEntity(pig);
        spawned.add(pig);
        node.mind().forceTarget(pig);
        helper.runAfterDelay(200, () -> {
            boolean hit = (!pig.isAlive() || pig.getHealth() < pig.getMaxHealth())
                    && pig.getLastHurtByMob() instanceof com.bryanamaya.tecnocinetico.entity.swarm.SwarmWaspEntity;
            Tecnocinetico.LOGGER.info("[GameTest] enjambre: cerdo {} (vida {}), avispas vivas {}", hit ? "atacado" : "INTACTO",
                    pig.getHealth(), node.mind().members().size());
            spawned.forEach(e -> Tecnocinetico.LOGGER.info("[GameTest]   {} vivo={} vida={} pos={}",
                    e.getType().getDescriptionId(), e.isAlive(), e instanceof net.minecraft.world.entity.LivingEntity le ? le.getHealth() : 0,
                    e.blockPosition()));
            boolean waspsAlive = spawned.stream().skip(1).limit(4).allMatch(net.minecraft.world.entity.Entity::isAlive);
            // limpiar (incluidas las avispas de refuerzo que haya creado el Nodo)
            node.mind().members().forEach(net.minecraft.world.entity.Entity::discard);
            spawned.forEach(net.minecraft.world.entity.Entity::discard);
            helper.assertTrue(hit, "Las avispas no atacaron al objetivo");
            helper.assertTrue(waspsAlive, "Alguna avispa murió sin que nadie la atacara");
            helper.succeed();
        });
    }

    /** Vuelca los bloques (no aire) a run/gametest_dump/<nombre>.txt para revisarlos con tools/render_dump.py. */
    private static void dump(ServerLevel level, BoundingBox bb, String name) {
        try {
            java.nio.file.Path dir = java.nio.file.Path.of("gametest_dump");
            java.nio.file.Files.createDirectories(dir);
            StringBuilder sb = new StringBuilder();
            BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ()).forEach(p -> {
                var s = level.getBlockState(p);
                if (!s.isAir()) {
                    sb.append(p.getX() - bb.minX()).append(' ').append(p.getY() - bb.minY()).append(' ')
                            .append(p.getZ() - bb.minZ()).append(' ')
                            .append(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock())).append('\n');
                }
            });
            java.nio.file.Files.writeString(dir.resolve(name + ".txt"), sb);
        } catch (java.io.IOException e) {
            Tecnocinetico.LOGGER.warn("[GameTest] no se pudo volcar {}", name, e);
        }
    }
}
