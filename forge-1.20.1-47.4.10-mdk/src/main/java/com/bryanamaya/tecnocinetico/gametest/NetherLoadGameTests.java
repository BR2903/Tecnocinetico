package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.nether.NetherMobEvents;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Nether con muchas entidades: todos los piglins armados y el coste por tick de las mejoras. */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class NetherLoadGameTests {

    /** Presupuesto: las mejoras del Nether no deben pasar de este tiempo medio por tick con la horda de abajo. */
    private static final double BUDGET_MS = 5.0;

    /**
     * 300 piglins zombificados (un tercio bebés), 60 piglins y 20 brutos alrededor de un jugador en campo abierto, todos
     * hostiles y viéndolo (el peor caso: hasta los de fusil disparando). Comprueba que TODOS llevan arma y mide el
     * tiempo de {@code NetherMobEvents.enemyTick} durante 3 s simulados.
     */
    @GameTest(template = "empty", batch = "nether_carga", timeoutTicks = 200)
    public static void hordeIsArmedAndCheap(GameTestHelper h) {
        var nether = h.getLevel().getServer().getLevel(Level.NETHER);
        h.assertTrue(nether != null, "Falta el Nether");
        BlockPos at = new BlockPos(1928, 80, 1928);
        for (int cx = -2; cx <= 2; cx++) {
            for (int cz = -2; cz <= 2; cz++) {
                nether.getChunk((at.getX() >> 4) + cx, (at.getZ() >> 4) + cz);
            }
        }
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-20, -1, -20), at.offset(20, 4, 20))) {   // explanada abierta
            nether.setBlock(p, (p.getY() == at.getY() - 1 ? Blocks.BLACKSTONE : Blocks.AIR).defaultBlockState(), 2);
        }
        var player = new FakePlayer(nether, new GameProfile(UUID.randomUUID(), "nether-horde"));
        player.setGameMode(GameType.SURVIVAL);
        player.setInvulnerable(true);                                         // recibe los tiros sin morir
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        nether.addNewPlayer(player);
        List<Mob> horde = new ArrayList<>();
        try {
            var random = nether.random;
            for (int i = 0; i < 380; i++) {
                EntityType<? extends Mob> type = i < 300 ? EntityType.ZOMBIFIED_PIGLIN : i < 360 ? EntityType.PIGLIN : EntityType.PIGLIN_BRUTE;
                Mob m = type.create(nether);
                m.moveTo(at.getX() + random.nextInt(31) - 15 + 0.5, at.getY(), at.getZ() + random.nextInt(31) - 15 + 0.5, 0, 0);
                m.setNoAi(true);                                              // solo se mide lo nuestro, no la IA vanilla
                if (i % 3 == 0 && i < 360) {
                    m.setBaby(true);
                }
                nether.addFreshEntity(m);                                     // aquí se aplica la mejora (EntityJoinLevelEvent)
                horde.add(m);
            }
            for (Mob m : horde) {
                h.assertTrue(NetherMobEvents.holdsNetherWeapon(m), "Piglin sin arma: " + m.getType().getDescriptionId()
                        + (m.isBaby() ? " (bebé)" : ""));
            }
            // pierde el arma: se rearma en su siguiente turno
            Mob unarmed = horde.get(0);
            unarmed.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.EMPTY);
            NetherMobEvents.setClock(unarmed, 10);
            NetherMobEvents.enemyTick(new LivingEvent.LivingTickEvent(unarmed));
            h.assertTrue(NetherMobEvents.holdsNetherWeapon(unarmed), "Un piglin desarmado no se rearma");

            int ticks = 60;
            long start = System.nanoTime();
            for (int t = 0; t < ticks; t++) {
                for (Mob m : horde) {
                    m.tickCount = t;
                    NetherMobEvents.enemyTick(new LivingEvent.LivingTickEvent(m));
                }
            }
            double msPerTick = (System.nanoTime() - start) / 1e6 / ticks;
            long hostile = horde.stream().filter(m -> m.getTarget() == player).count();
            Tecnocinetico.LOGGER.info("[GameTest] horda del Nether: {} mobs, {} hostiles, {} ms por tick (presupuesto {} ms)",
                    horde.size(), hostile, String.format("%.3f", msPerTick), BUDGET_MS);
            h.assertTrue(hostile > horde.size() * 0.9, "La horda no se volvió hostil: " + hostile);
            h.assertTrue(msPerTick < BUDGET_MS, String.format("Las mejoras del Nether cuestan %.2f ms por tick", msPerTick));
        } finally {
            horde.forEach(Entity::discard);
            nether.getEntitiesOfClass(Entity.class, new AABB(at).inflate(24), e -> !(e instanceof net.minecraft.world.entity.player.Player))
                    .forEach(Entity::discard);
            nether.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        }
        h.succeed();
    }
}
