package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedGunnerEntity;
import com.bryanamaya.tecnocinetico.item.weapon.LongGunItem;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Monster;

/**
 * Modelo de los Corruptos humanos: modelo de jugador (con capa exterior para el relieve de los cristales).
 * Desarmados: brazos al frente como un zombi. Tiradores: apuntan con el arma (a dos manos si es larga).
 */
public class CorruptedHumanModel<T extends Monster> extends PlayerModel<T> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "corrupted"), "main");

    public CorruptedHumanModel(ModelPart root) {
        super(root, false);
    }

    public static LayerDefinition createLayer() {
        return LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64);
    }

    @Override
    public void setupAnim(T e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        super.setupAnim(e, limbSwing, limbSwingAmount, age, netHeadYaw, headPitch);
        boolean armed = !e.getMainHandItem().isEmpty();
        if (e instanceof CorruptedGunnerEntity && armed && e.isAggressive()) {
            if (e.getMainHandItem().getItem() instanceof LongGunItem) {
                AnimationUtils.animateCrossbowHold(rightArm, leftArm, head, true);       // a dos manos
            } else {
                rightArm.xRot = -Mth.HALF_PI + head.xRot;                               // pistola a una mano
                rightArm.yRot = -0.1F + head.yRot;
            }
        } else if (!armed) {
            AnimationUtils.animateZombieArms(leftArm, rightArm, e.isAggressive(), attackTime, age);
        }
        leftSleeve.copyFrom(leftArm);
        rightSleeve.copyFrom(rightArm);
    }
}
