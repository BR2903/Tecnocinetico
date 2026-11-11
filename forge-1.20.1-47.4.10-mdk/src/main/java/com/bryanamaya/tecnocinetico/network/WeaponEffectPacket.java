package com.bryanamaya.tecnocinetico.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Solo servidor a cliente. No altera daño, impactos ni consumo de munición. */
public record WeaponEffectPacket(int owner, int item, int kind, boolean flash, Vec3 origin, Vec3 end) {
    public static final int TRACER = 0, FLASH = 1, FLAME = 2, ARC = 3, PULSE = 4;
    public static void encode(WeaponEffectPacket m, FriendlyByteBuf b) {
        b.writeVarInt(m.owner); b.writeVarInt(m.item); b.writeVarInt(m.kind); b.writeBoolean(m.flash);
        b.writeDouble(m.origin.x); b.writeDouble(m.origin.y); b.writeDouble(m.origin.z);
        b.writeDouble(m.end.x); b.writeDouble(m.end.y); b.writeDouble(m.end.z);
    }
    public static WeaponEffectPacket decode(FriendlyByteBuf b) {
        return new WeaponEffectPacket(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readBoolean(),
                new Vec3(b.readDouble(), b.readDouble(), b.readDouble()), new Vec3(b.readDouble(), b.readDouble(), b.readDouble()));
    }
    public static void handle(WeaponEffectPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.bryanamaya.tecnocinetico.client.ClientWeaponEffects.receive(m)));
        ctx.get().setPacketHandled(true);
    }
}
