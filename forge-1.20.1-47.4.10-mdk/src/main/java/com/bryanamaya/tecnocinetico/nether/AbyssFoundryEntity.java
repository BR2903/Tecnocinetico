package com.bryanamaya.tecnocinetico.nether;

import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Fundidor del Abismo: primer jefe del Nether. Horno-máquina de ~4,5 bloques con un martillo (brazo derecho) y un
 * crisol de escoria (brazo izquierdo). Se despierta con la consola de su fundición (guardián "abyss_foundry"). Lo
 * común de los jefes (consola, multijugador, reinicio, barra, escoria y charcos) está en {@link NetherBossEntity}.
 * Cada ataque avisa antes (pose + partículas):
 * <ul>
 *   <li><b>Martillazo</b> (cerca): levanta el martillo (brazo derecho) y lo estrella delante y a su derecha.</li>
 *   <li><b>Fractura de lava</b> (media distancia): grieta recta hacia el punto fijado que suelta fuego.</li>
 *   <li><b>Escoria fundida</b>: marca 3 (5 enfadado) puntos alrededor del objetivo y lanza desde el crisol bolas de
 *       escoria que vuelan en arco y dejan charcos ardientes unos segundos.</li>
 *   <li><b>Sobrecalentamiento</b> (cada 4.º ataque): onda de calor muy fuerte (32 de daño, la armadura lo reduce;
 *       +45 de calor del Nether) y luego queda <b>expuesto</b> {@link #EXPOSED_TICKS} ticks recibiendo ×1,6.</li>
 * </ul>
 * Por debajo de la mitad de vida se enfada: avisos y pausas más cortos. Suelta un Acoplador del Nether por jugador
 * que luchó (máx. 4).
 */
public class AbyssFoundryEntity extends NetherBossEntity {

    public static final int HAMMER = 1, FRACTURE = 2, SLAG = 3, OVERHEAT = 4;
    public static final float BASE_HEALTH = 1000.0F;   // la vida máxima de Minecraft es 1024
    public static final int EXPOSED_TICKS = 80;
    public static final float EXPOSED_MULTIPLIER = 1.6F;
    /** Desplazamiento del martillazo hacia su derecha (bloques): el martillo está en el brazo derecho. */
    public static final double HAMMER_SIDE = 1.4;
    /** Daño del sobrecalentamiento (golpe normal: la armadura lo reduce). Sin armadura buena, mata. */
    public static final float OVERHEAT_DAMAGE = 32.0F;

    private int cooldown = 40, sequence, exposedLeft;
    private Vec3 lock = Vec3.ZERO;
    private final List<Vec3> targets = new ArrayList<>();

    public AbyssFoundryEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED);
        xpReward = 650;
        setMaxUpStep(1.5F);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, BASE_HEALTH)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0D)
                .add(Attributes.ATTACK_DAMAGE, 26.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 24.0F));
    }

    @Override
    protected float exposedMultiplier() {
        return EXPOSED_MULTIPLIER;
    }

    @Override
    protected String defeatMessage() {
        return "message.tecnocinetico.foundry_defeated";
    }

    // ------------------------------------------------------------------ combate

    @Override
    protected void bossTick(ServerLevel server) {
        if (exposedLeft > 0) {                                                       // abierto: no ataca ni anda
            getNavigation().stop();
            if (--exposedLeft == 0) {
                setExposed(false);
            } else if (exposedLeft % 4 == 0) {
                server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 3.0, getZ(), 3, 0.6, 0.4, 0.6, 0.02);
            }
            return;
        }
        LivingEntity t = pickTarget();
        if (t == null) {
            setAttack(0, 0);
            return;
        }
        getLookControl().setLookAt(t, 20.0F, 20.0F);
        if (attack() != 0) {
            tickWindup(server);
            return;
        }
        double dist = distanceTo(t);
        if (dist > 4.0) {
            getNavigation().moveTo(t, 1.0D);
        } else {
            getNavigation().stop();
        }
        if (--cooldown > 0) {
            return;
        }
        // elegir ataque
        sequence++;
        int attack = sequence % 4 == 0 ? OVERHEAT : dist < 6.0 ? HAMMER : dist < 18.0 && random.nextBoolean() ? FRACTURE : SLAG;
        int windup = switch (attack) {
            case OVERHEAT -> 40;
            case SLAG -> 26;
            default -> 30;
        };
        if (enraged()) {
            windup = Math.round(windup * 0.7F);
        }
        setAttack(attack, windup);
        getNavigation().stop();
        if (attack == HAMMER) {                                                      // el martillo va en la mano derecha:
            Vec3 dir = flat(t.position().subtract(position()));                      // cae delante y a su derecha
            Vec3 right = new Vec3(-dir.z, 0, dir.x);
            lock = position().add(dir.scale(Math.min(dist, 3.5))).add(right.scale(HAMMER_SIDE));
        } else {
            lock = t.position();
        }
        targets.clear();
        if (attack == SLAG) {
            int n = enraged() ? 5 : 3;
            targets.add(t.position());
            for (int i = 1; i < n; i++) {
                double a = random.nextDouble() * Math.PI * 2, r = 2.5 + random.nextDouble() * 3.0;
                targets.add(t.position().add(Math.cos(a) * r, 0, Math.sin(a) * r));
            }
        }
        server.playSound(null, blockPosition(), attack == OVERHEAT ? SoundEvents.FURNACE_FIRE_CRACKLE : SoundEvents.ANVIL_PLACE,
                SoundSource.HOSTILE, 3.0F, attack == OVERHEAT ? 0.5F : 0.6F);
    }

    /** Durante el aviso: partículas que marcan dónde va a pegar; al terminar, el golpe. */
    private void tickWindup(ServerLevel server) {
        int left = windupLeft() - 1;
        setWindupLeft(left);
        getNavigation().stop();
        if (left % 3 == 0) {
            switch (attack()) {
                case HAMMER -> ring(server, lock, 3.5, ParticleTypes.FLAME);
                case FRACTURE -> line(server);
                case SLAG -> targets.forEach(p -> ring(server, p, 2.0, ParticleTypes.LAVA));
                case OVERHEAT -> {
                    ring(server, position(), 8.0, ParticleTypes.FLAME);
                    server.sendParticles(ParticleTypes.LAVA, getX(), getY() + 3.0, getZ(), 4, 1.0, 1.0, 1.0, 0.1);
                }
                default -> {}
            }
        }
        if (left > 0) {
            return;
        }
        switch (attack()) {
            case HAMMER -> hammer(server);
            case FRACTURE -> fracture(server);
            case SLAG -> slag(server);
            case OVERHEAT -> overheat(server);
            default -> {}
        }
        setAttack(0, 0);
        cooldown = enraged() ? 14 : 24;
    }

    private void hammer(ServerLevel server) {
        server.playSound(null, BlockPos.containing(lock), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.5F, 0.7F);
        server.sendParticles(ParticleTypes.EXPLOSION, lock.x, lock.y + 0.5, lock.z, 3, 1.0, 0.2, 1.0, 0.0);
        hurtArea(server, lock, 3.5, 36.0F, 1.8, 0);
    }

    private void fracture(ServerLevel server) {
        server.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 3.0F, 0.5F);
        Vec3 dir = flat(lock.subtract(position()));
        Vec3 a = position().add(dir.scale(2)), b = position().add(dir.scale(18));
        com.bryanamaya.tecnocinetico.util.ParticleShapes.line(server, ParticleTypes.LAVA, a.add(0, 0.3, 0), b.add(0, 0.3, 0), 16, 2, 0.3, 0.1, 0.3, 0.0);
        com.bryanamaya.tecnocinetico.util.ParticleShapes.line(server, ParticleTypes.FLAME, a.add(0, 0.6, 0), b.add(0, 0.6, 0), 16, 4, 0.3, 0.4, 0.3, 0.02);
        for (int i = 2; i <= 18; i++) {
            hurtArea(server, position().add(dir.scale(i)), 1.5, 24.0F, 0.5, 8);
        }
    }

    /** Lanza las bolas desde el crisol (mano izquierda, por encima del hombro); vuelan en arco y caen en los puntos marcados. */
    private void slag(ServerLevel server) {
        server.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 3.0F, 0.5F);
        Vec3 dir = targets.isEmpty() ? flat(getLookAngle()) : flat(targets.get(0).subtract(position()));
        Vec3 left = new Vec3(dir.z, 0, -dir.x);
        Vec3 hand = position().add(0, 5.5, 0).add(left.scale(1.6)).add(dir.scale(1.0));
        int i = 0;
        for (Vec3 p : targets) {
            launchSlag(hand, p, 2 * i++, 22.0F, 2.5);
        }
    }

    private void overheat(ServerLevel server) {
        server.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 4.0F, 0.5F);
        server.sendParticles(ParticleTypes.FLAME, getX(), getY() + 2.0, getZ(), 120, 4.0, 1.5, 4.0, 0.15);
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(8.0))) {
            if (p.isSpectator() || p.isCreative()) {
                continue;
            }
            p.hurt(damageSources().mobAttack(this), OVERHEAT_DAMAGE);              // la armadura cuenta (progresión)
            float factor = NetherHazards.heatFactor(p);                              // la piroxita también protege aquí
            p.getPersistentData().putFloat(NetherHazards.HEAT, Math.min(NetherHazards.MAX, NetherHazards.heat(p) + 45.0F * factor));
            Vec3 push = flat(p.position().subtract(position())).scale(1.4);
            p.push(push.x, 0.5, push.z);
            p.hurtMarked = true;
        }
        exposedLeft = EXPOSED_TICKS;
        setExposed(true);
    }

    private void line(ServerLevel server) {
        Vec3 dir = flat(lock.subtract(position()));
        com.bryanamaya.tecnocinetico.util.ParticleShapes.line(server, ParticleTypes.FLAME, position().add(dir.scale(2)).add(0, 0.1, 0), position().add(dir.scale(18)).add(0, 0.1, 0),
                8, 1, 0.2, 0, 0.2, 0);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return attack() == 0 && !exposed() && super.doHurtTarget(target);
    }

    /** Botín: un Acoplador del Nether por jugador que luchó (1-4), componentes, aleación, piroxita y netherita. */
    @Override
    protected void loot(int players) {
        drop(new ItemStack(ModItems.NETHER_COUPLER.get(), players));
        drop(new ItemStack(ModItems.ABYSSAL_COMPONENT.get(), 3 + 2 * players));
        drop(new ItemStack(ModItems.SLAG_ALLOY.get(), 16 + random.nextInt(9)));
        drop(new ItemStack(ModItems.PIROXITE_INGOT.get(), 8 + random.nextInt(5)));
        drop(new ItemStack(Items.NETHERITE_INGOT, 1 + random.nextInt(2)));
    }

    // pruebas
    public void testSetExposed(int ticks) {
        exposedLeft = ticks;
        setExposed(ticks > 0);
    }

    public int testCooldown() {
        return cooldown;
    }
}
