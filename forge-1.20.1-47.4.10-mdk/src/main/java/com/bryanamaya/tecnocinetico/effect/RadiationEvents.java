package com.bryanamaya.tecnocinetico.effect;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Exposición a la radiación del uranio (cada segundo, en el servidor):
 * - lo que lleva el jugador encima (inventario, armadura y mano secundaria): uranio en bruto, lingote, mena y
 *   bloque; las pepitas cuentan poco;
 * - bloques cercanos (radio 3): bloques de uranio mucho, menas poco (disminuye con la distancia).
 * Nivel I desde poca exposición, II a partir de 8 y III a partir de 32 (aprox. 8 lingotes / medio stack).
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID)
public final class RadiationEvents {

    public static final int RADIUS = 3;
    public static final float LEVEL_2 = 8.0F, LEVEL_3 = 32.0F;

    private RadiationEvents() {}

    /** Radiación de un objeto (por unidad). */
    public static float itemDose(ItemStack s) {
        if (s.isEmpty()) {
            return 0.0F;
        }
        Item i = s.getItem();
        if (i == ModItems.URANIUM_NUGGET.get()) {
            return 0.12F;
        }
        if (i == ModItems.RAW_URANIUM.get() || i == ModItems.URANIUM_INGOT.get()
                || i == ModBlocks.URANIUM_ORE.get().asItem() || i == ModBlocks.DEEPSLATE_URANIUM_ORE.get().asItem()) {
            return 1.0F;
        }
        if (i == ModBlocks.URANIUM_BLOCK.get().asItem()) {
            return 9.0F;
        }
        if (i == ModItems.URANIUM_FUEL_ROD.get()) {
            return 3.0F;
        }
        if (i == ModItems.DEPLETED_FUEL_ROD.get()) {
            return 5.0F;                                                       // productos de fisión: más radiactiva
        }
        return 0.0F;
    }

    public static float exposure(Player p) {
        float dose = 0.0F;
        for (ItemStack s : p.getInventory().items) {
            dose += itemDose(s) * s.getCount();
        }
        for (ItemStack s : p.getInventory().armor) {
            dose += itemDose(s) * s.getCount();
        }
        for (ItemStack s : p.getInventory().offhand) {
            dose += itemDose(s) * s.getCount();
        }
        Level level = p.level();
        BlockPos c = p.blockPosition();
        Block block = ModBlocks.URANIUM_BLOCK.get(), ore = ModBlocks.URANIUM_ORE.get(), deep = ModBlocks.DEEPSLATE_URANIUM_ORE.get();
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-RADIUS, -RADIUS, -RADIUS), c.offset(RADIUS, RADIUS + 1, RADIUS))) {
            var st = level.getBlockState(q);
            Block b = st.getBlock();
            float k = b == block ? 6.0F : (b == ore || b == deep) ? 0.6F : 0.0F;
            if (k > 0) {
                dose += k / (1.0F + (float) Math.sqrt(q.distSqr(c)));
            }
        }
        return dose + reactorDose(level, c);
    }

    /** Alcance de la radiación de un reactor encendido (desde su núcleo, que queda en el centro de la estructura 9x9). */
    public static final int REACTOR_RANGE = 12;

    /** Reactores de Fisión quemando combustible cerca (se buscan sus núcleos entre las entidades de bloque). */
    private static float reactorDose(Level level, BlockPos c) {
        float dose = 0.0F;
        for (int cx = (c.getX() - REACTOR_RANGE) >> 4; cx <= (c.getX() + REACTOR_RANGE) >> 4; cx++) {
            for (int cz = (c.getZ() - REACTOR_RANGE) >> 4; cz <= (c.getZ() + REACTOR_RANGE) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (var be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (be instanceof com.bryanamaya.tecnocinetico.machine.FissionReactorBlockEntity
                            && be.getBlockState().getValue(com.bryanamaya.tecnocinetico.machine.ElectricMachineBlock.POWERED)) {
                        double d = Math.sqrt(be.getBlockPos().distSqr(c));
                        if (d <= REACTOR_RANGE) {
                            dose += 12.0F / (1.0F + (float) d);              // ~2 pegado a la carcasa, ~1 a 10 bloques
                        }
                    }
                }
            }
        }
        return dose;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player p = event.player;
        if (event.phase != TickEvent.Phase.END || p.level().isClientSide || p.tickCount % 20 != 0 || p.isCreative() || p.isSpectator()) {
            return;
        }
        float dose = exposure(p);
        int suit = com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.hazmatPieces(p);
        dose = suit >= 4 ? 0.0F : dose * (1.0F - 0.2F * suit);                // traje antirradiación
        if (dose < 0.5F) {
            return;
        }
        int amp = dose >= LEVEL_3 ? 2 : dose >= LEVEL_2 ? 1 : 0;
        p.addEffect(new MobEffectInstance(ModEffects.RADIATION.get(), 80, amp, false, true, true));
    }
}
