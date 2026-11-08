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
        CHANNEL.registerMessage(2, FirePistolPacket.class,
                FirePistolPacket::encode, FirePistolPacket::decode, FirePistolPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(3, FillMagazinePacket.class,
                FillMagazinePacket::encode, FillMagazinePacket::decode, FillMagazinePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(4, OpenGearPacket.class, (m, b) -> {}, b -> new OpenGearPacket(), OpenGearPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(5, ToggleVisorPacket.class, (m, b) -> {}, b -> new ToggleVisorPacket(), ToggleVisorPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(6, JetpackPacket.class, (m, b) -> b.writeBoolean(m.on()), b -> new JetpackPacket(b.readBoolean()),
                JetpackPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(7, SyncGearPacket.class, (m, b) -> b.writeNbt(m.tag()), b -> new SyncGearPacket(b.readNbt()),
                SyncGearPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(8, WornGadgetsPacket.class, WornGadgetsPacket::encode, WornGadgetsPacket::decode,
                WornGadgetsPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(9, ExoLeapPacket.class, (m, b) -> {}, b -> new ExoLeapPacket(), ExoLeapPacket::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, SyncSkillsPacket.class,
                SyncSkillsPacket::encode, SyncSkillsPacket::decode, SyncSkillsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    /**
     * Clic izquierdo con la pistola: "dispara" (o recarga si está vacía). El servidor lo comprueba todo.
     * aiming: si el jugador apunta con la mira (el disparo sale del centro y no de la cadera).
     */
    public record FirePistolPacket(boolean aiming) {

        public static void encode(FirePistolPacket msg, FriendlyByteBuf buf) {
            buf.writeBoolean(msg.aiming);
        }

        public static FirePistolPacket decode(FriendlyByteBuf buf) {
            return new FirePistolPacket(buf.readBoolean());
        }

        public static void handle(FirePistolPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    if (player.getMainHandItem().getItem() instanceof com.bryanamaya.tecnocinetico.item.weapon.LongGunItem gun) {
                        gun.tryFire(player, msg.aiming());
                    } else {
                        KineticPistolItem.tryFire(player, msg.aiming());
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
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
                    if (stack.getItem() instanceof com.bryanamaya.tecnocinetico.item.weapon.LongGunItem gun) {
                        gun.startReload(player, stack);
                        return;
                    }
                    if (stack.getItem() instanceof KineticPistolItem && KineticPistolItem.canReload(stack)) {
                        KineticPistolItem.startReload(player, stack);
                        return;
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Tecla R con un cargador en la mano: llenarlo con Bala Cinética (o Munición Guiada si guided). */
    public record FillMagazinePacket(boolean guided) {

        public static void encode(FillMagazinePacket msg, FriendlyByteBuf buf) {
            buf.writeBoolean(msg.guided);
        }

        public static FillMagazinePacket decode(FriendlyByteBuf buf) {
            return new FillMagazinePacket(buf.readBoolean());
        }

        public static void handle(FillMagazinePacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) {
                    return;
                }
                for (InteractionHand hand : InteractionHand.values()) {
                    ItemStack stack = player.getItemInHand(hand);
                    if (stack.getItem() instanceof com.bryanamaya.tecnocinetico.item.custom.MagazineItem) {
                        com.bryanamaya.tecnocinetico.item.custom.MagazineItem.fillFromInventory(player, stack, msg.guided);
                        return;
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // ------------------------------------------------------------------ Equipo Táctico

    /** Pestaña o tecla J: abrir el Equipo Táctico. */
    public record OpenGearPacket() {
        public static void handle(OpenGearPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer p = ctx.get().getSender();
                if (p == null) {
                    return;
                }
                com.bryanamaya.tecnocinetico.gear.TacticalGear.of(p).ifPresent(g -> net.minecraftforge.network.NetworkHooks.openScreen(p,
                        new net.minecraft.world.SimpleMenuProvider((id, inv, pl) ->
                                new com.bryanamaya.tecnocinetico.menu.TacticalMenu(id, inv, g.items()),
                                net.minecraft.network.chat.Component.translatable("gui.tecnocinetico.gear.title"))));
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Tecla N: encender/apagar el Visor de Visión Nocturna. */
    /** Cliente -> servidor: saltó agachado con el exoesqueleto completo (el servidor gasta carga e impulsa). */
    public record ExoLeapPacket() {
        public static void handle(ExoLeapPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer p = ctx.get().getSender();
                if (p != null) {
                    com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorEvents.leap(p);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public record ToggleVisorPacket() {
        public static void handle(ToggleVisorPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer p = ctx.get().getSender();
                if (p == null) {
                    return;
                }
                com.bryanamaya.tecnocinetico.gear.TacticalGear.of(p).ifPresent(g -> {
                    if (!g.get(com.bryanamaya.tecnocinetico.gear.TacticalGear.FACE).is(com.bryanamaya.tecnocinetico.item.ModItems.NIGHT_VISION_VISOR.get())) {
                        p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.tecnocinetico.no_visor"), true);
                        return;
                    }
                    g.setVisorOn(!g.visorOn());
                    p.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                            g.visorOn() ? "message.tecnocinetico.visor_on" : "message.tecnocinetico.visor_off"), true);
                    p.level().playSound(null, p.getX(), p.getY(), p.getZ(), net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                            net.minecraft.sounds.SoundSource.PLAYERS, 0.4F, g.visorOn() ? 1.4F : 0.9F);
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** El cliente empieza/deja de empujar con el jetpack. */
    public record JetpackPacket(boolean on) {
        public static void handle(JetpackPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer p = ctx.get().getSender();
                if (p != null) {
                    if (msg.on) {
                        com.bryanamaya.tecnocinetico.gear.GearEvents.THRUSTING.add(p.getUUID());
                    } else {
                        com.bryanamaya.tecnocinetico.gear.GearEvents.THRUSTING.remove(p.getUUID());
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Copia del Equipo Táctico para el cliente (jetpack, pestaña). */
    public record SyncGearPacket(net.minecraft.nbt.CompoundTag tag) {
        public static void handle(SyncGearPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.bryanamaya.tecnocinetico.client.ClientGearHandler.apply(msg.tag)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** Gadgets que lleva puestos un jugador (cara, espalda, pies), para que los demás los vean. */
    public record WornGadgetsPacket(int entityId, ItemStack face, ItemStack back, ItemStack feet) {
        public static void encode(WornGadgetsPacket m, FriendlyByteBuf b) {
            b.writeVarInt(m.entityId);
            b.writeItem(m.face);
            b.writeItem(m.back);
            b.writeItem(m.feet);
        }

        public static WornGadgetsPacket decode(FriendlyByteBuf b) {
            return new WornGadgetsPacket(b.readVarInt(), b.readItem(), b.readItem(), b.readItem());
        }

        public static void handle(WornGadgetsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.bryanamaya.tecnocinetico.client.ClientGearHandler.applyWorn(msg.entityId, msg.face, msg.back, msg.feet)));
            ctx.get().setPacketHandled(true);
        }
    }
}
