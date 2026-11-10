package com.bryanamaya.tecnocinetico.gear;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;

/**
 * En un servidor dedicado con allow-flight=false, vanilla expulsa a quien pase más de 80 ticks en el aire sin caer
 * ("Flying is not enabled on this server"). El vuelo del jetpack es legítimo: mientras empuja se reinicia ese contador.
 */
public final class FlightKick {

    /** ServerGamePacketListenerImpl.aboveGroundTickCount (nombre SRG: f_9737_). */
    private static final Field ABOVE_GROUND = find();

    private FlightKick() {}

    private static Field find() {
        try {
            return ObfuscationReflectionHelper.findField(ServerGamePacketListenerImpl.class, "f_9737_");
        } catch (RuntimeException e) {
            Tecnocinetico.LOGGER.error("No se encontró aboveGroundTickCount: el jetpack podría expulsar en servidores", e);
            return null;
        }
    }

    public static void allow(ServerPlayer player) {
        if (ABOVE_GROUND == null || player.connection == null) {
            return;
        }
        try {
            ABOVE_GROUND.setInt(player.connection, 0);
        } catch (IllegalAccessException ignored) {
        }
    }
}
