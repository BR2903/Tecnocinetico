package com.bryanamaya.tecnocinetico.item;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.ModEntities;
import com.bryanamaya.tecnocinetico.item.custom.KineticHammerItem;
import com.bryanamaya.tecnocinetico.item.custom.KineticPistolItem;
import com.bryanamaya.tecnocinetico.item.custom.WeaponModule;
import com.bryanamaya.tecnocinetico.item.custom.WeaponModuleItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Tecnocinetico.MOD_ID);

    // --- MIGRACIÓN DEL SISTEMA VIEJO ---
    public static final RegistryObject<Item> RAW_CINETRITE = ITEMS.register("raw_cinetrite",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CINETRITE_INGOT = ITEMS.register("cinetrite_ingot",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CINETRITE_SHARD = ITEMS.register("cinetrite_shard",
            () -> new Item(new Item.Properties()));

    // --- INERCITA (TIER 2: CONTENCIÓN) ---
    public static final RegistryObject<Item> RAW_INERCITA = ITEMS.register("raw_inercita",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> INERCITA_INGOT = ITEMS.register("inercita_ingot",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> RESISTOR_330_OHM = ITEMS.register("resistor_330_ohm",
            () -> new Item(new Item.Properties()));

    // --- EL MARTILLO DE INICIO ---
    public static final RegistryObject<Item> KINETIC_HAMMER = ITEMS.register("kinetic_hammer",
            () -> new KineticHammerItem(new Item.Properties()));

    // --- COMPONENTES TIER 1 (Prensa Manual) ---
    public static final RegistryObject<Item> PIEZO_TRANSDUCER = ITEMS.register("piezo_transducer",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> DEEPSLATE_LOGIC_SUBSTRATE = ITEMS.register("deepslate_logic_substrate",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRUDE_KINETIC_COUPLER = ITEMS.register("crude_kinetic_coupler",
            () -> new Item(new Item.Properties()));

    // --- ARMAMENTO CINÉTICO TIER 1 ---
    public static final RegistryObject<Item> KINETIC_PISTOL = ITEMS.register("kinetic_pistol",
            () -> new KineticPistolItem(new Item.Properties().stacksTo(1).defaultDurability(500)));

    // --- MUNDO POST-APOCALÍPTICO ---
    /** Chatarra: sale de los Montones de Chatarra; se funde en pepitas de hierro (reciclaje). */
    public static final RegistryObject<Item> SCRAP_METAL = ITEMS.register("scrap_metal",
            () -> new Item(new Item.Properties()));

    // --- MÓDULOS DE MEJORA DE ARMAS ---
    public static final RegistryObject<Item> MODULE_OPTIC = module(WeaponModule.OPTIC);
    public static final RegistryObject<Item> MODULE_EXTENDED_MAG = module(WeaponModule.EXTENDED_MAG);
    public static final RegistryObject<Item> MODULE_INERCITA_BARREL = module(WeaponModule.INERCITA_BARREL);
    public static final RegistryObject<Item> MODULE_QUICK_RELOAD = module(WeaponModule.QUICK_RELOAD);
    public static final RegistryObject<Item> MODULE_SEEKER = module(WeaponModule.SEEKER);
    /** Bala Cinética: la munición normal de la pistola (va dentro de un cargador). */
    public static final RegistryObject<Item> KINETIC_ROUND = ITEMS.register("kinetic_round", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MAGAZINE = ITEMS.register("magazine",
            () -> new com.bryanamaya.tecnocinetico.item.custom.MagazineItem(1, 6, new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> EXTENDED_MAGAZINE = ITEMS.register("extended_magazine",
            () -> new com.bryanamaya.tecnocinetico.item.custom.MagazineItem(2, 10, new Item.Properties().stacksTo(16)));
    /** Munición Guiada: solo funciona en una pistola con la Guía de Enjambre (ver KineticPistolItem). */
    public static final RegistryObject<Item> GUIDED_ROUND = ITEMS.register("guided_round",
            () -> new com.bryanamaya.tecnocinetico.item.custom.GuidedRoundItem(new Item.Properties()));

    /** Objeto que corresponde a cada módulo. */
    public static Item moduleItem(WeaponModule module) {
        return switch (module) {
            case OPTIC -> MODULE_OPTIC.get();
            case EXTENDED_MAG -> MODULE_EXTENDED_MAG.get();
            case INERCITA_BARREL -> MODULE_INERCITA_BARREL.get();
            case QUICK_RELOAD -> MODULE_QUICK_RELOAD.get();
            case SEEKER -> MODULE_SEEKER.get();
        };
    }

    private static RegistryObject<Item> module(WeaponModule module) {
        return ITEMS.register("module_" + module.id(),
                () -> new WeaponModuleItem(module, new Item.Properties().stacksTo(16)));
    }

    // --- HUEVOS GENERADORES ---
    public static final RegistryObject<Item> SWARM_BODY_SPAWN_EGG = ITEMS.register("swarm_body_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SWARM_BODY, 0x2B1A24, 0x35E0F0, new Item.Properties()));

    public static final RegistryObject<Item> SWARM_WASP_SPAWN_EGG = ITEMS.register("swarm_wasp_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SWARM_WASP, 0x24252A, 0x3FD8E8, new Item.Properties()));
    public static final RegistryObject<Item> HIVE_NODE_SPAWN_EGG = ITEMS.register("hive_node_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.HIVE_NODE, 0x3A2A1E, 0xC8873E, new Item.Properties()));
    /** Núcleo de Enjambre: lo suelta el Nodo de Colmena. Base de la futura mejora de puntería asistida. */
    public static final RegistryObject<Item> SWARM_CORE = ITEMS.register("swarm_core",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON).stacksTo(16)));
    public static final RegistryObject<Item> SENTRY_TURRET_SPAWN_EGG = ITEMS.register("sentry_turret_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SENTRY_TURRET, 0x26282E, 0xD6A828, new Item.Properties()));
    // Gadgets del Equipo Táctico (batería = durabilidad; ranuras: 7 cara, 8 espalda, 9 pies)
    public static final RegistryObject<Item> NIGHT_VISION_VISOR = ITEMS.register("night_vision_visor",
            () -> new com.bryanamaya.tecnocinetico.gear.GadgetItem(7, 1200, "tooltip.tecnocinetico.night_vision_visor", new Item.Properties()));
    public static final RegistryObject<Item> JETPACK = ITEMS.register("jetpack",
            () -> new com.bryanamaya.tecnocinetico.gear.GadgetItem(8, 1200, "tooltip.tecnocinetico.jetpack", new Item.Properties()));
    public static final RegistryObject<Item> AQUA_PROPULSOR = ITEMS.register("aqua_propulsor",
            () -> new com.bryanamaya.tecnocinetico.gear.GadgetItem(9, 1200, "tooltip.tecnocinetico.aqua_propulsor", new Item.Properties()));
    public static final RegistryObject<Item> SURVIVOR_SPAWN_EGG = ITEMS.register("survivor_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SURVIVOR, 0x5A5236, 0x3FC8D8, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}