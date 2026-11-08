package com.bryanamaya.tecnocinetico.worldgen.structure;

import com.bryanamaya.tecnocinetico.worldgen.ModBiomes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Fábrica abandonada: nave industrial con maquinaria, pasarela, oficina, grúa y botín de Create.
 * Busca terreno seco y poco inclinado (la adaptación "beard_thin" rellena el resto). La construye {@link FactoryPiece}.
 */
public class FactoryStructure extends Structure {

    public static final Codec<FactoryStructure> CODEC = RecordCodecBuilder.create(i -> i.group(
            settingsCodec(i)
    ).apply(i, FactoryStructure::new));

    public FactoryStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkGenerator gen = ctx.chunkGenerator();
        var random = ctx.random();
        int w = 23 + random.nextInt(5) * 2;                       // 23..31
        int d = 15 + random.nextInt(4) * 2;                       // 15..21
        int h = 9 + random.nextInt(3);                            // 9..11
        ChunkPos cp = ctx.chunkPos();
        int x0 = cp.getMinBlockX(), z0 = cp.getMinBlockZ();

        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int[] c : new int[][]{{0, 0}, {w - 1, 0}, {0, d - 1}, {w - 1, d - 1}, {w / 2, d / 2}, {w / 2, 0}, {w / 2, d - 1}}) {
            int y = gen.getFirstOccupiedHeight(x0 + c[0], z0 + c[1], Heightmap.Types.WORLD_SURFACE_WG,
                    ctx.heightAccessor(), ctx.randomState());
            NoiseColumn col = gen.getBaseColumn(x0 + c[0], z0 + c[1], ctx.heightAccessor(), ctx.randomState());
            if (!col.getBlock(y).getFluidState().isEmpty() || y < gen.getSeaLevel()) {
                return Optional.empty();
            }
            min = Math.min(min, y);
            max = Math.max(max, y);
        }
        if (max - min > 6) {
            return Optional.empty();
        }
        int cx = x0 + w / 2, cz = z0 + d / 2;
        Holder<Biome> biome = ctx.biomeSource().getNoiseBiome(QuartPos.fromBlock(cx), QuartPos.fromBlock(min),
                QuartPos.fromBlock(cz), ctx.randomState().sampler());
        boolean overgrown = biome.is(ModBiomes.INDUSTRIAL_JUNGLE);
        // suelo a la altura media: beard_thin rellena por debajo y recorta por encima
        int floor = (min + max) / 2;
        BlockPos origin = new BlockPos(x0, floor, z0);
        long seed = random.nextLong();
        return Optional.of(new GenerationStub(origin,
                b -> b.addPiece(new FactoryPiece(origin, w, d, h, overgrown, seed))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.FACTORY.get();
    }
}
