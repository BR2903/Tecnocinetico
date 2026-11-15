package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.effect.ModEffects;
import com.bryanamaya.tecnocinetico.end.ClientEndData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Efectos de pantalla del End: el <b>temblor</b> del despertar del Dragón (sacude la cámara) y la <b>corrupción de
 * datos</b> (glitches magenta que parpadean en los bordes, textura de Codex {@code textures/misc/corruption_overlay.png}).
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID, value = Dist.CLIENT)
public final class EndClientEffects {

    private static final ResourceLocation OVERLAY = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/misc/corruption_overlay.png");

    private EndClientEffects() {}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ClientEndData.tick();
        }
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles event) {
        float q = ClientEndData.quake();
        if (q <= 0) {
            return;
        }
        double t = event.getPartialTick() + (Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.tickCount);
        event.setPitch(event.getPitch() + (float) (Math.sin(t * 2.7) * 1.4 * q));
        event.setYaw(event.getYaw() + (float) (Math.sin(t * 3.3 + 1.0) * 1.1 * q));
        event.setRoll(event.getRoll() + (float) (Math.sin(t * 1.9 + 2.0) * 1.8 * q));
    }

    @SubscribeEvent
    public static void overlay(RenderGuiOverlayEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (event.getOverlay() != VanillaGuiOverlay.VIGNETTE.type() || mc.player == null
                || !mc.player.hasEffect(ModEffects.DATA_CORRUPTION.get())) {
            return;
        }
        int w = event.getWindow().getGuiScaledWidth(), h = event.getWindow().getGuiScaledHeight();
        long time = mc.player.tickCount;
        float alpha = (time / 2) % 7 == 0 ? 0.35F : 0.75F;                    // parpadeo de glitch
        int shift = (int) ((time * 37) % 9) - 4;                              // salto horizontal
        RenderSystem.enableBlend();
        event.getGuiGraphics().setColor(1.0F, 1.0F, 1.0F, alpha);
        event.getGuiGraphics().blit(OVERLAY, shift, 0, 0, 0, w, h, w, h);
        event.getGuiGraphics().setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}
