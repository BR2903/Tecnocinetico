package com.bryanamaya.tecnocinetico.end;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Estado del evento del Dragón en el End de cada mundo (se guarda en {@code data/tecnocinetico_end_arena.dat}). */
public class EndArenaData extends SavedData {

    public enum State { DORMANT, AWAKENING, FIGHT, DEFEATED }

    private static final String NAME = "tecnocinetico_end_arena";

    public State state = State.DORMANT;
    /** Ticks transcurridos del despertar. */
    public int timer;
    /** Bit i = la torre i ya está rota. */
    public int ruined;
    /** El Dragón ya llamó refuerzos (a mitad de vida). */
    public boolean reinforced;
    @Nullable
    public UUID dragon;
    /** Jugadores que hirieron al Dragón (botín uno por cabeza, máx. 4). */
    public final List<UUID> fighters = new ArrayList<>();

    /** Impactos de la Lluvia de datos pendientes (no se guardan: duran 1,5 s). */
    public final transient List<Strike> strikes = new ArrayList<>();

    public record Strike(BlockPos at, long when) {}

    public static EndArenaData get(ServerLevel end) {
        return end.getDataStorage().computeIfAbsent(EndArenaData::load, EndArenaData::new, NAME);
    }

    public static EndArenaData load(CompoundTag tag) {
        EndArenaData d = new EndArenaData();
        d.state = State.values()[Math.max(0, Math.min(State.values().length - 1, tag.getInt("State")))];
        d.timer = tag.getInt("Timer");
        d.ruined = tag.getInt("Ruined");
        d.reinforced = tag.getBoolean("Reinforced");
        if (tag.hasUUID("Dragon")) {
            d.dragon = tag.getUUID("Dragon");
        }
        for (var t : tag.getList("Fighters", 11)) {
            d.fighters.add(net.minecraft.nbt.NbtUtils.loadUUID(t));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("State", state.ordinal());
        tag.putInt("Timer", timer);
        tag.putInt("Ruined", ruined);
        tag.putBoolean("Reinforced", reinforced);
        if (dragon != null) {
            tag.putUUID("Dragon", dragon);
        }
        var list = new net.minecraft.nbt.ListTag();
        fighters.forEach(u -> list.add(net.minecraft.nbt.NbtUtils.createUUID(u)));
        tag.put("Fighters", list);
        return tag;
    }

    public void set(State s) {
        state = s;
        timer = 0;
        setDirty();
    }
}
