package com.bryanamaya.tecnocinetico.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Mobiliario urbano destrozado del Overworld: farolas y postes eléctricos, de pie, doblados o caídos, y cables.
 * Los colocan por el mundo {@code worldgen/feature/StreetLampFeature} y {@code UtilityPoleFeature}; también se
 * pueden recoger y colocar. Modelos y texturas: Codex ({@code tools/world_props_gen.py}).
 */
public final class StreetPropBlocks {

    private StreetPropBlocks() {}

    private static VoxelShape box(double x0, double y0, double z0, double x1, double y1, double z1) {
        return Block.box(x0, y0, z0, x1, y1, z1);
    }

    /** Fuste o poste (farola: delgado; poste de luz: más grueso). axis = y de pie, x/z tumbado. */
    public static class Pole extends Block {
        public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
        private final VoxelShape x, y, z;

        public Pole(Properties properties, double width) {
            super(properties);
            double a = 8 - width / 2, b = 8 + width / 2;
            y = box(a, 0, a, b, 16, b);
            x = box(0, a, a, 16, b, b);
            z = box(a, a, 0, b, b, 16);
            registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Y));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(AXIS);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            return defaultBlockState().setValue(AXIS, ctx.getClickedFace().getAxis());
        }

        @Override
        public BlockState rotate(BlockState state, Rotation rotation) {
            Direction.Axis axis = state.getValue(AXIS);
            if (axis != Direction.Axis.Y && (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90)) {
                return state.setValue(AXIS, axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
            }
            return state;
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return switch (state.getValue(AXIS)) {
                case X -> x;
                case Y -> y;
                case Z -> z;
            };
        }
    }

    /** Pieza orientada hacia un lado (cabeza de farola, farola doblada): forma de un cuarto de bloque hacia `facing`. */
    public static class Facing extends HorizontalDirectionalBlock {
        public Facing(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return Shapes.or(box(6, 0, 6, 10, 10, 10), switch (state.getValue(FACING)) {
                case SOUTH -> box(5, 8, 6, 11, 14, 16);
                case EAST -> box(6, 8, 5, 16, 14, 11);
                case WEST -> box(0, 8, 5, 10, 14, 11);
                default -> box(5, 8, 0, 11, 14, 10);
            });
        }

        @Override
        public BlockState rotate(BlockState state, Rotation rotation) {
            return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        }

        @Override
        public BlockState mirror(BlockState state, Mirror mirror) {
            return state.rotate(mirror.getRotation(state.getValue(FACING)));
        }
    }

    /** Cruceta del poste: el poste por el centro y un travesaño de lado a lado (axis = x o z). */
    public static class Crossarm extends Block {
        public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
        private static final VoxelShape POST = box(5, 0, 5, 11, 16, 11);
        private static final VoxelShape X = Shapes.or(POST, box(0, 10, 6, 16, 14, 10)), Z = Shapes.or(POST, box(6, 10, 0, 10, 14, 16));

        public Crossarm(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(AXIS);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            return defaultBlockState().setValue(AXIS, ctx.getHorizontalDirection().getClockWise().getAxis());
        }

        @Override
        public BlockState rotate(BlockState state, Rotation rotation) {
            return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90
                    ? state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X) : state;
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return state.getValue(AXIS) == Direction.Axis.X ? X : Z;
        }
    }

    /** Cables: colgando entre postes (hanging) o tirados en el suelo. Se atraviesan (sin colisión). */
    public static class Cable extends Block {
        public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
        public static final BooleanProperty HANGING = BooleanProperty.create("hanging");
        private static final VoxelShape HANG_X = box(0, 6, 6, 16, 12, 10), HANG_Z = box(6, 6, 0, 10, 12, 16);
        private static final VoxelShape FLOOR = box(0, 0, 0, 16, 2, 16);

        public Cable(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X).setValue(HANGING, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(AXIS, HANGING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            return defaultBlockState().setValue(AXIS, ctx.getHorizontalDirection().getAxis())
                    .setValue(HANGING, ctx.getClickedFace() != Direction.UP);
        }

        @Override
        public BlockState rotate(BlockState state, Rotation rotation) {
            return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90
                    ? state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X) : state;
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            if (!state.getValue(HANGING)) {
                return FLOOR;
            }
            return state.getValue(AXIS) == Direction.Axis.X ? HANG_X : HANG_Z;
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return Shapes.empty();
        }
    }
}
