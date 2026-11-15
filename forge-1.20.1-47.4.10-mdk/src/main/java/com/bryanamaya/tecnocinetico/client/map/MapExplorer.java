package com.bryanamaya.tecnocinetico.client.map;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Terreno explorado para el Reloj de Navegación (solo cliente): al pasar cerca, cada chunk cargado se convierte en
 * 16x16 colores de mapa (como un mapa vanilla, con sombreado por altura; en el Nether, el suelo bajo el techo). Se pinta
 * en texturas de regiones de 32x32 chunks (512x512 px) y se guarda en disco por mundo/servidor y dimensión
 * ({@code tecnocinetico_maps/}), así el mapa se conserva entre sesiones. Trabaja poco cada vez: unos chunks cada 5 ticks.
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID, value = Dist.CLIENT)
public final class MapExplorer {

    public static final int REGION_CHUNKS = 32, REGION_SIZE = REGION_CHUNKS * 16;
    private static final int PER_PASS = 10, MAX_RADIUS = 12, SAVE_EVERY = 6000;

    private static final Map<String, DimMap> DIMS = new HashMap<>();
    @Nullable
    private static String world;
    private static int sinceSave;

    private static final class Region {
        final NativeImage image = new NativeImage(REGION_SIZE, REGION_SIZE, true);
        final DynamicTexture texture = new DynamicTexture(image);
        final ResourceLocation id;
        boolean upload = true;

        Region(String dim, long key) {
            image.fillRect(0, 0, REGION_SIZE, REGION_SIZE, 0);
            id = Minecraft.getInstance().getTextureManager().register("tecnocinetico_map_" + dim.replace(':', '_') + "_" + Long.toHexString(key), texture);
        }
    }

    private static final class DimMap {
        final Long2ObjectOpenHashMap<int[]> chunks = new Long2ObjectOpenHashMap<>();
        final Long2ObjectOpenHashMap<Region> regions = new Long2ObjectOpenHashMap<>();
        boolean dirty;
    }

    private MapExplorer() {}

