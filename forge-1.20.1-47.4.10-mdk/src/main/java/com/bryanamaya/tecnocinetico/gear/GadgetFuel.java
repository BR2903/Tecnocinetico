package com.bryanamaya.tecnocinetico.gear;

import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Combustible de cada gadget. Los de batería (visor, impulsor de esquiva, botas de anclaje) siguen recargándose con su
 * objeto; los de combustible gastan algo real y, al quedarse casi vacíos, <b>repostan solos</b> del inventario del
 * jugador (si el gadget está encendido):
 * <ul>
 *   <li>Jetpack: gasolina (Depósito de Combustible) o Combustible Cinético (rinde el triple).</li>
 *   <li>Propulsor Acuático: igual que el jetpack.</li>
 *   <li>Refrigerador Térmico: Cargas de Refrigerante.</li>
 *   <li>Filtro de Ceniza: Cartuchos Filtrantes (de carbón).</li>
 * </ul>
 * La cantidad es lo que suma un objeto a la carga (durabilidad del gadget); {@link #FULL} = lo llena.
 */
public final class GadgetFuel {

    public static final int FULL = Integer.MAX_VALUE;
    /** Por debajo de esta fracción de carga reposta solo. */
    public static final float REFUEL_BELOW = 0.05F;

    public record Fuel(Supplier<Item> item, int amount) {}

    private static Map<Item, List<Fuel>> table;

    private GadgetFuel() {}

    private static Map<Item, List<Fuel>> table() {
        if (table == null) {
            List<Fuel> gasoline = List.of(new Fuel(ModItems.FUEL_CANISTER, 800), new Fuel(ModItems.KINETIC_FUEL_CELL, 2400));
            table = Map.of(
                    ModItems.JETPACK.get(), concat(gasoline, new Fuel(ModItems.JETPACK_FUEL, FULL)),            // cartucho antiguo: sigue valiendo
                    ModItems.AQUA_PROPULSOR.get(), concat(List.of(new Fuel(ModItems.FUEL_CANISTER, 600), new Fuel(ModItems.KINETIC_FUEL_CELL, 1800)),
                            new Fuel(ModItems.PROPULSOR_CELL, FULL)),
                    ModItems.THERMAL_COOLER.get(), List.of(new Fuel(ModItems.COOLANT_PACK, FULL)),
                    ModItems.ASH_FILTER.get(), List.of(new Fuel(ModItems.FILTER_CELL, FULL)));
        }
        return table;
    }

    private static List<Fuel> concat(List<Fuel> a, Fuel b) {
        var out = new java.util.ArrayList<>(a);
        out.add(b);
        return List.copyOf(out);
    }

    /** ¿Gasta combustible (y no batería)? */
    public static boolean usesFuel(Item gadget) {
        return table().containsKey(gadget);
    }

    /** Lo que añade `fuel` al gadget, o 0 si no le sirve. */
    public static int value(Item gadget, ItemStack fuel) {
        for (Fuel f : table().getOrDefault(gadget, List.of())) {
            if (fuel.is(f.item().get())) {
                return f.amount();
            }
        }
        return 0;
    }

    public static List<Fuel> fuels(Item gadget) {
        return table().getOrDefault(gadget, List.of());
    }

    /** Añade combustible al gadget (sin pasarse). */
    public static void add(ItemStack gadget, int amount) {
        gadget.setDamageValue(amount == FULL ? 0 : Math.max(0, gadget.getDamageValue() - amount));
    }

    /**
     * Si el gadget de esa ranura gasta combustible, está encendido y casi vacío, gasta un objeto de combustible del
     * inventario del jugador (el que más rinde primero no: el primero que encuentre). Devuelve true si repostó.
     */
    public static boolean autoRefuel(Player p, TacticalGear g, int slot) {
        ItemStack gadget = g.active(slot);
        if (gadget.isEmpty() || !usesFuel(gadget.getItem())) {
            return false;
        }
        int max = gadget.getMaxDamage() - 1, left = max - gadget.getDamageValue();
        if (left > max * REFUEL_BELOW) {
            return false;
        }
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            int amount = value(gadget.getItem(), s);
            if (amount > 0) {
                add(gadget, amount);
                if (!p.getAbilities().instabuild) {
                    s.shrink(1);
                }
                g.setChanged();
                p.displayClientMessage(Component.translatable("message.tecnocinetico.gadget_refueled", gadget.getHoverName(), s.getHoverName())
                        .withStyle(ChatFormatting.GOLD), true);
                return true;
            }
        }
        return false;
    }
}
