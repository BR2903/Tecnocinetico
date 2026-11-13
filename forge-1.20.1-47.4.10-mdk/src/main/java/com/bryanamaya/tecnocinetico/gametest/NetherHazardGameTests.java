package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.gear.TacticalGear;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.nether.NetherHazards;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Nether parte 4: calor, ceniza y el equipo de preparación (Refrigerador, Filtro, Botas de Anclaje, Piroxita Abisal). */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class NetherHazardGameTests {

    private static TacticalGear gear(GameTestHelper helper, Player p) {
        var g = TacticalGear.of(p);
        helper.assertTrue(g.isPresent(), "El jugador no tiene Equipo Táctico");
        return g.get();
    }

    private static void seconds(Player p, int n, float heat, boolean ashy) {
        for (int i = 0; i < n; i++) {
            NetherHazards.step(p, heat, ashy);
        }
    }

    @GameTest(template = "empty")
    public static void heatAndCooler(GameTestHelper helper) {
        Player p = helper.makeMockPlayer();
        seconds(p, 10, 1.0F, false);
        helper.assertTrue(Math.abs(NetherHazards.heat(p) - 10) < 0.01F, "El calor no sube: " + NetherHazards.heat(p));
        // con mucho calor: lentitud
        p.getPersistentData().putFloat(NetherHazards.HEAT, 75);
        seconds(p, 1, 1.0F, false);
        helper.assertTrue(p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Sin lentitud con 76 de calor");
        // el Refrigerador Térmico frena el calor, enfría y gasta carga
        ItemStack cooler = new ItemStack(ModItems.THERMAL_COOLER.get());
        gear(helper, p).items().setStackInSlot(TacticalGear.BACK, cooler);
        float before = NetherHazards.heat(p);
        seconds(p, 5, 2.0F, false);
        ItemStack worn = gear(helper, p).get(TacticalGear.BACK);
        helper.assertTrue(NetherHazards.heat(p) < before, "El refrigerador no enfría");
        helper.assertTrue(worn.getDamageValue() > 0, "El refrigerador no gasta carga");
        // sin carga ya no protege
        worn.setDamageValue(worn.getMaxDamage() - 1);
        before = NetherHazards.heat(p);
        seconds(p, 2, 2.0F, false);
        helper.assertTrue(NetherHazards.heat(p) > before, "El refrigerador sin carga sigue protegiendo");
        // fuera del Nether se enfría
        before = NetherHazards.heat(p);
        seconds(p, 3, 0.0F, false);
        helper.assertTrue(NetherHazards.heat(p) < before, "No se enfría fuera del Nether");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void piroxiteArmorHeat(GameTestHelper helper) {
        Player p = helper.makeMockPlayer();
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PIROXITE_HELMET.get()));
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.PIROXITE_CHESTPLATE.get()));
        helper.assertTrue(Math.abs(NetherHazards.heatFactor(p) - 0.6F) < 0.01F, "2 piezas de piroxita no quitan el 40 %");
        for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST}) {
            p.setItemSlot(slot, ItemStack.EMPTY);
        }
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.REFINED_PIROXITE_HELMET.get()));
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.REFINED_PIROXITE_CHESTPLATE.get()));
        p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.REFINED_PIROXITE_LEGGINGS.get()));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.REFINED_PIROXITE_BOOTS.get()));
        seconds(p, 20, 3.0F, false);
        helper.assertTrue(NetherHazards.heat(p) == 0, "La Piroxita Abisal completa no anula el calor");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ashAndFilter(GameTestHelper helper) {
        Player p = helper.makeMockPlayer();
        seconds(p, 10, 0.0F, true);
        helper.assertTrue(NetherHazards.ash(p) > 10, "La ceniza no sube en un bioma de ceniza");
        seconds(p, 40, 0.0F, true);
        helper.assertTrue(p.hasEffect(MobEffects.WEAKNESS), "Sin debilidad con mucha ceniza");
        // a 100: daño (pulmones de ceniza)
        p.getPersistentData().putFloat(NetherHazards.ASH, 100);
        float hp = p.getHealth();
        for (int i = 0; i < 40; i++) {
            p.tickCount++;
            if (p.tickCount % 40 == 0) {
                seconds(p, 1, 0.0F, true);
            }
        }
        helper.assertTrue(p.getHealth() < hp, "La ceniza al máximo no hace daño");
        // el Filtro de Ceniza para la ceniza
        p.getPersistentData().putFloat(NetherHazards.ASH, 20);
        gear(helper, p).items().setStackInSlot(TacticalGear.FACE, new ItemStack(ModItems.ASH_FILTER.get()));
        seconds(p, 10, 0.0F, true);
        helper.assertTrue(NetherHazards.ash(p) < 20, "El Filtro de Ceniza no protege");
        helper.assertTrue(gear(helper, p).get(TacticalGear.FACE).getDamageValue() > 0, "El filtro no gasta carga");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void anchorBoots(GameTestHelper helper) {
        Player p = helper.makeMockPlayer();
        var e = new net.minecraftforge.event.entity.living.LivingKnockBackEvent(p, 1.0F, 1, 0);
        NetherHazards.knockback(e);
        helper.assertTrue(e.getStrength() == 1.0F, "Sin botas no debe cambiar el empuje");
        gear(helper, p).items().setStackInSlot(TacticalGear.FEET, new ItemStack(ModItems.ANCHOR_BOOTS.get()));
        e = new net.minecraftforge.event.entity.living.LivingKnockBackEvent(p, 1.0F, 1, 0);
        NetherHazards.knockback(e);
        helper.assertTrue(e.getStrength() < 0.2F, "Las Botas de Anclaje no reducen el empuje: " + e.getStrength());
        var fall = new net.minecraftforge.event.entity.living.LivingFallEvent(p, 10.0F, 1.0F);
        NetherHazards.fall(fall);
        helper.assertTrue(fall.getDamageMultiplier() < 0.3F, "Las Botas de Anclaje no reducen la caída");
        helper.succeed();
    }
}
