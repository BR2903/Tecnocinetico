package com.bryanamaya.tecnocinetico.item.custom;

import com.bryanamaya.tecnocinetico.client.PistolClientExtensions;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.skill.PlayerSkills;
import com.bryanamaya.tecnocinetico.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
import com.bryanamaya.tecnocinetico.sound.ModSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Pistola Cinética: arma hitscan con cargador.
 * - Clic derecho: dispara (gasta 1 bala del cargador).
 * - Tecla R (configurable): recarga usando Fragmentos de Cinetrita del inventario.
 * - La habilidad Manejo de Armas reduce el retroceso y acelera la recarga.
 */
public class KineticPistolItem extends Item {

    private static final int SHOT_COOLDOWN = 8;
    private static final double RANGE = 25.0D;
    private static final float DAMAGE = 8.0F;      // 4 corazones
    /** Margen de acierto alrededor de los objetivos. 0 = disparo recto (arma base). */
    private static final float AIM_ASSIST = 0.0F;

    // --- Valores con módulos instalados ---
    private static final double INERCITA_RANGE = 35.0D;
    private static final float INERCITA_DAMAGE_BONUS = 3.0F;
    /** Guía de Enjambre: margen de "búsqueda" alrededor del rayo (la bala se desvía hacia el mob). */
    private static final float SEEKER_AIM_ASSIST = 1.1F;
    private static final float QUICK_RELOAD_FACTOR = 0.7F;

    // ------------------------------------------------------------------ estadísticas según los módulos

    /** Capacidad del cargador puesto (0 = sin cargador). */
    public static int getMagSize(ItemStack stack) {
        return MagazineItem.capacityOfType(getMagType(stack));
    }

    /** ¿Admite este cargador? El Ampliado necesita el módulo Cargador Ampliado. */
    public static boolean acceptsMagazine(ItemStack pistol, int type) {
        return type == 1 || (type == 2 && WeaponModule.has(pistol, WeaponModule.EXTENDED_MAG));
    }

    public static float getShotDamage(ItemStack stack) {
        return DAMAGE + (WeaponModule.has(stack, WeaponModule.INERCITA_BARREL) ? INERCITA_DAMAGE_BONUS : 0.0F);
    }

    public static double getRange(ItemStack stack) {
        return WeaponModule.has(stack, WeaponModule.INERCITA_BARREL) ? INERCITA_RANGE : RANGE;
    }

    public static float getAimAssist(ItemStack stack) {
        return WeaponModule.has(stack, WeaponModule.SEEKER) ? SEEKER_AIM_ASSIST : AIM_ASSIST;
    }

    public static int getReloadTicks(ItemStack stack, int weaponLevel) {
        int base = Skill.reloadTicks(weaponLevel);
        return WeaponModule.has(stack, WeaponModule.QUICK_RELOAD) ? Math.round(base * QUICK_RELOAD_FACTOR) : base;
    }
    private static final float BASE_PITCH_KICK = 2.5F;
    private static final float BASE_YAW_KICK = 1.2F;
    // Progresión lenta: ~6 XP por mob hostil abatido (unas 750-800 bajas para el nivel 10)
    private static final int XP_PER_HIT = 1;
    private static final int XP_PER_KILL = 4;

    private static final String TAG_AMMO = "Ammo";
    /** Cargador puesto: 0 ninguno, 1 Cargador, 2 Cargador Ampliado. */
    private static final String TAG_MAG = "Mag";
    /** Cargador que entra al terminar la recarga (ya se sacó del inventario): tipo, balas y guiadas. */
    private static final String TAG_PENDING = "PendingMag";
    /** Cuántas de las balas cargadas son Munición Guiada (se disparan primero). */
    private static final String TAG_GUIDED = "GuidedAmmo";
    /** Munición Guiada: cono de búsqueda (grados) alrededor de la mira; solo persigue mobs hostiles. */
    private static final double GUIDED_CONE_DEG = 25.0D;
    private static final String TAG_RELOAD_LEFT = "ReloadTicks";   // ticks que faltan
    private static final String TAG_RELOAD_TOTAL = "ReloadTotal";  // duración total de esta recarga

    public KineticPistolItem(Properties properties) {
        super(properties);
    }

    // ------------------------------------------------------------------ estado (NBT)

    public static int getAmmo(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getInt(TAG_AMMO);
    }

