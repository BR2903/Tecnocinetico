package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.weapon.ThermalLanceItem;
import com.bryanamaya.tecnocinetico.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Gatillo de la Lanza Térmica (cliente): mantener clic izquierdo dispara (no golpea ni rompe bloques); el clic derecho
 * no usa el objeto: apunta (zoom y pose, en PistolInputHandler). Avisa al servidor al apretar, cada pocos ticks
 * mientras se mantiene y al soltar.
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID, value = Dist.CLIENT)
public final class LanceInput {

    private static boolean held;
    private static int keepAlive;

    private LanceInput() {}

    public static boolean holdingLance(LocalPlayer p) {
        return p != null && p.getMainHandItem().getItem() instanceof ThermalLanceItem
                && !(p.getVehicle() instanceof com.bryanamaya.tecnocinetico.entity.ship.CoreFighterEntity);
    }

    /** El jugador local aprieta el gatillo con plasma (para dibujar el rayo sin esperar al servidor). */
    public static boolean firingLocal(ItemStack stack) {
        var mc = Minecraft.getInstance();
        return held && mc.player != null && mc.player.getMainHandItem() == stack && ThermalLanceItem.canFire(stack);
    }

    @SubscribeEvent
    public static void onClick(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (!holdingLance(mc.player) || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return;
        }
        if (event.isAttack() || event.isUseItem()) {
            event.setCanceled(true);                                                         // ni golpe ni romper ni usar
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        boolean want = holdingLance(mc.player) && mc.screen == null && mc.options.keyAttack.isDown()
                && !ThermalLanceItem.reloading(mc.player.getMainHandItem());
        if (want != held || (want && ++keepAlive >= 5)) {
            keepAlive = 0;
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.LanceFirePacket(want));
        }
        held = want;
    }
}
