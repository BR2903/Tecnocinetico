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
 * Partículas de calor de la Lanza Térmica (texturas propias de Codex). Brillan siempre.
 * <ul>
 *   <li>SPARK: chispa de plasma que salta, cae y se apaga.</li>
 *   <li>DROP: gota fundida que cae y rebota en el suelo.</li>
 *   <li>FLASH: destello del impacto que crece y se desvanece (quieto).</li>
 *   <li>HAZE: calima que sube despacio y se disipa.</li>
 * </ul>
 */
public class HeatParticle extends TextureSheetParticle {

    public enum Kind { SPARK, DROP, FLASH, HAZE }

    private final SpriteSet sprites;
    private final float startSize, endSize, startAlpha;
    private final Kind kind;

    protected HeatParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites, Kind kind) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.kind = kind;
        xd = dx;
        yd = dy;
        zd = dz;
        switch (kind) {
            case SPARK -> {
                lifetime = 8 + random.nextInt(8);
                gravity = 0.5F;
                friction = 0.9F;
                hasPhysics = true;
                startSize = 0.08F;
                endSize = 0.02F;
                startAlpha = 1.0F;
            }
            case DROP -> {
                lifetime = 18 + random.nextInt(10);
                gravity = 1.0F;
                friction = 0.95F;
                hasPhysics = true;
                startSize = 0.07F;
                endSize = 0.05F;
                startAlpha = 1.0F;
            }
            case FLASH -> {
                lifetime = 6;
                gravity = 0;
                hasPhysics = false;
                xd = yd = zd = 0;
                startSize = 0.25F;
                endSize = 0.7F;
                startAlpha = 0.95F;
            }
            default -> {                                                                     // HAZE
                lifetime = 24 + random.nextInt(12);
                gravity = -0.03F;
                friction = 0.92F;
                hasPhysics = false;
                startSize = 0.2F;
                endSize = 0.55F;
                startAlpha = 0.45F;
            }
        }
        quadSize = startSize;
        alpha = startAlpha;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!isAlive()) {
            return;
        }
        float t = (float) age / lifetime;
        quadSize = Mth.lerp(t, startSize, endSize);
        alpha = startAlpha * (kind == Kind.FLASH ? 1 - t : 1 - t * t);
        setSpriteFromAge(sprites);
    }

    @Override
    public int getLightColor(float partial) {
        return kind == Kind.HAZE ? super.getLightColor(partial) : 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Kind kind;

        public Provider(SpriteSet sprites, Kind kind) {
            this.sprites = sprites;
            this.kind = kind;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new HeatParticle(level, x, y, z, dx, dy, dz, sprites, kind);
        }
    }
}
