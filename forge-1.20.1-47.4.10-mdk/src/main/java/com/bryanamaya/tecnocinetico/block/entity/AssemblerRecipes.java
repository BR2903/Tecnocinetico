package com.bryanamaya.tecnocinetico.block.entity;

import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Recetas del Ensamblador de Armas (tier 3). Sin forma: cada receta pide hasta 6 ingredientes distintos con su
 * cantidad, en cualquier hueco. Las armas piden un Procesador del Núcleo (solo de las Instalaciones de la IA).
 * JEI lee esta misma lista.
 */
public final class AssemblerRecipes {

    public record Recipe(List<ItemStack> inputs, ItemStack output, int ticks) {}

    private static List<Recipe> recipes;

    private AssemblerRecipes() {}

    private static ItemStack s(Item item, int n) {
        return new ItemStack(item, n);
    }

    private static Recipe armor(net.minecraftforge.registries.RegistryObject<Item> out, ItemStack... inputs) {
        return new Recipe(List.of(inputs), new ItemStack(out.get()), 300);
    }

    public static List<Recipe> all() {
        if (recipes == null) {
            recipes = List.of(
                    new Recipe(List.of(s(ModItems.CORE_PROCESSOR.get(), 1), s(ModItems.INERCITA_PLATE.get(), 6),
                            s(ModItems.PRECISION_MECHANISM.get(), 2), s(ModItems.KINETIC_SERVO.get(), 2),
                            s(ModItems.CINETRITE_INGOT.get(), 4), s(ModItems.TITANIUM_INGOT.get(), 4)),
                            new ItemStack(ModItems.RAIL_RIFLE.get()), 400),
                    new Recipe(List.of(s(ModItems.CORE_PROCESSOR.get(), 1), s(ModItems.INERCITA_PLATE.get(), 4),
                            s(ModItems.PRECISION_MECHANISM.get(), 3), s(ModItems.TITANIUM_INGOT.get(), 4), s(ModItems.BALLISTIC_FABRIC.get(), 2)),
                            new ItemStack(ModItems.ASSAULT_RIFLE.get()), 300),
                    new Recipe(List.of(s(ModItems.CORE_PROCESSOR.get(), 1), s(ModItems.INERCITA_PLATE.get(), 4),
                            s(ModItems.PRECISION_MECHANISM.get(), 2), s(ModItems.KINETIC_SERVO.get(), 1), s(ModItems.TITANIUM_INGOT.get(), 3)),
                            new ItemStack(ModItems.GRENADE_LAUNCHER.get()), 300),
                    new Recipe(List.of(s(ModItems.CORE_PROCESSOR.get(), 1), s(ModItems.KINETIC_SERVO.get(), 3),
                            s(ModItems.CINETRITE_INGOT.get(), 6), s(ModItems.PRECISION_MECHANISM.get(), 2),
                            s(ModItems.PIEZO_TRANSDUCER.get(), 2), s(ModItems.INERCITA_PLATE.get(), 2)),
                            new ItemStack(ModItems.PULSE_CANNON.get()), 400),
                    // ---- armaduras tier 3
                    armor(ModItems.CORE_HELMET, s(ModItems.CORE_PROCESSOR.get(), 1), s(ModItems.INERCITA_PLATE.get(), 4),
                            s(ModItems.PRECISION_MECHANISM.get(), 1), s(ModItems.CINETRITE_INGOT.get(), 2), s(ModItems.TITANIUM_INGOT.get(), 2)),
                    armor(ModItems.CORE_CHESTPLATE, s(ModItems.CORE_PROCESSOR.get(), 1), s(ModItems.INERCITA_PLATE.get(), 6),
                            s(ModItems.KINETIC_SERVO.get(), 2), s(ModItems.PRECISION_MECHANISM.get(), 2), s(ModItems.CINETRITE_INGOT.get(), 3), s(ModItems.TITANIUM_INGOT.get(), 3)),
                    armor(ModItems.CORE_LEGGINGS, s(ModItems.INERCITA_PLATE.get(), 5), s(ModItems.KINETIC_SERVO.get(), 2),
                            s(ModItems.PRECISION_MECHANISM.get(), 1), s(ModItems.CINETRITE_INGOT.get(), 2), s(ModItems.TITANIUM_INGOT.get(), 3)),
                    armor(ModItems.CORE_BOOTS, s(ModItems.INERCITA_PLATE.get(), 3), s(ModItems.KINETIC_SERVO.get(), 2),
                            s(ModItems.CINETRITE_INGOT.get(), 1), s(ModItems.TITANIUM_INGOT.get(), 2)),
                    armor(ModItems.PIROXITE_HELMET, s(ModItems.PIROXITE_INGOT.get(), 4), s(ModItems.INERCITA_PLATE.get(), 1),
                            s(ModItems.BALLISTIC_FABRIC.get(), 1)),
                    armor(ModItems.PIROXITE_CHESTPLATE, s(ModItems.PIROXITE_INGOT.get(), 7), s(ModItems.INERCITA_PLATE.get(), 2),
                            s(ModItems.KINETIC_SERVO.get(), 1), s(ModItems.BALLISTIC_FABRIC.get(), 2)),
                    armor(ModItems.PIROXITE_LEGGINGS, s(ModItems.PIROXITE_INGOT.get(), 6), s(ModItems.INERCITA_PLATE.get(), 2),
                            s(ModItems.BALLISTIC_FABRIC.get(), 2)),
                    armor(ModItems.PIROXITE_BOOTS, s(ModItems.PIROXITE_INGOT.get(), 3), s(ModItems.INERCITA_PLATE.get(), 1),
                            s(ModItems.BALLISTIC_FABRIC.get(), 1)),
                    armor(ModItems.NULLITE_HELMET, s(ModItems.NULLITE_INGOT.get(), 4), s(Items.ENDER_PEARL, 2),
                            s(ModItems.PRECISION_MECHANISM.get(), 1)),
                    armor(ModItems.NULLITE_CHESTPLATE, s(ModItems.NULLITE_INGOT.get(), 7), s(Items.ENDER_PEARL, 4),
                            s(ModItems.PRECISION_MECHANISM.get(), 1), s(ModItems.KINETIC_SERVO.get(), 1)),
                    armor(ModItems.NULLITE_LEGGINGS, s(ModItems.NULLITE_INGOT.get(), 6), s(Items.ENDER_PEARL, 3),
                            s(ModItems.PRECISION_MECHANISM.get(), 1)),
                    armor(ModItems.NULLITE_BOOTS, s(ModItems.NULLITE_INGOT.get(), 3), s(Items.ENDER_PEARL, 2),
                            s(ModItems.KINETIC_SERVO.get(), 1)),
                    // ---- armadura de combate Mk III (materiales de las tres dimensiones)
                    armor(ModItems.COMBAT_MK3_HELMET, s(ModItems.INERCITA_PLATE.get(), 3), s(ModItems.PIROXITE_INGOT.get(), 2),
                            s(ModItems.NULLITE_INGOT.get(), 2), s(ModItems.KINETIC_SERVO.get(), 1), s(ModItems.TITANIUM_INGOT.get(), 2)),
                    armor(ModItems.COMBAT_MK3_CHESTPLATE, s(ModItems.INERCITA_PLATE.get(), 5), s(ModItems.PIROXITE_INGOT.get(), 3),
                            s(ModItems.NULLITE_INGOT.get(), 3), s(ModItems.KINETIC_SERVO.get(), 2), s(ModItems.PRECISION_MECHANISM.get(), 1), s(ModItems.TITANIUM_INGOT.get(), 3)),
                    armor(ModItems.COMBAT_MK3_LEGGINGS, s(ModItems.INERCITA_PLATE.get(), 4), s(ModItems.PIROXITE_INGOT.get(), 3),
                            s(ModItems.NULLITE_INGOT.get(), 2), s(ModItems.KINETIC_SERVO.get(), 2), s(ModItems.TITANIUM_INGOT.get(), 3)),
                    armor(ModItems.COMBAT_MK3_BOOTS, s(ModItems.INERCITA_PLATE.get(), 2), s(ModItems.PIROXITE_INGOT.get(), 2),
                            s(ModItems.NULLITE_INGOT.get(), 1), s(ModItems.KINETIC_SERVO.get(), 1), s(ModItems.TITANIUM_INGOT.get(), 2)),
                    // ---- más armas tier 3
                    new Recipe(List.of(s(ModItems.CORE_PROCESSOR.get(), 1), s(ModItems.INERCITA_PLATE.get(), 5),
                            s(ModItems.PRECISION_MECHANISM.get(), 2), s(ModItems.KINETIC_SERVO.get(), 1), s(ModItems.TITANIUM_INGOT.get(), 4)),
                            new ItemStack(ModItems.ROCKET_LAUNCHER.get()), 300),
                    new Recipe(List.of(s(ModItems.CORE_PROCESSOR.get(), 1), s(ModItems.KINETIC_SERVO.get(), 2), s(ModItems.CINETRITE_INGOT.get(), 6),
                            s(ModItems.PIEZO_TRANSDUCER.get(), 3), s(ModItems.PRECISION_MECHANISM.get(), 2), s(Items.COPPER_INGOT, 8)),
                            new ItemStack(ModItems.ARC_CASTER.get()), 400),
                    new Recipe(List.of(s(ModItems.CORE_PROCESSOR.get(), 1), s(ModItems.INERCITA_PLATE.get(), 6), s(ModItems.KINETIC_SERVO.get(), 3),
                            s(ModItems.PRECISION_MECHANISM.get(), 2), s(ModItems.TITANIUM_INGOT.get(), 6)),
                            new ItemStack(ModItems.MINIGUN.get()), 400),
                    new Recipe(List.of(s(ModItems.INERCITA_INGOT.get(), 1), s(Items.GUNPOWDER, 4), s(ModItems.CINETRITE_SHARD.get(), 2), s(Items.IRON_INGOT, 2)),
                            new ItemStack(ModItems.GUIDED_ROCKET.get(), 2), 80),
                    // munición especial
                    new Recipe(List.of(s(ModItems.INERCITA_INGOT.get(), 1), s(ModItems.CINETRITE_SHARD.get(), 2), s(ModItems.TITANIUM_INGOT.get(), 1)),
                            new ItemStack(ModItems.RAIL_SLUG.get(), 4), 80),
                    new Recipe(List.of(s(Items.IRON_INGOT, 2), s(Items.GUNPOWDER, 2), s(ModItems.CINETRITE_SHARD.get(), 2)),
                            new ItemStack(ModItems.KINETIC_GRENADE.get(), 4), 80),
                    new Recipe(List.of(s(ModItems.CINETRITE_INGOT.get(), 2), s(Items.COPPER_INGOT, 4), s(ModItems.PIEZO_TRANSDUCER.get(), 1),
                            s(ModItems.LEAD_INGOT.get(), 2)),
                            new ItemStack(ModItems.PULSE_CELL.get()), 120));
        }
        return recipes;
    }

