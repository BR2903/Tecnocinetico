package com.bryanamaya.tecnocinetico.reservoir;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registro del Reservorio Universal (bloque, objeto, entidad de bloque e interfaz). */
public final class ReservoirRegistry {

    public static final DeferredRegister<BlockEntityType<?>> BE_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Tecnocinetico.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Tecnocinetico.MOD_ID);

    public static final RegistryObject<Block> RESERVOIR = ModBlocks.BLOCKS.register("universal_reservoir", ReservoirBlock::new);
    public static final RegistryObject<Item> RESERVOIR_ITEM = ModItems.ITEMS.register("universal_reservoir",
            () -> new BlockItem(RESERVOIR.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryObject<BlockEntityType<ReservoirBlockEntity>> RESERVOIR_BE = BE_TYPES.register("universal_reservoir",
            () -> BlockEntityType.Builder.of(ReservoirBlockEntity::new, RESERVOIR.get()).build(null));
    public static final RegistryObject<MenuType<ReservoirMenu>> RESERVOIR_MENU = MENUS.register("universal_reservoir",
            () -> IForgeMenuType.create(ReservoirMenu::new));

    private ReservoirRegistry() {}

    public static void register(IEventBus bus) {
        BE_TYPES.register(bus);
        MENUS.register(bus);
    }
}
