package com.bryanamaya.tecnocinetico.structure;

import com.bryanamaya.tecnocinetico.machine.ElectricMachineBlock;
import com.bryanamaya.tecnocinetico.machine.ElectricMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Núcleo de una máquina grande: al quitarlo (o al romper cualquier pieza) desmonta toda la estructura y suelta el
 * kit con el contenido de la máquina. Si la máquina se destruye (fusión del reactor) no suelta el kit.
 */
public class MultiblockMachineBlock extends ElectricMachineBlock {

    private static final Set<String> DISMANTLING = new HashSet<>();

    private final Supplier<MultiblockTemplate> template;
    private final Supplier<Item> kit;

    public MultiblockMachineBlock(Supplier<? extends BlockEntityType<? extends ElectricMachineBlockEntity>> type,
                                  BiFunction<BlockPos, BlockState, ? extends ElectricMachineBlockEntity> factory,
                                  Supplier<MultiblockTemplate> template, Supplier<Item> kit) {
        super(type, factory, false);
        this.template = template;
        this.kit = kit;
    }

    public MultiblockTemplate template() {
        return template.get();
    }

    private static String key(Level level, BlockPos core) {
        return level.dimension().location() + "|" + core.asLong();
    }

    public static boolean isDismantling(Level level, BlockPos core) {
        return DISMANTLING.contains(key(level, core));
    }

    /** Para la fusión del reactor: la próxima vez que se quite este núcleo no suelta el kit. */
    private static final Set<String> NO_KIT = new HashSet<>();

    public static void destroyWithoutKit(Level level, BlockPos core) {
        NO_KIT.add(key(level, core));
        level.setBlock(core, Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        return new ItemStack(kit.get());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (state.is(newState.getBlock()) || level.isClientSide) {
            super.onRemove(state, level, pos, newState, moving);
            return;
        }
        String k = key(level, pos);
        boolean dropKit = !NO_KIT.remove(k);
        DISMANTLING.add(k);
        try {
            super.onRemove(state, level, pos, newState, moving);                    // suelta el inventario de la máquina
            MultiblockTemplate t = template.get();
            BlockPos origin = t.originFromCore(pos, state.getValue(FACING).getOpposite());
            for (BlockPos p : t.occupied(origin, state.getValue(FACING).getOpposite())) {
                if (!p.equals(pos) && level.getBlockState(p).getBlock() instanceof StructurePartBlock) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                }
            }
            if (dropKit) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(kit.get()));
            }
        } finally {
            DISMANTLING.remove(k);
        }
    }
}
