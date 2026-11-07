package com.bryanamaya.tecnocinetico.worldgen;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.biome.OverworldBiomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.biome.AmbientAdditionsSettings;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Biomas del Overworld post-apocalíptico (fase 2 de docs/PLAN_MUNDO.md).
 *
 * Cada bioma reutiliza la generación (vegetación, minerales, cuevas) y los animales de su equivalente vanilla
 * y cambia el ambiente (cielo, niebla, agua, hierba, partículas, sonidos). El terreno de cada bioma lo dan
 * las reglas de superficie de ModSurfaceRules. VANILLA_TO_MOD reasigna cada bioma vanilla a su sustituto.
 */
public final class ModBiomes {

    public static final ResourceKey<Biome> OVERGROWN_RUINS = key("overgrown_ruins");
    public static final ResourceKey<Biome> INDUSTRIAL_JUNGLE = key("industrial_jungle");
    public static final ResourceKey<Biome> SCRAP_MEADOWS = key("scrap_meadows");
    public static final ResourceKey<Biome> DEAD_MEGACITY = key("dead_megacity");
    public static final ResourceKey<Biome> ASH_TAIGA = key("ash_taiga");
    public static final ResourceKey<Biome> COOLANT_SWAMP = key("coolant_swamp");
    public static final ResourceKey<Biome> EXTRACTION_SCARS = key("extraction_scars");
    public static final ResourceKey<Biome> VITRIFIED_DESERT = key("vitrified_desert");
    public static final ResourceKey<Biome> FRACTURED_LANDS = key("fractured_lands");
    public static final ResourceKey<Biome> ANTENNA_PEAKS = key("antenna_peaks");
    public static final ResourceKey<Biome> RUINED_TUNDRA = key("ruined_tundra");
    public static final ResourceKey<Biome> SCRAP_COAST = key("scrap_coast");
    public static final ResourceKey<Biome> DEBRIS_SEA = key("debris_sea");
    public static final ResourceKey<Biome> DEEP_DEBRIS_SEA = key("deep_debris_sea");
    public static final ResourceKey<Biome> FROZEN_DEBRIS_SEA = key("frozen_debris_sea");
    public static final ResourceKey<Biome> POLLUTED_RIVER = key("polluted_river");
    public static final ResourceKey<Biome> CINETRITE_CAVES = key("cinetrite_caves");
    public static final ResourceKey<Biome> INDUSTRIAL_TUNNELS = key("industrial_tunnels");
    public static final ResourceKey<Biome> DEEP_FACILITY = key("deep_facility");

    public static final List<ResourceKey<Biome>> ALL = List.of(OVERGROWN_RUINS, INDUSTRIAL_JUNGLE, SCRAP_MEADOWS,
            DEAD_MEGACITY, ASH_TAIGA, COOLANT_SWAMP, EXTRACTION_SCARS, VITRIFIED_DESERT, FRACTURED_LANDS, ANTENNA_PEAKS,
            RUINED_TUNDRA, SCRAP_COAST, DEBRIS_SEA, DEEP_DEBRIS_SEA, FROZEN_DEBRIS_SEA, POLLUTED_RIVER, CINETRITE_CAVES,
            INDUSTRIAL_TUNNELS, DEEP_FACILITY);

    /** Qué bioma del mod sustituye a cada bioma vanilla del Overworld. */
    public static final Map<ResourceKey<Biome>, ResourceKey<Biome>> VANILLA_TO_MOD = new LinkedHashMap<>();

