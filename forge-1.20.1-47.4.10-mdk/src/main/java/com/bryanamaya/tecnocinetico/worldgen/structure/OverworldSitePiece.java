package com.bryanamaya.tecnocinetico.worldgen.structure;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.block.ModWorldBlocks;
import com.bryanamaya.tecnocinetico.block.entity.CoreConsoleBlockEntity;
import com.bryanamaya.tecnocinetico.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import java.util.ArrayList;

/** Obra por coordenadas locales absolutas. Ningún aleatorio depende del orden de generación de los chunks. */
public class OverworldSitePiece extends StructurePiece {
    public static final String[] IDS = {"relay_antenna", "data_center", "survivor_bunker", "swarm_laboratory", "sunken_city"};
    private final int ox, oy, oz, surface, sea, site;
    private final long seed;

    public OverworldSitePiece(BlockPos p, int surface, int sea, int site, long seed) {
        super(ModStructures.OVERWORLD_SITE_PIECE.get(), 0,
                new BoundingBox(p.getX(), p.getY() - 4, p.getZ(), p.getX() + 32,
                        Math.max(surface + 6, p.getY() + 30), p.getZ() + 40));
        ox = p.getX(); oy = p.getY(); oz = p.getZ();
        this.surface = surface; this.sea = sea; this.site = site; this.seed = seed;
        setOrientation(null);
    }

    public OverworldSitePiece(CompoundTag tag) {
        super(ModStructures.OVERWORLD_SITE_PIECE.get(), tag);
        ox = tag.getInt("X"); oy = tag.getInt("Y"); oz = tag.getInt("Z");
        surface = tag.getInt("Surface"); sea = tag.getInt("Sea"); site = tag.getInt("Site"); seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("X", ox); tag.putInt("Y", oy); tag.putInt("Z", oz);
        tag.putInt("Surface", surface); tag.putInt("Sea", sea); tag.putInt("Site", site); tag.putLong("Seed", seed);
    }

