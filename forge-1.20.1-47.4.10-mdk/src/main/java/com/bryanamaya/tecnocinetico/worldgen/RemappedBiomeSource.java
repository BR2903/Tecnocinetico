package com.bryanamaya.tecnocinetico.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Fuente de biomas que envuelve otra (la multi-ruido vanilla del Overworld) y sustituye cada bioma vanilla por
 * el del mod según {@link ModBiomes#VANILLA_TO_MOD}. Así el terreno conserva la forma vanilla (montañas, ríos,
 * océanos en el mismo sitio) pero los biomas son los post-apocalípticos. Los biomas sin sustituto
 * (p. ej. de otros mods) pasan tal cual.
 *
 * JSON: {"type": "tecnocinetico:remapped", "source": { ...fuente original... }}
 */
public class RemappedBiomeSource extends BiomeSource {

    public static final Codec<RemappedBiomeSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            BiomeSource.CODEC.fieldOf("source").forGetter(s -> s.source),
            RegistryOps.retrieveGetter(Registries.BIOME)
    ).apply(i, i.stable(RemappedBiomeSource::new)));

    private final BiomeSource source;
    private final HolderGetter<Biome> biomes;
    /** Caché por identidad del holder (los holders de un registro son únicos). */
    private final Map<Holder<Biome>, Holder<Biome>> cache = new ConcurrentHashMap<>();

    public RemappedBiomeSource(BiomeSource source, HolderGetter<Biome> biomes) {
        this.source = source;
        this.biomes = biomes;
    }

    private Holder<Biome> remap(Holder<Biome> original) {
        return cache.computeIfAbsent(original, h -> h.unwrapKey()
                .map(ModBiomes.VANILLA_TO_MOD::get)
                .<Holder<Biome>>flatMap(biomes::get)
                .orElse(h));
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return source.possibleBiomes().stream().map(this::remap).distinct();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        return remap(source.getNoiseBiome(x, y, z, sampler));
    }
}
