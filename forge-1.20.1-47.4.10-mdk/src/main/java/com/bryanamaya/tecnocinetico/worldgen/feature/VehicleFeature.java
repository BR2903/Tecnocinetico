package com.bryanamaya.tecnocinetico.worldgen.feature;

import com.bryanamaya.tecnocinetico.block.ModWorldBlocks;
import com.bryanamaya.tecnocinetico.block.custom.WreckBlock;
import com.bryanamaya.tecnocinetico.worldgen.ModBiomes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Vehículos abandonados. Busca en el chunk un punto de carretera ({@link RoadFeature#roadAt}) y deja el vehículo en
 * un carril, alineado con la calzada; si no hay carretera, con probabilidad {@code offroad} lo deja tirado en
 * cualquier sitio llano con una orientación al azar. Pesos de coche / camión / autobús en la configuración (los
 * autobuses, sobre todo en la ciudad).
 */
public class VehicleFeature extends Feature<VehicleFeature.Config> {

    public record Config(int car, int truck, int bus, float offroad) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("car").forGetter(Config::car),
                Codec.INT.fieldOf("truck").forGetter(Config::truck),
                Codec.INT.fieldOf("bus").forGetter(Config::bus),
                Codec.FLOAT.fieldOf("offroad").forGetter(Config::offroad)
        ).apply(i, Config::new));
    }

    public VehicleFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        Config cfg = ctx.config();
        WreckBlock wreck = pick(cfg, random);
        if (wreck == null) {
            return false;
        }
        int bx = ctx.origin().getX() & ~15, bz = ctx.origin().getZ() & ~15;
        // varios intentos dentro del chunk (la carretera puede pasar solo por una parte)
        for (int attempt = 0; attempt < 12; attempt++) {
            int x = bx + random.nextInt(16), z = bz + random.nextInt(16);
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            BlockPos at = new BlockPos(x, y, z);
            boolean city = level.getBiome(at).is(ModBiomes.DEAD_MEGACITY);
            var road = RoadFeature.roadAt(level.getSeed(), x, z, city);
            Direction facing;
            if (road != null && Math.abs(road.offset()) < road.half()) {
                facing = road.along() == Direction.Axis.Z ? (random.nextBoolean() ? Direction.NORTH : Direction.SOUTH)
                        : (random.nextBoolean() ? Direction.EAST : Direction.WEST);
            } else if (attempt == 0 && random.nextFloat() < cfg.offroad()) {
                facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            } else {
                continue;
            }
            if (wreck.fits(level, at, facing)) {
                wreck.placeAll(level, at, facing, 2);
                return true;
            }
        }
        return false;
    }

    private static WreckBlock pick(Config cfg, RandomSource random) {
        int total = cfg.car() + cfg.truck() + cfg.bus();
        if (total <= 0) {
            return null;
        }
        int r = random.nextInt(total);
        if (r < cfg.car()) {
            return (WreckBlock) ModWorldBlocks.CAR_WRECK.get();
        }
        return (WreckBlock) (r < cfg.car() + cfg.truck() ? ModWorldBlocks.TRUCK_WRECK : ModWorldBlocks.BUS_WRECK).get();
    }
}
