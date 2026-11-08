package com.bryanamaya.tecnocinetico.item.weapon;

import com.bryanamaya.tecnocinetico.entity.custom.GuidedRocketEntity;
import com.bryanamaya.tecnocinetico.item.custom.WeaponModule;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Lanzacohetes (tier 3): un Cohete Guiado cada vez. Mientras lo tengas en la mano, el cohete sigue el punto al que
 * apuntas (guía láser); si lo sueltas o cambias de objeto, sigue recto. Explosión grande que no rompe bloques.
 */
public class RocketLauncherItem extends LongGunItem {

    public RocketLauncherItem(Stats stats, Properties properties) {
        super(WeaponModule.Family.HEAVY, Action.DRUM, stats, properties);
    }

    @Override
    protected void shoot(Level level, ServerPlayer player, ItemStack gun, boolean aiming, float charge) {
        Vec3 look = player.getLookAngle();
        GuidedRocketEntity r = new GuidedRocketEntity(level, player);
        Vec3 from = player.getEyePosition().add(look.scale(1.2)).add(0, -0.15, 0);
        r.setPos(from.x, from.y, from.z);
        r.setDeltaMovement(look.scale(0.9));
        level.addFreshEntity(r);
        if (level instanceof ServerLevel server) {
            Vec3 back = player.getEyePosition().subtract(look.scale(1.0));
            server.sendParticles(ParticleTypes.LARGE_SMOKE, back.x, back.y, back.z, 10, 0.2, 0.2, 0.2, 0.05);   // rebufo
            server.sendParticles(ModParticles.KINETIC_BURST.get(), from.x, from.y, from.z, 0, 0.45, 0, 0, 1);
        }
    }
}
