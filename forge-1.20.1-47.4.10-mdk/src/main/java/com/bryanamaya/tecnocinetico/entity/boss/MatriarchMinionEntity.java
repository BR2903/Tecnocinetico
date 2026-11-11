package com.bryanamaya.tecnocinetico.entity.boss;

import com.bryanamaya.tecnocinetico.entity.custom.SwarmBodyEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;
import java.util.UUID;

/** Cría pequeña de la Matriarca: salta y persigue, pero no da experiencia, botín ni represalias contra su madre. */
public class MatriarchMinionEntity extends SwarmBodyEntity {
    @Nullable private UUID matriarch;
    private int missingTicks;

    public MatriarchMinionEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 0;
        setPersistenceRequired();
    }
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 10).add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.MOVEMENT_SPEED, .34).add(Attributes.ARMOR, 2).add(Attributes.FOLLOW_RANGE, 32);
    }
    public void bind(OverworldGuardianEntity owner) {
        matriarch = owner.getUUID();
        setTarget(owner.getTarget());
    }
    @Override public boolean isAlliedTo(Entity other) {
        return other instanceof OverworldGuardianEntity || other instanceof SiegeSpiderEntity || other instanceof SwarmBodyEntity
                || other instanceof com.bryanamaya.tecnocinetico.entity.custom.DroneEntity
                || other instanceof com.bryanamaya.tecnocinetico.entity.swarm.HiveNodeEntity
                || other instanceof com.bryanamaya.tecnocinetico.entity.swarm.SwarmWaspEntity || super.isAlliedTo(other);
    }
    @Override public void setTarget(@Nullable LivingEntity t) {
        if (t == null || !isAlliedTo(t)) super.setTarget(t);
    }
    @Override public boolean hurt(DamageSource source, float amount) {
        return !(source.getEntity() != null && isAlliedTo(source.getEntity())) && super.hurt(source, amount);
    }
    @Override protected ResourceLocation getDefaultLootTable() {
        return net.minecraft.world.level.storage.loot.BuiltInLootTables.EMPTY;
    }
    @Override protected void customServerAiStep() {
        if (level() instanceof ServerLevel server && matriarch != null) {
            Entity e = server.getEntity(matriarch);
            if (e instanceof OverworldGuardianEntity owner) {
                missingTicks = 0;
                if (!owner.isAlive() || !owner.ownsMinion(getUUID())) { discard(); return; }
                if (owner.getTarget() != null && owner.getTarget().isAlive()) setTarget(owner.getTarget());
            } else if (++missingTicks > 200) { discard(); return; } // tolera el orden de carga de los chunks
        }
        super.customServerAiStep();
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (matriarch != null) tag.putUUID("Matriarch", matriarch);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        matriarch = tag.hasUUID("Matriarch") ? tag.getUUID("Matriarch") : null;
    }
}
