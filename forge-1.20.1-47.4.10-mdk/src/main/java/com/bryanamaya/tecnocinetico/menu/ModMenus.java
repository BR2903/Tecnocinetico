package com.bryanamaya.tecnocinetico.menu;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Tecnocinetico.MOD_ID);

    public static final RegistryObject<MenuType<WeaponBenchMenu>> WEAPON_BENCH =
            MENUS.register("weapon_bench", () -> new MenuType<>(WeaponBenchMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final RegistryObject<MenuType<AutoKineticPressMenu>> AUTO_KINETIC_PRESS =
            MENUS.register("auto_kinetic_press", () -> IForgeMenuType.create(AutoKineticPressMenu::new));

    public static final RegistryObject<MenuType<WeaponAssemblerMenu>> WEAPON_ASSEMBLER =
            MENUS.register("weapon_assembler", () -> IForgeMenuType.create(WeaponAssemblerMenu::new));

    public static final RegistryObject<MenuType<TacticalMenu>> TACTICAL_GEAR =
            MENUS.register("tactical_gear", () -> new MenuType<>(TacticalMenu::new, FeatureFlags.DEFAULT_FLAGS));

    /** Una interfaz por máquina eléctrica (todas usan MachineMenu / MachineScreen). */
    private static final java.util.Map<com.bryanamaya.tecnocinetico.machine.MachineKind, RegistryObject<MenuType<MachineMenu>>> MACHINES =
            new java.util.EnumMap<>(com.bryanamaya.tecnocinetico.machine.MachineKind.class);

    static {
        for (var kind : com.bryanamaya.tecnocinetico.machine.MachineKind.values()) {
            MACHINES.put(kind, MENUS.register(kind.id, () -> IForgeMenuType.create((id, inv, buf) -> new MachineMenu(kind, id, inv, buf))));
        }
    }

    public static RegistryObject<MenuType<MachineMenu>> machine(com.bryanamaya.tecnocinetico.machine.MachineKind kind) {
        return MACHINES.get(kind);
    }

    private ModMenus() {}

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
