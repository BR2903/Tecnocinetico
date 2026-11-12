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

    /** Features con código propio (los JSON que las usan los genera tools/world_features_gen.py). */
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, Tecnocinetico.MOD_ID);
    public static final RegistryObject<CraterFeature> CRATER = FEATURES.register("crater", CraterFeature::new);
    public static final RegistryObject<RuinWallFeature> RUIN_WALL = FEATURES.register("ruin_wall", RuinWallFeature::new);
    public static final RegistryObject<DeadTreeFeature> DEAD_TREE = FEATURES.register("dead_tree", DeadTreeFeature::new);
    /** Farolas y postes eléctricos abandonados (de pie, doblados o caídos). */
    public static final RegistryObject<com.bryanamaya.tecnocinetico.worldgen.feature.StreetPropFeature> STREET_PROP =
            FEATURES.register("street_prop", com.bryanamaya.tecnocinetico.worldgen.feature.StreetPropFeature::new);

    public static final ResourceKey<NoiseGeneratorSettings> OVERWORLD = noise("overworld");
    public static final ResourceKey<NoiseGeneratorSettings> LARGE_BIOMES = noise("large_biomes");
    public static final ResourceKey<NoiseGeneratorSettings> AMPLIFIED = noise("amplified");

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
    }

    private static NoiseGeneratorSettings withModSurface(NoiseGeneratorSettings v) {
        return new NoiseGeneratorSettings(v.noiseSettings(), v.defaultBlock(), v.defaultFluid(), v.noiseRouter(),
                ModSurfaceRules.overworld(), v.spawnTarget(), v.seaLevel(), v.disableMobGeneration(),
                v.aquifersEnabled(), v.oreVeinsEnabled(), v.useLegacyRandomSource());
    }
}
