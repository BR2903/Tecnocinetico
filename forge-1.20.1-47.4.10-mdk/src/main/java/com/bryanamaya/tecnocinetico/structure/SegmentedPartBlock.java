package com.bryanamaya.tecnocinetico.structure;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Pieza de una estructura que se ve como UNA sola máquina: todas las piezas son invisibles salvo unas pocas
 * (SEGMENT 1..18) que dibujan cada una un trozo de 3x3x3 bloques del modelo completo (los modelos de Minecraft no
 * pueden medir más de 3 bloques). SEGMENT 0 = invisible. Ver tools/reactor_model_gen.py.
 */
public class SegmentedPartBlock extends StructurePartBlock {

    public static final IntegerProperty SEGMENT = IntegerProperty.create("segment", 0, 18);

    public SegmentedPartBlock() {
        super(true, false);
        registerDefaultState(defaultBlockState().setValue(SEGMENT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SEGMENT);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(SEGMENT) == 0 ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    /** Número de trozo para la posición local (x, y, z) de la estructura: el centro de cada bloque de 3x3x3. */
    public static int segmentAt(int x, int y, int z) {
        if (x % 3 != 1 || y % 3 != 1 || z % 3 != 1) {
            return 0;
        }
        return 1 + x / 3 + 3 * (z / 3) + 9 * (y / 3);
    }
}
