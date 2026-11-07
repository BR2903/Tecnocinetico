package com.bryanamaya.tecnocinetico.item.custom;

import com.bryanamaya.tecnocinetico.client.PistolClientExtensions;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.skill.PlayerSkills;
import com.bryanamaya.tecnocinetico.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
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
import net.minecraft.world.entity.Mob;
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

    public static final int MAG_SIZE = 6;
    private static final int SHOT_COOLDOWN = 8;
    private static final double RANGE = 25.0D;
    private static final float DAMAGE = 8.0F;      // 4 corazones
    /** Margen de acierto alrededor de los objetivos. 0 = disparo recto (arma base). */
    private static final float AIM_ASSIST = 0.0F;

    // --- Valores con módulos instalados ---
    private static final int EXTENDED_MAG_SIZE = 10;
    private static final double INERCITA_RANGE = 35.0D;
    private static final float INERCITA_DAMAGE_BONUS = 3.0F;
    private static final float OPTIC_AIM_ASSIST = 0.35F;
    private static final float QUICK_RELOAD_FACTOR = 0.7F;

    // ------------------------------------------------------------------ estadísticas según los módulos

    public static int getMagSize(ItemStack stack) {
        return WeaponModule.has(stack, WeaponModule.EXTENDED_MAG) ? EXTENDED_MAG_SIZE : MAG_SIZE;
    }

    public static float getShotDamage(ItemStack stack) {
        return DAMAGE + (WeaponModule.has(stack, WeaponModule.INERCITA_BARREL) ? INERCITA_DAMAGE_BONUS : 0.0F);
    }

    public static double getRange(ItemStack stack) {
        return WeaponModule.has(stack, WeaponModule.INERCITA_BARREL) ? INERCITA_RANGE : RANGE;
    }

    public static float getAimAssist(ItemStack stack) {
        return WeaponModule.has(stack, WeaponModule.OPTIC) ? OPTIC_AIM_ASSIST : AIM_ASSIST;
    }

    public static int getReloadTicks(ItemStack stack, int weaponLevel) {
        int base = Skill.reloadTicks(weaponLevel);
        return WeaponModule.has(stack, WeaponModule.QUICK_RELOAD) ? Math.round(base * QUICK_RELOAD_FACTOR) : base;
    }
    private static final float BASE_PITCH_KICK = 2.5F;
    private static final float BASE_YAW_KICK = 1.2F;
    private static final int XP_PER_HIT = 3;
    private static final int XP_PER_KILL = 12;

    private static final String TAG_AMMO = "Ammo";
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
    }

    /**
     * Ajusta la munición al tamaño actual del cargador (p. ej. al quitar el Cargador Ampliado).
     * Devuelve las balas que sobran, para devolverlas al jugador como fragmentos.
     */
    public static int clampAmmoToMag(ItemStack stack) {
        int excess = getAmmo(stack) - getMagSize(stack);
        if (excess > 0) {
            setAmmo(stack, getMagSize(stack));
            return excess;
        }
        return 0;
    }

    public static boolean isReloading(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getInt(TAG_RELOAD_LEFT) > 0;
    }

    public static boolean canReload(ItemStack stack) {
        return !isReloading(stack) && getAmmo(stack) < getMagSize(stack);
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
    public static void tryFire(ServerPlayer player) {
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

        fire(level, player, InteractionHand.MAIN_HAND, pistol);
        if (!creative) {
            setAmmo(pistol, getAmmo(pistol) - 1);
            pistol.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.7F, 2.0F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS, 0.5F, 1.8F);
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

    private static void fire(Level level, Player player, InteractionHand hand, ItemStack pistol) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(getRange(pistol)));

        // El disparo se detiene en el primer bloque sólido
        BlockHitResult blockHit = level.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }

        AABB search = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0D);
        // Arma base: disparo recto, solo acierta si el rayo toca la caja real del objetivo.
        // Con la Mira de Puntería Cinética se añade un margen y la bala "busca" mobs cercanos al rayo.
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, eye, end, search,
                e -> !e.isSpectator() && e.isPickable(), getAimAssist(pistol));
        if (entityHit != null) {
            end = entityHit.getLocation();
            Entity target = entityHit.getEntity();
            // Sin esto, los disparos rápidos (cada 8 ticks) chocan con la invulnerabilidad de 10 ticks
            target.invulnerableTime = 0;
            boolean hurt = target.hurt(level.damageSources().playerAttack(player), getShotDamage(pistol));
            player.playNotifySound(SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.4F, 1.6F);

            // Práctica: solo cuenta disparar a criaturas (no a soportes de armadura ni a objetos)
            if (hurt && target instanceof Mob && player instanceof ServerPlayer sp) {
                int xp = XP_PER_HIT + (target.isAlive() ? 0 : XP_PER_KILL);
                PlayerSkills.addXp(sp, Skill.WEAPON_HANDLING, xp);
            }
        }

        if (level instanceof ServerLevel server) {
            spawnShotEffects(server, player, hand, look, end, entityHit == null ? blockHit : null);
        }
    }

    /**
     * Efectos del disparo de un Fragmento de Cinetrita:
     * fogonazo cian en la boca del cañón, haz de energía, onda expansiva en el impacto,
     * chispas, el fragmento haciéndose añicos y, si da en un bloque, polvo de ese bloque.
     */
    private static void spawnShotEffects(ServerLevel server, Player player, InteractionHand hand, Vec3 look,
                                         Vec3 end, @Nullable BlockHitResult blockHit) {
        // Boca del cañón: delante de los ojos, desplazada hacia la mano que sujeta el arma
        boolean rightHand = (hand == InteractionHand.MAIN_HAND) == (player.getMainArm() == HumanoidArm.RIGHT);
        Vec3 right = Vec3.directionFromRotation(0, player.getYRot() + 90.0F); // vale también mirando arriba/abajo
        Vec3 muzzle = player.getEyePosition()
                .add(look.scale(1.0D))
                .add(right.scale(rightHand ? 0.26D : -0.26D))
                .add(0, -0.08D, 0); // a la altura del cañón en primera persona

        // Fogonazo
        server.sendParticles(ModParticles.KINETIC_BURST.get(), muzzle.x, muzzle.y, muzzle.z, 0, 0.35, 0, 0, 1);
        server.sendParticles(ModParticles.KINETIC_SPARK.get(), muzzle.x, muzzle.y, muzzle.z, 4, 0.02, 0.02, 0.02, 0.08);

        // Haz de energía cinética
        Vec3 path = end.subtract(muzzle);
        int steps = Math.max(1, (int) (path.length() * 3));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = muzzle.add(path.scale(i / (double) steps));
            server.sendParticles(ModParticles.KINETIC_TRACER.get(), p.x, p.y, p.z, 0, 0, 0, 0, 0);
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

    /** Llamado en el servidor cuando el jugador pulsa la tecla de recarga. */
    public static void startReload(Player player, ItemStack pistol) {
        Level level = player.level();
        if (!canReload(pistol)) {
            return;
        }
        if (!player.getAbilities().instabuild && countAmmoItems(player) == 0) {
            player.displayClientMessage(Component.translatable("message.tecnocinetico.no_shards")
                    .withStyle(ChatFormatting.RED), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 0.9F);
            return;
        }
        int ticks = getReloadTicks(pistol, PlayerSkills.getLevel(player, Skill.WEAPON_HANDLING));
        CompoundTag tag = pistol.getOrCreateTag();
        tag.putInt(TAG_RELOAD_LEFT, ticks);
        tag.putInt(TAG_RELOAD_TOTAL, ticks);
        player.getCooldowns().addCooldown(pistol.getItem(), ticks);
        // Suelta el cargador vacío
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.PLAYERS, 0.6F, 1.6F);
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
                    SoundEvents.CROSSBOW_LOADING_MIDDLE, SoundSource.PLAYERS, 1.0F, 0.9F);
        } else if (left == Math.round(total * 0.28F)) {  // se tira de la corredera
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 1.0F, 1.3F);
        }
        if (left > 0) {
            tag.putInt(TAG_RELOAD_LEFT, left);
            return;
        }

        tag.remove(TAG_RELOAD_LEFT);
        tag.remove(TAG_RELOAD_TOTAL);
        int needed = getMagSize(stack) - getAmmo(stack);
        int loaded = player.getAbilities().instabuild ? needed : consumeAmmoItems(player, needed);
        setAmmo(stack, getAmmo(stack) + loaded);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.IRON_DOOR_CLOSE, SoundSource.PLAYERS, 0.4F, 2.0F);
    }

    private static int countAmmoItems(Player player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(ModItems.CINETRITE_SHARD.get())) {
                count += s.getCount();
            }
        }
        return count;
    }

    private static int consumeAmmoItems(Player player, int wanted) {
        int taken = 0;
        for (int i = 0; i < player.getInventory().getContainerSize() && taken < wanted; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(ModItems.CINETRITE_SHARD.get())) {
                int t = Math.min(s.getCount(), wanted - taken);
                s.shrink(t);
                taken += t;
            }
        }
        return taken;
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
        return Math.round(13.0F * getAmmo(stack) / getMagSize(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return isReloading(stack) ? 0xFFB13B : 0x46E3F5;
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
        tooltip.add(Component.translatable("tooltip.tecnocinetico.ammo", getAmmo(stack), getMagSize(stack))
                .withStyle(ChatFormatting.AQUA));
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
