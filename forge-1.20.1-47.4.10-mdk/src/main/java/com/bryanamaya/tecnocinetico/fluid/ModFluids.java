package com.bryanamaya.tecnocinetico.fluid;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Consumer;

/**
 * Líquidos del Laboratorio Químico. Usan la textura del agua vanilla teñida con su color (sin texturas propias);
 * cada uno tiene fuente, flujo, bloque y cubo (modelo forge:fluid_container). Las tuberías y bombas de Create
 * los mueven porque las máquinas exponen la capacidad de fluidos de Forge.
 */
public final class ModFluids {

    public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, Tecnocinetico.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, Tecnocinetico.MOD_ID);

    /** Ácido sulfúrico: amarillo verdoso, corrosivo (daña a quien está dentro). */
    public static final FluidSet SULFURIC_ACID = new FluidSet("sulfuric_acid", 0xD8D6E85A, 1800, 1400, true);
    /** Refrigerante: azul claro, para el reactor. */
    public static final FluidSet COOLANT = new FluidSet("coolant", 0xC84FC8F0, 1100, 1000, false);

    /** Celda Universal de Fluidos (1 cubo de cualquier líquido). */
    public static final net.minecraftforge.registries.RegistryObject<Item> UNIVERSAL_CELL = ModItems.ITEMS.register("universal_fluid_cell",
            () -> new UniversalCellItem(new Item.Properties().stacksTo(64)));

    private ModFluids() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        FLUIDS.register(bus);
    }

    /** Todo lo que compone un líquido: tipo, fuente, flujo, bloque y cubo. */
    public static final class FluidSet {
        public final String id;
        public final int color;
        public final RegistryObject<FluidType> type;
        public final RegistryObject<FlowingFluid> source, flowing;
        public final RegistryObject<LiquidBlock> block;
        public final RegistryObject<Item> bucket;

        FluidSet(String id, int color, int density, int viscosity, boolean corrosive) {
            this.id = id;
            this.color = color;
            this.type = TYPES.register(id, () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type." + Tecnocinetico.MOD_ID + "." + id)
                    .density(density).viscosity(viscosity).canSwim(true).canDrown(true).canPushEntity(true)) {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        @Override
                        public ResourceLocation getStillTexture() {
                            return ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_still");
                        }

                        @Override
                        public ResourceLocation getFlowingTexture() {
                            return ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_flow");
                        }

                        @Override
                        public ResourceLocation getOverlayTexture() {
                            return ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_overlay");
                        }

                        @Override
                        public int getTintColor() {
                            return color;
                        }
                    });
                }
            });
            this.source = FLUIDS.register(id, () -> new ForgeFlowingFluid.Source(props()));
            this.flowing = FLUIDS.register("flowing_" + id, () -> new ForgeFlowingFluid.Flowing(props()));
            this.block = ModBlocks.BLOCKS.register(id, () -> corrosive
                    ? new AcidBlock(source, BlockBehaviour.Properties.copy(Blocks.WATER).noLootTable())
                    : new LiquidBlock(source, BlockBehaviour.Properties.copy(Blocks.WATER).noLootTable()));
            this.bucket = ModItems.ITEMS.register(id + "_bucket",
                    () -> new BucketItem(source, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
        }

        private ForgeFlowingFluid.Properties props() {
            return new ForgeFlowingFluid.Properties(type, source, flowing).bucket(bucket).block(block)
                    .slopeFindDistance(3).levelDecreasePerBlock(2).tickRate(10);
        }

        public Fluid get() {
            return source.get();
        }
    }

    /** Bloque de ácido: quema a los seres vivos que están dentro. */
    public static class AcidBlock extends LiquidBlock {
        public AcidBlock(java.util.function.Supplier<? extends FlowingFluid> fluid, Properties properties) {
            super(fluid, properties);
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            if (!level.isClientSide && entity instanceof LivingEntity living && living.tickCount % 10 == 0) {
                living.hurt(level.damageSources().magic(), 2.0F);
            }
        }
    }
}
