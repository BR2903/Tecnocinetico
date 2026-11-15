package com.bryanamaya.tecnocinetico.client.map;

import com.bryanamaya.tecnocinetico.map.MapCatalog;
import com.bryanamaya.tecnocinetico.map.MapPackets;
import com.bryanamaya.tecnocinetico.map.NavigationData;
import com.bryanamaya.tecnocinetico.network.ModNetwork;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla del Reloj de Navegación con aspecto de terminal táctico (ámbar sobre grafito, como la pantalla del reloj):
 * el terreno explorado ({@link MapExplorer}) bajo una cuadrícula con coordenadas, líneas de barrido, un barrido de radar,
 * viñeta y marco con esquinas; encima las estructuras (rombos), los marcadores (chinchetas), los jugadores y el objetivo
 * fijado. Arrastrar mueve, la rueda acerca/aleja, clic derecho pone un marcador (o edita uno propio). A la derecha:
 * localizador de estructuras, marcadores y estado del enlace.
 */
public class WristMapScreen extends Screen {

    private static final float[] ZOOMS = {0.25F, 0.5F, 1.0F, 2.0F, 4.0F, 8.0F};
    private static final int[] COLORS = {0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x55FFFF, 0x5599FF, 0xFF55FF, 0xFFFFFF};
    private static final int PANEL = 150, PAD = 8, TOP = 24, BOTTOM = 22, ROW = 12;
    // paleta: ámbar de pantalla sobre grafito
    static final int BG = 0xF206080A, PANEL_BG = 0xE00E1215, AMBER = 0xFFF0B030, AMBER_DIM = 0xFF8A6418, AMBER_LOW = 0xFF4A3A1A,
            TEXT = 0xFFEBDDB8, DIM = 0xFF9A8A6A, ALERT = 0xFFFF5A40, OK = 0xFF7CD860, VOID = 0xFF07090B;

    private double cx, cz;
    private int zoom = 2;
    private boolean dragging;
    private List<MapCatalog.Entry> entries = List.of();
    private int entry;
    @Nullable
    private NavigationData.Found target;
    private long targetTime;
    private final long opened = System.currentTimeMillis();

    // editor de marcadores
    private boolean editing;
    @Nullable
    private NavigationData.Marker editingMarker;
    private int editX, editY, editZ, editColor;
    private boolean editShared;
    private EditBox nameBox;
    private final List<net.minecraft.client.gui.components.AbstractWidget> editorWidgets = new ArrayList<>();
    private int markerScroll;

    public WristMapScreen() {
        super(Component.translatable("map.tecnocinetico.title"));
    }

    // ------------------------------------------------------------------ botón con el estilo del terminal

    /** Botón plano con contorno ámbar (los vanilla desentonan con el terminal). */
    static class TechButton extends Button {
        TechButton(int x, int y, int w, int h, Component msg, OnPress press) {
            super(x, y, w, h, msg, press, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
            boolean hot = isHoveredOrFocused() && active;
            int x0 = getX(), y0 = getY(), x1 = x0 + width, y1 = y0 + height;
            g.fill(x0, y0, x1, y1, !active ? 0xC0101214 : hot ? 0xF0322610 : 0xE0161A1E);
            outline(g, x0, y0, x1, y1, !active ? AMBER_LOW : hot ? AMBER : AMBER_DIM);
            g.fill(x0 + 1, y1 - 2, x1 - 1, y1 - 1, hot ? 0x60F0B030 : 0x30F0B030);           // línea de luz inferior
            var font = Minecraft.getInstance().font;
            String text = font.plainSubstrByWidth(getMessage().getString(), width - 4);
            g.drawString(font, text, x0 + (width - font.width(text)) / 2, y0 + (height - 8) / 2, !active ? DIM : hot ? 0xFFFFE6B0 : AMBER, false);
        }
    }

    private Button button(int x, int y, int w, int h, Component msg, Button.OnPress press) {
        return new TechButton(x, y, w, h, msg, press);
    }

    // ------------------------------------------------------------------ coordenadas

    private Player player() {
        return Minecraft.getInstance().player;
    }

