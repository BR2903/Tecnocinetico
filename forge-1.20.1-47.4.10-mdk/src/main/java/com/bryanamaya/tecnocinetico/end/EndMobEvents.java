package com.bryanamaya.tecnocinetico.end;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem;
import com.bryanamaya.tecnocinetico.nether.NetherMobEvents;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.Comparator;

/**
 * End, parte 1: los mobs vanilla del End están <b>corruptos</b> por los restos de la red del Núcleo Directriz (la IA
 * vivió aquí antes de mudarse a otro planeta). Solo dentro del End; fuera vuelven a ser vanilla (como en el Nether).
 * <ul>
 *   <li><b>Enderman</b>: 60 de vida, 6 de armadura, 10 de daño. Hostil al ver a un jugador en supervivencia a menos de
 *       {@link #SIGHT} bloques (la armadura completa de Nulita lo evita, como fuera). <b>Salto de desfase</b>: si el
 *       objetivo está lejos, se llena de glitches 1 s (aviso) y aparece a su espalda golpeando.</li>
 *   <li><b>Shulker</b>: 45 de vida. <b>Ráfaga</b>: cada 6 s, tras un aviso de glitches, suelta 3 balas a la vez.</li>
 *   <li><b>Endermite</b>: 16 de vida y 4 de daño.</li>
 * </ul>
 * Rendimiento: igual que {@link NetherMobEvents}, cada mob trabaja 1 de cada 10 ticks con su reloj desfasado.
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID)
public final class EndMobEvents {

    private static final String UPGRADED = "EndCorrupted", OLD_HEALTH = "EndOriginalHealth", OLD_ARMOR = "EndOriginalArmor",
            OLD_DAMAGE = "EndOriginalDamage";
    /** Alcance de la hostilidad de los endermen (hay muchos en el End: más corto que en el Nether). */
    public static final double SIGHT = 16.0D;
    /** Ciclo de habilidades (ticks): aviso en {@link #WARN}, ataque en {@link #STRIKE}. */
    public static final int CYCLE = 120, WARN = 90, STRIKE = 100;
    public static final int VOLLEY = 3;

    private EndMobEvents() {}

    public static boolean inEnd(Level level) {
        return level.dimension() == Level.END;
    }

    // ------------------------------------------------------------------ mejora al entrar al mundo

    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        boolean upgraded = mob.getPersistentData().getBoolean(UPGRADED);
        if (!inEnd(event.getLevel()) && !com.bryanamaya.tecnocinetico.entity.MobVariants.is(mob, com.bryanamaya.tecnocinetico.entity.MobVariants.END)) {           // los de huevo de Tecnocinético: en todas partes
            if (upgraded) {
                restore(mob);                                                 // salió del End: vuelve a ser vanilla
            }
            return;
        }
        if (!upgraded) {
            if (mob instanceof EnderMan) {
                upgrade(mob, 60, 6, 10);
            } else if (mob instanceof Shulker) {
                upgrade(mob, 45, -1, -1);
            } else if (mob instanceof Endermite) {
                upgrade(mob, 16, -1, 4);
            }
        }
    }

    private static void upgrade(Mob mob, double health, double armor, double damage) {
        CompoundTag data = mob.getPersistentData();
        data.putDouble(OLD_HEALTH, mob.getAttribute(Attributes.MAX_HEALTH).getBaseValue());
        remember(mob, Attributes.ARMOR, OLD_ARMOR);
        remember(mob, Attributes.ATTACK_DAMAGE, OLD_DAMAGE);
        data.putBoolean(UPGRADED, true);
        double ratio = mob.getHealth() / mob.getMaxHealth();
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        mob.setHealth((float) (health * ratio));
        if (armor >= 0) {
            set(mob, Attributes.ARMOR, armor);
        }
        if (damage >= 0) {
            set(mob, Attributes.ATTACK_DAMAGE, damage);
        }
    }

    private static void restore(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        double ratio = mob.getHealth() / mob.getMaxHealth();
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(data.getDouble(OLD_HEALTH));
        if (data.contains(OLD_ARMOR)) {
            set(mob, Attributes.ARMOR, data.getDouble(OLD_ARMOR));
        }
        if (data.contains(OLD_DAMAGE)) {
            set(mob, Attributes.ATTACK_DAMAGE, data.getDouble(OLD_DAMAGE));
        }
        mob.setHealth((float) (ratio * mob.getMaxHealth()));
        data.remove(UPGRADED);
    }

    private static void remember(Mob mob, Attribute attribute, String key) {
        if (mob.getAttribute(attribute) != null) {
            mob.getPersistentData().putDouble(key, mob.getAttribute(attribute).getBaseValue());
        }
    }

    private static void set(Mob mob, Attribute attribute, double value) {
        if (mob.getAttribute(attribute) != null) {
            mob.getAttribute(attribute).setBaseValue(value);
        }
    }

    // ------------------------------------------------------------------ comportamiento (cada 10 ticks)

    @SubscribeEvent
    public static void enemyTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel server) || NetherMobEvents.clock(mob) % 10 != 0
                || !inEnd(server) && !com.bryanamaya.tecnocinetico.entity.MobVariants.is(mob, com.bryanamaya.tecnocinetico.entity.MobVariants.END)) {
            return;
        }
        if (mob.getTarget() instanceof Player old && (old.isCreative() || old.isSpectator())) {
            mob.setTarget(null);
        }
        if (mob instanceof EnderMan enderman) {
            Player player = nearestVisibleSurvivor(server, mob);
            if (player != null && mob.getTarget() == null) {
                enderman.setTarget(player);                                   // se enfada aunque no lo mires
            }
        }
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        int cycle = NetherMobEvents.clock(mob) % CYCLE;
        if (mob instanceof EnderMan enderman) {
            phaseStrike(server, enderman, target, cycle);
        } else if (mob instanceof Shulker shulker && mob.hasLineOfSight(target)) {
            volley(server, shulker, target, cycle);
        }
    }

    @Nullable
    private static Player nearestVisibleSurvivor(ServerLevel server, Mob mob) {
        return server.players().stream()
                .filter(p -> !p.isCreative() && !p.isSpectator() && p.isAlive() && mob.distanceToSqr(p) < SIGHT * SIGHT
                        && !Tier2ArmorItem.hasFullSet(p, Tier2ArmorItem.Kind.NULLITE) && mob.hasLineOfSight(p))
                .min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
    }

    /** Glitches de aviso alrededor de un mob (los ven todos los jugadores cercanos). */
    public static void glitch(ServerLevel server, LivingEntity e, int count) {
        server.sendParticles(ModParticles.DATA_GLITCH.get(), e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), count,
                e.getBbWidth() * 0.6, e.getBbHeight() * 0.4, e.getBbWidth() * 0.6, 0.02);
    }

    /** Salto de desfase: aviso de glitches y, 10 ticks después, aparece a la espalda del objetivo y golpea. */
    private static void phaseStrike(ServerLevel server, EnderMan enderman, LivingEntity target, int cycle) {
        if (enderman.distanceToSqr(target) < 16) {
            return;                                                           // cerca: pelea normal
        }
        if (cycle == WARN) {
            glitch(server, enderman, 24);
            server.playSound(null, enderman.getX(), enderman.getY(), enderman.getZ(), SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 0.8F, 1.4F);
        } else if (cycle == STRIKE) {
            Vec3 back = Vec3.directionFromRotation(0, target.getYRot()).scale(-1.6);
            glitch(server, enderman, 16);
            if (enderman.randomTeleport(target.getX() + back.x, target.getY(), target.getZ() + back.z, true)) {
                enderman.lookAt(target, 360, 360);
                glitch(server, enderman, 16);
                enderman.doHurtTarget(target);
            }
        }
    }

    /** Ráfaga del shulker: aviso de glitches y tres balas a la vez. */
    private static void volley(ServerLevel server, Shulker shulker, LivingEntity target, int cycle) {
        if (cycle == WARN) {
            glitch(server, shulker, 18);
            server.playSound(null, shulker.getX(), shulker.getY(), shulker.getZ(), SoundEvents.SHULKER_AMBIENT, SoundSource.HOSTILE, 1.0F, 0.6F);
        } else if (cycle == STRIKE) {
            Direction.Axis[] axes = {Direction.Axis.X, Direction.Axis.Y, Direction.Axis.Z};
            for (int i = 0; i < VOLLEY; i++) {
                server.addFreshEntity(new ShulkerBullet(server, shulker, target, axes[i]));
            }
            server.playSound(null, shulker.getX(), shulker.getY(), shulker.getZ(), SoundEvents.SHULKER_SHOOT, SoundSource.HOSTILE, 2.0F, 0.8F);
        }
    }
}
