package com.bryanamaya.tecnocinetico.worldgen.structure;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.ModWorldBlocks;
import com.bryanamaya.tecnocinetico.block.custom.PileBlock;
import com.bryanamaya.tecnocinetico.entity.ModEntities;
import com.bryanamaya.tecnocinetico.entity.custom.SwarmBodyEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Pieza única de la fábrica abandonada. Igual que los edificios en ruinas: coordenadas locales (x 0..w-1,
 * z 0..d-1, y 0..h; y=0 = suelo) que {@link #put} pasa a absolutas con orientación nula, y aleatorios que
 * salen de `seed` y de la posición, nunca del mundo (cada chunk construye su parte de la misma fábrica).
 *
 * Distribución: muros a lo largo del borde (portón en z=0), pasarela con oficina pegada al muro del fondo
 * (z=d-1), maquinaria en el centro, grúa puente bajo el techo y una esquina derrumbada.
 */
public class FactoryPiece extends StructurePiece {

    private static final ResourceLocation LOOT = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "chests/abandoned_factory");
    private static final ResourceLocation LOOT_OFFICE = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "chests/factory_office");
    private static final int WALK_Y = 5;                     // altura del suelo de la pasarela

    private final int w, d, h;
    private final boolean overgrown;
    private final long seed;

    public FactoryPiece(BlockPos origin, int w, int d, int h, boolean overgrown, long seed) {
        super(ModStructures.FACTORY_PIECE.get(), 0,
                new BoundingBox(origin.getX() - 1, origin.getY(), origin.getZ() - 1,
                        origin.getX() + w, origin.getY() + h + 1, origin.getZ() + d));
        this.w = w;
        this.d = d;
        this.h = h;
        this.overgrown = overgrown;
        this.seed = seed;
        setOrientation(null);
    }

    public FactoryPiece(CompoundTag tag) {
        super(ModStructures.FACTORY_PIECE.get(), tag);
        this.w = tag.getInt("W");
        this.d = tag.getInt("D");
        this.h = tag.getInt("H");
        this.overgrown = tag.getBoolean("Overgrown");
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("W", w);
        tag.putInt("D", d);
        tag.putInt("H", h);
        tag.putBoolean("Overgrown", overgrown);
        tag.putLong("Seed", seed);
    }

    // ------------------------------------------------------------------ utilidades

    private int ox() { return boundingBox.minX() + 1; }
    private int oy() { return boundingBox.minY(); }
    private int oz() { return boundingBox.minZ() + 1; }

    private void put(WorldGenLevel level, BlockState s, int x, int y, int z, BoundingBox box) {
        placeBlock(level, s, ox() + x, oy() + y, oz() + z, box);
    }

    private BlockState get(WorldGenLevel level, int x, int y, int z, BoundingBox box) {
        return getBlock(level, ox() + x, oy() + y, oz() + z, box);
    }

    private RandomSource at(int x, int y, int z, int salt) {
        return RandomSource.create(seed * 31L + salt + (x * 73856093L ^ y * 19349663L ^ z * 83492791L));
    }

    private void container(WorldGenLevel level, BlockState s, int x, int y, int z, BoundingBox box, ResourceLocation loot) {
        BlockPos p = new BlockPos(ox() + x, oy() + y, oz() + z);
        if (box.isInside(p)) {
            level.setBlock(p, s, 2);
            RandomizableContainerBlockEntity.setLootTable(level, at(x, y, z, 9), p, loot);
        }
    }

    private static BlockState beam(Direction.Axis axis) {
        return ModWorldBlocks.RUSTED_METAL_BEAM.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    private static BlockState sheet() {
        return ModWorldBlocks.RUSTED_METAL_SHEET.get().defaultBlockState();
    }

    /** Altura que conserva cada columna: la esquina derrumbada pierde muro y techo. */
    private int keep(int x, int z, int cornerX, int cornerZ, int radius) {
        double dist = Math.hypot(x - cornerX, z - cornerZ);
        if (dist >= radius) {
            return h;
        }
        return Math.max(1, (int) (h * (dist / radius)) - at(x, 0, z, 4).nextInt(2));
    }

    // ------------------------------------------------------------------ construcción

    @Override
    public void postProcess(WorldGenLevel level, StructureManager sm, ChunkGenerator gen, RandomSource ignored,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        RandomSource r = RandomSource.create(seed);
        int cornerX = r.nextBoolean() ? 0 : w - 1, cornerZ = r.nextBoolean() ? 0 : d - 1;
        int collapse = 4 + r.nextInt(4);
        int doorX = w / 2;
        int craneZ = 3 + r.nextInt(Math.max(1, d - 9));

        shell(level, box, cornerX, cornerZ, collapse, doorX);
        machines(level, box, r);
        catwalk(level, box);
        crane(level, box, craneZ, cornerX, cornerZ, collapse);
        debris(level, box, cornerX, cornerZ, collapse);
        loot(level, box, r);
        guards(level, box, r);
    }

    /** Suelo, muros (hormigón abajo, chapa arriba, franja de ventanas), pilares de viga, techo con agujeros. */
    private void shell(WorldGenLevel level, BoundingBox box, int cornerX, int cornerZ, int collapse, int doorX) {
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                int k = keep(x, z, cornerX, cornerZ, collapse);
                boolean edge = x == 0 || z == 0 || x == w - 1 || z == d - 1;
                boolean corner = (x == 0 || x == w - 1) && (z == 0 || z == d - 1);
                boolean pillar = corner || (edge && (z == 0 || z == d - 1) && x % 5 == 0)
                        || (edge && (x == 0 || x == w - 1) && z % 5 == 0);
                RandomSource cr = at(x, 0, z, 1);
                fillColumnDown(level, ModWorldBlocks.CONCRETE.get().defaultBlockState(), ox() + x, oy() - 1, oz() + z, box);
                // suelo: hormigón con manchas de aceite (andesita pulida) y alguna grieta
                float f = cr.nextFloat();
                put(level, f < 0.12F ? Blocks.POLISHED_ANDESITE.defaultBlockState()
                        : f < 0.4F ? ModWorldBlocks.CRACKED_CONCRETE.get().defaultBlockState()
                        : overgrown && f < 0.5F ? Blocks.MOSS_BLOCK.defaultBlockState()
                        : ModWorldBlocks.CONCRETE.get().defaultBlockState(), x, 0, z, box);
                for (int y = 1; y <= h + 1; y++) {
                    BlockState s = Blocks.AIR.defaultBlockState();
                    if (y > k) {
                        s = Blocks.AIR.defaultBlockState();
                    } else if (y == h) {
                        // techo: chapa con agujeros y alguna claraboya de vidrio
                        RandomSource rr = at(x, y, z, 2);
                        boolean hole = rr.nextFloat() < 0.14F;
                        s = edge ? sheet() : hole ? Blocks.AIR.defaultBlockState()
                                : (x % 6 == 3 && !edge) ? ModWorldBlocks.VITRIFIED_GLASS.get().defaultBlockState() : sheet();
                    } else if (y < h && edge) {
                        boolean door = z == 0 && Math.abs(x - doorX) <= 2 && y <= 4;
                        boolean backDoor = z == d - 1 && x == 2 && y <= 2;
                        RandomSource wr = at(x, y, z, 3);
                        if (pillar) {
                            s = beam(Direction.Axis.Y);
                        } else if (door) {
                            s = y == 4 ? ModWorldBlocks.RUSTED_GRATE.get().defaultBlockState() : Blocks.AIR.defaultBlockState();
                        } else if (backDoor) {
                            s = Blocks.AIR.defaultBlockState();
                        } else if (y <= 3) {
                            s = wr.nextFloat() < 0.35F ? ModWorldBlocks.CRACKED_CONCRETE.get().defaultBlockState()
                                    : ModWorldBlocks.CONCRETE.get().defaultBlockState();
                        } else if (y == 6 || y == 7) {
                            float v = wr.nextFloat();               // ventanas rotas
                            s = v < 0.55F ? Blocks.AIR.defaultBlockState()
                                    : v < 0.85F ? ModWorldBlocks.RUSTED_GRATE.get().defaultBlockState()
                                    : ModWorldBlocks.VITRIFIED_GLASS.get().defaultBlockState();
                        } else {
                            s = wr.nextFloat() < 0.04F ? Blocks.AIR.defaultBlockState() : sheet();
                        }
                    } else if (y == h - 1 && x % 5 == 0 && !edge) {
                        s = beam(Direction.Axis.Z);                 // cerchas del techo
                    }
                    put(level, s, x, y, z, box);
                }
            }
        }
        // enredaderas en las fachadas (variante invadida)
        if (overgrown) {
            for (int x = 0; x < w; x++) {
                for (int y = 1; y < h; y++) {
                    for (int side = 0; side < 2; side++) {
                        int vz = side == 0 ? -1 : d;
                        Direction out = side == 0 ? Direction.NORTH : Direction.SOUTH;
                        if (at(x, y, vz, 5).nextFloat() < 0.3F && get(level, x, y, side == 0 ? 0 : d - 1, box).isSolid()) {
                            put(level, Blocks.VINE.defaultBlockState()
                                    .setValue(VineBlock.getPropertyForFace(out.getOpposite()), true), x, y, vz, box);
                        }
                    }
                }
            }
        }
    }

    /** Maquinaria en filas por el centro de la nave: prensas, hornos con chimenea, tanques y una cinta. */
    private void machines(WorldGenLevel level, BoundingBox box, RandomSource r) {
        int beltZ = 3;
        // cinta transportadora a lo largo de la nave (con algo de chatarra encima)
        for (int x = 3; x < w - 3; x++) {
            put(level, Blocks.POLISHED_ANDESITE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM), x, 1, beltZ, box);
            if (x % 4 == 0) {
                put(level, beam(Direction.Axis.Y), x, 1, beltZ + 1, box);
            }
            if (at(x, 2, beltZ, 6).nextFloat() < 0.15F) {
                put(level, Blocks.CHAIN.defaultBlockState(), x, h - 1, beltZ, box);
                put(level, Blocks.CHAIN.defaultBlockState(), x, h - 2, beltZ, box);
            }
        }
        // máquinas cada 6 bloques en la fila central
        int mz = d / 2 - 1;
        int type = r.nextInt(3);
        for (int x = 3; x + 3 < w - 2; x += 6) {
            switch ((type++) % 3) {
                case 0 -> press(level, box, x, mz);
                case 1 -> furnaces(level, box, x, mz);
                default -> tank(level, box, x, mz);
            }
        }
    }

    private void press(WorldGenLevel level, BoundingBox box, int x, int z) {
        for (int dx = 0; dx < 3; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                put(level, Blocks.POLISHED_ANDESITE.defaultBlockState(), x + dx, 1, z + dz, box);
                boolean corner = dx != 1 && dz != 1;
                if (corner) {
                    for (int y = 2; y <= 4; y++) {
                        put(level, beam(Direction.Axis.Y), x + dx, y, z + dz, box);
                    }
                }
                put(level, sheet(), x + dx, 5, z + dz, box);
            }
        }
        put(level, Blocks.CHAIN.defaultBlockState(), x + 1, 4, z + 1, box);
        put(level, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE.defaultBlockState(), x + 1, 2, z + 1, box);
        put(level, ModWorldBlocks.RUSTED_GRATE.get().defaultBlockState(), x + 1, 3, z + 1, box);
    }

    private void furnaces(WorldGenLevel level, BoundingBox box, int x, int z) {
        for (int dx = 0; dx < 3; dx++) {
            put(level, Blocks.BLAST_FURNACE.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH),
                    x + dx, 1, z, box);
            put(level, Blocks.SMOOTH_STONE.defaultBlockState(), x + dx, 1, z + 1, box);
            put(level, Blocks.HOPPER.defaultBlockState(), x + dx, 2, z, box);
        }
        for (int y = 2; y < h; y++) {                                   // chimenea hasta el techo
            put(level, beam(Direction.Axis.Y), x + 1, y, z + 1, box);
        }
        put(level, Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false),
                x + 1, h + 1, z + 1, box);
    }

    private void tank(WorldGenLevel level, BoundingBox box, int x, int z) {
        for (int dx = 0; dx < 3; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                boolean corner = dx != 1 && dz != 1;
                for (int y = 1; y <= 3; y++) {
                    if (!corner) {
                        put(level, y == 3 ? Blocks.WEATHERED_CUT_COPPER.defaultBlockState()
                                : Blocks.WEATHERED_COPPER.defaultBlockState(), x + dx, y, z + dz, box);
                    }
                }
            }
        }
        put(level, ModWorldBlocks.RUSTED_GRATE.get().defaultBlockState(), x + 1, 4, z + 1, box);
        // tubería hacia el muro del fondo
        for (int dz = 3; dz < d - 5 - z && dz < 6; dz++) {
            put(level, Blocks.EXPOSED_CUT_COPPER_SLAB.defaultBlockState(), x + 1, 3, z + dz, box);
        }
    }

    /** Pasarela de rejilla pegada al muro del fondo, barandilla, escalera y oficina en un extremo. */
    private void catwalk(WorldGenLevel level, BoundingBox box) {
        for (int x = 1; x < w - 1; x++) {
            for (int z = d - 3; z <= d - 2; z++) {
                if (x == 1 && z == d - 3) {
                    continue;                                            // hueco de la escalera
                }
                put(level, ModWorldBlocks.RUSTED_GRATE.get().defaultBlockState(), x, WALK_Y, z, box);
            }
            if (x > 1) {
                put(level, Blocks.IRON_BARS.defaultBlockState(), x, WALK_Y + 1, d - 4, box);
            }
            if (x % 5 == 2) {
                for (int y = 1; y < WALK_Y; y++) {
                    put(level, beam(Direction.Axis.Y), x, y, d - 3, box);    // soportes
                }
            }
        }
        for (int y = 1; y <= WALK_Y; y++) {
            put(level, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST), 1, y, d - 3, box);
        }
        // oficina: caja de chapa con ventanas de rejilla, puerta hacia la pasarela, cofre y escritorio
        int x0 = w - 7;
        for (int x = x0; x <= w - 2; x++) {
            for (int z = d - 6; z <= d - 2; z++) {
                boolean wall = x == x0 || x == w - 2 || z == d - 6 || z == d - 2;
                put(level, ModWorldBlocks.DEAD_PLANKS.get().defaultBlockState(), x, WALK_Y, z, box);
                for (int y = WALK_Y + 1; y <= WALK_Y + 3; y++) {
                    BlockState s = Blocks.AIR.defaultBlockState();
                    if (wall) {
                        s = y == WALK_Y + 2 && (x + z) % 2 == 0 ? ModWorldBlocks.RUSTED_GRATE.get().defaultBlockState() : sheet();
                    }
                    put(level, s, x, y, z, box);
                }
                put(level, sheet(), x, WALK_Y + 4, z, box);
            }
            for (int y = 1; y < WALK_Y; y++) {
                if (x == x0 || x == w - 2) {
                    put(level, beam(Direction.Axis.Y), x, y, d - 6, box);    // patas de la oficina
                }
            }
        }
        put(level, Blocks.AIR.defaultBlockState(), x0, WALK_Y + 1, d - 3, box);   // puerta (hueco)
        put(level, Blocks.AIR.defaultBlockState(), x0, WALK_Y + 2, d - 3, box);
        put(level, Blocks.CRAFTING_TABLE.defaultBlockState(), w - 3, WALK_Y + 1, d - 5, box);
        put(level, Blocks.LECTERN.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.WEST),
                w - 3, WALK_Y + 1, d - 4, box);
        container(level, Blocks.CHEST.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.WEST),
                w - 3, WALK_Y + 1, d - 3, box, LOOT_OFFICE);
    }

    /** Grúa puente: viga de lado a lado bajo el techo con cadena y gancho colgando. */
    private void crane(WorldGenLevel level, BoundingBox box, int z, int cornerX, int cornerZ, int collapse) {
        for (int x = 1; x < w - 1; x++) {
            if (keep(x, z, cornerX, cornerZ, collapse) >= h) {
                put(level, beam(Direction.Axis.X), x, h - 2, z, box);
            }
        }
        int hx = w / 3;
        for (int y = h - 3; y >= h - 6; y--) {
            put(level, Blocks.CHAIN.defaultBlockState(), hx, y, z, box);
        }
        put(level, Blocks.GRINDSTONE.defaultBlockState(), hx, h - 7, z, box);
    }

    /** Escombros del derrumbe, montones de chatarra, pedazos y (invadida) musgo. */
    private void debris(WorldGenLevel level, BoundingBox box, int cornerX, int cornerZ, int collapse) {
        for (int x = 1; x < w - 1; x++) {
            for (int z = 1; z < d - 1; z++) {
                if (!get(level, x, 1, z, box).isAir() || !get(level, x, 0, z, box).isSolid()) {
                    continue;
                }
                RandomSource pr = at(x, 1, z, 7);
                float v = pr.nextFloat();
                boolean nearCollapse = Math.hypot(x - cornerX, z - cornerZ) < collapse + 2;
                if (v < (nearCollapse ? 0.35F : 0.05F)) {
                    put(level, (pr.nextFloat() < 0.75F ? ModWorldBlocks.RUBBLE : ModWorldBlocks.SCRAP_PILE).get()
                            .defaultBlockState().setValue(PileBlock.PIECES, 1 + pr.nextInt(4)), x, 1, z, box);
                } else if (v < (nearCollapse ? 0.5F : 0.1F)) {
                    put(level, ModWorldBlocks.DEBRIS_LAYER.get().defaultBlockState(), x, 1, z, box);
                } else if (overgrown && v < 0.2F) {
                    put(level, Blocks.MOSS_CARPET.defaultBlockState(), x, 1, z, box);
                }
            }
        }
    }

    /** Cofres y barriles repartidos por la planta (posiciones fijas por semilla). */
    private void loot(WorldGenLevel level, BoundingBox box, RandomSource r) {
        int chests = 2 + r.nextInt(2);
        for (int i = 0; i < chests; i++) {
            int x = 2 + r.nextInt(Math.max(1, w - 4)), z = 1 + r.nextInt(2);
            boolean barrel = r.nextBoolean();
            if (get(level, x, 1, z, box).isAir()) {
                container(level, barrel ? Blocks.BARREL.defaultBlockState()
                                : Blocks.CHEST.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH),
                        x, 1, z, box, LOOT);
            }
        }
    }

    /** 1-3 Cuerpos de Enjambre vigilando la nave (no desaparecen). */
    private void guards(WorldGenLevel level, BoundingBox box, RandomSource r) {
        int n = 1 + r.nextInt(3);
        for (int i = 0; i < n; i++) {
            int x = 3 + r.nextInt(Math.max(1, w - 6)), z = 2 + r.nextInt(Math.max(1, d - 7));
            float yaw = r.nextFloat() * 360.0F;                        // antes de mirar el chunk (determinista)
            BlockPos p = new BlockPos(ox() + x, oy() + 1, oz() + z);
            if (!box.isInside(p)) {
                continue;
            }
            SwarmBodyEntity e = ModEntities.SWARM_BODY.get().create(level.getLevel());
            if (e == null) {
                continue;
            }
            e.moveTo(p.getX() + 0.5D, p.getY(), p.getZ() + 0.5D, yaw, 0.0F);
            e.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null, null);
            e.setPersistenceRequired();
            level.addFreshEntityWithPassengers(e);
        }
    }
}