    // ------------------------------------------------------------------ exploración

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || mc.level == null || mc.player == null || mc.player.tickCount % 5 != 0) {
            return;
        }
        ClientLevel level = mc.level;
        DimMap map = dim(level.dimension().location().toString());
        int r = Math.min(mc.options.getEffectiveRenderDistance(), MAX_RADIUS);
        int pcx = mc.player.chunkPosition().x, pcz = mc.player.chunkPosition().z;
        boolean refresh = mc.player.tickCount % 100 == 0;                          // lo de alrededor cambia: repasar 3x3
        int budget = PER_PASS;
        for (int d = 0; d <= r && budget > 0; d++) {
            for (int x = pcx - d; x <= pcx + d && budget > 0; x++) {
                for (int z = pcz - d; z <= pcz + d && budget > 0; z++) {
                    if (Math.max(Math.abs(x - pcx), Math.abs(z - pcz)) != d) {
                        continue;                                                   // solo el anillo d
                    }
                    long key = ChunkPos.asLong(x, z);
                    if (map.chunks.containsKey(key) && !(refresh && d <= 1)) {
                        continue;
                    }
                    LevelChunk chunk = level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
                    if (chunk == null || chunk.isEmpty()) {
                        continue;
                    }
                    put(map, key, colors(level, chunk));
                    budget--;
                }
            }
        }
        if (++sinceSave >= SAVE_EVERY / 5) {
            sinceSave = 0;
            saveAll();
        }
    }

    private static void put(DimMap map, long key, int[] colors) {
        map.chunks.put(key, colors);
        map.dirty = true;
        Region region = map.regions.get(regionKey(key));
        if (region != null) {
            paint(region, key, colors);
        }
    }

    /** Colores de mapa (ABGR, como NativeImage) de las 16x16 columnas de un chunk. */
    public static int[] colors(ClientLevel level, LevelChunk chunk) {
        int[] out = new int[256];
        boolean ceiling = level.dimensionType().hasCeiling();
        int bx = chunk.getPos().getMinBlockX(), bz = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int[] heights = new int[256];
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int y = surface(level, chunk, bx + lx, bz + lz, ceiling, p);
                heights[lz * 16 + lx] = y;
            }
        }
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int y = heights[lz * 16 + lx];
                if (y <= level.getMinBuildHeight()) {
                    out[lz * 16 + lx] = 0xFF000000;                                  // vacío
                    continue;
                }
                p.set(bx + lx, y, bz + lz);
                BlockState state = chunk.getBlockState(p);
                MapColor color = state.getMapColor(level, p);
                for (int i = 0; i < 16 && color == MapColor.NONE && p.getY() > level.getMinBuildHeight(); i++) {
                    p.move(0, -1, 0);
                    state = chunk.getBlockState(p);
                    color = state.getMapColor(level, p);
                }
                MapColor.Brightness b;
                if (!state.getFluidState().isEmpty()) {                             // agua: más oscura cuanto más honda
                    int depth = 0;
                    while (depth < 10 && !chunk.getBlockState(p.move(0, -1, 0)).getFluidState().isEmpty()) {
                        depth++;
                    }
                    double shade = depth * 0.1 + ((lx + lz) & 1) * 0.2;
                    b = shade < 0.5 ? MapColor.Brightness.HIGH : shade > 0.9 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
                } else {
                    int north = lz > 0 ? heights[(lz - 1) * 16 + lx] : y;
                    b = y > north ? MapColor.Brightness.HIGH : y < north ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
                }
                out[lz * 16 + lx] = color == MapColor.NONE ? 0xFF000000 : color.calculateRGBColor(b);
            }
        }
        return out;
    }

    /** Altura del bloque visible desde arriba (bajo el techo del Nether, el primer suelo con aire encima). */
    private static int surface(ClientLevel level, LevelChunk chunk, int x, int z, boolean ceiling, BlockPos.MutableBlockPos p) {
        if (!ceiling) {
            return chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        }
        int y = Math.min(level.getMaxBuildHeight() - 1, 126);
        while (y > level.getMinBuildHeight() && !chunk.getBlockState(p.set(x, y, z)).isAir()) {
            y--;                                                                     // atraviesa el techo
        }
        while (y > level.getMinBuildHeight() && chunk.getBlockState(p.set(x, y, z)).isAir()) {
            y--;                                                                     // baja hasta el suelo
        }
        return y;
    }

    // ------------------------------------------------------------------ regiones (texturas)

    public static long regionKey(long chunkKey) {
        return ChunkPos.asLong(Math.floorDiv(ChunkPos.getX(chunkKey), REGION_CHUNKS), Math.floorDiv(ChunkPos.getZ(chunkKey), REGION_CHUNKS));
    }

    private static void paint(Region region, long chunkKey, int[] colors) {
        int ox = Math.floorMod(ChunkPos.getX(chunkKey), REGION_CHUNKS) * 16, oz = Math.floorMod(ChunkPos.getZ(chunkKey), REGION_CHUNKS) * 16;
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                region.image.setPixelRGBA(ox + lx, oz + lz, colors[lz * 16 + lx]);
            }
        }
        region.upload = true;
    }

    /**
     * Textura de la región (rx, rz) de esa dimensión, lista para dibujar, o null si no hay nada explorado allí.
     * La primera vez se pintan todos sus chunks conocidos.
     */
    @Nullable
    public static ResourceLocation region(String dimId, int rx, int rz) {
        DimMap map = dim(dimId);
        long rk = ChunkPos.asLong(rx, rz);
        Region region = map.regions.get(rk);
        if (region == null) {
            boolean any = false;
            for (int cx = rx * REGION_CHUNKS; cx < (rx + 1) * REGION_CHUNKS && !any; cx++) {
                for (int cz = rz * REGION_CHUNKS; cz < (rz + 1) * REGION_CHUNKS && !any; cz++) {
                    any = map.chunks.containsKey(ChunkPos.asLong(cx, cz));
                }
            }
            if (!any) {
                return null;
            }
            region = new Region(dimId, rk);
            for (var e : map.chunks.long2ObjectEntrySet()) {
                if (regionKey(e.getLongKey()) == rk) {
                    paint(region, e.getLongKey(), e.getValue());
                }
            }
            map.regions.put(rk, region);
        }
        if (region.upload) {
            region.texture.upload();
            region.upload = false;
        }
        return region.id;
    }

    /** ¿Se conoce ese chunk? (para las pruebas y el panel). */
    public static boolean known(String dimId, int cx, int cz) {
        return dim(dimId).chunks.containsKey(ChunkPos.asLong(cx, cz));
    }

    // ------------------------------------------------------------------ disco

    private static DimMap dim(String dimId) {
        return DIMS.computeIfAbsent(dimId, MapExplorer::load);
    }

    @Nullable
    private static Path file(String dimId) {
        Minecraft mc = Minecraft.getInstance();
        if (world == null) {
            if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
                Path root = mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
                world = "sp_" + root.getFileName();
            } else if (mc.getCurrentServer() != null) {
                world = "mp_" + mc.getCurrentServer().ip;
            } else {
                return null;
            }
            world = world.replaceAll("[^a-zA-Z0-9._-]", "_");
        }
        return mc.gameDirectory.toPath().resolve("tecnocinetico_maps").resolve(world).resolve(dimId.replaceAll("[^a-zA-Z0-9._-]", "_") + ".dat");
    }

    private static DimMap load(String dimId) {
        DimMap map = new DimMap();
        Path f = file(dimId);
        if (f == null || !Files.exists(f)) {
            return map;
        }
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(Files.newInputStream(f)))) {
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                long key = in.readLong();
                int[] colors = new int[256];
                for (int j = 0; j < 256; j++) {
                    colors[j] = in.readInt();
                }
                map.chunks.put(key, colors);
            }
        } catch (IOException e) {
            Tecnocinetico.LOGGER.warn("[Mapa] no se pudo leer {}", f, e);
        }
        return map;
    }

    public static void saveAll() {
        for (var e : DIMS.entrySet()) {
            DimMap map = e.getValue();
            Path f = file(e.getKey());
            if (!map.dirty || f == null) {
                continue;
            }
            try {
                Files.createDirectories(f.getParent());
                try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(f)))) {
                    out.writeInt(map.chunks.size());
                    for (var c : map.chunks.long2ObjectEntrySet()) {
                        out.writeLong(c.getLongKey());
                        for (int v : c.getValue()) {
                            out.writeInt(v);
                        }
                    }
                }
                map.dirty = false;
            } catch (IOException ex) {
                Tecnocinetico.LOGGER.warn("[Mapa] no se pudo guardar {}", f, ex);
            }
        }
    }

    /** Al salir del mundo: guardar y soltar las texturas. */
    public static void close() {
        saveAll();
        var textures = Minecraft.getInstance().getTextureManager();
        for (DimMap map : DIMS.values()) {
            for (Region r : map.regions.values()) {
                textures.release(r.id);
            }
        }
        DIMS.clear();
        world = null;
    }
}
