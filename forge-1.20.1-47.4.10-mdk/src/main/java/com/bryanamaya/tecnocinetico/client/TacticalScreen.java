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
        int[][] pos = {{TacticalGear.FACE, 18}, {TacticalGear.BACK, 44}, {TacticalGear.FEET, 70}, {TacticalGear.BELT, 96}};
        for (int[] s : pos) {
            int slot = s[0];
            addRenderableWidget(new GadgetToggle(leftPos + 27, topPos + s[1] + 2, slot));
        }
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

    /** Interruptor de una ranura de gadget: verde = encendido, rojo = apagado. */
    private class GadgetToggle extends net.minecraft.client.gui.components.AbstractButton {
        private final int slot;

        GadgetToggle(int x, int y, int slot) {
            super(x, y, 10, 12, Component.empty());
            this.slot = slot;
        }

        private boolean on() {
            return minecraft == null || minecraft.player == null
                    || TacticalGear.of(minecraft.player).map(g -> g.enabled(slot)).orElse(true);
        }

        @Override
        public void onPress() {
            com.bryanamaya.tecnocinetico.network.ModNetwork.CHANNEL.sendToServer(new com.bryanamaya.tecnocinetico.network.ModNetwork.ToggleGadgetPacket(slot));
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
            boolean on = on();
            g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF202326);
            g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, isHovered() ? 0xFF3A3F45 : 0xFF2B2F33);
            int led = on ? 0xFF4CE07A : 0xFFE04A3C;
            g.fill(getX() + 3, getY() + 3, getX() + width - 3, getY() + 6, led);           // luz
            g.fill(getX() + 3, getY() + (on ? 7 : 8), getX() + width - 3, getY() + (on ? 9 : 10), 0xFFB8BEC4);   // palanca
            if (isHovered()) {
                setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(
                        on ? "gui.tecnocinetico.gadget_on" : "gui.tecnocinetico.gadget_off")));
            }
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput out) {
            defaultButtonNarrationText(out);
        }
    }
}
