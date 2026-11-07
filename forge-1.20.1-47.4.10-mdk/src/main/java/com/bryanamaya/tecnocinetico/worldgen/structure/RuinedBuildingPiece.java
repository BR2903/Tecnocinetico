package com.bryanamaya.tecnocinetico.worldgen.structure;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.ModWorldBlocks;
import com.bryanamaya.tecnocinetico.block.custom.PileBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Pieza única del edificio en ruinas. Todo se calcula a partir de `seed`: postProcess se llama una vez por
 * cada chunk que atraviesa el edificio (con su recorte `box`) y cada chunk debe "ver" el mismo edificio,
 * así que los aleatorios NUNCA dependen de lo que haya en el mundo.
 *
 * Coordenadas locales: x 0..w-1, z 0..d-1, y 0..floors*4 (y=0 = losa del suelo). {@link #put} las pasa a
 * absolutas; la orientación es nula (sin espejo ni rotación) para que enredaderas y vigas queden bien.
 */
public class RuinedBuildingPiece extends StructurePiece {

    private static final int FLOOR_H = 4;
    private static final ResourceLocation LOOT = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "chests/ruined_building");

    private final int w, d, floors;
    private final RuinedBuildingStructure.Style style;
    private final long seed;

    public RuinedBuildingPiece(BlockPos origin, int w, int d, int floors, RuinedBuildingStructure.Style style, long seed) {
        super(ModStructures.RUINED_BUILDING_PIECE.get(), 0,
                new BoundingBox(origin.getX() - 1, origin.getY(), origin.getZ() - 1,
                        origin.getX() + w, origin.getY() + floors * FLOOR_H + 2, origin.getZ() + d));
        this.w = w;
        this.d = d;
        this.floors = floors;
        this.style = style;
        this.seed = seed;
        setOrientation(null);
    }

    public RuinedBuildingPiece(CompoundTag tag) {
        super(ModStructures.RUINED_BUILDING_PIECE.get(), tag);
        this.w = tag.getInt("W");
        this.d = tag.getInt("D");
        this.floors = tag.getInt("Floors");
        this.style = RuinedBuildingStructure.Style.values()[tag.getInt("Style")];
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("W", w);
        tag.putInt("D", d);
        tag.putInt("Floors", floors);
        tag.putInt("Style", style.ordinal());
        tag.putLong("Seed", seed);
    }

    // ------------------------------------------------------------------ coordenadas y aleatorios

    private int ox() { return boundingBox.minX() + 1; }
    private int oy() { return boundingBox.minY(); }
    private int oz() { return boundingBox.minZ() + 1; }

    private void put(WorldGenLevel level, BlockState s, int x, int y, int z, BoundingBox box) {
        placeBlock(level, s, ox() + x, oy() + y, oz() + z, box);
    }

    private BlockState get(WorldGenLevel level, int x, int y, int z, BoundingBox box) {
        return getBlock(level, ox() + x, oy() + y, oz() + z, box);
    }

    /** Aleatorio fijo para una posición (no depende del orden ni del chunk). */
    private RandomSource at(int x, int y, int z, int salt) {
        return RandomSource.create(seed * 31L + salt + (x * 73856093L ^ y * 19349663L ^ z * 83492791L));
    }

    // ------------------------------------------------------------------ materiales por estilo

    private BlockState wall(RandomSource r) {
        float v = r.nextFloat();
        return switch (style) {
            case OVERGROWN -> v < 0.5F ? ModWorldBlocks.MOSSY_CONCRETE.get().defaultBlockState()
                    : v < 0.8F ? ModWorldBlocks.CRACKED_CONCRETE.get().defaultBlockState()
                    : ModWorldBlocks.CONCRETE.get().defaultBlockState();
            case FROZEN -> v < 0.55F ? ModWorldBlocks.CRACKED_CONCRETE.get().defaultBlockState()
                    : ModWorldBlocks.CONCRETE.get().defaultBlockState();
            default -> v < 0.45F ? ModWorldBlocks.CONCRETE.get().defaultBlockState()
                    : v < 0.9F ? ModWorldBlocks.CRACKED_CONCRETE.get().defaultBlockState()
                    : ModWorldBlocks.MOSSY_CONCRETE.get().defaultBlockState();
        };
    }

    private static BlockState pile(RandomSource r) {
        boolean scrap = r.nextFloat() < 0.2F;
        int pieces = 1 + r.nextInt(r.nextFloat() < 0.3F ? 4 : 2);
        return (scrap ? ModWorldBlocks.SCRAP_PILE : ModWorldBlocks.RUBBLE).get().defaultBlockState()
                .setValue(PileBlock.PIECES, pieces);
    }

    // ------------------------------------------------------------------ construcción

    @Override
    public void postProcess(WorldGenLevel level, StructureManager sm, ChunkGenerator gen, RandomSource ignored,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        RandomSource r = RandomSource.create(seed);
        int top = floors * FLOOR_H;

        // altura conservada de cada columna: plano de derrumbe inclinado desde una esquina intacta + irregularidad
        int cornerX = r.nextBoolean() ? 0 : w - 1, cornerZ = r.nextBoolean() ? 0 : d - 1;
        // fracción de altura que se pierde en la esquina opuesta (60-100 %): la intacta conserva toda la altura
        double loss = 0.6 + r.nextDouble() * 0.4;
        int[][] keep = new int[w][d];
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                double dist = (Math.abs(x - cornerX) + Math.abs(z - cornerZ)) / (double) (w + d - 2);
                keep[x][z] = Math.max(1, (int) Math.round(top + 1 - dist * loss * top - r.nextInt(2)));
            }
        }
        // agujero grande del derrumbe en los forjados
        int holeX = 1 + r.nextInt(Math.max(1, w - 2)), holeZ = 1 + r.nextInt(Math.max(1, d - 2));
        int holeR = 1 + r.nextInt(3);
        int doorX = w / 2;

        // ---- estructura (todas las decisiones salen de at(...), independientes del mundo)
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                boolean edge = x == 0 || z == 0 || x == w - 1 || z == d - 1;
                boolean corner = (x == 0 || x == w - 1) && (z == 0 || z == d - 1);
                RandomSource cr = at(x, 0, z, 0);
                fillColumnDown(level, wall(cr), ox() + x, oy() - 1, oz() + z, box);   // cimiento hasta el terreno
                put(level, wall(cr), x, 0, z, box);                                  // losa de planta baja
                for (int y = 1; y <= top + 2; y++) {
                    RandomSource br = at(x, y, z, 0);
                    BlockState s;
                    if (y > keep[x][z]) {
                        s = Blocks.AIR.defaultBlockState();
                    } else if (edge) {
                        int inFloor = y % FLOOR_H;
                        boolean window = !corner && (inFloor == 2 || inFloor == 3)
                                && ((x > 0 && x < w - 1 ? x : z) % 3 == 1);
                        boolean door = z == 0 && x == doorX && (y == 1 || y == 2);
                        if (door) {
                            s = Blocks.AIR.defaultBlockState();
                        } else if (window) {
                            s = br.nextFloat() < 0.12F ? ModWorldBlocks.RUSTED_GRATE.get().defaultBlockState()
                                    : Blocks.AIR.defaultBlockState();
                        } else if (corner && br.nextFloat() < 0.5F) {
                            s = ModWorldBlocks.RUSTED_METAL_BEAM.get().defaultBlockState();
                        } else if (br.nextFloat() < 0.06F) {
                            s = Blocks.AIR.defaultBlockState();                      // boquete
                        } else {
                            s = wall(br);
                        }
                    } else if (y % FLOOR_H == 0 && y <= top) {
                        double dh = Math.hypot(x - holeX, z - holeZ);
                        s = (dh <= holeR || br.nextFloat() < 0.15F) ? Blocks.AIR.defaultBlockState() : wall(br);
                    } else {
                        s = Blocks.AIR.defaultBlockState();
                    }
                    put(level, s, x, y, z, box);
                }
            }
        }

        // ---- escombros, musgo y cofres (posiciones de cofre decididas antes de mirar el mundo)
        int[][] chestAt = new int[floors][];
        int chests = 1 + (floors > 2 ? 1 : 0);
        for (int f = 0; f < floors && chests > 0; f++) {
            if (f == 0 || r.nextFloat() < 0.5F) {
                chestAt[f] = new int[]{1 + r.nextInt(Math.max(1, w - 2)), 1 + r.nextInt(Math.max(1, d - 2))};
                chests--;
            }
        }
        for (int f = 0; f < floors; f++) {
            int y = f * FLOOR_H + 1;
            int[] chest = chestAt[f];
            for (int x = 1; x < w - 1; x++) {
                for (int z = 1; z < d - 1; z++) {
                    if (y > keep[x][z] || !solidBelow(level, x, y, z, box)) {
                        continue;
                    }
                    if (chest != null && chest[0] == x && chest[1] == z) {
                        createChest(level, box, at(x, y, z, 2), ox() + x, oy() + y, oz() + z, LOOT);
                        continue;
                    }
                    RandomSource pr = at(x, y, z, 1);
                    float v = pr.nextFloat();
                    if (v < 0.10F) {
                        put(level, pile(pr), x, y, z, box);
                    } else if (v < 0.16F) {
                        put(level, ModWorldBlocks.DEBRIS_LAYER.get().defaultBlockState(), x, y, z, box);
                    } else if (v < 0.22F && style == RuinedBuildingStructure.Style.OVERGROWN) {
                        put(level, Blocks.MOSS_CARPET.defaultBlockState(), x, y, z, box);
                    } else if (v < 0.25F && style == RuinedBuildingStructure.Style.FROZEN) {
                        put(level, Blocks.SNOW.defaultBlockState(), x, y, z, box);
                    }
                }
            }
        }

        // ---- vigas oxidadas que asoman por arriba
        for (int k = 0; k < 2 + floors; k++) {
            int x = r.nextInt(w), z = r.nextInt(d);
            boolean vertical = r.nextBoolean();
            if (x == 0 || z == 0 || x == w - 1 || z == d - 1) {
                Direction.Axis axis = vertical ? Direction.Axis.Y : (x == 0 || x == w - 1) ? Direction.Axis.Z : Direction.Axis.X;
                put(level, ModWorldBlocks.RUSTED_METAL_BEAM.get().defaultBlockState()
                        .setValue(RotatedPillarBlock.AXIS, axis), x, keep[x][z] + 1, z, box);
            }
        }

        // ---- enredaderas en las fachadas norte y sur (estilo invadido)
        if (style == RuinedBuildingStructure.Style.OVERGROWN) {
            for (int x = 0; x < w; x++) {
                for (int y = 1; y <= top; y++) {
                    for (int side = 0; side < 2; side++) {
                        int wz = side == 0 ? 0 : d - 1;
                        int vz = side == 0 ? -1 : d;
                        Direction out = side == 0 ? Direction.NORTH : Direction.SOUTH;
                        if (y <= keep[x][wz] && at(x, y, vz, 3).nextFloat() < 0.3F
                                && get(level, x, y, vz, box).isAir() && !get(level, x, y, wz, box).isAir()) {
                            put(level, Blocks.VINE.defaultBlockState()
                                    .setValue(VineBlock.getPropertyForFace(out.getOpposite()), true), x, y, vz, box);
                        }
                    }
                }
            }
        }
    }

    private boolean solidBelow(WorldGenLevel level, int x, int y, int z, BoundingBox box) {
        BlockState below = get(level, x, y - 1, z, box);
        return !below.isAir() && below.isSolid() && get(level, x, y, z, box).isAir();
    }
}
