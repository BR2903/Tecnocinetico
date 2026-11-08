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
        registration.addRecipeCategories(new AssemblerRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
        // máquinas eléctricas
        var gui = registration.getJeiHelpers().getGuiHelper();
        for (var t : List.of(MachineRecipeCategory.REFINER, MachineRecipeCategory.CHARGER, MachineRecipeCategory.FABRICATOR, MachineRecipeCategory.REACTION)) {
            registration.addRecipeCategories(new MachineRecipeCategory(gui, t, MachineRecipeCategory.block(t)));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(PressRecipeCategory.TYPE, PressRecipes.all());
        registration.addRecipes(AssemblerRecipeCategory.TYPE, com.bryanamaya.tecnocinetico.block.entity.AssemblerRecipes.all());
        registration.addRecipes(MachineRecipeCategory.REFINER, MachineRecipeCategory.refiner());
        registration.addRecipes(MachineRecipeCategory.CHARGER, MachineRecipeCategory.charger());
        registration.addRecipes(MachineRecipeCategory.FABRICATOR, MachineRecipeCategory.fabricator());
        registration.addRecipes(MachineRecipeCategory.REACTION, MachineRecipeCategory.reaction());
        // titanio y uranio: solo en la Cámara de Reacción; el uranio es radiactivo
        registration.addIngredientInfo(List.of(new ItemStack(ModItems.RAW_TITANIUM.get()), new ItemStack(ModItems.TITANIUM_DUST.get()),
                        new ItemStack(ModBlocks.TITANIUM_ORE.get()), new ItemStack(ModBlocks.DEEPSLATE_TITANIUM_ORE.get())),
                VanillaTypes.ITEM_STACK, Component.translatable("jei.tecnocinetico.titanium_info"));
        registration.addIngredientInfo(List.of(new ItemStack(ModItems.RAW_URANIUM.get()), new ItemStack(ModItems.URANIUM_INGOT.get()),
                        new ItemStack(ModBlocks.URANIUM_ORE.get()), new ItemStack(ModBlocks.DEEPSLATE_URANIUM_ORE.get()),
                        new ItemStack(ModBlocks.URANIUM_BLOCK.get())),
                VanillaTypes.ITEM_STACK, Component.translatable("jei.tecnocinetico.uranium_info"));

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
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.WEAPON_ASSEMBLER.get()), AssemblerRecipeCategory.TYPE);
        for (var t : List.of(MachineRecipeCategory.REFINER, MachineRecipeCategory.CHARGER, MachineRecipeCategory.FABRICATOR, MachineRecipeCategory.REACTION)) {
            registration.addRecipeCatalyst(new ItemStack(MachineRecipeCategory.block(t)), t);
        }
    }
}
