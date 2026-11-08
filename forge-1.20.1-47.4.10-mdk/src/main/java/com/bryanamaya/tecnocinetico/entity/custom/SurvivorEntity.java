package com.bryanamaya.tecnocinetico.entity.custom;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

/**
 * Superviviente: NPC de los Asentamientos. Usa el sistema de comercio vanilla (interfaz de aldeano) con la
 * Chatarra como moneda. Tres oficios ({@link Role}) con aspecto y ofertas propias; las existencias se
 * reponen cada día. Pasea cerca de su casa (el asentamiento), abre puertas y huye de los monstruos.
 */
public class SurvivorEntity extends AbstractVillager {

    public enum Role {
        SCRAPPER("scrapper"), GUNSMITH("gunsmith"), FARMER("farmer");

        public final String id;

        Role(String id) {
            this.id = id;
        }

        public static Role byId(int i) {
            return values()[Math.floorMod(i, values().length)];
        }
    }

    private static final EntityDataAccessor<Integer> ROLE =
            SynchedEntityData.defineId(SurvivorEntity.class, EntityDataSerializers.INT);
    /** Radio alrededor de su casa por el que pasea. */
    public static final int HOME_RADIUS = 16;

    private long lastRestockDay = -1;
    @Nullable
    private BlockPos home;

    public SurvivorEntity(EntityType<? extends AbstractVillager> type, Level level) {
        super(type, level);
        ((GroundPathNavigation) getNavigation()).setCanOpenDoors(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Monster.class, 10.0F, 0.55D, 0.7D));
        goalSelector.addGoal(1, new PanicGoal(this, 0.7D));
        goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        goalSelector.addGoal(2, new OpenDoorGoal(this, true));
        goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 0.45D));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.4D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ROLE, 0);
    }

    public Role getRole() {
        return Role.byId(entityData.get(ROLE));
    }

    public void setRole(Role role) {
        entityData.set(ROLE, role.ordinal());
        offers = null;                                         // se regeneran con el oficio nuevo
        equipForRole();
    }

    /** Fija la casa (centro del asentamiento) por la que pasea. */
    public void setHome(BlockPos pos) {
        home = pos.immutable();
        restrictTo(home, HOME_RADIUS);
    }

    private void equipForRole() {
        Item tool = switch (getRole()) {
            case GUNSMITH -> ModItems.KINETIC_HAMMER.get();
            case FARMER -> Items.IRON_HOE;
            default -> null;
        };
        setItemSlot(EquipmentSlot.MAINHAND, tool == null ? ItemStack.EMPTY : new ItemStack(tool));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        setRole(Role.byId(random.nextInt(Role.values().length)));
        if (home == null) {
            setHome(blockPosition());
        }
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    // ------------------------------------------------------------------ nombre por oficio

    @Override
    protected Component getTypeName() {
        return Component.translatable("entity." + Tecnocinetico.MOD_ID + ".survivor." + getRole().id);
    }

    // ------------------------------------------------------------------ comercio

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isAlive() && !isTrading() && !isBaby()) {
            if (!level().isClientSide) {
                if (getOffers().isEmpty()) {
                    return InteractionResult.CONSUME;
                }
                setTradingPlayer(player);
                openTradingScreen(player, getDisplayName(), 1);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected void updateTrades() {
        MerchantOffers offers = getOffers();
        for (MerchantOffer o : SurvivorTrades.forRole(getRole())) {
            offers.add(o);
        }
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        if (offer.shouldRewardExp()) {
            level().addFreshEntity(new net.minecraft.world.entity.ExperienceOrb(level(), getX(), getY() + 0.5D, getZ(),
                    1 + random.nextInt(3)));
        }
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        long day = level().getDayTime() / 24000L;
        if (day != lastRestockDay) {                           // reponer existencias cada día
            lastRestockDay = day;
            getOffers().forEach(MerchantOffer::resetUses);
        }
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // ------------------------------------------------------------------ sonidos (humanos, no de aldeano)

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    protected SoundEvent getTradeUpdatedSound(boolean success) {
        return success ? SoundEvents.BUNDLE_INSERT : SoundEvents.BUNDLE_REMOVE_ONE;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    // ------------------------------------------------------------------ guardado

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Role", getRole().ordinal());
        tag.putLong("LastRestock", lastRestockDay);
        if (home != null) {
            tag.put("Home", NbtUtils.writeBlockPos(home));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ROLE, tag.getInt("Role"));
        lastRestockDay = tag.getLong("LastRestock");
        if (tag.contains("Home")) {
            setHome(NbtUtils.readBlockPos(tag.getCompound("Home")));
        }
    }
}
