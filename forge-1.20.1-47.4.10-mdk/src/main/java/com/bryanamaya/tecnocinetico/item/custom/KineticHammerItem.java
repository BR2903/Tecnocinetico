package com.bryanamaya.tecnocinetico.item.custom;

import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Martillo Cinético: herramienta de nivel hierro.
 * - Pica todo lo que pica un pico de hierro, pero más lento (es un martillo, no un pico).
 * - Golpe pesado: mucho daño y ataque lento.
 * - Golpeando la Prensa de Ensamblaje 3 veces se fabrican componentes.
 */
public class KineticHammerItem extends PickaxeItem {

    private static final float MINING_SPEED_FACTOR = 0.7F;

    public KineticHammerItem(Properties properties) {
        // Tier de hierro: 7 de daño total (1 + 2 + 4) y velocidad de ataque 1.0 (-3.0)
        super(Tiers.IRON, 4, -3.0F, properties);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float speed = super.getDestroySpeed(stack, state);
        return speed > 1.0F ? speed * MINING_SPEED_FACTOR : speed;
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.CINETRITE_INGOT.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tecnocinetico.kinetic_hammer").withStyle(ChatFormatting.GRAY));
    }
}
