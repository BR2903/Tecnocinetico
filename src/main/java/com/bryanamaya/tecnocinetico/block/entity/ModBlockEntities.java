package com.bryanamaya.tecnocinetico.block.entity;

import com.bryanamaya.tecnocinetico.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "tecnocinetico");

    public static final RegistryObject<BlockEntityType<KineticAssemblyPressBlockEntity>> KINETIC_ASSEMBLY_PRESS_BE =
            BLOCK_ENTITIES.register("kinetic_assembly_press_be", () ->
                    BlockEntityType.Builder.of(KineticAssemblyPressBlockEntity::new,
                            ModBlocks.KINETIC_ASSEMBLY_PRESS.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}