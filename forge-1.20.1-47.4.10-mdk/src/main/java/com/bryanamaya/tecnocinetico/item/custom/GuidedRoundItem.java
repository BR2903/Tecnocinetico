package com.bryanamaya.tecnocinetico.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Munición Guiada: bala de cinetrita con circuito de guiado. La pistola solo la carga si tiene la Guía de
 * Enjambre instalada (sin ella no sirve); entonces busca al enemigo más alineado con la mira.
 */
public class GuidedRoundItem extends Item {

    public GuidedRoundItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tecnocinetico.guided_round").withStyle(ChatFormatting.GRAY));
    }
}
