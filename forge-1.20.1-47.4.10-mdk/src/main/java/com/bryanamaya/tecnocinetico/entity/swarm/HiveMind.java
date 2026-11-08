package com.bryanamaya.tecnocinetico.entity.swarm;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mente colmena: vive dentro del {@link HiveNodeEntity} y coordina a todas sus avispas. Las avispas solo
 * ejecutan lo que esta clase decide (así el grupo es "listo" y cada avispa es barata de simular).
 *
 * Estados: PATROL (patrulla) → ALERT (un intruso cerca: lo vigilan y zumban de aviso) → ATTACK (ataque
 * coordinado: rodean al objetivo en ángulos repartidos y atacan por turnos). Además ADAPTA la táctica según
 * cómo le hacen daño (a distancia o cuerpo a cuerpo); esa memoria se guarda con el Nodo.
 */
public class HiveMind {

    public enum State { PATROL, ALERT, ATTACK }

    /** Radio de la zona de la colmena: entrar aquí es atacar. */
    public static final double TERRITORY = 16.0D;
    /** Radio de vigilancia: aquí te siguen y avisan. */
    public static final double ALERT_RADIUS = 30.0D;
    /** Si el objetivo se aleja más que esto, se rinden y vuelven. */
    public static final double GIVE_UP = 56.0D;
    public static final int MAX_MEMBERS = 10;

    private final HiveNodeEntity node;
    private final Map<UUID, SwarmWaspEntity> members = new LinkedHashMap<>();
    private State state = State.PATROL;
    @Nullable
    private LivingEntity target;
    private int calmTicks;
    /** Ángulo base del círculo de ataque (gira despacio: las avispas orbitan). */
    private float orbitAngle;
    private int strikeCooldown;

    // memoria de adaptación (0..1): cuánto daño recibió a distancia y cuerpo a cuerpo
    private float rangedScore, meleeScore;
    private int spawnBudget = 24;
    private int spawnCooldown = 100;

    public HiveMind(HiveNodeEntity node) {
        this.node = node;
    }

    // ------------------------------------------------------------------ miembros

    /** Cada avispa se reporta cada tick; el Nodo descarta las que mueren o se pierden. */
    public void checkIn(SwarmWaspEntity wasp) {
        members.put(wasp.getUUID(), wasp);
    }

    public List<SwarmWaspEntity> members() {
        return new ArrayList<>(members.values());
    }

    public int count(WaspRole role) {
        return (int) members.values().stream().filter(w -> w.getRole() == role).count();
    }

    /** Índice de la avispa entre las de su papel (para repartir ángulos de ataque y patrulla). */
    public int slotOf(SwarmWaspEntity wasp) {
        int i = 0;
        for (SwarmWaspEntity w : members.values()) {
            if (w == wasp) {
                return i;
            }
            if (w.getRole() == wasp.getRole()) {
                i++;
            }
        }
        return i;
    }

    // ------------------------------------------------------------------ estado

    public State state() {
        return state;
    }

    @Nullable
    public LivingEntity target() {
        return target;
    }

    public float orbitAngle() {
        return orbitAngle;
    }

    /** Preferencia aprendida: >0 le hicieron daño sobre todo a distancia, <0 cuerpo a cuerpo. */
    public float rangedBias() {
        return rangedScore - meleeScore;
    }

    /** Una exploradora vio a un intruso: pasa a vigilancia (o directamente a ataque si está en la zona). */
    public void report(LivingEntity intruder) {
        if (state == State.ATTACK) {
            return;
        }
        target = intruder;
        state = intruder.distanceToSqr(node) < TERRITORY * TERRITORY ? State.ATTACK : State.ALERT;
        calmTicks = 0;
    }

    /** Alguien atacó a una avispa o al Nodo: ataque total y aprender cómo atacó. */
    public void provoked(LivingEntity attacker, double distance) {
        if (attacker instanceof Player p && (p.isCreative() || p.isSpectator())) {
            return;
        }
        target = attacker;
        state = State.ATTACK;
        calmTicks = 0;
        if (distance > 5.0D) {
            rangedScore = Math.min(1.0F, rangedScore + 0.06F);
            meleeScore = Math.max(0.0F, meleeScore - 0.02F);
        } else {
            meleeScore = Math.min(1.0F, meleeScore + 0.06F);
            rangedScore = Math.max(0.0F, rangedScore - 0.02F);
        }
    }

