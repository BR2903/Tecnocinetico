package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.rules.FriendlyFire;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Reglas de la partida multijugador. */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class RulesGameTests {

    /** Sin fuego amigo un jugador no puede atacar a otro (el ataque se cancela); con fuego amigo, sí. Uno mismo, siempre. */
    @GameTest(template = "empty", batch = "reglas")              // aparte: no mover a las demás pruebas
    public static void friendlyFire(GameTestHelper helper) {
        Player a = helper.makeMockPlayer(), b = helper.makeMockPlayer();
        var src = helper.getLevel().damageSources().playerAttack(a);
        boolean before = FriendlyFire.ENABLED.get();
        try {
            FriendlyFire.ENABLED.set(false);
            helper.assertFalse(ForgeHooks.onPlayerAttack(b, src, 5.0F), "sin fuego amigo el ataque a otro jugador debe cancelarse");
            helper.assertTrue(ForgeHooks.onPlayerAttack(a, src, 5.0F), "herirse a uno mismo no se bloquea");
            FriendlyFire.ENABLED.set(true);
            helper.assertTrue(ForgeHooks.onPlayerAttack(b, src, 5.0F), "con fuego amigo el ataque debe pasar");
        } finally {
            FriendlyFire.ENABLED.set(before);
        }
        helper.succeed();
    }
}
