package com.bryanamaya.tecnocinetico.block.custom;

import com.bryanamaya.tecnocinetico.block.entity.CoreConsoleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Consola del Núcleo: irrompible; clic derecho despierta a la Araña de Asedio ({@link CoreConsoleBlockEntity}). */
public class CoreConsoleBlock extends Block implements EntityBlock {

    public CoreConsoleBlock() {
        super(Properties.of().mapColor(MapColor.COLOR_CYAN).strength(-1.0F, 3600000.0F).noLootTable()
                .sound(SoundType.NETHERITE_BLOCK).lightLevel(s -> 7));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoreConsoleBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && hand == InteractionHand.MAIN_HAND && level.getBlockEntity(pos) instanceof CoreConsoleBlockEntity c) {
            c.activate(player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
