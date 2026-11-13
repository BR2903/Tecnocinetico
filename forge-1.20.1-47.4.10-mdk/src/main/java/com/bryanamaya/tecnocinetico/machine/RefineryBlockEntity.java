package com.bryanamaya.tecnocinetico.machine;

import com.bryanamaya.tecnocinetico.fluid.ModFluids;
import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * Refinería: destila petróleo (tanque de entrada) en gasolina (tanque de salida). Cada ciclo de {@link #TICKS} ticks
 * gasta {@link #CRUDE_PER_CYCLE} mB de petróleo y da {@link #GASOLINE_PER_CYCLE} mB de gasolina; cada
 * {@link #CYCLES_PER_TAR} ciclos sale una Brea. Además envasa: un lingote de hierro en la entrada + {@link #CANISTER_MB}
 * mB de gasolina = un Depósito de Combustible.
 */
public class RefineryBlockEntity extends TankMachineBlockEntity {

    public static final int TICKS = 40, PER_TICK = 40, CRUDE_PER_CYCLE = 250, GASOLINE_PER_CYCLE = 200, CYCLES_PER_TAR = 4;
    public static final int CANISTER_MB = 250;
    private int tarCounter;

    public RefineryBlockEntity(BlockPos pos, BlockState state) {
        super(MachineBlockEntities.REFINERY.get(), MachineKind.REFINERY, pos, state, fs -> fs.getFluid() == ModFluids.CRUDE_OIL.get());
    }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return stack.is(Items.IRON_INGOT);
    }

    @Override
    protected boolean work() {
        canning();
        FluidStack gas = new FluidStack(ModFluids.GASOLINE.get(), GASOLINE_PER_CYCLE);
        if (input.getFluidAmount() < CRUDE_PER_CYCLE || output.fill(gas, IFluidHandler.FluidAction.SIMULATE) < GASOLINE_PER_CYCLE
                || !fits(1, new ItemStack(ModItems.TAR.get()))) {
            return false;
        }
        if (advance(TICKS, PER_TICK)) {
            input.drain(CRUDE_PER_CYCLE, IFluidHandler.FluidAction.EXECUTE);
            output.fill(gas, IFluidHandler.FluidAction.EXECUTE);
            if (++tarCounter >= CYCLES_PER_TAR) {
                tarCounter = 0;
                output(1, new ItemStack(ModItems.TAR.get()));
            }
            level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.4F, 0.6F);
        }
        return true;
    }

    /** Envasado (sin gasto de energía): hierro + gasolina -> Depósito de Combustible. */
    private void canning() {
        if (level.getGameTime() % 10 != 0 || !input(0).is(Items.IRON_INGOT) || output.getFluidAmount() < CANISTER_MB
                || output.getFluid().getFluid() != ModFluids.GASOLINE.get()) {
            return;
        }
        ItemStack canister = new ItemStack(ModItems.FUEL_CANISTER.get());
        if (!fits(0, canister)) {
            return;
        }
        items.extractItem(0, 1, false);
        output.drain(CANISTER_MB, IFluidHandler.FluidAction.EXECUTE);
        output(0, canister);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("tar_counter", tarCounter);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        tarCounter = tag.getInt("tar_counter");
    }

    @Override
    protected void effects(ServerLevel level, long time) {
        loop(com.bryanamaya.tecnocinetico.sound.ModSounds.MACHINE_LAB.get(), time, 46);
        if (time % 6 == 0) {
            var p = worldPosition.getCenter();
            level.sendParticles(ParticleTypes.SMOKE, p.x, p.y + 0.7, p.z, 1, 0.1, 0.05, 0.1, 0.01);
        }
    }
}
