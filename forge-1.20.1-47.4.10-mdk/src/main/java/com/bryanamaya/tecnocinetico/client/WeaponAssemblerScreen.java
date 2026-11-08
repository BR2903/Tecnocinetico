package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.entity.WeaponAssemblerBlockEntity;
import com.bryanamaya.tecnocinetico.menu.WeaponAssemblerMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** Pantalla del Ensamblador de Armas: progreso, barra de energía y estado. */
public class WeaponAssemblerScreen extends AbstractContainerScreen<WeaponAssemblerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/gui/weapon_assembler.png");
    // Piezas "llenas" dentro de la textura (a la derecha del panel)
    private static final int ARROW_U = 176, ARROW_V = 0;
    private static final int ENERGY_U = 176, ENERGY_V = 17;

    public WeaponAssemblerScreen(WeaponAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Flecha de progreso (se llena de izquierda a derecha)
        int arrow = menu.getMaxProgress() <= 0 ? 0 : menu.getProgress() * WeaponAssemblerMenu.ARROW_W / menu.getMaxProgress();
        if (arrow > 0) {
            g.blit(TEXTURE, leftPos + WeaponAssemblerMenu.ARROW_X, topPos + WeaponAssemblerMenu.ARROW_Y,
                    ARROW_U, ARROW_V, arrow, WeaponAssemblerMenu.ARROW_H);
        }

        // Barra de energía (se llena de abajo arriba)
        int h = (int) ((long) menu.getEnergy() * WeaponAssemblerMenu.ENERGY_H / WeaponAssemblerBlockEntity.ENERGY_CAPACITY);
        if (h > 0) {
            int top = WeaponAssemblerMenu.ENERGY_Y + WeaponAssemblerMenu.ENERGY_H - h;
            g.blit(TEXTURE, leftPos + WeaponAssemblerMenu.ENERGY_X, topPos + top,
                    ENERGY_U, ENERGY_V + (WeaponAssemblerMenu.ENERGY_H - h), WeaponAssemblerMenu.ENERGY_W, h);
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
                    menu.getProgress() * 100 / Math.max(1, menu.getMaxProgress()));
            color = 0x1E8C9E;
        } else {
            status = Component.translatable("gui.tecnocinetico.press.ready");
            color = 0x1E8C9E;
        }
        g.drawString(font, status, 26, 64, color, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        int ex = leftPos + WeaponAssemblerMenu.ENERGY_X, ey = topPos + WeaponAssemblerMenu.ENERGY_Y;
        if (mouseX >= ex && mouseX < ex + WeaponAssemblerMenu.ENERGY_W
                && mouseY >= ey && mouseY < ey + WeaponAssemblerMenu.ENERGY_H) {
            g.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.tecnocinetico.press.energy", menu.getEnergy(),
                            WeaponAssemblerBlockEntity.ENERGY_CAPACITY).withStyle(ChatFormatting.AQUA),
                    Component.translatable("gui.tecnocinetico.press.usage",
                            WeaponAssemblerBlockEntity.ENERGY_PER_TICK).withStyle(ChatFormatting.GRAY)),
                    mouseX, mouseY);
        }
    }
}
