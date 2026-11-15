package com.bryanamaya.tecnocinetico.command;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.MobVariants;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Herramienta de rendimiento gráfico (junto con {@code /medirfps}, de cliente):
 * {@code /tecnoperf arena} pone delante del jugador una cuadrícula con TODOS los mobs del mod y las variantes de
 * Nether/End, quietos (sin IA) e invulnerables; {@code /tecnoperf limpiar} los quita. Solo operadores.
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID)
public final class PerfArenaCommand {

    private static final String TAG = "tecnoperf";
    private static final int PER_ROW = 8;

    private PerfArenaCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tecnoperf")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("arena").executes(PerfArenaCommand::arena))
                .then(Commands.literal("limpiar").executes(PerfArenaCommand::clear)));
    }

    private static int arena(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        clear(level);
        List<Mob> mobs = new ArrayList<>();
        for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
            if (BuiltInRegistries.ENTITY_TYPE.getKey(t).getNamespace().equals(Tecnocinetico.MOD_ID)
                    && t.create(level) instanceof Mob mob) {
                mobs.add(mob);
            }
        }
        for (EntityType<?> t : List.of(EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.ZOMBIFIED_PIGLIN, EntityType.GHAST,
                EntityType.BLAZE, EntityType.WITHER_SKELETON)) {
            variant(level, t, MobVariants.NETHER, mobs);
        }
        for (EntityType<?> t : List.of(EntityType.ENDERMAN, EntityType.SHULKER, EntityType.ENDERMITE)) {
            variant(level, t, MobVariants.END, mobs);
        }
        mobs.sort(Comparator.comparingDouble(m -> m.getBbWidth()));
        // cuadrícula delante del jugador: filas perpendiculares a la mirada, separadas según el mob más ancho de cada fila
        Vec3 fwd = Vec3.directionFromRotation(0, player.getYRot());
        Vec3 side = new Vec3(-fwd.z, 0, fwd.x);
        double depth = 6;
        for (int row = 0; row * PER_ROW < mobs.size(); row++) {
            List<Mob> line = mobs.subList(row * PER_ROW, Math.min(mobs.size(), (row + 1) * PER_ROW));
            double gap = Math.max(2.5, line.get(line.size() - 1).getBbWidth() + 1.5);
            depth += gap / 2;
            for (int i = 0; i < line.size(); i++) {
                Vec3 at = player.position().add(fwd.scale(depth)).add(side.scale((i - (line.size() - 1) / 2.0) * gap));
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(at));
                Mob mob = line.get(i);
                mob.moveTo(at.x, top.getY(), at.z, player.getYRot() + 180, 0);
                mob.setYHeadRot(player.getYRot() + 180);
                mob.setNoAi(true);
                mob.setInvulnerable(true);
                mob.setPersistenceRequired();
                mob.addTag(TAG);
                level.addFreshEntity(mob);
            }
            depth += gap / 2;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.tecnocinetico.tecnoperf.arena", mobs.size()), false);
        return mobs.size();
    }

    private static void variant(ServerLevel level, EntityType<?> t, String kind, List<Mob> out) {
        if (t.create(level) instanceof Mob mob) {
            MobVariants.mark(mob, kind);                     // al entrar al mundo se aplica la mejora (y el cliente ve el remodelado)
            out.add(mob);
        }
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) {
        int n = clear(ctx.getSource().getLevel());
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.tecnocinetico.tecnoperf.cleared", n), false);
        return n;
    }

    private static int clear(ServerLevel level) {
        List<Entity> found = new ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            if (e.getTags().contains(TAG)) {
                found.add(e);
            }
        }
        found.forEach(Entity::discard);
        return found.size();
    }
}