    private BlockPos pos(int x, int y, int z) { return new BlockPos(ox + x, oy + y, oz + z); }
    private void put(WorldGenLevel l, BoundingBox b, int x, int y, int z, BlockState s) {
        BlockPos p = pos(x, y, z);
        if (b.isInside(p)) l.setBlock(p, s, 2);
    }
    private void put(WorldGenLevel l, BoundingBox b, int x, int y, int z, Block block) { put(l, b, x, y, z, block.defaultBlockState()); }
    private void fill(WorldGenLevel l, BoundingBox b, int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) put(l, b, x, y, z, block);
    }
    private long hash(int x, int y, int z) { return seed ^ x * 73856093L ^ y * 19349663L ^ z * 83492791L; }
    private Block wall(int x, int y, int z) {
        return Math.floorMod(hash(x, y, z), 7) == 0 ? ModWorldBlocks.MOSSY_CONCRETE.get() : ModWorldBlocks.CRACKED_CONCRETE.get();
    }
    /** Habitación con suelo/techo, ventanas y puerta al norte; las ruinas marinas conservan el agua interior. */
    private void room(WorldGenLevel l, BoundingBox b, int x0, int z0, int w, int d, int h, boolean flooded) {
        for (int x = x0; x < x0 + w; x++) for (int z = z0; z < z0 + d; z++) for (int y = 0; y <= h; y++) {
            boolean edge = x == x0 || x == x0 + w - 1 || z == z0 || z == z0 + d - 1;
            Block block = y == 0 ? Blocks.POLISHED_DEEPSLATE : y == h || edge ? wall(x, y, z)
                    : flooded && oy + y < sea ? Blocks.WATER : Blocks.AIR;
            if (edge && y >= 2 && y <= 3 && (x + z) % 5 < 3) block = flooded ? Blocks.WATER : Blocks.CYAN_STAINED_GLASS;
            if (z == z0 && Math.abs(x - (x0 + w / 2)) <= 1 && y >= 1 && y <= 3)
                block = flooded ? Blocks.WATER : Blocks.AIR;
            if (flooded && y == h && Math.floorMod(hash(x, y, z), 4) == 0) block = Blocks.WATER;
            put(l, b, x, y, z, block);
        }
        for (int x = x0 + 2; x < x0 + w - 1; x += 5) put(l, b, x, h - 1, z0 + 2, Blocks.SEA_LANTERN);
    }
    private void chest(WorldGenLevel l, BoundingBox b, int x, int y, int z, String loot) {
        BlockPos p = pos(x, y, z);
        if (!b.isInside(p)) return;
        put(l, b, x, y, z, Blocks.CHEST);
        RandomizableContainerBlockEntity.setLootTable(l, RandomSource.create(hash(x, y, z)), p,
                ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "chests/" + loot));
    }
    private void guard(WorldGenLevel l, BoundingBox b, int x, int y, int z, boolean flying) {
        BlockPos p = pos(x, y, z);
        if (!b.isInside(p)) return;
        Mob mob = (flying ? ModEntities.DRONE.get() : site == 3 ? ModEntities.SWARM_BODY.get() : ModEntities.SENTRY_TURRET.get()).create(l.getLevel());
        if (mob == null) return;
        mob.moveTo(p.getX() + .5, p.getY(), p.getZ() + .5, 180, 0);
        mob.finalizeSpawn(l, l.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null, null);
        mob.setPersistenceRequired();
        l.addFreshEntityWithPassengers(mob);
    }
    private void console(WorldGenLevel l, BoundingBox b, int x, int z, String boss, int sx, int sy, int sz) {
        BlockPos p = pos(x, 1, z);
        if (!b.isInside(p)) return;
        put(l, b, x, 1, z, ModBlocks.CORE_CONSOLE.get());
        if (l.getBlockEntity(p) instanceof CoreConsoleBlockEntity c) {
            c.configureGuardian(boss, pos(sx, sy, sz));
            var seal = new ArrayList<BlockPos>();
            if (site == 3) {
                for (int y = 1; y <= 3; y++) seal.add(pos(4, y, 5));
            } else {
                for (int dx = -1; dx <= 1; dx++) for (int y = 1; y <= 3; y++) seal.add(pos(16 + dx, y, 0));
            }
            c.setSeal(seal);
        }
    }

    @Override
    public void postProcess(WorldGenLevel l, StructureManager sm, ChunkGenerator gen, RandomSource ignored,
                            BoundingBox b, ChunkPos cp, BlockPos pivot) {
        switch (site) {
            case 0 -> relay(l, b);
            case 1 -> data(l, b);
            case 2 -> bunker(l, b);
            case 3 -> laboratory(l, b);
            case 4 -> city(l, b);
            default -> throw new IllegalStateException("Instalación desconocida: " + site);
        }
    }

    private void relay(WorldGenLevel l, BoundingBox b) {
        // Plataforma industrial: cimentación corta, parapeto, acceso y zona despejada de combate.
        fill(l, b, 0, -3, 0, 32, 0, 40, ModWorldBlocks.CONCRETE.get());
        fill(l, b, 0, 1, 0, 32, 28, 40, Blocks.AIR);
        for (int x = 0; x <= 32; x++) for (int z = 0; z <= 40; z++) {
            if (x == 0 || x == 32 || z == 0 || z == 40) {
                if (z != 0 || Math.abs(x - 16) > 1) fill(l, b, x, 1, z, x, 3, z, ModWorldBlocks.RUSTED_GRATE.get());
            }
            if ((x == 3 || x == 29) && z % 6 == 3) put(l, b, x, 0, z, Blocks.SEA_LANTERN);
        }
        // Torre arriostrada de cuatro patas y tres niveles de crucetas, con paneles de antena.
        for (int x : new int[]{5, 9}) for (int z : new int[]{6, 10}) fill(l, b, x, 1, z, x, 23, z, ModWorldBlocks.RUSTED_METAL_BEAM.get());
        for (int y = 6; y <= 22; y += 8) {
            fill(l, b, 5, y, 6, 9, y, 10, ModWorldBlocks.RUSTED_GRATE.get());
            fill(l, b, 4, y + 1, 7, 4, y + 4, 9, Blocks.WHITE_CONCRETE);
            fill(l, b, 10, y + 1, 7, 10, y + 4, 9, Blocks.WHITE_CONCRETE);
        }
        fill(l, b, 7, 23, 8, 7, 27, 8, Blocks.COPPER_BLOCK);
        put(l, b, 7, 28, 8, Blocks.LIGHTNING_ROD);
        room(l, b, 22, 4, 8, 10, 6, false);
        chest(l, b, 24, 1, 10, IDS[0]);
        console(l, b, 16, 12, "network_watcher", 16, 5, 25);
        guard(l, b, 9, 12, 18, true); guard(l, b, 25, 14, 28, true);
    }

    private void data(WorldGenLevel l, BoundingBox b) {
        room(l, b, 1, 1, 31, 37, 8, false);
        // Filas de racks con pasillo central, lámparas de diagnóstico y refrigeración por el techo.
        for (int x : new int[]{5, 10, 21, 26}) for (int z = 8; z <= 29; z += 4) {
            fill(l, b, x, 1, z, x + 1, 4, z + 1, Blocks.POLISHED_BLACKSTONE);
            put(l, b, x, 3, z - 1, Blocks.REDSTONE_LAMP);
            put(l, b, x + 1, 2, z - 1, Blocks.CYAN_STAINED_GLASS);
        }
        fill(l, b, 14, 7, 4, 18, 7, 34, ModWorldBlocks.RUSTED_GRATE.get());
        fill(l, b, 13, 1, 33, 19, 2, 34, Blocks.POLISHED_DEEPSLATE);
        for (int x = 14; x <= 18; x += 2) put(l, b, x, 3, 34, Blocks.CYAN_STAINED_GLASS);
        chest(l, b, 8, 1, 34, IDS[1]); chest(l, b, 25, 1, 34, "data_archive");
        guard(l, b, 16, 1, 25, false); guard(l, b, 16, 1, 10, false);
        // Acceso hasta el borde del volumen, sin una pared exterior que tape la puerta.
        fill(l, b, 15, 0, 0, 17, 0, 1, Blocks.POLISHED_DEEPSLATE);
        fill(l, b, 15, 1, 0, 17, 3, 1, Blocks.AIR);
    }

    /** Pozo de servicio hasta la superficie: escalera apoyada y acceso lateral en una caseta. */
    private void shaft(WorldGenLevel l, BoundingBox b) {
        int top = surface - oy;
        for (int y = 1; y <= top + 4; y++) {
            for (int x = 3; x <= 5; x++) for (int z = 3; z <= 5; z++)
                put(l, b, x, y, z, x == 4 && z == 4 ? Blocks.AIR : ModWorldBlocks.CONCRETE.get());
            put(l, b, 4, y, 4, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        }
        // Huecos en la pared sur: salida a la sala y salida a la superficie.
        fill(l, b, 4, 1, 5, 4, 3, 5, Blocks.AIR);
        fill(l, b, 4, top + 1, 5, 4, top + 3, 5, Blocks.AIR);
        fill(l, b, 3, top + 5, 3, 5, top + 5, 5, ModWorldBlocks.RUSTED_METAL_SHEET.get());
        put(l, b, 4, top + 6, 4, Blocks.LIGHTNING_ROD);
    }

    private void bunker(WorldGenLevel l, BoundingBox b) {
        room(l, b, 1, 1, 25, 30, 6, false);
        // Dormitorios a un lado, taller y despensa al otro. Refugio sin hostiles prefijados.
        fill(l, b, 11, 1, 7, 11, 5, 27, ModWorldBlocks.CONCRETE.get());
        fill(l, b, 11, 1, 16, 11, 3, 18, Blocks.AIR);
        for (int z : new int[]{9, 14, 22, 26}) {
            fill(l, b, 3, 1, z, 7, 1, z + 1, ModWorldBlocks.DEAD_PLANKS.get());
            fill(l, b, 4, 2, z, 7, 2, z + 1, Blocks.RED_WOOL);
        }
        put(l, b, 22, 1, 10, Blocks.CRAFTING_TABLE); put(l, b, 22, 1, 12, Blocks.FURNACE);
        put(l, b, 22, 1, 14, Blocks.CAULDRON);
        chest(l, b, 21, 1, 23, IDS[2]); chest(l, b, 22, 1, 26, "settlement_storage");
        shaft(l, b);
    }

    private void laboratory(WorldGenLevel l, BoundingBox b) {
        room(l, b, 0, 0, 33, 41, 12, false);
        // Cultivos acristalados en los laterales; el centro permite esquivar las cargas de la Matriarca.
        for (int x : new int[]{6, 26}) for (int z : new int[]{12, 22, 32}) {
            fill(l, b, x - 2, 1, z - 2, x + 2, 1, z + 2, Blocks.POLISHED_DEEPSLATE);
            for (int y = 2; y <= 6; y++) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                boolean edge = Math.abs(dx) == 1 || Math.abs(dz) == 1;
                put(l, b, x + dx, y, z + dz, edge ? Blocks.CYAN_STAINED_GLASS : ModWorldBlocks.SWARM_COMB.get());
            }
            fill(l, b, x - 1, 7, z - 1, x + 1, 7, z + 1, Blocks.COPPER_BLOCK);
            put(l, b, x, 8, z, Blocks.SEA_LANTERN);
        }
        fill(l, b, 11, 0, 17, 21, 0, 33, ModWorldBlocks.SWARM_COMB.get());
        console(l, b, 16, 10, "swarm_matriarch", 16, 1, 26);
        chest(l, b, 28, 1, 37, IDS[3]); chest(l, b, 4, 1, 37, "data_archive");
        guard(l, b, 9, 1, 34, false); guard(l, b, 23, 1, 34, false);
        shaft(l, b);
    }

    private void city(WorldGenLevel l, BoundingBox b) {
        fill(l, b, 0, -2, 0, 32, 0, 40, ModWorldBlocks.CRACKED_CONCRETE.get());
        fill(l, b, 0, 1, 0, 32, sea - oy - 1, 40, Blocks.WATER);
        // Avenida inundada, tres edificios derruidos y un puente peatonal entre las torres.
        fill(l, b, 14, 0, 0, 18, 0, 40, ModWorldBlocks.CRACKED_ASPHALT.get());
        room(l, b, 1, 4, 11, 13, 9, true);
        room(l, b, 21, 4, 11, 13, 14, true);
        room(l, b, 2, 25, 11, 14, 7, true);
        fill(l, b, 10, 7, 9, 22, 7, 11, ModWorldBlocks.RUSTED_GRATE.get());
        for (int x = 14; x <= 18; x += 4) for (int z = 5; z <= 35; z += 10) {
            fill(l, b, x, 1, z, x, 4, z, ModWorldBlocks.RUSTED_METAL_BEAM.get());
            put(l, b, x, 5, z, Blocks.SEA_LANTERN);
        }
        chest(l, b, 4, 1, 13, IDS[4]); chest(l, b, 28, 1, 13, IDS[4]); chest(l, b, 5, 1, 35, "data_archive");
        // La avenida queda inundada, libre de roca; los edificios siguen siendo ruinas submarinas.
    }
}
