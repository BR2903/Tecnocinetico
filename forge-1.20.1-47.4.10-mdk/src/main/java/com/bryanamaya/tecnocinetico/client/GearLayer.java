package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.gear.JetpackItem;
import com.bryanamaya.tecnocinetico.gear.TacticalGear;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Dibuja sobre el jugador los gadgets que lleva en el Equipo Táctico: visor en la cara, jetpack en la espalda
 * (con las piezas de su nivel) y propulsores en los tobillos. Las lentes y toberas brillan (capa _glow).
 * El equipo de otros jugadores llega con WornGadgetsPacket.
 */
public class GearLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/entity/worn_gear.png");
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "textures/entity/worn_gear_glow.png");

    private final WornGearModel model;

    public GearLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet models) {
        super(parent);
        this.model = new WornGearModel(models.bakeLayer(WornGearModel.LAYER));
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float age, float headYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        TacticalGear.of(player).ifPresent(gear -> {
            int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
            VertexConsumer base = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX));
            PlayerModel<AbstractClientPlayer> m = getParentModel();
            ItemStack face = gear.get(TacticalGear.FACE), back = gear.get(TacticalGear.BACK), feet = gear.get(TacticalGear.FEET);

            if (face.is(ModItems.NIGHT_VISION_VISOR.get())) {
                draw(pose, base, buffers, m.head, model.visor, light, overlay);
            }
            if (back.is(ModItems.JETPACK.get())) {
                int lvl = JetpackItem.level(back);
                draw(pose, base, buffers, m.body, model.jetpack, light, overlay);
                if (lvl >= 2) {
                    draw(pose, base, buffers, m.body, model.jetpackMk2, light, overlay);
                }
                if (lvl >= 3) {
                    draw(pose, base, buffers, m.body, model.jetpackMk3, light, overlay);
                }
            }
            if (feet.is(ModItems.AQUA_PROPULSOR.get())) {
                draw(pose, base, buffers, m.rightLeg, model.propulsor, light, overlay);
                draw(pose, base, buffers, m.leftLeg, model.propulsor, light, overlay);
            }
        });
    }

    /** Pieza enganchada a un hueso del jugador; después, la misma pieza con la textura luminosa a brillo máximo. */
    private static void draw(PoseStack pose, VertexConsumer base, MultiBufferSource buffers, ModelPart bone, ModelPart part,
                             int light, int overlay) {
        pose.pushPose();
        bone.translateAndRotate(pose);
        part.render(pose, base, light, overlay);
        part.render(pose, buffers.getBuffer(RenderType.eyes(GLOW)), 15728880, overlay);
        pose.popPose();
    }
}
