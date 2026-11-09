package com.bryanamaya.tecnocinetico.reservoir;

import com.bryanamaya.tecnocinetico.machine.FluidMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Reservorio Universal: guarda CUALQUIER líquido, varios a la vez y en cualquier cantidad (contador de 64 bits;
 * hacia fuera cada uno se ve con hasta 2.147.483 cubos). Las tuberías/cubos meten cualquier líquido; lo que se saca
 * es el líquido elegido en la interfaz. Al romperlo conserva el contenido (botín copy_nbt -> BlockEntityTag).
 * No guarda los datos NBT de los líquidos (p. ej. pociones): los rechaza para no perder nada.
 */
public class ReservoirBlockEntity extends BlockEntity implements MenuProvider, FluidMachine {

    public static final String TAG = "Fluids";

    /** Un líquido guardado y su cantidad (mB). */
    public static final class Entry {
        public final Fluid fluid;
        public long amount;

        Entry(Fluid fluid, long amount) {
            this.fluid = fluid;
            this.amount = amount;
        }
    }

    private final List<Entry> fluids = new ArrayList<>();
    private int selected;
    private boolean dirty;
    private LazyOptional<IFluidHandler> lazy = LazyOptional.empty();

    private final IFluidHandler handler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return fluids.size() + 1;                                          // +1 hueco libre para un líquido nuevo
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return tank < fluids.size() ? stack(fluids.get(tank)) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return !stack.hasTag();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return insert(resource, action.execute());
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            for (Entry e : fluids) {
                if (e.fluid.isSame(resource.getFluid())) {
                    return extract(e, resource.getAmount(), action.execute());
                }
            }
            return FluidStack.EMPTY;
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            Entry e = selectedEntry();
            return e == null ? FluidStack.EMPTY : extract(e, maxDrain, action.execute());
        }
    };

    public ReservoirBlockEntity(BlockPos pos, BlockState state) {
        super(ReservoirRegistry.RESERVOIR_BE.get(), pos, state);
    }

    // ---------------------------------------------------------------- contenido
    public List<Entry> fluids() {
        return fluids;
    }

    public int selected() {
        return selected;
    }

    @Nullable
    public Entry selectedEntry() {
        return fluids.isEmpty() ? null : fluids.get(Math.min(selected, fluids.size() - 1));
    }

    private static FluidStack stack(Entry e) {
        return new FluidStack(e.fluid, (int) Math.min(Integer.MAX_VALUE, e.amount));
    }

    public int insert(FluidStack resource, boolean execute) {
        if (resource.isEmpty() || resource.hasTag() || resource.getFluid() == Fluids.EMPTY) {
            return 0;
        }
        if (execute) {
            Entry target = null;
            for (Entry e : fluids) {
                if (e.fluid.isSame(resource.getFluid())) {
                    target = e;
                }
            }
            if (target == null) {
                target = new Entry(resource.getFluid(), 0);
                fluids.add(target);
            }
            target.amount = Math.min(Long.MAX_VALUE / 2, target.amount + resource.getAmount());
            changed();
        }
        return resource.getAmount();
    }

    private FluidStack extract(Entry e, int amount, boolean execute) {
        int got = (int) Math.min(amount, e.amount);
        if (got <= 0) {
            return FluidStack.EMPTY;
        }
        if (execute) {
            e.amount -= got;
            if (e.amount <= 0) {
                fluids.remove(e);
            }
            changed();
        }
        return new FluidStack(e.fluid, got);
    }

    /** Botones de la interfaz: 0..99 = elegir el líquido de salida; 100+i = desechar el líquido i. */
    public void onButton(int id) {
        if (id >= 100) {
            int i = id - 100;
            if (i < fluids.size()) {
                fluids.remove(i);
            }
        } else if (id < fluids.size()) {
            selected = id;
        }
        changed();
    }

    private void changed() {
        if (selected >= fluids.size()) {
            selected = Math.max(0, fluids.size() - 1);
        }
        dirty = true;
        setChanged();
    }

    /** Envía el contenido al cliente como mucho cada 10 ticks (las tuberías lo cambian a cada tick). */
    public static void serverTick(Level level, BlockPos pos, BlockState state, ReservoirBlockEntity be) {
        if (be.dirty && level.getGameTime() % 10 == 0) {
            be.dirty = false;
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    @Override
    public boolean useContainer(Player player, InteractionHand hand) {
        return FluidUtil.getFluidHandler(player.getItemInHand(hand)).isPresent() && FluidUtil.interactWithFluidHandler(player, hand, handler);
    }

    public IFluidHandler fluidHandler() {
        return handler;
    }

    // ---------------------------------------------------------------- guardado y sincronización
    public CompoundTag writeFluids(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Entry e : fluids) {
            CompoundTag t = new CompoundTag();
            t.putString("Fluid", String.valueOf(BuiltInRegistries.FLUID.getKey(e.fluid)));
            t.putLong("Amount", e.amount);
            list.add(t);
        }
        tag.put(TAG, list);
        tag.putInt("Selected", selected);
        return tag;
    }

    public void readFluids(CompoundTag tag) {
        fluids.clear();
        for (Tag t : tag.getList(TAG, Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Fluid f = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(c.getString("Fluid")));
            if (f != null && f != Fluids.EMPTY && c.getLong("Amount") > 0) {
                fluids.add(new Entry(f, c.getLong("Amount")));
            }
        }
        selected = tag.getInt("Selected");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeFluids(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        readFluids(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return writeFluids(new CompoundTag());
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ---------------------------------------------------------------- interfaz y capacidades
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tecnocinetico.universal_reservoir");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ReservoirMenu(id, ContainerLevelAccess.create(level, worldPosition), worldPosition);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return lazy.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazy = LazyOptional.of(() -> handler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazy.invalidate();
    }
}
