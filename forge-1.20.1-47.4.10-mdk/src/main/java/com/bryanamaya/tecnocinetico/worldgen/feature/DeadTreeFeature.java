package com.bryanamaya.tecnocinetico.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Árbol muerto: tronco seco sin hojas, a veces partido, con 1-3 ramas desnudas (horizontales y a veces
 * subiendo en diagonal).
 */
public class DeadTreeFeature extends Feature<DeadTreeFeature.Config> {

    public record Config(BlockStateProvider log, IntProvider height) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockStateProvider.CODEC.fieldOf("log").forGetter(Config::log),
                IntProvider.codec(2, 16).fieldOf("height").forGetter(Config::height)
        ).apply(i, Config::new));
    }

    public DeadTreeFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        Config cfg = ctx.config();
        BlockPos origin = ctx.origin();
        BlockPos below = origin.below();
        BlockState ground = level.getBlockState(below);
        if (!(ground.is(BlockTags.DIRT) || ground.is(BlockTags.SAND) || ground.is(net.minecraft.world.level.block.Blocks.GRAVEL)
                || ground.isSolidRender(level, below)) || !level.getFluidState(origin).isEmpty()) {
            return false;
        }
        int h = cfg.height().sample(random);
        for (int y = 0; y < h; y++) {
            if (!level.getBlockState(origin.above(y)).canBeReplaced()) {
                return false;
            }
        }
        for (int y = 0; y < h; y++) {
            BlockPos p = origin.above(y);
            level.setBlock(p, log(cfg, random, p, Direction.Axis.Y), 2);
        }
        int branches = 1 + random.nextInt(3);
        for (int b = 0; b < branches; b++) {
            Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            BlockPos p = origin.above(h / 2 + random.nextInt(Math.max(1, h - h / 2)));
            int len = 1 + random.nextInt(3);
            for (int k = 0; k < len; k++) {
                p = p.relative(dir);
                if (k > 0 && random.nextFloat() < 0.4F) {
                    p = p.above();                                         // la rama sube en diagonal
                }
                if (!level.getBlockState(p).canBeReplaced()) {
                    break;
                }
                level.setBlock(p, log(cfg, random, p, dir.getAxis()), 2);
            }
        }
        return true;
    }

    private static BlockState log(Config cfg, RandomSource random, BlockPos pos, Direction.Axis axis) {
        BlockState s = cfg.log().getState(random, pos);
        return s.hasProperty(BlockStateProperties.AXIS) ? s.setValue(RotatedPillarBlock.AXIS, axis) : s;
    }
}
