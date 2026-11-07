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

        // Al usar un objeto, Minecraft reinicia la animación de "sacar el objeto" (el arma baja y sube).
        // Justo después de disparar la ignoramos: el movimiento lo da el retroceso de abajo.
        float t = GunAnimState.ticksSinceShot(player.level().getGameTime(), partialTick);
        boolean recoiling = t >= 0 && t < GunAnimState.RECOIL_TICKS;
        float equip = recoiling ? 0.0F : equipProcess;

        // Posición base del arma en la mano (la misma que usa vanilla)
        pose.translate(side * 0.56F, -0.52F + equip * -0.6F, -0.72F);

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
        // Golpe seco hacia atrás en el primer tick y vuelta suave a la posición (curva de muelle amortiguado).
        if (recoiling) {
            float kick = recoilCurve(t) * GunAnimState.kickStrength;
            pose.translate(0, 0.035F * kick, 0.20F * kick);                          // el arma retrocede hacia el jugador
            pose.mulPose(Axis.XP.rotationDegrees(13.0F * kick));                     // la boca del cañón sube
            pose.mulPose(Axis.ZP.rotationDegrees(side * 3.0F * GunAnimState.roll * kick)); // leve giro lateral
        }
        return true;
    }

    /**
     * Curva del retroceso: sube muy rápido (pico en ~0,8 ticks), vuelve con suavidad
     * y pasa un poco por debajo de cero antes de asentarse (como un muelle).
     */
    private static float recoilCurve(float t) {
        if (t < 0.8F) {
            return t / 0.8F;
        }
        float s = t - 0.8F;
        return (float) (Math.exp(-s * 0.55F) * Math.cos(s * 0.45F));
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
