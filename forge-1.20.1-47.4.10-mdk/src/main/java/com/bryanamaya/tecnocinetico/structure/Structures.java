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
     * Reactor de Fisión 9x9x6: estructura maciza de piezas invisibles; se ve como UNA máquina gracias a 18 trozos de
     * modelo (tools/reactor_model_gen.py) que dibujan los centros de cada bloque de 3x3x3 (el núcleo, en (4, 1, 4),
     * dibuja el trozo central de abajo). Dentro, la vasija con las barras visibles (client/FissionReactorRenderer).
     */
    public static final MultiblockTemplate REACTOR = new MultiblockTemplate(new String[][]{
            {"SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS"},
            {"SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSCSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS"},
            {"SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS"},
            {"SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS"},
            {"SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS"},
            {"SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS", "SSSSSSSSS"}},
            () -> ModBlocks.FISSION_REACTOR.get(), Map.of('S', () -> ModBlocks.REACTOR_SHELL.get()))
            .withPartState((p, st) -> st.hasProperty(SegmentedPartBlock.SEGMENT)
                    ? st.setValue(SegmentedPartBlock.SEGMENT, SegmentedPartBlock.segmentAt(p[0], p[1], p[2])) : st);
}
