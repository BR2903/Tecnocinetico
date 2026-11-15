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
 * Pantalla del Reloj de Navegación: el terreno explorado ({@link MapExplorer}) con las estructuras detectadas, los
 * marcadores y el jugador encima. Arrastrar mueve, la rueda acerca/aleja, clic derecho pone un marcador (o edita uno
 * propio). A la derecha: buscar una estructura concreta (el satélite la localiza) y la lista de marcadores.
 */
public class WristMapScreen extends Screen {

    private static final float[] ZOOMS = {0.25F, 0.5F, 1.0F, 2.0F, 4.0F, 8.0F};
    private static final int[] COLORS = {0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x55FFFF, 0x5599FF, 0xFF55FF, 0xFFFFFF};
    private static final int PANEL = 150, PAD = 6, TOP = 22, BOTTOM = 18;
    private static final int BG = 0xE0101418, FRAME = 0xFF3A4048, TEXT = 0xFFE0E0E0, DIM = 0xFF9AA0A8, ACCENT = 0xFFF0B030;
    private static final int ROW = 11;

    private double cx, cz;
    private int zoom = 2;
    private boolean dragging;
    private List<MapCatalog.Entry> entries = List.of();
    private int entry;
    @Nullable
    private NavigationData.Found target;
    private long targetTime;

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

