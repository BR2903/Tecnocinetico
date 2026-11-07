package com.bryanamaya.tecnocinetico.menu;

import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.item.custom.KineticPistolItem;
import com.bryanamaya.tecnocinetico.item.custom.WeaponModule;
import com.bryanamaya.tecnocinetico.item.custom.WeaponModuleItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Banco de Armas Cinético.
 * Hueco 0: el arma. Huecos 1-4: un módulo por ranura (en el orden de WeaponModule).
 *
 * Los huecos de módulo son una "vista" de los módulos instalados en el arma:
 * - al poner un arma, sus módulos aparecen en sus ranuras;
 * - poner un módulo en su ranura lo instala; sacarlo lo desinstala (vuelve al inventario);
 * - al sacar el arma, las ranuras se vacían (los módulos van montados en ella).
 * Así nunca se duplican ni se pierden módulos.
 */
public class WeaponBenchMenu extends AbstractContainerMenu {

    public static final int WEAPON_SLOT = 0;
    public static final int MODULE_COUNT = WeaponModule.values().length;
    private static final int INV_START = 1 + MODULE_COUNT;
    private static final int INV_END = INV_START + 36;

    // Posiciones en la interfaz (deben coincidir con la textura gui/weapon_bench.png)
    public static final int WEAPON_X = 20, WEAPON_Y = 46;
    public static final int[][] MODULE_POS = {
            {93, 17},   // OPTIC: encima de la corredera
            {66, 76},   // EXTENDED_MAG: debajo de la empuñadura
            {150, 36},  // INERCITA_BARREL: delante del cañón
            {40, 70},   // QUICK_RELOAD: junto a la boca de la empuñadura
    };
    public static final int INVENTORY_Y = 108;

    private final Player player;
    private final ContainerLevelAccess access;
    private final SimpleContainer container = new SimpleContainer(1 + MODULE_COUNT) {
        @Override
        public void setChanged() {
            super.setChanged();
            WeaponBenchMenu.this.slotsChanged(this);
        }
    };
    private ItemStack lastWeapon = ItemStack.EMPTY;
    private boolean syncing;

    /** Constructor del cliente. */
    public WeaponBenchMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public WeaponBenchMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.WEAPON_BENCH.get(), id);
        this.player = inventory.player;
        this.access = access;

        addSlot(new Slot(container, WEAPON_SLOT, WEAPON_X, WEAPON_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof KineticPistolItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (WeaponModule module : WeaponModule.values()) {
            int[] p = MODULE_POS[module.ordinal()];
            addSlot(new ModuleSlot(container, module, p[0], p[1]));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_Y + 58));
        }
    }

    /** Ranura de un módulo concreto: solo acepta ese módulo y solo si hay un arma en el banco. */
    public class ModuleSlot extends Slot {
        private final WeaponModule module;

        ModuleSlot(Container bench, WeaponModule module, int x, int y) {
            super(bench, 1 + module.ordinal(), x, y);
            this.module = module;
        }

        public WeaponModule getModule() {
            return module;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // Sin NBT (nombre, etc.): al instalarse solo se guarda el bit, así que se perdería
            return stack.getItem() instanceof WeaponModuleItem m && m.getModule() == module
                    && !stack.hasTag()
                    && !container.getItem(WEAPON_SLOT).isEmpty();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    @Override
    public void slotsChanged(Container changed) {
        super.slotsChanged(changed);
        if (syncing || player.level().isClientSide()) {
            return; // el servidor manda; el cliente recibe el resultado sincronizado
        }
        ItemStack weapon = container.getItem(WEAPON_SLOT);
        syncing = true;
        try {
            if (weapon.isEmpty()) {
                // Arma retirada: las ranuras se vacían (los módulos van montados en ella).
                // Ojo: al sacar el arma, el objeto original queda con cantidad 0 pero es el MISMO objeto,
                // por eso se comprueba isEmpty() y no solo si cambió la referencia (evita duplicar módulos).
                lastWeapon = ItemStack.EMPTY;
                for (int i = 1; i <= MODULE_COUNT; i++) {
                    container.setItem(i, ItemStack.EMPTY);
                }
            } else if (weapon != lastWeapon) {
                // Arma nueva: mostrar sus módulos en las ranuras
                lastWeapon = weapon;
                for (WeaponModule module : WeaponModule.values()) {
                    boolean installed = WeaponModule.has(weapon, module);
                    container.setItem(1 + module.ordinal(),
                            installed ? new ItemStack(ModItems.moduleItem(module)) : ItemStack.EMPTY);
                }
            } else {
                // Cambió un módulo: el arma pasa a tener exactamente los módulos de las ranuras
                int mask = 0;
                for (WeaponModule module : WeaponModule.values()) {
                    if (!container.getItem(1 + module.ordinal()).isEmpty()) {
                        mask |= module.bit();
                    }
                }
                if (mask != WeaponModule.getMask(weapon)) {
                    WeaponModule.setMask(weapon, mask);
                    // Al quitar el Cargador Ampliado, las balas que ya no caben vuelven como fragmentos
                    int excess = KineticPistolItem.clampAmmoToMag(weapon);
                    if (excess > 0) {
                        player.getInventory().placeItemBackInInventory(
                                new ItemStack(ModItems.CINETRITE_SHARD.get(), excess));
                    }
                }
            }
        } finally {
            syncing = false;
        }
        broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < INV_START) {
            // Del banco al inventario
            if (!moveItemStackTo(stack, INV_START, INV_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof KineticPistolItem
                && moveItemStackTo(stack, WEAPON_SLOT, WEAPON_SLOT + 1, false)) {
            // arma colocada en el banco
        } else if (stack.getItem() instanceof WeaponModuleItem m
                && moveItemStackTo(stack, 1 + m.getModule().ordinal(), 2 + m.getModule().ordinal(), false)) {
            // módulo instalado
        } else if (index < INV_START + 27) {
            // si no cabe en el banco: inventario <-> barra rápida, como siempre
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
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer) {
            // Solo se devuelve el arma: los módulos de las ranuras ya van montados en ella
            ItemStack weapon = container.removeItemNoUpdate(WEAPON_SLOT);
            for (int i = 1; i <= MODULE_COUNT; i++) {
                ItemStack leftover = container.removeItemNoUpdate(i);
                if (weapon.isEmpty() && !leftover.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(leftover); // por seguridad
                }
            }
            if (!weapon.isEmpty()) {
                // Si el jugador se desconectó o murió con el banco abierto, el arma cae al suelo
                // (su inventario ya se guardó o se va a descartar). Igual que hace vanilla.
                if (!player.isAlive() || ((ServerPlayer) player).hasDisconnected()) {
                    player.drop(weapon, false);
                } else {
                    player.getInventory().placeItemBackInInventory(weapon);
                }
            }
        }
    }

    /** El doble clic (recoger todo) no debe sacar módulos instalados del banco. */
    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != container && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.WEAPON_BENCH.get());
    }

    public boolean hasWeapon() {
        return slots.get(WEAPON_SLOT).hasItem();
    }
}
