package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.end.EndMobEvents;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.nether.NetherMobEvents;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** End, parte 1: endermen, shulkers y endermites corruptos (solo en el End). */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class EndMobGameTests {

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void endCorruptedMobs(GameTestHelper h) {
        var s = h.getLevel().getServer().getLevel(Level.END);
        h.assertTrue(s != null, "Falta el End");
        BlockPos at = new BlockPos(1600, 70, 1600);
        s.getChunk(at);
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-6, -1, -6), at.offset(6, 6, 6))) {
            s.setBlock(p, (p.getY() == 69 ? Blocks.END_STONE : Blocks.AIR).defaultBlockState(), 3);
        }
        var player = new net.minecraftforge.common.util.FakePlayer(s, new GameProfile(UUID.randomUUID(), "end-stage-one"));
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 3.5, 0, 0);            // mira al sur (+Z)
        s.addNewPlayer(player);
        try {
            // mejora y restauración
            var enderman = EntityType.ENDERMAN.create(s);
            enderman.moveTo(at.getX() + 0.5, at.getY(), at.getZ() - 2.5, 0, 0);
            EndMobEvents.join(new EntityJoinLevelEvent(enderman, s));
            h.assertTrue(enderman.getMaxHealth() == 60, "El enderman no se corrompe en el End: " + enderman.getMaxHealth());
            var copy = EntityType.ENDERMAN.create(s);
            copy.load(enderman.saveWithoutId(new CompoundTag()));
            EndMobEvents.join(new EntityJoinLevelEvent(copy, s));
            h.assertTrue(copy.getMaxHealth() == 60, "La mejora se acumula al guardar/cargar");
            EndMobEvents.join(new EntityJoinLevelEvent(copy, h.getLevel()));
            h.assertTrue(copy.getMaxHealth() == 40, "Fuera del End no vuelve a ser vanilla");
            var shulker = EntityType.SHULKER.create(s);
            EndMobEvents.join(new EntityJoinLevelEvent(shulker, s));
            h.assertTrue(shulker.getMaxHealth() == 45, "El shulker no se corrompe");
            var mite = EntityType.ENDERMITE.create(s);
            EndMobEvents.join(new EntityJoinLevelEvent(mite, s));
            h.assertTrue(mite.getMaxHealth() == 16, "La endermite no se corrompe");

            // hostil al verte, salvo con la armadura de Nulita
            s.addFreshEntity(enderman);
            NetherMobEvents.setClock(enderman, 0);
            EndMobEvents.enemyTick(new LivingEvent.LivingTickEvent(enderman));
            h.assertTrue(enderman.getTarget() == player, "El enderman corrupto no ataca al ver al jugador");
            enderman.setTarget(null);
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.NULLITE_HELMET.get()));
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.NULLITE_CHESTPLATE.get()));
            player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.NULLITE_LEGGINGS.get()));
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.NULLITE_BOOTS.get()));
            EndMobEvents.enemyTick(new LivingEvent.LivingTickEvent(enderman));
            h.assertTrue(enderman.getTarget() == null, "La armadura de Nulita no evita la hostilidad");
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                player.setItemSlot(slot, ItemStack.EMPTY);
            }

            // salto de desfase: aparece a la espalda del jugador
            enderman.setTarget(player);
            NetherMobEvents.setClock(enderman, EndMobEvents.STRIKE);
            EndMobEvents.enemyTick(new LivingEvent.LivingTickEvent(enderman));
            h.assertTrue(enderman.distanceTo(player) < 3.0F && enderman.getZ() < player.getZ(),
                    "El salto de desfase no lo lleva a la espalda del jugador: " + enderman.position());

            // ráfaga del shulker
            shulker.moveTo(at.getX() + 4.5, at.getY(), at.getZ() + 0.5, 0, 0);
            s.addFreshEntity(shulker);
            shulker.setTarget(player);
            NetherMobEvents.setClock(shulker, EndMobEvents.STRIKE);
            EndMobEvents.enemyTick(new LivingEvent.LivingTickEvent(shulker));
            var bullets = s.getEntitiesOfClass(ShulkerBullet.class, new AABB(shulker.blockPosition()).inflate(2), b -> b.getOwner() == shulker);
            h.assertTrue(bullets.size() == EndMobEvents.VOLLEY, "El shulker no dispara la ráfaga: " + bullets.size());
            bullets.forEach(Entity::discard);
            enderman.discard();
            shulker.discard();
        } finally {
            s.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        }
        h.succeed();
    }
}
