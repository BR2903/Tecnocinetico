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
            Block b = level.getBlockState(q).getBlock();
            float k = b == block ? 6.0F : (b == ore || b == deep) ? 0.6F : 0.0F;
            if (k > 0) {
                dose += k / (1.0F + (float) Math.sqrt(q.distSqr(c)));
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
        if (dose < 0.5F) {
            return;
        }
        int amp = dose >= LEVEL_3 ? 2 : dose >= LEVEL_2 ? 1 : 0;
        p.addEffect(new MobEffectInstance(ModEffects.RADIATION.get(), 80, amp, false, true, true));
    }
}
