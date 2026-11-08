package com.bryanamaya.tecnocinetico.item.weapon;

import com.bryanamaya.tecnocinetico.gear.TacticalGear;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
import com.bryanamaya.tecnocinetico.skill.PlayerSkills;
import com.bryanamaya.tecnocinetico.skill.Skill;
import com.bryanamaya.tecnocinetico.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/**
 * Arma larga de carga cartucho a cartucho (Escopeta de caño, Rifle de cerrojo). Comparte con la pistola la
 * entrada (clic izquierdo dispara, derecho apunta, R recarga) y el estado de animación del cliente.
 *
 * NBT: "Ammo" (cargados), "Loading" (recargando) y "LoadTimer" (ticks hasta el siguiente cartucho).
 * Recargar mete los cartuchos de uno en uno (de las ranuras de munición del Equipo Táctico y del inventario);
 * disparar durante la recarga la interrumpe. Cada disparo lanza `pellets` rayos con dispersión `spread`.
 */
public class LongGunItem extends Item {

    /** Estadísticas del arma (todo lo que cambia entre escopeta y rifle). */
    public record Stats(int capacity, Supplier<Item> ammo, int pellets, float damage, double range, float spreadDeg,
                        float aimSpreadDeg, int shotCooldown, int loadTicks, float pitchKick, float yawKick, float aimZoom,
                        float firePitch) {}

    private static final String TAG_AMMO = "Ammo", TAG_LOADING = "Loading", TAG_TIMER = "LoadTimer";
    private static final int XP_PER_HIT = 1, XP_PER_KILL = 4;

    private final Stats stats;

    public LongGunItem(Stats stats, Properties properties) {
        super(properties.stacksTo(1).durability(stats.capacity() == 2 ? 500 : 700));
        this.stats = stats;
    }

    public Stats stats() {
        return stats;
    }

    // ------------------------------------------------------------------ estado

    public static int getAmmo(ItemStack s) {
        return s.getTag() == null ? 0 : s.getTag().getInt(TAG_AMMO);
    }

    private static void setAmmo(ItemStack s, int n) {
        s.getOrCreateTag().putInt(TAG_AMMO, Math.max(0, n));
    }

    public static boolean isLoading(ItemStack s) {
        return s.getTag() != null && s.getTag().getBoolean(TAG_LOADING);
    }

    /** Progreso de meter el cartucho actual (0..1) mientras recarga; 0 si no recarga. Lo usa la animación. */
    public static float loadProgress(ItemStack s, float partialTick) {
        if (!isLoading(s) || !(s.getItem() instanceof LongGunItem g)) {
            return 0.0F;
        }
        float left = s.getTag().getInt(TAG_TIMER) - partialTick;
        return Mth.clamp(1.0F - left / g.stats.loadTicks(), 0.0F, 1.0F);
    }

    public boolean canFireNow(Player player, ItemStack s) {
        return !player.getCooldowns().isOnCooldown(this) && (player.getAbilities().instabuild || getAmmo(s) > 0);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.fail(player.getItemInHand(hand));     // clic derecho = apuntar (cliente)
    }

    @Override
    public boolean canAttackBlock(net.minecraft.world.level.block.state.BlockState state, Level level,
                                  net.minecraft.core.BlockPos pos, Player player) {
        return false;
    }

    // ------------------------------------------------------------------ disparo (servidor)

