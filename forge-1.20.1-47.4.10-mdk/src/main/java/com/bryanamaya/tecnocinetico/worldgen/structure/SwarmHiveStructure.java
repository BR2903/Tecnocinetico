package com.bryanamaya.tecnocinetico.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Colmena del Enjambre: cúpula irregular de Panal de Enjambre medio enterrada, con entradas, cristales de
 * cinetrita, botín, el Nodo de Colmena en el centro y su enjambre inicial. La construye {@link SwarmHivePiece}.
 */
public class SwarmHiveStructure extends Structure {

    public static final Codec<SwarmHiveStructure> CODEC = RecordCodecBuilder.create(i -> i.group(
            settingsCodec(i)
    ).apply(i, SwarmHiveStructure::new));

    public SwarmHiveStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkGenerator gen = ctx.chunkGenerator();
        ChunkPos cp = ctx.chunkPos();
        int x = cp.getMiddleBlockX(), z = cp.getMiddleBlockZ();
        int y = gen.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        NoiseColumn col = gen.getBaseColumn(x, z, ctx.heightAccessor(), ctx.randomState());
        if (!col.getBlock(y).getFluidState().isEmpty() || y < gen.getSeaLevel()) {
            return Optional.empty();
        }
        var random = ctx.random();
        int rx = 6 + random.nextInt(3), ry = 5 + random.nextInt(2), rz = 6 + random.nextInt(3);
        long seed = random.nextLong();
        BlockPos ground = new BlockPos(x, y, z);
        return Optional.of(new GenerationStub(ground, b -> b.addPiece(new SwarmHivePiece(ground, rx, ry, rz, seed))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SWARM_HIVE.get();
    }
}
