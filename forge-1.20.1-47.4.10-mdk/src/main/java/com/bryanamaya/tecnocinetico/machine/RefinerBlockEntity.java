package com.bryanamaya.tecnocinetico.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Refinador Cinético: 1 mena en bruto -> 2 de polvo (hueco 0 de salida) + subproducto (hueco 1). */
public class RefinerBlockEntity extends ElectricMachineBlockEntity {

    public RefinerBlockEntity(BlockPos pos, BlockState state) {
        super(MachineBlockEntities.REFINER.get(), MachineKind.REFINER, pos, state);
    }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return MachineRecipes.refine(stack) != null;
    }

    @Override
    protected boolean work() {
        MachineRecipes.Refine r = MachineRecipes.refine(input(0));
        if (r == null) {
            return false;
        }
        ItemStack main = new ItemStack(r.out().get(), r.count());
        ItemStack extra = new ItemStack(r.extra().get());
        if (!fits(0, main) || !fits(1, extra)) {
            return false;
        }
        if (advance(r.ticks(), r.perTick())) {
            items.extractItem(0, 1, false);
            output(0, main);
            if (level.random.nextFloat() < r.chance()) {
                output(1, extra);
            }
            level.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.5F, 0.8F);
        }
        return true;
    }
}
