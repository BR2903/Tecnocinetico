package com.bryanamaya.tecnocinetico.nether;

import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Devorador de Escoria: segundo jefe del Nether. Gusano biomecánico segmentado, con mandíbulas y placas, que vive en
 * su excavación (guardián "slag_devourer"). Lo común (consola, multijugador, reinicio, barra, escoria) está en
 * {@link NetherBossEntity}. Todos los ataques avisan:
 * <ul>
 *   <li><b>Mordisco</b> (cerca): abre las mandíbulas y muerde delante.</li>
 *   <li><b>Embestida</b>: se encoge apuntando y carga en línea recta arrasando; si choca contra una pared queda
 *       aturdido y <b>expuesto</b>.</li>
 *   <li><b>Escupe escoria</b>: 3 (5 enfadado) bolas de escoria en arco que dejan charcos.</li>
 *   <li><b>Excava y emerge</b> (cada 4.º ataque): se hunde (invulnerable bajo tierra), el suelo tiembla y echa escoria
 *       bajo el objetivo, y sale de golpe ahí lanzando por el aire; después queda expuesto un momento.</li>
 *   <li><b>Invoca ácaros</b> (enfadado, cada 5.º): suelta Ácaros de Escoria de sus segmentos (máx. {@link #MAX_MITES} en la arena).</li>
 * </ul>
 * Suelta un Núcleo de Forja por jugador que luchó (máx. 4) para el arma de tier 3 del Nether.
 * <p>
 * Contrato con el modelo: {@link #attack()} ({@link #BITE}, {@link #CHARGE} aviso, {@link #RUSH} cargando,
 * {@link #SPIT}, {@link #BURROW} hundiéndose, {@link #EMERGE} saliendo, {@link #SUMMON}), {@link #windup()},
 * {@link #burrowed()} (bajo tierra: no se dibuja), {@link #exposed()}, {@link #enraged()}.
 */
public class SlagDevourerEntity extends NetherBossEntity {

    public static final int BITE = 1, CHARGE = 2, RUSH = 3, SPIT = 4, BURROW = 5, EMERGE = 6, SUMMON = 7;
    public static final float BASE_HEALTH = 1000.0F;
    public static final int MAX_MITES = 6, EXPOSED_TICKS = 50, RUSH_TICKS = 16, UNDERGROUND_TICKS = 34;
    public static final float EXPOSED_MULTIPLIER = 1.5F;

    private static final EntityDataAccessor<Boolean> BURROWED = SynchedEntityData.defineId(SlagDevourerEntity.class, EntityDataSerializers.BOOLEAN);

    private int cooldown = 40, sequence, exposedLeft, rushLeft;
    private Vec3 lock = Vec3.ZERO, rushDir = Vec3.ZERO;
    private final Set<UUID> rushHit = new HashSet<>();

    public SlagDevourerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
        xpReward = 900;
        setMaxUpStep(1.5F);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, BASE_HEALTH)
                .add(Attributes.ARMOR, 14.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0D)
                .add(Attributes.ATTACK_DAMAGE, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(BURROWED, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 24.0F));
    }

    public boolean burrowed() {
        return entityData.get(BURROWED);
    }

    private void setBurrowed(boolean on) {
        entityData.set(BURROWED, on);
        noPhysics = on;
        setNoGravity(on);
        setInvisible(on);
    }

    @Override
    protected float exposedMultiplier() {
        return EXPOSED_MULTIPLIER;
    }

    @Override
    protected String defeatMessage() {
        return "message.tecnocinetico.devourer_defeated";
    }

    // ------------------------------------------------------------------ combate

    @Override
    protected void bossTick(ServerLevel server) {
        if (exposedLeft > 0) {                                                       // aturdido: ni ataca ni anda
            getNavigation().stop();
            if (--exposedLeft == 0) {
                setExposed(false);
            } else if (exposedLeft % 5 == 0) {
                server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.5, getZ(), 2, 0.8, 0.3, 0.8, 0.02);
            }
            return;
        }
        LivingEntity t = pickTarget();
        if (rushLeft > 0) {
            tickRush(server);
            return;
        }
        if (t == null) {
            if (burrowed()) {
                emerge(server);                                                      // sin objetivo: sale donde está
            }
            setAttack(0, 0);
            return;
        }
        if (attack() != 0) {
            tickWindup(server, t);
            return;
        }
        getLookControl().setLookAt(t, 30.0F, 30.0F);
        double dist = distanceTo(t);
        if (dist > 3.5) {
            getNavigation().moveTo(t, 1.0D);
        } else {
            getNavigation().stop();
        }
        if (--cooldown > 0) {
            return;
        }
        sequence++;
        long mites = level().getEntitiesOfClass(InfernalEntity.class, getBoundingBox().inflate(ARENA), InfernalEntity::mite).size();
        int attack;
        if (sequence % 4 == 0) {
            attack = BURROW;
        } else if (enraged() && sequence % 5 == 0 && mites < MAX_MITES) {
            attack = SUMMON;
        } else if (dist < 5.0) {
            attack = BITE;
        } else {
            attack = dist < 22.0 && random.nextBoolean() ? CHARGE : SPIT;
        }
        int windup = switch (attack) {
            case BITE -> 16;
            case CHARGE -> 24;
            case BURROW, SUMMON -> 20;
            default -> 22;
        };
        if (enraged()) {
            windup = Math.round(windup * 0.75F);
        }
        setAttack(attack, windup);
        getNavigation().stop();
        Vec3 dir = flat(t.position().subtract(position()));
        lock = attack == BITE ? position().add(dir.scale(Math.min(dist, 3.0))) : t.position();
        rushDir = dir;
        server.playSound(null, blockPosition(), attack == BURROW ? SoundEvents.RAVAGER_ROAR : SoundEvents.RAVAGER_STUNNED,
                SoundSource.HOSTILE, 3.0F, 0.6F);
    }

    private void tickWindup(ServerLevel server, LivingEntity t) {
        int left = windupLeft() - 1;
        setWindupLeft(left);
        getNavigation().stop();
        if (attack() == EMERGE) {                                                    // bajo tierra: sigue al objetivo
            if (left > 10) {
                lock = t.position();
            }
            if (left % 2 == 0) {
                rumble(server, lock);
            }
        } else if (left % 3 == 0) {
            switch (attack()) {
                case BITE -> ring(server, lock, 2.5, ParticleTypes.FLAME);
                case CHARGE -> line(server, rushDir);
                case SPIT -> ring(server, lock, 2.0, ParticleTypes.LAVA);
                case BURROW -> rumble(server, position());
                default -> {}
            }
        }
        if (left > 0) {
            return;
        }
        switch (attack()) {
            case BITE -> {
                server.playSound(null, blockPosition(), SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 2.5F, 0.5F);
                hurtArea(server, lock, 2.5, 28.0F, 1.0, 0);
                finish();
            }
            case CHARGE -> {
                setAttack(RUSH, RUSH_TICKS);
                rushLeft = RUSH_TICKS;
                rushHit.clear();
                server.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 3.0F, 0.8F);
            }
            case SPIT -> {
                spit(server);
                finish();
            }
            case BURROW -> {                                                         // se hunde: invulnerable
                server.playSound(null, blockPosition(), SoundEvents.GRAVEL_BREAK, SoundSource.HOSTILE, 3.0F, 0.5F);
                burst(server, position(), 40);
                setBurrowed(true);
                setAttack(EMERGE, enraged() ? UNDERGROUND_TICKS - 8 : UNDERGROUND_TICKS);
                lock = t.position();
            }
            case EMERGE -> emerge(server);
            case SUMMON -> {
                summon(server);
                finish();
            }
            default -> finish();
        }
    }

    private void finish() {
        setAttack(0, 0);
        cooldown = enraged() ? 16 : 26;
    }

    /** Embestida: avanza en línea recta; arrolla a lo que toca (una vez cada uno); contra una pared, aturdido. */
    private void tickRush(ServerLevel server) {
        rushLeft--;
        setWindupLeft(rushLeft);
        setDeltaMovement(rushDir.x * 0.9, getDeltaMovement().y, rushDir.z * 0.9);
        setYRot((float) (Math.atan2(-rushDir.x, rushDir.z) * 180 / Math.PI));
        yBodyRot = getYRot();
        if (tickCount % 2 == 0) {
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.NETHERRACK.defaultBlockState()),
                    getX(), getY() + 0.2, getZ(), 6, 1.0, 0.1, 1.0, 0.1);
        }
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.8), e -> e != this && e.isAlive()
                && !(e instanceof InfernalEntity) && !(e instanceof Player p && (p.isCreative() || p.isSpectator())))) {
            if (rushHit.add(e.getUUID())) {
                e.hurt(damageSources().mobAttack(this), 26.0F);
                e.push(rushDir.x * 1.6, 0.6, rushDir.z * 1.6);
                e.hurtMarked = true;
            }
        }
        if (horizontalCollision && rushLeft < RUSH_TICKS - 2) {                     // se estrella: aturdido
            server.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 3.0F, 0.5F);
            server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1.0, getZ(), 2, 0.5, 0.5, 0.5, 0);
            rushLeft = 0;
            exposedLeft = EXPOSED_TICKS;
            setExposed(true);
        }
        if (rushLeft <= 0) {
            setDeltaMovement(Vec3.ZERO);
            finish();
        }
    }

    private void spit(ServerLevel server) {
        server.playSound(null, blockPosition(), SoundEvents.LLAMA_SPIT, SoundSource.HOSTILE, 3.0F, 0.4F);
        Vec3 mouth = position().add(0, 2.0, 0).add(flat(lock.subtract(position())).scale(2.5));
        int n = enraged() ? 5 : 3;
        launchSlag(mouth, lock, 0, 18.0F, 2.2);
        for (int i = 1; i < n; i++) {
            double a = random.nextDouble() * Math.PI * 2, r = 2.0 + random.nextDouble() * 3.0;
            launchSlag(mouth, lock.add(Math.cos(a) * r, 0, Math.sin(a) * r), 3 * i, 18.0F, 2.2);
        }
    }

    /** Sale de golpe bajo el punto fijado: daño, lanza por el aire y queda expuesto un momento. */
    private void emerge(ServerLevel server) {
        BlockPos at = BlockPos.containing(lock);
        int ground = level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, at.getX(), at.getZ());
        double y = home != null && Math.abs(ground - home.getY()) > 6 ? home.getY() : ground;   // dentro de la excavación
        moveTo(lock.x, y, lock.z, getYRot(), 0);
        setBurrowed(false);
        server.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 3.0F, 0.6F);
        burst(server, position(), 60);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(2.0), e -> e != this && e.isAlive()
                && !(e instanceof InfernalEntity) && !(e instanceof Player p && (p.isCreative() || p.isSpectator())))) {
            e.hurt(damageSources().mobAttack(this), 32.0F);
            e.push(0, 1.1, 0);
            e.hurtMarked = true;
        }
        setAttack(0, 0);
        exposedLeft = EXPOSED_TICKS;
        setExposed(true);
        cooldown = enraged() ? 16 : 26;
    }

    private void summon(ServerLevel server) {
        server.playSound(null, blockPosition(), SoundEvents.SILVERFISH_AMBIENT, SoundSource.HOSTILE, 3.0F, 0.5F);
        for (int i = 0; i < 3; i++) {
            InfernalEntity mite = NetherEntities.MITE.get().create(server);
            if (mite == null) {
                continue;
            }
            double a = random.nextDouble() * Math.PI * 2;
            mite.moveTo(getX() + Math.cos(a) * 2, getY() + 1, getZ() + Math.sin(a) * 2, 0, 0);
            mite.finalizeSpawn(server, server.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            mite.setTarget(getTarget());
            server.addFreshEntity(mite);
        }
        burst(server, position(), 20);
    }

    /** Aviso de que algo sale del suelo: el suelo tiembla y echa escoria. */
    private void rumble(ServerLevel server, Vec3 c) {
        ring(server, c, 3.0, ParticleTypes.LAVA);
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.NETHERRACK.defaultBlockState()),
                c.x, c.y + 0.1, c.z, 12, 1.5, 0.1, 1.5, 0.15);
        if (tickCount % 6 == 0) {
            server.playSound(null, BlockPos.containing(c), SoundEvents.GRAVEL_BREAK, SoundSource.HOSTILE, 1.5F, 0.6F);
        }
    }

    private void burst(ServerLevel server, Vec3 c, int count) {
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.NETHERRACK.defaultBlockState()),
                c.x, c.y + 0.5, c.z, count, 1.2, 0.6, 1.2, 0.2);
        server.sendParticles(ParticleTypes.LAVA, c.x, c.y + 0.5, c.z, count / 4, 1.0, 0.3, 1.0, 0.1);
    }

    private void line(ServerLevel server, Vec3 dir) {
        for (int i = 2; i <= 16; i += 2) {
            Vec3 p = position().add(dir.scale(i));
            server.sendParticles(ParticleTypes.FLAME, p.x, p.y + 0.1, p.z, 1, 0.3, 0, 0.3, 0);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (burrowed() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;                                                            // bajo tierra no se le puede dar
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isPickable() {
        return !burrowed() && super.isPickable();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return attack() == 0 && !exposed() && !burrowed() && super.doHurtTarget(target);
    }

    /** Botín: un Núcleo de Forja por jugador que luchó (1-4), aleación, piroxita, netherita y componentes. */
    @Override
    protected void loot(int players) {
        drop(new ItemStack(ModItems.FORGE_HEART.get(), players));
        drop(new ItemStack(ModItems.SLAG_ALLOY.get(), 20 + random.nextInt(9)));
        drop(new ItemStack(ModItems.PIROXITE_INGOT.get(), 10 + random.nextInt(5)));
        drop(new ItemStack(Items.NETHERITE_INGOT, 2 + random.nextInt(2)));
        drop(new ItemStack(ModItems.ABYSSAL_COMPONENT.get(), 4));
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setBurrowed(false);                                                          // al cargar, siempre en la superficie
    }

    // pruebas
    public void testSetExposed(int ticks) {
        exposedLeft = ticks;
        setExposed(ticks > 0);
    }
}
