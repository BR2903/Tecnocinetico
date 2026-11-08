package com.bryanamaya.tecnocinetico.gear;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Jetpack Cinético con niveles Mk I-III (NBT "Level"). Cada nivel tiene más batería (más tiempo de vuelo)
 * y más empuje (sube más rápido y avanza algo más). Se mejora en la mesa con los Kits de Mejora.
 */
public class JetpackItem extends GadgetItem {

    public static final int MAX_LEVEL = 3;
    private static final int[] BATTERY = {0, 1200, 2000, 3200};       // gasto: 1 cada 2 ticks de empuje
    private static final double[] THRUST = {0, 0.09D, 0.115D, 0.14D};  // subida por tick
    private static final double[] MAX_RISE = {0, 0.36D, 0.46D, 0.56D}; // velocidad vertical máxima
    private static final double[] GLIDE = {0, 1.0D, 1.025D, 1.05D};    // empuje horizontal

    public JetpackItem(Properties properties) {
        super(TacticalGear.BACK, BATTERY[1], "tooltip.tecnocinetico.jetpack", properties);
    }

    public static int level(ItemStack s) {
        return s.getTag() == null ? 1 : Math.max(1, Math.min(MAX_LEVEL, s.getTag().getInt("Level")));
    }

    public static void setLevel(ItemStack s, int level) {
        s.getOrCreateTag().putInt("Level", level);
    }

    public static double thrust(ItemStack s) {
        return THRUST[level(s)];
    }

    public static double maxRise(ItemStack s) {
        return MAX_RISE[level(s)];
    }

    public static double glide(ItemStack s) {
        return GLIDE[level(s)];
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return BATTERY[level(stack)];
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.tecnocinetico.jetpack.mk", Component.translatable(getDescriptionId()),
                "I".repeat(level(stack)));
    }
}
