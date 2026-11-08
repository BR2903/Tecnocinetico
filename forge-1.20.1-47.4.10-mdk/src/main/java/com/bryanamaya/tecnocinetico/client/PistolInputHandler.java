package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.custom.GunAnimState;
import com.bryanamaya.tecnocinetico.item.custom.KineticPistolItem;
import com.bryanamaya.tecnocinetico.network.ModNetwork;
import com.bryanamaya.tecnocinetico.skill.PlayerSkills;
import com.bryanamaya.tecnocinetico.skill.Skill;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Controles de la Pistola Cinética (solo cliente):
 * - Clic izquierdo: dispara (semiautomática: un disparo por clic). Con el cargador vacío, recarga sola.
 * - Clic derecho mantenido: apuntar con la mira (arma centrada, zoom, sin punto de mira, menos retroceso).
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PistolInputHandler {

    private static final float AIM_SPEED = 0.34F;           // fracción por tick (≈3 ticks para apuntar)
    private static final float AIM_RECOIL_FACTOR = 0.6F;    // apuntando, el retroceso es menor

    /** ¿El botón de ataque estaba pulsado en el tick anterior? (para disparar solo al pulsar, no al mantener) */
    private static boolean attackHeld;

    private PistolInputHandler() {}

    /** Pistola o arma larga (escopeta, rifle) en la mano principal. */
    static boolean holdingPistol(LocalPlayer player) {
        return player != null && (player.getMainHandItem().getItem() instanceof KineticPistolItem
                || player.getMainHandItem().getItem() instanceof com.bryanamaya.tecnocinetico.item.weapon.LongGunItem);
    }

    /** Intercepta los clics: el izquierdo dispara y el derecho no hace nada más (apuntar va en el tick). */
    @SubscribeEvent
    public static void onClick(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (!holdingPistol(mc.player) || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return;
        }
        if (event.isAttack()) {
            event.setCanceled(true);      // no golpear ni romper bloques
            event.setSwingHand(false);
            if (!attackHeld) {            // semiautomática
                attackHeld = true;
                tryFire(mc.player);
            }
        } else if (event.isUseItem()) {
            event.setCanceled(true);      // clic derecho = solo apuntar
            event.setSwingHand(false);
        }
    }

    private static void tryFire(LocalPlayer player) {
        ItemStack pistol = player.getMainHandItem();
        if (pistol.getItem() instanceof com.bryanamaya.tecnocinetico.item.weapon.LongGunItem gun) {
            if (gun.canFireNow(player, pistol)) {
                float mult = Skill.recoilMultiplier(PlayerSkills.getLevel(player, Skill.WEAPON_HANDLING));
                mult *= Mth.lerp(GunAnimState.aim, 1.0F, AIM_RECOIL_FACTOR);
                player.setXRot(player.getXRot() - gun.stats().pitchKick() * mult);
                player.setYRot(player.getYRot() + (player.getRandom().nextFloat() - 0.5F) * gun.stats().yawKick() * mult);
                GunAnimState.onShot(player.level().getGameTime(), mult * 1.3F, player.getRandom().nextFloat() * 2.0F - 1.0F);
                player.getCooldowns().addCooldown(gun, gun.stats().shotCooldown());
            }
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.FirePistolPacket(GunAnimState.aim > 0.5F));
            return;
        }
        if (KineticPistolItem.canFireNow(player, pistol)) {
            // Predicción en el cliente: retroceso inmediato, sin esperar al servidor
            float mult = Skill.recoilMultiplier(PlayerSkills.getLevel(player, Skill.WEAPON_HANDLING));
            mult *= Mth.lerp(GunAnimState.aim, 1.0F, AIM_RECOIL_FACTOR);
            player.setXRot(player.getXRot() - KineticPistolItem.pitchKick() * mult);
            player.setYRot(player.getYRot() + (player.getRandom().nextFloat() - 0.5F) * KineticPistolItem.yawKick() * mult);
            GunAnimState.onShot(player.level().getGameTime(), mult, player.getRandom().nextFloat() * 2.0F - 1.0F);
            player.getCooldowns().addCooldown(pistol.getItem(), (int) KineticPistolItem.shotCooldownTicks());
        }
        // El servidor dispara, o recarga automáticamente si el cargador está vacío
        ModNetwork.CHANNEL.sendToServer(new ModNetwork.FirePistolPacket(GunAnimState.aim > 0.5F));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        attackHeld = mc.options.keyAttack.isDown();

        boolean aiming = holdingPistol(player) && mc.screen == null && mc.options.keyUse.isDown()
                && !KineticPistolItem.isReloading(player.getMainHandItem())
                && !com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.isLoading(player.getMainHandItem());
        GunAnimState.prevAim = GunAnimState.aim;
        float target = aiming ? 1.0F : 0.0F;
        GunAnimState.aim = Mth.approach(GunAnimState.aim, target, AIM_SPEED);
    }

    /** Zoom al apuntar. */
    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        float aim = GunAnimState.aim;
        var held = event.getPlayer().getMainHandItem();
        if (aim > 0 && (held.getItem() instanceof KineticPistolItem || held.getItem() instanceof com.bryanamaya.tecnocinetico.item.weapon.LongGunItem)) {
            float zoom = held.getItem() instanceof com.bryanamaya.tecnocinetico.item.weapon.LongGunItem g ? g.stats().aimZoom() : KineticPistolItem.aimZoom(held);
            event.setNewFovModifier(event.getNewFovModifier() * Mth.lerp(aim, 1.0F, zoom));
        }
    }

    /** Al apuntar se usan las miras del arma: se oculta el punto de mira de la pantalla. */
    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id()) && GunAnimState.aim > 0.5F
                && holdingPistol(Minecraft.getInstance().player)) {
            event.setCanceled(true);
        }
    }
}
