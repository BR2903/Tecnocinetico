package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.ship.CoreFighterEntity;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import javax.annotation.Nullable;

/**
 * Pantalla de vuelo del Caza del Núcleo en primera persona (la nave no se dibuja: se ve a través de la cabina):
 * marco de la cabina, retícula, escalera de cabeceo que gira con el alabeo, brújula de rumbo, escalas de velocidad
 * y altitud, empuje, recuadro de fijado proyectado sobre el objetivo, estado del casco, misil y motor de salto.
 * Se ocultan la mira, la barra rápida y la experiencia de vanilla.
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID, value = Dist.CLIENT)
public final class ShipHud {

    private static final int C = 0xCC46E3F5, C_DIM = 0x6646E3F5, WARN = 0xEEFF5A40, LOCK = 0xEEFF4030, JUMP = 0xDDB48CFF;
    private static final Matrix4f VIEW = new Matrix4f(), PROJ = new Matrix4f();
    private static Vec3 camPos = Vec3.ZERO;

    private ShipHud() {}

    @Nullable
    static CoreFighterEntity pilotedFirstPerson() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getVehicle() instanceof CoreFighterEntity s
                && mc.options.getCameraType().isFirstPerson() ? s : null;
    }

    /** Guarda las matrices de la cámara para proyectar el objetivo a la pantalla. */
    @SubscribeEvent
    public static void onLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            VIEW.set(event.getPoseStack().last().pose());
            PROJ.set(event.getProjectionMatrix());
            camPos = event.getCamera().getPosition();
        }
    }

    /** Punto del mundo -> píxel de la interfaz (null si queda detrás). */
    @Nullable
    private static float[] project(Vec3 p, int w, int h) {
        Vector4f v = new Vector4f((float) (p.x - camPos.x), (float) (p.y - camPos.y), (float) (p.z - camPos.z), 1.0F);
        v.mul(VIEW);
        v.mul(PROJ);
        if (v.w <= 0.05F) {
            return null;
        }
        return new float[]{(v.x / v.w + 1.0F) * 0.5F * w, (1.0F - v.y / v.w) * 0.5F * h};
    }

    @SubscribeEvent
    public static void hideVanilla(RenderGuiOverlayEvent.Pre event) {
        if (pilotedFirstPerson() == null) {
            return;
        }
        var id = event.getOverlay().id();
        if (id.equals(VanillaGuiOverlay.CROSSHAIR.id()) || id.equals(VanillaGuiOverlay.HOTBAR.id())
                || id.equals(VanillaGuiOverlay.EXPERIENCE_BAR.id()) || id.equals(VanillaGuiOverlay.ITEM_NAME.id())) {
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ dibujo

    private static void hLine(GuiGraphics g, int x0, int x1, int y, int c) {
        g.fill(Math.min(x0, x1), y, Math.max(x0, x1), y + 1, c);
    }

    private static void vLine(GuiGraphics g, int x, int y0, int y1, int c) {
        g.fill(x, Math.min(y0, y1), x + 1, Math.max(y0, y1), c);
    }

    private static void corners(GuiGraphics g, int x0, int y0, int x1, int y1, int len, int c) {
        hLine(g, x0, x0 + len, y0, c); vLine(g, x0, y0, y0 + len, c);
        hLine(g, x1 - len, x1, y0, c); vLine(g, x1 - 1, y0, y0 + len, c);
        hLine(g, x0, x0 + len, y1 - 1, c); vLine(g, x0, y1 - len, y1, c);
        hLine(g, x1 - len, x1, y1 - 1, c); vLine(g, x1 - 1, y1 - len, y1, c);
    }

    public static void draw(GuiGraphics g, CoreFighterEntity ship, float pt, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int cx = w / 2, cy = h / 2;
        float t = (mc.player.tickCount + pt);

        // marco de la cabina: viñeta oscura y montantes
        for (int i = 0; i < 18; i++) {
            int a = (int) (90 * (1 - i / 18.0F));
            g.fill(0, i, w, i + 1, a << 24);
            g.fill(0, h - 1 - i, w, h - i, a << 24);
        }
        for (int i = 0; i < 26; i++) {
            int a = (int) (110 * (1 - i / 26.0F));
            g.fill(i, 0, i + 1, h, a << 24);
            g.fill(w - 1 - i, 0, w - i, h, a << 24);
        }
        g.fill(cx - 1, 0, cx + 1, 10, 0xAA0A1418);                                  // montante central de la cúpula
        corners(g, 30, 22, w - 30, h - 22, 14, C_DIM);

        // escalera de cabeceo (gira con el alabeo de la nave)
        float pitch = mc.player.getViewXRot(pt);                                    // la cámara es la del piloto
        float roll = -Mth.lerp(pt, ship.rollO, ship.roll) * 0.6F;                    // inclinación de la nave al girar
        float pxPerDeg = (h * 0.5F) * PROJ.m11() * (float) Math.tan(Math.toRadians(1.0));
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(roll));
        for (int deg = -60; deg <= 60; deg += 10) {
            int y = Math.round(-(deg + pitch) * pxPerDeg);                           // mirar arriba (pitch < 0) baja el horizonte
            if (Math.abs(y) > h / 2 - 30) {
                continue;
            }
            int half = deg == 0 ? 70 : 34;
            int gap = 14;
            int col = deg == 0 ? C : C_DIM;
            if (deg < 0) {                                                            // por debajo del horizonte: discontinua
                for (int x = gap; x < half; x += 6) {
                    hLine(g, x, x + 3, y, col);
                    hLine(g, -x - 3, -x, y, col);
                }
            } else {
                hLine(g, gap, half, y, col);
                hLine(g, -half, -gap, y, col);
            }
            if (deg != 0) {
                vLine(g, half - 1, y, y + (deg > 0 ? 4 : -4), col);
                vLine(g, -half, y, y + (deg > 0 ? 4 : -4), col);
                String s = String.valueOf(Math.abs(deg));
                g.drawString(font, s, half + 3, y - 4, col, false);
                g.drawString(font, s, -half - 3 - font.width(s), y - 4, col, false);
            }
        }
        g.pose().popPose();

        // retícula central (vector de vuelo)
        hLine(g, cx - 12, cx - 4, cy, C);
        hLine(g, cx + 4, cx + 12, cy, C);
        vLine(g, cx, cy - 8, cy - 3, C);
        g.fill(cx - 1, cy - 1, cx + 1, cy + 1, C);

        // brújula de rumbo (arriba)
        float yaw = Mth.wrapDegrees(mc.player.getViewYRot(pt) + 180.0F);
        float heading = (yaw + 360.0F) % 360.0F;
        int tapeW = 160, ty = 26;
        g.fill(cx - tapeW / 2 - 2, ty - 2, cx + tapeW / 2 + 2, ty + 18, 0x55000000);
        for (int d = -40; d <= 40; d++) {
            int hd = Math.floorMod(Math.round(heading) + d, 360);
            int x = cx + Math.round((hd - heading + (hd - heading > 180 ? -360 : hd - heading < -180 ? 360 : 0)) * 2.0F);
            if (x < cx - tapeW / 2 || x > cx + tapeW / 2) {
                continue;
            }
            if (hd % 10 == 0) {
                vLine(g, x, ty, ty + (hd % 30 == 0 ? 6 : 4), C);
                if (hd % 30 == 0) {
                    String s = switch (hd) {
                        case 0 -> "N";
                        case 90 -> "E";
                        case 180 -> "S";
                        case 270 -> "O";
                        default -> String.valueOf(hd / 10);
                    };
                    g.drawCenteredString(font, s, x, ty + 8, C);
                }
            }
        }
        g.fill(cx - 1, ty - 4, cx + 1, ty + 1, 0xFFFFFFFF);                         // marcador de rumbo
        g.drawCenteredString(font, String.format("%03d", Math.round(heading) % 360), cx, ty - 12, C);

        // escalas de velocidad (izquierda) y altitud (derecha)
        double v = Math.sqrt(Math.pow(ship.getX() - ship.xo, 2) + Math.pow(ship.getY() - ship.yo, 2) + Math.pow(ship.getZ() - ship.zo, 2));
        int kmh = (int) Math.round(v * 20 * 3.6);
        int alt = (int) Math.floor(ship.getY());
        int sx = cx - 120, ax = cx + 120;
        box(g, font, sx - 22, cy - 7, 44, Component.literal(kmh + " km/h"), C);
        box(g, font, ax - 22, cy - 7, 44, Component.literal("Y " + alt), C);
        for (int i = -4; i <= 4; i++) {                                             // marcas que se deslizan
            int off = (int) ((t * v * 6) % 12);
            hLine(g, sx - 30, sx - 24, cy + i * 12 + off - 6, C_DIM);
            int aoff = Math.floorMod((int) Math.round(ship.getY() * 4), 12);
            hLine(g, ax + 24, ax + 30, cy + i * 12 + aoff - 6, C_DIM);
        }
        // empuje (barra vertical a la izquierda)
        float throttle = (float) Math.min(1.0, v / CoreFighterEntity.MAX_SPEED);
        int bx = sx - 40, by0 = cy - 40, bh = 80;
        g.fill(bx, by0, bx + 4, by0 + bh, 0x55000000);
        g.fill(bx, by0 + bh - Math.round(bh * throttle), bx + 4, by0 + bh, C);
        g.drawString(font, "EMP", bx - 4, by0 + bh + 3, C_DIM, false);

        // objetivo fijado: recuadro proyectado sobre él
        LivingEntity target = ship.lockTarget(mc.player);
        if (target != null) {
            Vec3 tp = target.getPosition(pt).add(0, target.getBbHeight() * 0.5, 0);
            float[] s = project(tp, w, h);
            if (s != null) {
                int size = Mth.clamp((int) (220 / Math.max(4, target.distanceTo(mc.player)) * target.getBbHeight()), 8, 40);
                int x = Math.round(s[0]), y = Math.round(s[1]);
                boolean ready = ship.missileCooldown() <= 0;
                int col = ready && ((int) t / 3) % 2 == 0 ? 0xFFFFFFFF : LOCK;
                corners(g, x - size, y - size, x + size, y + size, 5, col);
                g.drawString(font, Component.literal((int) target.distanceTo(mc.player) + " m"), x + size + 3, y - size, LOCK, false);
                g.drawString(font, target.getDisplayName(), x + size + 3, y - size + 10, LOCK, false);
                if (ready) {
                    g.drawCenteredString(font, Component.translatable("hud.tecnocinetico.ship_lock"), x, y + size + 3, LOCK);
                }
            }
        }

        // estado (abajo): casco, misil, salto
        int hull = Math.round(ship.health() / CoreFighterEntity.MAX_HEALTH * 100);
        int hullCol = hull > 30 ? C : ((int) t / 4) % 2 == 0 ? WARN : 0x66FF5A40;
        int yb = h - 72;                                                             // por encima de los corazones
        g.drawString(font, Component.translatable("hud.tecnocinetico.ship_hull", hull), cx - 110, yb, hullCol, false);
        g.fill(cx - 110, yb + 10, cx - 30, yb + 13, 0x55000000);
        g.fill(cx - 110, yb + 10, cx - 110 + Math.round(80 * hull / 100.0F), yb + 13, hullCol);
        boolean missile = ship.missileCooldown() <= 0;
        g.drawString(font, Component.translatable(missile ? "hud.tecnocinetico.ship_missile_ready" : "hud.tecnocinetico.ship_missile_wait"),
                cx + 30, yb, missile ? C : C_DIM, false);
        int cd = Math.round(80 * (1 - ship.missileCooldown() / (float) CoreFighterEntity.MISSILE_COOLDOWN));
        g.fill(cx + 30, yb + 10, cx + 110, yb + 13, 0x55000000);
        g.fill(cx + 30, yb + 10, cx + 30 + cd, yb + 13, missile ? C : C_DIM);
        int j = ship.jumpTicks();
        if (j > 0) {
            int bw = 160, filled = bw * j / CoreFighterEntity.JUMP_TICKS;
            g.fill(cx - bw / 2 - 1, cy + 40, cx + bw / 2 + 1, cy + 46, 0xAA000000);
            g.fill(cx - bw / 2, cy + 41, cx - bw / 2 + filled, cy + 45, JUMP);
            g.drawCenteredString(font, Component.translatable("hud.tecnocinetico.jump"), cx, cy + 50, JUMP);
        } else {
            g.drawCenteredString(font, Component.translatable("hud.tecnocinetico.ship_keys"), cx, h - 20, C_DIM);
        }
    }

    private static void box(GuiGraphics g, Font font, int x, int y, int w, Component text, int c) {
        g.fill(x, y, x + w, y + 14, 0x66000000);
        hLine(g, x, x + w, y, c);
        hLine(g, x, x + w, y + 13, c);
        vLine(g, x, y, y + 14, c);
        vLine(g, x + w - 1, y, y + 14, c);
        g.drawCenteredString(font, text, x + w / 2, y + 3, c);
    }
}
