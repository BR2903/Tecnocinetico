package com.bryanamaya.tecnocinetico.entity.ship;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Cabina blindada del Caza del Núcleo con el Acoplador del Nether: quien va dentro no se quema (fuego, lava, bolas de
 * fuego) y recibe solo un {@link CoreFighterEntity#NETHER_EXPLOSION_FACTOR} de las explosiones; tampoco le sube el calor
 * del Nether (NetherHazards). Pensado para que un disparo de ghast o caer a la lava con el caza no mate al piloto.
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID)
public final class ShipShielding {

    private ShipShielding() {}

    @SubscribeEvent
    public static void attack(LivingAttackEvent event) {
        if (CoreFighterEntity.shielded(event.getEntity()) && event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
            event.getEntity().clearFire();
        }
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (CoreFighterEntity.shielded(event.getEntity()) && event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
            event.setAmount(event.getAmount() * CoreFighterEntity.NETHER_EXPLOSION_FACTOR);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide && event.player.isOnFire()
                && CoreFighterEntity.shielded(event.player)) {
            event.player.clearFire();
        }
    }
}
