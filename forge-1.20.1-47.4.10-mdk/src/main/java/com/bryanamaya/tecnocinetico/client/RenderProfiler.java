package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code /medirfps <segundos>}: mide en el cliente, durante ese tiempo, cuánto tarda en dibujarse cada tipo de mob
 * (modelo + capas, en el hilo de render; el dibujo en la GPU va aparte), los tiempos de fotograma (media, p95, p99) y
 * las partículas vivas. Escribe {@code tecnoperf.txt} en la carpeta del juego. Se usa con {@code /tecnoperf arena}.
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID, value = Dist.CLIENT)
public final class RenderProfiler {

    private static long until;                                   // nanoTime de fin (0 = parado)
    private static long frameStart, started;
    private static final List<Long> frames = new ArrayList<>();
    private static final Map<String, long[]> byType = new HashMap<>(); // tipo -> {ns totales, veces dibujado}
    private static final Map<LivingEntity, Long> open = new IdentityHashMap<>();
    private static long particleSum, particleSamples;

    private RenderProfiler() {}

    @SubscribeEvent
    public static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("medirfps")
                .then(Commands.argument("segundos", IntegerArgumentType.integer(3, 120)).executes(ctx -> {
                    start(IntegerArgumentType.getInteger(ctx, "segundos"));
                    return 1;
                })));
    }

    private static void start(int seconds) {
        frames.clear();
        byType.clear();
        open.clear();
        particleSum = particleSamples = 0;
        started = System.nanoTime();
        until = started + seconds * 1_000_000_000L;
        Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("commands.tecnocinetico.medirfps.start", seconds));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void pre(RenderLivingEvent.Pre<?, ?> event) {
        if (until != 0) {
            open.put(event.getEntity(), System.nanoTime());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void post(RenderLivingEvent.Post<?, ?> event) {
        Long t0 = until == 0 ? null : open.remove(event.getEntity());
        if (t0 != null) {
            LivingEntity e = event.getEntity();
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
            String variant = e.getPersistentData().getString(com.bryanamaya.tecnocinetico.entity.MobVariants.KEY);
            long[] acc = byType.computeIfAbsent(variant.isEmpty() ? id : id + "#" + variant, k -> new long[2]);
            acc[0] += System.nanoTime() - t0;
            acc[1]++;
        }
    }

    @SubscribeEvent
    public static void frame(TickEvent.RenderTickEvent event) {
        if (until == 0) {
            return;
        }
        long now = System.nanoTime();
        if (event.phase == TickEvent.Phase.START) {
            if (frameStart != 0) {
                frames.add(now - frameStart);                         // de inicio a inicio: el fotograma entero
            }
            frameStart = now;
            if (now >= until) {
                finish();
            }
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (until != 0 && event.phase == TickEvent.Phase.END) {
            try {
                particleSum += Long.parseLong(Minecraft.getInstance().particleEngine.countParticles());
                particleSamples++;
            } catch (NumberFormatException ignored) {
                // formato inesperado: sin dato de partículas
            }
        }
    }

    private static void finish() {
        until = 0;
        frameStart = 0;
        Minecraft mc = Minecraft.getInstance();
        List<Long> sorted = new ArrayList<>(frames);
        sorted.sort(null);
        double avg = sorted.stream().mapToLong(Long::longValue).average().orElse(0) / 1e6;
        double p95 = sorted.isEmpty() ? 0 : sorted.get((int) (sorted.size() * 0.95)) / 1e6;
        double p99 = sorted.isEmpty() ? 0 : sorted.get((int) (sorted.size() * 0.99)) / 1e6;
        int n = Math.max(1, sorted.size());
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("fotogramas %d · media %.2f ms (%.0f FPS) · p95 %.2f ms · p99 %.2f ms · partículas vivas %.0f%n",
                sorted.size(), avg, avg > 0 ? 1000 / avg : 0, p95, p99, particleSamples == 0 ? 0 : particleSum / (double) particleSamples));
        sb.append(String.format("ajustes: distancia %d · gráficos %s · partículas %s%n%n", mc.options.renderDistance().get(),
                mc.options.graphicsMode().get(), mc.options.particles().get()));
        sb.append(String.format("%-48s %10s %12s %12s%n", "tipo", "µs/dibujo", "dibujos/fot.", "ms/fot."));
        byType.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0])).forEach(en -> {
            long[] v = en.getValue();
            sb.append(String.format("%-48s %10.1f %12.1f %12.3f%n", en.getKey(), v[0] / 1e3 / v[1], v[1] / (double) n, v[0] / 1e6 / n));
        });
        Path out = mc.gameDirectory.toPath().resolve("tecnoperf.txt");
        try {
            Files.writeString(out, sb.toString());
            mc.gui.getChat().addMessage(Component.translatable("commands.tecnocinetico.medirfps.done",
                    String.format("%.0f", avg > 0 ? 1000 / avg : 0), out.toString()));
        } catch (java.io.IOException e) {
            mc.gui.getChat().addMessage(Component.literal(e.toString()));
        }
        open.clear();
    }
}
