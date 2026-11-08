package com.bryanamaya.tecnocinetico.entity.swarm;

import com.bryanamaya.tecnocinetico.entity.ModEntities;
import com.bryanamaya.tecnocinetico.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Nodo de Colmena (la "reina"): criatura biomecánica grande que flota en el centro de la colmena y
 * contiene la {@link HiveMind}. Crea avispas, cura a las heridas que vuelven a ella y suelta un pulso
 * cinético si alguien se le acerca. Si muere, el enjambre se desorganiza.
 */
public class HiveNodeEntity extends Monster {

    private final HiveMind mind = new HiveMind(this);
    @Nullable
    private BlockPos home;
    private int pulseCooldown;

    public HiveNodeEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
        this.xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 90.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.1D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    public HiveMind mind() {
        return mind;
    }

    @Override
    protected void registerGoals() {
        // sin objetivos vanilla: todo lo decide la mente colmena
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        home = blockPosition();
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    // ------------------------------------------------------------------ comportamiento

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (home == null) {
            home = blockPosition();
        }
        mind.tick();
        // flotar sobre su sitio con un leve balanceo
        Vec3 want = Vec3.atBottomCenterOf(home).add(0, 0.6D + Math.sin(tickCount * 0.05D) * 0.4D, 0);
        Vec3 delta = want.subtract(position());
        setDeltaMovement(getDeltaMovement().scale(0.8D).add(delta.scale(0.02D)));
        LivingEntity t = mind.target();
        if (t != null) {
            getLookControl().setLookAt(t, 10.0F, 10.0F);
        }
        // curar a las avispas heridas que están a su lado
        if (tickCount % 20 == 0) {
            for (SwarmWaspEntity w : mind.members()) {
                if (w.distanceToSqr(this) < 9.0D && w.getHealth() < w.getMaxHealth()) {
                    w.heal(1.0F);
                }
            }
        }
        // pulso cinético contra quien se le pega
        if (pulseCooldown > 0) {
            pulseCooldown--;
        } else {
            for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(3.0D),
                    p -> !p.isCreative() && !p.isSpectator())) {
                doHurtTarget(p);
                Vec3 push = p.position().subtract(position()).normalize().scale(1.2D);
                p.push(push.x, 0.4D, push.z);
                p.hurtMarked = true;
                pulseCooldown = 60;
            }
            if (pulseCooldown == 60) {
                playSound(ModSounds.HIVE_NODE_PULSE.get(), 1.4F, 0.9F);
            }
        }
    }

    @Override
    public void travel(Vec3 input) {
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(0.91D));
    }

    /** Crea una avispa de la colmena junto al Nodo. */
    public void spawnWasp(WaspRole role) {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        SwarmWaspEntity w = ModEntities.SWARM_WASP.get().create(server);
        if (w == null) {
            return;
        }
        w.moveTo(getX() + random.nextGaussian(), getY() + 1.0D, getZ() + random.nextGaussian(), random.nextFloat() * 360F, 0);
        w.finalizeSpawn(server, server.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
        w.joinHive(this, role);
        server.addFreshEntity(w);
        playSound(ModSounds.SWARM_WASP_ALARM.get(), 0.6F, 1.3F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() instanceof LivingEntity attacker) {
            mind.provoked(attacker, attacker.distanceTo(this));
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) {
            mind.collapse();
        }
        super.die(source);
    }

    @Override
    public boolean causeFallDamage(float distance, float mult, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(1.0D);
    }

    // ------------------------------------------------------------------ sonidos

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.HIVE_NODE_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SWARM_BODY_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SWARM_BODY_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 1.6F;
    }

    // ------------------------------------------------------------------ guardado

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        CompoundTag m = new CompoundTag();
        mind.save(m);
        tag.put("Mind", m);
        if (home != null) {
            tag.put("Home", NbtUtils.writeBlockPos(home));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        mind.load(tag.getCompound("Mind"));
        if (tag.contains("Home")) {
            home = NbtUtils.readBlockPos(tag.getCompound("Home"));
        }
    }
}
