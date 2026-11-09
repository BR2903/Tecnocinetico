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

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void reactionChamberOnlyWhenTuned(GameTestHelper helper) {
        // el titanio y el uranio no se funden ni se refinan
        var furnace = helper.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,
                new net.minecraft.world.SimpleContainer(new ItemStack(ModItems.RAW_TITANIUM.get())), helper.getLevel());
        helper.assertTrue(furnace.isEmpty(), "El titanio no debería fundirse en el horno");
        helper.assertTrue(com.bryanamaya.tecnocinetico.machine.MachineRecipes.refine(new ItemStack(ModItems.RAW_URANIUM.get())) == null,
                "El uranio no debería refinarse");
        ElectricMachineBlockEntity be = place(helper, ModBlocks.REACTION_CHAMBER.get());
        var chamber = (com.bryanamaya.tecnocinetico.machine.ReactionChamberBlockEntity) be;
        chamber.configure(1, 1);                                              // mal ajustada: no avanza
        be.getItems().insertItem(0, new ItemStack(ModItems.RAW_TITANIUM.get(), 1), false);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(be.getItems().getStackInSlot(1).isEmpty(), "Mal ajustada no debería producir nada");
            chamber.configure(4, 6);                                          // resonancia del titanio en bruto
        });
        helper.runAfterDelay(250, () -> {
            ItemStack out = be.getItems().getStackInSlot(1);
            helper.assertTrue(out.is(ModItems.TITANIUM_INGOT.get()), "Bien ajustada debería dar un lingote de titanio: " + out);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void uraniumIsRadioactive(GameTestHelper helper) {
        var player = helper.makeMockPlayer();
        player.getInventory().add(new ItemStack(ModItems.URANIUM_INGOT.get(), 10));
        float dose = com.bryanamaya.tecnocinetico.effect.RadiationEvents.exposure(player);
        helper.assertTrue(dose >= com.bryanamaya.tecnocinetico.effect.RadiationEvents.LEVEL_2, "10 lingotes deberían dar radiación nivel II: " + dose);
        var pig = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.PIG, new BlockPos(1, 2, 1));
        pig.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.bryanamaya.tecnocinetico.effect.ModEffects.RADIATION.get(), 100, 2));
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "La radiación debería dañar");
            pig.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void chemicalLabMakesAcidAndLeaches(GameTestHelper helper) {
        var lab = (com.bryanamaya.tecnocinetico.machine.ChemicalLabBlockEntity) place(helper, ModBlocks.CHEMICAL_LAB.get());
        var acid = com.bryanamaya.tecnocinetico.fluid.ModFluids.SULFURIC_ACID.get();
        // por tubería: entra agua al tanque de entrada; con pólvora sale ácido al de salida
        int in = lab.fluidHandler().fill(new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 2000),
                net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(in == 2000, "El tanque debería aceptar agua: " + in);
        helper.assertTrue(lab.fluidHandler().fill(new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA, 1000),
                net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE) == 0, "No debe aceptar lava");
        lab.getItems().insertItem(0, new ItemStack(Items.GUNPOWDER, 2), false);
        helper.runAfterDelay(110, () -> {
            helper.assertTrue(lab.outputTank().getFluid().getFluid().isSame(acid) && lab.outputTank().getFluidAmount() == 1000,
                    "Debería haber 1000 mB de ácido: " + lab.outputTank().getFluid().getAmount());
            // botones: vaciar la entrada (queda agua) e intercambiar -> el ácido pasa a la entrada para lixiviar plomo
            lab.onButton(com.bryanamaya.tecnocinetico.machine.ChemicalLabBlockEntity.BTN_CLEAR_IN);
            helper.assertTrue(lab.inputTank().isEmpty(), "El botón de vaciar debería dejar la entrada vacía");
            lab.onButton(com.bryanamaya.tecnocinetico.machine.ChemicalLabBlockEntity.BTN_SWAP);
            helper.assertTrue(lab.outputTank().isEmpty() && lab.inputTank().getFluidAmount() == 1000, "El intercambio debería pasar el ácido a la entrada");
            lab.getItems().insertItem(0, new ItemStack(ModItems.RAW_LEAD.get()), false);
        });
        helper.runAfterDelay(250, () -> {
            ItemStack out = lab.getItems().getStackInSlot(2);
            helper.assertTrue(out.is(ModItems.LEAD_DUST.get()) && out.getCount() == 3, "La lixiviación debería dar 3 polvos de plomo: " + out);
            helper.assertTrue(lab.inputTank().getFluidAmount() == 750, "Debería gastar 250 mB de ácido: " + lab.inputTank().getFluidAmount());
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
