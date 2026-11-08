package com.bryanamaya.tecnocinetico.item.custom;

import com.bryanamaya.tecnocinetico.entity.ModEntities;
import com.bryanamaya.tecnocinetico.entity.ship.CoreFighterEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/** Caza del Núcleo (objeto): clic derecho en el suelo para desplegar la nave. Sin receta por ahora. */
public class CoreFighterItem extends Item {

    public CoreFighterItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        CoreFighterEntity ship = ModEntities.CORE_FIGHTER.get().create(level);
        if (ship == null) {
            return InteractionResult.FAIL;
        }
        Vec3 at = ctx.getClickLocation();
        ship.moveTo(at.x, at.y + 0.1, at.z, ctx.getPlayer() != null ? ctx.getPlayer().getYRot() : 0.0F, 0.0F);
        if (!level.noCollision(ship, ship.getBoundingBox())) {
            return InteractionResult.FAIL;                                        // no cabe aquí
        }
        level.addFreshEntity(ship);
        if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) {
            ctx.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tecnocinetico.core_fighter").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.tecnocinetico.core_fighter_keys").withStyle(ChatFormatting.DARK_GRAY));
    }
}
