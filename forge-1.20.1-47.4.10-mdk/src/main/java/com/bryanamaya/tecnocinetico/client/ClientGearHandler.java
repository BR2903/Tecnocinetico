package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.gear.TacticalGear;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

/** Aplica en el cliente la copia del Equipo Táctico que envía el servidor (solo cliente). */
public final class ClientGearHandler {

    private ClientGearHandler() {}

    public static void apply(CompoundTag tag) {
        var p = Minecraft.getInstance().player;
        if (p != null && tag != null) {
            TacticalGear.of(p).ifPresent(g -> g.load(tag));
        }
    }
}
