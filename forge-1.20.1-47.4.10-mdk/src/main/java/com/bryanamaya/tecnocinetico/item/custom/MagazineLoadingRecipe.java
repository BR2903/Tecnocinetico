package com.bryanamaya.tecnocinetico.item.custom;

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

/**
 * Receta especial de la mesa: un cargador (vacío o a medias) + balas sueltas = el cargador con esas balas.
 * Vale cualquier cantidad (1..8) de Bala Cinética y/o Munición Guiada, siempre que quepan (si no, no hay
 * resultado: así nunca se pierden balas). Las guiadas se disparan siempre primero.
 */
public class MagazineLoadingRecipe extends CustomRecipe {

    public MagazineLoadingRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    private record Parts(ItemStack magazine, int normal, int guided) {}

    private static Parts parts(CraftingContainer inv) {
        ItemStack mag = ItemStack.EMPTY;
        int normal = 0, guided = 0;
        java.util.List<ItemStack> packs = new java.util.ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            if (s.getItem() instanceof MagazineItem) {
                if (!mag.isEmpty()) {
                    return null;                                     // solo un cargador
                }
                mag = s;
            } else if (s.getItem() instanceof AmmoPackItem) {
                packs.add(s);
            } else {
                return null;
            }
        }
        if (mag.isEmpty() || packs.isEmpty()) {
            return null;
        }
        MagazineItem m = (MagazineItem) mag.getItem();
        for (ItemStack s : packs) {                                          // cada paquete aporta las balas que le quedan
            if (s.is(m.ammo())) {
                normal += AmmoPackItem.left(s);
            } else if (s.is(ModItems.GUIDED_ROUND.get()) && m.acceptsGuided()) {
                guided += AmmoPackItem.left(s);
            } else {
                return null;                                                 // otro calibre
            }
        }
        if (MagazineItem.getRounds(mag) + normal + guided > m.capacity()) {
            return null;
        }
        return new Parts(mag, normal, guided);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        return parts(inv) != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess access) {
        Parts p = parts(inv);
        if (p == null) {
            return ItemStack.EMPTY;
        }
        ItemStack out = p.magazine().copyWithCount(1);
        int rounds = MagazineItem.getRounds(out) + p.normal() + p.guided();
        int guided = MagazineItem.getGuided(out) + p.guided();      // las guiadas se disparan siempre primero
        MagazineItem.setContents(out, rounds, guided);
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return w * h >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MAGAZINE_LOADING.get();
    }
}
