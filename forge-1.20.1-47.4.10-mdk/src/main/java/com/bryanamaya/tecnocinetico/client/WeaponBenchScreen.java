package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.item.custom.WeaponModule;
import com.bryanamaya.tecnocinetico.menu.WeaponBenchMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Interfaz del Banco de Armas Cinético: el arma a la izquierda y un esquema con una ranura por módulo. */
public class WeaponBenchScreen extends AbstractContainerScreen<WeaponBenchMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/gui/weapon_bench.png");

    public WeaponBenchScreen(WeaponBenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 190;
        this.inventoryLabelY = WeaponBenchMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Módulos "fantasma" en las ranuras vacías: indican qué va en cada sitio
        for (WeaponModule module : WeaponModule.values()) {
            Slot slot = menu.getSlot(1 + module.ordinal());
            if (!slot.hasItem()) {
                int x = leftPos + slot.x, y = topPos + slot.y;
                graphics.renderFakeItem(new ItemStack(ModItems.moduleItem(module)), x, y);
                graphics.fill(x, y, x + 16, y + 16, 300, 0xB0383C44); // velo oscuro encima
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        // Ayuda al pasar el ratón por una ranura vacía
        if (hoveredSlot != null && !hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            if (hoveredSlot instanceof WeaponBenchMenu.ModuleSlot ms) {
                List<Component> lines = List.of(
                        ms.getModule().displayName().copy().withStyle(ChatFormatting.AQUA),
                        ms.getModule().effectText().copy().withStyle(ChatFormatting.GRAY),
                        Component.translatable(menu.hasWeapon() ? "gui.tecnocinetico.bench.drop_module"
                                : "gui.tecnocinetico.bench.need_weapon").withStyle(ChatFormatting.DARK_GRAY));
                graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            } else if (hoveredSlot.index == WeaponBenchMenu.WEAPON_SLOT) {
                graphics.renderComponentTooltip(font, List.of(
                        Component.translatable("gui.tecnocinetico.bench.weapon_slot").withStyle(ChatFormatting.AQUA)),
                        mouseX, mouseY);
            }
        }
    }
}
