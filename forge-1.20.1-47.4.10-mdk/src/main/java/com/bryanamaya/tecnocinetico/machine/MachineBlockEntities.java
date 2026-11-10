package com.bryanamaya.tecnocinetico.machine;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Tipos de entidad de bloque de las máquinas eléctricas. */
public final class MachineBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Tecnocinetico.MOD_ID);

    public static final RegistryObject<BlockEntityType<RefinerBlockEntity>> REFINER = TYPES.register("electric_refiner",
            () -> BlockEntityType.Builder.of(RefinerBlockEntity::new, ModBlocks.ELECTRIC_REFINER.get()).build(null));
    public static final RegistryObject<BlockEntityType<ChargerBlockEntity>> CHARGER = TYPES.register("cell_charger",
            () -> BlockEntityType.Builder.of(ChargerBlockEntity::new, ModBlocks.CELL_CHARGER.get()).build(null));
    public static final RegistryObject<BlockEntityType<AmmoFabricatorBlockEntity>> AMMO_FABRICATOR = TYPES.register("ammo_fabricator",
            () -> BlockEntityType.Builder.of(AmmoFabricatorBlockEntity::new, ModBlocks.AMMO_FABRICATOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<ReactionChamberBlockEntity>> REACTION_CHAMBER = TYPES.register("reaction_chamber",
            () -> BlockEntityType.Builder.of(ReactionChamberBlockEntity::new, ModBlocks.REACTION_CHAMBER.get()).build(null));

    public static final RegistryObject<BlockEntityType<ChemicalLabBlockEntity>> CHEMICAL_LAB = TYPES.register("chemical_lab",
            () -> BlockEntityType.Builder.of(ChemicalLabBlockEntity::new, ModBlocks.CHEMICAL_LAB.get()).build(null));

    public static final RegistryObject<BlockEntityType<FissionReactorBlockEntity>> FISSION_REACTOR = TYPES.register("fission_reactor",
            () -> BlockEntityType.Builder.of(FissionReactorBlockEntity::new, ModBlocks.FISSION_REACTOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<IndustrialAssemblerBlockEntity>> INDUSTRIAL_ASSEMBLER = TYPES.register("industrial_assembler",
            () -> BlockEntityType.Builder.of(IndustrialAssemblerBlockEntity::new, ModBlocks.INDUSTRIAL_ASSEMBLER.get()).build(null));
    /** Piezas de las estructuras grandes (pasan energía, líquidos y objetos al núcleo). */
    public static final RegistryObject<BlockEntityType<com.bryanamaya.tecnocinetico.structure.StructurePartBlockEntity>> STRUCTURE_PART =
            TYPES.register("structure_part", () -> BlockEntityType.Builder.of(com.bryanamaya.tecnocinetico.structure.StructurePartBlockEntity::new,
                    ModBlocks.ASSEMBLER_PART.get(), ModBlocks.REACTOR_SHELL.get(), ModBlocks.REACTION_CHAMBER_PART.get()).build(null));

    private MachineBlockEntities() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }
}
