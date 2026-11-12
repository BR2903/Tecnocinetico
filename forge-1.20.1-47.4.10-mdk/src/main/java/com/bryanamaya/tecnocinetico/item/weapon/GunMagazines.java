package com.bryanamaya.tecnocinetico.item.weapon;

import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Map;

/**
 * Qué cargador usa cada arma (los cargadores se comparten donde tiene sentido). Las armas que NO están aquí cargan
 * directamente de sus paquetes de munición, bala a bala (revólver y escopetas: un paquete abierto guarda lo que
 * sobra), o de su propio consumible (granadas, cohetes, células de impulso, depósito de combustible).
 * <pre>
 * Pistola ................ Cargador (12) / Cargador Ampliado (18)   (KineticPistolItem, no pasa por aquí)
 * Subfusil ............... Cargador Ampliado (18)                     Bala Cinética
 * Rifle de cerrojo, Francotirador, Rifle de Riel ... Peine (5)        Bala de Rifle
 * Rifle semiautomático ... Cargador de Fusil Corto (10)               Bala de Rifle
 * Fusil de asalto ........ Cargador de Fusil (30)                     Bala de Rifle
 * Ametralladora ligera ... Caja de Cinta (60)                         Cinta de Rifle
 * Minigun ................ Caja de Munición de Minigun (180)          Cinta de Rifle
 * </pre>
 */
public final class GunMagazines {

    private static Map<Item, List<Item>> table;

    private GunMagazines() {}

    private static Map<Item, List<Item>> table() {
        if (table == null) {
            table = Map.of(
                    ModItems.KINETIC_SMG.get(), List.of(ModItems.EXTENDED_MAGAZINE.get()),
                    ModItems.BOLT_RIFLE.get(), List.of(ModItems.STRIPPER_CLIP.get()),
                    ModItems.KINETIC_SNIPER.get(), List.of(ModItems.STRIPPER_CLIP.get()),
                    ModItems.RAIL_RIFLE.get(), List.of(ModItems.STRIPPER_CLIP.get()),
                    ModItems.SEMI_RIFLE.get(), List.of(ModItems.SHORT_RIFLE_MAGAZINE.get()),
                    ModItems.ASSAULT_RIFLE.get(), List.of(ModItems.RIFLE_MAGAZINE.get()),
                    ModItems.LIGHT_MACHINE_GUN.get(), List.of(ModItems.BELT_BOX.get()),
                    ModItems.MINIGUN.get(), List.of(ModItems.MINIGUN_BOX.get()));
        }
        return table;
    }

    /** Cargadores que admite el arma (vacío = carga de sus paquetes o de su consumible). */
    public static List<Item> of(Item gun) {
        return table().getOrDefault(gun, List.of());
    }

    public static boolean magazineFed(Item gun) {
        return table().containsKey(gun);
    }
}
