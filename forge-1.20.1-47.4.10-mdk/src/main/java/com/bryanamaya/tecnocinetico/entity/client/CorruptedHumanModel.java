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
        return RemodeledMobLayers.corruptedHuman();
    }

    @Override
    public void setupAnim(T e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        super.setupAnim(e, limbSwing, limbSwingAmount, age, netHeadYaw, headPitch);
        String skin = e instanceof com.bryanamaya.tecnocinetico.entity.corrupted.CorruptedSkin s ? s.skinName() : "wanderer";
        body.getChild("worker_pack").visible = skin.equals("worker");
        head.getChild("worker_helmet").visible = skin.equals("worker");
        leftArm.getChild("crystal_growth").visible = skin.equals("crystal");
        rightArm.getChild("raider_pad").visible = skin.equals("raider") || skin.equals("soldier");
        head.getChild("soldier_helmet").visible = skin.equals("soldier");
        body.getChild("soldier_chest").visible = skin.equals("soldier");
        head.getChild("raider_mask").visible = skin.equals("raider");
        body.getChild("drowned_tank").visible = skin.equals("drowned");
        head.getChild("drowned_mask").visible = skin.equals("drowned");
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
