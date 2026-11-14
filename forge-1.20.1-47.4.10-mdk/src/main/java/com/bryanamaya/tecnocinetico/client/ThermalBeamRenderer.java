package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.weapon.ThermalLanceItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Rayo de la Lanza Térmica (cliente). Para cada jugador cercano que la usa con plasma, calcula el mismo rayo que el
 * servidor ({@link ThermalLanceItem#beam}) y lo dibuja desde la boca del arma: un núcleo blanco-amarillo y un halo
 * naranja que tiembla, con chispas y humo donde pega.
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID, value = Dist.CLIENT)
public final class ThermalBeamRenderer {

    private ThermalBeamRenderer() {}

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        float partial = event.getPartialTick();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        PoseStack pose = event.getPoseStack();
        boolean any = false;
        for (Player p : mc.level.players()) {
            if (!p.isUsingItem() || !(p.getUseItem().getItem() instanceof ThermalLanceItem) || !ThermalLanceItem.canFire(p.getUseItem())
                    || p.distanceToSqr(mc.player) > 96 * 96) {
                continue;
            }
            var beam = ThermalLanceItem.beam(mc.level, p);
            Vec3 start = muzzle(mc, p, partial);
            Vec3 end = beam.end();
            float t = p.tickCount + partial;
            pose.pushPose();
            pose.translate(-cam.x, -cam.y, -cam.z);
            Matrix4f m = pose.last().pose();
            float flicker = 0.85F + 0.15F * (float) Math.sin(t * 2.3F);
            quad(vc, m, start, end, cam, 0.16F * flicker, 255, 110, 25, 110);           // halo naranja
            quad(vc, m, start, end, cam, 0.06F, 255, 235, 170, 230);                    // núcleo
            pose.popPose();
            any = true;
            if (mc.level.random.nextInt(2) == 0) {
                mc.level.addParticle(ParticleTypes.FLAME, end.x, end.y, end.z, 0, 0.03, 0);
                mc.level.addParticle(ParticleTypes.SMALL_FLAME, end.x, end.y, end.z,
                        (mc.level.random.nextDouble() - 0.5) * 0.1, 0.05, (mc.level.random.nextDouble() - 0.5) * 0.1);
            }
        }
        if (any) {
            buffers.endBatch(RenderType.lightning());
        }
    }

    /** Boca del arma: en primera persona, abajo a la derecha de la vista; si no, delante de la mano de la lanza. */
    private static Vec3 muzzle(Minecraft mc, Player p, float partial) {
        Vec3 eye = p.getEyePosition(partial), look = p.getViewVector(partial);
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(look).normalize();
        int side = p.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1 : -1;
        if (p == mc.player && mc.options.getCameraType().isFirstPerson()) {
            return eye.add(look.scale(0.9)).add(right.scale(0.28 * side)).add(up.scale(-0.22));
        }
        return eye.add(look.scale(1.1)).add(right.scale(0.32 * side)).add(up.scale(-0.45));
    }

    /** Cinta entre a y b, de ancho w, girada hacia la cámara. */
    private static void quad(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 cam, float w, int r, int g, int bl, int alpha) {
        Vec3 dir = b.subtract(a);
        Vec3 toCam = cam.subtract(a.add(dir.scale(0.5)));
        Vec3 side = dir.cross(toCam);
        if (side.lengthSqr() < 1e-8) {
            return;
        }
        side = side.normalize().scale(w);
        vertex(vc, m, a.add(side), r, g, bl, alpha);
        vertex(vc, m, a.subtract(side), r, g, bl, alpha);
        vertex(vc, m, b.subtract(side), r, g, bl, alpha);
        vertex(vc, m, b.add(side), r, g, bl, alpha);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Vec3 v, int r, int g, int b, int a) {
        vc.vertex(m, (float) v.x, (float) v.y, (float) v.z).color(r, g, b, a).endVertex();
    }
}
