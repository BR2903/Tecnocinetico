package com.bryanamaya.tecnocinetico.entity;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.custom.SwarmBodyEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Tecnocinetico.MOD_ID);

    // Cuerpo de Enjambre: criatura biomecánica con núcleo de Cinetrita (ver lore)
    public static final RegistryObject<EntityType<SwarmBodyEntity>> SWARM_BODY =
            ENTITY_TYPES.register("swarm_body", () -> EntityType.Builder.of(SwarmBodyEntity::new, MobCategory.MONSTER)
                    .sized(1.2f, 1.2f)
                    .clientTrackingRange(10)
                    .build("swarm_body"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
