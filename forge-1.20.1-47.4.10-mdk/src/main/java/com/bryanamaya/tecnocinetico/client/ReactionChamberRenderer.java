package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.machine.ElectricMachineBlock;
import com.bryanamaya.tecnocinetico.machine.ReactionChamberBlockEntity;
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
 * Cámara de Reacción (2x2x2): por la mirilla se ve la carga flotando en el centro; cuando el haz está encendido
 * baja del emisor del techo hasta la carga, que gira y brilla (más deprisa cuanto mejor la resonancia).
 */
public class ReactionChamberRenderer implements BlockEntityRenderer<ReactionChamberBlockEntity> {

    public static final ResourceLocation BEAM = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "block/reaction_beam");

    public ReactionChamberRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(ReactionChamberBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = be.getLevel();
        ItemStack load = be.load();
        if (level == null || load.isEmpty()) {
            return;
        }
        var mc = Minecraft.getInstance();
        float t = level.getGameTime() + partialTick;
        boolean on = be.beamOn();
        int rot = switch (be.getBlockState().getValue(ElectricMachineBlock.FACING)) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-rot));                           // igual que el giro del modelo
        pose.translate(-0.5, 0, 0.5);                                         // centro de la cámara (2x2)
        // carga flotando
        pose.pushPose();
        pose.translate(0, 0.85 + 0.04 * Mth.sin(t * 0.1F), 0);
        pose.mulPose(Axis.YP.rotationDegrees(t * (on ? 2.0F + be.resonance() * 0.06F : 0.5F)));
        pose.scale(0.45F, 0.45F, 0.45F);
        mc.getItemRenderer().renderStatic(load, ItemDisplayContext.FIXED, on ? 0xF000F0 : light, overlay, pose, buffers, level, 7);
        pose.popPose();
        // haz del emisor a la carga
        if (on) {
            float pulse = 0.75F + 0.25F * Mth.sin(t * 0.8F);
            pose.pushPose();
            pose.translate(-0.5, 1.0, -0.5);
            pose.scale(1.0F, 0.42F, 1.0F);
            mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.translucent()), null,
                    mc.getModelManager().getModel(BEAM), pulse, pulse, pulse, 0xF000F0, overlay, ModelData.EMPTY, RenderType.translucent());
            pose.popPose();
        }
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(ReactionChamberBlockEntity be) {
        return true;
    }
}
