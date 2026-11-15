package com.bryanamaya.tecnocinetico.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Path;

/**
 * Perseguir sin recalcular el camino en cada tick. {@code getNavigation().moveTo(entidad)} busca un camino NUEVO cada
 * vez que se llama; los jefes lo hacían cada tick (el Devorador costaba 30 veces un zombi). Aquí se recalcula solo si
 * el camino terminó, cada {@link #EVERY} ticks (desfasado por mob) o si el objetivo se alejó del final del camino.
 */
public final class Chase {

    public static final int EVERY = 10, QUICK = 4;
    private static final double DRIFT_SQR = 2.0 * 2.0;

    private Chase() {}

    public static void moveTo(Mob mob, Entity target, double speed) {
        var nav = mob.getNavigation();
        Path path = nav.getPath();
        int phase = mob.tickCount + mob.getId();
        // aunque el camino haya terminado (o no haya camino posible, p. ej. en un sitio estrecho) se espera al menos
        // QUICK ticks: si no, los mobs grandes atascados buscaban camino en cada tick
        boolean stale = phase % EVERY == 0 || phase % QUICK == 0
                && (nav.isDone() || path == null || path.getTarget().distToCenterSqr(target.position()) > DRIFT_SQR);
        if (stale) {
            nav.moveTo(target, speed);
        } else {
            nav.setSpeedModifier(speed);
        }
    }
}
