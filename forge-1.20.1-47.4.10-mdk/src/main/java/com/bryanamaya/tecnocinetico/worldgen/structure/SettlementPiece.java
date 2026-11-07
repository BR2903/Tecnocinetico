package com.bryanamaya.tecnocinetico.worldgen.structure;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.ModWorldBlocks;
import com.bryanamaya.tecnocinetico.block.custom.PileBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BeetrootBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Pieza única del Asentamiento de Supervivientes. Coordenadas locales lx/lz 0..SIZE-1 (columna) con la
 * altura del terreno guardada en `heights`; todas las decisiones salen de `seed` (nunca del mundo), así cada
 * chunk construye su parte del mismo asentamiento. Orientación nula: coordenadas absolutas, sin espejo.
 *
 * Distribución: muralla en el borde con puerta en un lado; plaza en el centro; 8 parcelas alrededor (la del
 * lado de la puerta queda para el camino) donde van casas, taller, almacén y huertos; torre en una esquina.
 */
public class SettlementPiece extends StructurePiece {

    private static final int S = SettlementStructure.SIZE, C = S / 2;
    private static final ResourceLocation LOOT_HOME = loot("settlement_home");
    private static final ResourceLocation LOOT_WORKSHOP = loot("settlement_workshop");
    private static final ResourceLocation LOOT_STORAGE = loot("settlement_storage");

    private enum Kind { HOME, WORKSHOP, STORAGE, FARM }

    private record Plot(int cx, int cz, Kind kind, int w, int d, Direction door) {}

    private final int x0, z0;
    private final int[] heights;
    private final long seed;

    public SettlementPiece(int x0, int z0, int minY, int maxY, int[] heights, long seed) {
        super(ModStructures.SETTLEMENT_PIECE.get(), 0,
                new BoundingBox(x0, minY - 6, z0, x0 + S - 1, maxY + 14, z0 + S - 1));
        this.x0 = x0;
        this.z0 = z0;
        this.heights = heights;
        this.seed = seed;
        setOrientation(null);
    }

    public SettlementPiece(CompoundTag tag) {
        super(ModStructures.SETTLEMENT_PIECE.get(), tag);
        this.x0 = tag.getInt("X0");
        this.z0 = tag.getInt("Z0");
        this.heights = tag.getIntArray("H");
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("X0", x0);
        tag.putInt("Z0", z0);
        tag.putIntArray("H", heights);
        tag.putLong("Seed", seed);
    }