    private static void setAmmo(ItemStack stack, int ammo) {
        stack.getOrCreateTag().putInt(TAG_AMMO, Mth.clamp(ammo, 0, getMagSize(stack)));
        if (getGuided(stack) > getAmmo(stack)) {
            setGuided(stack, getAmmo(stack));
        }
    }

    public static int getMagType(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return 0;
        }
        if (!tag.contains(TAG_MAG)) {
            // pistolas de antes de los cargadores: si tenían balas, se les supone el cargador que cabía
            int ammo = tag.getInt(TAG_AMMO);
            return ammo > 6 ? 2 : ammo > 0 ? 1 : 0;
        }
        return tag.getInt(TAG_MAG);
    }

    private static void setMagType(ItemStack stack, int type) {
        stack.getOrCreateTag().putInt(TAG_MAG, type);
    }

    /** Saca el cargador puesto como objeto (con sus balas) y deja la pistola vacía. EMPTY si no tenía. */
    public static ItemStack ejectMagazine(ItemStack pistol) {
        int type = getMagType(pistol);
        net.minecraft.world.item.Item item = MagazineItem.itemOfType(type);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        ItemStack mag = new ItemStack(item);
        MagazineItem.setContents(mag, getAmmo(pistol), getGuided(pistol));
        setMagType(pistol, 0);
        pistol.getOrCreateTag().putInt(TAG_AMMO, 0);
        setGuided(pistol, 0);
        return mag;
    }

    /**
     * Tras cambiar módulos en el banco: si el cargador puesto ya no es válido (Ampliado sin su módulo, o balas
     * guiadas sin la Guía de Enjambre), se saca entero. Devuelve el cargador para dárselo al jugador.
     */
    public static ItemStack ejectIfIncompatible(ItemStack pistol) {
        boolean badMag = getMagType(pistol) != 0 && !acceptsMagazine(pistol, getMagType(pistol));
        boolean badGuided = getGuided(pistol) > 0 && !WeaponModule.has(pistol, WeaponModule.SEEKER);
        return badMag || badGuided ? ejectMagazine(pistol) : ItemStack.EMPTY;
    }

    public static int getGuided(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getInt(TAG_GUIDED);
    }

    private static void setGuided(ItemStack stack, int n) {
        stack.getOrCreateTag().putInt(TAG_GUIDED, Math.max(0, n));
    }

    public static boolean isReloading(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getInt(TAG_RELOAD_LEFT) > 0;
    }

    public static boolean canReload(ItemStack stack) {
        return !isReloading(stack) && (getMagType(stack) == 0 || getAmmo(stack) < getMagSize(stack));
    }

    /** Progreso de la recarga de 0 a 1 (0 = no recarga). Lo usan la animación y los fotogramas del modelo. */
    public static float getReloadProgress(ItemStack stack, float partialTick) {
        CompoundTag tag = stack.getTag();
        if (tag == null || tag.getInt(TAG_RELOAD_LEFT) <= 0) {
            return 0.0F;
        }
        int total = Math.max(1, tag.getInt(TAG_RELOAD_TOTAL));
        float left = tag.getInt(TAG_RELOAD_LEFT) - partialTick;
        return Mth.clamp(1.0F - left / total, 0.01F, 1.0F);
    }

    // ------------------------------------------------------------------ disparo

    /**
     * Clic derecho = apuntar. Eso lo gestiona el cliente (PistolInputHandler) mientras se mantiene el botón;
     * aquí solo se evita cualquier otra acción. FAIL no reinicia la animación de la mano ni prueba la otra mano.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.fail(player.getItemInHand(hand));
    }

    /** ¿El cliente puede mostrar ya el disparo (retroceso)? Debe coincidir con lo que comprobará el servidor. */
    public static boolean canFireNow(Player player, ItemStack pistol) {
        return pistol.getItem() instanceof KineticPistolItem
                && !player.getCooldowns().isOnCooldown(pistol.getItem())
                && !isReloading(pistol)
                && (player.getAbilities().instabuild || getAmmo(pistol) > 0);
    }

    public static float shotCooldownTicks() {
        return SHOT_COOLDOWN;
    }

    /**
     * Clic izquierdo (llega por red desde el cliente): dispara con la pistola de la mano principal.
     * Si el cargador está vacío, recarga automáticamente.
     */
    public static void tryFire(ServerPlayer player, boolean aiming) {
        ItemStack pistol = player.getMainHandItem();
        if (!(pistol.getItem() instanceof KineticPistolItem item)
                || player.getCooldowns().isOnCooldown(item) || isReloading(pistol)) {
            return;
        }
        Level level = player.level();
        boolean creative = player.getAbilities().instabuild;

        if (!creative && getAmmo(pistol) <= 0) {
            startReload(player, pistol); // recarga automática (avisa si no hay fragmentos)
            return;
        }

        boolean guided = getGuided(pistol) > 0 && WeaponModule.has(pistol, WeaponModule.SEEKER);
        fire(level, player, InteractionHand.MAIN_HAND, pistol, aiming, guided);
        if (!creative) {
            if (getGuided(pistol) > 0) {
                setGuided(pistol, getGuided(pistol) - 1);   // las guiadas van arriba del cargador
            }
            setAmmo(pistol, getAmmo(pistol) - 1);
            pistol.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.PISTOL_FIRE.get(), SoundSource.PLAYERS, 1.0F, 0.95F + level.random.nextFloat() * 0.1F);
        player.getCooldowns().addCooldown(item, SHOT_COOLDOWN);
        player.awardStat(Stats.ITEM_USED.get(item));
    }

    /** Cuánto sube la cámara al disparar (el cliente lo aplica). */
    public static float pitchKick() {
        return BASE_PITCH_KICK;
    }

    public static float yawKick() {
        return BASE_YAW_KICK;
    }

    /** Zoom al apuntar: 1 = sin zoom. La mira de puntería amplía más. */
    public static float aimZoom(ItemStack stack) {
        return WeaponModule.has(stack, WeaponModule.OPTIC) ? 0.65F : 0.85F;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false; // con la pistola en la mano no se rompen bloques
    }

    /**
     * Punto real del impacto en el objetivo. OJO: {@code EntityHitResult#getLocation()} de esta versión de
     * {@code ProjectileUtil.getEntityHitResult} devuelve los PIES del mob, no el punto del rayo; usarlo torcía
     * el trazo hacia abajo al dar a mobs pequeños o cercanos.
     * - Disparo recto: donde el rayo entra en la caja del mob (la línea sigue siendo recta).
     * - Si solo acertó gracias a la Guía de Enjambre (el rayo pasa cerca pero no toca la caja): el punto de
     *   la caja más cercano al rayo, así se ve cómo la bala se desvía hacia el objetivo (solo con la mejora).
     */
    private static Vec3 impactPoint(Entity target, Vec3 eye, Vec3 end) {
        AABB box = target.getBoundingBox();
        java.util.Optional<Vec3> direct = box.clip(eye, end);
        if (direct.isPresent()) {
            return direct.get();
        }
        if (box.contains(eye)) {
            return eye;
        }
        Vec3 near = box.inflate(1.0D).clip(eye, end).orElse(box.getCenter());
        return new Vec3(Mth.clamp(near.x, box.minX, box.maxX), Mth.clamp(near.y, box.minY, box.maxY),
                Mth.clamp(near.z, box.minZ, box.maxZ));
    }

    /**
     * Munición Guiada: el mob HOSTIL más alineado con la mira dentro del cono, al alcance y con línea de visión
     * (nunca supervivientes, animales ni mascotas). Null si no hay ninguno: entonces la bala va recta.
     */
    @Nullable
    private static LivingEntity guidedTarget(Level level, Player player, Vec3 eye, Vec3 look, double range) {
        double minCos = Math.cos(Math.toRadians(GUIDED_CONE_DEG));
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                e -> e != player && e.isAlive() && e instanceof Enemy && !e.isSpectator())) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            if (dist > range || dist < 1e-3) {
                continue;
            }
            double cos = to.scale(1.0D / dist).dot(look);
            if (cos < minCos) {
                continue;
            }
            BlockHitResult wall = level.clip(new ClipContext(eye, e.getBoundingBox().getCenter(),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (wall.getType() != HitResult.Type.MISS) {
                continue;
            }
            double score = (1.0D - cos) * 60.0D + dist * 0.05D;       // prioriza lo que está en la mira
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    private static void fire(Level level, Player player, InteractionHand hand, ItemStack pistol, boolean aiming, boolean guided) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(getRange(pistol)));

        LivingEntity seek = guided ? guidedTarget(level, player, eye, look, getRange(pistol)) : null;
        if (seek != null) {
            Vec3 hit = seek.getBoundingBox().getCenter();
            hitTarget(level, player, pistol, seek);
            if (level instanceof ServerLevel server) {
                spawnShotEffects(server, player, hand, look, hit, null, aiming, true);
            }
            return;
        }

        // El disparo se detiene en el primer bloque sólido
        BlockHitResult blockHit = level.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }

        AABB search = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0D);
        // Arma base: disparo recto, solo acierta si el rayo toca la caja real del objetivo.
        // Con la Guía de Enjambre se añade un margen y la bala "busca" mobs cercanos al rayo.
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, eye, end, search,
                e -> !e.isSpectator() && e.isPickable(), getAimAssist(pistol));
        if (entityHit != null) {
            Entity target = entityHit.getEntity();
            end = impactPoint(target, eye, end);
            hitTarget(level, player, pistol, target);
        }

        if (level instanceof ServerLevel server) {
            spawnShotEffects(server, player, hand, look, end, entityHit == null ? blockHit : null, aiming, false);
        }
    }

    private static void hitTarget(Level level, Player player, ItemStack pistol, Entity target) {
        // Sin esto, los disparos rápidos (cada 8 ticks) chocan con la invulnerabilidad de 10 ticks
        target.invulnerableTime = 0;
        boolean hurt = target.hurt(level.damageSources().playerAttack(player), getShotDamage(pistol));
        player.playNotifySound(ModSounds.PISTOL_HIT.get(), SoundSource.PLAYERS, 0.5F, 1.0F);

        // Práctica: solo cuentan los mobs HOSTILES (no animales, soportes de armadura ni objetos)
        if (hurt && target instanceof Enemy && player instanceof ServerPlayer sp) {
            int xp = XP_PER_HIT + (target.isAlive() ? 0 : XP_PER_KILL);
            PlayerSkills.addXp(sp, Skill.WEAPON_HANDLING, xp);
        }
    }

    /**
     * Efectos del disparo de un Fragmento de Cinetrita:
     * fogonazo cian en la boca del cañón, haz de energía, onda expansiva en el impacto,
     * chispas, el fragmento haciéndose añicos y, si da en un bloque, polvo de ese bloque.
     */
    private static void spawnShotEffects(ServerLevel server, Player player, InteractionHand hand, Vec3 look,
                                         Vec3 end, @Nullable BlockHitResult blockHit, boolean aiming, boolean curved) {
        // Boca del cañón: delante de los ojos, desplazada hacia la mano que sujeta el arma
        boolean rightHand = (hand == InteractionHand.MAIN_HAND) == (player.getMainArm() == HumanoidArm.RIGHT);
        Vec3 right = Vec3.directionFromRotation(0, player.getYRot() + 90.0F); // vale también mirando arriba/abajo
        // Apuntando, el arma está centrada bajo la vista: el disparo sale del centro, no de la cadera
        double side = aiming ? 0.0D : (rightHand ? 0.26D : -0.26D);
        Vec3 muzzle = player.getEyePosition()
                .add(look.scale(1.0D))
                .add(right.scale(side))
                .add(0, aiming ? -0.06D : -0.08D, 0); // a la altura del cañón en primera persona

        // Fogonazo
        server.sendParticles(ModParticles.KINETIC_BURST.get(), muzzle.x, muzzle.y, muzzle.z, 0, 0.35, 0, 0, 1);
        server.sendParticles(ModParticles.KINETIC_SPARK.get(), muzzle.x, muzzle.y, muzzle.z, 4, 0.02, 0.02, 0.02, 0.08);

        // Haz de energía cinética: recto; la bala guiada sale hacia delante y gira hacia el objetivo (curva de Bézier)
        Vec3 path = end.subtract(muzzle);
        Vec3 control = muzzle.add(look.scale(path.length() * 0.55D));
        int steps = Math.max(1, (int) (path.length() * (curved ? 4 : 3)));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 p = curved
                    ? muzzle.scale((1 - t) * (1 - t)).add(control.scale(2 * (1 - t) * t)).add(end.scale(t * t))
                    : muzzle.add(path.scale(t));
            server.sendParticles(ModParticles.KINETIC_TRACER.get(), p.x, p.y, p.z, 0, 0, 0, 0, 0);
        }

        // Impacto: sonido del fragmento rompiéndose donde da la bala (solo si da en algo, no en el aire)
        boolean hitSomething = blockHit == null || blockHit.getType() == HitResult.Type.BLOCK;
        if (hitSomething) {
            server.playSound(null, end.x, end.y, end.z, ModSounds.PISTOL_IMPACT.get(), SoundSource.PLAYERS,
                    1.0F, 0.9F + server.random.nextFloat() * 0.2F);
        }
        // Impacto: onda expansiva + chispas + el fragmento de cinetrita haciéndose añicos
        server.sendParticles(ModParticles.KINETIC_BURST.get(), end.x, end.y, end.z, 0, 1.0, 0, 0, 1);
        server.sendParticles(ModParticles.KINETIC_SPARK.get(), end.x, end.y, end.z, 10, 0.05, 0.05, 0.05, 0.22);
        server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(ModItems.CINETRITE_SHARD.get())),
                end.x, end.y, end.z, 8, 0.05, 0.05, 0.05, 0.12);
        if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockState state = server.getBlockState(blockHit.getBlockPos());
            if (!state.isAir()) {
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                        end.x, end.y, end.z, 10, 0.08, 0.08, 0.08, 0.1);
            }
        }
    }

    // ------------------------------------------------------------------ recarga (tecla R)

    /**
     * Recarga (servidor, tecla R o gatillo con el cargador vacío): busca en el inventario el cargador válido con
     * más balas (y más que el puesto), lo saca ya y lo deja "en espera" dentro de la pistola; al terminar la
     * animación entra, y el cargador viejo vuelve al inventario con las balas que le queden.
     * Válido = Cargador, o Ampliado si la pistola tiene su módulo; con balas guiadas solo si tiene la Guía.
     */
    public static void startReload(Player player, ItemStack pistol) {
        Level level = player.level();
        if (!canReload(pistol)) {
            return;
        }
        boolean seeker = WeaponModule.has(pistol, WeaponModule.SEEKER);
        int bestSlot = -1, bestRounds = getMagType(pistol) == 0 ? 0 : getAmmo(pistol);
        boolean blockedByGuided = false;
        // primero las ranuras de munición del Equipo Táctico, luego el inventario
        java.util.List<ItemStack> sources = com.bryanamaya.tecnocinetico.gear.TacticalGear.ammoSources(player);
        for (int i = 0; i < sources.size(); i++) {
            ItemStack s = sources.get(i);
            if (!(s.getItem() instanceof MagazineItem m) || !acceptsMagazine(pistol, m.type())) {
                continue;
            }
            int rounds = MagazineItem.getRounds(s);
            if (MagazineItem.getGuided(s) > 0 && !seeker) {
                blockedByGuided |= rounds > 0;
                continue;
            }
            if (rounds > bestRounds || (rounds == bestRounds && rounds > 0 && bestSlot >= 0
                    && m.type() > ((MagazineItem) sources.get(bestSlot).getItem()).type())) {
                bestSlot = i;
                bestRounds = rounds;
            }
        }
        CompoundTag pending = new CompoundTag();
        if (bestSlot >= 0) {
            ItemStack chosen = sources.get(bestSlot);
            pending.putInt("Type", ((MagazineItem) chosen.getItem()).type());
            pending.putInt("Rounds", MagazineItem.getRounds(chosen));
            pending.putInt("Guided", MagazineItem.getGuided(chosen));
            chosen.shrink(1);
            com.bryanamaya.tecnocinetico.gear.TacticalGear.of(player).ifPresent(com.bryanamaya.tecnocinetico.gear.TacticalGear::setChanged);
        } else if (player.getAbilities().instabuild) {
            int type = acceptsMagazine(pistol, 2) ? 2 : 1;            // creativo: cargador lleno gratis
            pending.putInt("Type", type);
            pending.putInt("Rounds", MagazineItem.capacityOfType(type));
        } else {
            player.displayClientMessage(Component.translatable(blockedByGuided ? "message.tecnocinetico.guided_needs_module"
                    : "message.tecnocinetico.no_magazine").withStyle(ChatFormatting.RED), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.PISTOL_DRY_FIRE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            return;
        }
        int ticks = getReloadTicks(pistol, PlayerSkills.getLevel(player, Skill.WEAPON_HANDLING));
        CompoundTag tag = pistol.getOrCreateTag();
        tag.put(TAG_PENDING, pending);
        tag.putInt(TAG_RELOAD_LEFT, ticks);
        tag.putInt(TAG_RELOAD_TOTAL, ticks);
        player.getCooldowns().addCooldown(pistol.getItem(), ticks);
        // Suelta el cargador puesto
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.PISTOL_MAG_OUT.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide() || !(entity instanceof Player player) || !isReloading(stack)) {
            return;
        }
        CompoundTag tag = stack.getTag();
        int total = Math.max(1, tag.getInt(TAG_RELOAD_TOTAL));
        int left = tag.getInt(TAG_RELOAD_LEFT) - 1;

        // Sonidos sincronizados con los fotogramas de la animación
        if (left == Math.round(total * 0.5F)) {          // entra el cargador nuevo
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.PISTOL_MAG_IN.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        } else if (left == Math.round(total * 0.28F)) {  // se tira de la corredera
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.PISTOL_SLIDE.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        }
        if (left > 0) {
            tag.putInt(TAG_RELOAD_LEFT, left);
            return;
        }

        tag.remove(TAG_RELOAD_LEFT);
        tag.remove(TAG_RELOAD_TOTAL);
        CompoundTag pending = tag.getCompound(TAG_PENDING);
        tag.remove(TAG_PENDING);
        if (pending.getInt("Type") == 0) {
            return;
        }
        // el cargador viejo vuelve al inventario (aunque esté vacío: es reutilizable)
        ItemStack old = ejectMagazine(stack);
        if (!old.isEmpty()) {
            com.bryanamaya.tecnocinetico.gear.TacticalGear.giveBackAmmo(player, old);       // vuelve a una ranura de munición libre
        }
        setMagType(stack, pending.getInt("Type"));
        setAmmo(stack, pending.getInt("Rounds"));
        setGuided(stack, Math.min(pending.getInt("Guided"), getAmmo(stack)));
    }

    // ------------------------------------------------------------------ interfaz

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new PistolClientExtensions());
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true; // la barra muestra la munición
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return getMagSize(stack) == 0 ? 0 : Math.round(13.0F * getAmmo(stack) / getMagSize(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        if (isReloading(stack)) {
            return 0xFFB13B;
        }
        return getGuided(stack) > 0 && WeaponModule.has(stack, WeaponModule.SEEKER) ? 0xD86CFF : 0x46E3F5;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        // Sin esto, cada cambio de munición (NBT) hace que el arma "baje y suba" en la mano
        return slotChanged || !oldStack.is(newStack.getItem());
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.INERCITA_INGOT.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (getMagType(stack) == 0) {
            tooltip.add(Component.translatable("tooltip.tecnocinetico.no_magazine").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("tooltip.tecnocinetico.ammo", getAmmo(stack), getMagSize(stack))
                    .withStyle(ChatFormatting.AQUA));
        }
        if (getGuided(stack) > 0) {
            tooltip.add(Component.translatable(WeaponModule.has(stack, WeaponModule.SEEKER)
                    ? "tooltip.tecnocinetico.guided_loaded" : "tooltip.tecnocinetico.guided_inactive", getGuided(stack))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        // Módulos instalados
        for (WeaponModule module : WeaponModule.values()) {
            if (WeaponModule.has(stack, module)) {
                tooltip.add(Component.literal(" + ").append(module.displayName()).withStyle(ChatFormatting.GREEN));
            }
        }
        tooltip.add(Component.translatable("tooltip.tecnocinetico.durability",
                stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.tecnocinetico.pistol_help",
                Component.keybind("key.tecnocinetico.reload")).withStyle(ChatFormatting.DARK_GRAY));
        // tooltip de habilidad (en el cliente, con los datos sincronizados)
        if (level != null && level.isClientSide()) {
            addSkillTooltip(stack, tooltip);
        }
    }

    private static void addSkillTooltip(ItemStack stack, List<Component> tooltip) {
        Skill skill = Skill.WEAPON_HANDLING;
        int xp = com.bryanamaya.tecnocinetico.skill.ClientSkillData.getXp(skill);
        int lvl = skill.levelForXp(xp);
        if (lvl >= skill.maxLevel()) {
            tooltip.add(Component.translatable("tooltip.tecnocinetico.skill_max", skill.displayName(), lvl)
                    .withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("tooltip.tecnocinetico.skill_level", skill.displayName(), lvl,
                    xp - Skill.totalXpForLevel(lvl), Skill.totalXpForLevel(lvl + 1) - Skill.totalXpForLevel(lvl))
                    .withStyle(ChatFormatting.GOLD));
        }
        int recoilReduction = Math.round((1.0F - Skill.recoilMultiplier(lvl)) * 100);
        tooltip.add(Component.translatable("tooltip.tecnocinetico.weapon_bonus", recoilReduction,
                String.format("%.2f", getReloadTicks(stack, lvl) / 20.0F)).withStyle(ChatFormatting.DARK_AQUA));
    }
}
