package com.bryanamaya.tecnocinetico.compat.jei;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.block.entity.PressRecipes;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Categoría de JEI: Prensa de Ensamblaje Cinético (3 golpes de martillo, o automática). */
public class PressRecipeCategory implements IRecipeCategory<PressRecipes.Recipe> {

    public static final RecipeType<PressRecipes.Recipe> TYPE =
            RecipeType.create(Tecnocinetico.MOD_ID, "assembly_press", PressRecipes.Recipe.class);

    private static final int WIDTH = 150;
    private static final int HEIGHT = 52;

    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrow;

    public PressRecipeCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableItemStack(new ItemStack(ModBlocks.KINETIC_ASSEMBLY_PRESS.get()));
        this.slot = gui.getSlotDrawable();
        this.arrow = gui.getRecipeArrow();
    }

    @Override
    public RecipeType<PressRecipes.Recipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.tecnocinetico.press");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, PressRecipes.Recipe recipe, IFocusGroup focuses) {
        int x = 4;
        for (Item input : recipe.inputs()) {
            builder.addSlot(RecipeIngredientRole.INPUT, x + 1, 9).addItemStack(new ItemStack(input));
            x += 20;
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 113, 9).addItemStack(recipe.output());
    }

    @Override
    public void draw(PressRecipes.Recipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        for (int i = 0; i < 3; i++) {
            slot.draw(graphics, 4 + i * 20, 8);
        }
        arrow.draw(graphics, 70, 8);
        slot.draw(graphics, 112, 8);
        var font = Minecraft.getInstance().font;
        if (recipe.powered()) {                                                // solo la automática, con energía
            graphics.drawString(font, Component.translatable("jei.tecnocinetico.press_powered"), 4, 31, 0xB03020, false);
            graphics.drawString(font, Component.translatable("jei.tecnocinetico.press_powered2"), 4, 42, 0x808080, false);
        } else {
            graphics.drawString(font, Component.translatable("jei.tecnocinetico.press_strikes"), 4, 31, 0x404040, false);
            graphics.drawString(font, Component.translatable("jei.tecnocinetico.press_auto"), 4, 42, 0x808080, false);
        }
    }
}
