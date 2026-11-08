package com.bryanamaya.tecnocinetico.entity.corrupted;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

/**
 * Creeper Corrupto: un cuerpo hinchado de cinetrita inestable (sustituye al creeper en el Overworld). Mecha más
 * corta (1,2 s en vez de 1,5), más vida, y al estallar deja una nube de polvo de cinetrita que debilita y marea
 * ({@link CorruptionSpawns#onDetonate}).
 */
public class CorruptedCreeperEntity extends Creeper {

    public CorruptedCreeperEntity(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Creeper.createAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data, tag);
        // mecha corta: el campo es privado en Creeper, se ajusta por su NBT ("Fuse")
        CompoundTag nbt = new CompoundTag();
        addAdditionalSaveData(nbt);
        nbt.putShort("Fuse", (short) 24);
        readAdditionalSaveData(nbt);
        return out;
    }
}
