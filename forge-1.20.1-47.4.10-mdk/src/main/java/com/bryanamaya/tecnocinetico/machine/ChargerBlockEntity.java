package com.bryanamaya.tecnocinetico.machine;

import com.bryanamaya.tecnocinetico.gear.GadgetItem;
import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * Cargador de células: rellena con electricidad lo que se gasta.
 * - Célula de Impulso vacía -> llena (8.000 FE).
 * - Gadgets del Equipo Táctico (visor, propulsor, jetpack, impulsor de esquiva): 20 FE por punto de batería.
 * - Cualquier objeto con batería FE (baterías de otros mods).
 * Cuando el objeto está lleno pasa solo a la salida.
 */
public class ChargerBlockEntity extends ElectricMachineBlockEntity {

    public static final int CELL_TICKS = 80, CELL_PER_TICK = 100;
    public static final int GADGET_FE_PER_POINT = 20, GADGET_POINTS_PER_TICK = 4;
    public static final int ITEM_FE_PER_TICK = 2_000;

    public ChargerBlockEntity(BlockPos pos, BlockState state) {
        super(MachineBlockEntities.CHARGER.get(), MachineKind.CHARGER, pos, state);
    }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return stack.is(ModItems.EMPTY_PULSE_CELL.get()) || stack.getItem() instanceof GadgetItem
                || stack.getCapability(ForgeCapabilities.ENERGY).map(IEnergyStorage::canReceive).orElse(false);
    }

    @Override
    protected boolean work() {
        ItemStack in = input(0);
        if (in.isEmpty()) {
            return false;
        }
        // Célula de impulso: trabajo de duración fija
        if (in.is(ModItems.EMPTY_PULSE_CELL.get())) {
            ItemStack full = new ItemStack(ModItems.PULSE_CELL.get());
            if (!fits(0, full)) {
                return false;
            }
            if (advance(CELL_TICKS, CELL_PER_TICK)) {
                items.extractItem(0, 1, false);
                output(0, full);
                level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.3F, 2.0F);
            }
            return true;
        }
        if (!fits(0, in)) {
            return false;
        }
        // Gadget: su batería es el daño del objeto
        if (in.getItem() instanceof GadgetItem) {
            maxProgress = in.getMaxDamage();
            usage = GADGET_FE_PER_POINT * GADGET_POINTS_PER_TICK;
            if (in.getDamageValue() > 0) {
                int points = Math.min(GADGET_POINTS_PER_TICK, in.getDamageValue());
                points = Math.min(points, energy.getEnergyStored() / GADGET_FE_PER_POINT);
                if (points <= 0) {
                    return true;                                          // sin energía: espera
                }
                energy.drain(points * GADGET_FE_PER_POINT);
                ItemStack copy = in.copy();
                copy.setDamageValue(in.getDamageValue() - points);
                items.setStackInSlot(0, copy);
                progress = copy.getMaxDamage() - copy.getDamageValue();
                return true;
            }
            finish(in);
            return true;
        }
        // Objeto con batería FE
        IEnergyStorage cell = in.getCapability(ForgeCapabilities.ENERGY).orElse(null);
        if (cell != null) {
            maxProgress = Math.max(1, cell.getMaxEnergyStored() / 100);
            usage = ITEM_FE_PER_TICK;
            int want = cell.receiveEnergy(Math.min(ITEM_FE_PER_TICK, energy.getEnergyStored()), true);
            if (want > 0) {
                energy.drain(cell.receiveEnergy(want, false));
                progress = cell.getEnergyStored() / 100;
                setChanged();
                return true;
            }
            if (cell.getEnergyStored() >= cell.getMaxEnergyStored()) {
                finish(in);
            }
            return true;
        }
        return false;
    }

    private void finish(ItemStack in) {
        output(0, in.copy());
        items.setStackInSlot(0, ItemStack.EMPTY);
        progress = 0;
        level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.3F, 2.0F);
    }

    @Override
    protected void effects(net.minecraft.server.level.ServerLevel level, long time) {
        loop(com.bryanamaya.tecnocinetico.sound.ModSounds.MACHINE_CHARGER.get(), time, 44);
        if (time % 3 == 0) {
            var p = worldPosition.getCenter();
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 0.55, p.z, 2, 0.25, 0.05, 0.25, 0.05);
        }
    }
}
