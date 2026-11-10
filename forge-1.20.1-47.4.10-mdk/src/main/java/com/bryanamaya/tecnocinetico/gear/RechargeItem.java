package com.bryanamaya.tecnocinetico.gear;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/**
 * Recarga de un gadget concreto (Batería de Visor, Cartucho de Propelente, Célula de Propulsión).
 * Clic derecho: recarga del todo el gadget de ese tipo que lleves puesto en el Equipo Táctico.
 * En la mesa: gadget + recarga = gadget cargado (receta especial GadgetRechargeRecipe).
 */
public class RechargeItem extends Item {

    private final Supplier<Item> gadget;

    public RechargeItem(Supplier<Item> gadget, Properties properties) {
        super(properties);
        this.gadget = gadget;
    }

    public Item gadget() {
        return gadget.get();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(held);
        }
        var gear = TacticalGear.of(player);
        if (gear.isEmpty()) {
            return InteractionResultHolder.pass(held);
        }
        ItemStack target = ItemStack.EMPTY;
        for (int slot : new int[]{TacticalGear.FACE, TacticalGear.BACK, TacticalGear.FEET, TacticalGear.BELT}) {
            if (gear.get().get(slot).is(gadget.get())) {
                target = gear.get().get(slot);
            }
        }
        if (target.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.tecnocinetico.recharge_not_equipped",
                    gadget.get().getDescription()).withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(held);
        }
        if (target.getDamageValue() == 0) {
            player.displayClientMessage(Component.translatable("message.tecnocinetico.recharge_full"), true);
            return InteractionResultHolder.fail(held);
        }
        target.setDamageValue(0);
        gear.get().setChanged();
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 0.8F, 1.3F);
        player.displayClientMessage(Component.translatable("message.tecnocinetico.recharged", target.getHoverName()), true);
        return InteractionResultHolder.consume(held);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tecnocinetico.recharge", gadget.get().getDescription())
                .withStyle(ChatFormatting.GRAY));
    }
}
