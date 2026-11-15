package com.bryanamaya.tecnocinetico.map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Datos de navegación de TODO el servidor (guardados en el Overworld): si el Satélite de Reconocimiento está en órbita
 * (basta con que lo despliegue un jugador), los marcadores de cada jugador (personales o compartidos) y las estructuras
 * que ha detectado el satélite (las ven todos).
 */
public class NavigationData extends SavedData {

    private static final String NAME = "tecnocinetico_navigation";
    public static final int MAX_MARKERS_PER_PLAYER = 64, MAX_NAME = 32;

    /** Marcador del mapa. {@code dim} es el id de la dimensión ("minecraft:overworld"...). */
    public record Marker(UUID id, UUID owner, String ownerName, String name, String dim, int x, int y, int z, int color, boolean shared) {
        public void write(FriendlyByteBuf b) {
            b.writeUUID(id);
            b.writeUUID(owner);
            b.writeUtf(ownerName, 32);
            b.writeUtf(name, MAX_NAME);
            b.writeUtf(dim, 64);
            b.writeInt(x);
            b.writeInt(y);
            b.writeInt(z);
            b.writeInt(color);
            b.writeBoolean(shared);
        }

        public static Marker read(FriendlyByteBuf b) {
            return new Marker(b.readUUID(), b.readUUID(), b.readUtf(32), b.readUtf(MAX_NAME), b.readUtf(64), b.readInt(), b.readInt(), b.readInt(),
                    b.readInt(), b.readBoolean());
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", id);
            t.putUUID("Owner", owner);
            t.putString("OwnerName", ownerName);
            t.putString("Name", name);
            t.putString("Dim", dim);
            t.putInt("X", x);
            t.putInt("Y", y);
            t.putInt("Z", z);
            t.putInt("Color", color);
            t.putBoolean("Shared", shared);
            return t;
        }

        static Marker load(CompoundTag t) {
            return new Marker(t.getUUID("Id"), t.getUUID("Owner"), t.getString("OwnerName"), t.getString("Name"), t.getString("Dim"),
                    t.getInt("X"), t.getInt("Y"), t.getInt("Z"), t.getInt("Color"), t.getBoolean("Shared"));
        }
    }

    /** Estructura detectada: entrada del {@link MapCatalog} y su posición (centro aproximado). */
    public record Found(String key, String dim, int x, int z) {
        public void write(FriendlyByteBuf b) {
            b.writeUtf(key, 64);
            b.writeUtf(dim, 64);
            b.writeInt(x);
            b.writeInt(z);
        }

        public static Found read(FriendlyByteBuf b) {
            return new Found(b.readUtf(64), b.readUtf(64), b.readInt(), b.readInt());
        }

        /** Misma estructura (misma entrada, dimensión y a menos de 48 bloques). */
        public boolean same(Found o) {
            return key.equals(o.key) && dim.equals(o.dim) && Math.abs(x - o.x) < 48 && Math.abs(z - o.z) < 48;
        }
    }

    private boolean satellite;
    private String deployer = "";
    private final List<Marker> markers = new ArrayList<>();
    private final List<Found> found = new ArrayList<>();

    public static NavigationData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(NavigationData::load, NavigationData::new, NAME);
    }

    public boolean satellite() {
        return satellite;
    }

    public String deployer() {
        return deployer;
    }

    public void deploy(String by) {
        satellite = true;
        deployer = by;
        setDirty();
    }

    /** Para pruebas: quitar el satélite. */
    public void reset() {
        satellite = false;
        deployer = "";
        markers.clear();
        found.clear();
        setDirty();
    }

    public List<Marker> markers() {
        return markers;
    }

    public List<Found> found() {
        return found;
    }

    /** Los que ve este jugador: los suyos y los compartidos. */
    public List<Marker> visibleTo(UUID player) {
        return markers.stream().filter(m -> m.shared() || m.owner().equals(player)).toList();
    }

    public boolean addMarker(Marker m) {
        if (markers.stream().filter(x -> x.owner().equals(m.owner())).count() >= MAX_MARKERS_PER_PLAYER) {
            return false;
        }
        markers.add(m);
        setDirty();
        return true;
    }

    /** Solo su dueño lo puede borrar. */
    public boolean removeMarker(UUID id, UUID by) {
        boolean removed = markers.removeIf(m -> m.id().equals(id) && m.owner().equals(by));
        if (removed) {
            setDirty();
        }
        return removed;
    }

    /** Añade una estructura detectada si no estaba ya. Devuelve si es nueva. */
    public boolean addFound(Found f) {
        for (Found o : found) {
            if (o.same(f)) {
                return false;
            }
        }
        found.add(f);
        setDirty();
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Satellite", satellite);
        tag.putString("Deployer", deployer);
        ListTag ms = new ListTag();
        markers.forEach(m -> ms.add(m.save()));
        tag.put("Markers", ms);
        ListTag fs = new ListTag();
        for (Found f : found) {
            CompoundTag t = new CompoundTag();
            t.putString("Key", f.key());
            t.putString("Dim", f.dim());
            t.putInt("X", f.x());
            t.putInt("Z", f.z());
            fs.add(t);
        }
        tag.put("Found", fs);
        return tag;
    }

    private static NavigationData load(CompoundTag tag) {
        NavigationData d = new NavigationData();
        d.satellite = tag.getBoolean("Satellite");
        d.deployer = tag.getString("Deployer");
        for (Tag t : tag.getList("Markers", Tag.TAG_COMPOUND)) {
            d.markers.add(Marker.load((CompoundTag) t));
        }
        for (Tag t : tag.getList("Found", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            d.found.add(new Found(c.getString("Key"), c.getString("Dim"), c.getInt("X"), c.getInt("Z")));
        }
        return d;
    }
}
