package com.bryanamaya.tecnocinetico.menu;

import com.bryanamaya.tecnocinetico.machine.ElectricMachineBlockEntity;
import com.bryanamaya.tecnocinetico.machine.MachineKind;
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

/** Interfaz común de las máquinas eléctricas: entradas, salidas, progreso y energía (posiciones en MachineKind). */
public class MachineMenu extends AbstractContainerMenu {

    private final MachineKind kind;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final int machineSlots;

    /** Constructor del cliente: lee la posición de la máquina. */
    public MachineMenu(MachineKind kind, int id, Inventory inventory, FriendlyByteBuf buf) {
        this(kind, id, inventory, handlerAt(kind, inventory, buf), new SimpleContainerData(ElectricMachineBlockEntity.DATA_COUNT + kind.extraData),
                ContainerLevelAccess.NULL);
    }

    private static IItemHandler handlerAt(MachineKind kind, Inventory inventory, FriendlyByteBuf buf) {
        BlockEntity be = inventory.player.level().getBlockEntity(buf.readBlockPos());
        return be instanceof ElectricMachineBlockEntity m ? m.getItems() : new ItemStackHandler(kind.slots());
    }

    public MachineMenu(MachineKind kind, int id, Inventory inventory, IItemHandler machine, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.machine(kind).get(), id);
        this.kind = kind;
        this.access = access;
        this.data = data;
        this.machineSlots = kind.slots();
        checkContainerDataCount(data, ElectricMachineBlockEntity.DATA_COUNT + kind.extraData);
        for (int i = 0; i < kind.inputs.length; i++) {
            addSlot(new SlotItemHandler(machine, i, kind.inputs[i][0], kind.inputs[i][1]));
        }
        for (int i = 0; i < kind.outputs.length; i++) {
            addSlot(new SlotItemHandler(machine, kind.inputs.length + i, kind.outputs[i][0], kind.outputs[i][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 96 + row * 18));     // +12: franja de estado
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 154));
        }
        addDataSlots(data);
    }

    public MachineKind kind() {
        return kind;
    }

    /** Energía almacenada (FE). */
    public int energy() {
        return data.get(ElectricMachineBlockEntity.DATA_ENERGY) * 10;
    }

    public int progress() {
        return data.get(ElectricMachineBlockEntity.DATA_PROGRESS);
    }

    public int maxProgress() {
        return data.get(ElectricMachineBlockEntity.DATA_MAX);
    }

    /** Dato extra i de la máquina (ver MachineKind.extraData). */
    public int extra(int i) {
        return data.get(ElectricMachineBlockEntity.DATA_COUNT + i);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        access.execute((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof ElectricMachineBlockEntity m) {
                m.onButton(id);
            }
        });
        return true;
    }

    public int usage() {
        return data.get(ElectricMachineBlockEntity.DATA_USAGE);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int invStart = machineSlots, invEnd = invStart + 36;
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, invStart, invEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (moveItemStackTo(stack, 0, kind.inputs.length, false)) {
            // metido en la máquina
        } else if (index < invStart + 27) {
            if (!moveItemStackTo(stack, invStart + 27, invEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, invStart, invStart + 27, false)) {
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
        return access.evaluate((level, pos) -> level.getBlockEntity(pos) instanceof ElectricMachineBlockEntity
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0, true);
    }
}
