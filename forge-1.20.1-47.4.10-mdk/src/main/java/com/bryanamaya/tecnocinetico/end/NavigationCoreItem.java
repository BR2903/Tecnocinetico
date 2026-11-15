package com.bryanamaya.tecnocinetico.end;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.custom.LoreItem;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
import com.bryanamaya.tecnocinetico.util.ParticleShapes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Núcleo de Navegación (botín del Dragón): guarda la ruta de salto de la IA. Usado en el End señala el <b>Puerto de
 * Lanzamiento</b> (estructura única): un rastro de glitches hacia él, la distancia y el rumbo. Fuera del End no hay señal.
 */
public class NavigationCoreItem extends LoreItem {

    public static final TagKey<Structure> LAUNCH_PORT = TagKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "launch_port"));
    public static final int COOLDOWN = 40;
    private static final String[] HEADINGS = {"south", "southwest", "west", "northwest", "north", "northeast", "east", "southeast"};

    public NavigationCoreItem(Properties props) {
        super("tooltip.tecnocinetico.navigation_core", true, props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) {
            return InteractionResultHolder.success(stack);
        }
        player.getCooldowns().addCooldown(this, COOLDOWN);
        if (server.dimension() != Level.END) {
            player.displayClientMessage(Component.translatable("message.tecnocinetico.navigation.no_signal").withStyle(ChatFormatting.DARK_PURPLE), true);
            return InteractionResultHolder.consume(stack);
        }
        BlockPos port = find(server, player.blockPosition());
        if (port == null) {
            player.displayClientMessage(Component.translatable("message.tecnocinetico.navigation.lost").withStyle(ChatFormatting.DARK_PURPLE), true);
            return InteractionResultHolder.consume(stack);
        }
        Vec3 to = new Vec3(port.getX() + 0.5 - player.getX(), 0, port.getZ() + 0.5 - player.getZ());
        int dist = (int) to.length();
        player.displayClientMessage(Component.translatable("message.tecnocinetico.navigation.port", dist,
                Component.translatable("direction.tecnocinetico." + heading(to))).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        if (dist > 2) {                                                    // rastro de 8 bloques hacia el Puerto
            Vec3 dir = to.normalize();
            Vec3 from = player.getEyePosition().add(dir.scale(1.5)).add(0, -0.3, 0);
            ParticleShapes.line(server, ModParticles.DATA_GLITCH.get(), from, from.add(dir.scale(8)), 24);
        }
        server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.7F, 1.6F);
        return InteractionResultHolder.consume(stack);
    }

    /** Posición del Puerto de Lanzamiento más cercano (hay uno por mundo), o null. */
    @Nullable
    public static BlockPos find(ServerLevel end, BlockPos from) {
        return end.findNearestMapStructure(LAUNCH_PORT, from, 100, false);
    }

    /** Rumbo en 8 direcciones (norte = -Z). */
    public static String heading(Vec3 to) {
        float yaw = (float) (Mth.atan2(-to.x, to.z) * Mth.RAD_TO_DEG);                    // 0 = sur, como el yaw del jugador
        int i = Math.floorMod(Math.round(yaw / 45.0F), 8);
        return HEADINGS[i];
    }
}
