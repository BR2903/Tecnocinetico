package com.bryanamaya.tecnocinetico.machine;

import com.bryanamaya.tecnocinetico.fluid.ModFluids;
import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reactor de Fisión: quema Barras de Combustible de Uranio y genera electricidad (hasta 600 FE/t), que empuja
 * solo a lo que tenga al lado. La potencia se ajusta con las barras de control (0-100 %).
 * Calor: sube con la potencia (4 °/t al 100 %); lo quita el refrigerante del tanque (hasta 6 °/t, 1 mB cada 2 °)
 * o el agua (hasta 3 °/t, 1 mB por grado); sin nada solo se disipa 0,4 °/t. A 900 ° suena la alarma; a 1000 °
 * el núcleo se funde y el reactor EXPLOTA. Mientras quema, irradia a su alrededor (ver RadiationEvents).
 * Cada barra dura 20 min a plena potencia (menos potencia = dura más) y sale agotada (radiactiva, se reprocesa en
 * la Cámara de Reacción). Si la batería interna está llena, el reactor se pausa (no quema ni se calienta).
 */
public class FissionReactorBlockEntity extends ElectricMachineBlockEntity implements FluidMachine {

    public static final int ROD_TICKS = 24_000, MAX_FE_PER_TICK = 600, PUSH_PER_TICK = 2_000, TANK = 8_000;
    public static final float HEAT_PER_TICK = 4.0F, PASSIVE_COOLING = 0.4F, COOLANT_COOLING = 6.0F, WATER_COOLING = 3.0F;
    public static final int ALARM = 900, MELTDOWN = 1000, AMBIENT = 20;
    public static final int EXTRA_HEAT = 0, EXTRA_THROTTLE = 1, EXTRA_FLUID = 2, EXTRA_AMOUNT = 3, EXTRA_OUTPUT = 4, EXTRA_FUEL = 5;
    public static final int BTN_DOWN = 0, BTN_UP = 1;

    private float heat = AMBIENT, burnFraction;
    private int throttle = 50, burnLeft, output;

    private final FluidTank coolant = new FluidTank(TANK,
            fs -> fs.getFluid().isSame(Fluids.WATER) || fs.getFluid().isSame(ModFluids.COOLANT.get())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private LazyOptional<IFluidHandler> lazyFluid = LazyOptional.empty();

    /** Lo que ven los cables: solo se puede SACAR energía del reactor. */
    private final IEnergyStorage outputView = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int e = Math.min(maxExtract, energy.getEnergyStored());
            if (!simulate) {
                energy.drain(e);
                setChanged();
            }
            return e;
        }

        @Override
        public int getEnergyStored() {
            return energy.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energy.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    };
    private LazyOptional<IEnergyStorage> lazyOut = LazyOptional.empty();

