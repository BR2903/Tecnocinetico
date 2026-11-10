package com.bryanamaya.tecnocinetico.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Pieza de una estructura grande (Ensamblador Industrial, Reactor). Clic derecho = el del núcleo (interfaz, cubos);
 * romperla desmonta toda la estructura y devuelve el kit. Si `invisible`, solo la dibuja el modelo del núcleo.
 */
public class StructurePartBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final boolean invisible;

    public StructurePartBlock(boolean invisible, boolean glass) {
        super(props(invisible, glass));
        this.invisible = invisible;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    private static Properties props(boolean invisible, boolean glass) {
        Properties p = Properties.of().mapColor(MapColor.METAL).strength(6.0f, 12.0f).requiresCorrectToolForDrops()
                .sound(glass ? SoundType.GLASS : SoundType.NETHERITE_BLOCK).noLootTable();
        return invisible || glass ? p.noOcclusion() : p;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return invisible ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return invisible ? 1.0F : super.getShadeBrightness(state, level, pos);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return invisible;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StructurePartBlockEntity(pos, state);
    }

    @Nullable
    private static BlockPos core(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof StructurePartBlockEntity part ? part.core() : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos core = core(level, pos);
        if (core == null) {
            return InteractionResult.PASS;
        }
        BlockState cs = level.getBlockState(core);
        return cs.use(level, player, hand, new BlockHitResult(hit.getLocation(), hit.getDirection(), core, hit.isInside()));
    }

    /** Elegir bloque con el botón central: da el kit de la estructura. */
    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        if (level instanceof Level l) {
            BlockPos core = core(l, pos);
            if (core != null) {
                return l.getBlockState(core).getCloneItemStack(target, level, core, player);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockPos core = core(level, pos);
            super.onRemove(state, level, pos, newState, moving);
            if (core != null && !MultiblockMachineBlock.isDismantling(level, core) && level.getBlockState(core).getBlock() instanceof MultiblockMachineBlock) {
                level.destroyBlock(core, false);                                    // el núcleo desmonta el resto y da el kit
            }
            return;
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
