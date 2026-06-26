package com.bryanamaya.tecnocinetico.item;

import com.bryanamaya.tecnocinetico.item.custom.KineticPistolItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "tecnocinetico");

    // --- MIGRACIÓN DEL SISTEMA VIEJO ---
    public static final RegistryObject<Item> RAW_CINETRITE = ITEMS.register("raw_cinetrite",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CINETRITE_INGOT = ITEMS.register("cinetrite_ingot",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CINETRITE_SHARD = ITEMS.register("cinetrite_shard",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> RESISTOR_330_OHM = ITEMS.register("resistor_330_ohm",
            () -> new Item(new Item.Properties()));

    // --- EL MARTILLO DE INICIO ---
    public static final RegistryObject<Item> KINETIC_HAMMER = ITEMS.register("kinetic_hammer",
            () -> new Item(new Item.Properties().stacksTo(1).defaultDurability(256)));

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

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}