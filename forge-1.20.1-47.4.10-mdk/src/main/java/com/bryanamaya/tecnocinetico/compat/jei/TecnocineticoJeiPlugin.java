package com.bryanamaya.tecnocinetico.compat.jei;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.block.entity.PressRecipes;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.item.custom.WeaponModule;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Integración con JEI (visor de recetas). Solo se carga si JEI está instalado.
 * - Categoría propia para la Prensa de Ensamblaje Cinético.
 * - Página de información sobre el Banco de Armas y los módulos.
 */
@JeiPlugin
public class TecnocineticoJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new PressRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(PressRecipeCategory.TYPE, PressRecipes.all());

        // Información: cómo se montan los módulos
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(new ItemStack(ModBlocks.WEAPON_BENCH.get()));
        for (WeaponModule module : WeaponModule.values()) {
            stacks.add(new ItemStack(ModItems.moduleItem(module)));
        }
        registration.addIngredientInfo(stacks, VanillaTypes.ITEM_STACK,
                Component.translatable("jei.tecnocinetico.bench_info"));
    }

    /** La Fuente de Energía Creativa es solo para pruebas: no se muestra en JEI. */
    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK,
                List.of(new ItemStack(ModBlocks.CREATIVE_ENERGY_SOURCE.get())));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.KINETIC_ASSEMBLY_PRESS.get()), PressRecipeCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.KINETIC_HAMMER.get()), PressRecipeCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.AUTO_KINETIC_PRESS.get()), PressRecipeCategory.TYPE);
    }
}