    public void tryFire(ServerPlayer player, boolean aiming) {
        ItemStack gun = player.getMainHandItem();
        if (gun.getItem() != this || player.getCooldowns().isOnCooldown(this)) {
            return;
        }
        boolean creative = player.getAbilities().instabuild;
        if (isLoading(gun)) {
            stopLoading(player, gun);                                         // disparar interrumpe la recarga
        }
        if (!creative && getAmmo(gun) <= 0) {
            startReload(player, gun);
            return;
        }
        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        float spread = Mth.lerp(aiming ? 1.0F : 0.0F, stats.spreadDeg(), stats.aimSpreadDeg());
        for (int i = 0; i < stats.pellets(); i++) {
            Vec3 dir = spread(look, spread, player.getRandom());
            firePellet(level, player, gun, eye, dir, aiming, i == 0);
        }
        if (!creative) {
            setAmmo(gun, getAmmo(gun) - 1);
            gun.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PISTOL_FIRE.get(), SoundSource.PLAYERS,
                1.4F, stats.firePitch() + level.random.nextFloat() * 0.06F);
        player.getCooldowns().addCooldown(this, stats.shotCooldown());
        if (stats.capacity() > 2) {                                           // rifle: accionar el cerrojo
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PISTOL_SLIDE.get(),
                    SoundSource.PLAYERS, 0.8F, 0.8F);
        }
    }

    private static Vec3 spread(Vec3 look, float deg, net.minecraft.util.RandomSource r) {
        if (deg <= 0) {
            return look;
        }
        double rad = Math.toRadians(deg);
        Vec3 up = Math.abs(look.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = look.cross(up).normalize();
        Vec3 up2 = right.cross(look).normalize();
        double a = r.nextDouble() * Math.PI * 2, m = Math.sqrt(r.nextDouble()) * Math.tan(rad);
        return look.add(right.scale(Math.cos(a) * m)).add(up2.scale(Math.sin(a) * m)).normalize();
    }

    private void firePellet(Level level, Player player, ItemStack gun, Vec3 eye, Vec3 dir, boolean aiming, boolean flash) {
        Vec3 end = eye.add(dir.scale(stats.range()));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        AABB search = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, end, search,
                e -> !e.isSpectator() && e.isPickable(), 0.0F);
        if (hit != null) {
            Entity t = hit.getEntity();
            end = t.getBoundingBox().clip(eye, end).orElse(t.getBoundingBox().getCenter());
            t.invulnerableTime = 0;
            boolean hurt = t.hurt(level.damageSources().playerAttack(player), stats.damage());
            if (flash) {
                player.playNotifySound(ModSounds.PISTOL_HIT.get(), SoundSource.PLAYERS, 0.5F, 1.0F);
            }
            if (hurt && t instanceof Enemy && player instanceof ServerPlayer sp) {
                PlayerSkills.addXp(sp, Skill.WEAPON_HANDLING, XP_PER_HIT + (t.isAlive() ? 0 : XP_PER_KILL));
            }
        }
        if (level instanceof ServerLevel server) {
            effects(server, player, dir, end, hit == null ? block : null, aiming, flash);
        }
    }

    private static void effects(ServerLevel server, Player player, Vec3 dir, Vec3 end, @Nullable BlockHitResult block,
                                boolean aiming, boolean flash) {
        boolean rightHand = player.getMainArm() == HumanoidArm.RIGHT;
        Vec3 right = Vec3.directionFromRotation(0, player.getYRot() + 90.0F);
        Vec3 muzzle = player.getEyePosition().add(dir.scale(1.4D)).add(right.scale(aiming ? 0 : rightHand ? 0.26D : -0.26D))
                .add(0, aiming ? -0.06D : -0.1D, 0);
        if (flash) {
            server.sendParticles(ModParticles.KINETIC_BURST.get(), muzzle.x, muzzle.y, muzzle.z, 0, 0.45, 0, 0, 1);
            server.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 4, 0.04, 0.04, 0.04, 0.02);
        }
        Vec3 path = end.subtract(muzzle);
        int steps = Math.max(1, (int) (path.length() * 2));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = muzzle.add(path.scale(i / (double) steps));
            server.sendParticles(ModParticles.KINETIC_TRACER.get(), p.x, p.y, p.z, 0, 0, 0, 0, 0);
        }
        server.sendParticles(ModParticles.KINETIC_SPARK.get(), end.x, end.y, end.z, 4, 0.04, 0.04, 0.04, 0.15);
        if (block != null && block.getType() == HitResult.Type.BLOCK) {
            BlockState st = server.getBlockState(block.getBlockPos());
            if (!st.isAir()) {
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), end.x, end.y, end.z, 5, 0.06, 0.06, 0.06, 0.1);
            }
        }
    }

    // ------------------------------------------------------------------ recarga cartucho a cartucho

    public void startReload(Player player, ItemStack gun) {
        if (isLoading(gun) || getAmmo(gun) >= stats.capacity()) {
            return;
        }
        if (!player.getAbilities().instabuild && count(player) == 0) {
            player.displayClientMessage(Component.translatable("message.tecnocinetico.no_ammo_for",
                    stats.ammo().get().getDescription()).withStyle(ChatFormatting.RED), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PISTOL_DRY_FIRE.get(),
                    SoundSource.PLAYERS, 0.8F, 0.9F);
            return;
        }
        CompoundTag tag = gun.getOrCreateTag();
        tag.putBoolean(TAG_LOADING, true);
        tag.putInt(TAG_TIMER, stats.loadTicks());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PISTOL_MAG_OUT.get(),
                SoundSource.PLAYERS, 0.7F, stats.capacity() == 2 ? 0.75F : 1.1F);   // abrir la escopeta / el cerrojo
    }

    private void stopLoading(Player player, ItemStack gun) {
        gun.getOrCreateTag().putBoolean(TAG_LOADING, false);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PISTOL_SLIDE.get(),
                SoundSource.PLAYERS, 0.8F, stats.capacity() == 2 ? 0.7F : 1.0F);    // cerrar
    }

    @Override
    public void inventoryTick(ItemStack gun, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player) || !isLoading(gun)) {
            return;
        }
        if (!selected) {
            stopLoading(player, gun);                                         // cambiar de objeto cancela
            return;
        }
        CompoundTag tag = gun.getTag();
        int t = tag.getInt(TAG_TIMER) - 1;
        if (t > 0) {
            tag.putInt(TAG_TIMER, t);
            return;
        }
        if (player.getAbilities().instabuild || take(player)) {
            setAmmo(gun, getAmmo(gun) + 1);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PISTOL_MAG_IN.get(),
                    SoundSource.PLAYERS, 0.6F, 1.4F);                         // cartucho dentro
        }
        if (getAmmo(gun) >= stats.capacity() || (!player.getAbilities().instabuild && count(player) == 0)) {
            stopLoading(player, gun);
        } else {
            tag.putInt(TAG_TIMER, stats.loadTicks());
        }
    }

    private int count(Player player) {
        int n = 0;
        for (ItemStack s : TacticalGear.ammoSources(player)) {
            if (s.is(stats.ammo().get())) {
                n += s.getCount();
            }
        }
        return n;
    }

    private boolean take(Player player) {
        for (ItemStack s : TacticalGear.ammoSources(player)) {
            if (s.is(stats.ammo().get())) {
                s.shrink(1);
                TacticalGear.of(player).ifPresent(TacticalGear::setChanged);
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ interfaz

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new com.bryanamaya.tecnocinetico.client.LongGunClientExtensions());
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getAmmo(stack) / stats.capacity());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return isLoading(stack) ? 0xFFB13B : 0x46E3F5;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !oldStack.is(newStack.getItem());
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(net.minecraft.world.item.Items.IRON_INGOT);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tecnocinetico.ammo", getAmmo(stack), stats.capacity()).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.tecnocinetico.uses_ammo", stats.ammo().get().getDescription())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.DARK_GRAY));
    }
}
