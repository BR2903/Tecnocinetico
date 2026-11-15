package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.end.NavigationCoreItem;
import com.bryanamaya.tecnocinetico.end.NetworkBlocks;
import com.bryanamaya.tecnocinetico.gear.JetpackItem;
import com.bryanamaya.tecnocinetico.gear.ThrusterElytraItem;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.worldgen.structure.EndSitePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.Set;

/** End, parte 4: estructuras de la red de la IA, el Puerto de Lanzamiento, el Núcleo de Navegación y los Élitros de Propulsión. */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class EndStructureGameTests {

    private static ServerLevel end(GameTestHelper h) {
        ServerLevel s = h.getLevel().getServer().getLevel(Level.END);
        h.assertTrue(s != null, "Falta el End");
        return s;
    }

    /** Genera la estructura en el End probando chunks desde {@code base}; la coloca entera y devuelve su caja. */
    private static BoundingBox place(GameTestHelper h, ServerLevel end, String id, int baseX, int baseZ) {
        var access = end.registryAccess();
        var structure = access.registryOrThrow(Registries.STRUCTURE).get(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, id));
        h.assertTrue(structure != null, "Falta la estructura " + id);
        var biome = access.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.END_HIGHLANDS);
        var gen = end.getChunkSource().getGenerator();
        StructureStart start = StructureStart.INVALID_START;
        for (int tries = 0; tries < 400 && !start.isValid(); tries++) {
            var cp = new ChunkPos(new BlockPos(baseX + (tries % 20) * 48, 0, baseZ + (tries / 20) * 48));
            start = structure.generate(access, gen, new FixedBiomeSource(biome), end.getChunkSource().randomState(),
                    end.getStructureManager(), end.getSeed(), cp, 0, end, b -> true);
        }
        h.assertTrue(start.isValid(), "No se encontró isla para " + id);
        var bb = start.getBoundingBox();
        StructureStart placed = start;
        ChunkPos.rangeClosed(new ChunkPos(bb.minX() >> 4, bb.minZ() >> 4), new ChunkPos(bb.maxX() >> 4, bb.maxZ() >> 4)).forEach(c ->
                placed.placeInChunk(end, end.structureManager(), gen, end.getRandom(), new BoundingBox(c.getMinBlockX(), end.getMinBuildHeight(),
                        c.getMinBlockZ(), c.getMaxBlockX(), end.getMaxBuildHeight(), c.getMaxBlockZ()), c));
        Tecnocinetico.LOGGER.info("[GameTest] {} generado en el End en {}", id, bb);
        WorldgenGameTests.dump(end, bb, id);                                     // para tools/render_dump.py
        return bb;
    }

    private static int count(ServerLevel end, BoundingBox bb, net.minecraft.world.level.block.Block block) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
            if (end.getBlockState(p).is(block)) {
                n++;
            }
        }
        return n;
    }

    /** El Nodo y la Granja se generan sobre islas exteriores con racks, terminales y cofres. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void endNetworkSitesGenerate(GameTestHelper h) {
        ServerLevel end = end(h);
        int base = 6000 + end.getRandom().nextInt(200) * 512;                 // zona nueva en cada ejecución
        BoundingBox node = place(h, end, "end_network_node", base, 6000);
        h.assertTrue(count(end, node, ModBlocks.SERVER_RACK.get()) >= 4 && count(end, node, ModBlocks.NETWORK_TERMINAL.get()) == 1
                && count(end, node, Blocks.CHEST) == 1, "Nodo de Datos incompleto");
        BoundingBox farm = place(h, end, "end_server_farm", base + 3000, 6000);
        h.assertTrue(count(end, farm, ModBlocks.SERVER_RACK.get()) >= 40 && count(end, farm, Blocks.CHEST) == 3
                && count(end, farm, ModBlocks.NETWORK_PLATING.get()) > 200, "Granja de Servidores incompleta");
        h.succeed();
    }

    /** El Puerto: isla propia con el cráter, los cuatro registros del despegue y sus cofres; es único (un solo anillo). */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void launchPortIsUniqueWithCrater(GameTestHelper h) {
        ServerLevel end = end(h);
        var set = end.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "launch_port"));
        h.assertTrue(set != null && set.placement() instanceof ConcentricRingsStructurePlacement rings && rings.count() == 1,
                "El Puerto de Lanzamiento debería ser único (anillo de 1)");
        BoundingBox bb = place(h, end, "launch_port", 30000 + end.getRandom().nextInt(200) * 512, -30000);
        BlockPos center = new BlockPos(bb.minX() + EndSitePiece.size(2) / 2, bb.minY() + 34, bb.minZ() + EndSitePiece.size(2) / 2);
        h.assertTrue(end.getBlockState(center.below(5)).isAir() && end.getBlockState(center.above(1)).isAir(), "No hay cráter en el centro");
        var floor = end.getBlockState(center.below(12));
        h.assertTrue(floor.is(Blocks.OBSIDIAN) || floor.is(Blocks.CRYING_OBSIDIAN), "El fondo del cráter no está fundido: " + floor);
        h.assertTrue(end.getBlockState(center.offset(30, 0, 30)).is(Blocks.END_STONE) || end.getBlockState(center.offset(30, 0, 30))
                .is(ModBlocks.CORRUPTED_END_STONE.get()), "El Puerto no tiene su isla");
        Set<Integer> entries = new HashSet<>();
        for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
            var st = end.getBlockState(p);
            if (st.is(ModBlocks.NETWORK_TERMINAL.get())) {
                entries.add(st.getValue(NetworkBlocks.Terminal.ENTRY));
            }
        }
        h.assertTrue(entries.containsAll(Set.of(4, 5, 6, 7)), "Faltan registros del despegue: " + entries);
        h.assertTrue(count(end, bb, Blocks.CHEST) >= 3, "El Puerto tiene pocos cofres");
        h.assertTrue("north".equals(NavigationCoreItem.heading(new Vec3(0, 0, -1))) && "east".equals(NavigationCoreItem.heading(new Vec3(1, 0, 0)))
                && "southwest".equals(NavigationCoreItem.heading(new Vec3(-1, 0, 1))), "Rumbo del Núcleo de Navegación mal calculado");
        h.succeed();
    }

    /** Élitros + Jetpack Mk III + núcleo + nulita + acopladores = Élitros de Propulsión (y no con un Jetpack Mk II). */
    @GameTest(template = "empty")
    public static void thrusterElytraRecipe(GameTestHelper h) {
        var grid = new TransientCraftingContainer(new AbstractContainerMenu((MenuType<?>) null, -1) {
            @Override
            public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p, int i) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(net.minecraft.world.entity.player.Player p) {
                return true;
            }
        }, 3, 3);
        ItemStack jet = new ItemStack(ModItems.JETPACK.get());
        JetpackItem.setLevel(jet, 3);
        jet.setDamageValue(jet.getMaxDamage() / 2);
        ItemStack elytra = new ItemStack(Items.ELYTRA);
        elytra.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 3);
        ItemStack[] in = {new ItemStack(ModItems.NULLITE_INGOT.get()), new ItemStack(ModItems.CRUDE_KINETIC_COUPLER.get()),
                new ItemStack(ModItems.NULLITE_INGOT.get()), elytra, jet, new ItemStack(ModItems.DATA_CORE.get()),
                new ItemStack(ModItems.NULLITE_INGOT.get()), new ItemStack(ModItems.CRUDE_KINETIC_COUPLER.get()), new ItemStack(ModItems.NULLITE_INGOT.get())};
        for (int i = 0; i < 9; i++) {
            grid.setItem(i, in[i]);
        }
        var recipe = new ThrusterElytraItem.Recipe(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "test"),
                net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT);
        h.assertTrue(recipe.matches(grid, h.getLevel()), "La receta no reconoce los ingredientes");
        ItemStack out = recipe.assemble(grid, h.getLevel().registryAccess());
        int fuel = ThrusterElytraItem.fuel(out);
        h.assertTrue(out.is(ModItems.THRUSTER_ELYTRA.get()) && fuel > ThrusterElytraItem.MAX_FUEL / 3 && fuel < ThrusterElytraItem.MAX_FUEL * 2 / 3
                && out.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING) == 3,
                "Resultado incorrecto: " + out + " combustible " + fuel);
        h.assertTrue(ThrusterElytraItem.canThrust(out), "Con combustible debería poder empujar");
        Vec3 v = ThrusterElytraItem.boost(Vec3.ZERO, new Vec3(0, 0, 1));
        h.assertTrue(v.z > 0.1 && Math.abs(v.x) < 1e-9, "El empuje no va hacia donde se mira: " + v);
        JetpackItem.setLevel(jet, 2);
        h.assertFalse(recipe.matches(grid, h.getLevel()), "Con un Jetpack Mk II no debería valer");
        h.succeed();
    }
}
