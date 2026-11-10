package com.bryanamaya.tecnocinetico.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Fabricador de munición: 3 huecos de ingredientes en cualquier orden -> munición en lote. */
public class AmmoFabricatorBlockEntity extends ElectricMachineBlockEntity {

    public AmmoFabricatorBlockEntity(BlockPos pos, BlockState state) {
        super(MachineBlockEntities.AMMO_FABRICATOR.get(), MachineKind.AMMO_FABRICATOR, pos, state);
    }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        if (!MachineRecipes.isFabricateIngredient(stack)) {
            return false;
        }
        for (int i = 0; i < kind.inputs.length; i++) {                    // un tipo de objeto por hueco (no atasca tolvas)
            if (i != slot && input(i).is(stack.getItem())) {
                return false;
            }
        }
        return true;
    }

    private List<ItemStack> inputs() {
        return List.of(input(0), input(1), input(2));
    }

    @Override
    protected boolean work() {
        MachineRecipes.Fabricate f = MachineRecipes.fabricate(inputs());
        if (f == null) {
            return false;
        }
        ItemStack result = new ItemStack(f.out().get(), f.count());
        if (!fits(0, result)) {
            return false;
        }
        if (advance(f.ticks(), f.perTick())) {
            for (MachineRecipes.Ingredient ing : f.in()) {
                int left = ing.count();
                for (int i = 0; i < kind.inputs.length && left > 0; i++) {
                    if (input(i).is(ing.item().get())) {
                        left -= items.extractItem(i, left, false).getCount();
                    }
                }
            }
            output(0, result);
            level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.4F, 1.3F);
        }
        return true;
    }

    @Override
    protected void effects(net.minecraft.server.level.ServerLevel level, long time) {
        loop(com.bryanamaya.tecnocinetico.sound.ModSounds.MACHINE_FABRICATOR.get(), time, 46);
        if (time % 23 == 3) {                                                 // golpe del troquel
            var p = worldPosition.getCenter();
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, p.x, p.y + 0.55, p.z, 4, 0.2, 0.05, 0.2, 0.01);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, p.x, p.y + 0.55, p.z, 5, 0.15, 0.05, 0.15, 0.2);
        }
    }
}
