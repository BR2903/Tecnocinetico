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

    private static final net.minecraft.world.inventory.AbstractContainerMenu DUMMY = new net.minecraft.world.inventory.AbstractContainerMenu(null, -1) {
        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    };

    private static net.minecraft.world.inventory.TransientCraftingContainer grid(ItemStack a, ItemStack b) {
        var g = new net.minecraft.world.inventory.TransientCraftingContainer(DUMMY, 3, 3);
        g.setItem(0, a);
        g.setItem(4, b);
        return g;
    }

    /** Equipos guardados con el formato viejo (10 ranuras) se recolocan en las nuevas. */
    @GameTest(template = "empty")
    public static void migrateOldGear(GameTestHelper helper) {
        var old = new net.minecraftforge.items.ItemStackHandler(10);
        old.setStackInSlot(3, new ItemStack(ModItems.KINETIC_ROUND.get(), 5));
        old.setStackInSlot(5, new ItemStack(Items.BREAD, 2));
        old.setStackInSlot(7, new ItemStack(ModItems.NIGHT_VISION_VISOR.get()));
        old.setStackInSlot(8, new ItemStack(ModItems.JETPACK.get()));
        old.setStackInSlot(9, new ItemStack(ModItems.AQUA_PROPULSOR.get()));
        TacticalGear g = new TacticalGear();
        g.load(old.serializeNBT());
        helper.assertTrue(g.get(3).getCount() == 5 && g.get(TacticalGear.FOOD_START + 1).is(Items.BREAD)
                && g.get(TacticalGear.FACE).is(ModItems.NIGHT_VISION_VISOR.get()) && g.get(TacticalGear.BACK).is(ModItems.JETPACK.get())
                && g.get(TacticalGear.FEET).is(ModItems.AQUA_PROPULSOR.get()), "La migración del formato viejo no recolocó bien");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void jetpackLevelsAndRecharge(GameTestHelper helper) {
        var lvl = helper.getLevel();
        ItemStack jet = new ItemStack(ModItems.JETPACK.get());
        int b1 = jet.getMaxDamage();
        // mejora Mk I → Mk II en la mesa
        var up = new com.bryanamaya.tecnocinetico.gear.GearRecipes.JetpackUpgrade(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "t1"),
                net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT);
        var g1 = grid(jet, new ItemStack(ModItems.JETPACK_KIT_2.get()));
        helper.assertTrue(up.matches(g1, lvl), "Jetpack + Kit Mk II debería mejorar");
        ItemStack mk2 = up.assemble(g1, lvl.registryAccess());
        helper.assertTrue(com.bryanamaya.tecnocinetico.gear.JetpackItem.level(mk2) == 2 && mk2.getMaxDamage() > b1,
                "Mk II debería tener más batería que Mk I");
        helper.assertTrue(!up.matches(grid(jet, new ItemStack(ModItems.JETPACK_KIT_3.get())), lvl), "Mk I + Kit Mk III no vale");
        helper.assertTrue(com.bryanamaya.tecnocinetico.gear.JetpackItem.thrust(mk2) > com.bryanamaya.tecnocinetico.gear.JetpackItem.thrust(jet),
                "Mk II debería empujar más");
        // recarga en la mesa: jetpack gastado + cartucho = lleno
        mk2.setDamageValue(500);
        var rc = new com.bryanamaya.tecnocinetico.gear.GearRecipes.Recharge(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "t2"),
                net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT);
        var g2 = grid(mk2, new ItemStack(ModItems.JETPACK_FUEL.get()));
        helper.assertTrue(rc.matches(g2, lvl) && rc.assemble(g2, lvl.registryAccess()).getDamageValue() == 0,
                "Jetpack + Cartucho debería recargarse del todo");
        helper.assertTrue(!rc.matches(grid(mk2, new ItemStack(ModItems.VISOR_BATTERY.get())), lvl), "La batería del visor no recarga el jetpack");
        // recarga con clic derecho del visor puesto
        Player p = helper.makeMockPlayer();
        TacticalGear gear = gear(helper, p);
        ItemStack visor = new ItemStack(ModItems.NIGHT_VISION_VISOR.get());
        visor.setDamageValue(300);
        gear.items().setStackInSlot(TacticalGear.FACE, visor);
        ItemStack battery = new ItemStack(ModItems.VISOR_BATTERY.get(), 2);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, battery);
        battery.use(lvl, p, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(gear.get(TacticalGear.FACE).getDamageValue() == 0 && p.getMainHandItem().getCount() == 1,
                "La Batería de Visor (clic derecho) debería recargar el visor puesto y gastarse");
        helper.succeed();
    }
}
