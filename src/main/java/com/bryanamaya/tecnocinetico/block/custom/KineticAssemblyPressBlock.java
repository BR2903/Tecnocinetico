package com.bryanamaya.tecnocinetico.block.custom;

import com.bryanamaya.tecnocinetico.block.entity.KineticAssemblyPressBlockEntity;
import net.minecraft.core.BlockPos;
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

    // 1. VINCULAR LA CARCASA CON LA MEMORIA RAM
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new KineticAssemblyPressBlockEntity(pPos, pState);
    }

    // 2. LÓGICA DE INTERACCIÓN (CLIC DERECHO)
    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide()) { // Solo ejecutamos cálculos en el servidor
            BlockEntity entity = pLevel.getBlockEntity(pPos);

            if (entity instanceof KineticAssemblyPressBlockEntity pressEntity) {
                // Buscamos el inventario interno
                pressEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                    ItemStack itemInHand = pPlayer.getItemInHand(pHand);

                    if (!itemInHand.isEmpty()) {
                        // OPCIÓN A: Intentar meter 1 ítem en el primer hueco vacío
                        for (int i = 0; i < handler.getSlots(); i++) {
                            if (handler.getStackInSlot(i).isEmpty()) {
                                ItemStack toInsert = itemInHand.copy();
                                toInsert.setCount(1); // Solo metemos 1 a la vez
                                handler.insertItem(i, toInsert, false);

                                if (!pPlayer.isCreative()) {
                                    itemInHand.shrink(1); // Le quitamos 1 al jugador
                                }
                                break; // Ya metimos el ítem, salimos del ciclo
                            }
                        }
                    } else {
                        // OPCIÓN B: Si la mano está vacía, sacamos el último ítem que metimos
                        for (int i = handler.getSlots() - 1; i >= 0; i--) {
                            if (!handler.getStackInSlot(i).isEmpty()) {
                                ItemStack extracted = handler.extractItem(i, 1, false);
                                pPlayer.setItemInHand(pHand, extracted);
                                break; // Ya sacamos el ítem, salimos del ciclo
                            }
                        }
                    }
                });
            }
        }
        return InteractionResult.sidedSuccess(pLevel.isClientSide());
    }

    // 3. FÍSICAS DE DESTRUCCIÓN (VOMITAR ÍTEMS AL ROMPER)
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