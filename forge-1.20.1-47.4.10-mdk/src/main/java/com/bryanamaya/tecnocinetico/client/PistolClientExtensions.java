package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.item.custom.GunAnimState;
import com.bryanamaya.tecnocinetico.item.custom.KineticPistolItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * Animaciones de la Pistola Cinética (solo cliente):
 * - Primera persona: retroceso del arma al disparar y movimiento de recarga (inclinar, cambiar cargador, montar).
 * - Tercera persona: postura de apuntar con las dos manos.
 * Los cambios de piezas (cargador fuera, corredera atrás) los hacen los fotogramas del modelo.
 */
public class PistolClientExtensions implements IClientItemExtensions {

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return HumanoidModel.ArmPose.CROSSBOW_HOLD;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                           float partialTick, float equipProcess, float swingProcess) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;

        // Posición base del arma en la mano (la misma que usa vanilla)
        pose.translate(side * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F);

        // --- Recarga ---
        float progress = KineticPistolItem.getReloadProgress(stack, partialTick);
        if (progress > 0.0F) {
            // Entra y sale de la pose suavemente; en medio se mantiene inclinada
            float pose01 = smooth(Math.min(1.0F, Math.min(progress / 0.15F, (1.0F - progress) / 0.15F)));
            pose.translate(-side * 0.08F * pose01, -0.10F * pose01, 0.05F * pose01);
            pose.mulPose(Axis.ZP.rotationDegrees(side * 28.0F * pose01));   // gira el arma hacia dentro
            pose.mulPose(Axis.XP.rotationDegrees(18.0F * pose01));          // levanta la boca del cañón

            // Golpe de palma al meter el cargador (~55 %)
            float slap = bump(progress, 0.55F, 0.06F);
            pose.translate(0, 0.04F * slap, 0);
            // Tirón al montar la corredera (~75 %)
            float rack = bump(progress, 0.76F, 0.07F);
            pose.translate(0, 0, 0.06F * rack);
            pose.mulPose(Axis.XP.rotationDegrees(-6.0F * rack));
        }

        // --- Retroceso al disparar ---
        float t = (player.level().getGameTime() - GunAnimState.lastShotTick) + partialTick;
        if (t >= 0 && t < 10) {
            float kick = (float) Math.exp(-t * 0.55F) * GunAnimState.kickStrength;
            pose.translate(0, 0.015F * kick, 0.11F * kick);                   // empuja el arma hacia atrás
            pose.mulPose(Axis.XP.rotationDegrees(9.0F * kick));               // y levanta la boca
        }
        return true;
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3 - 2 * x);
    }

    /** Pico suave centrado en `center` con anchura `width` (0 fuera de rango, 1 en el centro). */
    private static float bump(float x, float center, float width) {
        float d = Math.abs(x - center) / width;
        return d >= 1.0F ? 0.0F : (float) Math.cos(d * Math.PI / 2);
    }
}
