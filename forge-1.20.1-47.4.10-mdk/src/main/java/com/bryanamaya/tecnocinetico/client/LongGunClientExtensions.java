package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.item.custom.GunAnimState;
import com.bryanamaya.tecnocinetico.item.weapon.LongGunItem;
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
 * Animación en primera persona de las armas largas (escopeta, rifle): misma base que la pistola (de la cadera a
 * apuntar con las miras centradas) con más retroceso, y una recarga cartucho a cartucho (el arma se inclina y
 * hay un empujón cada vez que entra un cartucho).
 */
public class LongGunClientExtensions implements IClientItemExtensions {

    private static final float AIM_X_RIGHT = 0.0625F, AIM_X_LEFT = 0.031F;
    private static final float AIM_Y = -0.45F, AIM_Z = -0.38F;

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return HumanoidModel.ArmPose.CROSSBOW_HOLD;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                           float partialTick, float equipProcess, float swingProcess) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float t = GunAnimState.ticksSinceShot(player.level().getGameTime(), partialTick);
        boolean recoiling = t >= 0 && t < GunAnimState.RECOIL_TICKS + 4;
        float equip = recoiling ? 0.0F : equipProcess;

        float aim = smooth(GunAnimState.aim(partialTick));
        float hipX = side * 0.5F, hipY = -0.5F + equip * -0.6F, hipZ = -0.66F;
        pose.translate(Mth.lerp(aim, hipX, side > 0 ? AIM_X_RIGHT : AIM_X_LEFT), Mth.lerp(aim, hipY, AIM_Y), Mth.lerp(aim, hipZ, AIM_Z));

        // recarga: arma inclinada y empujón de pulgar en cada cartucho
        if (LongGunItem.isLoading(stack)) {
            float p = LongGunItem.loadProgress(stack, partialTick);
            pose.translate(-side * 0.1F, -0.12F, 0.06F);
            pose.mulPose(Axis.ZP.rotationDegrees(side * 32.0F));
            pose.mulPose(Axis.XP.rotationDegrees(14.0F));
            float push = p > 0.7F ? (float) Math.sin((p - 0.7F) / 0.3F * Math.PI) : 0.0F;
            pose.translate(0, 0.03F * push, -0.03F * push);
        }
        // retroceso: más fuerte y más lento que la pistola
        if (recoiling) {
            float k = recoil(t) * GunAnimState.kickStrength;
            pose.translate(0, 0.05F * k, 0.28F * k);
            pose.mulPose(Axis.XP.rotationDegrees(18.0F * k));
            pose.mulPose(Axis.ZP.rotationDegrees(side * 3.5F * GunAnimState.roll * k));
        }
        return true;
    }

    private static float recoil(float t) {
        if (t < 1.0F) {
            return t;
        }
        float s = t - 1.0F;
        return (float) (Math.exp(-s * 0.4F) * Math.cos(s * 0.35F));
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3 - 2 * x);
    }
}
