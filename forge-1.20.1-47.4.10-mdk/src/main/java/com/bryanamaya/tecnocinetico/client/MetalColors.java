package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;

/**
 * Lingotes, pepitas y menas en bruto con la MISMA forma que los de hierro de Minecraft: sus modelos usan la
 * textura vanilla (minecraft:item/iron_ingot, iron_nugget, raw_iron) y aquí se tiñen con el color de cada metal
 * (multiplica: el hierro es casi gris, así que conserva su sombreado; el hierro en bruto es beige y sus colores
 * están compensados para que no salgan marrones). Los modelos los escribe tools/metals_gen.py.
 */
public final class MetalColors {

    private MetalColors() {}

    public static final Map<RegistryObject<Item>, Integer> COLORS = Map.ofEntries(
            Map.entry(ModItems.LEAD_INGOT, 0x8E95B8), Map.entry(ModItems.LEAD_NUGGET, 0x8E95B8), Map.entry(ModItems.RAW_LEAD, 0x8BA9F5),
            Map.entry(ModItems.TIN_INGOT, 0xE4E8DE), Map.entry(ModItems.TIN_NUGGET, 0xE4E8DE), Map.entry(ModItems.RAW_TIN, 0xDAFFFF),
            Map.entry(ModItems.TITANIUM_INGOT, 0xC6C0E4), Map.entry(ModItems.TITANIUM_NUGGET, 0xC6C0E4), Map.entry(ModItems.RAW_TITANIUM, 0xB5CCFF),
            Map.entry(ModItems.URANIUM_INGOT, 0x9BE05A), Map.entry(ModItems.URANIUM_NUGGET, 0x9BE05A), Map.entry(ModItems.RAW_URANIUM, 0x85E367),
            Map.entry(ModItems.INERCITA_INGOT, 0x8492C4),
            Map.entry(ModItems.PIROXITE_INGOT, 0xF08A40),
            Map.entry(ModItems.NULLITE_INGOT, 0xBDAAF2),
            // en bruto (colores compensados por el tono beige del hierro en bruto)
            Map.entry(ModItems.RAW_PIROXITE, 0xED8146),
            Map.entry(ModItems.RAW_NULLITE, 0xB1A4FF));

    public static void register(RegisterColorHandlersEvent.Item event) {
        COLORS.forEach((item, color) -> event.register((stack, layer) -> layer == 0 ? 0xFF000000 | color : -1, item.get()));
    }
}
