package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.entity.PressRecipes;
import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Recetas de la revisión de progresión: lo que faltaba (resistencia, reciclaje del mundo) y los ajustes. */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class RecipeBalanceGameTests {

    private static void expect(GameTestHelper h, String id, Item result, int count) {
        var level = h.getLevel();
        var recipe = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, id));
        h.assertTrue(recipe.isPresent(), "Falta la receta " + id);
        var out = recipe.get().getResultItem(level.registryAccess());
        h.assertTrue(out.is(result) && out.getCount() == count, "La receta " + id + " da " + out);
    }

    @GameTest(template = "empty")
    public static void progressionRecipes(GameTestHelper h) {
        expect(h, "resistor_330_ohm", ModItems.RESISTOR_330_OHM.get(), 4);                 // el Cargador de Celdas ya se puede hacer
        expect(h, "sticks_from_utility_pole", Items.STICK, 3);
        expect(h, "sticks_from_utility_crossarm", Items.STICK, 2);
        expect(h, "smelting_iron_nugget_from_scrap_pile", Items.IRON_NUGGET, 1);
        expect(h, "blasting_iron_nugget_from_street_lamp_pole", Items.IRON_NUGGET, 1);
        expect(h, "copper_from_power_cable", Items.COPPER_INGOT, 1);
        expect(h, "gravel_from_rubble", Items.GRAVEL, 1);
        expect(h, "rifle_round", ModItems.RIFLE_ROUND.get(), 1);
        var rifle = h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "rifle_round")).orElseThrow();
        h.assertTrue(rifle.getIngredients().stream().noneMatch(i -> i.test(new net.minecraft.world.item.ItemStack(ModItems.CINETRITE_SHARD.get()))),
                "La bala de rifle aún pide cinetrita");
        boolean coupler = PressRecipes.all().stream().anyMatch(r -> r.output().is(ModItems.CRUDE_KINETIC_COUPLER.get()) && r.output().getCount() == 2);
        h.assertTrue(coupler, "El Acoplador no sale de 2 en 2");
        h.succeed();
    }
}
