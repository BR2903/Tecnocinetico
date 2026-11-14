package com.bryanamaya.tecnocinetico.item.weapon;

import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Lanza Térmica (arma tier 3 del Nether, Ensamblador de Armas con un Núcleo de Forja). Manteniendo clic derecho
 * dispara un <b>rayo continuo de calor</b>: el Núcleo de Forja quema Plasma Ígneo (Combustible Cinético + piroxita +
 * azufre, en el Laboratorio Químico). El rayo llega a {@link #RANGE} bloques, lo paran los bloques y atraviesa hasta {@link #PIERCE} enemigos,
 * haciendo {@link #DAMAGE} cada {@link #HIT_EVERY} ticks y prendiéndolos.
 * <ul>
 *   <li><b>Combustible</b>: depósito de {@link #TANK}; 1 por tick disparando; cada Célula de Plasma Ígneo da
 *       {@link #FUEL_PER_CELL}.</li>
 *   <li><b>Calor</b>: sube disparando; a 100 se <b>sobrecalienta</b> y no dispara hasta enfriarse a {@link #COOLED}.</li>
 *   <li>Tecla R: reposta con las Células de Plasma Ígneo del inventario.</li>
 * </ul>
 * Todo se decide en el servidor; el rayo lo dibuja cada cliente ({@code client/ThermalBeamRenderer}) con el mismo cálculo.
 */
public class ThermalLanceItem extends Item {

    public static final int TANK = 2000, FUEL_PER_CELL = 500, HIT_EVERY = 4, PIERCE = 3;
    public static final double RANGE = 24.0D;
    public static final float DAMAGE = 3.6F, HEAT_PER_TICK = 0.8F, COOL_PER_TICK = 1.5F, COOLED = 20.0F;
    private static final String FUEL = "Fuel", HEAT = "Heat", OVERHEATED = "Overheated";

    public ThermalLanceItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant());
    }

    // ------------------------------------------------------------------ estado

    public static int fuel(ItemStack s) {
        return s.getTag() == null ? 0 : s.getTag().getInt(FUEL);
    }

    public static float heat(ItemStack s) {
        return s.getTag() == null ? 0 : s.getTag().getFloat(HEAT);
    }

    public static boolean overheated(ItemStack s) {
        return s.getTag() != null && s.getTag().getBoolean(OVERHEATED);
    }

    public static void setFuel(ItemStack s, int v) {
        s.getOrCreateTag().putInt(FUEL, Mth.clamp(v, 0, TANK));
    }

    private static void setHeat(ItemStack s, float v) {
        s.getOrCreateTag().putFloat(HEAT, Mth.clamp(v, 0, 100));
    }

    public static boolean canFire(ItemStack s) {
        return fuel(s) > 0 && !overheated(s);
    }

    /** Estado para el modelo: 0 apagada/sin combustible, 1 disparando, 2 sobrecalentada. */
    public static int state(ItemStack s, @Nullable LivingEntity holder) {
        if (overheated(s)) return 2;
        if (holder != null && holder.isUsingItem() && holder.getUseItem() == s && canFire(s)) return 1;
        return 0;
    }

    // ------------------------------------------------------------------ disparo

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!canFire(stack)) {
            if (!level.isClientSide) {
                String key = overheated(stack) ? "message.tecnocinetico.lance_overheated" : "message.tecnocinetico.lance_no_fuel";
                player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
                level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.4F, 1.6F);
            }
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        if (!level.isClientSide) {
            level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 0.6F);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level instanceof ServerLevel server && entity instanceof Player player) {
            if (!fireTick(server, player, stack, getUseDuration(stack) - remaining)) {
                player.releaseUsingItem();
            }
        }
    }

    /** Un tick de rayo (servidor). Devuelve false si ya no puede seguir (sin combustible, sin lente o sobrecalentada). */
    public static boolean fireTick(ServerLevel level, Player player, ItemStack stack, int tick) {
        if (!canFire(stack)) {
            return false;
        }
        boolean creative = player.getAbilities().instabuild;
        if (!creative) {
            setFuel(stack, fuel(stack) - 1);
        }
        setHeat(stack, heat(stack) + HEAT_PER_TICK);
        if (tick % HIT_EVERY == 0) {
            Beam beam = beam(level, player);
            for (Entity e : beam.hits()) {
                if (e instanceof LivingEntity l) {
                    l.invulnerableTime = 0;                                                // es un rayo: sin tiempo de invulnerabilidad
                    l.hurt(level.damageSources().playerAttack(player), DAMAGE);
                    l.setSecondsOnFire(4);
                }
            }
            Vec3 end = beam.end();
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, end.x, end.y, end.z, 2, 0.1, 0.1, 0.1, 0.01);
        }
        if (tick % 12 == 0) {
            level.playSound(null, player.blockPosition(), com.bryanamaya.tecnocinetico.sound.ModSounds.FLAMETHROWER.get(),
                    SoundSource.PLAYERS, 0.6F, 1.5F);
        }
        if (heat(stack) >= 100) {                                                         // sobrecalentada
            stack.getOrCreateTag().putBoolean(OVERHEATED, true);
            level.playSound(null, player.blockPosition(), SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 0.6F);
            player.displayClientMessage(Component.translatable("message.tecnocinetico.lance_overheated").withStyle(ChatFormatting.GOLD), true);
            return false;
        }
        return canFire(stack);
    }

    /** Recorrido del rayo: hasta el primer bloque (o {@link #RANGE}) y los enemigos que atraviesa (máx. {@link #PIERCE}). */
    public record Beam(Vec3 start, Vec3 end, List<Entity> hits) {}

    public static Beam beam(Level level, Player player) {
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        Vec3 far = eye.add(look.scale(RANGE));
        var block = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = block.getType() == HitResult.Type.MISS ? far : block.getLocation();
        List<Entity> hits = new ArrayList<>();
        AABB area = new AABB(eye, end).inflate(1.0);
        for (Entity e : level.getEntities(player, area, e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator())) {
            if (e.getBoundingBox().inflate(0.3).clip(eye, end).isPresent()) {
                hits.add(e);
            }
        }
        hits.sort(Comparator.comparingDouble(e -> e.distanceToSqr(eye)));
        return new Beam(eye, end, hits.size() > PIERCE ? hits.subList(0, PIERCE) : hits);
    }

    /** Se enfría cuando no dispara; al bajar de {@link #COOLED} vuelve a poder disparar. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || heat(stack) <= 0) {
            return;
        }
        if (entity instanceof LivingEntity l && l.isUsingItem() && l.getUseItem() == stack) {
            return;
        }
        setHeat(stack, heat(stack) - COOL_PER_TICK);
        if (overheated(stack) && heat(stack) <= COOLED) {
            stack.getOrCreateTag().remove(OVERHEATED);
            level.playSound(null, entity.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.3F);
        }
    }

    // ------------------------------------------------------------------ tecla R

    /** Reposta con las Células de Plasma Ígneo del inventario. */
    public static void service(ServerPlayer player, ItemStack stack) {
        var inv = player.getInventory();
        int cells = 0;
        if (player.getAbilities().instabuild) {
            setFuel(stack, TANK);
            player.level().playSound(null, player.blockPosition(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 0.8F, 1.2F);
            return;
        }
        for (int i = 0; i < inv.getContainerSize() && TANK - fuel(stack) >= FUEL_PER_CELL / 2; i++) {
            ItemStack s = inv.getItem(i);
            while (s.is(ModItems.PLASMA_CELL.get()) && !s.isEmpty() && TANK - fuel(stack) >= FUEL_PER_CELL / 2) {
                s.shrink(1);
                setFuel(stack, fuel(stack) + FUEL_PER_CELL);
                cells++;
            }
        }
        if (cells > 0) {
            player.level().playSound(null, player.blockPosition(), com.bryanamaya.tecnocinetico.sound.ModSounds.DRILL_REFUEL_END.get(),
                    SoundSource.PLAYERS, 0.8F, 1.3F);
            player.displayClientMessage(Component.translatable("message.tecnocinetico.lance_serviced", cells, fuel(stack), TANK)
                    .withStyle(ChatFormatting.GOLD), true);
        } else {
            player.displayClientMessage(Component.translatable("message.tecnocinetico.lance_nothing").withStyle(ChatFormatting.RED), true);
        }
    }

    // ------------------------------------------------------------------ presentación

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tecnocinetico.thermal_lance").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.tecnocinetico.lance_fuel", fuel(stack), TANK).withStyle(ChatFormatting.AQUA));
        if (heat(stack) > 0) {
            tooltip.add(Component.translatable(overheated(stack) ? "tooltip.tecnocinetico.lance_overheated" : "tooltip.tecnocinetico.lance_heat",
                    Math.round(heat(stack))).withStyle(ChatFormatting.RED));
        }
    }

    /** Barra: el calor (rojo) si está caliente; si no, el combustible (cian). */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return heat(stack) > 0 ? Math.round(13.0F * heat(stack) / 100) : Math.round(13.0F * fuel(stack) / TANK);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return overheated(stack) ? 0xFF3020 : heat(stack) > 0 ? 0xFF9A30 : 0x3FE0D4;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !newStack.is(oldStack.getItem());
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.model.HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return net.minecraft.client.model.HumanoidModel.ArmPose.CROSSBOW_HOLD;
            }
        });
    }
}
