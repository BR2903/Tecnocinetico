package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.gear.ThrusterElytraItem;
import com.bryanamaya.tecnocinetico.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Élitros de Propulsión en el cliente: el empuje al planear con saltar pulsado y las alas dibujadas sobre el jugador. */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID, value = Dist.CLIENT)
public final class ElytraThrustClient {

    private static boolean thrusting;

    private ElytraThrustClient() {}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        boolean want = p != null && mc.screen == null && p.isFallFlying() && mc.options.keyJump.isDown()
                && ThrusterElytraItem.canThrust(ThrusterElytraItem.worn(p));
        if (want) {
            p.setDeltaMovement(ThrusterElytraItem.boost(p.getDeltaMovement(), p.getLookAngle()));   // el movimiento es del cliente
        }
        if (want != thrusting && p != null) {
            thrusting = want;
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.ElytraThrustPacket(want));
        }
    }

    /** Dibuja los Élitros de Propulsión con su textura (la capa vanilla solo dibuja los élitros normales). */
    public static class Layer<T extends LivingEntity, M extends EntityModel<T>> extends ElytraLayer<T, M> {
        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/entity/thruster_elytra.png");

        public Layer(RenderLayerParent<T, M> parent, EntityModelSet models) {
            super(parent, models);
        }

        @Override
        public boolean shouldRender(ItemStack stack, T entity) {
            return stack.getItem() instanceof ThrusterElytraItem;
        }

        @Override
        public ResourceLocation getElytraTexture(ItemStack stack, T entity) {
            return TEXTURE;
        }
    }
}
