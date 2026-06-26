package com.bryanamaya.tecnocinetico.block.entity;

import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item; // Importante mantenerlo para las comprobaciones
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

            ItemStack result = checkRecipes();
            // Si la receta es válida (no está vacía)
            if (!result.isEmpty()) {
                // 1. Consumir 1 unidad de cada slot que tenga algún ítem
                for (int i = 0; i < itemHandler.getSlots(); i++) {
                    if (!itemHandler.getStackInSlot(i).isEmpty()) {
                        itemHandler.extractItem(i, 1, false);
                    }
                }
                // 2. Hacer aparecer el objeto(s) terminado(s) flotando sobre la prensa
                Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D, result.copy());
            }
        }
    }

    // Algoritmo de correspondencia mejorado que devuelve ItemStacks (para cantidades exactas)
    private @NotNull ItemStack checkRecipes() {
        boolean hasCinetriteIngot = hasItem(ModItems.CINETRITE_INGOT.get());
        boolean hasCinetriteShard = hasItem(ModItems.CINETRITE_SHARD.get());
        boolean hasCopper = hasItem(Items.COPPER_INGOT);
        boolean hasIron = hasItem(Items.IRON_INGOT);
        boolean hasDeepslateSlab = hasItem(Items.COBBLED_DEEPSLATE_SLAB);
        boolean hasRedstone = hasItem(Items.REDSTONE);
        boolean hasAlloyBlock = hasItem(ModBlocks.ANCIENT_ALLOY_BLOCK.get().asItem());

        // Contamos cuántos objetos hay en el yunque para evitar conflictos de recetas
        int filledSlots = getFilledSlotsCount();

        // Receta 1: Transductor Piezo-Cúprico (Cobre + Lingote de Cinetrita) -> Requiere 2 objetos
        if (hasCopper && hasCinetriteIngot && filledSlots == 2) {
            return new ItemStack(ModItems.PIEZO_TRANSDUCER.get(), 1);
        }

        // Receta 2: Sustrato Lógico de Pizarra (Losa Pizarra + Redstone + Lingote) -> Requiere 3 objetos
        if (hasDeepslateSlab && hasRedstone && hasCinetriteIngot && filledSlots == 3) {
            return new ItemStack(ModItems.DEEPSLATE_LOGIC_SUBSTRATE.get(), 1);
        }

        // Receta 3: Acoplador Cinético Bruto (Hierro + Bloque Aleación + Fragmento) -> Requiere 3 objetos
        if (hasIron && hasAlloyBlock && hasCinetriteShard && filledSlots == 3) {
            return new ItemStack(ModItems.CRUDE_KINETIC_COUPLER.get(), 1);
        }

        // NUEVA RECETA: Fragmentación Cinética (Solo el Lingote de Cinetrita) -> Requiere 1 objeto
        if (hasCinetriteIngot && filledSlots == 1) {
            return new ItemStack(ModItems.CINETRITE_SHARD.get(), 2); // ¡Aquí soltamos 2 fragmentos!
        }

        return ItemStack.EMPTY; // No coincide con ninguna combinación válida
    }

    // Función auxiliar para saber si un ítem específico está en la máquina
    private boolean hasItem(Item target) {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (itemHandler.getStackInSlot(i).is(target)) {
                return true;
            }
        }
        return false;
    }

    // Función auxiliar para contar cuántos slots están siendo ocupados
    private int getFilledSlotsCount() {
        int count = 0;
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                count++;
            }
        }
        return count;
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