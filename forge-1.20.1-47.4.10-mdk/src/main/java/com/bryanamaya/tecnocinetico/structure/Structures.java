package com.bryanamaya.tecnocinetico.structure;

import com.bryanamaya.tecnocinetico.block.ModBlocks;

import java.util.Map;

/** Planos de las estructuras grandes (ver MultiblockTemplate: capas de abajo arriba, filas de delante atrás). */
public final class Structures {

    private Structures() {}

    /** Ensamblador Industrial 3x3x3: núcleo en el centro; el resto, piezas invisibles (lo dibuja el modelo del núcleo). */
    public static final MultiblockTemplate ASSEMBLER = new MultiblockTemplate(new String[][]{
            {"AAA", "AAA", "AAA"},
            {"AAA", "ACA", "AAA"},
            {"AAA", "AAA", "AAA"}},
            () -> ModBlocks.INDUSTRIAL_ASSEMBLER.get(), Map.of('A', () -> ModBlocks.ASSEMBLER_PART.get()));

    /**
     * Reactor de Fisión 9x9x6: suelo y muros de blindaje, ventanas en los 4 lados, la vasija de vidrio 5x5 en el
     * centro (con el núcleo abajo y las barras visibles dentro), tejado con claraboya sobre la vasija y
     * respiraderos en las esquinas. F = blindaje, P = escotilla, V = ventana, G = vidrio de la vasija, X = respiradero.
     */
    public static final MultiblockTemplate REACTOR = new MultiblockTemplate(new String[][]{
            {"FFFFFFFFF", "FFFFFFFFF", "FFFFFFFFF", "FFFFFFFFF", "FFFFFFFFF", "FFFFFFFFF", "FFFFFFFFF", "FFFFFFFFF", "FFFFFFFFF"},
            {"FFFFPFFFF", "F       F", "F FFFFF F", "F FFFFF F", "F FFCFF F", "F FFFFF F", "F FFFFF F", "F       F", "FFFFFFFFF"},
            {"FFFVVVFFF", "F       F", "F GGGGG F", "V G   G V", "V G   G V", "V G   G V", "F GGGGG F", "F       F", "FFFVVVFFF"},
            {"FFFVVVFFF", "F       F", "F GGGGG F", "V G   G V", "V G   G V", "V G   G V", "F GGGGG F", "F       F", "FFFVVVFFF"},
            {"FFFFFFFFF", "F       F", "F GGGGG F", "F G   G F", "F G   G F", "F G   G F", "F GGGGG F", "F       F", "FFFFFFFFF"},
            {"XFFFFFFFX", "FFFFFFFFF", "FFFFFFFFF", "FFFVVVFFF", "FFFVVVFFF", "FFFVVVFFF", "FFFFFFFFF", "FFFFFFFFF", "XFFFFFFFX"}},
            () -> ModBlocks.FISSION_REACTOR.get(), Map.of(
            'F', () -> ModBlocks.REACTOR_CASING.get(), 'P', () -> ModBlocks.REACTOR_PORT.get(),
            'V', () -> ModBlocks.REACTOR_WINDOW.get(), 'G', () -> ModBlocks.REACTOR_VESSEL_GLASS.get(),
            'X', () -> ModBlocks.REACTOR_VENT.get()));
}
