package com.bryanamaya.tecnocinetico.effect;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Radiación (uranio): daño periódico que ignora la armadura y hambre. Cuanto más nivel, más seguido.
 * La aplica {@link RadiationEvents} mientras el jugador esté expuesto; al alejarse se pasa en unos segundos.
 */
public class RadiationEffect extends MobEffect {

    /** Tipo de daño propio (data/tecnocinetico/damage_type/radiation.json; ignora armadura). */
    public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "radiation"));

    public RadiationEffect() {
        super(MobEffectCategory.HARMFUL, 0x7CD43A);
    }

    /** Ticks entre daños según el nivel (I, II, III...). */
    public static int interval(int amplifier) {
        return switch (amplifier) {
            case 0 -> 60;
            case 1 -> 35;
            default -> 20;
        };
    }

    @Override
    public void applyEffectTick(LivingEntity e, int amplifier) {
        if (e.level().isClientSide) {
            return;
        }
        if (e instanceof Player p) {
            p.causeFoodExhaustion(0.01F * (amplifier + 1));                   // náuseas: se gasta el hambre
        }
        if (e.tickCount % interval(amplifier) == 0) {
            e.hurt(new net.minecraft.world.damagesource.DamageSource(
                    e.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE)), 1.0F);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
