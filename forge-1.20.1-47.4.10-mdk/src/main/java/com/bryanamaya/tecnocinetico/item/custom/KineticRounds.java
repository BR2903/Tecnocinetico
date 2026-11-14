package com.bryanamaya.tecnocinetico.item.custom;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Bala Normal y Bala Cinética (calibre de pistola, subfusil y revólver). La normal es plomo, latón y pólvora (lo
 * primero que se fabrica); la cinética lleva un núcleo de cinetrita que libera su energía al impactar:
 * <b>Impacto cinético</b> = además del daño normal, {@link #PIERCE} de ese daño atraviesa la armadura
 * (daño {@code tecnocinetico:kinetic_impact}) y empuja al objetivo, con el estallido cian.
 * <p>
 * Un cargador (o el arma, si carga bala a bala) lleva un solo tipo: la marca {@link #TAG} dice si son cinéticas. Al
 * llenar un cargador vacío se usan cinéticas si hay; si ya tiene balas, sigue con el mismo tipo.
 */
public final class KineticRounds {

    public static final String TAG = "KineticRounds";
    /** Fracción del daño que el impacto cinético añade atravesando la armadura. */
    public static final float PIERCE = 0.35F;
    public static final double PUSH = 0.45D;
    public static final ResourceKey<DamageType> KINETIC_IMPACT = ResourceKey.create(Registries.DAMAGE_TYPE,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "kinetic_impact"));

    private KineticRounds() {}

    public static boolean is(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().getBoolean(TAG);
    }

    public static void set(ItemStack stack, boolean kinetic) {
        if (kinetic) {
            stack.getOrCreateTag().putBoolean(TAG, true);
        } else if (stack.getTag() != null) {
            stack.removeTagKey(TAG);
            if (stack.getTag() != null && stack.getTag().isEmpty()) {
                stack.setTag(null);
            }
        }
    }

    /** Paquete de munición de ese tipo. */
    public static Item pack(boolean kinetic) {
        return kinetic ? ModItems.KINETIC_ROUND.get() : ModItems.STANDARD_ROUND.get();
    }

    /** ¿Es un paquete de bala de pistola (normal o cinética)? */
    public static boolean isPistolPack(ItemStack s) {
        return s.is(ModItems.STANDARD_ROUND.get()) || s.is(ModItems.KINETIC_ROUND.get());
    }

    /**
     * Tipo a cargar en un arma o cargador VACÍO: cinéticas si hay alguna en el inventario, si no normales.
     */
    public static boolean chooseFor(Player player) {
        return player.getAbilities().instabuild || AmmoPackItem.available(player, ModItems.KINETIC_ROUND.get()) > 0;
    }

    /** Impacto cinético: daño extra que atraviesa la armadura, empuje en la dirección del disparo y estallido cian. */
    public static void onHit(ServerLevel level, Player shooter, Entity target, float baseDamage, Vec3 dir) {
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        living.invulnerableTime = 0;
        living.hurt(new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(KINETIC_IMPACT),
                shooter), baseDamage * PIERCE);
        Vec3 d = new Vec3(dir.x, 0, dir.z);
        if (d.lengthSqr() > 1e-6) {
            d = d.normalize().scale(PUSH);
            living.push(d.x, 0.12, d.z);
            living.hurtMarked = true;
        }
        Vec3 c = living.getBoundingBox().getCenter();
        level.sendParticles(ModParticles.KINETIC_BURST.get(), c.x, c.y, c.z, 0, 0.8, 0, 0, 1);
        level.sendParticles(ModParticles.KINETIC_SPARK.get(), c.x, c.y, c.z, 8, 0.1, 0.1, 0.1, 0.2);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.7F, 1.6F);
    }
}
