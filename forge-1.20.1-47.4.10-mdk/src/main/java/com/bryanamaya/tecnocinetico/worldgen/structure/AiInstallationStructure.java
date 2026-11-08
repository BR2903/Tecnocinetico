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
 * Instalación de la IA: búnker de entrada en la superficie y un complejo subterráneo de 3x3 salas (la central es
 * la cámara del Núcleo) a 16 bloques de profundidad, vigilado por Torretas Centinela. Ver {@link AiInstallationPiece}.
 */
public class AiInstallationStructure extends Structure {

    public static final Codec<AiInstallationStructure> CODEC = RecordCodecBuilder.create(i -> i.group(
            settingsCodec(i)
    ).apply(i, AiInstallationStructure::new));

    public AiInstallationStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkGenerator gen = ctx.chunkGenerator();
        ChunkPos cp = ctx.chunkPos();
        int x = cp.getMiddleBlockX(), z = cp.getMiddleBlockZ();
        int y = gen.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        NoiseColumn col = gen.getBaseColumn(x, z, ctx.heightAccessor(), ctx.randomState());
        if (!col.getBlock(y).getFluidState().isEmpty() || y < gen.getSeaLevel() + 2
                || y - AiInstallationPiece.DEPTH - AiInstallationPiece.ARENA_DEPTH < ctx.heightAccessor().getMinBuildHeight() + 6) {
            return Optional.empty();
        }
        long seed = ctx.random().nextLong();
        BlockPos entrance = new BlockPos(x, y, z);
        return Optional.of(new GenerationStub(entrance, b -> b.addPiece(new AiInstallationPiece(entrance, seed))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.AI_INSTALLATION.get();
    }
}
