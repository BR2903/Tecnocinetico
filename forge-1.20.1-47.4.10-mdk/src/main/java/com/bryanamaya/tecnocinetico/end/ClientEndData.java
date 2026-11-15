package com.bryanamaya.tecnocinetico.end;

/**
 * Datos del End en el cliente (los rellena {@code EndQuakePacket}; los usa {@code client/EndClientEffects}). Clase común
 * sin referencias al cliente, para que el paquete pueda cargarse también en el servidor.
 */
public final class ClientEndData {

    private static int quakeTicks;
    private static float quakeStrength;

    private ClientEndData() {}

    public static void quake(int ticks, float strength) {
        quakeTicks = Math.max(quakeTicks, ticks);
        quakeStrength = Math.max(0.0F, Math.min(1.0F, strength));
    }

    /** Fuerza actual del temblor (0 = nada); baja al final. */
    public static float quake() {
        return quakeTicks <= 0 ? 0.0F : quakeStrength * Math.min(1.0F, quakeTicks / 8.0F);
    }

    public static void tick() {
        if (quakeTicks > 0) {
            quakeTicks--;
        }
    }
}
