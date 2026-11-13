package com.bryanamaya.tecnocinetico.item;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.block.ModWorldBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Tecnocinetico.MOD_ID);

    /** Pestaña principal: máquinas, herramientas, gadgets (por tier), la nave y las criaturas. */
    public static final RegistryObject<CreativeModeTab> TECNOCINETICO_TAB = CREATIVE_MODE_TABS.register("tecnocinetico_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModBlocks.AUTO_KINETIC_PRESS.get()))
                    .title(Component.translatable("creativetab.tecnocinetico_tab"))
                    .displayItems((params, out) -> {
                        // máquinas y herramientas
                        out.accept(ModItems.KINETIC_HAMMER.get());
                        out.accept(ModBlocks.KINETIC_ASSEMBLY_PRESS.get());
                        out.accept(ModBlocks.AUTO_KINETIC_PRESS.get());
                        out.accept(ModBlocks.WEAPON_BENCH.get());
                        out.accept(ModBlocks.WEAPON_ASSEMBLER.get());
                        // máquinas eléctricas
                        out.accept(ModBlocks.ELECTRIC_REFINER.get());
                        out.accept(ModBlocks.CELL_CHARGER.get());
                        out.accept(ModBlocks.AMMO_FABRICATOR.get());
                        out.accept(ModItems.REACTION_CHAMBER_KIT.get());
                        out.accept(ModBlocks.CHEMICAL_LAB.get());
                        out.accept(ModItems.INDUSTRIAL_ASSEMBLER_KIT.get());
                        out.accept(ModItems.FISSION_REACTOR_KIT.get());
                        out.accept(ModBlocks.CREATIVE_ENERGY_SOURCE.get());
                        // gadgets del Equipo Táctico (por tier)
                        out.accept(ModItems.NIGHT_VISION_VISOR.get());
                        out.accept(ModItems.VISOR_BATTERY.get());
                        out.accept(ModItems.AQUA_PROPULSOR.get());
                        out.accept(ModItems.PROPULSOR_CELL.get());
                        out.accept(ModItems.DASH_THRUSTER.get());
                        out.accept(ModItems.DASH_CELL.get());
                        out.accept(ModItems.THERMAL_COOLER.get());
                        out.accept(ModItems.COOLANT_PACK.get());
                        out.accept(ModItems.ASH_FILTER.get());
                        out.accept(ModItems.FILTER_CELL.get());
                        out.accept(ModItems.ANCHOR_BOOTS.get());
                        out.accept(ModItems.ANCHOR_CELL.get());
                        for (var g : java.util.List.of(ModItems.THERMAL_COOLER, ModItems.ASH_FILTER, ModItems.ANCHOR_BOOTS)) {
                            ItemStack mk2 = new ItemStack(g.get());
                            com.bryanamaya.tecnocinetico.gear.NetherGadgetItem.setLevel(mk2, 2);
                            out.accept(mk2);
                        }
                        out.accept(ModItems.ABYSSAL_COMPONENT.get());
                        out.accept(ModItems.NETHER_COUPLER.get());
                        for (int lvl = 1; lvl <= com.bryanamaya.tecnocinetico.gear.JetpackItem.MAX_LEVEL; lvl++) {
                            ItemStack jet = new ItemStack(ModItems.JETPACK.get());
                            com.bryanamaya.tecnocinetico.gear.JetpackItem.setLevel(jet, lvl);
                            out.accept(jet);
                        }
                        out.accept(ModItems.JETPACK_FUEL.get());
                        out.accept(ModItems.JETPACK_KIT_2.get());
                        out.accept(ModItems.JETPACK_KIT_3.get());
                        // nave
                        out.accept(ModItems.CORE_FIGHTER.get());
                        // criaturas (huevos) y arena del guardián
                        out.accept(ModItems.SURVIVOR_SPAWN_EGG.get());
                        out.accept(ModItems.CORRUPTED_SPAWN_EGG.get());
                        out.accept(ModItems.CORRUPTED_GUNNER_SPAWN_EGG.get());
                        out.accept(ModItems.CORRUPTED_SPIDER_SPAWN_EGG.get());
                        out.accept(ModItems.CORRUPTED_CAVE_SPIDER_SPAWN_EGG.get());
                        out.accept(ModItems.CORRUPTED_DROWNED_SPAWN_EGG.get());
                        out.accept(ModItems.CORRUPTED_CREEPER_SPAWN_EGG.get());
                        out.accept(ModItems.SWARM_BODY_SPAWN_EGG.get());
                        out.accept(ModItems.SLAG_MITE_SPAWN_EGG.get());
                        out.accept(ModItems.ASH_SENTINEL_SPAWN_EGG.get());
                        out.accept(ModItems.ABYSS_FOUNDRY_SPAWN_EGG.get());
                        out.accept(ModItems.SWARM_WASP_SPAWN_EGG.get());
                        out.accept(ModItems.HIVE_NODE_SPAWN_EGG.get());
                        out.accept(ModItems.DRONE_SPAWN_EGG.get());
                        out.accept(ModItems.SENTRY_TURRET_SPAWN_EGG.get());
                        out.accept(ModItems.ANCHOR_SPAWN_EGG.get());
                        out.accept(ModItems.SIEGE_SPIDER_SPAWN_EGG.get());
                        out.accept(ModItems.NETWORK_WATCHER_SPAWN_EGG.get());
                        out.accept(ModItems.SWARM_MATRIARCH_SPAWN_EGG.get());
                        out.accept(ModBlocks.CORE_CONSOLE.get());
                    })
                    .build());

    /** Armas y sus módulos, ordenadas por tier (1 a 3). */
    public static final RegistryObject<CreativeModeTab> WEAPONS_TAB = CREATIVE_MODE_TABS.register("tecnocinetico_weapons_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.KINETIC_PISTOL.get()))
                    .title(Component.translatable("creativetab.tecnocinetico_weapons_tab"))
                    .withTabsBefore(TECNOCINETICO_TAB.getKey())
                    .displayItems((params, out) -> {
                        // tier 1
                        out.accept(ModItems.KINETIC_PISTOL.get());
                        out.accept(ModItems.MODULE_OPTIC.get());
                        out.accept(ModItems.MODULE_EXTENDED_MAG.get());
                        out.accept(ModItems.MODULE_QUICK_RELOAD.get());
                        out.accept(ModItems.MODULE_INERCITA_BARREL.get());
                        out.accept(ModItems.REVOLVER.get());
                        out.accept(ModItems.PIPE_SHOTGUN.get());
                        out.accept(ModItems.MODULE_CHOKE.get());
                        out.accept(ModItems.MODULE_SAWED_OFF.get());
                        out.accept(ModItems.MODULE_SPEED_LOADER.get());
                        out.accept(ModItems.BOLT_RIFLE.get());
                        out.accept(ModItems.MODULE_SCOPE.get());
                        out.accept(ModItems.MODULE_POLISHED_BOLT.get());
                        out.accept(ModItems.MODULE_RIFLE_BARREL.get());
                        out.accept(ModItems.SCRAP_MACHETE.get());
                        // tier 2
                        out.accept(ModItems.KINETIC_SMG.get());
                        out.accept(ModItems.PUMP_SHOTGUN.get());
                        out.accept(ModItems.SEMI_RIFLE.get());
                        out.accept(ModItems.LIGHT_MACHINE_GUN.get());
                        out.accept(ModItems.KINETIC_SNIPER.get());
                        out.accept(ModItems.FLAMETHROWER.get());
                        out.accept(ModItems.KINETIC_BLADE.get());
                        // tier 3
                        out.accept(ModItems.ASSAULT_RIFLE.get());
                        out.accept(ModItems.RAIL_RIFLE.get());
                        out.accept(ModItems.GRENADE_LAUNCHER.get());
                        out.accept(ModItems.PULSE_CANNON.get());
                        out.accept(ModItems.ROCKET_LAUNCHER.get());
                        out.accept(ModItems.ARC_CASTER.get());
                        out.accept(ModItems.MINIGUN.get());
                        out.accept(ModItems.COMBUSTION_DRILL.get());                 // herramienta de tier 3
                        out.accept(ModItems.MODULE_SEEKER.get());
                    })
                    .build());

    /** Armaduras por tier: de combate (resistencia y movilidad) y de habilidad. */
    public static final RegistryObject<CreativeModeTab> ARMOR_TAB = CREATIVE_MODE_TABS.register("tecnocinetico_armor_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.COMBAT_MK2_CHESTPLATE.get()))
                    .title(Component.translatable("creativetab.tecnocinetico_armor_tab"))
                    .withTabsBefore(WEAPONS_TAB.getKey())
                    .displayItems((params, out) -> {
                        // tier 1
                        out.accept(ModItems.SCRAP_HELMET.get());
                        out.accept(ModItems.SCRAP_CHESTPLATE.get());
                        out.accept(ModItems.SCRAP_LEGGINGS.get());
                        out.accept(ModItems.SCRAP_BOOTS.get());
                        out.accept(ModItems.COMBAT_MK1_HELMET.get());
                        out.accept(ModItems.COMBAT_MK1_CHESTPLATE.get());
                        out.accept(ModItems.COMBAT_MK1_LEGGINGS.get());
                        out.accept(ModItems.COMBAT_MK1_BOOTS.get());
                        // tier 2
                        out.accept(ModItems.COMBAT_MK2_HELMET.get());
                        out.accept(ModItems.COMBAT_MK2_CHESTPLATE.get());
                        out.accept(ModItems.COMBAT_MK2_LEGGINGS.get());
                        out.accept(ModItems.COMBAT_MK2_BOOTS.get());
                        out.accept(ModItems.INERCITA_HELMET.get());
                        out.accept(ModItems.INERCITA_CHESTPLATE.get());
                        out.accept(ModItems.INERCITA_LEGGINGS.get());
                        out.accept(ModItems.INERCITA_BOOTS.get());
                        out.accept(ModItems.TACTICAL_HELMET.get());
                        out.accept(ModItems.TACTICAL_CHESTPLATE.get());
                        out.accept(ModItems.TACTICAL_LEGGINGS.get());
                        out.accept(ModItems.TACTICAL_BOOTS.get());
                        out.accept(ModItems.HAZMAT_HELMET.get());
                        out.accept(ModItems.HAZMAT_CHESTPLATE.get());
                        out.accept(ModItems.HAZMAT_LEGGINGS.get());
                        out.accept(ModItems.HAZMAT_BOOTS.get());
                        out.accept(ModItems.EXO_HELMET.get());
                        out.accept(ModItems.EXO_CHESTPLATE.get());
                        out.accept(ModItems.EXO_LEGGINGS.get());
                        out.accept(ModItems.EXO_BOOTS.get());
                        // tier 3
                        out.accept(ModItems.COMBAT_MK3_HELMET.get());
                        out.accept(ModItems.COMBAT_MK3_CHESTPLATE.get());
                        out.accept(ModItems.COMBAT_MK3_LEGGINGS.get());
                        out.accept(ModItems.COMBAT_MK3_BOOTS.get());
                        out.accept(ModItems.CORE_HELMET.get());
                        out.accept(ModItems.CORE_CHESTPLATE.get());
                        out.accept(ModItems.CORE_LEGGINGS.get());
                        out.accept(ModItems.CORE_BOOTS.get());
                        out.accept(ModItems.PIROXITE_HELMET.get());
                        out.accept(ModItems.PIROXITE_CHESTPLATE.get());
                        out.accept(ModItems.PIROXITE_LEGGINGS.get());
                        out.accept(ModItems.PIROXITE_BOOTS.get());
                        out.accept(ModItems.REFINED_PIROXITE_HELMET.get());
                        out.accept(ModItems.REFINED_PIROXITE_CHESTPLATE.get());
                        out.accept(ModItems.REFINED_PIROXITE_LEGGINGS.get());
                        out.accept(ModItems.REFINED_PIROXITE_BOOTS.get());
                        out.accept(ModItems.NULLITE_HELMET.get());
                        out.accept(ModItems.NULLITE_CHESTPLATE.get());
                        out.accept(ModItems.NULLITE_LEGGINGS.get());
                        out.accept(ModItems.NULLITE_BOOTS.get());
                    })
                    .build());

    /** Munición, cargadores, cartuchos, cohetes y células, por tier. */
    public static final RegistryObject<CreativeModeTab> AMMO_TAB = CREATIVE_MODE_TABS.register("tecnocinetico_ammo_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.MAGAZINE.get()))
                    .title(Component.translatable("creativetab.tecnocinetico_ammo_tab"))
                    .withTabsBefore(ARMOR_TAB.getKey())
                    .displayItems((params, out) -> {
                        // tier 1
                        out.accept(ModItems.KINETIC_ROUND.get());
                        out.accept(ModItems.MAGAZINE.get());
                        out.accept(ModItems.EXTENDED_MAGAZINE.get());
                        out.accept(ModItems.SHOTGUN_SHELL.get());
                        out.accept(ModItems.RIFLE_ROUND.get());
                        out.accept(ModItems.STRIPPER_CLIP.get());
                        out.accept(ModItems.SHORT_RIFLE_MAGAZINE.get());
                        // tier 2
                        out.accept(ModItems.FUEL_CANISTER.get());
                        out.accept(ModItems.RIFLE_BELT.get());
                        out.accept(ModItems.BELT_BOX.get());
                        // tier 3
                        out.accept(ModItems.RIFLE_MAGAZINE.get());
                        out.accept(ModItems.MINIGUN_BOX.get());
                        out.accept(ModItems.KINETIC_GRENADE.get());
                        out.accept(ModItems.GUIDED_ROCKET.get());
                        out.accept(ModItems.PULSE_CELL.get());
                        out.accept(ModItems.EMPTY_PULSE_CELL.get());
                        out.accept(ModItems.GUIDED_ROUND.get());
                    })
                    .build());

    /** Materiales: menas, lingotes, piezas de prensa y botín clave, por tier. */
    public static final RegistryObject<CreativeModeTab> MATERIALS_TAB = CREATIVE_MODE_TABS.register("tecnocinetico_materials_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.CINETRITE_INGOT.get()))
                    .title(Component.translatable("creativetab.tecnocinetico_materials_tab"))
                    .withTabsBefore(AMMO_TAB.getKey())
                    .displayItems((params, out) -> {
                        // tier 1
                        out.accept(ModItems.SCRAP_METAL.get());
                        out.accept(ModBlocks.CINETRITE_ORE.get());
                        out.accept(ModBlocks.DEEPSLATE_CINETRITE_ORE.get());
                        out.accept(ModItems.RAW_CINETRITE.get());
                        out.accept(ModItems.CINETRITE_INGOT.get());
                        out.accept(ModItems.CINETRITE_SHARD.get());
                        out.accept(ModBlocks.INERCITA_ORE.get());
                        out.accept(ModBlocks.DEEPSLATE_INERCITA_ORE.get());
                        out.accept(ModItems.RAW_INERCITA.get());
                        out.accept(ModItems.INERCITA_INGOT.get());
                        out.accept(ModBlocks.ANCIENT_ALLOY_BLOCK.get());
                        out.accept(ModBlocks.LEAD_ORE.get());
                        out.accept(ModBlocks.DEEPSLATE_LEAD_ORE.get());
                        out.accept(ModItems.RAW_LEAD.get());
                        out.accept(ModItems.LEAD_INGOT.get());
                        out.accept(ModItems.LEAD_NUGGET.get());
                        out.accept(ModBlocks.LEAD_BLOCK.get());
                        out.accept(ModBlocks.TIN_ORE.get());
                        out.accept(ModBlocks.DEEPSLATE_TIN_ORE.get());
                        out.accept(ModItems.RAW_TIN.get());
                        out.accept(ModItems.TIN_INGOT.get());
                        out.accept(ModItems.TIN_NUGGET.get());
                        out.accept(ModBlocks.TIN_BLOCK.get());
                        out.accept(ModItems.RESISTOR_330_OHM.get());
                        out.accept(ModItems.PIEZO_TRANSDUCER.get());
                        out.accept(ModItems.DEEPSLATE_LOGIC_SUBSTRATE.get());
                        out.accept(ModItems.CRUDE_KINETIC_COUPLER.get());
                        // tier 2 (prensa con energía)
                        out.accept(ModBlocks.TITANIUM_ORE.get());
                        out.accept(ModBlocks.DEEPSLATE_TITANIUM_ORE.get());
                        out.accept(ModItems.RAW_TITANIUM.get());
                        out.accept(ModItems.TITANIUM_INGOT.get());
                        out.accept(ModItems.TITANIUM_NUGGET.get());
                        out.accept(ModBlocks.TITANIUM_BLOCK.get());
                        out.accept(ModBlocks.URANIUM_ORE.get());
                        out.accept(ModBlocks.DEEPSLATE_URANIUM_ORE.get());
                        out.accept(ModItems.RAW_URANIUM.get());
                        out.accept(ModItems.URANIUM_INGOT.get());
                        out.accept(ModItems.URANIUM_NUGGET.get());
                        out.accept(ModBlocks.URANIUM_BLOCK.get());
                        out.accept(ModItems.URANIUM_FUEL_ROD.get());
                        out.accept(ModItems.DEPLETED_FUEL_ROD.get());
                        out.accept(com.bryanamaya.tecnocinetico.fluid.ModFluids.UNIVERSAL_CELL.get());
                        out.accept(com.bryanamaya.tecnocinetico.fluid.ModFluids.SULFURIC_ACID.bucket.get());
                        out.accept(com.bryanamaya.tecnocinetico.fluid.ModFluids.COOLANT.bucket.get());
                        out.accept(ModItems.IRON_DUST.get());
                        out.accept(ModItems.GOLD_DUST.get());
                        out.accept(ModItems.COPPER_DUST.get());
                        out.accept(ModItems.LEAD_DUST.get());
                        out.accept(ModItems.TIN_DUST.get());
                        out.accept(ModItems.TITANIUM_DUST.get());
                        out.accept(ModItems.CINETRITE_DUST.get());
                        out.accept(ModItems.INERCITA_DUST.get());
                        out.accept(ModItems.PIROXITE_DUST.get());
                        out.accept(ModItems.NULLITE_DUST.get());
                        out.accept(ModItems.INERCITA_PLATE.get());
                        out.accept(ModItems.PRECISION_MECHANISM.get());
                        out.accept(ModItems.KINETIC_SERVO.get());
                        out.accept(ModItems.BALLISTIC_FABRIC.get());
                        out.accept(ModItems.SWARM_CORE.get());
                        // tier 3 (Nether, End e Instalaciones de la IA)
                        out.accept(ModItems.CORE_PROCESSOR.get());
                        out.accept(ModBlocks.PIROXITE_ORE.get());
                        out.accept(ModItems.RAW_PIROXITE.get());
                        out.accept(ModItems.PIROXITE_INGOT.get());
                        out.accept(ModItems.SULFUR_DUST.get());                 // Nether: ácaros y centinelas
                        out.accept(ModItems.SLAG_ALLOY.get());
                        out.accept(ModBlocks.NULLITE_ORE.get());
                        out.accept(ModItems.RAW_NULLITE.get());
                        out.accept(ModItems.NULLITE_INGOT.get());
                        out.accept(ModItems.DIMENSIONAL_DRIVE.get());
                        out.accept(ModItems.VECTOR_THRUSTER.get());
                        out.accept(ModItems.ADAPTIVE_FLIGHT_CORE.get());
                        out.accept(ModItems.DRILL_HEAD.get());
                        out.accept(ModItems.COMBUSTION_ENGINE.get());
                    })
                    .build());

    /** Pestaña de los bloques del mundo post-apocalíptico (hormigón, asfalto, metal oxidado, ceniza...). */
    public static final RegistryObject<CreativeModeTab> WORLD_TAB = CREATIVE_MODE_TABS.register("tecnocinetico_world_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModWorldBlocks.CRACKED_CONCRETE.get()))
                    .title(Component.translatable("creativetab.tecnocinetico_world_tab"))
                    .withTabsBefore(MATERIALS_TAB.getKey())
                    .displayItems((params, output) -> {
                        ModWorldBlocks.ALL.forEach(b -> output.accept(b.get()));
                        output.accept(ModItems.SCRAP_METAL.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
