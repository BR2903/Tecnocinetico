package com.bryanamaya.tecnocinetico.skill;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Minería Cinética: se entrena picando con el Martillo Cinético y lo hace más rápido. */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MiningSkillEvents {

    private static final int XP_PER_BLOCK = 1;
    private static final int XP_PER_ORE = 5;

    private MiningSkillEvents() {}

    private static boolean usingHammerOn(Player player, BlockState state) {
        ItemStack tool = player.getMainHandItem();
        return tool.is(ModItems.KINETIC_HAMMER.get()) && tool.isCorrectToolForDrops(state);
    }

    /** XP al romper un bloque con el martillo (los minerales dan más). */
    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && !player.isCreative()
                && usingHammerOn(player, event.getState())) {
            int xp = event.getState().is(Tags.Blocks.ORES) ? XP_PER_ORE : XP_PER_BLOCK;
            PlayerSkills.addXp(player, Skill.MINING, xp);
        }
    }

    /** Velocidad extra según el nivel (funciona en cliente y servidor: ambos conocen el nivel). */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (usingHammerOn(player, event.getState())) {
            float bonus = Skill.miningSpeedBonus(PlayerSkills.getLevel(player, Skill.MINING));
            event.setNewSpeed(event.getNewSpeed() * (1.0F + bonus));
        }
    }
}
