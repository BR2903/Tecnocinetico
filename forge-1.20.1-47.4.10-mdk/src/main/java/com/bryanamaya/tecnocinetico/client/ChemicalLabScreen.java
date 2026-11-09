package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.machine.ChemicalLabBlockEntity;
import com.bryanamaya.tecnocinetico.menu.MachineMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

/** Laboratorio Químico: lo común + dos tanques con el líquido dibujado (entrada a la izquierda, salida a la derecha). */
public class ChemicalLabScreen extends MachineScreen {

    public static final int IN_X = 10, OUT_X = 122, TANK_Y = 17, TANK_W = 16, TANK_H = 52;

    public ChemicalLabScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    private Fluid fluid(int idSlot) {
        return ChemicalLabBlockEntity.fluidById(menu.extra(idSlot));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        super.renderBg(g, partialTick, mouseX, mouseY);
        drawTank(g, IN_X, fluid(ChemicalLabBlockEntity.EXTRA_IN_FLUID), menu.extra(ChemicalLabBlockEntity.EXTRA_IN_AMOUNT));
        drawTank(g, OUT_X, fluid(ChemicalLabBlockEntity.EXTRA_OUT_FLUID), menu.extra(ChemicalLabBlockEntity.EXTRA_OUT_AMOUNT));
    }

    private void drawTank(GuiGraphics g, int x, Fluid fluid, int amount) {
        if (fluid == Fluids.EMPTY || amount <= 0) {
            return;
        }
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid);
        TextureAtlasSprite sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(new FluidStack(fluid, amount)));
        int tint = ext.getTintColor(new FluidStack(fluid, amount));
        int h = Math.max(1, amount * TANK_H / ChemicalLabBlockEntity.TANK);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(((tint >> 16) & 255) / 255F, ((tint >> 8) & 255) / 255F, (tint & 255) / 255F, 1.0F);
        int bottom = topPos + TANK_Y + TANK_H;
        for (int drawn = 0; drawn < h; drawn += 16) {                         // en mosaico de 16 px
            int piece = Math.min(16, h - drawn);
            g.blit(leftPos + x, bottom - drawn - piece, 0, TANK_W, piece, sprite);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        for (int y = TANK_Y + 6; y < TANK_Y + TANK_H; y += 8) {               // marcas de nivel encima del líquido
            g.fill(leftPos + x, topPos + y, leftPos + x + 4, topPos + y + 1, 0x80202020);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
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
        g.drawString(font, status, imageWidth - 8 - font.width(status), titleLabelY, color, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        tankTooltip(g, mouseX, mouseY, IN_X, "gui.tecnocinetico.lab.tank_in", ChemicalLabBlockEntity.EXTRA_IN_FLUID, ChemicalLabBlockEntity.EXTRA_IN_AMOUNT);
        tankTooltip(g, mouseX, mouseY, OUT_X, "gui.tecnocinetico.lab.tank_out", ChemicalLabBlockEntity.EXTRA_OUT_FLUID, ChemicalLabBlockEntity.EXTRA_OUT_AMOUNT);
    }

    private void tankTooltip(GuiGraphics g, int mx, int my, int x, String key, int idSlot, int amountSlot) {
        int bx = leftPos + x, by = topPos + TANK_Y;
        if (mx < bx || mx >= bx + TANK_W || my < by || my >= by + TANK_H) {
            return;
        }
        Fluid f = fluid(idSlot);
        int amount = menu.extra(amountSlot);
        Component name = f == Fluids.EMPTY || amount <= 0 ? Component.translatable("gui.tecnocinetico.lab.empty")
                : f.getFluidType().getDescription(new FluidStack(f, amount));
        g.renderComponentTooltip(font, List.of(
                Component.translatable(key).withStyle(ChatFormatting.GRAY),
                name.copy().withStyle(ChatFormatting.AQUA),
                Component.translatable("gui.tecnocinetico.lab.amount", amount, ChemicalLabBlockEntity.TANK).withStyle(ChatFormatting.GRAY),
                Component.translatable("gui.tecnocinetico.lab.bucket_hint").withStyle(ChatFormatting.DARK_GRAY)), mx, my);
    }
}
