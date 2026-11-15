package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.gear.GadgetFuel;
import com.bryanamaya.tecnocinetico.gear.TacticalGear;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.item.custom.KineticRounds;
import com.bryanamaya.tecnocinetico.item.custom.MagazineItem;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** v2: todas las balas tienen versión cinética y las 16 ranuras de munición aceptan suministros de todo tipo. */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class KineticAmmoGameTests {

    private static FakePlayer player(GameTestHelper h) {
        FakePlayer p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "artillero"));
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    @GameTest(template = "empty")
    public static void everyRoundHasKineticVersion(GameTestHelper h) {
        h.assertTrue(KineticRounds.kineticOf(ModItems.STANDARD_ROUND.get()) == ModItems.KINETIC_ROUND.get()
                && KineticRounds.kineticOf(ModItems.RIFLE_ROUND.get()) == ModItems.KINETIC_RIFLE_ROUND.get()
                && KineticRounds.kineticOf(ModItems.SHOTGUN_SHELL.get()) == ModItems.KINETIC_SHOTGUN_SHELL.get()
                && KineticRounds.kineticOf(ModItems.RIFLE_BELT.get()) == ModItems.KINETIC_RIFLE_BELT.get(), "Falta una versión cinética");
        for (String id : new String[]{"kinetic_rifle_round", "kinetic_shotgun_shell", "kinetic_rifle_belt"}) {
            h.assertTrue(h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, id)).isPresent(),
                    "Falta la receta " + id);
        }
        // todas las armas de balas pueden usar cinéticas
        for (var gun : new net.minecraft.world.item.Item[]{ModItems.PUMP_SHOTGUN.get(), ModItems.ASSAULT_RIFLE.get(), ModItems.LIGHT_MACHINE_GUN.get(),
                ModItems.KINETIC_SNIPER.get(), ModItems.MINIGUN.get(), ModItems.REVOLVER.get(), ModItems.RAIL_RIFLE.get()}) {
            h.assertTrue(((com.bryanamaya.tecnocinetico.item.weapon.LongGunItem) gun).kineticCapable(), "No admite cinéticas: " + gun);
        }
        // un cargador de rifle se llena con las cinéticas y queda marcado
        FakePlayer p = player(h);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SHORT_RIFLE_MAGAZINE.get()));
        p.getInventory().add(new ItemStack(ModItems.KINETIC_RIFLE_ROUND.get(), 2));                // (después: si no, la mano las tapa)
        int got = MagazineItem.fillFromInventory(p, p.getMainHandItem(), false);
        h.assertTrue(got == 10 && MagazineItem.isKinetic(p.getMainHandItem()), "El cargador de rifle no se llenó de cinéticas: " + got);
        h.assertTrue(((MagazineItem) ModItems.MINIGUN_BOX.get()).accepts(ModItems.KINETIC_RIFLE_BELT.get()), "La caja de la minigun no admite la cinta cinética");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void sixteenSupplySlots(GameTestHelper h) {
        h.assertTrue(TacticalGear.AMMO_COUNT == 16, "Deberían ser 16 ranuras de munición");
        int last = TacticalGear.AMMO_START + TacticalGear.AMMO_COUNT - 1;
        for (var item : new net.minecraft.world.item.Item[]{ModItems.RIFLE_ROUND.get(), ModItems.RIFLE_MAGAZINE.get(), ModItems.KINETIC_GRENADE.get(),
                ModItems.GUIDED_ROCKET.get(), ModItems.PULSE_CELL.get(), ModItems.PLASMA_CELL.get(), ModItems.FUEL_CANISTER.get(),
                ModItems.KINETIC_FUEL_CELL.get(), ModItems.VISOR_BATTERY.get(), ModItems.DASH_CELL.get(), Items.ARROW, Items.FIREWORK_ROCKET}) {
            h.assertTrue(TacticalGear.accepts(last, new ItemStack(item)), "La ranura de munición no acepta " + item);
        }
        h.assertFalse(TacticalGear.accepts(last, new ItemStack(Items.BREAD)), "La comida no va en munición");
        h.assertFalse(TacticalGear.accepts(TacticalGear.FOOD_START, new ItemStack(ModItems.FUEL_CANISTER.get())), "El combustible no va en comida");
        // el jetpack reposta solo desde una ranura de munición
        FakePlayer p = player(h);
        TacticalGear g = TacticalGear.of(p).orElseThrow();
        ItemStack jet = new ItemStack(ModItems.JETPACK.get());
        jet.setDamageValue(jet.getMaxDamage() - 2);
        g.items().setStackInSlot(TacticalGear.BACK, jet);
        g.items().setStackInSlot(last, new ItemStack(ModItems.FUEL_CANISTER.get(), 2));
        h.assertTrue(GadgetFuel.autoRefuel(p, g, TacticalGear.BACK) && g.get(last).getCount() == 1,
                "El jetpack no repostó desde la ranura de munición");
        h.succeed();
    }
}
