package com.bryanamaya.tecnocinetico.machine;

import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Supplier;

/** Recetas del Refinador Cinético y del Fabricador de munición (en código, como las de la prensa). */
public final class MachineRecipes {

    private MachineRecipes() {}

    /** Refinado: 1 mena en bruto -> `count` de polvo, y `extra` con probabilidad `chance`. */
    public record Refine(Supplier<Item> in, Supplier<Item> out, int count, Supplier<Item> extra, float chance, int ticks, int perTick) {}

    /** Fabricación: hasta 3 ingredientes (objeto, cantidad) en cualquier orden -> resultado. */
    public record Fabricate(List<Ingredient> in, Supplier<Item> out, int count, int ticks, int perTick) {}

    public record Ingredient(Supplier<Item> item, int count) {}

    private static Ingredient of(Supplier<Item> item, int count) {
        return new Ingredient(item, count);
    }

    private static Supplier<Item> v(Item item) {
        return () -> item;
    }

    public static final List<Refine> REFINE = List.of(
            new Refine(v(Items.RAW_IRON), ModItems.IRON_DUST, 2, ModItems.TITANIUM_DUST, 0.12F, 100, 20),    // arenas de ilmenita
            new Refine(v(Items.RAW_COPPER), ModItems.COPPER_DUST, 2, ModItems.GOLD_DUST, 0.10F, 100, 20),
            new Refine(v(Items.RAW_GOLD), ModItems.GOLD_DUST, 2, ModItems.COPPER_DUST, 0.10F, 100, 20),
            new Refine(ModItems.RAW_LEAD, ModItems.LEAD_DUST, 2, ModItems.TIN_DUST, 0.12F, 100, 20),
            new Refine(ModItems.RAW_TIN, ModItems.TIN_DUST, 2, ModItems.LEAD_DUST, 0.10F, 100, 20),
            new Refine(ModItems.RAW_TITANIUM, ModItems.TITANIUM_DUST, 2, ModItems.IRON_DUST, 0.15F, 160, 40),
            new Refine(ModItems.RAW_CINETRITE, ModItems.CINETRITE_DUST, 2, ModItems.CINETRITE_SHARD, 0.15F, 120, 30),
            new Refine(ModItems.RAW_INERCITA, ModItems.INERCITA_DUST, 2, ModItems.CINETRITE_DUST, 0.08F, 160, 40),
            new Refine(ModItems.RAW_PIROXITE, ModItems.PIROXITE_DUST, 2, ModItems.GOLD_DUST, 0.10F, 200, 60),
            new Refine(ModItems.RAW_NULLITE, ModItems.NULLITE_DUST, 2, ModItems.INERCITA_DUST, 0.05F, 200, 60),
            new Refine(ModItems.SCRAP_METAL, ModItems.IRON_DUST, 1, ModItems.TIN_DUST, 0.25F, 60, 20));        // chatarra: hierro y soldadura

    public static final List<Fabricate> FABRICATE = List.of(
            new Fabricate(List.of(of(ModItems.LEAD_INGOT, 1), of(v(Items.COPPER_INGOT), 1), of(ModItems.CINETRITE_SHARD, 2)),
                    ModItems.KINETIC_ROUND, 8, 100, 30),
            new Fabricate(List.of(of(ModItems.LEAD_INGOT, 1), of(v(Items.COPPER_INGOT), 1), of(v(Items.GUNPOWDER), 2)),
                    ModItems.RIFLE_ROUND, 8, 100, 30),
            new Fabricate(List.of(of(ModItems.LEAD_NUGGET, 4), of(v(Items.PAPER), 2), of(v(Items.GUNPOWDER), 2)),
                    ModItems.SHOTGUN_SHELL, 8, 100, 30),
            new Fabricate(List.of(of(ModItems.TITANIUM_INGOT, 1), of(ModItems.CINETRITE_SHARD, 2)),
                    ModItems.RAIL_SLUG, 4, 140, 50),
            new Fabricate(List.of(of(v(Items.IRON_INGOT), 2), of(v(Items.GUNPOWDER), 2), of(ModItems.CINETRITE_SHARD, 1)),
                    ModItems.KINETIC_GRENADE, 2, 120, 40));

    public static Refine refine(ItemStack in) {
        for (Refine r : REFINE) {
            if (in.is(r.in().get())) {
                return r;
            }
        }
        return null;
    }

    public static boolean isFabricateIngredient(ItemStack stack) {
        for (Fabricate f : FABRICATE) {
            for (Ingredient i : f.in()) {
                if (stack.is(i.item().get())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Receta de fabricación cuyos ingredientes están todos en los huecos (y no sobra ningún tipo de objeto). */
    public static Fabricate fabricate(List<ItemStack> slots) {
        outer:
        for (Fabricate f : FABRICATE) {
            for (Ingredient i : f.in()) {
                int have = 0;
                for (ItemStack s : slots) {
                    if (s.is(i.item().get())) {
                        have += s.getCount();
                    }
                }
                if (have < i.count()) {
                    continue outer;
                }
            }
            for (ItemStack s : slots) {
                if (!s.isEmpty() && f.in().stream().noneMatch(i -> s.is(i.item().get()))) {
                    continue outer;
                }
            }
            return f;
        }
        return null;
    }
}
