package com.bryanamaya.tecnocinetico.item;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.block.ModWorldBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Tecnocinetico.MOD_ID);

    public static final RegistryObject<CreativeModeTab> TECNOCINETICO_TAB = CREATIVE_MODE_TABS.register("tecnocinetico_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModBlocks.ANCIENT_ALLOY_BLOCK.get()))
                    .title(Component.translatable("creativetab.tecnocinetico_tab"))
                    .displayItems((pParameters, pOutput) -> {

                        // --- ÍTEMS ---
                        pOutput.accept(ModItems.RAW_CINETRITE.get());
                        pOutput.accept(ModItems.CINETRITE_INGOT.get());
                        pOutput.accept(ModItems.CINETRITE_SHARD.get());
                        pOutput.accept(ModItems.RAW_INERCITA.get());
                        pOutput.accept(ModItems.INERCITA_INGOT.get());
                        pOutput.accept(ModItems.RESISTOR_330_OHM.get());
                        pOutput.accept(ModItems.PIEZO_TRANSDUCER.get());
                        pOutput.accept(ModItems.DEEPSLATE_LOGIC_SUBSTRATE.get());
                        pOutput.accept(ModItems.CRUDE_KINETIC_COUPLER.get());

                        // --- BLOQUES ---
                        pOutput.accept(ModBlocks.CINETRITE_ORE.get());
                        pOutput.accept(ModBlocks.DEEPSLATE_CINETRITE_ORE.get());
                        pOutput.accept(ModBlocks.INERCITA_ORE.get());
                        pOutput.accept(ModBlocks.DEEPSLATE_INERCITA_ORE.get());
                        pOutput.accept(ModBlocks.ANCIENT_ALLOY_BLOCK.get());
                        pOutput.accept(ModBlocks.ANCIENT_ALLOY_TL.get());
                        pOutput.accept(ModBlocks.ANCIENT_ALLOY_TR.get());
                        pOutput.accept(ModBlocks.ANCIENT_ALLOY_BL.get());
                        pOutput.accept(ModBlocks.ANCIENT_ALLOY_BR.get());
                        pOutput.accept(ModBlocks.KINETIC_ASSEMBLY_PRESS.get());
                        pOutput.accept(ModBlocks.AUTO_KINETIC_PRESS.get());
                        pOutput.accept(ModBlocks.WEAPON_BENCH.get());
                        pOutput.accept(ModBlocks.CREATIVE_ENERGY_SOURCE.get()); // solo pruebas
                        pOutput.accept(ModItems.KINETIC_HAMMER.get());
                        pOutput.accept(ModItems.KINETIC_PISTOL.get());
                        pOutput.accept(ModItems.MODULE_OPTIC.get());
                        pOutput.accept(ModItems.MODULE_EXTENDED_MAG.get());
                        pOutput.accept(ModItems.MODULE_INERCITA_BARREL.get());
                        pOutput.accept(ModItems.MODULE_QUICK_RELOAD.get());
                        pOutput.accept(ModItems.SWARM_BODY_SPAWN_EGG.get());
                        pOutput.accept(ModItems.SURVIVOR_SPAWN_EGG.get());
                    })
                    .build());

    /** Pestaña de los bloques del mundo post-apocalíptico (hormigón, asfalto, metal oxidado, ceniza...). */
    public static final RegistryObject<CreativeModeTab> WORLD_TAB = CREATIVE_MODE_TABS.register("tecnocinetico_world_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModWorldBlocks.CRACKED_CONCRETE.get()))
                    .title(Component.translatable("creativetab.tecnocinetico_world_tab"))
                    .withTabsBefore(TECNOCINETICO_TAB.getKey())
                    .displayItems((params, output) -> {
                        ModWorldBlocks.ALL.forEach(b -> output.accept(b.get()));
                        output.accept(ModItems.SCRAP_METAL.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}