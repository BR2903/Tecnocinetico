package com.bryanamaya.tecnocinetico.structure;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Kit de una estructura grande: al usarlo sobre el suelo despliega toda la estructura delante del jugador,
 * centrada y mirándole, si el volumen está libre (si no, avisa de qué estorba).
 */
public class StructureKitItem extends Item {

    private final Supplier<Block> core;
    private final String sizeText;

    public StructureKitItem(Supplier<Block> core, String sizeText, Properties properties) {
        super(properties);
        this.core = core;
        this.sizeText = sizeText;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (!(core.get() instanceof MultiblockMachineBlock mb) || ctx.getPlayer() == null) {
            return InteractionResult.FAIL;
        }
        MultiblockTemplate t = mb.template();
        Direction facing = ctx.getHorizontalDirection();
        BlockPos start = level.getBlockState(ctx.getClickedPos()).canBeReplaced() ? ctx.getClickedPos() : ctx.getClickedPos().relative(ctx.getClickedFace());
        BlockPos origin = start.relative(facing.getCounterClockWise(), t.width / 2);
        BlockPos obstacle = t.firstObstacle(level, origin, facing);
        if (obstacle != null) {
            if (!level.isClientSide) {
                BlockState s = level.getBlockState(obstacle);
                ctx.getPlayer().displayClientMessage(Component.translatable("message.tecnocinetico.kit_blocked", sizeText,
                        s.getBlock().getName(), obstacle.getX(), obstacle.getY(), obstacle.getZ()).withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            t.place(level, origin, facing, s -> s.setValue(MultiblockMachineBlock.FACING, facing.getOpposite()));
            level.playSound(null, start, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, 0.7F);
            if (!ctx.getPlayer().getAbilities().instabuild) {
                ctx.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tecnocinetico.kit", sizeText).withStyle(ChatFormatting.GRAY));
    }
}
