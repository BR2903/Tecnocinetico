package com.bryanamaya.tecnocinetico.compat.jei;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.ModBlocks;
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

/** JEI: Laboratorio Químico (ingredientes + líquido de entrada -> producto y/o líquido de salida). */
public class LabRecipeCategory implements IRecipeCategory<MachineRecipes.Lab> {

    public static final RecipeType<MachineRecipes.Lab> TYPE = RecipeType.create(Tecnocinetico.MOD_ID, "chemical_lab", MachineRecipes.Lab.class);
    private static final int TANK_CAP = 1000;

    private final IDrawable icon, slot, arrow;

    public LabRecipeCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemStack(new ItemStack(ModBlocks.CHEMICAL_LAB.get()));
        slot = gui.getSlotDrawable();
        arrow = gui.getRecipeArrow();
    }

    @Override
    public RecipeType<MachineRecipes.Lab> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return ModBlocks.CHEMICAL_LAB.get().getName();
    }

    @Override
    public int getWidth() {
        return 176;
    }

    @Override
    public int getHeight() {
        return 56;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, MachineRecipes.Lab r, IFocusGroup focuses) {
        b.addSlot(RecipeIngredientRole.INPUT, 5, 5).addFluidStack(r.inFluid().get(), r.inAmount())
                .setFluidRenderer(TANK_CAP, false, 16, 34);
        for (int i = 0; i < r.items().size(); i++) {
            var ing = r.items().get(i);
            b.addSlot(RecipeIngredientRole.INPUT, 29 + i * 20, 14).addItemStack(new ItemStack(ing.item().get(), ing.count()));
        }
        if (r.out() != null) {
            b.addSlot(RecipeIngredientRole.OUTPUT, 107, 14).addItemStack(new ItemStack(r.out().get(), r.outCount()));
        }
        if (r.outFluid() != null) {
            b.addSlot(RecipeIngredientRole.OUTPUT, 133, 5).addFluidStack(r.outFluid().get(), r.outAmount())
                    .setFluidRenderer(TANK_CAP, false, 16, 34);
        }
    }

    @Override
    public void draw(MachineRecipes.Lab r, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        g.fill(4, 4, 22, 40, 0xFF373737);                                     // marco del tanque de entrada
        for (int i = 0; i < r.items().size(); i++) {
            slot.draw(g, 28 + i * 20, 13);
        }
        arrow.draw(g, 74, 13);
        if (r.out() != null) {
            slot.draw(g, 106, 13);
        }
        if (r.outFluid() != null) {
            g.fill(132, 4, 150, 40, 0xFF373737);
        }
        var font = Minecraft.getInstance().font;
        g.drawString(font, Component.translatable("jei.tecnocinetico.machine_time", String.format("%.1f", r.ticks() / 20.0F), r.perTick(),
                r.ticks() * r.perTick()), 4, 45, 0x404040, false);
    }
}
