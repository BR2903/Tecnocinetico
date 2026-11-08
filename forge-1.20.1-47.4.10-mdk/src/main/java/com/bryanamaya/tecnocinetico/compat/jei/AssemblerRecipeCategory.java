package com.bryanamaya.tecnocinetico.compat.jei;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.block.entity.AssemblerRecipes;
import com.bryanamaya.tecnocinetico.block.entity.WeaponAssemblerBlockEntity;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Categoría de JEI: Ensamblador de Armas (6 ingredientes con cantidad, electricidad). */
public class AssemblerRecipeCategory implements IRecipeCategory<AssemblerRecipes.Recipe> {

    public static final RecipeType<AssemblerRecipes.Recipe> TYPE =
            RecipeType.create(Tecnocinetico.MOD_ID, "weapon_assembler", AssemblerRecipes.Recipe.class);

    private final IDrawable icon, slot, arrow;

    public AssemblerRecipeCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableItemStack(new ItemStack(ModBlocks.WEAPON_ASSEMBLER.get()));
        this.slot = gui.getSlotDrawable();
        this.arrow = gui.getRecipeArrow();
    }

    @Override
    public RecipeType<AssemblerRecipes.Recipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.tecnocinetico.weapon_assembler");
    }

    @Override
    public int getWidth() {
        return 150;
    }

    @Override
    public int getHeight() {
        return 60;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AssemblerRecipes.Recipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < recipe.inputs().size(); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, 5 + (i % 3) * 18, 5 + (i / 3) * 18).addItemStack(recipe.inputs().get(i));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 113, 14).addItemStack(recipe.output());
    }

    @Override
    public void draw(AssemblerRecipes.Recipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        for (int i = 0; i < 6; i++) {
            slot.draw(graphics, 4 + (i % 3) * 18, 4 + (i / 3) * 18);
        }
        arrow.draw(graphics, 70, 14);
        slot.draw(graphics, 112, 13);
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("jei.tecnocinetico.assembler_energy",
                recipe.ticks() * WeaponAssemblerBlockEntity.ENERGY_PER_TICK, recipe.ticks() / 20), 4, 46, 0x808080, false);
    }
}