    static {
        map(OVERGROWN_RUINS, Biomes.FOREST, Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST,
                Biomes.DARK_FOREST, Biomes.MEADOW, Biomes.CHERRY_GROVE);
        map(INDUSTRIAL_JUNGLE, Biomes.JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE);
        map(SCRAP_MEADOWS, Biomes.PLAINS, Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.MUSHROOM_FIELDS);
        map(DEAD_MEGACITY, Biomes.SUNFLOWER_PLAINS, Biomes.WINDSWEPT_SAVANNA);
        map(ASH_TAIGA, Biomes.TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA);
        map(COOLANT_SWAMP, Biomes.SWAMP, Biomes.MANGROVE_SWAMP);
        map(EXTRACTION_SCARS, Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS);
        map(VITRIFIED_DESERT, Biomes.DESERT);
        map(FRACTURED_LANDS, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST);
        map(ANTENNA_PEAKS, Biomes.JAGGED_PEAKS, Biomes.FROZEN_PEAKS, Biomes.STONY_PEAKS, Biomes.SNOWY_SLOPES, Biomes.GROVE);
        map(RUINED_TUNDRA, Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES, Biomes.SNOWY_TAIGA);
        map(SCRAP_COAST, Biomes.BEACH, Biomes.SNOWY_BEACH, Biomes.STONY_SHORE);
        map(DEBRIS_SEA, Biomes.OCEAN, Biomes.WARM_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.COLD_OCEAN);
        map(DEEP_DEBRIS_SEA, Biomes.DEEP_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN, Biomes.DEEP_COLD_OCEAN);
        map(FROZEN_DEBRIS_SEA, Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN);
        map(POLLUTED_RIVER, Biomes.RIVER, Biomes.FROZEN_RIVER);
        map(CINETRITE_CAVES, Biomes.LUSH_CAVES);
        map(INDUSTRIAL_TUNNELS, Biomes.DRIPSTONE_CAVES);
        map(DEEP_FACILITY, Biomes.DEEP_DARK);
    }

