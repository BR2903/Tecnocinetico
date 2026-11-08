package com.bryanamaya.tecnocinetico.gear;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Gadget del Equipo Táctico. Va en su ranura (cara, espalda o pies) y gasta BATERÍA (la durabilidad del
 * objeto) mientras funciona; sin batería deja de funcionar pero no se rompe. Se recarga con su objeto propio
 * (Batería de Visor, Cartucho de Propelente, Célula de Propulsión): clic derecho con él o en la mesa.
 */
public class GadgetItem extends Item {

    private final int slot;
    private final String effectKey;

    public GadgetItem(int slot, int battery, String effectKey, Properties properties) {
        super(properties.durability(battery));
        this.slot = slot;
        this.effectKey = effectKey;
    }

    /** Ranura del Equipo Táctico donde va (TacticalGear.FACE / BACK / FEET). */
    public int slot() {
        return slot;
    }

    public static boolean hasCharge(ItemStack s) {
        return !s.isEmpty() && s.getDamageValue() < s.getMaxDamage() - 1;
    }

    /** Gasta batería sin romper el objeto. */
    public static void drain(ItemStack s, int amount) {
        if (!s.isEmpty()) {
            s.setDamageValue(Math.min(s.getMaxDamage() - 1, s.getDamageValue() + amount));
        }
    }

    /** Ya no se repara en el yunque: cada gadget tiene su propio objeto de recarga (RechargeItem). */
    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return false;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x46E3F5;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(effectKey).withStyle(ChatFormatting.GRAY));
        int pct = Math.round(100.0F * (stack.getMaxDamage() - 1 - stack.getDamageValue()) / (stack.getMaxDamage() - 1));
        tooltip.add(Component.translatable("tooltip.tecnocinetico.battery", pct)
                .withStyle(pct > 0 ? ChatFormatting.AQUA : ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.tecnocinetico.gadget_slot." + TacticalGear.slotName(slot))
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
