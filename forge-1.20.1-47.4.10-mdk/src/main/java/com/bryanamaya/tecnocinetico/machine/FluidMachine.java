package com.bryanamaya.tecnocinetico.machine;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/** Máquina con tanques: al hacer clic derecho con un cubo (u otro recipiente) se llenan/vacían sus tanques. */
public interface FluidMachine {
    boolean useContainer(Player player, InteractionHand hand);
}
