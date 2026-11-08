package com.bryanamaya.tecnocinetico.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Lastre de Inercia: un campo de inercita "pesa" al afectado. En el aire cae a plomo (no anula el daño por caída);
 * en el suelo va más lento y no salta tanto. El jetpack no empuja mientras dure (ver GearClient/GearEvents).
 *
 * El movimiento del jugador lo calcula su cliente, así que el tirón hacia abajo del jugador lo aplica
 * {@code client/GearClient}; aquí solo se arrastra a los demás mobs y se cortan los élitros.
 */
public class InertiaAnchorEffect extends MobEffect {

    public InertiaAnchorEffect() {
        super(MobEffectCategory.HARMFUL, 0x6A7CC8);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, "3f6b0a52-6c55-4c1a-9a2f-7d0e3b9a4c11", -0.25D,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    /** Tirón hacia abajo por tick (lo usan el servidor para los mobs y el cliente para el jugador). */
    public static Vec3 pull(Vec3 v) {
        return new Vec3(v.x * 0.75D, Math.min(v.y - 0.35D, -0.7D), v.z * 0.75D);
    }

    @Override
    public void applyEffectTick(LivingEntity e, int amplifier) {
        if (e.isFallFlying() && e instanceof Player p) {
            p.stopFallFlying();                                                 // los élitros no aguantan el peso
        }
        if (!(e instanceof Player) && !e.onGround() && !e.isInWater()) {
            e.setDeltaMovement(pull(e.getDeltaMovement()));
            e.hurtMarked = true;
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
