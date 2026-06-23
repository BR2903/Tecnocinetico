package com.bryanamaya.tecnocinetico.block.entity;

import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KineticAssemblyPressBlockEntity extends BlockEntity {

    private int strikeCount = 0; // Contador de martillazos recibido

    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    public KineticAssemblyPressBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.KINETIC_ASSEMBLY_PRESS_BE.get(), pPos, pBlockState);
    }

    // Método que se ejecuta cuando el jugador golpea con el martillo
    public void attemptCraft(Level level, BlockPos pos) {
        this.strikeCount++;
        setChanged();

        if (this.strikeCount >= 3) {
            this.strikeCount = 0; // Resetear contador

            Item result = checkRecipes();
            if (result != null) {
                // 1. Consumir 1 unidad de cada slot de la mesa
                for (int i = 0; i < itemHandler.getSlots(); i++) {
                    itemHandler.extractItem(i, 1, false);
                }
                // 2. Hacer aparecer el objeto terminado flotando sobre la prensa
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, new ItemStack(result));
            }
        }
    }

    // Algoritmo de correspondencia para las 3 recetas del Tier 1
    private @Nullable Item checkRecipes() {
        boolean hasCinetriteIngot = hasItem(ModItems.CINETRITE_INGOT.get());
        boolean hasCinetriteShard = hasItem(ModItems.CINETRITE_SHARD.get());
        boolean hasCopper = hasItem(Items.COPPER_INGOT);
        boolean hasIron = hasItem(Items.IRON_INGOT);
        boolean hasDeepslateSlab = hasItem(Items.COBBLED_DEEPSLATE_SLAB);
        boolean hasRedstone = hasItem(Items.REDSTONE);
        boolean hasAlloyBlock = hasItem(ModBlocks.ANCIENT_ALLOY_BLOCK.get().asItem());

        // Receta 1: Transductor Piezo-Cúprico (Cobre + Lingote de Cinetrita)
        if (hasCopper && hasCinetriteIngot) {
            return ModItems.PIEZO_TRANSDUCER.get();
        }

        // Receta 2: Sustrato Lógico de Pizarra (Losa Pizarra + Redstone + Lingote de Cinetrita)
        if (hasDeepslateSlab && hasRedstone && hasCinetriteIngot) {
            return ModItems.DEEPSLATE_LOGIC_SUBSTRATE.get();
        }

        // Receta 3: Acoplador Cinético Bruto (Hierro + Bloque Aleación Antigua + Fragmento Cinetrita)
        if (hasIron && hasAlloyBlock && hasCinetriteShard) {
            return ModItems.CRUDE_KINETIC_COUPLER.get();
        }

        return null; // No coincide con ninguna combinación válida
    }

    private boolean hasItem(Item target) {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (itemHandler.getStackInSlot(i).is(target)) {
                return true;
            }
        }
        return false;
    }

    // Guardar el estado de los golpes en el disco duro del mundo (NBT)
    @Override
    protected void saveAdditional(CompoundTag pTag) {
        pTag.put("inventory", itemHandler.serializeNBT());
        pTag.putInt("strikes", this.strikeCount);
        super.saveAdditional(pTag);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));
        this.strikeCount = pTag.getInt("strikes");
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }
}