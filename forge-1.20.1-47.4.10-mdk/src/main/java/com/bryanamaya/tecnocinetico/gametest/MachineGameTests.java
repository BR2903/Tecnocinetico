package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.machine.ElectricMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Pruebas de las máquinas eléctricas: refinar, cargar y fabricar munición con energía. */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class MachineGameTests {

    private static ElectricMachineBlockEntity place(GameTestHelper helper, Block block) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, block);
        ElectricMachineBlockEntity be = (ElectricMachineBlockEntity) helper.getBlockEntity(pos);
        for (int i = 0; i < 40; i++) {
            be.getEnergy().receiveEnergy(2_000, false);
        }
        return be;
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void refinerMakesDust(GameTestHelper helper) {
        ElectricMachineBlockEntity be = place(helper, ModBlocks.ELECTRIC_REFINER.get());
        be.getItems().insertItem(0, new ItemStack(ModItems.RAW_LEAD.get(), 1), false);
        helper.assertTrue(be.getItems().insertItem(0, new ItemStack(Items.DIRT), true).getCount() == 1, "No debe aceptar tierra");
        helper.succeedWhen(() -> {
            ItemStack out = be.getItems().getStackInSlot(1);
            helper.assertTrue(out.is(ModItems.LEAD_DUST.get()) && out.getCount() == 2, "Esperaba 2 polvos de plomo, hay " + out);
            helper.assertTrue(be.getItems().getStackInSlot(0).isEmpty(), "La mena debería haberse gastado");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void chargerFillsCellsAndGadgets(GameTestHelper helper) {
        ElectricMachineBlockEntity be = place(helper, ModBlocks.CELL_CHARGER.get());
        be.getItems().insertItem(0, new ItemStack(ModItems.EMPTY_PULSE_CELL.get()), false);
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(be.getItems().getStackInSlot(1).is(ModItems.PULSE_CELL.get()), "La célula debería estar cargada");
            be.getItems().extractItem(1, 64, false);
            ItemStack visor = new ItemStack(ModItems.NIGHT_VISION_VISOR.get());
            visor.setDamageValue(200);
            be.getItems().insertItem(0, visor, false);
        });
        helper.runAfterDelay(160, () -> {
            ItemStack out = be.getItems().getStackInSlot(1);
            helper.assertTrue(out.is(ModItems.NIGHT_VISION_VISOR.get()) && out.getDamageValue() == 0, "El visor debería salir cargado: " + out);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void fabricatorMakesAmmo(GameTestHelper helper) {
        ElectricMachineBlockEntity be = place(helper, ModBlocks.AMMO_FABRICATOR.get());
        be.getItems().insertItem(0, new ItemStack(ModItems.CINETRITE_SHARD.get(), 2), false);
        be.getItems().insertItem(1, new ItemStack(ModItems.LEAD_INGOT.get()), false);
        be.getItems().insertItem(2, new ItemStack(Items.COPPER_INGOT), false);
        helper.succeedWhen(() -> {
            ItemStack out = be.getItems().getStackInSlot(3);
            helper.assertTrue(out.is(ModItems.KINETIC_ROUND.get()) && out.getCount() == 8, "Esperaba 8 balas cinéticas, hay " + out);
        });
    }
}
