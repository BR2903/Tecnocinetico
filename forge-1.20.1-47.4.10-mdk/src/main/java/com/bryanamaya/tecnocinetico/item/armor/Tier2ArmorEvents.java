package com.bryanamaya.tecnocinetico.item.armor;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/** Bonos de set de las armaduras tier 2 (ver {@link Tier2ArmorItem}). */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID)
public final class Tier2ArmorEvents {

    public static final float INERCITA_BONUS = 0.65F;
    public static final float EXO_MAX_CHARGE = 20.0F, EXO_LEAP_COST = 6.0F;
    private static final String TAG_CHARGE = "TecnoExoCharge", TAG_LEAP = "TecnoExoLeap";
    private static final UUID TACTICAL_SPEED = UUID.fromString("0c1d7e52-8f3a-4b2e-b6a1-2d9e4f7a5c10");

    private Tier2ArmorEvents() {}

    public static float exoCharge(Player p) {
        return p.getPersistentData().getFloat(TAG_CHARGE);
    }

    private static void setCharge(Player p, float v) {
        p.getPersistentData().putFloat(TAG_CHARGE, Math.max(0.0F, Math.min(EXO_MAX_CHARGE, v)));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity e = event.getEntity();
        var src = event.getSource();
        // Inercita: blindaje contra balas y explosiones
        if ((src.is(DamageTypeTags.IS_PROJECTILE) || src.is(DamageTypeTags.IS_EXPLOSION)) && Tier2ArmorItem.hasFullSet(e, Kind.INERCITA)) {
            event.setAmount(event.getAmount() * INERCITA_BONUS);
        }
        // Exoesqueleto: acumula la energía de los golpes; con la carga llena, un golpe cuerpo a cuerpo suelta la onda
        if (e instanceof ServerPlayer p && Tier2ArmorItem.hasFullSet(p, Kind.EXO) && !src.is(DamageTypeTags.IS_FALL)) {
            float charge = exoCharge(p);
            if (charge >= EXO_MAX_CHARGE && src.getEntity() instanceof LivingEntity attacker && attacker.distanceTo(p) < 5.0F) {
                shockwave(p);
                setCharge(p, 0.0F);
            } else {
                setCharge(p, charge + event.getAmount() * 1.5F);
            }
            showCharge(p);
        }
    }