    private String dimId() {
        return player().level().dimension().location().toString();
    }

    private float scale() {
        return ZOOMS[zoom];
    }

    private int mapX0() {
        return PAD;
    }

    private int mapX1() {
        return width - PANEL - PAD;
    }

    private int mapY0() {
        return TOP;
    }

    private int mapY1() {
        return height - BOTTOM;
    }

    private double toScreenX(double wx) {
        return (mapX0() + mapX1()) / 2.0 + (wx - cx) * scale();
    }

    private double toScreenY(double wz) {
        return (mapY0() + mapY1()) / 2.0 + (wz - cz) * scale();
    }

    private double toWorldX(double sx) {
        return cx + (sx - (mapX0() + mapX1()) / 2.0) / scale();
    }

    private double toWorldZ(double sy) {
        return cz + (sy - (mapY0() + mapY1()) / 2.0) / scale();
    }

    private boolean inMap(double mx, double my) {
        return mx >= mapX0() && mx < mapX1() && my >= mapY0() && my < mapY1();
    }

    private long now() {
        return System.currentTimeMillis();
    }

    @Override
    protected void init() {
        if (cx == 0 && cz == 0) {
            cx = player().getX();
            cz = player().getZ();
        }
        entries = MapCatalog.in(player().level().dimension());
        entry = Mth.clamp(entry, 0, Math.max(0, entries.size() - 1));
        int px = width - PANEL;
        addRenderableWidget(button(px + 2, TOP + 16, 14, 14, Component.literal("<"),
                b -> entry = Math.floorMod(entry - 1, Math.max(1, entries.size()))));
        addRenderableWidget(button(width - PAD - 14, TOP + 16, 14, 14, Component.literal(">"),
                b -> entry = Math.floorMod(entry + 1, Math.max(1, entries.size()))));
        addRenderableWidget(button(px + 2, TOP + 34, PANEL - PAD - 2, 16, Component.translatable("map.tecnocinetico.search_button"), b -> {
            if (!entries.isEmpty()) {
                ModNetwork.CHANNEL.sendToServer(MapPackets.Action.locate(entries.get(entry).key()));
            }
        }));
        addRenderableWidget(button(px + 2, TOP + 54, PANEL - PAD - 2, 16, Component.translatable("map.tecnocinetico.center"), b -> {
            cx = player().getX();
            cz = player().getZ();
        }));
        // editor (oculto hasta que se abre)
        editorWidgets.clear();
        int ex = width / 2 - 90, ey = height / 2 - 44;
        nameBox = new EditBox(font, ex + 9, ey + 26, 162, 12, Component.translatable("map.tecnocinetico.marker.new"));
        nameBox.setBordered(false);
        nameBox.setTextColor(TEXT);
        nameBox.setMaxLength(NavigationData.MAX_NAME);
        editorWidgets.add(nameBox);
        editorWidgets.add(button(ex + 6, ey + 44, 20, 16, Component.empty(), b -> editColor = (editColor + 1) % COLORS.length));
        editorWidgets.add(button(ex + 30, ey + 44, 144, 16, Component.empty(), b -> editShared = !editShared));
        editorWidgets.add(button(ex + 6, ey + 66, 54, 16, Component.translatable("map.tecnocinetico.marker.save"), b -> saveMarker()));
        editorWidgets.add(button(ex + 63, ey + 66, 54, 16, Component.translatable("map.tecnocinetico.marker.delete"), b -> deleteMarker()));
        editorWidgets.add(button(ex + 120, ey + 66, 54, 16, Component.translatable("map.tecnocinetico.marker.cancel"), b -> closeEditor()));
        editorWidgets.forEach(this::addRenderableWidget);
        showEditor(editing);
    }

    private void showEditor(boolean on) {
        editing = on;
        editorWidgets.forEach(w -> w.visible = on);
        if (editorWidgets.size() > 4) {
            editorWidgets.get(4).active = editingMarker != null;                    // borrar: solo uno existente
            editorWidgets.get(3).active = editingMarker == null;                    // guardar: solo uno nuevo
        }
        if (on) {
            setFocused(nameBox);
        }
    }

