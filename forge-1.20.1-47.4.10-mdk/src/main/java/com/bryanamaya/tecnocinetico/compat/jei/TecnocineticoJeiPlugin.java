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

import com.bryanamaya.tecnocinetico.block.entity.AutoKineticPressBlockEntity;
import com.bryanamaya.tecnocinetico.block.entity.WeaponAssemblerBlockEntity;
import com.bryanamaya.tecnocinetico.machine.MachineKind;
import com.bryanamaya.tecnocinetico.menu.AutoKineticPressMenu;
import com.bryanamaya.tecnocinetico.menu.MachineMenu;
import com.bryanamaya.tecnocinetico.menu.ModMenus;
import com.bryanamaya.tecnocinetico.menu.WeaponAssemblerMenu;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        registration.addRecipeCategories(new LabRecipeCategory(gui));
        registration.addRecipeCategories(new AssemblyRecipeCategory(gui));
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
        registration.addRecipes(LabRecipeCategory.TYPE, com.bryanamaya.tecnocinetico.machine.MachineRecipes.LAB);
        registration.addRecipes(AssemblyRecipeCategory.TYPE, com.bryanamaya.tecnocinetico.machine.MachineRecipes.ASSEMBLY);
        // titanio y uranio: solo en la Cámara de Reacción; el uranio es radiactivo
        registration.addIngredientInfo(List.of(new ItemStack(ModItems.RAW_TITANIUM.get()), new ItemStack(ModItems.TITANIUM_DUST.get()),
                        new ItemStack(ModBlocks.TITANIUM_ORE.get()), new ItemStack(ModBlocks.DEEPSLATE_TITANIUM_ORE.get())),
                VanillaTypes.ITEM_STACK, Component.translatable("jei.tecnocinetico.titanium_info"));
        registration.addIngredientInfo(List.of(new ItemStack(ModItems.RAW_URANIUM.get()), new ItemStack(ModItems.URANIUM_INGOT.get()),
                        new ItemStack(ModBlocks.URANIUM_ORE.get()), new ItemStack(ModBlocks.DEEPSLATE_URANIUM_ORE.get()),
                        new ItemStack(ModBlocks.URANIUM_BLOCK.get())),
                VanillaTypes.ITEM_STACK, Component.translatable("jei.tecnocinetico.uranium_info"));
        registration.addIngredientInfo(List.of(new ItemStack(ModItems.FISSION_REACTOR_KIT.get()), new ItemStack(ModItems.URANIUM_FUEL_ROD.get()),
                        new ItemStack(ModItems.DEPLETED_FUEL_ROD.get())),
                VanillaTypes.ITEM_STACK, Component.translatable("jei.tecnocinetico.reactor_info"));
        registration.addIngredientInfo(List.of(new ItemStack(com.bryanamaya.tecnocinetico.block.ModBlocks.OIL_PUMP.get()),
                new ItemStack(com.bryanamaya.tecnocinetico.fluid.ModFluids.CRUDE_OIL.bucket.get())), VanillaTypes.ITEM_STACK, Component.translatable("jei.tecnocinetico.oil_pump"));
        registration.addIngredientInfo(List.of(new ItemStack(com.bryanamaya.tecnocinetico.block.ModBlocks.REFINERY.get()),
                new ItemStack(com.bryanamaya.tecnocinetico.fluid.ModFluids.GASOLINE.bucket.get()), new ItemStack(ModItems.TAR.get()),
                new ItemStack(ModItems.FUEL_CANISTER.get())), VanillaTypes.ITEM_STACK, Component.translatable("jei.tecnocinetico.refinery"));
        registration.addIngredientInfo(List.of(new ItemStack(com.bryanamaya.tecnocinetico.fluid.ModFluids.KINETIC_FUEL.bucket.get()),
                new ItemStack(ModItems.KINETIC_FUEL_CELL.get())), VanillaTypes.ITEM_STACK, Component.translatable("jei.tecnocinetico.kinetic_fuel"));
        registration.addIngredientInfo(new ItemStack(com.bryanamaya.tecnocinetico.fluid.ModFluids.UNIVERSAL_CELL.get()),
                VanillaTypes.ITEM_STACK, Component.translatable("jei.tecnocinetico.cell_info"));

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

    /**
     * Botón "+" de JEI: pasa los ingredientes del inventario a los huecos de entrada de la máquina abierta (en el
     * mismo orden en que JEI los dibuja). Los líquidos del laboratorio no se pasan: solo los objetos.
     */
    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        var h = registration.getTransferHelper();
        machine(registration, MachineKind.REFINER, MachineRecipeCategory.REFINER);
        machine(registration, MachineKind.CHARGER, MachineRecipeCategory.CHARGER);
        machine(registration, MachineKind.AMMO_FABRICATOR, MachineRecipeCategory.FABRICATOR);
        machine(registration, MachineKind.REACTION_CHAMBER, MachineRecipeCategory.REACTION);
        machine(registration, MachineKind.CHEMICAL_LAB, LabRecipeCategory.TYPE);
        machine(registration, MachineKind.INDUSTRIAL_ASSEMBLER, AssemblyRecipeCategory.TYPE);
        itemsOnly(registration, h.createBasicRecipeTransferInfo(AutoKineticPressMenu.class, ModMenus.AUTO_KINETIC_PRESS.get(),
                PressRecipeCategory.TYPE, 0, AutoKineticPressBlockEntity.INPUT_SLOTS, AutoKineticPressBlockEntity.INPUT_SLOTS + 1, 36));
        itemsOnly(registration, h.createBasicRecipeTransferInfo(WeaponAssemblerMenu.class, ModMenus.WEAPON_ASSEMBLER.get(),
                AssemblerRecipeCategory.TYPE, 0, WeaponAssemblerBlockEntity.INPUT_SLOTS, WeaponAssemblerBlockEntity.INPUT_SLOTS + 1, 36));
    }

    private static <R> void machine(IRecipeTransferRegistration registration, MachineKind kind, RecipeType<R> type) {
        itemsOnly(registration, registration.getTransferHelper().createBasicRecipeTransferInfo(MachineMenu.class,
                ModMenus.machine(kind).get(), type, 0, kind.inputs.length, kind.slots(), 36));
    }

    /** El manejador básico de JEI, pero ignorando las entradas que no son objetos (tanques de líquido). */
    private static <C extends AbstractContainerMenu, R> void itemsOnly(IRecipeTransferRegistration registration, IRecipeTransferInfo<C, R> info) {
        IRecipeTransferHandlerHelper h = registration.getTransferHelper();
        IRecipeTransferHandler<C, R> base = h.createUnregisteredRecipeTransferHandler(info);
        registration.addRecipeTransferHandler(new IRecipeTransferHandler<C, R>() {
            @Override
            public Class<? extends C> getContainerClass() {
                return base.getContainerClass();
            }

            @Override
            public Optional<MenuType<C>> getMenuType() {
                return base.getMenuType();
            }

            @Override
            public RecipeType<R> getRecipeType() {
                return base.getRecipeType();
            }

            @Override
            public @Nullable IRecipeTransferError transferRecipe(C container, R recipe, IRecipeSlotsView slots, Player player,
                                                                 boolean maxTransfer, boolean doTransfer) {
                List<IRecipeSlotView> views = slots.getSlotViews().stream()
                        .filter(v -> v.getRole() != RecipeIngredientRole.INPUT || v.getItemStacks().findAny().isPresent())
                        .toList();
                return base.transferRecipe(container, recipe, h.createRecipeSlotsView(views), player, maxTransfer, doTransfer);
            }
        }, info.getRecipeType());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.KINETIC_ASSEMBLY_PRESS.get()), PressRecipeCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.KINETIC_HAMMER.get()), PressRecipeCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.AUTO_KINETIC_PRESS.get()), PressRecipeCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.WEAPON_ASSEMBLER.get()), AssemblerRecipeCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CHEMICAL_LAB.get()), LabRecipeCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.INDUSTRIAL_ASSEMBLER_KIT.get()), AssemblyRecipeCategory.TYPE);
        for (var t : List.of(MachineRecipeCategory.REFINER, MachineRecipeCategory.CHARGER, MachineRecipeCategory.FABRICATOR, MachineRecipeCategory.REACTION)) {
            registration.addRecipeCatalyst(new ItemStack(MachineRecipeCategory.block(t)), t);
        }
    }
}
