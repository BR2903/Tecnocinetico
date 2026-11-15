package com.bryanamaya.tecnocinetico.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: la isla del End tiembla (despertar del Dragón). Solo cambia la cámara de quien lo recibe. */
public record EndQuakePacket(int ticks, float strength) {

    public static void encode(EndQuakePacket m, FriendlyByteBuf b) {
        b.writeVarInt(m.ticks);
        b.writeFloat(m.strength);
    }

    public static EndQuakePacket decode(FriendlyByteBuf b) {
        return new EndQuakePacket(b.readVarInt(), b.readFloat());
    }

    public static void handle(EndQuakePacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> com.bryanamaya.tecnocinetico.end.ClientEndData.quake(m.ticks, m.strength));
        ctx.get().setPacketHandled(true);
    }
}