    private void openEditor(@Nullable NavigationData.Marker existing, int x, int y, int z) {
        editingMarker = existing;
        editX = x;
        editY = y;
        editZ = z;
        editColor = 0;
        editShared = false;
        if (existing != null) {
            nameBox.setValue(existing.name());
            editShared = existing.shared();
            for (int i = 0; i < COLORS.length; i++) {
                if (COLORS[i] == existing.color()) {
                    editColor = i;
                }
            }
        } else {
            nameBox.setValue("");
        }
        showEditor(true);
    }

    private void closeEditor() {
        editingMarker = null;
        showEditor(false);
    }

    private void saveMarker() {
        ModNetwork.CHANNEL.sendToServer(MapPackets.Action.add(nameBox.getValue(), editX, editY, editZ, COLORS[editColor], editShared));
        closeEditor();
    }

    private void deleteMarker() {
        if (editingMarker != null) {
            ModNetwork.CHANNEL.sendToServer(MapPackets.Action.remove(editingMarker.id()));
        }
        closeEditor();
    }

    public void onLocated(MapPackets.LocateResult r) {
        if (r.found()) {
            cx = r.x();
            cz = r.z();
            target = new NavigationData.Found(r.key(), dimId(), r.x(), r.z());
            targetTime = now();
        }
    }

    // ------------------------------------------------------------------ dibujo: utilidades

    static void outline(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        g.fill(x0, y0, x1, y0 + 1, color);
        g.fill(x0, y1 - 1, x1, y1, color);
        g.fill(x0, y0, x0 + 1, y1, color);
        g.fill(x1 - 1, y0, x1, y1, color);
    }

    /** Esquinas en L (marco de visor). */
    private static void brackets(GuiGraphics g, int x0, int y0, int x1, int y1, int len, int color) {
        g.fill(x0, y0, x0 + len, y0 + 2, color);
        g.fill(x0, y0, x0 + 2, y0 + len, color);
        g.fill(x1 - len, y0, x1, y0 + 2, color);
        g.fill(x1 - 2, y0, x1, y0 + len, color);
        g.fill(x0, y1 - 2, x0 + len, y1, color);
        g.fill(x0, y1 - len, x0 + 2, y1, color);
        g.fill(x1 - len, y1 - 2, x1, y1, color);
        g.fill(x1 - 2, y1 - len, x1, y1, color);
    }

    /** Recuadro de sección del panel: fondo, contorno y cabecera con su título. */
    private void section(GuiGraphics g, int x0, int y0, int x1, int y1, Component title) {
        g.fill(x0, y0, x1, y1, PANEL_BG);
        outline(g, x0, y0, x1, y1, AMBER_LOW);
        g.fill(x0, y0, x1, y0 + 11, 0xC0241C0C);
        g.fill(x0, y0, x0 + 3, y0 + 11, AMBER);
        g.drawString(font, title.getString().toUpperCase(), x0 + 6, y0 + 2, AMBER, false);
    }

    private void small(GuiGraphics g, String text, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.75F, 0.75F, 1);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private static int alpha(int color, float a) {
        return (Mth.clamp((int) (a * 255), 0, 255) << 24) | (color & 0xFFFFFF);
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fill(0, 0, width, height, BG);
        renderHeader(g);
        renderMap(g, mouseX, mouseY);
        renderPanel(g, mouseX, mouseY);
        if (editing) {
            int ex = width / 2 - 90, ey = height / 2 - 44;
            g.fill(0, 0, width, height, 0x90000000);
            g.fill(ex, ey, ex + 180, ey + 88, 0xF60C0F12);
            outline(g, ex, ey, ex + 180, ey + 88, AMBER_DIM);
            brackets(g, ex - 2, ey - 2, ex + 182, ey + 90, 8, AMBER);
            g.fill(ex, ey, ex + 180, ey + 12, 0xD0241C0C);
            g.drawString(font, Component.translatable(editingMarker == null ? "map.tecnocinetico.marker.new" : "map.tecnocinetico.markers")
                    .getString().toUpperCase(), ex + 6, ey + 2, AMBER, false);
            small(g, "X " + editX + "  Z " + editZ, ex + 120, ey + 3, DIM);
            g.fill(ex + 6, ey + 22, ex + 174, ey + 38, 0xFF050607);                   // campo del nombre
            outline(g, ex + 6, ey + 22, ex + 174, ey + 38, getFocused() == nameBox ? AMBER : AMBER_DIM);
            editorWidgets.get(2).setMessage(Component.translatable(editShared ? "map.tecnocinetico.marker.shared" : "map.tecnocinetico.marker.private"));
        }
        super.render(g, mouseX, mouseY, partial);
        if (editing) {
            int ex = width / 2 - 90, ey = height / 2 - 44;
            g.fill(ex + 11, ey + 48, ex + 21, ey + 56, 0xFF000000 | COLORS[editColor]);
        }
    }

