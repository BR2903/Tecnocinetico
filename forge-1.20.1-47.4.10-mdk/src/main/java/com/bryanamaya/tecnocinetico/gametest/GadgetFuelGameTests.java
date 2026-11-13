package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.gear.GadgetFuel;
import com.bryanamaya.tecnocinetico.gear.TacticalGear;
import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Gadgets: encender/apagar desde el Equipo Táctico y repostar solos con su combustible. */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class GadgetFuelGameTests {

    @GameTest(template = "empty")
    public static void gadgetToggleSaves(GameTestHelper helper) {
        TacticalGear g = new TacticalGear();
        g.items().setStackInSlot(TacticalGear.BACK, new ItemStack(ModItems.JETPACK.get()));
        helper.assertTrue(!g.active(TacticalGear.BACK).isEmpty(), "Encendido por defecto");
        g.setEnabled(TacticalGear.BACK, false);
        helper.assertTrue(g.active(TacticalGear.BACK).isEmpty() && !g.get(TacticalGear.BACK).isEmpty(), "Apagado sigue funcionando");
        TacticalGear copy = new TacticalGear();
        copy.load(g.save());
        helper.assertTrue(!copy.enabled(TacticalGear.BACK) && copy.enabled(TacticalGear.FACE), "El encendido no se guarda");
        CompoundTag old = new TacticalGear().save();                                  // formato antiguo: solo VisorOn
        old.remove("Enabled");
        old.putBoolean("VisorOn", false);
        copy.load(old);
        helper.assertTrue(!copy.visorOn() && copy.enabled(TacticalGear.BACK), "No migra el visor apagado");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void jetpackRefuelsFromInventory(GameTestHelper helper) {
        Player p = helper.makeMockPlayer();
        p.getInventory().clearContent();
        TacticalGear g = TacticalGear.of(p).orElseThrow();
        ItemStack jet = new ItemStack(ModItems.JETPACK.get());
        jet.setDamageValue(jet.getMaxDamage() - 1);                                    // vacío
        g.items().setStackInSlot(TacticalGear.BACK, jet);
        p.getInventory().add(new ItemStack(ModItems.FUEL_CANISTER.get(), 2));
        // apagado no reposta
        g.setEnabled(TacticalGear.BACK, false);
        helper.assertTrue(!GadgetFuel.autoRefuel(p, g, TacticalGear.BACK), "Reposta estando apagado");
        g.setEnabled(TacticalGear.BACK, true);
        helper.assertTrue(GadgetFuel.autoRefuel(p, g, TacticalGear.BACK), "No reposta con gasolina en el inventario");
        ItemStack worn = g.get(TacticalGear.BACK);
        int left = worn.getMaxDamage() - 1 - worn.getDamageValue();
        helper.assertTrue(left == 800, "Un depósito debería dar 800: " + left);
        helper.assertTrue(p.getInventory().countItem(ModItems.FUEL_CANISTER.get()) == 1, "No gasta el depósito");
        helper.assertTrue(!GadgetFuel.autoRefuel(p, g, TacticalGear.BACK), "Reposta sin estar casi vacío");
        // el visor es de batería: nunca reposta solo
        ItemStack visor = new ItemStack(ModItems.NIGHT_VISION_VISOR.get());
        visor.setDamageValue(visor.getMaxDamage() - 1);
        g.items().setStackInSlot(TacticalGear.FACE, visor);
        p.getInventory().add(new ItemStack(ModItems.VISOR_BATTERY.get()));
        helper.assertTrue(!GadgetFuel.autoRefuel(p, g, TacticalGear.FACE), "El visor no es de combustible");
        helper.succeed();
    }
}
