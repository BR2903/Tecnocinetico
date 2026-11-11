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
    /** Persigue al Vigía desde debajo como un atacante cuerpo a cuerpo: debe volar y completar sus cuatro ataques. */
    @GameTest(template = "empty", timeoutTicks = 560, batch = "vigia_vuelo")
    public static void watcherKeepsFlyingAndVariesAttacks(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos p = air(h);
        var watcher = ModEntities.NETWORK_WATCHER.get().create(level);
        watcher.moveTo(p, 0, 0); // como un huevo sobre el suelo: no regalar altura inicial a la prueba
        level.addFreshEntity(watcher);
        var target = EntityType.PIG.create(level);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(2000);
        target.setHealth(2000); target.setNoAi(true); target.setNoGravity(true);
        target.moveTo(p.offset(3, 0, 0), 0, 0); level.addFreshEntity(target);
        watcher.setTarget(target);
        var seen = new java.util.HashSet<Integer>();
        double[] lowest = {Double.MAX_VALUE};
        boolean[] hit = {false};
        for (int tick = 1; tick < 500; tick++) {
            int time = tick;
            h.runAfterDelay(tick, () -> {
                if (watcher.mode() != 0) seen.add(watcher.mode());
                if (target.getHealth() < target.getMaxHealth() && target.getLastHurtByMob() == watcher) hit[0] = true;
                // Simular que el atacante lo sigue en el suelo, incluso mientras el jefe carga o dispara.
                if (time > 120) {
                    var delta = watcher.position().subtract(target.position()).multiply(1, 0, 1);
                    if (delta.length() > .28) delta = delta.normalize().scale(.28);
                    target.setPos(target.getX() + delta.x, p.getY(), target.getZ() + delta.z);
                }
                if (time > 100) lowest[0] = Math.min(lowest[0], watcher.getY() - target.getY());
            });
        }
        h.runAfterDelay(500, () -> {
            boolean alive = watcher.isAlive();
            watcher.discard(); target.discard();
            h.assertTrue(alive && lowest[0] > 5, "El Vigía bajó a distancia de espada durante un ataque: altura " + lowest[0]);
            h.assertTrue(seen.containsAll(java.util.Set.of(1, 2, 4, 5)), "No se observaron ráfaga, bombardeo, barrido y EMP: " + seen);
            h.assertTrue(hit[0], "El Vigía voló y animó ataques, pero no logró dañar al objetivo real");
            h.succeed();
        });
    }

    /** Invocación real, alianzas, guardado, límite de seis y limpieza al morir la Matriarca. */
    @GameTest(template = "empty", timeoutTicks = 520, batch = "matriarca_refuerzos")
    public static void matriarchSummonsAlliedBrood(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos p = air(h);
        for (int x = -16; x <= 16; x++) for (int z = -16; z <= 16; z++)
            level.setBlock(p.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
        var mother = ModEntities.SWARM_MATRIARCH.get().create(level);
        mother.moveTo(p, 0, 0); mother.setHealth(250); level.addFreshEntity(mother);
        var target = EntityType.PIG.create(level);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20000);
        target.setHealth(20000); target.setNoAi(true); target.setNoGravity(true);
        target.moveTo(p.offset(4, 0, 0), 0, 0); level.addFreshEntity(target);
        mother.setTarget(target);
        h.runAfterDelay(110, () -> {
            var brood = level.getEntitiesOfClass(com.bryanamaya.tecnocinetico.entity.boss.MatriarchMinionEntity.class,
                    new AABB(p).inflate(30), m -> mother.ownsMinion(m.getUUID()));
            h.assertTrue(!brood.isEmpty() && brood.size() <= 6, "La Matriarca no invocó crías o excedió el límite");
            var child = brood.get(0);
            h.assertTrue(child.isAlliedTo(mother) && mother.isAlliedTo(child), "Las crías no reconocen a su madre como aliada");
            h.assertFalse(child.hurt(level.damageSources().mobAttack(mother), 100), "La madre puede matar a sus crías");
            h.assertFalse(mother.hurt(level.damageSources().mobAttack(child), 100), "Una cría puede herir a su madre");
            child.setTarget(mother);
            h.assertTrue(child.getTarget() != mother, "La cría puede elegir como objetivo a la Matriarca");
            h.assertTrue(child.getLootTable().equals(net.minecraft.world.level.storage.loot.BuiltInLootTables.EMPTY), "Las crías permiten cultivar botín");
            var saved = new net.minecraft.nbt.CompoundTag(); mother.addAdditionalSaveData(saved); mother.readAdditionalSaveData(saved);
            h.assertTrue(mother.ownsMinion(child.getUUID()), "El guardado perdió la propiedad de las crías");
            mother.setNoAi(true); mother.getNavigation().stop(); mother.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        });
        h.runAfterDelay(230, () -> {
            h.assertTrue(target.getLastHurtByMob() instanceof com.bryanamaya.tecnocinetico.entity.boss.MatriarchMinionEntity,
                    "Las crías aparecen, pero no atacan al enemigo real");
            mother.setNoAi(false);
        });
        h.runAfterDelay(470, () -> {
            var brood = level.getEntitiesOfClass(com.bryanamaya.tecnocinetico.entity.boss.MatriarchMinionEntity.class,
                    new AABB(p).inflate(40), m -> mother.ownsMinion(m.getUUID()));
            boolean capped = !brood.isEmpty() && brood.size() <= 6;
            mother.kill();
            boolean cleaned = brood.stream().noneMatch(net.minecraft.world.entity.Entity::isAlive);
            mother.discard(); target.discard();
            for (int x = -16; x <= 16; x++) for (int z = -16; z <= 16; z++) level.removeBlock(p.offset(x, -1, z), false);
            h.assertTrue(capped, "Las oleadas no respetan el límite de seis crías");
            h.assertTrue(cleaned, "Las crías permanecen tras la muerte de la Matriarca");
            h.succeed();
        });
    }

    /** Debilidad real y persistente del Vigía; ni los impactos frontales ni la Matriarca tienen ese efecto. */
    @GameTest(template = "empty", batch = "guardianes")
    public static void watcherCapacitorsAndMatriarchArmor(GameTestHelper h) {
        var level = h.getLevel();
        var watcher = ModEntities.NETWORK_WATCHER.get().create(level);
        var matriarch = ModEntities.SWARM_MATRIARCH.get().create(level);
        var spider = ModEntities.SIEGE_SPIDER.get().create(level);
        watcher.moveTo(air(h), 0, 0); watcher.yBodyRot = 0;
        matriarch.moveTo(air(h), 0, 0);
        var player = h.makeMockPlayer();
        try {
            h.assertTrue(watcher.getMaxHealth() > spider.getMaxHealth() && matriarch.getMaxHealth() > watcher.getMaxHealth(),
                    "La vida no respeta Araña < Vigía < Matriarca");
            // Un jefe guardado antes del cambio debe ganar los nuevos atributos sin curarse por completo.
            for (var boss : java.util.List.of(watcher, matriarch)) {
                boss.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(boss.isWatcher() ? 360 : 520);
                boss.setHealth(boss.getMaxHealth() / 2);
                var old = new net.minecraft.nbt.CompoundTag(); boss.addAdditionalSaveData(old); old.remove("GuardianBalance");
                boss.readAdditionalSaveData(old);
                h.assertTrue(boss.getMaxHealth() == (boss.isWatcher() ? 700 : 1000)
                                && Math.abs(boss.getHealth() / boss.getMaxHealth() - .5F) < .001F,
                        "Un jefe antiguo no adoptó el equilibrio nuevo conservando su fracción de vida");
                boss.setHealth(boss.getMaxHealth());
            }
            float before = watcher.getHealth();
            watcher.hurt(level.damageSources().generic(), 16);
            float normal = before - watcher.getHealth();
            var point = watcher.capacitorPosition(0);
            var arrow = new net.minecraft.world.entity.projectile.Arrow(level, point.x, point.y, point.z);
            watcher.invulnerableTime = 0;
            watcher.hurt(level.damageSources().arrow(arrow, player), 12);
            h.assertFalse(watcher.capacitorBroken(0) || watcher.stunned(), "Un condensador se rompió antes de acumular daño suficiente");
            watcher.invulnerableTime = 0;
            watcher.hurt(level.damageSources().arrow(arrow, player), 12);
            h.assertTrue(watcher.capacitorBroken(0) && watcher.stunned() && watcher.mode() == 3,
                    "Romper el condensador no interrumpió y aturdió al Vigía");
            before = watcher.getHealth(); watcher.invulnerableTime = 0;
            watcher.hurt(level.damageSources().generic(), 16);
            h.assertTrue(before - watcher.getHealth() > normal, "El aturdimiento no permite aprovechar la debilidad");
            var saved = new net.minecraft.nbt.CompoundTag();
            watcher.addAdditionalSaveData(saved);
            watcher.readAdditionalSaveData(saved);
            h.assertTrue(watcher.capacitorBroken(0) && watcher.stunned(), "Guardar y cargar reparó el condensador o eliminó el aturdimiento");
            saved.putInt("Stun", 0); watcher.readAdditionalSaveData(saved);
            watcher.invulnerableTime = 0;
            watcher.hurt(level.damageSources().arrow(arrow, player), 24);
            h.assertFalse(watcher.stunned(), "Se puede aturdir repetidamente disparando al mismo condensador roto");

            point = watcher.capacitorPosition(1);
            var rear = watcher.capacitorPosition(0).add(point).scale(.5).subtract(watcher.position()).multiply(1,0,1).normalize();
            var eye = point.subtract(rear.scale(6));
            player.setPos(eye.x, eye.y - player.getEyeHeight(), eye.z);
            player.setXRot(0); player.setYRot((float) (Math.toDegrees(Math.atan2(rear.z, rear.x)) - 90));
            watcher.invulnerableTime = 0;
            watcher.hurt(level.damageSources().playerAttack(player), 24);
            h.assertFalse(watcher.capacitorBroken(1), "Una línea frontal rompió el condensador a través del blindaje");
            arrow.setPos(point.x, point.y, point.z); watcher.invulnerableTime = 0;
            watcher.hurt(level.damageSources().arrow(arrow, player), 24);
            h.assertTrue(watcher.capacitorBroken(1) && watcher.stunned(), "El segundo condensador no se puede romper por detrás");

            before = matriarch.getHealth(); matriarch.hurt(level.damageSources().generic(), 16);
            normal = before - matriarch.getHealth();
            matriarch.addAdditionalSaveData(saved);
            saved.putInt("Stun", 60); saved.putByte("BrokenCapacitors", (byte) 3);
            matriarch.readAdditionalSaveData(saved);
            before = matriarch.getHealth(); matriarch.invulnerableTime = 0;
            matriarch.hurt(level.damageSources().generic(), 16);
            h.assertTrue(!matriarch.stunned() && !matriarch.capacitorBroken(0)
                            && Math.abs((before - matriarch.getHealth()) - normal) < .001F,
                    "La Matriarca conserva debilidades o una bonificación de daño por exposición");
            h.succeed();
        } finally { watcher.discard(); matriarch.discard(); spider.discard(); }
    }

    /** Evaluar las tablas reales: una pieza exclusiva garantizada por jefe, independientemente de la suerte. */
    @GameTest(template = "empty", batch = "guardianes")
    public static void fighterComponentsDrop(GameTestHelper h) {
        var level = h.getLevel();
        var bosses = java.util.List.of(ModEntities.SIEGE_SPIDER.get().create(level),
                ModEntities.NETWORK_WATCHER.get().create(level), ModEntities.SWARM_MATRIARCH.get().create(level));
        var parts = java.util.List.of(com.bryanamaya.tecnocinetico.item.ModItems.DIMENSIONAL_DRIVE.get(),
                com.bryanamaya.tecnocinetico.item.ModItems.VECTOR_THRUSTER.get(),
                com.bryanamaya.tecnocinetico.item.ModItems.ADAPTIVE_FLIGHT_CORE.get());
        try {
            for (int i = 0; i < bosses.size(); i++) {
                var boss = bosses.get(i);
                var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, boss)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, boss.position())
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE, level.damageSources().generic())
                        .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
                var table = level.getServer().getLootData().getLootTable(boss.getLootTable());
                for (int roll = 0; roll < 5; roll++) {
                    var drops = table.getRandomItems(params);
                    for (int j = 0; j < parts.size(); j++) {
                        var part = parts.get(j);
                        int count = drops.stream().filter(s -> s.is(part)).mapToInt(net.minecraft.world.item.ItemStack::getCount).sum();
                        h.assertTrue(count == (i == j ? 1 : 0), "Drop exclusivo incorrecto: " + boss.getName().getString() + " / " + part.getDescriptionId());
                    }
                }
            }
            h.succeed();
        } finally { bosses.forEach(net.minecraft.world.entity.Entity::discard); }
    }
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
            boolean noExposure = watcher || (boss.mode() != 3 && !boss.stunned());
            boss.discard(); pig.discard();
            if (!watcher) for (int x = -2; x <= 12; x++) for (int z = -5; z <= 5; z++) level.removeBlock(p.offset(x, -1, z), false);
            h.assertTrue(hit && alive, "El guardián no logró dañar al enemigo con su ataque real: " + (watcher ? "Vigía" : "Matriarca"));
            h.assertTrue(noExposure, "La Matriarca quedó expuesta después de atacar");
            h.succeed();
        });
    }
}
