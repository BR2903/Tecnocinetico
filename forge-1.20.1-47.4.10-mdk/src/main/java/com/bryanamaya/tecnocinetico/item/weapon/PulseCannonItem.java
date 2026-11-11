package com.bryanamaya.tecnocinetico.item.weapon;

import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.item.custom.WeaponModule;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
import com.bryanamaya.tecnocinetico.skill.PlayerSkills;
import com.bryanamaya.tecnocinetico.skill.Skill;
import com.bryanamaya.tecnocinetico.sound.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Cañón de Impulso (tier 3): suelta un cono de onda cinética (8 bloques, ~70°) que hiere más cuanto más cerca,
 * lanza a los enemigos y los aturde (lentitud IV + debilidad 2 s). No usa munición: una Célula de Impulso da
 * 6 disparos; al recargar se gasta la célula y devuelve una Célula Vacía (se recarga en la Prensa Automática).
 */
public class PulseCannonItem extends LongGunItem {

    private static final double RANGE = 8.0D, CONE_COS = Math.cos(Math.toRadians(35));

    public PulseCannonItem(Stats stats, Properties properties) {
        super(WeaponModule.Family.HEAVY, Action.SEMI, stats, properties);
    }

    @Override
    protected int boxRefill(Player player, ItemStack gun) {
        if (getAmmo(gun) > 0 && !player.getAbilities().instabuild) {
            return 0;                                                            // solo con la célula agotada
        }
        if (player.getAbilities().instabuild || take(player)) {
            if (!player.getAbilities().instabuild) {
                ItemStack empty = new ItemStack(ModItems.EMPTY_PULSE_CELL.get());
                if (!player.getInventory().add(empty)) {
                    player.drop(empty, false);
                }
            }
            return stats().capacity() - getAmmo(gun);
        }
        return 0;
    }

    @Override
    protected void shoot(Level level, ServerPlayer player, ItemStack gun, boolean aiming, float charge) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        int hits = 0;
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RANGE),
                t -> t != player && t.isAlive() && !t.isSpectator())) {
            Vec3 to = t.position().add(0, t.getBbHeight() * 0.5, 0).subtract(eye);
            double d = to.length();
            if (d > RANGE || d < 1e-3 || to.normalize().dot(look) < CONE_COS) {
                continue;
            }
            if (level.clip(new ClipContext(eye, eye.add(to), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
                    .getType() != HitResult.Type.MISS) {
                continue;                                                          // detrás de una pared
            }
            if (com.bryanamaya.tecnocinetico.rules.FriendlyFire.blocks(player, t)) {                // sin fuego amigo: ni daño, ni empuje, ni efectos
                continue;
            }
            float dmg = (float) (damage(gun) * (1.0 - d / (RANGE + 2.0)));
            t.invulnerableTime = 0;
            if (t.hurt(level.damageSources().playerAttack(player), dmg)) {
                hits++;
                if (t instanceof Enemy) {
                    PlayerSkills.addXp(player, Skill.WEAPON_HANDLING, 1 + (t.isAlive() ? 0 : 4));
                }
            }
            double kb = 2.2 * (1.0 - d / (RANGE + 4.0)) * (1.0 - t.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) * 0.7);
            t.setDeltaMovement(t.getDeltaMovement().add(look.x * kb, 0.35 + Math.max(0, look.y) * kb * 0.5, look.z * kb));
            t.hurtMarked = true;
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3), player);
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1), player);
        }
        if (hits > 0) {
            player.playNotifySound(ModSounds.PISTOL_HIT.get(), SoundSource.PLAYERS, 0.5F, 0.6F);
        }
        WeaponEffects.send(player, gun, aiming, com.bryanamaya.tecnocinetico.network.WeaponEffectPacket.PULSE,
                eye.add(look.scale(RANGE)), true);
        Vec3 back = look.scale(-0.3);                                              // el cañón empuja al que dispara
        player.setDeltaMovement(player.getDeltaMovement().add(back.x, 0.0, back.z));
        player.hurtMarked = true;
    }
}
