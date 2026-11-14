package com.bryanamaya.tecnocinetico.item.weapon;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lanza Térmica (arma tier 3 del Nether, Ensamblador de Armas con un Núcleo de Forja). Como las demás armas del mod:
 * <b>clic izquierdo (mantener) dispara</b> un rayo continuo de calor y <b>clic derecho apunta</b>. El Núcleo de Forja
 * quema Plasma Ígneo (Combustible Cinético + piroxita + azufre, en el Laboratorio Químico).
 * <ul>
 *   <li>Rayo de {@link #RANGE} bloques: lo paran los bloques, atraviesa hasta {@link #PIERCE} enemigos, {@link #DAMAGE}
 *       cada {@link #HIT_EVERY} ticks y los prende.</li>
 *   <li>Plasma: depósito de {@link #TANK}; 1 por tick disparando; cada Célula de Plasma Ígneo da {@link #FUEL_PER_CELL}.</li>
 *   <li>Calor: sube disparando; a 100 se <b>sobrecalienta</b> y no dispara hasta enfriarse a {@link #COOLED}.</li>
 *   <li>Tecla R: recarga en {@link #RELOAD_TICKS} ticks (animación) con las células del inventario.</li>
 * </ul>
 * El cliente avisa de que aprieta el gatillo ({@code ModNetwork.LanceFirePacket}); el servidor decide si dispara y
 * marca el arma ({@code Firing}) para que todos los clientes dibujen el rayo ({@code client/ThermalBeamRenderer}).
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID)
public class ThermalLanceItem extends Item {

    public static final int TANK = 2000, FUEL_PER_CELL = 500, HIT_EVERY = 4, PIERCE = 3, RELOAD_TICKS = 34;
    public static final double RANGE = 24.0D;
    public static final float DAMAGE = 3.6F, HEAT_PER_TICK = 0.8F, COOL_PER_TICK = 1.5F, COOLED = 20.0F;
    private static final String FUEL = "Fuel", HEAT = "Heat", OVERHEATED = "Overheated", FIRING = "Firing", RELOAD_END = "ReloadEnd";
    /** Jugadores con el gatillo apretado (servidor) y el último tick en que lo confirmaron. */
    private static final Map<UUID, Long> TRIGGER = new HashMap<>();
    /** Sin confirmación del cliente en este tiempo, se suelta el gatillo (por si se pierde el paquete de soltar). */
    private static final int TRIGGER_TIMEOUT = 15;

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

    /** El servidor lo marca mientras el rayo sale (lo ven todos los clientes). */
    public static boolean firing(ItemStack s) {
        return s.getTag() != null && s.getTag().getBoolean(FIRING);
    }

    public static boolean reloading(ItemStack s) {
        return s.getTag() != null && s.getTag().contains(RELOAD_END);
    }

    /** Progreso de la recarga (0..1) o -1 si no recarga. Lo usa la animación. */
    public static float reloadProgress(ItemStack s, float gameTime) {
        if (!reloading(s)) {
            return -1;
        }
        return Mth.clamp(1.0F - (s.getTag().getLong(RELOAD_END) - gameTime) / RELOAD_TICKS, 0.0F, 1.0F);
    }

    public static void setFuel(ItemStack s, int v) {
        s.getOrCreateTag().putInt(FUEL, Mth.clamp(v, 0, TANK));
    }

    private static void setHeat(ItemStack s, float v) {
        s.getOrCreateTag().putFloat(HEAT, Mth.clamp(v, 0, 100));
    }

    private static void setFiring(ItemStack s, boolean on) {
        if (firing(s) != on) {
            if (on) {
                s.getOrCreateTag().putBoolean(FIRING, true);
            } else {
                s.getOrCreateTag().remove(FIRING);
            }
        }
    }

    public static boolean canFire(ItemStack s) {
        return fuel(s) > 0 && !overheated(s) && !reloading(s);
    }

    /** Estado para el modelo: 0 apagada/sin plasma, 1 disparando, 2 sobrecalentada. */
    public static int state(ItemStack s, @Nullable LivingEntity holder) {
        if (overheated(s)) return 2;
        return firing(s) && canFire(s) ? 1 : 0;
    }

    // ------------------------------------------------------------------ gatillo (clic izquierdo, desde el cliente)

    /** El cliente aprieta (o mantiene) / suelta el gatillo. */
    public static void trigger(ServerPlayer player, boolean on) {
        if (on && player.getMainHandItem().getItem() instanceof ThermalLanceItem) {
            TRIGGER.put(player.getUUID(), player.level().getGameTime());
        } else {
            TRIGGER.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ThermalLanceItem)) {
            TRIGGER.remove(player.getUUID());
            return;
        }
        Long last = TRIGGER.get(player.getUUID());
        boolean held = last != null && player.level().getGameTime() - last <= TRIGGER_TIMEOUT;
        if (!held) {
            TRIGGER.remove(player.getUUID());
        }
        if (held && canFire(stack)) {
            if (!firing(stack)) {
                player.level().playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 0.6F);
            }
            setFiring(stack, true);
            if (!fireTick(player.serverLevel(), player, stack, player.tickCount)) {
                setFiring(stack, false);
            }
        } else {
            if (held && firing(stack) == false && !canFire(stack) && player.tickCount % 20 == 0) {
                String key = overheated(stack) ? "message.tecnocinetico.lance_overheated" : reloading(stack) ? null : "message.tecnocinetico.lance_no_fuel";
                if (key != null) {
                    player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
                }
            }
            setFiring(stack, false);
        }
    }

    /** El clic derecho apunta (lo hace el cliente): el objeto no se "usa". */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    /** Un tick de rayo (servidor). Devuelve false si ya no puede seguir (sin plasma o sobrecalentada). */
    public static boolean fireTick(ServerLevel level, Player player, ItemStack stack, int tick) {
        if (!canFire(stack)) {
            return false;
        }
        if (!player.getAbilities().instabuild) {
            setFuel(stack, fuel(stack) - 1);
        }
        setHeat(stack, heat(stack) + HEAT_PER_TICK);
        if (tick % HIT_EVERY == 0) {
            for (Entity e : beam(level, player).hits()) {
                if (e instanceof LivingEntity l) {
                    l.invulnerableTime = 0;                                                // es un rayo: sin tiempo de invulnerabilidad
                    l.hurt(level.damageSources().playerAttack(player), DAMAGE);
                    l.setSecondsOnFire(4);
                }
            }
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

    /** Rayo con la mirada actual (servidor y partículas). */
    public static Beam beam(Level level, Player player) {
        return beam(level, player, player.getEyePosition(), player.getLookAngle());
    }

    /** Rayo con la mirada interpolada del fotograma (dibujo en el cliente). */
    public static Beam beam(Level level, Player player, float partial) {
        return beam(level, player, player.getEyePosition(partial), player.getViewVector(partial));
    }

    private static Beam beam(Level level, Player player, Vec3 eye, Vec3 look) {
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
        if (hits.size() >= PIERCE) {                                                       // el último que atraviesa corta el rayo
            hits = new ArrayList<>(hits.subList(0, PIERCE));
            Entity lastHit = hits.get(PIERCE - 1);
            end = lastHit.getBoundingBox().clip(eye, end).orElse(end);
        }
        return new Beam(eye, end, hits);
    }

    /** Se enfría cuando no dispara y termina la recarga. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) {
            return;
        }
        if (reloading(stack) && entity instanceof ServerPlayer player) {
            if (player.getMainHandItem() != stack) {
                stack.getOrCreateTag().remove(RELOAD_END);                                  // cambió de objeto: se cancela
            } else if (level.getGameTime() >= stack.getTag().getLong(RELOAD_END)) {
                stack.getOrCreateTag().remove(RELOAD_END);
                finishReload(player, stack);
            }
        }
        if (firing(stack) && !(entity instanceof Player p && p.getMainHandItem() == stack)) {
            setFiring(stack, false);                                                         // ya no está en la mano
        }
        if (heat(stack) <= 0 || firing(stack)) {
            return;
        }
        setHeat(stack, heat(stack) - COOL_PER_TICK);
        if (overheated(stack) && heat(stack) <= COOLED) {
            stack.getOrCreateTag().remove(OVERHEATED);
            level.playSound(null, entity.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.3F);
        }
    }

    // ------------------------------------------------------------------ tecla R

    private static boolean hasCell(Player player) {
        return player.getInventory().items.stream().anyMatch(s -> s.is(ModItems.PLASMA_CELL.get()));
    }

    /** Tecla R: empieza la recarga (animación) si cabe plasma y hay células (o en creativo). */
    public static void service(ServerPlayer player, ItemStack stack) {
        if (reloading(stack)) {
            return;
        }
        boolean room = TANK - fuel(stack) >= FUEL_PER_CELL / 2;
        if (!room || (!player.getAbilities().instabuild && !hasCell(player))) {
            player.displayClientMessage(Component.translatable(room ? "message.tecnocinetico.lance_nothing" : "message.tecnocinetico.lance_full")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        TRIGGER.remove(player.getUUID());
        setFiring(stack, false);
        stack.getOrCreateTag().putLong(RELOAD_END, player.level().getGameTime() + RELOAD_TICKS);
        player.level().playSound(null, player.blockPosition(), com.bryanamaya.tecnocinetico.sound.ModSounds.DRILL_REFUEL.get(),
                SoundSource.PLAYERS, 0.8F, 1.25F);
    }

    /** Fin de la recarga: mete las células que quepan. */
    public static void finishReload(ServerPlayer player, ItemStack stack) {
        if (player.getAbilities().instabuild) {
            setFuel(stack, TANK);
        }
        int cells = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && TANK - fuel(stack) >= FUEL_PER_CELL / 2 && !player.getAbilities().instabuild; i++) {
            ItemStack s = inv.getItem(i);
            while (s.is(ModItems.PLASMA_CELL.get()) && !s.isEmpty() && TANK - fuel(stack) >= FUEL_PER_CELL / 2) {
                s.shrink(1);
                setFuel(stack, fuel(stack) + FUEL_PER_CELL);
                cells++;
            }
        }
        player.level().playSound(null, player.blockPosition(), com.bryanamaya.tecnocinetico.sound.ModSounds.DRILL_REFUEL_END.get(),
                SoundSource.PLAYERS, 0.8F, 1.3F);
        player.displayClientMessage(Component.translatable("message.tecnocinetico.lance_serviced", cells, fuel(stack), TANK)
                .withStyle(ChatFormatting.GOLD), true);
    }

    // ------------------------------------------------------------------ presentación

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tecnocinetico.thermal_lance").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.tecnocinetico.lance_fuel", fuel(stack), TANK).withStyle(ChatFormatting.GOLD));
        if (heat(stack) > 0) {
            tooltip.add(Component.translatable(overheated(stack) ? "tooltip.tecnocinetico.lance_overheated" : "tooltip.tecnocinetico.lance_heat",
                    Math.round(heat(stack))).withStyle(ChatFormatting.RED));
        }
    }

    /** Barra: el calor (rojo/naranja) si está caliente; si no, el plasma (naranja claro). */
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
        return overheated(stack) ? 0xFF3020 : heat(stack) > 0 ? 0xFF9A30 : 0xFFC860;
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return true;                                                                         // sin golpe de brazo
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !newStack.is(oldStack.getItem());
    }

    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return !newStack.is(oldStack.getItem());
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
        consumer.accept(new com.bryanamaya.tecnocinetico.client.LanceClientExtensions());
    }
}
