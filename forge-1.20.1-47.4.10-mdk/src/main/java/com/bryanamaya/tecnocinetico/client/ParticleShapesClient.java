package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.network.ParticleShapePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** Dibuja en el cliente las figuras de {@link ParticleShapePacket} (mismo resultado que los sendParticles de antes). */
public final class ParticleShapesClient {

    private ParticleShapesClient() {}

    public static void spawn(ParticleShapePacket m) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        RandomSource r = level.random;
        switch (m.shape()) {
            case ParticleShapePacket.LINE -> {
                if (m.n() <= 0) {
                    return;
                }
                Vec3 path = m.b().subtract(m.a());
                for (int i = 0; i <= m.n(); i++) {
                    Vec3 p = m.a().add(path.scale(i / (double) m.n()));
                    if (m.jitter() > 0 && i > 0 && i < m.n()) {                    // solo LINE usa jitter como temblor
                        double j = m.jitter();
                        p = p.add((r.nextDouble() - 0.5) * j, (r.nextDouble() - 0.5) * j, (r.nextDouble() - 0.5) * j);
                    }
                    point(level, m, p, r);
                }
            }
            case ParticleShapePacket.CURVE -> {
                if (m.n() <= 0) {
                    return;
                }
                for (int i = 0; i <= m.n(); i++) {
                    double t = i / (double) m.n();
                    point(level, m, m.a().scale((1 - t) * (1 - t)).add(m.b().scale(2 * (1 - t) * t)).add(m.c().scale(t * t)), r);
                }
            }
            case ParticleShapePacket.RING -> {
                if (m.n() <= 0) {
                    return;
                }
                for (int i = 0; i < m.n(); i++) {
                    double a = i * Math.PI * 2 / m.n();
                    point(level, m, m.a().add(Math.cos(a) * m.r0(), 0, Math.sin(a) * m.r0()), r);
                }
            }
            case ParticleShapePacket.DISK -> {
                if (m.dr() <= 0 || m.n() <= 0) {
                    return;
                }
                for (int i = 0; i < m.n(); i++) {
                    double a = i * Math.PI * 2 / m.n();
                    for (double d = m.r0(); d <= m.r1() + 1.0E-4; d += m.dr()) {
                        point(level, m, m.a().add(Math.cos(a) * d, 0, Math.sin(a) * d), r);
                    }
                }
            }
            case ParticleShapePacket.FAN -> {
                if (m.dr() <= 0) {
                    return;
                }
                for (int i = -m.n(); i <= m.n(); i++) {
                    Vec3 d = m.b().yRot((float) Math.toRadians(i * m.jitter()));
                    for (double rr = m.r0(); rr <= m.r1() + 1.0E-4; rr += m.dr()) {
                        point(level, m, m.a().add(d.scale(rr)), r);
                    }
                }
            }
            default -> { }
        }
    }

    /** Igual que el cliente vanilla con un paquete de partículas: count 0 = una partícula con velocidad dispersión·speed. */
    private static void point(ClientLevel level, ParticleShapePacket m, Vec3 p, RandomSource r) {
        if (m.count() == 0) {
            level.addParticle(m.particle(), p.x, p.y, p.z, m.sx() * m.speed(), m.sy() * m.speed(), m.sz() * m.speed());
            return;
        }
        for (int k = 0; k < m.count(); k++) {
            level.addParticle(m.particle(), p.x + r.nextGaussian() * m.sx(), p.y + r.nextGaussian() * m.sy(), p.z + r.nextGaussian() * m.sz(),
                    r.nextGaussian() * m.speed(), r.nextGaussian() * m.speed(), r.nextGaussian() * m.speed());
        }
    }
}
