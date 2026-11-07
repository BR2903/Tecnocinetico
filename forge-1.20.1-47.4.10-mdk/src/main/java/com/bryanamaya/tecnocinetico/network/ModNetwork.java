package com.bryanamaya.tecnocinetico.network;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.custom.KineticPistolItem;
import com.bryanamaya.tecnocinetico.skill.ClientSkillData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

/** Canal de red del mod: el cliente avisa al servidor de acciones con teclas (p. ej. recargar). */
public final class ModNetwork {

    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private ModNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(0, ReloadPistolPacket.class,
                ReloadPistolPacket::encode, ReloadPistolPacket::decode, ReloadPistolPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, SyncSkillsPacket.class,
                SyncSkillsPacket::encode, SyncSkillsPacket::decode, SyncSkillsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    /** El servidor envía al cliente las habilidades del jugador (para retroceso, descripciones...). */
    public record SyncSkillsPacket(CompoundTag skills) {

        public static void encode(SyncSkillsPacket msg, FriendlyByteBuf buf) {
            buf.writeNbt(msg.skills);
        }

        public static SyncSkillsPacket decode(FriendlyByteBuf buf) {
            CompoundTag tag = buf.readNbt();
            return new SyncSkillsPacket(tag == null ? new CompoundTag() : tag);
        }

        public static void handle(SyncSkillsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> ClientSkillData.set(msg.skills));
            ctx.get().setPacketHandled(true);
        }
    }

    /** "Quiero recargar la pistola que tengo en la mano". No lleva datos: el servidor lo comprueba todo. */
    public record ReloadPistolPacket() {

        public static void encode(ReloadPistolPacket msg, FriendlyByteBuf buf) {}

        public static ReloadPistolPacket decode(FriendlyByteBuf buf) {
            return new ReloadPistolPacket();
        }

        public static void handle(ReloadPistolPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) {
                    return;
                }
                for (InteractionHand hand : InteractionHand.values()) {
                    ItemStack stack = player.getItemInHand(hand);
                    // Con dos pistolas, recarga la primera que lo necesite
                    if (stack.getItem() instanceof KineticPistolItem && KineticPistolItem.canReload(stack)) {
                        KineticPistolItem.startReload(player, stack);
                        return;
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
