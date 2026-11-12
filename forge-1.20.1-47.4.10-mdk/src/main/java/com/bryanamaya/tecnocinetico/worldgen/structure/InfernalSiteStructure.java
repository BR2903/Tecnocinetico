package com.bryanamaya.tecnocinetico.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Instalaciones del Nether industrial ({@code site}: 2 = Depósito de Refrigeración, 3 = Puesto de Vigilancia; 0 y 1
 * quedan para la fundición y la excavación de los jefes). Se colocan en una caverna real: busca, en la columna del
 * centro del chunk, un suelo sólido entre Y 34 y 95 con al menos {@link #HEADROOM} bloques de aire encima (nunca sobre
 * el techo de lecho de roca ni en lava).
 */
public class InfernalSiteStructure extends Structure {

    public static final Codec<InfernalSiteStructure> CODEC = RecordCodecBuilder.create(i -> i.group(
            settingsCodec(i),
            Codec.intRange(0, 3).fieldOf("site").forGetter(s -> s.site)
    ).apply(i, InfernalSiteStructure::new));

    public static final int DEPOT = 2, WATCHPOST = 3;
    private static final int HEADROOM = 14, MIN_Y = 34, MAX_Y = 95;

    private final int site;

    public InfernalSiteStructure(StructureSettings settings, int site) {
        super(settings);
        this.site = site;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int x = ctx.chunkPos().getMiddleBlockX(), z = ctx.chunkPos().getMiddleBlockZ();
        var column = ctx.chunkGenerator().getBaseColumn(x, z, ctx.heightAccessor(), ctx.randomState());
        int headroom = site == WATCHPOST ? 20 : HEADROOM;
        for (int y = MAX_Y; y >= MIN_Y; y--) {
            var floor = column.getBlock(y);
            if (!floor.isSolid() || !floor.getFluidState().isEmpty()) {
                continue;
            }
            boolean clear = true;
            for (int h = 1; h <= headroom; h++) {
                if (!column.getBlock(y + h).isAir()) {
                    clear = false;
                    break;
                }
            }
            if (!clear) {
                continue;
            }
            int half = InfernalSitePiece.size(site) / 2;
            BlockPos origin = new BlockPos(x - half, y, z - half);
            long seed = ctx.random().nextLong();
            return Optional.of(new GenerationStub(origin, b -> b.addPiece(new InfernalSitePiece(origin, site, seed))));
        }
        return Optional.empty();
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.INFERNAL_SITE.get();
    }
}
