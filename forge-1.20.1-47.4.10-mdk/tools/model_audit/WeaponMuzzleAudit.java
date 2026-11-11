package com.bryanamaya.tecnocinetico.audit;

import com.bryanamaya.tecnocinetico.client.MuzzleProjection;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.ItemTransform;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import java.nio.file.Files;
import java.nio.file.Path;

/** Proyección de las bocas reales de todos los cañones, ambas manos, cadera/mira y tres FOV. */
public final class WeaponMuzzleAudit {
    private static Vector3f vector(JsonObject object,String name,float fallback) {
        if (!object.has(name)) return new Vector3f(fallback);
        var a=object.getAsJsonArray(name);
        return new Vector3f(a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat());
    }
    public static void run(Path root) throws Exception {
        var profiles=JsonParser.parseString(Files.readString(root.resolve("build/weapon_muzzles/profiles.json"))).getAsJsonArray();
        int checks=0;
        for (var entry:profiles) {
            var profile=entry.getAsJsonObject(); String name=profile.get("name").getAsString();
            var data=JsonParser.parseString(Files.readString(root.resolve("src/main/resources/assets/tecnocinetico/models/item/"+name+".json"))).getAsJsonObject();
            var point=vector(profile,"point",0).div(16).sub(.5F,.5F,.5F);
            for (boolean left:new boolean[]{false,true}) for (boolean aim:new boolean[]{false,true}) {
                var display=data.getAsJsonObject("display").getAsJsonObject(left?"firstperson_lefthand":"firstperson_righthand");
                var transform=new ItemTransform(vector(display,"rotation",0),vector(display,"translation",0).div(16),vector(display,"scale",1));
                PoseStack pose=new PoseStack();
                pose.translate(aim?(left?-.03125:.0625):(left?-.5:.5),aim?-.45:-.5,aim?-.38:-.66);
                transform.apply(left,pose);
                Vector3f hand=pose.last().pose().transformPosition(new Vector3f(point));
                if (aim && Math.abs(hand.x)>.001) throw new IllegalStateException("Mira descentrada: "+name+" izquierda="+left);
                for (float fov:new float[]{35,70,110}) {
                    var hp=new Matrix4f().perspective((float)Math.toRadians(70),1.777F,.05F,512);
                    var wp=new Matrix4f().perspective((float)Math.toRadians(fov),1.777F,.05F,512);
                    Vector3f view=MuzzleProjection.toWorldView(hand,hp,new Matrix4f(wp).invert());
                    Vector4f a=hp.transform(new Vector4f(hand,1)),b=wp.transform(new Vector4f(view,1));
                    a.div(a.w); b.div(b.w);
                    if (Math.abs(a.x-b.x)> .0002 || Math.abs(a.y-b.y)> .0002 || Math.abs(a.z-b.z)> .0002)
                        throw new IllegalStateException("Boca fuera de su proyeccion: "+name+" FOV="+fov);
                    checks++;
                }
            }
        }
        System.out.println("BOCAS CORRECTAS: "+profiles.size()+" perfiles; "+checks+" proyecciones, ambas manos y cadera/mira.");
    }
}
