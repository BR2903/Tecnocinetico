package com.bryanamaya.tecnocinetico.item.custom;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Taladro de Combustión (tier 3, herramienta, no arma). Funciona con gasolina: el Depósito de Combustible del
 * lanzallamas (tecla R con el taladro en la mano: llena el tanque gastando depósitos del inventario).
 * <ul>
 *   <li>Rompe en <b>3x3</b> sobre la cara que miras; <b>agachado</b>, un solo bloque.</li>
 *   <li>Pica todo lo de pico y pala con nivel de netherita: llega a la obsidiana, la obsidiana llorona y los
 *       escombros ancestrales. No rompe lo irrompible (lecho de roca, barreras).</li>
 *   <li>Cada bloque roto gasta {@link #FUEL_PER_BLOCK}. Sin combustible no sirve (pica como la mano y no suelta nada).</li>
 *   <li>Alrededor del bloque central solo rompe bloques que no sean mucho más duros que él (picando piedra no se
 *       lleva la obsidiana de al lado).</li>
 *   <li>No tiene durabilidad (el límite es el combustible) y no admite encantamientos.</li>
 * </ul>
 * Todo se decide en el servidor; los bloques extra se rompen con {@code gameMode.destroyBlock}, así respetan las
 * protecciones de otros mods y sueltan sus objetos y experiencia como un pico normal.
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID)
public class CombustionDrillItem extends Item {

    public static final int TANK = 300, FUEL_PER_CANISTER = 100, FUEL_PER_BLOCK = 1;
    /** Velocidad de picado con combustible (un pico de netherita es 9). Obsidiana: ~2 s; piedra: casi al instante. */
    public static final float SPEED = 32.0F;
    /** Un vecino del 3x3 solo se rompe si su dureza no pasa de (dureza central x4 + 1). */
    private static final float HARDNESS_FACTOR = 4.0F;
    private static final String TAG_FUEL = "Fuel";
    /** Tick de juego en que termina el repostaje en curso (no existe si no se está repostando). */
    private static final String TAG_REFUEL_END = "RefuelEnd";
    /** Lo que tarda en entrar la gasolina (1,5 s); mientras, el taladro no pica. */
    public static final int REFUEL_TICKS = 30;

    /** Jugadores rompiendo ahora mismo los bloques extra (evita que cada bloque extra vuelva a abrir otro 3x3). */
    private static final Set<UUID> BREAKING = new HashSet<>();

