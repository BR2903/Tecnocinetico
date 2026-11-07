package com.bryanamaya.tecnocinetico.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fuente de Energía Creativa (SOLO PRUEBAS): energía infinita.
 * - Empuja Forge Energy a todos los bloques vecinos cada tick.
 * - También deja que cables/máquinas la "saquen" (extract) sin límite.
 */
public class CreativeEnergySourceBlockEntity extends BlockEntity {

    private static final int OUTPUT_PER_TICK = 100_000; // por cara

    /** Almacén infinito: siempre lleno, nunca se gasta, no acepta energía. */
    private final IEnergyStorage infinite = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return maxExtract;
        }

        @Override
        public int getEnergyStored() {
            return Integer.MAX_VALUE / 2;
        }

        @Override
        public int getMaxEnergyStored() {
            return Integer.MAX_VALUE / 2;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    };

    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

    public CreativeEnergySourceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_ENERGY_SOURCE_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CreativeEnergySourceBlockEntity entity) {
        if (level.isClientSide()) {
            return;
        }
        for (Direction dir : Direction.values()) {
            BlockEntity neighbour = level.getBlockEntity(pos.relative(dir));
            if (neighbour == null || neighbour instanceof CreativeEnergySourceBlockEntity) {
                continue;
            }
            neighbour.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(target -> {
                if (target.canReceive()) {
                    target.receiveEnergy(OUTPUT_PER_TICK, false);
                }
            });
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return lazyEnergy.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyEnergy = LazyOptional.of(() -> infinite);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyEnergy.invalidate();
    }
}
