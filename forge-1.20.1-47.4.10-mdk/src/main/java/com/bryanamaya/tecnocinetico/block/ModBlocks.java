package com.bryanamaya.tecnocinetico.block;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.custom.AutoKineticPressBlock;
import com.bryanamaya.tecnocinetico.block.custom.KineticAssemblyPressBlock;
import com.bryanamaya.tecnocinetico.block.custom.CreativeEnergySourceBlock;
import com.bryanamaya.tecnocinetico.block.custom.WeaponBenchBlock;
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
            DeferredRegister.create(ForgeRegistries.BLOCKS, Tecnocinetico.MOD_ID);

    // --- MINERALES ---
    public static final RegistryObject<Block> CINETRITE_ORE = registerBlock("cinetrite_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> DEEPSLATE_CINETRITE_ORE = registerBlock("deepslate_cinetrite_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE)
                    .strength(4.5f, 3.0f).requiresCorrectToolForDrops()));

    // --- INERCITA (TIER 2: CONTENCIÓN) ---
    public static final RegistryObject<Block> INERCITA_ORE = registerBlock("inercita_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(5.0f, 6.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> DEEPSLATE_INERCITA_ORE = registerBlock("deepslate_inercita_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE)
                    .strength(6.5f, 6.0f).requiresCorrectToolForDrops()));

    // --- PIROXITA (Nether: disipa el calor) y NULITA (End: sin fricción) ---
    public static final RegistryObject<Block> PIROXITE_ORE = registerBlock("piroxite_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.NETHERRACK)
                    .strength(4.0f, 6.0f).requiresCorrectToolForDrops().lightLevel(s -> 3)));

    public static final RegistryObject<Block> NULLITE_ORE = registerBlock("nullite_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.END_STONE)
                    .strength(5.0f, 9.0f).requiresCorrectToolForDrops()));

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

    public static final RegistryObject<Block> AUTO_KINETIC_PRESS = registerBlock("auto_kinetic_press",
            () -> new AutoKineticPressBlock());

    public static final RegistryObject<Block> WEAPON_ASSEMBLER = registerBlock("weapon_assembler",
            () -> new com.bryanamaya.tecnocinetico.block.custom.WeaponAssemblerBlock());

    public static final RegistryObject<Block> WEAPON_BENCH = registerBlock("weapon_bench",
            WeaponBenchBlock::new);

    // --- SOLO PRUEBAS (creativo): energía infinita. Sin receta ni drops. ---
    public static final RegistryObject<Block> CREATIVE_ENERGY_SOURCE = registerBlock("creative_energy_source",
            CreativeEnergySourceBlock::new);


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