package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.entity.AssemblerRecipes;
import com.bryanamaya.tecnocinetico.block.entity.PressRecipes;
import com.bryanamaya.tecnocinetico.machine.MachineRecipes;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.function.Supplier;

/**
 * Volcado de TODAS las recetas que tocan el mod (mesa, horno, cortapiedras… y las de máquinas, prensas y
 * ensambladores, que están en Java) a {@code run/recipe_audit.json}, para revisar el equilibrio con
 * {@code tools/recipe_audit.py}. Solo con la variable de entorno {@code TECNO_AUDIT=1}.
 */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class RecipeAuditGameTests {

    private static String id(Item i) {
        return BuiltInRegistries.ITEM.getKey(i).toString();
    }

    private static JsonArray options(Ingredient ing) {
        JsonArray a = new JsonArray();
        for (ItemStack s : ing.getItems()) {
            a.add(id(s.getItem()));
        }
        return a;
    }

    private static JsonObject stack(String item, int count) {
        JsonObject o = new JsonObject();
        o.addProperty("item", item);
        o.addProperty("count", count);
        return o;
    }

    private static JsonArray ingredients(List<MachineRecipes.Ingredient> list) {
        JsonArray a = new JsonArray();
        for (var i : list) {
            a.add(stack(id(i.item().get()), i.count()));
        }
        return a;
    }

    private static JsonObject machine(String type, JsonArray in, Supplier<Item> out, int count, int ticks, int perTick) {
        JsonObject o = new JsonObject();
        o.addProperty("type", type);
        o.add("in", in);
        o.add("out", out == null || out.get() == null ? null : stack(id(out.get()), count));
        o.addProperty("ticks", ticks);
        o.addProperty("fe_per_tick", perTick);
        return o;
    }

    @GameTest(template = "empty", batch = "auditoria_recetas")
    public static void dumpRecipes(GameTestHelper h) {
        if (!"1".equals(System.getenv("TECNO_AUDIT"))) {
            h.succeed();
            return;
        }
        var level = h.getLevel();
        JsonObject root = new JsonObject();
        JsonArray items = new JsonArray();
        for (Item i : BuiltInRegistries.ITEM) {
            if (BuiltInRegistries.ITEM.getKey(i).getNamespace().equals(Tecnocinetico.MOD_ID)) {
                items.add(id(i));
            }
        }
        root.add("items", items);
        JsonArray recipes = new JsonArray();
        for (Recipe<?> r : level.getRecipeManager().getRecipes()) {
            ItemStack out = r.getResultItem(level.registryAccess());
            boolean mod = r.getId().getNamespace().equals(Tecnocinetico.MOD_ID) || !out.isEmpty()
                    && BuiltInRegistries.ITEM.getKey(out.getItem()).getNamespace().equals(Tecnocinetico.MOD_ID);
            JsonArray ing = new JsonArray();
            for (Ingredient i : r.getIngredients()) {
                if (!i.isEmpty()) {
                    JsonArray opts = options(i);
                    ing.add(opts);
                    for (var e : opts) {
                        mod |= e.getAsString().startsWith(Tecnocinetico.MOD_ID + ":");
                    }
                }
            }
            if (!mod) {
                continue;
            }
            JsonObject o = new JsonObject();
            o.addProperty("id", r.getId().toString());
            o.addProperty("type", BuiltInRegistries.RECIPE_TYPE.getKey(r.getType()).toString());
            o.add("in", ing);
            o.add("out", out.isEmpty() ? null : stack(id(out.getItem()), out.getCount()));
            recipes.add(o);
        }
        root.add("recipes", recipes);
        JsonArray machines = new JsonArray();
        for (var r : MachineRecipes.REFINE) {
            JsonObject o = machine("refiner", ingredients(List.of(new MachineRecipes.Ingredient(r.in(), 1))), r.out(), r.count(), r.ticks(), r.perTick());
            if (r.extra() != null) {
                o.addProperty("extra", id(r.extra().get()) + " " + r.chance());
            }
            machines.add(o);
        }
        for (var r : MachineRecipes.FABRICATE) {
            machines.add(machine("fabricator", ingredients(r.in()), r.out(), r.count(), r.ticks(), r.perTick()));
        }
        for (var r : MachineRecipes.REACT) {
            JsonObject o = machine("reaction", ingredients(List.of(new MachineRecipes.Ingredient(r.in(), 1))), r.out(), r.count(), r.ticks(), r.perTick());
            if (r.extra() != null) {
                o.addProperty("extra", id(r.extra().get()) + " " + r.chance());
            }
            machines.add(o);
        }
        for (var r : MachineRecipes.LAB) {
            JsonObject o = machine("lab", ingredients(r.items()), r.out(), r.outCount(), r.ticks(), r.perTick());
            o.addProperty("fluid_in", BuiltInRegistries.FLUID.getKey(r.inFluid().get()) + " " + r.inAmount());
            if (r.outFluid() != null) {
                o.addProperty("fluid_out", BuiltInRegistries.FLUID.getKey(r.outFluid().get()) + " " + r.outAmount());
            }
            machines.add(o);
        }
        for (var r : MachineRecipes.ASSEMBLY) {
            JsonObject o = machine("industrial_assembler", ingredients(r.in()), r.out(), r.count(), r.ticks(), r.perTick());
            o.addProperty("upgrade", r.upgrade() != null);
            machines.add(o);
        }
        for (var r : PressRecipes.all()) {
            JsonArray in = new JsonArray();
            r.inputs().forEach(i -> in.add(stack(id(i), 1)));
            JsonObject o = new JsonObject();
            o.addProperty("type", r.powered() ? "auto_press" : "press");
            o.add("in", in);
            o.add("out", stack(id(r.output().getItem()), r.output().getCount()));
            machines.add(o);
        }
        for (var r : AssemblerRecipes.all()) {
            JsonArray in = new JsonArray();
            r.inputs().forEach(i -> in.add(stack(id(i.getItem()), i.getCount())));
            JsonObject o = new JsonObject();
            o.addProperty("type", "weapon_assembler");
            o.add("in", in);
            o.add("out", stack(id(r.output().getItem()), r.output().getCount()));
            o.addProperty("ticks", r.ticks());
            machines.add(o);
        }
        root.add("machines", machines);
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("recipe_audit.json"), new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        Tecnocinetico.LOGGER.info("[Auditoría] {} objetos, {} recetas, {} de máquinas", items.size(), recipes.size(), machines.size());
        h.succeed();
    }
}
