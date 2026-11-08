package com.bryanamaya.tecnocinetico.block.entity;

import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Recetas compartidas por la Prensa de Ensamblaje (manual) y la Prensa Cinética Automática.
 * Cada receta es un conjunto exacto de ingredientes (uno por hueco, en cualquier orden).
 * JEI lee esta misma lista para mostrarlas.
 */
public final class PressRecipes {

    /**
     * Una receta de prensa: ingredientes distintos (1 de cada) -> resultado. Las "powered" (piezas del tier 2)
     * solo las hace la Prensa Cinética Automática con electricidad: a martillo no salen.
     */
    public record Recipe(List<Item> inputs, ItemStack output, boolean powered) {
        public Recipe(List<Item> inputs, ItemStack output) {
            this(inputs, output, false);
        }
    }

    private static List<Recipe> recipes;

    private PressRecipes() {}

    /** Lista de recetas (se crea al primer uso, cuando los objetos ya están registrados). */
    public static List<Recipe> all() {
        if (recipes == null) {
            recipes = List.of(
                    // Transductor Piezo-Cúprico
                    new Recipe(List.of(Items.COPPER_INGOT, ModItems.CINETRITE_INGOT.get()),
                            new ItemStack(ModItems.PIEZO_TRANSDUCER.get())),
                    // Sustrato Lógico de Pizarra
                    new Recipe(List.of(Items.COBBLED_DEEPSLATE_SLAB, Items.REDSTONE, ModItems.CINETRITE_INGOT.get()),
                            new ItemStack(ModItems.DEEPSLATE_LOGIC_SUBSTRATE.get())),
                    // Acoplador Cinético Bruto
                    new Recipe(List.of(Items.IRON_INGOT, ModBlocks.ANCIENT_ALLOY_BLOCK.get().asItem(), ModItems.CINETRITE_SHARD.get()),
                            new ItemStack(ModItems.CRUDE_KINETIC_COUPLER.get())),
                    // Fragmentación: un Cristal de Cinetrita -> 2 fragmentos
                    new Recipe(List.of(ModItems.CINETRITE_INGOT.get()),
                            new ItemStack(ModItems.CINETRITE_SHARD.get(), 2)),
                    // ---- tier 2: solo con energía
                    // Placa de Inercita
                    new Recipe(List.of(ModItems.INERCITA_INGOT.get(), Items.IRON_INGOT, ModItems.SCRAP_METAL.get()),
                            new ItemStack(ModItems.INERCITA_PLATE.get()), true),
                    // Mecanismo de Precisión
                    new Recipe(List.of(Items.IRON_INGOT, ModItems.PIEZO_TRANSDUCER.get(), ModItems.DEEPSLATE_LOGIC_SUBSTRATE.get()),
                            new ItemStack(ModItems.PRECISION_MECHANISM.get()), true),
                    // Servo Cinético
                    new Recipe(List.of(ModItems.CINETRITE_INGOT.get(), ModItems.CRUDE_KINETIC_COUPLER.get(), Items.COPPER_INGOT),
                            new ItemStack(ModItems.KINETIC_SERVO.get()), true),
                    // Recargar una Célula de Impulso vacía
                    new Recipe(List.of(ModItems.EMPTY_PULSE_CELL.get()), new ItemStack(ModItems.PULSE_CELL.get()), true),
                    // Tela Balística
                    new Recipe(List.of(Items.LEATHER, Items.STRING, ModItems.CINETRITE_SHARD.get()),
                            new ItemStack(ModItems.BALLISTIC_FABRIC.get(), 2), true));
        }
        return recipes;
    }

    public static @NotNull ItemStack findResult(IItemHandler inv, int inputSlots, boolean powered) {
        int filled = 0;
        for (int i = 0; i < inputSlots; i++) {
            if (!inv.getStackInSlot(i).isEmpty()) {
                filled++;
            }
        }
        for (Recipe recipe : all()) {
            if ((powered || !recipe.powered()) && recipe.inputs().size() == filled
                    && recipe.inputs().stream().allMatch(item -> has(inv, inputSlots, item))) {
                return recipe.output().copy();
            }
        }
        return ItemStack.EMPTY;
    }

    /** ¿Este objeto se usa en alguna receta de prensa? Evita que se metan objetos inútiles (p. ej. el martillo). */
    public static boolean isIngredient(ItemStack stack, boolean powered) {
        return all().stream().anyMatch(r -> (powered || !r.powered()) && r.inputs().contains(stack.getItem()));
    }

    /** Consume 1 unidad de cada hueco de entrada ocupado. */
    public static void consumeInputs(IItemHandler inv, int inputSlots) {
        for (int i = 0; i < inputSlots; i++) {
            if (!inv.getStackInSlot(i).isEmpty()) {
                inv.extractItem(i, 1, false);
            }
        }
    }

    private static boolean has(IItemHandler inv, int inputSlots, Item target) {
        for (int i = 0; i < inputSlots; i++) {
            if (inv.getStackInSlot(i).is(target)) {
                return true;
            }
        }
        return false;
    }
}
