package com.bryanamaya.tecnocinetico.gear;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Gadget del Nether (Refrigerador Térmico, Filtro de Ceniza, Botas de Anclaje) con dos niveles, como el jetpack:
 * NBT {@code Level} 1 o 2. El Mk II (con un Componente de Forja Abisal del Fundidor del Abismo, receta
 * {@code nether_gadget_upgrade}) tiene el doble de batería y un efecto más fuerte (ver nether/NetherHazards).
 */
public class NetherGadgetItem extends GadgetItem {

    public static final int MAX_LEVEL = 2;
    private final int battery;

    public NetherGadgetItem(int slot, int battery, String effectKey, Properties properties) {
        super(slot, battery, effectKey, properties);
        this.battery = battery;
    }

    public static int level(ItemStack s) {
        return s.getTag() == null ? 1 : Math.max(1, Math.min(MAX_LEVEL, s.getTag().getInt("Level")));
    }

    public static void setLevel(ItemStack s, int level) {
        s.getOrCreateTag().putInt("Level", Math.max(1, Math.min(MAX_LEVEL, level)));
    }

    /** Mk II: el doble de batería. */
    @Override
    public int getMaxDamage(ItemStack stack) {
        return battery * level(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component base = super.getName(stack);
        return level(stack) >= 2 ? Component.translatable("item.tecnocinetico.gadget_mk2", base) : base;
    }

    @Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.Level level,
                                java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (level(stack) >= 2) {
            tooltip.add(Component.translatable(getDescriptionId() + ".mk2").withStyle(net.minecraft.ChatFormatting.GOLD));
        }
    }
}
