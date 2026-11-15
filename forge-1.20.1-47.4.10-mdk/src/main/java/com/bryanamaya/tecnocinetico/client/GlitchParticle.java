package com.bryanamaya.tecnocinetico.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Fallo de datos del End corrupto: un fragmento de píxeles magenta/violeta que <b>salta</b> de sitio a trompicones
 * (no se desliza: se recoloca de golpe cada pocos ticks), cambia de forma con la edad y parpadea antes de apagarse.
 * Texturas blancas (data_glitch_0..3) teñidas aquí.
 */
public class GlitchParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    protected GlitchParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        lifetime = 8 + random.nextInt(8);
        gravity = 0.0F;
        hasPhysics = false;
        xd = dx * 0.3;
        yd = dy * 0.3;
        zd = dz * 0.3;
        quadSize = 0.06F + random.nextFloat() * 0.08F;
        recolor();
        alpha = 0.95F;
        setSpriteFromAge(sprites);
    }

    private void recolor() {
        if (random.nextBoolean()) {
            setColor(0.75F, 0.30F, 1.0F);                                     // violeta de datos (#C04CFF)
        } else {
            setColor(1.0F, 0.31F, 0.85F);                                     // magenta (#FF4FD8)
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!isAlive()) {
            return;
        }
        setSpriteFromAge(sprites);
        if (random.nextInt(3) == 0) {                                         // salto brusco: glitch
            x += (random.nextDouble() - 0.5) * 0.25;
            y += (random.nextDouble() - 0.5) * 0.25;
            z += (random.nextDouble() - 0.5) * 0.25;
            setPos(x, y, z);
            recolor();
        }
        float t = (float) age / lifetime;
        alpha = t > 0.6F && random.nextBoolean() ? 0.15F : 0.95F * (1.0F - t * t);   // parpadea al final
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new GlitchParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
