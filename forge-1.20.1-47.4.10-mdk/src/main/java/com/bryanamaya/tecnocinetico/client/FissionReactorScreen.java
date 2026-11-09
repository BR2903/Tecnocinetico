package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.machine.ChemicalLabBlockEntity;
import com.bryanamaya.tecnocinetico.machine.FissionReactorBlockEntity;
import com.bryanamaya.tecnocinetico.menu.MachineMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

/**
 * Reactor de Fisión: barras de control (potencia -/+), tanque de refrigerante, termómetro (verde -> rojo, con la
 * marca de alarma) y lo que genera ahora mismo. La flecha muestra lo consumido de la barra actual.
 */
public class FissionReactorScreen extends MachineScreen {

    public static final int TANK_X = 100, TANK_Y = 17, TANK_W = 16, TANK_H = 44;
    public static final int HEAT_X = 124, HEAT_Y = 17, HEAT_W = 6, HEAT_H = 52;
    private static final int HEAT_U = 192, HEAT_V = 17;
    private static final int ROW_Y = 60, CTRL_X = 26;

    public FissionReactorScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    private int extra(int i) {
        return menu.extra(i);
    }

    @Override
    protected void init() {
        super.init();
        button(CTRL_X, FissionReactorBlockEntity.BTN_DOWN, "-", "gui.tecnocinetico.reactor.down");
        button(CTRL_X + 40, FissionReactorBlockEntity.BTN_UP, "+", "gui.tecnocinetico.reactor.up");
    }

    private void button(int x, int id, String label, String tip) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
            }
        }).bounds(leftPos + x, topPos + ROW_Y, 11, 11).tooltip(Tooltip.create(Component.translatable(tip))).build());
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        super.renderBg(g, partialTick, mouseX, mouseY);
        // termómetro
        int h = Math.min(HEAT_H, extra(FissionReactorBlockEntity.EXTRA_HEAT) * HEAT_H / FissionReactorBlockEntity.MELTDOWN);
        if (h > 0) {
            g.blit(texture, leftPos + HEAT_X, topPos + HEAT_Y + HEAT_H - h, HEAT_U, HEAT_V + HEAT_H - h, HEAT_W, h);
        }
        // refrigerante
        Fluid f = ChemicalLabBlockEntity.fluidById(extra(FissionReactorBlockEntity.EXTRA_FLUID));
        int amount = extra(FissionReactorBlockEntity.EXTRA_AMOUNT);
        if (f != Fluids.EMPTY && amount > 0) {
            FluidStack fs = new FluidStack(f, amount);
            IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(f);
            TextureAtlasSprite sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fs));
            int tint = ext.getTintColor(fs);
            int th = Math.max(1, amount * TANK_H / FissionReactorBlockEntity.TANK);
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(((tint >> 16) & 255) / 255F, ((tint >> 8) & 255) / 255F, (tint & 255) / 255F, 1.0F);
            int bottom = topPos + TANK_Y + TANK_H;
            for (int drawn = 0; drawn < th; drawn += 16) {
                int piece = Math.min(16, th - drawn);
                g.blit(leftPos + TANK_X, bottom - drawn - piece, 0, TANK_W, piece, sprite);
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        g.drawString(font, Component.translatable("gui.tecnocinetico.reactor.power"), CTRL_X, ROW_Y - 10, 0x404040, false);
        g.drawCenteredString(font, extra(FissionReactorBlockEntity.EXTRA_THROTTLE) + "%", CTRL_X + 26, ROW_Y + 2, 0x7FE7F5);
        int heat = extra(FissionReactorBlockEntity.EXTRA_HEAT), out = extra(FissionReactorBlockEntity.EXTRA_OUTPUT);
        Component status;
        int color = 0x1E8C9E;
        if (heat >= FissionReactorBlockEntity.ALARM) {
            status = Component.translatable("gui.tecnocinetico.reactor.meltdown");
            color = 0xE02020;
        } else if (out > 0) {
            status = Component.translatable("gui.tecnocinetico.reactor.generating", out);
        } else {
            status = Component.translatable("gui.tecnocinetico.reactor.idle");
            color = 0x808080;
        }
        g.drawString(font, status, imageWidth - 8 - font.width(status), titleLabelY, color, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (in(mouseX, mouseY, HEAT_X, HEAT_Y, HEAT_W, HEAT_H)) {
            int heat = extra(FissionReactorBlockEntity.EXTRA_HEAT);
            g.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.tecnocinetico.reactor.heat", heat, FissionReactorBlockEntity.MELTDOWN)
                            .withStyle(heat >= FissionReactorBlockEntity.ALARM ? ChatFormatting.RED : ChatFormatting.GOLD),
                    Component.translatable("gui.tecnocinetico.reactor.heat_hint").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        } else if (in(mouseX, mouseY, TANK_X, TANK_Y, TANK_W, TANK_H)) {
            Fluid f = ChemicalLabBlockEntity.fluidById(extra(FissionReactorBlockEntity.EXTRA_FLUID));
            int amount = extra(FissionReactorBlockEntity.EXTRA_AMOUNT);
            Component name = f == Fluids.EMPTY || amount <= 0 ? Component.translatable("gui.tecnocinetico.lab.empty")
                    : f.getFluidType().getDescription(new FluidStack(f, amount));
            g.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.tecnocinetico.reactor.coolant").withStyle(ChatFormatting.GRAY),
                    name.copy().withStyle(ChatFormatting.AQUA),
                    Component.translatable("gui.tecnocinetico.lab.amount", amount, FissionReactorBlockEntity.TANK).withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.tecnocinetico.reactor.coolant_hint").withStyle(ChatFormatting.DARK_GRAY)), mouseX, mouseY);
        } else if (in(mouseX, mouseY, menu.kind().arrowX, menu.kind().arrowY, 22, 16)) {
            g.renderComponentTooltip(font, List.of(Component.translatable("gui.tecnocinetico.reactor.fuel",
                    extra(FissionReactorBlockEntity.EXTRA_FUEL)).withStyle(ChatFormatting.GREEN)), mouseX, mouseY);
        }
    }

    private boolean in(int mx, int my, int x, int y, int w, int h) {
        int bx = leftPos + x, by = topPos + y;
        return mx >= bx && mx < bx + w && my >= by && my < by + h;
    }
}
