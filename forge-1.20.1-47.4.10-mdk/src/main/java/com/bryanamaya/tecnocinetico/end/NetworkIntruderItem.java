package com.bryanamaya.tecnocinetico.end;

import com.bryanamaya.tecnocinetico.gear.GadgetItem;
import com.bryanamaya.tecnocinetico.gear.TacticalGear;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Intrusor de Red: gadget de muñeca (cualquiera de las dos). La lógica está en {@link NetworkHack}. */
public class NetworkIntruderItem extends GadgetItem {

    public NetworkIntruderItem(Properties props) {
        super(TacticalGear.WRIST, 2, "tooltip.tecnocinetico.network_intruder", props);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tecnocinetico.network_intruder", NetworkHack.DURATION / 20, NetworkHack.COOLDOWN / 20)
                .withStyle(ChatFormatting.GRAY));
        long left = level == null ? 0 : NetworkHack.cooldownLeft(stack, level.getGameTime());
        tooltip.add(left > 0 ? Component.translatable("tooltip.tecnocinetico.network_intruder.charging", (left + 19) / 20).withStyle(ChatFormatting.YELLOW)
                : Component.translatable("tooltip.tecnocinetico.network_intruder.ready").withStyle(ChatFormatting.GREEN));
    }
}
