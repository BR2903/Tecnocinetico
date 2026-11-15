package com.bryanamaya.tecnocinetico.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pasada brillante (RenderType.eyes) sin redibujar el modelo entero: la primera vez que un modelo se dibuja con una
 * textura de brillo, mira qué piezas tienen algún píxel encendido bajo sus UV; en esa pasada las demás no se dibujan
 * (skipDraw) y las ramas enteras sin brillo se ocultan. El resultado en pantalla es idéntico (eyes es aditivo: un
 * píxel negro no suma nada) y la pasada cuesta solo lo que brilla.
 * Uso: {@link #eyes} en vez de {@code RenderType.eyes} (registra la textura) y {@link #render} en vez de
 * {@code model.renderToBuffer} en la capa brillante; o {@link GlowEyesLayer}.
 */
public final class GlowMask {

    public static final int FULL_BRIGHT = 15728640;
    private static final Map<RenderType, ResourceLocation> TEXTURES = new ConcurrentHashMap<>();
    private static final Map<Model, Map<ResourceLocation, Mask>> MASKS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Class<?>, List<Field>> PART_FIELDS = new ConcurrentHashMap<>();
    private static final Mask NONE = new Mask(new ModelPart[0], new ModelPart[0]);

    /** hide: ramas sin ningún brillo (visible=false); skip: piezas sin brillo propio pero con hijos que brillan. */
    private record Mask(ModelPart[] hide, ModelPart[] skip) {}

    private GlowMask() {}

    /** Como {@code RenderType.eyes(texture)}, pero recordando la textura para poder recortar el modelo. */
    public static RenderType eyes(ResourceLocation texture) {
        RenderType type = RenderType.eyes(texture);
        TEXTURES.put(type, texture);
        return type;
    }

    public static void render(Model model, RenderType type, PoseStack pose, MultiBufferSource buffers, float r, float g, float b, float a) {
        render(model, type, pose, buffers.getBuffer(type), r, g, b, a);
    }

    public static void render(Model model, RenderType type, PoseStack pose, VertexConsumer vc, float r, float g, float b, float a) {
        ResourceLocation tex = TEXTURES.get(type);
        Mask m = tex == null ? NONE : MASKS.computeIfAbsent(model, k -> new HashMap<>()).computeIfAbsent(tex, t -> build(model, t));
        if (m == NONE) {
            model.renderToBuffer(pose, vc, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, r, g, b, a);
            return;
        }
        boolean[] visible = new boolean[m.hide.length];
        boolean[] skip = new boolean[m.skip.length];
        for (int i = 0; i < m.hide.length; i++) {
            visible[i] = m.hide[i].visible;
            m.hide[i].visible = false;
        }
        for (int i = 0; i < m.skip.length; i++) {
            skip[i] = m.skip[i].skipDraw;
            m.skip[i].skipDraw = true;
        }
        try {
            model.renderToBuffer(pose, vc, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, r, g, b, a);
        } finally {
            for (int i = 0; i < m.hide.length; i++) {
                m.hide[i].visible = visible[i];
            }
            for (int i = 0; i < m.skip.length; i++) {
                m.skip[i].skipDraw = skip[i];
            }
        }
    }

    /** Las piezas del modelo: todos sus campos ModelPart (de la clase y sus padres) y sus descendientes. */
    private static Set<ModelPart> parts(Model model) {
        Set<ModelPart> out = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Field f : PART_FIELDS.computeIfAbsent(model.getClass(), GlowMask::partFields)) {
            try {
                if (f.get(model) instanceof ModelPart p) {
                    p.getAllParts().forEach(out::add);
                }
            } catch (IllegalAccessException ignored) {
                // campo inaccesible: esas piezas se dibujan siempre
            }
        }
        return out;
    }

    private static List<Field> partFields(Class<?> c) {
        List<Field> out = new ArrayList<>();
        for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
            for (Field f : k.getDeclaredFields()) {
                if (ModelPart.class.isAssignableFrom(f.getType()) && !Modifier.isStatic(f.getModifiers())) {
                    try {
                        f.setAccessible(true);
                        out.add(f);
                    } catch (RuntimeException ignored) {
                        // módulo cerrado: se ignora
                    }
                }
            }
        }
        return out;
    }

    private static Mask build(Model model, ResourceLocation tex) {
        var res = Minecraft.getInstance().getResourceManager().getResource(tex);
        if (res.isEmpty()) {
            return NONE;
        }
        Set<ModelPart> all = parts(model);
        if (all.isEmpty()) {
            return NONE;
        }
        Map<ModelPart, Boolean> lit = new IdentityHashMap<>();
        try (var in = res.get().open(); NativeImage img = NativeImage.read(in)) {
            for (ModelPart p : all) {
                lit.put(p, lit(p, img));
            }
        } catch (Exception e) {
            return NONE;
        }
        Set<ModelPart> dark = Collections.newSetFromMap(new IdentityHashMap<>());
        List<ModelPart> skip = new ArrayList<>();
        for (ModelPart p : all) {
            if (p.getAllParts().noneMatch(q -> lit.getOrDefault(q, true))) {
                dark.add(p);
            } else if (!lit.get(p) && !p.cubes.isEmpty()) {
                skip.add(p);
            }
        }
        // ocultar solo la pieza más alta de cada rama apagada (sus descendientes ya no se recorren)
        Set<ModelPart> covered = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ModelPart p : dark) {
            p.getAllParts().skip(1).forEach(covered::add);
        }
        List<ModelPart> hide = new ArrayList<>();
        for (ModelPart p : new LinkedHashSet<>(dark)) {
            if (!covered.contains(p)) {
                hide.add(p);
            }
        }
        return hide.isEmpty() && skip.isEmpty() ? NONE : new Mask(hide.toArray(ModelPart[]::new), skip.toArray(ModelPart[]::new));
    }

    /** ¿Algún píxel encendido (color no negro: eyes suma el color) bajo las caras de los cubos de esta pieza? */
    private static boolean lit(ModelPart part, NativeImage img) {
        int w = img.getWidth(), h = img.getHeight();
        for (ModelPart.Cube cube : part.cubes) {
            for (ModelPart.Polygon poly : cube.polygons) {
                float u0 = 1, v0 = 1, u1 = 0, v1 = 0;
                for (ModelPart.Vertex v : poly.vertices) {
                    u0 = Math.min(u0, v.u);
                    u1 = Math.max(u1, v.u);
                    v0 = Math.min(v0, v.v);
                    v1 = Math.max(v1, v.v);
                }
                int x0 = Math.max(0, (int) Math.floor(u0 * w)), x1 = Math.min(w, Math.max(x0 + 1, (int) Math.ceil(u1 * w)));
                int y0 = Math.max(0, (int) Math.floor(v0 * h)), y1 = Math.min(h, Math.max(y0 + 1, (int) Math.ceil(v1 * h)));
                for (int y = y0; y < y1; y++) {
                    for (int x = x0; x < x1; x++) {
                        int abgr = img.getPixelRGBA(x, y);
                        if ((abgr & 0xFF) > 3 || (abgr >> 8 & 0xFF) > 3 || (abgr >> 16 & 0xFF) > 3) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
