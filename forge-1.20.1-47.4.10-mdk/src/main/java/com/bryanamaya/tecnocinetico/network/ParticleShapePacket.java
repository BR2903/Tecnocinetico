package com.bryanamaya.tecnocinetico.network;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Servidor -> cliente: una FIGURA entera de partículas (línea, anillo, disco, curva) en un solo paquete; el cliente
 * calcula los puntos ({@code client/ParticleShapesClient}). Sustituye a los bucles de {@code sendParticles} punto a punto
 * (un paquete por punto y por jugador). Cada punto se comporta como un {@code sendParticles} vanilla con
 * {@code count}/dispersión/velocidad. Se crea con {@link com.bryanamaya.tecnocinetico.util.ParticleShapes}.
 *
 * @param shape LINE: a a b en n tramos (n+1 puntos); RING: centro a, radio r0, n puntos; DISK: centro a, anillos de
 *              r0 a r1 cada dr, n ángulos; CURVE: Bézier a, control b, fin c en n tramos
 * @param jitter desplazamiento al azar de los puntos interiores (rayos)
 */
public record ParticleShapePacket(ParticleOptions particle, int shape, Vec3 a, Vec3 b, Vec3 c, int n, float r0, float r1, float dr,
                                  float jitter, int count, float sx, float sy, float sz, float speed) {

    /** FAN: abanico horizontal desde a hacia la dirección b, ángulos de -n a n pasos de {@code jitter} grados, radios r0..r1 cada dr. */
    public static final int LINE = 0, RING = 1, DISK = 2, CURVE = 3, FAN = 4;

    public static void encode(ParticleShapePacket m, FriendlyByteBuf buf) {
        buf.writeId(BuiltInRegistries.PARTICLE_TYPE, m.particle.getType());
        m.particle.writeToNetwork(buf);
        buf.writeByte(m.shape);
        for (Vec3 v : new Vec3[]{m.a, m.b, m.c}) {
            buf.writeDouble(v.x);
            buf.writeDouble(v.y);
            buf.writeDouble(v.z);
        }
        buf.writeVarInt(m.n);
        buf.writeFloat(m.r0);
        buf.writeFloat(m.r1);
        buf.writeFloat(m.dr);
        buf.writeFloat(m.jitter);
        buf.writeVarInt(m.count);
        buf.writeFloat(m.sx);
        buf.writeFloat(m.sy);
        buf.writeFloat(m.sz);
        buf.writeFloat(m.speed);
    }

    public static ParticleShapePacket decode(FriendlyByteBuf buf) {
        ParticleType<?> type = buf.readById(BuiltInRegistries.PARTICLE_TYPE);
        ParticleOptions particle = read(buf, type);
        int shape = buf.readByte();
        Vec3[] v = new Vec3[3];
        for (int i = 0; i < 3; i++) {
            v[i] = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        }
        int n = Math.max(0, Math.min(buf.readVarInt(), 2048));     // FAN admite 0; las demás figuras llegan con n >= 1
        float r0 = buf.readFloat(), r1 = buf.readFloat(), dr = buf.readFloat(), jitter = buf.readFloat();
        int count = Math.min(buf.readVarInt(), 64);
        return new ParticleShapePacket(particle, shape, v[0], v[1], v[2], n, r0, r1, dr, jitter, count, buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readFloat());
    }

    private static <T extends ParticleOptions> T read(FriendlyByteBuf buf, ParticleType<T> type) {
        return type.getDeserializer().fromNetwork(type, buf);
    }

    public static void handle(ParticleShapePacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.bryanamaya.tecnocinetico.client.ParticleShapesClient.spawn(m)));
        ctx.get().setPacketHandled(true);
    }
}
