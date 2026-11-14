package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.item.custom.GunAnimState;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.weapon.ThermalLanceItem;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Rayo de la Lanza Térmica (cliente). Para cada jugador cercano que dispara (el propio: en cuanto aprieta; los demás:
 * cuando el servidor marca el arma), calcula el mismo rayo que el servidor ({@link ThermalLanceItem#beam}) y lo
 * dibuja desde la boca del arma con la textura propia {@code textures/effect/thermal_beam.png}, que fluye a lo largo:
 * dos cintas en cruz (se ve desde cualquier ángulo, sin caras ocultas) más un halo ancho y tenue. En el impacto:
 * destello, chispas, gotas fundidas y calima (partículas propias, {@link HeatParticle}); chispas en la boca.
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID, value = Dist.CLIENT)
public final class ThermalBeamRenderer {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/effect/thermal_beam.png");

    private ThermalBeamRenderer() {}

    /** ¿Está disparando ese jugador? (el propio: según su gatillo; los demás: según lo que marca el servidor). */
    static boolean firing(Minecraft mc, Player p) {
        ItemStack s = p.getMainHandItem();
        if (!(s.getItem() instanceof ThermalLanceItem)) {
            return false;
        }
        return p == mc.player ? LanceInput.firingLocal(s) : ThermalLanceItem.firing(s) && ThermalLanceItem.canFire(s);
    }

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
        RenderType type = RenderType.eyes(TEXTURE);                                       // aditiva (ONE, ONE), siempre brillante
        PoseStack pose = event.getPoseStack();
        boolean any = false;
        for (Player p : mc.level.players()) {
            if (!firing(mc, p) || p.distanceToSqr(mc.player) > 96 * 96) {
                continue;
            }
            Vec3 start = muzzle(mc, p, partial);
            Vec3 end = ThermalLanceItem.beam(mc.level, p, partial).end();
            float t = p.tickCount + partial;
            VertexConsumer vc = buffers.getBuffer(type);
            pose.pushPose();
            pose.translate(-cam.x, -cam.y, -cam.z);
            Matrix4f m = pose.last().pose();
            Matrix3f n = pose.last().normal();
            float flicker = 0.9F + 0.1F * (float) Math.sin(t * 2.3F);
            float scroll = -t * 0.35F;
            Vec3 dir = end.subtract(start);
            Vec3 a1 = perpendicular(dir), a2 = dir.normalize().cross(a1).normalize();
            ribbon(vc, m, n, start, end, a1.scale(0.22 * flicker), scroll, 255);           // núcleo, dos cintas en cruz
            ribbon(vc, m, n, start, end, a2.scale(0.22 * flicker), scroll, 255);
            Vec3 toCam = cam.subtract(start.add(dir.scale(0.5)));
            Vec3 side = dir.cross(toCam);
            if (side.lengthSqr() > 1e-8) {
                ribbon(vc, m, n, start, end, side.normalize().scale(0.55 * flicker), scroll * 0.6F, 110);   // halo
            }
            pose.popPose();
            any = true;
        }
        if (any) {
            buffers.endBatch(type);
        }
    }

    /** Partículas del rayo: en el impacto y en la boca (cada cliente las crea con su propio cálculo del rayo). */
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || mc.level == null || mc.player == null || mc.isPaused()) {
            return;
        }
        RandomSource r = mc.level.random;
        for (Player p : mc.level.players()) {
            if (!firing(mc, p) || p.distanceToSqr(mc.player) > 64 * 64) {
                continue;
            }
            var beam = ThermalLanceItem.beam(mc.level, p);
            Vec3 end = beam.end(), back = p.getEyePosition().subtract(end).normalize().scale(0.15);
            Vec3 at = end.add(back);
            if (p.tickCount % 3 == 0) {
                mc.level.addParticle(ModParticles.HEAT_FLASH.get(), at.x, at.y, at.z, 0, 0, 0);
            }
            for (int i = 0; i < 2; i++) {
                mc.level.addParticle(ModParticles.HEAT_SPARK.get(), at.x, at.y, at.z,
                        (r.nextDouble() - 0.5) * 0.35 + back.x, r.nextDouble() * 0.25, (r.nextDouble() - 0.5) * 0.35 + back.z);
            }
            if (p.tickCount % 2 == 0) {
                mc.level.addParticle(ModParticles.MOLTEN_DROP.get(), at.x, at.y, at.z,
                        (r.nextDouble() - 0.5) * 0.12, 0.05, (r.nextDouble() - 0.5) * 0.12);
            }
            if (p.tickCount % 3 == 1) {
                mc.level.addParticle(ModParticles.HEAT_HAZE.get(), at.x, at.y + 0.1, at.z, 0, 0.02, 0);
            }
            if (p.tickCount % 2 == 0) {                                                     // chispas en la boca
                Vec3 mz = muzzle(mc, p, 1.0F);
                mc.level.addParticle(ModParticles.HEAT_SPARK.get(), mz.x, mz.y, mz.z,
                        (r.nextDouble() - 0.5) * 0.05, 0.03, (r.nextDouble() - 0.5) * 0.05);
            }
        }
    }

    /** Boca del arma: en primera persona, abajo a la derecha de la vista (al centro si apunta); si no, delante de la mano. */
    static Vec3 muzzle(Minecraft mc, Player p, float partial) {
        Vec3 eye = p.getEyePosition(partial), look = p.getViewVector(partial);
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(look).normalize();
        int side = p.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1 : -1;
        if (p == mc.player && mc.options.getCameraType().isFirstPerson()) {
            float aim = GunAnimState.aim(partial);
            return eye.add(look.scale(0.95)).add(right.scale(0.24 * side * (1 - aim))).add(up.scale(-0.2 + 0.08 * aim));
        }
        return eye.add(look.scale(1.15)).add(right.scale(0.3 * side)).add(up.scale(-0.42));
    }

    private static Vec3 perpendicular(Vec3 dir) {
        Vec3 d = dir.normalize();
        Vec3 ref = Math.abs(d.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        return d.cross(ref).normalize();
    }

    /**
     * Cinta texturizada de a a b, de medio ancho `half`, con la textura repitiéndose cada 2 bloques y desplazándose.
     * Se emite por las dos caras (la capa descarta las caras de espaldas). `bright` (0-255) atenúa el color (aditivo).
     */
    private static void ribbon(VertexConsumer vc, Matrix4f m, Matrix3f n, Vec3 a, Vec3 b, Vec3 half, float scroll, int bright) {
        float len = (float) a.distanceTo(b);
        float v0 = scroll, v1 = scroll + len / 2.0F;
        vertex(vc, m, n, a.add(half), 0, v0, bright);
        vertex(vc, m, n, a.subtract(half), 1, v0, bright);
        vertex(vc, m, n, b.subtract(half), 1, v1, bright);
        vertex(vc, m, n, b.add(half), 0, v1, bright);
        vertex(vc, m, n, b.add(half), 0, v1, bright);                                      // cara de atrás
        vertex(vc, m, n, b.subtract(half), 1, v1, bright);
        vertex(vc, m, n, a.subtract(half), 1, v0, bright);
        vertex(vc, m, n, a.add(half), 0, v0, bright);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, Vec3 v, float u, float tv, int bright) {
        vc.vertex(m, (float) v.x, (float) v.y, (float) v.z).color(bright, bright, bright, 255).uv(u, tv)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(n, 0, 1, 0).endVertex();
    }
}
