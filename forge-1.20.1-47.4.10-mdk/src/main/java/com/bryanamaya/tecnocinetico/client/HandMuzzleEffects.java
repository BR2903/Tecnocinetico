package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.network.WeaponEffectPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.Set;

/** Efectos del tirador sujetos al cañón del fotograma actual, durante el retroceso de armas lentas. */
public final class HandMuzzleEffects {
    private static final Set<String> WEAPONS = Set.of("pipe_shotgun", "pump_shotgun", "bolt_rifle", "semi_rifle",
            "rail_rifle", "kinetic_sniper", "revolver", "grenade_launcher", "rocket_launcher");
    private static final RenderType GLOW = RenderType.eyes(ResourceLocation.fromNamespaceAndPath(
            Tecnocinetico.MOD_ID, "textures/particle/kinetic_glow.png"));
    private record Shot(WeaponEffectPacket packet, double received) {}
    private static final ArrayList<Shot> shots = new ArrayList<>();

    private HandMuzzleEffects() {}
    public static void clear() { shots.clear(); }

    /** Solo cambia la presentación local de las armas reportadas. Otros jugadores usan los efectos del mundo. */
    public static boolean queue(WeaponEffectPacket packet) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || packet.owner() != mc.player.getId()
                || !mc.options.getCameraType().isFirstPerson() || mc.options.hideGui || PistolInputHandler.scoped()
                || !mc.player.getMainHandItem().is(Item.byId(packet.item()))) return false;
        var id = ForgeRegistries.ITEMS.getKey(Item.byId(packet.item()));
        if (id == null || !WEAPONS.contains(id.getPath())) return false;
        if (shots.size() >= 64) shots.remove(0);
        shots.add(new Shot(packet, mc.level.getGameTime() + mc.getFrameTime()));
        return true;
    }

    /** La posición recibida ya incluye la pose y el display que también van a dibujar el arma. */
    public static void render(ItemStack stack, Vector3f muzzle, Matrix4f inverseWorldProjection) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || shots.isEmpty()) return;
        double now = mc.level.getGameTime() + mc.getFrameTime();
        shots.removeIf(s -> now-s.received > 2 || now<s.received-1 || Item.byId(s.packet.item()) != stack.getItem());
        if (shots.isEmpty() || muzzle.z >= -.05F) return;
        var consumer = mc.renderBuffers().bufferSource().getBuffer(GLOW);
        Matrix4f worldProjection = new Matrix4f(inverseWorldProjection).invert();
        Matrix4f inverseHandProjection = new Matrix4f(RenderSystem.getProjectionMatrix()).invert();
        Matrix3f worldToView = new Matrix3f(RenderSystem.getInverseViewRotationMatrix()).invert();
        var camera = mc.gameRenderer.getMainCamera().getPosition();
        for (Shot shot : shots) {
            float fade = (float) Math.max(0, 1-(now-shot.received)/2);
            if (shot.packet.flash()) {
                billboard(consumer, muzzle, .085F, .8F, .95F, 1, fade);
                billboard(consumer, muzzle, .025F, 1, 1, 1, fade);
            }
            if (shot.packet.kind() == WeaponEffectPacket.FLASH) continue;
            var relative = shot.packet.end().subtract(camera);
            Vector3f target = worldToView.transform(new Vector3f((float)relative.x, (float)relative.y, (float)relative.z));
            target = MuzzleProjection.toHandView(target, worldProjection, inverseHandProjection);
            if (target.z < -.05F) beam(consumer, muzzle, target,
                    shot.packet.kind()==WeaponEffectPacket.ARC ? .012F : .005F, fade);
        }
    }

    private static void billboard(VertexConsumer out, Vector3f p, float radius, float r, float g, float b, float alpha) {
        var points = HandMuzzleGeometry.flash(p,radius);
        for (int i=0;i<4;i++) vertex(out,points[i].x,points[i].y,points[i].z,
                i==1||i==2?1:0,i<2?1:0,r,g,b,alpha);
    }

    private static void beam(VertexConsumer out, Vector3f start, Vector3f end, float width, float alpha) {
        var points = HandMuzzleGeometry.beam(start,end,width);
        for (int i=0;i<4;i++) vertex(out,points[i].x,points[i].y,points[i].z,
                i==1||i==2?1:0,i<2?1:0,.35F,.85F,1,alpha);
    }

    private static void vertex(VertexConsumer out,float x,float y,float z,float u,float v,float r,float g,float b,float a) {
        out.vertex(x,y,z).color(r,g,b,a).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728640).normal(0,0,1).endVertex();
    }
}
