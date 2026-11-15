package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.end.DataWormEntity;
import com.bryanamaya.tecnocinetico.end.DataWormPart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Gusano de Datos (modelo de Codex, {@link DataWormModel}): cabeza, segmentos sanos y cola, cada pieza en la posición
 * de su parte y girada según la línea del cuerpo (de la pieza anterior a la siguiente). Bajo tierra no se dibuja.
 */
public class DataWormRenderer extends EntityRenderer<DataWormEntity> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/entity/data_worm.png");
    private static final RenderType GLOW = RenderType.eyes(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/entity/data_worm_glow.png"));

    private final DataWormModel model;

    public DataWormRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        model = new DataWormModel(ctx.bakeLayer(DataWormModel.HEAD_LAYER), ctx.bakeLayer(DataWormModel.SEGMENT_LAYER), ctx.bakeLayer(DataWormModel.TAIL_LAYER));
        shadowRadius = 0.0F;
    }

    private record Node(Vec3 at, int part) {
    }

    @Override
    public void render(DataWormEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        int state = e.state();
        if (state == DataWormEntity.BURROWED || state == DataWormEntity.WARN) {
            return;                                                           // bajo tierra
        }
        Vec3 base = new Vec3(Mth.lerp(partial, e.xo, e.getX()), Mth.lerp(partial, e.yo, e.getY()), Mth.lerp(partial, e.zo, e.getZ()));
        List<Node> nodes = new ArrayList<>();
        nodes.add(new Node(base.add(0, DataWormEntity.HEAD_HALF, 0), -1));
        DataWormPart[] parts = e.parts();
        for (int i = 0; i < parts.length; i++) {
            if (!e.broken(i)) {
                DataWormPart p = parts[i];
                float half = DataWormEntity.partSize(i) / 2.0F;
                nodes.add(new Node(new Vec3(Mth.lerp(partial, p.xo, p.getX()), Mth.lerp(partial, p.yo, p.getY()) + half,
                        Mth.lerp(partial, p.zo, p.getZ())), i));
            }
        }
        float age = e.tickCount + partial;
        int overlay = OverlayTexture.pack(0.0F, e.hurtTime > 0 || e.deathTime > 0);
        VertexConsumer solid = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        VertexConsumer glow = buffers.getBuffer(GLOW);
        for (int n = 0; n < nodes.size(); n++) {
            Vec3 ahead = nodes.get(Math.max(0, n - 1)).at, behind = nodes.get(Math.min(nodes.size() - 1, n + 1)).at;
            Vec3 dir = ahead.subtract(behind);
            if (dir.lengthSqr() < 1e-6) {
                dir = Vec3.directionFromRotation(e.getXRot(), e.getYRot());
            }
            dir = dir.normalize();
            Node node = nodes.get(n);
            pose.pushPose();
            Vec3 off = node.at.subtract(base);
            pose.translate(off.x, off.y, off.z);
            pose.mulPose(Axis.YP.rotation((float) Math.atan2(dir.x, dir.z)));      // +Z del modelo hacia "dir"
            pose.mulPose(Axis.XP.rotation((float) -Math.asin(Mth.clamp(dir.y, -1.0, 1.0))));
            if (e.deathTime > 0) {
                float s = 1.0F - Math.min(1.0F, (e.deathTime + partial) / 20.0F) * 0.6F;
                pose.scale(s, s, s);
            }
            for (int pass = 0; pass < 2; pass++) {
                VertexConsumer vc = pass == 0 ? solid : glow;
                int l = pass == 0 ? light : 0xF000F0;
                if (node.part < 0) {
                    model.renderHead(pose, vc, l, overlay, e.jaw(partial), age);
                } else if (node.part == DataWormEntity.SEGMENTS) {
                    model.renderTail(pose, vc, l, overlay, age);
                } else {
                    model.renderSegment(pose, vc, l, overlay, node.part, age);
                }
            }
            pose.popPose();
        }
        super.render(e, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(DataWormEntity e) {
        return TEXTURE;
    }
}