    /** Fuerza un objetivo (pruebas automáticas). */
    public void forceTarget(LivingEntity t) {
        target = t;
        state = State.ATTACK;
        calmTicks = 0;
    }

    /**
     * ¿Puede esta avispa lanzarse ya al ataque? Turnos: pocas a la vez (más si aprendió que le disparan
     * de lejos: entonces abruma en masa) y con una pausa entre lanzamientos.
     */
    public boolean requestStrike(SwarmWaspEntity wasp) {
        if (strikeCooldown > 0) {
            return false;
        }
        long striking = members.values().stream().filter(SwarmWaspEntity::isStriking).count();
        int maxStrikers = rangedBias() > 0.25F ? 4 : rangedBias() < -0.25F ? 1 : 2;
        if (striking >= maxStrikers) {
            return false;
        }
        strikeCooldown = rangedBias() > 0.25F ? 6 : 14;
        return true;
    }

    // ------------------------------------------------------------------ tick (lo llama el Nodo)

    public void tick() {
        orbitAngle += 0.025F;
        if (strikeCooldown > 0) {
            strikeCooldown--;
        }
        if (node.tickCount % 20 == 0) {
            members.values().removeIf(w -> w.isRemoved() || !w.isAlive() || w.distanceToSqr(node) > 96 * 96);
        }
        if (node.tickCount % 10 == 0) {
            scan();
        }
        if (target != null && (!target.isAlive() || target.isRemoved() || target.distanceToSqr(node) > GIVE_UP * GIVE_UP
                || (target instanceof Player p && (p.isCreative() || p.isSpectator())))) {
            target = null;
        }
        if (target == null) {
            state = State.PATROL;
        } else if (state == State.ALERT && ++calmTicks > 400) {
            target = null;                                            // se fue sin entrar: se calman
            state = State.PATROL;
        }
        spawning();
    }

    /** El Nodo también vigila: jugadores dentro de la zona = ataque; algo más lejos = vigilancia. */
    private void scan() {
        List<Player> players = node.level().getEntitiesOfClass(Player.class, new AABB(node.blockPosition()).inflate(ALERT_RADIUS),
                p -> !p.isCreative() && !p.isSpectator() && p.isAlive());
        players.sort(Comparator.comparingDouble(p -> p.distanceToSqr(node)));
        if (players.isEmpty()) {
            return;
        }
        Player p = players.get(0);
        double d2 = p.distanceToSqr(node);
        if (d2 < TERRITORY * TERRITORY) {
            target = p;
            state = State.ATTACK;
            calmTicks = 0;
        } else if (state == State.PATROL && d2 < ALERT_RADIUS * ALERT_RADIUS && node.hasLineOfSight(p)) {
            report(p);
        }
    }

    /** Repone avispas (más deprisa durante un ataque = refuerzos), con un presupuesto total limitado. */
    private void spawning() {
        if (spawnCooldown > 0) {
            spawnCooldown--;
            return;
        }
        if (members.size() >= MAX_MEMBERS || spawnBudget <= 0) {
            return;
        }
        WaspRole role = count(WaspRole.SCOUT) < 2 ? WaspRole.SCOUT
                : count(WaspRole.GUARD) < 2 ? WaspRole.GUARD : WaspRole.STINGER;
        node.spawnWasp(role);
        spawnBudget--;
        spawnCooldown = state == State.ATTACK ? 120 : 600;
    }

    /** El Nodo murió: las avispas quedan desorganizadas (sin tácticas ni curación ni refuerzos). */
    public void collapse() {
        members.values().forEach(SwarmWaspEntity::orphan);
        members.clear();
    }

    // ------------------------------------------------------------------ guardado

    public void save(CompoundTag tag) {
        tag.putFloat("Ranged", rangedScore);
        tag.putFloat("Melee", meleeScore);
        tag.putInt("Budget", spawnBudget);
    }

    public void load(CompoundTag tag) {
        rangedScore = tag.getFloat("Ranged");
        meleeScore = tag.getFloat("Melee");
        if (tag.contains("Budget")) {
            spawnBudget = tag.getInt("Budget");
        }
    }
}
