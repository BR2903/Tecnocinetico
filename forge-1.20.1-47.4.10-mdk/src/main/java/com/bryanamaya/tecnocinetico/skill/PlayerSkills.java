package com.bryanamaya.tecnocinetico.skill;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Habilidades guardadas en los datos persistentes del jugador.
 * Se usa la etiqueta "PlayerPersisted" de Forge, que se conserva al morir.
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerSkills {

    private static final String ROOT = "tecnocinetico_skills";

    private PlayerSkills() {}

    private static CompoundTag skillsTag(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!player.getPersistentData().contains(Player.PERSISTED_NBT_TAG)) {
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        }
        CompoundTag skills = persisted.getCompound(ROOT);
        if (!persisted.contains(ROOT)) {
            persisted.put(ROOT, skills);
        }
        return skills;
    }

    /** XP de una habilidad. Funciona en ambos lados (en el cliente usa la copia sincronizada). */
    public static int getXp(Player player, Skill skill) {
        if (player.level().isClientSide()) {
            return ClientSkillData.getXp(skill);
        }
        return skillsTag(player).getInt(skill.id());
    }

    public static int getLevel(Player player, Skill skill) {
        return skill.levelForXp(getXp(player, skill));
    }

    /** Suma XP en el servidor; avisa al jugador si sube de nivel y sincroniza con su cliente. */
    public static void addXp(ServerPlayer player, Skill skill, int amount) {
        if (amount <= 0) {
            return;
        }
        CompoundTag skills = skillsTag(player);
        int before = skill.levelForXp(skills.getInt(skill.id()));
        int cap = Skill.totalXpForLevel(skill.maxLevel());
        int xp = Math.min(cap, skills.getInt(skill.id()) + amount);
        skills.putInt(skill.id(), xp);
        int after = skill.levelForXp(xp);

        if (after > before) {
            player.displayClientMessage(Component.translatable("message.tecnocinetico.skill_level_up",
                    skill.displayName(), after).withStyle(ChatFormatting.AQUA), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
        }
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ModNetwork.SyncSkillsPacket(skillsTag(player).copy()));
    }

    // ------------------------------------------------------------------ sincronizar al entrar / reaparecer / cambiar de dimensión

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            sync(sp);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            sync(sp);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            sync(sp);
        }
    }
}
