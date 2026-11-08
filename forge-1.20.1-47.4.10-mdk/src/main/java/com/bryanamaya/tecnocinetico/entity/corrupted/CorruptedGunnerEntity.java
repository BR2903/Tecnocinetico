package com.bryanamaya.tecnocinetico.entity.corrupted;

import com.bryanamaya.tecnocinetico.entity.custom.CoreShots;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.sound.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * Tirador Corrupto: superviviente infectado que aún dispara (sustituye a esqueletos y strays en el Overworld).
 * Hereda de AbstractSkeleton para que los generadores de mazmorra cuenten bien a los que hay cerca, pero la IA de
 * arco se sustituye por {@link GunnerAttackGoal}. Arma según el azar:
 * - pistola (55 %): ráfagas de 2 tiros de 3, 18 bloques;
 * - escopeta (30 %): 6 perdigones de 2 a corta distancia, se acerca;
 * - rifle (15 %): carga con línea roja que se fija al final, 11 de daño, 30 bloques, se queda lejos.
 * El arma es solo visual: nunca se suelta (no regala progresión); a veces suelta munición de ese tipo.
 */
public class CorruptedGunnerEntity extends AbstractSkeleton implements CorruptedSkin {

    public enum Weapon { PISTOL, SHOTGUN, RIFLE }

    private static final EntityDataAccessor<Byte> VARIANT = SynchedEntityData.defineId(CorruptedGunnerEntity.class, EntityDataSerializers.BYTE);
    private static final String[] SKINS = {"raider", "soldier"};

