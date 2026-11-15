package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.effect.ModEffects;
import com.bryanamaya.tecnocinetico.end.EndArena;
import com.bryanamaya.tecnocinetico.end.EndArenaData;
import com.bryanamaya.tecnocinetico.end.EndRelayTurretEntity;
import com.bryanamaya.tecnocinetico.gear.NetherGadgetItem;
import com.bryanamaya.tecnocinetico.gear.TacticalGear;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/** End, parte 2: torres rotas, despertar, Dragón modificado con torretas, EMP, lluvia de datos, botín y acoplador. */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class EndArenaGameTests {

    @GameTest(template = "empty", timeoutTicks = 900, batch = "end_arena")
    public static void endArenaEvent(GameTestHelper h) {
        var s = h.getLevel().getServer().getLevel(Level.END);
        h.assertTrue(s != null && s.getDragonFight() != null, "Falta el End o su pelea del Dragón");
        EndArena.reset(s);
        // sin jugadores el End no carga la arena: se fuerzan el centro y las torres durante la prueba
        List<net.minecraft.world.level.ChunkPos> forced = new java.util.ArrayList<>();
        forced.add(new net.minecraft.world.level.ChunkPos(0, 0));
        for (SpikeFeature.EndSpike sp : SpikeFeature.getSpikesForLevel(s)) {
            forced.add(new net.minecraft.world.level.ChunkPos(new BlockPos(sp.getCenterX(), 0, sp.getCenterZ())));
        }
        forced.forEach(c -> s.setChunkForced(c.x, c.z, true));
        // torre rota: sin punta ni cristal
        List<SpikeFeature.EndSpike> spikes = SpikeFeature.getSpikesForLevel(s);
        SpikeFeature.EndSpike spike = spikes.get(0);
        s.getChunk(new BlockPos(spike.getCenterX(), 0, spike.getCenterZ()));
        EndArena.ruinSpike(s, spike, 0);
        h.assertTrue(!s.getBlockState(new BlockPos(spike.getCenterX(), spike.getHeight(), spike.getCenterZ())).is(Blocks.OBSIDIAN)
                        && s.getEntitiesOfClass(EndCrystal.class, spike.getTopBoundingBox().inflate(4)).isEmpty()
                        && EndArena.towerTop(s, spike) < spike.getHeight() - 5,
                "La torre no está rota o conserva su cristal");
        // despertar casi terminado: el siguiente tick del End saca al Dragón y las torretas
        EndArenaData data = EndArenaData.get(s);
        EndArena.startAwakening(s, data);
        data.timer = EndArena.AWAKEN_TICKS - 1;
        var player = new net.minecraftforge.common.util.FakePlayer(s, new GameProfile(UUID.randomUUID(), "end-arena"));
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(20, 80, 20, 0, 0);
        h.succeedWhen(() -> {
            EnderDragon d = EndArena.dragon(s);
            h.assertTrue(d != null && data.state == EndArenaData.State.FIGHT, "No sale el Dragón al terminar el despertar");
            var turrets = s.getEntitiesOfClass(EndRelayTurretEntity.class, new AABB(BlockPos.ZERO).inflate(160, 256, 160));
            h.assertTrue(turrets.size() >= spikes.size() - 2, "Faltan torretas-repetidor: " + turrets.size());   // chunks recién forzados
            try {
                h.assertTrue(EndArena.isCorrupted(d) && d.getMaxHealth() == EndArena.DRAGON_HEALTH
                        && d.getUUID().equals(s.getDragonFight().dragonUUID) && !s.getDragonFight().dragonKilled,
                        "El Dragón no está modificado o la pelea vanilla no lo sigue");
                // la torreta más cercana cura al Dragón
                EndRelayTurretEntity t = turrets.get(0);
                d.moveTo(t.getX(), t.getY() + 6, t.getZ() + 3);
                d.setHealth(300);
                h.assertTrue(t.healStep(d) && d.getHealth() > 300, "La torreta no cura al Dragón");
                // Pulso EMP: el jetpack se apaga, un gadget Mk III no
                s.addNewPlayer(player);
                EndArena.empPulse(s, player.position());
                ItemStack mk3 = new ItemStack(ModItems.ANCHOR_BOOTS.get());
                NetherGadgetItem.setLevel(mk3, 3);
                h.assertTrue(player.hasEffect(ModEffects.EMP.get()) && TacticalGear.jammed(player, new ItemStack(ModItems.JETPACK.get()))
                        && !TacticalGear.jammed(player, mk3), "El EMP no apaga los gadgets o apaga los Mk III");
                // Lluvia de datos: el impacto daña
                var zombie = EntityType.ZOMBIE.create(s);
                zombie.moveTo(player.getX(), player.getY(), player.getZ());
                s.addFreshEntity(zombie);
                float hp = zombie.getHealth();
                data.strikes.add(new EndArenaData.Strike(zombie.blockPosition(), s.getGameTime()));
                EndArena.tickStrikes(s, data, s.getGameTime());
                h.assertTrue(zombie.getHealth() < hp, "La Lluvia de datos no hace daño");
                zombie.discard();
                // botín: por jugador que luchó
                List<ItemEntity> loot = EndArena.dropLoot(s, new Vec3(0, 90, 0), 2);
                h.assertTrue(loot.size() == 8 && loot.stream().filter(i -> i.getItem().is(ModItems.END_COUPLER.get())).count() == 2,
                        "El botín no es uno por jugador");
                loot.forEach(Entity::discard);
                turrets.forEach(Entity::discard);
            } catch (net.minecraft.gametest.framework.GameTestAssertException e) {
                throw new IllegalStateException(e.getMessage());                  // falla ya, sin reintentar con el estado borrado
            } finally {
                turrets.forEach(Entity::discard);
                d.discard();
                EndArena.reset(s);
                s.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
                forced.forEach(c -> s.setChunkForced(c.x, c.z, false));
            }
        });
    }

    /** Acoplador del End en el Ensamblador Industrial (pide el del Nether antes) y gadgets Mk III. */
    @GameTest(template = "empty")
    public static void endCouplerAndMk3(GameTestHelper h) {
        ItemStack fighter = new ItemStack(ModItems.CORE_FIGHTER.get());
        List<ItemStack> extras = List.of(new ItemStack(ModItems.END_COUPLER.get()), new ItemStack(ModItems.NULLITE_INGOT.get(), 12),
                new ItemStack(ModItems.DATA_CORE.get()), new ItemStack(ModItems.CORE_PROCESSOR.get(), 2));
        var slots = new java.util.ArrayList<ItemStack>(List.of(fighter));
        slots.addAll(extras);
        h.assertTrue(com.bryanamaya.tecnocinetico.machine.MachineRecipes.assembly(slots) == null,
                "Se instala el Acoplador del End sin el del Nether");
        fighter.getOrCreateTag().putBoolean("NetherDrive", true);
        var recipe = com.bryanamaya.tecnocinetico.machine.MachineRecipes.assembly(slots);
        h.assertTrue(recipe != null && recipe.result(slots).getTag().getBoolean("EndDrive")
                && recipe.result(slots).getTag().getBoolean("NetherDrive"), "No se instala el Acoplador del End");
        ItemStack mk3 = new ItemStack(ModItems.THERMAL_COOLER.get());
        int base = mk3.getMaxDamage();
        NetherGadgetItem.setLevel(mk3, 3);
        h.assertTrue(NetherGadgetItem.level(mk3) == 3 && mk3.getMaxDamage() == base * 3, "El Mk III no triplica la batería");
        h.succeed();
    }
}
