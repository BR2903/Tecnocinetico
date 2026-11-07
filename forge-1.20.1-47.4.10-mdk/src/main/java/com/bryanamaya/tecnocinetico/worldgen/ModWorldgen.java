package com.bryanamaya.tecnocinetico.worldgen;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registro del generador del Overworld: el tipo de fuente de biomas "tecnocinetico:remapped" y los
 * ajustes de ruido (copias de los vanilla con nuestras reglas de superficie). Los presets de mundo
 * vanilla (data/minecraft/worldgen/world_preset) se sobrescriben para usarlos.
 */
public final class ModWorldgen {

    public static final DeferredRegister<Codec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, Tecnocinetico.MOD_ID);

    public static final RegistryObject<Codec<? extends BiomeSource>> REMAPPED =
            BIOME_SOURCES.register("remapped", () -> RemappedBiomeSource.CODEC);

    public static final ResourceKey<NoiseGeneratorSettings> OVERWORLD = noise("overworld");
    public static final ResourceKey<NoiseGeneratorSettings> LARGE_BIOMES = noise("large_biomes");
    public static final ResourceKey<NoiseGeneratorSettings> AMPLIFIED = noise("amplified");

    private ModWorldgen() {}

    public static void register(IEventBus bus) {
        BIOME_SOURCES.register(bus);
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
