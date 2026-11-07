package com.bryanamaya.tecnocinetico.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Asentamiento de Supervivientes (sustituye a las aldeas): recinto amurallado de {@link #SIZE}x{@link #SIZE}
 * con chozas, huertos, torre de vigía y plaza. Lo construye {@link SettlementPiece}.
 *
 * Aquí se elige el sitio: bioma válido, terreno sin agua y con desnivel moderado, y se guarda la altura del
 * terreno de cada columna (así cada chunk construye sobre las mismas alturas, sin depender del mundo).
 */
public class SettlementStructure extends Structure {

    public static final int SIZE = 33;
    public static final Codec<SettlementStructure> CODEC = RecordCodecBuilder.create(i -> i.group(
            settingsCodec(i)
    ).apply(i, SettlementStructure::new));

    public SettlementStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkGenerator gen = ctx.chunkGenerator();
        ChunkPos cp = ctx.chunkPos();
        int x0 = cp.getMinBlockX(), z0 = cp.getMinBlockZ();
        int c = SIZE / 2;
        int sea = gen.getSeaLevel();

        // 1) comprobaciones baratas: bioma en el centro y 9 puntos sin agua y sin mucho desnivel
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int sx = 0; sx < SIZE; sx += c) {
            for (int sz = 0; sz < SIZE; sz += c) {
                int top = surface(ctx, x0 + sx, z0 + sz, Heightmap.Types.WORLD_SURFACE_WG);
                int floor = surface(ctx, x0 + sx, z0 + sz, Heightmap.Types.OCEAN_FLOOR_WG);
                if (top != floor || top < sea) {
                    return Optional.empty();                              // agua
                }
                min = Math.min(min, top);
                max = Math.max(max, top);
            }
        }
        if (max - min > 7) {
            return Optional.empty();
        }
        int cy = surface(ctx, x0 + c, z0 + c, Heightmap.Types.WORLD_SURFACE_WG);
        Holder<Biome> biome = ctx.biomeSource().getNoiseBiome(QuartPos.fromBlock(x0 + c), QuartPos.fromBlock(cy),
                QuartPos.fromBlock(z0 + c), ctx.randomState().sampler());
        if (!ctx.validBiome().test(biome)) {
            return Optional.empty();
        }

        // 2) altura de cada columna (y agua cada 4 columnas)
        int[] heights = new int[SIZE * SIZE];
        for (int lx = 0; lx < SIZE; lx++) {
            for (int lz = 0; lz < SIZE; lz++) {
                int h = surface(ctx, x0 + lx, z0 + lz, Heightmap.Types.WORLD_SURFACE_WG);
                if (lx % 4 == 0 && lz % 4 == 0
                        && h != surface(ctx, x0 + lx, z0 + lz, Heightmap.Types.OCEAN_FLOOR_WG)) {
                    return Optional.empty();
                }
                heights[lx * SIZE + lz] = h;
                min = Math.min(min, h);
                max = Math.max(max, h);
            }
        }
        if (max - min > 10) {
            return Optional.empty();
        }
        long seed = ctx.random().nextLong();
        int lo = min, hi = max;
        BlockPos center = new BlockPos(x0 + c, cy, z0 + c);
        return Optional.of(new GenerationStub(center, b -> b.addPiece(new SettlementPiece(x0, z0, lo, hi, heights, seed))));
    }

    /** Y del bloque más alto de la columna según el heightmap (sin contar el aire). */
    private static int surface(GenerationContext ctx, int x, int z, Heightmap.Types type) {
        return ctx.chunkGenerator().getFirstOccupiedHeight(x, z, type, ctx.heightAccessor(), ctx.randomState());
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SETTLEMENT.get();
    }
}
