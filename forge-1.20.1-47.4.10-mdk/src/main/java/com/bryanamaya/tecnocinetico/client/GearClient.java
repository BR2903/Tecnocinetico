package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.gear.GadgetItem;
import com.bryanamaya.tecnocinetico.gear.TacticalGear;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Cliente del Equipo Táctico: empuje del jetpack (el movimiento del jugador lo calcula el cliente) y aviso al
 * servidor de cuándo empieza/termina de empujar (para gastar batería y anular el daño por caída).
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID, value = Dist.CLIENT)
public final class GearClient {

    // empuje, subida máxima y avance dependen del nivel del jetpack (JetpackItem)
    private static boolean thrusting;

    private GearClient() {}

    /** Exoesqueleto: saltar agachado pide al servidor el salto cinético. */
    @SubscribeEvent
    public static void onJump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getEntity() == mc.player && mc.player.isShiftKeyDown()
                && com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.hasFullSet(mc.player,
                com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.Kind.EXO)) {
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.ExoLeapPacket());
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        boolean want = false;
        ItemStack jet = ItemStack.EMPTY;
        if (p != null && mc.screen == null && mc.options.keyJump.isDown() && !p.onGround() && !p.isInWater()
                && !p.getAbilities().flying && !p.isPassenger() && !p.isFallFlying()) {
            jet = TacticalGear.of(p).map(g -> g.active(TacticalGear.BACK)).orElse(ItemStack.EMPTY);
            want = jet.is(ModItems.JETPACK.get()) && GadgetItem.hasCharge(jet)
                    && !p.hasEffect(com.bryanamaya.tecnocinetico.effect.ModEffects.INERTIA_ANCHOR.get());   // el lastre lo apaga
        }
        // Lastre de Inercia (Anclador): arrastra al jugador al suelo (su movimiento lo calcula el cliente)
        if (p != null && p.hasEffect(com.bryanamaya.tecnocinetico.effect.ModEffects.INERTIA_ANCHOR.get()) && !p.onGround()
                && !p.isInWater() && !p.getAbilities().flying) {
            p.setDeltaMovement(com.bryanamaya.tecnocinetico.effect.InertiaAnchorEffect.pull(p.getDeltaMovement()));
        }
        if (want) {
            Vec3 v = p.getDeltaMovement();
            double glide = com.bryanamaya.tecnocinetico.gear.JetpackItem.glide(jet);
            p.setDeltaMovement(v.x * glide, Math.min(v.y + com.bryanamaya.tecnocinetico.gear.JetpackItem.thrust(jet),
                    com.bryanamaya.tecnocinetico.gear.JetpackItem.maxRise(jet)), v.z * glide);
            p.fallDistance = 0.0F;
        }
        if (want != thrusting && p != null) {
            thrusting = want;
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.JetpackPacket(want));
        }
    }
}
