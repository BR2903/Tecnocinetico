package com.bryanamaya.tecnocinetico.menu;

import com.bryanamaya.tecnocinetico.gear.TacticalGear;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Menú del Equipo Táctico: las 10 ranuras de {@link TacticalGear} + el inventario del jugador.
 * En el servidor usa el equipo real del jugador; en el cliente una copia vacía que el menú sincroniza.
 * Las posiciones deben coincidir con la textura gui/tactical_gear.png (tools/tactical_gui_gen.py).
 */
public class TacticalMenu extends AbstractContainerMenu {

    public static final int[][] POS = new int[TacticalGear.SIZE][];
    public static final int INV_Y = 118;

    static {
        for (int i = 0; i < TacticalGear.AMMO_COUNT; i++) {
            POS[TacticalGear.AMMO_START + i] = new int[]{116 + (i % 2) * 18, 16 + (i / 2) * 18};
        }
        for (int i = 0; i < TacticalGear.FOOD_COUNT; i++) {
            POS[TacticalGear.FOOD_START + i] = new int[]{116 + i * 18, 72};
        }
        POS[TacticalGear.FACE] = new int[]{8, 16};
        POS[TacticalGear.BACK] = new int[]{8, 40};
        POS[TacticalGear.FEET] = new int[]{8, 64};
    }

    /** Cliente. */
    public TacticalMenu(int id, Inventory inv) {
        this(id, inv, new ItemStackHandler(TacticalGear.SIZE));
    }

    /** Servidor: con el equipo real del jugador. */
    public TacticalMenu(int id, Inventory inv, IItemHandler gear) {
        super(ModMenus.TACTICAL_GEAR.get(), id);
        for (int i = 0; i < TacticalGear.SIZE; i++) {
            final int slot = i;
            addSlot(new SlotItemHandler(gear, i, POS[i][0], POS[i][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return TacticalGear.accepts(slot, stack);
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return slot >= TacticalGear.FACE ? 1 : super.getMaxStackSize(stack);
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, 9 + row * 9 + col, 8 + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, INV_Y + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int gearEnd = TacticalGear.SIZE, invEnd = gearEnd + 36;
        if (index < gearEnd) {
            if (!moveItemStackTo(stack, gearEnd, invEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // del inventario a la ranura del equipo que le corresponda (munición, comida o gadget)
            boolean moved = false;
            for (int i = 0; i < gearEnd && !stack.isEmpty(); i++) {
                if (TacticalGear.accepts(i, stack) && moveItemStackTo(stack, i, i + 1, false)) {
                    moved = true;
                }
            }
            if (!moved) {
                boolean fromMain = index < gearEnd + 27;
                if (!moveItemStackTo(stack, fromMain ? gearEnd + 27 : gearEnd, fromMain ? invEnd : gearEnd + 27, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }
}
