package com.bryanamaya.tecnocinetico.block.custom;

import com.bryanamaya.tecnocinetico.block.entity.KineticAssemblyPressBlockEntity;
import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

public class KineticAssemblyPressBlock extends Block implements EntityBlock {

    public KineticAssemblyPressBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .requiresCorrectToolForDrops()
                .strength(5.0f, 6.0f)
                .sound(SoundType.ANVIL));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new KineticAssemblyPressBlockEntity(pPos, pState);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide()) {
            BlockEntity entity = pLevel.getBlockEntity(pPos);

            if (entity instanceof KineticAssemblyPressBlockEntity pressEntity) {
                ItemStack itemInHand = pPlayer.getItemInHand(pHand);

                // ACCIÓN ESPECIAL: ¿El jugador está usando el martillo cinético?
                if (itemInHand.is(ModItems.KINETIC_HAMMER.get())) {
                    // Ejecutar golpe en el cerebro de la mesa
                    pressEntity.attemptCraft(pLevel, pPos);

                    // Reproducir el sonido metálico de impacto
                    pLevel.playSound(null, pPos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.5f, 1.5f);

                    if (!pPlayer.isCreative()) {
                        itemInHand.hurtAndBreak(1, pPlayer, (player) -> player.broadcastBreakEvent(pHand));
                    }
                    return InteractionResult.SUCCESS;
                }

                // ACCIÓN NORMAL: Colocar o extraer materiales si no es un martillo
                pressEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                    if (!itemInHand.isEmpty()) {
                        for (int i = 0; i < handler.getSlots(); i++) {
                            if (handler.getStackInSlot(i).isEmpty()) {
                                ItemStack toInsert = itemInHand.copy();
                                toInsert.setCount(1);
                                handler.insertItem(i, toInsert, false);

                                if (!pPlayer.isCreative()) {
                                    itemInHand.shrink(1);
                                }
                                break;
                            }
                        }
                    } else {
                        for (int i = handler.getSlots() - 1; i >= 0; i--) {
                            if (!handler.getStackInSlot(i).isEmpty()) {
                                ItemStack extracted = handler.extractItem(i, 1, false);
                                pPlayer.setItemInHand(pHand, extracted);
                                break;
                            }
                        }
                    }
                });
            }
        }
        return InteractionResult.sidedSuccess(pLevel.isClientSide());
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (pState.getBlock() != pNewState.getBlock()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof KineticAssemblyPressBlockEntity pressEntity) {
                pressEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        Containers.dropItemStack(pLevel, pPos.getX(), pPos.getY(), pPos.getZ(), handler.getStackInSlot(i));
                    }
                });
            }
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }
}