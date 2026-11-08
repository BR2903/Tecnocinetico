package com.bryanamaya.tecnocinetico.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/** Barrera de energía del Núcleo: sella la arena durante el combate contra el guardián. Irrompible y translúcida. */
public class CoreBarrierBlock extends Block {

    public CoreBarrierBlock() {
        super(Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(-1.0F, 3600000.0F).noLootTable()
                .noOcclusion().sound(SoundType.GLASS).lightLevel(s -> 10).pushReaction(PushReaction.BLOCK)
                .isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false));
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction side) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, side);
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
