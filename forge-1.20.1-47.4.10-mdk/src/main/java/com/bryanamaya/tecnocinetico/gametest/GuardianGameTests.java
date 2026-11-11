package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.block.entity.CoreConsoleBlockEntity;
import com.bryanamaya.tecnocinetico.entity.ModEntities;
import com.bryanamaya.tecnocinetico.entity.boss.OverworldGuardianEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class GuardianGameTests {
    private static BlockPos air(GameTestHelper h) {
        BlockPos p = h.absolutePos(BlockPos.ZERO);
        return new BlockPos(p.getX(), h.getLevel().getHeight(Heightmap.Types.MOTION_BLOCKING, p.getX(), p.getZ()) + 12, p.getZ());
    }

    /** Consolas persistentes: jefe correcto, una sola activación, abandono y victoria que restaura la salida. */
    @GameTest(template = "empty", batch = "guardianes")
    public static void guardianConsoles(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos p = air(h), seal = p.east(2).above(3);
        for (String id : new String[]{"network_watcher", "swarm_matriarch"}) {
            level.setBlock(p, ModBlocks.CORE_CONSOLE.get().defaultBlockState(), 3);
            level.setBlock(seal, Blocks.COPPER_BLOCK.defaultBlockState(), 3);
            var c = (CoreConsoleBlockEntity) level.getBlockEntity(p);
            BlockPos spawn = p.south(7).above(id.equals("network_watcher") ? 4 : 0);
            c.configureGuardian(id, spawn);
            c.setSeal(java.util.List.of(seal));
            c.load(c.saveWithoutMetadata());
            h.assertTrue(c.guardian().equals(id), "La consola perdió el tipo de guardián al cargar NBT");
            var player = h.makeMockPlayer();
            AABB box = new AABB(spawn).inflate(4);
            try {
                c.activate(player); c.activate(player);
                var bosses = level.getEntitiesOfClass(OverworldGuardianEntity.class, box);
                h.assertTrue(c.isActive() && bosses.size() == 1, "La consola no invocó exactamente un guardián");
                var boss = bosses.get(0);
                h.assertTrue(boss.isWatcher() == id.equals("network_watcher"), "Se invocó el jefe equivocado");
                h.assertTrue(level.getBlockState(seal).is(ModBlocks.CORE_BARRIER.get()), "No se selló la arena");
                boss.discard();
                h.assertTrue(!c.isActive() && !c.isBeaten() && level.getBlockState(seal).is(Blocks.COPPER_BLOCK),
                        "Abandonar no restauró la salida ni permitió reintentar");
                c.activate(player);
                boss = level.getEntitiesOfClass(OverworldGuardianEntity.class, box).get(0);
                boss.kill();
                h.assertTrue(c.isBeaten() && !c.isActive() && level.getBlockState(seal).is(Blocks.COPPER_BLOCK),
                        "Vencer al jefe no restauró la salida");
                boss.discard();
                c.activate(player);
                h.assertTrue(level.getEntitiesOfClass(OverworldGuardianEntity.class, box).isEmpty(), "La consola vencida permite duplicar jefes");
            } finally {
                level.getEntitiesOfClass(OverworldGuardianEntity.class, box).forEach(OverworldGuardianEntity::discard);
                level.removeBlock(p, false); level.removeBlock(seal, false);
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 260, batch = "guardianes_combate")
    public static void watcherAttacks(GameTestHelper h) { combat(h, true); }

    @GameTest(template = "empty", timeoutTicks = 260, batch = "guardianes_combate")
    public static void matriarchAttacks(GameTestHelper h) { combat(h, false); }

    private static void combat(GameTestHelper h, boolean watcher) {
        var level = h.getLevel();
        BlockPos p = air(h);
        // Un suelo propio y acotado para la embestida; las entidades están en el chunk activo del test.
        if (!watcher) for (int x = -2; x <= 12; x++) for (int z = -5; z <= 5; z++)
            level.setBlock(p.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
        var boss = (watcher ? ModEntities.NETWORK_WATCHER.get() : ModEntities.SWARM_MATRIARCH.get()).create(level);
        boss.moveTo(p.getX() + .5, p.getY() + (watcher ? 4 : 0), p.getZ() + .5, 0, 0);
        level.addFreshEntity(boss);
        var pig = EntityType.PIG.create(level);
        pig.moveTo(p.getX() + 7.5, p.getY(), p.getZ() + .5, 0, 0);
        pig.setNoAi(true); pig.setNoGravity(true);
        level.addFreshEntity(pig);
        boss.setTarget(pig);
        h.runAfterDelay(220, () -> {
            boolean hit = pig.getHealth() < pig.getMaxHealth() && pig.getLastHurtByMob() == boss;
            boolean alive = boss.isAlive();
            boss.discard(); pig.discard();
            if (!watcher) for (int x = -2; x <= 12; x++) for (int z = -5; z <= 5; z++) level.removeBlock(p.offset(x, -1, z), false);
            h.assertTrue(hit && alive, "El guardián no logró dañar al enemigo con su ataque real: " + (watcher ? "Vigía" : "Matriarca"));
            h.succeed();
        });
    }
}
