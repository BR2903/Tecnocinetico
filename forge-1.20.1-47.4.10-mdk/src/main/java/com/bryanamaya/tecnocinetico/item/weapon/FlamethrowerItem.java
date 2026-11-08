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
            // chorro: gotas ardiendo con la velocidad de la boquilla (count 0 = velocidad exacta), algo de dispersión
            Vec3 right = Vec3.directionFromRotation(0, player.getYRot() + 90.0F);
            Vec3 muzzle = eye.add(look.scale(1.1)).add(right.scale(aiming ? 0.0 : 0.22)).add(0, aiming ? -0.12 : -0.28, 0);
            var r = player.getRandom();
            for (int i = 0; i < 9; i++) {
                double sp = 0.72 + r.nextDouble() * 0.16;
                Vec3 v = look.scale(sp).add(r.nextGaussian() * 0.045, r.nextGaussian() * 0.045, r.nextGaussian() * 0.045)
                        .add(player.getDeltaMovement());
                Vec3 at = muzzle.add(look.scale(r.nextDouble() * 0.3));
                server.sendParticles(com.bryanamaya.tecnocinetico.particle.ModParticles.FLAME_STREAM.get(), at.x, at.y, at.z, 0, v.x, v.y, v.z, 1.0);
            }
            if (r.nextFloat() < 0.4F) {                                          // humo negro del combustible
                Vec3 sm = muzzle.add(look.scale(4 + r.nextDouble() * 3));
                server.sendParticles(ParticleTypes.LARGE_SMOKE, sm.x, sm.y + 0.5, sm.z, 1, 0.3, 0.2, 0.3, 0.01);
            }
        }
        if (player.tickCount % 6 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), com.bryanamaya.tecnocinetico.sound.ModSounds.FLAMETHROWER.get(),
                    SoundSource.PLAYERS, 0.9F, 0.9F + player.getRandom().nextFloat() * 0.15F);
        }
    }
}
