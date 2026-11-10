package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.machine.ElectricMachineBlock;
import com.bryanamaya.tecnocinetico.machine.IndustrialAssemblerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.data.ModelData;

import java.util.List;

/**
 * Ensamblador Industrial: los ingredientes descansan sobre la mesa de trabajo, el producto aparece en el centro
 * como un holograma que crece con el progreso, y dos brazos robóticos (modelo aparte) se mueven sobre la pieza
 * soltando chispas mientras trabaja. Al terminar, el producto queda sobre la mesa.
 */
public class IndustrialAssemblerRenderer implements BlockEntityRenderer<IndustrialAssemblerBlockEntity> {

    public static final ResourceLocation ARM = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "block/industrial_assembler_arm");
    private static final float TABLE_Y = -0.36F;                              // altura de la mesa respecto al núcleo

    public IndustrialAssemblerRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(IndustrialAssemblerBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = be.getLevel();
        if (level == null) {
            return;
        }
        var mc = Minecraft.getInstance();
        boolean working = be.maxProgress() > 0 && be.progress() > 0;
        float k = working ? Mth.clamp((be.progress() + partialTick) / be.maxProgress(), 0, 1) : 0;
        float time = level.getGameTime() + partialTick;
        float facing = be.getBlockState().getValue(ElectricMachineBlock.FACING).toYRot();

        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-facing));
        // ingredientes tumbados en círculo sobre la mesa
        List<ItemStack> inputs = be.inputs();
        int n = 0;
        for (ItemStack s : inputs) {
            if (s.isEmpty()) {
                continue;
            }
            float a = (float) (n * Math.PI * 2 / 9);
            pose.pushPose();
            pose.translate(Math.cos(a) * 0.62, TABLE_Y + 0.02, Math.sin(a) * 0.62);
            pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.mulPose(Axis.ZP.rotationDegrees(n * 37));
            pose.scale(0.38F, 0.38F, 0.38F);
            mc.getItemRenderer().renderStatic(s, ItemDisplayContext.FIXED, light, overlay, pose, buffers, level, n);
            pose.popPose();
            n++;
        }
        // producto: holograma que crece (trabajando) o pieza terminada en el centro
        ItemStack result = working ? be.currentResult() : be.outputStack();
        if (!result.isEmpty()) {
            pose.pushPose();
            float scale = working ? 0.25F + 0.55F * k : 0.7F;
            pose.translate(0, TABLE_Y + 0.1 + scale * 0.4, 0);
            if (working) {
                pose.mulPose(Axis.YP.rotationDegrees(time * 2.0F));
            }
            pose.scale(scale, scale, scale);
            mc.getItemRenderer().renderStatic(result, ItemDisplayContext.FIXED, working ? 0xF000F0 : light, overlay, pose, buffers, level, 99);
            pose.popPose();
        }
        // brazos robóticos colgando del raíl: van y vienen sobre la pieza mientras trabaja
        BakedModel arm = mc.getModelManager().getModel(ARM);
        for (int side = -1; side <= 1; side += 2) {
            float sweep = working ? Mth.sin(time * 0.15F + side) * 0.35F : 0.0F;
            float drop = working ? 0.25F + 0.15F * Mth.sin(time * 0.4F + side * 1.7F) : 0.0F;
            pose.pushPose();
            pose.translate(side * 0.75F + sweep * -side, 0.65F - drop, 0);
            pose.mulPose(Axis.YP.rotationDegrees(working ? time * 3.0F * side : 0));
            pose.translate(-0.5, 0, -0.5);
            mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()), null, arm,
                    1.0F, 1.0F, 1.0F, light, overlay, ModelData.EMPTY, RenderType.cutout());
            pose.popPose();
        }
        pose.popPose();
        // chispas de soldadura
        if (working && level.random.nextFloat() < 0.35F) {
            var p = be.getBlockPos();
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, p.getX() + 0.5 + (level.random.nextFloat() - 0.5) * 0.4,
                    p.getY() + TABLE_Y + 0.3, p.getZ() + 0.5 + (level.random.nextFloat() - 0.5) * 0.4,
                    (level.random.nextFloat() - 0.5) * 0.2, 0.1, (level.random.nextFloat() - 0.5) * 0.2);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(IndustrialAssemblerBlockEntity be) {
        return true;
    }
}
