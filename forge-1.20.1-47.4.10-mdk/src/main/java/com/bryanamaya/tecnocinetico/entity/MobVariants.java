package com.bryanamaya.tecnocinetico.entity;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.network.ModNetwork;
import com.bryanamaya.tecnocinetico.network.MobVariantPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Mobs vanilla modificados <b>fuera de su dimensión</b> (los huevos de la pestaña de Tecnocinético): un piglin infernal
 * o un enderman corrupto marcado así conserva sus mejoras, su comportamiento y su aspecto en cualquier mundo. Sin la
 * marca, las mejoras solo existen dentro del Nether/End. La marca va en los datos persistentes (se guarda) y se manda
 * al cliente al empezar a verlo (para el modelo).
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID)
public final class MobVariants {

    public static final String KEY = "TecnoVariant", NETHER = "nether", END = "end";

    private MobVariants() {}

    public static boolean is(Entity e, String kind) {
        return kind.equals(e.getPersistentData().getString(KEY));
    }

    public static void mark(Entity e, String kind) {
        e.getPersistentData().putString(KEY, kind);
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        Entity e = event.getTarget();
        String kind = e.getPersistentData().getString(KEY);
        if (!kind.isEmpty() && event.getEntity() instanceof ServerPlayer sp) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new MobVariantPacket(e.getId(), kind));
        }
    }
}
