package com.bryanamaya.tecnocinetico.rules;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Fuego amigo (regla general de la partida): si está desactivado, ningún jugador puede herir a otro jugador, ni con
 * armas del mod, ni cuerpo a cuerpo, ni con proyectiles o explosiones que haya lanzado. Uno mismo sí puede herirse.
 * <p>
 * Se guarda en la configuración de SERVIDOR de cada mundo ({@code <mundo>/serverconfig/tecnocinetico-server.toml}).
 * En LAN se elige solo al abrir el mundo a LAN (botón en esa pantalla, {@code client/LanFriendlyFireOption}); en un
 * servidor dedicado lo pone el administrador en ese archivo.
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID)
public final class FriendlyFire {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        ENABLED = b.comment("Fuego amigo: true = los jugadores pueden herirse entre si; false = no (por defecto).",
                        "Friendly fire: true = players can hurt each other; false = they can't (default).")
                .define("friendlyFire", false);
        SPEC = b.build();
    }

    private FriendlyFire() {}

    public static boolean enabled() {
        try {
            return ENABLED.get();
        } catch (IllegalStateException notLoaded) {               // sin mundo cargado
            return false;
        }
    }

    /** true si el atacante (o quien lanzó el proyectil) es un jugador, el objetivo es OTRO jugador y no hay fuego amigo. */
    public static boolean blocks(Entity attacker, Entity target) {
        return attacker instanceof Player a && target instanceof Player t && a != t && !enabled();
    }

    /** Cambia la regla (en el hilo del servidor) y la guarda en el archivo del mundo. */
    public static void set(MinecraftServer server, boolean on) {
        server.execute(() -> {
            ENABLED.set(on);
            ENABLED.save();
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(LivingAttackEvent event) {
        if (blocks(event.getSource().getEntity(), event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
