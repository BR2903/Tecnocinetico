package com.bryanamaya.tecnocinetico.worldgen.feature;

import com.bryanamaya.tecnocinetico.block.ModWorldBlocks;
import com.bryanamaya.tecnocinetico.block.custom.StreetPropBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Farolas y postes eléctricos abandonados (mundo arrasado). {@code kind}: "lamp" (farola) o "pole" (poste de luz).
 * Pesos (de pie / doblada-inclinada / caída) en {@code standing} y {@code fallen}; el resto, doblada (solo farolas).
 * <ul>
 *   <li>Farola de pie: 3-4 fustes y la cabeza. Doblada: 2-3 fustes y el tramo torcido. Caída: tumbada en el suelo.</li>
 *   <li>Poste de pie: 4-6 tramos con la cruceta arriba y 1-3 cables colgando hacia un lado (hacia el siguiente
 *       poste que ya no está). Caído: tumbado con la cruceta en la punta y cables enredados alrededor.</li>
 * </ul>
 * Solo en suelo firme, seco y con hueco libre; si no cabe, no pone nada.
 */
public class StreetPropFeature extends Feature<StreetPropFeature.Config> {

    public record Config(String kind, int standing, int fallen) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("kind").forGetter(Config::kind),
                Codec.INT.fieldOf("standing").forGetter(Config::standing),
                Codec.INT.fieldOf("fallen").forGetter(Config::fallen)
        ).apply(i, Config::new));
    }

    public StreetPropFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        Config cfg = ctx.config();
        BlockPos origin = ctx.origin();
        if (!firmGround(level, origin)) {
            return false;
        }
        Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        boolean pole = "pole".equals(cfg.kind());
        int roll = random.nextInt(Math.max(1, cfg.standing() + cfg.fallen() + (pole ? 0 : 2)));
        if (roll < cfg.standing()) {
            return pole ? standingPole(level, origin, dir, random) : standingLamp(level, origin, dir, random, false);
        }
        if (roll < cfg.standing() + cfg.fallen()) {
            return fallen(level, origin, dir, random, pole);
        }
        return standingLamp(level, origin, dir, random, true);
    }

    private static boolean firmGround(WorldGenLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) && level.getFluidState(pos).isEmpty()
                && level.getBlockState(pos).canBeReplaced();
    }

    private static boolean free(WorldGenLevel level, BlockPos pos) {
        return level.getBlockState(pos).canBeReplaced() && level.getFluidState(pos).isEmpty();
    }

    private static BlockState axis(BlockState state, Direction.Axis axis) {
        return state.setValue(StreetPropBlocks.Pole.AXIS, axis);
    }

    // ------------------------------------------------------------------ farolas

    private static boolean standingLamp(WorldGenLevel level, BlockPos origin, Direction dir, RandomSource random, boolean bent) {
        int h = bent ? 2 + random.nextInt(2) : 3 + random.nextInt(2);
        for (int y = 0; y <= h; y++) {
            if (!free(level, origin.above(y))) {
                return false;
            }
        }
        BlockState shaft = axis(ModWorldBlocks.STREET_LAMP_POLE.get().defaultBlockState(), Direction.Axis.Y);
        for (int y = 0; y < h; y++) {
            level.setBlock(origin.above(y), shaft, 2);
        }
        BlockState top = (bent ? ModWorldBlocks.STREET_LAMP_BENT : ModWorldBlocks.STREET_LAMP_HEAD).get().defaultBlockState()
                .setValue(StreetPropBlocks.Facing.FACING, dir);
        level.setBlock(origin.above(h), top, 2);
        return true;
    }

    // ------------------------------------------------------------------ postes

    private static boolean standingPole(WorldGenLevel level, BlockPos origin, Direction dir, RandomSource random) {
        int h = 4 + random.nextInt(3);
        for (int y = 0; y < h; y++) {
            if (!free(level, origin.above(y))) {
                return false;
            }
        }
        BlockState wood = axis(ModWorldBlocks.UTILITY_POLE.get().defaultBlockState(), Direction.Axis.Y);
        for (int y = 0; y < h - 1; y++) {
            level.setBlock(origin.above(y), wood, 2);
        }
        Direction.Axis armAxis = dir.getClockWise().getAxis();               // la cruceta cruza la línea de cables
        BlockPos top = origin.above(h - 1);
        level.setBlock(top, ModWorldBlocks.UTILITY_CROSSARM.get().defaultBlockState()
                .setValue(StreetPropBlocks.Crossarm.AXIS, armAxis), 2);
        // cables que salen hacia el poste siguiente (que ya no existe) y se quedan colgando
        BlockState cable = ModWorldBlocks.POWER_CABLE.get().defaultBlockState()
                .setValue(StreetPropBlocks.Cable.AXIS, dir.getAxis()).setValue(StreetPropBlocks.Cable.HANGING, true);
        int len = 1 + random.nextInt(3);
        for (int i = 1; i <= len; i++) {
            BlockPos p = top.relative(dir, i);
            if (!free(level, p)) {
                break;
            }
            level.setBlock(p, cable, 2);
        }
        return true;
    }

    /** Tumbado en el suelo hacia `dir`; con la cabeza (farola) o la cruceta y cables enredados (poste). */
    private static boolean fallen(WorldGenLevel level, BlockPos origin, Direction dir, RandomSource random, boolean pole) {
        int len = pole ? 4 + random.nextInt(2) : 3 + random.nextInt(2);
        for (int i = 0; i <= len; i++) {
            BlockPos p = origin.relative(dir, i);
            if (!free(level, p) || !level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) {
                return false;
            }
        }
        BlockState body = axis((pole ? ModWorldBlocks.UTILITY_POLE : ModWorldBlocks.STREET_LAMP_POLE).get().defaultBlockState(), dir.getAxis());
        for (int i = 0; i < len; i++) {
            level.setBlock(origin.relative(dir, i), body, 2);
        }
        BlockPos end = origin.relative(dir, len);
        if (pole) {
            level.setBlock(end, ModWorldBlocks.UTILITY_CROSSARM.get().defaultBlockState()
                    .setValue(StreetPropBlocks.Crossarm.AXIS, dir.getClockWise().getAxis()), 2);
            BlockState cable = ModWorldBlocks.POWER_CABLE.get().defaultBlockState().setValue(StreetPropBlocks.Cable.HANGING, false);
            for (int k = 0; k < 2 + random.nextInt(3); k++) {                // cables enredados al lado
                BlockPos p = origin.relative(dir, random.nextInt(len + 1)).relative(dir.getClockWise(), random.nextBoolean() ? 1 : -1);
                if (free(level, p) && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) {
                    level.setBlock(p, cable.setValue(StreetPropBlocks.Cable.AXIS,
                            random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z), 2);
                }
            }
        } else {
            level.setBlock(end, ModWorldBlocks.STREET_LAMP_HEAD.get().defaultBlockState()
                    .setValue(StreetPropBlocks.Facing.FACING, dir.getClockWise()), 2);
        }
        return true;
    }
}
