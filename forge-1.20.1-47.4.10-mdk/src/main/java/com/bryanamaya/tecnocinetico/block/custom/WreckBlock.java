package com.bryanamaya.tecnocinetico.block.custom;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Vehículo abandonado troceado en celdas: cada celda es un bloque con {@code facing} (hacia dónde mira el frontal) y
 * {@code part} = (h*W + w)*L + l (l a lo largo, 0 = frontal; w a lo ancho; h en altura). Cada celda dibuja su trozo
 * (modelos de Codex, {@code tools/vehicles_gen.py}) y tiene la colisión de su trozo
 * ({@code assets/tecnocinetico/wreck_shapes.json}, en píxeles con facing=north, girada para las demás).
 * Romper cualquier pieza desarma el vehículo entero; solo la pieza rota suelta su botín (chatarra).
 */
public abstract class WreckBlock extends HorizontalDirectionalBlock {

    /** Dimensiones en celdas. */
    public record Size(int length, int width, int height) {
        public int parts() {
            return length * width * height;
        }
    }

    private final String id;
    private final Size size;
    private final VoxelShape[][] shapes;                                     // [part][facing 2D index]

    protected WreckBlock(Properties properties, String id, Size size) {
        super(properties);
        this.id = id;
        this.size = size;
        this.shapes = loadShapes(id, size);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(part(), 0));
    }

    public abstract IntegerProperty part();

    public Size size() {
        return size;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, partProperty());
    }

    /** La propiedad se pide antes de que exista la instancia (createBlockStateDefinition va en el constructor padre). */
    protected abstract IntegerProperty partProperty();

    // ------------------------------------------------------------------ geometría

    public static int partIndex(Size s, int l, int w, int h) {
        return (h * s.width() + w) * s.length() + l;
    }

    /** Posición en el mundo de la celda (l, w, h) de un vehículo cuyo frontal-izquierda-abajo (l=w=h=0) está en origin. */
    public static BlockPos cell(BlockPos origin, Direction facing, int l, int w, int h) {
        Direction back = facing.getOpposite(), right = facing.getClockWise();
        return origin.relative(back, l).relative(right, w).above(h);
    }

    /** Origen (celda 0) a partir de una celda cualquiera. */
    public BlockPos origin(BlockPos pos, BlockState state) {
        int p = state.getValue(part());
        int l = p % size.length(), w = (p / size.length()) % size.width(), h = p / (size.length() * size.width());
        Direction facing = state.getValue(FACING);
        return pos.relative(facing.getOpposite(), -l).relative(facing.getClockWise(), -w).below(h);
    }

    /** Celdas con algo dentro (las vacías, p. ej. encima del capó, no se colocan). */
    public boolean solid(int part) {
        return shapes[part] != null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        VoxelShape[] byFacing = shapes[state.getValue(part())];
        return byFacing == null ? Shapes.empty() : byFacing[state.getValue(FACING).get2DDataValue()];
    }

    private static VoxelShape[][] loadShapes(String id, Size size) {
        VoxelShape[][] out = new VoxelShape[size.parts()][];
        try (var in = WreckBlock.class.getResourceAsStream("/assets/tecnocinetico/wreck_shapes.json")) {
            if (in == null) {
                throw new IllegalStateException("falta wreck_shapes.json");
            }
            // formato de Codex (build/vehiculos/integracion.json): vehicles.<id>.collision[part] = caja o null
            JsonObject all = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            var parts = all.getAsJsonObject("vehicles").getAsJsonObject(id).getAsJsonArray("collision");
            for (int p = 0; p < size.parts(); p++) {
                JsonElement e = p < parts.size() ? parts.get(p) : null;
                if (e == null || e.isJsonNull()) {
                    continue;
                }
                var a = e.getAsJsonArray();
                double x0 = a.get(0).getAsDouble(), y0 = a.get(1).getAsDouble(), z0 = a.get(2).getAsDouble();
                double x1 = a.get(3).getAsDouble(), y1 = a.get(4).getAsDouble(), z1 = a.get(5).getAsDouble();
                out[p] = new VoxelShape[4];
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    out[p][d.get2DDataValue()] = rotated(d, x0, y0, z0, x1, y1, z1);
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo leer la colisión de " + id, ex);
        }
        return out;
    }

    /** Caja en píxeles definida para facing=north, girada alrededor del centro del bloque. */
    private static VoxelShape rotated(Direction facing, double x0, double y0, double z0, double x1, double y1, double z1) {
        return switch (facing) {
            case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
            case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
            case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
            default -> Block.box(x0, y0, z0, x1, y1, z1);
        };
    }

    // ------------------------------------------------------------------ colocar y desarmar

    /** Celdas que ocuparía un vehículo con el frontal-izquierda-abajo en origin. */
    public List<BlockPos> cells(BlockPos origin, Direction facing) {
        List<BlockPos> out = new ArrayList<>();
        for (int h = 0; h < size.height(); h++) {
            for (int w = 0; w < size.width(); w++) {
                for (int l = 0; l < size.length(); l++) {
                    if (solid(partIndex(size, l, w, h))) {
                        out.add(cell(origin, facing, l, w, h));
                    }
                }
            }
        }
        return out;
    }

    /** ¿Cabe? (celdas libres y la fila de abajo apoyada en suelo firme). */
    public boolean fits(LevelAccessor level, BlockPos origin, Direction facing) {
        for (int w = 0; w < size.width(); w++) {
            for (int l = 0; l < size.length(); l++) {
                BlockPos below = cell(origin, facing, l, w, 0).below();
                if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                    return false;
                }
            }
        }
        for (BlockPos p : cells(origin, facing)) {
            if (!level.getBlockState(p).canBeReplaced() || !level.getFluidState(p).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void placeAll(LevelAccessor level, BlockPos origin, Direction facing, int flags) {
        for (int h = 0; h < size.height(); h++) {
            for (int w = 0; w < size.width(); w++) {
                for (int l = 0; l < size.length(); l++) {
                    int p = partIndex(size, l, w, h);
                    if (solid(p)) {
                        level.setBlock(cell(origin, facing, l, w, h), defaultBlockState().setValue(FACING, facing).setValue(part(), p), flags);
                    }
                }
            }
        }
    }

    /** Al romper una pieza, el resto del vehículo desaparece sin soltar nada (la rota suelta su botín normal). */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        super.playerWillDestroy(level, pos, state, player);
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            for (BlockPos p : cells(origin(pos, state), facing)) {
                if (!p.equals(pos) && level.getBlockState(p).is(this) && level.getBlockState(p).getValue(FACING) == facing) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
                }
            }
        }
    }

    // ------------------------------------------------------------------ los tres vehículos

    public static class Car extends WreckBlock {
        public static final IntegerProperty PART = IntegerProperty.create("part", 0, 15);

        public Car(Properties p) {
            super(p, "car_wreck", new Size(4, 2, 2));
        }

        @Override
        public IntegerProperty part() {
            return PART;
        }

        @Override
        protected IntegerProperty partProperty() {
            return PART;
        }
    }

    public static class Truck extends WreckBlock {
        public static final IntegerProperty PART = IntegerProperty.create("part", 0, 35);

        public Truck(Properties p) {
            super(p, "truck_wreck", new Size(6, 2, 3));
        }

        @Override
        public IntegerProperty part() {
            return PART;
        }

        @Override
        protected IntegerProperty partProperty() {
            return PART;
        }
    }

    public static class Bus extends WreckBlock {
        public static final IntegerProperty PART = IntegerProperty.create("part", 0, 71);

        public Bus(Properties p) {
            super(p, "bus_wreck", new Size(8, 3, 3));
        }

        @Override
        public IntegerProperty part() {
            return PART;
        }

        @Override
        protected IntegerProperty partProperty() {
            return PART;
        }
    }
}
