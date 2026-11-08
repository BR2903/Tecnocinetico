package com.bryanamaya.tecnocinetico.item.weapon;

import com.bryanamaya.tecnocinetico.item.custom.WeaponModule;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Lanzallamas (tier 2): mientras se mantiene el clic suelta un chorro de fuego en cono (7 bloques) que quema y
 * hiere a lo que alcance (atraviesa enemigos, no paredes). Usa un Depósito de Combustible por recarga (100
 * unidades, 1 por disparo, 10 disparos/s). No prende bloques.
 */
public class FlamethrowerItem extends LongGunItem {

    private static final double RANGE = 7.0D, CONE_COS = Math.cos(Math.toRadians(14));

    public FlamethrowerItem(Stats stats, Properties properties) {
        super(WeaponModule.Family.HEAVY, Action.AUTO, stats, properties);
    }

    @Override
    protected int boxRefill(Player player, ItemStack gun) {
        if (getAmmo(gun) > stats().capacity() / 2 && !player.getAbilities().instabuild) {
            return 0;                                                            // solo se cambia el depósito medio vacío
        }
        return player.getAbilities().instabuild || take(player) ? stats().capacity() - getAmmo(gun) : 0;
    }

    @Override
    protected void shoot(Level level, ServerPlayer player, ItemStack gun, boolean aiming, float charge) {
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RANGE),
                t -> t != player && t.isAlive() && !t.isSpectator())) {
            Vec3 to = t.getBoundingBox().getCenter().subtract(eye);
            if (to.length() > RANGE + 0.5 || to.normalize().dot(look) < CONE_COS) {
                continue;
            }
            if (level.clip(new ClipContext(eye, eye.add(to), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
                    .getType() != HitResult.Type.MISS) {
                continue;
            }
            t.invulnerableTime = 0;
            t.hurt(level.damageSources().playerAttack(player), damage(gun));
            t.setSecondsOnFire(6);
        }
        if (level instanceof ServerLevel server) {
            Vec3 muzzle = eye.add(look.scale(0.9)).add(0, -0.25, 0);
            for (int i = 0; i < 6; i++) {
                double d = 0.5 + player.getRandom().nextDouble() * RANGE;
                double sp = 0.12 * d;
                Vec3 p = muzzle.add(look.scale(d));
                server.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 2, sp * 0.3, sp * 0.3, sp * 0.3, 0.01);
            }
            server.sendParticles(ParticleTypes.LARGE_SMOKE, muzzle.x + look.x * RANGE, muzzle.y + look.y * RANGE, muzzle.z + look.z * RANGE,
                    2, 0.4, 0.4, 0.4, 0.01);
        }
        if (player.tickCount % 4 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.5F, 0.6F);
        }
    }
}
