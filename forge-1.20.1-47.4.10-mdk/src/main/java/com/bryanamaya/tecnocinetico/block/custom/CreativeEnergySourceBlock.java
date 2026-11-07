package com.bryanamaya.tecnocinetico.block.custom;

import com.bryanamaya.tecnocinetico.block.entity.CreativeEnergySourceBlockEntity;
import com.bryanamaya.tecnocinetico.block.entity.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Fuente de Energía Creativa — SOLO PARA PRUEBAS.
 * Sin receta, no suelta nada al romperse e irrompible en supervivencia (como la roca madre).
 * Solo se obtiene desde la pestaña creativa.
 */
public class CreativeEnergySourceBlock extends Block implements EntityBlock {

    public CreativeEnergySourceBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(-1.0F, 3_600_000.0F)   // irrompible salvo en creativo
                .noLootTable()                    // no suelta nada
                .sound(SoundType.NETHERITE_BLOCK)
                .lightLevel(s -> 7));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeEnergySourceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.CREATIVE_ENERGY_SOURCE_BE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> CreativeEnergySourceBlockEntity.tick(lvl, pos, st, (CreativeEnergySourceBlockEntity) be);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tecnocinetico.creative_energy_source").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.tecnocinetico.creative_only").withStyle(ChatFormatting.DARK_GRAY));
    }
}
