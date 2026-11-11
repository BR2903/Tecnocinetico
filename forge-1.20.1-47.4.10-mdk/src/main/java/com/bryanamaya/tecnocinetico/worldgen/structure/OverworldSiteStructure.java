package com.bryanamaya.tecnocinetico.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import java.util.Optional;

/** Cinco instalaciones con un formato común; cada variante conserva su distribución y colocación propias. */
public class OverworldSiteStructure extends Structure {
    public static final Codec<OverworldSiteStructure> CODEC = RecordCodecBuilder.create(i -> i.group(
            settingsCodec(i), Codec.intRange(0, 4).fieldOf("site").forGetter(s -> s.site)
    ).apply(i, OverworldSiteStructure::new));
    private final int site;

    public OverworldSiteStructure(StructureSettings settings, int site) {
        super(settings);
        this.site = site;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        var gen = ctx.chunkGenerator();
        int x = ctx.chunkPos().getMiddleBlockX(), z = ctx.chunkPos().getMiddleBlockZ();
        int surface = gen.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        int floor;
        if (site == 4) {
            floor = gen.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState());
            if (surface < gen.getSeaLevel() - 1 || floor > gen.getSeaLevel() - 18) return Optional.empty();
            // Comprobar toda la huella: el centro puede estar en agua aunque una esquina caiga bajo una isla.
            int low = floor, high = floor;
            for (int dx : new int[]{-16, 0, 16}) for (int dz : new int[]{-20, 0, 20}) {
                int h = gen.getFirstOccupiedHeight(x + dx, z + dz, Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState());
                low = Math.min(low, h); high = Math.max(high, h);
            }
            if (high > gen.getSeaLevel() - 16 || high - low > 12) return Optional.empty();
            floor = low;
        } else {
            var column = gen.getBaseColumn(x, z, ctx.heightAccessor(), ctx.randomState());
            if (!column.getBlock(surface).getFluidState().isEmpty() || surface < gen.getSeaLevel()) return Optional.empty();
            floor = site == 2 ? surface - 12 : site == 3 ? Math.min(-24, surface - 30) : surface;
            if (floor < ctx.heightAccessor().getMinBuildHeight() + 6) return Optional.empty();
        }
        if (Math.max(surface + 6, floor + 30) >= ctx.heightAccessor().getMaxBuildHeight()) return Optional.empty();
        BlockPos origin = new BlockPos(x - 16, floor, z - 20);
        long seed = ctx.random().nextLong();
        return Optional.of(new GenerationStub(new BlockPos(x, floor, z),
                b -> b.addPiece(new OverworldSitePiece(origin, surface, gen.getSeaLevel(), site, seed))));
    }

    @Override
    public StructureType<?> type() { return ModStructures.OVERWORLD_SITE.get(); }
}
