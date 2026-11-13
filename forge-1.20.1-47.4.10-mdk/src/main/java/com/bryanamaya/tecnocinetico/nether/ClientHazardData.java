package com.bryanamaya.tecnocinetico.nether;

/** Copia en el cliente del calor y la ceniza del jugador local (la manda NetherHazards). Sin clases de cliente. */
public final class ClientHazardData {

    private static int heat, ash;

    private ClientHazardData() {}

    public static void set(int h, int a) {
        heat = h;
        ash = a;
    }

    public static int heat() {
        return heat;
    }

    public static int ash() {
        return ash;
    }
}
