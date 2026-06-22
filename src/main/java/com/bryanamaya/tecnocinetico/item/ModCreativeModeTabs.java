package com.bryanamaya.tecnocinetico.item;

import com.bryanamaya.tecnocinetico.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "tecnocinetico");

    public static final RegistryObject<CreativeModeTab> TECNOCINETICO_TAB = CREATIVE_MODE_TABS.register("tecnocinetico_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModBlocks.ANCIENT_ALLOY_BLOCK.get()))
                    .title(Component.literal("Tecnocinético"))
                    .displayItems((pParameters, pOutput) -> {

                        // --- ÍTEMS ---
                        pOutput.accept(ModItems.RAW_CINETRITE.get());
                        pOutput.accept(ModItems.CINETRITE_INGOT.get());
                        pOutput.accept(ModItems.CINETRITE_SHARD.get());
                        pOutput.accept(ModItems.RESISTOR_330_OHM.get());
                        pOutput.accept(ModItems.PIEZO_TRANSDUCER.get());
                        pOutput.accept(ModItems.DEEPSLATE_LOGIC_SUBSTRATE.get());
                        pOutput.accept(ModItems.CRUDE_KINETIC_COUPLER.get());

                        // --- BLOQUES ---
                        pOutput.accept(ModBlocks.CINETRITE_ORE.get());
                        pOutput.accept(ModBlocks.DEEPSLATE_CINETRITE_ORE.get());
                        pOutput.accept(ModBlocks.ANCIENT_ALLOY_BLOCK.get());
                        pOutput.accept(ModBlocks.ANCIENT_ALLOY_TL.get());
                        pOutput.accept(ModBlocks.ANCIENT_ALLOY_TR.get());
                        pOutput.accept(ModBlocks.ANCIENT_ALLOY_BL.get());
                        pOutput.accept(ModBlocks.ANCIENT_ALLOY_BR.get());
                        pOutput.accept(ModBlocks.KINETIC_ASSEMBLY_PRESS.get());

                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}