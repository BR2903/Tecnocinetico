package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Armaduras en 3D (como las de Techguns): cada set tiene un modelo por pieza (casco, pecho+brazos, pantalones,
 * botas) hecho de cubos que sobresalen del cuerpo, descrito en assets/tecnocinetico/armor_models/<set>.json y con
 * su textura textures/models/armor/3d/<set>.png (las dos las genera tools/armor3d_gen.py). Se cargan la primera
 * vez que se dibujan y se vacían al recargar recursos.
 */
public final class Armor3DModels {

    private static final Map<String, HumanoidModel<LivingEntity>> CACHE = new HashMap<>();
    private static final String[] BONES = {"head", "hat", "body", "right_arm", "left_arm", "right_leg", "left_leg"};
    private static final Map<String, PartPose> POSES = Map.of(
            "head", PartPose.ZERO, "hat", PartPose.ZERO, "body", PartPose.ZERO,
            "right_arm", PartPose.offset(-5.0F, 2.0F, 0.0F), "left_arm", PartPose.offset(5.0F, 2.0F, 0.0F),
            "right_leg", PartPose.offset(-1.9F, 12.0F, 0.0F), "left_leg", PartPose.offset(1.9F, 12.0F, 0.0F));

    private Armor3DModels() {}

    public static void clear() {
        CACHE.clear();
    }

    public static ResourceLocation texture(String set) {
        return ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/models/armor/3d/" + set + ".png");
    }

    private static String slotKey(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> "head";
            case CHEST -> "chest";
            case LEGS -> "legs";
            default -> "feet";
        };
    }

    /** Modelo de la pieza `slot` del set (o null si no hay datos: entonces se usa el plano de siempre). */
    public static HumanoidModel<LivingEntity> get(String set, EquipmentSlot slot) {
        String key = set + "/" + slotKey(slot);
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }
        HumanoidModel<LivingEntity> model = null;
        try {
            var res = Minecraft.getInstance().getResourceManager()
                    .getResource(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "armor_models/" + set + ".json"));
            if (res.isPresent()) {
                try (var reader = new InputStreamReader(res.get().open(), StandardCharsets.UTF_8)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    model = build(json, slotKey(slot));
                }
            }
        } catch (Exception e) {
            Tecnocinetico.LOGGER.error("No se pudo cargar la armadura 3D {}", key, e);
        }
        CACHE.put(key, model);
        return model;
    }

    private static HumanoidModel<LivingEntity> build(JsonObject json, String slot) {
        JsonArray size = json.getAsJsonArray("texture");
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        Map<String, CubeListBuilder> builders = new HashMap<>();
        for (String b : BONES) {
            builders.put(b, CubeListBuilder.create());
        }
        JsonArray cubes = json.getAsJsonObject("slots").getAsJsonArray(slot);
        if (cubes != null) {
            for (JsonElement el : cubes) {
                JsonObject c = el.getAsJsonObject();
                CubeListBuilder cb = builders.get(c.get("bone").getAsString());
                JsonArray uv = c.getAsJsonArray("uv"), from = c.getAsJsonArray("from"), dim = c.getAsJsonArray("size");
                cb.texOffs(uv.get(0).getAsInt(), uv.get(1).getAsInt());
                cb.mirror(c.has("mirror") && c.get("mirror").getAsBoolean());
                float inflate = c.has("inflate") ? c.get("inflate").getAsFloat() : 0.0F;
                cb.addBox(from.get(0).getAsFloat(), from.get(1).getAsFloat(), from.get(2).getAsFloat(),
                        dim.get(0).getAsFloat(), dim.get(1).getAsFloat(), dim.get(2).getAsFloat(), new CubeDeformation(inflate));
            }
        }
        for (String b : BONES) {
            root.addOrReplaceChild(b, builders.get(b), POSES.get(b));
        }
        ModelPart part = LayerDefinition.create(mesh, size.get(0).getAsInt(), size.get(1).getAsInt()).bakeRoot();
        return new HumanoidModel<>(part);
    }
}