    public FissionReactorBlockEntity(BlockPos pos, BlockState state) {
        super(MachineBlockEntities.FISSION_REACTOR.get(), MachineKind.FISSION_REACTOR, pos, state);
    }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return stack.is(ModItems.URANIUM_FUEL_ROD.get());
    }

    public void setThrottle(int t) {
        throttle = Mth.clamp(t, 0, 100);
        setChanged();
    }

    public float heat() {
        return heat;
    }

    public FluidTank coolantTank() {
        return coolant;
    }

    @Override
    public void onButton(int id) {
        if (id == BTN_DOWN) {
            setThrottle(throttle - 10);
        } else if (id == BTN_UP) {
            setThrottle(throttle + 10);
        }
    }

    @Override
    protected boolean syncsToClient() {
        return true;
    }

    /** Para dibujar: barras esperando en el hueco de entrada. */
    public int waitingRods() {
        return input(0).getCount();
    }

    public boolean burning() {
        return burnLeft > 0;
    }

    public int throttle() {
        return throttle;
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(worldPosition).inflate(1.5, 0, 1.5).expandTowards(0, 4, 0);
    }

    @Override
    protected boolean isLit() {
        return output > 0;
    }

    @Override
    protected boolean work() {
        output = 0;
        // 1. combustible: empieza una barra nueva si hace falta (y hay sitio para la agotada)
        if (burnLeft <= 0 && throttle > 0 && input(0).is(ModItems.URANIUM_FUEL_ROD.get())
                && fits(0, new ItemStack(ModItems.DEPLETED_FUEL_ROD.get()))) {
            items.extractItem(0, 1, false);
            burnLeft = ROD_TICKS;
        }
        // 2. fisión: genera y calienta (se pausa con la batería llena)
        boolean full = energy.getEnergyStored() >= energy.getMaxEnergyStored();
        if (burnLeft > 0 && throttle > 0 && !full) {
            output = energy.produce(MAX_FE_PER_TICK * throttle / 100);
            heat += HEAT_PER_TICK * throttle / 100.0F;
            burnFraction += throttle / 100.0F;
            while (burnFraction >= 1.0F) {
                burnFraction -= 1.0F;
                burnLeft--;
            }
            if (burnLeft <= 0) {
                output(0, new ItemStack(ModItems.DEPLETED_FUEL_ROD.get()));
            }
        }
        // 3. refrigeración
        cool();
        // 4. reparto de la energía a los vecinos
        push();
        maxProgress = ROD_TICKS;
        progress = burnLeft > 0 ? ROD_TICKS - burnLeft : 0;
        usage = output;
        // 5. peligro
        if (heat >= ALARM && level.getGameTime() % 20 == 0) {
            level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 1.5F, 0.5F);
            if (level instanceof ServerLevel s) {
                s.sendParticles(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5,
                        4, 0.3, 0.1, 0.3, 0.02);
            }
        }
        if (heat >= MELTDOWN) {
            meltdown();
            return false;
        }
        setChanged();
        return burnLeft > 0;
    }

    private void cool() {
        float c = PASSIVE_COOLING;
        float excess = heat - AMBIENT;
        FluidStack f = coolant.getFluid();
        if (excess > 0 && !f.isEmpty()) {
            boolean good = f.getFluid().isSame(ModFluids.COOLANT.get());
            float max = Math.min(excess, good ? COOLANT_COOLING : WATER_COOLING);
            int mb = Math.max(1, Math.round(good ? max / 2.0F : max));
            int got = coolant.drain(mb, IFluidHandler.FluidAction.EXECUTE).getAmount();
            c += good ? got * 2.0F : got;
        }
        heat = Math.max(AMBIENT, heat - c);
    }

    private void push() {
        for (Direction d : Direction.values()) {
            if (energy.getEnergyStored() <= 0) {
                return;
            }
            BlockEntity n = level.getBlockEntity(worldPosition.relative(d));
            if (n == null || n instanceof FissionReactorBlockEntity) {
                continue;
            }
            n.getCapability(ForgeCapabilities.ENERGY, d.getOpposite()).ifPresent(target -> {
                if (target.canReceive()) {
                    int sent = target.receiveEnergy(Math.min(PUSH_PER_TICK, energy.getEnergyStored()), false);
                    energy.drain(sent);
                }
            });
        }
    }

    /** Fusión del núcleo: explosión que rompe bloques. */
    private void meltdown() {
        Level lvl = level;
        BlockPos p = worldPosition;
        com.bryanamaya.tecnocinetico.structure.MultiblockMachineBlock.destroyWithoutKit(lvl, p);   // se desmonta sin dar el kit
        lvl.explode(null, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6.0F, true, Level.ExplosionInteraction.BLOCK);
        lvl.setBlock(p, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);                     // restos del núcleo
    }

    @Override
    public boolean useContainer(Player player, InteractionHand hand) {
        return FluidUtil.getFluidHandler(player.getItemInHand(hand)).isPresent() && FluidUtil.interactWithFluidHandler(player, hand, coolant);
    }

    // ---------------------------------------------------------------- interfaz
    @Override
    protected int getExtra(int i) {
        return switch (i) {
            case EXTRA_HEAT -> Math.round(heat);
            case EXTRA_THROTTLE -> throttle;
            case EXTRA_FLUID -> coolant.isEmpty() ? 0 : BuiltInRegistries.FLUID.getId(coolant.getFluid().getFluid());
            case EXTRA_AMOUNT -> coolant.getFluidAmount();
            case EXTRA_OUTPUT -> output;
            default -> burnLeft * 100 / ROD_TICKS;
        };
    }

    // ---------------------------------------------------------------- guardado y capacidades
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("heat", heat);
        tag.putInt("throttle", throttle);
        tag.putInt("burn_left", burnLeft);
        tag.putFloat("burn_fraction", burnFraction);
        tag.put("coolant", coolant.writeToNBT(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        heat = tag.contains("heat") ? tag.getFloat("heat") : AMBIENT;
        throttle = tag.contains("throttle") ? tag.getInt("throttle") : 50;
        burnLeft = tag.getInt("burn_left");
        burnFraction = tag.getFloat("burn_fraction");
        coolant.readFromNBT(tag.getCompound("coolant"));
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return lazyFluid.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return lazyOut.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyFluid = LazyOptional.of(() -> coolant);
        lazyOut = LazyOptional.of(() -> outputView);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyFluid.invalidate();
        lazyOut.invalidate();
    }
}
