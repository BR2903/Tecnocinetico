package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.weapon.WeaponMuzzles;
import com.bryanamaya.tecnocinetico.network.WeaponEffectPacket;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Solo presentación. El servidor sigue decidiendo impactos, daño y cuándo se dispara. */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID, value = Dist.CLIENT)
public final class ClientWeaponEffects {
    private static Matrix4f inverseWorldProjection;
    private static ClientLevel capturedLevel;
    private static Item capturedItem;
    private static Vec3 cameraOffset;
    private static long captureTick;

    private ClientWeaponEffects() {}

    @SubscribeEvent
    public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    private static void clear() {
        HandMuzzleEffects.clear();
        capturedLevel = null; capturedItem = null; cameraOffset = null; inverseWorldProjection = null; captureTick = 0;
    }

    @SubscribeEvent
    public static void projection(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            if (capturedLevel != null && capturedLevel != Minecraft.getInstance().level) clear();
            inverseWorldProjection = new Matrix4f(event.getProjectionMatrix()).invert();
        }
    }

    /** Captura después de apuntado, retroceso, vibración y recarga; aplica el display del modelo real. */
    public static void capture(PoseStack pose, ItemStack stack, HumanoidArm arm) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || inverseWorldProjection == null || arm != mc.player.getMainArm()
                || stack != mc.player.getMainHandItem()) return;
        var model = mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0);
        boolean left = arm == HumanoidArm.LEFT;
        Vec3 raw = WeaponMuzzles.modelPoint(stack);
        Vector3f hand = HandMuzzleGeometry.tip(pose.last().pose(), model.getTransforms().getTransform(
                left ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND), left, raw);
        HandMuzzleEffects.render(stack, hand, inverseWorldProjection);
        Vector3f view = MuzzleProjection.toWorldView(hand, RenderSystem.getProjectionMatrix(), inverseWorldProjection);
        RenderSystem.getInverseViewRotationMatrix().transform(view);
        cameraOffset = new Vec3(view.x, view.y, view.z);
        capturedLevel = mc.level; capturedItem = stack.getItem(); captureTick = mc.level.getGameTime();
    }

    public static void receive(WeaponEffectPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null || mc.player == null) return;
        if (HandMuzzleEffects.queue(packet)) return;
        Vec3 muzzle = packet.origin();
        if (packet.owner() == mc.player.getId() && capturedLevel == level && cameraOffset != null
                && capturedItem == Item.byId(packet.item()) && mc.player.getMainHandItem().is(capturedItem)
                && level.getGameTime() - captureTick <= 2 && mc.options.getCameraType().isFirstPerson()
                && !PistolInputHandler.scoped()) {
            Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
            muzzle = eye.add(cameraOffset);
            var wall = level.clip(new ClipContext(eye, muzzle, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
            if (wall.getType() != HitResult.Type.MISS) muzzle = wall.getLocation().subtract(mc.player.getLookAngle().scale(.04));
        }
        var random = level.random;
        if (packet.kind() == WeaponEffectPacket.FLAME) {
            var shooter = level.getEntity(packet.owner());
            Vec3 velocity = shooter == null ? Vec3.ZERO : shooter.getDeltaMovement();
            for (int i = 0; i < 9; i++) {
                Vec3 v = packet.end().scale(.72 + random.nextDouble() * .16)
                        .add(random.nextGaussian() * .045, random.nextGaussian() * .045, random.nextGaussian() * .045).add(velocity);
                level.addParticle(ModParticles.FLAME_STREAM.get(), muzzle.x, muzzle.y, muzzle.z, v.x, v.y, v.z);
            }
            return;
        }
        if (packet.flash()) {
            level.addParticle(ModParticles.KINETIC_BURST.get(), muzzle.x, muzzle.y, muzzle.z, .45, 0, 0);
            for (int i = 0; i < 4; i++) level.addParticle(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z,
                    random.nextGaussian() * .02, .02, random.nextGaussian() * .02);
        }
        if (packet.kind() == WeaponEffectPacket.FLASH) return;
        Vec3 path = packet.end().subtract(muzzle);
        if (packet.kind() == WeaponEffectPacket.PULSE) {
            Vec3 axis = path.normalize();
            Vec3 reference = Math.abs(axis.y) > .95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            Vec3 right = axis.cross(reference).normalize(), up = right.cross(axis).normalize();
            for (double distance = .2; distance <= path.length(); distance += .8) {
                double radius = distance * Math.tan(Math.toRadians(30));
                for (int i = 0; i < 10; i++) {
                    double angle = i * Math.PI / 5 + distance;
                    Vec3 at = muzzle.add(axis.scale(distance)).add(right.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
                    level.addParticle(ModParticles.KINETIC_SPARK.get(), at.x, at.y, at.z, 0, 0, 0);
                }
            }
            return;
        }
        int steps = Math.min(512, Math.max(1, (int) (path.length() * 2)));
        for (int i = 0; i <= steps; i++) {
            Vec3 at = muzzle.add(path.scale(i / (double) steps));
            level.addParticle(ModParticles.KINETIC_TRACER.get(), at.x, at.y, at.z, 0, 0, 0);
            if (packet.kind() == WeaponEffectPacket.ARC && i > 0 && i < steps) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z,
                        random.nextGaussian() * .03, random.nextGaussian() * .03, random.nextGaussian() * .03);
            }
        }
    }
}
