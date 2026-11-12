package com.bryanamaya.tecnocinetico.worldgen.feature;

import com.bryanamaya.tecnocinetico.block.ModWorldBlocks;
import com.bryanamaya.tecnocinetico.worldgen.InfernalBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Set;

/**
 * Depósitos del Nether industrial: desde la altura de colocación baja hasta el primer suelo con aire encima y deja
 * una mancha (radio ~3) del material de su bioma sobre la roca del Nether, y además:
 * <ul>
 *   <li>Eriales de Obsidiana y Canteras de Basalto: una columna de obsidiana o basalto (2-6).</li>
 *   <li>Bosque de Carbón: a veces un árbol carbonizado (tronco de basalto, copa de placas carbonizadas, una luz).</li>
 * </ul>
 * Funciona por bioma del mod o por su equivalente vanilla, así que también decora el Nether de mundos ya creados
 * (en chunks nuevos), donde los biomas siguen siendo los vanilla.
 */
public class InfernalDepositFeature extends Feature<NoneFeatureConfiguration> {

    private static final Set<Block> NETHER_ROCK = Set.of(Blocks.NETHERRACK, Blocks.BASALT, Blocks.BLACKSTONE, Blocks.SOUL_SOIL,
            Blocks.SOUL_SAND, Blocks.CRIMSON_NYLIUM, Blocks.WARPED_NYLIUM);
    private static final int MIN_Y = 32;

    public InfernalDepositFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    /** Bioma del mod que corresponde a este (el propio, o el sustituto si es vanilla). */
    private static ResourceKey<Biome> region(Holder<Biome> biome) {
        var key = biome.unwrapKey().orElse(null);
        return key == null ? InfernalBiomes.SLAG_FORGES : InfernalBiomes.MAP.getOrDefault(key, key);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        BlockPos start = ctx.origin();
        for (int y = start.getY(); y > MIN_Y; y--) {
            BlockPos floor = new BlockPos(start.getX(), y, start.getZ());
            if (!level.getBlockState(floor).isSolid() || !level.isEmptyBlock(floor.above())) {
                continue;
            }
            var region = region(level.getBiome(floor));
            Block crust = region == InfernalBiomes.SULFUR_PITS ? ModWorldBlocks.SULFUR_CRUST.get()
                    : region == InfernalBiomes.CARBON_FOREST ? ModWorldBlocks.CARBONIZED_PLATE.get() : ModWorldBlocks.COMPACTED_SLAG.get();
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos at = floor.offset(x, 0, z);
                    if (x * x + z * z < 10 && random.nextInt(4) != 0 && NETHER_ROCK.contains(level.getBlockState(at).getBlock())
                            && level.isEmptyBlock(at.above())) {
                        level.setBlock(at, crust.defaultBlockState(), 2);
                    }
                }
            }
            if (region == InfernalBiomes.OBSIDIAN_WASTES || region == InfernalBiomes.BASALT_QUARRIES) {
                Block column = region == InfernalBiomes.BASALT_QUARRIES ? Blocks.BASALT : Blocks.OBSIDIAN;
                int height = 2 + random.nextInt(5);
                for (int h = 1; h <= height && level.isEmptyBlock(floor.above(h)); h++) {
                    level.setBlock(floor.above(h), column.defaultBlockState(), 2);
                }
            } else if (region == InfernalBiomes.CARBON_FOREST && random.nextInt(3) == 0) {
                carbonTree(level, random, floor);
            }
            return true;
        }
        return false;
    }

    private static void carbonTree(WorldGenLevel level, RandomSource random, BlockPos floor) {
        int height = 4 + random.nextInt(3);
        for (int h = 1; h <= height + 1; h++) {
            if (!level.isEmptyBlock(floor.above(h))) {
                return;
            }
        }
        for (int h = 1; h <= height; h++) {
            level.setBlock(floor.above(h), Blocks.BASALT.defaultBlockState(), 2);
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos branch = floor.offset(x, height, z);
                if (Math.abs(x) + Math.abs(z) <= 3 && level.isEmptyBlock(branch)) {
                    level.setBlock(branch, ModWorldBlocks.CARBONIZED_PLATE.get().defaultBlockState(), 2);
                }
            }
        }
        level.setBlock(floor.above(height + 1), ModWorldBlocks.CARBONIZED_PLATE.get().defaultBlockState(), 2);
        level.setBlock(floor.offset(1, height - 1, 0), Blocks.SHROOMLIGHT.defaultBlockState(), 2);
    }
}
