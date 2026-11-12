package com.bryanamaya.tecnocinetico.block;

import com.bryanamaya.tecnocinetico.block.custom.PileBlock;
import net.minecraft.world.level.material.PushReaction;
import com.bryanamaya.tecnocinetico.block.custom.AshLayerBlock;
import com.bryanamaya.tecnocinetico.block.custom.RoadLineBlock;
import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SandBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Bloques del mundo post-apocalíptico (fase 1 de docs/PLAN_MUNDO.md).
 * Los modelos, texturas, loot, tags, recetas y textos los genera tools/world_blocks_gen.py.
 */
public final class ModWorldBlocks {

    /** Todos los bloques de esta clase, en orden, para la pestaña creativa "Mundo". */
    public static final List<RegistryObject<Block>> ALL = new ArrayList<>();

    // ------------------------------------------------------------------ hormigón (3 estados, cada uno con familia)
    public static final RegistryObject<Block> CONCRETE = block("concrete", () -> new Block(concrete()));
    public static final RegistryObject<Block> CONCRETE_SLAB = slab("concrete_slab", CONCRETE);
    public static final RegistryObject<Block> CONCRETE_STAIRS = stairs("concrete_stairs", CONCRETE);
    public static final RegistryObject<Block> CONCRETE_WALL = wall("concrete_wall", CONCRETE);

    public static final RegistryObject<Block> CRACKED_CONCRETE = block("cracked_concrete", () -> new Block(concrete()));
    public static final RegistryObject<Block> CRACKED_CONCRETE_SLAB = slab("cracked_concrete_slab", CRACKED_CONCRETE);
    public static final RegistryObject<Block> CRACKED_CONCRETE_STAIRS = stairs("cracked_concrete_stairs", CRACKED_CONCRETE);
    public static final RegistryObject<Block> CRACKED_CONCRETE_WALL = wall("cracked_concrete_wall", CRACKED_CONCRETE);

    public static final RegistryObject<Block> MOSSY_CONCRETE = block("mossy_concrete", () -> new Block(concrete()));
    public static final RegistryObject<Block> MOSSY_CONCRETE_SLAB = slab("mossy_concrete_slab", MOSSY_CONCRETE);
    public static final RegistryObject<Block> MOSSY_CONCRETE_STAIRS = stairs("mossy_concrete_stairs", MOSSY_CONCRETE);
    public static final RegistryObject<Block> MOSSY_CONCRETE_WALL = wall("mossy_concrete_wall", MOSSY_CONCRETE);

