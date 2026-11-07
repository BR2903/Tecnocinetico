package com.bryanamaya.tecnocinetico.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Cráter de impacto: cuenco hundido (paraboloide) con fondo de `floor` y un borde levantado de `rim`.
 * No se genera junto al agua (dejaría agujeros raros en la costa).
 */
public class CraterFeature extends Feature<CraterFeature.Config> {

    public record Config(IntProvider radius, BlockStateProvider floor, BlockStateProvider rim) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                IntProvider.codec(2, 10).fieldOf("radius").forGetter(Config::radius),
                BlockStateProvider.CODEC.fieldOf("floor").forGetter(Config::floor),
                BlockStateProvider.CODEC.fieldOf("rim").forGetter(Config::rim)
        ).apply(i, Config::new));
    }

    public CraterFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        Config cfg = ctx.config();
        BlockPos origin = ctx.origin();
        int r = cfg.radius().sample(random);
        double depth = Math.max(2, r * 0.6);
        int sea = ctx.chunkGenerator().getSeaLevel();

        // Comprobar que todo el cráter está en tierra seca
        for (int dx = -r - 2; dx <= r + 2; dx += 2) {
            for (int dz = -r - 2; dz <= r + 2; dz += 2) {
                int top = surface(level, origin.getX() + dx, origin.getZ() + dz);
                if (top <= sea || !level.getFluidState(new BlockPos(origin.getX() + dx, top, origin.getZ() + dz)).isEmpty()) {
                    return false;
                }
            }
        }
        // El cráter se mide desde la altura media del terreno (si no, en laderas queda torcido)
        int base = surface(level, origin.getX(), origin.getZ());

        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -r - 2; dx <= r + 2; dx++) {
            for (int dz = -r - 2; dz <= r + 2; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz) + random.nextDouble() * 0.6;
                int x = origin.getX() + dx, z = origin.getZ() + dz;
                int top = surface(level, x, z);
                if (d < r) {
                    int bottom = (int) Math.round(base - depth * (1 - (d / r) * (d / r)));
                    // vaciar desde el fondo hasta la superficie (y las plantas de encima)
                    for (int y = bottom + 1; y <= Math.max(top, bottom) + 3; y++) {
                        p.set(x, y, z);
                        BlockState s = level.getBlockState(p);
                        if (!s.isAir() && (y <= top || s.canBeReplaced())) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                    p.set(x, bottom, z);
                    level.setBlock(p, cfg.floor().getState(random, p), 2);
                    if (random.nextFloat() < 0.5F) {
                        p.set(x, bottom - 1, z);
                        level.setBlock(p, cfg.floor().getState(random, p), 2);
                    }
                } else if (d < r + 2 && random.nextFloat() < (r + 2 - d) * 0.55F) {
                    // borde levantado: tierra expulsada por el impacto
                    p.set(x, top + 1, z);
                    if (level.getBlockState(p).canBeReplaced()) {
                        level.setBlock(p, cfg.rim().getState(random, p), 2);
                    }
                }
            }
        }
        return true;
    }

    /** Altura del bloque sólido de la superficie en (x, z). */
    static int surface(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
    }
}
