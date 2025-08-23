package com.br2903.tecnocinetico;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, TecnocineticoMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TECNOCINETICO_TAB =
            CREATIVE_MODE_TABS.register("tecnocinetico_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.tecnocinetico"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.TECNOCINETICO_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.TECNOCINETICO_ITEM.get());
                        output.accept(ModItems.TECNOCINETICO_BLOCK_ITEM.get());
                    }).build());

    public static void registerToBus(net.neoforged.bus.api.IEventBus bus) {
        CREATIVE_MODE_TABS.register(bus);
    }
}