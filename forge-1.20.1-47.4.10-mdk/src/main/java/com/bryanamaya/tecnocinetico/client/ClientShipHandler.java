package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.ship.CoreFighterEntity;
import com.bryanamaya.tecnocinetico.network.ModNetwork;
import com.bryanamaya.tecnocinetico.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Cliente del Caza del Núcleo: entradas del piloto (W/S empuje, espacio/Ctrl altura; clic izquierdo cañones,
 * derecho misil), pantalla de vuelo (casco, velocidad, misiles, objetivo fijado, carga del salto), campo de visión
 * que se abre con la velocidad y al cargar el salto, destello del salto y sonido del motor.
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID, value = Dist.CLIENT)
public final class ClientShipHandler {

    private static boolean sentGuns, sentMissile;
    private static long flashStart;

    private ClientShipHandler() {}

    private static CoreFighterEntity ship(LocalPlayer p) {
        return p != null && p.getVehicle() instanceof CoreFighterEntity s ? s : null;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        CoreFighterEntity ship = ship(mc.player);
        boolean guns = false, missile = false;
        if (ship != null) {
            boolean free = mc.screen == null;
            ship.inForward = free ? mc.player.input.forwardImpulse : 0.0F;
            ship.inUp = free && mc.player.input.jumping;
            ship.inDown = free && mc.options.keySprint.isDown();
            guns = free && mc.options.keyAttack.isDown();
            missile = free && mc.options.keyUse.isDown();
            if (ship.jumpTicks() >= CoreFighterEntity.JUMP_TICKS - 3 && System.currentTimeMillis() - flashStart > 3000) {
                flashStart = System.currentTimeMillis();                          // destello del salto
            }
        }
        if (guns != sentGuns || missile != sentMissile) {
            sentGuns = guns;
            sentMissile = missile;
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.ShipControlPacket(guns, missile));
        }
    }

    /** Dentro de la nave los clics son las armas: ni golpear ni usar objetos. */
    @SubscribeEvent
    public static void onClick(InputEvent.InteractionKeyMappingTriggered event) {
        if (ship(Minecraft.getInstance().player) != null) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void onHand(RenderHandEvent event) {
        if (ship(Minecraft.getInstance().player) != null) {
            event.setCanceled(true);                                              // las manos van en los mandos
        }
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        CoreFighterEntity ship = ship(Minecraft.getInstance().player);
        if (ship == null) {
            return;
        }
        float f = 1.0F + 0.12F * (float) Math.min(1.0, ship.getDeltaMovement().length() / CoreFighterEntity.MAX_SPEED);
        if (ship.jumpTicks() > 0) {
            float k = ship.jumpTicks() / (float) CoreFighterEntity.JUMP_TICKS;
            f += 0.45F * k * k;
        }
        event.setNewFovModifier(event.getNewFovModifier() * f);
    }

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = event.getGuiGraphics();
        int w = event.getWindow().getGuiScaledWidth(), h = event.getWindow().getGuiScaledHeight();
        // destello del salto (sigue aunque cambie la dimensión)
        long since = System.currentTimeMillis() - flashStart;
        if (since < 1600) {
            float a = since < 150 ? since / 150.0F : 1.0F - (since - 150) / 1450.0F;
            int alpha = (int) (Mth.clamp(a, 0.0F, 1.0F) * 230);
            g.fill(0, 0, w, h, (alpha << 24) | 0xD8F8FF);
        }
        CoreFighterEntity ship = ship(mc.player);
        if (ship == null || mc.options.hideGui) {
            return;
        }
        if (mc.options.getCameraType().isFirstPerson()) {                           // cabina: pantalla de vuelo completa
            ShipHud.draw(g, ship, event.getPartialTick(), w, h);
            return;
        }
        var font = mc.font;
        int y = h - 62;
        int hull = Math.round(ship.health() / CoreFighterEntity.MAX_HEALTH * 100);
        int spd = (int) Math.round(ship.getDeltaMovement().length() * 20 * 3.6);  // km/h aproximados
        String miss = ship.missileCooldown() <= 0 ? "●" : "○";
        Component line = Component.translatable("hud.tecnocinetico.ship", hull, spd, miss);
        g.drawCenteredString(font, line, w / 2, y, hull > 30 ? 0x46E3F5 : 0xFF5A40);
        LivingEntity target = ship.lockTarget(mc.player);
        if (target != null) {
            int dist = (int) target.distanceTo(mc.player);
            g.drawCenteredString(font, Component.translatable("hud.tecnocinetico.lock", target.getDisplayName(), dist)
                    .withStyle(ChatFormatting.RED), w / 2, h / 2 + 14, 0xFF5A40);
            g.drawCenteredString(font, "[ + ]", w / 2, h / 2 - 4, 0xFF5A40);
        }
        int j = ship.jumpTicks();
        if (j > 0) {                                                              // barra de carga del salto
            int bw = 120, filled = bw * j / CoreFighterEntity.JUMP_TICKS;
            g.fill(w / 2 - bw / 2 - 1, y - 13, w / 2 + bw / 2 + 1, y - 7, 0xAA000000);
            g.fill(w / 2 - bw / 2, y - 12, w / 2 - bw / 2 + filled, y - 8, 0xFFB48CFF);
            g.drawCenteredString(font, Component.translatable("hud.tecnocinetico.jump"), w / 2, y - 24, 0xB48CFF);
        }
    }

    // ------------------------------------------------------------------ motor

    /** Sonido continuo del motor; lo arranca la propia nave al aparecer en el cliente. */
    public static void startEngine(CoreFighterEntity ship) {
        Minecraft.getInstance().getSoundManager().play(new Engine(ship));
    }

    private static class Engine extends AbstractTickableSoundInstance {
        private final CoreFighterEntity ship;

        Engine(CoreFighterEntity ship) {
            super(ModSounds.SHIP_ENGINE.get(), SoundSource.NEUTRAL, RandomSource.create());
            this.ship = ship;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01F;
            this.x = ship.getX();
            this.y = ship.getY();
            this.z = ship.getZ();
        }

        @Override
        public void tick() {
            if (ship.isRemoved()) {
                stop();
                return;
            }
            x = ship.getX();
            y = ship.getY();
            z = ship.getZ();
            double v = Math.sqrt(Math.pow(ship.getX() - ship.xo, 2) + Math.pow(ship.getY() - ship.yo, 2) + Math.pow(ship.getZ() - ship.zo, 2));
            float k = (float) Math.min(1.0, v / CoreFighterEntity.MAX_SPEED);
            boolean manned = ship.isVehicle();
            volume = manned ? 0.35F + 0.65F * k : 0.0F;
            pitch = 0.7F + 0.7F * k + (ship.jumpTicks() > 0 ? ship.jumpTicks() / (float) CoreFighterEntity.JUMP_TICKS * 0.6F : 0);
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }
}
