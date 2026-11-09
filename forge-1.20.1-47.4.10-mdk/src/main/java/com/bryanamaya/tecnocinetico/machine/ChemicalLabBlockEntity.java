package com.bryanamaya.tecnocinetico.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Laboratorio Químico: 2 ingredientes + líquido del tanque de entrada -> producto y/o líquido en el tanque de
 * salida (ver MachineRecipes.LAB). Los tanques se llenan/vacían con cubos (clic derecho al bloque) o con
 * tuberías: lo que entra va al tanque de entrada, lo que se saca sale del de salida.
 */
public class ChemicalLabBlockEntity extends ElectricMachineBlockEntity {

    public static final int TANK = 8_000;
    public static final int EXTRA_IN_FLUID = 0, EXTRA_IN_AMOUNT = 1, EXTRA_OUT_FLUID = 2, EXTRA_OUT_AMOUNT = 3;
    /** Botones: vaciar entrada, vaciar salida, intercambiar los dos tanques. */
    public static final int BTN_CLEAR_IN = 0, BTN_CLEAR_OUT = 1, BTN_SWAP = 2;

    private final FluidTank input = new FluidTank(TANK, fs -> MachineRecipes.isLabFluid(fs.getFluid())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank output = new FluidTank(TANK) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    /** Vista para tuberías y cubos: llenar = tanque de entrada; vaciar = tanque de salida. */
    private final IFluidHandler pipes = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return tank == 0 ? input.getFluid() : output.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return TANK;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return tank == 0 && input.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return input.fill(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            return output.drain(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return output.drain(maxDrain, action);
        }
    };
    private LazyOptional<IFluidHandler> lazyFluids = LazyOptional.empty();

    // valores sincronizados en el cliente
    private int clientInFluid, clientInAmount, clientOutFluid, clientOutAmount;

    public ChemicalLabBlockEntity(BlockPos pos, BlockState state) {
        super(MachineBlockEntities.CHEMICAL_LAB.get(), MachineKind.CHEMICAL_LAB, pos, state);
    }

    public FluidTank inputTank() {
        return input;
    }

    public FluidTank outputTank() {
        return output;
    }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        if (!MachineRecipes.isLabIngredient(stack)) {
            return false;
        }
        for (int i = 0; i < kind.inputs.length; i++) {                     // un tipo de objeto por hueco
            if (i != slot && input(i).is(stack.getItem())) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean work() {
        MachineRecipes.Lab r = MachineRecipes.lab(List.of(input(0), input(1)), input.getFluid());
        if (r == null) {
            return false;
        }
        ItemStack product = r.out() == null ? ItemStack.EMPTY : new ItemStack(r.out().get(), r.outCount());
        FluidStack liquid = r.outFluid() == null ? FluidStack.EMPTY : new FluidStack(r.outFluid().get(), r.outAmount());
        if (!fits(0, product) || (!liquid.isEmpty() && output.fill(liquid, IFluidHandler.FluidAction.SIMULATE) < liquid.getAmount())) {
            return false;
        }
        if (advance(r.ticks(), r.perTick())) {
            for (MachineRecipes.Ingredient ing : r.items()) {
                int left = ing.count();
                for (int i = 0; i < kind.inputs.length && left > 0; i++) {
                    if (input(i).is(ing.item().get())) {
                        left -= items.extractItem(i, left, false).getCount();
                    }
                }
            }
            input.drain(r.inAmount(), IFluidHandler.FluidAction.EXECUTE);
            output(0, product);
            if (!liquid.isEmpty()) {
                output.fill(liquid, IFluidHandler.FluidAction.EXECUTE);
            }
            level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.5F, 0.9F);
        }
        return true;
    }

    @Override
    public void onButton(int id) {
        switch (id) {
            case BTN_CLEAR_IN -> input.setFluid(FluidStack.EMPTY);
            case BTN_CLEAR_OUT -> output.setFluid(FluidStack.EMPTY);
            case BTN_SWAP -> {                                                 // p. ej. el ácido recién hecho pasa a la entrada
                FluidStack a = input.getFluid().copy();
                input.setFluid(output.getFluid().copy());
                output.setFluid(a);
            }
            default -> {
                return;
            }
        }
        progress = 0;
        setChanged();
    }

    /** Clic derecho con un cubo u otro recipiente: llena el tanque de entrada o vacía el de salida. */
    public boolean useContainer(Player player, InteractionHand hand) {
        if (FluidUtil.getFluidHandler(player.getItemInHand(hand)).isPresent()) {
            return FluidUtil.interactWithFluidHandler(player, hand, pipes);
        }
        return false;
    }

    // ---------------------------------------------------------------- interfaz
    @Override
    protected int getExtra(int i) {
        return switch (i) {
            case EXTRA_IN_FLUID -> fluidId(input.getFluid());
            case EXTRA_IN_AMOUNT -> input.getFluidAmount();
            case EXTRA_OUT_FLUID -> fluidId(output.getFluid());
            default -> output.getFluidAmount();
        };
    }

    @Override
    protected void setExtra(int i, int v) {
        switch (i) {
            case EXTRA_IN_FLUID -> clientInFluid = v;
            case EXTRA_IN_AMOUNT -> clientInAmount = v;
            case EXTRA_OUT_FLUID -> clientOutFluid = v;
            default -> clientOutAmount = v;
        }
    }

    private static int fluidId(FluidStack s) {
        return s.isEmpty() ? 0 : BuiltInRegistries.FLUID.getId(s.getFluid());
    }

    /** Líquido a partir del número que llega a la interfaz. */
    public static Fluid fluidById(int id) {
        Fluid f = BuiltInRegistries.FLUID.byId(id);
        return f == null ? Fluids.EMPTY : f;
    }

    // ---------------------------------------------------------------- guardado y capacidades
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("input_tank", input.writeToNBT(new CompoundTag()));
        tag.put("output_tank", output.writeToNBT(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        input.readFromNBT(tag.getCompound("input_tank"));
        output.readFromNBT(tag.getCompound("output_tank"));
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return lazyFluids.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyFluids = LazyOptional.of(() -> pipes);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyFluids.invalidate();
    }

    public IFluidHandler fluidHandler() {
        return pipes;
    }
}