    // ------------------------------------------------------------------ escombros
    public static final RegistryObject<Block> RUBBLE = block("rubble",
            () -> new PileBlock(Properties.copy(Blocks.GRAVEL).mapColor(MapColor.STONE).strength(0.4F)
                    .sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> DEBRIS_LAYER = block("debris_layer",
            () -> new CarpetBlock(Properties.copy(Blocks.GRAVEL).strength(0.2F).noOcclusion()));

    // ------------------------------------------------------------------ asfalto
    public static final RegistryObject<Block> ASPHALT = block("asphalt", () -> new Block(asphalt()));
    public static final RegistryObject<Block> CRACKED_ASPHALT = block("cracked_asphalt", () -> new Block(asphalt()));
    public static final RegistryObject<Block> ASPHALT_ROAD_LINE = block("asphalt_road_line", () -> new RoadLineBlock(asphalt()));

    // ------------------------------------------------------------------ metal oxidado
    public static final RegistryObject<Block> RUSTED_METAL_SHEET = block("rusted_metal_sheet", () -> new Block(metal()));
    public static final RegistryObject<Block> RUSTED_METAL_BEAM = block("rusted_metal_beam", () -> new RotatedPillarBlock(metal()));
    public static final RegistryObject<Block> RUSTED_GRATE = block("rusted_grate", () -> new Block(metal()
            .noOcclusion()
            .isValidSpawn((s, l, p, e) -> false)
            .isRedstoneConductor((s, l, p) -> false)
            .isSuffocating((s, l, p) -> false)
            .isViewBlocking((s, l, p) -> false)));

    // ------------------------------------------------------------------ chatarra
    public static final RegistryObject<Block> SCRAP_PILE = block("scrap_pile", () -> new PileBlock(Properties.of()
            .mapColor(MapColor.TERRACOTTA_BROWN).strength(1.0F, 3.0F).sound(SoundType.CHAIN).requiresCorrectToolForDrops()
            .noOcclusion().pushReaction(PushReaction.DESTROY)));

    // ------------------------------------------------------------------ ceniza
    public static final RegistryObject<Block> ASH_BLOCK = block("ash_block",
            () -> new SandBlock(0x9A9894, Properties.copy(Blocks.SAND).mapColor(MapColor.COLOR_LIGHT_GRAY).sound(SoundType.SAND)));
    public static final RegistryObject<Block> ASH_LAYER = block("ash_layer",
            () -> new AshLayerBlock(Properties.copy(Blocks.SNOW).mapColor(MapColor.COLOR_LIGHT_GRAY).sound(SoundType.SAND)));

    // ------------------------------------------------------------------ vitrificado
    public static final RegistryObject<Block> VITRIFIED_SAND = block("vitrified_sand", () -> new Block(Properties.of()
            .mapColor(MapColor.SAND).strength(1.5F, 3.0F).sound(SoundType.GLASS).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> VITRIFIED_GLASS = block("vitrified_glass", () -> new GlassBlock(
            Properties.copy(Blocks.GLASS).mapColor(MapColor.COLOR_ORANGE)));

    // ------------------------------------------------------------------ cinetrita
    public static final RegistryObject<Block> CINETRITE_CLUSTER = block("cinetrite_cluster", () -> new AmethystClusterBlock(7, 3,
            Properties.copy(Blocks.AMETHYST_CLUSTER).mapColor(MapColor.COLOR_CYAN).lightLevel(s -> 6)));

    // ------------------------------------------------------------------ enjambre
    /** Panal de Enjambre: material de las colmenas de las Avispas del Enjambre. */
    public static final RegistryObject<Block> SWARM_COMB = block("swarm_comb", () -> new Block(Properties.of()
            .mapColor(MapColor.COLOR_BLACK).strength(2.5F, 6.0F).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops()));

    // ------------------------------------------------------------------ madera muerta
    public static final RegistryObject<Block> DEAD_LOG = block("dead_log", () -> new RotatedPillarBlock(
            Properties.copy(Blocks.OAK_LOG).mapColor(MapColor.COLOR_GRAY)));
    public static final RegistryObject<Block> DEAD_PLANKS = block("dead_planks", () -> new Block(
            Properties.copy(Blocks.OAK_PLANKS).mapColor(MapColor.COLOR_GRAY)));

    // ------------------------------------------------------------------ mobiliario urbano destrozado (farolas, postes)
    public static final RegistryObject<Block> STREET_LAMP_POLE = block("street_lamp_pole",
            () -> new com.bryanamaya.tecnocinetico.block.custom.StreetPropBlocks.Pole(metal().noOcclusion(), 4));
    public static final RegistryObject<Block> STREET_LAMP_HEAD = block("street_lamp_head",
            () -> new com.bryanamaya.tecnocinetico.block.custom.StreetPropBlocks.Facing(metal().noOcclusion()));
    public static final RegistryObject<Block> STREET_LAMP_BENT = block("street_lamp_bent",
            () -> new com.bryanamaya.tecnocinetico.block.custom.StreetPropBlocks.Facing(metal().noOcclusion()));
    public static final RegistryObject<Block> UTILITY_POLE = block("utility_pole",
            () -> new com.bryanamaya.tecnocinetico.block.custom.StreetPropBlocks.Pole(
                    Properties.copy(Blocks.OAK_LOG).mapColor(MapColor.COLOR_BROWN).noOcclusion(), 6));
    public static final RegistryObject<Block> UTILITY_CROSSARM = block("utility_crossarm",
            () -> new com.bryanamaya.tecnocinetico.block.custom.StreetPropBlocks.Crossarm(
                    Properties.copy(Blocks.OAK_LOG).mapColor(MapColor.COLOR_BROWN).noOcclusion()));
    public static final RegistryObject<Block> POWER_CABLE = block("power_cable",
            () -> new com.bryanamaya.tecnocinetico.block.custom.StreetPropBlocks.Cable(Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(0.3F).sound(SoundType.WOOL).noOcclusion().noCollission()));

    // ------------------------------------------------------------------ Nether industrial (arte de Codex)
    public static final RegistryObject<Block> COMPACTED_SLAG = block("compacted_slag", () -> new Block(Properties.of()
            .mapColor(MapColor.NETHER).strength(5.0F, 12.0F).sound(SoundType.NETHER_BRICKS).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> SULFUR_CRUST = block("sulfur_crust", () -> new Block(Properties.of()
            .mapColor(MapColor.COLOR_YELLOW).strength(2.0F, 12.0F).sound(SoundType.NETHERRACK).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> CARBONIZED_PLATE = block("carbonized_plate", () -> new Block(Properties.of()
            .mapColor(MapColor.COLOR_BLACK).strength(4.0F, 12.0F).sound(SoundType.BASALT).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> INFERNAL_CASING = block("infernal_casing", () -> new Block(Properties.of()
            .mapColor(MapColor.COLOR_GRAY).strength(8.0F, 12.0F).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops()));

    private ModWorldBlocks() {}

    /** Se llama desde el constructor del mod para que esta clase registre sus bloques a tiempo. */
    public static void init() {}

    // ------------------------------------------------------------------ propiedades comunes
    private static Properties concrete() {
        return Properties.of().mapColor(MapColor.STONE).strength(1.8F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops();
    }

    private static Properties asphalt() {
        return Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.5F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops();
    }

    private static Properties metal() {
        return Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    // ------------------------------------------------------------------ registro
    private static RegistryObject<Block> block(String name, Supplier<Block> factory) {
        RegistryObject<Block> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        ALL.add(block);
        return block;
    }

    private static RegistryObject<Block> slab(String name, RegistryObject<Block> base) {
        return block(name, () -> new SlabBlock(Properties.copy(base.get())));
    }

    private static RegistryObject<Block> stairs(String name, RegistryObject<Block> base) {
        return block(name, () -> new StairBlock(() -> base.get().defaultBlockState(), Properties.copy(base.get())));
    }

    private static RegistryObject<Block> wall(String name, RegistryObject<Block> base) {
        return block(name, () -> new WallBlock(Properties.copy(base.get()).forceSolidOn()));
    }
}
