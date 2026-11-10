package com.bryanamaya.tecnocinetico.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cámara de Reacción: un haz de energía procesa el titanio y el uranio (y SOLO aquí se pueden procesar).
 * Cada material resuena a una frecuencia y una intensidad (1-10) que el jugador ajusta en la interfaz;
 * la barra de resonancia indica lo cerca que está.
 * - Ajuste exacto: avanza a velocidad normal y la cámara se estabiliza.
 * - A 1 punto de distancia: avanza a mitad de velocidad y pierde estabilidad poco a poco.
 * - Más lejos: no avanza y pierde estabilidad deprisa.
 * Si la estabilidad llega a 0 la carga se desintegra (se pierde una unidad de entrada).
 */
public class ReactionChamberBlockEntity extends ElectricMachineBlockEntity {

    public static final int MIN = 1, MAX = 10, STABILITY_MAX = 1000;
    public static final int EXTRA_FREQUENCY = 0, EXTRA_INTENSITY = 1, EXTRA_STABILITY = 2, EXTRA_RESONANCE = 3;
    /** Botones: frecuencia -/+, intensidad -/+. */
    public static final int BTN_FREQ_DOWN = 0, BTN_FREQ_UP = 1, BTN_INT_DOWN = 2, BTN_INT_UP = 3;

    private int frequency = 5, intensity = 5, stability = STABILITY_MAX, resonance;
    private boolean halfStep;

    public ReactionChamberBlockEntity(BlockPos pos, BlockState state) {
        super(MachineBlockEntities.REACTION_CHAMBER.get(), MachineKind.REACTION_CHAMBER, pos, state);
    }

    @Override
    protected boolean syncsToClient() {
        return true;                                                          // la carga se ve por la mirilla
    }

    public ItemStack load() {
        return input(0);
    }

    public boolean beamOn() {
        return progress > 0;
    }

    public int resonance() {
        return resonance;
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(worldPosition).inflate(2);
    }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return MachineRecipes.react(stack) != null;
    }

    public void configure(int frequency, int intensity) {
        this.frequency = Mth.clamp(frequency, MIN, MAX);
        this.intensity = Mth.clamp(intensity, MIN, MAX);
        setChanged();
    }

    @Override
    public void onButton(int id) {
        switch (id) {
            case BTN_FREQ_DOWN -> configure(frequency - 1, intensity);
            case BTN_FREQ_UP -> configure(frequency + 1, intensity);
            case BTN_INT_DOWN -> configure(frequency, intensity - 1);
            case BTN_INT_UP -> configure(frequency, intensity + 1);
            default -> {
            }
        }
    }

    @Override
    protected boolean work() {
        MachineRecipes.React r = MachineRecipes.react(input(0));
        if (r == null) {
            resonance = 0;
            stability = Math.min(STABILITY_MAX, stability + 5);
            return false;
        }
        int miss = Math.abs(frequency - r.frequency()) + Math.abs(intensity - r.intensity());
        resonance = Math.max(0, 100 - miss * 12);
        ItemStack main = new ItemStack(r.out().get(), r.count());
        ItemStack extra = new ItemStack(r.extra().get());
        if (!fits(0, main) || !fits(1, extra)) {
            return false;
        }
        maxProgress = r.ticks();
        usage = r.perTick();
        if (energy.getEnergyStored() < r.perTick()) {
            return true;                                                      // sin energía el haz está apagado: nada cambia
        }
        if (miss == 0) {
            stability = Math.min(STABILITY_MAX, stability + 4);
        } else if (miss == 1) {
            stability -= 2;
            halfStep = !halfStep;
            if (halfStep) {                                                  // mitad de velocidad: gasta, no avanza
                energy.consume(r.perTick());
                checkCollapse();
                return true;
            }
        } else {
            stability -= 3;                                                   // ~17 s para ajustarla antes de perder la carga
            energy.consume(r.perTick() / 2);                                  // el haz sigue encendido
            checkCollapse();
            return true;                                                      // se detiene sin perder el progreso
        }
        if (checkCollapse()) {
            return true;
        }
        if (advance(r.ticks(), r.perTick())) {
            items.extractItem(0, 1, false);
            output(0, main);
            if (level.random.nextFloat() < r.chance()) {
                output(1, extra);
            }
            level.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.5F, 1.6F);
        }
        return true;
    }

    /** La carga se desintegra si la cámara pierde toda la estabilidad. */
    private boolean checkCollapse() {
        if (stability > 0) {
            return false;
        }
        items.extractItem(0, 1, false);
        progress = 0;
        stability = STABILITY_MAX;
        level.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 0.6F, 1.4F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5,
                    30, 0.4, 0.3, 0.4, 0.2);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                    8, 0.3, 0.2, 0.3, 0.02);
        }
        setChanged();
        return true;
    }

    @Override
    protected int getExtra(int i) {
        return switch (i) {
            case EXTRA_FREQUENCY -> frequency;
            case EXTRA_INTENSITY -> intensity;
            case EXTRA_STABILITY -> stability;
            default -> resonance;
        };
    }

    @Override
    protected void setExtra(int i, int v) {
        switch (i) {
            case EXTRA_FREQUENCY -> frequency = v;
            case EXTRA_INTENSITY -> intensity = v;
            case EXTRA_STABILITY -> stability = v;
            default -> resonance = v;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("frequency", frequency);
        tag.putInt("intensity", intensity);
        tag.putInt("stability", stability);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        frequency = tag.contains("frequency") ? tag.getInt("frequency") : 5;
        intensity = tag.contains("intensity") ? tag.getInt("intensity") : 5;
        stability = tag.contains("stability") ? tag.getInt("stability") : STABILITY_MAX;
    }

    @Override
    protected void effects(net.minecraft.server.level.ServerLevel level, long time) {
        loop(com.bryanamaya.tecnocinetico.sound.ModSounds.MACHINE_REACTION.get(), time, 50);
        if (time % 2 == 0) {                                                  // chispas en el emisor del techo
            var p = local(1.0, 2.0, 1.0);
            level.sendParticles(com.bryanamaya.tecnocinetico.particle.ModParticles.KINETIC_SPARK.get(), p.x, p.y, p.z, 1, 0.08, 0.02, 0.08, 0.02);
        }
    }
}
