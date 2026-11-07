package com.bryanamaya.tecnocinetico.item.custom;

/**
 * Estado del último disparo del jugador local, para la animación de retroceso en primera persona.
 * Solo datos (sin clases del cliente): se escribe desde KineticPistolItem en el lado cliente.
 */
public final class GunAnimState {

    public static long lastShotTick = Long.MIN_VALUE / 2;
    public static float kickStrength = 1.0F;

    private GunAnimState() {}

    public static void onShot(long gameTime, float strength) {
        lastShotTick = gameTime;
        kickStrength = strength;
    }
}
