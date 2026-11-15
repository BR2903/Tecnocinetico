package com.bryanamaya.tecnocinetico.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Objeto sin uso directo con una descripción (botín de jefes: acopladores, núcleos...). */
public class LoreItem extends Item {

    private final String tooltip;
    private final boolean foil;

    public LoreItem(String tooltip, boolean foil, Properties properties) {
        super(properties);
        this.tooltip = tooltip;
        this.foil = foil;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(tooltip).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return foil;
    }
}
