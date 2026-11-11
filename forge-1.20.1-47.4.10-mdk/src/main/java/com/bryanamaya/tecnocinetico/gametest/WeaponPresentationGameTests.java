package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.item.weapon.LongGunItem;
import com.bryanamaya.tecnocinetico.item.weapon.WeaponEffects;
import com.bryanamaya.tecnocinetico.network.WeaponEffectPacket;
import com.bryanamaya.tecnocinetico.sound.ModSounds;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class WeaponPresentationGameTests {
    @GameTest(template = "empty", timeoutTicks = 70, batch = "presentacion_armas")
    public static void shotPrecedesMechanismAndSwitchCancelsCycle(GameTestHelper h) {
        var level = h.getLevel();
        var p = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "PruebaSonidos"));
        BlockPos at = h.absolutePos(new BlockPos(1, 3, 1));
        p.moveTo(at.getX()+.5, at.getY(), at.getZ()+.5, 0, -70);
        p.getAbilities().instabuild = true;
        var pump = new ItemStack(ModItems.PUMP_SHOTGUN.get());
        var sniper = new ItemStack(ModItems.KINETIC_SNIPER.get());
        p.getInventory().setItem(0, pump); p.getInventory().selected = 0;
        var sounds = new ArrayList<String>();
        long start = level.getGameTime();
        Consumer<PlayLevelSoundEvent.AtPosition> listener = event -> {
            if (event.getLevel() == level && event.getPosition().distanceToSqr(p.position()) < .01)
                sounds.add((level.getGameTime()-start)+":"+event.getSound().value().getLocation().getPath());
        };
        MinecraftForge.EVENT_BUS.addListener(listener);
        ((LongGunItem)pump.getItem()).tryFire(p, false);
        for (int i=1;i<=38;i++) h.runAfterDelay(i, () -> {
            p.getMainHandItem().inventoryTick(level, p, 0, true);
        });
        h.runAfterDelay(12, () -> {
            p.getInventory().setItem(0, sniper);
            ((LongGunItem)sniper.getItem()).tryFire(p, false);
        });
        h.runAfterDelay(25, () -> {
            p.getCooldowns().removeCooldown(pump.getItem());
            p.getInventory().setItem(0, pump);
            ((LongGunItem)pump.getItem()).tryFire(p, false);
            p.getInventory().setItem(0, ItemStack.EMPTY);
            pump.inventoryTick(level, p, 0, false);
        });
        h.runAfterDelay(40, () -> {
            MinecraftForge.EVENT_BUS.unregister(listener);
            h.assertTrue(sounds.contains("0:item.gun.shotgun"), "Falta el estampido inicial: "+sounds);
            h.assertTrue(sounds.stream().noneMatch(s -> s.equals("0:item.gun.pump") || s.equals("12:item.gun.bolt")),
                    "El mecanismo sonó a la vez que el disparo: "+sounds);
            h.assertTrue(sounds.stream().filter(s -> s.endsWith(":item.gun.pump")).count()==1,
                    "La corredera no sonó una vez o siguió sonando al cambiar de arma: "+sounds);
            h.assertTrue(sounds.contains("12:item.gun.sniper") && sounds.stream().anyMatch(s -> s.endsWith(":item.gun.bolt")),
                    "Falta el estampido o el cerrojo posterior: "+sounds);
            h.assertTrue(!pump.getOrCreateTag().contains("CycleSoundAt"), "Quedó un ciclo pendiente al cambiar de arma");
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "presentacion_armas")
    public static void interruptReloadFiresWithoutClosingSound(GameTestHelper h) {
        var level = h.getLevel();
        var p = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "PruebaRecarga"));
        p.moveTo(h.absolutePos(new BlockPos(1, 3, 1)), 0, -70);
        p.getAbilities().instabuild = true;
        var gun = new ItemStack(ModItems.KINETIC_SNIPER.get());
        gun.getOrCreateTag().putInt("Ammo", 2);
        p.getInventory().setItem(0, gun); p.getInventory().selected = 0;
        var item = (LongGunItem)gun.getItem();
        item.startReload(p, gun);
        var sounds = new ArrayList<net.minecraft.sounds.SoundEvent>();
        Consumer<PlayLevelSoundEvent.AtPosition> listener = event -> {
            if (event.getLevel()==level && event.getPosition().distanceToSqr(p.position())<.01) sounds.add(event.getSound().value());
        };
        MinecraftForge.EVENT_BUS.addListener(listener);
        try {
            item.tryFire(p, false);
            h.assertTrue(sounds.contains(ModSounds.SNIPER_FIRE.get()) && !sounds.contains(ModSounds.PISTOL_SLIDE.get())
                    && !sounds.contains(ModSounds.RIFLE_BOLT.get()), "Interrumpir recarga adelantó el sonido de cierre");
            h.assertTrue(!LongGunItem.isLoading(gun) && LongGunItem.getAmmo(gun)==2, "Interrumpir cambió la munición en creativo");
        } finally { MinecraftForge.EVENT_BUS.unregister(listener); }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "presentacion_armas")
    public static void muzzleRespectsHandsPitchAndWallsAndPacket(GameTestHelper h) {
        var p = h.makeMockPlayer();
        BlockPos at = h.absolutePos(new BlockPos(1, 3, 1));
        p.moveTo(at.getX()+.5, at.getY(), at.getZ()+.5, 0, 0);
        var flame = new ItemStack(ModItems.FLAMETHROWER.get());
        var smg = new ItemStack(ModItems.KINETIC_SMG.get());
        p.setMainArm(HumanoidArm.RIGHT);
        Vec3 right = WeaponEffects.muzzle(p, flame, false);
        double forward = right.subtract(p.getEyePosition()).dot(p.getLookAngle());
        h.assertTrue(forward>2 && forward>WeaponEffects.muzzle(p,smg,false).subtract(p.getEyePosition()).dot(p.getLookAngle()),
                "Se sigue usando la misma salida genérica para cañones distintos");
        p.setMainArm(HumanoidArm.LEFT);
        Vec3 left = WeaponEffects.muzzle(p, flame, false);
        h.assertTrue((right.x-p.getX())*(left.x-p.getX())<0, "La boca no cambia de costado con la mano");
        p.setXRot(-80);
        h.assertTrue(WeaponEffects.muzzle(p,flame,true).subtract(p.getEyePosition()).dot(p.getLookAngle())>1,
                "La salida no sigue el cabeceo vertical");
        p.setXRot(0);
        BlockPos wall = BlockPos.containing(p.getEyePosition().add(p.getLookAngle().scale(1.2)));
        var old = h.getLevel().getBlockState(wall);
        h.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
        Vec3 stopped = WeaponEffects.muzzle(p, flame, false);
        h.getLevel().setBlockAndUpdate(wall,old);
        h.assertTrue(stopped.distanceTo(p.getEyePosition())<1.3, "La boca atravesó la pared cercana");
        var packet = new WeaponEffectPacket(9, 10, WeaponEffectPacket.FLAME, false, left, new Vec3(.2,.3,.4));
        var buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            WeaponEffectPacket.encode(packet,buffer);
            h.assertTrue(packet.equals(WeaponEffectPacket.decode(buffer)), "El efecto cambió al serializarlo");
        } finally { buffer.release(); }
        h.succeed();
    }
}
