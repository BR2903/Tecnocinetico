package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.machine.FissionReactorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.model.data.ModelData;

/**
 * Reactor de Fisión: dentro de la vasija (3x3 sobre el núcleo) se ven las barras de combustible. La que está
 * quemando, en el centro, brilla verde y late con la potencia; las que esperan en el hueco de entrada (hasta 8)
 * rodean a la central, apagadas.
 */
public class FissionReactorRenderer implements BlockEntityRenderer<FissionReactorBlockEntity> {

    public static final ResourceLocation ROD = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "block/reactor_fuel_rod");
    public static final ResourceLocation ROD_SPENT = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "block/reactor_fuel_rod_idle");
    private static final int[][] RING = {{-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};

    public FissionReactorRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(FissionReactorBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = be.getLevel();
        if (level == null) {
            return;
        }
        var mc = Minecraft.getInstance();
        BakedModel active = mc.getModelManager().getModel(ROD), idle = mc.getModelManager().getModel(ROD_SPENT);
        int inside = LevelRenderer.getLightColor(level, be.getBlockPos().above());
        if (be.burning()) {
            float t = level.getGameTime() + partialTick;
            float pulse = 0.85F + 0.15F * Mth.sin(t * 0.3F) * be.throttle() / 100.0F;
            rod(pose, buffers, active, 0, 0, 0xF000F0, overlay, pulse);           // la barra que quema: brilla
        }
        for (int i = 0; i < Math.min(8, be.waitingRods()); i++) {
            rod(pose, buffers, idle, RING[i][0], RING[i][1], inside, overlay, 1.0F);
        }
    }

    private static void rod(PoseStack pose, MultiBufferSource buffers, BakedModel model, int dx, int dz, int light, int overlay, float bright) {
        pose.pushPose();
        pose.translate(dx, 1.0, dz);
        pose.scale(1.0F, 2.85F, 1.0F);                                          // barra de casi 3 bloques
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()), null,
                model, bright, bright, bright, light, overlay, ModelData.EMPTY, RenderType.cutout());
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(FissionReactorBlockEntity be) {
        return true;
    }
}
