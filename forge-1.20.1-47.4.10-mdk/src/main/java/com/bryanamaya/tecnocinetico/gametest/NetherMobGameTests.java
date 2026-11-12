package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.nether.NetherMobEvents;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.gametest.*;
import java.util.UUID;

/** Primera entrega del Nether: hostilidad, armas y ataques vanilla mejorados. */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class NetherMobGameTests {
    @GameTest(template="empty",timeoutTicks=120)
    public static void netherHostilityAndAttacks(GameTestHelper h){
        var s=h.getLevel().getServer().getLevel(Level.NETHER);
        h.assertTrue(s!=null,"Falta el Nether");
        BlockPos at=new BlockPos(1280,70,1280);s.getChunk(at);
        for(BlockPos p:BlockPos.betweenClosed(at.offset(-4,-1,-4),at.offset(4,6,4)))s.setBlock(p,(p.getY()==69?Blocks.BLACKSTONE:Blocks.AIR).defaultBlockState(),3);
        var player=new net.minecraftforge.common.util.FakePlayer(s,new GameProfile(UUID.randomUUID(),"nether-stage-one"));
        player.setGameMode(GameType.SURVIVAL);player.moveTo(at.offset(0,0,3),0,0);
        player.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.GOLDEN_CHESTPLATE));s.addNewPlayer(player);
        try{
            var pig=EntityType.PIGLIN.create(s);pig.moveTo(at,0,0);
            NetherMobEvents.join(new EntityJoinLevelEvent(pig,s));
            NetherMobEvents.setClock(pig,0);NetherMobEvents.enemyTick(new LivingEvent.LivingTickEvent(pig));
            h.assertTrue(pig.getMaxHealth()==36&&!pig.getMainHandItem().isEmpty(),"El piglin no mejora o no obtiene arma");
            var brute=EntityType.PIGLIN_BRUTE.create(s);NetherMobEvents.join(new EntityJoinLevelEvent(brute,s));
            h.assertTrue(brute.getMaxHealth()==72&&!brute.getMainHandItem().isEmpty(),"El bruto no supera sus atributos vanilla");
            h.assertTrue(pig.getTarget()==player&&pig.getBrain().getMemory(MemoryModuleType.ANGRY_AT).orElseThrow().equals(player.getUUID()),"El oro aún evita la hostilidad del piglin");
            var save=pig.saveWithoutId(new CompoundTag());var restored=EntityType.PIGLIN.create(s);restored.load(save);
            NetherMobEvents.join(new EntityJoinLevelEvent(restored,s));h.assertTrue(restored.getMaxHealth()==36,"La mejora se acumula al guardar/cargar");
            NetherMobEvents.join(new EntityJoinLevelEvent(restored,h.getLevel()));
            h.assertTrue(restored.getMaxHealth()==16&&!restored.getPersistentData().getBoolean("InfernalUpgrade"),"Las mejoras se conservan fuera del Nether");
            var zombie=EntityType.ZOMBIFIED_PIGLIN.create(s);zombie.moveTo(at,0,0);
            NetherMobEvents.setClock(zombie,0);NetherMobEvents.enemyTick(new LivingEvent.LivingTickEvent(zombie));
            h.assertTrue(zombie.getTarget()==player&&zombie.getRemainingPersistentAngerTime()>0,"El piglin zombificado sigue siendo neutral");
            var ghast=EntityType.GHAST.create(s);NetherMobEvents.join(new EntityJoinLevelEvent(ghast,s));
            var fireball=new LargeFireball(s,ghast,0,0,1,1);NetherMobEvents.join(new EntityJoinLevelEvent(fireball,s));
            h.assertTrue(ghast.getMaxHealth()==44&&fireball.saveWithoutId(new CompoundTag()).getByte("ExplosionPower")==3,"El ghast no tiene la mejora prevista");
            var blaze=EntityType.BLAZE.create(s);blaze.moveTo(at,0,0);blaze.setTarget(player);NetherMobEvents.setClock(blaze,80);
            NetherMobEvents.enemyTick(new LivingEvent.LivingTickEvent(blaze));
            var shots=s.getEntitiesOfClass(SmallFireball.class,new net.minecraft.world.phys.AABB(at).inflate(2),f->f.getOwner()==blaze);
            h.assertTrue(shots.size()==3,"El blaze no dispara el abanico adicional");shots.forEach(Entity::discard);
            var skeleton=EntityType.WITHER_SKELETON.create(s);skeleton.moveTo(at,0,0);skeleton.setTarget(player);NetherMobEvents.setClock(skeleton,80);
            NetherMobEvents.enemyTick(new LivingEvent.LivingTickEvent(skeleton));
            h.assertTrue(skeleton.getDeltaMovement().horizontalDistance()>.7,"El esqueleto wither no embiste tras el aviso");
            player.setGameMode(GameType.CREATIVE);pig.setTarget(null);
            NetherMobEvents.setClock(pig,0);NetherMobEvents.enemyTick(new LivingEvent.LivingTickEvent(pig));h.assertTrue(pig.getTarget()==null,"La hostilidad selecciona jugadores creativos");
        }finally{s.removePlayerImmediately(player,Entity.RemovalReason.DISCARDED);}
        h.succeed();
    }
}
