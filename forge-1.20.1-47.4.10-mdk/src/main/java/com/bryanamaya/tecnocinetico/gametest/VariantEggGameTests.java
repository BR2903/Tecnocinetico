package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.MobVariants;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.nether.NetherMobEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Huevos de mobs modificados: en el Overworld salen ya mejorados y no vuelven a vanilla. */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class VariantEggGameTests {

    private static <T extends Mob> T use(GameTestHelper h, Item egg, BlockPos floor, EntityType<T> type) {
        h.setBlock(floor, Blocks.STONE);
        var player = h.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(egg));
        BlockPos abs = h.absolutePos(floor);
        egg.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
        var found = h.getLevel().getEntitiesOfClass(Mob.class, new AABB(abs.above()).inflate(0.5), e -> e.getType() == type);
        h.assertTrue(found.size() == 1, "El huevo no hizo aparecer " + type.getDescriptionId());
        @SuppressWarnings("unchecked") T mob = (T) found.get(0);
        return mob;
    }

    @GameTest(template = "empty")
    public static void variantEggsKeepUpgrades(GameTestHelper h) {
        Mob enderman = use(h, ModItems.END_ENDERMAN_EGG.get(), new BlockPos(0, 1, 0), EntityType.ENDERMAN);
        h.assertTrue(MobVariants.is(enderman, MobVariants.END) && enderman.getMaxHealth() == 60,
                "El Enderman Corrupto del huevo no está mejorado: " + enderman.getMaxHealth());
        Piglin piglin = (Piglin) use(h, ModItems.NETHER_PIGLIN_EGG.get(), new BlockPos(2, 1, 0), EntityType.PIGLIN);
        h.assertTrue(piglin.getMaxHealth() == 36 && NetherMobEvents.holdsNetherWeapon(piglin) && piglin.saveWithoutId(new CompoundTag()).getBoolean("IsImmuneToZombification"),
                "El Piglin Infernal del huevo no está mejorado o se zombificaría");
        // guardar y volver a entrar en el Overworld: sigue mejorado
        var copy = EntityType.ENDERMAN.create(h.getLevel());
        copy.load(enderman.saveWithoutId(new CompoundTag()));
        com.bryanamaya.tecnocinetico.end.EndMobEvents.join(new EntityJoinLevelEvent(copy, h.getLevel()));
        h.assertTrue(copy.getMaxHealth() == 60, "Al cargarse fuera del End vuelve a vanilla");
        // un enderman normal del Overworld sigue siendo vanilla
        var normal = EntityType.ENDERMAN.create(h.getLevel());
        com.bryanamaya.tecnocinetico.end.EndMobEvents.join(new EntityJoinLevelEvent(normal, h.getLevel()));
        h.assertTrue(normal.getMaxHealth() == 40, "Un enderman normal no debería mejorarse");
        enderman.discard();
        piglin.discard();
        h.succeed();
    }
}
