package com.bryanamaya.tecnocinetico.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

    // Instanciamos la memoria: Un inventario interno de exactamente 3 espacios.
    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged(); // Le avisa al juego que debe guardar la partida si meten o sacan un ítem
        }
    };

    // Un puntero seguro para que otros sistemas (o tolvas) puedan ver nuestro inventario
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    public KineticAssemblyPressBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.KINETIC_ASSEMBLY_PRESS_BE.get(), pPos, pBlockState);
    }

    // Le exponemos el inventario al motor de Forge
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    // Cuando el bloque carga en el mundo, activamos la memoria
    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    // Cuando rompes el bloque, destruimos el puntero para no causar fugas de memoria
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }
}