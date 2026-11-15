package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.end.NetworkHack;
import com.bryanamaya.tecnocinetico.entity.ModEntities;
import com.bryanamaya.tecnocinetico.gear.TacticalGear;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.mojang.authlib.GameProfile;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Intrusor de Red: hackear apuntando, recarga larga, la máquina lucha contra enemigos y nunca daña a jugadores. */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class NetworkHackGameTests {

    @GameTest(template = "empty", timeoutTicks = 120, batch = "intrusor")
    public static void hackTurnsMachineIntoAlly(GameTestHelper h) {
        var level = h.getLevel();
        Vec3 base = h.absoluteVec(new Vec3(1.5, 2, 1.5));
        Mob body = ModEntities.SWARM_BODY.get().create(level);
        body.moveTo(base.x + 5, base.y, base.z, 0, 0);
        body.setNoAi(false);
        level.addFreshEntity(body);
        var zombie = EntityType.ZOMBIE.create(level);
        zombie.moveTo(base.x + 8, base.y, base.z + 2, 0, 0);
        zombie.setNoAi(true);
        level.addFreshEntity(zombie);
        FakePlayer p = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "hacker"));
        p.setGameMode(GameType.SURVIVAL);
        p.moveTo(base.x, base.y, base.z, 0, 0);
        p.lookAt(EntityAnchorArgument.Anchor.EYES, body.getBoundingBox().getCenter());
        p.setYHeadRot(p.getYRot());                                             // la mirada del jugador usa la cabeza
        TacticalGear g = TacticalGear.of(p).orElseThrow();
        g.items().setStackInSlot(TacticalGear.WRIST_RIGHT, new ItemStack(ModItems.NETWORK_INTRUDER.get()));
        h.assertTrue(TacticalGear.accepts(TacticalGear.WRIST_RIGHT, new ItemStack(ModItems.NETWORK_INTRUDER.get()))
                && TacticalGear.accepts(TacticalGear.WRIST, new ItemStack(ModItems.NETWORK_INTRUDER.get())), "El intrusor va en cualquier muñeca");
        h.assertTrue(NetworkHack.aimed(p) == body, "No apunta a la máquina");
        NetworkHack.activate(p);
        h.assertTrue(NetworkHack.channeling(p), "No empezó a hackear");
        // los jefes no se pueden hackear
        h.assertFalse(ModEntities.SIEGE_SPIDER.get().is(NetworkHack.HACKABLE), "La Araña de Asedio no debería ser hackeable");
        h.runAfterDelay(NetworkHack.CHANNEL + 2, () -> {
            NetworkHack.playerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, p));   // el falso jugador no está en el mundo
            h.assertTrue(NetworkHack.hacked(body), "La máquina no quedó hackeada");
            ItemStack gadget = g.get(TacticalGear.WRIST_RIGHT);
            h.assertTrue(NetworkHack.cooldownLeft(gadget, level.getGameTime()) > NetworkHack.COOLDOWN - 40, "No empezó la recarga");
            NetworkHack.activate(p);
            h.assertFalse(NetworkHack.channeling(p), "En recarga no debería volver a hackear");
            // nada de la máquina hackeada daña a un jugador
            var event = new LivingAttackEvent(p, level.damageSources().mobAttack(body), 6.0F);
            NetworkHack.attack(event);
            h.assertTrue(event.isCanceled(), "La máquina hackeada podría dañar a un jugador");
        });
        h.runAfterDelay(NetworkHack.CHANNEL + 20, () -> {
            h.assertTrue(body.getTarget() == zombie, "La máquina hackeada no ataca al zombi: " + body.getTarget());
            body.discard();
            zombie.discard();
            h.succeed();
        });
    }
}
