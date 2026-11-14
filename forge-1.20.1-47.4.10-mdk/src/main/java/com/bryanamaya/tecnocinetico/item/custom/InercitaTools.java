package com.bryanamaya.tecnocinetico.item.custom;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Herramientas de Inercita (nivel 2 del mod, entre el diamante y la netherita). La inercita absorbe las vibraciones:
 * <ul>
 *   <li>Pico, hacha, pala y azada pican <b>a velocidad completa bajo el agua y en el aire</b> (sin las penalizaciones de
 *       vanilla de x5 cada una).</li>
 *   <li>La espada <b>aturde</b>: cada golpe deja al objetivo lento y débil un momento (la vibración no vuelve a la mano:
 *       va entera al objetivo).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID)
public final class InercitaTools {

    /** Nivel 3 (como el diamante), más duradera y rápida; se repara con lingotes de inercita. */
    public static final Tier TIER = TierSortingRegistry.registerTier(
            new ForgeTier(3, 2200, 9.0F, 3.5F, 14, BlockTags.NEEDS_DIAMOND_TOOL, () -> Ingredient.of(ModItems.INERCITA_INGOT.get())),
            ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "inercita"), List.of(Tiers.DIAMOND), List.of(Tiers.NETHERITE));
    public static final int STUN_TICKS = 30;

    private InercitaTools() {}

    public static boolean isInercitaTool(ItemStack s) {
        return s.getItem() instanceof TieredItem t && t.getTier() == TIER;
    }

    /** Sin penalización bajo el agua ni en el aire (vanilla divide entre 5 cada una). */
    @SubscribeEvent
    public static void breakSpeed(PlayerEvent.BreakSpeed event) {
        Player p = event.getEntity();
        if (!isInercitaTool(p.getMainHandItem())) {
            return;
        }
        float speed = event.getNewSpeed();
        if (p.isEyeInFluid(net.minecraft.tags.FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(p)) {
            speed *= 5.0F;
        }
        if (!p.onGround()) {
            speed *= 5.0F;
        }
        event.setNewSpeed(speed);
    }

    /** La espada aturde (lentitud II y debilidad) al golpear. */
    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (event.getSource().getDirectEntity() instanceof Player p && p.getMainHandItem().is(ModItems.INERCITA_SWORD.get())) {
            LivingEntity t = event.getEntity();
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, STUN_TICKS, 1));
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, STUN_TICKS, 0));
        }
    }
}
