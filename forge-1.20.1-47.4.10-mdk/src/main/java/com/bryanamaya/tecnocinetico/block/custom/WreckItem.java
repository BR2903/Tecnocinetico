package com.bryanamaya.tecnocinetico.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;

/**
 * Objeto de un vehículo abandonado: coloca el vehículo ENTERO (todas sus celdas) con el frontal hacia donde mira el
 * jugador, empezando en el bloque señalado. Si no cabe o no está apoyado en suelo firme, no hace nada.
 */
public class WreckItem extends BlockItem {

    private final WreckBlock wreck;

    public WreckItem(WreckBlock block, Properties properties) {
        super(block, properties);
        this.wreck = block;
    }

    @Override
    public InteractionResult place(BlockPlaceContext ctx) {
        if (!ctx.canPlace()) {
            return InteractionResult.FAIL;
        }
        Direction facing = ctx.getHorizontalDirection();
        BlockPos origin = ctx.getClickedPos();
        var level = ctx.getLevel();
        if (!wreck.fits(level, origin, facing)) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            wreck.placeAll(level, origin, facing, 3);
            if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) {
                ctx.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
