package com.bryanamaya.tecnocinetico.gear;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Impulsor de Esquiva (gadget de cinturón). El impulso lo aplica el cliente (su movimiento lo calcula él, ver
 * {@code client/DashClient}); aquí el servidor comprueba el gadget, la batería y el enfriamiento, gasta la carga y
 * da la ventana de esquiva: durante {@link #IFRAMES} ticks se anulan los golpes y las balas (no las caídas ni el vacío).
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID)
public final class DashEvents {

    public static final int COOLDOWN = 20, IFRAMES = 6, COST = 10;
    private static final String TAG_LAST = "TecnoDashLast", TAG_UNTIL = "TecnoDashUntil";

    private DashEvents() {}

    /** ¿Lleva el impulsor con batería? (lo usan cliente y servidor). */
    public static ItemStack thruster(Player p) {
        return TacticalGear.of(p).map(g -> g.get(TacticalGear.BELT)).filter(s -> s.is(ModItems.DASH_THRUSTER.get())
                && GadgetItem.hasCharge(s)).orElse(ItemStack.EMPTY);
    }

    public static void serverDash(ServerPlayer p) {
        long now = p.level().getGameTime();
        if (now - p.getPersistentData().getLong(TAG_LAST) < COOLDOWN - 2 || p.isPassenger() || p.isSpectator()) {
            return;
        }
        ItemStack s = thruster(p);
        if (s.isEmpty()) {
            return;
        }
        p.getPersistentData().putLong(TAG_LAST, now);
        p.getPersistentData().putLong(TAG_UNTIL, now + IFRAMES);
        if (!p.getAbilities().instabuild) {
            GadgetItem.drain(s, COST);
            TacticalGear.of(p).ifPresent(TacticalGear::setChanged);
        }
        p.fallDistance = 0.0F;
        p.serverLevel().sendParticles(ModParticles.KINETIC_SPARK.get(), p.getX(), p.getY() + 0.6, p.getZ(), 14, 0.3, 0.3, 0.3, 0.12);
        p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(), 6, 0.2, 0.05, 0.2, 0.02);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), com.bryanamaya.tecnocinetico.sound.ModSounds.JETPACK_THRUST.get(),
                SoundSource.PLAYERS, 0.9F, 1.6F);
    }

    /** Ventana de esquiva: los ataques y balas que llegan durante el impulso no hacen nada. */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player p) || p.level().isClientSide) {
            return;
        }
        var src = event.getSource();
        if (p.level().getGameTime() <= p.getPersistentData().getLong(TAG_UNTIL) && src.getEntity() != null
                && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !src.is(DamageTypeTags.IS_FALL)) {
            event.setCanceled(true);
        }
    }
}
