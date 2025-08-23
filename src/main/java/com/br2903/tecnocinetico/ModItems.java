package com.br2903.tecnocinetico;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TecnocineticoMod.MODID);

    public static final DeferredItem<Item> TECNOCINETICO_ITEM = ITEMS.registerSimpleItem(
            "tecnocinetico_item",
            new Item.Properties().food(new FoodProperties.Builder().alwaysEdible().nutrition(1).saturationModifier(2f).build())
    );

    public static final DeferredItem<BlockItem> TECNOCINETICO_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(
            "tecnocinetico_block",
            ModBlocks.TECNOCINETICO_BLOCK
    );

    public static void registerToBus(net.neoforged.bus.api.IEventBus bus) {
        ITEMS.register(bus);
    }
}