    /** Barra superior: título, enlace satelital (punto que parpadea), dimensión y hora del mundo. */
    private void renderHeader(GuiGraphics g) {
        g.fill(0, 0, width, 17, 0xF0101418);
        g.fill(0, 17, width, 18, AMBER_DIM);
        for (int x = 0; x < width; x += 6) {                                         // remaches de la barra
            g.fill(x, 15, x + 3, 16, 0x30F0B030);
        }
        g.fill(PAD, 4, PAD + 3, 13, AMBER);
        g.drawString(font, title.getString().toUpperCase(), PAD + 7, 5, AMBER, false);
        boolean blink = (now() / 500) % 2 == 0;
        String link = Component.translatable("map.tecnocinetico.link").getString().toUpperCase();
        int lx = (width - font.width(link)) / 2;
        g.fill(lx - 10, 7, lx - 5, 12, blink ? OK : 0xFF2C4A24);
        g.drawString(font, link, lx, 5, OK, false);
        long day = player().level().getDayTime() % 24000L;
        int hours = (int) ((day / 1000 + 6) % 24), minutes = (int) (day % 1000 * 60 / 1000);
        String dim = Component.translatable("map.tecnocinetico.dim." + player().level().dimension().location().getPath()).getString().toUpperCase();
        String right = dim + "  " + String.format("%02d:%02d", hours, minutes);
        g.drawString(font, right, width - PAD - font.width(right), 5, TEXT, false);
    }

