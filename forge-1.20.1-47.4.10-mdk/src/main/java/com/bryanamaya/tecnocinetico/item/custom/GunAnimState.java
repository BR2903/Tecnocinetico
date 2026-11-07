package com.bryanamaya.tecnocinetico.item.custom;

/**
 * Estado del último disparo del jugador local, para la animación de retroceso en primera persona
 * y el retroceso de la corredera. Solo datos (sin clases del cliente): se escribe desde
 * KineticPistolItem en el lado cliente.
 */
public final class GunAnimState {

    /** Ticks que la corredera se queda atrás tras un disparo. */
    public static final int SLIDE_BACK_TICKS = 2;
    /** Ticks que dura la animación de retroceso. */
    public static final int RECOIL_TICKS = 10;

    public static long lastShotTick = Long.MIN_VALUE / 2;
    public static float kickStrength = 1.0F;
    /** Giro lateral de este disparo (-1..1), distinto en cada tiro. */
    public static float roll = 0.0F;

    private GunAnimState() {}

    public static void onShot(long gameTime, float strength, float randomRoll) {
        lastShotTick = gameTime;
        kickStrength = strength;
        roll = randomRoll;
    }

    /** Ticks (con fracción) desde el último disparo. */
    public static float ticksSinceShot(long gameTime, float partialTick) {
        return (gameTime - lastShotTick) + partialTick;
    }
}
