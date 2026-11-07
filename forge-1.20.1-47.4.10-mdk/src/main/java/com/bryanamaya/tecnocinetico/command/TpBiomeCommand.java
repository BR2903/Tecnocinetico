package com.bryanamaya.tecnocinetico.command;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.worldgen.ModBiomes;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

/**
 * /tpbioma <bioma>: busca el bioma más cercano y teletransporta al jugador a su superficie
 * (o a un hueco de cueva si es un bioma subterráneo). Requiere permisos de operador (trucos activados).
 * Sin namespace se entiende "tecnocinetico:" (p. ej. /tpbioma dead_megacity).
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID)
public final class TpBiomeCommand {

    private static final DynamicCommandExceptionType NOT_FOUND = new DynamicCommandExceptionType(
            id -> Component.translatable("commands.tecnocinetico.tpbioma.not_found", id));
    private static final Set<ResourceKey<Biome>> CAVES = Set.of(ModBiomes.CINETRITE_CAVES, ModBiomes.INDUSTRIAL_TUNNELS,
            ModBiomes.DEEP_FACILITY);

    private TpBiomeCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("tpbioma")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("bioma", ResourceLocationArgument.id())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggestResource(
                                ctx.getSource().registryAccess().registryOrThrow(Registries.BIOME).keySet().stream()
                                        .filter(id -> id.getNamespace().equals(Tecnocinetico.MOD_ID)), b))
                        .executes(TpBiomeCommand::run)));
    }

    private static int run(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = ctx.getSource().getLevel();
        ResourceLocation raw = ResourceLocationArgument.getId(ctx, "bioma");
        // "/tpbioma dead_megacity" → tecnocinetico:dead_megacity
        ResourceLocation id = raw.getNamespace().equals("minecraft") && !ctx.getInput().contains("minecraft:")
                ? ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, raw.getPath()) : raw;
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, id);
        if (level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(key).isEmpty()) {
            throw NOT_FOUND.create(id.toString());
        }

        ctx.getSource().sendSuccess(() -> Component.translatable("commands.tecnocinetico.tpbioma.searching", id.toString()), false);
        Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(h -> h.is(key), player.blockPosition(), 6400, 32, 64);
        if (found == null) {
            throw NOT_FOUND.create(id.toString());
        }
        BlockPos target = CAVES.contains(key) ? caveSpot(level, found.getFirst()) : surface(level, found.getFirst());
        if (target == null) {
            target = surface(level, found.getFirst());
        }
        player.teleportTo(level, target.getX() + 0.5, target.getY(), target.getZ() + 0.5, player.getYRot(), player.getXRot());
        BlockPos t = target;
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.tecnocinetico.tpbioma.done",
                id.toString(), t.getX(), t.getY(), t.getZ()), true);
        return 1;
    }

    private static BlockPos surface(ServerLevel level, BlockPos pos) {
        level.getChunk(pos);                                                // genera el chunk si hace falta
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        return new BlockPos(pos.getX(), y, pos.getZ());
    }

    /** Hueco con suelo firme y 2 bloques libres cerca de la posición encontrada (para biomas de cueva). */
    private static BlockPos caveSpot(ServerLevel level, BlockPos pos) {
        level.getChunk(pos);
        for (int r = 0; r <= 8; r += 2) {
            for (int dy = -16; dy <= 16; dy++) {
                for (int dx = -r; dx <= r; dx += Math.max(1, r)) {
                    for (int dz = -r; dz <= r; dz += Math.max(1, r)) {
                        BlockPos p = pos.offset(dx, dy, dz);
                        if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()
                                && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP)
                                && level.getFluidState(p).isEmpty()) {
                            return p;
                        }
                    }
                }
            }
        }
        return null;
    }
}
