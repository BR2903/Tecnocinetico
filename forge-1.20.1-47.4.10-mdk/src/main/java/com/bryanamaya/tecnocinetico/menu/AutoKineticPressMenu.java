package com.bryanamaya.tecnocinetico.menu;

import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.block.entity.AutoKineticPressBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/** Interfaz de la Prensa Cinética Automática: 3 ingredientes, 1 resultado, progreso y energía. */
public class AutoKineticPressMenu extends AbstractContainerMenu {

    // Posiciones en la interfaz (deben coincidir con gui/auto_kinetic_press.png)
    public static final int[][] INPUT_POS = {{26, 35}, {44, 35}, {62, 35}};
    public static final int OUTPUT_X = 116, OUTPUT_Y = 35;
    public static final int ARROW_X = 85, ARROW_Y = 35, ARROW_W = 22, ARROW_H = 16;
    public static final int ENERGY_X = 152, ENERGY_Y = 17, ENERGY_W = 14, ENERGY_H = 52;

    private static final int MACHINE_SLOTS = AutoKineticPressBlockEntity.INPUT_SLOTS + 1;
    private static final int INV_START = MACHINE_SLOTS;
    private static final int INV_END = INV_START + 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;

    /** Constructor del cliente: lee la posición de la máquina que envía el servidor. */
    public AutoKineticPressMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, handlerAt(inventory, buf), new SimpleContainerData(AutoKineticPressBlockEntity.DATA_COUNT),
                ContainerLevelAccess.NULL);
    }

    private static IItemHandler handlerAt(Inventory inventory, FriendlyByteBuf buf) {
        BlockEntity be = inventory.player.level().getBlockEntity(buf.readBlockPos());
        return be instanceof AutoKineticPressBlockEntity press ? press.getItemHandler()
                : new ItemStackHandler(MACHINE_SLOTS);
    }

    public AutoKineticPressMenu(int id, Inventory inventory, IItemHandler machine, ContainerData data,
                                ContainerLevelAccess access) {
        super(ModMenus.AUTO_KINETIC_PRESS.get(), id);
        this.access = access;
        this.data = data;
        checkContainerDataCount(data, AutoKineticPressBlockEntity.DATA_COUNT);

        for (int i = 0; i < AutoKineticPressBlockEntity.INPUT_SLOTS; i++) {
            addSlot(new SlotItemHandler(machine, i, INPUT_POS[i][0], INPUT_POS[i][1]));
        }
        addSlot(new SlotItemHandler(machine, AutoKineticPressBlockEntity.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // solo salida
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    public int getEnergy() {
        return data.get(AutoKineticPressBlockEntity.DATA_ENERGY);
    }

    public int getProgress() {
        return data.get(AutoKineticPressBlockEntity.DATA_PROGRESS);
    }

    public boolean hasPower() {
        return getEnergy() >= AutoKineticPressBlockEntity.ENERGY_PER_TICK;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, INV_START, INV_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (moveItemStackTo(stack, 0, AutoKineticPressBlockEntity.INPUT_SLOTS, false)) {
            // ingrediente metido en la máquina
        } else if (index < INV_START + 27) {
            if (!moveItemStackTo(stack, INV_START + 27, INV_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, INV_START, INV_START + 27, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.AUTO_KINETIC_PRESS.get());
    }
}
