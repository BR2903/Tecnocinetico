package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.item.custom.KineticPistolItem;
import com.bryanamaya.tecnocinetico.item.custom.MagazineItem;
import com.bryanamaya.tecnocinetico.item.custom.MagazineLoadingRecipe;
import com.bryanamaya.tecnocinetico.item.custom.WeaponModule;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Pruebas del sistema de munición: receta de carga, llenar con R y recarga por cambio de cargador. */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class AmmoGameTests {

    private static final AbstractContainerMenu DUMMY = new AbstractContainerMenu(null, -1) {
        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    };

    @GameTest(template = "empty")
    public static void magazineRecipe(GameTestHelper helper) {
        var recipe = new MagazineLoadingRecipe(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "test"), CraftingBookCategory.EQUIPMENT);
        var grid = new TransientCraftingContainer(DUMMY, 3, 3);
        grid.setItem(0, new ItemStack(ModItems.MAGAZINE.get()));
        for (int i = 1; i <= 3; i++) {
            grid.setItem(i, new ItemStack(ModItems.KINETIC_ROUND.get()));
        }
        grid.setItem(4, new ItemStack(ModItems.GUIDED_ROUND.get()));
        grid.setItem(5, new ItemStack(ModItems.GUIDED_ROUND.get()));
        helper.assertTrue(recipe.matches(grid, helper.getLevel()), "La receta debería aceptar cargador + 5 balas");
        ItemStack out = recipe.assemble(grid, helper.getLevel().registryAccess());
        helper.assertTrue(MagazineItem.getRounds(out) == 5 && MagazineItem.getGuided(out) == 2,
                "Esperaba 5 balas (2 guiadas), hay " + MagazineItem.getRounds(out) + " (" + MagazineItem.getGuided(out) + ")");
        // demasiadas balas para un cargador de 6: no debe haber resultado (no se pierden balas)
        for (int i = 6; i <= 8; i++) {
            grid.setItem(i, new ItemStack(ModItems.KINETIC_ROUND.get()));
        }
        helper.assertTrue(!recipe.matches(grid, helper.getLevel()), "Con 8 balas no caben en un cargador de 6");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fillAndReload(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        var inv = player.getInventory();
        inv.clearContent();
        inv.setItem(0, new ItemStack(ModItems.MAGAZINE.get()));
        inv.setItem(1, new ItemStack(ModItems.KINETIC_ROUND.get(), 4));
        // llenar con R: 4 balas entran, se gastan
        int put = MagazineItem.fillFromInventory(player, inv.getItem(0), false);
        helper.assertTrue(put == 4 && MagazineItem.getRounds(inv.getItem(0)) == 4 && inv.getItem(1).isEmpty(),
                "Llenar con R: esperaba 4 balas dentro, metió " + put);

        // recargar una pistola sin cargador: entra el de 4, y el inventario se queda sin cargadores
        ItemStack pistol = new ItemStack(ModItems.KINETIC_PISTOL.get());
        KineticPistolItem.startReload(player, pistol);
        helper.assertTrue(KineticPistolItem.isReloading(pistol), "La recarga no empezó");
        for (int i = 0; i < 200 && KineticPistolItem.isReloading(pistol); i++) {
            pistol.inventoryTick(helper.getLevel(), player, 0, true);
        }
        helper.assertTrue(KineticPistolItem.getAmmo(pistol) == 4 && KineticPistolItem.getMagType(pistol) == 1,
                "Tras recargar esperaba 4 balas en un Cargador, hay " + KineticPistolItem.getAmmo(pistol));
        helper.assertTrue(inv.getItem(0).isEmpty(), "El cargador debería haber salido del inventario");

        // segundo cargador lleno: al recargar, el viejo (4 balas) vuelve al inventario
        ItemStack full = new ItemStack(ModItems.MAGAZINE.get());
        MagazineItem.setContents(full, 6, 0);
        inv.setItem(5, full);
        KineticPistolItem.startReload(player, pistol);
        for (int i = 0; i < 200 && KineticPistolItem.isReloading(pistol); i++) {
            pistol.inventoryTick(helper.getLevel(), player, 0, true);
        }
        int returned = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() instanceof MagazineItem) {
                returned += MagazineItem.getRounds(inv.getItem(i));
            }
        }
        helper.assertTrue(KineticPistolItem.getAmmo(pistol) == 6 && returned == 4,
                "Cambio de cargador: esperaba 6 en la pistola y el viejo con 4, hay " + KineticPistolItem.getAmmo(pistol) + " y " + returned);

        // el Cargador Ampliado no entra sin su módulo
        helper.assertTrue(!KineticPistolItem.acceptsMagazine(pistol, 2), "El Ampliado no debería valer sin el módulo");
        WeaponModule.install(pistol, WeaponModule.EXTENDED_MAG);
        helper.assertTrue(KineticPistolItem.acceptsMagazine(pistol, 2), "Con el módulo el Ampliado sí vale");
        helper.succeed();
    }
}
