package com.bryanamaya.tecnocinetico.item.weapon;

import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Machete de Chatarra (tier 1, cuerpo a cuerpo): hoja de chapa afilada con mango forrado. Daño como una espada de
 * hierro (6) y algo más rápido; corta hojas, enredaderas y plantas deprisa. Se repara con Chatarra.
 */
public class MacheteItem extends SwordItem {

    public static final Tier SCRAP = new Tier() {
        @Override
        public int getUses() {
            return 320;
        }

        @Override
        public float getSpeed() {
            return 6.0F;
        }

        @Override
        public float getAttackDamageBonus() {
            return 2.0F;
        }

        @Override
        public int getLevel() {
            return 2;
        }

        @Override
        public int getEnchantmentValue() {
            return 12;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.SCRAP_METAL.get());
        }
    };

    public MacheteItem(Properties properties) {
        super(SCRAP, 3, -2.2F, properties);                                  // 1 + 2 + 3 = 6 de daño, 1.8 golpes/s
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (state.is(BlockTags.LEAVES) || state.is(Blocks.VINE) || state.is(Blocks.BAMBOO) || state.is(BlockTags.REPLACEABLE)) {
            return 12.0F;                                                    // desbrozar
        }
        return super.getDestroySpeed(stack, state);
    }
}
