package com.bryanamaya.tecnocinetico.machine;

import com.bryanamaya.tecnocinetico.fluid.ModFluids;
import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * Generador de Combustible: paso intermedio entre el alternador de Create y el Reactor de Fisión. Quema lo que tenga en
 * su tanque y empuja la energía a las máquinas y cables pegados a cualquier cara:
 * <ul>
 *   <li>Gasolina: {@link #GASOLINE_FE} FE/t.</li>
 *   <li>Combustible Cinético: {@link #KINETIC_FE} FE/t (rinde el triple por cubo).</li>
 * </ul>
 * Gasta 1 mB cada {@link #TICKS_PER_MB} ticks (un cubo dura {@code 1000 x TICKS_PER_MB} ticks). Se llena con tuberías,
 * cubos (clic derecho) o metiendo en su hueco cubos, Depósitos de Combustible (250 mB de gasolina) o Células de
 * Combustible Cinético (500 mB); los cubos vacíos salen por el hueco de salida.
 */
public class FuelGeneratorBlockEntity extends TankMachineBlockEntity {

    public static final int GASOLINE_FE = 400, KINETIC_FE = 1200, TICKS_PER_MB = 4, PUSH_PER_TICK = 2_000;
    public static final int CANISTER_MB = 250, CELL_MB = 500;
    private int burnTicks;
    /** FE/t que da ahora (para la interfaz y la luz). */
    private int output;

    public FuelGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(MachineBlockEntities.FUEL_GENERATOR.get(), MachineKind.FUEL_GENERATOR, pos, state, fs -> power(fs.getFluid()) > 0);
    }

    /** FE/t de cada combustible (0 = no se quema). */
    public static int power(Fluid fluid) {
        if (fluid == ModFluids.GASOLINE.get()) return GASOLINE_FE;
        if (fluid == ModFluids.KINETIC_FUEL.get()) return KINETIC_FE;
        return 0;
    }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return stack.is(ModItems.FUEL_CANISTER.get()) || stack.is(ModItems.KINETIC_FUEL_CELL.get())
                || FluidUtil.getFluidContained(stack).map(fs -> power(fs.getFluid()) > 0).orElse(false);
    }

    @Override
    protected boolean work() {
        loadItem();
        int power = power(input.getFluid().getFluid());
        output = 0;
        if (power > 0 && input.getFluidAmount() > 0 && energy.getEnergyStored() < energy.getMaxEnergyStored()) {
            output = energy.produce(power);
            usage = output;
            if (++burnTicks >= TICKS_PER_MB) {
                burnTicks = 0;
                input.drain(1, IFluidHandler.FluidAction.EXECUTE);
            }
        } else {
            usage = 0;
        }
        push();
        return output > 0;
    }

    /** Vacía en el tanque lo que haya en el hueco de entrada (cubos -> cubo vacío a la salida; latas y células se gastan). */
    private void loadItem() {
        ItemStack in = input(0);
        if (in.isEmpty() || level.getGameTime() % 5 != 0) {
            return;
        }
        FluidStack add = in.is(ModItems.FUEL_CANISTER.get()) ? new FluidStack(ModFluids.GASOLINE.get(), CANISTER_MB)
                : in.is(ModItems.KINETIC_FUEL_CELL.get()) ? new FluidStack(ModFluids.KINETIC_FUEL.get(), CELL_MB) : FluidStack.EMPTY;
        if (!add.isEmpty()) {
            if (input.fill(add, IFluidHandler.FluidAction.SIMULATE) == add.getAmount()) {
                input.fill(add, IFluidHandler.FluidAction.EXECUTE);
                items.extractItem(0, 1, false);
            }
            return;
        }
        var result = FluidUtil.tryEmptyContainer(in.copyWithCount(1), input, Integer.MAX_VALUE, null, false);
        if (result.isSuccess() && fits(0, result.getResult())) {
            result = FluidUtil.tryEmptyContainer(in.copyWithCount(1), input, Integer.MAX_VALUE, null, true);
            items.extractItem(0, 1, false);
            if (!result.getResult().isEmpty()) {
                output(0, result.getResult());
            }
        }
    }

    /** Reparte la energía a lo que tenga pegado (máquinas, cables). */
    private void push() {
        for (Direction d : Direction.values()) {
            if (energy.getEnergyStored() <= 0) {
                return;
            }
            BlockEntity n = level.getBlockEntity(worldPosition.relative(d));
            if (n == null || n instanceof FuelGeneratorBlockEntity) {
                continue;
            }
            n.getCapability(ForgeCapabilities.ENERGY, d.getOpposite()).filter(IEnergyStorage::canReceive).ifPresent(target -> {
                int sent = target.receiveEnergy(Math.min(PUSH_PER_TICK, energy.getEnergyStored()), false);
                energy.drain(sent);
            });
        }
    }

    public int output() {
        return output;
    }

    @Override
    protected boolean isLit() {
        return output > 0;
    }

    @Override
    protected void effects(ServerLevel level, long time) {
        loop(com.bryanamaya.tecnocinetico.sound.ModSounds.DRILL_IDLE.get(), time, 40);
        if (time % 4 == 0) {
            var p = worldPosition.getCenter();
            level.sendParticles(ParticleTypes.SMOKE, p.x, p.y + 0.6, p.z, 1, 0.1, 0.05, 0.1, 0.01);
        }
    }
}
