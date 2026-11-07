package com.bryanamaya.tecnocinetico.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Capa de Ceniza: se apila por capas (1-8) como la nieve, pero NO se derrite con la luz.
 * Típica de la Taiga de Ceniza y de las zonas de batalla.
 */
public class AshLayerBlock extends SnowLayerBlock {

    public AshLayerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return false;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // La ceniza no se derrite
    }
}
