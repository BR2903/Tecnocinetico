package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.entity.ship.CoreFighterEntity;
import com.bryanamaya.tecnocinetico.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Comparator;
import java.util.List;

/**
 * Menú del motor de salto (J dentro del Caza del Núcleo): un botón por cada dimensión del servidor (salvo la
 * actual). Las dimensiones nuevas del mod aparecerán aquí solas cuando existan.
 */
public class ShipJumpScreen extends Screen {

    public ShipJumpScreen() {
        super(Component.translatable("screen.tecnocinetico.ship_jump"));
    }

    public static Component dimensionName(ResourceKey<Level> key) {
        return Component.translatableWithFallback("dimension." + key.location().getNamespace() + "." + key.location().getPath(),
                key.location().toString());
    }

    @Override
    protected void init() {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        ResourceKey<Level> here = minecraft.player.level().dimension();
        List<ResourceKey<Level>> dims = minecraft.player.connection.levels().stream()
                .filter(k -> k != here)
                .sorted(Comparator.comparing(k -> k.location().toString()))
                .toList();
        int y = height / 2 - dims.size() * 12;
        CoreFighterEntity ship = minecraft.player.getVehicle() instanceof CoreFighterEntity s ? s : null;
        for (ResourceKey<Level> key : dims) {
            boolean ok = ship == null || ship.canJumpTo(key);
            Button button = Button.builder(ok ? dimensionName(key) : Component.translatable("screen.tecnocinetico.ship_jump.locked", dimensionName(key)), b -> {
                ModNetwork.CHANNEL.sendToServer(new ModNetwork.ShipJumpPacket(key.location().toString()));
                onClose();
            }).bounds(width / 2 - 90, y, 180, 20).build();
            button.active = ok;
            if (!ok) {
                button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("message.tecnocinetico.need_coupler")));
            }
            addRenderableWidget(button);
            y += 24;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(width / 2 - 50, y + 8, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, height / 2 - 70, 0xB48CFF);
        if (minecraft != null && minecraft.player != null) {
            g.drawCenteredString(font, Component.translatable("screen.tecnocinetico.ship_jump.here",
                    dimensionName(minecraft.player.level().dimension())), width / 2, height / 2 - 56, 0xA0A0A0);
            if (minecraft.player.getVehicle() instanceof CoreFighterEntity s && s.jumpTicks() > 0) {
                g.drawCenteredString(font, Component.translatable("hud.tecnocinetico.jump"), width / 2, height / 2 - 44, 0xB48CFF);
            }
        }
        super.render(g, mouseX, mouseY, pt);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
