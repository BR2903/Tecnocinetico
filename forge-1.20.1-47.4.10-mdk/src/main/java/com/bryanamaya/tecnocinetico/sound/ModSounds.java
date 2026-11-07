package com.bryanamaya.tecnocinetico.sound;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Sonidos propios del mod. Los archivos están en assets/tecnocinetico/sounds/ y se declaran en sounds.json.
 * Los del Cuerpo de Enjambre se sintetizan con tools/sound_gen.py.
 */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Tecnocinetico.MOD_ID);

    public static final RegistryObject<SoundEvent> SWARM_BODY_AMBIENT = register("entity.swarm_body.ambient");
    public static final RegistryObject<SoundEvent> SWARM_BODY_HURT = register("entity.swarm_body.hurt");
    public static final RegistryObject<SoundEvent> SWARM_BODY_DEATH = register("entity.swarm_body.death");
    public static final RegistryObject<SoundEvent> SWARM_BODY_STEP = register("entity.swarm_body.step");
    public static final RegistryObject<SoundEvent> SWARM_BODY_LEAP = register("entity.swarm_body.leap");

    private ModSounds() {}

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, name)));
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}
