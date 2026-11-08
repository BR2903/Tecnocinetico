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

    // Avispas del Enjambre y Nodo de Colmena (tools/sound_gen.py, generate_wasps)
    public static final RegistryObject<SoundEvent> SWARM_WASP_BUZZ = register("entity.swarm_wasp.buzz");
    public static final RegistryObject<SoundEvent> SWARM_WASP_ALARM = register("entity.swarm_wasp.alarm");
    public static final RegistryObject<SoundEvent> SWARM_WASP_STING = register("entity.swarm_wasp.sting");
    public static final RegistryObject<SoundEvent> SWARM_WASP_HURT = register("entity.swarm_wasp.hurt");
    public static final RegistryObject<SoundEvent> SWARM_WASP_DEATH = register("entity.swarm_wasp.death");
    public static final RegistryObject<SoundEvent> HIVE_NODE_AMBIENT = register("entity.hive_node.ambient");
    public static final RegistryObject<SoundEvent> HIVE_NODE_PULSE = register("entity.hive_node.pulse");

    // Torreta Centinela
    public static final RegistryObject<SoundEvent> SENTRY_TURRET_CHARGE = register("entity.sentry_turret.charge");

    // Dron del Núcleo Directriz
    public static final RegistryObject<SoundEvent> DRONE_HUM = register("entity.drone.hum");
    public static final RegistryObject<SoundEvent> DRONE_SCAN = register("entity.drone.scan");
    public static final RegistryObject<SoundEvent> DRONE_ALERT = register("entity.drone.alert");

    // Gadgets del Equipo Táctico
    public static final RegistryObject<SoundEvent> JETPACK_THRUST = register("item.jetpack.thrust");

    // Pistola Cinética
    public static final RegistryObject<SoundEvent> PISTOL_FIRE = register("item.kinetic_pistol.fire");
    public static final RegistryObject<SoundEvent> PISTOL_IMPACT = register("item.kinetic_pistol.impact");
    public static final RegistryObject<SoundEvent> PISTOL_HIT = register("item.kinetic_pistol.hit");
    public static final RegistryObject<SoundEvent> PISTOL_DRY_FIRE = register("item.kinetic_pistol.dry_fire");
    public static final RegistryObject<SoundEvent> PISTOL_MAG_OUT = register("item.kinetic_pistol.mag_out");
    public static final RegistryObject<SoundEvent> PISTOL_MAG_IN = register("item.kinetic_pistol.mag_in");
    public static final RegistryObject<SoundEvent> PISTOL_SLIDE = register("item.kinetic_pistol.slide");

    private ModSounds() {}

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, name)));
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}
