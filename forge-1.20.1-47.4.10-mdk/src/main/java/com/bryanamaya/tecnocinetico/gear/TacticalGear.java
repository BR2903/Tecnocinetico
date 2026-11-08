package com.bryanamaya.tecnocinetico.gear;

import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.item.custom.MagazineItem;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.HoneyBottleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Equipo Táctico del jugador (pestaña extra del inventario, tecla J): ranuras de munición, comida y gadgets.
 * Se guarda como capacidad de Forge en el jugador y se sincroniza al cliente con {@code SyncGearPacket}.
 *
 * Ranuras: 0-5 munición (balas y cargadores), 6-14 comida (se come sola), 15 cara, 16 espalda, 17 pies.
 * (Hasta el 2026-11-07 eran 10 ranuras: 0-3, 4-6, 7, 8, 9; {@link #load} migra los equipos guardados.)
 */
public class TacticalGear {

    public static final int AMMO_START = 0, AMMO_COUNT = 6;
    public static final int FOOD_START = 6, FOOD_COUNT = 9;
    public static final int FACE = 15, BACK = 16, FEET = 17;
    public static final int SIZE = 18;
    /** Nombre de cada ranura de gadget (textos "tooltip.tecnocinetico.gadget_slot.<nombre>"). */
    public static String slotName(int slot) {
        return slot == FACE ? "face" : slot == BACK ? "back" : "feet";
    }

    public static final Capability<TacticalGear> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    private final ItemStackHandler items = new ItemStackHandler(SIZE) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return accepts(slot, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            dirty = true;
        }
    };
    private boolean visorOn = true;
    private boolean dirty = true;

    public ItemStackHandler items() {
        return items;
    }

    public ItemStack get(int slot) {
        return items.getStackInSlot(slot);
    }

    public boolean visorOn() {
        return visorOn;
    }

    public void setVisorOn(boolean on) {
        visorOn = on;
        dirty = true;
    }

    /** Hay que reenviar el equipo al cliente (lo consume el tick del servidor). */
    public boolean takeDirty() {
        boolean d = dirty;
        dirty = false;
        return d;
    }

    public void setChanged() {
        dirty = true;
    }

    // ------------------------------------------------------------------ qué cabe en cada ranura

    public static boolean isAmmo(ItemStack s) {
        return s.is(ModItems.KINETIC_ROUND.get()) || s.is(ModItems.GUIDED_ROUND.get()) || s.getItem() instanceof MagazineItem;
    }

    public static boolean isAutoFood(ItemStack s) {
        return s.isEdible() && !(s.getItem() instanceof BowlFoodItem) && !(s.getItem() instanceof SuspiciousStewItem)
                && !(s.getItem() instanceof HoneyBottleItem);
    }

    public static boolean accepts(int slot, ItemStack s) {
        if (slot < FOOD_START) {
            return isAmmo(s);
        }
        if (slot < FACE) {
            return isAutoFood(s);
        }
        return s.getItem() instanceof GadgetItem g && g.slot() == slot;
    }

    // ------------------------------------------------------------------ acceso

    public static Optional<TacticalGear> of(Player player) {
        return player.getCapability(CAPABILITY).resolve();
    }

    /**
     * Pilas donde buscar munición: primero las ranuras de munición del equipo y luego el inventario.
     * Son las pilas reales (se pueden encoger); tras usarlas, llama a {@link #setChanged()}.
     */
    public static List<ItemStack> ammoSources(Player player) {
        List<ItemStack> out = new ArrayList<>();
        of(player).ifPresent(g -> {
            for (int i = AMMO_START; i < AMMO_START + AMMO_COUNT; i++) {
                out.add(g.items.getStackInSlot(i));
            }
        });
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            out.add(player.getInventory().getItem(i));
        }
        return out;
    }

    /** Devuelve un cargador (u otra munición) al jugador: primero a una ranura de munición libre, si no al inventario. */
    public static void giveBackAmmo(Player player, ItemStack stack) {
        Optional<TacticalGear> gear = of(player);
        if (gear.isPresent()) {
            ItemStackHandler h = gear.get().items;
            for (int i = AMMO_START; i < AMMO_START + AMMO_COUNT && !stack.isEmpty(); i++) {
                stack = h.insertItem(i, stack, false);
            }
        }
        if (!stack.isEmpty()) {
            player.getInventory().placeItemBackInInventory(stack);
        }
    }

    // ------------------------------------------------------------------ guardado

    public CompoundTag save() {
        CompoundTag tag = items.serializeNBT();
        tag.putBoolean("VisorOn", visorOn);
        return tag;
    }

    public void load(CompoundTag tag) {
        int savedSize = tag.contains("Size") ? tag.getInt("Size") : SIZE;
        if (savedSize == 10) {
            // formato antiguo (4 munición, 3 comida, cara, espalda, pies): se recolocan en las ranuras nuevas
            ItemStackHandler old = new ItemStackHandler(10);
            old.deserializeNBT(tag);
            int[] map = {0, 1, 2, 3, FOOD_START, FOOD_START + 1, FOOD_START + 2, FACE, BACK, FEET};
            items.setSize(SIZE);
            for (int i = 0; i < 10; i++) {
                items.setStackInSlot(map[i], old.getStackInSlot(i));
            }
        } else {
            items.deserializeNBT(tag);
            if (items.getSlots() != SIZE) {
                items.setSize(SIZE);
            }
        }
        visorOn = !tag.contains("VisorOn") || tag.getBoolean("VisorOn");
        dirty = true;
    }

    /** Proveedor que se engancha a cada jugador (ver GearEvents). */
    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final TacticalGear gear = new TacticalGear();
        private final LazyOptional<TacticalGear> opt = LazyOptional.of(() -> gear);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
            return cap == CAPABILITY ? opt.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return gear.save();
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            gear.load(tag);
        }
    }
}
