package com.bryanamaya.tecnocinetico.client;

import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.skill.ClientSkillData;
import com.bryanamaya.tecnocinetico.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Pantalla de habilidades (tecla K): nivel, experiencia y bonificaciones de cada habilidad. */
public class SkillsScreen extends Screen {

    private static final int WIDTH = 280;
    private static final int HEADER = 26;
    private static final int ROW_TOP = 24;   // nombre + barra de XP
    private static final int LINE_H = 10;
    private static final int ROW_GAP = 6;

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

    /** Texto de una habilidad ya partido en líneas que caben en el panel. */
    private record Row(Skill skill, int xp, int level, List<FormattedCharSequence> effects,
                       List<FormattedCharSequence> train) {
        int height() {
            return ROW_TOP + (effects.size() + train.size()) * LINE_H + ROW_GAP;
        }
    }

    private List<Row> layout(int textWidth) {
        List<Row> rows = new ArrayList<>();
        for (Skill skill : Skill.values()) {
            int xp = ClientSkillData.getXp(skill);
            int level = skill.levelForXp(xp);
            List<FormattedCharSequence> effects = new ArrayList<>();
            for (Component effect : skill.effects(level)) {
                effects.addAll(font.split(Component.literal("• ").append(effect), textWidth));
            }
            rows.add(new Row(skill, xp, level, effects, font.split(skill.howToTrain(), textWidth)));
        }
        return rows;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        int textX = 32;                       // margen izquierdo del texto (deja sitio al icono)
        int textWidth = WIDTH - textX - 10;   // ancho disponible: el texto largo pasa a la línea siguiente
        List<Row> rows = layout(textWidth);

        int height = HEADER + rows.stream().mapToInt(Row::height).sum() + 4;
        int left = (this.width - WIDTH) / 2;
        int top = Math.max(4, (this.height - height) / 2);

        // Panel con borde cian
        g.fill(left, top, left + WIDTH, top + height, PANEL);
        g.renderOutline(left, top, WIDTH, height, BORDER);
        g.drawCenteredString(font, title.copy().withStyle(ChatFormatting.BOLD), left + WIDTH / 2, top + 9, 0xFFFFFF);
        g.fill(left + 8, top + HEADER - 6, left + WIDTH - 8, top + HEADER - 5, 0x6046D7EB);

        int y = top + HEADER;
        for (Row row : rows) {
            Skill skill = row.skill();
            boolean max = row.level() >= skill.maxLevel();
            int x = left + textX;

            // Icono, nombre y nivel
            g.renderItem(iconFor(skill), left + 10, y + 2);
            g.drawString(font, skill.displayName().copy().withStyle(ChatFormatting.BOLD), x, y + 2, 0xFFFFFF);
            Component lvl = Component.translatable("gui.tecnocinetico.skills.level", row.level(), skill.maxLevel());
            g.drawString(font, lvl, left + WIDTH - 10 - font.width(lvl), y + 2, max ? 0xF0C040 : 0x9AE6F0);

            // Barra de experiencia (el texto de XP va a su derecha, en la misma línea)
            Component xpText = max ? Component.translatable("gui.tecnocinetico.skills.max")
                    : Component.literal((row.xp() - Skill.totalXpForLevel(row.level())) + " / "
                    + (Skill.totalXpForLevel(row.level() + 1) - Skill.totalXpForLevel(row.level())) + " XP");
            int xpW = font.width(xpText);
            int barY = y + 14, barW = textWidth - xpW - 6;
            g.fill(x, barY, x + barW, barY + 5, BAR_BG);
            if (max) {
                g.fill(x, barY, x + barW, barY + 5, BAR_MAX);
            } else {
                int from = Skill.totalXpForLevel(row.level()), to = Skill.totalXpForLevel(row.level() + 1);
                g.fill(x, barY, x + (int) ((long) (row.xp() - from) * barW / Math.max(1, to - from)), barY + 5, BAR_FILL);
            }
            g.drawString(font, xpText, left + WIDTH - 10 - xpW, barY - 1, max ? 0xF0C040 : 0x707A86, false);

            // Bonificaciones actuales y cómo entrenarla (ya partidas en líneas)
            int ly = y + ROW_TOP;
            for (FormattedCharSequence line : row.effects()) {
                g.drawString(font, line, x, ly, 0xC8D0DA, false);
                ly += LINE_H;
            }
            for (FormattedCharSequence line : row.train()) {
                g.drawString(font, line, x, ly, 0x6E7884, false);
                ly += LINE_H;
            }
            y += row.height();
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
