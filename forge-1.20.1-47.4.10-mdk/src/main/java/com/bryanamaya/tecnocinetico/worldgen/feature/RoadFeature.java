package com.bryanamaya.tecnocinetico.worldgen.feature;

import com.bryanamaya.tecnocinetico.block.ModWorldBlocks;
import com.bryanamaya.tecnocinetico.worldgen.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Carreteras rotas (mundo arrasado). Se ejecuta una vez por chunk y solo toca las columnas de su propio chunk, así
 * que los tramos de chunks vecinos encajan sin depender del orden de generación: todo sale de una cuadrícula fija
 * desplazada por la semilla del mundo.
 * <ul>
 *   <li><b>Calles</b> de la Megaciudad Muerta: cuadrícula cada {@link #CITY_SPACING} bloques, 7 de ancho, con línea
 *       discontinua en el centro.</li>
 *   <li><b>Autopistas</b> por todo el mapa: cada {@link #HIGHWAY_SPACING} bloques, 9 de ancho, línea en el centro y
 *       quitamiedos de hormigón (rotos a trozos) en los bordes.</li>
 * </ul>
 * Daños: asfalto agrietado, baches de grava, tramos que faltan y cortes en el agua (puentes caídos) y en la montaña.
 * La vegetación encima se despeja; los edificios en ruinas, cráteres y escombros que se generan después quedan encima.
 */
public class RoadFeature extends Feature<NoneFeatureConfiguration> {

    public static final int CITY_SPACING = 48, CITY_HALF = 3;
    public static final int HIGHWAY_SPACING = 512, HIGHWAY_HALF = 4;
    /** Por encima de esto no hay autopista (montañas). */
    private static final int MAX_HIGHWAY_Y = 110;

    public RoadFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    /** Tramo de carretera en una columna: eje a lo largo, distancia al centro, medio ancho y si es autopista. */
    public record Road(Direction.Axis along, int offset, int half, boolean highway) {}

    /** ¿Qué carretera pasa por (x, z)? (null = ninguna). `city` indica si la columna es Megaciudad. */
    public static Road roadAt(long seed, int x, int z, boolean city) {
        Road best = null;
        if (city) {
            best = line(seed, x, z, CITY_SPACING, CITY_HALF, 0x5EED1L, false);
        }
        Road hw = line(seed, x, z, HIGHWAY_SPACING, HIGHWAY_HALF + 1, 0xA1B2C3L, true);  // +1: quitamiedos
        return hw != null ? hw : best;
    }

    private static Road line(long seed, int x, int z, int spacing, int half, long salt, boolean highway) {
        int ox = (int) Math.floorMod(seed ^ salt, (long) spacing), oz = (int) Math.floorMod((seed >> 20) ^ salt, (long) spacing);
        int dx = Math.floorMod(x - ox + half, spacing) - half;           // distancia a la línea vertical (eje Z)
        int dz = Math.floorMod(z - oz + half, spacing) - half;
        boolean onX = Math.abs(dx) <= half, onZ = Math.abs(dz) <= half;
        if (onX && onZ) {
            return new Road(Math.abs(dx) <= Math.abs(dz) ? Direction.Axis.Z : Direction.Axis.X, 0, half, highway);  // cruce, sin línea
        }
        if (onX) {
            return new Road(Direction.Axis.Z, dx, half, highway);
        }
        if (onZ) {
            return new Road(Direction.Axis.X, dz, half, highway);
        }
        return null;
    }

    /** Ruido entero por celda (determinista): para baches, tramos perdidos y quitamiedos rotos. */
    private static double hash(long seed, int a, int b, int salt) {
        long h = seed * 0x9E3779B97F4A7C15L + a * 0x632BE59BD9B4E019L + b * 0x85157AF5L + salt * 0x2545F4914F6CDD1DL;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h >>> 11) * 0x1.0p-53;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        long seed = level.getSeed();
        int bx = ctx.origin().getX() & ~15, bz = ctx.origin().getZ() & ~15;
        boolean placed = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = bx + dx, z = bz + dz;
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                BlockPos top = new BlockPos(x, y, z);
                boolean city = level.getBiome(top).is(ModBiomes.DEAD_MEGACITY);
                Road road = roadAt(seed, x, z, city);
                if (road == null) {
                    continue;
                }
                placed |= paint(level, seed, top, x, z, road);
            }
        }
        return placed;
    }

    /** Pinta una columna de carretera (público para las pruebas). */
    public static boolean paint(WorldGenLevel level, long seed, BlockPos top, int x, int z, Road road) {
        if (!level.getFluidState(top).isEmpty() || !level.getFluidState(top.above()).isEmpty()
                || (road.highway() && top.getY() > MAX_HIGHWAY_Y) || !level.getBlockState(top).isSolid()) {
            return false;                                                      // puente caído / montaña
        }
        int along = road.along() == Direction.Axis.Z ? z : x;
        int across = road.along() == Direction.Axis.Z ? x : z;
        if (hash(seed, along >> 3, across >> 4, 1) < 0.10) {
            return false;                                                      // tramo perdido
        }
        int off = Math.abs(road.offset());
        int half = road.highway() ? HIGHWAY_HALF : road.half();
        if (road.highway() && off == half + 1) {                               // quitamiedos a trozos
            if (hash(seed, along >> 2, across, 2) < 0.55) {
                clearAbove(level, top);
                level.setBlock(top.above(), (hash(seed, along, across, 3) < 0.5 ? ModWorldBlocks.CRACKED_CONCRETE_WALL
                        : ModWorldBlocks.CONCRETE_WALL).get().defaultBlockState(), 2);
            }
            return true;
        }
        double r = hash(seed, x, z, 4);
        BlockState surface;
        if (r < 0.05) {
            surface = net.minecraft.world.level.block.Blocks.GRAVEL.defaultBlockState();       // bache
        } else if (off == 0 && road.offset() == 0 && Math.floorMod(along, 6) < 3 && (r > 0.15)) {
            Direction facing = road.along() == Direction.Axis.Z ? Direction.NORTH : Direction.EAST;
            surface = ModWorldBlocks.ASPHALT_ROAD_LINE.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
        } else {
            surface = (r < 0.40 ? ModWorldBlocks.CRACKED_ASPHALT : ModWorldBlocks.ASPHALT).get().defaultBlockState();
        }
        clearAbove(level, top);
        level.setBlock(top, surface, 2);
        return true;
    }

    /** Quita plantas, nieve y hojas sueltas de encima (no troncos ni bloques sólidos). */
    private static void clearAbove(WorldGenLevel level, BlockPos top) {
        for (int k = 1; k <= 3; k++) {
            BlockPos p = top.above(k);
            BlockState s = level.getBlockState(p);
            if (!s.isAir() && (s.canBeReplaced() || s.is(net.minecraft.tags.BlockTags.LEAVES)) && level.getFluidState(p).isEmpty()) {
                level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }
}
