package com.bryanamaya.tecnocinetico.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Ensamblador Industrial (núcleo de la estructura 3x3x3): fabrica lo más complejo del mod (kit del reactor...).
 * 9 huecos de ingredientes en cualquier orden (un tipo por hueco) -> 1 producto. Mucha energía y mucho tiempo.
 * Envía su contenido al cliente para que se vea en la plataforma (client/IndustrialAssemblerRenderer).
 */
public class IndustrialAssemblerBlockEntity extends ElectricMachineBlockEntity {

    public IndustrialAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(MachineBlockEntities.INDUSTRIAL_ASSEMBLER.get(), MachineKind.INDUSTRIAL_ASSEMBLER, pos, state);
    }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        if (!MachineRecipes.isAssemblyIngredient(stack)) {
            return false;
        }
        for (int i = 0; i < kind.inputs.length; i++) {
            if (i != slot && input(i).is(stack.getItem())) {
                return false;
            }
        }
        return true;
    }

    public List<ItemStack> inputs() {
        List<ItemStack> l = new ArrayList<>();
        for (int i = 0; i < kind.inputs.length; i++) {
            l.add(input(i));
        }
        return l;
    }

    /** Lo que se está fabricando ahora (para dibujarlo), o vacío. */
    public ItemStack currentResult() {
        MachineRecipes.Assembly a = MachineRecipes.assembly(inputs());
        return a == null ? ItemStack.EMPTY : new ItemStack(a.out().get(), a.count());
    }

    public ItemStack outputStack() {
        return items.getStackInSlot(outSlot(0));
    }

    public int progress() {
        return progress;
    }

    public int maxProgress() {
        return maxProgress;
    }

    @Override
    protected boolean syncsToClient() {
        return true;
    }

    @Override
    protected boolean work() {
        MachineRecipes.Assembly a = MachineRecipes.assembly(inputs());
        if (a == null) {
            return false;
        }
        ItemStack result = new ItemStack(a.out().get(), a.count());
        if (!fits(0, result)) {
            return false;
        }
        if (progress % 40 == 0 && energy.getEnergyStored() >= a.perTick()) {
            level.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.4F, 0.6F + level.random.nextFloat() * 0.4F);
        }
        if (advance(a.ticks(), a.perTick())) {
            for (MachineRecipes.Ingredient ing : a.in()) {
                int left = ing.count();
                for (int i = 0; i < kind.inputs.length && left > 0; i++) {
                    if (input(i).is(ing.item().get())) {
                        left -= items.extractItem(i, left, false).getCount();
                    }
                }
            }
            output(0, result);
            level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 1.2F);
        }
        return true;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1.5);
    }
}
