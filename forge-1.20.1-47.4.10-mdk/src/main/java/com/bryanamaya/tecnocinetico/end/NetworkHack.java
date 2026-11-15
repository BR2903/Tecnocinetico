package com.bryanamaya.tecnocinetico.end;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.gear.TacticalGear;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
import com.bryanamaya.tecnocinetico.util.ParticleShapes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Intrusor de Red (gadget de muñeca): con su tecla, el jugador hackea la máquina de la IA a la que mira (hasta
 * {@link #RANGE} bloques; etiqueta {@code tecnocinetico:hackable}, sin jefes). Tras {@link #CHANNEL} ticks de subida
 * (rayo de datos verde, se corta si se aleja o muere) la máquina queda <b>aliada</b> {@link #DURATION} ticks: ataca a
 * los enemigos cercanos (incluidas otras máquinas) y nada de lo que haga puede dañar a un jugador. Después, el gadget
 * se recarga {@link #COOLDOWN} ticks (marca {@code ReadyAt} en el propio gadget).
 * <p>
 * Las máquinas con IA de objetivos (Cuerpo de Enjambre, Centinela de Ceniza, Anclador, Cría) se redirigen solas; las que
 * buscan jugadores a mano (torretas, avispas, Espectro) preguntan a {@link #target(Mob)} si están hackeadas.
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID)
public final class NetworkHack {

    public static final int RANGE = 16, CHANNEL = 20, DURATION = 300, COOLDOWN = 3600, RETARGET = 20;
    public static final double LEASH = 24.0D;
    private static final String UNTIL = "TecnoHackedUntil", BY = "TecnoHackedBy", READY = "ReadyAt";
    public static final TagKey<EntityType<?>> HACKABLE = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "hackable"));
    private static final DustParticleOptions HACK = new DustParticleOptions(new Vector3f(0.49F, 0.85F, 0.38F), 0.8F);

    /** Subidas en curso: jugador -> (id de la máquina, tick de inicio). */
    private record Job(int entity, long start) {}

    private static final Map<UUID, Job> JOBS = new HashMap<>();

    private NetworkHack() {}

    // ------------------------------------------------------------------ estado

    public static boolean hacked(Entity e) {
        return e.getPersistentData().getLong(UNTIL) > e.level().getGameTime();
    }

    /** El gadget que lleva puesto (cualquiera de las dos muñecas, encendido) o vacío. */
    public static ItemStack worn(Player p) {
        return TacticalGear.of(p).map(g -> {
            for (int slot : new int[]{TacticalGear.WRIST, TacticalGear.WRIST_RIGHT}) {
                ItemStack s = g.active(p, slot);
                if (s.is(ModItems.NETWORK_INTRUDER.get())) {
                    return s;
                }
            }
            return ItemStack.EMPTY;
        }).orElse(ItemStack.EMPTY);
    }

    /** Ticks que faltan para poder usarlo (0 = listo). */
    public static long cooldownLeft(ItemStack gadget, long now) {
        return gadget.getTag() == null ? 0 : Math.max(0, gadget.getTag().getLong(READY) - now);
    }

    /**
     * Objetivo de una máquina hackeada: su objetivo actual si sigue siendo un enemigo válido; si no, el enemigo más
     * cercano. Null si no está hackeada o no hay nadie contra quien luchar.
     */
    @Nullable
    public static LivingEntity target(Mob self) {
        if (!hacked(self)) {
            return null;
        }
        LivingEntity cur = self.getTarget();
        if (cur != null && foe(self, cur) && cur.distanceToSqr(self) < LEASH * LEASH) {
            return cur;
        }
        LivingEntity best = null;
        for (LivingEntity e : self.level().getEntitiesOfClass(LivingEntity.class, self.getBoundingBox().inflate(LEASH), e -> foe(self, e))) {
            if (best == null || e.distanceToSqr(self) < best.distanceToSqr(self)) {
                best = e;
            }
        }
        return best;
    }

    /** Enemigo de una máquina hackeada: hostil, vivo, no jugador y no hackeado. */
    private static boolean foe(Mob self, LivingEntity e) {
        return e != self && e.isAlive() && e instanceof Enemy && !(e instanceof Player) && !hacked(e);
    }

    // ------------------------------------------------------------------ uso (tecla)

    /** El jugador pulsa la tecla de hackear (servidor). */
    public static void activate(ServerPlayer p) {
        ItemStack gadget = worn(p);
        long now = p.level().getGameTime();
        if (gadget.isEmpty() || JOBS.containsKey(p.getUUID())) {
            return;
        }
        long left = cooldownLeft(gadget, now);
        if (left > 0) {
            p.displayClientMessage(Component.translatable("message.tecnocinetico.hack.cooldown", (left + 19) / 20).withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        Mob mob = aimed(p);
        if (mob == null) {
            p.displayClientMessage(Component.translatable("message.tecnocinetico.hack.no_target").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        JOBS.put(p.getUUID(), new Job(mob.getId(), now));
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 2.0F);
    }

    /** La máquina hackeable a la que mira (sin jefes), o null. */
    @Nullable
    public static Mob aimed(Player p) {
        Vec3 eye = p.getEyePosition(), end = eye.add(p.getLookAngle().scale(RANGE));
        var block = p.level().clip(new net.minecraft.world.level.ClipContext(eye, end, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, p));
        if (block.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            end = block.getLocation();
        }
        var hit = ProjectileUtil.getEntityHitResult(p.level(), p, eye, end, new AABB(eye, end).inflate(1.5),
                e -> e instanceof Mob m && m.isAlive() && m.getType().is(HACKABLE) && !hacked(m), 0.6F);
        return hit != null && hit.getEntity() instanceof Mob m ? m : null;
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)) {
            return;
        }
        Job job = JOBS.get(p.getUUID());
        if (job == null) {
            return;
        }
        ServerLevel level = p.serverLevel();
        Entity e = level.getEntity(job.entity);
        ItemStack gadget = worn(p);
        long t = level.getGameTime() - job.start;
        if (!(e instanceof Mob mob) || !mob.isAlive() || mob.distanceToSqr(p) > LEASH * LEASH || gadget.isEmpty()) {
            JOBS.remove(p.getUUID());
            p.displayClientMessage(Component.translatable("message.tecnocinetico.hack.lost").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (t % 2 == 0) {                                                          // rayo de datos de la muñeca a la máquina
            Vec3 from = p.position().add(0, p.getBbHeight() * 0.55, 0).add(p.getLookAngle().scale(0.4));
            ParticleShapes.line(level, HACK, from, mob.getBoundingBox().getCenter(), Math.max(4, (int) (from.distanceTo(mob.position()) * 2)));
        }
        int bars = (int) Math.min(10, t * 10 / CHANNEL);
        p.displayClientMessage(Component.translatable("message.tecnocinetico.hack.progress", "▮".repeat(bars) + "▯".repeat(10 - bars))
                .withStyle(ChatFormatting.GREEN), true);
        if (t < CHANNEL) {
            return;
        }
        JOBS.remove(p.getUUID());
        hack(level, mob, p);
        gadget.getOrCreateTag().putLong(READY, level.getGameTime() + COOLDOWN);
        TacticalGear.of(p).ifPresent(TacticalGear::setChanged);
        p.displayClientMessage(Component.translatable("message.tecnocinetico.hack.done", mob.getDisplayName(), DURATION / 20)
                .withStyle(ChatFormatting.GREEN), true);
    }

    /** Hackea la máquina: aliada {@link #DURATION} ticks, contorno visible y objetivo nuevo. */
    public static void hack(ServerLevel level, Mob mob, @Nullable Player by) {
        mob.getPersistentData().putLong(UNTIL, level.getGameTime() + DURATION);
        if (by != null) {
            mob.getPersistentData().putUUID(BY, by.getUUID());
        }
        mob.setTarget(null);
        mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, DURATION, 0, false, false));
        Vec3 c = mob.getBoundingBox().getCenter();
        level.sendParticles(HACK, c.x, c.y, c.z, 30, 0.5, 0.5, 0.5, 0.0);
        level.sendParticles(ModParticles.KINETIC_SPARK.get(), c.x, c.y, c.z, 12, 0.3, 0.3, 0.3, 0.15);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.0F, 1.8F);
        mob.setTarget(target(mob));
    }

    private static void unhack(ServerLevel level, Mob mob) {
        mob.getPersistentData().remove(UNTIL);
        mob.getPersistentData().remove(BY);
        mob.setTarget(null);
        Vec3 c = mob.getBoundingBox().getCenter();
        level.sendParticles(ModParticles.DATA_GLITCH.get(), c.x, c.y, c.z, 20, 0.4, 0.4, 0.4, 0.1);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.0F, 1.6F);
    }

    // ------------------------------------------------------------------ máquinas hackeadas

    @SubscribeEvent
    public static void mobTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        long until = mob.getPersistentData().getLong(UNTIL);
        if (until == 0) {
            return;
        }
        if (until <= level.getGameTime()) {
            unhack(level, mob);
            return;
        }
        if ((mob.tickCount + mob.getId()) % 5 == 0) {
            LivingEntity t = target(mob);
            if (t != mob.getTarget()) {
                mob.setTarget(t);
            }
        }
        if (mob.tickCount % 10 == 0) {
            Vec3 c = mob.getBoundingBox().getCenter();
            level.sendParticles(HACK, c.x, c.y + mob.getBbHeight() * 0.3, c.z, 2, 0.3, 0.3, 0.3, 0.0);
        }
    }

    /** Hackeada no elige jugadores como objetivo (la IA vanilla lo intentaría). */
    @SubscribeEvent
    public static void changeTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof Player && hacked(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** Nada de una máquina hackeada daña a un jugador (ni sus disparos ni áreas); y los jugadores no la dañan por error. */
    @SubscribeEvent
    public static void attack(LivingAttackEvent event) {
        Entity src = event.getSource().getEntity();
        if (event.getEntity() instanceof Player && src != null && hacked(src)) {
            event.setCanceled(true);
        } else if (src instanceof Player && hacked(event.getEntity())) {
            event.setCanceled(true);
        } else if (src != null && hacked(src) && src.level() instanceof ServerLevel level) {
            Player hacker = hacker(level, src);
            if (hacker != null) {
                event.getEntity().setLastHurtByPlayer(hacker);                       // experiencia y botín "matado por jugador"
            }
        }
    }

    /** El jugador que hackeó esa máquina (si sigue conectado). */
    @Nullable
    public static Player hacker(ServerLevel level, Entity machine) {
        var tag = machine.getPersistentData();
        return tag.hasUUID(BY) ? level.getPlayerByUUID(tag.getUUID(BY)) : null;
    }

    /** Para pruebas: estado de subidas. */
    public static boolean channeling(Player p) {
        return JOBS.containsKey(p.getUUID());
    }
}
