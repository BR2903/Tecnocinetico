package com.bryanamaya.tecnocinetico.item.weapon;

import com.bryanamaya.tecnocinetico.entity.custom.KineticGrenadeEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Granada Cinética: munición del Lanzagranadas y también granada de mano. Mantener el clic derecho quita la anilla
 * y prepara el lanzamiento (más fuerza cuanto más tiempo, hasta 1 s); al soltar se lanza. La mecha (2,5 s) empieza
 * al quitar la anilla: se puede "cocinar" para que estalle antes, pero si se agota en la mano, estalla en la mano.
 * Lanzada a mano rebota y no estalla al tocar a un enemigo (la del lanzagranadas sí).
 */
public class KineticGrenadeItem extends Item {

    public static final int FUSE = KineticGrenadeEntity.FUSE, FULL_POWER = 20;

    public KineticGrenadeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIPWIRE_CLICK_OFF, SoundSource.PLAYERS,
                0.9F, 1.6F);                                                     // anilla fuera
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;                                                    // brazo atrás en tercera persona
    }

    /** Ticks que lleva sin anilla (para la animación y la mecha). */
    public static int heldTicks(ItemStack stack, int remaining) {
        return 72000 - remaining;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        int held = heldTicks(stack, remaining);
        if (!level.isClientSide && held >= FUSE) {                               // demasiado tiempo: estalla en la mano
            KineticGrenadeEntity g = new KineticGrenadeEntity(level, user);
            g.setPos(user.getX(), user.getEyeY() - 0.4, user.getZ());
            g.setFuse(0);
            level.addFreshEntity(g);
            consume(user, stack);
            user.stopUsingItem();
        } else if (!level.isClientSide && held > 0 && held % 10 == 0) {
            level.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS,
                    0.5F, 1.2F + held / (float) FUSE);                           // tic-tac de la mecha
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        int held = heldTicks(stack, remaining);
        if (level.isClientSide || held >= FUSE) {
            return;
        }
        float power = Math.min(1.0F, held / (float) FULL_POWER);
        KineticGrenadeEntity g = new KineticGrenadeEntity(level, user);
        g.setFuse(FUSE - held);
        g.setImpact(false);
        g.shootFromRotation(user, user.getXRot() - 8.0F, user.getYRot(), 0.0F, 0.4F + 0.9F * power, 1.0F);
        level.addFreshEntity(g);
        level.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.8F, 0.6F);
        consume(user, stack);
        if (user instanceof Player p) {
            p.getCooldowns().addCooldown(this, 20);
            p.swing(p.getUsedItemHand(), true);
        }
    }

    private static void consume(LivingEntity user, ItemStack stack) {
        if (!(user instanceof Player p && p.getAbilities().instabuild)) {
            stack.shrink(1);
        }
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new com.bryanamaya.tecnocinetico.client.GrenadeClientExtensions());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tecnocinetico.kinetic_grenade").withStyle(ChatFormatting.GRAY));
    }
}
