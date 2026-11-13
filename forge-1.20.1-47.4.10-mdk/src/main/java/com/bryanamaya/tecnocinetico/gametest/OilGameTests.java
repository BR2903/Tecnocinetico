package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.fluid.ModFluids;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.machine.ElectricMachineBlockEntity;
import com.bryanamaya.tecnocinetico.machine.MachineRecipes;
import com.bryanamaya.tecnocinetico.machine.OilPumpBlockEntity;
import com.bryanamaya.tecnocinetico.machine.RefineryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Petróleo: Bomba de Extracción, Refinería (gasolina, brea, envasado) y combustible cinético en el laboratorio. */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class OilGameTests {

    private static void power(ElectricMachineBlockEntity m) {
        for (int i = 0; i < 40; i++) {
            m.getEnergy().receiveEnergy(2_000, false);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void refineryMakesGasoline(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at, ModBlocks.REFINERY.get());
        var r = (RefineryBlockEntity) helper.getBlockEntity(at);
        power(r);
        r.fluidHandler().fill(new FluidStack(ModFluids.CRUDE_OIL.get(), 2000), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(r.fluidHandler().fill(new FluidStack(ModFluids.GASOLINE.get(), 100), IFluidHandler.FluidAction.SIMULATE) == 0,
                "La refinería acepta gasolina en la entrada");
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(r.outputTank().getFluid().getFluid() == ModFluids.GASOLINE.get() && r.outputTank().getFluidAmount() >= 600,
                    "No refina: " + r.outputTank().getFluidAmount() + " mB de gasolina");
            helper.assertTrue(r.getItems().getStackInSlot(2).is(ModItems.TAR.get()), "No suelta brea");
            r.getItems().insertItem(0, new ItemStack(Items.IRON_INGOT, 2), false);
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(r.getItems().getStackInSlot(1).is(ModItems.FUEL_CANISTER.get()), "No envasa Depósitos de Combustible");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void pumpExtractsOil(GameTestHelper helper) {
        BlockPos oil = new BlockPos(2, 1, 2), pump = new BlockPos(1, 4, 1);
        helper.setBlock(oil, ModFluids.CRUDE_OIL.block.get());
        helper.setBlock(oil.below(), Blocks.STONE);
        helper.setBlock(pump, ModBlocks.OIL_PUMP.get());
        var p = (OilPumpBlockEntity) helper.getBlockEntity(pump);
        power(p);
        p.getItems().insertItem(0, new ItemStack(Items.BUCKET), false);
        helper.runAfterDelay(160, () -> {
            boolean pumped = helper.getBlockState(oil).is(Blocks.STONE);
            ItemStack out = p.getItems().getStackInSlot(1);
            helper.assertTrue(pumped, "La bomba no saca el petróleo");
            helper.assertTrue(out.is(ModFluids.CRUDE_OIL.bucket.get()), "La bomba no llena el cubo: " + out);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void kineticFuelRecipe(GameTestHelper helper) {
        var r = MachineRecipes.lab(List.of(new ItemStack(ModItems.CINETRITE_SHARD.get(), 2), ItemStack.EMPTY),
                new FluidStack(ModFluids.GASOLINE.get(), 1000));
        helper.assertTrue(r != null && r.outFluid() != null && r.outFluid().get() == ModFluids.KINETIC_FUEL.get(),
                "Gasolina + cinetrita no da combustible cinético");
        helper.succeed();
    }

    /** Motosierra: tala un árbol natural entero; una columna de troncos sin hojas (casa) solo pierde un bloque. */
    @GameTest(template = "empty")
    public static void chainsawFellsTrees(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos base = new BlockPos(1, 1, 1), house = new BlockPos(3, 1, 1);
        for (int y = 0; y < 5; y++) {
            helper.setBlock(base.above(y), Blocks.OAK_LOG);
            helper.setBlock(house.above(y), Blocks.OAK_LOG);
        }
        helper.setBlock(base.above(5), Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, false));
        var player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack saw = new ItemStack(ModItems.CHAINSAW.get());
        com.bryanamaya.tecnocinetico.item.custom.ChainsawItem.setFuel(saw, 200);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, saw);
        player.gameMode.destroyBlock(helper.absolutePos(base));
        player.gameMode.destroyBlock(helper.absolutePos(house));
        int treeLeft = 0, houseLeft = 0;
        for (int y = 0; y < 5; y++) {
            if (helper.getBlockState(base.above(y)).is(Blocks.OAK_LOG)) treeLeft++;
            if (helper.getBlockState(house.above(y)).is(Blocks.OAK_LOG)) houseLeft++;
        }
        int fuel = com.bryanamaya.tecnocinetico.item.custom.ChainsawItem.fuel(player.getMainHandItem());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(treeLeft == 0, "No tala el árbol entero: quedan " + treeLeft);
        helper.assertTrue(houseLeft == 4, "Tala troncos sin hojas (casa): quedan " + houseLeft);
        helper.assertTrue(fuel == 200 - 6, "Gasolina gastada: " + fuel);
        helper.succeed();
    }
}
