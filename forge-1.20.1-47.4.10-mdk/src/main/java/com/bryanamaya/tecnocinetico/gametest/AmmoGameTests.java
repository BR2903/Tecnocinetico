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
        // Cargador (12) + 1 paquete cinético (6) + 1 paquete guiado (6) = lleno
        grid.setItem(0, new ItemStack(ModItems.MAGAZINE.get()));
        grid.setItem(1, new ItemStack(ModItems.KINETIC_ROUND.get()));
        grid.setItem(2, new ItemStack(ModItems.GUIDED_ROUND.get()));
        helper.assertTrue(recipe.matches(grid, helper.getLevel()), "La receta debería aceptar cargador + 2 paquetes");
        ItemStack out = recipe.assemble(grid, helper.getLevel().registryAccess());
        helper.assertTrue(MagazineItem.getRounds(out) == 12 && MagazineItem.getGuided(out) == 6,
                "Esperaba 12 balas (6 guiadas), hay " + MagazineItem.getRounds(out) + " (" + MagazineItem.getGuided(out) + ")");
        // un tercer paquete no cabe en el de 12 (no se pierden balas); en el Ampliado (18) sí
        grid.setItem(3, new ItemStack(ModItems.KINETIC_ROUND.get()));
        helper.assertTrue(!recipe.matches(grid, helper.getLevel()), "3 paquetes no caben en un Cargador de 12");
        grid.setItem(0, new ItemStack(ModItems.EXTENDED_MAGAZINE.get()));
        helper.assertTrue(recipe.matches(grid, helper.getLevel()), "3 paquetes deberían llenar el Cargador Ampliado (18)");
        // otro calibre: un Cargador de Fusil no acepta balas cinéticas
        grid.setItem(0, new ItemStack(ModItems.RIFLE_MAGAZINE.get()));
        helper.assertTrue(!recipe.matches(grid, helper.getLevel()), "El Cargador de Fusil acepta balas cinéticas");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fillAndReload(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        var inv = player.getInventory();
        inv.clearContent();
        inv.setItem(0, new ItemStack(ModItems.MAGAZINE.get()));
        inv.setItem(1, new ItemStack(ModItems.KINETIC_ROUND.get(), 1));
        // llenar con R: un paquete = 6 balas, y el paquete se gasta
        int put = MagazineItem.fillFromInventory(player, inv.getItem(0), false);
        helper.assertTrue(put == 6 && MagazineItem.getRounds(inv.getItem(0)) == 6 && inv.getItem(1).isEmpty(),
                "Llenar con R: esperaba 6 balas dentro, metió " + put);

        // recargar una pistola sin cargador: entra el de 4, y el inventario se queda sin cargadores
        ItemStack pistol = new ItemStack(ModItems.KINETIC_PISTOL.get());
        KineticPistolItem.startReload(player, pistol);
        helper.assertTrue(KineticPistolItem.isReloading(pistol), "La recarga no empezó");
        for (int i = 0; i < 200 && KineticPistolItem.isReloading(pistol); i++) {
            pistol.inventoryTick(helper.getLevel(), player, 0, true);
        }
        helper.assertTrue(KineticPistolItem.getAmmo(pistol) == 6 && KineticPistolItem.getMagType(pistol) == 1,
                "Tras recargar esperaba 6 balas en un Cargador, hay " + KineticPistolItem.getAmmo(pistol));
        helper.assertTrue(inv.getItem(0).isEmpty(), "El cargador debería haber salido del inventario");

        // segundo cargador lleno: al recargar, el viejo (6 balas) vuelve al inventario
        ItemStack full = new ItemStack(ModItems.MAGAZINE.get());
        MagazineItem.setContents(full, 12, 0);
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
        helper.assertTrue(KineticPistolItem.getAmmo(pistol) == 12 && returned == 6,
                "Cambio de cargador: esperaba 12 en la pistola y el viejo con 6, hay " + KineticPistolItem.getAmmo(pistol) + " y " + returned);

        // el Cargador Ampliado no entra sin su módulo
        helper.assertTrue(!KineticPistolItem.acceptsMagazine(pistol, 2), "El Ampliado no debería valer sin el módulo");
        WeaponModule.install(pistol, WeaponModule.EXTENDED_MAG);
        helper.assertTrue(KineticPistolItem.acceptsMagazine(pistol, 2), "Con el módulo el Ampliado sí vale");
        helper.succeed();
    }

    /** Escopeta: R carga cartucho a cartucho hasta llenarse (2); el paquete de 6 queda abierto con 4. */
    @GameTest(template = "empty")
    public static void longGunReload(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        player.getInventory().clearContent();
        player.getInventory().setItem(3, new ItemStack(ModItems.SHOTGUN_SHELL.get(), 1));
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
        int left = com.bryanamaya.tecnocinetico.item.custom.AmmoPackItem.available(player, ModItems.SHOTGUN_SHELL.get());
        helper.assertTrue(ammo == 2 && left == 4, "Esperaba 2 cartuchos dentro y 4 en el paquete abierto, hay " + ammo + " y " + left);
        // armadura: bono de set solo con las 4 piezas
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(ModItems.SCRAP_HELMET.get()));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(ModItems.SCRAP_CHESTPLATE.get()));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(ModItems.SCRAP_LEGGINGS.get()));
        helper.assertTrue(!com.bryanamaya.tecnocinetico.item.armor.ScrapArmorItem.hasFullSet(player), "Con 3 piezas no hay bono");
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(ModItems.SCRAP_BOOTS.get()));
        helper.assertTrue(com.bryanamaya.tecnocinetico.item.armor.ScrapArmorItem.hasFullSet(player), "Con las 4 piezas hay bono");
        helper.succeed();
    }

    /**
     * Paquetes: 1 paquete cinético llena el revólver (6); la Caja de Cinta (60) gasta 4 cintas de 5; la de la Minigun
     * (180), 12; la cinta es de rifle (15 = 3 paquetes); el subfusil usa el Cargador Ampliado.
     */
    @GameTest(template = "empty")
    public static void packsBeltsAndBoxes(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        var inv = player.getInventory();
        inv.clearContent();
        // revólver: un paquete = tambor lleno
        inv.setItem(3, new ItemStack(ModItems.KINETIC_ROUND.get(), 1));
        ItemStack revolver = new ItemStack(ModItems.REVOLVER.get());
        inv.setItem(0, revolver);
        inv.selected = 0;
        var rev = (com.bryanamaya.tecnocinetico.item.weapon.LongGunItem) revolver.getItem();
        rev.startReload(player, revolver);
        for (int i = 0; i < 400 && com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.isLoading(revolver); i++) {
            revolver.inventoryTick(helper.getLevel(), player, 0, true);
        }
        helper.assertTrue(com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.getAmmo(revolver) == 6 && inv.getItem(3).isEmpty(),
                "El revólver no se llenó con un paquete: " + com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.getAmmo(revolver));
        // cajas de cinta
        inv.clearContent();
        inv.setItem(0, new ItemStack(ModItems.BELT_BOX.get()));
        inv.setItem(1, new ItemStack(ModItems.RIFLE_BELT.get(), 5));
        int put = MagazineItem.fillFromInventory(player, inv.getItem(0), false);
        helper.assertTrue(put == 60 && inv.getItem(1).getCount() == 1, "Caja de Cinta: metió " + put + ", quedan " + inv.getItem(1).getCount() + " cintas");
        inv.setItem(0, new ItemStack(ModItems.MINIGUN_BOX.get()));
        inv.setItem(1, new ItemStack(ModItems.RIFLE_BELT.get(), 12));
        put = MagazineItem.fillFromInventory(player, inv.getItem(0), false);
        helper.assertTrue(put == 180 && inv.getItem(1).isEmpty(), "Caja de Minigun: metió " + put);
        helper.assertTrue(com.bryanamaya.tecnocinetico.item.custom.AmmoPackItem.size(ModItems.RIFLE_BELT.get())
                == 3 * com.bryanamaya.tecnocinetico.item.custom.AmmoPackItem.size(ModItems.RIFLE_ROUND.get()), "La cinta no son 3 paquetes de rifle");
        // cada arma con su cargador
        helper.assertTrue(com.bryanamaya.tecnocinetico.item.weapon.GunMagazines.of(ModItems.MINIGUN.get()).contains(ModItems.MINIGUN_BOX.get())
                && com.bryanamaya.tecnocinetico.item.weapon.GunMagazines.of(ModItems.KINETIC_SMG.get()).contains(ModItems.EXTENDED_MAGAZINE.get())
                && com.bryanamaya.tecnocinetico.item.weapon.GunMagazines.of(ModItems.RAIL_RIFLE.get()).contains(ModItems.STRIPPER_CLIP.get())
                && !com.bryanamaya.tecnocinetico.item.weapon.GunMagazines.magazineFed(ModItems.REVOLVER.get()), "Tabla de cargadores incorrecta");
        // un cargador de rifle no acepta balas cinéticas al llenarlo con R
        inv.clearContent();
        inv.setItem(0, new ItemStack(ModItems.RIFLE_MAGAZINE.get()));
        inv.setItem(1, new ItemStack(ModItems.KINETIC_ROUND.get(), 3));
        helper.assertTrue(MagazineItem.fillFromInventory(player, inv.getItem(0), false) == 0 && inv.getItem(1).getCount() == 3,
                "El Cargador de Fusil se llenó con balas cinéticas");
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

    /**
     * Tier 2: las piezas "powered" solo salen de la prensa con energía; el rifle semiautomático recarga el cargador
     * entero de golpe; el subfusil gasta 1 bala por disparo y no dispara dos veces en el mismo tick; bonos de set.
     */
    @GameTest(template = "empty")
    public static void tier2GunsAndArmor(GameTestHelper helper) {
        // prensa: placa de inercita
        var inv = new net.minecraftforge.items.ItemStackHandler(3);
        inv.setStackInSlot(0, new ItemStack(ModItems.INERCITA_INGOT.get()));
        inv.setStackInSlot(1, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT));
        inv.setStackInSlot(2, new ItemStack(ModItems.SCRAP_METAL.get()));
        helper.assertTrue(com.bryanamaya.tecnocinetico.block.entity.PressRecipes.findResult(inv, 3, false).isEmpty(),
                "La placa de inercita no debe salir a martillo");
        helper.assertTrue(com.bryanamaya.tecnocinetico.block.entity.PressRecipes.findResult(inv, 3, true).is(ModItems.INERCITA_PLATE.get()),
                "La prensa con energía debe hacer la placa de inercita");

        // rifle semiautomático: recarga de golpe del Cargador de Fusil Corto más lleno (10); el de 5 se queda
        Player player = helper.makeMockPlayer();
        player.getInventory().clearContent();
        ItemStack magFull = new ItemStack(ModItems.SHORT_RIFLE_MAGAZINE.get()), magHalf = new ItemStack(ModItems.SHORT_RIFLE_MAGAZINE.get());
        MagazineItem.setContents(magFull, 10, 0);
        MagazineItem.setContents(magHalf, 5, 0);
        player.getInventory().setItem(3, magHalf);
        player.getInventory().setItem(4, magFull);
        player.getInventory().setItem(5, new ItemStack(ModItems.RIFLE_ROUND.get(), 9));            // paquetes sueltos: no sirven sin cargador
        ItemStack rifle = new ItemStack(ModItems.SEMI_RIFLE.get());
        player.getInventory().setItem(0, rifle);
        player.getInventory().selected = 0;
        var semi = (com.bryanamaya.tecnocinetico.item.weapon.LongGunItem) rifle.getItem();
        semi.startReload(player, rifle);
        int ticks = 0;
        while (ticks < 200 && com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.isLoading(rifle)) {
            rifle.inventoryTick(helper.getLevel(), player, 0, true);
            ticks++;
        }
        int ammo = com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.getAmmo(rifle);
        int inMags = MagazineItem.getRounds(player.getInventory().getItem(3)) + MagazineItem.getRounds(player.getInventory().getItem(4));
        helper.assertTrue(ammo == 10 && inMags == 5 && player.getInventory().getItem(5).getCount() == 9 && ticks == semi.stats().loadTicks(),
                "Semiautomático: esperaba 10 dentro, 5 en cargadores, paquetes intactos y una sola espera; hay " + ammo + ", "
                        + inMags + " y " + ticks + " ticks");

        // subfusil: 1 bala por disparo, sin doble disparo en el mismo tick
        var sp = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());   // conexión simulada
        sp.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1, 3, 1)));
        sp.getInventory().clearContent();
        ItemStack smg = new ItemStack(ModItems.KINETIC_SMG.get());
        smg.getOrCreateTag().putInt("Ammo", 18);
        sp.getInventory().setItem(0, smg);
        sp.getInventory().selected = 0;
        var auto = (com.bryanamaya.tecnocinetico.item.weapon.LongGunItem) smg.getItem();
        auto.tryFire(sp, false);
        auto.tryFire(sp, false);
        int left = com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.getAmmo(sp.getMainHandItem());
        helper.assertTrue(left == 17, "Subfusil: esperaba 17 balas tras dos intentos en el mismo tick, hay " + left);

        // armaduras: set completo y freno de las placas de inercita
        double base = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(ModItems.INERCITA_HELMET.get()));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(ModItems.INERCITA_CHESTPLATE.get()));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(ModItems.INERCITA_LEGGINGS.get()));
        var kind = com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.INERCITA;
        helper.assertTrue(!com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.hasFullSet(player, kind), "Con 3 piezas no hay bono");
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(ModItems.INERCITA_BOOTS.get()));
        helper.assertTrue(com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.hasFullSet(player, kind), "Con las 4 piezas hay bono");
        player.tick();                                                         // aplica los modificadores del equipo
        double slow = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        helper.assertTrue(slow < base * 0.9, "Las placas de inercita deberían frenar (" + base + " -> " + slow + ")");
        helper.assertTrue(!com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.hasFullSet(player,
                com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.EXO), "No es un exoesqueleto");
        helper.succeed();
    }

    /**
     * Tier 3: el Ensamblador reconoce la receta del fusil y consume justo lo pedido; el Rifle de Riel atraviesa a dos
     * cerdos en fila; la Granada Cinética estalla junto a un cerdo; el Cañón de Impulso cambia la célula por una vacía.
     */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void tier3Weapons(GameTestHelper helper) {
        var inv = new net.minecraftforge.items.ItemStackHandler(6);
        inv.setStackInSlot(0, new ItemStack(ModItems.CORE_PROCESSOR.get(), 2));
        inv.setStackInSlot(1, new ItemStack(ModItems.INERCITA_PLATE.get(), 5));
        inv.setStackInSlot(2, new ItemStack(ModItems.PRECISION_MECHANISM.get(), 3));
        inv.setStackInSlot(3, new ItemStack(ModItems.TITANIUM_INGOT.get(), 6));          // pide 4: sobran 2
        inv.setStackInSlot(4, new ItemStack(ModItems.BALLISTIC_FABRIC.get(), 2));
        var recipe = com.bryanamaya.tecnocinetico.block.entity.AssemblerRecipes.find(inv, 6);
        helper.assertTrue(recipe != null && recipe.output().is(ModItems.ASSAULT_RIFLE.get()), "El ensamblador no reconoce el fusil");
        com.bryanamaya.tecnocinetico.block.entity.AssemblerRecipes.consume(inv, 6, recipe);
        helper.assertTrue(inv.getStackInSlot(0).getCount() == 1 && inv.getStackInSlot(1).getCount() == 1
                && inv.getStackInSlot(3).getCount() == 2 && inv.getStackInSlot(4).isEmpty(), "El ensamblador consumió mal");

        var level = helper.getLevel();
        net.minecraft.core.BlockPos col = helper.absolutePos(net.minecraft.core.BlockPos.ZERO);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, col.getX(), col.getZ()) + 6;
        // riel: dos cerdos en fila hacia +X
        var sp = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
        sp.moveTo(col.getX() + 0.5, y, col.getZ() + 0.5, -90.0F, 0.0F);
        sp.getInventory().clearContent();
        ItemStack rail = new ItemStack(ModItems.RAIL_RIFLE.get());
        rail.getOrCreateTag().putInt("Ammo", 5);
        sp.getInventory().setItem(0, rail);
        sp.getInventory().selected = 0;
        java.util.List<net.minecraft.world.entity.Entity> spawned = new java.util.ArrayList<>();
        java.util.function.BiFunction<Double, Double, net.minecraft.world.entity.animal.Pig> pig = (dx, dy) -> {
            var p = net.minecraft.world.entity.EntityType.PIG.create(level);
            p.moveTo(col.getX() + 0.5 + dx, y + dy, col.getZ() + 0.5, 0, 0);
            p.setNoAi(true);
            p.setNoGravity(true);
            level.addFreshEntity(p);
            spawned.add(p);
            return p;
        };
        var p1 = pig.apply(3.0, 0.9);
        var p2 = pig.apply(6.0, 0.9);
        ((com.bryanamaya.tecnocinetico.item.weapon.LongGunItem) rail.getItem()).tryFire(sp, true, 1.0F);
        boolean pierced = p1.getHealth() < p1.getMaxHealth() && p2.getHealth() < p2.getMaxHealth();
        // granada junto a un cerdo
        var p3 = pig.apply(0.0, 0.0);
        p3.moveTo(col.getX() + 0.5, y + 12, col.getZ() + 6.5, 0, 0);
        var grenade = new com.bryanamaya.tecnocinetico.entity.custom.KineticGrenadeEntity(level, sp);
        grenade.setPos(col.getX() + 0.5, y + 12.3, col.getZ() + 7.5);
        grenade.setDeltaMovement(0, 0, -0.3);
        level.addFreshEntity(grenade);
        spawned.add(grenade);
        // cañón: célula agotada -> recarga -> devuelve una vacía
        Player player = helper.makeMockPlayer();
        player.getInventory().clearContent();
        player.getInventory().setItem(5, new ItemStack(ModItems.PULSE_CELL.get(), 2));
        ItemStack cannon = new ItemStack(ModItems.PULSE_CANNON.get());
        player.getInventory().setItem(0, cannon);
        player.getInventory().selected = 0;
        var c = (com.bryanamaya.tecnocinetico.item.weapon.LongGunItem) cannon.getItem();
        c.startReload(player, cannon);
        for (int i = 0; i < 100 && com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.isLoading(cannon); i++) {
            cannon.inventoryTick(level, player, 0, true);
        }
        int shots = com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.getAmmo(cannon);
        boolean cellSwap = shots == 6 && player.getInventory().getItem(5).getCount() == 1
                && player.getInventory().countItem(ModItems.EMPTY_PULSE_CELL.get()) == 1;
        // granada de mano: lanzada tras "cocinarla" 10 ticks -> sale con 40 de mecha y se gasta una
        ItemStack hand = new ItemStack(ModItems.KINETIC_GRENADE.get(), 3);
        sp.getInventory().setItem(1, hand);
        int before = level.getEntitiesOfClass(com.bryanamaya.tecnocinetico.entity.custom.KineticGrenadeEntity.class,
                sp.getBoundingBox().inflate(4)).size();
        hand.getItem().releaseUsing(hand, level, sp, 72000 - 10);
        var thrown = level.getEntitiesOfClass(com.bryanamaya.tecnocinetico.entity.custom.KineticGrenadeEntity.class,
                sp.getBoundingBox().inflate(4));
        boolean handThrow = thrown.size() == before + 1 && hand.getCount() == 2;
        spawned.addAll(thrown);
        helper.assertTrue(handThrow, "La granada de mano no se lanzó (" + thrown.size() + ", quedan " + hand.getCount() + ")");
        helper.runAfterDelay(70, () -> {
            boolean blown = p3.getHealth() < p3.getMaxHealth();
            Tecnocinetico.LOGGER.info("[GameTest] tier 3: riel atraviesa={}, granada={}, célula={} ({} disparos)", pierced, blown, cellSwap, shots);
            spawned.forEach(net.minecraft.world.entity.Entity::discard);
            helper.assertTrue(pierced, "El Rifle de Riel no atravesó a los dos cerdos");
            helper.assertTrue(blown, "La granada no hirió al cerdo");
            helper.assertTrue(cellSwap, "El Cañón de Impulso no cambió la célula por una vacía");
            helper.succeed();
        });
    }

    /** Armaduras tier 3: el Ensamblador hace la coraza del Núcleo; la Piroxita completa anula el daño de la lava. */
    @GameTest(template = "empty")
    public static void tier3Armor(GameTestHelper helper) {
        var inv = new net.minecraftforge.items.ItemStackHandler(6);
        inv.setStackInSlot(0, new ItemStack(ModItems.CORE_PROCESSOR.get()));
        inv.setStackInSlot(1, new ItemStack(ModItems.INERCITA_PLATE.get(), 6));
        inv.setStackInSlot(2, new ItemStack(ModItems.KINETIC_SERVO.get(), 2));
        inv.setStackInSlot(3, new ItemStack(ModItems.PRECISION_MECHANISM.get(), 2));
        inv.setStackInSlot(4, new ItemStack(ModItems.CINETRITE_INGOT.get(), 3));
        inv.setStackInSlot(5, new ItemStack(ModItems.TITANIUM_INGOT.get(), 3));
        var r = com.bryanamaya.tecnocinetico.block.entity.AssemblerRecipes.find(inv, 6);
        helper.assertTrue(r != null && r.output().is(ModItems.CORE_CHESTPLATE.get()), "El ensamblador no reconoce la coraza del Núcleo");

        Player player = helper.makeMockPlayer();
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(ModItems.PIROXITE_HELMET.get()));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(ModItems.PIROXITE_CHESTPLATE.get()));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(ModItems.PIROXITE_LEGGINGS.get()));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(ModItems.PIROXITE_BOOTS.get()));
        float hp = player.getHealth();
        player.hurt(player.damageSources().lava(), 6.0F);
        helper.assertTrue(player.getHealth() == hp, "La Piroxita no anuló la lava (" + player.getHealth() + ")");
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, ItemStack.EMPTY);
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().lava(), 6.0F);
        helper.assertTrue(player.getHealth() < hp, "Sin el set completo la lava debería herir");
        helper.succeed();
    }

    /** Armas nuevas: lanzallamas quema, arco voltaico encadena, minigun gira antes de disparar, cuchilla se carga. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void moreWeapons(GameTestHelper helper) {
        var level = helper.getLevel();
        net.minecraft.core.BlockPos col = helper.absolutePos(net.minecraft.core.BlockPos.ZERO);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, col.getX(), col.getZ()) + 6;
        var sp = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
        sp.moveTo(col.getX() + 0.5, y, col.getZ() + 0.5, -90.0F, 0.0F);
        sp.getInventory().clearContent();
        sp.getInventory().selected = 0;
        java.util.List<net.minecraft.world.entity.Entity> spawned = new java.util.ArrayList<>();
        java.util.function.BiFunction<Double, Double, net.minecraft.world.entity.animal.Pig> pig = (dx, dz) -> {
            var p = net.minecraft.world.entity.EntityType.PIG.create(level);
            p.moveTo(col.getX() + 0.5 + dx, y + 0.9, col.getZ() + 0.5 + dz, 0, 0);
            p.setNoAi(true);
            p.setNoGravity(true);
            level.addFreshEntity(p);
            spawned.add(p);
            return p;
        };
        // lanzallamas
        var p1 = pig.apply(3.0, 0.0);
        ItemStack flame = new ItemStack(ModItems.FLAMETHROWER.get());
        flame.getOrCreateTag().putInt("Ammo", 100);
        sp.getInventory().setItem(0, flame);
        ((com.bryanamaya.tecnocinetico.item.weapon.LongGunItem) flame.getItem()).tryFire(sp, false);
        boolean burned = p1.isOnFire() && p1.getHealth() < p1.getMaxHealth();
        p1.discard();
        // arco voltaico: dos cerdos juntos
        var a1 = pig.apply(6.0, 0.0);
        var a2 = pig.apply(6.0, 3.0);
        ItemStack arc = new ItemStack(ModItems.ARC_CASTER.get());
        arc.getOrCreateTag().putInt("Ammo", 12);
        sp.getInventory().setItem(0, arc);
        ((com.bryanamaya.tecnocinetico.item.weapon.LongGunItem) arc.getItem()).tryFire(sp, false);
        boolean chained = a1.getHealth() < a1.getMaxHealth() && a2.getHealth() < a2.getMaxHealth();
        // minigun: el primer intento solo gira (no gasta munición)
        ItemStack mini = new ItemStack(ModItems.MINIGUN.get());
        mini.getOrCreateTag().putInt("Ammo", 200);
        sp.getInventory().setItem(0, mini);
        ((com.bryanamaya.tecnocinetico.item.weapon.LongGunItem) mini.getItem()).tryFire(sp, false);
        boolean spins = com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.getAmmo(sp.getMainHandItem()) == 200
                && com.bryanamaya.tecnocinetico.item.weapon.MinigunItem.spin(sp.getMainHandItem()) > 0;
        // cuchilla: cada golpe carga
        ItemStack blade = new ItemStack(ModItems.KINETIC_BLADE.get());
        blade.getItem().hurtEnemy(blade, a1, sp);
        boolean charged = com.bryanamaya.tecnocinetico.item.weapon.KineticBladeItem.charge(blade) == 1;
        boolean scope = com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.hasScope(new ItemStack(ModItems.KINETIC_SNIPER.get()));
        Tecnocinetico.LOGGER.info("[GameTest] armas: fuego={}, cadena={}, minigun gira={}, cuchilla={}, mira={}", burned, chained, spins, charged, scope);
        spawned.forEach(net.minecraft.world.entity.Entity::discard);
        sp.getInventory().clearContent();
        helper.assertTrue(burned, "El lanzallamas no quemó al cerdo");
        helper.assertTrue(chained, "El arco voltaico no saltó al segundo cerdo");
        helper.assertTrue(spins, "La minigun disparó sin girar o no giró");
        helper.assertTrue(charged && scope, "La cuchilla no se cargó o el francotirador no tiene mira");
        helper.succeed();
    }
}
