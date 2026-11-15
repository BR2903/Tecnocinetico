package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.entity.MobVariants;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** Cliente: apunta la marca de variante en el mob (sus datos persistentes del lado del cliente) para el renderizador. */
public final class ClientMobVariants {

    private ClientMobVariants() {}

    public static void apply(int id, String kind) {
        var level = Minecraft.getInstance().level;
        Entity e = level == null ? null : level.getEntity(id);
        if (e != null) {
            MobVariants.mark(e, kind);
        }
    }
}
