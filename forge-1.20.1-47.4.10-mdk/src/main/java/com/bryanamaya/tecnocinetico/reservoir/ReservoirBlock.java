package com.bryanamaya.tecnocinetico.reservoir;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Reservorio Universal: clic con un cubo = llenar/vaciar; clic con la mano vacía = interfaz. Al romperlo conserva el contenido. */
public class ReservoirBlock extends Block implements EntityBlock {

    public ReservoirBlock() {
        super(Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(5.0f, 8.0f).sound(SoundType.NETHERITE_BLOCK).noOcclusion());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReservoirBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != ReservoirRegistry.RESERVOIR_BE.get() ? null
                : (l, p, s, be) -> ReservoirBlockEntity.serverTick(l, p, s, (ReservoirBlockEntity) be);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ReservoirBlockEntity r)) {
            return InteractionResult.PASS;
        }
        if (FluidUtil.getFluidHandler(player.getItemInHand(hand)).isPresent()) {
            if (!level.isClientSide) {
                r.useContainer(player, hand);
            }
        } else if (!level.isClientSide && player instanceof ServerPlayer sp) {
            NetworkHooks.openScreen(sp, r, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Descripción del objeto con lo que lleva dentro (viene en BlockEntityTag al romperlo). */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag be = stack.getTagElement("BlockEntityTag");
        if (be == null || be.getList(ReservoirBlockEntity.TAG, Tag.TAG_COMPOUND).isEmpty()) {
            tooltip.add(Component.translatable("tooltip.tecnocinetico.reservoir_empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        for (Tag t : be.getList(ReservoirBlockEntity.TAG, Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.tryParse(c.getString("Fluid")));
            Component name = fluid == null ? Component.literal(c.getString("Fluid"))
                    : fluid.getFluidType().getDescription(new net.minecraftforge.fluids.FluidStack(fluid, 1000));
            tooltip.add(name.copy().append(": " + ReservoirScreenText.amount(c.getLong("Amount"))).withStyle(ChatFormatting.AQUA));
        }
    }
}
