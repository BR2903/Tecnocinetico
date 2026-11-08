package com.bryanamaya.tecnocinetico.machine;

/**
 * Las máquinas eléctricas (Forge Energy) que comparten bloque, inventario, interfaz y luz de estado.
 * Las posiciones de los huecos deben coincidir con las texturas que genera tools/machine_gui_gen.py.
 */
public enum MachineKind {
    /** Refinador Cinético: muele la mena en bruto y da 2 de polvo (+ subproducto con probabilidad). */
    REFINER("electric_refiner", new int[][]{{44, 35}}, new int[][]{{100, 35}, {130, 35}}, 68, 35, 20_000, 1_000),
    /** Cargador de células: rellena células de impulso, gadgets y cualquier objeto con batería FE. */
    CHARGER("cell_charger", new int[][]{{44, 35}}, new int[][]{{104, 35}}, 68, 35, 40_000, 2_000),
    /** Fabricador de munición: hace munición en lote. */
    AMMO_FABRICATOR("ammo_fabricator", new int[][]{{26, 35}, {44, 35}, {62, 35}}, new int[][]{{116, 35}}, 85, 35, 20_000, 1_000);

    public static final int ENERGY_X = 152, ENERGY_Y = 17, ENERGY_W = 14, ENERGY_H = 52;
    public static final int ARROW_W = 22, ARROW_H = 16;

    public final String id;
    public final int[][] inputs;
    public final int[][] outputs;
    public final int arrowX, arrowY;
    public final int capacity, maxReceive;

    MachineKind(String id, int[][] inputs, int[][] outputs, int arrowX, int arrowY, int capacity, int maxReceive) {
        this.id = id;
        this.inputs = inputs;
        this.outputs = outputs;
        this.arrowX = arrowX;
        this.arrowY = arrowY;
        this.capacity = capacity;
        this.maxReceive = maxReceive;
    }

    public int slots() {
        return inputs.length + outputs.length;
    }

    public boolean isOutput(int slot) {
        return slot >= inputs.length;
    }
}
