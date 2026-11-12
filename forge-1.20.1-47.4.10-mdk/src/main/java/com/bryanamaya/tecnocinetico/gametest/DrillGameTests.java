package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.item.custom.CombustionDrillItem;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Taladro de Combustión: 3x3, un bloque agachado, obsidiana, gasolina y repostaje. */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class DrillGameTests {

    @GameTest(template = "empty", batch = "taladro")
    public static void drillBreaks3x3AndUsesFuel(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos base = h.absolutePos(BlockPos.ZERO).atY(205);
        BlockPos center = base.south(3);
        AABB box = new AABB(center).inflate(5);
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "taladro"));
        // mira al centro de la pared (yaw 0 = sur, +Z), con los ojos a la altura del bloque central
        player.moveTo(base.getX() + 0.5, center.getY() + 0.5 - player.getEyeHeight(), base.getZ() + 0.5, 0, 0);
        ItemStack drill = new ItemStack(ModItems.COMBUSTION_DRILL.get());
        CombustionDrillItem.setFuel(drill, CombustionDrillItem.TANK);
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        try {
            wall(level, center, Blocks.STONE.defaultBlockState());
            // 1) 3x3 en el plano de la pared, ni un bloque más
            player.gameMode.destroyBlock(center);
            int broken = 0;
            for (BlockPos p : BlockPos.betweenClosed(center.offset(-2, -2, 0), center.offset(2, 2, 0))) {
                boolean inside = Math.abs(p.getX() - center.getX()) <= 1 && Math.abs(p.getY() - center.getY()) <= 1;
                if (level.getBlockState(p).isAir()) {
                    broken++;
                    h.assertTrue(inside, "Rompió un bloque fuera del 3x3: " + p);
                }
            }
            h.assertTrue(broken == 9, "El 3x3 rompió " + broken + " bloques");
            h.assertTrue(level.getBlockState(center.south()).is(Blocks.STONE), "Rompió en profundidad en vez de en el plano");
            h.assertTrue(CombustionDrillItem.fuel(drill) == CombustionDrillItem.TANK - 9, "Gasolina gastada: "
                    + (CombustionDrillItem.TANK - CombustionDrillItem.fuel(drill)) + " (se esperaban 9)");
            h.assertTrue(!level.getEntitiesOfClass(ItemEntity.class, box).isEmpty(), "Los bloques rotos no soltaron nada");

            // 2) agachado: un solo bloque
            wall(level, center, Blocks.STONE.defaultBlockState());
            player.setShiftKeyDown(true);
            player.gameMode.destroyBlock(center);
            player.setShiftKeyDown(false);
            h.assertTrue(level.getBlockState(center).isAir() && level.getBlockState(center.east()).is(Blocks.STONE),
                    "Agachado no rompe un solo bloque");

            // 3) obsidiana: la pica; picando piedra no se lleva la obsidiana de al lado
            wall(level, center, Blocks.STONE.defaultBlockState());
            level.setBlock(center.east(), Blocks.OBSIDIAN.defaultBlockState(), 3);
            player.gameMode.destroyBlock(center);
            h.assertTrue(level.getBlockState(center.east()).is(Blocks.OBSIDIAN), "Picando piedra rompió la obsidiana vecina");
            h.assertTrue(drill.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState())
                    && drill.getDestroySpeed(Blocks.OBSIDIAN.defaultBlockState()) == CombustionDrillItem.SPEED, "No pica obsidiana");
            h.assertFalse(drill.isCorrectToolForDrops(Blocks.BEDROCK.defaultBlockState()), "Picaría el lecho de roca");

            // 4) sin gasolina no sirve
            CombustionDrillItem.setFuel(drill, 0);
            h.assertFalse(drill.isCorrectToolForDrops(Blocks.STONE.defaultBlockState()) || drill.getDestroySpeed(Blocks.STONE.defaultBlockState()) > 1,
                    "Sin gasolina sigue picando");

            // 5) repostar: 3 depósitos llenan los 300; con el tanque casi lleno no gasta ninguno
            player.getInventory().add(new ItemStack(ModItems.FUEL_CANISTER.get(), 5));
            player.setItemInHand(InteractionHand.MAIN_HAND, drill);
            CombustionDrillItem.startRefuel(player, drill);
            h.assertTrue(CombustionDrillItem.isRefueling(drill) && drill.getDestroySpeed(Blocks.STONE.defaultBlockState()) == 0
                    && CombustionDrillItem.fuel(drill) == 0, "Repostar debe tardar y no picar mientras");
            drill.getTag().putLong("RefuelEnd", level.getGameTime());                                       // ya pasó el tiempo
            drill.inventoryTick(level, player, 0, true);
            h.assertTrue(CombustionDrillItem.fuel(drill) == CombustionDrillItem.TANK && count(player) == 2,
                    "Repostar: gasolina " + CombustionDrillItem.fuel(drill) + ", depósitos " + count(player));
            CombustionDrillItem.setFuel(drill, CombustionDrillItem.TANK - 20);
            CombustionDrillItem.refuel(player, drill);
            h.assertTrue(count(player) == 2, "Gastó un depósito con el tanque casi lleno");
        } finally {
            for (BlockPos p : BlockPos.betweenClosed(center.offset(-3, -3, -1), center.offset(3, 3, 2))) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
            level.getEntitiesOfClass(Entity.class, box, e -> e instanceof ItemEntity
                    || e instanceof net.minecraft.world.entity.ExperienceOrb).forEach(Entity::discard);
        }
        h.succeed();
    }

    /** Pared de 5x5 (y una capa detrás) delante del jugador. */
    private static void wall(net.minecraft.server.level.ServerLevel level, BlockPos center, net.minecraft.world.level.block.state.BlockState state) {
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-2, -2, 0), center.offset(2, 2, 1))) {
            level.setBlock(p, state, 3);
        }
    }

    private static int count(FakePlayer player) {
        int n = 0;
        for (ItemStack s : player.getInventory().items) {
            if (s.is(ModItems.FUEL_CANISTER.get())) {
                n += s.getCount();
            }
        }
        return n;
    }
}
