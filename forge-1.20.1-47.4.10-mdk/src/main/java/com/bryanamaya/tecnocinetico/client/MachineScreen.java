package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.machine.MachineKind;
import com.bryanamaya.tecnocinetico.menu.MachineMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** Pantalla común de las máquinas eléctricas (textura gui/<máquina>.png de tools/machine_gui_gen.py). */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {

    // Piezas "llenas" a la derecha del panel en la textura
    private static final int ARROW_U = 176, ARROW_V = 0, ENERGY_U = 176, ENERGY_V = 17;
    protected final ResourceLocation texture;

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.texture = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/gui/" + menu.kind().id + ".png");
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        MachineKind k = menu.kind();
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (menu.maxProgress() > 0 && menu.progress() > 0) {
            int w = Math.min(MachineKind.ARROW_W, menu.progress() * MachineKind.ARROW_W / menu.maxProgress());
            g.blit(texture, leftPos + k.arrowX, topPos + k.arrowY, ARROW_U, ARROW_V, w, MachineKind.ARROW_H);
        }
        int h = (int) ((long) menu.energy() * MachineKind.ENERGY_H / k.capacity);
        if (h > 0) {
            int top = MachineKind.ENERGY_Y + MachineKind.ENERGY_H - h;
            g.blit(texture, leftPos + MachineKind.ENERGY_X, topPos + top, ENERGY_U, ENERGY_V + (MachineKind.ENERGY_H - h), MachineKind.ENERGY_W, h);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        Component status;
        int color = 0x1E8C9E;
        if (menu.energy() <= 0) {
            status = Component.translatable("gui.tecnocinetico.press.no_power");
            color = 0xC03030;
        } else if (menu.progress() > 0 && menu.maxProgress() > 0) {
            status = Component.translatable("gui.tecnocinetico.press.working", menu.progress() * 100 / menu.maxProgress());
        } else {
            status = Component.translatable("gui.tecnocinetico.press.ready");
        }
        g.drawString(font, status, menu.kind().inputs[0][0], 58, color, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int ex = leftPos + MachineKind.ENERGY_X, ey = topPos + MachineKind.ENERGY_Y;
        if (mouseX >= ex && mouseX < ex + MachineKind.ENERGY_W && mouseY >= ey && mouseY < ey + MachineKind.ENERGY_H) {
            g.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.tecnocinetico.press.energy", menu.energy(), menu.kind().capacity).withStyle(ChatFormatting.AQUA),
                    Component.translatable("gui.tecnocinetico.press.usage", menu.usage()).withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }
}