    private void renderMap(GuiGraphics g, int mouseX, int mouseY) {
        int x0 = mapX0(), y0 = mapY0(), x1 = mapX1(), y1 = mapY1();
        g.fill(x0 - 3, y0 - 3, x1 + 3, y1 + 3, 0xFF0B0E11);
        outline(g, x0 - 3, y0 - 3, x1 + 3, y1 + 3, AMBER_LOW);
        outline(g, x0 - 1, y0 - 1, x1 + 1, y1 + 1, AMBER_DIM);
        g.fill(x0, y0, x1, y1, VOID);
        g.enableScissor(x0, y0, x1, y1);
        String dim = dimId();
        float s = scale();
        // terreno explorado
        int rx0 = Math.floorDiv((int) Math.floor(toWorldX(x0)), MapExplorer.REGION_SIZE), rx1 = Math.floorDiv((int) Math.ceil(toWorldX(x1)), MapExplorer.REGION_SIZE);
        int rz0 = Math.floorDiv((int) Math.floor(toWorldZ(y0)), MapExplorer.REGION_SIZE), rz1 = Math.floorDiv((int) Math.ceil(toWorldZ(y1)), MapExplorer.REGION_SIZE);
        for (int rx = rx0; rx <= rx1; rx++) {
            for (int rz = rz0; rz <= rz1; rz++) {
                var id = MapExplorer.region(dim, rx, rz);
                if (id == null) {
                    continue;
                }
                g.pose().pushPose();
                g.pose().translate(toScreenX(rx * (double) MapExplorer.REGION_SIZE), toScreenY(rz * (double) MapExplorer.REGION_SIZE), 0);
                g.pose().scale(s, s, 1);
                g.blit(id, 0, 0, 0, 0, MapExplorer.REGION_SIZE, MapExplorer.REGION_SIZE, MapExplorer.REGION_SIZE, MapExplorer.REGION_SIZE);
                g.pose().popPose();
            }
        }
        // capa de pantalla: tinte cálido, líneas de barrido y cuadrícula con coordenadas
        g.fill(x0, y0, x1, y1, 0x1CF0B030);
        for (int y = y0; y < y1; y += 3) {
            g.fill(x0, y, x1, y + 1, 0x26000000);
        }
        int step = 16;
        while (step * s < 48) {
            step *= 2;
        }
        int gx0 = Math.floorDiv((int) toWorldX(x0), step) * step, gz0 = Math.floorDiv((int) toWorldZ(y0), step) * step;
        for (int wx = gx0; toScreenX(wx) < x1; wx += step) {
            int sx = (int) toScreenX(wx);
            boolean major = Math.floorMod(wx, step * 4) == 0;
            g.fill(sx, y0, sx + 1, y1, major ? 0x50F0B030 : 0x22F0B030);
            if (major || step * s >= 80) {
                small(g, String.valueOf(wx), sx + 2, y0 + 2, major ? AMBER_DIM : AMBER_LOW);
            }
        }
        for (int wz = gz0; toScreenY(wz) < y1; wz += step) {
            int sy = (int) toScreenY(wz);
            boolean major = Math.floorMod(wz, step * 4) == 0;
            g.fill(x0, sy, x1, sy + 1, major ? 0x50F0B030 : 0x22F0B030);
            if (major || step * s >= 80) {
                small(g, String.valueOf(wz), x0 + 2, sy + 2, major ? AMBER_DIM : AMBER_LOW);
            }
        }
        // barrido de radar que baja cada 4 s
        int h = y1 - y0;
        int sweep = y0 + (int) ((now() % 4000L) * h / 4000L);
        g.fillGradient(x0, sweep - 28, x1, sweep, 0x00F0B030, 0x24F0B030);
        g.fill(x0, sweep, x1, sweep + 1, 0x70F0B030);
        // viñeta
        g.fillGradient(x0, y0, x1, y0 + 18, 0xB0000000, 0x00000000);
        g.fillGradient(x0, y1 - 18, x1, y1, 0x00000000, 0xB0000000);
        for (int i = 0; i < 9; i++) {
            int a = (int) (0xA0 * (1 - i / 9.0)) << 24;
            g.fill(x0 + i * 2, y0, x0 + i * 2 + 2, y1, a);
            g.fill(x1 - i * 2 - 2, y0, x1 - i * 2, y1, a);
        }
        Component hover = null;
        // estructuras detectadas: rombos con su color
        for (NavigationData.Found f : ClientMapState.found) {
            if (!f.dim().equals(dim)) {
                continue;
            }
            MapCatalog.Entry e = MapCatalog.byKey(f.key());
            int sx = (int) toScreenX(f.x() + 0.5), sy = (int) toScreenY(f.z() + 0.5);
            int color = 0xFF000000 | (e == null ? 0xAAAAAA : e.color());
            boolean over = Math.abs(mouseX - sx) <= 5 && Math.abs(mouseY - sy) <= 5;
            diamond(g, sx, sy, over ? 6 : 5, color);
            if (e != null && (over || s >= 2.0F)) {
                small(g, Component.translatable(e.langKey()).getString().toUpperCase(), sx + 8, sy - 3, over ? TEXT : DIM);
            }
            if (over && e != null) {
                hover = Component.translatable(e.langKey()).append(" · ").append(distance(f.x(), f.z()));
            }
        }
        // objetivo de la última búsqueda: fijación que se cierra y parpadea
        if (target != null && target.dim().equals(dim) && now() - targetTime < 10000) {
            int sx = (int) toScreenX(target.x() + 0.5), sy = (int) toScreenY(target.z() + 0.5);
            float t = Math.min(1.0F, (now() - targetTime) / 600.0F);
            int r = (int) Mth.lerp(t, 40, 10) + (int) ((now() / 150) % 3);
            brackets(g, sx - r, sy - r, sx + r + 1, sy + r + 1, 5, (now() / 250) % 2 == 0 ? AMBER : ALERT);
            g.fill(sx - r - 6, sy, sx - r - 1, sy + 1, AMBER);
            g.fill(sx + r + 2, sy, sx + r + 7, sy + 1, AMBER);
            g.fill(sx, sy - r - 6, sx + 1, sy - r - 1, AMBER);
            g.fill(sx, sy + r + 2, sx + 1, sy + r + 7, AMBER);
            small(g, Component.translatable("map.tecnocinetico.target").getString().toUpperCase() + " · " + distance(target.x(), target.z()).getString(),
                    sx + r + 4, sy + r + 2, ALERT);
        }
        // marcadores: chinchetas
        for (NavigationData.Marker m : ClientMapState.markers) {
            if (!m.dim().equals(dim)) {
                continue;
            }
            int sx = (int) toScreenX(m.x() + 0.5), sy = (int) toScreenY(m.z() + 0.5);
            int color = 0xFF000000 | m.color();
            g.fill(sx, sy - 5, sx + 1, sy + 1, 0xFF000000);                          // aguja
            diamond(g, sx, sy - 7, 4, color);
            g.fill(sx - 1, sy, sx + 2, sy + 1, color);
            boolean over = Math.abs(mouseX - sx) <= 5 && mouseY >= sy - 12 && mouseY <= sy + 2;
            if (s >= 1.0F || over) {
                small(g, m.name(), sx + 6, sy - 10, color);
            }
            if (over) {
                hover = Component.literal(m.name()).append(" · ").append(distance(m.x(), m.z()))
                        .append(m.shared() ? Component.literal(" · ").append(Component.translatable("map.tecnocinetico.marker.by", m.ownerName())) : Component.empty());
            }
        }
        // otros jugadores
        for (Player other : player().level().players()) {
            if (other != player()) {
                int sx = (int) toScreenX(other.getX()), sy = (int) toScreenY(other.getZ());
                g.fill(sx - 3, sy - 3, sx + 4, sy + 4, 0xFF000000);
                g.fill(sx - 2, sy - 2, sx + 3, sy + 3, 0xFF6FB0FF);
                if (Math.abs(mouseX - sx) <= 3 && Math.abs(mouseY - sy) <= 3) {
                    hover = other.getDisplayName();
                }
            }
        }
        // el jugador: pulso de radar y flecha
        int px = (int) toScreenX(player().getX()), py = (int) toScreenY(player().getZ());
        float ping = (now() % 1500L) / 1500.0F;
        int pr = 3 + (int) (ping * 18);
        outline(g, px - pr, py - pr, px + pr + 1, py + pr + 1, alpha(AMBER, 0.8F * (1 - ping)));
        g.pose().pushPose();
        g.pose().translate(px, py, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(player().getYRot() + 180.0F));
        g.fill(-1, -7, 2, 5, 0xFF000000);                                            // flecha (apunta a -Y antes de girar)
        g.fill(-4, -2, 5, 5, 0xFF000000);
        g.fill(0, -6, 1, 4, 0xFFFFF0C8);
        g.fill(-3, -1, 4, 4, AMBER);
        g.fill(-1, -1, 2, 4, 0xFFFFF0C8);
        g.pose().popPose();
        g.disableScissor();
        // marco: esquinas, marcas en los bordes y brújula
        brackets(g, x0 - 3, y0 - 3, x1 + 3, y1 + 3, 12, AMBER);
        for (int x = x0 + 16; x < x1 - 8; x += 16) {
            g.fill(x, y0 - 3, x + 1, y0, AMBER_DIM);
            g.fill(x, y1, x + 1, y1 + 3, AMBER_DIM);
        }
        for (int y = y0 + 16; y < y1 - 8; y += 16) {
            g.fill(x0 - 3, y, x0, y + 1, AMBER_DIM);
            g.fill(x1, y, x1 + 3, y + 1, AMBER_DIM);
        }
        int mx = (x0 + x1) / 2, my = (y0 + y1) / 2;
        compass(g, "N", mx, y0 + 3);
        compass(g, "S", mx, y1 - 11);
        compass(g, "O", x0 + 6, my - 4);
        compass(g, "E", x1 - 10, my - 4);
        // pie: escala, coordenadas bajo el ratón y zoom
        int bar = (int) Math.max(8, step * s);
        g.fill(x0, y1 + 8, x0 + bar, y1 + 9, AMBER);
        g.fill(x0, y1 + 5, x0 + 1, y1 + 12, AMBER);
        g.fill(x0 + bar - 1, y1 + 5, x0 + bar, y1 + 12, AMBER);
        small(g, step + " m", x0 + bar + 4, y1 + 7, AMBER_DIM);
        if (inMap(mouseX, mouseY)) {
            String coords = "X " + (int) Math.floor(toWorldX(mouseX)) + "   Z " + (int) Math.floor(toWorldZ(mouseY));
            g.drawString(font, coords, (x0 + x1 - font.width(coords)) / 2, y1 + 7, TEXT, false);
        }
        String z = "ZOOM ×" + (s < 1 ? String.valueOf(s) : String.valueOf((int) s));
        g.drawString(font, z, x1 - font.width(z), y1 + 7, AMBER_DIM, false);
        if (hover != null && !editing) {
            g.renderTooltip(font, hover, mouseX, mouseY);
        }
    }

