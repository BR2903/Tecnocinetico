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

    /** Carga del Rifle de Riel (0 = sin cargar). */
    private static float railCharge;

    private PistolInputHandler() {}

    /** Carga actual del Rifle de Riel (0..1) para la animación. */
    public static float railCharge() {
        return railCharge;
    }

    private static boolean holdingCharge(LocalPlayer player) {
        return player != null && player.getMainHandItem().getItem() instanceof com.bryanamaya.tecnocinetico.item.weapon.LongGunItem g
                && g.action() == com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.Action.CHARGE;
    }

    /** Pistola o arma larga (escopeta, rifle) en la mano principal. */
    static boolean holdingPistol(LocalPlayer player) {
        return player != null && !(player.getVehicle() instanceof com.bryanamaya.tecnocinetico.entity.ship.CoreFighterEntity) && (player.getMainHandItem().getItem() instanceof KineticPistolItem
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
                if (holdingCharge(mc.player)) {
                    var g = (com.bryanamaya.tecnocinetico.item.weapon.LongGunItem) mc.player.getMainHandItem().getItem();
                    if (g.canFireNow(mc.player, mc.player.getMainHandItem())) {
                        railCharge = 0.001F;                                  // empieza a cargar; dispara al soltar
                        mc.player.playSound(com.bryanamaya.tecnocinetico.sound.ModSounds.ANCHOR_CHARGE.get(), 0.6F, 1.8F);
                    } else {
                        ModNetwork.CHANNEL.sendToServer(new ModNetwork.FirePistolPacket(false));   // vacío: recarga
                    }
                } else {
                    tryFire(mc.player);
                }
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
                float mult = Skill.recoilMultiplier(PlayerSkills.getLevel(player, Skill.WEAPON_HANDLING))
                        * com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.recoilMultiplier(player);
                mult *= Mth.lerp(GunAnimState.aim, 1.0F, AIM_RECOIL_FACTOR);
                player.setXRot(player.getXRot() - gun.stats().pitchKick() * mult);
                player.setYRot(player.getYRot() + (player.getRandom().nextFloat() - 0.5F) * gun.stats().yawKick() * mult);
                GunAnimState.onShot(player.level().getGameTime(), mult * 1.3F, player.getRandom().nextFloat() * 2.0F - 1.0F);
                player.getCooldowns().addCooldown(gun, gun.cooldown(pistol));
            }
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.FirePistolPacket(GunAnimState.aim > 0.5F));
            return;
        }
        if (KineticPistolItem.canFireNow(player, pistol)) {
            // Predicción en el cliente: retroceso inmediato, sin esperar al servidor
            float mult = Skill.recoilMultiplier(PlayerSkills.getLevel(player, Skill.WEAPON_HANDLING))
                        * com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.recoilMultiplier(player);
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
        // Rifle de Riel: carga mientras se mantiene el clic; al soltar, dispara con la carga alcanzada
        if (railCharge > 0) {
            if (!holdingCharge(player) || mc.screen != null) {
                railCharge = 0;
            } else if (attackHeld) {
                railCharge = Math.min(1.0F, railCharge + 1.0F / com.bryanamaya.tecnocinetico.item.weapon.RailRifleItem.CHARGE_TICKS);
                int bars = Math.round(railCharge * 10);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("▮".repeat(bars) + "▯".repeat(10 - bars))
                        .withStyle(railCharge >= 1.0F ? net.minecraft.ChatFormatting.AQUA : railCharge
                                >= com.bryanamaya.tecnocinetico.item.weapon.RailRifleItem.MIN_CHARGE
                                ? net.minecraft.ChatFormatting.DARK_AQUA : net.minecraft.ChatFormatting.DARK_GRAY), true);
            } else {
                if (railCharge >= com.bryanamaya.tecnocinetico.item.weapon.RailRifleItem.MIN_CHARGE) {
                    var g = (com.bryanamaya.tecnocinetico.item.weapon.LongGunItem) player.getMainHandItem().getItem();
                    float mult = Skill.recoilMultiplier(PlayerSkills.getLevel(player, Skill.WEAPON_HANDLING))
                            * com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem.recoilMultiplier(player)
                            * Mth.lerp(GunAnimState.aim, 1.0F, AIM_RECOIL_FACTOR) * railCharge;
                    player.setXRot(player.getXRot() - g.stats().pitchKick() * mult);
                    GunAnimState.onShot(player.level().getGameTime(), mult * 1.6F, player.getRandom().nextFloat() * 2.0F - 1.0F);
                    player.getCooldowns().addCooldown(g, g.cooldown(player.getMainHandItem()));
                    ModNetwork.CHANNEL.sendToServer(new ModNetwork.FireChargedPacket(GunAnimState.aim > 0.5F, railCharge));
                }
                railCharge = 0;
            }
        }
        // automático (Subfusil): dispara mientras se mantiene el clic
        if (attackHeld && player != null && mc.screen == null
                && player.getMainHandItem().getItem() instanceof com.bryanamaya.tecnocinetico.item.weapon.LongGunItem g
                && g.action() == com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.Action.AUTO
                && g.canFireNow(player, player.getMainHandItem())) {
            tryFire(player);
        }

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
            float zoom = held.getItem() instanceof com.bryanamaya.tecnocinetico.item.weapon.LongGunItem g ? g.zoom(held) : KineticPistolItem.aimZoom(held);
            event.setNewFovModifier(event.getNewFovModifier() * Mth.lerp(aim, 1.0F, zoom));
        }
    }

    /** Al apuntar se usan las miras del arma: se oculta el punto de mira de la pantalla. Con mira telescópica: visor. */
    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id()) && GunAnimState.aim > 0.5F
                && holdingPistol(Minecraft.getInstance().player)) {
            event.setCanceled(true);
            if (scoped()) {
                drawScope(event.getGuiGraphics(), event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight());
            }
        }
    }

    private static final net.minecraft.resources.ResourceLocation SCOPE = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/spyglass_scope.png");

    /** ¿Apuntando del todo con un rifle con Mira Telescópica? */
    static boolean scoped() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && GunAnimState.aim > 0.9F && mc.options.getCameraType().isFirstPerson()
                && com.bryanamaya.tecnocinetico.item.weapon.LongGunItem.hasScope(mc.player.getMainHandItem())
                && mc.player.getMainHandItem().getItem() instanceof com.bryanamaya.tecnocinetico.item.weapon.LongGunItem;
    }

    /** Visor a pantalla completa (la textura del catalejo vanilla) con los bordes en negro. */
    private static void drawScope(net.minecraft.client.gui.GuiGraphics g, int w, int h) {
        int size = Math.min(w, h);
        int x = (w - size) / 2, y = (h - size) / 2;
        // la textura es negra con un círculo TRANSPARENTE: sin mezcla de transparencia todo sale negro
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        g.blit(SCOPE, x, y, -90, 0.0F, 0.0F, size, size, size, size);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        g.fill(0, 0, w, y, -90, 0xFF000000);
        g.fill(0, y + size, w, h, -90, 0xFF000000);
        g.fill(0, y, x, y + size, -90, 0xFF000000);
        g.fill(x + size, y, w, y + size, -90, 0xFF000000);
    }

    /** Mirando por la mira telescópica no se dibuja el arma. */
    @SubscribeEvent
    public static void onRenderHand(net.minecraftforge.client.event.RenderHandEvent event) {
        if (scoped()) {
            event.setCanceled(true);
        }
    }
}
