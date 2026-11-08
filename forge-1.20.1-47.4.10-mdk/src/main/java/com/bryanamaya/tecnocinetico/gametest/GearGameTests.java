package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.gear.GearEvents;
import com.bryanamaya.tecnocinetico.gear.TacticalGear;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.item.custom.KineticPistolItem;
import com.bryanamaya.tecnocinetico.item.custom.MagazineItem;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Pruebas del Equipo Táctico: munición desde sus ranuras, comer solo, visor y filtros de ranura. */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class GearGameTests {

    private static TacticalGear gear(GameTestHelper helper, Player p) {
        var g = TacticalGear.of(p);
        helper.assertTrue(g.isPresent(), "El jugador no tiene Equipo Táctico (capacidad sin enganchar)");
        return g.get();
    }

    @GameTest(template = "empty")
    public static void reloadFromAmmoSlots(GameTestHelper helper) {
        Player p = helper.makeMockPlayer();
        p.getInventory().clearContent();
        TacticalGear g = gear(helper, p);
        ItemStack mag = new ItemStack(ModItems.MAGAZINE.get());
        MagazineItem.setContents(mag, 5, 0);
        g.items().setStackInSlot(TacticalGear.AMMO_START, mag);
        ItemStack pistol = new ItemStack(ModItems.KINETIC_PISTOL.get());
        KineticPistolItem.startReload(p, pistol);
        for (int i = 0; i < 200 && KineticPistolItem.isReloading(pistol); i++) {
            pistol.inventoryTick(helper.getLevel(), p, 0, true);
        }
        helper.assertTrue(KineticPistolItem.getAmmo(pistol) == 5, "Debería recargar 5 balas desde la ranura de munición");
        // segundo cargador: el viejo (vacío ya no, con 5) debe volver a una ranura de munición
        ItemStack full = new ItemStack(ModItems.MAGAZINE.get());
        MagazineItem.setContents(full, 6, 0);
        g.items().setStackInSlot(TacticalGear.AMMO_START + 1, full);
        KineticPistolItem.startReload(p, pistol);
        for (int i = 0; i < 200 && KineticPistolItem.isReloading(pistol); i++) {
            pistol.inventoryTick(helper.getLevel(), p, 0, true);
        }
        boolean backInSlot = false;
        for (int i = TacticalGear.AMMO_START; i < TacticalGear.AMMO_START + TacticalGear.AMMO_COUNT; i++) {
            ItemStack s = g.get(i);
            backInSlot |= s.getItem() instanceof MagazineItem && MagazineItem.getRounds(s) == 5;
        }
        helper.assertTrue(KineticPistolItem.getAmmo(pistol) == 6 && backInSlot,
                "El cargador viejo (5 balas) debería volver a una ranura de munición");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void autoEatAndVisor(GameTestHelper helper) {
        Player p = helper.makeMockPlayer();
        TacticalGear g = gear(helper, p);
        p.getFoodData().setFoodLevel(8);
        g.items().setStackInSlot(TacticalGear.FOOD_START, new ItemStack(Items.BREAD, 3));
        GearEvents.autoEat(p, g);
        helper.assertTrue(p.getFoodData().getFoodLevel() > 8 && g.get(TacticalGear.FOOD_START).getCount() == 2,
                "Con hambre debería comerse un pan de la ranura de comida");

        ItemStack visor = new ItemStack(ModItems.NIGHT_VISION_VISOR.get());
        g.items().setStackInSlot(TacticalGear.FACE, visor);
        GearEvents.serverTick(p, g);                                       // tickCount 0: se aplica el visor
        helper.assertTrue(p.hasEffect(MobEffects.NIGHT_VISION), "El visor debería dar visión nocturna");
        helper.assertTrue(g.get(TacticalGear.FACE).getDamageValue() == 1, "El visor debería gastar batería");
        g.setVisorOn(false);
        GearEvents.serverTick(p, g);
        helper.assertTrue(!p.hasEffect(MobEffects.NIGHT_VISION), "Al apagar el visor se quita la visión nocturna");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void slotFilters(GameTestHelper helper) {
        helper.assertTrue(TacticalGear.accepts(TacticalGear.AMMO_START, new ItemStack(ModItems.KINETIC_ROUND.get())), "balas → munición");
        helper.assertTrue(!TacticalGear.accepts(TacticalGear.AMMO_START, new ItemStack(Items.BREAD)), "pan ✗ munición");
        helper.assertTrue(TacticalGear.accepts(TacticalGear.FOOD_START, new ItemStack(Items.BREAD)), "pan → comida");
        helper.assertTrue(!TacticalGear.accepts(TacticalGear.FOOD_START, new ItemStack(Items.MUSHROOM_STEW)), "estofado ✗ (devuelve cuenco)");
        helper.assertTrue(TacticalGear.accepts(TacticalGear.BACK, new ItemStack(ModItems.JETPACK.get())), "jetpack → espalda");
        helper.assertTrue(!TacticalGear.accepts(TacticalGear.FACE, new ItemStack(ModItems.JETPACK.get())), "jetpack ✗ cara");
        helper.succeed();
    }
}
