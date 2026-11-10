package com.bryanamaya.tecnocinetico.compat.jei;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.machine.MachineRecipes;
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

/** JEI: Ensamblador Industrial (rejilla de 3x3 ingredientes con su cantidad -> producto). */
public class AssemblyRecipeCategory implements IRecipeCategory<MachineRecipes.Assembly> {

    public static final RecipeType<MachineRecipes.Assembly> TYPE = RecipeType.create(Tecnocinetico.MOD_ID, "industrial_assembler", MachineRecipes.Assembly.class);
    private final IDrawable icon, slot, arrow;

    public AssemblyRecipeCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemStack(new ItemStack(ModItems.INDUSTRIAL_ASSEMBLER_KIT.get()));
        slot = gui.getSlotDrawable();
        arrow = gui.getRecipeArrow();
    }

    @Override
    public RecipeType<MachineRecipes.Assembly> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("item.tecnocinetico.industrial_assembler");
    }

    @Override
    public int getWidth() {
        return 150;
    }

    @Override
    public int getHeight() {
        return 66;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, MachineRecipes.Assembly a, IFocusGroup focuses) {
        for (int i = 0; i < a.in().size(); i++) {
            var ing = a.in().get(i);
            b.addSlot(RecipeIngredientRole.INPUT, 1 + (i % 3) * 18, 1 + (i / 3) * 18).addItemStack(new ItemStack(ing.item().get(), ing.count()));
        }
        b.addSlot(RecipeIngredientRole.OUTPUT, 95, 19).addItemStack(new ItemStack(a.out().get(), a.count()));
    }

    @Override
    public void draw(MachineRecipes.Assembly a, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        for (int i = 0; i < 9; i++) {
            slot.draw(g, (i % 3) * 18, (i / 3) * 18);
        }
        arrow.draw(g, 62, 18);
        slot.draw(g, 94, 18);
        g.drawString(Minecraft.getInstance().font, Component.translatable("jei.tecnocinetico.machine_time",
                String.format("%.1f", a.ticks() / 20.0F), a.perTick(), a.ticks() * a.perTick()), 0, 57, 0x404040, false);
    }
}
