package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.gear.GadgetItem;
import com.bryanamaya.tecnocinetico.gear.TacticalGear;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Cliente del Equipo Táctico: empuje del jetpack (el movimiento del jugador lo calcula el cliente) y aviso al
 * servidor de cuándo empieza/termina de empujar (para gastar batería y anular el daño por caída).
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID, value = Dist.CLIENT)
public final class GearClient {

    /** Subida por tick y velocidad vertical máxima del jetpack. */
    private static final double THRUST = 0.11D, MAX_RISE = 0.42D;
    private static boolean thrusting;

    private GearClient() {}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        boolean want = false;
        if (p != null && mc.screen == null && mc.options.keyJump.isDown() && !p.onGround() && !p.isInWater()
                && !p.getAbilities().flying && !p.isPassenger() && !p.isFallFlying()) {
            ItemStack back = TacticalGear.of(p).map(g -> g.get(TacticalGear.BACK)).orElse(ItemStack.EMPTY);
            want = back.is(ModItems.JETPACK.get()) && GadgetItem.hasCharge(back);
        }
        if (want) {
            Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(v.x * 1.02D, Math.min(v.y + THRUST, MAX_RISE), v.z * 1.02D);
            p.fallDistance = 0.0F;
        }
        if (want != thrusting && p != null) {
            thrusting = want;
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.JetpackPacket(want));
        }
    }
}
