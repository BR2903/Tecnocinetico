package com.bryanamaya.tecnocinetico.particle;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Partículas de energía cinética (disparos de cinetrita). */
public final class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Tecnocinetico.MOD_ID);

    /** Punto de energía del haz del disparo. */
    public static final RegistryObject<SimpleParticleType> KINETIC_TRACER =
            PARTICLE_TYPES.register("kinetic_tracer", () -> new SimpleParticleType(true));
    /** Chispa cinética que salta y cae. */
    public static final RegistryObject<SimpleParticleType> KINETIC_SPARK =
            PARTICLE_TYPES.register("kinetic_spark", () -> new SimpleParticleType(true));
    /** Onda expansiva (fogonazo y lugar de impacto). La velocidad X indica su tamaño. */
    public static final RegistryObject<SimpleParticleType> KINETIC_BURST =
            PARTICLE_TYPES.register("kinetic_burst", () -> new SimpleParticleType(true));

    private ModParticles() {}

    public static void register(IEventBus bus) {
        PARTICLE_TYPES.register(bus);
    }
}
