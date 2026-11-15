package com.bryanamaya.tecnocinetico.effect;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Corrupción de datos (aliento del Dragón modificado, nubes magenta): daño cada segundo que atraviesa la armadura
 * ({@code tecnocinetico:data_corruption}) y, en el cliente, glitches en los bordes de la pantalla
 * ({@code client/EndClientEffects}). El Filtro de Ceniza Mk III la filtra (ver end/EndArena).
 */
public class DataCorruptionEffect extends MobEffect {

    public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "data_corruption"));

    public DataCorruptionEffect() {
        super(MobEffectCategory.HARMFUL, 0xD04CFF);
    }

    @Override
    public void applyEffectTick(LivingEntity e, int amplifier) {
        e.hurt(new DamageSource(e.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE)),
                1.5F + amplifier);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }
}
