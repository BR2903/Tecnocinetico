package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.effect.ModEffects;
import com.bryanamaya.tecnocinetico.end.DataSpecterEntity;
import com.bryanamaya.tecnocinetico.end.EndEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** End, parte 3: Espectro de Datos (en el End de verdad, lejos de otras pruebas). */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class EndEnemyGameTests {

    private static ServerLevel end(GameTestHelper h) {
        ServerLevel s = h.getLevel().getServer().getLevel(Level.END);
        h.assertTrue(s != null, "Falta el End");
        return s;
    }

    /** Plataforma de 21x21 del bloque dado (suelo en y-1) con aire encima; fuerza los chunks de alrededor. */
    private static void platform(ServerLevel s, BlockPos at, Block floor) {
        force(s, at, true);
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-10, -4, -10), at.offset(10, 10, 10))) {
            s.setBlock(p, (p.getY() < at.getY() ? floor : Blocks.AIR).defaultBlockState(), 2);
        }
    }

    private static void release(ServerLevel s, BlockPos at) {
        force(s, at, false);
    }

    /** 5x5 chunks: el gusano avanza bajo tierra al hundirse y no debe salir de la zona activa. */
    private static void force(ServerLevel s, BlockPos at, boolean on) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                s.setChunkForced((at.getX() >> 4) + dx, (at.getZ() >> 4) + dz, on);
            }
        }
    }

    /** Desfasado nada le da; el pulso daña y corrompe a lo que tiene cerca y luego queda expuesto. */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void dataSpecterPhasesAndPulses(GameTestHelper h) {
        ServerLevel s = end(h);
        BlockPos at = new BlockPos(2216, 70, 2344);
        platform(s, at, Blocks.END_STONE);
        DataSpecterEntity specter = EndEntities.DATA_SPECTER.get().create(s);
        specter.moveTo(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 0, 0);
        s.addFreshEntity(specter);
        var zombie = EntityType.ZOMBIE.create(s);
        zombie.moveTo(at.getX() + 2.5, at.getY(), at.getZ() + 0.5, 0, 0);
        zombie.setNoAi(true);
        s.addFreshEntity(zombie);
        h.assertFalse(specter.hurt(s.damageSources().generic(), 10), "Desfasado no debería recibir daño");
        h.assertFalse(specter.isPickable(), "Desfasado no se puede apuntar");
        specter.testState(DataSpecterEntity.CHARGE, DataSpecterEntity.PULSE);
        h.assertTrue(specter.isPickable(), "Al materializarse ya se puede apuntar");
        h.succeedWhen(() -> {
            h.assertTrue(specter.state() == DataSpecterEntity.VULNERABLE, "No ha descargado el pulso: " + specter.state());
            h.assertTrue(zombie.getHealth() < zombie.getMaxHealth() && zombie.hasEffect(ModEffects.DATA_CORRUPTION.get()),
                    "El pulso no daña ni corrompe");
            float before = specter.getHealth();
            h.assertTrue(specter.hurt(s.damageSources().generic(), 10) && before - specter.getHealth() > 10,
                    "Expuesto debería recibir más daño");
            release(s, at);
            specter.discard();
            zombie.discard();
        });
    }

    /** Aparecen en las islas exteriores, no en la isla central. */
    @GameTest(template = "empty")
    public static void endEnemiesSpawnOnOuterIslands(GameTestHelper h) {
        var biomes = h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        var outer = biomes.getOrThrow(Biomes.END_HIGHLANDS).getMobSettings().getMobs(MobCategory.MONSTER).unwrap();
        var center = biomes.getOrThrow(Biomes.THE_END).getMobSettings().getMobs(MobCategory.MONSTER).unwrap();
        h.assertTrue(outer.stream().anyMatch(d -> d.type == EndEntities.DATA_SPECTER.get()), "Falta en las tierras altas del End");
        h.assertTrue(center.stream().noneMatch(d -> d.type == EndEntities.DATA_SPECTER.get()),
                "No deberían aparecer en la isla central");
        h.assertFalse(EndEntities.outer(new BlockPos(100, 60, 100)), "La isla central no cuenta como exterior");
        h.succeed();
    }
}
