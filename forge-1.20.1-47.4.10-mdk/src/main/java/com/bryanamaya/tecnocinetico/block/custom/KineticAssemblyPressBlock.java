package com.bryanamaya.tecnocinetico.block.custom;

import com.bryanamaya.tecnocinetico.block.entity.KineticAssemblyPressBlockEntity;
import com.bryanamaya.tecnocinetico.block.entity.PressRecipes;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.skill.PlayerSkills;
import com.bryanamaya.tecnocinetico.skill.Skill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class KineticAssemblyPressBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);
    private static final int ENGINEERING_XP_PER_CRAFT = 6;

    public KineticAssemblyPressBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .requiresCorrectToolForDrops()
                .strength(5.0f, 6.0f)
                .sound(SoundType.ANVIL)
                .noOcclusion()); // El modelo no es un cubo completo: sin esto se ve el suelo transparente
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
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
        pBuilder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new KineticAssemblyPressBlockEntity(pPos, pState);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        ItemStack inHand = pPlayer.getItemInHand(pHand);
        boolean isHammer = inHand.is(ModItems.KINETIC_HAMMER.get());

        // Objetos que no sirven aquí: dejamos que el objeto haga lo suyo (p. ej. colocar un bloque)
        if (!isHammer && !inHand.isEmpty() && !PressRecipes.isIngredient(inHand, false)) {
            return InteractionResult.PASS;
        }
        if (pLevel.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(pLevel.getBlockEntity(pPos) instanceof KineticAssemblyPressBlockEntity press)) {
            return InteractionResult.PASS;
        }

        if (isHammer) {
            int engineering = PlayerSkills.getLevel(pPlayer, Skill.ENGINEERING);
            KineticAssemblyPressBlockEntity.StrikeResult result =
                    press.strike(pLevel, pPos, Skill.pressStrikes(engineering));
            if (result == KineticAssemblyPressBlockEntity.StrikeResult.CRAFTED && pPlayer instanceof ServerPlayer sp) {
                PlayerSkills.addXp(sp, Skill.ENGINEERING, ENGINEERING_XP_PER_CRAFT);
            }
            switch (result) {
                case NO_RECIPE -> pLevel.playSound(null, pPos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.6f, 1.2f);
                case STRUCK -> pLevel.playSound(null, pPos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.5f, 1.5f);
                case CRAFTED -> pLevel.playSound(null, pPos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8f, 1.2f);
            }
            if (result != KineticAssemblyPressBlockEntity.StrikeResult.NO_RECIPE) {
                if (pLevel instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.CRIT, pPos.getX() + 0.5, pPos.getY() + 1.0, pPos.getZ() + 0.5,
                            8, 0.25, 0.1, 0.25, 0.2);
                }
                // Ingeniería: probabilidad de que el golpe no gaste el martillo
                if (!pPlayer.isCreative() && pLevel.random.nextFloat() >= Skill.hammerSaveChance(engineering)) {
                    inHand.hurtAndBreak(1, pPlayer, p -> p.broadcastBreakEvent(pHand));
                }
            }
            return InteractionResult.CONSUME;
        }

        PressInteraction.handle(pLevel, pPos, pPlayer, pHand, press.getItemHandler(),
                KineticAssemblyPressBlockEntity.SLOTS, -1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pState.is(pNewState.getBlock())
                && pLevel.getBlockEntity(pPos) instanceof KineticAssemblyPressBlockEntity press) {
            PressInteraction.dropContents(pLevel, pPos, press.getItemHandler());
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }
}
