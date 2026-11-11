package com.bryanamaya.tecnocinetico.item.weapon;

import com.bryanamaya.tecnocinetico.particle.ModParticles;
import com.bryanamaya.tecnocinetico.skill.PlayerSkills;
import com.bryanamaya.tecnocinetico.skill.Skill;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Arco Voltaico (tier 3): rayo de cinetrita contra el primer ser vivo en la mira (24 bloques) que salta en cadena
 * a otros 4 cercanos (6 bloques), con un 20 % menos de daño en cada salto. Usa Células de Impulso como el Cañón
 * de Impulso (12 descargas por célula; devuelve la célula vacía).
 */
public class ArcCasterItem extends PulseCannonItem {

    private static final double RANGE = 24.0D, JUMP = 6.0D;
    private static final int CHAIN = 4;

    public ArcCasterItem(Stats stats, Properties properties) {
        super(stats, properties);
    }

    @Override
    protected void shoot(Level level, ServerPlayer player, ItemStack gun, boolean aiming, float charge) {
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle(), end = eye.add(look.scale(RANGE));
        var block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, end, new AABB(eye, end).inflate(1.0),
                e -> e instanceof LivingEntity && !e.isSpectator() && e.isPickable(), 0.6F);
        Vec3 from = WeaponEffects.muzzle(player, gun, aiming);
        if (hit == null) {
            WeaponEffects.send(player, gun, aiming, com.bryanamaya.tecnocinetico.network.WeaponEffectPacket.ARC, end, true);
            return;
        }
        List<Entity> struck = new ArrayList<>();
        Entity cur = hit.getEntity();
        float dmg = damage(gun);
        for (int i = 0; i <= CHAIN && cur != null; i++) {
            Vec3 at = cur.getBoundingBox().getCenter();
            if (i == 0) WeaponEffects.send(player, gun, aiming, com.bryanamaya.tecnocinetico.network.WeaponEffectPacket.ARC, at, true);
            else bolt(level, from, at);
            cur.invulnerableTime = 0;
            if (cur.hurt(level.damageSources().playerAttack(player), dmg) && cur instanceof Enemy) {
                PlayerSkills.addXp(player, Skill.WEAPON_HANDLING, 1 + (cur.isAlive() ? 0 : 4));
            }
            struck.add(cur);
            dmg *= 0.8F;
            from = at;
            Vec3 fromF = from;
            cur = level.getEntitiesOfClass(LivingEntity.class, cur.getBoundingBox().inflate(JUMP),
                            e -> e != player && e.isAlive() && !struck.contains(e) && !e.isSpectator())
                    .stream().min(Comparator.comparingDouble(e -> e.getBoundingBox().getCenter().distanceToSqr(fromF))).orElse(null);
        }
        if (!struck.isEmpty()) {                                                  // chasquido en el último alcanzado
            Entity last = struck.get(struck.size() - 1);
            level.playSound(null, last.getX(), last.getY(), last.getZ(), com.bryanamaya.tecnocinetico.sound.ModSounds.ARC_FIRE.get(),
                    SoundSource.PLAYERS, 0.6F, 1.3F);
        }
    }

    /** Rayo quebrado entre dos puntos. */
    private static void bolt(Level level, Vec3 a, Vec3 b) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        Vec3 path = b.subtract(a);
        int steps = Math.max(2, (int) (path.length() * 3));
        var r = server.getRandom();
        for (int i = 0; i <= steps; i++) {
            double k = i / (double) steps;
            double j = (i == 0 || i == steps) ? 0 : 0.25;
            Vec3 p = a.add(path.scale(k)).add((r.nextDouble() - 0.5) * j, (r.nextDouble() - 0.5) * j, (r.nextDouble() - 0.5) * j);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            if (i % 3 == 0) {
                server.sendParticles(ModParticles.KINETIC_TRACER.get(), p.x, p.y, p.z, 0, 0, 0, 0, 0);
            }
        }
        server.sendParticles(ModParticles.KINETIC_SPARK.get(), b.x, b.y, b.z, 6, 0.1, 0.1, 0.1, 0.2);
    }
}
