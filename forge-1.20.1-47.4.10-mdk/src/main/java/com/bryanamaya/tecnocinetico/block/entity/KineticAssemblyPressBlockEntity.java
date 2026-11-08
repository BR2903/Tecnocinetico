package com.bryanamaya.tecnocinetico.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KineticAssemblyPressBlockEntity extends BlockEntity {

    public static final int SLOTS = 3;
    public static final int STRIKES_NEEDED = 3;

    public enum StrikeResult { NO_RECIPE, STRUCK, CRAFTED }

    private int strikeCount = 0; // Martillazos acumulados sobre la receta actual

    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            strikeCount = 0; // Si cambian los materiales, se reinicia el trabajo
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return PressRecipes.isIngredient(stack, false);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1; // La prensa manual trabaja pieza a pieza
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    public KineticAssemblyPressBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.KINETIC_ASSEMBLY_PRESS_BE.get(), pPos, pBlockState);
    }

    /** Se llama cuando el jugador golpea la prensa con el Martillo Cinético (3 golpes por defecto). */
    public StrikeResult strike(Level level, BlockPos pos) {
        return strike(level, pos, STRIKES_NEEDED);
    }

    /** Golpe de martillo; strikesNeeded depende de la Ingeniería del jugador. */
    public StrikeResult strike(Level level, BlockPos pos, int strikesNeeded) {
        ItemStack result = PressRecipes.findResult(itemHandler, SLOTS, false);
        if (result.isEmpty()) {
            strikeCount = 0;
            return StrikeResult.NO_RECIPE;
        }

        strikeCount++;
        if (strikeCount < strikesNeeded) {
            setChanged();
            return StrikeResult.STRUCK;
        }

        PressRecipes.consumeInputs(itemHandler, SLOTS); // esto ya reinicia strikeCount
        strikeCount = 0;
        Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D, result);
        setChanged();
        return StrikeResult.CRAFTED;
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        pTag.put("inventory", itemHandler.serializeNBT());
        pTag.putInt("strikes", this.strikeCount);
        super.saveAdditional(pTag);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));
        this.strikeCount = pTag.getInt("strikes");
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }
}
