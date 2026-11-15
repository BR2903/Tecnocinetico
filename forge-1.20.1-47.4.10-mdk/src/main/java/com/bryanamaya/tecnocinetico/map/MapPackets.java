package com.bryanamaya.tecnocinetico.map;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Paquetes del Reloj de Navegación (ids 24-26 en ModNetwork). */
public final class MapPackets {

    private MapPackets() {}

    /** Servidor -> cliente: estado del satélite, marcadores que ve el jugador y estructuras detectadas. */
    public record Sync(boolean satellite, List<NavigationData.Marker> markers, List<NavigationData.Found> found) {
        public static void encode(Sync m, FriendlyByteBuf b) {
            b.writeBoolean(m.satellite);
            b.writeVarInt(m.markers.size());
            m.markers.forEach(x -> x.write(b));
            b.writeVarInt(m.found.size());
            m.found.forEach(x -> x.write(b));
        }

        public static Sync decode(FriendlyByteBuf b) {
            boolean sat = b.readBoolean();
            int n = b.readVarInt();
            List<NavigationData.Marker> markers = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                markers.add(NavigationData.Marker.read(b));
            }
            n = b.readVarInt();
            List<NavigationData.Found> found = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                found.add(NavigationData.Found.read(b));
            }
            return new Sync(sat, markers, found);
        }

        public static void handle(Sync m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.bryanamaya.tecnocinetico.client.map.ClientMapState.apply(m)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** Cliente -> servidor: crear o borrar un marcador, o buscar una estructura. */
    public record Action(int op, String text, int x, int y, int z, int color, boolean shared, UUID id) {
        public static final int ADD = 0, REMOVE = 1, LOCATE = 2;

        public static Action add(String name, int x, int y, int z, int color, boolean shared) {
            return new Action(ADD, name, x, y, z, color, shared, new UUID(0, 0));
        }

        public static Action remove(UUID id) {
            return new Action(REMOVE, "", 0, 0, 0, 0, false, id);
        }

        public static Action locate(String key) {
            return new Action(LOCATE, key, 0, 0, 0, 0, false, new UUID(0, 0));
        }

        public static void encode(Action m, FriendlyByteBuf b) {
            b.writeVarInt(m.op);
            b.writeUtf(m.text, 64);
            b.writeInt(m.x);
            b.writeInt(m.y);
            b.writeInt(m.z);
            b.writeInt(m.color);
            b.writeBoolean(m.shared);
            b.writeUUID(m.id);
        }

        public static Action decode(FriendlyByteBuf b) {
            return new Action(b.readVarInt(), b.readUtf(64), b.readInt(), b.readInt(), b.readInt(), b.readInt(), b.readBoolean(), b.readUUID());
        }

        public static void handle(Action m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer p = ctx.get().getSender();
                if (p != null) {
                    MapEvents.action(p, m);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Servidor -> cliente: resultado de una búsqueda (para centrar el mapa en ella). */
    public record LocateResult(String key, boolean found, int x, int z) {
        public static void encode(LocateResult m, FriendlyByteBuf b) {
            b.writeUtf(m.key, 64);
            b.writeBoolean(m.found);
            b.writeInt(m.x);
            b.writeInt(m.z);
        }

        public static LocateResult decode(FriendlyByteBuf b) {
            return new LocateResult(b.readUtf(64), b.readBoolean(), b.readInt(), b.readInt());
        }

        public static void handle(LocateResult m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.bryanamaya.tecnocinetico.client.map.ClientMapState.located(m)));
            ctx.get().setPacketHandled(true);
        }
    }
}
