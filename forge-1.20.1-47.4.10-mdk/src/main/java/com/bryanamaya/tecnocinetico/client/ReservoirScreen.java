package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.reservoir.ReservoirBlockEntity;
import com.bryanamaya.tecnocinetico.reservoir.ReservoirMenu;
import com.bryanamaya.tecnocinetico.reservoir.ReservoirScreenText;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

/**
 * Reservorio Universal: lista de líquidos guardados (icono, nombre, cantidad). Clic en una fila = líquido que sale
 * por las tuberías/cubos (marcado en cian); ✕ = desechar ese líquido. Rueda del ratón para desplazar la lista.
 * Lee el contenido de la entidad de bloque del cliente (el servidor la sincroniza cada 10 ticks).
 */
public class ReservoirScreen extends AbstractContainerScreen<ReservoirMenu> {

    private static final int ROWS = 7, ROW_H = 18, LIST_X = 8, LIST_Y = 20, LIST_W = 160;
    private int scroll;

    public ReservoirScreen(ReservoirMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = LIST_Y + ROWS * ROW_H + 22;
    }

    private List<ReservoirBlockEntity.Entry> entries() {
        return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(menu.pos) instanceof ReservoirBlockEntity r
                ? r.fluids() : List.of();
    }

    private int selected() {
        return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(menu.pos) instanceof ReservoirBlockEntity r
                ? r.selected() : 0;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x0 = leftPos, y0 = topPos, x1 = leftPos + imageWidth, y1 = topPos + imageHeight;
        g.fill(x0, y0, x1, y1, 0xFF000000);                                    // panel estilo vanilla
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0xFFC6C6C6);
        g.fill(x0 + 1, y0 + 1, x1 - 2, y0 + 2, 0xFFFFFFFF);
        g.fill(x0 + 1, y0 + 1, x0 + 2, y1 - 2, 0xFFFFFFFF);
        g.fill(x0 + 2, y1 - 2, x1 - 1, y1 - 1, 0xFF555555);
        g.fill(x1 - 2, y0 + 2, x1 - 1, y1 - 1, 0xFF555555);
        int lx = leftPos + LIST_X, ly = topPos + LIST_Y;
        g.fill(lx - 1, ly - 1, lx + LIST_W + 1, ly + ROWS * ROW_H + 1, 0xFF373737);
        g.fill(lx, ly, lx + LIST_W, ly + ROWS * ROW_H, 0xFF22262C);
        List<ReservoirBlockEntity.Entry> list = entries();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, list.size() - ROWS)));
        for (int row = 0; row < ROWS; row++) {
            int i = row + scroll;
            if (i >= list.size()) {
                break;
            }
            ReservoirBlockEntity.Entry e = list.get(i);
            int ry = ly + row * ROW_H;
            boolean hover = mouseX >= lx && mouseX < lx + LIST_W - 14 && mouseY >= ry && mouseY < ry + ROW_H;
            if (i == selected()) {
                g.fill(lx, ry, lx + LIST_W, ry + ROW_H, 0xFF1E5A66);
            } else if (hover) {
                g.fill(lx, ry, lx + LIST_W, ry + ROW_H, 0xFF30363E);
            }
            // icono del líquido (textura teñida)
            FluidStack fs = new FluidStack(e.fluid, 1000);
            IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(e.fluid);
            TextureAtlasSprite sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fs));
            int tint = ext.getTintColor(fs);
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(((tint >> 16) & 255) / 255F, ((tint >> 8) & 255) / 255F, (tint & 255) / 255F, 1.0F);
            g.blit(lx + 2, ry + 2, 0, 14, 14, sprite);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
            g.drawString(font, e.fluid.getFluidType().getDescription(fs), lx + 20, ry + 5, 0xFFFFFF, false);
            String amount = ReservoirScreenText.amount(e.amount);
            g.drawString(font, amount, lx + LIST_W - 16 - font.width(amount), ry + 5, 0x7FE7F5, false);
            // ✕ desechar
            int bx = lx + LIST_W - 13;
            boolean overX = mouseX >= bx && mouseX < bx + 11 && mouseY >= ry + 3 && mouseY < ry + 14;
            g.fill(bx, ry + 3, bx + 11, ry + 14, overX ? 0xFFB03030 : 0xFF5A2A2A);
            g.drawCenteredString(font, "✕", bx + 6, ry + 5, 0xFFFFFF);
        }
        if (list.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.tecnocinetico.reservoir.empty"), lx + LIST_W / 2, ly + ROWS * ROW_H / 2 - 4, 0x808080);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, 7, 0x404040, false);
        g.drawString(font, Component.translatable("gui.tecnocinetico.reservoir.hint"), LIST_X, LIST_Y + ROWS * ROW_H + 6, 0x606060, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        int lx = leftPos + LIST_X, ly = topPos + LIST_Y;
        List<ReservoirBlockEntity.Entry> list = entries();
        for (int row = 0; row < ROWS && row + scroll < list.size(); row++) {
            int ry = ly + row * ROW_H, bx = lx + LIST_W - 13;
            if (mouseX >= bx && mouseX < bx + 11 && mouseY >= ry + 3 && mouseY < ry + 14) {
                g.renderTooltip(font, Component.translatable("gui.tecnocinetico.reservoir.discard").withStyle(ChatFormatting.RED), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int lx = leftPos + LIST_X, ly = topPos + LIST_Y;
        List<ReservoirBlockEntity.Entry> list = entries();
        if (button == 0 && mx >= lx && mx < lx + LIST_W && my >= ly && my < ly + ROWS * ROW_H) {
            int i = (int) ((my - ly) / ROW_H) + scroll;
            if (i < list.size() && minecraft != null && minecraft.gameMode != null) {
                boolean discard = mx >= lx + LIST_W - 13;
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, discard ? 100 + i : i);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll = Math.max(0, scroll - (int) Math.signum(delta));
        return true;
    }
}
