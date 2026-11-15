package com.bryanamaya.tecnocinetico.item;

import com.bryanamaya.tecnocinetico.entity.MobVariants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/**
 * Huevo de un mob vanilla <b>modificado</b> (piglins y demás del Nether, endermen y shulkers del End): lo hace aparecer
 * ya marcado con {@link MobVariants}, así conserva sus mejoras en cualquier dimensión. No usa SpawnEggItem para no
 * quitarle al mob vanilla su propio huevo (al elegir bloque con la rueda, dispensadores…).
 */
public class VariantEggItem extends Item {

    private final Supplier<? extends EntityType<? extends Mob>> type;
    private final String kind;
    public final int background, highlight;

    public VariantEggItem(Supplier<? extends EntityType<? extends Mob>> type, String kind, int background, int highlight, Properties props) {
        super(props);
        this.type = type;
        this.kind = kind;
        this.background = background;
        this.highlight = highlight;
    }

    /** Color de las dos capas del modelo {@code template_spawn_egg}. */
    public int color(int layer) {
        return layer == 0 ? background : highlight;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos clicked = ctx.getClickedPos();
        Direction face = ctx.getClickedFace();
        BlockPos at = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(face);
        ItemStack stack = ctx.getItemInHand();
        Mob mob = type.get().spawn(server, null, m -> {
            MobVariants.mark(m, kind);                                       // antes de entrar al mundo: se mejora al unirse
            if (m instanceof AbstractPiglin piglin) {
                piglin.setImmuneToZombification(true);                        // fuera del Nether no se zombifica
            }
        }, at, MobSpawnType.SPAWN_EGG, true, !clicked.equals(at) && face == Direction.UP);
        if (mob != null) {
            if (stack.hasCustomHoverName()) {
                mob.setCustomName(stack.getHoverName());
            }
            if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.gameEvent(ctx.getPlayer(), GameEvent.ENTITY_PLACE, clicked);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tecnocinetico.variant_egg").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
