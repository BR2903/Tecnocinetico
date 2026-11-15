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
 * Ranuras: 0-15 suministros (munición, cargadores, granadas, cohetes, células, combustible y baterías: {@link #isSupply}),
 * 16-24 comida (se come sola), 25 cara, 26 espalda, 27 pies, 28 cinturón, 29 muñeca.
 * (Hasta la v1 había 6 de munición: 0-5, 6-14, 15-19; {@link #load} recoloca los equipos guardados.)
 * (Hasta el 2026-11-07 eran 10 ranuras: 0-3, 4-6, 7, 8, 9; {@link #load} migra los equipos guardados.)
 */
public class TacticalGear {

    public static final int AMMO_START = 0, AMMO_COUNT = 16;
    public static final int FOOD_START = 16, FOOD_COUNT = 9;
    public static final int FACE = 25, BACK = 26, FEET = 27, BELT = 28, WRIST = 29;
    /** Ranuras de munición del formato anterior (v1): 6. */
    private static final int OLD_AMMO = 6;
    /** Última ranura de gadget (los gadgets van de FACE a LAST_GADGET). */
    public static final int LAST_GADGET = WRIST;
    public static final int SIZE = 30;
    /** Suministros extra que caben en las ranuras de munición (los mods o packs pueden añadir más). */
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> SUPPLIES = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.bryanamaya.tecnocinetico.Tecnocinetico.MOD_ID, "tactical_supplies"));
    /** Nombre de cada ranura de gadget (textos "tooltip.tecnocinetico.gadget_slot.<nombre>"). */
    public static String slotName(int slot) {
        return slot == FACE ? "face" : slot == BACK ? "back" : slot == FEET ? "feet" : slot == BELT ? "belt" : "wrist";
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
    /** Encendido de cada gadget (cara, espalda, pies, cinturón). Apagado no funciona ni gasta. */
    private final boolean[] enabled = {true, true, true, true, true};
    private boolean dirty = true;
    /** Lo que otros ven puesto (gadget y nivel de cara, espalda y pies) la última vez que se envió; no se guarda. */
    private int wornSignature = Integer.MIN_VALUE;

    public ItemStackHandler items() {
        return items;
    }

    public ItemStack get(int slot) {
        return items.getStackInSlot(slot);
    }

    public boolean enabled(int slot) {
        return slot < FACE || slot > LAST_GADGET || enabled[slot - FACE];
    }

    public void setEnabled(int slot, boolean on) {
        if (slot >= FACE && slot <= LAST_GADGET) {
            enabled[slot - FACE] = on;
            dirty = true;
        }
    }

    /** El gadget de la ranura si está encendido; si está apagado, vacío (para aplicar efectos). */
    public ItemStack active(int slot) {
        return enabled(slot) ? get(slot) : ItemStack.EMPTY;
    }

    /**
     * Como {@link #active(int)} pero con el Pulso EMP: si el jugador lo sufre, los gadgets no funcionan salvo los
     * Mk III (apantallados). Úsalo siempre que haya jugador.
     */
    public ItemStack active(net.minecraft.world.entity.player.Player p, int slot) {
        ItemStack s = active(slot);
        return jammed(p, s) ? ItemStack.EMPTY : s;
    }

    /** ¿El EMP apaga este gadget? */
    public static boolean jammed(net.minecraft.world.entity.player.Player p, ItemStack gadget) {
        return !gadget.isEmpty() && p.hasEffect(com.bryanamaya.tecnocinetico.effect.ModEffects.EMP.get())
                && !(gadget.getItem() instanceof NetherGadgetItem && NetherGadgetItem.level(gadget) >= 3);
    }

    /** El visor se enciende/apaga como su ranura (tecla N o el botón del Equipo Táctico). */
    public boolean visorOn() {
        return enabled(FACE);
    }

    public void setVisorOn(boolean on) {
        setEnabled(FACE, on);
    }

    /**
     * ¿Cambió lo que los demás ven puesto (no la batería)? Así el paquete de gadgets visibles no se manda a todos los que
     * te ven cada vez que se gasta batería (el jetpack la gasta cada 2 ticks).
     */
    public boolean takeWornChanged() {
        int sig = 1;
        for (int slot : new int[]{FACE, BACK, FEET, WRIST}) {
            ItemStack s = get(slot);
            sig = sig * 31 + (s.isEmpty() ? 0 : net.minecraft.world.item.Item.getId(s.getItem()));
            sig = sig * 31 + (s.getTag() == null ? 0 : s.getTag().getInt("Level"));
        }
        boolean changed = sig != wornSignature;
        wornSignature = sig;
        return changed;
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
        return s.getItem() instanceof com.bryanamaya.tecnocinetico.item.custom.AmmoPackItem || s.getItem() instanceof MagazineItem;
    }

    /** ¿Cabe en una ranura de munición? Munición y cargadores, recargas de gadgets (baterías, combustible) y la etiqueta {@link #SUPPLIES}. */
    public static boolean isSupply(ItemStack s) {
        return isAmmo(s) || s.getItem() instanceof RechargeItem || s.is(SUPPLIES);
    }

    public static boolean isAutoFood(ItemStack s) {
        return s.isEdible() && !(s.getItem() instanceof BowlFoodItem) && !(s.getItem() instanceof SuspiciousStewItem)
                && !(s.getItem() instanceof HoneyBottleItem);
    }

    public static boolean accepts(int slot, ItemStack s) {
        if (slot < FOOD_START) {
            return isSupply(s);
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
        byte bits = 0;
        for (int i = 0; i < enabled.length; i++) {
            if (enabled[i]) {
                bits |= (byte) (1 << i);
            }
        }
        tag.putByte("Enabled", bits);
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
        } else if (savedSize != SIZE) {
            // formatos anteriores (18-20 ranuras, con 6 de munición): la munición se queda y lo demás baja 10 huecos
            ItemStackHandler old = new ItemStackHandler(savedSize);
            old.deserializeNBT(tag);
            items.setSize(SIZE);
            for (int i = 0; i < savedSize; i++) {
                int to = i < OLD_AMMO ? i : i + (AMMO_COUNT - OLD_AMMO);
                if (to < SIZE) {
                    items.setStackInSlot(to, old.getStackInSlot(i));
                }
            }
        } else {
            items.deserializeNBT(tag);
        }
        if (tag.contains("Enabled")) {
            byte bits = tag.getByte("Enabled");
            for (int i = 0; i < enabled.length; i++) {
                enabled[i] = (bits & (1 << i)) != 0 || i >= 4 && savedSize < SIZE;   // ranuras nuevas: encendidas
            }
        } else {                                                                     // formato antiguo: solo el visor
            java.util.Arrays.fill(enabled, true);
            enabled[0] = !tag.contains("VisorOn") || tag.getBoolean("VisorOn");
        }
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
