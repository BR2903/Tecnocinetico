package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.item.custom.GunAnimState;

import com.bryanamaya.tecnocinetico.item.weapon.ThermalLanceItem;
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
 * Lanza Térmica en primera persona (sin golpe de brazo):
 * <ul>
 *   <li>Reposo: a la derecha, con un leve vaivén de respiración.</li>
 *   <li>Apuntando (clic derecho): se lleva al centro, más cerca, y la vista hace zoom (PistolInputHandler).</li>
 *   <li>Disparando: tiembla con el motor de plasma y retrocede un poco; al sobrecalentarse, tiembla y humea.</li>
 *   <li>Recarga (tecla R): baja e inclina el arma, saca el cartucho gastado (baja más), mete el nuevo con un golpe
 *       seco y vuelve a su sitio.</li>
 * </ul>
 * En tercera persona se lleva con las dos manos.
 */
public class LanceClientExtensions implements IClientItemExtensions {

    /** Giro en Y del modelo en primera persona (display firstperson_*hand de thermal_lance.json). */
    private static final float DISPLAY_YAW = 8.0F;

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return HumanoidModel.ArmPose.CROSSBOW_HOLD;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                           float partialTick, float equipProcess, float swingProcess) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        if (player.getMainHandItem().getItem() instanceof ThermalLanceItem && arm == player.getMainArm()) {
            stack = player.getMainHandItem();                                               // el real (el NBT cambia cada tick)
        }
        float t = player.tickCount + partialTick;
        float aim = smooth(GunAnimState.aim(partialTick));
        // reposo -> apuntando (al centro y más cerca)
        pose.translate(Mth.lerp(aim, side * 0.5F, 0.0F), Mth.lerp(aim, -0.52F, -0.44F), Mth.lerp(aim, -0.66F, -0.5F));
        // el modelo viene girado 8° hacia dentro en primera persona (de cadera se ve en diagonal); al apuntar se
        // compensa para que el cañón quede recto al centro, como las demás armas
        pose.mulPose(Axis.YP.rotationDegrees(-side * DISPLAY_YAW * aim));
        pose.translate(0, -0.3F * equipProcess, 0);
        pose.translate(0, Mth.sin(t * 0.08F) * 0.004F * (1 - aim), 0);                       // respiración

        float reload = ThermalLanceItem.reloadProgress(stack, player.level().getGameTime() + partialTick);
        if (reload >= 0) {
            // bajar e inclinar (0-20 %), sacar el cartucho (20-45 %: baja más), meter el nuevo (45-70 %: sube con golpe),
            // volver (70-100 %)
            float down = smooth(Mth.clamp(reload / 0.2F, 0, 1)) * (1 - smooth(Mth.clamp((reload - 0.7F) / 0.3F, 0, 1)));
            float pull = smooth(Mth.clamp((reload - 0.2F) / 0.25F, 0, 1)) * (1 - smooth(Mth.clamp((reload - 0.45F) / 0.25F, 0, 1)));
            float slam = reload > 0.6F && reload < 0.7F ? Mth.sin((reload - 0.6F) / 0.1F * Mth.PI) : 0;
            pose.translate(-side * 0.1F * down, -0.22F * down - 0.08F * pull + 0.03F * slam, 0.12F * down);
            pose.mulPose(Axis.ZP.rotationDegrees(side * 32.0F * down));
            pose.mulPose(Axis.XP.rotationDegrees(18.0F * down + 8.0F * pull - 4.0F * slam));
            return true;
        }
        boolean firing = ThermalLanceItem.firing(stack) && ThermalLanceItem.canFire(stack) || LanceInput.firingLocal(stack);
        if (firing) {
            float buzz = 0.008F;
            pose.translate(Mth.sin(t * 11.3F) * buzz, Mth.cos(t * 13.7F) * buzz, 0.03F + Mth.sin(t * 9.1F) * buzz);   // retroceso y temblor
            pose.mulPose(Axis.XP.rotationDegrees(-1.2F + Mth.sin(t * 7.0F) * 0.5F));
        } else if (ThermalLanceItem.overheated(stack)) {
            float buzz = 0.004F;
            pose.translate(Mth.sin(t * 17.0F) * buzz, Mth.cos(t * 19.0F) * buzz, 0);
        }
        return true;
    }

    private static float smooth(float x) {
        return x * x * (3 - 2 * x);
    }
}
