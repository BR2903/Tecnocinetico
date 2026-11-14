package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.item.custom.KineticRounds;
import com.bryanamaya.tecnocinetico.item.custom.MagazineItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Bala Normal y Bala Cinética: un tipo por cargador, y el impacto cinético atraviesa la armadura. */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class RoundTypeGameTests {

    @GameTest(template = "empty")
    public static void magazineRoundTypes(GameTestHelper helper) {
        var p = net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),            // supervivencia: gasta paquetes
                new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("mags".getBytes()), "[mags]"));
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.getInventory().clearContent();
        // solo normales: el cargador se llena de normales
        p.getInventory().add(new ItemStack(ModItems.STANDARD_ROUND.get(), 2));
        ItemStack mag = new ItemStack(ModItems.MAGAZINE.get());                             // en la mano (no en el inventario)
        int got = MagazineItem.fillFromInventory(p, mag, false);
        helper.assertTrue(got == 12 && !MagazineItem.isKinetic(mag), "No se llena con balas normales: " + got);
        // con cinéticas en el inventario, un cargador VACÍO se llena de cinéticas
        p.getInventory().clearContent();
        p.getInventory().add(new ItemStack(ModItems.STANDARD_ROUND.get(), 2));
        p.getInventory().add(new ItemStack(ModItems.KINETIC_ROUND.get(), 1));
        ItemStack mag2 = new ItemStack(ModItems.MAGAZINE.get());
        got = MagazineItem.fillFromInventory(p, mag2, false);
        helper.assertTrue(got == 6 && MagazineItem.isKinetic(mag2), "Un cargador vacío debería llenarse con cinéticas: " + got);
        // ya con cinéticas: no mezcla normales (no quedan cinéticas -> no mete nada)
        got = MagazineItem.fillFromInventory(p, mag2, false);
        helper.assertTrue(got == 0 && MagazineItem.getRounds(mag2) == 6, "Mezcla normales en un cargador de cinéticas");
        // vaciado: pierde la marca
        MagazineItem.setContents(mag2, 0, 0);
        helper.assertTrue(!MagazineItem.isKinetic(mag2), "Un cargador vacío conserva la marca de cinéticas");
        p.getInventory().clearContent();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void kineticImpactPierces(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = net.minecraftforge.common.util.FakePlayerFactory.get(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("rounds".getBytes()), "[rounds]"));
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        var a = EntityType.ZOMBIE.create(level);
        var b = EntityType.ZOMBIE.create(level);
        for (var z : java.util.List.of(a, b)) {
            z.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE));
            z.setNoAi(true);
        }
        a.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        b.moveTo(at.getX() + 3.5, at.getY(), at.getZ() + 0.5);
        level.addFreshEntity(a);
        level.addFreshEntity(b);
        float hpA = a.getHealth(), hpB = b.getHealth();
        a.hurt(level.damageSources().playerAttack(player), 8.0F);                           // bala normal
        b.hurt(level.damageSources().playerAttack(player), 8.0F);                           // bala cinética
        KineticRounds.onHit(level, player, b, 8.0F, new Vec3(1, 0, 0));
        float lostA = hpA - a.getHealth(), lostB = hpB - b.getHealth();
        a.discard();
        b.discard();
        helper.assertTrue(lostB >= lostA + 2.5F, "El impacto cinético no añade daño que atraviesa la armadura: " + lostA + " / " + lostB);
        helper.succeed();
    }

    /** Herramientas de Inercita: nivel de diamante, pican la obsidiana y la espada aturde. */
    @GameTest(template = "empty")
    public static void inercitaTools(GameTestHelper helper) {
        ItemStack pick = new ItemStack(ModItems.INERCITA_PICKAXE.get());
        helper.assertTrue(pick.isCorrectToolForDrops(net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState()),
                "El pico de inercita no pica obsidiana");
        helper.assertTrue(pick.getDestroySpeed(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())
                > new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE).getDestroySpeed(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()),
                "El pico de inercita debería ser más rápido que el de diamante");
        var level = helper.getLevel();
        var player = net.minecraftforge.common.util.FakePlayerFactory.get(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("sword".getBytes()), "[sword]"));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.INERCITA_SWORD.get()));
        var zombie = EntityType.ZOMBIE.create(level);
        com.bryanamaya.tecnocinetico.item.custom.InercitaTools.hurt(new net.minecraftforge.event.entity.living.LivingHurtEvent(zombie,
                level.damageSources().playerAttack(player), 4.0F));
        boolean stunned = zombie.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(stunned, "La espada de inercita no aturde");
        helper.succeed();
    }
}
