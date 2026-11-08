package com.bryanamaya.tecnocinetico.worldgen.structure;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Tipos de estructura y de pieza del mod (fase 4 de docs/PLAN_MUNDO.md). Las estructuras concretas
 * (dónde y con qué parámetros) están en data/tecnocinetico/worldgen/structure y structure_set,
 * generadas por tools/world_structures_gen.py.
 */
public final class ModStructures {

    public static final DeferredRegister<StructureType<?>> TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, Tecnocinetico.MOD_ID);
    public static final DeferredRegister<StructurePieceType> PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, Tecnocinetico.MOD_ID);

    public static final RegistryObject<StructureType<RuinedBuildingStructure>> RUINED_BUILDING =
            TYPES.register("ruined_building", () -> () -> RuinedBuildingStructure.CODEC);
    public static final RegistryObject<StructurePieceType> RUINED_BUILDING_PIECE =
            PIECES.register("ruined_building", () -> (StructurePieceType.ContextlessType) RuinedBuildingPiece::new);

    public static final RegistryObject<StructureType<SettlementStructure>> SETTLEMENT =
            TYPES.register("settlement", () -> () -> SettlementStructure.CODEC);
    public static final RegistryObject<StructurePieceType> SETTLEMENT_PIECE =
            PIECES.register("settlement", () -> (StructurePieceType.ContextlessType) SettlementPiece::new);

    public static final RegistryObject<StructureType<FactoryStructure>> FACTORY =
            TYPES.register("abandoned_factory", () -> () -> FactoryStructure.CODEC);
    public static final RegistryObject<StructurePieceType> FACTORY_PIECE =
            PIECES.register("abandoned_factory", () -> (StructurePieceType.ContextlessType) FactoryPiece::new);

    public static final RegistryObject<StructureType<SwarmHiveStructure>> SWARM_HIVE =
            TYPES.register("swarm_hive", () -> () -> SwarmHiveStructure.CODEC);
    public static final RegistryObject<StructurePieceType> SWARM_HIVE_PIECE =
            PIECES.register("swarm_hive", () -> (StructurePieceType.ContextlessType) SwarmHivePiece::new);

    private ModStructures() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        PIECES.register(bus);
    }
}
