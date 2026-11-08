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
 * Chorro del Lanzallamas: una gota de combustible ardiendo. Sale disparada con la velocidad que le da el servidor,
 * cae en arco por su peso, se frena con el aire y crece; su color pasa de blanco-amarillo (recién encendida) a
 * naranja, rojo y humo gris que se desvanece; al final sube (el aire caliente) y al tocar el suelo se esparce.
 * Brilla en la oscuridad mientras es fuego.
 */
public class FlameStreamParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final float spin;
    private final float startSize;

    protected FlameStreamParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.sprites = sprites;
        this.friction = 0.92F;
        this.gravity = 0.035F;
        this.hasPhysics = true;
        this.lifetime = 16 + random.nextInt(9);
        this.startSize = 0.1F + random.nextFloat() * 0.06F;
        this.quadSize = startSize;
        this.spin = (random.nextFloat() - 0.5F) * 0.25F;
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = roll;
        setSpriteFromAge(sprites);
        tint(0.0F);
    }

    private void tint(float k) {
        if (k < 0.12F) {                                            // recién encendida: casi blanca
            setColor(1.0F, 0.96F, 0.78F);
            alpha = 0.95F;
        } else if (k < 0.45F) {                                     // naranja
            float t = (k - 0.12F) / 0.33F;
            setColor(1.0F, Mth.lerp(t, 0.85F, 0.55F), Mth.lerp(t, 0.45F, 0.15F));
            alpha = 0.92F;
        } else if (k < 0.7F) {                                      // rojo oscuro
            float t = (k - 0.45F) / 0.25F;
            setColor(Mth.lerp(t, 1.0F, 0.55F), Mth.lerp(t, 0.55F, 0.18F), Mth.lerp(t, 0.15F, 0.08F));
            alpha = 0.88F;
        } else {                                                    // humo que se desvanece
            float t = (k - 0.7F) / 0.3F;
            setColor(Mth.lerp(t, 0.35F, 0.22F), Mth.lerp(t, 0.2F, 0.21F), Mth.lerp(t, 0.12F, 0.2F));
            alpha = 0.7F * (1.0F - t);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }
        float k = age / (float) lifetime;
        quadSize = startSize + 0.8F * k;
        oRoll = roll;
        roll += spin;
        if (k > 0.45F) {
            gravity = -0.015F;                                      // el aire caliente sube
        }
        if (onGround) {                                             // se esparce por el suelo
            xd *= 1.15;
            zd *= 1.15;
        }
        tint(k);
        setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return age < lifetime * 0.7F ? 0xF000F0 : super.getLightColor(partialTick);   // brilla mientras es fuego
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new FlameStreamParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
