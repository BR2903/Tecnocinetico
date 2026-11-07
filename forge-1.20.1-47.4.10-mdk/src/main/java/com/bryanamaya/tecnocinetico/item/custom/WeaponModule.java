package com.bryanamaya.tecnocinetico.item.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Módulos de mejora para armas. Cada uno ocupa su propia ranura, así que se pueden combinar todos.
 * Se guardan en el NBT del arma como una máscara de bits ("Modules").
 */
public enum WeaponModule {
    /** Mira de Puntería Cinética: la bala corrige ligeramente hacia objetivos cercanos al rayo. */
    OPTIC("optic"),
    /** Cargador Ampliado: más balas por cargador. */
    EXTENDED_MAG("extended_mag"),
    /** Cañón de Inercita: más daño y alcance. */
    INERCITA_BARREL("inercita_barrel"),
    /** Acelerador de Recarga: recarga más rápida. */
    QUICK_RELOAD("quick_reload");

    private static final String TAG = "Modules";

    private final String id;

    WeaponModule(String id) {
        this.id = id;
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
