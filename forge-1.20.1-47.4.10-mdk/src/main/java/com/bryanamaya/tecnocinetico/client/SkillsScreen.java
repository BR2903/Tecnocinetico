package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.skill.ClientSkillData;
import com.bryanamaya.tecnocinetico.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Pantalla de habilidades (tecla K): nivel, experiencia y bonificaciones de cada habilidad. */
public class SkillsScreen extends Screen {

    private static final int WIDTH = 236;
    private static final int ROW_H = 54;
    private static final int HEADER = 26;

    private static final int PANEL = 0xE8141820;
    private static final int BORDER = 0xFF46D7EB;
    private static final int BAR_BG = 0xFF2A3038;
    private static final int BAR_FILL = 0xFF46D7EB;
    private static final int BAR_MAX = 0xFFF0C040;

    public SkillsScreen() {
        super(Component.translatable("gui.tecnocinetico.skills.title"));
    }

    private static ItemStack iconFor(Skill skill) {
        return switch (skill) {
            case WEAPON_HANDLING -> new ItemStack(ModItems.KINETIC_PISTOL.get());
            case ENGINEERING -> new ItemStack(ModBlocks.KINETIC_ASSEMBLY_PRESS.get());
            case MINING -> new ItemStack(ModItems.KINETIC_HAMMER.get());
        };
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        int height = HEADER + Skill.values().length * ROW_H + 6;
        int left = (this.width - WIDTH) / 2;
        int top = (this.height - height) / 2;

        // Panel con borde cian
        g.fill(left, top, left + WIDTH, top + height, PANEL);
        g.renderOutline(left, top, WIDTH, height, BORDER);
        g.drawCenteredString(font, title.copy().withStyle(ChatFormatting.BOLD), left + WIDTH / 2, top + 9, 0xFFFFFF);
        g.fill(left + 8, top + HEADER - 6, left + WIDTH - 8, top + HEADER - 5, 0x6046D7EB);

        int y = top + HEADER;
        for (Skill skill : Skill.values()) {
            int xp = ClientSkillData.getXp(skill);
            int level = skill.levelForXp(xp);
            boolean max = level >= skill.maxLevel();

            g.renderItem(iconFor(skill), left + 10, y + 2);
            g.drawString(font, skill.displayName().copy().withStyle(ChatFormatting.BOLD), left + 32, y + 2, 0xFFFFFF);
            Component lvl = Component.translatable("gui.tecnocinetico.skills.level", level, skill.maxLevel());
            g.drawString(font, lvl, left + WIDTH - 10 - font.width(lvl), y + 2, max ? 0xF0C040 : 0x9AE6F0);

            // Barra de experiencia hacia el siguiente nivel
            int barX = left + 32, barY = y + 13, barW = WIDTH - 42;
            g.fill(barX, barY, barX + barW, barY + 5, BAR_BG);
            if (max) {
                g.fill(barX, barY, barX + barW, barY + 5, BAR_MAX);
            } else {
                int from = Skill.totalXpForLevel(level), to = Skill.totalXpForLevel(level + 1);
                int fill = (int) ((long) (xp - from) * barW / Math.max(1, to - from));
                g.fill(barX, barY, barX + fill, barY + 5, BAR_FILL);
                Component xpText = Component.literal((xp - from) + " / " + (to - from) + " XP");
                g.drawString(font, xpText, left + WIDTH - 10 - font.width(xpText), barY + 7, 0x707A86, false);
            }

            // Bonificaciones actuales y cómo entrenarla
            int ly = barY + 7;
            for (Component effect : skill.effects(level)) {
                g.drawString(font, Component.literal("• ").append(effect), left + 32, ly, 0xC8D0DA, false);
                ly += 10;
            }
            g.drawString(font, skill.howToTrain(), left + 32, ly, 0x6E7884, false);
            y += ROW_H;
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeyBindings.SKILLS.matches(keyCode, scanCode)) {
            onClose(); // la misma tecla cierra la pantalla
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
