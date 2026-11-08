package com.bryanamaya.tecnocinetico.item.weapon;

import com.bryanamaya.tecnocinetico.sound.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Minigun (tier 3): mantener el clic hace girar los cañones (~1 s) y solo entonces dispara, a 20 disparos por
 * segundo. Al soltar, los cañones se frenan poco a poco (si vuelves a disparar enseguida no hay que esperar tanto).
 * Frena mucho al jugador mientras la lleva en la mano.
 */
public class MinigunItem extends HeavyGunItem {

    private static final String TAG_SPIN = "Spin", TAG_SPIN_T = "SpinTick";

    public MinigunItem(Stats stats, Properties properties) {
        super(0.35, stats, properties);
    }

    public static float spin(ItemStack s) {
        return s.getTag() == null ? 0.0F : s.getTag().getFloat(TAG_SPIN);
    }

    @Override
    protected boolean readyToFire(ServerPlayer player, ItemStack gun) {
        CompoundTag tag = gun.getOrCreateTag();
        long now = player.level().getGameTime();
        if (tag.getLong(TAG_SPIN_T) == now) {
            return tag.getFloat(TAG_SPIN) >= 1.0F;
        }
        float spin = Math.min(1.0F, tag.getFloat(TAG_SPIN) + 0.05F);
        tag.putFloat(TAG_SPIN, spin);
        tag.putLong(TAG_SPIN_T, now);
        if (spin <= 0.05F + 1e-4F) {                                           // los cañones arrancan (dura 1 s, como el giro)
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MINIGUN_SPIN.get(),
                    SoundSource.PLAYERS, 0.9F, 1.0F);
        }
        return spin >= 1.0F;
    }

    @Override
    public void inventoryTick(ItemStack gun, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(gun, level, entity, slot, selected);
        if (!level.isClientSide && gun.getTag() != null && gun.getTag().getFloat(TAG_SPIN) > 0
                && level.getGameTime() - gun.getTag().getLong(TAG_SPIN_T) > 2) {
            gun.getTag().putFloat(TAG_SPIN, Math.max(0.0F, gun.getTag().getFloat(TAG_SPIN) - 0.03F));   // se frena
        }
    }
}
