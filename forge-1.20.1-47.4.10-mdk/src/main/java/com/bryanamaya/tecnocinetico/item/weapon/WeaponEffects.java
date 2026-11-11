package com.bryanamaya.tecnocinetico.item.weapon;

import com.bryanamaya.tecnocinetico.network.ModNetwork;
import com.bryanamaya.tecnocinetico.network.WeaponEffectPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/** Posición física de salida y avisos visuales autorizados exclusivamente por el servidor. */
public final class WeaponEffects {
    private WeaponEffects() {}

    public static Vec3 muzzle(Player player, ItemStack stack, boolean aiming) {
        Vec3 point = WeaponMuzzles.modelPoint(stack).scale(1.0 / 16).subtract(.5, .5, .5);
        boolean right = player.getMainArm() == HumanoidArm.RIGHT;
        double side = right ? 1 : -1;
        double x = (aiming ? (right ? .0625 : -.03125) : side * .5) + (right ? -.0625 : .03125);
        double y = (aiming ? -.45 : -.5) + point.y + (stack.getItem() instanceof MinigunItem ? 5.5 / 16 : 0);
        double forward = -((aiming ? -.38 : -.66) + point.z + (right ? -3.25 : -3) / 16);
        Vec3 look = player.getLookAngle();
        Vec3 lateral = Vec3.directionFromRotation(0, player.getYRot() + 90);
        Vec3 up = lateral.cross(look).normalize();
        Vec3 from = player.getEyePosition().add(look.scale(forward)).add(lateral.scale(x)).add(up.scale(y));
        var hit = player.level().clip(new ClipContext(player.getEyePosition(), from, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? from : hit.getLocation().subtract(look.scale(.04));
    }

    public static void send(ServerPlayer player, ItemStack stack, boolean aiming, int kind, Vec3 end, boolean flash) {
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new WeaponEffectPacket(player.getId(), Item.getId(stack.getItem()), kind, flash, muzzle(player, stack, aiming), end));
    }
}