    private static ResourceLocation loot(String name) {
        return ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "chests/" + name);
    }

    // ------------------------------------------------------------------ utilidades

    private int h(int lx, int lz) {
        lx = Math.max(0, Math.min(S - 1, lx));
        lz = Math.max(0, Math.min(S - 1, lz));
        return heights[lx * S + lz];
    }

    private void put(WorldGenLevel level, BlockState s, int lx, int y, int lz, BoundingBox box) {
        placeBlock(level, s, x0 + lx, y, z0 + lz, box);
    }

    private BlockState get(WorldGenLevel level, int lx, int y, int lz, BoundingBox box) {
        return getBlock(level, x0 + lx, y, z0 + lz, box);
    }

    private RandomSource at(int lx, int y, int lz, int salt) {
        return RandomSource.create(seed * 31L + salt + (lx * 73856093L ^ y * 19349663L ^ lz * 83492791L));
    }

    private void container(WorldGenLevel level, BlockState s, int lx, int y, int lz, BoundingBox box, ResourceLocation loot) {
        BlockPos p = new BlockPos(x0 + lx, y, z0 + lz);
        if (box.isInside(p)) {
            level.setBlock(p, s, 2);
            RandomizableContainerBlockEntity.setLootTable(level, at(lx, y, lz, 9), p, loot);
        }
    }

    /** Rellena por debajo hasta el terreno (cimientos) y despeja por encima hasta `clearTo`. */
    private void column(WorldGenLevel level, int lx, int lz, int floorY, int clearTo, BlockState fill, BoundingBox box) {
        for (int y = h(lx, lz) + 1; y < floorY; y++) {
            put(level, fill, lx, y, lz, box);
        }
        for (int y = floorY + 1; y <= clearTo; y++) {
            put(level, Blocks.AIR.defaultBlockState(), lx, y, lz, box);
        }
    }

    private static BlockState log(Direction.Axis axis) {
        return ModWorldBlocks.DEAD_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    // ------------------------------------------------------------------ construcción

    @Override
    public void postProcess(WorldGenLevel level, StructureManager sm, ChunkGenerator gen, RandomSource ignored,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        RandomSource r = RandomSource.create(seed);
        Direction gate = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        int towerSx = r.nextBoolean() ? 1 : -1, towerSz = r.nextBoolean() ? 1 : -1;
        List<Plot> plots = layout(r, gate);

        clearVegetation(level, box);
        ground(level, box, gate, plots);
        wall(level, box, gate);
        for (Plot p : plots) {
            if (p.kind() == Kind.FARM) {
                farm(level, box, p);
            } else {
                hut(level, box, p);
            }
        }
        tower(level, box, C + towerSx * 13, C + towerSz * 13);
        plaza(level, box);
    }

    /** Parcelas alrededor de la plaza; la que está en el lado de la puerta queda libre para el camino. */
    private List<Plot> layout(RandomSource r, Direction gate) {
        List<int[]> slots = new ArrayList<>(List.of(new int[]{-8, -8}, new int[]{8, -8}, new int[]{-8, 8}, new int[]{8, 8},
                new int[]{0, -8}, new int[]{0, 8}, new int[]{-8, 0}, new int[]{8, 0}));
        slots.removeIf(s -> s[0] == gate.getStepX() * 8 && s[1] == gate.getStepZ() * 8);
        for (int i = slots.size() - 1; i > 0; i--) {                       // barajar
            int j = r.nextInt(i + 1);
            int[] t = slots.get(i);
            slots.set(i, slots.get(j));
            slots.set(j, t);
        }
        Kind[] order = {Kind.HOME, Kind.FARM, Kind.WORKSHOP, Kind.HOME, Kind.STORAGE, Kind.FARM, Kind.HOME};
        int count = 5 + r.nextInt(3);
        List<Plot> plots = new ArrayList<>();
        for (int i = 0; i < count && i < slots.size(); i++) {
            int[] s = slots.get(i);
            Kind k = order[i];
            int w = k == Kind.FARM ? 7 : 5 + r.nextInt(2) * 2;            // 5 o 7
            int d = k == Kind.FARM ? 5 : 5;
            // la puerta mira a la plaza
            Direction door = Math.abs(s[0]) >= Math.abs(s[1])
                    ? (s[0] > 0 ? Direction.WEST : Direction.EAST)
                    : (s[1] > 0 ? Direction.NORTH : Direction.SOUTH);
            plots.add(new Plot(C + s[0], C + s[1], k, w, d, door));
        }
        return plots;
    }

    /** Quita árboles y plantas dentro del recinto (no consume aleatorios: solo lee el mundo). */
    private void clearVegetation(WorldGenLevel level, BoundingBox box) {
        for (int lx = 1; lx < S - 1; lx++) {
            for (int lz = 1; lz < S - 1; lz++) {
                for (int y = h(lx, lz) + 1; y <= h(lx, lz) + 16; y++) {
                    BlockState s = get(level, lx, y, lz, box);
                    if (s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES) || s.is(Blocks.CACTUS) || s.is(Blocks.BAMBOO)
                            || s.is(Blocks.SUGAR_CANE) || (!s.isAir() && s.canBeReplaced()
                            && s.getFluidState().isEmpty())) {
                        put(level, Blocks.AIR.defaultBlockState(), lx, y, lz, box);
                    }
                }
            }
        }
    }

    /** Caminos de tierra: de la puerta a la plaza y de la plaza a cada parcela; restos y barriles sueltos. */
    private void ground(WorldGenLevel level, BoundingBox box, Direction gate, List<Plot> plots) {
        path(level, box, C, C, C + gate.getStepX() * (C - 1), C + gate.getStepZ() * (C - 1), 2);
        for (Plot p : plots) {
            path(level, box, C, C, p.cx(), p.cz(), 1);
        }
        // cosas sueltas cerca de la muralla
        for (int i = 0; i < 14; i++) {
            RandomSource pr = at(i, 0, 0, 5);
            int lx = 2 + pr.nextInt(S - 4), lz = 2 + pr.nextInt(S - 4);
            boolean nearWall = lx <= 3 || lz <= 3 || lx >= S - 4 || lz >= S - 4;
            if (!nearWall) {
                continue;
            }
            int y = h(lx, lz) + 1;
            BlockState s = pr.nextFloat() < 0.4F
                    ? ModWorldBlocks.SCRAP_PILE.get().defaultBlockState().setValue(PileBlock.PIECES, 1 + pr.nextInt(3))
                    : pr.nextFloat() < 0.5F ? Blocks.HAY_BLOCK.defaultBlockState() : Blocks.BARREL.defaultBlockState();
            if (s.is(Blocks.BARREL)) {
                container(level, s, lx, y, lz, box, LOOT_STORAGE);
            } else {
                put(level, s, lx, y, lz, box);
            }
        }
    }

    private void path(WorldGenLevel level, BoundingBox box, int ax, int az, int bx, int bz, int half) {
        int x = ax, z = az;
        while (x != bx || z != bz) {                                       // en L: primero x, luego z
            boolean alongX = x != bx;
            if (alongX) {
                x += Integer.signum(bx - x);
            } else {
                z += Integer.signum(bz - z);
            }
            for (int o = -half + 1; o < half; o++) {                       // ancho perpendicular al avance
                int px = alongX ? x : x + o, pz = alongX ? z + o : z;
                RandomSource pr = at(px, 0, pz, 6);
                BlockState s = pr.nextFloat() < 0.75F ? Blocks.DIRT_PATH.defaultBlockState()
                        : pr.nextBoolean() ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
                put(level, s, px, h(px, pz), pz, box);
            }
        }
    }

    /** Muralla de chapa, tablones y hormigón con postes cada 4 bloques, puerta con faroles. */
    private void wall(WorldGenLevel level, BoundingBox box, Direction gate) {
        for (int lx = 0; lx < S; lx++) {
            for (int lz = 0; lz < S; lz++) {
                if (lx != 0 && lz != 0 && lx != S - 1 && lz != S - 1) {
                    continue;
                }
                int base = h(lx, lz);
                // puerta: hueco de 3 en el centro del lado elegido
                int along = gate.getAxis() == Direction.Axis.X ? lz : lx;
                boolean onGateSide = gate.getStepX() * (lx - C) == C || gate.getStepZ() * (lz - C) == C;
                if (onGateSide && Math.abs(along - C) <= 1) {
                    continue;
                }
                boolean gatePost = onGateSide && Math.abs(along - C) == 2;
                boolean post = gatePost || (lx % 4 == 0 && lz % 4 == 0) || ((lx == 0 || lx == S - 1) && lz % 4 == 0)
                        || ((lz == 0 || lz == S - 1) && lx % 4 == 0);
                RandomSource pr = at(lx, 0, lz, 7);
                fillColumnDown(level, ModWorldBlocks.RUSTED_METAL_SHEET.get().defaultBlockState(), x0 + lx, base, z0 + lz, box);
                if (post) {
                    for (int y = 1; y <= 4; y++) {
                        put(level, log(Direction.Axis.Y), lx, base + y, lz, box);
                    }
                    if (gatePost) {
                        put(level, Blocks.LANTERN.defaultBlockState(), lx, base + 5, lz, box);
                    }
                    continue;
                }
                int top = pr.nextFloat() < 0.08F ? 2 : 3;                    // tramos más bajos (remiendos)
                for (int y = 1; y <= top; y++) {
                    float v = pr.nextFloat();
                    BlockState s = v < 0.5F ? ModWorldBlocks.RUSTED_METAL_SHEET.get().defaultBlockState()
                            : v < 0.82F ? ModWorldBlocks.DEAD_PLANKS.get().defaultBlockState()
                            : ModWorldBlocks.CRACKED_CONCRETE.get().defaultBlockState();
                    put(level, s, lx, base + y, lz, box);
                }
            }
        }
    }

    /** Choza: plataforma con cimiento, postes de tronco seco, paredes de chapa/tablones, techo de chapa. */
    private void hut(WorldGenLevel level, BoundingBox box, Plot p) {
        int hw = p.w() / 2, hd = p.d() / 2;
        int fy = platformY(p.cx(), p.cz(), hw, hd);
        RandomSource r = at(p.cx(), fy, p.cz(), 11);
        for (int dx = -hw; dx <= hw; dx++) {
            for (int dz = -hd; dz <= hd; dz++) {
                int lx = p.cx() + dx, lz = p.cz() + dz;
                column(level, lx, lz, fy, fy + 5, Blocks.COBBLESTONE.defaultBlockState(), box);
                put(level, ModWorldBlocks.DEAD_PLANKS.get().defaultBlockState(), lx, fy, lz, box);
                boolean edgeX = Math.abs(dx) == hw, edgeZ = Math.abs(dz) == hd;
                for (int y = 1; y <= 3; y++) {
                    BlockState s = Blocks.AIR.defaultBlockState();
                    if (edgeX && edgeZ) {
                        s = log(Direction.Axis.Y);
                    } else if (edgeX || edgeZ) {
                        boolean window = y == 2 && (edgeX ? dz == 0 : dx == 0);
                        float v = at(lx, fy + y, lz, 12).nextFloat();
                        s = window ? ModWorldBlocks.RUSTED_GRATE.get().defaultBlockState()
                                : v < 0.55F ? ModWorldBlocks.RUSTED_METAL_SHEET.get().defaultBlockState()
                                : ModWorldBlocks.DEAD_PLANKS.get().defaultBlockState();
                    }
                    put(level, s, lx, fy + y, lz, box);
                }
                put(level, ModWorldBlocks.RUSTED_METAL_SHEET.get().defaultBlockState(), lx, fy + 4, lz, box);
            }
        }
        // puerta en la pared que mira a la plaza
        Direction out = p.door();
        int dlx = p.cx() + out.getStepX() * hw, dlz = p.cz() + out.getStepZ() * hd;
        BlockState door = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, out.getOpposite())
                .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        put(level, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), dlx, fy + 1, dlz, box);
        put(level, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), dlx, fy + 2, dlz, box);
        // farol colgando del techo
        put(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), p.cx(), fy + 3, p.cz(), box);

        // muebles contra la pared del fondo (la opuesta a la puerta)
        Direction back = out.getOpposite();
        Direction side = back.getClockWise();
        int bx = p.cx() + back.getStepX() * (hw - 1), bz = p.cz() + back.getStepZ() * (hd - 1);
        int y = fy + 1;
        switch (p.kind()) {
            case HOME -> {
                // cama: los pies hacia la puerta, la cabecera contra la pared del fondo
                put(level, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, back)
                        .setValue(BedBlock.PART, BedPart.HEAD), bx + side.getStepX(), y, bz + side.getStepZ(), box);
                put(level, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, back)
                        .setValue(BedBlock.PART, BedPart.FOOT), bx + side.getStepX() - back.getStepX(), y,
                        bz + side.getStepZ() - back.getStepZ(), box);
                container(level, Blocks.CHEST.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, out),
                        bx - side.getStepX(), y, bz - side.getStepZ(), box, LOOT_HOME);
                if (r.nextBoolean()) {
                    container(level, Blocks.BARREL.defaultBlockState(), bx, y, bz, box, LOOT_STORAGE);
                }
            }
            case WORKSHOP -> {
                put(level, Blocks.CRAFTING_TABLE.defaultBlockState(), bx, y, bz, box);
                put(level, Blocks.FURNACE.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, out),
                        bx + side.getStepX(), y, bz + side.getStepZ(), box);
                put(level, Blocks.CHIPPED_ANVIL.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, side),
                        bx - side.getStepX(), y, bz - side.getStepZ(), box);
                // cofre contra la pared lateral, a media profundidad
                container(level, Blocks.CHEST.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, side.getOpposite()),
                        p.cx() + side.getStepX() * (hw - 1), y, p.cz() + side.getStepZ() * (hd - 1), box, LOOT_WORKSHOP);
            }
            case STORAGE -> {
                for (int k = -1; k <= 1; k++) {
                    container(level, Blocks.BARREL.defaultBlockState(), bx + side.getStepX() * k, y,
                            bz + side.getStepZ() * k, box, LOOT_STORAGE);
                }
                put(level, Blocks.HAY_BLOCK.defaultBlockState(), bx + side.getStepX() * -1, y + 1,
                        bz + side.getStepZ() * -1, box);
            }
            default -> { }
        }
    }

    /** Huerto: tierra de cultivo con canal de agua en el centro y borde de troncos secos. */
    private void farm(WorldGenLevel level, BoundingBox box, Plot p) {
        int hw = p.w() / 2, hd = p.d() / 2;
        int fy = platformY(p.cx(), p.cz(), hw, hd);
        boolean alongX = at(p.cx(), 0, p.cz(), 13).nextBoolean();
        Block crop = switch (at(p.cx(), 1, p.cz(), 13).nextInt(4)) {
            case 0 -> Blocks.WHEAT;
            case 1 -> Blocks.POTATOES;
            case 2 -> Blocks.CARROTS;
            default -> Blocks.BEETROOTS;
        };
        for (int dx = -hw; dx <= hw; dx++) {
            for (int dz = -hd; dz <= hd; dz++) {
                int lx = p.cx() + dx, lz = p.cz() + dz;
                column(level, lx, lz, fy, fy + 3, Blocks.DIRT.defaultBlockState(), box);
                boolean border = Math.abs(dx) == hw || Math.abs(dz) == hd;
                boolean channel = alongX ? dz == 0 : dx == 0;
                if (border) {
                    put(level, log(Math.abs(dx) == hw ? Direction.Axis.Z : Direction.Axis.X), lx, fy, lz, box);
                } else if (channel) {
                    put(level, Blocks.WATER.defaultBlockState(), lx, fy, lz, box);
                } else {
                    put(level, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), lx, fy, lz, box);
                    int age = at(lx, fy, lz, 14).nextInt(8);
                    BlockState s = crop == Blocks.BEETROOTS
                            ? crop.defaultBlockState().setValue(BeetrootBlock.AGE, age / 2)
                            : crop.defaultBlockState().setValue(CropBlock.AGE, age);
                    put(level, s, lx, fy + 1, lz, box);
                }
            }
        }
    }

    /** Torre de vigía 3x3: pilar central con escalera, plataforma, barandilla de barrotes y farol. */
    private void tower(WorldGenLevel level, BoundingBox box, int cx, int cz) {
        int fy = platformY(cx, cz, 1, 1);
        int top = fy + 6;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int lx = cx + dx, lz = cz + dz;
                column(level, lx, lz, fy, top + 2, Blocks.COBBLESTONE.defaultBlockState(), box);
                put(level, ModWorldBlocks.DEAD_PLANKS.get().defaultBlockState(), lx, fy, lz, box);
                boolean corner = dx != 0 && dz != 0;
                if (corner) {
                    for (int y = fy + 1; y <= top + 1; y++) {
                        put(level, log(Direction.Axis.Y), lx, y, lz, box);
                    }
                } else if (!(dx == 0 && dz == 1)) {                          // hueco de la escalera
                    put(level, ModWorldBlocks.DEAD_PLANKS.get().defaultBlockState(), lx, top, lz, box);
                    if (dx != 0 || dz != 0) {
                        put(level, Blocks.IRON_BARS.defaultBlockState(), lx, top + 1, lz, box);
                    }
                }
            }
        }
        for (int y = fy + 1; y < top; y++) {
            put(level, log(Direction.Axis.Y), cx, y, cz, box);              // pilar central
        }
        for (int y = fy + 1; y <= top; y++) {
            put(level, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH), cx, y, cz + 1, box);
        }
        put(level, Blocks.LANTERN.defaultBlockState(), cx - 1, top + 2, cz - 1, box);
    }

    /** Plaza: suelo de grava y tierra, fogata en el centro y faroles en las esquinas. */
    private void plaza(WorldGenLevel level, BoundingBox box) {
        int fy = h(C, C);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int lx = C + dx, lz = C + dz;
                column(level, lx, lz, fy, fy + 4, Blocks.DIRT.defaultBlockState(), box);
                float v = at(lx, fy, lz, 15).nextFloat();
                put(level, v < 0.5F ? Blocks.GRAVEL.defaultBlockState() : v < 0.8F ? Blocks.DIRT_PATH.defaultBlockState()
                        : Blocks.COARSE_DIRT.defaultBlockState(), lx, fy, lz, box);
            }
        }
        put(level, Blocks.CAMPFIRE.defaultBlockState(), C, fy + 1, C, box);
        for (int[] c : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
            put(level, log(Direction.Axis.Y), C + c[0], fy + 1, C + c[1], box);
            put(level, log(Direction.Axis.Y), C + c[0], fy + 2, C + c[1], box);
            put(level, Blocks.LANTERN.defaultBlockState(), C + c[0], fy + 3, C + c[1], box);
        }
    }

    /** Altura de la plataforma de una parcela: la media del terreno bajo ella. */
    private int platformY(int cx, int cz, int hw, int hd) {
        int sum = 0, n = 0;
        for (int dx = -hw; dx <= hw; dx++) {
            for (int dz = -hd; dz <= hd; dz++) {
                sum += h(cx + dx, cz + dz);
                n++;
            }
        }
        return Math.round(sum / (float) n);
    }
}
