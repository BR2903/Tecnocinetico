package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.rules.FriendlyFire;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Botón "Fuego amigo" en la pantalla "Abrir en LAN": es la única forma de elegirlo en una partida LAN. Se aplica
 * solo si de verdad se abre el mundo a LAN (si se cancela, no cambia nada).
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID, value = Dist.CLIENT)
public final class LanFriendlyFireOption {

    /** Lo elegido en la pantalla; null = no hay pantalla de LAN abierta. */
    private static Boolean choice;

    private LanFriendlyFireOption() {}

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof ShareToLanScreen screen) || Minecraft.getInstance().getSingleplayerServer() == null) {
            return;
        }
        boolean current = choice != null ? choice : FriendlyFire.enabled();
        choice = current;
        event.addListener(CycleButton.onOffBuilder(current)
                .withTooltip(v -> Tooltip.create(Component.translatable("gui.tecnocinetico.friendly_fire.tooltip")))
                .create(screen.width / 2 - 75, 188, 150, 20, Component.translatable("gui.tecnocinetico.friendly_fire"),
                        (button, value) -> choice = value));
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || choice == null || mc.screen instanceof ShareToLanScreen) {
            return;
        }
        var server = mc.getSingleplayerServer();
        if (server != null && server.isPublished()) {                    // se abrió a LAN: aplicar
            FriendlyFire.set(server, choice);
            mc.gui.getChat().addMessage(Component.translatable(choice ? "message.tecnocinetico.friendly_fire_on"
                    : "message.tecnocinetico.friendly_fire_off"));
        }
        choice = null;
    }
}
