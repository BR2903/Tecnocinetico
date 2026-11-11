package com.bryanamaya.tecnocinetico.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Geometría de los efectos en el mismo espacio de vista que el arma actual. Sin caché de fotogramas. */
public final class HandMuzzleGeometry {
    private HandMuzzleGeometry() {}
    public static Vector3f tip(Matrix4f handPose, ItemTransform display, boolean left, Vec3 raw) {
        PoseStack copy = new PoseStack();
        copy.mulPoseMatrix(handPose);
        display.apply(left, copy);
        return copy.last().pose().transformPosition(new Vector3f((float)raw.x/16-.5F,
                (float)raw.y/16-.5F,(float)raw.z/16-.5F));
    }
    public static Vector3f[] flash(Vector3f p, float radius) {
        return new Vector3f[]{new Vector3f(p.x-radius,p.y-radius,p.z),new Vector3f(p.x+radius,p.y-radius,p.z),
                new Vector3f(p.x+radius,p.y+radius,p.z),new Vector3f(p.x-radius,p.y+radius,p.z)};
    }
    public static Vector3f[] beam(Vector3f start, Vector3f end, float width) {
        float dx=end.x-start.x,dy=end.y-start.y;
        float length=(float)Math.sqrt(dx*dx+dy*dy);
        float x=length>.0001F ? -dy/length*width : width;
        float y=length>.0001F ? dx/length*width : 0;
        return new Vector3f[]{new Vector3f(start.x-x,start.y-y,start.z),new Vector3f(end.x-x,end.y-y,end.z),
                new Vector3f(end.x+x,end.y+y,end.z),new Vector3f(start.x+x,start.y+y,start.z)};
    }
}