    /** Receta que encaja con lo que hay en los huecos (todo lo que hay debe ser ingrediente y alcanzar). */
    @Nullable
    public static Recipe find(IItemHandler inv, int slots) {
        outer:
        for (Recipe r : all()) {
            for (int i = 0; i < slots; i++) {
                ItemStack st = inv.getStackInSlot(i);
                if (!st.isEmpty() && r.inputs().stream().noneMatch(in -> in.is(st.getItem()))) {
                    continue outer;
                }
            }
            for (ItemStack in : r.inputs()) {
                if (count(inv, slots, in.getItem()) < in.getCount()) {
                    continue outer;
                }
            }
            return r;
        }
        return null;
    }

    public static boolean isIngredient(ItemStack stack) {
        return all().stream().anyMatch(r -> r.inputs().stream().anyMatch(in -> in.is(stack.getItem())));
    }

    public static void consume(IItemHandler inv, int slots, Recipe r) {
        for (ItemStack in : r.inputs()) {
            int left = in.getCount();
            for (int i = 0; i < slots && left > 0; i++) {
                if (inv.getStackInSlot(i).is(in.getItem())) {
                    left -= inv.extractItem(i, left, false).getCount();
                }
            }
        }
    }

    private static int count(IItemHandler inv, int slots, Item item) {
        int n = 0;
        for (int i = 0; i < slots; i++) {
            if (inv.getStackInSlot(i).is(item)) {
                n += inv.getStackInSlot(i).getCount();
            }
        }
        return n;
    }
}
