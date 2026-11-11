package com.bryanamaya.tecnocinetico.item.weapon;

import com.bryanamaya.tecnocinetico.entity.custom.KineticGrenadeEntity;
import com.bryanamaya.tecnocinetico.item.custom.WeaponModule;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Lanzagranadas (tier 3): tambor de 4 Granadas Cinéticas que se cargan una a una. La granada sale en parábola,
 * rebota en las paredes y estalla al tocar a un enemigo o a los 2,5 s (daño en área, no rompe bloques).
 */
public class GrenadeLauncherItem extends LongGunItem {

    public GrenadeLauncherItem(Stats stats, Properties properties) {
        super(WeaponModule.Family.HEAVY, Action.DRUM, stats, properties);
    }

    @Override
    protected void shoot(Level level, ServerPlayer player, ItemStack gun, boolean aiming, float charge) {
        KineticGrenadeEntity g = new KineticGrenadeEntity(level, player);
        Vec3 from = WeaponEffects.muzzle(player, gun, aiming);
        g.setPos(from.x, from.y, from.z);
        g.shootFromRotation(player, player.getXRot() - 4.0F, player.getYRot(), 0.0F, 1.35F, spread(gun, aiming));
        level.addFreshEntity(g);
        if (level instanceof ServerLevel server) {
            WeaponEffects.send(player, gun, aiming, com.bryanamaya.tecnocinetico.network.WeaponEffectPacket.FLASH, from, true);
        }
    }
}
