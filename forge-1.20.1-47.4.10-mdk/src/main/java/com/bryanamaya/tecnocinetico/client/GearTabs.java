package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.function.Consumer;

/**
 * Pestañas "Inventario" / "Equipo Táctico" encima del inventario normal y de la pantalla del equipo.
 * Se recolocan en cada fotograma (el libro de recetas desplaza el inventario).
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID, value = Dist.CLIENT)
public final class GearTabs {

    private GearTabs() {}

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen inv) {
            add(inv, w -> event.addListener(w), true);
        }
    }

    /** Añade las dos pestañas; inInventory = estamos en el inventario normal (pestaña 0 activa). */
    static void add(AbstractContainerScreen<?> screen, Consumer<AbstractButton> adder, boolean inInventory) {
        adder.accept(new Tab(screen, 0, TacticalScreen.inventoryIcon(), Component.translatable("gui.tecnocinetico.tab.inventory"),
                inInventory, () -> {
            Minecraft mc = Minecraft.getInstance();
            if (!inInventory && mc.player != null) {
                mc.player.closeContainer();
                mc.setScreen(new InventoryScreen(mc.player));
            }
        }));
        adder.accept(new Tab(screen, 1, TacticalScreen.tabIcon(), Component.translatable("gui.tecnocinetico.tab.gear"),
                !inInventory, () -> {
            if (inInventory) {
                ModNetwork.CHANNEL.sendToServer(new ModNetwork.OpenGearPacket());
            }
        }));
    }

    /** Pestaña con un icono de objeto; la activa se dibuja más alta y pegada al panel. */
    static final class Tab extends AbstractButton {
        private final AbstractContainerScreen<?> screen;
        private final int index;
        private final ItemStack icon;
        private final boolean selected;
        private final Runnable action;

        Tab(AbstractContainerScreen<?> screen, int index, ItemStack icon, Component label, boolean active, Runnable action) {
            super(0, 0, 26, 24, label);
            this.screen = screen;
            this.index = index;
            this.icon = icon;
            this.selected = active;
            this.action = action;
            setTooltip(Tooltip.create(label));
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            setX(screen.getGuiLeft() + 4 + index * 28);
            setY(screen.getGuiTop() - 24);
            int v = selected ? 24 : 0;
            g.blit(TacticalScreen.TEXTURE, getX(), getY(), 176 + (isHovered() && !selected ? 26 : 0), v, 26, 24 + (selected ? 3 : 0));
            g.renderItem(icon, getX() + 5, getY() + 5 + (selected ? 1 : 2));
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
            defaultButtonNarrationText(out);
        }
    }
}
