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
        // el cargador viejo vuelve a una ranura de munición del Equipo Táctico (o al inventario si están llenas)
        int returned = 0;
        for (ItemStack s : com.bryanamaya.tecnocinetico.gear.TacticalGear.ammoSources(player)) {
            if (s.getItem() instanceof MagazineItem) {
                returned += MagazineItem.getRounds(s);
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

    /** Escopeta: R carga cartucho a cartucho hasta llenarse (2) y gasta exactamente esos cartuchos. */
    @GameTest(template = "empty")
    public static void longGunReload(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        player.getInventory().clearContent();
        player.getInventory().setItem(3, new ItemStack(ModItems.SHOTGUN_SHELL.get(), 5));
        ItemStack gun = new ItemStack(ModItems.PIPE_SHOTGUN.get());
        player.getInventory().setItem(0, gun);
        player.getInventory().selected = 0;
        var item = (com.bryanamaya.tecnocinetico.item.weapon.LongGunItem) gun.getItem();
        item.startReload(player, gun);
        helper.assertTrue(com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.isLoading(gun), "La recarga no empezó");
        for (int i = 0; i < 200 && com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.isLoading(gun); i++) {
            gun.inventoryTick(helper.getLevel(), player, 0, true);
        }
        int ammo = com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.getAmmo(gun);
        helper.assertTrue(ammo == 2 && player.getInventory().getItem(3).getCount() == 3,
                "Esperaba 2 cartuchos dentro y 3 en el inventario, hay " + ammo + " y " + player.getInventory().getItem(3).getCount());
        // armadura: bono de set solo con las 4 piezas
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(ModItems.SCRAP_HELMET.get()));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(ModItems.SCRAP_CHESTPLATE.get()));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(ModItems.SCRAP_LEGGINGS.get()));
        helper.assertTrue(!com.bryanamaya.tecnocinetico.item.armor.ScrapArmorItem.hasFullSet(player), "Con 3 piezas no hay bono");
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(ModItems.SCRAP_BOOTS.get()));
        helper.assertTrue(com.bryanamaya.tecnocinetico.item.armor.ScrapArmorItem.hasFullSet(player), "Con las 4 piezas hay bono");
        helper.succeed();
    }

    /** Módulos de armas largas: solo en su arma, incompatibilidades y efecto en las estadísticas. */
    @GameTest(template = "empty")
    public static void longGunModules(GameTestHelper helper) {
        ItemStack shotgun = new ItemStack(ModItems.PIPE_SHOTGUN.get());
        ItemStack rifle = new ItemStack(ModItems.BOLT_RIFLE.get());
        ItemStack pistol = new ItemStack(ModItems.KINETIC_PISTOL.get());
        helper.assertTrue(WeaponModule.CHOKE.appliesTo(shotgun) && !WeaponModule.CHOKE.appliesTo(rifle)
                && !WeaponModule.CHOKE.appliesTo(pistol), "El estrangulador solo va en la escopeta");
        helper.assertTrue(WeaponModule.SCOPE.appliesTo(rifle) && !WeaponModule.SCOPE.appliesTo(shotgun), "La mira solo va en el rifle");
        helper.assertTrue(!WeaponModule.OPTIC.appliesTo(rifle), "Los módulos de pistola no van en el rifle");
        helper.assertTrue(WeaponModule.CHOKE.conflictsWith(WeaponModule.SAWED_OFF), "Estrangulador y recortados se excluyen");
        var sg = (com.bryanamaya.tecnocinetico.item.weapon.LongGunItem) shotgun.getItem();
        var rf = (com.bryanamaya.tecnocinetico.item.weapon.LongGunItem) rifle.getItem();
        float spread0 = sg.spread(shotgun, false);
        int load0 = sg.loadTicks(shotgun), cd0 = rf.cooldown(rifle);
        float dmg0 = rf.damage(rifle), zoom0 = rf.zoom(rifle);
        WeaponModule.install(shotgun, WeaponModule.CHOKE);
        WeaponModule.install(shotgun, WeaponModule.SPEED_LOADER);
        WeaponModule.install(rifle, WeaponModule.POLISHED_BOLT);
        WeaponModule.install(rifle, WeaponModule.RIFLE_BARREL);
        WeaponModule.install(rifle, WeaponModule.SCOPE);
        helper.assertTrue(sg.spread(shotgun, false) < spread0 && sg.loadTicks(shotgun) < load0, "Estrangulador y cartuchera no surten efecto");
        helper.assertTrue(rf.cooldown(rifle) < cd0 && rf.damage(rifle) > dmg0 && rf.zoom(rifle) < zoom0, "Módulos del rifle sin efecto");
        helper.assertTrue(WeaponModule.localMask(rifle) == 7 && WeaponModule.localMask(shotgun) == 5,
                "Máscara local para el modelo: rifle " + WeaponModule.localMask(rifle) + ", escopeta " + WeaponModule.localMask(shotgun));
        helper.succeed();
    }
}
