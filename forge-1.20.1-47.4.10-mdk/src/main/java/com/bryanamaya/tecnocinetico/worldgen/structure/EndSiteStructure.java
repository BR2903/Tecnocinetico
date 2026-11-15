package com.bryanamaya.tecnocinetico.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Restos de la red de la IA en las islas exteriores del End ({@code site}: 0 = Nodo de Datos, 1 = Granja de
 * Servidores, 2 = Puerto de Lanzamiento). El nodo y la granja necesitan una isla de verdad bajo todo su terreno (centro y
 * esquinas con suelo a una altura parecida). El Puerto es <b>único</b> (anillo concéntrico de un solo elemento en su
 * structure_set) y se construye su propia isla, así siempre existe aunque caiga sobre el vacío.
 */
public class EndSiteStructure extends Structure {

    public static final Codec<EndSiteStructure> CODEC = RecordCodecBuilder.create(i -> i.group(
            settingsCodec(i),
            Codec.intRange(0, 2).fieldOf("site").forGetter(s -> s.site)
    ).apply(i, EndSiteStructure::new));

    public static final int NODE = 0, FARM = 1, PORT = 2;
    /** Altura mínima del suelo de una isla para construir encima. */
    public static final int MIN_GROUND = 45;
    /** Altura del Puerto si cae sobre el vacío. */
    public static final int PORT_Y = 62;

    private final int site;

    public EndSiteStructure(StructureSettings settings, int site) {
        super(settings);
        this.site = site;
    }

    private static int ground(GenerationContext ctx, int x, int z) {
        return ctx.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int x = ctx.chunkPos().getMiddleBlockX(), z = ctx.chunkPos().getMiddleBlockZ();
        int half = EndSitePiece.size(site) / 2;
        int y = ground(ctx, x, z);
        if (site == PORT) {
            y = y >= MIN_GROUND && y <= 80 ? y : PORT_Y;                          // su propia isla
        } else {
            if (y < MIN_GROUND) {
                return Optional.empty();
            }
            for (int[] c : new int[][]{{-half, -half}, {half, -half}, {-half, half}, {half, half}}) {
                int h = ground(ctx, x + c[0], z + c[1]);
                if (h < MIN_GROUND || Math.abs(h - y) > 6) {
                    return Optional.empty();                                     // borde de la isla o desnivel
                }
            }
        }
        BlockPos origin = new BlockPos(x - half, y, z - half);
        long seed = ctx.random().nextLong();
        return Optional.of(new GenerationStub(origin, b -> b.addPiece(new EndSitePiece(origin, site, seed))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.END_SITE.get();
    }
}
