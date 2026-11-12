package com.bryanamaya.tecnocinetico.worldgen;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.data.worldgen.biome.NetherBiomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.SurfaceRules;

import java.util.List;
import java.util.Map;

/**
 * Biomas del Nether industrial (parte 3 del Nether por etapas, ver docs/PLAN_NETHER.md). Cada uno sustituye a un
 * bioma vanilla del Nether ({@link #MAP}): conserva su relieve, sus estructuras (fortalezas, bastiones), sus recursos
 * y sus criaturas, y cambia el ambiente (niebla rojiza/sulfurosa, ceniza en el aire). Los detalles físicos (costras de
 * azufre, escoria, columnas, árboles carbonizados) los pone {@code worldgen/feature/InfernalDepositFeature}.
 * Solo en mundos del tipo Postapocalíptico (la fuente {@link InfernalBiomeSource} va en sus world presets).
 */
public final class InfernalBiomes {

    public static final ResourceKey<Biome> SLAG_FORGES = key("slag_forges"), SULFUR_PITS = key("sulfur_pits"),
            BASALT_QUARRIES = key("basalt_quarries"), CARBON_FOREST = key("carbon_forest"), OBSIDIAN_WASTES = key("obsidian_wastes");

    public static final List<ResourceKey<Biome>> ALL = List.of(SLAG_FORGES, SULFUR_PITS, BASALT_QUARRIES, CARBON_FOREST, OBSIDIAN_WASTES);

    /** Bioma vanilla del Nether → bioma del mod. */
    public static final Map<ResourceKey<Biome>, ResourceKey<Biome>> MAP = Map.of(
            Biomes.NETHER_WASTES, SLAG_FORGES,
            Biomes.SOUL_SAND_VALLEY, SULFUR_PITS,
            Biomes.BASALT_DELTAS, BASALT_QUARRIES,
            Biomes.CRIMSON_FOREST, CARBON_FOREST,
            Biomes.WARPED_FOREST, OBSIDIAN_WASTES);

    private InfernalBiomes() {}

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, name));
    }

    /** Datagen: copia generación, criaturas y sonidos del vanilla y cambia niebla, cielo, agua y partículas. */
    public static void bootstrap(BootstapContext<Biome> ctx) {
        var placed = ctx.lookup(Registries.PLACED_FEATURE);
        var carvers = ctx.lookup(Registries.CONFIGURED_CARVER);
        Biome[] originals = {NetherBiomes.netherWastes(placed, carvers), NetherBiomes.soulSandValley(placed, carvers),
                NetherBiomes.basaltDeltas(placed, carvers), NetherBiomes.crimsonForest(placed, carvers), NetherBiomes.warpedForest(placed, carvers)};
        // niebla: escoria rojiza, azufre amarillento, basalto gris, carbón pardo, obsidiana violácea
        int[] fog = {0x5B2317, 0x55501B, 0x302D33, 0x3C2520, 0x241D36};
        for (int i = 0; i < ALL.size(); i++) {
            Biome old = originals[i];
            var effects = new BiomeSpecialEffects.Builder()
                    .fogColor(fog[i]).waterColor(0x58392D).waterFogColor(0x281E1D).skyColor(0x120A08)
                    .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                    .ambientParticle(new AmbientParticleSettings(i == 1 ? ParticleTypes.ASH : ParticleTypes.WHITE_ASH, i == 1 ? 0.008F : 0.004F));
            old.getAmbientLoop().ifPresent(effects::ambientLoopSound);
            old.getAmbientMood().ifPresent(effects::ambientMoodSound);
            old.getAmbientAdditions().ifPresent(effects::ambientAdditionsSound);
            old.getBackgroundMusic().ifPresent(effects::backgroundMusic);
            ctx.register(ALL.get(i), new Biome.BiomeBuilder().hasPrecipitation(false).temperature(2.0F).downfall(0.0F)
                    .specialEffects(effects.build()).mobSpawnSettings(old.getMobSettings())
                    .generationSettings(old.getGenerationSettings()).build());
        }
    }

    /** Reglas de superficie vanilla del Nether con las condiciones de bioma traducidas a los biomas del mod. */
    public static SurfaceRules.RuleSource surface() {
        JsonElement json = SurfaceRules.RuleSource.CODEC.encodeStart(JsonOps.INSTANCE, SurfaceRuleData.nether()).getOrThrow(false, e -> {});
        translate(json);
        return SurfaceRules.RuleSource.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, e -> {});
    }

    private static void translate(JsonElement value) {
        if (value.isJsonArray()) {
            value.getAsJsonArray().forEach(InfernalBiomes::translate);
        } else if (value.isJsonObject()) {
            var obj = value.getAsJsonObject();
            if (obj.has("biome_is")) {
                JsonArray result = new JsonArray();
                for (var id : obj.getAsJsonArray("biome_is")) {
                    var key = ResourceKey.create(Registries.BIOME, ResourceLocation.tryParse(id.getAsString()));
                    result.add(MAP.getOrDefault(key, key).location().toString());
                }
                obj.add("biome_is", result);
            }
            obj.entrySet().forEach(e -> translate(e.getValue()));
        }
    }
}
