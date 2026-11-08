package com.bryanamaya.tecnocinetico.gear;

import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.item.ModRecipes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Recetas especiales de los gadgets (conservan el NBT del gadget: nivel, batería). */
public final class GearRecipes {

    private GearRecipes() {}

    /** Busca exactamente dos objetos en la mesa: devuelve {a, b} o null. */
    private static ItemStack[] pair(CraftingContainer inv) {
        ItemStack a = ItemStack.EMPTY, b = ItemStack.EMPTY;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            if (a.isEmpty()) {
                a = s;
            } else if (b.isEmpty()) {
                b = s;
            } else {
                return null;
            }
        }
        return a.isEmpty() || b.isEmpty() ? null : new ItemStack[]{a, b};
    }

    /** Gadget + su objeto de recarga = gadget con la batería llena. */
    public static class Recharge extends CustomRecipe {
        public Recharge(ResourceLocation id, CraftingBookCategory cat) {
            super(id, cat);
        }

        private static ItemStack gadget(CraftingContainer inv) {
            ItemStack[] p = pair(inv);
            if (p == null) {
                return ItemStack.EMPTY;
            }
            for (int k = 0; k < 2; k++) {
                ItemStack g = p[k], r = p[1 - k];
                if (g.getItem() instanceof GadgetItem && r.getItem() instanceof RechargeItem ri && g.is(ri.gadget())
                        && g.getDamageValue() > 0) {
                    return g;
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public boolean matches(CraftingContainer inv, Level level) {
            return !gadget(inv).isEmpty();
        }

        @Override
        public ItemStack assemble(CraftingContainer inv, RegistryAccess access) {
            ItemStack out = gadget(inv).copyWithCount(1);
            out.setDamageValue(0);
            return out;
        }

        @Override
        public boolean canCraftInDimensions(int w, int h) {
            return w * h >= 2;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return ModRecipes.GADGET_RECHARGE.get();
        }
    }

    /** Jetpack Mk N + Kit de Mejora Mk N+1 = Jetpack Mk N+1 (conserva la batería que tenía). */
    public static class JetpackUpgrade extends CustomRecipe {
        public JetpackUpgrade(ResourceLocation id, CraftingBookCategory cat) {
            super(id, cat);
        }

        private static ItemStack jetpack(CraftingContainer inv) {
            ItemStack[] p = pair(inv);
            if (p == null) {
                return ItemStack.EMPTY;
            }
            for (int k = 0; k < 2; k++) {
                ItemStack j = p[k], kit = p[1 - k];
                if (j.is(ModItems.JETPACK.get())) {
                    int lvl = JetpackItem.level(j);
                    if ((lvl == 1 && kit.is(ModItems.JETPACK_KIT_2.get())) || (lvl == 2 && kit.is(ModItems.JETPACK_KIT_3.get()))) {
                        return j;
                    }
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public boolean matches(CraftingContainer inv, Level level) {
            return !jetpack(inv).isEmpty();
        }

        @Override
        public ItemStack assemble(CraftingContainer inv, RegistryAccess access) {
            ItemStack out = jetpack(inv).copyWithCount(1);
            JetpackItem.setLevel(out, JetpackItem.level(out) + 1);
            return out;
        }

        @Override
        public boolean canCraftInDimensions(int w, int h) {
            return w * h >= 2;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return ModRecipes.JETPACK_UPGRADE.get();
        }
    }
}