    private void compass(GuiGraphics g, String letter, int x, int y) {
        g.fill(x - 5, y - 2, x + 6, y + 10, 0xC0000000);
        g.drawString(font, letter, x - font.width(letter) / 2 + 1, y, AMBER, false);
    }

    private static void diamond(GuiGraphics g, int x, int y, int r, int color) {
        g.pose().pushPose();
        g.pose().translate(x + 0.5F, y + 0.5F, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(45));
        int k = Math.max(2, (int) (r * 0.75F));
        g.fill(-k - 1, -k - 1, k + 1, k + 1, 0xFF000000);
        g.fill(-k, -k, k, k, color);
        g.fill(-k + 2, -k + 2, k - 2, k - 2, (color & 0xFFFFFF) | 0x60000000);
        g.pose().popPose();
    }

    private void renderPanel(GuiGraphics g, int mouseX, int mouseY) {
        int px = width - PANEL, x1 = width - PAD;
        // localizador
        section(g, px, TOP, x1, TOP + 74, Component.translatable("map.tecnocinetico.search"));
        Component name = entries.isEmpty() ? Component.translatable("map.tecnocinetico.none_here")
                : Component.translatable(entries.get(entry).langKey());
        int w = PANEL - PAD - 34;
        String shown = font.plainSubstrByWidth(name.getString(), w);
        g.fill(px + 18, TOP + 16, x1 - 16, TOP + 30, 0xFF050607);
        if (!entries.isEmpty()) {
            g.fill(px + 19, TOP + 17, px + 22, TOP + 29, 0xFF000000 | entries.get(entry).color());
        }
        g.drawString(font, shown, px + 24 + (w - 6 - font.width(shown)) / 2, TOP + 19, TEXT, false);
        // marcadores
        int my0 = TOP + 80, my1 = height - BOTTOM - 40;
        section(g, px, my0, x1, my1, Component.translatable("map.tecnocinetico.markers"));
        List<NavigationData.Marker> list = markersHere();
        int y = my0 + 14, max = Math.max(0, (my1 - y - 2) / ROW);
        markerScroll = Mth.clamp(markerScroll, 0, Math.max(0, list.size() - max));
        for (int i = markerScroll; i < list.size() && i < markerScroll + max; i++, y += ROW) {
            var m = list.get(i);
            boolean over = mouseX >= px && mouseX < x1 && mouseY >= y && mouseY < y + ROW;
            if (over) {
                g.fill(px + 1, y - 1, x1 - 1, y + ROW - 1, 0x30F0B030);
            }
            g.fill(px + 4, y + 2, px + 9, y + 7, 0xFF000000 | m.color());
            g.drawString(font, font.plainSubstrByWidth(m.name(), PANEL - 62), px + 12, y + 1, over ? 0xFFFFF0C8 : TEXT, false);
            String d = (int) Math.sqrt(Mth.square(m.x() - player().getX()) + Mth.square(m.z() - player().getZ())) + "m";
            small(g, d, x1 - (int) (font.width(d) * 0.75F) - 4, y + 2, DIM);
        }
        if (list.isEmpty()) {
            small(g, Component.translatable("map.tecnocinetico.help").getString(), px + 4, my0 + 16, DIM);
        }
        // estado del enlace
        int sy0 = height - BOTTOM - 36;
        section(g, px, sy0, x1, height - BOTTOM + 3, Component.translatable("map.tecnocinetico.status"));
        long found = ClientMapState.found.stream().filter(f -> f.dim().equals(dimId())).count();
        small(g, Component.translatable("map.tecnocinetico.detected", found).getString(), px + 5, sy0 + 15, TEXT);
        small(g, "X " + (int) player().getX() + "  Y " + (int) player().getY() + "  Z " + (int) player().getZ(), px + 5, sy0 + 24, DIM);
        // señal: barras que suben
        int bars = 2 + (int) ((now() - opened) / 300 % 3);
        for (int i = 0; i < 4; i++) {
            int bh = 3 + i * 2;
            g.fill(x1 - 22 + i * 4, sy0 + 26 - bh, x1 - 20 + i * 4, sy0 + 26, i <= bars ? OK : 0xFF2C4A24);
        }
    }

