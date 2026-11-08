package com.bryanamaya.tecnocinetico.compat.jei;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.gear.GadgetItem;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.machine.ChargerBlockEntity;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Categorías de JEI de las máquinas eléctricas: hasta 3 entradas, flecha, salida principal y secundaria, y dos
 * líneas de texto (tiempo, energía, probabilidad o ajuste de la Cámara de Reacción).
 */
public class MachineRecipeCategory implements IRecipeCategory<MachineRecipeCategory.Entry> {

    /** Una receta tal como se dibuja: entradas, salidas (la 2.ª puede ser probabilística) y dos líneas de texto. */
    public record Entry(List<ItemStack> inputs, ItemStack output, ItemStack extra, float chance, Component line1, Component line2) {}

    public static final RecipeType<Entry> REFINER = type("electric_refiner");
    public static final RecipeType<Entry> CHARGER = type("cell_charger");
    public static final RecipeType<Entry> FABRICATOR = type("ammo_fabricator");
    public static final RecipeType<Entry> REACTION = type("reaction_chamber");

    private static RecipeType<Entry> type(String id) {
        return RecipeType.create(Tecnocinetico.MOD_ID, id, Entry.class);
    }

    private static final int WIDTH = 176, HEIGHT = 52, OUT_X = 96, EXTRA_X = 124;

    private final RecipeType<Entry> type;
    private final Component title;
    private final IDrawable icon, slot, arrow;

    public MachineRecipeCategory(IGuiHelper gui, RecipeType<Entry> type, Block block) {
        this.type = type;
        this.title = block.getName();
        this.icon = gui.createDrawableItemStack(new ItemStack(block));
        this.slot = gui.getSlotDrawable();
        this.arrow = gui.getRecipeArrow();
    }

    @Override
    public RecipeType<Entry> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
        return title;
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
    public void setRecipe(IRecipeLayoutBuilder builder, Entry e, IFocusGroup focuses) {
        for (int i = 0; i < e.inputs().size(); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, 5 + i * 20, 9).addItemStack(e.inputs().get(i));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X + 1, 9).addItemStack(e.output());
        if (!e.extra().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, EXTRA_X + 1, 9).addItemStack(e.extra())
                    .addTooltipCallback((view, tooltip) -> tooltip.add(chance(e.chance()).copy().withStyle(net.minecraft.ChatFormatting.GOLD)));
        }
    }

    @Override
    public void draw(Entry e, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        for (int i = 0; i < e.inputs().size(); i++) {
            slot.draw(g, 4 + i * 20, 8);
        }
        arrow.draw(g, 68, 8);
        slot.draw(g, OUT_X, 8);
        if (!e.extra().isEmpty()) {
            slot.draw(g, EXTRA_X, 8);
        }
        var font = Minecraft.getInstance().font;
        g.drawString(font, e.line1(), 4, 31, 0x404040, false);
        g.drawString(font, e.line2(), 4, 42, 0x808080, false);
    }

    // ---------------------------------------------------------------- recetas
    private static Component time(int ticks, int perTick) {
        return Component.translatable("jei.tecnocinetico.machine_time", String.format("%.1f", ticks / 20.0F), perTick, ticks * perTick);
    }

    private static Component chance(float c) {
        return Component.translatable("jei.tecnocinetico.machine_chance", Math.round(c * 100));
    }

    public static List<Entry> refiner() {
        List<Entry> out = new ArrayList<>();
        for (MachineRecipes.Refine r : MachineRecipes.REFINE) {
            out.add(new Entry(List.of(new ItemStack(r.in().get())), new ItemStack(r.out().get(), r.count()), new ItemStack(r.extra().get()), r.chance(),
                    time(r.ticks(), r.perTick()), chance(r.chance())));
        }
        return out;
    }

    public static List<Entry> fabricator() {
        List<Entry> out = new ArrayList<>();
        for (MachineRecipes.Fabricate f : MachineRecipes.FABRICATE) {
            List<ItemStack> in = f.in().stream().map(i -> new ItemStack(i.item().get(), i.count())).toList();
            out.add(new Entry(in, new ItemStack(f.out().get(), f.count()), ItemStack.EMPTY, 0.0F, time(f.ticks(), f.perTick()),
                    Component.translatable("jei.tecnocinetico.fabricator_any_order")));
        }
        return out;
    }

    public static List<Entry> reaction() {
        List<Entry> out = new ArrayList<>();
        for (MachineRecipes.React r : MachineRecipes.REACT) {
            out.add(new Entry(List.of(new ItemStack(r.in().get())), new ItemStack(r.out().get(), r.count()), new ItemStack(r.extra().get()), r.chance(),
                    Component.translatable("jei.tecnocinetico.reaction_tuning", r.frequency(), r.intensity()),
                    time(r.ticks(), r.perTick())));
        }
        return out;
    }

    public static List<Entry> charger() {
        List<Entry> out = new ArrayList<>();
        out.add(new Entry(List.of(new ItemStack(ModItems.EMPTY_PULSE_CELL.get())), new ItemStack(ModItems.PULSE_CELL.get()), ItemStack.EMPTY, 0.0F,
                time(ChargerBlockEntity.CELL_TICKS, ChargerBlockEntity.CELL_PER_TICK), Component.empty()));
        for (Item item : ForgeRegistries.ITEMS.getValues()) {                     // gadgets del Equipo Táctico
            if (item instanceof GadgetItem) {
                ItemStack empty = new ItemStack(item);
                empty.setDamageValue(empty.getMaxDamage() - 1);
                out.add(new Entry(List.of(empty), new ItemStack(item), ItemStack.EMPTY, 0.0F,
                        Component.translatable("jei.tecnocinetico.charger_gadget", ChargerBlockEntity.GADGET_FE_PER_POINT),
                        Component.translatable("jei.tecnocinetico.charger_any")));
            }
        }
        return out;
    }

    /** Bloques de cada categoría (catalizadores). */
    public static Block block(RecipeType<Entry> t) {
        if (t == REFINER) {
            return ModBlocks.ELECTRIC_REFINER.get();
        }
        if (t == CHARGER) {
            return ModBlocks.CELL_CHARGER.get();
        }
        if (t == FABRICATOR) {
            return ModBlocks.AMMO_FABRICATOR.get();
        }
        return ModBlocks.REACTION_CHAMBER.get();
    }
}
