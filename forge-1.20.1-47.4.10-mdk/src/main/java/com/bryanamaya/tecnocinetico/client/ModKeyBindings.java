package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.custom.KineticPistolItem;
import com.bryanamaya.tecnocinetico.network.ModNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Teclas del mod (se pueden cambiar en Opciones > Controles > Tecnocinético). */
public final class ModKeyBindings {

    public static final KeyMapping RELOAD = new KeyMapping(
            "key.tecnocinetico.reload",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.tecnocinetico");

    public static final KeyMapping SKILLS = new KeyMapping(
            "key.tecnocinetico.skills",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.tecnocinetico");

    private ModKeyBindings() {}

    @Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class Registration {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(RELOAD);
            event.register(SKILLS);
        }
    }

    @Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class Handler {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            while (SKILLS.consumeClick()) {
                if (mc.player != null && mc.screen == null) {
                    mc.setScreen(new SkillsScreen());
                }
            }
            while (RELOAD.consumeClick()) {
                if (mc.player == null) {
                    continue;
                }
                boolean holdingPistol = mc.player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof KineticPistolItem
                        || mc.player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof KineticPistolItem;
                boolean holdingMag = mc.player.getMainHandItem().getItem() instanceof com.bryanamaya.tecnocinetico.item.custom.MagazineItem
                        || mc.player.getOffhandItem().getItem() instanceof com.bryanamaya.tecnocinetico.item.custom.MagazineItem;
                if (holdingPistol) {
                    ModNetwork.CHANNEL.sendToServer(new ModNetwork.ReloadPistolPacket());
                } else if (holdingMag) {
                    // R llena el cargador con Bala Cinética; Mayús+R con Munición Guiada
                    ModNetwork.CHANNEL.sendToServer(new ModNetwork.FillMagazinePacket(mc.player.isShiftKeyDown()));
                }
            }
        }
    }
}
