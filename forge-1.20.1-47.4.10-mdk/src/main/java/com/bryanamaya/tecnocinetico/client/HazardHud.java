package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.nether.ClientHazardData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Barras de calor (naranja) y ceniza (gris verdosa) del Nether, verticales a la derecha de la barra rápida. Solo se
 * ven cuando tienen algo; parpadean al llegar a 85.
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID, value = Dist.CLIENT)
public final class HazardHud {

    private static final int H = 20, W = 4;

    private HazardHud() {}

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type() || mc.player == null || mc.options.hideGui
                || mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        int x = event.getWindow().getGuiScaledWidth() / 2 + 91 + 6;
        int y = event.getWindow().getGuiScaledHeight() - 2 - H;
        if (mc.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT) {
            x += 28;                                                                  // la ranura secundaria va a la derecha
        }
        long t = mc.level == null ? 0 : mc.level.getGameTime();
        x = bar(g, x, y, ClientHazardData.heat(), 0xFFFF7A1E, 0xFFFFD040, t);
        bar(g, x, y, ClientHazardData.ash(), 0xFF8A9474, 0xFFC8D0B0, t);
    }

    /** Dibuja una barra si tiene valor y devuelve la x de la siguiente. */
    private static int bar(GuiGraphics g, int x, int y, int value, int color, int blink, long t) {
        if (value <= 0) {
            return x;
        }
        g.fill(x - 1, y - 1, x + W + 1, y + H + 1, 0xC0101010);
        int fill = Math.max(1, Math.round(H * value / 100.0F));
        g.fill(x, y + H - fill, x + W, y + H, value >= 85 && (t / 8) % 2 == 0 ? blink : color);
        return x + W + 4;
    }
}
