package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.entity.boss.SiegeSpiderEntity;
import com.bryanamaya.tecnocinetico.entity.custom.AnchorEntity;
import com.bryanamaya.tecnocinetico.entity.custom.DroneEntity;
import com.bryanamaya.tecnocinetico.entity.custom.SentryTurretEntity;
import com.bryanamaya.tecnocinetico.entity.swarm.SwarmWaspEntity;
import com.bryanamaya.tecnocinetico.entity.swarm.WaspRole;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

/** Animación de rigs propios. La geometría se genera en RemodeledMobLayers desde tools/mob_remodel_rigs.py. */
public class RemodeledMobModel<T extends Mob> extends HierarchicalModel<T> {
    private final ModelPart root;
    private final String kind;
    protected float flapSpeed = 2.1F;

    public RemodeledMobModel(ModelPart root, String kind) {
        super("swarm_wasp".equals(kind) ? RenderType::entityTranslucent : RenderType::entityCutoutNoCull);
        this.root = root;
        this.kind = kind;
    }
    @Override public ModelPart root() { return root; }

    @Override public void setupAnim(T e, float swing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        amount = Math.min(1, amount);
        switch (kind) {
            case "siege_spider" -> siege((SiegeSpiderEntity) e, swing, amount, age, yaw, pitch);
            case "swarm_body" -> {
                ModelPart body = root.getChild("body"), head = root.getChild("head");
                head.yRot = yaw * Mth.DEG_TO_RAD; head.xRot = pitch * Mth.DEG_TO_RAD;
                String[] names = {"front_right", "front_left", "mid_right", "mid_left", "rear_right", "rear_left"};
                for (int i = 0; i < names.length; i++) {
                    ModelPart leg = root.getChild(names[i]);
                    leg.xRot = Mth.cos(swing * .6662F + (i % 2) * Mth.PI) * amount * .8F;
                    if (i < 2) leg.xRot -= .25F + Mth.sin(attackTime * Mth.PI) * 1.7F;
                    if (i == 2 || i == 3) leg.zRot = i == 2 ? .6F : -.6F;
                    if (!e.onGround() && !e.isInWater()) leg.xRot = i < 2 ? -1.3F : i > 3 ? .9F : .2F;
                }
                body.y += Mth.sin(age * .1F) * .3F;
                float pulse = 1 + Mth.sin(age * .25F) * .06F;
                ModelPart core = body.getChild("core"); core.xScale = pulse; core.yScale = pulse; core.zScale = pulse;
            }
            case "swarm_wasp" -> {
                ModelPart body = root.getChild("body");
                for (String pair : new String[]{"front", "back"}) {
                    float flap = Mth.sin(age * flapSpeed + (pair.equals("back") ? .9F : 0)) * .65F;
                    body.getChild("wing_left_" + pair).zRot = -.3F + flap;
                    body.getChild("wing_right_" + pair).zRot = .3F - flap;
                }
                body.y += Mth.sin(age * .15F) * .6F;
                body.getChild("head").yRot = yaw * Mth.DEG_TO_RAD * .5F;
                body.getChild("head").xRot = pitch * Mth.DEG_TO_RAD * .5F;
                body.getChild("abdomen").xRot += Mth.sin(age * .2F) * .08F;
                WaspRole role = e instanceof SwarmWaspEntity wasp ? wasp.getRole() : WaspRole.STINGER;
                body.getChild("guard0").visible = role == WaspRole.GUARD;
                body.getChild("guard2").visible = role == WaspRole.GUARD;
                body.getChild("head").getChild("scoutSensor").visible = role == WaspRole.SCOUT;
            }
            case "hive_node" -> {
                ModelPart body = root.getChild("body"); body.y += Mth.sin(age * .1F) * .6F;
                body.getChild("crown").yRot = age * .025F;
                for (int i = 0; i < 6; i++) body.getChild("petal" + i).xRot += Mth.sin(age * .08F + i) * .06F;
            }
            case "drone" -> {
                ModelPart body = root.getChild("body"); body.y += Mth.sin(age * .15F) * .6F;
                body.xRot = amount * .35F; body.zRot = Mth.sin(age * .11F) * .05F;
                body.getChild("gun").xRot = Mth.clamp(pitch, -60, 80) * Mth.DEG_TO_RAD - body.xRot;
                float spin = age * (((DroneEntity) e).mode() == 0 ? 1.6F : 2.4F);
                for (int i = 0; i < 4; i++) body.getChild("arm" + i).getChild("rotor" + i).yRot = i % 2 == 0 ? spin : -spin;
            }
            case "sentry_turret" -> {
                ModelPart head = root.getChild("head"); head.yRot = yaw * Mth.DEG_TO_RAD; head.xRot = pitch * Mth.DEG_TO_RAD;
                if (((SentryTurretEntity) e).isCharging()) head.x += Mth.sin(age * 3.1F) * .15F;
            }
            case "anchor" -> {
                ModelPart body = root.getChild("body"), head = body.getChild("head");
                head.yRot = yaw * Mth.DEG_TO_RAD; head.xRot = Mth.clamp(pitch, -60, 30) * Mth.DEG_TO_RAD;
                body.y += Mth.sin(age * .08F) * .3F;
                for (int i = 0; i < 3; i++) {
                    ModelPart leg = root.getChild("leg" + i);
                    leg.yRot += Mth.sin(swing * .55F + i * 2.1F) * amount * .3F;
                    leg.getChild("shin" + i).xRot = Mth.cos(swing * .55F + i * 2.1F) * amount * .25F;
                    leg.y -= Math.max(0, Mth.sin(swing * .55F + i * 2.1F)) * amount * 1.2F;
                }
                if (((AnchorEntity) e).mode() == 1) head.x += Mth.sin(age * 2.7F) * .2F;
                if (((AnchorEntity) e).mode() == 2) body.y += 1.5F;
            }
            case "corrupted_spider" -> {
                ModelPart head = root.getChild("head"); head.yRot = yaw * Mth.DEG_TO_RAD; head.xRot = pitch * Mth.DEG_TO_RAD;
                for (int i = 0; i < 8; i++) {
                    ModelPart leg = root.getChild("leg" + i);
                    float step = swing * .7F + (i % 2) * Mth.PI;
                    leg.yRot += Mth.sin(step) * amount * .35F;
                    leg.y -= Math.max(0, Mth.cos(step)) * amount * 1.5F;
                }
            }
        }
    }

