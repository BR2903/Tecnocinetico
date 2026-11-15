package com.bryanamaya.tecnocinetico.map;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.gear.TacticalGear;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.network.ModNetwork;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servidor del Reloj de Navegación: sincroniza el satélite, los marcadores y las estructuras; el satélite detecta solo
 * las estructuras importantes cerca de quien lleva el reloj ({@link #SCAN_RADIUS} chunks cada {@link #SCAN_EVERY}
 * ticks) y busca una estructura concreta cuando se le pide (como /locate, con espera de {@link #LOCATE_COOLDOWN}).
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID)
public final class MapEvents {

    public static final int SCAN_EVERY = 100, SCAN_RADIUS = 8, LOCATE_RADIUS = 100, LOCATE_COOLDOWN = 100;
    private static final String NEXT_LOCATE = "TecnoNextLocate";

    private MapEvents() {}

    // ------------------------------------------------------------------ requisitos

    /** ¿Lleva el reloj puesto en la muñeca (encendido) o en la mano? */
    public static boolean hasWatch(Player p) {
        if (p.getMainHandItem().is(ModItems.WRIST_MAP.get()) || p.getOffhandItem().is(ModItems.WRIST_MAP.get())) {
            return true;
        }
        return TacticalGear.of(p).map(g -> g.active(TacticalGear.WRIST).is(ModItems.WRIST_MAP.get())).orElse(false);
    }

    // ------------------------------------------------------------------ sincronización

    public static void sync(ServerPlayer p) {
        NavigationData d = NavigationData.get(p.server);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                new MapPackets.Sync(d.satellite(), d.visibleTo(p.getUUID()), List.copyOf(d.found())));
    }

    public static void syncAll(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(MapEvents::sync);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            sync(p);
        }
    }

    // ------------------------------------------------------------------ satélite

    /** Despliega el satélite (lo llama el objeto ya comprobada la altura). */
    public static void deploy(ServerPlayer p) {
        NavigationData d = NavigationData.get(p.server);
        d.deploy(p.getGameProfile().getName());
        var adv = p.server.getAdvancements().getAdvancement(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "satellite_deployed"));
        if (adv != null) {
            var progress = p.getAdvancements().getOrStartProgress(adv);
            for (String c : progress.getRemainingCriteria()) {
                p.getAdvancements().award(adv, c);
            }
        }
        p.server.getPlayerList().broadcastSystemMessage(Component.translatable("message.tecnocinetico.satellite.deployed",
                p.getDisplayName()).withStyle(ChatFormatting.GOLD), false);
        syncAll(p.server);
    }

    // ------------------------------------------------------------------ detección automática

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p) || p.tickCount % SCAN_EVERY != 0
                || !hasWatch(p) || !NavigationData.get(p.server).satellite()) {
            return;
        }
        if (scan(p.serverLevel(), p.chunkPosition().x, p.chunkPosition().z) > 0) {
            syncAll(p.server);
        }
    }

    /** Busca comienzos de estructuras importantes en los chunks cargados alrededor. Devuelve cuántas nuevas. */
    public static int scan(ServerLevel level, int cx, int cz) {
        NavigationData d = NavigationData.get(level.getServer());
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        String dim = level.dimension().location().toString();
        int added = 0;
        for (int x = cx - SCAN_RADIUS; x <= cx + SCAN_RADIUS; x++) {
            for (int z = cz - SCAN_RADIUS; z <= cz + SCAN_RADIUS; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) {
                    continue;
                }
                for (var e : chunk.getAllStarts().entrySet()) {
                    if (!e.getValue().isValid()) {
                        continue;
                    }
                    ResourceLocation id = registry.getKey(e.getKey());
                    MapCatalog.Entry entry = id == null ? null : MapCatalog.byStructure(id);
                    if (entry != null) {
                        BlockPos c = e.getValue().getBoundingBox().getCenter();
                        if (d.addFound(new NavigationData.Found(entry.key(), dim, c.getX(), c.getZ()))) {
                            added++;
                        }
                    }
                }
            }
        }
        return added;
    }

    // ------------------------------------------------------------------ acciones del cliente

    public static void action(ServerPlayer p, MapPackets.Action a) {
        NavigationData d = NavigationData.get(p.server);
        if (!d.satellite() || !hasWatch(p)) {
            return;
        }
        switch (a.op()) {
            case MapPackets.Action.ADD -> {
                String name = a.text().isBlank() ? Component.translatable("map.tecnocinetico.marker.default").getString() : a.text().trim();
                if (name.length() > NavigationData.MAX_NAME) {
                    name = name.substring(0, NavigationData.MAX_NAME);
                }
                var m = new NavigationData.Marker(UUID.randomUUID(), p.getUUID(), p.getGameProfile().getName(), name,
                        p.level().dimension().location().toString(), a.x(), a.y(), a.z(), a.color() & 0xFFFFFF, a.shared());
                if (!d.addMarker(m)) {
                    p.displayClientMessage(Component.translatable("map.tecnocinetico.marker.full", NavigationData.MAX_MARKERS_PER_PLAYER)
                            .withStyle(ChatFormatting.RED), true);
                    return;
                }
                if (m.shared()) {
                    syncAll(p.server);
                } else {
                    sync(p);
                }
            }
            case MapPackets.Action.REMOVE -> {
                if (d.removeMarker(a.id(), p.getUUID())) {
                    syncAll(p.server);
                }
            }
            case MapPackets.Action.LOCATE -> locate(p, a.text());
            default -> {}
        }
    }

    /** Busca la estructura más cercana de ese tipo en la dimensión del jugador y la apunta para todos. */
    public static void locate(ServerPlayer p, String key) {
        MapCatalog.Entry entry = MapCatalog.byKey(key);
        ServerLevel level = p.serverLevel();
        if (entry == null) {
            return;
        }
        long now = level.getGameTime();
        if (now < p.getPersistentData().getLong(NEXT_LOCATE)) {
            p.displayClientMessage(Component.translatable("map.tecnocinetico.locate.busy").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        p.getPersistentData().putLong(NEXT_LOCATE, now + LOCATE_COOLDOWN);
        if (!entry.dimension().equals(level.dimension())) {
            p.displayClientMessage(Component.translatable("map.tecnocinetico.locate.other_dimension").withStyle(ChatFormatting.YELLOW), true);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new MapPackets.LocateResult(key, false, 0, 0));
            return;
        }
        Optional<BlockPos> pos = find(level, entry, p.blockPosition());
        if (pos.isEmpty()) {
            p.displayClientMessage(Component.translatable("map.tecnocinetico.locate.none", Component.translatable(entry.langKey()))
                    .withStyle(ChatFormatting.YELLOW), true);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new MapPackets.LocateResult(key, false, 0, 0));
            return;
        }
        BlockPos b = pos.get();
        NavigationData.get(p.server).addFound(new NavigationData.Found(key, level.dimension().location().toString(), b.getX(), b.getZ()));
        syncAll(p.server);
        int dist = (int) Math.sqrt(p.distanceToSqr(b.getX() + 0.5, p.getY(), b.getZ() + 0.5));
        p.displayClientMessage(Component.translatable("map.tecnocinetico.locate.found", Component.translatable(entry.langKey()), dist)
                .withStyle(ChatFormatting.GREEN), true);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new MapPackets.LocateResult(key, true, b.getX(), b.getZ()));
    }

    /** La más cercana de cualquiera de los ids de la entrada (como /locate). */
    public static Optional<BlockPos> find(ServerLevel level, MapCatalog.Entry entry, BlockPos from) {
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        List<Holder<Structure>> holders = new ArrayList<>();
        for (ResourceLocation id : entry.ids()) {
            registry.getHolder(ResourceKey.create(Registries.STRUCTURE, id)).ifPresent(holders::add);
        }
        if (holders.isEmpty()) {
            return Optional.empty();
        }
        Pair<BlockPos, Holder<Structure>> r = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, HolderSet.direct(holders), from, LOCATE_RADIUS, false);
        return r == null ? Optional.empty() : Optional.of(r.getFirst());
    }
}
