package com.bryanamaya.tecnocinetico.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Celda Universal de Fluidos (como la de IndustrialCraft 2): guarda 1 cubo de CUALQUIER líquido (vanilla, del mod o
 * de otros mods). Vacías se apilan hasta 64; las llenas del mismo líquido también. Clic derecho: recoge una fuente
 * de líquido (vacía) o la vierte (llena). Funciona con máquinas y tuberías (capacidad de fluidos de Forge) y el
 * modelo forge:fluid_container dibuja el líquido que lleva dentro.
 */
public class UniversalCellItem extends Item {

    public static final int CAPACITY = FluidType.BUCKET_VOLUME;

    public UniversalCellItem(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new FluidHandlerItemStack(stack, CAPACITY);
    }

    public static FluidStack contents(ItemStack stack) {
        return FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
    }

    @Override
    public Component getName(ItemStack stack) {
        FluidStack f = contents(stack);
        return f.isEmpty() ? super.getName(stack)
                : Component.translatable("item.tecnocinetico.universal_fluid_cell.filled", f.getDisplayName());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        FluidStack inside = contents(stack);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, inside.isEmpty() ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        BlockPos pos = hit.getBlockPos();
        if (!level.mayInteract(player, pos)) {
            return InteractionResultHolder.fail(stack);
        }
        if (inside.isEmpty()) {
            // recoger una fuente (solo si de verdad es un líquido con fuente)
            BlockState state = level.getBlockState(pos);
            FluidState fs = state.getFluidState();
            if (!fs.isSource() || !(state.getBlock() instanceof BucketPickup pickup)) {
                return InteractionResultHolder.pass(stack);
            }
            if (!level.isClientSide) {
                ItemStack bucket = pickup.pickupBlock(level, pos, state);
                FluidStack got = FluidUtil.getFluidContained(bucket).orElse(new FluidStack(fs.getType(), CAPACITY));
                ItemStack filled = new ItemStack(this);
                FluidUtil.getFluidHandler(filled).ifPresent(h -> h.fill(new FluidStack(got.getFluid(), CAPACITY), IFluidHandler.FluidAction.EXECUTE));
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0F, 1.2F);
                return InteractionResultHolder.sidedSuccess(ItemUtils.createFilledResult(stack, player, filled), false);
            }
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        // verter: delante de la cara tocada (o en el propio bloque si es reemplazable)
        BlockPos target = level.getBlockState(pos).canBeReplaced() ? pos : pos.relative(hit.getDirection());
        ItemStack single = stack.copyWithCount(1);
        var handler = FluidUtil.getFluidHandler(single).orElse(null);
        if (handler == null || !FluidUtil.tryPlaceFluid(player, level, hand, target, handler, inside.copy())) {
            return InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.sidedSuccess(ItemUtils.createFilledResult(stack, player, new ItemStack(this)), level.isClientSide);
    }
}
