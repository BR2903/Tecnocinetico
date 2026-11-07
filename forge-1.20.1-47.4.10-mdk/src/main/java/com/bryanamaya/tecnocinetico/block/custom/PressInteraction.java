package com.bryanamaya.tecnocinetico.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;

/** Lógica común de clic derecho para las prensas: meter ingredientes o recoger objetos. */
final class PressInteraction {

    private PressInteraction() {}

    /**
     * Mano con objeto: mete 1 (o el stack entero si el jugador va agachado).
     * Mano vacía: recoge primero el hueco de salida (si existe) y si no, el último ingrediente.
     */
    static void handle(Level level, BlockPos pos, Player player, InteractionHand hand,
                       IItemHandler handler, int inputSlots, int outputSlot) {
        ItemStack inHand = player.getItemInHand(hand);

        if (!inHand.isEmpty()) {
            int amount = player.isShiftKeyDown() ? inHand.getCount() : 1;
            ItemStack toInsert = inHand.copyWithCount(amount);
            for (int i = 0; i < inputSlots && !toInsert.isEmpty(); i++) {
                toInsert = handler.insertItem(i, toInsert, false);
            }
            int inserted = amount - toInsert.getCount();
            if (inserted > 0) {
                if (!player.isCreative()) {
                    inHand.shrink(inserted);
                }
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8f, 0.8f);
            }
            return;
        }

        if (outputSlot >= 0 && !handler.getStackInSlot(outputSlot).isEmpty()) {
            player.setItemInHand(hand, handler.extractItem(outputSlot, 64, false));
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 1.0f);
            return;
        }
        for (int i = inputSlots - 1; i >= 0; i--) {
            if (!handler.getStackInSlot(i).isEmpty()) {
                player.setItemInHand(hand, handler.extractItem(i, 64, false));
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 1.0f);
                return;
            }
        }
    }

    static void dropContents(Level level, BlockPos pos, IItemHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), handler.getStackInSlot(i).copy());
        }
    }
}
