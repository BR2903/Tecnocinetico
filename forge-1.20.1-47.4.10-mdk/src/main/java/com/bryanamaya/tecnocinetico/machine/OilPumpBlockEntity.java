package com.bryanamaya.tecnocinetico.machine;

import com.bryanamaya.tecnocinetico.fluid.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

/**
 * Bomba de Extracción: busca bolsas de petróleo bajo ella (cuadrado de {@link #RADIUS} bloques, hasta el fondo del
 * mundo; una capa por tick para no cargar el servidor) y saca un bloque de petróleo (1000 mB) cada {@link #TICKS} ticks
 * gastando FE. El hueco se rellena de piedra (el yacimiento se agota). El petróleo va al tanque de salida; se saca con
 * tuberías, cubos o envasándolo: un recipiente vacío (cubo, Celda Universal) en la entrada sale lleno por la salida.
 */
public class OilPumpBlockEntity extends TankMachineBlockEntity {

    public static final int RADIUS = 6, TICKS = 100, PER_TICK = 40;
    /** Capa que se está explorando (baja de la bomba al fondo y vuelve a empezar). */
    private int scanY = Integer.MIN_VALUE;
    @Nullable
    private BlockPos found;

    public OilPumpBlockEntity(BlockPos pos, BlockState state) {
        super(MachineBlockEntities.OIL_PUMP.get(), MachineKind.OIL_PUMP, pos, state, fs -> false);
    }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return FluidUtil.getFluidHandler(stack.copyWithCount(1)).isPresent();
    }

    @Override
    protected boolean work() {
        fillContainer();
        if (found == null || !isOil(found)) {
            found = null;
            scanLayer();
            return false;
        }
        FluidStack oil = new FluidStack(ModFluids.CRUDE_OIL.get(), 1000);
        if (output.fill(oil, IFluidHandler.FluidAction.SIMULATE) < 1000) {
            return false;
        }
        if (advance(TICKS, PER_TICK)) {
            level.setBlock(found, Blocks.STONE.defaultBlockState(), 3);
            output.fill(oil, IFluidHandler.FluidAction.EXECUTE);
            found = null;
            level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.6F, 0.6F);
        }
        return true;
    }

    private boolean isOil(BlockPos p) {
        var fs = level.getFluidState(p);
        return fs.isSource() && fs.getType() == ModFluids.CRUDE_OIL.get();
    }

    /** Explora una capa (13x13) por llamada, de la bomba hacia abajo; al llegar al fondo vuelve a empezar. */
    private void scanLayer() {
        if (scanY == Integer.MIN_VALUE || scanY < level.getMinBuildHeight()) {
            scanY = worldPosition.getY() - 1;
        }
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        for (int dx = -RADIUS; dx <= RADIUS && found == null; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                q.set(worldPosition.getX() + dx, scanY, worldPosition.getZ() + dz);
                if (isOil(q)) {
                    found = q.immutable();
                    break;
                }
            }
        }
        if (found == null) {
            scanY--;
        }
    }

    /** Recipiente vacío en la entrada -> lleno de petróleo en la salida. */
    private void fillContainer() {
        ItemStack empty = input(0);
        if (empty.isEmpty() || output.getFluidAmount() <= 0) {
            return;
        }
        var result = FluidUtil.tryFillContainer(empty.copyWithCount(1), output, Integer.MAX_VALUE, null, false);
        if (!result.isSuccess() || !fits(0, result.getResult())) {
            return;
        }
        result = FluidUtil.tryFillContainer(empty.copyWithCount(1), output, Integer.MAX_VALUE, null, true);
        items.extractItem(0, 1, false);
        output(0, result.getResult());
    }

    /** Hay petróleo localizado bajo la bomba (para las pruebas y la interfaz). */
    public boolean hasDeposit() {
        return found != null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("scan_y", scanY);
        if (found != null) {
            tag.put("found", NbtUtils.writeBlockPos(found));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        scanY = tag.contains("scan_y") ? tag.getInt("scan_y") : Integer.MIN_VALUE;
        found = tag.contains("found") ? NbtUtils.readBlockPos(tag.getCompound("found")) : null;
    }

    @Override
    protected void effects(ServerLevel level, long time) {
        loop(com.bryanamaya.tecnocinetico.sound.ModSounds.MACHINE_REFINER.get(), time, 44);
        if (time % 5 == 0) {
            var p = worldPosition.getCenter();
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ModFluids.CRUDE_OIL.block.get().defaultBlockState()),
                    p.x, p.y - 0.3, p.z, 2, 0.25, 0.05, 0.25, 0.0);
        }
    }
}
