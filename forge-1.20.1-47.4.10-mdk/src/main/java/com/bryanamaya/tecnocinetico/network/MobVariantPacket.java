package com.bryanamaya.tecnocinetico.network;

import com.bryanamaya.tecnocinetico.entity.MobVariants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: este mob es una variante modificada fuera de su dimensión (ver {@link MobVariants}). */
public record MobVariantPacket(int entityId, String kind) {

    public static void encode(MobVariantPacket m, FriendlyByteBuf b) {
        b.writeVarInt(m.entityId);
        b.writeUtf(m.kind, 16);
    }

    public static MobVariantPacket decode(FriendlyByteBuf b) {
        return new MobVariantPacket(b.readVarInt(), b.readUtf(16));
    }

    public static void handle(MobVariantPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.bryanamaya.tecnocinetico.client.ClientMobVariants.apply(m.entityId, m.kind)));
        ctx.get().setPacketHandled(true);
    }
}
