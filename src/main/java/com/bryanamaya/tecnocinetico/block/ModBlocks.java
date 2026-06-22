package com.bryanamaya.tecnocinetico.block;

import com.bryanamaya.tecnocinetico.block.custom.KineticAssemblyPressBlock;
import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "tecnocinetico");

    // --- MINERALES ---
    public static final RegistryObject<Block> CINETRITE_ORE = registerBlock("cinetrite_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> DEEPSLATE_CINETRITE_ORE = registerBlock("deepslate_cinetrite_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE)
                    .strength(4.5f, 3.0f).requiresCorrectToolForDrops()));

    // --- ALEACIÓN ANTIGUA ---
    public static final RegistryObject<Block> ANCIENT_ALLOY_BLOCK = registerBlock("ancient_alloy_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f).sound(SoundType.NETHERITE_BLOCK)));

    public static final RegistryObject<Block> ANCIENT_ALLOY_TL = registerBlock("ancient_alloy_tl",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)));

    public static final RegistryObject<Block> ANCIENT_ALLOY_TR = registerBlock("ancient_alloy_tr",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)));

    public static final RegistryObject<Block> ANCIENT_ALLOY_BL = registerBlock("ancient_alloy_bl",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)));

    public static final RegistryObject<Block> ANCIENT_ALLOY_BR = registerBlock("ancient_alloy_br",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)));

    // --- MAQUINARIA ---
    public static final RegistryObject<Block> KINETIC_ASSEMBLY_PRESS = registerBlock("kinetic_assembly_press",
            () -> new KineticAssemblyPressBlock());


    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}