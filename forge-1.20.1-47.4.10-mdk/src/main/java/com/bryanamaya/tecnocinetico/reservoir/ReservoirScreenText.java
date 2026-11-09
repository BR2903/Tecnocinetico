package com.bryanamaya.tecnocinetico.reservoir;

/** Formato de cantidades de líquido (común a la interfaz y a la descripción del objeto). */
public final class ReservoirScreenText {

    private ReservoirScreenText() {}

    /** 1.250 mB -> "1,25 B"; 3.400.000 mB -> "3.400 B". */
    public static String amount(long mb) {
        if (mb < 1000) {
            return mb + " mB";
        }
        double b = mb / 1000.0;
        if (b < 100) {
            return String.format("%.2f B", b);
        }
        return String.format("%,d B", (long) b);
    }
}
