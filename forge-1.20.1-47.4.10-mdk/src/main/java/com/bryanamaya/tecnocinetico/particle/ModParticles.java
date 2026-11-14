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

    /** Gota de combustible ardiendo del Lanzallamas (la velocidad la da quien la crea). */
    public static final RegistryObject<SimpleParticleType> FLAME_STREAM =
            PARTICLE_TYPES.register("flame_stream", () -> new SimpleParticleType(true));

    // Lanza Térmica (texturas propias; client/HeatParticle)
    public static final RegistryObject<SimpleParticleType> HEAT_SPARK = PARTICLE_TYPES.register("heat_spark", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> MOLTEN_DROP = PARTICLE_TYPES.register("molten_drop", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> HEAT_FLASH = PARTICLE_TYPES.register("heat_flash", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> HEAT_HAZE = PARTICLE_TYPES.register("heat_haze", () -> new SimpleParticleType(false));

    private ModParticles() {}

    public static void register(IEventBus bus) {
        PARTICLE_TYPES.register(bus);
    }
}
