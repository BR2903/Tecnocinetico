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
            "ruined_building_overgrown", "ruined_building_frozen", "survivor_settlement"};

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
