package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.item.custom.ChainsawItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * Motosierra en primera persona: se sostiene firme (sin golpe de brazo; ver {@code ChainsawItem.onEntitySwing}).
 * Encendida tiembla con el motor; cortando, se hunde hacia delante y abajo contra la madera, vibra fuerte y se mece
 * con el corte (la cadena gira con el predicado {@code chainsaw_run}). Al repostar se gira de lado y se baja mientras
 * entra la gasolina. Sin gasolina, quieta. En tercera persona se lleva con las dos manos.
 */
public class ChainsawClientExtensions implements IClientItemExtensions {

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return HumanoidModel.ArmPose.CROSSBOW_HOLD;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                           float partialTick, float equipProcess, float swingProcess) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        if (player.getMainHandItem().getItem() instanceof ChainsawItem && arm == player.getMainArm()) {
            stack = player.getMainHandItem();                                         // el real (el gasto cambia el NBT)
        }
        pose.translate(side * 0.5F, -0.52F, -0.66F);
        pose.translate(0, -0.25F * equipProcess, 0);                                    // al sacarla, sube
        float t = player.tickCount + partialTick;

        float refuel = ChainsawItem.refuelProgress(stack, player.level().getGameTime() + partialTick);
        if (refuel >= 0) {
            // repostar: la gira de lado y la baja (entra 15 %), vaivén mientras cae la gasolina, y vuelve (último 15 %)
            float in = Mth.clamp(refuel / 0.15F, 0, 1), out = Mth.clamp((1 - refuel) / 0.15F, 0, 1);
            float k = smooth(Math.min(in, out));
            pose.translate(-side * 0.18F * k, -0.2F * k, 0.1F * k);
            pose.mulPose(Axis.ZP.rotationDegrees(side * 55.0F * k));
            pose.mulPose(Axis.YP.rotationDegrees(-side * 20.0F * k));
            pose.translate(0, 0.015F * k * Mth.sin(t * 0.8F), 0);
            return true;
        }
        if (!ChainsawItem.hasFuel(stack)) {
            return true;                                                              // apagada: quieta
        }
        var gameMode = Minecraft.getInstance().gameMode;
        boolean cutting = gameMode != null && gameMode.isDestroying();
        float buzz;
        if (cutting) {
            // se hunde contra la madera (hacia delante y un poco abajo) y se mece con el corte
            pose.translate(0, -0.05F, -0.12F);
            pose.mulPose(Axis.XP.rotationDegrees(-8.0F + Mth.sin(t * 0.35F) * 4.0F));
            buzz = 0.02F;
        } else {
            buzz = 0.005F;                                                            // ralentí: tiembla
        }
        pose.translate(Mth.sin(t * 9.7F) * buzz, Mth.cos(t * 12.3F) * buzz, Mth.sin(t * 15.1F) * buzz * 0.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(t * 8.3F) * (cutting ? 1.6F : 0.4F)));
        return true;
    }

    private static float smooth(float x) {
        return x * x * (3 - 2 * x);
    }
}
