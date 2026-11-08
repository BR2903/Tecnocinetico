package com.bryanamaya.tecnocinetico.worldgen.structure;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.ModWorldBlocks;
import com.bryanamaya.tecnocinetico.block.custom.PileBlock;
import com.bryanamaya.tecnocinetico.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Pieza única de la Instalación de la IA. Coordenadas absolutas (orientación nula); todo sale de `seed` o de
 * aleatorios por posición, nunca del mundo (cada chunk construye su parte del mismo complejo).
 *
 * Geometría (E = entrada en la superficie, a la altura del suelo sy):
 * - Escalera de 3 de ancho que baja hacia +Z desde E durante {@link #DEPTH} bloques.
 * - Complejo a fy = sy - DEPTH: salas de 11x11 (interior) en rejilla 3x3 con paso 16; la sala (1,0) recibe la
 *   escalera; la (1,1) es la cámara del Núcleo (más alta). Pasillos de 3x3 entre salas vecinas.
 */
public class AiInstallationPiece extends StructurePiece {

    public static final int DEPTH = 16;
    private static final int ROOM = 11, PITCH = 16;
    private static final ResourceLocation LOOT = loot("ai_installation");
    private static final ResourceLocation LOOT_CORE = loot("ai_core");

    private enum Room { SECURITY, SERVERS, LAB, STORAGE, COLLAPSED, CORE }

    private final int ex, sy, ez;
    private final long seed;

    public AiInstallationPiece(BlockPos entrance, long seed) {
        super(ModStructures.AI_INSTALLATION_PIECE.get(), 0, bounds(entrance));
        this.ex = entrance.getX();
        this.sy = entrance.getY();
        this.ez = entrance.getZ();
        this.seed = seed;
        setOrientation(null);
    }

    public AiInstallationPiece(CompoundTag tag) {
        super(ModStructures.AI_INSTALLATION_PIECE.get(), tag);
        this.ex = tag.getInt("EX");
        this.sy = tag.getInt("SY");
        this.ez = tag.getInt("EZ");
        this.seed = tag.getLong("Seed");
    }

    private static BoundingBox bounds(BlockPos e) {
        int x0 = e.getX() - 21 - 2, z0 = e.getZ() - 6;
        return new BoundingBox(x0, e.getY() - DEPTH - 2, z0, x0 + 3 * PITCH + 2, e.getY() + 10, e.getZ() + 15 + 3 * PITCH + 2);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("EX", ex);
        tag.putInt("SY", sy);
        tag.putInt("EZ", ez);
        tag.putLong("Seed", seed);
    }

    private static ResourceLocation loot(String n) {
        return ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "chests/" + n);
    }

    // ------------------------------------------------------------------ utilidades

    private int fy() { return sy - DEPTH; }
    private int roomX(int i) { return ex - 21 + i * PITCH; }          // primera x interior de la columna i
    private int roomZ(int j) { return ez + 15 + j * PITCH; }          // primera z interior de la fila j

    private RandomSource at(int x, int y, int z, int salt) {
        return RandomSource.create(seed * 31L + salt + (x * 73856093L ^ y * 19349663L ^ z * 83492791L));
    }

    private void set(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState s) {
        placeBlock(level, s, x, y, z, box);
    }

    private BlockState wall(int x, int y, int z) {
        float v = at(x, y, z, 1).nextFloat();
        return v < 0.72F ? Blocks.DEEPSLATE_TILES.defaultBlockState()
                : v < 0.92F ? Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState()
                : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    }

    /**
     * Caja hueca: paredes/suelo/techo de pizarra, interior de aire; las luces del techo (una cada 4, alguna rota).
     * Interior: x0..x1, y0..y1 (y0 = primera capa de aire), z0..z1.
     */
    private void hollow(WorldGenLevel level, BoundingBox box, int x0, int y0, int z0, int x1, int y1, int z1, boolean lights) {
        for (int x = x0 - 1; x <= x1 + 1; x++) {
            for (int z = z0 - 1; z <= z1 + 1; z++) {
                for (int y = y0 - 1; y <= y1 + 1; y++) {
                    boolean shell = x < x0 || x > x1 || z < z0 || z > z1 || y < y0 || y > y1;
                    BlockState s;
                    if (!shell) {
                        s = Blocks.AIR.defaultBlockState();
                    } else if (y == y0 - 1 && x >= x0 && x <= x1 && z >= z0 && z <= z1) {
                        s = Blocks.POLISHED_DEEPSLATE.defaultBlockState();                     // suelo
                    } else if (lights && y == y1 + 1 && (x - x0) % 4 == 1 && (z - z0) % 4 == 1
                            && at(x, y, z, 2).nextFloat() < 0.7F) {
                        s = Blocks.SEA_LANTERN.defaultBlockState();                            // luz del techo
                    } else {
                        s = wall(x, y, z);
                    }
                    set(level, box, x, y, z, s);
                }
            }
        }
    }

    private void container(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState s, ResourceLocation loot) {
        BlockPos p = new BlockPos(x, y, z);
        if (box.isInside(p)) {
            level.setBlock(p, s, 2);
            RandomizableContainerBlockEntity.setLootTable(level, at(x, y, z, 9), p, loot);
        }
    }

    private void spawn(WorldGenLevel level, BoundingBox box, net.minecraft.world.entity.EntityType<? extends Mob> type,
                       int x, int y, int z) {
        BlockPos p = new BlockPos(x, y, z);
        if (!box.isInside(p)) {
            return;
        }
        Mob m = type.create(level.getLevel());
        if (m == null) {
            return;
        }
        m.moveTo(x + 0.5D, y, z + 0.5D, at(x, y, z, 7).nextFloat() * 360.0F, 0.0F);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null, null);
        m.setPersistenceRequired();
        level.addFreshEntityWithPassengers(m);
    }

    // ------------------------------------------------------------------ construcción

    @Override
    public void postProcess(WorldGenLevel level, StructureManager sm, ChunkGenerator gen, RandomSource ignored,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        RandomSource r = RandomSource.create(seed);
        Room[][] rooms = new Room[3][3];
        Room[] pool = {Room.SERVERS, Room.LAB, Room.STORAGE, Room.SERVERS, Room.COLLAPSED, Room.STORAGE, Room.LAB};
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                rooms[i][j] = pool[r.nextInt(pool.length)];
            }
        }
        rooms[1][0] = Room.SECURITY;
        rooms[1][1] = Room.CORE;

        // obra: primero los pasillos (sus tapas caen dentro de las salas), luego las salas encima (las vacían),
        // y al final se abren las puertas en las paredes de las salas
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (i < 2) {                                            // pasillo hacia el este
                    int zc = roomZ(j) + ROOM / 2;
                    hollow(level, box, roomX(i) + ROOM, fy() + 1, zc - 1, roomX(i + 1) - 1, fy() + 3, zc + 1, false);
                }
                if (j < 2) {                                            // pasillo hacia el sur
                    int xc = roomX(i) + ROOM / 2;
                    hollow(level, box, xc - 1, fy() + 1, roomZ(j) + ROOM, xc + 1, fy() + 3, roomZ(j + 1) - 1, false);
                }
            }
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int h = rooms[i][j] == Room.CORE ? 8 : 5;
                hollow(level, box, roomX(i), fy() + 1, roomZ(j), roomX(i) + ROOM - 1, fy() + h, roomZ(j) + ROOM - 1, true);
            }
        }
        // abrir las puertas entre salas y pasillos (las paredes de las salas taparon los extremos)
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                for (int y = fy() + 1; y <= fy() + 3; y++) {
                    for (int o = -1; o <= 1; o++) {
                        if (i < 2) {
                            set(level, box, roomX(i) + ROOM, y, roomZ(j) + ROOM / 2 + o, Blocks.AIR.defaultBlockState());
                            set(level, box, roomX(i + 1) - 1, y, roomZ(j) + ROOM / 2 + o, Blocks.AIR.defaultBlockState());
                        }
                        if (j < 2) {
                            set(level, box, roomX(i) + ROOM / 2 + o, y, roomZ(j) + ROOM, Blocks.AIR.defaultBlockState());
                            set(level, box, roomX(i) + ROOM / 2 + o, y, roomZ(j + 1) - 1, Blocks.AIR.defaultBlockState());
                        }
                    }
                }
            }
        }
        stairs(level, box);
        bunker(level, box);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int x0 = roomX(i), z0 = roomZ(j), y = fy() + 1;
                switch (rooms[i][j]) {
                    case SECURITY -> security(level, box, x0, y, z0);
                    case SERVERS -> servers(level, box, x0, y, z0);
                    case LAB -> lab(level, box, x0, y, z0);
                    case STORAGE -> storage(level, box, x0, y, z0);
                    case COLLAPSED -> collapsed(level, box, x0, y, z0);
                    case CORE -> core(level, box, x0, y, z0);
                }
            }
        }
        // un par de Cuerpos de Enjambre merodeando por salas al azar
        for (int k = 0; k < 2; k++) {
            int i = r.nextInt(3), j = 1 + r.nextInt(2);
            spawn(level, box, ModEntities.SWARM_BODY.get(), roomX(i) + 2 + r.nextInt(7), fy() + 1, roomZ(j) + 2 + r.nextInt(7));
        }
    }

    /**
     * Escalera de 3 de ancho que baja hacia el sur desde la entrada. Peldaño k en z = ez + k, y = sy - k.
     * Tramos 0..14: túnel con paredes y techo (el 14 atraviesa la pared norte de la sala de seguridad).
     * Tramo 15: solo el último peldaño, ya dentro de la sala (y = fy + 1).
     */
    private void stairs(WorldGenLevel level, BoundingBox box) {
        BlockState step = Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        for (int k = 0; k <= 15; k++) {
            int z = ez + k, y = sy - k;
            for (int x = ex - 2; x <= ex + 2; x++) {
                boolean side = x == ex - 2 || x == ex + 2;
                if (k == 15) {
                    if (!side) {
                        set(level, box, x, y, z, step);
                    }
                    continue;
                }
                for (int yy = y - 1; yy <= y + 4; yy++) {
                    BlockState s;
                    if (side || yy == y - 1) {
                        s = wall(x, yy, z);
                    } else if (yy == y + 4) {
                        s = k % 5 == 2 ? Blocks.SEA_LANTERN.defaultBlockState() : wall(x, yy, z);
                    } else if (yy == y) {
                        s = step;
                    } else {
                        s = Blocks.AIR.defaultBlockState();
                    }
                    set(level, box, x, yy, z, s);
                }
            }
        }
    }

    /**
     * Búnker de entrada (7x8): muros de hormigón con puerta al norte y marco de franjas de peligro, tejado y
     * antena oxidada. Dentro queda abierto el hueco de la escalera (no se rellena el cimiento ahí).
     */
    private void bunker(WorldGenLevel level, BoundingBox box) {
        for (int x = ex - 3; x <= ex + 3; x++) {
            for (int z = ez - 4; z <= ez + 3; z++) {
                boolean edge = x == ex - 3 || x == ex + 3 || z == ez - 4 || z == ez + 3;
                boolean stairwell = Math.abs(x - ex) <= 1 && z >= ez;
                if (!stairwell) {
                    fillColumnDown(level, ModWorldBlocks.CONCRETE.get().defaultBlockState(), x, sy, z, box);
                }
                for (int y = sy + 1; y <= sy + 5; y++) {
                    BlockState s;
                    if (y == sy + 5) {
                        s = ModWorldBlocks.CONCRETE.get().defaultBlockState();
                    } else if (!edge) {
                        s = Blocks.AIR.defaultBlockState();
                    } else if (z == ez - 4 && Math.abs(x - ex) <= 1 && y <= sy + 3) {
                        s = Blocks.AIR.defaultBlockState();                              // puerta
                    } else if (z == ez - 4 && Math.abs(x - ex) == 2) {
                        s = (y % 2 == 0 ? Blocks.YELLOW_CONCRETE : Blocks.BLACK_CONCRETE).defaultBlockState();
                    } else {
                        s = at(x, y, z, 3).nextFloat() < 0.3F ? ModWorldBlocks.CRACKED_CONCRETE.get().defaultBlockState()
                                : ModWorldBlocks.CONCRETE.get().defaultBlockState();
                    }
                    set(level, box, x, y, z, s);
                }
            }
        }
        for (int y = sy + 6; y <= sy + 11; y++) {                       // antena
            set(level, box, ex + 2, y, ez + 2, ModWorldBlocks.RUSTED_METAL_BEAM.get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
        }
        set(level, box, ex + 2, sy + 12, ez + 2, Blocks.LIGHTNING_ROD.defaultBlockState());
        set(level, box, ex + 1, sy + 9, ez + 2, ModWorldBlocks.RUSTED_GRATE.get().defaultBlockState());
        set(level, box, ex + 3, sy + 9, ez + 2, ModWorldBlocks.RUSTED_GRATE.get().defaultBlockState());
        // Anclador: vigila la entrada (antiaéreo: nada de llegar volando con el jetpack)
        spawn(level, box, ModEntities.ANCHOR.get(), ex + 5, sy + 1, ez - 7);
        // drones de patrulla: rondan la antena (2, a veces 3)
        spawn(level, box, ModEntities.DRONE.get(), ex - 4, sy + 10, ez);
        spawn(level, box, ModEntities.DRONE.get(), ex + 5, sy + 13, ez + 4);
        if (at(ex, sy, ez, 11).nextFloat() < 0.4F) {
            spawn(level, box, ModEntities.DRONE.get(), ex, sy + 15, ez - 6);
        }
    }

    // ------------------------------------------------------------------ salas

    private void security(WorldGenLevel level, BoundingBox box, int x0, int y, int z0) {
        // barricada de cajas y una torreta mirando a la escalera
        for (int x = x0 + 2; x <= x0 + 8; x += 3) {
            set(level, box, x, y, z0 + 6, Blocks.BARREL.defaultBlockState());
            set(level, box, x + 1, y, z0 + 6, ModWorldBlocks.RUSTED_METAL_SHEET.get().defaultBlockState());
        }
        spawn(level, box, ModEntities.SENTRY_TURRET.get(), x0 + 5, y, z0 + 8);
        container(level, box, x0 + 1, y, z0 + 9, Blocks.CHEST.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST), LOOT);
    }

    private void servers(WorldGenLevel level, BoundingBox box, int x0, int y, int z0) {
        // dos filas de racks: armazón de pizarra pulida, luces (lámparas apagadas = servidores muertos) y cristal cian
        for (int row : new int[]{z0 + 2, z0 + 8}) {
            for (int x = x0 + 1; x <= x0 + 9; x++) {
                if (x == x0 + 5) {
                    continue;                                            // pasillo
                }
                for (int yy = y; yy <= y + 2; yy++) {
                    RandomSource rr = at(x, yy, row, 4);
                    BlockState s = yy == y + 1 ? (rr.nextFloat() < 0.5F ? Blocks.REDSTONE_LAMP.defaultBlockState()
                            : Blocks.CYAN_STAINED_GLASS.defaultBlockState()) : Blocks.POLISHED_BLACKSTONE.defaultBlockState();
                    set(level, box, x, yy, row, s);
                }
            }
        }
        if (at(x0, y, z0, 5).nextBoolean()) {
            spawn(level, box, ModEntities.SENTRY_TURRET.get(), x0 + 5, y, z0 + 5);
        }
    }

    private void lab(WorldGenLevel level, BoundingBox box, int x0, int y, int z0) {
        // tanques de cultivo de cinetrita: cristal con racimo dentro, mesas con soportes de pociones
        for (int k = 0; k < 3; k++) {
            int x = x0 + 2 + k * 3, z = z0 + 2;
            set(level, box, x, y, z, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
            set(level, box, x, y + 1, z, ModWorldBlocks.CINETRITE_CLUSTER.get().defaultBlockState()
                    .setValue(AmethystClusterBlock.FACING, Direction.UP));
            set(level, box, x, y + 2, z, Blocks.GLASS.defaultBlockState());
            for (Direction d : Direction.Plane.HORIZONTAL) {
                set(level, box, x + d.getStepX(), y + 1, z + d.getStepZ(), Blocks.GLASS.defaultBlockState());
            }
        }
        for (int x = x0 + 2; x <= x0 + 8; x++) {
            set(level, box, x, y, z0 + 8, Blocks.SMOOTH_STONE_SLAB.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.TOP));
            if (x % 3 == 0) {
                set(level, box, x, y + 1, z0 + 8, Blocks.BREWING_STAND.defaultBlockState());
            }
        }
        set(level, box, x0 + 9, y, z0 + 5, Blocks.CAULDRON.defaultBlockState());
        container(level, box, x0 + 1, y, z0 + 5, Blocks.CHEST.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST), LOOT);
    }

    private void storage(WorldGenLevel level, BoundingBox box, int x0, int y, int z0) {
        for (int k = 0; k < 6; k++) {
            RandomSource cr = at(x0, k, z0, 6);
            int x = x0 + 1 + cr.nextInt(9), z = z0 + (cr.nextBoolean() ? 1 : 9);
            container(level, box, x, y, z, k < 2 ? Blocks.CHEST.defaultBlockState() : Blocks.BARREL.defaultBlockState(), LOOT);
            if (cr.nextBoolean()) {
                set(level, box, x, y + 1, z, Blocks.BARREL.defaultBlockState());
            }
        }
    }

    private void collapsed(WorldGenLevel level, BoundingBox box, int x0, int y, int z0) {
        for (int x = x0; x < x0 + ROOM; x++) {
            for (int z = z0; z < z0 + ROOM; z++) {
                RandomSource cr = at(x, y, z, 8);
                float v = cr.nextFloat();
                double d = Math.hypot(x - (x0 + 5), z - (z0 + 5));
                if (d < 2.5D) {                                          // montón de roca caída del techo
                    for (int yy = y; yy < y + 3 - (int) d; yy++) {
                        set(level, box, x, yy, z, Blocks.COBBLED_DEEPSLATE.defaultBlockState());
                    }
                } else if (v < 0.15F) {
                    set(level, box, x, y, z, ModWorldBlocks.RUBBLE.get().defaultBlockState().setValue(PileBlock.PIECES, 1 + cr.nextInt(4)));
                } else if (v < 0.22F) {
                    set(level, box, x, y + 3 + cr.nextInt(2), z, Blocks.COBWEB.defaultBlockState());
                }
            }
        }
    }

    private void core(WorldGenLevel level, BoundingBox box, int x0, int y, int z0) {
        int cx = x0 + 5, cz = z0 + 5;
        // zócalo 5x5 y columna de cristal tintado con el núcleo de cinetrita dentro
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                set(level, box, cx + dx, y, cz + dz, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    for (int yy = y + 1; yy <= y + 5; yy++) {
                        boolean inner = dx == 0 && dz == 0;
                        set(level, box, cx + dx, yy, cz + dz, inner
                                ? (yy == y + 3 ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.AIR.defaultBlockState())
                                : Blocks.TINTED_GLASS.defaultBlockState());
                    }
                }
            }
        }
        set(level, box, cx, y + 2, cz, ModWorldBlocks.CINETRITE_CLUSTER.get().defaultBlockState()
                .setValue(AmethystClusterBlock.FACING, Direction.DOWN));
        set(level, box, cx, y + 4, cz, ModWorldBlocks.CINETRITE_CLUSTER.get().defaultBlockState()
                .setValue(AmethystClusterBlock.FACING, Direction.UP));
        set(level, box, cx, y + 6, cz, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        // conductos que irradian hacia las paredes
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int k = 3; k <= 5; k++) {
                set(level, box, cx + d.getStepX() * k, y, cz + d.getStepZ() * k, ModWorldBlocks.RUSTED_METAL_BEAM.get()
                        .defaultBlockState().setValue(RotatedPillarBlock.AXIS, d.getAxis()));
            }
        }
        // terminal, cofre del núcleo y dos torretas en esquinas opuestas
        set(level, box, cx - 3, y, cz - 3, Blocks.LECTERN.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        container(level, box, cx + 3, y, cz + 3, Blocks.CHEST.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), LOOT_CORE);
        spawn(level, box, ModEntities.SENTRY_TURRET.get(), x0 + 1, y, z0 + 9);
        spawn(level, box, ModEntities.SENTRY_TURRET.get(), x0 + 9, y, z0 + 1);
    }
}
