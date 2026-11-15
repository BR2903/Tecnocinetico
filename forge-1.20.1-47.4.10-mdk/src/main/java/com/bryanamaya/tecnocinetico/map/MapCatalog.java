package com.bryanamaya.tecnocinetico.map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Estructuras importantes que el Reloj de Navegación conoce: las del mod y las vanilla útiles. Cada entrada agrupa uno o
 * varios ids (p. ej. las cinco aldeas) bajo un nombre ({@code map.tecnocinetico.structure.<key>}) y un color de icono.
 */
public final class MapCatalog {

    public record Entry(String key, int color, ResourceKey<Level> dimension, List<ResourceLocation> ids) {
        public String langKey() {
            return "map.tecnocinetico.structure." + key;
        }
    }

    public static final List<Entry> ALL = new ArrayList<>();

    static {
        // Overworld del mod
        mod("ai_installation", 0xFF4FD8, Level.OVERWORLD);
        mod("data_center", 0xC04CFF, Level.OVERWORLD);
        mod("relay_antenna", 0xB070FF, Level.OVERWORLD);
        mod("swarm_hive", 0x73EB96, Level.OVERWORLD);
        mod("swarm_laboratory", 0x4FC77A, Level.OVERWORLD);
        mod("abandoned_factory", 0xC8843C, Level.OVERWORLD);
        mod("ancient_workshop", 0xE0B050, Level.OVERWORLD);
        mod("survivor_settlement", 0x5FA8FF, Level.OVERWORLD);
        mod("survivor_bunker", 0x8CA0B8, Level.OVERWORLD);
        mod("sunken_city", 0x3C78C8, Level.OVERWORLD);
        // Overworld vanilla
        vanilla("village", 0xD8C890, Level.OVERWORLD, "village_plains", "village_desert", "village_savanna", "village_snowy", "village_taiga");
        vanilla("stronghold", 0x6E6E6E, Level.OVERWORLD, "stronghold");
        vanilla("ancient_city", 0x2A5060, Level.OVERWORLD, "ancient_city");
        vanilla("monument", 0x50B0A0, Level.OVERWORLD, "monument");
        vanilla("mansion", 0x7A5530, Level.OVERWORLD, "mansion");
        vanilla("pillager_outpost", 0x9A9A9A, Level.OVERWORLD, "pillager_outpost");
        vanilla("desert_pyramid", 0xE8D48A, Level.OVERWORLD, "desert_pyramid");
        vanilla("jungle_pyramid", 0x6A9A40, Level.OVERWORLD, "jungle_pyramid");
        // Nether
        mod("abyss_foundry", 0xFF4F11, Level.NETHER);
        mod("slag_excavation", 0xFFA525, Level.NETHER);
        mod("cooling_depot", 0x6AB4E0, Level.NETHER);
        mod("infernal_watchpost", 0xF5A72A, Level.NETHER);
        vanilla("fortress", 0x5A2028, Level.NETHER, "fortress");
        vanilla("bastion_remnant", 0x3A3438, Level.NETHER, "bastion_remnant");
        // End
        mod("launch_port", 0xFF4FD8, Level.END);
        mod("end_server_farm", 0xC04CFF, Level.END);
        mod("end_network_node", 0x9A60D0, Level.END);
        vanilla("end_city", 0xC8A8E0, Level.END, "end_city");
    }

    private MapCatalog() {}

    private static void mod(String key, int color, ResourceKey<Level> dim) {
        ALL.add(new Entry(key, color, dim, List.of(ResourceLocation.fromNamespaceAndPath(com.bryanamaya.tecnocinetico.Tecnocinetico.MOD_ID, key))));
    }

    private static void vanilla(String key, int color, ResourceKey<Level> dim, String... ids) {
        List<ResourceLocation> list = new ArrayList<>();
        for (String id : ids) {
            list.add(ResourceLocation.fromNamespaceAndPath("minecraft", id));
        }
        ALL.add(new Entry(key, color, dim, List.copyOf(list)));
    }

    @Nullable
    public static Entry byKey(String key) {
        for (Entry e : ALL) {
            if (e.key.equals(key)) {
                return e;
            }
        }
        return null;
    }

    /** Entrada a la que pertenece una estructura (por su id), o null si no es importante. */
    @Nullable
    public static Entry byStructure(ResourceLocation id) {
        for (Entry e : ALL) {
            if (e.ids.contains(id)) {
                return e;
            }
        }
        return null;
    }

    public static List<Entry> in(ResourceKey<Level> dim) {
        return ALL.stream().filter(e -> e.dimension.equals(dim)).toList();
    }
}
