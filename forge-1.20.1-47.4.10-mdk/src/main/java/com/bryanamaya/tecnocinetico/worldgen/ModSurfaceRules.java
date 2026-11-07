package com.bryanamaya.tecnocinetico.worldgen;

import com.bryanamaya.tecnocinetico.block.ModWorldBlocks;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Reglas de superficie del Overworld del mod.
 *
 * 1) Reglas propias (parches de arena vitrificada, ceniza, asfalto, hormigón, escombros...) que se evalúan primero.
 * 2) Debajo, las reglas vanilla con sus condiciones de bioma traducidas a nuestros biomas, para que cada bioma
 *    conserve el suelo base de su equivalente (arena en el desierto, terracota en las Cicatrices, barro en el
 *    pantano, nieve/hielo en las cumbres...).
 */
public final class ModSurfaceRules {

    private ModSurfaceRules() {}

    public static SurfaceRules.RuleSource overworld() {
        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(), modRules()),
                remapVanilla(SurfaceRuleData.overworld()));
    }

    // ------------------------------------------------------------------ reglas propias

    private static SurfaceRules.RuleSource modRules() {
        SurfaceRules.ConditionSource dry = SurfaceRules.waterBlockCheck(-1, 0);   // superficie fuera del agua
        return SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                // Desierto Vitrificado: placas de arena fundida por el calor de las armas
                biome(ModBiomes.VITRIFIED_DESERT, SurfaceRules.sequence(
                        SurfaceRules.ifTrue(noise(1.2), block(ModWorldBlocks.VITRIFIED_SAND.get())),
                        SurfaceRules.ifTrue(noiseBelow(-2.2), block(ModWorldBlocks.VITRIFIED_SAND.get())))),
                // Taiga de Ceniza: mantos de ceniza
                biome(ModBiomes.ASH_TAIGA, SurfaceRules.ifTrue(dry, SurfaceRules.sequence(
                        SurfaceRules.ifTrue(noise(-0.4), block(ModWorldBlocks.ASH_BLOCK.get())),
                        SurfaceRules.ifTrue(noiseBelow(-1.6), block(Blocks.COARSE_DIRT))))),
                // Tierras Fracturadas: ceniza y grava entre la piedra
                biome(ModBiomes.FRACTURED_LANDS, SurfaceRules.ifTrue(dry, SurfaceRules.sequence(
                        SurfaceRules.ifTrue(noise(1.6), block(ModWorldBlocks.ASH_BLOCK.get())),
                        SurfaceRules.ifTrue(noiseBelow(-1.6), block(Blocks.GRAVEL))))),
                // Megaciudad Muerta: suelo de calles (asfalto con bordes agrietados) y escombros
                biome(ModBiomes.DEAD_MEGACITY, SurfaceRules.ifTrue(dry, SurfaceRules.sequence(
                        SurfaceRules.ifTrue(noise(1.0), block(ModWorldBlocks.ASPHALT.get())),
                        SurfaceRules.ifTrue(noise(0.5), block(ModWorldBlocks.CRACKED_ASPHALT.get())),
                        SurfaceRules.ifTrue(noiseBelow(-2.0), block(ModWorldBlocks.CRACKED_CONCRETE.get())),
                        SurfaceRules.ifTrue(noiseBelow(-1.4), block(ModWorldBlocks.RUBBLE.get()))))),
                // Ruinas Invadidas y Selva Industrial: losas de hormigón cubiertas de musgo
                biome(ModBiomes.OVERGROWN_RUINS, SurfaceRules.ifTrue(dry, mossySlabs())),
                biome(ModBiomes.INDUSTRIAL_JUNGLE, SurfaceRules.ifTrue(dry, mossySlabs())),
                // Praderas de Chatarra: tierra removida y escombros
                biome(ModBiomes.SCRAP_MEADOWS, SurfaceRules.ifTrue(dry, SurfaceRules.sequence(
                        SurfaceRules.ifTrue(noise(2.2), block(Blocks.COARSE_DIRT)),
                        SurfaceRules.ifTrue(noiseBelow(-2.6), block(ModWorldBlocks.RUBBLE.get()))))),
                // Tundra en Ruinas: hormigón agrietado bajo la nieve
                biome(ModBiomes.RUINED_TUNDRA, SurfaceRules.ifTrue(dry,
                        SurfaceRules.ifTrue(noise(2.0), block(ModWorldBlocks.CRACKED_CONCRETE.get())))),
                // Costa de Chatarra: montones de chatarra y escombros
                biome(ModBiomes.SCRAP_COAST, SurfaceRules.sequence(
                        SurfaceRules.ifTrue(noise(1.4), block(ModWorldBlocks.SCRAP_PILE.get())),
                        SurfaceRules.ifTrue(noiseBelow(-1.0), block(ModWorldBlocks.RUBBLE.get())))),
                // Mares y río: lecho de escombros
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.DEBRIS_SEA, ModBiomes.DEEP_DEBRIS_SEA,
                                ModBiomes.FROZEN_DEBRIS_SEA, ModBiomes.POLLUTED_RIVER),
                        SurfaceRules.ifTrue(SurfaceRules.not(dry), SurfaceRules.sequence(
                                SurfaceRules.ifTrue(noise(0.8), block(ModWorldBlocks.RUBBLE.get())),
                                SurfaceRules.ifTrue(noiseBelow(-2.0), block(ModWorldBlocks.SCRAP_PILE.get())))))
        ));
    }

    private static SurfaceRules.RuleSource mossySlabs() {
        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(noise(2.4), block(ModWorldBlocks.MOSSY_CONCRETE.get())),
                SurfaceRules.ifTrue(noise(1.9), block(Blocks.MOSS_BLOCK)));
    }

    private static SurfaceRules.RuleSource biome(ResourceKey<Biome> key, SurfaceRules.RuleSource rule) {
        return SurfaceRules.ifTrue(SurfaceRules.isBiome(key), rule);
    }

    private static SurfaceRules.RuleSource block(Block b) {
        return SurfaceRules.state(b.defaultBlockState());
    }

    /** Ruido de superficie por encima de {@code v} (mismas unidades que SurfaceRuleData.surfaceNoiseAbove). */
    private static SurfaceRules.ConditionSource noise(double v) {
        return SurfaceRules.noiseCondition(Noises.SURFACE, v / 8.25D, Double.MAX_VALUE);
    }

    private static SurfaceRules.ConditionSource noiseBelow(double v) {
        return SurfaceRules.noiseCondition(Noises.SURFACE, -Double.MAX_VALUE, v / 8.25D);
    }

    // ------------------------------------------------------------------ reglas vanilla traducidas

    /** Serializa las reglas vanilla, cambia cada condición "biome_is" por nuestros biomas y las vuelve a leer. */
    static SurfaceRules.RuleSource remapVanilla(SurfaceRules.RuleSource vanilla) {
        JsonElement json = SurfaceRules.RuleSource.CODEC.encodeStart(JsonOps.INSTANCE, vanilla)
                .getOrThrow(false, e -> {});
        remapBiomes(json);
        return SurfaceRules.RuleSource.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, e -> {});
    }

    private static void remapBiomes(JsonElement el) {
        if (el.isJsonArray()) {
            for (JsonElement child : el.getAsJsonArray()) {
                remapBiomes(child);
            }
        } else if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            if (obj.has("biome_is") && obj.get("biome_is").isJsonArray()) {
                Set<String> out = new LinkedHashSet<>();
                for (JsonElement id : obj.getAsJsonArray("biome_is")) {
                    ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, ResourceLocation.tryParse(id.getAsString()));
                    out.add(ModBiomes.VANILLA_TO_MOD.getOrDefault(key, key).location().toString());
                }
                JsonArray arr = new JsonArray();
                out.forEach(s -> arr.add(new JsonPrimitive(s)));
                obj.add("biome_is", arr);
            }
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                remapBiomes(e.getValue());
            }
        }
    }
}
