package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.entity.AutoKineticPressBlockEntity;
import com.bryanamaya.tecnocinetico.menu.AutoKineticPressMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** Pantalla de la Prensa Cinética Automática: progreso, barra de energía y estado. */
public class AutoKineticPressScreen extends AbstractContainerScreen<AutoKineticPressMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/gui/auto_kinetic_press.png");
    // Piezas "llenas" dentro de la textura (a la derecha del panel)
    private static final int ARROW_U = 176, ARROW_V = 0;
    private static final int ENERGY_U = 176, ENERGY_V = 17;

    public AutoKineticPressScreen(AutoKineticPressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 178;                                               // +12: franja de estado
        this.inventoryLabelY = 85;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Flecha de progreso (se llena de izquierda a derecha)
        int arrow = menu.getProgress() * AutoKineticPressMenu.ARROW_W / AutoKineticPressBlockEntity.CYCLE_TICKS;
        if (arrow > 0) {
            g.blit(TEXTURE, leftPos + AutoKineticPressMenu.ARROW_X, topPos + AutoKineticPressMenu.ARROW_Y,
                    ARROW_U, ARROW_V, arrow, AutoKineticPressMenu.ARROW_H);
        }

        // Barra de energía (se llena de abajo arriba)
        int h = (int) ((long) menu.getEnergy() * AutoKineticPressMenu.ENERGY_H / AutoKineticPressBlockEntity.ENERGY_CAPACITY);
        if (h > 0) {
            int top = AutoKineticPressMenu.ENERGY_Y + AutoKineticPressMenu.ENERGY_H - h;
            g.blit(TEXTURE, leftPos + AutoKineticPressMenu.ENERGY_X, topPos + top,
                    ENERGY_U, ENERGY_V + (AutoKineticPressMenu.ENERGY_H - h), AutoKineticPressMenu.ENERGY_W, h);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        // Estado debajo de los ingredientes
        Component status;
        int color;
        if (!menu.hasPower()) {
            status = Component.translatable("gui.tecnocinetico.press.no_power");
            color = 0xC03030;
        } else if (menu.getProgress() > 0) {
            status = Component.translatable("gui.tecnocinetico.press.working",
                    menu.getProgress() * 100 / AutoKineticPressBlockEntity.CYCLE_TICKS);
            color = 0x1E8C9E;
        } else {
            status = Component.translatable("gui.tecnocinetico.press.ready");
            color = 0x1E8C9E;
        }
        g.drawString(font, status, 10, 74, brighten(color), false);                // en la franja de estado
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        int ex = leftPos + AutoKineticPressMenu.ENERGY_X, ey = topPos + AutoKineticPressMenu.ENERGY_Y;
        if (mouseX >= ex && mouseX < ex + AutoKineticPressMenu.ENERGY_W
                && mouseY >= ey && mouseY < ey + AutoKineticPressMenu.ENERGY_H) {
            g.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.tecnocinetico.press.energy", menu.getEnergy(),
                            AutoKineticPressBlockEntity.ENERGY_CAPACITY).withStyle(ChatFormatting.AQUA),
                    Component.translatable("gui.tecnocinetico.press.usage",
                            AutoKineticPressBlockEntity.ENERGY_PER_TICK).withStyle(ChatFormatting.GRAY)),
                    mouseX, mouseY);
        }
    }

    /** Colores pensados para fondo claro, aclarados para la franja oscura. */
    private static int brighten(int c) {
        return c == 0xC03030 ? 0xFF5050 : 0x7FE7F5;
    }
}
