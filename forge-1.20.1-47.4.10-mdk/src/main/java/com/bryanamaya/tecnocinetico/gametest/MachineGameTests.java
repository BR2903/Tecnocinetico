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
        // sin energía: mal ajustada no pierde estabilidad ni desintegra la carga
        BlockPos dark = new BlockPos(3, 2, 1);
        helper.setBlock(dark, ModBlocks.REACTION_CHAMBER.get());
        var unpowered = (com.bryanamaya.tecnocinetico.machine.ReactionChamberBlockEntity) helper.getBlockEntity(dark);
        unpowered.configure(1, 1);
        unpowered.getItems().insertItem(0, new ItemStack(ModItems.RAW_TITANIUM.get(), 1), false);
        helper.runAfterDelay(380, () -> helper.assertTrue(unpowered.getItems().getStackInSlot(0).getCount() == 1,
                "Sin energía la carga no debería desintegrarse"));
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
        });
        helper.runAfterDelay(390, helper::succeed);
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void uraniumIsRadioactive(GameTestHelper helper) {
        var player = helper.makeMockPlayer();
        player.getInventory().add(new ItemStack(ModItems.URANIUM_INGOT.get(), 10));
        float dose = com.bryanamaya.tecnocinetico.effect.RadiationEvents.exposure(player);
        helper.assertTrue(dose >= com.bryanamaya.tecnocinetico.effect.RadiationEvents.LEVEL_2, "10 lingotes deberían dar radiación nivel II: " + dose);
        // el traje completo anula la radiación
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(ModItems.HAZMAT_HELMET.get()));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(ModItems.HAZMAT_CHESTPLATE.get()));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(ModItems.HAZMAT_LEGGINGS.get()));
        helper.assertTrue(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.hazmatPieces(player) == 3, "3 piezas puestas");
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(ModItems.HAZMAT_BOOTS.get()));
        helper.assertTrue(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.hasFullSet(player,
                com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.HAZMAT), "Traje completo");
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
    public static void reactorGeneratesAndStaysCool(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, ModBlocks.FISSION_REACTOR.get());
        var reactor = (com.bryanamaya.tecnocinetico.machine.FissionReactorBlockEntity) helper.getBlockEntity(pos);
        reactor.coolantTank().fill(new net.minecraftforge.fluids.FluidStack(com.bryanamaya.tecnocinetico.fluid.ModFluids.COOLANT.get(), 8000),
                net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        reactor.getItems().insertItem(0, new ItemStack(ModItems.URANIUM_FUEL_ROD.get()), false);
        reactor.setThrottle(100);
        helper.assertTrue(reactor.getItems().insertItem(0, new ItemStack(Items.COAL), true).getCount() == 1, "Solo acepta barras de uranio");
        helper.runAfterDelay(120, () -> {
            int e = reactor.getEnergy().getEnergyStored();
            helper.assertTrue(e > 50_000, "Al 100 % debería haber generado ~70.000 FE: " + e);
            helper.assertTrue(reactor.heat() < 100, "Con refrigerante no debería calentarse: " + reactor.heat());
            helper.assertTrue(reactor.getEnergy().receiveEnergy(1000, true) == 0 || reactor.getCapability(
                    net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY).map(s -> s.receiveEnergy(1000, true)).orElse(0) == 0,
                    "No se le puede meter energía al reactor");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void universalCellHoldsAnyFluid(GameTestHelper helper) {
        var exec = net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
        for (var fluid : java.util.List.of(net.minecraft.world.level.material.Fluids.WATER, net.minecraft.world.level.material.Fluids.LAVA,
                com.bryanamaya.tecnocinetico.fluid.ModFluids.SULFURIC_ACID.get(), com.bryanamaya.tecnocinetico.fluid.ModFluids.COOLANT.get())) {
            ItemStack cell = new ItemStack(com.bryanamaya.tecnocinetico.fluid.ModFluids.UNIVERSAL_CELL.get());
            var h = net.minecraftforge.fluids.FluidUtil.getFluidHandler(cell).orElseThrow(IllegalStateException::new);
            int in = h.fill(new net.minecraftforge.fluids.FluidStack(fluid, 5000), exec);
            helper.assertTrue(in == 1000, "Una celda guarda 1 cubo: " + in);
            var out = net.minecraftforge.fluids.FluidUtil.getFluidContained(cell).orElse(net.minecraftforge.fluids.FluidStack.EMPTY);
            helper.assertTrue(out.getFluid().isSame(fluid) && out.getAmount() == 1000, "Debería contener " + fluid);
        }
        // de una celda al laboratorio y de vuelta
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, ModBlocks.CHEMICAL_LAB.get());
        var lab = (com.bryanamaya.tecnocinetico.machine.ChemicalLabBlockEntity) helper.getBlockEntity(pos);
        ItemStack water = new ItemStack(com.bryanamaya.tecnocinetico.fluid.ModFluids.UNIVERSAL_CELL.get());
        net.minecraftforge.fluids.FluidUtil.getFluidHandler(water).ifPresent(h -> h.fill(new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), exec));
        var res = net.minecraftforge.fluids.FluidUtil.tryEmptyContainer(water, lab.fluidHandler(), 1000, null, true);
        helper.assertTrue(res.isSuccess() && lab.inputTank().getFluidAmount() == 1000, "La celda debería vaciarse en el laboratorio");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void assemblerDeploysBuildsAndDismantles(GameTestHelper helper) {
        var t = com.bryanamaya.tecnocinetico.structure.Structures.ASSEMBLER;
        BlockPos origin = helper.absolutePos(new BlockPos(1, 2, 1));
        var level = helper.getLevel();
        helper.assertTrue(t.firstObstacle(level, origin, net.minecraft.core.Direction.SOUTH) == null, "El espacio debería estar libre");
        BlockPos core = t.place(level, origin, net.minecraft.core.Direction.SOUTH,
                s -> s.setValue(com.bryanamaya.tecnocinetico.machine.ElectricMachineBlock.FACING, net.minecraft.core.Direction.NORTH));
        helper.assertTrue(t.occupied(origin, net.minecraft.core.Direction.SOUTH).size() == 27, "3x3x3 = 27 bloques");
        var be = (com.bryanamaya.tecnocinetico.machine.IndustrialAssemblerBlockEntity) level.getBlockEntity(core);
        // la energía entra por cualquier pieza (la pieza la pasa al núcleo)
        BlockPos part = origin;
        var cap = level.getBlockEntity(part).getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY, null).orElse(null);
        helper.assertTrue(cap != null && cap.receiveEnergy(5000, false) == 5000, "Las piezas deberían pasar energía al núcleo");
        for (int i = 0; i < 60; i++) {
            be.getEnergy().receiveEnergy(10_000, false);
        }
        var recipe = com.bryanamaya.tecnocinetico.machine.MachineRecipes.ASSEMBLY.get(0);
        for (int i = 0; i < recipe.in().size(); i++) {
            var ing = recipe.in().get(i);
            be.getItems().insertItem(i, new ItemStack(ing.item().get(), ing.count()), false);
        }
        helper.assertTrue(be.currentResult().is(ModItems.FISSION_REACTOR_KIT.get()), "Debería estar fabricando el kit del reactor");
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(be.progress() > 0, "Debería avanzar");
            // romper una pieza desmonta todo y suelta el kit
            level.destroyBlock(part, false);
            helper.assertTrue(level.getBlockState(core).isAir(), "El núcleo debería desaparecer");
            for (BlockPos p : t.occupied(origin, net.minecraft.core.Direction.SOUTH)) {
                helper.assertTrue(level.getBlockState(p).isAir(), "Queda una pieza en " + p);
            }
            var kits = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(core).inflate(2),
                    e -> e.getItem().is(ModItems.INDUSTRIAL_ASSEMBLER_KIT.get()));
            helper.assertTrue(kits.size() == 1, "Debería soltar el kit");
            kits.forEach(net.minecraft.world.entity.Entity::discard);
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(core).inflate(2))
                    .forEach(net.minecraft.world.entity.Entity::discard);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200, batch = "reactor")   // aparte: ocupa 9x9x6
    public static void reactorStructureDeploys(GameTestHelper helper) {
        var t = com.bryanamaya.tecnocinetico.structure.Structures.REACTOR;
        var level = helper.getLevel();
        var dir = net.minecraft.core.Direction.EAST;
        BlockPos origin = helper.absolutePos(new BlockPos(0, 2, 0));
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-1, 0, -10), origin.offset(10, 6, 10))) {
            level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
        helper.assertTrue(t.firstObstacle(level, origin, dir) == null, "El espacio 9x9x6 debería estar libre");
        BlockPos core = t.place(level, origin, dir, s -> s.setValue(com.bryanamaya.tecnocinetico.machine.ElectricMachineBlock.FACING, dir.getOpposite()));
        int parts = 0;
        for (BlockPos p : t.occupied(origin, dir)) {
            if (level.getBlockState(p).getBlock() instanceof com.bryanamaya.tecnocinetico.structure.StructurePartBlock) {
                parts++;
            }
        }
        helper.assertTrue(parts > 250, "Deberían colocarse cientos de piezas: " + parts);
        var reactor = (com.bryanamaya.tecnocinetico.machine.FissionReactorBlockEntity) level.getBlockEntity(core);
        // el refrigerante entra por una pieza cualquiera del muro
        BlockPos wall = com.bryanamaya.tecnocinetico.structure.MultiblockTemplate.toWorld(origin, dir, 0, 2, 0);
        var fluid = level.getBlockEntity(wall).getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, null).orElse(null);
        helper.assertTrue(fluid != null && fluid.fill(new net.minecraftforge.fluids.FluidStack(com.bryanamaya.tecnocinetico.fluid.ModFluids.COOLANT.get(), 4000),
                net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 4000, "El muro debería pasar el refrigerante al núcleo");
        helper.assertTrue(reactor.coolantTank().getFluidAmount() == 4000, "El núcleo debería tener el refrigerante");
        // una máquina pegada por FUERA al muro recibe la energía que genera
        BlockPos outside = com.bryanamaya.tecnocinetico.structure.MultiblockTemplate.toWorld(origin, dir, 0, 2, -1);
        level.setBlock(outside, ModBlocks.CELL_CHARGER.get().defaultBlockState(), 3);
        reactor.getItems().insertItem(0, new ItemStack(ModItems.URANIUM_FUEL_ROD.get()), false);
        reactor.setThrottle(100);
        helper.runAfterDelay(60, () -> {
            var charger = (com.bryanamaya.tecnocinetico.machine.ElectricMachineBlockEntity) level.getBlockEntity(outside);
            helper.assertTrue(charger.getEnergy().getEnergyStored() > 10_000, "La máquina de fuera debería recibir energía: "
                    + charger.getEnergy().getEnergyStored());
            level.setBlock(outside, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        // romper el muro desmonta el reactor entero y da el kit
        level.destroyBlock(wall, false);
        helper.assertTrue(level.getBlockState(core).isAir(), "El núcleo debería desaparecer");
        var kits = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(core).inflate(3),
                e -> e.getItem().is(ModItems.FISSION_REACTOR_KIT.get()));
        helper.assertTrue(kits.size() == 1, "Debería soltar el kit del reactor");
        level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(core).inflate(6))
                .forEach(net.minecraft.world.entity.Entity::discard);
        helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void reactionChamberDeploys(GameTestHelper helper) {
        var t = com.bryanamaya.tecnocinetico.structure.Structures.CHAMBER;
        var level = helper.getLevel();
        var dir = net.minecraft.core.Direction.NORTH;
        BlockPos origin = helper.absolutePos(new BlockPos(1, 2, 2));
        BlockPos core = t.place(level, origin, dir, s -> s.setValue(com.bryanamaya.tecnocinetico.machine.ElectricMachineBlock.FACING, dir.getOpposite()));
        helper.assertTrue(t.occupied(origin, dir).size() == 8, "2x2x2 = 8 bloques");
        BlockPos top = com.bryanamaya.tecnocinetico.structure.MultiblockTemplate.toWorld(origin, dir, 1, 1, 1);
        var cap = level.getBlockEntity(top).getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY, null).orElse(null);
        helper.assertTrue(cap != null && cap.receiveEnergy(3000, false) == 3000, "La energía debería entrar por cualquier pieza");
        helper.assertTrue(((com.bryanamaya.tecnocinetico.machine.ElectricMachineBlockEntity) level.getBlockEntity(core)).getEnergy().getEnergyStored() == 3000,
                "El núcleo debería tenerla");
        level.destroyBlock(top, false);
        helper.assertTrue(level.getBlockState(core).isAir(), "Romper una pieza desmonta la cámara");
        level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(core).inflate(3))
                .forEach(net.minecraft.world.entity.Entity::discard);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void fabricatorMakesAmmo(GameTestHelper helper) {
        ElectricMachineBlockEntity be = place(helper, ModBlocks.AMMO_FABRICATOR.get());
        be.getItems().insertItem(0, new ItemStack(ModItems.CINETRITE_SHARD.get(), 2), false);
        be.getItems().insertItem(1, new ItemStack(ModItems.LEAD_INGOT.get()), false);
        be.getItems().insertItem(2, new ItemStack(Items.COPPER_INGOT), false);
        helper.succeedWhen(() -> {
            ItemStack out = be.getItems().getStackInSlot(3);
            helper.assertTrue(out.is(ModItems.KINETIC_ROUND.get()) && out.getCount() == 2, "Esperaba 2 paquetes de balas cinéticas, hay " + out);
        });
    }
}
