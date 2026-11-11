package com.bryanamaya.tecnocinetico.client;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Las manos y el mundo usan proyecciones distintas: conservar la posición en pantalla de la boca. */
public final class MuzzleProjection {
    private MuzzleProjection() {}
    public static Vector3f toWorldView(Vector3f handPoint, Matrix4f handProjection, Matrix4f inverseWorldProjection) {
        Vector4f clip = handProjection.transform(new Vector4f(handPoint, 1));
        inverseWorldProjection.transform(clip);
        return new Vector3f(clip.x / clip.w, clip.y / clip.w, clip.z / clip.w);
    }
}
