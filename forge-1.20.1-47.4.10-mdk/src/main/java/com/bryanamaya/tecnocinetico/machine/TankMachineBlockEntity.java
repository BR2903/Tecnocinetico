package com.bryanamaya.tecnocinetico.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Máquina eléctrica con un tanque de entrada y otro de salida (Refinería, Bomba de Extracción). Misma interfaz que el
 * Laboratorio Químico (ChemicalLabScreen): 4 datos extra con los dos tanques y botones para vaciarlos. Tuberías y cubos:
 * lo que entra va al tanque de entrada, lo que se saca sale del de salida.
 */
public abstract class TankMachineBlockEntity extends ElectricMachineBlockEntity implements FluidMachine {

    public static final int TANK = ChemicalLabBlockEntity.TANK;

    protected final FluidTank input, output;
    private final IFluidHandler pipes;
    private LazyOptional<IFluidHandler> lazyFluids = LazyOptional.empty();

    protected TankMachineBlockEntity(BlockEntityType<?> type, MachineKind kind, BlockPos pos, BlockState state,
                                     Predicate<FluidStack> acceptsInput) {
        super(type, kind, pos, state);
        input = new FluidTank(TANK, acceptsInput) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
        output = new FluidTank(TANK) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
        pipes = new IFluidHandler() {
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
    }

    public FluidTank inputTank() {
        return input;
    }

    public FluidTank outputTank() {
        return output;
    }

    public IFluidHandler fluidHandler() {
        return pipes;
    }

    @Override
    public void onButton(int id) {
        switch (id) {
            case ChemicalLabBlockEntity.BTN_CLEAR_IN -> input.setFluid(FluidStack.EMPTY);
            case ChemicalLabBlockEntity.BTN_CLEAR_OUT -> output.setFluid(FluidStack.EMPTY);
            default -> {
                return;
            }
        }
        progress = 0;
        setChanged();
    }

    @Override
    public boolean useContainer(Player player, InteractionHand hand) {
        if (FluidUtil.getFluidHandler(player.getItemInHand(hand)).isPresent()) {
            return FluidUtil.interactWithFluidHandler(player, hand, pipes);
        }
        return false;
    }

    @Override
    protected int getExtra(int i) {
        return switch (i) {
            case ChemicalLabBlockEntity.EXTRA_IN_FLUID -> fluidId(input.getFluid());
            case ChemicalLabBlockEntity.EXTRA_IN_AMOUNT -> input.getFluidAmount();
            case ChemicalLabBlockEntity.EXTRA_OUT_FLUID -> fluidId(output.getFluid());
            default -> output.getFluidAmount();
        };
    }

    private static int fluidId(FluidStack s) {
        return s.isEmpty() ? 0 : BuiltInRegistries.FLUID.getId(s.getFluid());
    }

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
}
