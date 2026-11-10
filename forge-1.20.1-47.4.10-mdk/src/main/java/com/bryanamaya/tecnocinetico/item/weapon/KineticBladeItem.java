package com.bryanamaya.tecnocinetico.item.weapon;

import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Cuchilla Cinética (tier 2): espada de inercita. Cada golpe a un enemigo acumula carga (máx. 5, barra cian); con
 * la carga llena, clic derecho suelta un tajo de onda en arco (6 bloques, 14 de daño, empuja) que la vacía.
 */
public class KineticBladeItem extends SwordItem {

    public static final int MAX_CHARGE = 5;
    private static final String TAG = "BladeCharge";

    public static final Tier INERCITA = new Tier() {
        @Override
        public int getUses() {
            return 1100;
        }

        @Override
        public float getSpeed() {
            return 8.0F;
        }

        @Override
        public float getAttackDamageBonus() {
            return 4.0F;
        }

        @Override
        public int getLevel() {
            return 3;
        }

        @Override
        public int getEnchantmentValue() {
            return 14;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.INERCITA_PLATE.get());
        }
    };

    public KineticBladeItem(Properties properties) {
        super(INERCITA, 4, -2.2F, properties);                                    // 12 de daño, 1,8 golpes/s
    }

    public static int charge(ItemStack s) {
        return s.getTag() == null ? 0 : s.getTag().getInt(TAG);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.getOrCreateTag().putInt(TAG, Math.min(MAX_CHARGE, charge(stack) + 1));
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (charge(stack) < MAX_CHARGE) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            stack.getOrCreateTag().putInt(TAG, 0);
            Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
            for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6.0),
                    t -> t != player && t.isAlive() && !t.isSpectator())) {
                Vec3 to = t.getBoundingBox().getCenter().subtract(eye);
                if (to.length() <= 6.5 && to.normalize().dot(look) > Math.cos(Math.toRadians(60)) && !com.bryanamaya.tecnocinetico.rules.FriendlyFire.blocks(player, t)) {
                    t.invulnerableTime = 0;
                    t.hurt(level.damageSources().playerAttack(player), 14.0F);
                    t.knockback(1.2, -look.x, -look.z);
                }
            }
            if (level instanceof ServerLevel server) {
                for (int i = -10; i <= 10; i++) {                                 // arco de la onda
                    Vec3 d = look.yRot((float) Math.toRadians(i * 6));
                    for (double r = 1.5; r <= 6; r += 1.5) {
                        Vec3 p = eye.add(d.scale(r)).add(0, -0.4, 0);
                        server.sendParticles(ModParticles.KINETIC_SPARK.get(), p.x, p.y, p.z, 1, 0, 0, 0, 0.02);
                    }
                }
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK, eye.x + look.x * 2, eye.y - 0.3,
                        eye.z + look.z * 2, 3, 0.8, 0.1, 0.8, 0);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), com.bryanamaya.tecnocinetico.sound.ModSounds.BLADE_WAVE.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
            stack.hurtAndBreak(3, player, p -> p.broadcastBreakEvent(hand));
            player.getCooldowns().addCooldown(this, 20);
        }
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return charge(stack) > 0 || stack.isDamaged();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return charge(stack) > 0 ? Math.round(13.0F * charge(stack) / MAX_CHARGE) : super.getBarWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return charge(stack) > 0 ? (charge(stack) >= MAX_CHARGE ? 0xB4F8FF : 0x46E3F5) : super.getBarColor(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tecnocinetico.kinetic_blade", charge(stack), MAX_CHARGE).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.tecnocinetico.kinetic_blade.desc").withStyle(ChatFormatting.DARK_GRAY));
    }
}
