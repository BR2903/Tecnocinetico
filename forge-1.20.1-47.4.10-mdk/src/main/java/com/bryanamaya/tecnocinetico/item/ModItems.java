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
    public static final RegistryObject<Item> MODULE_CHOKE = module(WeaponModule.CHOKE);
    public static final RegistryObject<Item> MODULE_SAWED_OFF = module(WeaponModule.SAWED_OFF);
    public static final RegistryObject<Item> MODULE_SPEED_LOADER = module(WeaponModule.SPEED_LOADER);
    public static final RegistryObject<Item> MODULE_SCOPE = module(WeaponModule.SCOPE);
    public static final RegistryObject<Item> MODULE_POLISHED_BOLT = module(WeaponModule.POLISHED_BOLT);
    public static final RegistryObject<Item> MODULE_RIFLE_BARREL = module(WeaponModule.RIFLE_BARREL);
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
            case CHOKE -> MODULE_CHOKE.get();
            case SAWED_OFF -> MODULE_SAWED_OFF.get();
            case SPEED_LOADER -> MODULE_SPEED_LOADER.get();
            case SCOPE -> MODULE_SCOPE.get();
            case POLISHED_BOLT -> MODULE_POLISHED_BOLT.get();
            case RIFLE_BARREL -> MODULE_RIFLE_BARREL.get();
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
    public static final RegistryObject<Item> CORRUPTED_SPAWN_EGG = ITEMS.register("corrupted_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CORRUPTED, 0x5C6A66, 0x34C8DC, new Item.Properties()));
    public static final RegistryObject<Item> CORRUPTED_GUNNER_SPAWN_EGG = ITEMS.register("corrupted_gunner_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CORRUPTED_GUNNER, 0x5A4232, 0x34C8DC, new Item.Properties()));
    public static final RegistryObject<Item> CORRUPTED_SPIDER_SPAWN_EGG = ITEMS.register("corrupted_spider_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CORRUPTED_SPIDER, 0x1C1B20, 0x34C8DC, new Item.Properties()));
    public static final RegistryObject<Item> DRONE_SPAWN_EGG = ITEMS.register("drone_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.DRONE, 0x2A2D34, 0x46D2E6, new Item.Properties()));
    public static final RegistryObject<Item> SENTRY_TURRET_SPAWN_EGG = ITEMS.register("sentry_turret_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SENTRY_TURRET, 0x26282E, 0xD6A828, new Item.Properties()));
    // Gadgets del Equipo Táctico (batería = durabilidad; ranuras: cara, espalda, pies)
    public static final RegistryObject<Item> NIGHT_VISION_VISOR = ITEMS.register("night_vision_visor",
            () -> new com.bryanamaya.tecnocinetico.gear.GadgetItem(com.bryanamaya.tecnocinetico.gear.TacticalGear.FACE, 1200, "tooltip.tecnocinetico.night_vision_visor", new Item.Properties()));
    public static final RegistryObject<Item> JETPACK = ITEMS.register("jetpack",
            () -> new com.bryanamaya.tecnocinetico.gear.JetpackItem(new Item.Properties()));
    public static final RegistryObject<Item> AQUA_PROPULSOR = ITEMS.register("aqua_propulsor",
            () -> new com.bryanamaya.tecnocinetico.gear.GadgetItem(com.bryanamaya.tecnocinetico.gear.TacticalGear.FEET, 1200, "tooltip.tecnocinetico.aqua_propulsor", new Item.Properties()));
    // Recargas de cada gadget y kits de mejora del jetpack
    public static final RegistryObject<Item> VISOR_BATTERY = ITEMS.register("visor_battery",
            () -> new com.bryanamaya.tecnocinetico.gear.RechargeItem(() -> NIGHT_VISION_VISOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> JETPACK_FUEL = ITEMS.register("jetpack_fuel",
            () -> new com.bryanamaya.tecnocinetico.gear.RechargeItem(() -> JETPACK.get(), new Item.Properties()));
    public static final RegistryObject<Item> PROPULSOR_CELL = ITEMS.register("propulsor_cell",
            () -> new com.bryanamaya.tecnocinetico.gear.RechargeItem(() -> AQUA_PROPULSOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> JETPACK_KIT_2 = ITEMS.register("jetpack_kit_2",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Item> JETPACK_KIT_3 = ITEMS.register("jetpack_kit_3",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    // ---------------------------------------------------------------- tier 1: armadura y armas
    public static final RegistryObject<Item> SCRAP_HELMET = ITEMS.register("scrap_helmet",
            () -> new com.bryanamaya.tecnocinetico.item.armor.ScrapArmorItem(net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> SCRAP_CHESTPLATE = ITEMS.register("scrap_chestplate",
            () -> new com.bryanamaya.tecnocinetico.item.armor.ScrapArmorItem(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> SCRAP_LEGGINGS = ITEMS.register("scrap_leggings",
            () -> new com.bryanamaya.tecnocinetico.item.armor.ScrapArmorItem(net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> SCRAP_BOOTS = ITEMS.register("scrap_boots",
            () -> new com.bryanamaya.tecnocinetico.item.armor.ScrapArmorItem(net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));
    public static final RegistryObject<Item> SCRAP_MACHETE = ITEMS.register("scrap_machete",
            () -> new com.bryanamaya.tecnocinetico.item.weapon.MacheteItem(new Item.Properties()));
    public static final RegistryObject<Item> SHOTGUN_SHELL = ITEMS.register("shotgun_shell", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RIFLE_ROUND = ITEMS.register("rifle_round", () -> new Item(new Item.Properties()));
    /** Escopeta de caño: 2 cartuchos, 8 perdigones de 2.5 (20 de cerca), mucha dispersión, 14 bloques. */
    public static final RegistryObject<Item> PIPE_SHOTGUN = ITEMS.register("pipe_shotgun",
            () -> new com.bryanamaya.tecnocinetico.item.weapon.LongGunItem(com.bryanamaya.tecnocinetico.item.custom.WeaponModule.Family.SHOTGUN,
                    new com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.Stats(
                    2, () -> SHOTGUN_SHELL.get(), 8, 2.5F, 14.0D, 7.0F, 5.0F, 14, 16, 9.0F, 3.0F, 0.85F, 0.65F), new Item.Properties()));
    /** Rifle de cerrojo: 5 balas, 16 de daño, 60 bloques, preciso; 24 ticks entre disparos (cerrojo). */
    public static final RegistryObject<Item> BOLT_RIFLE = ITEMS.register("bolt_rifle",
            () -> new com.bryanamaya.tecnocinetico.item.weapon.LongGunItem(com.bryanamaya.tecnocinetico.item.custom.WeaponModule.Family.RIFLE,
                    new com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.Stats(
                    5, () -> RIFLE_ROUND.get(), 1, 16.0F, 60.0D, 1.2F, 0.0F, 24, 14, 7.0F, 1.2F, 0.55F, 0.8F), new Item.Properties()));
    public static final RegistryObject<Item> SURVIVOR_SPAWN_EGG = ITEMS.register("survivor_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SURVIVOR, 0x5A5236, 0x3FC8D8, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}