    public CombustionDrillItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant());
    }

    // ------------------------------------------------------------------ combustible

    public static int fuel(ItemStack stack) {
        return stack.getTag() == null ? 0 : stack.getTag().getInt(TAG_FUEL);
    }

    public static void setFuel(ItemStack stack, int fuel) {
        stack.getOrCreateTag().putInt(TAG_FUEL, Mth.clamp(fuel, 0, TANK));
    }

    public static boolean hasFuel(ItemStack stack) {
        return fuel(stack) >= FUEL_PER_BLOCK;
    }

    /** Progreso del repostaje (0..1) o -1 si no se está repostando. Lo usa la animación. */
    public static float refuelProgress(ItemStack stack, float gameTime) {
        if (stack.getTag() == null || !stack.getTag().contains(TAG_REFUEL_END)) {
            return -1;
        }
        float left = stack.getTag().getLong(TAG_REFUEL_END) - gameTime;
        return Mth.clamp(1.0F - left / REFUEL_TICKS, 0.0F, 1.0F);
    }

    public static boolean isRefueling(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().contains(TAG_REFUEL_END);
    }

    /**
     * Tecla R: empieza a repostar si hay sitio y un Depósito de Combustible (o en creativo). La gasolina entra al
     * terminar ({@link #REFUEL_TICKS}); cambiar de objeto lo cancela sin gastar nada.
     */
    public static void startRefuel(ServerPlayer player, ItemStack drill) {
        if (isRefueling(drill)) {
            return;
        }
        boolean room = TANK - fuel(drill) >= FUEL_PER_CANISTER / 2;
        if (!room || (!player.getAbilities().instabuild && canisterSlot(player) < 0)) {
            player.displayClientMessage(Component.translatable(!room ? "message.tecnocinetico.drill_full"
                    : "message.tecnocinetico.drill_no_canister").withStyle(ChatFormatting.RED), true);
            return;
        }
        drill.getOrCreateTag().putLong(TAG_REFUEL_END, player.level().getGameTime() + REFUEL_TICKS);
        player.level().playSound(null, player.blockPosition(), com.bryanamaya.tecnocinetico.sound.ModSounds.DRILL_REFUEL.get(),
                SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || !isRefueling(stack)) {
            return;
        }
        if (player.getMainHandItem() != stack) {
            stack.removeTagKey(TAG_REFUEL_END);                               // cambió de objeto: se cancela
        } else if (level.getGameTime() >= stack.getTag().getLong(TAG_REFUEL_END)) {
            stack.removeTagKey(TAG_REFUEL_END);
            refuel(player, stack);
        }
    }

    /**
     * Echa Depósitos de Combustible del inventario hasta llenar el tanque (usa uno si caben al menos la mitad de su
     * contenido; lo que sobra se pierde). En creativo llena gratis. Lo llama el final del repostaje.
     */
    public static void refuel(ServerPlayer player, ItemStack drill) {
        if (player.getAbilities().instabuild) {
            setFuel(drill, TANK);
            player.level().playSound(null, player.blockPosition(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 0.8F, 0.8F);
            return;
        }
        int used = 0;
        while (TANK - fuel(drill) >= FUEL_PER_CANISTER / 2) {
            int slot = canisterSlot(player);
            if (slot < 0) {
                break;
            }
            ItemStack can = player.getInventory().getItem(slot);
            int value = can.is(ModItems.KINETIC_FUEL_CELL.get()) ? FUEL_PER_CANISTER * 3 : FUEL_PER_CANISTER;   // cinético: el triple
            can.shrink(1);
            setFuel(drill, fuel(drill) + value);
            used++;
        }
        if (used > 0) {
            player.level().playSound(null, player.blockPosition(), com.bryanamaya.tecnocinetico.sound.ModSounds.DRILL_REFUEL_END.get(),
                    SoundSource.PLAYERS, 0.8F, 1.0F);
            player.displayClientMessage(Component.translatable("message.tecnocinetico.drill_refuel", used, fuel(drill), TANK)
                    .withStyle(ChatFormatting.GOLD), true);
        } else {
            player.displayClientMessage(Component.translatable(fuel(drill) >= TANK - FUEL_PER_CANISTER / 2
                    ? "message.tecnocinetico.drill_full" : "message.tecnocinetico.drill_no_canister").withStyle(ChatFormatting.RED), true);
        }
    }

    private static int canisterSlot(Player player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(ModItems.FUEL_CANISTER.get()) || inv.getItem(i).is(ModItems.KINETIC_FUEL_CELL.get())) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------ picado

    /** ¿Lo puede picar (y soltar) el taladro? Lo de pico o pala hasta nivel netherita; nunca lo irrompible. */
    public static boolean canDrill(BlockState state) {
        return (state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL))
                && TierSortingRegistry.isCorrectTierForDrops(Tiers.NETHERITE, state);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (isRefueling(stack)) {
            return 0.0F;                                                      // con el bidón puesto no se taladra
        }
        return hasFuel(stack) && canDrill(state) ? SPEED : 1.0F;
    }

    /** Sin golpe de brazo al picar o pegar: el taladro se sostiene firme (la animación está en DrillClientExtensions). */
    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return true;
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new com.bryanamaya.tecnocinetico.client.DrillClientExtensions());
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return hasFuel(stack) && canDrill(state);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0.0F
                && !(miner instanceof Player p && p.getAbilities().instabuild)) {
            boolean had = hasFuel(stack);
            setFuel(stack, fuel(stack) - FUEL_PER_BLOCK);                     // cada bloque roto (central o extra)
            if (had && !hasFuel(stack)) {                                     // se acabó: el motor se ahoga
                level.playSound(null, miner.getX(), miner.getY(), miner.getZ(),
                        com.bryanamaya.tecnocinetico.sound.ModSounds.DRILL_SPUTTER.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
            }
        }
        return true;
    }

    /** Al romper el bloque central: abre el 3x3 (salvo agachado) en el plano de la cara que se mira. */
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || BREAKING.contains(player.getUUID())) {
            return;
        }
        ItemStack drill = player.getMainHandItem();
        if (!(drill.getItem() instanceof CombustionDrillItem) || !hasFuel(drill) || !canDrill(event.getState())) {
            return;
        }
        var level = player.serverLevel();
        BlockPos center = event.getPos();
        level.playSound(null, center, com.bryanamaya.tecnocinetico.sound.ModSounds.DRILL_BITE.get(), SoundSource.BLOCKS, 0.9F,
                0.9F + level.random.nextFloat() * 0.2F);
        activity(player);
        if (player.isShiftKeyDown()) {
            return;                                                           // agachado: un solo bloque
        }
        float centerHardness = event.getState().getDestroySpeed(level, center);
        BREAKING.add(player.getUUID());
        try {
            for (BlockPos p : area(center, face(player, center))) {
                if (!hasFuel(drill) || p.equals(center)) {
                    continue;
                }
                BlockState s = level.getBlockState(p);
                float hardness = s.getDestroySpeed(level, p);
                if (s.isAir() || hardness < 0 || !canDrill(s) || hardness > centerHardness * HARDNESS_FACTOR + 1.0F) {
                    continue;
                }
                player.gameMode.destroyBlock(p);                              // protecciones, botín, experiencia, combustible
            }
        } finally {
            BREAKING.remove(player.getUUID());
        }
    }

    /** Avisa a los jugadores que lo ven de que está taladrando (para su sonido de motor). */
    public static void activity(ServerPlayer player) {
        com.bryanamaya.tecnocinetico.network.ModNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY.with(() -> player),
                new com.bryanamaya.tecnocinetico.network.ModNetwork.DrillActivityPacket(player.getId()));
    }

    /** Empieza a picar un bloque con el taladro: los demás oyen el motor acelerar. */
    @SubscribeEvent
    public static void onStartMining(net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() == net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START
                && event.getEntity() instanceof ServerPlayer player && player.getMainHandItem().getItem() instanceof CombustionDrillItem
                && hasFuel(player.getMainHandItem()) && !isRefueling(player.getMainHandItem())) {
            activity(player);
        }
    }

    /** Cara del bloque que mira el jugador (si la mirada no da en ese bloque, se deduce de la inclinación). */
    static Direction face(Player player, BlockPos center) {
        HitResult hit = player.pick(player.getBlockReach() + 1.0D, 0.0F, false);
        if (hit instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK && block.getBlockPos().equals(center)) {
            return block.getDirection();
        }
        float pitch = player.getXRot();
        return pitch > 45 ? Direction.UP : pitch < -45 ? Direction.DOWN : player.getDirection().getOpposite();
    }

    /** Los 9 bloques del plano perpendicular a {@code face} que pasa por {@code center}. */
    public static List<BlockPos> area(BlockPos center, Direction face) {
        List<BlockPos> out = new java.util.ArrayList<>(9);
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                out.add(switch (face.getAxis()) {
                    case X -> center.offset(0, a, b);
                    case Y -> center.offset(a, 0, b);
                    case Z -> center.offset(a, b, 0);
                });
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ presentación

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;                                                          // la barra es el tanque de gasolina
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
        return slotChanged || !newStack.is(oldStack.getItem());               // gastar combustible no "re-equipa"
    }

    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return !newStack.is(oldStack.getItem());                              // ni corta el picado al cambiar el NBT
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tecnocinetico.drill_fuel", fuel(stack), TANK).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.tecnocinetico.drill").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.tecnocinetico.drill_keys").withStyle(ChatFormatting.DARK_GRAY));
    }
}
