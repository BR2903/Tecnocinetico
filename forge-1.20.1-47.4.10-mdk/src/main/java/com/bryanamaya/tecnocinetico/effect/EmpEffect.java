package com.bryanamaya.tecnocinetico.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Pulso EMP (Dragón modificado al aterrizar): mientras dura, los gadgets del Equipo Táctico no funcionan
 * ({@code TacticalGear.active(Player, int)}), salvo los de nivel Mk III (apantallados).
 */
public class EmpEffect extends MobEffect {
    public EmpEffect() {
        super(MobEffectCategory.HARMFUL, 0x8A7CFF);
    }
}
