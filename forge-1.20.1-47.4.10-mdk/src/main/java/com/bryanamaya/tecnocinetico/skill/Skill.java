package com.bryanamaya.tecnocinetico.skill;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Habilidades del Ingeniero Superviviente. Cada una sube de nivel con la práctica.
 * Para añadir una nueva: añadir una constante aquí + sus textos en los archivos de idioma,
 * describir sus efectos en effects() y llamar a PlayerSkills.addXp(...) donde se practique.
 */
public enum Skill {
    /** Manejo de Armas: menos retroceso y recarga más rápida. Se entrena acertando y matando con armas. */
    WEAPON_HANDLING("weapon_handling", 10),
    /** Ingeniería: menos golpes de martillo en la prensa y menos desgaste. Se entrena fabricando en la prensa. */
    ENGINEERING("engineering", 10),
    /** Minería Cinética: el Martillo Cinético pica más rápido. Se entrena picando con él. */
    MINING("mining", 10);

    private final String id;
    private final int maxLevel;

    Skill(String id, int maxLevel) {
        this.id = id;
        this.maxLevel = maxLevel;
    }

    public String id() {
        return id;
    }

    public int maxLevel() {
        return maxLevel;
    }

    public Component displayName() {
        return Component.translatable("skill.tecnocinetico." + id);
    }

    /** Cómo se entrena (texto para la pantalla de habilidades). */
    public Component howToTrain() {
        return Component.translatable("skill.tecnocinetico." + id + ".train");
    }

    /** XP total necesaria para alcanzar un nivel: 40, 100, 180, 280... (cada nivel cuesta 20 más que el anterior). */
    public static int totalXpForLevel(int level) {
        return 40 * level + 10 * level * (level - 1);
    }

    public int levelForXp(int xp) {
        int level = 0;
        while (level < maxLevel && xp >= totalXpForLevel(level + 1)) {
            level++;
        }
        return level;
    }

    /** Bonificaciones actuales de la habilidad, para mostrarlas al jugador. */
    public List<Component> effects(int level) {
        return switch (this) {
            case WEAPON_HANDLING -> List.of(
                    Component.translatable("skill.tecnocinetico.effect.recoil", Math.round((1 - recoilMultiplier(level)) * 100)),
                    Component.translatable("skill.tecnocinetico.effect.reload", String.format("%.2f", reloadTicks(level) / 20.0F)));
            case ENGINEERING -> List.of(
                    Component.translatable("skill.tecnocinetico.effect.strikes", pressStrikes(level)),
                    Component.translatable("skill.tecnocinetico.effect.hammer_save", Math.round(hammerSaveChance(level) * 100)));
            case MINING -> List.of(
                    Component.translatable("skill.tecnocinetico.effect.mining_speed", Math.round(miningSpeedBonus(level) * 100)));
        };
    }

    // ------------------------------------------------------------------ efectos de Manejo de Armas

    /** Multiplicador de retroceso: 1.0 en nivel 0 -> 0.35 en nivel 10. */
    public static float recoilMultiplier(int weaponLevel) {
        return 1.0F - 0.065F * weaponLevel;
    }

    /** Ticks de recarga: 30 (1,5 s) en nivel 0 -> 20 (1 s) en nivel 10. */
    public static int reloadTicks(int weaponLevel) {
        return 30 - weaponLevel;
    }

    // ------------------------------------------------------------------ efectos de Ingeniería

    /** Golpes de martillo por pieza en la Prensa de Ensamblaje: 3 (niv. 0-4), 2 (niv. 5-9), 1 (niv. 10). */
    public static int pressStrikes(int engineeringLevel) {
        return engineeringLevel >= 10 ? 1 : engineeringLevel >= 5 ? 2 : 3;
    }

    /** Probabilidad de que un golpe de martillo no gaste durabilidad: 5% por nivel (50% en nivel 10). */
    public static float hammerSaveChance(int engineeringLevel) {
        return 0.05F * engineeringLevel;
    }

    // ------------------------------------------------------------------ efectos de Minería Cinética

    /** Velocidad extra al picar con el Martillo Cinético: +6% por nivel (+60% en nivel 10). */
    public static float miningSpeedBonus(int miningLevel) {
        return 0.06F * miningLevel;
    }
}
