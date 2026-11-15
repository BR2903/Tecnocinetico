package com.bryanamaya.tecnocinetico.end;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Bloques de la red abandonada de la IA (estructuras del End): rack de servidores, terminal con registros, cristal de datos. */
public final class NetworkBlocks {

    private NetworkBlocks() {}

    /** Bloque orientable (frente hacia el jugador al ponerlo). */
    public static class Facing extends HorizontalDirectionalBlock {
        public Facing(Properties props) {
            super(props);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
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

    /**
     * Terminal de la Red: al usarlo muestra un registro de la IA ({@link #ENTRY}: 0-3 registros de la red, 4-7 del
     * Puerto de Lanzamiento). Los textos están en el lang: {@code lore.tecnocinetico.terminal.<n>.title} y {@code .<n>.1..3}.
     */
    public static class Terminal extends Facing {
        public static final IntegerProperty ENTRY = IntegerProperty.create("entry", 0, 7);
        public static final int LINES = 3;

        public Terminal(Properties props) {
            super(props);
            registerDefaultState(defaultBlockState().setValue(ENTRY, 0));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, ENTRY);
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (!level.isClientSide) {
                int n = state.getValue(ENTRY);
                player.sendSystemMessage(Component.translatable("lore.tecnocinetico.terminal." + n + ".title")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
                for (int i = 1; i <= LINES; i++) {
                    player.sendSystemMessage(Component.literal("  ").append(Component.translatable("lore.tecnocinetico.terminal." + n + "." + i))
                            .withStyle(ChatFormatting.GRAY));
                }
                level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.6F, 1.8F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    /** Cristal de Datos: racimo que brilla (luz en las propiedades del registro). */
    public static class Crystal extends Block {
        private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 13, 13);

        public Crystal(Properties props) {
            super(props);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return SHAPE;
        }
    }
}
