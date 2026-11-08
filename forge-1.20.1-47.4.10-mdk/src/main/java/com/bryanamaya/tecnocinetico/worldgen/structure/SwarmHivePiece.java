package com.bryanamaya.tecnocinetico.worldgen.structure;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.ModWorldBlocks;
import com.bryanamaya.tecnocinetico.entity.ModEntities;
import com.bryanamaya.tecnocinetico.entity.swarm.HiveNodeEntity;
import com.bryanamaya.tecnocinetico.entity.swarm.SwarmWaspEntity;
import com.bryanamaya.tecnocinetico.entity.swarm.WaspRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Pieza de la Colmena del Enjambre. Elipsoide de radios rx/ry/rz centrado algo por encima del suelo: cáscara
 * de Panal (con algo de chapa oxidada), interior hueco, suelo de panal, 3 túneles de entrada y un óculo
 * arriba; cristales de cinetrita por dentro, un cofre y el Nodo con 8 avispas. Coordenadas absolutas y
 * aleatorios por posición (deterministas en todos los chunks).
 */
public class SwarmHivePiece extends StructurePiece {

    private static final ResourceLocation LOOT = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "chests/swarm_hive");

    private final int gx, gy, gz, rx, ry, rz;
    private final long seed;

    public SwarmHivePiece(BlockPos ground, int rx, int ry, int rz, long seed) {
        super(ModStructures.SWARM_HIVE_PIECE.get(), 0,
                new BoundingBox(ground.getX() - rx - 1, ground.getY() - 3, ground.getZ() - rz - 1,
                        ground.getX() + rx + 1, ground.getY() + ry * 2, ground.getZ() + rz + 1));
        this.gx = ground.getX();
        this.gy = ground.getY();
        this.gz = ground.getZ();
        this.rx = rx;
        this.ry = ry;
        this.rz = rz;
        this.seed = seed;
        setOrientation(null);
    }

    public SwarmHivePiece(CompoundTag tag) {
        super(ModStructures.SWARM_HIVE_PIECE.get(), tag);
        this.gx = tag.getInt("GX");
        this.gy = tag.getInt("GY");
        this.gz = tag.getInt("GZ");
        this.rx = tag.getInt("RX");
        this.ry = tag.getInt("RY");
        this.rz = tag.getInt("RZ");
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("GX", gx);
        tag.putInt("GY", gy);
        tag.putInt("GZ", gz);
        tag.putInt("RX", rx);
        tag.putInt("RY", ry);
        tag.putInt("RZ", rz);
        tag.putLong("Seed", seed);
    }

    private RandomSource at(int x, int y, int z, int salt) {
        return RandomSource.create(seed * 31L + salt + (x * 73856093L ^ y * 19349663L ^ z * 83492791L));
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager sm, ChunkGenerator gen, RandomSource ignored,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        RandomSource r = RandomSource.create(seed);
        double cy = gy + ry * 0.45D;                                   // centro: un poco enterrado
        float[] tunnels = {r.nextFloat() * Mth.TWO_PI, 0, 0};
        tunnels[1] = tunnels[0] + 2.1F + r.nextFloat() * 0.4F;
        tunnels[2] = tunnels[1] + 2.0F + r.nextFloat() * 0.4F;

        for (int x = -rx - 1; x <= rx + 1; x++) {
            for (int z = -rz - 1; z <= rz + 1; z++) {
                for (int y = gy - 3; y <= gy + ry * 2; y++) {
                    double nx = x / (double) rx, ny = (y - cy) / ry, nz = z / (double) rz;
                    double d = Math.sqrt(nx * nx + ny * ny + nz * nz) + (at(x, y, z, 1).nextFloat() - 0.5F) * 0.08F;
                    double shell = 1.6D / Math.min(rx, Math.min(ry, rz));      // ~1.5 bloques de grosor
                    if (d > 1.0D) {
                        continue;
                    }
                    BlockPos p = new BlockPos(gx + x, y, gz + z);
                    BlockState s;
                    if (d > 1.0D - shell) {
                        float v = at(x, y, z, 2).nextFloat();
                        s = v < 0.86F ? ModWorldBlocks.SWARM_COMB.get().defaultBlockState()
                                : ModWorldBlocks.RUSTED_METAL_SHEET.get().defaultBlockState();   // chatarra incrustada
                    } else if (y <= gy) {
                        s = y == gy ? ModWorldBlocks.SWARM_COMB.get().defaultBlockState() : null;   // suelo de panal
                    } else {
                        s = Blocks.AIR.defaultBlockState();
                    }
                    // túneles de entrada (a 2 bloques del suelo) y óculo en lo alto
                    if (s != null && y > gy && y <= gy + 3 && inTunnel(x, z, tunnels)) {
                        s = Blocks.AIR.defaultBlockState();
                    }
                    if (s != null && y > cy + ry * 0.6D && x * x + z * z <= 2) {
                        s = Blocks.AIR.defaultBlockState();
                    }
                    if (s != null) {
                        placeBlock(level, s, p.getX(), p.getY(), p.getZ(), box);
                    }
                }
            }
        }
        crystals(level, box);
        // cofre junto a la pared
        BlockPos chest = new BlockPos(gx + rx - 3, gy + 1, gz);
        if (box.isInside(chest)) {
            createChest(level, box, at(0, 0, 0, 9), chest.getX(), chest.getY(), chest.getZ(),
                    LOOT);
        }
        spawnHive(level, box);
    }

    private boolean inTunnel(int x, int z, float[] angles) {
        for (float a : angles) {
            double dx = Math.cos(a), dz = Math.sin(a);
            double along = x * dx + z * dz;
            double across = Math.abs(-x * dz + z * dx);
            if (along > 1 && across < 1.3D) {
                return true;
            }
        }
        return false;
    }

    /** Racimos de cinetrita en el suelo y colgando del techo (iluminan el interior con luz cian). */
    private void crystals(WorldGenLevel level, BoundingBox box) {
        for (int i = 0; i < 18; i++) {
            RandomSource cr = at(i, 0, 0, 3);
            int x = gx + cr.nextInt(rx * 2 - 3) - rx + 2, z = gz + cr.nextInt(rz * 2 - 3) - rz + 2;
            boolean ceiling = cr.nextBoolean();
            if (!ceiling) {
                BlockPos p = new BlockPos(x, gy + 1, z);
                if (box.isInside(p) && level.getBlockState(p).isAir() && level.getBlockState(p.below()).isSolid()) {
                    level.setBlock(p, ModWorldBlocks.CINETRITE_CLUSTER.get().defaultBlockState()
                            .setValue(AmethystClusterBlock.FACING, Direction.UP), 2);
                }
            } else {
                for (int y = gy + ry * 2; y > gy + 2; y--) {          // buscar el techo desde dentro
                    BlockPos p = new BlockPos(x, y, z);
                    if (!box.isInside(p)) {
                        break;
                    }
                    if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).is(ModWorldBlocks.SWARM_COMB.get())) {
                        level.setBlock(p, ModWorldBlocks.CINETRITE_CLUSTER.get().defaultBlockState()
                                .setValue(AmethystClusterBlock.FACING, Direction.DOWN), 2);
                        break;
                    }
                }
            }
        }
    }

    /** El Nodo en el centro y su enjambre inicial (solo en el chunk que contiene el centro). */
    private void spawnHive(WorldGenLevel level, BoundingBox box) {
        BlockPos center = new BlockPos(gx, gy + 2, gz);
        if (!box.isInside(center)) {
            return;
        }
        HiveNodeEntity node = ModEntities.HIVE_NODE.get().create(level.getLevel());
        if (node == null) {
            return;
        }
        node.moveTo(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D, 0.0F, 0.0F);
        node.finalizeSpawn(level, level.getCurrentDifficultyAt(center), MobSpawnType.STRUCTURE, null, null);
        level.addFreshEntityWithPassengers(node);
        WaspRole[] roles = {WaspRole.SCOUT, WaspRole.SCOUT, WaspRole.GUARD, WaspRole.GUARD,
                WaspRole.STINGER, WaspRole.STINGER, WaspRole.STINGER, WaspRole.STINGER};
        for (int i = 0; i < roles.length; i++) {
            SwarmWaspEntity w = ModEntities.SWARM_WASP.get().create(level.getLevel());
            if (w == null) {
                continue;
            }
            double a = i * Mth.TWO_PI / roles.length;
            w.moveTo(center.getX() + 0.5D + Math.cos(a) * 1.2D, center.getY() + 1.5D, center.getZ() + 0.5D + Math.sin(a) * 1.2D,
                    (float) (a * Mth.RAD_TO_DEG), 0.0F);
            w.finalizeSpawn(level, level.getCurrentDifficultyAt(center), MobSpawnType.STRUCTURE, null, null);
            w.joinHive(node, roles[i]);
            level.addFreshEntityWithPassengers(w);
        }
    }
}
