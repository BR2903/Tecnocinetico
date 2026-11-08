package com.bryanamaya.tecnocinetico.item.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Módulos de mejora para armas. Cada uno ocupa su propia ranura, así que se pueden combinar todos (salvo
 * los que chocan entre sí, ver {@link #conflictsWith}). Cada módulo es de una familia de arma (pistola,
 * escopeta, rifle) y solo se instala en esa. Se guardan en el NBT del arma como una máscara de bits
 * ("Modules"; bit = 1 << ordinal, así que los nuevos se AÑADEN AL FINAL para no cambiar los guardados).
 */
public enum WeaponModule {
    /** Mira de Puntería Cinética: zoom y retícula al apuntar (no corrige la trayectoria). */
    OPTIC("optic", Family.PISTOL),
    /** Cargador Ampliado: más balas por cargador. */
    EXTENDED_MAG("extended_mag", Family.PISTOL),
    /** Cañón de Inercita: más daño y alcance. */
    INERCITA_BARREL("inercita_barrel", Family.PISTOL),
    /** Acelerador de Recarga: recarga más rápida. */
    QUICK_RELOAD("quick_reload", Family.PISTOL),
    /**
     * Guía de Enjambre: trayectoria automática; la bala busca al mob más cercano al rayo. Se fabrica con
     * Núcleos de Enjambre (Nodos de Colmena) y es, a propósito, el módulo más caro del mod.
     */
    SEEKER("seeker", Family.PISTOL),

    // ---- Escopeta de Caño
    /** Estrangulador: -40 % de dispersión. No se combina con los cañones recortados. */
    CHOKE("choke", Family.SHOTGUN),
    /** Cañones Recortados: +25 % de daño por perdigón, +50 % de dispersión, -30 % de alcance. */
    SAWED_OFF("sawed_off", Family.SHOTGUN),
    /** Cartuchera Rápida: carga los cartuchos un 40 % más rápido. */
    SPEED_LOADER("speed_loader", Family.SHOTGUN),

    // ---- Rifle de Cerrojo
    /** Mira Telescópica: zoom fuerte y visor a pantalla completa al apuntar. */
    SCOPE("scope", Family.RIFLE),
    /** Cerrojo Pulido: un 35 % más de cadencia. */
    POLISHED_BOLT("polished_bolt", Family.RIFLE),
    /** Cañón de Inercita (rifle): +6 de daño y +20 bloques de alcance. */
    RIFLE_BARREL("rifle_barrel", Family.RIFLE);

    /** Tipo de arma al que pertenece cada módulo. */
    public enum Family { PISTOL, SHOTGUN, RIFLE }

    private static final String TAG = "Modules";

    private final String id;
    private final Family family;

    WeaponModule(String id, Family family) {
        this.id = id;
        this.family = family;
    }

    public Family family() {
        return family;
    }

    /** Familia del arma (null si no es un arma con módulos). */
    @org.jetbrains.annotations.Nullable
    public static Family familyOf(ItemStack weapon) {
        if (weapon.getItem() instanceof KineticPistolItem) {
            return Family.PISTOL;
        }
        if (weapon.getItem() instanceof com.bryanamaya.tecnocinetico.item.weapon.LongGunItem g) {
            return g.family();
        }
        return null;
    }

    public boolean appliesTo(ItemStack weapon) {
        return familyOf(weapon) == family;
    }

    /** Módulos incompatibles entre sí (no pueden instalarse a la vez). */
    public boolean conflictsWith(WeaponModule other) {
        return (this == CHOKE && other == SAWED_OFF) || (this == SAWED_OFF && other == CHOKE);
    }

    /**
     * Máscara "local" de 0..7 con los módulos de la familia del arma, en orden (la usa el modelo del arma
     * para elegir la combinación: bit 0 = primer módulo de la familia, etc.).
     */
    public static int localMask(ItemStack weapon) {
        Family f = familyOf(weapon);
        int out = 0, i = 0;
        for (WeaponModule m : values()) {
            if (m.family == f) {
                if (has(weapon, m)) {
                    out |= 1 << i;
                }
                i++;
            }
        }
        return out;
    }

    public String id() {
        return id;
    }

    public int bit() {
        return 1 << ordinal();
    }

    public Component displayName() {
        return Component.translatable("item.tecnocinetico.module_" + id);
    }

    public Component effectText() {
        return Component.translatable("tooltip.tecnocinetico.module_" + id);
    }

    // ------------------------------------------------------------------ NBT del arma

    public static int getMask(ItemStack weapon) {
        CompoundTag tag = weapon.getTag();
        return tag == null ? 0 : tag.getInt(TAG);
    }

    public static boolean has(ItemStack weapon, WeaponModule module) {
        return (getMask(weapon) & module.bit()) != 0;
    }

    public static void install(ItemStack weapon, WeaponModule module) {
        setMask(weapon, getMask(weapon) | module.bit());
    }

    public static void setMask(ItemStack weapon, int mask) {
        weapon.getOrCreateTag().putInt(TAG, mask);
    }
}
