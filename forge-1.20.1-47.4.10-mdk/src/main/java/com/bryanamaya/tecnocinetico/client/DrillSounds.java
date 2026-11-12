package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.custom.CombustionDrillItem;
import com.bryanamaya.tecnocinetico.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * Motor del Taladro de Combustión (cliente). Cada jugador cercano con un taladro con gasolina en la mano lleva dos
 * bucles que le siguen y se funden entre sí: ralentí (petardeo) y taladrando (motor acelerado, broca y roca). Al
 * empezar a taladrar suena el arranque (sube de vueltas).
 * <p>
 * ¿Está taladrando? El propio jugador: lo sabe su cliente ({@code gameMode.isDestroying()}). Los demás: el servidor
 * avisa con {@code DrillActivityPacket} al empezar a picar y en cada bloque roto (vale ~0,6 s).
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID, value = Dist.CLIENT)
public final class DrillSounds {

    private static final double RANGE_SQR = 48.0 * 48.0;
    private static final int REMOTE_HOLD = 12, LOCAL_HOLD = 4;
    private static final Map<Integer, Engine> ENGINES = new HashMap<>();
    /** Hasta qué tick "taladra" cada jugador (otros: por paquete; el propio: por su gameMode). */
    private static final Map<Integer, Long> DRILLING_UNTIL = new HashMap<>();

    private DrillSounds() {}

    /** Aviso del servidor: este jugador está taladrando. */
    public static void remoteActivity(int entityId) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            DRILLING_UNTIL.put(entityId, level.getGameTime() + REMOTE_HOLD);
        }
    }

    static boolean drilling(Player p) {
        return p.level().getGameTime() <= DRILLING_UNTIL.getOrDefault(p.getId(), Long.MIN_VALUE);
    }

    static boolean running(Player p) {
        ItemStack s = p.getMainHandItem();
        return p.isAlive() && !p.isRemoved() && s.getItem() instanceof CombustionDrillItem && CombustionDrillItem.hasFuel(s);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        var mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (mc.level == null || mc.player == null) {
            ENGINES.clear();
            DRILLING_UNTIL.clear();
            return;
        }
        long now = mc.level.getGameTime();
        // el propio jugador: taladra mientras rompe un bloque (con un margen entre bloques que caen al instante)
        ItemStack mine = mc.player.getMainHandItem();
        if (mc.gameMode != null && mc.gameMode.isDestroying() && mine.getItem() instanceof CombustionDrillItem
                && CombustionDrillItem.hasFuel(mine) && !CombustionDrillItem.isRefueling(mine)) {
            DRILLING_UNTIL.put(mc.player.getId(), now + LOCAL_HOLD);
        }
        ENGINES.values().removeIf(Engine::finished);
        for (Player p : mc.level.players()) {
            if (p.distanceToSqr(mc.player) > RANGE_SQR || !running(p)) {
                continue;
            }
            Engine engine = ENGINES.get(p.getId());
            if (engine == null) {
                engine = new Engine(p);
                ENGINES.put(p.getId(), engine);
                mc.getSoundManager().play(engine.idle);
                mc.getSoundManager().play(engine.drill);
            }
            boolean d = drilling(p);
            if (d && !engine.wasDrilling) {                                   // empieza a taladrar: arranque
                mc.getSoundManager().play(new SimpleSoundInstance(ModSounds.DRILL_REV.get(), SoundSource.PLAYERS, 0.8F,
                        0.95F + p.getRandom().nextFloat() * 0.1F, RandomSource.create(), p.getX(), p.getY() + 1.0, p.getZ()));
            }
            engine.wasDrilling = d;
        }
    }

    /** Los dos bucles de un jugador. */
    private static final class Engine {
        final Loop idle, drill;
        boolean wasDrilling;

        Engine(Player p) {
            idle = new Loop(ModSounds.DRILL_IDLE.get(), p, false);
            drill = new Loop(ModSounds.DRILL_LOOP.get(), p, true);
        }

        boolean finished() {
            return idle.isStopped() && drill.isStopped();
        }
    }

    private static final class Loop extends AbstractTickableSoundInstance {
        private final Player player;
        private final boolean drillLoop;
        private float level;

        Loop(SoundEvent event, Player player, boolean drillLoop) {
            super(event, SoundSource.PLAYERS, RandomSource.create());
            this.player = player;
            this.drillLoop = drillLoop;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.001F;
            follow();
        }

        private void follow() {
            x = player.getX();
            y = player.getY() + 1.0;
            z = player.getZ();
        }

        @Override
        public void tick() {
            if (!running(player)) {
                level -= 0.2F;                                                // se apaga suave al soltarlo o vaciarse
                if (level <= 0) {
                    stop();
                    return;
                }
            } else {
                boolean d = drilling(player);
                float target = drillLoop ? (d ? 1.0F : 0.0F) : (d ? 0.18F : 0.45F);
                level += (target - level) * (target > level ? 0.45F : 0.25F);
            }
            follow();
            volume = Math.max(0.001F, level);
            float wobble = (float) Math.sin(player.tickCount * 0.37F) * 0.015F;
            pitch = drillLoop ? 0.97F + wobble : 1.0F + wobble * 0.5F;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }
}
