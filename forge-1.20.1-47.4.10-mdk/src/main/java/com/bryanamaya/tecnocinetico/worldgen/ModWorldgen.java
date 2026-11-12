package com.bryanamaya.tecnocinetico.worldgen;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import com.bryanamaya.tecnocinetico.worldgen.feature.CraterFeature;
import com.bryanamaya.tecnocinetico.worldgen.feature.DeadTreeFeature;
import com.bryanamaya.tecnocinetico.worldgen.feature.RuinWallFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registro del generador del Overworld: el tipo de fuente de biomas "tecnocinetico:remapped" y los
 * ajustes de ruido (copias de los vanilla con nuestras reglas de superficie). Los usan los tipos de mundo propios
 * del mod (data/tecnocinetico/worldgen/world_preset/post_apocalyptic*.json, en la lista de "Tipo de mundo" por la
 * etiqueta data/minecraft/tags/worldgen/world_preset/normal.json); los tipos de mundo vanilla quedan intactos.
 */
public final class ModWorldgen {

    public static final DeferredRegister<Codec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, Tecnocinetico.MOD_ID);

    public static final RegistryObject<Codec<? extends BiomeSource>> REMAPPED =
            BIOME_SOURCES.register("remapped", () -> RemappedBiomeSource.CODEC);
    /** Nether industrial: la fuente vanilla del Nether con los biomas del mod (InfernalBiomes). */
    public static final RegistryObject<Codec<? extends BiomeSource>> INFERNAL =
            BIOME_SOURCES.register("infernal", () -> InfernalBiomeSource.CODEC);

    /** Features con código propio (los JSON que las usan los genera tools/world_features_gen.py). */
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, Tecnocinetico.MOD_ID);
    public static final RegistryObject<CraterFeature> CRATER = FEATURES.register("crater", CraterFeature::new);
    public static final RegistryObject<RuinWallFeature> RUIN_WALL = FEATURES.register("ruin_wall", RuinWallFeature::new);
    public static final RegistryObject<DeadTreeFeature> DEAD_TREE = FEATURES.register("dead_tree", DeadTreeFeature::new);
    /** Carreteras rotas: calles de la Megaciudad y autopistas (una vez por chunk, solo en su chunk). */
    public static final RegistryObject<com.bryanamaya.tecnocinetico.worldgen.feature.RoadFeature> ROAD =
            FEATURES.register("road", com.bryanamaya.tecnocinetico.worldgen.feature.RoadFeature::new);
    /** Vehículos abandonados, sobre todo en las carreteras. */
    public static final RegistryObject<com.bryanamaya.tecnocinetico.worldgen.feature.VehicleFeature> VEHICLE =
            FEATURES.register("vehicle", com.bryanamaya.tecnocinetico.worldgen.feature.VehicleFeature::new);
    /** Costras, columnas y árboles carbonizados del Nether industrial. */
    public static final RegistryObject<com.bryanamaya.tecnocinetico.worldgen.feature.InfernalDepositFeature> INFERNAL_DEPOSITS =
            FEATURES.register("infernal_deposits", com.bryanamaya.tecnocinetico.worldgen.feature.InfernalDepositFeature::new);
    /** Farolas y postes eléctricos abandonados (de pie, doblados o caídos). */
    public static final RegistryObject<com.bryanamaya.tecnocinetico.worldgen.feature.StreetPropFeature> STREET_PROP =
            FEATURES.register("street_prop", com.bryanamaya.tecnocinetico.worldgen.feature.StreetPropFeature::new);

    public static final ResourceKey<NoiseGeneratorSettings> OVERWORLD = noise("overworld");
    public static final ResourceKey<NoiseGeneratorSettings> LARGE_BIOMES = noise("large_biomes");
    public static final ResourceKey<NoiseGeneratorSettings> AMPLIFIED = noise("amplified");
    /** Nether vanilla con las reglas de superficie traducidas a los biomas del mod. */
    public static final ResourceKey<NoiseGeneratorSettings> INFERNAL_NETHER = noise("infernal_nether");

    private ModWorldgen() {}

    public static void register(IEventBus bus) {
        BIOME_SOURCES.register(bus);
        FEATURES.register(bus);
    }

    private static ResourceKey<NoiseGeneratorSettings> noise(String name) {
        return ResourceKey.create(Registries.NOISE_SETTINGS,
                ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, name));
    }

    /** Datagen: mismas formas de terreno vanilla, con las reglas de superficie del mod. */
    public static void bootstrapNoise(BootstapContext<NoiseGeneratorSettings> context) {
        context.register(OVERWORLD, withModSurface(NoiseGeneratorSettings.overworld(context, false, false)));
        context.register(LARGE_BIOMES, withModSurface(NoiseGeneratorSettings.overworld(context, false, true)));
        context.register(AMPLIFIED, withModSurface(NoiseGeneratorSettings.overworld(context, true, false)));
        NoiseGeneratorSettings n = NoiseGeneratorSettings.nether(context);
        context.register(INFERNAL_NETHER, new NoiseGeneratorSettings(n.noiseSettings(), n.defaultBlock(), n.defaultFluid(), n.noiseRouter(),
                InfernalBiomes.surface(), n.spawnTarget(), n.seaLevel(), n.disableMobGeneration(), n.aquifersEnabled(),
                n.oreVeinsEnabled(), n.useLegacyRandomSource()));
    }

    private static NoiseGeneratorSettings withModSurface(NoiseGeneratorSettings v) {
        return new NoiseGeneratorSettings(v.noiseSettings(), v.defaultBlock(), v.defaultFluid(), v.noiseRouter(),
                ModSurfaceRules.overworld(), v.spawnTarget(), v.seaLevel(), v.disableMobGeneration(),
                v.aquifersEnabled(), v.oreVeinsEnabled(), v.useLegacyRandomSource());
    }
}
