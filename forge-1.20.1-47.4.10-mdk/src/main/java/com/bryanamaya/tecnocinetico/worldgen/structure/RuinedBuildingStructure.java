package com.bryanamaya.tecnocinetico.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Edificio en ruinas generado por código (sin plantilla NBT): planta rectangular, varios pisos, derrumbe
 * diagonal, ventanas rotas, forjados agujereados, escombros y cofre. Ver {@link RuinedBuildingPiece}.
 *
 * JSON (data/tecnocinetico/worldgen/structure): campos estándar + "floors", "width" y "style" (urban|overgrown|frozen).
 */
public class RuinedBuildingStructure extends Structure {

    public enum Style implements StringRepresentable {
        URBAN, OVERGROWN, FROZEN;

        public static final Codec<Style> CODEC = StringRepresentable.fromEnum(Style::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }

    public static final Codec<RuinedBuildingStructure> CODEC = RecordCodecBuilder.create(i -> i.group(
            settingsCodec(i),
            IntProvider.codec(1, 10).fieldOf("floors").forGetter(s -> s.floors),
            IntProvider.codec(5, 21).fieldOf("width").forGetter(s -> s.width),
            Style.CODEC.fieldOf("style").forGetter(s -> s.style)
    ).apply(i, RuinedBuildingStructure::new));

    private final IntProvider floors;
    private final IntProvider width;
    private final Style style;

    public RuinedBuildingStructure(StructureSettings settings, IntProvider floors, IntProvider width, Style style) {
        super(settings);
        this.floors = floors;
        this.width = width;
        this.style = style;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        var random = ctx.random();
        int w = width.sample(random);
        int d = Math.max(5, w + random.nextInt(5) - 2);
        int x0 = cp.getMinBlockX(), z0 = cp.getMinBlockZ();

        // terreno razonablemente plano y fuera del agua
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int[] c : new int[][]{{0, 0}, {w - 1, 0}, {0, d - 1}, {w - 1, d - 1}, {w / 2, d / 2}}) {
            int h = ctx.chunkGenerator().getFirstOccupiedHeight(x0 + c[0], z0 + c[1], Heightmap.Types.WORLD_SURFACE_WG,
                    ctx.heightAccessor(), ctx.randomState());
            NoiseColumn col = ctx.chunkGenerator().getBaseColumn(x0 + c[0], z0 + c[1], ctx.heightAccessor(), ctx.randomState());
            if (!col.getBlock(h).getFluidState().isEmpty() || h < ctx.chunkGenerator().getSeaLevel()) {
                return Optional.empty();
            }
            min = Math.min(min, h);
            max = Math.max(max, h);
        }
        if (max - min > 5) {
            return Optional.empty();
        }
        BlockPos origin = new BlockPos(x0, min, z0);
        int f = floors.sample(random);
        long seed = random.nextLong();
        return Optional.of(new GenerationStub(origin, b -> b.addPiece(new RuinedBuildingPiece(origin, w, d, f, style, seed))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.RUINED_BUILDING.get();
    }
}
