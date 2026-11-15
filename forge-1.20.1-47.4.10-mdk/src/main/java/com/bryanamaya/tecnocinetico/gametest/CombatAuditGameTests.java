package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.MobVariants;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Volcado de las estadísticas de combate de todos los enemigos (los del mod y los vanilla mejorados del Nether/End) a
 * {@code run/combat_audit.json}, para {@code tools/combat_audit.py}. Solo con la variable de entorno {@code TECNO_AUDIT=1}.
 */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class CombatAuditGameTests {

    private static double attr(LivingEntity e, Attribute a) {
        var inst = e.getAttribute(a);
        return inst == null ? 0 : inst.getValue();
    }

    @GameTest(template = "empty", batch = "auditoria_combate")
    public static void dumpCombat(GameTestHelper h) {
        if (!"1".equals(System.getenv("TECNO_AUDIT"))) {
            h.succeed();
            return;
        }
        var level = h.getLevel();
        List<Object[]> types = new ArrayList<>();                                  // {tipo, variante}
        for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
            if (BuiltInRegistries.ENTITY_TYPE.getKey(t).getNamespace().equals(Tecnocinetico.MOD_ID)) {
                types.add(new Object[]{t, null});
            }
        }
        for (EntityType<?> t : List.of(EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.ZOMBIFIED_PIGLIN, EntityType.GHAST,
                EntityType.BLAZE, EntityType.WITHER_SKELETON)) {
            types.add(new Object[]{t, MobVariants.NETHER});
        }
        for (EntityType<?> t : List.of(EntityType.ENDERMAN, EntityType.SHULKER, EntityType.ENDERMITE)) {
            types.add(new Object[]{t, MobVariants.END});
        }
        for (EntityType<?> t : List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CREEPER, EntityType.ENDER_DRAGON,
                EntityType.WITHER, EntityType.WARDEN)) {
            types.add(new Object[]{t, null});
        }
        JsonArray out = new JsonArray();
        for (Object[] o : types) {
            EntityType<?> t = (EntityType<?>) o[0];
            var e = t.create(level);
            if (!(e instanceof Mob mob)) {
                if (e != null) {
                    e.discard();
                }
                continue;
            }
            mob.moveTo(0, -60, 0);
            if (o[1] != null) {
                MobVariants.mark(mob, (String) o[1]);
                com.bryanamaya.tecnocinetico.nether.NetherMobEvents.join(new EntityJoinLevelEvent(mob, level));
                com.bryanamaya.tecnocinetico.end.EndMobEvents.join(new EntityJoinLevelEvent(mob, level));
            }
            JsonObject j = new JsonObject();
            j.addProperty("id", BuiltInRegistries.ENTITY_TYPE.getKey(t) + (o[1] != null ? "#" + o[1] : ""));
            j.addProperty("health", mob.getMaxHealth());
            j.addProperty("armor", attr(mob, Attributes.ARMOR));
            j.addProperty("toughness", attr(mob, Attributes.ARMOR_TOUGHNESS));
            j.addProperty("attack", attr(mob, Attributes.ATTACK_DAMAGE));
            j.addProperty("speed", attr(mob, Attributes.MOVEMENT_SPEED));
            j.addProperty("knockback_res", attr(mob, Attributes.KNOCKBACK_RESISTANCE));
            out.add(j);
            mob.discard();
        }
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("combat_audit.json"), new GsonBuilder().setPrettyPrinting().create().toJson(out));
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
        h.succeed();
    }
}
