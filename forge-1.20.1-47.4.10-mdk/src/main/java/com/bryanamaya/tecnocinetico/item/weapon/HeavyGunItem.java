package com.bryanamaya.tecnocinetico.item.weapon;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.custom.WeaponModule;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Arma pesada automática (Ametralladora ligera, Minigun): mientras se lleva en la mano el jugador va más lento
 * ({@link #speedPenalty}). El freno se pone y se quita en el tick del jugador según lo que tenga en la mano.
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID)
public class HeavyGunItem extends LongGunItem {

    private static final UUID SLOW = UUID.fromString("9a1d2c4e-3f5b-4a6c-8d7e-1f2a3b4c5d6e");
    private final double speedPenalty;

    public HeavyGunItem(double speedPenalty, Stats stats, Properties properties) {
        super(WeaponModule.Family.HEAVY, Action.AUTO, stats, properties);
        this.speedPenalty = speedPenalty;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || event.player.tickCount % 5 != 0) {
            return;
        }
        Player p = event.player;
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        double want = p.getMainHandItem().getItem() instanceof HeavyGunItem g ? -g.speedPenalty : 0;
        AttributeModifier cur = speed.getModifier(SLOW);
        if (cur != null && cur.getAmount() != want) {
            speed.removeModifier(SLOW);
            cur = null;
        }
        if (cur == null && want != 0) {
            speed.addTransientModifier(new AttributeModifier(SLOW, "Arma pesada", want, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }
}