    private ModBiomes() {}

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, name));
    }

    @SafeVarargs
    private static void map(ResourceKey<Biome> target, ResourceKey<Biome>... vanilla) {
        for (ResourceKey<Biome> v : vanilla) {
            VANILLA_TO_MOD.put(v, target);
        }
    }

    // =================================================================== creación (datagen)

    public static void bootstrap(BootstapContext<Biome> context) {
        HolderGetter<PlacedFeature> pf = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> cv = context.lookup(Registries.CONFIGURED_CARVER);

        // Verde reclamado
        context.register(OVERGROWN_RUINS, retheme(OverworldBiomes.forest(pf, cv, false, false, false),
                look(0x8FA6B4, 0xB6C2C4, 0x3F6E7A, 0x0E2A30).grass(0x5E8A3C).foliage(0x4E7A30)));
        context.register(INDUSTRIAL_JUNGLE, retheme(OverworldBiomes.jungle(pf, cv),
                look(0x86A8B0, 0xA8C0B8, 0x356E6A, 0x0C2A28).grass(0x4F9A38).foliage(0x3F8A2C)));
        context.register(SCRAP_MEADOWS, retheme(OverworldBiomes.plains(pf, cv, false, false, false),
                look(0x9AACB4, 0xBEC6C2, 0x46707A, 0x102A30).grass(0x8A9A50).foliage(0x7A8A44)));
        context.register(DEAD_MEGACITY, retheme(OverworldBiomes.plains(pf, cv, false, false, false),
                look(0x9AA2A8, 0xA9ADB0, 0x4A6670, 0x14262C).grass(0x7A8A54).foliage(0x6A7A48)
                        .particle(ParticleTypes.WHITE_ASH, 0.004F)));
        context.register(COOLANT_SWAMP, retheme(OverworldBiomes.swamp(pf, cv),
                look(0x8EA6A0, 0x9DB8A8, 0x3FA89A, 0x1D5A50).grass(0x5E7A40).foliage(0x4E6A34)
                        .particle(ParticleTypes.SPORE_BLOSSOM_AIR, 0.002F)));

        // Gris de batalla y ceniza
        context.register(ASH_TAIGA, retheme(OverworldBiomes.taiga(pf, cv, false),
                look(0x9A9A98, 0xA8A6A2, 0x4E5E64, 0x1A2226).grass(0x7E846E).foliage(0x6A6E5E)
                        .particle(ParticleTypes.WHITE_ASH, 0.025F)));
        context.register(FRACTURED_LANDS, retheme(OverworldBiomes.windsweptHills(pf, cv, false),
                look(0x8A8480, 0x9A928C, 0x4A585E, 0x161E22).grass(0x7A7E5E).foliage(0x6A6E50)
                        .particle(ParticleTypes.ASH, 0.012F)
                        .additions(0.0111D)));
        context.register(EXTRACTION_SCARS, retheme(OverworldBiomes.badlands(pf, cv, false),
                look(0x8FA8B8, 0xA0C0C8, 0x3E7884, 0x10303A).grass(0x8A8E5A).foliage(0x7A7E4E)));
        context.register(VITRIFIED_DESERT, retheme(OverworldBiomes.desert(pf, cv),
                look(0xC8B490, 0xD8C8A8, 0x5A8090, 0x1A3038)));

        // Frío y montaña
        context.register(ANTENNA_PEAKS, retheme(OverworldBiomes.jaggedPeaks(pf, cv),
                look(0x9CB0C4, 0xC0CCD6, 0x3A6680, 0x0E2436)));
        context.register(RUINED_TUNDRA, retheme(OverworldBiomes.plains(pf, cv, false, true, false),
                look(0xA4B4C4, 0xC8D0D8, 0x3A5E78, 0x0E2234)));

        // Agua
        context.register(SCRAP_COAST, retheme(OverworldBiomes.beach(pf, cv, false, false),
                look(0x9AB0BC, 0xB8C6CA, 0x3E6E78, 0x10282E)));
        context.register(DEBRIS_SEA, retheme(OverworldBiomes.ocean(pf, cv, false),
                look(0x96AEBC, 0xB4C2C8, 0x3A6A70, 0x24484C)));
        context.register(DEEP_DEBRIS_SEA, retheme(OverworldBiomes.ocean(pf, cv, true),
                look(0x92AABA, 0xB0BEC6, 0x325E66, 0x1A3A40)));
        context.register(FROZEN_DEBRIS_SEA, retheme(OverworldBiomes.frozenOcean(pf, cv, false),
                look(0xA2B6C8, 0xC4CED8, 0x3A5A70, 0x10243A)));
        context.register(POLLUTED_RIVER, retheme(OverworldBiomes.river(pf, cv, false),
                look(0x96ACB6, 0xB6C2C4, 0x5A7A5A, 0x1E3020)));

        // Cuevas
        context.register(CINETRITE_CAVES, retheme(OverworldBiomes.lushCaves(pf, cv),
                look(0x7BA4FF, 0x7AC8D8, 0x3FB6C8, 0x0E3A44)));
        context.register(INDUSTRIAL_TUNNELS, retheme(OverworldBiomes.dripstoneCaves(pf, cv),
                look(0x7BA4FF, 0xA0A4A8, 0x3F76E4, 0x050533)));
        context.register(DEEP_FACILITY, retheme(OverworldBiomes.deepDark(pf, cv),
                look(0x7BA4FF, 0x1E2A30, 0x3F76E4, 0x050533)));
    }

    /** Copia clima, generación y animales del bioma vanilla y aplica el ambiente nuevo. */
    private static Biome retheme(Biome vanilla, Look look) {
        BiomeSpecialEffects base = vanilla.getSpecialEffects();
        BiomeSpecialEffects.Builder fx = new BiomeSpecialEffects.Builder()
                .skyColor(look.sky).fogColor(look.fog).waterColor(look.water).waterFogColor(look.waterFog)
                .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS);
        if (look.grass != null) {
            fx.grassColorOverride(look.grass);
        }
        if (look.foliage != null) {
            fx.foliageColorOverride(look.foliage);
        }
        if (look.particle != null) {
            fx.ambientParticle(new AmbientParticleSettings(look.particle, look.particleChance));
        }
        if (look.additions > 0) {
            fx.ambientAdditionsSound(new AmbientAdditionsSettings(SoundEvents.AMBIENT_BASALT_DELTAS_ADDITIONS, look.additions));
        }
        base.getBackgroundMusic().ifPresent(fx::backgroundMusic);

        var climate = vanilla.getModifiedClimateSettings();
        return new Biome.BiomeBuilder()
                .hasPrecipitation(climate.hasPrecipitation())
                .temperature(climate.temperature())
                .temperatureAdjustment(climate.temperatureModifier())
                .downfall(climate.downfall())
                .specialEffects(fx.build())
                .mobSpawnSettings(vanilla.getMobSettings())
                .generationSettings(vanilla.getGenerationSettings())
                .build();
    }

    private static Look look(int sky, int fog, int water, int waterFog) {
        return new Look(sky, fog, water, waterFog);
    }

    /** Ambiente de un bioma (colores, partículas, sonidos). */
    private static final class Look {
        final int sky, fog, water, waterFog;
        Integer grass, foliage;
        ParticleOptions particle;
        float particleChance;
        double additions;

        Look(int sky, int fog, int water, int waterFog) {
            this.sky = sky;
            this.fog = fog;
            this.water = water;
            this.waterFog = waterFog;
        }

        Look grass(int c) {
            grass = c;
            return this;
        }

        Look foliage(int c) {
            foliage = c;
            return this;
        }

        Look particle(ParticleOptions p, float chance) {
            particle = p;
            particleChance = chance;
            return this;
        }

        Look additions(double chance) {
            additions = chance;
            return this;
        }
    }
}
