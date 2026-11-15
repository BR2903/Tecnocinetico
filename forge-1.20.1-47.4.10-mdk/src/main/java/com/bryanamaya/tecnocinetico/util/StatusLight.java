package com.bryanamaya.tecnocinetico.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Luz de estado de las máquinas (POWERED) sin parpadeo. Cambiar el estado del bloque obliga al cliente a reconstruir la
 * sección del chunk; si la energía oscila alrededor del consumo (llega justo lo que gasta), la luz cambiaba casi cada
 * tick. Ahora solo cambia cuando el nuevo valor se mantiene {@link #SETTLE} ticks seguidos.
 */
public final class StatusLight {
    public static final int SETTLE = 10;

    private StatusLight() {
    }

    /** Devuelve el contador de ticks pendientes (guárdalo en la máquina y pásalo la próxima vez). */
    public static int update(Level level, BlockPos pos, BlockState state, BooleanProperty prop, boolean want, int pending) {
        if (!state.hasProperty(prop) || state.getValue(prop) == want) {
            return 0;
        }
        if (++pending < SETTLE) {
            return pending;
        }
        level.setBlock(pos, state.setValue(prop, want), Block.UPDATE_CLIENTS);
        return 0;
    }
}
