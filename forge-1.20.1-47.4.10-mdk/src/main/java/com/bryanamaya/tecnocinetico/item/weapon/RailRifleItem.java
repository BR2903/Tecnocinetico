package com.bryanamaya.tecnocinetico.item.weapon;

import com.bryanamaya.tecnocinetico.item.custom.WeaponModule;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
import com.bryanamaya.tecnocinetico.skill.PlayerSkills;
import com.bryanamaya.tecnocinetico.skill.Skill;
import com.bryanamaya.tecnocinetico.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Rifle de Riel (tier 3): mantener el clic carga el disparo (1,5 s); al soltar dispara una Bala de Riel con un
 * daño proporcional a la carga (mínimo 30 %). Atraviesa hasta 4 enemigos (cada uno quita un 25 % al siguiente) y
 * bloques blandos (cristal, hojas, lana, plantas...). Se detiene en el primer bloque duro.
 */
public class RailRifleItem extends LongGunItem {

    public static final int CHARGE_TICKS = 30;
    public static final float MIN_CHARGE = 0.3F;
    private static final int MAX_PIERCE = 4, MAX_SOFT_BLOCKS = 6;

    public RailRifleItem(Stats stats, Properties properties) {
        super(WeaponModule.Family.HEAVY, Action.CHARGE, stats, properties);
    }

    /** Bloque que la bala atraviesa (casi sin dureza). */
    public static boolean soft(BlockState st, Level level, BlockPos pos) {
        float h = st.getDestroySpeed(level, pos);
        return h >= 0 && h < 1.0F;
    }

    @Override
    protected void shoot(Level level, ServerPlayer player, ItemStack gun, boolean aiming, float charge) {
        charge = Math.max(MIN_CHARGE, Math.min(1.0F, charge));
        Vec3 eye = player.getEyePosition();
        Vec3 dir = spread(player.getLookAngle(), spread(gun, aiming), player.getRandom());
        double range = range(gun);
        // recorrido: atraviesa bloques blandos hasta el primero duro
        Vec3 from = eye, end = eye.add(dir.scale(range));
        BlockHitResult stop = null;
        for (int i = 0; i <= MAX_SOFT_BLOCKS; i++) {
            BlockHitResult hit = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.MISS) {
                break;
            }
            BlockState st = level.getBlockState(hit.getBlockPos());
            if (i == MAX_SOFT_BLOCKS || !soft(st, level, hit.getBlockPos())) {
                stop = hit;
                end = hit.getLocation();
                break;
            }
            if (level instanceof ServerLevel server) {
                server.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, st),
                        hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, 8, 0.1, 0.1, 0.1, 0.15);
            }
            from = hit.getLocation().add(dir.scale(0.05));
            // salta el resto del bloque blando
            AABB box = new AABB(hit.getBlockPos());
            Optional<Vec3> out = box.clip(from.add(dir.scale(2.0)), from);
            from = out.orElse(from).add(dir.scale(0.05));
        }
        // enemigos en la línea, del más cercano al más lejano
        Vec3 fEnd = end;
        List<Entity> hits = level.getEntities(player, new AABB(eye, end).inflate(1.0D),
                        e -> e instanceof LivingEntity && e.isPickable() && !e.isSpectator()
                                && e.getBoundingBox().inflate(0.3D).clip(eye, fEnd).isPresent())
                .stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(eye))).limit(MAX_PIERCE).toList();
        float dmg = damage(gun) * charge;
        for (Entity t : hits) {
            if (com.bryanamaya.tecnocinetico.rules.FriendlyFire.blocks(player, t)) {                // sin fuego amigo: lo atraviesa sin empujarlo
                continue;
            }
            t.invulnerableTime = 0;
            boolean hurt = t.hurt(level.damageSources().playerAttack(player), dmg);
            if (hurt && t instanceof Enemy) {
                PlayerSkills.addXp(player, Skill.WEAPON_HANDLING, 1 + (t.isAlive() ? 0 : 4));
            }
            t.push(dir.x * 0.6 * charge, 0.15, dir.z * 0.6 * charge);
            dmg *= 0.75F;
        }
        if (!hits.isEmpty()) {
            player.playNotifySound(ModSounds.PISTOL_HIT.get(), SoundSource.PLAYERS, 0.6F, 0.7F);
        }
        if (level instanceof ServerLevel server) {
            effects(server, player, dir, end, stop, aiming, true);
            server.sendParticles(ModParticles.KINETIC_SPARK.get(), end.x, end.y, end.z, 16, 0.1, 0.1, 0.1, 0.3);
        }
        if (charge >= 0.99F) {                                                // retroceso a carga completa
            Vec3 back = dir.scale(-0.35);
            player.setDeltaMovement(player.getDeltaMovement().add(back.x, 0.05, back.z));
            player.hurtMarked = true;
        }
    }
}
