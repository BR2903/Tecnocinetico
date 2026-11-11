package com.bryanamaya.tecnocinetico.nether;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.custom.CoreShots;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.piglin.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Tecnocinetico.MOD_ID)
public final class NetherMobEvents {
    @SubscribeEvent public static void join(EntityJoinLevelEvent e){
        if(e.getLevel().isClientSide)return;
        if(e.getLevel().dimension()!=Level.NETHER){
            if(e.getEntity() instanceof Mob m&&m.getPersistentData().getBoolean("InfernalUpgrade")){
                var n=m.getPersistentData();double ratio=m.getHealth()/m.getMaxHealth();
                m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(n.getDouble("InfernalOriginalHealth"));
                if(m.getAttribute(Attributes.ARMOR)!=null)m.getAttribute(Attributes.ARMOR).setBaseValue(n.getDouble("InfernalOriginalArmor"));
                if(m.getAttribute(Attributes.ATTACK_DAMAGE)!=null)m.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(n.getDouble("InfernalOriginalDamage"));
                if(n.contains("InfernalOriginalWeapon"))m.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.of(n.getCompound("InfernalOriginalWeapon")));
                m.setHealth((float)(ratio*m.getMaxHealth()));n.remove("InfernalUpgrade");
            }
            return;
        }
        if(e.getEntity() instanceof LargeFireball f){var n=f.saveWithoutId(new net.minecraft.nbt.CompoundTag());n.putByte("ExplosionPower",(byte)3);f.load(n);}
        if(!(e.getEntity() instanceof Mob m)||m.getPersistentData().getBoolean("InfernalUpgrade"))return;
        double hp=m instanceof PiglinBrute?72:m instanceof AbstractPiglin||m instanceof ZombifiedPiglin?36:m instanceof Ghast?44:m instanceof Blaze?44:m instanceof WitherSkeleton?48:0;
        if(hp==0)return;
        var n=m.getPersistentData();n.putDouble("InfernalOriginalHealth",m.getAttribute(Attributes.MAX_HEALTH).getBaseValue());
        if(m.getAttribute(Attributes.ARMOR)!=null)n.putDouble("InfernalOriginalArmor",m.getAttribute(Attributes.ARMOR).getBaseValue());
        if(m.getAttribute(Attributes.ATTACK_DAMAGE)!=null)n.putDouble("InfernalOriginalDamage",m.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue());
        n.put("InfernalOriginalWeapon",m.getMainHandItem().save(new net.minecraft.nbt.CompoundTag()));
        m.getPersistentData().putBoolean("InfernalUpgrade",true);
        double ratio=m.getHealth()/m.getMaxHealth();m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);m.setHealth((float)(hp*ratio));
        if(m.getAttribute(Attributes.ARMOR)!=null)m.getAttribute(Attributes.ARMOR).setBaseValue(m instanceof PiglinBrute?12:m instanceof Ghast?4:8);
        if(m.getAttribute(Attributes.ATTACK_DAMAGE)!=null)m.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(m instanceof PiglinBrute?15:m instanceof WitherSkeleton?12:9);
        if(m instanceof Piglin||m instanceof PiglinBrute){
            m.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(m.getRandom().nextBoolean()?Items.NETHERITE_AXE:com.bryanamaya.tecnocinetico.item.ModItems.ASSAULT_RIFLE.get()));
            m.setDropChance(EquipmentSlot.MAINHAND,.025F);
        }
    }
    @SubscribeEvent public static void enemyTick(LivingEvent.LivingTickEvent e){
        if(!(e.getEntity() instanceof Mob m)||!(m.level() instanceof ServerLevel s)||s.dimension()!=Level.NETHER)return;
        if(m.tickCount%10!=0)return;
        if(m.getTarget() instanceof Player previous&&(previous.isCreative()||previous.isSpectator())){
            m.setTarget(null);
            if(m instanceof AbstractPiglin pig){pig.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);pig.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);}
            if(m instanceof ZombifiedPiglin z)z.stopBeingAngry();
        }
        if(m instanceof AbstractPiglin||m instanceof ZombifiedPiglin||m instanceof EnderMan){
            Player p=s.players().stream().filter(v->!v.isCreative()&&!v.isSpectator()&&v.isAlive()&&m.distanceToSqr(v)<1024&&m.hasLineOfSight(v)).min(java.util.Comparator.comparingDouble(m::distanceToSqr)).orElse(null);if(p!=null){
                m.setTarget(p);
                if(m instanceof AbstractPiglin pig){
                    pig.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT,p.getUUID(),400);
                    pig.getBrain().setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET,p,40);
                }
                if(m instanceof ZombifiedPiglin z){z.setPersistentAngerTarget(p.getUUID());z.setRemainingPersistentAngerTime(400);}
                if(m.getMainHandItem().is(com.bryanamaya.tecnocinetico.item.ModItems.ASSAULT_RIFLE.get())){
                    if(m.distanceToSqr(p)>36)m.getNavigation().stop();
                    if(m.tickCount%60==40)s.sendParticles(ParticleTypes.ELECTRIC_SPARK,m.getX(),m.getEyeY(),m.getZ(),8,.1,.1,.1,.01);
                    if(m.tickCount%60<30){
                        CoreShots.fire(m,m.getEyePosition(),p.getEyePosition().subtract(m.getEyePosition()).normalize(),32,10,3,Monster.class,false);
                        s.playSound(null,m.blockPosition(),com.bryanamaya.tecnocinetico.sound.ModSounds.AUTO_FIRE.get(),net.minecraft.sounds.SoundSource.HOSTILE,1,.8F);
                    }
                }
            }
        }
        LivingEntity t=m.getTarget();if(t==null||!t.isAlive()||!m.hasLineOfSight(t))return;
        int cycle=m.tickCount%100;
        if(m instanceof Blaze){
            if(cycle==60)s.sendParticles(ParticleTypes.FLAME,m.getX(),m.getEyeY(),m.getZ(),25,.8,.8,.8,.03);
            if(cycle==80)for(int i=-1;i<=1;i++){
                Vec3 dir=t.getEyePosition().subtract(m.getEyePosition()).normalize().yRot(i*.12F);
                var ball=new net.minecraft.world.entity.projectile.SmallFireball(s,m,dir.x,dir.y,dir.z);ball.setPos(m.getEyePosition());s.addFreshEntity(ball);
            }
        }else if(m instanceof WitherSkeleton){
            if(cycle==60)s.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,m.getX(),m.getY()+1,m.getZ(),18,.4,.5,.4,.01);
            if(cycle==80&&m.distanceToSqr(t)<100){Vec3 d=t.position().subtract(m.position()).normalize().scale(.8);m.setDeltaMovement(d.x,.2,d.z);m.hasImpulse=true;}
        }
    }
    private NetherMobEvents(){}
}

