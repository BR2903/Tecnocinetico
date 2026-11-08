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
        g.shootFromRotation(player, player.getXRot() - 4.0F, player.getYRot(), 0.0F, 1.35F, spread(gun, aiming));
        level.addFreshEntity(g);
        if (level instanceof ServerLevel server) {
            Vec3 m = player.getEyePosition().add(player.getLookAngle().scale(1.2)).add(0, -0.15, 0);
            server.sendParticles(ModParticles.KINETIC_BURST.get(), m.x, m.y, m.z, 0, 0.45, 0, 0, 1);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, m.x, m.y, m.z, 6, 0.06, 0.06, 0.06, 0.02);
        }
    }
}
