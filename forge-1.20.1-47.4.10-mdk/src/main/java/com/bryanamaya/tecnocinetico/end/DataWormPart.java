package com.bryanamaya.tecnocinetico.end;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.entity.PartEntity;

import javax.annotation.Nullable;

/**
 * Segmento (o cola) del {@link DataWormEntity}. Recibe los golpes por separado: el daño va a la vida de este segmento
 * (y la mitad al gusano); si se rompe, el gusano se acorta. No se guarda: lo recoloca el gusano cada tick.
 */
public class DataWormPart extends PartEntity<DataWormEntity> {

    public final int index;
    private final EntityDimensions size;

    public DataWormPart(DataWormEntity worm, int index, float size) {
        super(worm);
        this.index = index;
        this.size = EntityDimensions.scalable(size, size);
        refreshDimensions();
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return getParent().partExposed(index);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !isInvulnerableTo(source) && getParent().hurtSegment(index, source, amount);
    }

    @Override
    public boolean is(Entity other) {
        return this == other || getParent() == other;
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        return getParent().getPickResult();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return size;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
