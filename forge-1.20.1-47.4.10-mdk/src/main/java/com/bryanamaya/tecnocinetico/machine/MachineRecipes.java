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
            new Refine(ModItems.RAW_CINETRITE, ModItems.CINETRITE_DUST, 2, ModItems.CINETRITE_SHARD, 0.15F, 120, 30),
            new Refine(ModItems.RAW_INERCITA, ModItems.INERCITA_DUST, 2, ModItems.CINETRITE_DUST, 0.08F, 160, 40),
            new Refine(ModItems.RAW_PIROXITE, ModItems.PIROXITE_DUST, 2, ModItems.GOLD_DUST, 0.10F, 200, 60),
            new Refine(ModItems.RAW_NULLITE, ModItems.NULLITE_DUST, 2, ModItems.INERCITA_DUST, 0.05F, 200, 60),
            new Refine(ModItems.SCRAP_METAL, ModItems.IRON_DUST, 1, ModItems.TIN_DUST, 0.25F, 60, 20),         // chatarra: hierro y soldadura
            // azufre del carbón (para la pólvora negra y el laboratorio)
            new Refine(v(Items.COAL), ModItems.SULFUR_DUST, 1, ModItems.SULFUR_DUST, 0.25F, 80, 20),
            new Refine(v(Items.COAL_ORE), ModItems.SULFUR_DUST, 2, v(Items.COAL), 0.5F, 160, 40),
            new Refine(v(Items.DEEPSLATE_COAL_ORE), ModItems.SULFUR_DUST, 2, v(Items.COAL), 0.5F, 160, 40));

    public static final List<Fabricate> FABRICATE = List.of(
            new Fabricate(List.of(of(ModItems.LEAD_INGOT, 1), of(v(Items.COPPER_INGOT), 1), of(ModItems.CINETRITE_SHARD, 2)),
                    ModItems.KINETIC_ROUND, 2, 100, 30),                       // paquetes (12 balas)
            new Fabricate(List.of(of(ModItems.LEAD_INGOT, 1), of(v(Items.COPPER_INGOT), 1), of(v(Items.GUNPOWDER), 2)),
                    ModItems.RIFLE_ROUND, 2, 100, 30),
            new Fabricate(List.of(of(ModItems.LEAD_NUGGET, 4), of(v(Items.PAPER), 2), of(v(Items.GUNPOWDER), 2)),
                    ModItems.SHOTGUN_SHELL, 2, 100, 30),
            new Fabricate(List.of(of(v(Items.IRON_INGOT), 2), of(v(Items.GUNPOWDER), 2), of(ModItems.CINETRITE_SHARD, 1)),
                    ModItems.KINETIC_GRENADE, 2, 120, 40));

    /**
     * Cámara de Reacción: cada receta resuena a una frecuencia y una intensidad (1-10). Titanio y uranio SOLO se
     * procesan aquí (no tienen receta de horno ni de Refinador).
     */
    public record React(Supplier<Item> in, Supplier<Item> out, int count, Supplier<Item> extra, float chance,
                        int frequency, int intensity, int ticks, int perTick) {}

    private static Supplier<Item> block(Supplier<? extends net.minecraft.world.level.block.Block> b) {
        return () -> b.get().asItem();
    }

    public static final List<React> REACT = List.of(
            new React(ModItems.RAW_TITANIUM, ModItems.TITANIUM_INGOT, 1, ModItems.TITANIUM_NUGGET, 0.35F, 4, 6, 160, 200),
            new React(block(com.bryanamaya.tecnocinetico.block.ModBlocks.TITANIUM_ORE), ModItems.TITANIUM_INGOT, 2, ModItems.TITANIUM_NUGGET, 0.35F, 4, 8, 200, 220),
            new React(block(com.bryanamaya.tecnocinetico.block.ModBlocks.DEEPSLATE_TITANIUM_ORE), ModItems.TITANIUM_INGOT, 2, ModItems.TITANIUM_NUGGET, 0.35F, 4, 8, 200, 220),
            new React(ModItems.TITANIUM_DUST, ModItems.TITANIUM_INGOT, 1, ModItems.TITANIUM_NUGGET, 0.10F, 4, 3, 100, 150),
            new React(ModItems.RAW_URANIUM, ModItems.URANIUM_INGOT, 1, ModItems.URANIUM_NUGGET, 0.25F, 7, 5, 200, 300),
            new React(block(com.bryanamaya.tecnocinetico.block.ModBlocks.URANIUM_ORE), ModItems.URANIUM_INGOT, 2, ModItems.URANIUM_NUGGET, 0.25F, 7, 7, 240, 320),
            new React(block(com.bryanamaya.tecnocinetico.block.ModBlocks.DEEPSLATE_URANIUM_ORE), ModItems.URANIUM_INGOT, 2, ModItems.URANIUM_NUGGET, 0.25F, 7, 7, 240, 320),
            new React(ModItems.DEPLETED_FUEL_ROD, ModItems.URANIUM_INGOT, 1, ModItems.TITANIUM_NUGGET, 0.5F, 8, 9, 300, 400));     // reprocesado

    public static React react(ItemStack in) {
        for (React r : REACT) {
            if (in.is(r.in().get())) {
                return r;
            }
        }
        return null;
    }

    /**
     * Laboratorio Químico: hasta 2 ingredientes (cualquier orden) + líquido del tanque de entrada -> producto y/o
     * líquido en el tanque de salida. `out`/`outFluid` pueden ser null.
     */
    public record Lab(List<Ingredient> items, Supplier<net.minecraft.world.level.material.Fluid> inFluid, int inAmount,
                      Supplier<Item> out, int outCount, Supplier<net.minecraft.world.level.material.Fluid> outFluid, int outAmount,
                      int ticks, int perTick) {}

    private static final Supplier<net.minecraft.world.level.material.Fluid> WATER = () -> net.minecraft.world.level.material.Fluids.WATER;
    private static final Supplier<net.minecraft.world.level.material.Fluid> ACID = com.bryanamaya.tecnocinetico.fluid.ModFluids.SULFURIC_ACID::get;
    private static final Supplier<net.minecraft.world.level.material.Fluid> COOLANT = com.bryanamaya.tecnocinetico.fluid.ModFluids.COOLANT::get;

    /** Lixiviación: la mena se disuelve en ácido y da 3 polvos (el Refinador da 2). */
    private static Lab leach(Supplier<Item> raw, Supplier<Item> dust, int acid, int ticks, int perTick) {
        return new Lab(List.of(of(raw, 1)), ACID, acid, dust, 3, null, 0, ticks, perTick);
    }

    public static final List<Lab> LAB = List.of(
            new Lab(List.of(of(v(Items.GUNPOWDER), 2)), WATER, 1000, null, 0, ACID, 1000, 100, 40),
            new Lab(List.of(of(v(Items.SNOWBALL), 4), of(v(Items.LAPIS_LAZULI), 1)), WATER, 1000, null, 0, COOLANT, 1000, 100, 40),
            leach(v(Items.RAW_IRON), ModItems.IRON_DUST, 250, 120, 40),
            leach(v(Items.RAW_COPPER), ModItems.COPPER_DUST, 250, 120, 40),
            leach(v(Items.RAW_GOLD), ModItems.GOLD_DUST, 250, 120, 40),
            leach(ModItems.RAW_LEAD, ModItems.LEAD_DUST, 250, 120, 40),
            leach(ModItems.RAW_TIN, ModItems.TIN_DUST, 250, 120, 40),
            leach(ModItems.RAW_CINETRITE, ModItems.CINETRITE_DUST, 250, 140, 50),
            leach(ModItems.RAW_INERCITA, ModItems.INERCITA_DUST, 500, 180, 60),
            leach(ModItems.RAW_PIROXITE, ModItems.PIROXITE_DUST, 500, 220, 80),
            leach(ModItems.RAW_NULLITE, ModItems.NULLITE_DUST, 500, 220, 80),
            new Lab(List.of(of(ModItems.SCRAP_METAL, 2)), ACID, 100, ModItems.IRON_DUST, 3, null, 0, 100, 30),
            // pólvora industrial: azufre + carbón con ácido sulfúrico
            new Lab(List.of(of(ModItems.SULFUR_DUST, 2), of(v(Items.COAL), 2)), ACID, 250, v(Items.GUNPOWDER), 12, null, 0, 120, 40),
            new Lab(List.of(of(ModItems.SULFUR_DUST, 2), of(v(Items.CHARCOAL), 2)), ACID, 250, v(Items.GUNPOWDER), 12, null, 0, 120, 40));

    public static boolean isLabFluid(net.minecraft.world.level.material.Fluid f) {
        return LAB.stream().anyMatch(r -> r.inFluid().get().isSame(f));
    }

    public static boolean isLabIngredient(ItemStack stack) {
        return LAB.stream().anyMatch(r -> r.items().stream().anyMatch(i -> stack.is(i.item().get())));
    }

    /** ¿Están todos los ingredientes en los huecos (y no sobra ningún tipo de objeto)? */
    private static boolean matches(List<Ingredient> needed, List<ItemStack> slots) {
        for (Ingredient i : needed) {
            int have = 0;
            for (ItemStack s : slots) {
                if (s.is(i.item().get())) {
                    have += s.getCount();
                }
            }
            if (have < i.count()) {
                return false;
            }
        }
        for (ItemStack s : slots) {
            if (!s.isEmpty() && needed.stream().noneMatch(i -> s.is(i.item().get()))) {
                return false;
            }
        }
        return true;
    }

    public static Lab lab(List<ItemStack> slots, net.minecraftforge.fluids.FluidStack tank) {
        for (Lab r : LAB) {
            if (!tank.isEmpty() && tank.getFluid().isSame(r.inFluid().get()) && tank.getAmount() >= r.inAmount() && matches(r.items(), slots)) {
                return r;
            }
        }
        return null;
    }

    /** Ensamblador Industrial: hasta 9 ingredientes (cualquier orden, un tipo por hueco) -> producto. */
    public record Assembly(List<Ingredient> in, Supplier<Item> out, int count, int ticks, int perTick) {}

    private static Supplier<Item> blockItem(Supplier<? extends net.minecraft.world.level.block.Block> b) {
        return () -> b.get().asItem();
    }

    public static final List<Assembly> ASSEMBLY = List.of(
            new Assembly(List.of(of(blockItem(com.bryanamaya.tecnocinetico.block.ModBlocks.LEAD_BLOCK), 16),
                    of(ModItems.TITANIUM_INGOT, 32), of(v(Items.GLASS), 24), of(ModItems.CORE_PROCESSOR, 2),
                    of(ModItems.PRECISION_MECHANISM, 8), of(ModItems.KINETIC_SERVO, 6), of(ModItems.INERCITA_PLATE, 16),
                    of(v(Items.COPPER_BLOCK), 8), of(v(Items.REDSTONE_BLOCK), 4)),
                    ModItems.FISSION_REACTOR_KIT, 1, 1200, 400),
            // piezas del Taladro de Combustión (se termina en el Ensamblador de Armas)
            new Assembly(List.of(of(ModItems.TITANIUM_INGOT, 6), of(v(Items.DIAMOND), 4), of(ModItems.INERCITA_PLATE, 2),
                    of(ModItems.KINETIC_SERVO, 1)),
                    ModItems.DRILL_HEAD, 1, 600, 250),
            new Assembly(List.of(of(ModItems.TITANIUM_INGOT, 4), of(ModItems.PRECISION_MECHANISM, 2), of(ModItems.KINETIC_SERVO, 2),
                    of(v(Items.COPPER_INGOT), 8)),
                    ModItems.COMBUSTION_ENGINE, 1, 600, 250));

    public static boolean isAssemblyIngredient(ItemStack stack) {
        return ASSEMBLY.stream().anyMatch(a -> a.in().stream().anyMatch(i -> stack.is(i.item().get())));
    }

    public static Assembly assembly(List<ItemStack> slots) {
        for (Assembly a : ASSEMBLY) {
            if (matches(a.in(), slots)) {
                return a;
            }
        }
        return null;
    }

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