    public CorruptedGunnerEntity(EntityType<? extends AbstractSkeleton> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractSkeleton.createAttributes()
                .add(Attributes.MAX_HEALTH, 22.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.FOLLOW_RANGE, 36.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new GunnerAttackGoal());
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    @Override
    public void reassessWeaponGoal() {
        // sin arco: la IA de disparo es GunnerAttackGoal
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

    public Weapon weapon() {
        Item held = getMainHandItem().getItem();
        if (held == ModItems.PIPE_SHOTGUN.get()) {
            return Weapon.SHOTGUN;
        }
        if (held == ModItems.BOLT_RIFLE.get()) {
            return Weapon.RIFLE;
        }
        return Weapon.PISTOL;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        entityData.set(VARIANT, (byte) random.nextInt(SKINS.length));
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource r, DifficultyInstance difficulty) {
        float w = r.nextFloat();
        Item gun = w < 0.55F ? ModItems.KINETIC_PISTOL.get() : w < 0.85F ? ModItems.PIPE_SHOTGUN.get() : ModItems.BOLT_RIFLE.get();
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(gun));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        if (r.nextFloat() < 0.08F + 0.12F * difficulty.getSpecialMultiplier()) {
            setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.SCRAP_HELMET.get()));
            setDropChance(EquipmentSlot.HEAD, 0.04F);
        }
    }

    @Override
    protected void populateDefaultEquipmentEnchantments(RandomSource r, DifficultyInstance difficulty) {
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (recentlyHit && random.nextFloat() < 0.35F + 0.1F * looting) {          // munición de su arma
            Item ammo = switch (weapon()) {
                case PISTOL -> ModItems.KINETIC_ROUND.get();
                case SHOTGUN -> ModItems.SHOTGUN_SHELL.get();
                case RIFLE -> ModItems.RIFLE_ROUND.get();
            };
            spawnAtLocation(new ItemStack(ammo, 1 + random.nextInt(weapon() == Weapon.PISTOL ? 4 : 2)));
        }
    }

    @Override
    protected boolean isSunBurnTick() {
        return false;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        // no se usa (no tiene la IA de arco); el disparo lo hace GunnerAttackGoal
    }

    // ------------------------------------------------------------------ sonidos (voz humana rota, no huesos)

    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.ZOMBIE_STEP;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.85F;
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

    // ------------------------------------------------------------------ combate

    /** Acercarse hasta su distancia preferida, moverse de lado y disparar según el arma. */
    class GunnerAttackGoal extends Goal {
        private int cooldown = 20, shotsLeft, shotTimer, charge, seeTime, strafeTime;
        private boolean strafeLeft;
        private Vec3 lockPoint = Vec3.ZERO;

        GunnerAttackGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public void start() {
            setAggressive(true);
        }

        @Override
        public void stop() {
            setAggressive(false);
            charge = 0;
            shotsLeft = 0;
            getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) {
                return;
            }
            Weapon w = weapon();
            double range = w == Weapon.SHOTGUN ? 10.0D : w == Weapon.RIFLE ? 30.0D : 18.0D;
            double keep = w == Weapon.SHOTGUN ? 4.0D : w == Weapon.RIFLE ? 16.0D : 9.0D;
            double dist = distanceTo(t);
            boolean see = getSensing().hasLineOfSight(t);
            seeTime = see ? seeTime + 1 : 0;
            getLookControl().setLookAt(t, 30.0F, 30.0F);

            // movimiento
            if (!see || dist > range * 0.85D) {
                getNavigation().moveTo(t, 1.05D);
            } else {
                getNavigation().stop();
                if (++strafeTime > 25 + random.nextInt(30)) {
                    strafeLeft = !strafeLeft;
                    strafeTime = 0;
                }
                float forward = dist > keep + 3 ? 0.4F : dist < keep - 2 ? -0.5F : 0.0F;
                getMoveControl().strafe(forward, strafeLeft ? 0.45F : -0.45F);
                lookAt(t, 30.0F, 30.0F);
            }

            // disparo
            if (cooldown > 0) {
                cooldown--;
            }
            Vec3 muzzle = getEyePosition().subtract(0, 0.25, 0).add(getViewVector(1.0F).scale(0.6));
            if (charge > 0) {                                                   // rifle: carga con aviso
                charge++;
                if (charge <= 22) {
                    lockPoint = CoreShots.aimPoint(t);                          // sigue... y se fija los últimos 8 ticks
                }
                if (charge % 2 == 0) {
                    CoreShots.laser(CorruptedGunnerEntity.this, muzzle, lockPoint.add(lockPoint.subtract(muzzle).normalize().scale(3)));
                }
                if (charge >= 30) {
                    CoreShots.fire(CorruptedGunnerEntity.this, muzzle, lockPoint.subtract(muzzle), range + 4, 11.0F, 0.0,
                            CorruptedGunnerEntity.class, true);
                    playSound(ModSounds.PISTOL_FIRE.get(), 1.6F, 0.55F);
                    swing(InteractionHand.MAIN_HAND);
                    charge = 0;
                    cooldown = 45;
                }
                return;
            }
            if (shotsLeft > 0) {                                                // pistola: ráfaga
                if (--shotTimer <= 0) {
                    CoreShots.fire(CorruptedGunnerEntity.this, muzzle, CoreShots.aimPoint(t).subtract(muzzle), range + 4, 3.0F,
                            3.5, CorruptedGunnerEntity.class, false);
                    playSound(ModSounds.PISTOL_FIRE.get(), 1.0F, 1.1F + random.nextFloat() * 0.15F);
                    swing(InteractionHand.MAIN_HAND);
                    shotsLeft--;
                    shotTimer = 4;
                }
                return;
            }
            if (cooldown > 0 || !see || seeTime < 8 || dist > range) {
                return;
            }
            switch (w) {
                case PISTOL -> {
                    shotsLeft = 2;
                    shotTimer = 0;
                    cooldown = 26;
                }
                case SHOTGUN -> {
                    for (int i = 0; i < 6; i++) {
                        CoreShots.fire(CorruptedGunnerEntity.this, muzzle, CoreShots.aimPoint(t).subtract(muzzle), range + 2, 2.0F,
                                7.0, CorruptedGunnerEntity.class, false);
                    }
                    playSound(ModSounds.PISTOL_FIRE.get(), 1.5F, 0.7F);
                    swing(InteractionHand.MAIN_HAND);
                    cooldown = 40;
                }
                case RIFLE -> {
                    charge = 1;
                    lockPoint = CoreShots.aimPoint(t);
                    playSound(ModSounds.SENTRY_TURRET_CHARGE.get(), 0.6F, 1.7F);
                }
            }
        }
    }
}
