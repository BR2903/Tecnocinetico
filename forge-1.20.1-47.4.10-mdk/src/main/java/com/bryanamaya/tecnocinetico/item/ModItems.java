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

    // --- METALES COMUNES (plomo, estaño, titanio) y POLVOS del Refinador Cinético ---
    public static final RegistryObject<Item> RAW_LEAD = simple("raw_lead");
    public static final RegistryObject<Item> LEAD_INGOT = simple("lead_ingot");
    public static final RegistryObject<Item> LEAD_NUGGET = simple("lead_nugget");
    public static final RegistryObject<Item> RAW_TIN = simple("raw_tin");
    public static final RegistryObject<Item> TIN_INGOT = simple("tin_ingot");
    public static final RegistryObject<Item> TIN_NUGGET = simple("tin_nugget");
    public static final RegistryObject<Item> RAW_TITANIUM = reactorOnly("raw_titanium");
    public static final RegistryObject<Item> TITANIUM_INGOT = simple("titanium_ingot");
    public static final RegistryObject<Item> TITANIUM_NUGGET = simple("titanium_nugget");
    public static final RegistryObject<Item> RAW_URANIUM = radioactive("raw_uranium", true);
    public static final RegistryObject<Item> URANIUM_INGOT = radioactive("uranium_ingot", false);
    public static final RegistryObject<Item> URANIUM_NUGGET = radioactive("uranium_nugget", false);
    public static final RegistryObject<Item> URANIUM_FUEL_ROD = radioactive("uranium_fuel_rod", false);
    public static final RegistryObject<Item> DEPLETED_FUEL_ROD = radioactive("depleted_fuel_rod", true);
    public static final RegistryObject<Item> REACTION_CHAMBER_KIT = ITEMS.register("reaction_chamber",
            () -> new com.bryanamaya.tecnocinetico.structure.StructureKitItem(() -> com.bryanamaya.tecnocinetico.block.ModBlocks.REACTION_CHAMBER.get(),
                    "2x2x2", new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Item> INDUSTRIAL_ASSEMBLER_KIT = ITEMS.register("industrial_assembler",
            () -> new com.bryanamaya.tecnocinetico.structure.StructureKitItem(() -> com.bryanamaya.tecnocinetico.block.ModBlocks.INDUSTRIAL_ASSEMBLER.get(),
                    "3x3x3", new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> FISSION_REACTOR_KIT = ITEMS.register("fission_reactor_kit",
            () -> new com.bryanamaya.tecnocinetico.structure.StructureKitItem(() -> com.bryanamaya.tecnocinetico.block.ModBlocks.FISSION_REACTOR.get(),
                    "9x9x6", new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final RegistryObject<Item> IRON_DUST = simple("iron_dust");
    public static final RegistryObject<Item> GOLD_DUST = simple("gold_dust");
    public static final RegistryObject<Item> COPPER_DUST = simple("copper_dust");
    public static final RegistryObject<Item> LEAD_DUST = simple("lead_dust");
    public static final RegistryObject<Item> TIN_DUST = simple("tin_dust");
    public static final RegistryObject<Item> TITANIUM_DUST = reactorOnly("titanium_dust");
    public static final RegistryObject<Item> CINETRITE_DUST = simple("cinetrite_dust");
    public static final RegistryObject<Item> INERCITA_DUST = simple("inercita_dust");
    public static final RegistryObject<Item> PIROXITE_DUST = ITEMS.register("piroxite_dust", () -> new Item(new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> NULLITE_DUST = simple("nullite_dust");

    /** Titanio y uranio: solo se procesan en la Cámara de Reacción (lo dice la descripción). */
    private static RegistryObject<Item> reactorOnly(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()) {
            @Override
            public void appendHoverText(net.minecraft.world.item.ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level,
                                        java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
                tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.tecnocinetico.reactor_only")
                        .withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
            }
        });
    }

    /** Uranio: radiactivo (ver effect/RadiationEvents); el que está en bruto además solo va a la Cámara de Reacción. */
    private static RegistryObject<Item> radioactive(String name, boolean reactorOnly) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()) {
            @Override
            public void appendHoverText(net.minecraft.world.item.ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level,
                                        java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
                tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.tecnocinetico.radioactive")
                        .withStyle(net.minecraft.ChatFormatting.GREEN));
                if (reactorOnly) {
                    tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.tecnocinetico.reactor_only")
                            .withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
                }
            }
        });
    }

    private static RegistryObject<Item> simple(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

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
    public static final RegistryObject<Item> KINETIC_ROUND = ITEMS.register("kinetic_round",
            () -> new com.bryanamaya.tecnocinetico.item.custom.AmmoPackItem(6, new Item.Properties()));          // paquete de 6
    public static final RegistryObject<Item> MAGAZINE = ITEMS.register("magazine",
            () -> new com.bryanamaya.tecnocinetico.item.custom.MagazineItem(1, 12, () -> KINETIC_ROUND.get(), new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> EXTENDED_MAGAZINE = ITEMS.register("extended_magazine",
            () -> new com.bryanamaya.tecnocinetico.item.custom.MagazineItem(2, 18, () -> KINETIC_ROUND.get(), new Item.Properties().stacksTo(16)));
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
    // ---- Nether (parte 2): enemigos nuevos y lo que sueltan
    public static final RegistryObject<Item> SLAG_MITE_SPAWN_EGG = ITEMS.register("slag_mite_spawn_egg",
            () -> new ForgeSpawnEggItem(com.bryanamaya.tecnocinetico.nether.NetherEntities.MITE, 0x352B29, 0xFF791B, new Item.Properties()));
    public static final RegistryObject<Item> ASH_SENTINEL_SPAWN_EGG = ITEMS.register("ash_sentinel_spawn_egg",
            () -> new ForgeSpawnEggItem(com.bryanamaya.tecnocinetico.nether.NetherEntities.SENTINEL, 0x292C30, 0xF5A72A, new Item.Properties()));
    /** Azufre industrial (lo sueltan los ácaros) y Aleación de escoria (los centinelas): materiales del equipo del Nether. */
    public static final RegistryObject<Item> SULFUR_DUST = ITEMS.register("sulfur_dust", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SLAG_ALLOY = ITEMS.register("slag_alloy", () -> new Item(new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> CORRUPTED_SPAWN_EGG = ITEMS.register("corrupted_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CORRUPTED, 0x5C6A66, 0x34C8DC, new Item.Properties()));
    public static final RegistryObject<Item> CORRUPTED_GUNNER_SPAWN_EGG = ITEMS.register("corrupted_gunner_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CORRUPTED_GUNNER, 0x5A4232, 0x34C8DC, new Item.Properties()));
    public static final RegistryObject<Item> CORRUPTED_SPIDER_SPAWN_EGG = ITEMS.register("corrupted_spider_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CORRUPTED_SPIDER, 0x1C1B20, 0x34C8DC, new Item.Properties()));
    public static final RegistryObject<Item> CORRUPTED_CAVE_SPIDER_SPAWN_EGG = ITEMS.register("corrupted_cave_spider_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CORRUPTED_CAVE_SPIDER, 0x1C1B20, 0x7FE6F0, new Item.Properties()));
    public static final RegistryObject<Item> CORRUPTED_DROWNED_SPAWN_EGG = ITEMS.register("corrupted_drowned_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CORRUPTED_DROWNED, 0x3E6A70, 0x34C8DC, new Item.Properties()));
    public static final RegistryObject<Item> CORRUPTED_CREEPER_SPAWN_EGG = ITEMS.register("corrupted_creeper_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CORRUPTED_CREEPER, 0x384238, 0x46D2E6, new Item.Properties()));
    public static final RegistryObject<Item> ANCHOR_SPAWN_EGG = ITEMS.register("anchor_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ANCHOR, 0x40465A, 0x6E7CE6, new Item.Properties()));
    public static final RegistryObject<Item> SIEGE_SPIDER_SPAWN_EGG = ITEMS.register("siege_spider_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SIEGE_SPIDER, 0x40465A, 0x46E3F5, new Item.Properties()));
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
    // Equipo de preparación para el Nether (nether/NetherHazards): calor, ceniza y empuje
    public static final RegistryObject<Item> THERMAL_COOLER = ITEMS.register("thermal_cooler",
            () -> new com.bryanamaya.tecnocinetico.gear.NetherGadgetItem(com.bryanamaya.tecnocinetico.gear.TacticalGear.BACK, 1800, "tooltip.tecnocinetico.thermal_cooler", new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> ASH_FILTER = ITEMS.register("ash_filter",
            () -> new com.bryanamaya.tecnocinetico.gear.NetherGadgetItem(com.bryanamaya.tecnocinetico.gear.TacticalGear.FACE, 1500, "tooltip.tecnocinetico.ash_filter", new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> ANCHOR_BOOTS = ITEMS.register("anchor_boots",
            () -> new com.bryanamaya.tecnocinetico.gear.NetherGadgetItem(com.bryanamaya.tecnocinetico.gear.TacticalGear.FEET, 1400, "tooltip.tecnocinetico.anchor_boots", new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> COOLANT_PACK = ITEMS.register("coolant_pack",
            () -> new com.bryanamaya.tecnocinetico.gear.RechargeItem(() -> THERMAL_COOLER.get(), new Item.Properties()));
    public static final RegistryObject<Item> FILTER_CELL = ITEMS.register("filter_cell",
            () -> new com.bryanamaya.tecnocinetico.gear.RechargeItem(() -> ASH_FILTER.get(), new Item.Properties()));
    public static final RegistryObject<Item> ANCHOR_CELL = ITEMS.register("anchor_cell",
            () -> new com.bryanamaya.tecnocinetico.gear.RechargeItem(() -> ANCHOR_BOOTS.get(), new Item.Properties()));
    // Fundidor del Abismo: botín
    public static final RegistryObject<Item> NETHER_COUPLER = ITEMS.register("nether_coupler",
            () -> new com.bryanamaya.tecnocinetico.item.custom.NetherCouplerItem(new Item.Properties().stacksTo(16).fireResistant()
                    .rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final RegistryObject<Item> ABYSSAL_COMPONENT = ITEMS.register("abyssal_component",
            () -> new Item(new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> ABYSS_FOUNDRY_SPAWN_EGG = ITEMS.register("abyss_foundry_spawn_egg",
            () -> new ForgeSpawnEggItem(com.bryanamaya.tecnocinetico.nether.NetherEntities.FOUNDRY, 0x393B3D, 0xFF4F11, new Item.Properties()));
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
    /** Munición en paquetes (el objeto es 1; el modelo muestra varias): cartuchos 6, balas de rifle 5, cinta 15. */
    public static final RegistryObject<Item> SHOTGUN_SHELL = ITEMS.register("shotgun_shell",
            () -> new com.bryanamaya.tecnocinetico.item.custom.AmmoPackItem(6, new Item.Properties()));
    public static final RegistryObject<Item> RIFLE_ROUND = ITEMS.register("rifle_round",
            () -> new com.bryanamaya.tecnocinetico.item.custom.AmmoPackItem(5, new Item.Properties()));
    /** Cinta de Balas de Rifle: 3 paquetes de rifle encadenados (15). Llena la Caja de Cinta y la de la Minigun. */
    public static final RegistryObject<Item> RIFLE_BELT = ITEMS.register("rifle_belt",
            () -> new com.bryanamaya.tecnocinetico.item.custom.AmmoPackItem(15, new Item.Properties()));
    // cargadores de las armas largas (ver item/weapon/GunMagazines)
    public static final RegistryObject<Item> STRIPPER_CLIP = ITEMS.register("stripper_clip",
            () -> new com.bryanamaya.tecnocinetico.item.custom.MagazineItem(0, 5, () -> RIFLE_ROUND.get(), new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> SHORT_RIFLE_MAGAZINE = ITEMS.register("short_rifle_magazine",
            () -> new com.bryanamaya.tecnocinetico.item.custom.MagazineItem(0, 10, () -> RIFLE_ROUND.get(), new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> RIFLE_MAGAZINE = ITEMS.register("rifle_magazine",
            () -> new com.bryanamaya.tecnocinetico.item.custom.MagazineItem(0, 30, () -> RIFLE_ROUND.get(), new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> BELT_BOX = ITEMS.register("belt_box",
            () -> new com.bryanamaya.tecnocinetico.item.custom.MagazineItem(0, 60, () -> RIFLE_BELT.get(), new Item.Properties().stacksTo(8)));
    public static final RegistryObject<Item> MINIGUN_BOX = ITEMS.register("minigun_box",
            () -> new com.bryanamaya.tecnocinetico.item.custom.MagazineItem(0, 180, () -> RIFLE_BELT.get(), new Item.Properties().stacksTo(4)));
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

    // ---------------------------------------------------------------- tier 2 (piezas de la Prensa Automática)
    public static final RegistryObject<Item> INERCITA_PLATE = ITEMS.register("inercita_plate", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PRECISION_MECHANISM = ITEMS.register("precision_mechanism", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> KINETIC_SERVO = ITEMS.register("kinetic_servo", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BALLISTIC_FABRIC = ITEMS.register("ballistic_fabric", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> INERCITA_HELMET = ITEMS.register("inercita_helmet",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.INERCITA, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> INERCITA_CHESTPLATE = ITEMS.register("inercita_chestplate",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.INERCITA, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> INERCITA_LEGGINGS = ITEMS.register("inercita_leggings",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.INERCITA, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> INERCITA_BOOTS = ITEMS.register("inercita_boots",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.INERCITA, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));
    // Equipo de protección: traje antirradiación (forrado de plomo)
    public static final RegistryObject<Item> HAZMAT_HELMET = ITEMS.register("hazmat_helmet",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.HAZMAT,
                    net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> HAZMAT_CHESTPLATE = ITEMS.register("hazmat_chestplate",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.HAZMAT,
                    net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> HAZMAT_LEGGINGS = ITEMS.register("hazmat_leggings",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.HAZMAT,
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> HAZMAT_BOOTS = ITEMS.register("hazmat_boots",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.HAZMAT,
                    net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));
    public static final RegistryObject<Item> TACTICAL_HELMET = ITEMS.register("tactical_helmet",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.TACTICAL, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> TACTICAL_CHESTPLATE = ITEMS.register("tactical_chestplate",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.TACTICAL, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> TACTICAL_LEGGINGS = ITEMS.register("tactical_leggings",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.TACTICAL, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> TACTICAL_BOOTS = ITEMS.register("tactical_boots",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.TACTICAL, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));
    public static final RegistryObject<Item> EXO_HELMET = ITEMS.register("exo_helmet",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.EXO, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> EXO_CHESTPLATE = ITEMS.register("exo_chestplate",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.EXO, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> EXO_LEGGINGS = ITEMS.register("exo_leggings",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.EXO, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> EXO_BOOTS = ITEMS.register("exo_boots",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.EXO, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));
    /** Escopeta de corredera: 6 cartuchos, 8 perdigones de 2.8, 18 bloques, bombea entre disparos. */
    public static final RegistryObject<Item> PUMP_SHOTGUN = ITEMS.register("pump_shotgun",
            () -> new com.bryanamaya.tecnocinetico.item.weapon.LongGunItem(com.bryanamaya.tecnocinetico.item.custom.WeaponModule.Family.SHOTGUN, com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.Action.PUMP, new com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.Stats(
                    6, () -> SHOTGUN_SHELL.get(), 8, 2.8F, 18.0D, 6.0F, 4.0F, 16, 12, 8.0F, 2.5F, 0.85F, 0.75F), new Item.Properties()));
    /** Rifle semiautomático: cargador de 10, 13 de daño, 60 bloques, 6 ticks entre disparos. */
    public static final RegistryObject<Item> SEMI_RIFLE = ITEMS.register("semi_rifle",
            () -> new com.bryanamaya.tecnocinetico.item.weapon.LongGunItem(com.bryanamaya.tecnocinetico.item.custom.WeaponModule.Family.RIFLE, com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.Action.SEMI, new com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.Stats(
                    10, () -> RIFLE_ROUND.get(), 1, 13.0F, 60.0D, 1.6F, 0.15F, 6, 50, 4.5F, 1.0F, 0.55F, 0.95F), new Item.Properties()));
    /** Subfusil: automático, Cargador Ampliado (18 balas cinéticas), 3.5 de daño, 10 disparos/s; la ráfaga abre el grupo. */
    public static final RegistryObject<Item> KINETIC_SMG = ITEMS.register("kinetic_smg",
            () -> new com.bryanamaya.tecnocinetico.item.weapon.LongGunItem(com.bryanamaya.tecnocinetico.item.custom.WeaponModule.Family.SMG, com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.Action.AUTO, new com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.Stats(
                    18, () -> KINETIC_ROUND.get(), 1, 3.5F, 40.0D, 3.5F, 1.6F, 2, 40, 1.1F, 0.9F, 0.85F, 1.25F), new Item.Properties()));

    // ---------------------------------------------------------------- tier 3 (Ensamblador de Armas + Procesador del Núcleo)
    public static final RegistryObject<Item> CORE_PROCESSOR = ITEMS.register("core_processor",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> RAIL_SLUG = ITEMS.register("rail_slug", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> KINETIC_GRENADE = ITEMS.register("kinetic_grenade",
            () -> new com.bryanamaya.tecnocinetico.item.weapon.KineticGrenadeItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> PULSE_CELL = ITEMS.register("pulse_cell", () -> new Item(new Item.Properties().stacksTo(8)));
    public static final RegistryObject<Item> EMPTY_PULSE_CELL = ITEMS.register("empty_pulse_cell", () -> new Item(new Item.Properties().stacksTo(8)));
    /** Rifle de Riel: carga 1,5 s (mantener el clic), 32 de daño a carga completa, atraviesa 4 enemigos, 100 bloques. */
    public static final RegistryObject<Item> RAIL_RIFLE = ITEMS.register("rail_rifle",
            () -> new com.bryanamaya.tecnocinetico.item.weapon.RailRifleItem(new com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.Stats(
                    5, () -> RIFLE_ROUND.get(), 1, 32.0F, 100.0D, 0.6F, 0.0F, 20, 60, 6.0F, 1.0F, 0.45F, 0.5F),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    /** Fusil de asalto: automático, 30 balas de rifle, 7 de daño, 70 bloques, ~7 disparos/s. */
    public static final RegistryObject<Item> ASSAULT_RIFLE = ITEMS.register("assault_rifle",
            () -> new com.bryanamaya.tecnocinetico.item.weapon.LongGunItem(com.bryanamaya.tecnocinetico.item.custom.WeaponModule.Family.HEAVY, com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.Action.AUTO, new com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.Stats(
                    30, () -> RIFLE_ROUND.get(), 1, 7.0F, 70.0D, 2.8F, 0.7F, 3, 50, 1.8F, 1.2F, 0.7F, 1.05F),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    /** Lanzagranadas: tambor de 4 Granadas Cinéticas (rebotan, estallan sin romper bloques). */
    public static final RegistryObject<Item> GRENADE_LAUNCHER = ITEMS.register("grenade_launcher",
            () -> new com.bryanamaya.tecnocinetico.item.weapon.GrenadeLauncherItem(new com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.Stats(
                    4, () -> KINETIC_GRENADE.get(), 1, 0.0F, 0.0D, 1.5F, 0.6F, 16, 18, 5.0F, 1.5F, 0.85F, 0.6F),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    /** Cañón de Impulso: cono de onda (8 bloques) que hiere, lanza y aturde; 6 disparos por Célula de Impulso. */
    public static final RegistryObject<Item> PULSE_CANNON = ITEMS.register("pulse_cannon",
            () -> new com.bryanamaya.tecnocinetico.item.weapon.PulseCannonItem(new com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.Stats(
                    6, () -> PULSE_CELL.get(), 1, 12.0F, 8.0D, 0.0F, 0.0F, 14, 30, 6.0F, 2.0F, 0.9F, 1.0F),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    // ---------------------------------------------------------------- materiales del Nether y el End (lore)
    public static final RegistryObject<Item> RAW_PIROXITE = ITEMS.register("raw_piroxite", () -> new Item(new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> PIROXITE_INGOT = ITEMS.register("piroxite_ingot", () -> new Item(new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> RAW_NULLITE = ITEMS.register("raw_nullite", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> NULLITE_INGOT = ITEMS.register("nullite_ingot", () -> new Item(new Item.Properties()));
    // ---------------------------------------------------------------- armaduras tier 3 (una por dimensión)
    public static final RegistryObject<Item> CORE_HELMET = ITEMS.register("core_helmet",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.CORE, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> CORE_CHESTPLATE = ITEMS.register("core_chestplate",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.CORE, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> CORE_LEGGINGS = ITEMS.register("core_leggings",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.CORE, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> CORE_BOOTS = ITEMS.register("core_boots",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.CORE, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> PIROXITE_HELMET = ITEMS.register("piroxite_helmet",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.PIROXITE, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).fireResistant()));
    public static final RegistryObject<Item> PIROXITE_CHESTPLATE = ITEMS.register("piroxite_chestplate",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.PIROXITE, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).fireResistant()));
    public static final RegistryObject<Item> PIROXITE_LEGGINGS = ITEMS.register("piroxite_leggings",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.PIROXITE, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).fireResistant()));
    public static final RegistryObject<Item> PIROXITE_BOOTS = ITEMS.register("piroxite_boots",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.PIROXITE, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).fireResistant()));
    public static final RegistryObject<Item> NULLITE_HELMET = ITEMS.register("nullite_helmet",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.NULLITE, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> NULLITE_CHESTPLATE = ITEMS.register("nullite_chestplate",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.NULLITE, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> NULLITE_LEGGINGS = ITEMS.register("nullite_leggings",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.NULLITE, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> REFINED_PIROXITE_HELMET = ITEMS.register("refined_piroxite_helmet",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.REFINED_PIROXITE, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> REFINED_PIROXITE_CHESTPLATE = ITEMS.register("refined_piroxite_chestplate",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.REFINED_PIROXITE, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> REFINED_PIROXITE_LEGGINGS = ITEMS.register("refined_piroxite_leggings",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.REFINED_PIROXITE, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> REFINED_PIROXITE_BOOTS = ITEMS.register("refined_piroxite_boots",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.REFINED_PIROXITE, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> NULLITE_BOOTS = ITEMS.register("nullite_boots",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.NULLITE, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    // ---------------------------------------------------------------- nave de fin de juego (sin receta por ahora)
    public static final RegistryObject<Item> CORE_FIGHTER = ITEMS.register("core_fighter",
            () -> new com.bryanamaya.tecnocinetico.item.custom.CoreFighterItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));
    /** Solo para dibujar el misil de la nave (no se obtiene). */
    public static final RegistryObject<Item> CORE_MISSILE = ITEMS.register("core_missile", () -> new Item(new Item.Properties()));

    // ---------------------------------------------------------------- Impulsor de Esquiva (gadget de cinturón)
    public static final RegistryObject<Item> DASH_THRUSTER = ITEMS.register("dash_thruster",
            () -> new com.bryanamaya.tecnocinetico.gear.GadgetItem(com.bryanamaya.tecnocinetico.gear.TacticalGear.BELT, 900,
                    "tooltip.tecnocinetico.dash_thruster", new Item.Properties()));
    public static final RegistryObject<Item> DASH_CELL = ITEMS.register("dash_cell",
            () -> new com.bryanamaya.tecnocinetico.gear.RechargeItem(() -> DASH_THRUSTER.get(), new Item.Properties()));
    // ---------------------------------------------------------------- armaduras de combate (sin habilidades: resistencia y movilidad)
    public static final RegistryObject<Item> COMBAT_MK1_HELMET = ITEMS.register("combat_mk1_helmet",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.COMBAT_MK1, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> COMBAT_MK1_CHESTPLATE = ITEMS.register("combat_mk1_chestplate",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.COMBAT_MK1, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> COMBAT_MK1_LEGGINGS = ITEMS.register("combat_mk1_leggings",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.COMBAT_MK1, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> COMBAT_MK1_BOOTS = ITEMS.register("combat_mk1_boots",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.COMBAT_MK1, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));
    public static final RegistryObject<Item> COMBAT_MK2_HELMET = ITEMS.register("combat_mk2_helmet",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.COMBAT_MK2, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> COMBAT_MK2_CHESTPLATE = ITEMS.register("combat_mk2_chestplate",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.COMBAT_MK2, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> COMBAT_MK2_LEGGINGS = ITEMS.register("combat_mk2_leggings",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.COMBAT_MK2, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> COMBAT_MK2_BOOTS = ITEMS.register("combat_mk2_boots",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.COMBAT_MK2, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));
    public static final RegistryObject<Item> COMBAT_MK3_HELMET = ITEMS.register("combat_mk3_helmet",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.COMBAT_MK3, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> COMBAT_MK3_CHESTPLATE = ITEMS.register("combat_mk3_chestplate",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.COMBAT_MK3, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> COMBAT_MK3_LEGGINGS = ITEMS.register("combat_mk3_leggings",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.COMBAT_MK3, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> COMBAT_MK3_BOOTS = ITEMS.register("combat_mk3_boots",
            () -> new com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.COMBAT_MK3, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    /** Motor Dimensional: lo suelta la Araña de Asedio; pieza fundamental de la nave (por implementar). */
    public static final RegistryObject<Item> DIMENSIONAL_DRIVE = ITEMS.register("dimensional_drive",
            () -> new Item(new Item.Properties().stacksTo(1).fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC)) {
                @Override
                public void appendHoverText(net.minecraft.world.item.ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level,
                                            java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
                    tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.tecnocinetico.dimensional_drive")
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
                }

                @Override
                public boolean isFoil(net.minecraft.world.item.ItemStack stack) {
                    return true;
                }
            });

    // ---------------------------------------------------------------- más armas (tiers 1-3)
    /** Brea: subproducto de la Refinería (asfalto). */
    public static final RegistryObject<Item> TAR = ITEMS.register("tar", () -> new Item(new Item.Properties()));
    /** Célula de Combustible Cinético: rinde como 3 Depósitos de Combustible. */
    public static final RegistryObject<Item> KINETIC_FUEL_CELL = ITEMS.register("kinetic_fuel_cell", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> FUEL_CANISTER = ITEMS.register("fuel_canister", () -> new Item(new Item.Properties().stacksTo(16)));

    // ---- Taladro de Combustión (tier 3): se monta en el Ensamblador de Armas con dos piezas del Ensamblador Industrial
    /** Motosierra (tier 2): tala árboles enteros con gasolina. */
    public static final RegistryObject<Item> CHAINSAW = ITEMS.register("chainsaw",
            () -> new com.bryanamaya.tecnocinetico.item.custom.ChainsawItem(new Item.Properties()));
    public static final RegistryObject<Item> COMBUSTION_DRILL = ITEMS.register("combustion_drill",
            () -> new com.bryanamaya.tecnocinetico.item.custom.CombustionDrillItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final RegistryObject<Item> DRILL_HEAD = ITEMS.register("drill_head",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> COMBUSTION_ENGINE = ITEMS.register("combustion_engine",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> GUIDED_ROCKET = ITEMS.register("guided_rocket", () -> new Item(new Item.Properties().stacksTo(8)));
    /** Revólver (tier 1): 6 balas cinéticas cargadas una a una, 9 de daño. */
    public static final RegistryObject<Item> REVOLVER = ITEMS.register("revolver",
            () -> new com.bryanamaya.tecnocinetico.item.weapon.LongGunItem(com.bryanamaya.tecnocinetico.item.custom.WeaponModule.Family.HEAVY, com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.Action.DRUM, new com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.Stats(
                    6, () -> KINETIC_ROUND.get(), 1, 9.0F, 40.0D, 1.5F, 0.4F, 10, 10, 6.0F, 1.5F, 0.85F, 0.85F), new Item.Properties()));
    /** Lanzallamas (tier 2): chorro de fuego en cono (7 bloques), 100 de combustible por depósito. */
    public static final RegistryObject<Item> FLAMETHROWER = ITEMS.register("flamethrower",
            () -> new com.bryanamaya.tecnocinetico.item.weapon.FlamethrowerItem(new com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.Stats(
                    100, () -> FUEL_CANISTER.get(), 1, 1.5F, 7.0D, 0.0F, 0.0F, 2, 40, 0.2F, 0.3F, 0.95F, 1.0F), new Item.Properties()));
    /** Ametralladora ligera (tier 2): 60 balas de rifle, 6 de daño, 10 disparos/s; frena un 15 % al llevarla. */
    public static final RegistryObject<Item> LIGHT_MACHINE_GUN = ITEMS.register("light_machine_gun",
            () -> new com.bryanamaya.tecnocinetico.item.weapon.HeavyGunItem(0.15, new com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.Stats(
                    60, () -> RIFLE_ROUND.get(), 1, 6.0F, 60.0D, 3.5F, 1.8F, 2, 80, 1.4F, 1.1F, 0.75F, 0.9F), new Item.Properties()));
    /** Francotirador cinético (tier 2): cerrojo, 5 balas de rifle, 26 de daño, 150 bloques, mira integrada. */
    public static final RegistryObject<Item> KINETIC_SNIPER = ITEMS.register("kinetic_sniper",
            () -> new com.bryanamaya.tecnocinetico.item.weapon.SniperItem(new com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.Stats(
                    5, () -> RIFLE_ROUND.get(), 1, 26.0F, 150.0D, 2.0F, 0.0F, 30, 16, 8.0F, 1.2F, 0.15F, 0.7F), new Item.Properties()));
    /** Cuchilla cinética (tier 2): espada de inercita que acumula carga y la suelta en un tajo de onda. */
    public static final RegistryObject<Item> KINETIC_BLADE = ITEMS.register("kinetic_blade",
            () -> new com.bryanamaya.tecnocinetico.item.weapon.KineticBladeItem(new Item.Properties()));
    /** Lanzacohetes (tier 3): cohete guiado por la mira, explosión de potencia 4 sin romper bloques. */
    public static final RegistryObject<Item> ROCKET_LAUNCHER = ITEMS.register("rocket_launcher",
            () -> new com.bryanamaya.tecnocinetico.item.weapon.RocketLauncherItem(new com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.Stats(
                    1, () -> GUIDED_ROCKET.get(), 1, 0.0F, 0.0D, 1.0F, 0.5F, 30, 40, 7.0F, 2.0F, 0.8F, 0.5F), new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    /** Arco voltaico (tier 3): rayo en cadena (5 objetivos), 12 descargas por Célula de Impulso. */
    public static final RegistryObject<Item> ARC_CASTER = ITEMS.register("arc_caster",
            () -> new com.bryanamaya.tecnocinetico.item.weapon.ArcCasterItem(new com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.Stats(
                    12, () -> PULSE_CELL.get(), 1, 10.0F, 24.0D, 0.0F, 0.0F, 8, 30, 2.0F, 1.0F, 0.9F, 1.4F), new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    /** Minigun (tier 3): gira ~1 s antes de disparar; Caja de Munición de Minigun (180 balas de rifle en cinta) a 20 disparos/s; frena un 35 %. */
    public static final RegistryObject<Item> MINIGUN = ITEMS.register("minigun",
            () -> new com.bryanamaya.tecnocinetico.item.weapon.MinigunItem(new com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.Stats(
                    180, () -> RIFLE_ROUND.get(), 1, 4.0F, 50.0D, 4.5F, 3.0F, 1, 100, 0.4F, 0.6F, 0.9F, 1.2F), new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    public static final RegistryObject<Item> NETWORK_WATCHER_SPAWN_EGG = ITEMS.register("network_watcher_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(com.bryanamaya.tecnocinetico.entity.ModEntities.NETWORK_WATCHER, 0x344959, 0x41DCF5, new Item.Properties()));
    public static final RegistryObject<Item> SWARM_MATRIARCH_SPAWN_EGG = ITEMS.register("swarm_matriarch_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(com.bryanamaya.tecnocinetico.entity.ModEntities.SWARM_MATRIARCH, 0x3B4E44, 0x73EB96, new Item.Properties()));

    public static final RegistryObject<Item> VECTOR_THRUSTER = ITEMS.register("vector_thruster", () -> shipComponent("vector_thruster"));
    public static final RegistryObject<Item> ADAPTIVE_FLIGHT_CORE = ITEMS.register("adaptive_flight_core", () -> shipComponent("adaptive_flight_core"));

    private static Item shipComponent(String id) {
        return new Item(new Item.Properties().stacksTo(1).fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC)) {
            @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,
                    @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level,
                    java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
                tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.tecnocinetico." + id).withStyle(net.minecraft.ChatFormatting.GRAY));
            }
        };
    }
}
