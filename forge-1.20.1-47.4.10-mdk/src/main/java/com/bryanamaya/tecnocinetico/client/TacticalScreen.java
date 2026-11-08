package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.gear.TacticalGear;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.menu.TacticalMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Pantalla del Equipo Táctico: gadgets a la izquierda, el personaje en el centro, munición y comida a la
 * derecha, inventario abajo. Pestañas arriba para volver al inventario normal (ver {@link GearTabs}).
 */
public class TacticalScreen extends AbstractContainerScreen<TacticalMenu> {

    static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/gui/tactical_gear.png");

    public TacticalScreen(TacticalMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 218;
        inventoryLabelY = TacticalMenu.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        GearTabs.add(this, this::addRenderableWidget, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        // personaje en la ventana central
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, leftPos + 64, topPos + 108, 40,
                leftPos + 64 - mouseX, topPos + 45 - mouseY, minecraft.player);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 30, 4, 0x3A3A3A, false);
        g.drawString(font, Component.translatable("gui.tecnocinetico.gear.ammo"), 116, 5, 0x3A3A3A, false);
        g.drawString(font, Component.translatable("gui.tecnocinetico.gear.food"), 116, 55, 0x3A3A3A, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x3A3A3A, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        // ayuda al pasar por una ranura vacía del equipo
        var slot = getSlotUnderMouse();
        if (slot != null && slot.index < TacticalGear.SIZE && !slot.hasItem() && menu.getCarried().isEmpty()) {
            String key = slot.index < TacticalGear.FOOD_START ? "gui.tecnocinetico.gear.hint.ammo"
                    : slot.index < TacticalGear.FACE ? "gui.tecnocinetico.gear.hint.food"
                    : "tooltip.tecnocinetico.gadget_slot." + TacticalGear.slotName(slot.index);
            g.renderTooltip(font, Component.translatable(key), mouseX, mouseY);
        }
    }

    /** Icono de la pestaña del equipo. */
    static ItemStack tabIcon() {
        return new ItemStack(ModItems.NIGHT_VISION_VISOR.get());
    }

    static ItemStack inventoryIcon() {
        return new ItemStack(Items.CHEST);
    }
}
