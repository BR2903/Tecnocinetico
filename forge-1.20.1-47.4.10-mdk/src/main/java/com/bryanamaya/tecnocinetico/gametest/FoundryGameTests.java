package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.ModEntities;
import com.bryanamaya.tecnocinetico.gear.GearRecipes;
import com.bryanamaya.tecnocinetico.gear.NetherGadgetItem;
import com.bryanamaya.tecnocinetico.gear.TacticalGear;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.nether.AbyssFoundryEntity;
import com.bryanamaya.tecnocinetico.nether.NetherEntities;
import com.bryanamaya.tecnocinetico.nether.NetherHazards;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Nether parte 5: Fundidor del Abismo, Acoplador del Nether en el caza y equipo Mk II. */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class FoundryGameTests {

    private static AbyssFoundryEntity foundry(GameTestHelper helper, int dx) {
        var level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(1 + dx, 1, 1));
        at = new BlockPos(at.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING, at.getX(), at.getZ()), at.getZ());
        AbyssFoundryEntity f = NetherEntities.FOUNDRY.get().create(level);
        helper.assertTrue(f != null, "No se crea el Fundidor");
        f.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        f.setNoAi(true);
        level.addFreshEntity(f);
        return f;
    }

    /** Expuesto tras el sobrecalentamiento recibe ×1,6 de daño. */
    @GameTest(template = "empty", batch = "fundidor")
    public static void foundryExposedTakesMore(GameTestHelper helper) {
        AbyssFoundryEntity a = foundry(helper, 0), b = foundry(helper, 4);
        helper.assertTrue(a.getMaxHealth() == AbyssFoundryEntity.BASE_HEALTH, "Vida base del Fundidor: " + a.getMaxHealth());
        b.testSetExposed(40);
        var src = helper.getLevel().damageSources().generic();
        a.hurt(src, 50.0F);
        b.hurt(src, 50.0F);
        float lostA = a.getMaxHealth() - a.getHealth(), lostB = b.getMaxHealth() - b.getHealth();
        a.discard();
        b.discard();
        helper.assertTrue(lostB > lostA * 1.4F, "Expuesto no recibe más daño: " + lostA + " / " + lostB);
        helper.succeed();
    }

    /** El caza solo salta al Nether con el Acoplador instalado (y lo conserva en sus datos). */
    @GameTest(template = "empty")
    public static void netherCouplerOnShip(GameTestHelper helper) {
        var ship = ModEntities.CORE_FIGHTER.get().create(helper.getLevel());
        helper.assertTrue(ship != null, "No se crea el caza");
        helper.assertTrue(!ship.canJumpTo(Level.NETHER) && ship.canJumpTo(Level.END) && ship.canJumpTo(Level.OVERWORLD),
                "Sin acoplador debe poder ir a todas partes menos al Nether");
        Player p = helper.makeMockPlayer();
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.NETHER_COUPLER.get()));
        ship.interact(p, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(ship.netherDrive() && ship.canJumpTo(Level.NETHER), "El acoplador no se instala");
        var tag = new net.minecraft.nbt.CompoundTag();
        ship.saveWithoutId(tag);
        var copy = ModEntities.CORE_FIGHTER.get().create(helper.getLevel());
        copy.load(tag);
        helper.assertTrue(copy.netherDrive(), "El acoplador no se guarda");
        helper.succeed();
    }

    /** Gadget Mk I + Componente de Forja Abisal = Mk II con el doble de batería; Botas Mk II anulan el empuje. */
    @GameTest(template = "empty")
    public static void netherGadgetMk2(GameTestHelper helper) {
        var level = helper.getLevel();
        var grid = new TransientCraftingContainer(new AbstractContainerMenu((MenuType<?>) null, -1) {
            @Override
            public ItemStack quickMoveStack(Player player, int slot) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        }, 3, 3);
        grid.setItem(0, new ItemStack(ModItems.ANCHOR_BOOTS.get()));
        grid.setItem(1, new ItemStack(ModItems.ABYSSAL_COMPONENT.get()));
        var recipe = new GearRecipes.NetherGadgetUpgrade(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "t"),
                CraftingBookCategory.EQUIPMENT);
        helper.assertTrue(recipe.matches(grid, level), "La mejora Mk II no reconoce botas + componente");
        ItemStack mk2 = recipe.assemble(grid, level.registryAccess());
        helper.assertTrue(NetherGadgetItem.level(mk2) == 2 && mk2.getMaxDamage() == 2 * new ItemStack(ModItems.ANCHOR_BOOTS.get()).getMaxDamage(),
                "Mk II sin el doble de batería");
        grid.setItem(0, mk2.copy());
        helper.assertTrue(!recipe.matches(grid, level), "Se puede mejorar un Mk II otra vez");
        Player p = helper.makeMockPlayer();
        TacticalGear.of(p).orElseThrow().items().setStackInSlot(TacticalGear.FEET, mk2);
        var e = new net.minecraftforge.event.entity.living.LivingKnockBackEvent(p, 1.0F, 1, 0);
        NetherHazards.knockback(e);
        helper.assertTrue(e.getStrength() == 0.0F, "Las Botas de Anclaje Mk II no anulan el empuje");
        helper.succeed();
    }

    /** Con la IA activa: avisa (ataque en curso) y luego hace daño a un objetivo cercano que no se mueve. */
    @GameTest(template = "empty", batch = "fundidor_ia", timeoutTicks = 300)
    public static void foundryAttacks(GameTestHelper helper) {
        AbyssFoundryEntity f = foundry(helper, 0);
        f.setNoAi(false);
        var level = helper.getLevel();
        var golem = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        golem.moveTo(f.getX() + 3.5, f.getY(), f.getZ());
        golem.setNoAi(true);
        level.addFreshEntity(golem);
        f.setTarget(golem);
        var sawWindup = new java.util.concurrent.atomic.AtomicBoolean();
        helper.onEachTick(() -> {
            if (f.attack() != 0) {
                sawWindup.set(true);
            }
        });
        helper.runAfterDelay(260, () -> {
            boolean hurt = golem.getHealth() < golem.getMaxHealth();
            f.discard();
            golem.discard();
            helper.assertTrue(sawWindup.get(), "El Fundidor no avisa ningún ataque");
            helper.assertTrue(hurt, "El Fundidor no hace daño a un objetivo quieto");
            helper.succeed();
        });
    }
}
