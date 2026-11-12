package com.bryanamaya.tecnocinetico.nether;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.custom.CoreShots;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;

/**
 * Nether hostil (por etapas, ver docs/PLAN_NETHER.md). Todo ocurre en el servidor y solo dentro del Nether.
 * <ul>
 *   <li><b>Parte 1 — enemigos vanilla mejorados</b>: piglins, brutos y zombificados (también los bebés) más duros,
 *       TODOS armados (hacha de netherita o Fusil de Asalto, que NUNCA sueltan; si pierden el arma se rearman) y
 *       hostiles aunque lleves oro; endermen hostiles; ghasts con
 *       bolas de potencia 3; blazes con un abanico extra de tres bolas; esqueletos wither que embisten. Al salir del
 *       Nether recuperan sus atributos y su arma.</li>
 *   <li><b>Parte 2 — enemigos nuevos</b>: al excavar roca o minerales del Nether por debajo de Y 80, a veces salen
 *       Ácaros de Escoria ({@link InfernalEntity}).</li>
 * </ul>
 * Rendimiento: el Nether puede tener cientos de piglins zombificados. Cada mob trabaja solo 1 de cada 10 ticks y con
 * un desfase según su id ({@link #clock}), así que la carga se reparte entre ticks; la línea de visión (lo caro) se
 * comprueba solo con jugadores ya dentro del alcance; las trazadoras de los disparos son un paquete por disparo.
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID)
public final class NetherMobEvents {

    // datos persistentes de un mob mejorado (para no mejorar dos veces y poder restaurarlo)
    private static final String UPGRADED = "InfernalUpgrade", OLD_HEALTH = "InfernalOriginalHealth", OLD_ARMOR = "InfernalOriginalArmor",
            OLD_DAMAGE = "InfernalOriginalDamage", OLD_WEAPON = "InfernalOriginalWeapon";
    /** Último tick en que este jugador despertó ácaros al excavar. */
    private static final String MITE_DISTURBANCE = "MiteDisturbance";

    private static final int GHAST_FIREBALL_POWER = 3;
    /** Distancia (al cuadrado) a la que piglins, zombificados y endermen detectan a un jugador. */
    private static final double SIGHT_SQR = 32.0D * 32.0D;
    private static final int MITE_COOLDOWN = 240, MITE_CHANCE = 18, MITE_LIMIT = 6;

    private NetherMobEvents() {}

    // ------------------------------------------------------------------ mejora al entrar al mundo

    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        if (event.getLevel().dimension() != Level.NETHER) {
            if (event.getEntity() instanceof Mob mob && mob.getPersistentData().getBoolean(UPGRADED)) {
                restore(mob);                                                 // salió del Nether: vuelve a ser vanilla
            }
            return;
        }
        if (event.getEntity() instanceof LargeFireball fireball) {            // bolas de ghast más potentes
            CompoundTag tag = fireball.saveWithoutId(new CompoundTag());
            tag.putByte("ExplosionPower", (byte) GHAST_FIREBALL_POWER);
            fireball.load(tag);
        }
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (mob.getPersistentData().getBoolean(UPGRADED)) {
            disarmDrop(mob);                                                  // piglins de antes de este cambio
            return;
        }
        double health = upgradedHealth(mob);
        if (health > 0) {
            upgrade(mob, health);
        }
    }

    /** Vida mejorada de cada mob vanilla (0 = no se mejora). */
    private static double upgradedHealth(Mob mob) {
        if (mob instanceof PiglinBrute) {
            return 72;
        }
        if (mob instanceof AbstractPiglin || mob instanceof ZombifiedPiglin) {
            return 36;
        }
        if (mob instanceof Ghast || mob instanceof Blaze) {
            return 44;
        }
        return mob instanceof WitherSkeleton ? 48 : 0;
    }

    private static void upgrade(Mob mob, double health) {
        CompoundTag data = mob.getPersistentData();
        data.putDouble(OLD_HEALTH, mob.getAttribute(Attributes.MAX_HEALTH).getBaseValue());
        remember(mob, Attributes.ARMOR, OLD_ARMOR);
        remember(mob, Attributes.ATTACK_DAMAGE, OLD_DAMAGE);
        data.put(OLD_WEAPON, mob.getMainHandItem().save(new CompoundTag()));
        data.putBoolean(UPGRADED, true);

        double ratio = mob.getHealth() / mob.getMaxHealth();
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        mob.setHealth((float) (health * ratio));
        set(mob, Attributes.ARMOR, mob instanceof PiglinBrute ? 12 : mob instanceof Ghast ? 4 : 8);
        set(mob, Attributes.ATTACK_DAMAGE, mob instanceof PiglinBrute ? 15 : mob instanceof WitherSkeleton ? 12 : 9);
        if (armedPiglin(mob)) {
            arm(mob);
        }
    }

    /** Piglins, brutos y zombificados, de cualquier edad: todos llevan arma en el Nether. */
    public static boolean armedPiglin(Mob mob) {
        return mob instanceof AbstractPiglin || mob instanceof ZombifiedPiglin;
    }

    /** ¿Lleva ya una de las armas del Nether? */
    public static boolean holdsNetherWeapon(Mob mob) {
        return mob.getMainHandItem().is(Items.NETHERITE_AXE) || mob.getMainHandItem().is(ModItems.ASSAULT_RIFLE.get());
    }

    /** Hacha de netherita o Fusil de Asalto, mitad y mitad. */
    private static void arm(Mob mob) {
        mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(mob.getRandom().nextBoolean() ? Items.NETHERITE_AXE : ModItems.ASSAULT_RIFLE.get()));
        disarmDrop(mob);
    }

    /**
     * El Fusil de Asalto es un arma de tier 3 (pide un Procesador del Núcleo): los piglins no lo sueltan nunca.
     * El hacha de netherita conserva una probabilidad pequeña.
     */
    private static void disarmDrop(Mob mob) {
        if (armedPiglin(mob)) {
            mob.setDropChance(EquipmentSlot.MAINHAND, mob.getMainHandItem().is(ModItems.ASSAULT_RIFLE.get()) ? 0.0F : 0.025F);
        }
    }

    private static void restore(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        double ratio = mob.getHealth() / mob.getMaxHealth();
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(data.getDouble(OLD_HEALTH));
        set(mob, Attributes.ARMOR, data.getDouble(OLD_ARMOR));
        set(mob, Attributes.ATTACK_DAMAGE, data.getDouble(OLD_DAMAGE));
        if (data.contains(OLD_WEAPON)) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.of(data.getCompound(OLD_WEAPON)));
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

    /**
     * Reloj de cada mob: su edad desfasada por su id. Así no trabajan todos en el mismo tick (los que aparecen juntos
     * tendrían la misma edad). Los ciclos de ataque (ráfaga, abanico, embestida) usan este reloj.
     */
    public static int clock(Mob mob) {
        return mob.tickCount + Math.floorMod(mob.getId(), 10);
    }

    /** Para las pruebas: deja el reloj del mob en {@code value}. */
    public static void setClock(Mob mob, int value) {
        mob.tickCount = value - Math.floorMod(mob.getId(), 10);
    }

    @SubscribeEvent
    public static void enemyTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel server) || server.dimension() != Level.NETHER
                || clock(mob) % 10 != 0) {
            return;
        }
        if (armedPiglin(mob) && mob.getPersistentData().getBoolean(UPGRADED) && !holdsNetherWeapon(mob)) {
            arm(mob);                                                         // perdió o cambió el arma
        }
        if (mob.getTarget() instanceof Player old && (old.isCreative() || old.isSpectator())) {
            forget(mob);                                                      // pasó a creativo/espectador
        }
        if (mob instanceof AbstractPiglin || mob instanceof ZombifiedPiglin || mob instanceof EnderMan) {
            Player player = nearestVisibleSurvivor(server, mob);
            if (player != null) {
                provoke(mob, player);
                if (mob.getMainHandItem().is(ModItems.ASSAULT_RIFLE.get())) {
                    rifleBurst(server, mob, player);
                }
            }
        }
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive() || !mob.hasLineOfSight(target)) {
            return;
        }
        int cycle = clock(mob) % 100;                                         // aviso a los 60, ataque a los 80
        if (mob instanceof Blaze) {
            blazeFan(server, mob, target, cycle);
        } else if (mob instanceof WitherSkeleton) {
            witherCharge(server, mob, target, cycle);
        }
    }

    private static Player nearestVisibleSurvivor(ServerLevel server, Mob mob) {
        return server.players().stream()
                .filter(p -> !p.isCreative() && !p.isSpectator() && p.isAlive() && mob.distanceToSqr(p) < SIGHT_SQR && mob.hasLineOfSight(p))
                .min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
    }

    /** Hostilidad aunque lleve oro: objetivo y memoria de ira (se renuevan mientras lo vea). */
    private static void provoke(Mob mob, Player player) {
        mob.setTarget(player);
        if (mob instanceof AbstractPiglin piglin) {
            piglin.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, player.getUUID(), 400);
            piglin.getBrain().setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, player, 40);
        }
        if (mob instanceof ZombifiedPiglin zombified) {
            zombified.setPersistentAngerTarget(player.getUUID());
            zombified.setRemainingPersistentAngerTime(400);
        }
    }

    private static void forget(Mob mob) {
        mob.setTarget(null);
        if (mob instanceof AbstractPiglin piglin) {
            piglin.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            piglin.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
        }
        if (mob instanceof ZombifiedPiglin zombified) {
            zombified.stopBeingAngry();
        }
    }

    /** Piglin con fusil: tres disparos (uno cada 10 ticks), pausa de 1,5 s con chispas de aviso antes de la siguiente ráfaga. */
    private static void rifleBurst(ServerLevel server, Mob mob, Player player) {
        if (mob.distanceToSqr(player) > 36) {
            mob.getNavigation().stop();                                       // dispara desde lejos, no carga
        }
        int phase = clock(mob) % 60;
        if (phase == 40) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, mob.getX(), mob.getEyeY(), mob.getZ(), 8, 0.1, 0.1, 0.1, 0.01);
        }
        if (phase < 30) {
            CoreShots.fire(mob, mob.getEyePosition(), player.getEyePosition().subtract(mob.getEyePosition()).normalize(), 32, 10, 3,
                    Monster.class, false);
            server.playSound(null, mob.blockPosition(), ModSounds.AUTO_FIRE.get(), SoundSource.HOSTILE, 1.0F, 0.8F);
        }
    }

    private static void blazeFan(ServerLevel server, Mob blaze, LivingEntity target, int cycle) {
        if (cycle == 60) {
            server.sendParticles(ParticleTypes.FLAME, blaze.getX(), blaze.getEyeY(), blaze.getZ(), 25, 0.8, 0.8, 0.8, 0.03);
        }
        if (cycle == 80) {
            for (int i = -1; i <= 1; i++) {
                Vec3 dir = target.getEyePosition().subtract(blaze.getEyePosition()).normalize().yRot(i * 0.12F);
                SmallFireball ball = new SmallFireball(server, blaze, dir.x, dir.y, dir.z);
                ball.setPos(blaze.getEyePosition());
                server.addFreshEntity(ball);
            }
        }
    }

    private static void witherCharge(ServerLevel server, Mob skeleton, LivingEntity target, int cycle) {
        if (cycle == 60) {
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, skeleton.getX(), skeleton.getY() + 1, skeleton.getZ(), 18, 0.4, 0.5, 0.4, 0.01);
        }
        if (cycle == 80 && skeleton.distanceToSqr(target) < 100) {
            Vec3 dash = target.position().subtract(skeleton.position()).normalize().scale(0.8);
            skeleton.setDeltaMovement(dash.x, 0.2, dash.z);
            skeleton.hasImpulse = true;
        }
    }

    // ------------------------------------------------------------------ ácaros al excavar

    /** Roca y minerales del Nether en los que anidan los ácaros. */
    public static boolean isMiteNest(BlockState state) {
        return state.is(BlockTags.BASE_STONE_NETHER) || state.is(Blocks.NETHER_QUARTZ_ORE) || state.is(Blocks.NETHER_GOLD_ORE)
                || state.is(Blocks.ANCIENT_DEBRIS);
    }

    @SubscribeEvent
    public static void mining(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (!(event.getLevel() instanceof ServerLevel server) || server.dimension() != Level.NETHER || player.isCreative()
                || event.getPos().getY() > NetherEntities.MITE_MAX_Y || !isMiteNest(event.getState())) {
            return;
        }
        long now = server.getGameTime();
        if (now - player.getPersistentData().getLong(MITE_DISTURBANCE) < MITE_COOLDOWN || server.random.nextInt(MITE_CHANCE) != 0) {
            return;
        }
        if (disturbMites(server, player, event.getPos())) {
            player.getPersistentData().putLong(MITE_DISTURBANCE, now);
        }
    }

    /**
     * Saca un ácaro cerca del jugador si el bloque roto estaba dentro de la roca (túnel, no superficie abierta) y no
     * hay ya demasiados alrededor. Devuelve si apareció.
     */
    public static boolean disturbMites(ServerLevel server, Player player, BlockPos broken) {
        if (server.getEntitiesOfClass(InfernalEntity.class, player.getBoundingBox().inflate(12), InfernalEntity::mite).size() >= MITE_LIMIT) {
            return false;
        }
        int walls = 0;
        for (Direction d : Direction.values()) {
            if (server.getBlockState(broken.relative(d)).isSolid()) {
                walls++;
            }
        }
        if (walls < 3) {
            return false;
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            InfernalEntity mite = NetherEntities.MITE.get().create(server);
            if (mite == null) {
                return false;
            }
            mite.moveTo(player.getX() + server.random.nextInt(7) - 3, player.getY(), player.getZ() + server.random.nextInt(7) - 3, 0, 0);
            if (mite.distanceToSqr(player) < 2 || !server.noCollision(mite) || server.getBlockState(mite.blockPosition().below()).isAir()) {
                continue;
            }
            mite.setTarget(player);
            server.addFreshEntity(mite);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, mite.getX(), mite.getY() + 0.2, mite.getZ(), 6, 0.2, 0.1, 0.2, 0.01);
            return true;
        }
        return false;
    }
}
