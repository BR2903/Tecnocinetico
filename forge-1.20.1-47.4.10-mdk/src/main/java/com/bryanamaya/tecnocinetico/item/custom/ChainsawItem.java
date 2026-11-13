package com.bryanamaya.tecnocinetico.item.custom;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Motosierra (tier 2, herramienta). Gasta gasolina como el Taladro de Combustión (tecla R: reposta con Depósitos de
 * Combustible o Células de Combustible Cinético del inventario). Corta madera muy rápido y, al cortar un tronco de un
 * <b>árbol natural</b> (troncos unidos con hojas naturales cerca), <b>tala el árbol entero</b> (hasta {@link #MAX_LOGS}
 * troncos); agachado corta un solo bloque. Las casas de troncos no se talan (sus troncos no tienen hojas naturales).
 * También sirve de arma cuerpo a cuerpo mientras tiene gasolina. Todo en el servidor; los troncos se rompen con
 * {@code gameMode.destroyBlock} (protecciones, botín y experiencia como un hacha).
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID)
public class ChainsawItem extends Item {

    public static final int TANK = 200, FUEL_PER_CANISTER = 100, MAX_LOGS = 256;
    public static final float SPEED = 20.0F;
    private static final String TAG_FUEL = "Fuel";
    /** Tick en que termina el repostaje en curso; mientras, no corta (animación de repostar). */
    private static final String TAG_REFUEL_END = "RefuelEnd";
    public static final int REFUEL_TICKS = 30;
    private static final UUID DAMAGE_ID = UUID.fromString("5f4e2a0c-7b1d-4c2e-9e61-3c8a1b7d2f10");
    private static final Set<UUID> FELLING = new HashSet<>();

    public ChainsawItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    // ------------------------------------------------------------------ combustible

    public static int fuel(ItemStack s) {
        return s.getTag() == null ? 0 : s.getTag().getInt(TAG_FUEL);
    }

    public static void setFuel(ItemStack s, int fuel) {
        s.getOrCreateTag().putInt(TAG_FUEL, Mth.clamp(fuel, 0, TANK));
    }

    public static boolean hasFuel(ItemStack s) {
        return fuel(s) > 0;
    }

    public static boolean isRefueling(ItemStack s) {
        return s.getTag() != null && s.getTag().contains(TAG_REFUEL_END);
    }

    /** Progreso del repostaje (0..1) o -1 si no reposta. Lo usa la animación. */
    public static float refuelProgress(ItemStack s, float gameTime) {
        if (!isRefueling(s)) {
            return -1;
        }
        return Mth.clamp(1.0F - (s.getTag().getLong(TAG_REFUEL_END) - gameTime) / REFUEL_TICKS, 0.0F, 1.0F);
    }

