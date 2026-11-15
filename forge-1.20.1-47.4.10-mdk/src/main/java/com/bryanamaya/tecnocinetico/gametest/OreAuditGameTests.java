package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.TreeMap;

/**
 * Cuenta las menas que salen de verdad en chunks recién generados de cada dimensión (las del mod y referencias vanilla)
 * y escribe la media por chunk en {@code run/ore_audit.json} para {@code tools/progression_audit.py}. Solo con
 * {@code TECNO_AUDIT=1}.
 */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class OreAuditGameTests {

    private static final int SIDE = 10;                                      // 10x10 = 100 chunks por dimensión

    private static boolean counted(Block b) {
        String id = BuiltInRegistries.BLOCK.getKey(b).toString();
        return id.startsWith(Tecnocinetico.MOD_ID + ":") && (id.endsWith("_ore") || id.contains("cluster") || id.contains("crust"))
                || b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE || b == Blocks.COPPER_ORE || b == Blocks.DEEPSLATE_COPPER_ORE
                || b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE || b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE
                || b == Blocks.NETHER_QUARTZ_ORE || b == Blocks.ANCIENT_DEBRIS || b == Blocks.NETHER_GOLD_ORE || b == Blocks.REDSTONE_ORE
                || b == Blocks.DEEPSLATE_REDSTONE_ORE || b == Blocks.COAL_ORE || b == Blocks.DEEPSLATE_COAL_ORE || b == Blocks.END_STONE;
    }

    @GameTest(template = "empty", batch = "auditoria_menas", timeoutTicks = 100)
    public static void countOres(GameTestHelper h) {
        if (!"1".equals(System.getenv("TECNO_AUDIT"))) {
            h.succeed();
            return;
        }
        var server = h.getLevel().getServer();
        JsonObject root = new JsonObject();
        for (String name : java.util.List.of("overworld", "the_nether", "the_end", "the_end_centro")) {
            var key = name.equals("overworld") ? Level.OVERWORLD : name.equals("the_nether") ? Level.NETHER : Level.END;
            ServerLevel level = server.getLevel(key);
            int base = name.equals("the_end_centro") ? -80 : (key == Level.END ? 2400 : 60000) + level.getRandom().nextInt(2000) * 16 * SIDE;
            Map<String, Integer> counts = new TreeMap<>();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int cx = 0; cx < SIDE; cx++) {
                for (int cz = 0; cz < SIDE; cz++) {
                    LevelChunk chunk = level.getChunk((base >> 4) + cx, (base >> 4) + cz);
                    for (var section : chunk.getSections()) {
                        if (section.hasOnlyAir()) {
                            continue;
                        }
                        section.getStates().count((state, n) -> {
                            if (counted(state.getBlock())) {
                                counts.merge(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), n, Integer::sum);
                            }
                        });
                    }
                }
            }
            JsonObject dim = new JsonObject();
            counts.forEach((k, v) -> dim.addProperty(k, v / (double) (SIDE * SIDE)));
            dim.addProperty("_base", base);
            root.add("minecraft:" + name, dim);
        }
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("ore_audit.json"), new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        h.succeed();
    }
}
