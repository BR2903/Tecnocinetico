package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.item.custom.CombustionDrillItem;
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
 * Taladro de Combustión en primera persona: se sostiene firme y recto (sin el golpe de brazo de los objetos
 * normales; ver {@code CombustionDrillItem.onEntitySwing}). Mientras taladra, empuja hacia delante y vibra con el
 * motor (la broca gira con el predicado {@code drill_spin}). Al repostar se baja e inclina hacia un lado mientras
 * entra la gasolina. En tercera persona se lleva con las dos manos.
 */
public class DrillClientExtensions implements IClientItemExtensions {

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return HumanoidModel.ArmPose.CROSSBOW_HOLD;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                           float partialTick, float equipProcess, float swingProcess) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        // el objeto que dibuja la mano puede ir un tick por detrás del real (el gasto de gasolina lo cambia): usar el real
        if (player.getMainHandItem().getItem() instanceof CombustionDrillItem && arm == player.getMainArm()) {
            stack = player.getMainHandItem();
        }
        // posición de reposo, sin golpe de brazo ni el bajón que hace vanilla después de cada golpe
        pose.translate(side * 0.5F, -0.5F, -0.66F);
        float t = player.tickCount + partialTick;

        float refuel = CombustionDrillItem.refuelProgress(stack, player.level().getGameTime() + partialTick);
        if (refuel >= 0) {
            // repostar: bajar e inclinar (entra 15 %), mantener con un leve vaivén del bidón, y volver (último 15 %)
            float in = Mth.clamp(refuel / 0.15F, 0, 1), out = Mth.clamp((1 - refuel) / 0.15F, 0, 1);
            float k = smooth(Math.min(in, out));
            pose.translate(-side * 0.12F * k, -0.18F * k, 0.08F * k);
            pose.mulPose(Axis.ZP.rotationDegrees(side * 38.0F * k));
            pose.mulPose(Axis.XP.rotationDegrees(22.0F * k));
            pose.translate(0, 0.012F * k * Mth.sin(t * 0.9F), 0);
            return true;
        }

        var gameMode = Minecraft.getInstance().gameMode;
        boolean drilling = gameMode != null && gameMode.isDestroying() && CombustionDrillItem.hasFuel(stack);
        if (drilling) {
            // empuja contra la roca y vibra con el motor
            pose.translate(0, 0, -0.06F);
            float buzz = 0.012F;
            pose.translate(Mth.sin(t * 9.7F) * buzz, Mth.cos(t * 12.3F) * buzz, Mth.sin(t * 15.1F) * buzz * 0.6F);
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(t * 7.9F) * 1.2F));
        } else if (CombustionDrillItem.hasFuel(stack)) {
            pose.translate(Mth.sin(t * 6.1F) * 0.0015F, Mth.cos(t * 7.3F) * 0.0015F, 0);   // motor al ralentí
        }
        return true;
    }

    private static float smooth(float x) {
        return x * x * (3 - 2 * x);
    }
}
