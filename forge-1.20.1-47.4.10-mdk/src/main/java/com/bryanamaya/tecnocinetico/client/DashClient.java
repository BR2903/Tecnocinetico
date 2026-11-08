package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.gear.DashEvents;
import com.bryanamaya.tecnocinetico.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

/**
 * Impulsor de Esquiva en el cliente: con Alt, impulso horizontal hacia donde te mueves (WASD; quieto: hacia delante)
 * y un pequeño salto si estás en el suelo. El servidor gasta la batería y da la ventana de esquiva ({@link DashEvents}).
 */
public final class DashClient {

    private static long lastDash = -100;

    private DashClient() {}

    public static void tryDash() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.screen != null || p.isPassenger() || p.isSpectator() || p.getAbilities().flying) {
            return;
        }
        long now = p.level().getGameTime();
        if (now - lastDash < DashEvents.COOLDOWN || DashEvents.thruster(p).isEmpty()) {
            return;
        }
        lastDash = now;
        float fwd = p.input.forwardImpulse, side = p.input.leftImpulse;
        Vec3 dir;
        if (Math.abs(fwd) < 0.1F && Math.abs(side) < 0.1F) {
            dir = Vec3.directionFromRotation(0, p.getYRot());
        } else {
            Vec3 f = Vec3.directionFromRotation(0, p.getYRot()), l = Vec3.directionFromRotation(0, p.getYRot() - 90.0F);
            dir = f.scale(fwd).add(l.scale(side)).normalize();
        }
        Vec3 v = p.getDeltaMovement();
        double up = p.onGround() ? 0.22 : Math.max(v.y, 0.08);
        p.setDeltaMovement(dir.x * 1.15, up, dir.z * 1.15);
        for (int i = 0; i < 10; i++) {
            p.level().addParticle(ParticleTypes.CLOUD, p.getX() - dir.x * i * 0.15, p.getY() + 0.3, p.getZ() - dir.z * i * 0.15, 0, 0.01, 0);
        }
        ModNetwork.CHANNEL.sendToServer(new ModNetwork.DashPacket());
    }

    /** Progreso del enfriamiento (0 listo .. 1 recién usado), para la interfaz. */
    public static float cooldown(LocalPlayer p) {
        return Math.max(0.0F, 1.0F - (p.level().getGameTime() - lastDash) / (float) DashEvents.COOLDOWN);
    }
}