    private void siege(SiegeSpiderEntity e, float swing, float amount, float age, float yaw, float pitch) {
        ModelPart body = root.getChild("body"), turret = body.getChild("turret");
        turret.yRot = yaw * Mth.DEG_TO_RAD; turret.xRot = Mth.clamp(pitch, -30, 30) * Mth.DEG_TO_RAD;
        body.y += Mth.sin(age * .07F) * .3F;
        for (int i = 0; i < 6; i++) {
            boolean right = i < 3;
            ModelPart leg = root.getChild("leg" + i), shin = leg.getChild("shin" + i);
            float ph = swing * .5F + (i % 2 == (right ? 0 : 1) ? 0 : Mth.PI);
            float lift = Math.max(0, Mth.sin(ph)) * .35F * amount;
            leg.zRot += right ? -lift : lift; shin.zRot += right ? lift : -lift;
            leg.yRot += Mth.cos(ph) * .25F * amount;
            if (e.legBroken(i)) {
                shin.zRot += (right ? .55F : -.55F) + Mth.sin(age * 1.7F + i) * .04F;
                shin.xRot += .25F;
            }
            if (e.mode() == 2) leg.y += 2;
            if (e.mode() == 4) { leg.y += 6; leg.zRot += right ? .45F : -.45F; shin.zRot += right ? -.2F : .2F; }
        }
        if (e.mode() == 1) turret.x += Mth.sin(age * 3.1F) * .25F;
        if (e.mode() == 2) body.y += 2.5F;
        if (e.mode() == 3) turret.xRot -= .15F + Mth.sin(age * 2) * .05F;
        if (e.mode() == 4) { body.y += 6; turret.xRot += .35F; }
    }
}
