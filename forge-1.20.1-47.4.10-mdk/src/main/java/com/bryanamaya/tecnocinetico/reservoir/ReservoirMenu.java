package com.bryanamaya.tecnocinetico.reservoir;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

/** Interfaz del Reservorio (sin huecos): lista de líquidos; los botones eligen la salida o desechan un líquido. */
public class ReservoirMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess access;
    public final BlockPos pos;

    public ReservoirMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, ContainerLevelAccess.NULL, buf.readBlockPos());
    }

    public ReservoirMenu(int id, ContainerLevelAccess access, BlockPos pos) {
        super(ReservoirRegistry.RESERVOIR_MENU.get(), id);
        this.access = access;
        this.pos = pos;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        access.execute((level, p) -> {
            if (level.getBlockEntity(p) instanceof ReservoirBlockEntity r) {
                r.onButton(id);
            }
        });
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, p) -> level.getBlockEntity(p) instanceof ReservoirBlockEntity
                && player.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5) <= 64.0, true);
    }
}
