package com.bryanamaya.tecnocinetico.machine;

import com.bryanamaya.tecnocinetico.menu.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base de las máquinas eléctricas: batería FE (entra por cualquier cara), inventario de entradas + salidas
 * (las tolvas/tuberías meten por arriba y los lados y sacan por abajo), progreso, luz de estado e interfaz.
 * Cada máquina implementa {@link #work()}.
 */
public abstract class ElectricMachineBlockEntity extends BlockEntity implements MenuProvider {

    public static final int DATA_ENERGY = 0, DATA_PROGRESS = 1, DATA_MAX = 2, DATA_USAGE = 3, DATA_COUNT = 4;

    protected final MachineKind kind;
    protected int progress, maxProgress, usage;

    protected final Battery energy;
    protected final ItemStackHandler items;
    private final IItemHandler inputView, outputView;
    private LazyOptional<IItemHandler> lazyFull = LazyOptional.empty(), lazyIn = LazyOptional.empty(), lazyOut = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

    /** Datos para la interfaz (16 bits: la energía va dividida entre 10). */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case DATA_ENERGY -> energy.getEnergyStored() / 10;
                case DATA_PROGRESS -> progress;
                case DATA_MAX -> maxProgress;
                default -> usage;
            };
        }

        @Override
        public void set(int i, int v) {
            switch (i) {
                case DATA_ENERGY -> energy.set(v * 10);
                case DATA_PROGRESS -> progress = v;
                case DATA_MAX -> maxProgress = v;
                default -> usage = v;
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    protected ElectricMachineBlockEntity(BlockEntityType<?> type, MachineKind kind, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.kind = kind;
        this.energy = new Battery(kind.capacity, kind.maxReceive);
        this.items = new ItemStackHandler(kind.slots()) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return !kind.isOutput(slot) && accepts(slot, stack);
            }
        };
        this.inputView = new SidedView(true);
        this.outputView = new SidedView(false);
    }

    /** ¿Se puede meter este objeto en este hueco de entrada? */
    protected abstract boolean accepts(int slot, ItemStack stack);

    /** Un tick de trabajo en el servidor. Devuelve si la máquina está trabajando (para la luz). */
    protected abstract boolean work();

    public static void serverTick(Level level, BlockPos pos, BlockState state, ElectricMachineBlockEntity be) {
        boolean working = be.work();
        if (!working && be.progress != 0) {
            be.progress = 0;
            be.setChanged();
        }
        boolean lit = be.energy.getEnergyStored() > 0;
        if (state.hasProperty(ElectricMachineBlock.POWERED) && state.getValue(ElectricMachineBlock.POWERED) != lit) {
            level.setBlock(pos, state.setValue(ElectricMachineBlock.POWERED, lit), Block.UPDATE_CLIENTS);
        }
    }

    // ---------------------------------------------------------------- ayudas para las máquinas
    protected ItemStack input(int i) {
        return items.getStackInSlot(i);
    }

    protected int outSlot(int i) {
        return kind.inputs.length + i;
    }

    /** ¿Cabe `stack` en la salida i? */
    protected boolean fits(int i, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        ItemStack out = items.getStackInSlot(outSlot(i));
        return out.isEmpty() || ItemHandlerHelper.canItemStacksStack(out, stack) && out.getCount() + stack.getCount() <= out.getMaxStackSize();
    }

    protected void output(int i, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack out = items.getStackInSlot(outSlot(i));
        if (out.isEmpty()) {
            items.setStackInSlot(outSlot(i), stack.copy());
        } else {
            out.grow(stack.getCount());
            items.setStackInSlot(outSlot(i), out);
        }
    }

    /**
     * Avanza un trabajo de `ticks` ticks a `perTick` FE. Devuelve true el tick en que termina.
     * Si falta energía la máquina se para y conserva el progreso.
     */
    protected boolean advance(int ticks, int perTick) {
        maxProgress = ticks;
        usage = perTick;
        if (!energy.consume(perTick)) {
            return false;
        }
        progress++;
        setChanged();
        if (progress >= ticks) {
            progress = 0;
            return true;
        }
        return false;
    }

    public IItemHandler getItems() {
        return items;
    }

    public IEnergyStorage getEnergy() {
        return energy;
    }

    public MachineKind kind() {
        return kind;
    }

    // ---------------------------------------------------------------- interfaz
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tecnocinetico." + kind.id);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MachineMenu(kind, id, inventory, items, data, ContainerLevelAccess.create(level, worldPosition));
    }

    // ---------------------------------------------------------------- guardado
    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", items.serializeNBT());
        tag.putInt("progress", progress);
        tag.putInt("energy", energy.getEnergyStored());
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        CompoundTag inv = tag.getCompound("inventory");
        inv.putInt("Size", kind.slots());
        items.deserializeNBT(inv);
        progress = tag.getInt("progress");
        energy.set(tag.getInt("energy"));
    }

    // ---------------------------------------------------------------- capacidades
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return side == null ? lazyFull.cast() : side == Direction.DOWN ? lazyOut.cast() : lazyIn.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return lazyEnergy.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyFull = LazyOptional.of(() -> items);
        lazyIn = LazyOptional.of(() -> inputView);
        lazyOut = LazyOptional.of(() -> outputView);
        lazyEnergy = LazyOptional.of(() -> energy);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyFull.invalidate();
        lazyIn.invalidate();
        lazyOut.invalidate();
        lazyEnergy.invalidate();
    }

    /** Batería interna: solo recibe desde fuera; la máquina gasta con consume(). */
    public class Battery extends EnergyStorage {
        Battery(int capacity, int maxReceive) {
            super(capacity, maxReceive, 0);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int r = super.receiveEnergy(maxReceive, simulate);
            if (r > 0 && !simulate) {
                setChanged();
            }
            return r;
        }

        public boolean consume(int amount) {
            if (energy < amount) {
                return false;
            }
            energy -= amount;
            return true;
        }

        /** Gasta hasta `amount` y devuelve lo gastado. */
        public int drain(int amount) {
            int d = Math.min(energy, amount);
            energy -= d;
            return d;
        }

        public void set(int value) {
            energy = Math.max(0, Math.min(capacity, value));
        }
    }

    /** Vista por cara: solo entradas (meter) o solo salidas (sacar). */
    private class SidedView implements IItemHandler {
        private final boolean input;

        SidedView(boolean input) {
            this.input = input;
        }

        @Override
        public int getSlots() {
            return items.getSlots();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return input && !kind.isOutput(slot) ? items.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return !input && kind.isOutput(slot) ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return input && items.isItemValid(slot, stack);
        }
    }
}