    private static void shockwave(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(4.5D),
                t -> t != p && t.isAlive() && (t instanceof Enemy || t == p.getLastHurtByMob()))) {
            Vec3 away = t.position().subtract(p.position()).multiply(1, 0, 1).normalize();
            t.hurt(p.damageSources().playerAttack(p), 4.0F);
            t.knockback(1.6D, -away.x, -away.z);
            t.push(0, 0.35D, 0);
        }
        for (int i = 0; i < 24; i++) {
            double a = i / 24.0 * Math.PI * 2;
            level.sendParticles(ModParticles.KINETIC_SPARK.get(), p.getX() + Math.cos(a) * 1.5, p.getY() + 0.3, p.getZ() + Math.sin(a) * 1.5,
                    2, 0.1, 0.05, 0.1, 0.25);
        }
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.6F, 1.6F);
    }

    private static void showCharge(ServerPlayer p) {
        int bars = Math.round(exoCharge(p) / EXO_MAX_CHARGE * 10);
        p.displayClientMessage(Component.translatable("message.tecnocinetico.exo_charge")
                .append(Component.literal(" " + "■".repeat(bars)).withStyle(ChatFormatting.AQUA))
                .append(Component.literal("■".repeat(10 - bars)).withStyle(ChatFormatting.DARK_GRAY)), true);
    }

    /** Salto cinético (el cliente lo pide al saltar agachado con el exoesqueleto completo). */
    public static void leap(ServerPlayer p) {
        if (!Tier2ArmorItem.hasFullSet(p, Kind.EXO) || exoCharge(p) < EXO_LEAP_COST) {
            return;
        }
        setCharge(p, exoCharge(p) - EXO_LEAP_COST);
        Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
        p.setDeltaMovement(look.x * 1.1D, 1.05D, look.z * 1.1D);
        p.hurtMarked = true;                                                    // manda la velocidad al cliente
        p.getPersistentData().putLong(TAG_LEAP, p.level().getGameTime());
        p.serverLevel().sendParticles(ModParticles.KINETIC_SPARK.get(), p.getX(), p.getY() + 0.1, p.getZ(), 16, 0.3, 0.05, 0.3, 0.2);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 0.8F, 1.4F);
        showCharge(p);
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player p) || !Tier2ArmorItem.hasFullSet(p, Kind.EXO)) {
            return;
        }
        if (p.level().getGameTime() - p.getPersistentData().getLong(TAG_LEAP) < 120) {   // aterrizaje del salto: sin daño
            p.getPersistentData().remove(TAG_LEAP);
            event.setCanceled(true);
            return;
        }
        event.setDamageMultiplier(event.getDamageMultiplier() * 0.5F);
    }

    // ------------------------------------------------------------------ tier 3

    private static final String TAG_CORE_HURT = "TecnoCoreHurt";
    public static final float CORE_SHIELD = 8.0F;

    /** Piroxita: inmune al fuego. Nulita: esquiva por desfase y perlas sin daño. (Antes de que el golpe cuente.) */
    @SubscribeEvent
    public static void onAttack(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        LivingEntity e = event.getEntity();
        var src = event.getSource();
        if (src.is(DamageTypeTags.IS_FIRE) && Tier2ArmorItem.hasPiroxiteSet(e)) {
            event.setCanceled(true);
            e.clearFire();
            return;
        }
        if (!Tier2ArmorItem.hasFullSet(e, Kind.NULLITE) || e.level().isClientSide) {
            return;
        }
        if (src.getDirectEntity() instanceof net.minecraft.world.entity.projectile.ThrownEnderpearl) {
            event.setCanceled(true);
            return;
        }
        if (src.getEntity() != null && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && e.getRandom().nextFloat() < 0.15F) {
            for (int i = 0; i < 12; i++) {                                     // desfase: aparece unos bloques más allá
                double x = e.getX() + (e.getRandom().nextDouble() - 0.5) * 8.0;
                double y = e.getY() + e.getRandom().nextInt(5) - 2;
                double z = e.getZ() + (e.getRandom().nextDouble() - 0.5) * 8.0;
                Vec3 from = e.position();
                if (e.randomTeleport(x, y, z, true)) {
                    if (e.level() instanceof ServerLevel server) {
                        server.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, from.x, from.y + 1, from.z,
                                30, 0.3, 0.6, 0.3, 0.05);
                    }
                    e.level().playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.4F);
                    event.setCanceled(true);
                    return;
                }
            }
        }
    }

    /** Nulita: los enderman no se enfadan al mirarlos. */
    @SubscribeEvent
    public static void onEndermanAnger(net.minecraftforge.event.entity.living.EnderManAngerEvent event) {
        if (Tier2ArmorItem.hasFullSet(event.getPlayer(), Kind.NULLITE)) {
            event.setCanceled(true);
        }
    }

    /** Piroxita: quien golpea cuerpo a cuerpo arde. Núcleo: apunta cuándo se recibió daño (para el escudo). */
    @SubscribeEvent
    public static void onHurtT3(LivingHurtEvent event) {
        LivingEntity e = event.getEntity();
        var src = event.getSource();
        if (src.getEntity() instanceof LivingEntity attacker && src.getDirectEntity() == attacker
                && Tier2ArmorItem.hasPiroxiteSet(e)) {
            attacker.setSecondsOnFire(4);
        }
        if (e instanceof Player p) {
            p.getPersistentData().putLong(TAG_CORE_HURT, p.level().getGameTime());
        }
    }

    /** Núcleo: el escudo (absorción) se recarga 1 corazón por segundo tras 8 s sin recibir daño. */
    private static void coreShield(Player p) {
        if (!Tier2ArmorItem.hasFullSet(p, Kind.CORE)) {
            return;
        }
        long since = p.level().getGameTime() - p.getPersistentData().getLong(TAG_CORE_HURT);
        if (since >= 160 && p.getAbsorptionAmount() < CORE_SHIELD) {
            boolean empty = p.getAbsorptionAmount() <= 0;
            p.setAbsorptionAmount(Math.min(CORE_SHIELD, p.getAbsorptionAmount() + 2.0F));
            if (empty) {
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.4F, 1.8F);
            }
        }
    }

    /** Táctica: +15 % de velocidad con el set completo (modificador temporal). */
    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || event.player.tickCount % 10 != 0) {
            return;
        }
        Player p = event.player;
        if (p.tickCount % 20 == 0) {
            coreShield(p);
        }
        if (Tier2ArmorItem.hasPiroxiteSet(p) && p.isOnFire()) {
            p.clearFire();
        }
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean want = Tier2ArmorItem.hasFullSet(p, Kind.TACTICAL);
        boolean has = speed.getModifier(TACTICAL_SPEED) != null;
        if (want && !has) {
            speed.addTransientModifier(new AttributeModifier(TACTICAL_SPEED, "Armadura táctica", 0.15D,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        } else if (!want && has) {
            speed.removeModifier(TACTICAL_SPEED);
        }
    }
}
