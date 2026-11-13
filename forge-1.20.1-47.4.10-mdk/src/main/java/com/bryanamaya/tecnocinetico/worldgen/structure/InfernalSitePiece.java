package com.bryanamaya.tecnocinetico.worldgen.structure;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.ModWorldBlocks;
import com.bryanamaya.tecnocinetico.nether.InfernalEntity;
import com.bryanamaya.tecnocinetico.nether.NetherEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Instalaciones del Nether industrial (ver {@link InfernalSiteStructure}). Coordenadas locales desde la esquina
 * (x, z) y el suelo (y = 0 es la losa). Todo lo aleatorio sale de la semilla de la pieza y de la posición
 * ({@link #roll}), así el resultado no depende del orden en que se generan los chunks.
 * <ul>
 *   <li><b>Depósito de Refrigeración</b> (23x23): losa de escoria con pretil, tres tanques de carcasa infernal con
 *       ventanas al refrigerante (hielo azul), tuberías entre ellos y una caseta de control con dos cofres.</li>
 *   <li><b>Fundición del Abismo</b> (31x31): nave de carcasa infernal de 9 de alto con la puerta al sur, dos canales de
 *       lava, cuatro pilares con foco, el horno apagado donde duerme el Fundidor y la consola junto a la puerta (al
 *       activarla se cierra la puerta con barrera y despierta el jefe). Tres cofres.</li>
 *   <li><b>Puesto de Vigilancia</b> (11x11): torre de 18 con plataforma, barandilla, foco, escalera interior,
 *       un cofre abajo y otro arriba y dos Centinelas de Ceniza de guardia.</li>
 * </ul>
 */
public class InfernalSitePiece extends StructurePiece {

    private static final ResourceLocation LOOT = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "chests/infernal_supplies");
    private final int ox, oy, oz, site;
    private final long seed;

    /** Lado de cada instalación. */
    public static int size(int site) {
        return site == InfernalSiteStructure.WATCHPOST ? 11 : site == InfernalSiteStructure.FOUNDRY ? 31 : 23;
    }

    private static int height(int site) {
        return site == InfernalSiteStructure.WATCHPOST ? 20 : site == InfernalSiteStructure.FOUNDRY ? 18 : 12;
    }

    public InfernalSitePiece(BlockPos origin, int site, long seed) {
        super(ModStructures.INFERNAL_PIECE.get(), 0, new BoundingBox(origin.getX(), origin.getY() - 6, origin.getZ(),
                origin.getX() + size(site) - 1, origin.getY() + height(site), origin.getZ() + size(site) - 1));
        this.ox = origin.getX();
        this.oy = origin.getY();
        this.oz = origin.getZ();
        this.site = site;
        this.seed = seed;
        setOrientation(null);
    }

    public InfernalSitePiece(CompoundTag tag) {
        super(ModStructures.INFERNAL_PIECE.get(), tag);
        this.ox = tag.getInt("X");
        this.oy = tag.getInt("Y");
        this.oz = tag.getInt("Z");
        this.site = tag.getInt("Site");
        this.seed = tag.getLong("Seed");
        setOrientation(null);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("X", ox);
        tag.putInt("Y", oy);
        tag.putInt("Z", oz);
        tag.putInt("Site", site);
        tag.putLong("Seed", seed);
    }

    // ------------------------------------------------------------------ utilidades

    private BlockPos pos(int x, int y, int z) {
        return new BlockPos(ox + x, oy + y, oz + z);
    }

    /** Número determinista en [0, 1) para una posición local y una sal. */
    private double roll(int x, int y, int z, int salt) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L) ^ (salt * 0x27D4EB2F165667C5L);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return ((h ^ (h >>> 31)) >>> 11) * 0x1.0p-53;
    }

    private void put(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        BlockPos p = pos(x, y, z);
        if (box.isInside(p)) {
            level.setBlock(p, state, 2);
        }
    }

    private void put(WorldGenLevel level, BoundingBox box, int x, int y, int z, Block block) {
        put(level, box, x, y, z, block.defaultBlockState());
    }

    private void fill(WorldGenLevel level, BoundingBox box, int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    put(level, box, x, y, z, block);
                }
            }
        }
    }

    /** Cimientos: rellena con piedra negra hacia abajo hasta tocar suelo (máx. 6) bajo la losa. */
    private void foundation(WorldGenLevel level, BoundingBox box, int x, int z) {
        for (int y = -1; y >= -6; y--) {
            BlockPos p = pos(x, y, z);
            if (!box.isInside(p) || level.getBlockState(p).isSolid()) {
                return;
            }
            level.setBlock(p, Blocks.BLACKSTONE.defaultBlockState(), 2);
        }
    }

    private void chest(WorldGenLevel level, BoundingBox box, int x, int y, int z, Direction facing) {
        BlockPos p = pos(x, y, z);
        if (!box.isInside(p)) {
            return;
        }
        level.setBlock(p, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
        RandomizableContainerBlockEntity.setLootTable(level, RandomSource.create(seed ^ p.asLong()), p, LOOT);
    }

    // ------------------------------------------------------------------ construcción

    @Override
    public void postProcess(WorldGenLevel level, StructureManager manager, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunk, BlockPos pivot) {
        if (site == InfernalSiteStructure.WATCHPOST) {
            watchpost(level, box);
        } else if (site == InfernalSiteStructure.FOUNDRY) {
            foundry(level, box);
        } else {
            depot(level, box);
        }
    }

    /** Fundición del Abismo (jefe). Puerta en z = n (sur), x 14-16; la consola, dentro junto a la puerta. */
    private void foundry(WorldGenLevel level, BoundingBox box) {
        int n = size(site) - 1;
        for (int x = 0; x <= n; x++) {
            for (int z = 0; z <= n; z++) {
                foundation(level, box, x, z);
                boolean edge = x == 0 || z == 0 || x == n || z == n;
                put(level, box, x, 0, z, roll(x, 0, z, 11) < 0.3 ? ModWorldBlocks.COMPACTED_SLAG.get() : ModWorldBlocks.CARBONIZED_PLATE.get());
                fill(level, box, x, 1, z, x, 17, z, Blocks.AIR);                      // excava la nave en la roca
                if (edge) {
                    for (int y = 1; y <= 9; y++) {
                        boolean band = y == 4 || y == 8;
                        put(level, box, x, y, z, band ? Blocks.POLISHED_BLACKSTONE_BRICKS : ModWorldBlocks.INFERNAL_CASING.get());
                    }
                    if (roll(x, 10, z, 12) < 0.6) {
                        put(level, box, x, 10, z, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);   // almenas rotas
                    }
                }
            }
        }
        // puerta (3x3) al sur
        fill(level, box, 14, 1, n, 16, 3, n, Blocks.AIR);
        // vigas del techo a la altura 9 (en cruz) con cadenas
        fill(level, box, 1, 9, 15, n - 1, 9, 15, ModWorldBlocks.RUSTED_METAL_BEAM.get());
        fill(level, box, 15, 9, 1, 15, 9, n - 1, ModWorldBlocks.RUSTED_METAL_BEAM.get());
        // dos canales de lava (x 5 y 25, de z 5 a 25), con bordillo
        for (int cx : new int[]{5, 25}) {
            for (int z = 5; z <= 25; z++) {
                put(level, box, cx, 0, z, Blocks.LAVA);
                put(level, box, cx, -1, z, Blocks.BLACKSTONE);
                put(level, box, cx - 1, 1, z, z % 4 == 0 ? Blocks.POLISHED_BLACKSTONE_BRICK_WALL : Blocks.AIR);
                put(level, box, cx + 1, 1, z, z % 4 == 0 ? Blocks.POLISHED_BLACKSTONE_BRICK_WALL : Blocks.AIR);
            }
        }
        // cuatro pilares de carcasa con foco arriba
        for (int[] c : new int[][]{{9, 9}, {21, 9}, {9, 21}, {21, 21}}) {
            fill(level, box, c[0], 1, c[1], c[0] + 1, 8, c[1] + 1, ModWorldBlocks.INFERNAL_CASING.get());
            put(level, box, c[0], 9, c[1], Blocks.SHROOMLIGHT);
            put(level, box, c[0] + 1, 9, c[1] + 1, Blocks.SHROOMLIGHT);
        }
        // el horno apagado donde duerme el Fundidor (al norte, z 2-6)
        fill(level, box, 11, 1, 1, 19, 1, 6, ModWorldBlocks.INFERNAL_CASING.get());
        fill(level, box, 12, 2, 1, 18, 7, 2, ModWorldBlocks.INFERNAL_CASING.get());
        fill(level, box, 14, 2, 2, 16, 5, 2, Blocks.MAGMA_BLOCK);                        // boca del horno
        put(level, box, 15, 8, 1, Blocks.POLISHED_BLACKSTONE_BRICKS);
        fill(level, box, 13, 8, 1, 17, 8, 1, ModWorldBlocks.CARBONIZED_PLATE.get());
        // cofres: dos en las esquinas del fondo, uno junto a la puerta
        chest(level, box, 2, 1, 2, Direction.SOUTH);
        chest(level, box, n - 2, 1, 2, Direction.SOUTH);
        chest(level, box, 19, 1, n - 2, Direction.NORTH);
        // consola junto a la puerta; el jefe aparece frente al horno
        BlockPos console = pos(12, 1, n - 2);
        if (box.isInside(console)) {
            level.setBlock(console, com.bryanamaya.tecnocinetico.block.ModBlocks.CORE_CONSOLE.get().defaultBlockState(), 2);
            if (level.getBlockEntity(console) instanceof com.bryanamaya.tecnocinetico.block.entity.CoreConsoleBlockEntity c) {
                c.configureGuardian("abyss_foundry", pos(15, 1, 10));
                var seal = new java.util.ArrayList<BlockPos>();
                for (int x = 14; x <= 16; x++) {
                    for (int y = 1; y <= 3; y++) {
                        seal.add(pos(x, y, n));
                    }
                }
                c.setSeal(seal);
            }
        }
    }

    private void depot(WorldGenLevel level, BoundingBox box) {
        int n = size(site) - 1;
        // losa de escoria con cimientos, aire encima y pretil roto de ladrillo de piedra negra
        for (int x = 0; x <= n; x++) {
            for (int z = 0; z <= n; z++) {
                foundation(level, box, x, z);
                put(level, box, x, 0, z, roll(x, 0, z, 1) < 0.15 ? ModWorldBlocks.SULFUR_CRUST.get() : ModWorldBlocks.COMPACTED_SLAG.get());
                fill(level, box, x, 1, z, x, 11, z, Blocks.AIR);
                boolean rim = x == 0 || z == 0 || x == n || z == n;
                if (rim && roll(x, 1, z, 2) < 0.75 && !(z == 0 && x >= 10 && x <= 12)) {
                    fill(level, box, x, 1, z, x, roll(x, 2, z, 3) < 0.5 ? 2 : 1, z, Blocks.POLISHED_BLACKSTONE_BRICKS);
                }
            }
        }
        // tres tanques octogonales (5x5, 7 de alto): carcasa infernal con una franja de ventanas al refrigerante
        int[][] tanks = {{4, 4}, {16, 4}, {10, 13}};
        for (int[] t : tanks) {
            for (int dx = 0; dx < 5; dx++) {
                for (int dz = 0; dz < 5; dz++) {
                    boolean corner = (dx == 0 || dx == 4) && (dz == 0 || dz == 4);
                    if (corner) {
                        continue;
                    }
                    boolean shell = dx == 0 || dz == 0 || dx == 4 || dz == 4;
                    for (int y = 1; y <= 7; y++) {
                        Block b = !shell ? Blocks.BLUE_ICE
                                : (y == 3 || y == 4) && (dx == 2 || dz == 2) ? Blocks.LIGHT_BLUE_STAINED_GLASS : ModWorldBlocks.INFERNAL_CASING.get();
                        put(level, box, t[0] + dx, y, t[1] + dz, b);
                    }
                    put(level, box, t[0] + dx, 8, t[1] + dz, Blocks.POLISHED_BLACKSTONE_SLAB);
                }
            }
            put(level, box, t[0] + 2, 9, t[1] + 2, Blocks.SHROOMLIGHT);                       // baliza encima
        }
        // tuberías entre tanques (a la altura 6) y válvulas con fugas de magma debajo
        fill(level, box, 9, 6, 6, 15, 6, 6, Blocks.IRON_BARS);
        fill(level, box, 12, 6, 7, 12, 6, 12, Blocks.IRON_BARS);
        put(level, box, 12, 1, 9, Blocks.MAGMA_BLOCK);
        put(level, box, 6, 1, 10, Blocks.MAGMA_BLOCK);
        // caseta de control (5x4) al sur, con techo, puerta, luz y dos cofres
        int cx = 16, cz = 15;
        fill(level, box, cx, 1, cz, cx + 4, 4, cz + 4, ModWorldBlocks.CARBONIZED_PLATE.get());
        fill(level, box, cx + 1, 1, cz + 1, cx + 3, 3, cz + 3, Blocks.AIR);
        fill(level, box, cx, 2, cz + 2, cx, 2, cz + 2, Blocks.IRON_BARS);                     // ventana
        fill(level, box, cx + 2, 1, cz, cx + 2, 2, cz, Blocks.AIR);                           // puerta (norte)
        put(level, box, cx + 2, 3, cz + 2, Blocks.SHROOMLIGHT);
        chest(level, box, cx + 1, 1, cz + 3, Direction.NORTH);
        chest(level, box, cx + 3, 1, cz + 3, Direction.NORTH);
        // escombros de escoria y cadenas colgando
        for (int i = 0; i < 18; i++) {
            int x = 1 + (int) (roll(i, 4, 0, 5) * (n - 1)), z = 1 + (int) (roll(i, 4, 1, 5) * (n - 1));
            BlockPos p = pos(x, 1, z);
            if (box.isInside(p) && level.isEmptyBlock(p)) {
                level.setBlock(p, ModWorldBlocks.COMPACTED_SLAG.get().defaultBlockState(), 2);
            }
        }
    }

    private void watchpost(WorldGenLevel level, BoundingBox box) {
        int n = size(site) - 1;
        // losa de escoria
        for (int x = 0; x <= n; x++) {
            for (int z = 0; z <= n; z++) {
                foundation(level, box, x, z);
                put(level, box, x, 0, z, ModWorldBlocks.COMPACTED_SLAG.get());
                fill(level, box, x, 1, z, x, 19, z, Blocks.AIR);
            }
        }
        // torre 7x7, de 1 a 16, con esquinas de carcasa infernal y huecos de vigilancia
        int a = 2, b = 8;
        for (int y = 1; y <= 16; y++) {
            for (int x = a; x <= b; x++) {
                for (int z = a; z <= b; z++) {
                    boolean wall = x == a || x == b || z == a || z == b;
                    if (!wall) {
                        continue;
                    }
                    boolean corner = (x == a || x == b) && (z == a || z == b);
                    boolean slit = !corner && y % 5 == 3 && (x == 5 || z == 5);
                    put(level, box, x, y, z, slit ? Blocks.IRON_BARS
                            : corner ? ModWorldBlocks.INFERNAL_CASING.get() : Blocks.POLISHED_BLACKSTONE_BRICKS);
                }
            }
        }
        fill(level, box, 5, 1, a, 5, 2, a, Blocks.AIR);                                       // entrada (norte)
        // escalera interior pegada a la pared sur, hasta la plataforma
        for (int y = 1; y <= 17; y++) {
            put(level, box, 5, y, b - 1, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH));
        }
        // plataforma (con trampilla de la escalera), barandilla y foco
        for (int x = a - 1; x <= b + 1; x++) {
            for (int z = a - 1; z <= b + 1; z++) {
                boolean edge = x == a - 1 || x == b + 1 || z == a - 1 || z == b + 1;
                if (!(x == 5 && z == b - 1)) {
                    put(level, box, x, 17, z, ModWorldBlocks.CARBONIZED_PLATE.get());
                }
                if (edge) {
                    put(level, box, x, 18, z, Blocks.IRON_BARS);
                }
            }
        }
        put(level, box, a, 18, a, ModWorldBlocks.INFERNAL_CASING.get());
        put(level, box, a, 19, a, Blocks.SHROOMLIGHT);                                          // foco
        put(level, box, b, 18, b, ModWorldBlocks.INFERNAL_CASING.get());
        put(level, box, b, 19, b, Blocks.SHROOMLIGHT);
        chest(level, box, 3, 1, 3, Direction.SOUTH);
        chest(level, box, b - 1, 18, a + 1, Direction.WEST);
        // dos Centinelas de Ceniza de guardia en la plataforma
        for (int i = 0; i < 2; i++) {
            BlockPos p = pos(i == 0 ? a + 1 : b - 1, 18, 5);
            if (!box.isInside(p)) {
                continue;
            }
            InfernalEntity sentinel = NetherEntities.SENTINEL.get().create(level.getLevel());
            if (sentinel != null) {
                sentinel.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, i * 180, 0);
                sentinel.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null, null);
                sentinel.setPersistenceRequired();
                level.addFreshEntityWithPassengers(sentinel);
            }
        }
    }
}