    /** Tecla R: empieza a repostar si cabe gasolina y hay depósitos o células en el inventario (o en creativo). */
    public static void startRefuel(ServerPlayer player, ItemStack saw) {
        if (isRefueling(saw)) {
            return;
        }
        boolean room = TANK - fuel(saw) >= FUEL_PER_CANISTER / 2;
        boolean any = player.getAbilities().instabuild || player.getInventory().items.stream()
                .anyMatch(s -> s.is(ModItems.FUEL_CANISTER.get()) || s.is(ModItems.KINETIC_FUEL_CELL.get()));
        if (!room || !any) {
            player.displayClientMessage(Component.translatable(!room ? "message.tecnocinetico.drill_full"
                    : "message.tecnocinetico.drill_no_canister").withStyle(ChatFormatting.RED), true);
            return;
        }
        saw.getOrCreateTag().putLong(TAG_REFUEL_END, player.level().getGameTime() + REFUEL_TICKS);
        player.level().playSound(null, player.blockPosition(), com.bryanamaya.tecnocinetico.sound.ModSounds.DRILL_REFUEL.get(),
                SoundSource.PLAYERS, 0.8F, 1.05F);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || !isRefueling(stack)) {
            return;
        }
        if (player.getMainHandItem() != stack) {
            stack.removeTagKey(TAG_REFUEL_END);                                       // cambió de objeto: se cancela
        } else if (level.getGameTime() >= stack.getTag().getLong(TAG_REFUEL_END)) {
            stack.removeTagKey(TAG_REFUEL_END);
            refuel(player, stack);
        }
    }

    /** Fin del repostaje: llena el tanque con lo que haya en el inventario (cinético = 3 depósitos). */
    public static void refuel(ServerPlayer player, ItemStack saw) {
        if (player.getAbilities().instabuild) {
            setFuel(saw, TANK);
            player.level().playSound(null, player.blockPosition(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 0.8F, 0.8F);
            return;
        }
        int used = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && TANK - fuel(saw) >= FUEL_PER_CANISTER / 2; i++) {
            ItemStack s = inv.getItem(i);
            int value = s.is(ModItems.FUEL_CANISTER.get()) ? FUEL_PER_CANISTER : s.is(ModItems.KINETIC_FUEL_CELL.get()) ? FUEL_PER_CANISTER * 3 : 0;
            while (value > 0 && !s.isEmpty() && TANK - fuel(saw) >= FUEL_PER_CANISTER / 2) {
                s.shrink(1);
                setFuel(saw, fuel(saw) + value);
                used++;
            }
        }
        if (used > 0) {
            player.level().playSound(null, player.blockPosition(), com.bryanamaya.tecnocinetico.sound.ModSounds.DRILL_REFUEL_END.get(),
                    SoundSource.PLAYERS, 0.8F, 1.1F);
            player.displayClientMessage(Component.translatable("message.tecnocinetico.drill_refuel", used, fuel(saw), TANK)
                    .withStyle(ChatFormatting.GOLD), true);
        } else {
            player.displayClientMessage(Component.translatable(fuel(saw) >= TANK - FUEL_PER_CANISTER / 2
                    ? "message.tecnocinetico.drill_full" : "message.tecnocinetico.drill_no_canister").withStyle(ChatFormatting.RED), true);
        }
    }

    // ------------------------------------------------------------------ corte

    public static boolean canCut(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_AXE);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (isRefueling(stack)) {
            return 0.0F;
        }
        return hasFuel(stack) && canCut(state) ? SPEED : 1.0F;
    }

    /** Sin golpe de brazo: se sostiene firme (la animación está en ChainsawClientExtensions). */
    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return true;
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new com.bryanamaya.tecnocinetico.client.ChainsawClientExtensions());
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return hasFuel(stack) && canCut(state);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0.0F && canCut(state)
                && !(miner instanceof Player p && p.getAbilities().instabuild)) {
            boolean had = hasFuel(stack);
            setFuel(stack, fuel(stack) - 1);
            if (had && !hasFuel(stack)) {
                level.playSound(null, miner.getX(), miner.getY(), miner.getZ(),
                        com.bryanamaya.tecnocinetico.sound.ModSounds.CHAINSAW_STOP.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
            }
        }
        return true;
    }

    /** Empieza a cortar: los demás oyen el motor acelerar. */
    @SubscribeEvent
    public static void onStartCutting(net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() == net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START
                && event.getEntity() instanceof ServerPlayer player && player.getMainHandItem().getItem() instanceof ChainsawItem
                && hasFuel(player.getMainHandItem())) {
            CombustionDrillItem.activity(player);
        }
    }

    /** Al cortar un tronco de un árbol natural: tala el resto del árbol. */
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || FELLING.contains(player.getUUID()) || player.isShiftKeyDown()) {
            return;
        }
        ItemStack saw = player.getMainHandItem();
        if (saw.getItem() instanceof ChainsawItem && hasFuel(saw) && canCut(event.getState())) {
            CombustionDrillItem.activity(player);                                     // los demás oyen el corte
        }
        if (!(saw.getItem() instanceof ChainsawItem) || !hasFuel(saw) || !event.getState().is(BlockTags.LOGS)) {
            return;
        }
        var level = player.serverLevel();
        List<BlockPos> logs = tree(level, event.getPos());
        if (logs.isEmpty()) {
            return;
        }
        level.playSound(null, event.getPos(), SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);   // el árbol cede
        FELLING.add(player.getUUID());
        try {
            for (BlockPos p : logs) {
                if (!hasFuel(saw) && !player.getAbilities().instabuild) {
                    break;
                }
                player.gameMode.destroyBlock(p);                                      // protecciones, botín, combustible
            }
        } finally {
            FELLING.remove(player.getUUID());
        }
    }

    /**
     * Troncos del árbol al que pertenece {@code start} (sin él), de abajo arriba; vacío si no parece un árbol natural
     * (ningún bloque de hojas natural tocando los troncos) o si es demasiado grande.
     */
    public static List<BlockPos> tree(Level level, BlockPos start) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> logs = new ArrayList<>();
        boolean leaves = false;
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos q = p.offset(dx, dy, dz);
                        if (q.getY() < start.getY() || !seen.add(q)) {                 // no baja de donde se corta
                            continue;
                        }
                        BlockState s = level.getBlockState(q);
                        if (s.is(BlockTags.LOGS)) {
                            if (logs.size() >= MAX_LOGS) {
                                return List.of();
                            }
                            logs.add(q);
                            queue.add(q);
                        } else if (s.getBlock() instanceof LeavesBlock && !s.getValue(LeavesBlock.PERSISTENT)) {
                            leaves = true;
                        }
                    }
                }
            }
        }
        if (!leaves) {
            return List.of();
        }
        logs.sort(java.util.Comparator.comparingInt(BlockPos::getY));
        return logs;
    }

    // ------------------------------------------------------------------ arma

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (hasFuel(stack) && !(attacker instanceof Player p && p.getAbilities().instabuild)) {
            setFuel(stack, fuel(stack) - 1);
        }
        return true;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND || !hasFuel(stack)) {
            return super.getAttributeModifiers(slot, stack);
        }
        return ImmutableMultimap.of(
                Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Motosierra", 8.0D, AttributeModifier.Operation.ADDITION),
                Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Motosierra", -2.4D, AttributeModifier.Operation.ADDITION));
    }

    // ------------------------------------------------------------------ presentación

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tecnocinetico.chainsaw").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.tecnocinetico.chainsaw_fuel", fuel(stack), TANK).withStyle(ChatFormatting.GOLD));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * fuel(stack) / TANK);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xE8892A;
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
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !newStack.is(oldStack.getItem());
    }

    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return !newStack.is(oldStack.getItem());
    }
}
