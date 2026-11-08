package com.bryanamaya.tecnocinetico.entity.swarm;

/**
 * Papel de cada Avispa del Enjambre dentro de la colmena.
 * Vida, daño y velocidad (bloques/tick) los aplica {@link SwarmWaspEntity#setRole}.
 */
public enum WaspRole {
    /** Exploradora: rápida y frágil; patrulla lejos, detecta intrusos y da la alarma. */
    SCOUT("scout", 8.0D, 2.0D, 0.62D, 0.85F),
    /** Aguijón: la atacante; rodea al objetivo y ataca por turnos (con veneno). */
    STINGER("stinger", 12.0D, 3.0D, 0.52D, 1.0F),
    /** Escolta: blindada y lenta; protege al Nodo y solo se lanza si el intruso se acerca a él. */
    GUARD("guard", 20.0D, 4.0D, 0.42D, 1.2F);

    public final String id;
    public final double health, damage, speed;
    /** Escala visual del modelo. */
    public final float scale;

    WaspRole(String id, double health, double damage, double speed, float scale) {
        this.id = id;
        this.health = health;
        this.damage = damage;
        this.speed = speed;
        this.scale = scale;
    }

    public static WaspRole byId(int i) {
        return values()[Math.floorMod(i, values().length)];
    }
}
