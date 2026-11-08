package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.item.weapon.KineticGrenadeItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * Animación en primera persona de la Granada Cinética:
 * - al quitar la anilla la granada sube y se acerca (3 ticks);
 * - luego el brazo se echa atrás y arriba según la fuerza acumulada (1 s);
 * - en el último segundo de mecha tiembla cada vez más (aviso de que va a estallar);
 * - al lanzar, el balanceo normal del brazo hace de lanzamiento.
 */
public class GrenadeClientExtensions implements IClientItemExtensions {

    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                           float partialTick, float equipProcess, float swingProcess) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        boolean using = player.isUsingItem() && player.getUseItem() == stack;
        // posición base de un objeto en la mano (como vanilla), con el balanceo del lanzamiento
        float swing = Mth.sin(Mth.sqrt(swingProcess) * Mth.PI);
        pose.translate(side * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F);
        if (swingProcess > 0) {
            pose.translate(0, 0.1F * swing, -0.35F * swing);
            pose.mulPose(Axis.XP.rotationDegrees(-60.0F * swing));
        }
        if (!using) {
            return true;
        }
        float held = KineticGrenadeItem.heldTicks(stack, player.getUseItemRemainingTicks()) + partialTick;
        float lift = Math.min(1.0F, held / 3.0F);
        float wind = Mth.clamp((held - 3.0F) / KineticGrenadeItem.FULL_POWER, 0.0F, 1.0F);
        wind = wind * wind * (3 - 2 * wind);
        // anilla fuera: sube y se acerca a la cámara
        pose.translate(-side * 0.08F * lift, 0.12F * lift, 0.1F * lift);
        // carga: brazo atrás, arriba e inclinado hacia fuera
        pose.translate(side * 0.1F * wind, 0.22F * wind, 0.3F * wind);
        pose.mulPose(Axis.XP.rotationDegrees(35.0F * wind));
        pose.mulPose(Axis.ZP.rotationDegrees(-side * 15.0F * wind));
        // último segundo de mecha: tiembla
        float danger = Mth.clamp((held - (KineticGrenadeItem.FUSE - 20)) / 20.0F, 0.0F, 1.0F);
        if (danger > 0) {
            float t = player.tickCount + partialTick;
            pose.translate(Mth.sin(t * 2.7F) * 0.012F * danger, Mth.cos(t * 3.3F) * 0.012F * danger, 0);
        }
        return true;
    }
}
