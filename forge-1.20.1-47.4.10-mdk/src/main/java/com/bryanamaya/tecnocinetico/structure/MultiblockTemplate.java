package com.bryanamaya.tecnocinetico.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Plano de una estructura grande que se despliega de una vez (kit). Capas de abajo arriba; en cada capa, filas de
 * delante (z = 0, lado del jugador) hacia atrás y columnas de izquierda a derecha. Caracteres: 'C' = núcleo,
 * ' ' = aire (debe estar libre y queda vacío), cualquier otro = pieza según `parts`.
 * Coordenadas locales: x a la derecha del jugador, y arriba, z hacia delante (en la dirección en que mira).
 */
public final class MultiblockTemplate {

    public final String[][] layers;            // [y][z] -> fila de x
    public final int width, height, depth;
    private final Map<Character, Supplier<Block>> parts;
    private final Supplier<Block> core;
    private final int cx, cy, cz;
    /** Ajuste del estado de cada pieza según su posición local (p. ej. qué trozo del modelo dibuja). */
    private java.util.function.BiFunction<int[], BlockState, BlockState> partState = (p, s) -> s;

    public MultiblockTemplate withPartState(java.util.function.BiFunction<int[], BlockState, BlockState> f) {
        this.partState = f;
        return this;
    }

    public MultiblockTemplate(String[][] layers, Supplier<Block> core, Map<Character, Supplier<Block>> parts) {
        this.layers = layers;
        this.height = layers.length;
        this.depth = layers[0].length;
        this.width = layers[0][0].length();
        this.parts = parts;
        this.core = core;
        int fx = -1, fy = -1, fz = -1;
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                int x = layers[y][z].indexOf('C');
                if (x >= 0) {
                    fx = x;
                    fy = y;
                    fz = z;
                }
            }
        }
        if (fx < 0) {
            throw new IllegalArgumentException("plano sin núcleo");
        }
        this.cx = fx;
        this.cy = fy;
        this.cz = fz;
    }

    public static BlockPos toWorld(BlockPos origin, Direction facing, int x, int y, int z) {
        Direction right = facing.getClockWise();
        return origin.offset(right.getStepX() * x + facing.getStepX() * z, y, right.getStepZ() * x + facing.getStepZ() * z);
    }

    /** Esquina delantera-izquierda-inferior a partir de la posición del núcleo. */
    public BlockPos originFromCore(BlockPos corePos, Direction facing) {
        Direction right = facing.getClockWise();
        return corePos.offset(-(right.getStepX() * cx + facing.getStepX() * cz), -cy, -(right.getStepZ() * cx + facing.getStepZ() * cz));
    }

    public BlockPos corePos(BlockPos origin, Direction facing) {
        return toWorld(origin, facing, cx, cy, cz);
    }

    /** Todas las posiciones ocupadas (piezas + núcleo), sin el aire. */
    public List<BlockPos> occupied(BlockPos origin, Direction facing) {
        List<BlockPos> out = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    if (layers[y][z].charAt(x) != ' ') {
                        out.add(toWorld(origin, facing, x, y, z));
                    }
                }
            }
        }
        return out;
    }

    /** ¿Está libre todo el volumen (aire o reemplazable, como hierba o agua)? Devuelve el primer estorbo o null. */
    public BlockPos firstObstacle(Level level, BlockPos origin, Direction facing) {
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    BlockPos p = toWorld(origin, facing, x, y, z);
                    BlockState s = level.getBlockState(p);
                    if (!s.canBeReplaced() || level.isOutsideBuildHeight(p)) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Coloca la estructura. El núcleo mira hacia el jugador (FACING = opuesto a `facing`); las piezas guardan dónde
     * está el núcleo. Devuelve la posición del núcleo.
     */
    public BlockPos place(Level level, BlockPos origin, Direction facing, java.util.function.Function<BlockState, BlockState> coreState) {
        BlockPos corePos = corePos(origin, facing);
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    BlockPos p = toWorld(origin, facing, x, y, z);
                    char c = layers[y][z].charAt(x);
                    if (c == ' ') {
                        level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    } else if (c != 'C') {
                        BlockState s = parts.get(c).get().defaultBlockState();
                        if (s.hasProperty(StructurePartBlock.FACING)) {
                            s = s.setValue(StructurePartBlock.FACING, facing.getOpposite());
                        }
                        s = partState.apply(new int[]{x, y, z}, s);
                        level.setBlock(p, s, 3);
                        if (level.getBlockEntity(p) instanceof StructurePartBlockEntity part) {
                            part.setCore(corePos);
                        }
                    }
                }
            }
        }
        level.setBlock(corePos, coreState.apply(core.get().defaultBlockState()), 3);
        return corePos;
    }
}
