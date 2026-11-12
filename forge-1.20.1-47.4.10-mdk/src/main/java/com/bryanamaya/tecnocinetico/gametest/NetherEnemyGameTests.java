package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.nether.InfernalEntity;
import com.bryanamaya.tecnocinetico.nether.NetherEntities;
import com.bryanamaya.tecnocinetico.nether.NetherMobEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Segunda parte del Nether: Ácaro de Escoria, Centinela de Ceniza y el fusil de los piglins. */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class NetherEnemyGameTests {

    /** El centinela fija un punto, avisa y dispara: un objetivo quieto recibe el tiro; los aliados no se dañan entre sí. */
    @GameTest(template = "empty", batch = "nether_enemigos", timeoutTicks = 200)
    public static void ashSentinelShootsAfterWarning(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos col = helper.absolutePos(BlockPos.ZERO);
        level.getChunk(col);
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, col.getX(), col.getZ());
        InfernalEntity sentinel = NetherEntities.SENTINEL.get().create(level);
        sentinel.moveTo(col.getX() + 0.5D, ground, col.getZ() + 0.5D, 0, 0);
        level.addFreshEntity(sentinel);
        Pig pig = EntityType.PIG.create(level);
        pig.moveTo(col.getX() + 8.5D, ground, col.getZ() + 0.5D, 0, 0);
        pig.setNoAi(true);
        level.addFreshEntity(pig);
        InfernalEntity mite = NetherEntities.MITE.get().create(level);
        helper.assertTrue(sentinel.isAlliedTo(mite) && !mite.hurt(level.damageSources().mobAttack(sentinel), 5.0F),
                "Los enemigos del Nether se dañan entre sí");
        sentinel.setTarget(pig);
        var warned = new java.util.concurrent.atomic.AtomicBoolean();
        helper.onEachTick(() -> {
            if (sentinel.attack() == 1) {
                warned.set(true);                                             // aviso (láser) antes del disparo
                helper.assertTrue(pig.getHealth() == pig.getMaxHealth() || pig.getLastHurtByMob() == sentinel, "daño ajeno al centinela");
            }
        });
        helper.runAfterDelay(150, () -> {
            boolean hit = pig.getHealth() < pig.getMaxHealth() && pig.getLastHurtByMob() == sentinel;
            sentinel.discard();
            pig.discard();
            helper.assertTrue(warned.get(), "El centinela no avisó antes de disparar");
            helper.assertTrue(hit, "El centinela no alcanzó a un objetivo quieto");
            helper.succeed();
        });
    }

    /** Excavar dentro de la roca saca un ácaro (hasta el límite); en superficie abierta, no. Y los piglins no sueltan el fusil. */
    @GameTest(template = "empty", batch = "nether_acaros")
    public static void mitesFromRockAndRifleNeverDrops(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "Falta el Nether");
        // La sala de roca se monta en la zona de la prueba (chunks activos), en alto para no estorbar a otras pruebas:
        // disturbMites no depende de la dimensión (eso lo filtra el evento de excavar).
        ServerLevel level = helper.getLevel();
        BlockPos at = helper.absolutePos(BlockPos.ZERO).atY(200);
        var shell = BlockPos.betweenClosed(at.offset(-6, -2, -6), at.offset(6, 5, 6));
        for (BlockPos p : shell) {                                                                   // sala de 9x9 dentro de roca
            boolean room = Math.abs(p.getX() - at.getX()) <= 4 && Math.abs(p.getZ() - at.getZ()) <= 4 && p.getY() >= 200 && p.getY() <= 202;
            level.setBlock(p, (room ? Blocks.AIR : Blocks.NETHERRACK).defaultBlockState(), 3);
        }
        var player = helper.makeMockPlayer();
        player.setPos(at.getX() + 0.5D, 200, at.getZ() + 0.5D);
        var box = new net.minecraft.world.phys.AABB(at).inflate(8);
        try {
            helper.assertTrue(NetherMobEvents.isMiteNest(Blocks.NETHERRACK.defaultBlockState())
                    && !NetherMobEvents.isMiteNest(Blocks.NETHER_BRICKS.defaultBlockState()), "Nidos de ácaro mal definidos");
            helper.assertFalse(NetherMobEvents.disturbMites(level, player, at.above()), "Salió un ácaro al romper un bloque al aire libre");
            BlockPos inWall = at.offset(5, 1, 0);                                                    // dentro de la pared de roca
            int spawned = 0;
            for (int i = 0; i < 12; i++) {
                if (NetherMobEvents.disturbMites(level, player, inWall)) {
                    spawned++;
                }
            }
            var mites = level.getEntitiesOfClass(InfernalEntity.class, box, InfernalEntity::mite);
            helper.assertTrue(spawned == 6 && mites.size() == 6, "Ácaros al excavar: " + spawned + " (se esperaban 6, el límite)");
            helper.assertTrue(mites.stream().allMatch(m -> level.noCollision(m) && m.distanceToSqr(player) >= 2),
                    "Un ácaro apareció dentro de un bloque o encima del jugador");
        } finally {
            level.getEntitiesOfClass(InfernalEntity.class, box).forEach(net.minecraft.world.entity.Entity::discard);
            for (BlockPos p : shell) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        // el Fusil de Asalto (tier 3) nunca se suelta; el hacha sí puede
        boolean rifleSeen = false, axeSeen = false;
        for (int i = 0; i < 40 && !(rifleSeen && axeSeen); i++) {
            var piglin = EntityType.PIGLIN.create(nether);
            NetherMobEvents.join(new EntityJoinLevelEvent(piglin, nether));
            float chance = piglin.saveWithoutId(new net.minecraft.nbt.CompoundTag()).getList("HandDropChances", 5).getFloat(0);
            if (piglin.getMainHandItem().is(ModItems.ASSAULT_RIFLE.get())) {
                rifleSeen = true;
                helper.assertTrue(chance == 0.0F, "Un piglin puede soltar el Fusil de Asalto");
            } else {
                axeSeen = true;
                helper.assertTrue(chance > 0.0F, "El hacha de netherita perdió su probabilidad de caer");
            }
        }
        helper.assertTrue(rifleSeen && axeSeen, "Los piglins no reciben las dos armas");
        helper.succeed();
    }

    /**
     * Nether industrial (parte 3): cinco biomas que siguen siendo Nether (fortalezas y bastiones), los tipos de mundo
     * Postapocalíptico usan su fuente, y el depósito y el puesto de vigilancia se generan en una caverna con cofres
     * (el puesto, con sus centinelas).
     */
    @GameTest(template = "empty", batch = "nether_biomas", timeoutTicks = 400)
    public static void netherBiomesAndSites(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var biomes = access.registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
        for (var key : com.bryanamaya.tecnocinetico.worldgen.InfernalBiomes.ALL) {
            var holder = biomes.getHolder(key);
            helper.assertTrue(holder.isPresent() && holder.get().is(net.minecraft.tags.BiomeTags.IS_NETHER)
                    && holder.get().is(net.minecraft.tags.BiomeTags.HAS_NETHER_FORTRESS)
                    && holder.get().is(net.minecraft.tags.BiomeTags.HAS_BASTION_REMNANT), "Bioma del Nether mal etiquetado: " + key.location());
        }
        var preset = access.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getOrThrow(
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.WORLD_PRESET,
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "post_apocalyptic")));
        var nether = preset.createWorldDimensions().dimensions().get(net.minecraft.world.level.dimension.LevelStem.NETHER);
        helper.assertTrue(nether != null && nether.generator().getBiomeSource() instanceof com.bryanamaya.tecnocinetico.worldgen.InfernalBiomeSource,
                "El tipo de mundo Postapocalíptico no usa el Nether industrial");

        ServerLevel level = helper.getLevel().getServer().getLevel(Level.NETHER);
        var gen = level.getChunkSource().getGenerator();
        var structures = access.registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        int i = 0;
        for (String id : new String[]{"cooling_depot", "infernal_watchpost"}) {
            var structure = structures.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, id));
            helper.assertTrue(structure != null, "Falta la estructura " + id);
            var biome = biomes.getHolderOrThrow(com.bryanamaya.tecnocinetico.worldgen.InfernalBiomes.SLAG_FORGES);
            var start = net.minecraft.world.level.levelgen.structure.StructureStart.INVALID_START;
            for (int tries = 0; tries < 200 && !start.isValid(); tries++) {
                var cp = new net.minecraft.world.level.ChunkPos(new BlockPos(4000 + i * 2048 + (tries % 10) * 64, 0, 4000 + (tries / 10) * 64));
                start = structure.generate(access, gen, new net.minecraft.world.level.biome.FixedBiomeSource(biome), level.getChunkSource().randomState(),
                        level.getStructureManager(), level.getSeed(), cp, 0, level, b -> true);
            }
            helper.assertTrue(start.isValid(), "No se encontró caverna para " + id);
            var placed = start;
            var bb = start.getBoundingBox();
            // chunks forzados: así las entidades que pone la estructura (centinelas) quedan activas y se pueden buscar
            var forced = new java.util.ArrayList<net.minecraft.world.level.ChunkPos>();
            net.minecraft.world.level.ChunkPos.rangeClosed(new net.minecraft.world.level.ChunkPos(bb.minX() >> 4, bb.minZ() >> 4),
                    new net.minecraft.world.level.ChunkPos(bb.maxX() >> 4, bb.maxZ() >> 4)).forEach(c -> {
                level.setChunkForced(c.x, c.z, true);
                forced.add(c);
            });
            // el mundo de prueba se guarda entre ejecuciones: quitar lo que dejaron las anteriores
            level.getEntitiesOfClass(InfernalEntity.class, net.minecraft.world.phys.AABB.of(bb).inflate(2)).forEach(net.minecraft.world.entity.Entity::discard);
            net.minecraft.world.level.ChunkPos.rangeClosed(new net.minecraft.world.level.ChunkPos(bb.minX() >> 4, bb.minZ() >> 4),
                    new net.minecraft.world.level.ChunkPos(bb.maxX() >> 4, bb.maxZ() >> 4)).forEach(c -> placed.placeInChunk(level,
                    level.structureManager(), gen, level.getRandom(), new net.minecraft.world.level.levelgen.structure.BoundingBox(c.getMinBlockX(),
                    level.getMinBuildHeight(), c.getMinBlockZ(), c.getMaxBlockX(), level.getMaxBuildHeight(), c.getMaxBlockZ()), c));
            int chests = 0, casing = 0;
            for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
                var st = level.getBlockState(p);
                if (st.is(Blocks.CHEST)) chests++;
                if (st.is(com.bryanamaya.tecnocinetico.block.ModWorldBlocks.INFERNAL_CASING.get())) casing++;
            }
            helper.assertTrue(chests == 2 && casing > 10, id + ": " + chests + " cofres y " + casing + " bloques de carcasa");
            if (id.equals("infernal_watchpost")) {
                // las entidades de un chunk recién forzado se ven al tick siguiente: comprobar los centinelas después
                var guardBox = net.minecraft.world.phys.AABB.of(bb).inflate(2);
                helper.runAfterDelay(10, () -> {
                    var guards = level.getEntitiesOfClass(InfernalEntity.class, guardBox, InfernalEntity::sentinel);
                    guards.forEach(net.minecraft.world.entity.Entity::discard);
                    forced.forEach(c -> level.setChunkForced(c.x, c.z, false));
                    helper.assertTrue(guards.size() == 2 && guards.stream().allMatch(net.minecraft.world.entity.Mob::isPersistenceRequired),
                            "El puesto de vigilancia tiene " + guards.size() + " centinelas");
                    helper.succeed();
                });
            } else {
                forced.forEach(c -> level.setChunkForced(c.x, c.z, false));
            }
            Tecnocinetico.LOGGER.info("[GameTest] {} generado en el Nether en {}", id, bb);
            i++;
        }
    }
}
