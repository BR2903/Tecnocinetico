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

    // Anclador
    public static final RegistryObject<SoundEvent> ANCHOR_AMBIENT = register("entity.anchor.ambient");
    public static final RegistryObject<SoundEvent> ANCHOR_CHARGE = register("entity.anchor.charge");
    public static final RegistryObject<SoundEvent> ANCHOR_PULL = register("entity.anchor.pull");
    public static final RegistryObject<SoundEvent> ANCHOR_STOMP = register("entity.anchor.stomp");

    // Caza del Núcleo
    public static final RegistryObject<SoundEvent> SHIP_ENGINE = register("entity.core_fighter.engine");
    public static final RegistryObject<SoundEvent> SHIP_CANNON = register("entity.core_fighter.cannon");
    public static final RegistryObject<SoundEvent> SHIP_MISSILE = register("entity.core_fighter.missile");
    public static final RegistryObject<SoundEvent> SHIP_JUMP_CHARGE = register("entity.core_fighter.jump_charge");
    public static final RegistryObject<SoundEvent> SHIP_JUMP = register("entity.core_fighter.jump");

    // Araña de Asedio
    public static final RegistryObject<SoundEvent> SIEGE_ROAR = register("entity.siege_spider.roar");
    public static final RegistryObject<SoundEvent> SIEGE_STEP = register("entity.siege_spider.step");

    // Lanzallamas (rugido del chorro)
    public static final RegistryObject<SoundEvent> FLAMETHROWER = register("item.flamethrower.roar");

    // Armas: disparos propios de cada arma (grabaciones reales en tools/import_sounds.py, energía en sound_gen.py)
    public static final RegistryObject<SoundEvent> REVOLVER_FIRE = register("item.gun.revolver");
    public static final RegistryObject<SoundEvent> SHOTGUN_FIRE = register("item.gun.shotgun");
    public static final RegistryObject<SoundEvent> RIFLE_FIRE = register("item.gun.rifle");
    public static final RegistryObject<SoundEvent> RIFLE_BOLT = register("item.gun.bolt");
    public static final RegistryObject<SoundEvent> SNIPER_FIRE = register("item.gun.sniper");
    public static final RegistryObject<SoundEvent> SHOTGUN_PUMP = register("item.gun.pump");
    public static final RegistryObject<SoundEvent> SEMI_FIRE = register("item.gun.semi");
    public static final RegistryObject<SoundEvent> SMG_FIRE = register("item.gun.smg");
    public static final RegistryObject<SoundEvent> AUTO_FIRE = register("item.gun.auto");
    // Taladro de Combustión (tools/drill_sounds.py)
    public static final RegistryObject<SoundEvent> DRILL_IDLE = register("item.drill.idle");
    public static final RegistryObject<SoundEvent> DRILL_LOOP = register("item.drill.drilling");
    public static final RegistryObject<SoundEvent> DRILL_REV = register("item.drill.rev");
    public static final RegistryObject<SoundEvent> DRILL_BITE = register("item.drill.bite");
    public static final RegistryObject<SoundEvent> DRILL_REFUEL = register("item.drill.refuel");
    public static final RegistryObject<SoundEvent> DRILL_REFUEL_END = register("item.drill.refuel_end");
    public static final RegistryObject<SoundEvent> DRILL_SPUTTER = register("item.drill.sputter");
    // Motosierra: grabaciones reales (CC0, BigSoundBank; tools/chainsaw_sounds.py)
    public static final RegistryObject<SoundEvent> CHAINSAW_START = register("item.chainsaw.start");
    public static final RegistryObject<SoundEvent> CHAINSAW_IDLE = register("item.chainsaw.idle");
    public static final RegistryObject<SoundEvent> CHAINSAW_CUT = register("item.chainsaw.cut");
    public static final RegistryObject<SoundEvent> CHAINSAW_REV = register("item.chainsaw.rev");
    public static final RegistryObject<SoundEvent> CHAINSAW_STOP = register("item.chainsaw.stop");
    public static final RegistryObject<SoundEvent> MINIGUN_FIRE = register("item.gun.minigun");
    public static final RegistryObject<SoundEvent> MINIGUN_SPIN = register("item.gun.minigun_spin");
    public static final RegistryObject<SoundEvent> RAIL_FIRE = register("item.gun.rail");
    public static final RegistryObject<SoundEvent> PULSE_FIRE = register("item.gun.pulse");
    public static final RegistryObject<SoundEvent> ARC_FIRE = register("item.gun.arc");
    public static final RegistryObject<SoundEvent> GRENADE_LAUNCH = register("item.gun.grenade_launch");
    public static final RegistryObject<SoundEvent> ROCKET_LAUNCH = register("item.gun.rocket_launch");
    public static final RegistryObject<SoundEvent> BLADE_WAVE = register("item.gun.blade_wave");

    // Máquinas trabajando (tools/sound_gen.generate_machines)
    public static final RegistryObject<SoundEvent> MACHINE_REFINER = register("block.machine.refiner");
    public static final RegistryObject<SoundEvent> MACHINE_CHARGER = register("block.machine.charger");
    public static final RegistryObject<SoundEvent> MACHINE_FABRICATOR = register("block.machine.fabricator");
    public static final RegistryObject<SoundEvent> MACHINE_REACTION = register("block.machine.reaction");
    public static final RegistryObject<SoundEvent> MACHINE_LAB = register("block.machine.lab");
    public static final RegistryObject<SoundEvent> MACHINE_REACTOR = register("block.machine.reactor");
    public static final RegistryObject<SoundEvent> MACHINE_ASSEMBLER = register("block.machine.assembler");

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
