package com.bryanamaya.tecnocinetico.block.entity;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Tecnocinetico.MOD_ID);

    public static final RegistryObject<BlockEntityType<KineticAssemblyPressBlockEntity>> KINETIC_ASSEMBLY_PRESS_BE =
            BLOCK_ENTITIES.register("kinetic_assembly_press_be", () ->
                    BlockEntityType.Builder.of(KineticAssemblyPressBlockEntity::new,
                            ModBlocks.KINETIC_ASSEMBLY_PRESS.get()).build(null));

    public static final RegistryObject<BlockEntityType<AutoKineticPressBlockEntity>> AUTO_KINETIC_PRESS_BE =
            BLOCK_ENTITIES.register("auto_kinetic_press_be", () ->
                    BlockEntityType.Builder.of(AutoKineticPressBlockEntity::new,
                            ModBlocks.AUTO_KINETIC_PRESS.get()).build(null));

    public static final RegistryObject<BlockEntityType<CreativeEnergySourceBlockEntity>> CREATIVE_ENERGY_SOURCE_BE =
            BLOCK_ENTITIES.register("creative_energy_source_be", () ->
                    BlockEntityType.Builder.of(CreativeEnergySourceBlockEntity::new,
                            ModBlocks.CREATIVE_ENERGY_SOURCE.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}