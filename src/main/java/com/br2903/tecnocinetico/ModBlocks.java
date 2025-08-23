package com.br2903.tecnocinetico;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TecnocineticoMod.MODID);

    public static final DeferredBlock<Block> TECNOCINETICO_BLOCK = BLOCKS.registerSimpleBlock(
            "tecnocinetico_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
    );

    public static void registerToBus(net.neoforged.bus.api.IEventBus bus) {
        BLOCKS.register(bus);
    }
}