    @Override
    protected void init() {
        if (cx == 0 && cz == 0) {
            cx = player().getX();
            cz = player().getZ();
        }
        entries = MapCatalog.in(player().level().dimension());
        entry = Mth.clamp(entry, 0, Math.max(0, entries.size() - 1));
        int px = width - PANEL;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> entry = Math.floorMod(entry - 1, Math.max(1, entries.size())))
                .bounds(px, TOP + 12, 16, 16).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> entry = Math.floorMod(entry + 1, Math.max(1, entries.size())))
                .bounds(width - PAD - 16, TOP + 12, 16, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("map.tecnocinetico.search_button"), b -> {
            if (!entries.isEmpty()) {
                ModNetwork.CHANNEL.sendToServer(MapPackets.Action.locate(entries.get(entry).key()));
            }
        }).bounds(px, TOP + 32, PANEL - PAD, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("map.tecnocinetico.center"), b -> {
            cx = player().getX();
            cz = player().getZ();
        }).bounds(px, TOP + 54, PANEL - PAD, 18).build());
        // editor (oculto hasta que se abre)
        editorWidgets.clear();
        int ex = width / 2 - 90, ey = height / 2 - 40;
        nameBox = new EditBox(font, ex + 6, ey + 18, 168, 16, Component.translatable("map.tecnocinetico.marker.new"));
        nameBox.setMaxLength(NavigationData.MAX_NAME);
        editorWidgets.add(nameBox);
        editorWidgets.add(Button.builder(Component.literal("■"), b -> editColor = (editColor + 1) % COLORS.length)
                .bounds(ex + 6, ey + 40, 20, 18).build());
        editorWidgets.add(Button.builder(Component.empty(), b -> editShared = !editShared).bounds(ex + 30, ey + 40, 144, 18).build());
        editorWidgets.add(Button.builder(Component.translatable("map.tecnocinetico.marker.save"), b -> saveMarker())
                .bounds(ex + 6, ey + 62, 54, 18).build());
        editorWidgets.add(Button.builder(Component.translatable("map.tecnocinetico.marker.delete"), b -> deleteMarker())
                .bounds(ex + 63, ey + 62, 54, 18).build());
        editorWidgets.add(Button.builder(Component.translatable("map.tecnocinetico.marker.cancel"), b -> closeEditor())
                .bounds(ex + 120, ey + 62, 54, 18).build());
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
            targetTime = System.currentTimeMillis();
        }
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fill(0, 0, width, height, BG);
        g.drawString(font, title, PAD, 7, ACCENT, false);
        renderMap(g, mouseX, mouseY);
        renderPanel(g, mouseX, mouseY);
        if (editing) {
            int ex = width / 2 - 90, ey = height / 2 - 40;
            g.fill(ex - 1, ey - 1, ex + 181, ey + 85, FRAME);
            g.fill(ex, ey, ex + 180, ey + 84, 0xF0181C22);
            g.drawString(font, Component.translatable(editingMarker == null ? "map.tecnocinetico.marker.new" : "map.tecnocinetico.markers"),
                    ex + 6, ey + 6, ACCENT, false);
            editorWidgets.get(2).setMessage(Component.translatable(editShared ? "map.tecnocinetico.marker.shared" : "map.tecnocinetico.marker.private"));
        }
        super.render(g, mouseX, mouseY, partial);
        if (editing) {
            int ex = width / 2 - 90, ey = height / 2 - 40;
            g.fill(ex + 11, ey + 45, ex + 21, ey + 53, 0xFF000000 | COLORS[editColor]);
        }
    }

    private void renderMap(GuiGraphics g, int mouseX, int mouseY) {
        int x0 = mapX0(), y0 = mapY0(), x1 = mapX1(), y1 = mapY1();
        g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, FRAME);
        g.fill(x0, y0, x1, y1, 0xFF06080A);
        g.enableScissor(x0, y0, x1, y1);
        String dim = dimId();
        float s = scale();
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
        // estructuras detectadas
        Component hover = null;
        for (NavigationData.Found f : ClientMapState.found) {
            if (!f.dim().equals(dim)) {
                continue;
            }
            MapCatalog.Entry e = MapCatalog.byKey(f.key());
            int sx = (int) toScreenX(f.x() + 0.5), sy = (int) toScreenY(f.z() + 0.5);
            int color = 0xFF000000 | (e == null ? 0xAAAAAA : e.color());
            g.fill(sx - 4, sy - 4, sx + 5, sy + 5, 0xFF000000);
            g.fill(sx - 3, sy - 3, sx + 4, sy + 4, color);
            g.fill(sx - 1, sy - 1, sx + 2, sy + 2, 0xFF000000);
            if (Math.abs(mouseX - sx) <= 4 && Math.abs(mouseY - sy) <= 4 && e != null) {
                hover = Component.translatable(e.langKey()).append(" · ").append(distance(f.x(), f.z()));
            }
        }
        // objetivo de la última búsqueda (parpadea unos segundos)
        if (target != null && target.dim().equals(dim) && (System.currentTimeMillis() - targetTime) < 8000) {
            int sx = (int) toScreenX(target.x() + 0.5), sy = (int) toScreenY(target.z() + 0.5);
            int r = 7 + (int) ((System.currentTimeMillis() / 120) % 5);
            outline(g, sx - r, sy - r, sx + r + 1, sy + r + 1, 0xFF000000 | ACCENT);
        }
        // marcadores
        for (NavigationData.Marker m : ClientMapState.markers) {
            if (!m.dim().equals(dim)) {
                continue;
            }
            int sx = (int) toScreenX(m.x() + 0.5), sy = (int) toScreenY(m.z() + 0.5);
            g.fill(sx - 2, sy - 4, sx + 3, sy + 5, 0xFF000000);
            g.fill(sx - 4, sy - 2, sx + 5, sy + 3, 0xFF000000);
            g.fill(sx - 1, sy - 3, sx + 2, sy + 4, 0xFF000000 | m.color());
            g.fill(sx - 3, sy - 1, sx + 4, sy + 2, 0xFF000000 | m.color());
            boolean over = Math.abs(mouseX - sx) <= 4 && Math.abs(mouseY - sy) <= 4;
            if (s >= 1.0F || over) {
                g.drawString(font, m.name(), sx + 6, sy - 4, 0xFF000000 | m.color(), true);
            }
            if (over) {
                hover = Component.literal(m.name()).append(" · ").append(distance(m.x(), m.z()))
                        .append(m.shared() ? Component.literal(" · ").append(Component.translatable("map.tecnocinetico.marker.by", m.ownerName())) : Component.empty());
            }
        }
        // otros jugadores cercanos y el propio
        for (Player other : player().level().players()) {
            if (other != player()) {
                int sx = (int) toScreenX(other.getX()), sy = (int) toScreenY(other.getZ());
                g.fill(sx - 2, sy - 2, sx + 3, sy + 3, 0xFF000000);
                g.fill(sx - 1, sy - 1, sx + 2, sy + 2, 0xFF55AAFF);
                if (Math.abs(mouseX - sx) <= 3 && Math.abs(mouseY - sy) <= 3) {
                    hover = other.getDisplayName();
                }
            }
        }
        g.pose().pushPose();
        g.pose().translate(toScreenX(player().getX()), toScreenY(player().getZ()), 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(player().getYRot() + 180.0F));
        g.fill(-1, -6, 2, 4, 0xFF000000);                                            // flecha (apunta a -Y antes de girar)
        g.fill(-3, -2, 4, 4, 0xFF000000);
        g.fill(0, -5, 1, 3, 0xFFFFFFFF);
        g.fill(-2, -1, 3, 3, 0xFFFFFFFF);
        g.pose().popPose();
        g.disableScissor();
        // pie: coordenadas bajo el ratón y ayuda
        String foot = Component.translatable("map.tecnocinetico.help").getString();
        if (inMap(mouseX, mouseY)) {
            foot = "X " + (int) Math.floor(toWorldX(mouseX)) + "  Z " + (int) Math.floor(toWorldZ(mouseY)) + "   ·   " + foot;
        }
        g.drawString(font, foot, x0, y1 + 5, DIM, false);
        if (hover != null && !editing) {
            g.renderTooltip(font, hover, mouseX, mouseY);
        }
    }

    private void renderPanel(GuiGraphics g, int mouseX, int mouseY) {
        int px = width - PANEL;
        g.drawString(font, Component.translatable("map.tecnocinetico.search"), px, TOP, ACCENT, false);
        Component name = entries.isEmpty() ? Component.translatable("map.tecnocinetico.none_here")
                : Component.translatable(entries.get(entry).langKey());
        int w = PANEL - PAD - 36;
        String shown = font.plainSubstrByWidth(name.getString(), w);
        g.drawString(font, shown, px + 18 + (w - font.width(shown)) / 2, TOP + 16, TEXT, false);
        g.drawString(font, Component.translatable("map.tecnocinetico.markers"), px, TOP + 80, ACCENT, false);
        List<NavigationData.Marker> list = markersHere();
        int y = TOP + 92, max = (mapY1() - y) / ROW;
        markerScroll = Mth.clamp(markerScroll, 0, Math.max(0, list.size() - max));
        for (int i = markerScroll; i < list.size() && i < markerScroll + max; i++, y += ROW) {
            var m = list.get(i);
            boolean over = mouseX >= px && mouseX < width - PAD && mouseY >= y && mouseY < y + ROW;
            if (over) {
                g.fill(px - 2, y - 1, width - PAD, y + ROW - 1, 0x40FFFFFF);
            }
            g.fill(px, y + 2, px + 6, y + 8, 0xFF000000 | m.color());
            g.drawString(font, font.plainSubstrByWidth(m.name(), PANEL - 60), px + 9, y + 1, TEXT, false);
            String d = String.valueOf((int) Math.sqrt(Mth.square(m.x() - player().getX()) + Mth.square(m.z() - player().getZ())));
            g.drawString(font, d, width - PAD - font.width(d) - 2, y + 1, DIM, false);
        }
    }

    private List<NavigationData.Marker> markersHere() {
        String dim = dimId();
        return ClientMapState.markers.stream().filter(m -> m.dim().equals(dim)).toList();
    }

    private Component distance(int x, int z) {
        return Component.translatable("map.tecnocinetico.distance", (int) Math.sqrt(Mth.square(x - player().getX()) + Mth.square(z - player().getZ())));
    }

    private static void outline(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        g.fill(x0, y0, x1, y0 + 1, color);
        g.fill(x0, y1 - 1, x1, y1, color);
        g.fill(x0, y0, x0 + 1, y1, color);
        g.fill(x1 - 1, y0, x1, y1, color);
    }

    // ------------------------------------------------------------------ entrada

    @Nullable
    private NavigationData.Marker markerAt(double mx, double my) {
        for (NavigationData.Marker m : markersHere()) {
            if (Math.abs(mx - toScreenX(m.x() + 0.5)) <= 4 && Math.abs(my - toScreenY(m.z() + 0.5)) <= 4) {
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
        int px = width - PANEL, y = TOP + 92;
        List<NavigationData.Marker> list = markersHere();
        if (mx >= px && mx < width - PAD && my >= y) {
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
