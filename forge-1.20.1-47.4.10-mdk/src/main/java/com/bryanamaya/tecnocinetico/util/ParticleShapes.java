package com.bryanamaya.tecnocinetico.util;

import com.bryanamaya.tecnocinetico.network.ModNetwork;
import com.bryanamaya.tecnocinetico.network.ParticleShapePacket;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/**
 * Figuras de partículas enviadas en UN paquete a los jugadores cercanos (ver {@link ParticleShapePacket}). Úsalo en vez
 * de un bucle de {@code ServerLevel.sendParticles}: cada llamada a este último es un paquete por punto y por jugador.
 * La reciben los jugadores a menos de 32 bloques de la figura (como {@code sendParticles}).
 */
public final class ParticleShapes {

    private ParticleShapes() {}

    /** Línea de {@code from} a {@code to} en {@code steps} tramos (steps+1 puntos), quietos. */
    public static void line(ServerLevel level, ParticleOptions p, Vec3 from, Vec3 to, int steps) {
        jaggedLine(level, p, from, to, steps, 0.0F);
    }

    /** Línea con los puntos interiores desplazados al azar hasta ±jitter/2 (rayos eléctricos). */
    public static void jaggedLine(ServerLevel level, ParticleOptions p, Vec3 from, Vec3 to, int steps, float jitter) {
        send(level, new ParticleShapePacket(p, ParticleShapePacket.LINE, from, to, Vec3.ZERO, Math.max(1, steps), 0, 0, 0, jitter, 0, 0, 0, 0, 0),
                from.add(to).scale(0.5), from.distanceTo(to) * 0.5);
    }

    /** Línea en la que cada punto es como {@code sendParticles(p, x, y, z, count, sx, sy, sz, speed)}. */
    public static void line(ServerLevel level, ParticleOptions p, Vec3 from, Vec3 to, int steps, int count, double sx, double sy, double sz,
                            double speed) {
        send(level, new ParticleShapePacket(p, ParticleShapePacket.LINE, from, to, Vec3.ZERO, Math.max(1, steps), 0, 0, 0, 0, count,
                (float) sx, (float) sy, (float) sz, (float) speed), from.add(to).scale(0.5), from.distanceTo(to) * 0.5);
    }

    /**
     * Abanico horizontal desde {@code from} hacia {@code dir}: direcciones de -half a +half pasos de {@code stepDeg} grados,
     * con puntos de radio {@code r0} a {@code r1} cada {@code dr} (cada punto como {@code sendParticles} con esos valores).
     */
    public static void fan(ServerLevel level, ParticleOptions p, Vec3 from, Vec3 dir, int half, float stepDeg, double r0, double r1, double dr,
                           int count, double sx, double sy, double sz, double speed) {
        send(level, new ParticleShapePacket(p, ParticleShapePacket.FAN, from, dir, Vec3.ZERO, half, (float) r0, (float) r1, (float) dr, stepDeg,
                count, (float) sx, (float) sy, (float) sz, (float) speed), from, r1);
    }

    /** Curva de Bézier cuadrática {@code from}, {@code control}, {@code to} en {@code steps} tramos. */
    public static void curve(ServerLevel level, ParticleOptions p, Vec3 from, Vec3 control, Vec3 to, int steps) {
        Vec3 mid = from.add(to).scale(0.5);
        send(level, new ParticleShapePacket(p, ParticleShapePacket.CURVE, from, control, to, Math.max(1, steps), 0, 0, 0, 0, 0, 0, 0, 0, 0),
                mid, from.distanceTo(to) * 0.5 + control.distanceTo(mid));
    }

    /** Anillo horizontal de {@code points} puntos quietos. */
    public static void ring(ServerLevel level, ParticleOptions p, Vec3 center, double radius, int points) {
        send(level, new ParticleShapePacket(p, ParticleShapePacket.RING, center, Vec3.ZERO, Vec3.ZERO, points, (float) radius, 0, 0, 0, 0, 0, 0, 0, 0),
                center, radius);
    }

    /**
     * Disco: anillos de radio {@code r0} a {@code r1} cada {@code dr}, con {@code angles} puntos cada uno; cada punto es
     * como {@code sendParticles(p, x, y, z, count, sx, sy, sz, speed)}.
     */
    public static void disk(ServerLevel level, ParticleOptions p, Vec3 center, double r0, double r1, double dr, int angles,
                            int count, double sx, double sy, double sz, double speed) {
        send(level, new ParticleShapePacket(p, ParticleShapePacket.DISK, center, Vec3.ZERO, Vec3.ZERO, angles, (float) r0, (float) r1,
                (float) dr, 0, count, (float) sx, (float) sy, (float) sz, (float) speed), center, r1);
    }

    private static void send(ServerLevel level, ParticleShapePacket packet, Vec3 center, double extent) {
        double radius = Math.min(512.0, extent + 32.0);
        ModNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(center.x, center.y, center.z, radius,
                level.dimension())), packet);
    }
}
