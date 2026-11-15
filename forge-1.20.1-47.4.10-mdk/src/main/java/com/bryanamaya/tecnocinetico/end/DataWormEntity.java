package com.bryanamaya.tecnocinetico.end;

import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Gusano de Datos (End, islas exteriores): serpiente mecánica de la red de la IA que excava la piedra del End.
 * <ul>
 *   <li><b>Bajo tierra</b> ({@link #BURROWED}) es invulnerable y va hacia el jugador más cercano (lo siente sin verlo);
 *       el suelo echa polvo y glitches por encima, así que se le ve venir. Solo avanza bajo piedra del End o tierra
 *       blanda: la obsidiana o los bloques puestos lo frenan.</li>
 *   <li><b>Aviso</b> ({@link #WARN}): el suelo tiembla bajo el objetivo {@link #WARN_TICKS} ticks (hay tiempo de
 *       apartarse).</li>
 *   <li><b>Embestida</b> ({@link #LUNGE}): sale en arco, la cabeza hace {@link #LUNGE_DAMAGE} y el cuerpo
 *       {@link #BODY_DAMAGE} al pasar; vuelve a hundirse ({@link #DIVE}). Mientras está fuera, todo él se puede golpear.</li>
 *   <li>Si cae sobre algo que no puede excavar, queda <b>aturdido</b> ({@link #STUNNED}) {@link #STUN_TICKS} ticks y
 *       recibe ×{@link #STUN_MULTIPLIER}.</li>
 *   <li><b>Segmentos</b> ({@link DataWormPart}): cada uno tiene su vida; el golpe le pasa la mitad al gusano y, al
 *       romperse, el gusano recibe {@link #BREAK_DAMAGE} más y se acorta.</li>
 * </ul>
 * Contrato con el renderizador: {@link #state()}, {@link #broken(int)}, {@link #jaw(float)}, las partes con su
 * posición (xo/getX) y {@link #partSize(int)}.
 */
public class DataWormEntity extends Monster {

    public static final int BURROWED = 0, WARN = 1, LUNGE = 2, DIVE = 3, STUNNED = 4;
    /** Segmentos del cuerpo; la cola es la parte {@code SEGMENTS}. */
    public static final int SEGMENTS = 6;
    public static final double SPACING = 1.1D, DEPTH = 2.5D, DIG_SPEED = 0.32D, SENSE = 32.0D;
    public static final double LUNGE_UP = 0.85D, GRAVITY = 0.07D, HEAD_HALF = 0.6D;
    public static final float HEAD_HEALTH = 110.0F, SEGMENT_HEALTH = 24.0F, TAIL_HEALTH = 16.0F;
    public static final float LUNGE_DAMAGE = 14.0F, BODY_DAMAGE = 6.0F, BREAK_DAMAGE = 12.0F, STUN_MULTIPLIER = 1.5F;
    public static final int WARN_TICKS = 14, STUN_TICKS = 60, COOLDOWN = 50, STUCK_LUNGE = 30;
    private static final int NONE = Integer.MIN_VALUE;

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(DataWormEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BROKEN = SynchedEntityData.defineId(DataWormEntity.class, EntityDataSerializers.INT);

    private final DataWormPart[] parts;
    private final float[] partHealth;
    private final ArrayDeque<Vec3> trail = new ArrayDeque<>();
    private final Set<UUID> hit = new HashSet<>();
    private int timer, cooldown = 40, stuck, stateTick, resetTrail;
    private Vec3 lock = Vec3.ZERO;
    @Nullable
    private Player prey;

    public DataWormEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        parts = new DataWormPart[SEGMENTS + 1];
        partHealth = new float[SEGMENTS + 1];
        for (int i = 0; i <= SEGMENTS; i++) {
            parts[i] = new DataWormPart(this, i, partSize(i));
            partHealth[i] = i == SEGMENTS ? TAIL_HEALTH : SEGMENT_HEALTH;
        }
        setId(ENTITY_COUNTER.getAndAdd(parts.length + 1) + 1);               // ids de las partes = siguientes al gusano
        noPhysics = true;
        setNoGravity(true);
        xpReward = 30;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HEAD_HEALTH)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0D)
                .add(Attributes.ATTACK_DAMAGE, LUNGE_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, SENSE);
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (parts != null) {
            for (int i = 0; i < parts.length; i++) {
                parts[i].setId(id + i + 1);
            }
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(STATE, BURROWED);
        entityData.define(BROKEN, 0);
    }

    @Override
    protected void registerGoals() {
    }

    // ------------------------------------------------------------------ consultas (también el cliente)

    public int state() {
        return entityData.get(STATE);
    }

    public boolean broken(int part) {
        return (entityData.get(BROKEN) & (1 << part)) != 0;
    }

    public static float partSize(int part) {
        return part == SEGMENTS ? 0.8F : 1.0F;
    }

    public DataWormPart[] parts() {
        return parts;
    }

    /** Fuera de la tierra: se le puede golpear. */
    public boolean exposed() {
        int s = state();
        return s == LUNGE || s == DIVE || s == STUNNED;
    }

    /** ¿Este segmento se puede golpear? (fuera, sin romper y no metido en un bloque). */
    public boolean partExposed(int part) {
        if (!exposed() || broken(part)) {
            return false;
        }
        BlockPos b = BlockPos.containing(parts[part].getBoundingBox().getCenter());
        return level().getBlockState(b).getCollisionShape(level(), b).isEmpty();
    }

    /** Apertura de las fauces, 0..1 (renderizador). */
    public float jaw(float partial) {
        float t = tickCount - stateTick + partial;
        return switch (state()) {
            case WARN -> 0.3F;
            case LUNGE -> t < 12 ? 1.0F : Math.max(0.0F, 1.0F - (t - 12) / 6.0F);
            case STUNNED -> 0.45F + 0.1F * Mth.sin(t * 0.4F);
            default -> 0.0F;
        };
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return parts;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (STATE.equals(key)) {
            stateTick = tickCount;
            if (level().isClientSide && state() == LUNGE) {
                resetTrail = 4;                                               // la posición llega por separado: varios ticks
            }
        }
    }

    // ------------------------------------------------------------------ tick común: el cuerpo sigue a la cabeza

    @Override
    public void tick() {
        super.tick();
        if (resetTrail > 0) {
            resetTrail--;
            column(position());
        }
        layout();
    }

    /** Rastro recto hacia abajo desde {@code top}: el cuerpo sale del suelo detrás de la cabeza. */
    private void column(Vec3 top) {
        trail.clear();
        for (int j = 0; j < 14; j++) {
            trail.addLast(top.add(0, -j * 0.6, 0));
        }
    }

    /** Coloca cada segmento sano a {@link #SPACING} del anterior siguiendo el rastro de la cabeza. */
    private void layout() {
        Vec3 head = position();
        if (trail.isEmpty() || trail.peekFirst().distanceToSqr(head) > 0.0025) {
            trail.addFirst(head);
        }
        while (trail.size() > 90) {
            trail.removeLast();
        }
        boolean lying = state() == STUNNED;
        Vec3 back = Vec3.directionFromRotation(0, getYRot()).scale(-1);
        Vec3[] pts = trail.toArray(new Vec3[0]);
        int idx = 0;
        Vec3 cur = pts[0];
        double walked = 0.0D;
        int k = 0;
        for (int i = 0; i < parts.length; i++) {
            Vec3 at;
            if (broken(i)) {
                at = head;                                                    // roto: no se dibuja ni se puede golpear
            } else {
                double want = ++k * SPACING + 0.2D + (i == SEGMENTS ? 0.1D : 0.0D);   // la cabeza es más larga; la cola, algo
                if (lying) {
                    at = head.add(back.scale(want));                          // aturdido: tendido en el suelo
                } else {
                    while (true) {
                        if (idx + 1 >= pts.length) {
                            at = cur.add(0, -(want - walked), 0);            // sin rastro: hacia abajo (bajo tierra)
                            break;
                        }
                        Vec3 next = pts[idx + 1];
                        double seg = cur.distanceTo(next);
                        if (walked + seg >= want) {
                            at = cur.add(next.subtract(cur).scale((want - walked) / Math.max(seg, 1e-6)));
                            cur = at;
                            walked = want;
                            break;
                        }
                        walked += seg;
                        cur = next;
                        idx++;
                    }
                }
            }
            DataWormPart p = parts[i];
            float s = partSize(i);
            p.xo = p.getX();
            p.yo = p.getY();
            p.zo = p.getZ();
            p.xOld = p.xo;
            p.yOld = p.yo;
            p.zOld = p.zo;
            p.setPos(at.x, at.y + HEAD_HALF - s / 2.0, at.z);
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate((SEGMENTS + 1) * SPACING + 1.0D);
    }

    /** El cliente salta directamente a las posiciones lejanas (al salir del suelo) en vez de deslizarse bajo tierra. */
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        super.lerpTo(x, y, z, yRot, xRot, distanceToSqr(x, y, z) > 16.0D ? 1 : steps, teleport);
    }

    // ------------------------------------------------------------------ servidor

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        if (tickCount % 10 == 0 || prey != null && !valid(prey)) {
            prey = findPrey();
        }
        setTarget(prey);
        switch (state()) {
            case BURROWED -> burrowed(server);
            case WARN -> warn(server);
            case LUNGE, DIVE -> flight(server);
            case STUNNED -> stunned(server);
            default -> setState(BURROWED);
        }
    }

    private void setState(int s) {
        entityData.set(STATE, s);
        stateTick = tickCount;
    }

    private boolean valid(Player p) {
        return p.isAlive() && !p.isCreative() && !p.isSpectator() && p.level() == level() && distanceToSqr(p) < SENSE * SENSE;
    }

    @Nullable
    private Player findPrey() {
        Player best = null;
        for (Player p : level().players()) {
            if (valid(p) && (best == null || distanceToSqr(p) < distanceToSqr(best))) {
                best = p;
            }
        }
        return best;
    }

    /** ¿Lo puede excavar? Piedra del End (también la corrupta) y tierra blanda. */
    public static boolean diggable(BlockState s) {
        return s.is(Blocks.END_STONE) || s.is(ModBlocks.CORRUPTED_END_STONE.get()) || s.is(BlockTags.MINEABLE_WITH_SHOVEL);
    }

    /** Altura de la superficie (primer aire sobre el suelo) o NONE si es vacío o el chunk no está cargado. */
    private int topAt(double x, double z) {
        BlockPos col = BlockPos.containing(x, 0, z);
        if (!level().hasChunkAt(col)) {
            return NONE;
        }
        int top = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, col.getX(), col.getZ());
        return top <= level().getMinBuildHeight() ? NONE : top;
    }

    /** Superficie excavable en esa columna, o NONE. */
    private int groundAt(double x, double z) {
        int top = topAt(x, z);
        return top != NONE && diggable(level().getBlockState(BlockPos.containing(x, top - 1, z))) ? top : NONE;
    }

    private void burrowed(ServerLevel server) {
        setDeltaMovement(Vec3.ZERO);
        if (cooldown > 0) {
            cooldown--;
        }
        int here = topAt(getX(), getZ());
        if (prey == null) {
            if (here != NONE && tickCount % 40 == 0) {
                dust(server, new Vec3(getX(), here, getZ()), 4);
            }
            return;
        }
        Vec3 to = new Vec3(prey.getX() - getX(), 0, prey.getZ() - getZ());
        double d = to.length();
        if (d > 2.0D) {
            Vec3 step = to.scale(Math.min(DIG_SPEED, d) / d);
            int ground = groundAt(getX() + step.x, getZ() + step.z);
            if (ground != NONE) {
                setPos(getX() + step.x, ground - DEPTH, getZ() + step.z);
                stuck = 0;
                here = ground;
            } else {
                stuck++;
            }
            face(step);
        }
        if (here != NONE && tickCount % 3 == 0) {                             // rastro en la superficie: se le ve venir
            dust(server, new Vec3(getX(), here, getZ()), 3);
        }
        if (here != NONE && tickCount % 12 == 0) {
            server.playSound(null, getX(), here, getZ(), SoundEvents.WARDEN_DIG, SoundSource.HOSTILE, 0.5F, 1.3F);
        }
        if (cooldown <= 0 && (d <= 2.5D || stuck > STUCK_LUNGE && d < 10.0D)) {
            lock = new Vec3(getX(), here != NONE ? here : getY() + DEPTH, getZ());
            timer = WARN_TICKS;
            setState(WARN);
            server.playSound(null, lock.x, lock.y, lock.z, SoundEvents.WARDEN_DIG, SoundSource.HOSTILE, 1.5F, 0.7F);
        }
    }

    private void warn(ServerLevel server) {
        setDeltaMovement(Vec3.ZERO);
        if (timer % 2 == 0) {
            com.bryanamaya.tecnocinetico.util.ParticleShapes.ring(server, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.END_STONE.defaultBlockState()),
                    lock.add(0, 0.1, 0), 1.6, 14);
            server.sendParticles(ModParticles.DATA_GLITCH.get(), lock.x, lock.y + 0.3, lock.z, 4, 0.8, 0.1, 0.8, 0.02);
        }
        if (--timer <= 0) {
            lunge(server);
        }
    }

    private void lunge(ServerLevel server) {
        Vec3 dir = Vec3.directionFromRotation(0, getYRot());
        double dist = 0.0D;
        if (prey != null) {
            Vec3 to = new Vec3(prey.getX() - lock.x, 0, prey.getZ() - lock.z);
            dist = to.length();
            if (dist > 0.3D) {
                dir = to.normalize();
            }
        }
        double air = 2.0D * LUNGE_UP / GRAVITY;
        double h = Mth.clamp(dist / air * 1.2D + 0.12D, 0.12D, 0.6D);
        Vec3 vel = dir.scale(h).add(0, LUNGE_UP, 0);
        if (topAt(lock.x + vel.x * air, lock.z + vel.z * air) == NONE) {     // caería al vacío: al revés o recto
            vel = topAt(lock.x - vel.x * air, lock.z - vel.z * air) != NONE ? new Vec3(-vel.x, vel.y, -vel.z) : new Vec3(0, LUNGE_UP, 0);
        }
        moveTo(lock.x, lock.y - 1.0D, lock.z, getYRot(), 0.0F);
        setDeltaMovement(vel);
        face(vel);
        column(position());
        hit.clear();
        setState(LUNGE);
        burst(server, lock, 40);
        server.playSound(null, lock.x, lock.y, lock.z, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 1.6F, 1.4F);
        server.playSound(null, lock.x, lock.y, lock.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0F, 0.7F);
    }

    private void flight(ServerLevel server) {
        Vec3 v = getDeltaMovement();
        int top = topAt(getX(), getZ());
        if (state() == LUNGE) {
            v = new Vec3(v.x, Math.max(v.y - GRAVITY, -0.9D), v.z);
            if (v.y < 0 && top != NONE && getY() <= top + 0.2D) {               // vuelve al suelo
                BlockState ground = level().getBlockState(BlockPos.containing(getX(), top - 1, getZ()));
                if (diggable(ground)) {
                    setState(DIVE);
                    burst(server, new Vec3(getX(), top, getZ()), 24);
                    server.playSound(null, getX(), top, getZ(), SoundEvents.WARDEN_DIG, SoundSource.HOSTILE, 1.4F, 0.9F);
                } else {                                                        // no puede excavar: se estrella
                    setPos(getX(), top, getZ());
                    setDeltaMovement(Vec3.ZERO);
                    timer = STUN_TICKS;
                    setState(STUNNED);
                    server.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5F, 0.6F);
                    server.sendParticles(ModParticles.DATA_GLITCH.get(), getX(), getY() + 0.6, getZ(), 30, 1.0, 0.4, 1.0, 0.1);
                    return;
                }
            }
        } else {                                                                 // DIVE: se mete y avanza hasta esconder la cola
            Vec3 flat = new Vec3(v.x, 0, v.z);
            if (flat.lengthSqr() < 0.09D) {
                flat = Vec3.directionFromRotation(0, getYRot()).scale(0.3D);
            }
            double vy = top != NONE && getY() < top - DEPTH ? 0.0D : Math.max(v.y - GRAVITY, -0.6D);
            if (topAt(getX() + flat.x * 4, getZ() + flat.z * 4) == NONE) {     // borde de la isla: baja en vertical
                flat = Vec3.ZERO;
                vy = -0.3D;
            }
            v = new Vec3(flat.x, vy, flat.z);
            if (tailHidden() || tickCount - stateTick > 80) {
                int ground = groundAt(getX(), getZ());
                if (ground != NONE) {
                    setPos(getX(), ground - DEPTH, getZ());
                } else {                                                        // sin suelo excavable: vuelve a donde salió
                    setPos(lock.x, lock.y - DEPTH, lock.z);
                }
                setDeltaMovement(Vec3.ZERO);
                cooldown = COOLDOWN;
                setState(BURROWED);
                return;
            }
        }
        setDeltaMovement(v);
        face(v);
        contact(server);
    }

    /** ¿La última parte sana ya está bajo tierra? */
    private boolean tailHidden() {
        for (int i = parts.length - 1; i >= 0; i--) {
            if (!broken(i)) {
                DataWormPart p = parts[i];
                int top = topAt(p.getX(), p.getZ());
                return top == NONE || p.getBoundingBox().maxY < top;
            }
        }
        return true;
    }

    private void stunned(ServerLevel server) {
        setDeltaMovement(Vec3.ZERO);
        if (timer % 5 == 0) {
            server.sendParticles(ModParticles.DATA_GLITCH.get(), getX(), getY() + 0.6, getZ(), 4, 0.6, 0.3, 0.6, 0.05);
            server.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.8, getZ(), 2, 0.3, 0.2, 0.3, 0.01);
        }
        if (--timer <= 0) {                                                    // se abre paso hacia abajo como sea
            setDeltaMovement(Vec3.directionFromRotation(0, getYRot()).scale(0.3D).add(0, -0.4D, 0));
            setState(DIVE);
        }
    }

    /** Daño por contacto mientras está fuera: cabeza fuerte, cuerpo flojo; una vez por embestida a cada uno. */
    private void contact(ServerLevel server) {
        if (state() != LUNGE && state() != DIVE) {
            return;
        }
        hurtTouching(getBoundingBox().inflate(0.3D), LUNGE_DAMAGE, 0.7D);
        for (int i = 0; i < parts.length; i++) {
            if (partExposed(i)) {
                hurtTouching(parts[i].getBoundingBox().inflate(0.15D), BODY_DAMAGE, 0.3D);
            }
        }
    }

    private void hurtTouching(AABB box, float damage, double lift) {
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive()
                && !(e instanceof DataWormEntity) && !(e instanceof Player p && (p.isCreative() || p.isSpectator())))) {
            if (hit.add(e.getUUID()) && e.hurt(damageSources().mobAttack(this), damage)) {
                Vec3 v = getDeltaMovement();
                e.push(v.x * 0.6D, lift, v.z * 0.6D);
                e.hurtMarked = true;
            }
        }
    }

    private void face(Vec3 v) {
        if (v.lengthSqr() < 1e-6) {
            return;
        }
        float yaw = (float) (Mth.atan2(-v.x, v.z) * Mth.RAD_TO_DEG);
        if (v.x * v.x + v.z * v.z > 1e-6) {
            setYRot(yaw);
            yBodyRot = yaw;
            yHeadRot = yaw;
        }
        setXRot((float) (-Mth.atan2(v.y, v.horizontalDistance()) * Mth.RAD_TO_DEG));
    }

    private void dust(ServerLevel server, Vec3 at, int count) {
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.END_STONE.defaultBlockState()), at.x, at.y + 0.05, at.z,
                count, 0.4, 0.05, 0.4, 0.1);
        server.sendParticles(ModParticles.DATA_GLITCH.get(), at.x, at.y + 0.2, at.z, 1, 0.3, 0.05, 0.3, 0.01);
    }

    private void burst(ServerLevel server, Vec3 at, int count) {
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.END_STONE.defaultBlockState()), at.x, at.y + 0.3, at.z,
                count, 0.9, 0.3, 0.9, 0.25);
        server.sendParticles(ModParticles.DATA_GLITCH.get(), at.x, at.y + 0.5, at.z, count / 2, 0.8, 0.5, 0.8, 0.1);
    }

    // ------------------------------------------------------------------ daño

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!exposed() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;                                                     // bajo tierra no se le puede dar
        }
        return super.hurt(source, state() == STUNNED ? amount * STUN_MULTIPLIER : amount);
    }

    /** Golpe a un segmento: su vida, la mitad al gusano; si se rompe, daño extra y se acorta. */
    public boolean hurtSegment(int part, DamageSource source, float amount) {
        if (level().isClientSide || !partExposed(part) || isDeadOrDying()) {
            return false;
        }
        if (state() == STUNNED) {
            amount *= STUN_MULTIPLIER;
        }
        partHealth[part] -= amount;
        float toWorm = amount * 0.5F;
        if (partHealth[part] <= 0.0F) {
            entityData.set(BROKEN, entityData.get(BROKEN) | (1 << part));
            toWorm += BREAK_DAMAGE;
            if (level() instanceof ServerLevel server) {
                Vec3 c = parts[part].getBoundingBox().getCenter();
                server.sendParticles(ModParticles.DATA_GLITCH.get(), c.x, c.y, c.z, 30, 0.4, 0.4, 0.4, 0.15);
                server.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 1, 0, 0, 0, 0);
                server.playSound(null, c.x, c.y, c.z, SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.HOSTILE, 1.4F, 0.6F);
            }
        }
        invulnerableTime = 0;
        super.hurt(source, toWorm);
        return true;
    }

    @Override
    public boolean isPickable() {
        return exposed() && super.isPickable();
    }

    @Override
    public boolean isInWall() {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity e) {
    }

    @Override
    public void knockback(double strength, double x, double z) {
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    /** Movimiento propio: sin rozamiento ni gravedad vanilla (la del arco la pone {@link #flight}). */
    @Override
    public void travel(Vec3 input) {
        if (isEffectiveAi()) {
            move(MoverType.SELF, getDeltaMovement());
        }
    }

    // ------------------------------------------------------------------ aparición y guardado

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data, tag);
        int ground = groundAt(getX(), getZ());
        if (ground != NONE) {
            setPos(getX(), ground - DEPTH, getZ());                          // nace bajo tierra
        }
        return out;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag hp = new ListTag();
        for (float f : partHealth) {
            hp.add(FloatTag.valueOf(f));
        }
        tag.put("PartHealth", hp);
        tag.putInt("Broken", entityData.get(BROKEN));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ListTag hp = tag.getList("PartHealth", Tag.TAG_FLOAT);
        for (int i = 0; i < Math.min(hp.size(), partHealth.length); i++) {
            partHealth[i] = hp.getFloat(i);
        }
        entityData.set(BROKEN, tag.getInt("Broken"));
        entityData.set(STATE, BURROWED);                                     // al cargar, siempre bajo tierra
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server) {
            for (int i = 0; i < parts.length; i++) {
                if (!broken(i)) {
                    Vec3 c = parts[i].getBoundingBox().getCenter();
                    server.sendParticles(ModParticles.DATA_GLITCH.get(), c.x, c.y, c.z, 12, 0.4, 0.4, 0.4, 0.12);
                }
            }
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WARDEN_DEATH;
    }

    // ------------------------------------------------------------------ pruebas

    public float partHealth(int part) {
        return partHealth[part];
    }

    /** Fuerza un estado (pruebas): {@code LUNGE} sale ahí mismo. */
    public void testState(int s) {
        if (s == LUNGE && level() instanceof ServerLevel server) {
            lock = new Vec3(getX(), getY() + DEPTH, getZ());
            lunge(server);
        } else {
            timer = s == STUNNED ? STUN_TICKS : WARN_TICKS;
            setState(s);
        }
    }
}
