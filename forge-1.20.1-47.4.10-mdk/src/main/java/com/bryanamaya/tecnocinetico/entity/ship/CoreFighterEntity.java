package com.bryanamaya.tecnocinetico.entity.ship;

import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
import com.bryanamaya.tecnocinetico.sound.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.UUID;

/**
 * Caza del Núcleo: nave de fin de juego (tecnología robada a la IA). Vuelo "arcade de caza": la nave sigue a la
 * cámara del piloto con giro limitado, W acelera (hasta ~250 km/h), S frena (hasta quedarse flotando: despegue vertical), espacio
 * sube y Ctrl baja. Clic izquierdo: dos cañones cinéticos automáticos. Clic derecho: misil guiado que fija al
 * objetivo más centrado. J: menú del motor de salto entre dimensiones ({@link ShipJump}).
 *
 * El movimiento lo calcula el cliente del piloto (como las barcas); el servidor dispara, recibe daño y salta.
 * Las entradas del piloto las escribe {@code client/ClientShipHandler} en cada tick.
 */
public class CoreFighterEntity extends Entity {

    public static final float MAX_HEALTH = 150.0F;
    public static final double MAX_SPEED = 3.5D;                      // ~250 km/h
    public static final int JUMP_TICKS = 60, MISSILE_COOLDOWN = 50;
    private static final float TURN = 6.0F;

    private static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(CoreFighterEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> JUMP = SynchedEntityData.defineId(CoreFighterEntity.class, EntityDataSerializers.INT);
    /** Acoplador del Nether instalado (botín del Fundidor del Abismo): sin él no puede saltar al Nether. */
    private static final EntityDataAccessor<Boolean> NETHER_DRIVE = SynchedEntityData.defineId(CoreFighterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MISSILE_CD = SynchedEntityData.defineId(CoreFighterEntity.class, EntityDataSerializers.INT);

    // entradas del piloto (cliente)
    public float inForward;
    public boolean inUp, inDown;
    // disparo (servidor, por ModNetwork.ShipControlPacket)
    public boolean fireGuns, fireMissile;
    // estado de vuelo
    public double speed;
    /** Alabeo visual (cliente). */
    public float roll, rollO;
    private int gunCd, lerpSteps;
    private boolean leftGun;
    private double lerpX, lerpY, lerpZ, lerpYRot, lerpXRot;
    private double lastSpeedSeen;
    private Vec3 lastMove = Vec3.ZERO;
    /** Vida con la que quedan los restos al destruirse (hay que repararlos con Placas de Inercita). */
    public static final float WRECK_HEALTH = 15.0F;
    @Nullable
    private ResourceKey<Level> jumpTo;
    @Nullable
    UUID pendingPilot;
    int pendingTicks;

    public CoreFighterEntity(EntityType<? extends CoreFighterEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        this.blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(HEALTH, MAX_HEALTH);
        entityData.define(JUMP, 0);
        entityData.define(MISSILE_CD, 0);
        entityData.define(NETHER_DRIVE, false);
    }

    public float health() {
        return entityData.get(HEALTH);
    }

    public void setHealth(float h) {
        entityData.set(HEALTH, Mth.clamp(h, 0.0F, MAX_HEALTH));
    }

    /** Ticks de carga del salto (0 = sin saltar). */
    public int jumpTicks() {
        return entityData.get(JUMP);
    }

    public int missileCooldown() {
        return entityData.get(MISSILE_CD);
    }

    // ------------------------------------------------------------------ montura

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.0D;
    }

    /**
     * Escala del modelo (a tamaño de diseño no cabía un personaje de Minecraft): ~11 bloques de largo y 10 de
     * envergadura. Todas las posiciones de la nave (asiento, cañones, toberas, salto...) se multiplican por ella.
     */
    public static final float SCALE = 2.5F;
    /** Altura del asiento (donde apoya el piloto) sobre la base de la nave, distancia hacia el morro y pivote de giro. */
    public static final double SEAT_HEIGHT = 2.3D, SEAT_FORWARD = 1.06D * SCALE, PIVOT = 0.75D * SCALE;

    /** Asiento en la cabina, siguiendo el cabeceo de la nave (gira alrededor del mismo pivote que el modelo). */
    public Vec3 seat(float partialTick) {
        float pitch = Mth.lerp(partialTick, xRotO, getXRot()), yaw = Mth.rotLerp(partialTick, yRotO, getYRot());
        Vec3 fwd = Vec3.directionFromRotation(pitch, yaw), up = Vec3.directionFromRotation(pitch - 90.0F, yaw);
        Vec3 base = new Vec3(Mth.lerp(partialTick, xo, getX()), Mth.lerp(partialTick, yo, getY()), Mth.lerp(partialTick, zo, getZ()));
        return base.add(0, PIVOT, 0).add(fwd.scale(SEAT_FORWARD)).add(up.scale(SEAT_HEIGHT - PIVOT));
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        Vec3 s = seat(1.0F);
        move.accept(passenger, s.x, s.y - 0.55D, s.z);                             // los pies quedan 0,55 bajo el asiento
        if (passenger instanceof LivingEntity l) {
            l.setYBodyRot(getYRot());                                                // el cuerpo mira al morro; la cabeza, libre
            l.yBodyRotO = yRotO;
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (passenger instanceof LivingEntity l && !onGround()) {
            l.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0));  // saltar en el aire: paracaídas
        }
    }

