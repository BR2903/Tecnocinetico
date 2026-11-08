package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.gear.TacticalGear;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

/** Aplica en el cliente la copia del Equipo Táctico que envía el servidor (solo cliente). */
public final class ClientGearHandler {

    private ClientGearHandler() {}

    /** Gadgets puestos de otro jugador (solo se guardan las tres ranuras de gadget, para dibujarlos). */
    public static void applyWorn(int entityId, net.minecraft.world.item.ItemStack face, net.minecraft.world.item.ItemStack back,
                                 net.minecraft.world.item.ItemStack feet) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(entityId) instanceof net.minecraft.world.entity.player.Player p
                && p != Minecraft.getInstance().player) {
            TacticalGear.of(p).ifPresent(g -> {
                g.items().setStackInSlot(TacticalGear.FACE, face);
                g.items().setStackInSlot(TacticalGear.BACK, back);
                g.items().setStackInSlot(TacticalGear.FEET, feet);
            });
        }
    }

    public static void apply(CompoundTag tag) {
        var p = Minecraft.getInstance().player;
        if (p != null && tag != null) {
            TacticalGear.of(p).ifPresent(g -> g.load(tag));
        }
    }
}
