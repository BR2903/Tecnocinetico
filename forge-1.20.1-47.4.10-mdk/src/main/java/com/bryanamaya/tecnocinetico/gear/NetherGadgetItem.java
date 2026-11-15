package com.bryanamaya.tecnocinetico.gear;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Gadget del Nether (Refrigerador Térmico, Filtro de Ceniza, Botas de Anclaje) con dos niveles, como el jetpack:
 * NBT {@code Level} 1 a 3. Mk III (con un Núcleo de Datos del Dragón modificado): triple batería, apantallado contra
 * el Pulso EMP y el filtro además filtra la corrupción de datos del End. El Mk II (con un Componente de Forja Abisal del Fundidor del Abismo, receta
 * {@code nether_gadget_upgrade}) tiene el doble de batería y un efecto más fuerte (ver nether/NetherHazards).
 */
public class NetherGadgetItem extends GadgetItem {

    public static final int MAX_LEVEL = 3;
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
        int lvl = level(stack);
        return lvl >= 3 ? Component.translatable("item.tecnocinetico.gadget_mk3", base)
                : lvl >= 2 ? Component.translatable("item.tecnocinetico.gadget_mk2", base) : base;
    }

    @Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.Level level,
                                java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (level(stack) >= 2) {
            tooltip.add(Component.translatable(getDescriptionId() + ".mk2").withStyle(net.minecraft.ChatFormatting.GOLD));
        }
        if (level(stack) >= 3) {
            tooltip.add(Component.translatable("tooltip.tecnocinetico.gadget_mk3").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
        }
    }
}
