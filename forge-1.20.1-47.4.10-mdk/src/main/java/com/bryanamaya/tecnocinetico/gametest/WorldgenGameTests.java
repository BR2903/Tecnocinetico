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
            "ruined_building_overgrown", "ruined_building_frozen", "survivor_settlement", "abandoned_factory", "swarm_hive", "ai_installation",
            "relay_antenna", "data_center", "survivor_bunker", "swarm_laboratory", "sunken_city"};

    @GameTest(template = "empty", timeoutTicks = 800)
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
            int attempts = STRUCTURES[i].equals("sunken_city") ? 512 : i >= 8 ? 256 : 64;
            for (int tries = 0; tries < attempts && !start.isValid(); tries++) {
                int stride = i >= 8 ? 128 : 48;
                int columns = i >= 8 ? 16 : 8;
                ChunkPos cp = new ChunkPos(base.offset(i * 1024 + (tries % columns) * stride, 0, (tries / columns) * stride));
                start = structure.generate(level.registryAccess(), gen, new FixedBiomeSource(biome),
                        level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(), cp, 0, level,
                        b -> true);
            }
            helper.assertTrue(start.isValid(), "No se pudo colocar " + key.location() + " en " + attempts + " intentos");
            StructureStart placed = start;
            BoundingBox bb = start.getBoundingBox();
            ChunkPos.rangeClosed(new ChunkPos(bb.minX() >> 4, bb.minZ() >> 4), new ChunkPos(bb.maxX() >> 4, bb.maxZ() >> 4))
                    .forEach(c -> placed.placeInChunk(level, level.structureManager(), gen, level.getRandom(),
                            new BoundingBox(c.getMinBlockX(), level.getMinBuildHeight(), c.getMinBlockZ(),
                                    c.getMaxBlockX(), level.getMaxBuildHeight(), c.getMaxBlockZ()), c));
            Tecnocinetico.LOGGER.info("[GameTest] {} generado en {}", key.location(), bb);
            dump(level, placed.getPieces().get(0).getBoundingBox().inflatedBy(2), STRUCTURES[i]);
            if (i >= 8) {
                BoundingBox raw = placed.getPieces().get(0).getBoundingBox();
                BlockPos origin = new BlockPos(raw.minX(), raw.minY() + 4, raw.minZ());
                if (STRUCTURES[i].equals("relay_antenna") || STRUCTURES[i].equals("swarm_laboratory")) {
                    boolean relay = STRUCTURES[i].equals("relay_antenna");
                    var console = level.getBlockEntity(origin.offset(16, 1, relay ? 12 : 10));
                    helper.assertTrue(console instanceof com.bryanamaya.tecnocinetico.block.entity.CoreConsoleBlockEntity c
                                    && c.guardian().equals(relay ? "network_watcher" : "swarm_matriarch"),
                            "La arena generada no tiene una consola configurada para su jefe");
                }
                if (STRUCTURES[i].equals("survivor_bunker")) {
                    helper.assertTrue(level.getBlockState(origin.offset(4, 1, 4)).is(net.minecraft.world.level.block.Blocks.LADDER)
                                    && level.getBlockState(origin.offset(4, 1, 5)).isAir(),
                            "El pozo del búnker no conecta con su interior");
                }
                if (STRUCTURES[i].equals("sunken_city")) {
                    helper.assertTrue(level.getFluidState(origin.offset(16, 1, 20)).is(net.minecraft.tags.FluidTags.WATER),
                            "La avenida de la ciudad sumergida está bloqueada o no está inundada");
                }
            }
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

    /** Caza del Núcleo: el motor de salto la lleva al Nether a la posición 1:8 y en el aire (sin bloques sólidos). */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void shipJumpToNether(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos col = helper.absolutePos(BlockPos.ZERO);
        var ship = com.bryanamaya.tecnocinetico.entity.ModEntities.CORE_FIGHTER.get().create(level);
        ship.moveTo(col.getX() + 0.5, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                col.getX(), col.getZ()) + 3, col.getZ() + 0.5, 0, 0);
        level.addFreshEntity(ship);
        var moved = com.bryanamaya.tecnocinetico.entity.ship.ShipJump.perform(ship, net.minecraft.world.level.Level.NETHER);
        helper.assertTrue(moved != null && moved.level().dimension() == net.minecraft.world.level.Level.NETHER,
                "La nave no llegó al Nether");
        // a escala 1:8 (la búsqueda de hueco del Nether puede moverla hasta 24 bloques)
        boolean scaled = Math.hypot(moved.getX() - ship.getX() / 8.0, moved.getZ() - ship.getZ() / 8.0) < 26;
        boolean clear = moved.level().noCollision(moved, moved.getBoundingBox());
        Tecnocinetico.LOGGER.info("[GameTest] salto: {} -> {} (libre={})", ship.position(), moved.position(), clear);
        moved.discard();
        helper.assertTrue(scaled, "La posición no está a escala 1:8");
        helper.assertTrue(clear, "La nave llegó dentro de bloques");
        helper.succeed();
    }

    /** Consola del Núcleo: al activarla aparece la Araña de Asedio y la barrera sella; al morir, la barrera se abre. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void siegeSpiderArena(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos col = helper.absolutePos(BlockPos.ZERO);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, col.getX(), col.getZ()) + 2;
        BlockPos consolePos = new BlockPos(col.getX(), y, col.getZ());
        BlockPos sealPos = consolePos.above(6);
        level.setBlock(consolePos, com.bryanamaya.tecnocinetico.block.ModBlocks.CORE_CONSOLE.get().defaultBlockState(), 3);
        level.setBlock(sealPos, net.minecraft.world.level.block.Blocks.LADDER.defaultBlockState(), 3);
        var console = (com.bryanamaya.tecnocinetico.block.entity.CoreConsoleBlockEntity) level.getBlockEntity(consolePos);
        console.setSeal(java.util.List.of(sealPos));
        var player = helper.makeMockPlayer();
        console.activate(player);
        var bosses = level.getEntitiesOfClass(com.bryanamaya.tecnocinetico.entity.boss.SiegeSpiderEntity.class,
                new net.minecraft.world.phys.AABB(consolePos).inflate(10));
        boolean sealed = level.getBlockState(sealPos).is(com.bryanamaya.tecnocinetico.block.ModBlocks.CORE_BARRIER.get());
        helper.assertTrue(bosses.size() == 1 && console.isActive() && sealed, "La consola no despertó al guardián o no selló");
        // punto débil: un impacto en la rodilla 0 rompe esa pata y la tumba; otro en la misma ya no cuenta
        var boss = bosses.get(0);
        var knee = boss.knee(0);
        var arrow = new net.minecraft.world.entity.projectile.Arrow(level, knee.x, knee.y, knee.z);
        boss.invulnerableTime = 0;
        boss.hurt(level.damageSources().arrow(arrow, player), 4.0F);
        boolean broke = boss.legBroken(0) && boss.stunned();
        helper.assertTrue(broke, "Acertar en la rodilla no rompió la pata ni tumbó a la araña");
        bosses.get(0).kill();
        boolean open = level.getBlockState(sealPos).is(net.minecraft.world.level.block.Blocks.LADDER);
        Tecnocinetico.LOGGER.info("[GameTest] arena: guardián={}, sellada={}, abierta tras morir={}, vencida={}",
                bosses.size(), sealed, open, console.isBeaten());
        bosses.get(0).discard();
        level.removeBlock(consolePos, false);
        level.removeBlock(sealPos, false);
        helper.assertTrue(open && console.isBeaten(), "La barrera no se abrió al morir el guardián");
        helper.succeed();
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

    @GameTest(template = "empty", timeoutTicks = 400, batch = "enjambre")   // aparte: los enemigos de otras pruebas la distraían
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

    /**
     * Tipos de mundo propios: los tres existen, salen en la lista de "Tipo de mundo" (etiqueta normal) y su Overworld
     * usa los biomas del mod; los tipos vanilla siguen siendo vanilla (compatibilidad con otros mods de terreno).
     */
    @GameTest(template = "empty", batch = "tipos_de_mundo")
    public static void worldPresets(GameTestHelper h) {
        var presets = h.getLevel().registryAccess().registryOrThrow(Registries.WORLD_PRESET);
        var tag = net.minecraft.tags.TagKey.create(Registries.WORLD_PRESET, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "normal"));
        for (String id : new String[]{"post_apocalyptic", "post_apocalyptic_large_biomes", "post_apocalyptic_amplified"}) {
            var key = net.minecraft.resources.ResourceKey.create(Registries.WORLD_PRESET,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, id));
            var preset = presets.getHolder(key);
            h.assertTrue(preset.isPresent(), "Falta el tipo de mundo " + id);
            h.assertTrue(preset.get().is(tag), "El tipo de mundo " + id + " no sale en la pantalla de crear mundo");
            var overworld = preset.get().value().overworld().orElseThrow();
            h.assertTrue(overworld.generator().getBiomeSource() instanceof com.bryanamaya.tecnocinetico.worldgen.RemappedBiomeSource,
                    "El Overworld de " + id + " no usa los biomas del mod");
        }
        var vanilla = presets.getOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.NORMAL).overworld().orElseThrow();
        h.assertFalse(vanilla.generator().getBiomeSource() instanceof com.bryanamaya.tecnocinetico.worldgen.RemappedBiomeSource,
                "El tipo de mundo vanilla sigue sobrescrito");
        h.succeed();
    }

    /**
     * Mundo arrasado (2026-11-12): los modificadores de bioma quitan árboles y flores vanilla de los biomas abiertos y
     * ponen árboles vivos ralos; el bosque conserva sus árboles pero pierde las flores; las llanuras son ciudad muerta.
     */
    @GameTest(template = "empty", batch = "tipos_de_mundo")
    public static void wastelandVegetation(GameTestHelper h) {
        var biomes = h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        java.util.function.BiFunction<net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome>, String, Boolean> has = (b, id) ->
                biomes.getOrThrow(b).getGenerationSettings().features().stream().flatMap(net.minecraft.core.HolderSet::stream)
                        .anyMatch(f -> f.unwrapKey().map(k -> k.location().toString().equals(id)).orElse(false));
        var meadows = com.bryanamaya.tecnocinetico.worldgen.ModBiomes.SCRAP_MEADOWS;
        var forest = com.bryanamaya.tecnocinetico.worldgen.ModBiomes.OVERGROWN_RUINS;
        var city = com.bryanamaya.tecnocinetico.worldgen.ModBiomes.DEAD_MEGACITY;
        h.assertFalse(has.apply(meadows, "minecraft:trees_plains") || has.apply(meadows, "minecraft:flower_plains"),
                "Las Praderas de Chatarra siguen con árboles o flores vanilla");
        h.assertTrue(has.apply(meadows, "tecnocinetico:vegetation/sickly_oak_meadows") && has.apply(meadows, "tecnocinetico:scrap_meadows/dead_tree"),
                "Las Praderas de Chatarra no tienen la mezcla de árboles vivos ralos y muertos");
        h.assertTrue(has.apply(forest, "minecraft:trees_birch_and_oak") && !has.apply(forest, "minecraft:forest_flowers")
                && has.apply(forest, "tecnocinetico:overgrown_ruins/dead_tree"), "Ruinas Invadidas: árboles vivos + muertos y sin flores");
        h.assertFalse(has.apply(city, "minecraft:trees_plains") || has.apply(city, "minecraft:patch_grass_plain"),
                "La Megaciudad Muerta tiene vegetación vanilla");
        h.assertTrue(com.bryanamaya.tecnocinetico.worldgen.ModBiomes.VANILLA_TO_MOD.get(net.minecraft.world.level.biome.Biomes.PLAINS) == city,
                "Las llanuras no son la Megaciudad Muerta");
        h.succeed();
    }

    /** Farolas y postes: de pie (fustes + cabeza; poste + cruceta + cables colgando) y caídos (tumbados en el suelo). */
    @GameTest(template = "empty", batch = "tipos_de_mundo")
    public static void streetProps(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos base = h.absolutePos(BlockPos.ZERO).atY(210);
        var feature = com.bryanamaya.tecnocinetico.worldgen.ModWorldgen.STREET_PROP.get();
        var gen = level.getChunkSource().getGenerator();
        try {
            for (BlockPos p : BlockPos.betweenClosed(base.offset(-8, -1, -8), base.offset(8, -1, 8))) {
                level.setBlock(p, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
            }
            var rnd = net.minecraft.util.RandomSource.create(7);
            BlockPos lamp = base.offset(-5, 0, -5), pole = base.offset(5, 0, -5), fallen = base;                                  // centro: cabe tumbado hacia cualquier lado
            h.assertTrue(feature.place(new com.bryanamaya.tecnocinetico.worldgen.feature.StreetPropFeature.Config("lamp", 1, 0), level, gen, rnd, lamp),
                    "No colocó la farola de pie");
            boolean top = false;                                                   // de pie o doblada (la config lo permite)
            for (int y = 2; y <= 4; y++) {
                var t = level.getBlockState(lamp.above(y));
                top |= t.is(com.bryanamaya.tecnocinetico.block.ModWorldBlocks.STREET_LAMP_HEAD.get())
                        || t.is(com.bryanamaya.tecnocinetico.block.ModWorldBlocks.STREET_LAMP_BENT.get());
            }
            h.assertTrue(level.getBlockState(lamp).is(com.bryanamaya.tecnocinetico.block.ModWorldBlocks.STREET_LAMP_POLE.get()) && top,
                    "Farola: faltan fustes o la parte de arriba");
            h.assertTrue(feature.place(new com.bryanamaya.tecnocinetico.worldgen.feature.StreetPropFeature.Config("pole", 1, 0), level, gen, rnd, pole),
                    "No colocó el poste");
            int crossarms = 0, cables = 0;
            for (BlockPos p : BlockPos.betweenClosed(pole.offset(-4, 0, -4), pole.offset(4, 7, 4))) {
                var st = level.getBlockState(p);
                if (st.is(com.bryanamaya.tecnocinetico.block.ModWorldBlocks.UTILITY_CROSSARM.get())) crossarms++;
                if (st.is(com.bryanamaya.tecnocinetico.block.ModWorldBlocks.POWER_CABLE.get())
                        && st.getValue(com.bryanamaya.tecnocinetico.block.custom.StreetPropBlocks.Cable.HANGING)) cables++;
            }
            h.assertTrue(crossarms == 1 && cables >= 1, "Poste: " + crossarms + " crucetas y " + cables + " cables colgando");
            h.assertTrue(feature.place(new com.bryanamaya.tecnocinetico.worldgen.feature.StreetPropFeature.Config("pole", 0, 1), level, gen, rnd, fallen),
                    "No colocó el poste caído");
            var st = level.getBlockState(fallen);
            h.assertTrue(st.is(com.bryanamaya.tecnocinetico.block.ModWorldBlocks.UTILITY_POLE.get())
                    && st.getValue(com.bryanamaya.tecnocinetico.block.custom.StreetPropBlocks.Pole.AXIS) != net.minecraft.core.Direction.Axis.Y
                    && level.getBlockState(fallen.above()).isAir(), "El poste caído no está tumbado en el suelo");
            h.assertFalse(feature.place(new com.bryanamaya.tecnocinetico.worldgen.feature.StreetPropFeature.Config("lamp", 1, 0), level, gen, rnd,
                    base.offset(0, 5, 0)), "Puso una farola en el aire");
        } finally {
            for (BlockPos p : BlockPos.betweenClosed(base.offset(-12, -1, -12), base.offset(12, 8, 12))) {
                level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            }
        }
        h.succeed();
    }

    /** Carreteras: calles de 7 cada 48 bloques solo en la ciudad; autopistas de 9 con quitamiedos; pintado sobre suelo. */
    @GameTest(template = "empty", batch = "tipos_de_mundo")
    public static void roads(GameTestHelper h) {
        long seed = 12345L;
        // calles: a lo largo de una fila, exactamente 7 columnas de cada 48 son calle (sin contar autopistas)
        int street = 0;
        for (int x = 0; x < 48; x++) {
            var r = com.bryanamaya.tecnocinetico.worldgen.feature.RoadFeature.roadAt(seed, x, 20, true);
            if (r != null && !r.highway() && r.along() == net.minecraft.core.Direction.Axis.Z) street++;
        }
        h.assertTrue(street == 7, "Calles: " + street + " columnas de 48 (se esperaban 7)");
        h.assertTrue(java.util.stream.IntStream.range(0, 48).noneMatch(x -> {
            var r = com.bryanamaya.tecnocinetico.worldgen.feature.RoadFeature.roadAt(seed, x, 20, false);
            return r != null && !r.highway();
        }), "Hay calles fuera de la ciudad");
        int hw = 0;
        for (int x = 0; x < 512; x++) {
            var r = com.bryanamaya.tecnocinetico.worldgen.feature.RoadFeature.roadAt(seed, x, 7, false);
            if (r != null && r.highway() && r.along() == net.minecraft.core.Direction.Axis.Z) hw++;
        }
        h.assertTrue(hw == 11, "Autopista: " + hw + " columnas de 512 (9 de calzada + 2 de quitamiedos)");
        // pintado sobre piedra: asfalto/línea/bache en la calzada y nada encima
        var level = h.getLevel();
        BlockPos base = h.absolutePos(BlockPos.ZERO).atY(200);
        try {
            int painted = 0;
            for (int i = 0; i < 20; i++) {
                BlockPos top = base.offset(i, 0, 0);
                level.setBlock(top, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
                level.setBlock(top.above(), net.minecraft.world.level.block.Blocks.GRASS.defaultBlockState(), 2);
                var road = new com.bryanamaya.tecnocinetico.worldgen.feature.RoadFeature.Road(net.minecraft.core.Direction.Axis.X, 1, 3, false);
                if (com.bryanamaya.tecnocinetico.worldgen.feature.RoadFeature.paint(level, seed, top, top.getX(), top.getZ(), road)) {
                    painted++;
                    var st = level.getBlockState(top);
                    h.assertTrue((st.is(com.bryanamaya.tecnocinetico.block.ModWorldBlocks.ASPHALT.get())
                            || st.is(com.bryanamaya.tecnocinetico.block.ModWorldBlocks.CRACKED_ASPHALT.get())
                            || st.is(net.minecraft.world.level.block.Blocks.GRAVEL)) && level.getBlockState(top.above()).isAir(),
                            "Calzada mal pintada: " + st);
                }
            }
            h.assertTrue(painted >= 12, "Se pintaron solo " + painted + " de 20 columnas");
        } finally {
            for (BlockPos p : BlockPos.betweenClosed(base.offset(-1, 0, -1), base.offset(21, 2, 1))) {
                level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            }
        }
        h.succeed();
    }

    /** Patrullas nocturnas: grupos de 2-3, tope de 5 alrededor, se guardan, desaparecen lejos y de día se van. */
    @GameTest(template = "empty", batch = "patrullas", timeoutTicks = 100)
    public static void nightPatrols(GameTestHelper h) {
        var level = h.getLevel();
        var player = h.makeMockPlayer();
        BlockPos at = h.absolutePos(BlockPos.ZERO);
        player.setPos(at.getX() + 0.5, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, at.getX(), at.getZ()), at.getZ() + 0.5);
        for (int cx = -5; cx <= 5; cx++) {
            for (int cz = -5; cz <= 5; cz++) {
                level.getChunk((at.getX() >> 4) + cx, (at.getZ() >> 4) + cz);
            }
        }
        var all = new java.util.ArrayList<com.bryanamaya.tecnocinetico.entity.custom.DroneEntity>();
        try {
            var first = com.bryanamaya.tecnocinetico.entity.custom.NightPatrols.spawnPatrol(level, player);
            all.addAll(first);
            h.assertTrue(first.size() >= 2 && first.size() <= 3 && first.stream().allMatch(
                    com.bryanamaya.tecnocinetico.entity.custom.DroneEntity::isNightPatrol), "Patrulla de " + first.size() + " drones");
            for (int i = 0; i < 6; i++) {
                all.addAll(com.bryanamaya.tecnocinetico.entity.custom.NightPatrols.spawnPatrol(level, player));
            }
            h.assertTrue(all.size() <= com.bryanamaya.tecnocinetico.entity.custom.NightPatrols.MAX_NEARBY,
                    "Se acumularon " + all.size() + " drones de patrulla");
            var d = first.get(0);
            h.assertTrue(d.removeWhenFarAway(200 * 200), "Una patrulla no desaparece lejos de los jugadores");
            var copy = com.bryanamaya.tecnocinetico.entity.ModEntities.DRONE.get().create(level);
            copy.load(d.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
            h.assertTrue(copy.isNightPatrol(), "La patrulla no se guarda");
            d.tick();
            h.assertTrue(d.isLeaving() == level.isDay(), "De día debe irse y de noche patrullar (día=" + level.isDay() + ")");
        } finally {
            all.forEach(net.minecraft.world.entity.Entity::discard);
        }
        h.succeed();
    }
}
