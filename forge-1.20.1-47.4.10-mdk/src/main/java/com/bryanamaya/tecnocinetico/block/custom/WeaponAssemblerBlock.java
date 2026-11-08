package com.bryanamaya.tecnocinetico.block.custom;

import com.bryanamaya.tecnocinetico.block.entity.WeaponAssemblerBlockEntity;
import com.bryanamaya.tecnocinetico.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/** Ensamblador de Armas (tier 3): máquina eléctrica; clic derecho abre su interfaz. */
public class WeaponAssemblerBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** ¿Tiene electricidad suficiente para trabajar? Enciende la luz de estado del modelo. */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public WeaponAssemblerBlock() {
        // Es un cubo completo: no necesita noOcclusion() ni forma personalizada
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .requiresCorrectToolForDrops()
                .strength(6.0f, 6.0f)
                .sound(SoundType.NETHERITE_BLOCK));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, POWERED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new WeaponAssemblerBlockEntity(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pType) {
        if (pLevel.isClientSide() || pType != ModBlockEntities.WEAPON_ASSEMBLER_BE.get()) {
            return null;
        }
        return (level, pos, state, be) -> WeaponAssemblerBlockEntity.tick(level, pos, state, (WeaponAssemblerBlockEntity) be);
    }

    /** Clic derecho: abre la interfaz del ensamblador. */
    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide() && pPlayer instanceof ServerPlayer sp
                && pLevel.getBlockEntity(pPos) instanceof WeaponAssemblerBlockEntity press) {
            NetworkHooks.openScreen(sp, press, pPos);
        }
        return InteractionResult.sidedSuccess(pLevel.isClientSide());
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pState.is(pNewState.getBlock())
                && pLevel.getBlockEntity(pPos) instanceof WeaponAssemblerBlockEntity press) {
            PressInteraction.dropContents(pLevel, pPos, press.getItemHandler());
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }
}
