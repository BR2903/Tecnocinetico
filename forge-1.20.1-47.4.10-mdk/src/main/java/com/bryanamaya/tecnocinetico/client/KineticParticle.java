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

    /** TRACER/SPARK/BURST: cinetrita (cian). BULLET_*, MUZZLE_FLASH: bala corriente de pólvora (cálidos). */
    public enum Kind { TRACER, SPARK, BURST, BULLET_TRACER, BULLET_SPARK, MUZZLE_FLASH }

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
            case BULLET_TRACER -> {
                lifetime = 3 + random.nextInt(2);
                gravity = 0.0F;
                hasPhysics = false;
                xd = yd = zd = 0;
                startSize = 0.055F;
                endSize = 0.01F;
                setColor(1.0F, 0.86F, 0.52F);       // estela amarilla pálida de bala trazadora
                pickSprite(sprites);
            }
            case BULLET_SPARK -> {
                lifetime = 5 + random.nextInt(6);
                gravity = 0.8F;
                friction = 0.86F;
                hasPhysics = true;
                xd = dx;
                yd = dy;
                zd = dz;
                startSize = 0.05F;
                endSize = 0.0F;
                setColor(1.0F, 0.95F, 0.7F);        // nace blanca-amarilla y se enfría a naranja
                setSpriteFromAge(sprites);
            }
            case MUZZLE_FLASH -> {
                lifetime = 3;
                gravity = 0.0F;
                hasPhysics = false;
                xd = yd = zd = 0;
                float scale = dx > 0 ? (float) dx : 1.0F;
                startSize = 0.42F * scale;         // destello que se apaga encogiendo (no una onda que se expande)
                endSize = 0.12F * scale;
                setColor(1.0F, 0.78F, 0.38F);
                pickSprite(sprites);
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
        } else if (kind == Kind.BULLET_SPARK) {
            setSpriteFromAge(sprites);
            setColor(1.0F, Mth.lerp(t, 0.95F, 0.45F), Mth.lerp(t, 0.7F, 0.1F));
        } else if (kind == Kind.MUZZLE_FLASH) {
            setColor(1.0F, Mth.lerp(t, 0.78F, 0.45F), Mth.lerp(t, 0.38F, 0.12F));
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
