package com.bryanamaya.tecnocinetico.map;

import com.bryanamaya.tecnocinetico.particle.ModParticles;
import com.bryanamaya.tecnocinetico.util.ParticleShapes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Satélite de Reconocimiento: se usa por encima de {@link #ORBIT_Y} en el Overworld (llegando con el jetpack, los
 * élitros o el Caza del Núcleo). Sale disparado hacia arriba y queda en órbita para todo el servidor: desde entonces
 * los Relojes de Navegación funcionan. Logro "En órbita" para quien lo lanza.
 */
public class SatelliteItem extends Item {

    public static final int ORBIT_Y = 320;

    public SatelliteItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }
        if (NavigationData.get(server.getServer()).satellite()) {
            player.displayClientMessage(Component.translatable("message.tecnocinetico.satellite.already").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResultHolder.fail(stack);
        }
        if (level.dimension() != Level.OVERWORLD) {
            player.displayClientMessage(Component.translatable("message.tecnocinetico.satellite.overworld").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        if (player.getY() < ORBIT_Y) {
            player.displayClientMessage(Component.translatable("message.tecnocinetico.satellite.too_low", ORBIT_Y, (int) Math.ceil(ORBIT_Y - player.getY()))
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        launch(server, player.position().add(0, 1.0, 0));
        MapEvents.deploy(sp);
        return InteractionResultHolder.consume(stack);
    }

    /** Estela del lanzamiento: chispas y fogonazos subiendo 64 bloques, con su sonido. */
    public static void launch(ServerLevel server, Vec3 from) {
        Vec3 top = from.add(0, 64, 0);
        ParticleShapes.line(server, ModParticles.MUZZLE_FLASH.get(), from, top, 48, 2, 0.15, 0.1, 0.15, 0.02);
        ParticleShapes.line(server, ModParticles.KINETIC_SPARK.get(), from, top, 64, 2, 0.25, 0.2, 0.25, 0.05);
        server.sendParticles(ModParticles.BULLET_SPARK.get(), from.x, from.y, from.z, 30, 0.6, 0.3, 0.6, 0.2);
        server.playSound(null, from.x, from.y, from.z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 3.0F, 0.6F);
        server.playSound(null, from.x, from.y, from.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 1.4F);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tecnocinetico.satellite", ORBIT_Y).withStyle(ChatFormatting.GRAY));
    }
}
