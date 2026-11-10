package com.bryanamaya.tecnocinetico.structure;

import com.bryanamaya.tecnocinetico.machine.MachineBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Pieza de una estructura grande: recuerda dónde está su núcleo y le pasa energía, líquidos y objetos. */
public class StructurePartBlockEntity extends BlockEntity {

    @Nullable
    private BlockPos core;

    public StructurePartBlockEntity(BlockPos pos, BlockState state) {
        super(MachineBlockEntities.STRUCTURE_PART.get(), pos, state);
    }

    public void setCore(BlockPos core) {
        this.core = core.immutable();
        setChanged();
    }

    @Nullable
    public BlockPos core() {
        return core;
    }

    @Nullable
    public BlockEntity coreEntity() {
        return core == null || level == null ? null : level.getBlockEntity(core);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        BlockEntity c = coreEntity();
        if (c != null && c != this && !(c instanceof StructurePartBlockEntity)) {
            return c.getCapability(cap, side);                                  // tuberías y cables en cualquier pieza
        }
        return super.getCapability(cap, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (core != null) {
            tag.put("core", NbtUtils.writeBlockPos(core));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        core = tag.contains("core") ? NbtUtils.readBlockPos(tag.getCompound("core")) : null;
    }
}
