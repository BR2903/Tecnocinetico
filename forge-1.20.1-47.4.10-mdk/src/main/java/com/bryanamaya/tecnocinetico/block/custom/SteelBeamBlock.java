package com.bryanamaya.tecnocinetico.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Viga de acero con perfil en H (Viga Oxidada): dos alas y un alma, girada según `axis`. No es un cubo entero, así que
 * no tapa a los vecinos (noOcclusion en el registro). Cajas del modelo de Codex (`build/remodelar_bloques/integracion_bloques.json`).
 */
public class SteelBeamBlock extends RotatedPillarBlock {

    private static final VoxelShape Y = Shapes.or(
            Block.box(1, 0, 1, 15, 16, 4),            // ala
            Block.box(6.5, 0, 4, 9.5, 16, 12),        // alma
            Block.box(1, 0, 12, 15, 16, 15));         // ala
    /** Tumbada a lo largo de X: el largo pasa a X y el "fondo" de las alas a Y (alas arriba y abajo, como una viga de verdad). */
    private static final VoxelShape X = Shapes.or(
            Block.box(0, 1, 1, 16, 4, 15),
            Block.box(0, 4, 6.5, 16, 12, 9.5),
            Block.box(0, 12, 1, 16, 15, 15));
    private static final VoxelShape Z = Shapes.or(
            Block.box(1, 1, 0, 15, 4, 16),
            Block.box(6.5, 4, 0, 9.5, 12, 16),
            Block.box(1, 12, 0, 15, 15, 16));

    public SteelBeamBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction.Axis axis = state.getValue(AXIS);
        return axis == Direction.Axis.X ? X : axis == Direction.Axis.Z ? Z : Y;
    }
}
