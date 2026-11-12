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

import java.util.stream.Stream;

/**
 * Fuente de biomas del Nether del mod (tipo {@code tecnocinetico:infernal}): envuelve la fuente vanilla del Nether y
 * cambia cada bioma por su sustituto de {@link InfernalBiomes#MAP}. Mismo relieve y reparto vanilla, biomas nuevos.
 */
public final class InfernalBiomeSource extends BiomeSource {

    public static final Codec<InfernalBiomeSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            BiomeSource.CODEC.fieldOf("source").forGetter(s -> s.source),
            RegistryOps.retrieveGetter(Registries.BIOME)
    ).apply(i, i.stable(InfernalBiomeSource::new)));

    private final BiomeSource source;
    private final HolderGetter<Biome> biomes;

    public InfernalBiomeSource(BiomeSource source, HolderGetter<Biome> biomes) {
        this.source = source;
        this.biomes = biomes;
    }

    private Holder<Biome> remap(Holder<Biome> value) {
        return value.unwrapKey().map(InfernalBiomes.MAP::get).<Holder<Biome>>flatMap(biomes::get).orElse(value);
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
