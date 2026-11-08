package com.bryanamaya.tecnocinetico.entity.custom;

import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.ArrayList;
import java.util.List;

/**
 * Ofertas de los Supervivientes. Moneda: Chatarra. Precios altos a propósito (mod difícil): lo bueno
 * (módulos, piezas) cuesta mucha chatarra y además material del mod. Ajustar aquí el equilibrio.
 * Cada oferta se puede usar `uses` veces al día (se repone al amanecer).
 */
public final class SurvivorTrades {

    private SurvivorTrades() {}

    private static ItemStack s(Item item, int n) {
        return new ItemStack(item, n);
    }

    private static ItemStack scrap(int n) {
        return new ItemStack(ModItems.SCRAP_METAL.get(), n);
    }

    /** El jugador VENDE `give` y recibe chatarra. */
    private static MerchantOffer buys(ItemStack give, int scrap, int uses) {
        return new MerchantOffer(give, scrap(scrap), uses, 2, 0.05F);
    }

    /** El jugador COMPRA `get` pagando chatarra (y opcionalmente otro objeto). */
    private static MerchantOffer sells(int scrap, ItemStack extra, ItemStack get, int uses) {
        return extra.isEmpty() ? new MerchantOffer(scrap(scrap), get, uses, 3, 0.05F)
                : new MerchantOffer(scrap(scrap), extra, get, uses, 5, 0.05F);
    }

    public static List<MerchantOffer> forRole(SurvivorEntity.Role role) {
        List<MerchantOffer> o = new ArrayList<>();
        switch (role) {
            case SCRAPPER -> {                                       // Chatarrero: materiales básicos
                o.add(buys(s(Items.COPPER_INGOT, 6), 2, 12));
                o.add(buys(s(Items.ROTTEN_FLESH, 16), 1, 12));
                o.add(buys(s(Items.STRING, 8), 1, 8));
                o.add(sells(6, ItemStack.EMPTY, s(Items.IRON_INGOT, 1), 8));
                o.add(sells(10, ItemStack.EMPTY, s(ModItems.CINETRITE_SHARD.get(), 2), 6));
                o.add(sells(14, ItemStack.EMPTY, s(ModItems.RAW_CINETRITE.get(), 1), 4));
                o.add(sells(24, ItemStack.EMPTY, s(ModItems.PIEZO_TRANSDUCER.get(), 1), 2));
            }
            case GUNSMITH -> {                                       // Armero: piezas y módulos de arma
                o.add(buys(s(Items.GUNPOWDER, 6), 2, 10));
                o.add(buys(s(ModItems.CINETRITE_INGOT.get(), 1), 4, 8));
                o.add(sells(4, ItemStack.EMPTY, s(ModItems.KINETIC_ROUND.get(), 6), 8));
                o.add(sells(12, ItemStack.EMPTY, s(ModItems.MAGAZINE.get(), 1), 2));
                o.add(sells(20, ItemStack.EMPTY, s(ModItems.CRUDE_KINETIC_COUPLER.get(), 1), 3));
                o.add(sells(40, s(ModItems.CINETRITE_INGOT.get(), 2), s(ModItems.MODULE_QUICK_RELOAD.get(), 1), 1));
                o.add(sells(48, s(ModItems.CINETRITE_INGOT.get(), 3), s(ModItems.MODULE_EXTENDED_MAG.get(), 1), 1));
                o.add(sells(56, s(ModItems.INERCITA_INGOT.get(), 2), s(ModItems.MODULE_OPTIC.get(), 1), 1));
                o.add(sells(64, s(ModItems.INERCITA_INGOT.get(), 4), s(ModItems.MODULE_INERCITA_BARREL.get(), 1), 1));
            }
            case FARMER -> {                                         // Granjero: comida
                o.add(buys(s(Items.WHEAT, 20), 1, 16));
                o.add(buys(s(Items.POTATO, 20), 1, 16));
                o.add(buys(s(Items.BEETROOT, 15), 1, 16));
                o.add(sells(2, ItemStack.EMPTY, s(Items.BREAD, 3), 12));
                o.add(sells(2, ItemStack.EMPTY, s(Items.BAKED_POTATO, 4), 12));
                o.add(sells(4, ItemStack.EMPTY, s(Items.COOKED_MUTTON, 3), 8));
                o.add(sells(8, ItemStack.EMPTY, s(Items.GOLDEN_CARROT, 2), 4));
            }
        }
        return o;
    }
}
