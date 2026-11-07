package com.bryanamaya.tecnocinetico.worldgen.feature;

import com.bryanamaya.tecnocinetico.block.ModWorldBlocks;
import com.bryanamaya.tecnocinetico.block.custom.PileBlock;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Resto de muro derrumbado: tramo recto (a veces en L) de altura irregular, con vigas oxidadas que asoman,
 * escombros al pie y, si `vines`, enredaderas. Es lo que queda de los edificios de antes de la caída.
 */
public class RuinWallFeature extends Feature<RuinWallFeature.Config> {

    public record Config(BlockStateProvider wall, IntProvider height, IntProvider length, boolean vines)
            implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockStateProvider.CODEC.fieldOf("wall").forGetter(Config::wall),
                IntProvider.codec(1, 12).fieldOf("height").forGetter(Config::height),
                IntProvider.codec(1, 12).fieldOf("length").forGetter(Config::length),
                Codec.BOOL.fieldOf("vines").orElse(false).forGetter(Config::vines)
        ).apply(i, Config::new));
    }

    public RuinWallFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        Config cfg = ctx.config();
        BlockPos origin = ctx.origin();
        if (!level.getFluidState(origin).isEmpty() || !level.getFluidState(origin.below()).isEmpty()) {
            return false;
        }
        Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int len = cfg.length().sample(random);
        int h = cfg.height().sample(random);
        boolean placed = segment(level, random, cfg, origin, dir, len, h);
        if (random.nextFloat() < 0.4F) {                                   // esquina en L
            Direction side = random.nextBoolean() ? dir.getClockWise() : dir.getCounterClockWise();
            placed |= segment(level, random, cfg, origin, side, Math.max(2, len - 2), Math.max(1, h - 1));
        }
        return placed;
    }

    private boolean segment(WorldGenLevel level, RandomSource random, Config cfg, BlockPos start, Direction dir,
                            int len, int h) {
        boolean any = false;
        int peak = random.nextInt(len);
        for (int i = 0; i < len; i++) {
            BlockPos col = start.relative(dir, i);
            int ground = CraterFeature.surface(level, col.getX(), col.getZ());
            BlockPos base = new BlockPos(col.getX(), ground, col.getZ());
            if (!level.getFluidState(base.above()).isEmpty() || !level.getBlockState(base).isSolidRender(level, base)) {
                continue;                                                  // sin suelo firme o en agua: hueco
            }
            // altura rota: más alta cerca de un "pico" y bajando a saltos
            int colH = Math.max(0, h - (int) (Math.abs(i - peak) * (0.5F + random.nextFloat())));
            if (colH == 0) {
                continue;
            }
            any = true;
            for (int y = 1; y <= colH; y++) {
                BlockPos p = base.above(y);
                level.setBlock(p, cfg.wall().getState(random, p), 2);
                if (cfg.vines() && random.nextFloat() < 0.35F) {
                    vine(level, p, dir.getClockWise());
                    vine(level, p, dir.getCounterClockWise());
                }
            }
            // cimiento: rellena hasta 2 bloques si el muro queda colgando en una ladera
            for (int y = 0; y >= -2; y--) {
                BlockPos p = base.above(y);
                if (level.getBlockState(p).canBeReplaced()) {
                    level.setBlock(p, cfg.wall().getState(random, p), 2);
                }
            }
            BlockPos top = base.above(colH + 1);
            if (random.nextFloat() < 0.25F && level.getBlockState(top).isAir()) {
                level.setBlock(top, ModWorldBlocks.CRACKED_CONCRETE_SLAB.get().defaultBlockState(), 2);
            }
            // viga oxidada que asoma del muro
            if (colH >= 2 && random.nextFloat() < 0.18F) {
                Direction out = random.nextBoolean() ? dir.getClockWise() : dir.getCounterClockWise();
                BlockPos beam = base.above(1 + random.nextInt(colH)).relative(out);
                if (level.getBlockState(beam).canBeReplaced()) {
                    level.setBlock(beam, ModWorldBlocks.RUSTED_METAL_BEAM.get().defaultBlockState()
                            .setValue(RotatedPillarBlock.AXIS, out.getAxis()), 2);
                }
            }
            // escombros caídos al pie
            if (random.nextFloat() < 0.35F) {
                Direction out = random.nextBoolean() ? dir.getClockWise() : dir.getCounterClockWise();
                BlockPos pile = base.above().relative(out);
                BlockPos below = pile.below();
                if (level.getBlockState(pile).isAir() && level.getBlockState(below).isSolidRender(level, below)) {
                    level.setBlock(pile, ModWorldBlocks.RUBBLE.get().defaultBlockState()
                            .setValue(PileBlock.PIECES, 1 + random.nextInt(3)), 2);
                }
            }
        }
        return any;
    }

    /** Enredadera colgando de la cara del muro que mira hacia `face`. */
    private static void vine(WorldGenLevel level, BlockPos wall, Direction face) {
        BlockPos p = wall.relative(face);
        if (level.getBlockState(p).isAir()) {
            level.setBlock(p, Blocks.VINE.defaultBlockState()
                    .setValue(VineBlock.getPropertyForFace(face.getOpposite()), true), 2);
        }
    }
}
