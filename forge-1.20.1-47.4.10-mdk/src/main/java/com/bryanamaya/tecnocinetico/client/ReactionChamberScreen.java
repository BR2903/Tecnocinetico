package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.machine.ReactionChamberBlockEntity;
import com.bryanamaya.tecnocinetico.menu.MachineMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Cámara de Reacción: además de lo común, botones de frecuencia e intensidad y dos barras verticales
 * (resonancia: lo cerca que está el ajuste; estabilidad: si llega a 0 la carga se desintegra).
 */
public class ReactionChamberScreen extends MachineScreen {

    public static final int RES_X = 132, STAB_X = 141, BAR_Y = 17, BAR_W = 6, BAR_H = 52;
    private static final int RES_U = 192, STAB_U = 200, BAR_V = 17;
    private static final int ROW_Y = 60, FREQ_X = 10, INT_X = 66;

    public ReactionChamberScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        addButton(FREQ_X, ReactionChamberBlockEntity.BTN_FREQ_DOWN, "-");
        addButton(FREQ_X + 34, ReactionChamberBlockEntity.BTN_FREQ_UP, "+");
        addButton(INT_X, ReactionChamberBlockEntity.BTN_INT_DOWN, "-");
        addButton(INT_X + 34, ReactionChamberBlockEntity.BTN_INT_UP, "+");
    }

    private void addButton(int x, int id, String label) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
            }
        }).bounds(leftPos + x, topPos + ROW_Y, 11, 11).build());
    }

    private int frequency() {
        return menu.extra(ReactionChamberBlockEntity.EXTRA_FREQUENCY);
    }

    private int intensity() {
        return menu.extra(ReactionChamberBlockEntity.EXTRA_INTENSITY);
    }

    private int stability() {
        return menu.extra(ReactionChamberBlockEntity.EXTRA_STABILITY);
    }

    private int resonance() {
        return menu.extra(ReactionChamberBlockEntity.EXTRA_RESONANCE);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        super.renderBg(g, partialTick, mouseX, mouseY);
        int r = resonance() * BAR_H / 100;
        if (r > 0) {
            g.blit(texture, leftPos + RES_X, topPos + BAR_Y + BAR_H - r, RES_U, BAR_V + BAR_H - r, BAR_W, r);
        }
        int s = stability() * BAR_H / ReactionChamberBlockEntity.STABILITY_MAX;
        if (s > 0) {
            g.blit(texture, leftPos + STAB_X, topPos + BAR_Y + BAR_H - s, STAB_U, BAR_V + BAR_H - s, BAR_W, s);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        g.drawString(font, Component.translatable("gui.tecnocinetico.reaction.frequency"), FREQ_X, ROW_Y - 10, 0x404040, false);
        g.drawString(font, Component.translatable("gui.tecnocinetico.reaction.intensity"), INT_X, ROW_Y - 10, 0x404040, false);
        g.drawCenteredString(font, String.valueOf(frequency()), FREQ_X + 22, ROW_Y + 2, 0x7FE7F5);
        g.drawCenteredString(font, String.valueOf(intensity()), INT_X + 22, ROW_Y + 2, 0x7FE7F5);
    }

    @Override
    protected Component status() {
        if (menu.energy() <= 0) {
            return Component.translatable("gui.tecnocinetico.press.no_power").withStyle(s -> s.withColor(0xFF5050));
        }
        if (stability() < ReactionChamberBlockEntity.STABILITY_MAX / 3) {
            return Component.translatable("gui.tecnocinetico.reaction.unstable").withStyle(s -> s.withColor(0xFF5050));
        }
        return super.status();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (inBar(mouseX, mouseY, RES_X)) {
            g.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.tecnocinetico.reaction.resonance", resonance()).withStyle(ChatFormatting.AQUA),
                    Component.translatable("gui.tecnocinetico.reaction.resonance_hint").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        } else if (inBar(mouseX, mouseY, STAB_X)) {
            g.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.tecnocinetico.reaction.stability", stability() / 10).withStyle(ChatFormatting.GOLD),
                    Component.translatable("gui.tecnocinetico.reaction.stability_hint").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }

    private boolean inBar(int mx, int my, int x) {
        int bx = leftPos + x, by = topPos + BAR_Y;
        return mx >= bx && mx < bx + BAR_W && my >= by && my < by + BAR_H;
    }
}
