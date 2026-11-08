package com.bryanamaya.tecnocinetico.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import com.bryanamaya.tecnocinetico.block.custom.WeaponAssemblerBlock;
import com.bryanamaya.tecnocinetico.menu.WeaponAssemblerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Ensamblador de Armas (tier 3): máquina eléctrica que monta las armas tier 3 y su munición especial
 * ({@link AssemblerRecipes}). Huecos 0-5: entrada. Hueco 6: salida. Gasta 100 FE/tick (30.000-40.000 FE por arma).
 * Automatización igual que la prensa: entrada por arriba y los lados, salida por abajo.
 */
public class WeaponAssemblerBlockEntity extends BlockEntity implements MenuProvider {

    public static final int INPUT_SLOTS = 6;
    public static final int OUTPUT_SLOT = 6;

    // --- Electricidad (Forge Energy, la que generan Create Crafts & Additions y otros mods) ---
    public static final int ENERGY_CAPACITY = 60_000;
    public static final int MAX_RECEIVE = 2_000;      // FE por tick que acepta como máximo
    public static final int ENERGY_PER_TICK = 100;

    private int progress = 0, maxProgress = 0;

    // --- Datos que se sincronizan con la interfaz (valores de 16 bits: caben hasta 32.767) ---
    // la energía se manda dividida entre 10 (60.000 no cabe en 16 bits)
    public static final int DATA_ENERGY = 0;
    public static final int DATA_PROGRESS = 1;
    public static final int DATA_MAX = 2;
    public static final int DATA_COUNT = 3;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_ENERGY -> energy.getEnergyStored() / 10;
                case DATA_PROGRESS -> progress;
                default -> maxProgress;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_ENERGY -> energy.set(value * 10);
                case DATA_PROGRESS -> progress = value;
                default -> maxProgress = value;
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    /** Batería interna: solo recibe energía desde fuera; la máquina la gasta con consume(). */
    private final MachineEnergy energy = new MachineEnergy();

    private class MachineEnergy extends EnergyStorage {
        MachineEnergy() {
            super(ENERGY_CAPACITY, MAX_RECEIVE, 0);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
            }
            return received;
        }

        boolean consume(int amount) {
            if (energy < amount) {
                return false;
            }
            energy -= amount;
            return true;
        }

        void set(int value) {
            energy = Math.max(0, Math.min(capacity, value));
        }
    }

    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

    private final ItemStackHandler itemHandler = new ItemStackHandler(INPUT_SLOTS + 1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == OUTPUT_SLOT || !AssemblerRecipes.isIngredient(stack)) {
                return false;
            }
            // No permitir el mismo ingrediente en dos huecos distintos: evita que una tolva atasque la máquina
            for (int i = 0; i < INPUT_SLOTS; i++) {
                if (i != slot && getStackInSlot(i).is(stack.getItem())) {
                    return false;
                }
            }
            return true;
        }
    };

    private final IItemHandler inputView = new SidedView(true);
    private final IItemHandler outputView = new SidedView(false);

    private LazyOptional<IItemHandler> lazyFull = LazyOptional.empty();
    private LazyOptional<IItemHandler> lazyInput = LazyOptional.empty();
    private LazyOptional<IItemHandler> lazyOutput = LazyOptional.empty();

    public WeaponAssemblerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.WEAPON_ASSEMBLER_BE.get(), pPos, pBlockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WeaponAssemblerBlockEntity entity) {
        if (level.isClientSide()) {
            return;
        }
        // Luz de estado del bloque: encendida (cian) si hay electricidad suficiente
        boolean powered = entity.energy.getEnergyStored() >= ENERGY_PER_TICK;
        if (state.getValue(WeaponAssemblerBlock.POWERED) != powered) {
            level.setBlock(pos, state.setValue(WeaponAssemblerBlock.POWERED, powered), Block.UPDATE_CLIENTS);
        }
        AssemblerRecipes.Recipe recipe = AssemblerRecipes.find(entity.itemHandler, INPUT_SLOTS);
        ItemStack result = recipe == null ? ItemStack.EMPTY : recipe.output().copy();
        if (result.isEmpty() || !entity.canOutput(result)) {
            if (entity.progress != 0 || entity.maxProgress != 0) {
                entity.progress = 0;
                entity.maxProgress = 0;
                entity.setChanged();
            }
            return;
        }
        entity.maxProgress = recipe.ticks();

        // Sin electricidad la prensa se detiene (y conserva su progreso)
        if (!entity.energy.consume(ENERGY_PER_TICK)) {
            return;
        }
        entity.progress++;
        if (entity.progress >= recipe.ticks()) {
            entity.progress = 0;
            AssemblerRecipes.consume(entity.itemHandler, INPUT_SLOTS, recipe);
            ItemStack out = entity.itemHandler.getStackInSlot(OUTPUT_SLOT);
            if (out.isEmpty()) {
                entity.itemHandler.setStackInSlot(OUTPUT_SLOT, result);
            } else {
                out.grow(result.getCount());
                entity.itemHandler.setStackInSlot(OUTPUT_SLOT, out); // dispara onContentsChanged
            }
            level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8f, 0.8f);
        } else if (entity.progress % 20 == 0) {
            level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.25f, 1.6f);   // brazos trabajando
        }
        entity.setChanged();
    }

    private boolean canOutput(ItemStack result) {
        ItemStack out = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (out.isEmpty()) {
            return true;
        }
        return ItemHandlerHelper.canItemStacksStack(out, result)
                && out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    public int getProgress() {
        return progress;
    }

    // --- Interfaz ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tecnocinetico.weapon_assembler");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new WeaponAssemblerMenu(id, inventory, itemHandler, data,
                ContainerLevelAccess.create(level, worldPosition));
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        pTag.put("inventory", itemHandler.serializeNBT());
        pTag.putInt("progress", this.progress);
        pTag.putInt("maxProgress", this.maxProgress);
        pTag.putInt("energy", energy.getEnergyStored());
        super.saveAdditional(pTag);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        CompoundTag inv = pTag.getCompound("inventory");
        // Compatibilidad con mundos antiguos (3 huecos): se fuerza el tamaño nuevo
        inv.putInt("Size", INPUT_SLOTS + 1);
        itemHandler.deserializeNBT(inv);
        this.progress = pTag.getInt("progress");
        this.maxProgress = pTag.getInt("maxProgress");
        energy.set(pTag.getInt("energy"));
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (side == null) {
                return lazyFull.cast();
            }
            return side == Direction.DOWN ? lazyOutput.cast() : lazyInput.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return lazyEnergy.cast(); // acepta electricidad por cualquier cara
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyFull = LazyOptional.of(() -> itemHandler);
        lazyInput = LazyOptional.of(() -> inputView);
        lazyOutput = LazyOptional.of(() -> outputView);
        lazyEnergy = LazyOptional.of(() -> energy);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyFull.invalidate();
        lazyInput.invalidate();
        lazyOutput.invalidate();
        lazyEnergy.invalidate();
    }

    /** Vista del inventario para una cara: solo entrada (ingredientes) o solo salida (producto). */
    private class SidedView implements IItemHandler {
        private final boolean input;

        SidedView(boolean input) {
            this.input = input;
        }

        @Override
        public int getSlots() {
            return itemHandler.getSlots();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return itemHandler.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!input || slot == OUTPUT_SLOT) {
                return stack;
            }
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (input || slot != OUTPUT_SLOT) {
                return ItemStack.EMPTY;
            }
            return itemHandler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return input && itemHandler.isItemValid(slot, stack);
        }
    }
}