    private List<NavigationData.Marker> markersHere() {
        String dim = dimId();
        return ClientMapState.markers.stream().filter(m -> m.dim().equals(dim)).toList();
    }

    private Component distance(int x, int z) {
        return Component.translatable("map.tecnocinetico.distance", (int) Math.sqrt(Mth.square(x - player().getX()) + Mth.square(z - player().getZ())));
    }

    // ------------------------------------------------------------------ entrada

    @Nullable
    private NavigationData.Marker markerAt(double mx, double my) {
        for (NavigationData.Marker m : markersHere()) {
            double sx = toScreenX(m.x() + 0.5), sy = toScreenY(m.z() + 0.5);
            if (Math.abs(mx - sx) <= 5 && my >= sy - 12 && my <= sy + 2) {
                return m;
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (editing) {
            return super.mouseClicked(mx, my, button);
        }
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        if (inMap(mx, my)) {
            if (button == 1) {
                NavigationData.Marker m = markerAt(mx, my);
                if (m != null && m.owner().equals(player().getUUID())) {
                    openEditor(m, m.x(), m.y(), m.z());
                } else if (m == null) {
                    openEditor(null, (int) Math.floor(toWorldX(mx)), (int) Math.floor(player().getY()), (int) Math.floor(toWorldZ(my)));
                }
                return true;
            }
            dragging = button == 0;
            return true;
        }
        // lista de marcadores: clic = centrar en él
        int px = width - PANEL, y = TOP + 94;
        List<NavigationData.Marker> list = markersHere();
        if (mx >= px && mx < width - PAD && my >= y && my < height - BOTTOM - 40) {
            int i = markerScroll + (int) ((my - y) / ROW);
            if (i >= 0 && i < list.size()) {
                cx = list.get(i).x();
                cz = list.get(i).z();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging && !editing) {
            cx -= dx / scale();
            cz -= dy / scale();
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (editing) {
            return false;
        }
        if (inMap(mx, my)) {
            double wx = toWorldX(mx), wz = toWorldZ(my);
            zoom = Mth.clamp(zoom + (delta > 0 ? 1 : -1), 0, ZOOMS.length - 1);
            cx = wx - (mx - (mapX0() + mapX1()) / 2.0) / scale();                 // acercar hacia el ratón
            cz = wz - (my - (mapY0() + mapY1()) / 2.0) / scale();
            return true;
        }
        markerScroll -= (int) Math.signum(delta);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (editing) {
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                closeEditor();
                return true;
            }
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER && editingMarker == null) {
                saveMarker();
                return true;
            }
            return super.keyPressed(key, scan, mods);
        }
        if (com.bryanamaya.tecnocinetico.client.ModKeyBindings.MAP.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
