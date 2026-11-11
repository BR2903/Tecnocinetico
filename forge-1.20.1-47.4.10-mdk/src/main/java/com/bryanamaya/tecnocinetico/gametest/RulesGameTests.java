package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.ModEntities;
import com.bryanamaya.tecnocinetico.entity.boss.SiegeSpiderEntity;
import com.bryanamaya.tecnocinetico.entity.custom.CoreShots;
import com.bryanamaya.tecnocinetico.entity.custom.DroneEntity;
import com.bryanamaya.tecnocinetico.entity.ship.ShipMissileEntity;
import com.bryanamaya.tecnocinetico.rules.FriendlyFire;
import net.minecraft.core.BlockPos;
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

    /** El jefe y sus drones no se hieren ni se provocan; sus balas ignoran al aliado que cruza la línea. */
    @GameTest(template = "empty", batch = "reglas")
    public static void siegeSpiderAndDronesAreAllies(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos col = helper.absolutePos(BlockPos.ZERO);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                col.getX(), col.getZ()) + 16;
        var boss = ModEntities.SIEGE_SPIDER.get().create(level);
        var drone = ModEntities.DRONE.get().create(level);
        var pig = net.minecraft.world.entity.EntityType.PIG.create(level);
        var missile = new ShipMissileEntity(level, boss, pig);
        try {
            for (var mob : java.util.List.of(boss, drone, pig)) {
                mob.setNoAi(true);
                mob.setNoGravity(true);
                mob.moveTo(col.getX() + 1.5, y, col.getZ() + 1.5, 0, 0);
                level.addFreshEntity(mob);
            }
            boss.setTarget(pig);
            drone.engage(pig);
            float bossHealth = boss.getHealth(), droneHealth = drone.getHealth();
            helper.assertFalse(drone.hurt(level.damageSources().mobAttack(boss), 12), "El jefe dañó al dron cuerpo a cuerpo");
            helper.assertFalse(drone.hurt(level.damageSources().mobProjectile(boss, boss), 10), "La bala del jefe dañó al dron");
            helper.assertFalse(drone.hurt(level.damageSources().explosion(missile, boss), 20), "El misil del jefe dañó al dron");
            helper.assertFalse(boss.hurt(level.damageSources().mobProjectile(drone, drone), 10), "El dron dañó al jefe");
            drone.engage(boss);
            drone.setTarget(boss);
            boss.setTarget(drone);
            helper.assertTrue(drone.getTarget() == pig && boss.getTarget() == pig, "Un aliado sustituyó al objetivo del combate");
            helper.assertTrue(drone.getLastHurtByMob() == null && boss.getLastHurtByMob() == null,
                    "El daño aliado dejó un agresor para la represalia");
            helper.assertTrue(boss.getHealth() == bossHealth && drone.getHealth() == droneHealth, "El fuego aliado quitó vida");

            // El dron se interpone entre la torreta del jefe y un enemigo: la bala debe seguir hasta el enemigo.
            var from = new net.minecraft.world.phys.Vec3(col.getX() + 0.5, y + 1, col.getZ() + 1.5);
            drone.moveTo(from.x + 3, y + 0.8, from.z, 0, 0);
            pig.moveTo(from.x + 8, y + 0.5, from.z, 0, 0);
            CoreShots.fire(boss, from, new net.minecraft.world.phys.Vec3(1, 0, 0), 10, 2, 0, SiegeSpiderEntity.class, false);
            helper.assertTrue(pig.getHealth() < pig.getMaxHealth() && pig.getLastHurtByMob() == boss,
                    "La bala del jefe no atravesó al dron aliado para alcanzar al enemigo");
            helper.assertTrue(drone.getHealth() == droneHealth, "La bala hirió al dron interpuesto");

            // Comprobar también el trayecto inverso, con el jefe interpuesto ante el dron.
            boss.moveTo(from.x + 3, y, from.z, 0, 0);
            pig.invulnerableTime = 0;
            CoreShots.fire(drone, from, new net.minecraft.world.phys.Vec3(1, 0, 0), 10, 2, 0, DroneEntity.class, true);
            helper.assertTrue(pig.getLastHurtByMob() == drone && boss.getHealth() == bossHealth,
                    "El disparo del dron no ignoró al jefe interpuesto");

            var player = helper.makeMockPlayer();
            helper.assertTrue(drone.hurt(level.damageSources().playerAttack(player), 2), "El jugador ya no puede dañar al dron");
            helper.assertTrue(drone.getTarget() == player, "El dron dejó de responder al jugador que lo ataca");
            helper.assertTrue(boss.hurt(level.damageSources().playerAttack(player), 2), "El jugador ya no puede dañar al jefe");
            helper.succeed();
        } finally {
            boss.discard();
            drone.discard();
            pig.discard();
            missile.discard();
        }
    }

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

    /** Recetas del Ensamblador de Armas con los mismos tipos: cada una sale con sus cantidades (antes ganaba la primera). */
    @GameTest(template = "empty", batch = "reglas")
    public static void assemblerPicksExactRecipe(GameTestHelper helper) {
        for (var r : com.bryanamaya.tecnocinetico.block.entity.AssemblerRecipes.all()) {
            var inv = new net.minecraftforge.items.ItemStackHandler(6);
            for (int i = 0; i < r.inputs().size(); i++) {
                inv.setStackInSlot(i, r.inputs().get(i).copy());
            }
            var found = com.bryanamaya.tecnocinetico.block.entity.AssemblerRecipes.find(inv, 6);
            helper.assertTrue(found == r, "con los ingredientes de " + r.output().getHoverName().getString()
                    + " sale " + (found == null ? "nada" : found.output().getHoverName().getString()));
        }
        helper.succeed();
    }
}
