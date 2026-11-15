package com.bryanamaya.tecnocinetico.worldgen.structure;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.end.EndEntities;
import com.bryanamaya.tecnocinetico.end.NetworkBlocks;
import com.bryanamaya.tecnocinetico.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
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
 * Restos de la red de la IA en el End (ver {@link EndSiteStructure}). Coordenadas locales desde la esquina (x, z) y el
 * suelo (y = 0 es la losa). Lo aleatorio sale de la semilla de la pieza y de la posición ({@link #roll}).
 * <ul>
 *   <li><b>Nodo de Datos</b> (9x9): losa de blindaje, pilar de racks con un cristal encima, cables, un terminal
 *       (registros 0-3), un cofre pequeño y a veces un Espectro.</li>
 *   <li><b>Granja de Servidores</b> (31x31): nave de 29x23 medio derruida con tres pasillos de racks, el núcleo de datos
 *       en el centro con su terminal, cofre principal y dos pequeños; dos Espectros dentro y dos Torretas en el tejado.</li>
 *   <li><b>Puerto de Lanzamiento</b> (97x97, único): isla propia con un gran cráter en el centro (la marca del salto
 *       de la IA), torre de control con los registros del despegue, hangar con restos, depósitos de combustible, la
 *       torre de servicio derribada dentro del cráter, cerca perimetral, cofres del puerto y guardias.</li>
 * </ul>
 */
public class EndSitePiece extends StructurePiece {

    private static final ResourceLocation NODE_LOOT = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "chests/end_network_node");
    private static final ResourceLocation FARM_LOOT = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "chests/end_server_farm");
    private static final ResourceLocation PORT_LOOT = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "chests/launch_port");
    /** Radio de la isla del Puerto y del cráter. */
    public static final int PORT_RADIUS = 46, CRATER_RADIUS = 18;

    private final int ox, oy, oz, site;
    private final long seed;

    public static int size(int site) {
        return site == EndSiteStructure.NODE ? 9 : site == EndSiteStructure.FARM ? 31 : 97;
    }

    private static int below(int site) {
        return site == EndSiteStructure.PORT ? 34 : 8;
    }

    private static int above(int site) {
        return site == EndSiteStructure.NODE ? 10 : site == EndSiteStructure.FARM ? 12 : 32;
    }

    public EndSitePiece(BlockPos origin, int site, long seed) {
        super(ModStructures.END_SITE_PIECE.get(), 0, new BoundingBox(origin.getX(), origin.getY() - below(site), origin.getZ(),
                origin.getX() + size(site) - 1, origin.getY() + above(site), origin.getZ() + size(site) - 1));
        this.ox = origin.getX();
        this.oy = origin.getY();
        this.oz = origin.getZ();
        this.site = site;
        this.seed = seed;
        setOrientation(null);
    }

    public EndSitePiece(CompoundTag tag) {
        super(ModStructures.END_SITE_PIECE.get(), tag);
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
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                    put(level, box, x, y, z, block);
                }
            }
        }
    }

    /** Cimientos de piedra del End hasta tocar suelo (máx. {@code depth}). */
    private void foundation(WorldGenLevel level, BoundingBox box, int x, int z, int depth) {
        for (int y = -1; y >= -depth; y--) {
            BlockPos p = pos(x, y, z);
            if (!box.isInside(p) || level.getBlockState(p).isSolid()) {
                return;
            }
            level.setBlock(p, Blocks.END_STONE.defaultBlockState(), 2);
        }
    }

    private BlockState rack(Direction facing) {
        return ModBlocks.SERVER_RACK.get().defaultBlockState().setValue(NetworkBlocks.Facing.FACING, facing);
    }

    private void terminal(WorldGenLevel level, BoundingBox box, int x, int y, int z, Direction facing, int entry) {
        put(level, box, x, y, z, ModBlocks.NETWORK_TERMINAL.get().defaultBlockState()
                .setValue(NetworkBlocks.Facing.FACING, facing).setValue(NetworkBlocks.Terminal.ENTRY, entry));
    }

    private void chest(WorldGenLevel level, BoundingBox box, int x, int y, int z, Direction facing, ResourceLocation loot) {
        BlockPos p = pos(x, y, z);
        if (!box.isInside(p)) {
            return;
        }
        level.setBlock(p, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
        RandomizableContainerBlockEntity.setLootTable(level, RandomSource.create(seed ^ p.asLong()), p, loot);
    }

    /** Guardia persistente (solo en el chunk que contiene su posición). */
    private void guard(WorldGenLevel level, BoundingBox box, int x, int y, int z, EntityType<? extends Mob> type) {
        BlockPos p = pos(x, y, z);
        if (!box.isInside(p)) {
            return;
        }
        Mob mob = type.create(level.getLevel());
        if (mob != null) {
            mob.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, (float) (roll(x, y, z, 99) * 360), 0);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null, null);
            mob.setPersistenceRequired();
            level.addFreshEntityWithPassengers(mob);
        }
    }

    private Block floorBlock(int x, int z, int salt) {
        double r = roll(x, 0, z, salt);
        return r < 0.12 ? ModBlocks.CORRUPTED_END_STONE.get() : ModBlocks.NETWORK_PLATING.get();
    }

    // ------------------------------------------------------------------ construcción

    @Override
    public void postProcess(WorldGenLevel level, StructureManager manager, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunk, BlockPos pivot) {
        switch (site) {
            case EndSiteStructure.NODE -> node(level, box);
            case EndSiteStructure.FARM -> farm(level, box);
            default -> port(level, box);
        }
    }

    /** Nodo de Datos: pilar de racks en el centro (x/z 3-5) con el cristal encima. */
    private void node(WorldGenLevel level, BoundingBox box) {
        for (int x = 1; x <= 7; x++) {
            for (int z = 1; z <= 7; z++) {
                foundation(level, box, x, z, 6);
                put(level, box, x, 0, z, floorBlock(x, z, 1));
                fill(level, box, x, 1, z, x, 8, z, Blocks.AIR);
            }
        }
        for (int y = 1; y <= 5; y++) {
            for (int x = 3; x <= 5; x++) {
                for (int z = 3; z <= 5; z++) {
                    boolean corner = (x == 3 || x == 5) && (z == 3 || z == 5);
                    boolean broken = y >= 4 && roll(x, y, z, 2) < 0.3;
                    if (broken) {
                        continue;
                    }
                    if (corner || x == 4 && z == 4) {
                        put(level, box, x, y, z, ModBlocks.NETWORK_PLATING.get());
                    } else {
                        Direction out = x == 3 ? Direction.WEST : x == 5 ? Direction.EAST : z == 3 ? Direction.NORTH : Direction.SOUTH;
                        put(level, box, x, y, z, rack(out));
                    }
                }
            }
        }
        fill(level, box, 3, 6, 3, 5, 6, 5, ModBlocks.NETWORK_PLATING.get());
        put(level, box, 4, 7, 4, ModBlocks.DATA_CRYSTAL.get());
        // cables en cruz desde el pilar
        for (int i = 1; i <= 7; i++) {
            if (i < 3 || i > 5) {
                put(level, box, i, 1, 4, ModBlocks.DATA_CABLE.get());
                put(level, box, 4, 1, i, ModBlocks.DATA_CABLE.get());
            }
        }
        for (int[] c : new int[][]{{1, 1}, {7, 1}, {1, 7}, {7, 7}}) {
            if (roll(c[0], 1, c[1], 3) < 0.6) {
                put(level, box, c[0], 1, c[1], ModBlocks.DATA_CRYSTAL.get());
            }
        }
        terminal(level, box, 2, 1, 2, Direction.NORTH, (int) (roll(2, 1, 2, 4) * 4));
        chest(level, box, 6, 1, 2, Direction.NORTH, NODE_LOOT);
        if (roll(4, 1, 7, 5) < 0.4) {
            guard(level, box, 4, 2, 7, EndEntities.DATA_SPECTER.get());
        }
    }

    /** Granja de Servidores: nave de x 1-29, z 4-26, puertas al norte y al sur. */
    private void farm(WorldGenLevel level, BoundingBox box) {
        int x0 = 1, x1 = 29, z0 = 4, z1 = 26, h = 7;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                foundation(level, box, x, z, 8);
                put(level, box, x, 0, z, floorBlock(x, z, 10));
                fill(level, box, x, 1, z, x, h + 2, z, Blocks.AIR);
                boolean wall = x == x0 || x == x1 || z == z0 || z == z1;
                if (wall) {
                    int top = roll(x, 0, z, 11) < 0.12 ? 0 : 2 + (int) (roll(x, 1, z, 12) * (h - 1));   // muros derruidos
                    if (roll(x, 2, z, 13) < 0.7) {
                        top = h;
                    }
                    for (int y = 1; y <= top; y++) {
                        put(level, box, x, y, z, y == 4 && (x + z) % 4 == 0 ? Blocks.TINTED_GLASS : ModBlocks.NETWORK_PLATING.get());
                    }
                } else if (roll(x, h + 1, z, 14) > 0.35) {
                    put(level, box, x, h + 1, z, ModBlocks.NETWORK_PLATING.get());            // tejado con agujeros
                }
            }
        }
        // puertas
        fill(level, box, 14, 1, z0, 16, 3, z0, Blocks.AIR);
        fill(level, box, 14, 1, z1, 16, 3, z1, Blocks.AIR);
        // tres pasillos de racks espalda con espalda (sin el centro, donde está el núcleo)
        for (int[] row : new int[][]{{8, 9}, {14, 15}, {20, 21}}) {
            for (int x = 4; x <= 26; x++) {
                if (x >= 12 && x <= 18) {
                    continue;
                }
                for (int y = 1; y <= 3; y++) {
                    if (roll(x, y, row[0], 15) < 0.15 + y * 0.05) {
                        continue;                                                       // racks desmontados
                    }
                    put(level, box, x, y, row[0], rack(Direction.NORTH));
                    put(level, box, x, y, row[1], rack(Direction.SOUTH));
                }
            }
        }
        // cables por los pasillos
        for (int x = 3; x <= 27; x++) {
            for (int z : new int[]{6, 11, 12, 17, 18, 24}) {
                if (roll(x, 1, z, 16) < 0.55) {
                    put(level, box, x, 1, z, ModBlocks.DATA_CABLE.get());
                }
            }
        }
        // núcleo de datos central (x 13-17, z 13-17)
        fill(level, box, 14, 1, 14, 16, 1, 16, ModBlocks.NETWORK_PLATING.get());
        put(level, box, 15, 2, 15, ModBlocks.NETWORK_PLATING.get());
        put(level, box, 15, 3, 15, ModBlocks.DATA_CRYSTAL.get());
        for (int[] c : new int[][]{{14, 14}, {16, 14}, {14, 16}, {16, 16}}) {
            put(level, box, c[0], 2, c[1], ModBlocks.DATA_CRYSTAL.get());
        }
        terminal(level, box, 15, 1, 13, Direction.NORTH, 1 + (int) (roll(15, 1, 13, 17) * 3));
        chest(level, box, 15, 1, 17, Direction.SOUTH, FARM_LOOT);
        chest(level, box, 2, 1, 6, Direction.EAST, NODE_LOOT);
        chest(level, box, 28, 1, 24, Direction.WEST, NODE_LOOT);
        // guardias: dos Espectros dentro y dos Torretas en esquinas del tejado
        guard(level, box, 8, 1, 11, EndEntities.DATA_SPECTER.get());
        guard(level, box, 22, 1, 18, EndEntities.DATA_SPECTER.get());
        for (int[] c : new int[][]{{2, 5}, {28, 25}}) {
            fill(level, box, c[0] - 1, h + 1, c[1] - 1, c[0] + 1, h + 1, c[1] + 1, ModBlocks.NETWORK_PLATING.get());
            guard(level, box, c[0], h + 2, c[1], ModEntities.END_RELAY_TURRET.get());
        }
    }

    // ------------------------------------------------------------------ Puerto de Lanzamiento

    /** Puerto de Lanzamiento: centro local (c, c); la losa de la isla en y = 0. */
    private void port(WorldGenLevel level, BoundingBox box) {
        int c = size(site) / 2;
        // isla, cráter y suelo
        for (int x = 0; x < size(site); x++) {
            for (int z = 0; z < size(site); z++) {
                double d = Math.sqrt((x - c) * (x - c) + (z - c) * (z - c));
                double edge = PORT_RADIUS - 2 + roll(x / 3, 0, z / 3, 20) * 4;              // borde irregular
                if (d > edge) {
                    continue;
                }
                int depth = 6 + (int) ((1 - (d / edge) * (d / edge)) * 24 + roll(x, 1, z, 21) * 3);
                fill(level, box, x, -depth, z, x, 0, z, Blocks.END_STONE);
                fill(level, box, x, 1, z, x, 30, z, Blocks.AIR);                          // despeja lo que hubiera
                if (d < CRATER_RADIUS) {
                    double k = d / CRATER_RADIUS;
                    int floor = -(int) Math.round(12 * (1 - k * k));
                    fill(level, box, x, floor + 1, z, x, 0, z, Blocks.AIR);
                    double r = roll(x, floor, z, 22);
                    Block lining = d < 5 ? (r < 0.5 ? Blocks.CRYING_OBSIDIAN : Blocks.OBSIDIAN)
                            : r < 0.45 ? ModBlocks.CORRUPTED_END_STONE.get() : r < 0.8 ? Blocks.BLACKSTONE : Blocks.OBSIDIAN;
                    put(level, box, x, floor, z, lining);
                    put(level, box, x, floor - 1, z, Blocks.OBSIDIAN);
                } else if (d < CRATER_RADIUS + 3) {                                      // reborde levantado
                    int lip = (int) Math.round(2.5 * (1 - Math.abs(d - (CRATER_RADIUS + 1.5)) / 1.5));
                    for (int y = 1; y <= lip; y++) {
                        put(level, box, x, y, z, roll(x, y, z, 23) < 0.5 ? ModBlocks.CORRUPTED_END_STONE.get() : Blocks.BLACKSTONE);
                    }
                    put(level, box, x, 0, z, Blocks.BLACKSTONE);
                } else {
                    double r = roll(x, 0, z, 24);
                    boolean scorched = d < CRATER_RADIUS + 9 && r < 0.6 - (d - CRATER_RADIUS - 3) * 0.08;
                    put(level, box, x, 0, z, scorched ? (r < 0.25 ? Blocks.BLACKSTONE : ModBlocks.CORRUPTED_END_STONE.get())
                            : r < 0.05 ? ModBlocks.CORRUPTED_END_STONE.get() : Blocks.END_STONE);
                }
            }
        }
        // cables desde el cráter a cada edificio y cristales en un anillo
        for (int r = CRATER_RADIUS + 4; r <= 34; r++) {
            for (int[] dir : new int[][]{{0, -1}, {1, 0}, {0, 1}, {-1, 0}}) {
                if (roll(r, 1, dir[0] * 7 + dir[1], 25) < 0.8) {
                    put(level, box, c + dir[0] * r, 1, c + dir[1] * r, ModBlocks.DATA_CABLE.get());
                }
            }
        }
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI * 2 / 24 + 0.13;
            int x = c + (int) Math.round(Math.cos(a) * (CRATER_RADIUS + 6)), z = c + (int) Math.round(Math.sin(a) * (CRATER_RADIUS + 6));
            if (roll(i, 1, 0, 26) < 0.7) {
                put(level, box, x, 1, z, ModBlocks.DATA_CRYSTAL.get());
            }
        }
        // cerca perimetral rota
        for (int i = 0; i < 280; i++) {
            double a = i * Math.PI * 2 / 280;
            int x = c + (int) Math.round(Math.cos(a) * (PORT_RADIUS - 5)), z = c + (int) Math.round(Math.sin(a) * (PORT_RADIUS - 5));
            if (roll(i / 4, 0, 0, 27) < 0.7) {
                put(level, box, x, 1, z, ModBlocks.NETWORK_PLATING.get());
                if (roll(i, 2, 0, 28) < 0.6) {
                    put(level, box, x, 2, z, Blocks.IRON_BARS);
                }
            }
        }
        controlTower(level, box, c, c - 34);
        hangar(level, box, c + 34, c);
        fuelTanks(level, box, c - 34, c);
        fallenGantry(level, box, c);
        // guardias alrededor del cráter
        guard(level, box, c + 24, 1, c + 24, EndEntities.DATA_SPECTER.get());
        guard(level, box, c - 24, 1, c + 24, EndEntities.DATA_SPECTER.get());
        guard(level, box, c + 24, 1, c - 24, EndEntities.DATA_SPECTER.get());
        guard(level, box, c - 24, 1, c - 24, EndEntities.DATA_SPECTER.get());
    }

    /** Torre de control (norte), 7x7 de 15 de alto, con la sala de mando arriba (registros 4 y 5, cofre del puerto). */
    private void controlTower(WorldGenLevel level, BoundingBox box, int cx, int cz) {
        int a = cx - 3, b = cx + 3, n = cz - 3, s = cz + 3, top = 14;
        for (int y = 1; y <= top + 3; y++) {
            for (int x = a; x <= b; x++) {
                for (int z = n; z <= s; z++) {
                    boolean wall = x == a || x == b || z == n || z == s;
                    boolean corner = (x == a || x == b) && (z == n || z == s);
                    if (y <= top) {
                        if (wall) {
                            boolean window = !corner && y % 4 == 2 && (x == cx || z == cz);
                            put(level, box, x, y, z, window ? Blocks.TINTED_GLASS : ModBlocks.NETWORK_PLATING.get());
                        } else {
                            put(level, box, x, y, z, Blocks.AIR);
                        }
                    } else if (y == top + 1) {
                        put(level, box, x, y, z, wall ? Blocks.TINTED_GLASS : Blocks.AIR);       // cristalera de la sala de mando
                    } else if (y == top + 2) {
                        put(level, box, x, y, z, wall ? Blocks.TINTED_GLASS : Blocks.AIR);
                    } else {
                        put(level, box, x, y, z, ModBlocks.NETWORK_PLATING.get());                // techo
                    }
                }
            }
        }
        fill(level, box, cx, 1, s, cx, 2, s, Blocks.AIR);                                       // puerta al sur (hacia el cráter)
        for (int y = 1; y <= top; y++) {
            put(level, box, b - 1, y, n + 1, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST));
        }
        // suelo de la sala de mando con hueco de la escalera
        for (int x = a + 1; x <= b - 1; x++) {
            for (int z = n + 1; z <= s - 1; z++) {
                if (!(x == b - 1 && z == n + 1)) {
                    put(level, box, x, top, z, ModBlocks.NETWORK_PLATING.get());
                }
            }
        }
        terminal(level, box, cx - 1, top + 1, s - 1, Direction.SOUTH, 4);
        terminal(level, box, cx + 1, top + 1, s - 1, Direction.SOUTH, 5);
        put(level, box, cx, top + 1, s - 1, rack(Direction.SOUTH));
        chest(level, box, a + 1, top + 1, n + 1, Direction.EAST, PORT_LOOT);
        put(level, box, cx, top + 4, cz, ModBlocks.DATA_CRYSTAL.get());                         // baliza
        fill(level, box, cx - 1, top + 3, cz - 1, cx + 1, top + 3, cz + 1, ModBlocks.NETWORK_PLATING.get());
        guard(level, box, cx + 1, top + 4, cz + 1, ModEntities.END_RELAY_TURRET.get());
    }

    /** Hangar (este): nave de 13x17 con el tejado medio caído, restos de la lanzadera y el registro 6. */
    private void hangar(WorldGenLevel level, BoundingBox box, int cx, int cz) {
        int a = cx - 6, b = cx + 6, n = cz - 8, s = cz + 8, h = 9;
        for (int x = a; x <= b; x++) {
            for (int z = n; z <= s; z++) {
                put(level, box, x, 0, z, floorBlock(x, z, 30));
                boolean wall = x == a || x == b || z == n || z == s;
                boolean door = x == a && z >= cz - 3 && z <= cz + 3;                            // portón hacia el cráter (oeste)
                for (int y = 1; y <= h; y++) {
                    put(level, box, x, y, z, wall && !door && roll(x, y, z, 31) < 0.92 ? ModBlocks.NETWORK_PLATING.get() : Blocks.AIR);
                }
                if (roll(x, h + 1, z, 32) < 0.55) {
                    put(level, box, x, h + 1, z, ModBlocks.NETWORK_PLATING.get());
                }
            }
        }
        // restos: cono de la lanzadera tumbado y placas sueltas
        for (int i = 0; i < 7; i++) {
            int r = i < 3 ? 2 : 1;
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = 0; dy <= r * 2; dy++) {
                    if (dx * dx + (dy - r) * (dy - r) <= r * r + 1) {
                        put(level, box, cx + dx, 1 + dy, cz - 4 + i, i == 6 ? Blocks.IRON_BLOCK : ModBlocks.NETWORK_PLATING.get());
                    }
                }
            }
        }
        for (int i = 0; i < 18; i++) {
            int x = a + 1 + (int) (roll(i, 0, 1, 33) * 11), z = n + 1 + (int) (roll(i, 0, 2, 33) * 15);
            put(level, box, x, 1, z, roll(i, 0, 3, 33) < 0.5 ? Blocks.IRON_BARS : ModBlocks.DATA_CABLE.get());
        }
        terminal(level, box, b - 1, 1, s - 1, Direction.WEST, 6);
        chest(level, box, b - 1, 1, n + 1, Direction.WEST, PORT_LOOT);
        fill(level, box, cx - 1, h + 1, cz - 1, cx + 1, h + 1, cz + 1, ModBlocks.NETWORK_PLATING.get());
        guard(level, box, cx, h + 2, cz, ModEntities.END_RELAY_TURRET.get());
    }

    /** Depósitos de combustible (oeste): tres cilindros con bandas y el registro 7. */
    private void fuelTanks(WorldGenLevel level, BoundingBox box, int cx, int cz) {
        for (int t = -1; t <= 1; t++) {
            int tz = cz + t * 7, height = 7 + (int) (roll(t, 0, 0, 40) * 3);
            for (int x = cx - 3; x <= cx + 3; x++) {
                for (int z = tz - 3; z <= tz + 3; z++) {
                    double d = Math.sqrt((x - cx) * (x - cx) + (z - tz) * (z - tz));
                    if (d > 3.2) {
                        continue;
                    }
                    put(level, box, x, 0, z, ModBlocks.NETWORK_PLATING.get());
                    for (int y = 1; y <= height; y++) {
                        boolean shell = d > 2.2;
                        if (shell && !(t == 0 && y > height - 3 && roll(x, y, z, 41) < 0.5)) {   // el del medio, reventado
                            put(level, box, x, y, z, y % 3 == 0 ? Blocks.IRON_BLOCK : ModBlocks.NETWORK_PLATING.get());
                        } else if (!shell) {
                            put(level, box, x, y, z, Blocks.AIR);
                        }
                    }
                    put(level, box, x, height + 1, z, d <= 2.2 ? ModBlocks.NETWORK_PLATING.get() : Blocks.AIR);
                }
            }
        }
        // tuberías hacia el cráter
        for (int x = cx + 4; x <= cx + 10; x++) {
            put(level, box, x, 2, cz, Blocks.IRON_BARS);
        }
        terminal(level, box, cx + 5, 1, cz + 4, Direction.EAST, 7);
        chest(level, box, cx + 5, 1, cz - 4, Direction.EAST, PORT_LOOT);
    }

    /** Torre de servicio (sur): el pie sigue en pie junto al reborde y el resto cayó dentro del cráter. */
    private void fallenGantry(WorldGenLevel level, BoundingBox box, int c) {
        int baseZ = c + CRATER_RADIUS + 6;
        for (int y = 1; y <= 10; y++) {                                                        // pie de 3x3 en celosía
            for (int[] p : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
                put(level, box, c + p[0], y, baseZ + p[1], ModBlocks.NETWORK_PLATING.get());
            }
            if (y % 3 == 0) {
                fill(level, box, c - 1, y, baseZ - 1, c + 1, y, baseZ + 1, Blocks.IRON_BARS);
            }
        }
        // tramo caído: de la boca del pie hacia el centro del cráter, bajando
        for (int i = 0; i <= CRATER_RADIUS + 2; i++) {
            int z = baseZ - 2 - i;
            double d = Math.abs(z - c);
            int y = d < CRATER_RADIUS ? -(int) Math.round(12 * (1 - (d / CRATER_RADIUS) * (d / CRATER_RADIUS))) + 1 : 1;
            y = Math.max(y, 8 - i);                                                            // apoyado: baja poco a poco
            put(level, box, c - 1, y, z, ModBlocks.NETWORK_PLATING.get());
            put(level, box, c + 1, y, z, ModBlocks.NETWORK_PLATING.get());
            if (i % 3 == 0) {
                put(level, box, c, y, z, Blocks.IRON_BARS);
            }
        }
    }

    /** Para pruebas: centro del Puerto en coordenadas del mundo (al nivel de la losa). */
    public BlockPos portCenter() {
        int c = size(site) / 2;
        return pos(c, 0, c);
    }

    public int site() {
        return site;
    }
}
