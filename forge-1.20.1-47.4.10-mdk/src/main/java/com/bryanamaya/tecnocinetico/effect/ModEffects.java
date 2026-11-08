package com.bryanamaya.tecnocinetico.effect;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Tecnocinetico.MOD_ID);

    /** Lastre de Inercia: arrastra al suelo y apaga el jetpack (lo aplica el Anclador). */
    public static final RegistryObject<MobEffect> INERTIA_ANCHOR = EFFECTS.register("inertia_anchor", InertiaAnchorEffect::new);

    /** Radiación: la da el uranio (llevarlo encima o estar cerca). Ver RadiationEvents. */
    public static final RegistryObject<MobEffect> RADIATION = EFFECTS.register("radiation", RadiationEffect::new);

    private ModEffects() {}

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }
}
