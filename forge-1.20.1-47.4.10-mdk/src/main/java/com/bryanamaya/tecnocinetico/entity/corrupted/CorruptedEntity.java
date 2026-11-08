package com.bryanamaya.tecnocinetico.entity.corrupted;

import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

/**
 * Corrupto: superviviente infectado por la cinetrita (sustituye a zombis y husks en el Overworld, ver
 * {@link CorruptionSpawns}). Usa la IA del zombi, pero no arde al sol, no se ahoga en drowned, nunca es bebé y
 * es más duro. Tres aspectos (errante, obrero, cristalizado). Algunos llevan machete o pala, y piezas de
 * armadura de chatarra.
 */
public class CorruptedEntity extends Zombie implements CorruptedSkin {

    private static final EntityDataAccessor<Byte> VARIANT = SynchedEntityData.defineId(CorruptedEntity.class, EntityDataSerializers.BYTE);
    private static final String[] SKINS = {"wanderer", "worker", "crystal"};

    public CorruptedEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 26.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.ARMOR, 3.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(VARIANT, (byte) 0);
    }

    @Override
    public String skinName() {
        return SKINS[Math.floorMod(entityData.get(VARIANT), SKINS.length)];
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        float v = random.nextFloat();
        entityData.set(VARIANT, (byte) (v < 0.45F ? 0 : v < 0.8F ? 1 : 2));
        // nunca bebé ni jinete de gallina
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, new ZombieGroupData(false, false), tag);
        setBaby(false);
        return out;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource r, DifficultyInstance difficulty) {
        float w = r.nextFloat();
        if (w < 0.22F) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.SCRAP_MACHETE.get()));
        } else if (w < 0.32F) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SHOVEL));
        }
        float bonus = difficulty.getSpecialMultiplier();
        if (r.nextFloat() < 0.10F + 0.15F * bonus) {
            setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.SCRAP_HELMET.get()));
        }
        if (r.nextFloat() < 0.06F + 0.12F * bonus) {
            setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.SCRAP_CHESTPLATE.get()));
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0.04F);
        }
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.8F;                                     // voz más grave y rota
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Variant", entityData.get(VARIANT));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(VARIANT, tag.getByte("Variant"));
    }
}