    public boolean netherDrive() {
        return entityData.get(NETHER_DRIVE);
    }

    public void setNetherDrive(boolean on) {
        entityData.set(NETHER_DRIVE, on);
    }

    /** ¿Puede saltar a esa dimensión? Al Nether solo con el Acoplador (volver desde el Nether siempre se puede). */
    public boolean canJumpTo(net.minecraft.resources.ResourceKey<Level> dest) {
        return dest != Level.NETHER || netherDrive();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.NETHER_COUPLER.get())) {                            // instalar el Acoplador del Nether
            if (!level().isClientSide) {
                if (netherDrive()) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.tecnocinetico.coupler_installed_already"), true);
                    return InteractionResult.FAIL;
                }
                setNetherDrive(true);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                playSound(net.minecraft.sounds.SoundEvents.SMITHING_TABLE_USE, 1.0F, 0.8F);
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.tecnocinetico.coupler_installed")
                        .withStyle(net.minecraft.ChatFormatting.GOLD), true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (held.is(ModItems.INERCITA_PLATE.get()) && health() < MAX_HEALTH) {      // reparar el casco
            if (!level().isClientSide) {
                setHealth(health() + 30.0F);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                playSound(net.minecraft.sounds.SoundEvents.ANVIL_USE, 0.6F, 1.4F);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (player.isSecondaryUseActive() || isVehicle()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(3.5D * SCALE, 1.0D * SCALE, 3.5D * SCALE);    // el modelo es más grande que la caja
    }

    // ------------------------------------------------------------------ daño

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved() || isInvulnerableTo(source)) {
            return false;
        }
        if (source.getEntity() != null && hasPassenger(source.getEntity())) {
            return false;                                                         // el piloto no se dispara a sí mismo
        }
        if (source.getEntity() instanceof Player p && p.getAbilities().instabuild && !isVehicle()) {
            discard();                                                            // creativo: quitarla de un golpe
            return true;
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            return false;
        }
        setHealth(health() - amount);
        playSound(net.minecraft.sounds.SoundEvents.IRON_GOLEM_DAMAGE, 0.8F, 1.3F);
        if (health() <= 0) {
            destroy();
        }
        return true;
    }

    private void destroy() {
        level().explode(this, getX(), getY() + PIVOT, getZ(), 4.0F, Level.ExplosionInteraction.NONE);
        ejectPassengers();
        ItemStack wreck = new ItemStack(ModItems.CORE_FIGHTER.get());          // los restos se pueden recuperar,
        wreck.getOrCreateTag().putFloat("Health", WRECK_HEALTH);              // pero muy dañados
        if (netherDrive()) {
            wreck.getOrCreateTag().putBoolean("NetherDrive", true);              // el acoplador se conserva
        }
        spawnAtLocation(wreck);
        discard();
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean causeFallDamage(float distance, float mult, DamageSource source) {
        return false;
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        super.tick();
        rollO = roll;
        LivingEntity pilot = getControllingPassenger();
        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
            fly(pilot);
            move(MoverType.SELF, getDeltaMovement());
        } else if (level().isClientSide) {
            tickLerp();
        } else if (pilot == null) {
            setDeltaMovement(getDeltaMovement().scale(0.85D));                    // sin piloto: se queda flotando
            if (getDeltaMovement().lengthSqr() > 1e-4) {
                move(MoverType.SELF, getDeltaMovement());
            }
        }
        if (level().isClientSide) {
            float yawRate = Mth.wrapDegrees(getYRot() - yRotO);
            roll = Mth.lerp(0.2F, roll, Mth.clamp(-yawRate * 6.0F, -50.0F, 50.0F));
            clientEffects();
        } else {
            serverTick(pilot);
        }
    }

    /** Vuelo arcade: sigue a la cámara del piloto, con empuje, frenada y ascenso/descenso. */
    private void fly(@Nullable LivingEntity pilot) {
        if (pilot == null) {
            setDeltaMovement(getDeltaMovement().scale(0.85D));
            return;
        }
        float wantYaw = pilot.getYRot(), wantPitch = Mth.clamp(pilot.getXRot(), -60.0F, 60.0F);
        setYRot(Mth.approachDegrees(getYRot(), wantYaw, TURN));
        setXRot(Mth.approach(getXRot(), speed > 0.15 ? wantPitch : 0.0F, TURN * 0.6F));
        if (jumpTicks() > 0) {                                                     // cargando el salto: se detiene
            speed = Math.max(0.0D, speed - 0.05D);
        } else if (inForward > 0) {
            speed = Math.min(MAX_SPEED, speed + 0.07D);
        } else if (inForward < 0) {
            speed = Math.max(0.0D, speed - 0.12D);
        } else {
            speed = Math.max(0.0D, speed - 0.006D);
        }
        Vec3 fwd = Vec3.directionFromRotation(getXRot(), getYRot()).scale(speed);
        double vert = (inUp ? 0.28D : 0.0D) - (inDown ? 0.28D : 0.0D);
        Vec3 want = fwd.add(0, vert, 0);
        setDeltaMovement(getDeltaMovement().lerp(want, 0.25D));
        if (horizontalCollision && speed > 0.7D) {
            speed *= 0.2D;                                                          // choque (el daño lo calcula el servidor)
        }
    }

    private void clientEffects() {
        // toberas: chispas
        Vec3 back = Vec3.directionFromRotation(getXRot(), getYRot()).scale(-2.1D * SCALE);
        Vec3 side = Vec3.directionFromRotation(0, getYRot() + 90.0F).scale(0.5D * SCALE);
        if (tickCount % 2 == 0) {
            for (int s = -1; s <= 1; s += 2) {
                Vec3 p = position().add(back).add(side.scale(s)).add(0, 0.82 * SCALE, 0);
                level().addParticle(ModParticles.KINETIC_SPARK.get(), p.x, p.y, p.z, back.x * 0.05, 0, back.z * 0.05);
            }
        }
        int j = jumpTicks();
        if (j > 0) {                                                                // anillo de salto que se cierra
            float k = j / (float) JUMP_TICKS;
            double r = (4.5 - 3.5 * k) * SCALE;
            for (int i = 0; i < 6; i++) {
                double a = (tickCount * 0.35 + i * Math.PI / 3);
                double x = getX() + Math.cos(a) * r, z = getZ() + Math.sin(a) * r;
                level().addParticle(ParticleTypes.REVERSE_PORTAL, x, getY() + PIVOT + Math.sin(a * 2) * 0.9, z, 0, 0.02, 0);
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, x, getY() + PIVOT, z, 0, 0, 0);
            }
        }
    }

    private void serverTick(@Nullable LivingEntity pilot) {
        // choque: frenazo brusco a mucha velocidad
        Vec3 moved = new Vec3(getX() - xo, getY() - yo, getZ() - zo);
        double seen = moved.length();
        // La posición la manda el cliente: si en un tick no llega su paquete parece un frenazo. Solo es choque si
        // de verdad hay algo sólido delante.
        if (lastSpeedSeen > 0.9D && seen < lastSpeedSeen * 0.3D && pilot != null
                && !level().noCollision(this, getBoundingBox().move(lastMove.normalize().scale(0.6D)))) {
            float dmg = (float) (lastSpeedSeen * 14.0D);
            setHealth(health() - dmg);
            pilot.hurt(damageSources().flyIntoWall(), dmg * 0.25F);
            playSound(net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, 0.8F, 1.4F);
            if (health() <= 0) {
                destroy();
                return;
            }
        }
        lastSpeedSeen = seen;
        if (seen > 1.0E-4D) {
            lastMove = moved;
        }
        if (gunCd > 0) {
            gunCd--;
        }
        if (missileCooldown() > 0) {
            entityData.set(MISSILE_CD, missileCooldown() - 1);
        }
        if (pilot instanceof ServerPlayer sp2 && jumpTicks() == 0) {
            if (fireGuns && gunCd <= 0) {
                fireCannon(sp2);
                gunCd = 2;
            }
            if (fireMissile && missileCooldown() <= 0) {
                launchMissile(sp2);
            }
        }
        if (pilot == null) {
            fireGuns = fireMissile = false;
        }
        // salto entre dimensiones
        int j = jumpTicks();
        if (j > 0) {
            entityData.set(JUMP, j + 1);
            if (j == 1 || j == 30) {
                playSound(ModSounds.SHIP_JUMP_CHARGE.get(), 2.0F, j == 1 ? 0.8F : 1.2F);
            }
            if (j + 1 >= JUMP_TICKS && jumpTo != null) {
                entityData.set(JUMP, 0);
                ShipJump.perform(this, jumpTo);
                return;
            }
        }
        // volver a subir al piloto tras saltar
        if (pendingPilot != null) {
            Player p = level().getPlayerByUUID(pendingPilot);
            if (p != null && p.distanceToSqr(this) < 64 && !p.isPassenger()) {
                p.startRiding(this, true);
                pendingPilot = null;
            } else if (++pendingTicks > 200) {
                pendingPilot = null;
            }
        }
        if (getY() < level().getMinBuildHeight() - 10) {
            destroy();
        }
    }

    /** Pide el salto a otra dimensión (desde el menú del piloto). */
    public void startJump(ResourceKey<Level> to) {
        if (jumpTicks() > 0 || to == level().dimension()) {
            return;
        }
        jumpTo = to;
        entityData.set(JUMP, 1);
    }

    @Nullable
    public static ResourceKey<Level> dimensionKey(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? null : ResourceKey.create(Registries.DIMENSION, rl);
    }

    // ------------------------------------------------------------------ armas

    private Vec3 muzzle(boolean left) {
        Vec3 fwd = Vec3.directionFromRotation(getXRot(), getYRot());
        Vec3 side = Vec3.directionFromRotation(0, getYRot() + 90.0F);
        return position().add(0, 0.75 * SCALE, 0).add(fwd.scale(1.8 * SCALE)).add(side.scale((left ? 1.1 : -1.1) * SCALE));
    }

    private void fireCannon(ServerPlayer pilot) {
        ServerLevel server = (ServerLevel) level();
        leftGun = !leftGun;
        Vec3 from = muzzle(leftGun);
        Vec3 dir = pilot.getLookAngle();
        double s = Math.toRadians(1.2);
        dir = dir.add(random.nextGaussian() * s, random.nextGaussian() * s, random.nextGaussian() * s).normalize();
        Vec3 end = from.add(dir.scale(120));
        BlockHitResult block = level().clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level(), this, from, end, new AABB(from, end).inflate(1.0),
                e -> e instanceof LivingEntity && e != pilot && !e.isSpectator() && !hasPassenger(e), 0.3F);
        if (hit != null) {
            end = hit.getEntity().getBoundingBox().clip(from, end).orElse(hit.getEntity().getBoundingBox().getCenter());
            hit.getEntity().invulnerableTime = 0;
            hit.getEntity().hurt(damageSources().playerAttack(pilot), 6.0F);
        }
        Vec3 path = end.subtract(from);
        int steps = (int) (path.length() * 2);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(path.scale(i / (double) Math.max(1, steps)));
            server.sendParticles(ModParticles.KINETIC_TRACER.get(), p.x, p.y, p.z, 0, 0, 0, 0, 0);
        }
        server.sendParticles(ModParticles.KINETIC_SPARK.get(), end.x, end.y, end.z, 4, 0.05, 0.05, 0.05, 0.2);
        level().playSound(null, from.x, from.y, from.z, ModSounds.SHIP_CANNON.get(), SoundSource.PLAYERS, 1.2F,
                0.95F + random.nextFloat() * 0.1F);
    }

    /** Objetivo para el misil: el ser vivo más centrado en la mira (prefiere enemigos), a menos de 80 bloques. */
    @Nullable
    public LivingEntity lockTarget(LivingEntity pilot) {
        Vec3 eye = pilot.getEyePosition(), look = pilot.getLookAngle();
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(120),
                        e -> e != pilot && e.isAlive() && !e.isSpectator() && !hasPassenger(e) && !(e instanceof Player p && p.isCreative()))
                .stream()
                .filter(e -> {
                    Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
                    return to.length() < 120 && to.normalize().dot(look) > Math.cos(Math.toRadians(14)) && pilot.hasLineOfSight(e);
                })
                .min(Comparator.comparingDouble(e -> (e instanceof Enemy ? 0 : 1000)
                        - e.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) * 100))
                .orElse(null);
    }

    private void launchMissile(ServerPlayer pilot) {
        LivingEntity target = lockTarget(pilot);
        ShipMissileEntity m = new ShipMissileEntity(level(), pilot, target);
        Vec3 from = muzzle(random.nextBoolean()).add(0, -0.4 * SCALE, 0);
        m.setPos(from.x, from.y, from.z);
        Vec3 dir = pilot.getLookAngle();
        m.setDeltaMovement(getDeltaMovement().add(dir.scale(1.0)));
        level().addFreshEntity(m);
        entityData.set(MISSILE_CD, MISSILE_COOLDOWN);
        level().playSound(null, from.x, from.y, from.z, ModSounds.SHIP_MISSILE.get(), SoundSource.PLAYERS, 1.4F, 1.0F);
    }

    // ------------------------------------------------------------------ interpolación (no pilotada por este cliente)

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYRot = yRot;
        lerpXRot = xRot;
        lerpSteps = 6;
    }

    private void tickLerp() {
        if (lerpSteps > 0) {
            double x = getX() + (lerpX - getX()) / lerpSteps;
            double y = getY() + (lerpY - getY()) / lerpSteps;
            double z = getZ() + (lerpZ - getZ()) / lerpSteps;
            setYRot(getYRot() + (float) Mth.wrapDegrees(lerpYRot - getYRot()) / lerpSteps);
            setXRot(getXRot() + (float) (lerpXRot - getXRot()) / lerpSteps);
            lerpSteps--;
            setPos(x, y, z);
            setRot(getYRot(), getXRot());
        }
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (level().isClientSide) {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.bryanamaya.tecnocinetico.client.ClientShipHandler.startEngine(this));
        }
    }

    // ------------------------------------------------------------------ guardado

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setNetherDrive(tag.getBoolean("NetherDrive"));
        if (tag.contains("Health")) {
            setHealth(tag.getFloat("Health"));
        }
        if (tag.hasUUID("PendingPilot")) {
            pendingPilot = tag.getUUID("PendingPilot");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Health", health());
        tag.putBoolean("NetherDrive", netherDrive());
        if (pendingPilot != null) {
            tag.putUUID("PendingPilot", pendingPilot);
        }
    }
}
