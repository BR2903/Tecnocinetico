package com.bryanamaya.tecnocinetico.client.map;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.map.MapEvents;
import com.bryanamaya.tecnocinetico.map.MapPackets;
import com.bryanamaya.tecnocinetico.map.NavigationData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** Lo que el cliente sabe del Reloj de Navegación (lo manda el servidor con {@link MapPackets.Sync}). */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID, value = Dist.CLIENT)
public final class ClientMapState {

    public static boolean satellite;
    public static List<NavigationData.Marker> markers = List.of();
    public static List<NavigationData.Found> found = List.of();

    private ClientMapState() {}

    public static void apply(MapPackets.Sync m) {
        satellite = m.satellite();
        markers = List.copyOf(m.markers());
        found = List.copyOf(m.found());
    }

    public static void located(MapPackets.LocateResult r) {
        if (Minecraft.getInstance().screen instanceof WristMapScreen screen) {
            screen.onLocated(r);
        }
    }

    /** Abre el mapa si hay satélite y lleva el reloj; si no, dice qué falta. */
    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (!satellite) {
            mc.player.displayClientMessage(Component.translatable("map.tecnocinetico.no_satellite").withStyle(ChatFormatting.YELLOW), true);
        } else if (!MapEvents.hasWatch(mc.player)) {
            mc.player.displayClientMessage(Component.translatable("map.tecnocinetico.no_watch").withStyle(ChatFormatting.YELLOW), true);
        } else {
            mc.setScreen(new WristMapScreen());
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        satellite = false;
        markers = List.of();
        found = List.of();
        MapExplorer.close();
    }
}
