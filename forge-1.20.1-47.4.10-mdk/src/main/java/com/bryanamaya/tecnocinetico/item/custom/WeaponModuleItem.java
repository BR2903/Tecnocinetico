package com.bryanamaya.tecnocinetico.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Objeto de módulo de mejora. Se instala combinándolo con la pistola en la mesa de crafteo. */
public class WeaponModuleItem extends Item {

    private final WeaponModule module;

    public WeaponModuleItem(WeaponModule module, Properties properties) {
        super(properties);
        this.module = module;
    }

    public WeaponModule getModule() {
        return module;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(module.effectText().copy().withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.tecnocinetico.module_install").withStyle(ChatFormatting.DARK_GRAY));
    }
}
