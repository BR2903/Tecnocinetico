package com.bryanamaya.tecnocinetico.skill;

import net.minecraft.nbt.CompoundTag;

/**
 * Copia en el cliente de las habilidades del jugador local (la envía el servidor).
 * Solo guarda datos: no usa clases del cliente, así que es seguro en un servidor dedicado.
 */
public final class ClientSkillData {

    private static CompoundTag data = new CompoundTag();

    private ClientSkillData() {}

    public static void set(CompoundTag tag) {
        data = tag.copy();
    }

    public static int getXp(Skill skill) {
        return data.getInt(skill.id());
    }
}
