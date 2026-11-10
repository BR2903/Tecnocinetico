package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.entity.AutoKineticPressBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.data.ModelData;

/**
 * Prensa Cinética Automática (estilo Create): los ingredientes descansan sobre el soporte, el cabezal baja poco a
 * poco y golpea cada segundo mientras trabaja, y al terminar la pieza fabricada queda en el soporte.
 */
public class AutoPressRenderer implements BlockEntityRenderer<AutoKineticPressBlockEntity> {

    public static final ResourceLocation HEAD = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "block/auto_press_head");
    private static final float DEPOT_Y = 6.5F / 16.0F;

    public AutoPressRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    /** Bajada del cabezal (en bloques) según el momento del golpe: 0 en reposo, ~0.12 al golpear. */
    public static float drop(float t) {
        return t < 0.75F ? 0.015F * t / 0.75F : 0.015F + 0.105F * Mth.sin((t - 0.75F) / 0.25F * Mth.PI);
    }

    @Override
    public void render(AutoKineticPressBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = be.getLevel();
        if (level == null) {
            return;
        }
        var mc = Minecraft.getInstance();
        boolean working = be.getProgress() > 0;
        float cycle = working ? ((be.getProgress() + partialTick) % 20.0F) / 20.0F : 0.0F;
        // cabezal
        pose.pushPose();
        pose.translate(0, -drop(cycle), 0);
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()), null,
                mc.getModelManager().getModel(HEAD), 1.0F, 1.0F, 1.0F, light, overlay, ModelData.EMPTY, RenderType.cutout());
        pose.popPose();
        // piezas en el soporte
        var inv = be.getItemHandler();
        ItemStack out = inv.getStackInSlot(AutoKineticPressBlockEntity.OUTPUT_SLOT);
        int shown = 0;
        for (int i = 0; i < AutoKineticPressBlockEntity.INPUT_SLOTS; i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (s.isEmpty()) {
                continue;
            }
            pose.pushPose();
            pose.translate(0.5 + (shown - 1) * 0.16, DEPOT_Y + 0.01 + shown * 0.012, 0.5 + (shown % 2) * 0.06 - 0.03);
            pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.mulPose(Axis.ZP.rotationDegrees(shown * 40));
            pose.scale(0.3F, 0.3F, 0.3F);
            mc.getItemRenderer().renderStatic(s, ItemDisplayContext.FIXED, light, overlay, pose, buffers, level, i);
            pose.popPose();
            shown++;
        }
        if (!out.isEmpty() && shown == 0) {
            pose.pushPose();
            pose.translate(0.5, DEPOT_Y + 0.02, 0.5);
            pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.scale(0.38F, 0.38F, 0.38F);
            mc.getItemRenderer().renderStatic(out, ItemDisplayContext.FIXED, light, overlay, pose, buffers, level, 9);
            pose.popPose();
        }
    }
}
