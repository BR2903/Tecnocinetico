package com.bryanamaya.tecnocinetico.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * Partículas de energía cinética. Brillan siempre (no dependen de la luz del entorno)
 * y usan el cian de la cinetrita.
 */
public class KineticParticle extends TextureSheetParticle {

    public enum Kind { TRACER, SPARK, BURST }

    private static final float R = 0.30F, G = 0.88F, B = 1.0F;   // cian cinetrita

    private final SpriteSet sprites;
    private final Kind kind;
    private final float startSize;
    private final float endSize;

    protected KineticParticle(ClientLevel level, double x, double y, double z,
                              double dx, double dy, double dz, SpriteSet sprites, Kind kind) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.kind = kind;
        switch (kind) {
            case TRACER -> {
                lifetime = 5 + random.nextInt(3);
                gravity = 0.0F;
                hasPhysics = false;
                xd = yd = zd = 0;
                startSize = 0.09F;
                endSize = 0.02F;
                setColor(R, G, B);
                pickSprite(sprites);
            }
            case SPARK -> {
                lifetime = 8 + random.nextInt(8);
                gravity = 0.6F;
                friction = 0.88F;
                hasPhysics = true;
                xd = dx;
                yd = dy;
                zd = dz;
                startSize = 0.07F;
                endSize = 0.0F;
                setColor(0.85F, 1.0F, 1.0F); // nace casi blanca y se enfría a cian
                setSpriteFromAge(sprites);
            }
            default -> { // BURST
                lifetime = 7;
                gravity = 0.0F;
                hasPhysics = false;
                xd = yd = zd = 0;
                float scale = dx > 0 ? (float) dx : 1.0F;
                startSize = 0.12F * scale;
                endSize = 0.9F * scale;
                setColor(R, G, B);
                pickSprite(sprites);
            }
        }
        quadSize = startSize;
        alpha = 0.95F;
    }

    @Override
    public void tick() {
        super.tick();
        if (!isAlive()) {
            return;
        }
        float t = (float) age / lifetime;
        quadSize = Mth.lerp(t, startSize, endSize);
        alpha = 0.95F * (1.0F - t * t);
        if (kind == Kind.SPARK) {
            setSpriteFromAge(sprites);
            setColor(Mth.lerp(t, 0.85F, R), Mth.lerp(t, 1.0F, G), B);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0; // brillo propio
    }

    /** Fábrica de partículas (una por tipo). */
    public record Provider(SpriteSet sprites, Kind kind) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new KineticParticle(level, x, y, z, dx, dy, dz, sprites, kind);
        }
    }
}
